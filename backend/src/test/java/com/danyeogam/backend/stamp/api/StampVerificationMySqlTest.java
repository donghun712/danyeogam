package com.danyeogam.backend.stamp.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.danyeogam.backend.identity.application.SessionTokenCodec;
import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.SpotType;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-integration")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_STAMP_TESTS", matches = "true")
@Sql(
        scripts = "/sql/clean-database.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class StampVerificationMySqlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RegionRepository regionRepository;
    @Autowired private TouristSpotRepository spotRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private SessionTokenCodec tokenCodec;

    private TouristSpot firstSpot;
    private TouristSpot concurrentSpot;

    @BeforeEach
    void setUp() {
        Region region = regionRepository.save(Region.province("STAMP-REGION", "스탬프 테스트 지역"));
        firstSpot = stampSpot("STAMP-SPOT-1", "첫 관광지", region, 127.0, 37.0);
        concurrentSpot = stampSpot("STAMP-SPOT-2", "동시성 관광지", region, 127.001, 37.001);
        firstSpot = spotRepository.save(firstSpot);
        concurrentSpot = spotRepository.save(concurrentSpot);
    }

    @Test
    void verifiesAllStatusesIdempotencyRateLimitAndConcurrency() throws Exception {
        String token = issueSession();
        Long actorId = actorId(token);
        Instant now = Instant.now();
        String firstKey = UUID.randomUUID().toString();

        perform(token, firstKey, firstSpot.getId(), "37.0", "127.0", "5", now)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED_NEW"))
                .andExpect(jsonPath("$.data.visitState").value("VISITED"));
        assertThat(count("visit", actorId)).isOne();
        assertThat(count("verification_attempt", actorId)).isOne();

        perform(token, firstKey, firstSpot.getId(), "37.0", "127.0", "5", now)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED_NEW"));
        assertThat(count("verification_attempt", actorId)).isOne();

        perform(token, UUID.randomUUID().toString(), firstSpot.getId(), "37.0", "127.0", "5", now)
                .andExpect(jsonPath("$.data.status").value("VERIFIED_ALREADY_ACQUIRED"));
        perform(token, UUID.randomUUID().toString(), firstSpot.getId(), "37.01", "127.0", "5", now)
                .andExpect(jsonPath("$.data.status").value("OUT_OF_RANGE"));
        perform(token, UUID.randomUUID().toString(), firstSpot.getId(), "37.0", "127.0", "51", now)
                .andExpect(jsonPath("$.data.status").value("GPS_ACCURACY_INSUFFICIENT"));
        perform(token, UUID.randomUUID().toString(), firstSpot.getId(), "37.0", "127.0", "5", now.minusSeconds(121))
                .andExpect(jsonPath("$.data.status").value("LOCATION_STALE"));
        assertThat(count("verification_attempt", actorId)).isEqualTo(5);

        perform(token, UUID.randomUUID().toString(), firstSpot.getId(), "37.0", "127.0", "5", now)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));

        String secondToken = issueSession();
        Long secondActorId = actorId(secondToken);
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<String>> calls = List.of(
                    () -> responseBody(secondToken, concurrentSpot, UUID.randomUUID().toString()),
                    () -> responseBody(secondToken, concurrentSpot, UUID.randomUUID().toString())
            );
            List<String> bodies = pool.invokeAll(calls).stream()
                    .map(future -> {
                        try { return future.get(); } catch (Exception exception) { throw new RuntimeException(exception); }
                    })
                    .toList();
            assertThat(bodies).anyMatch(body -> body.contains("\"status\":\"VERIFIED_NEW\""));
            assertThat(bodies).anyMatch(body -> body.contains("\"status\":\"VERIFIED_ALREADY_ACQUIRED\""));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM visit WHERE actor_id = ? AND tourist_spot_id = ?",
                    Integer.class, secondActorId, concurrentSpot.getId()
            )).isOne();
        } finally {
            pool.shutdownNow();
        }
    }

    private TouristSpot stampSpot(String contentId, String name, Region region, double lon, double lat) {
        TouristSpot spot = TouristSpot.fromTourApi(
                contentId, "12", name, region, point(lon, lat), "d".repeat(64)
        );
        ReflectionTestUtils.setField(spot, "spotType", SpotType.STAMP_TARGET);
        ReflectionTestUtils.setField(spot, "stampEnabled", true);
        ReflectionTestUtils.setField(spot, "stampRadiusMeters", 100);
        return spot;
    }

    private String issueSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions/anonymous"))
                .andExpect(status().isOk()).andReturn();
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String pair = header.substring(0, header.indexOf(';'));
        return pair.substring(pair.indexOf('=') + 1);
    }

    private Long actorId(String token) {
        return jdbcTemplate.queryForObject(
                "SELECT actor_id FROM anonymous_session WHERE token_hash = ?",
                Long.class, tokenCodec.hash(token)
        );
    }

    private int count(String table, long actorId) {
        String sql = switch (table) {
            case "visit" -> "SELECT COUNT(*) FROM visit WHERE actor_id = ?";
            case "verification_attempt" -> "SELECT COUNT(*) FROM verification_attempt WHERE actor_id = ?";
            default -> throw new IllegalArgumentException("unsupported table");
        };
        return jdbcTemplate.queryForObject(sql, Integer.class, actorId);
    }

    private org.springframework.test.web.servlet.ResultActions perform(
            String token, String key, long spotId, String lat, String lon,
            String accuracy, Instant measuredAt
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/stamp-verifications")
                .cookie(new Cookie("dg_session", token))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(spotId, lat, lon, accuracy, measuredAt)));
    }

    private String responseBody(String token, TouristSpot spot, String key) throws Exception {
        return perform(token, key, spot.getId(), "37.001", "127.001", "5", Instant.now())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static String body(
            long spotId, String lat, String lon, String accuracy, Instant measuredAt
    ) {
        return """
                {"touristSpotId":%d,"position":{"latitude":%s,"longitude":%s,
                "accuracyMeters":%s,"measuredAt":"%s"}}
                """.formatted(spotId, lat, lon, accuracy, measuredAt);
    }

    private static Point point(double longitude, double latitude) {
        return new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(longitude, latitude));
    }
}

package com.danyeogam.backend.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.danyeogam.backend.identity.application.SessionTokenCodec;
import com.danyeogam.backend.touristspot.domain.Region;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-integration")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_SESSION_TESTS", matches = "true")
@Transactional
@Sql(
        scripts = "/sql/clean-database.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class AnonymousSessionMySqlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TouristSpotRepository spotRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SessionTokenCodec tokenCodec;

    private TouristSpot spot;

    @BeforeEach
    void setUp() {
        Region region = regionRepository.saveAndFlush(Region.province(
                "SESSION-REGION", "세션 테스트 지역"
        ));
        spot = TouristSpot.fromTourApi(
                "SESSION-SPOT", "12", "세션 테스트 관광지", region,
                point(127.0, 37.0), "c".repeat(64)
        );
        spot = spotRepository.saveAndFlush(spot);
    }

    @Test
    void issuesReusesHashedSessionAndConnectsVisitState() throws Exception {
        MvcResult firstIssue = mockMvc.perform(post("/api/v1/sessions/anonymous"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.data.actorType").value("ANONYMOUS"))
                .andExpect(jsonPath("$.data.expiresAt").isString())
                .andReturn();

        String rawToken = cookieValue(firstIssue.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(rawToken).hasSize(43).matches("[A-Za-z0-9_-]+");

        Long actorId = jdbcTemplate.queryForObject(
                "SELECT actor_id FROM anonymous_session WHERE token_hash = ?",
                Long.class,
                tokenCodec.hash(rawToken)
        );
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM actor WHERE id = ?", Integer.class, actorId
        )).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT OCTET_LENGTH(token_hash) FROM anonymous_session WHERE actor_id = ?",
                Integer.class,
                actorId
        )).isEqualTo(32);

        MvcResult reuse = mockMvc.perform(post("/api/v1/sessions/anonymous")
                        .cookie(new Cookie("dg_session", rawToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(cookieValue(reuse.getResponse().getHeader(HttpHeaders.SET_COOKIE)))
                .isEqualTo(rawToken);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM actor WHERE id = ?", Integer.class, actorId
        )).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM anonymous_session WHERE actor_id = ?", Integer.class, actorId
        )).isOne();

        mockMvc.perform(boundsRequest())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("public")))
                .andExpect(jsonPath("$.data.items[0].visitState").value("UNKNOWN"));

        mockMvc.perform(boundsRequest().cookie(new Cookie("dg_session", rawToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.data.items[0].visitState").value("NOT_VISITED"));

        long verificationAttemptId = insertVerificationAttempt(actorId, spot.getId());
        jdbcTemplate.update(
                """
                INSERT INTO visit (
                    actor_id, tourist_spot_id, verification_attempt_id,
                    verified_at, distance_meters
                ) VALUES (?, ?, ?, ?, ?)
                """,
                actorId,
                spot.getId(),
                verificationAttemptId,
                Timestamp.from(Instant.parse("2026-09-03T00:00:00Z")),
                10.25
        );

        mockMvc.perform(boundsRequest().cookie(new Cookie("dg_session", rawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].visitState").value("VISITED"));
        mockMvc.perform(get("/api/v1/tourist-spots/{spotId}", spot.getId())
                        .cookie(new Cookie("dg_session", rawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visitState").value("VISITED"));

        jdbcTemplate.update(
                "UPDATE anonymous_session SET expires_at = ? WHERE actor_id = ?",
                Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),
                actorId
        );
        mockMvc.perform(get("/api/v1/tourist-spots/{spotId}", spot.getId())
                        .cookie(new Cookie("dg_session", rawToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("public")))
                .andExpect(jsonPath("$.data.visitState").value("UNKNOWN"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder boundsRequest() {
        return get("/api/v1/tourist-spots")
                .param("northEastLatitude", "37.1")
                .param("northEastLongitude", "127.1")
                .param("southWestLatitude", "36.9")
                .param("southWestLongitude", "126.9");
    }

    private long insertVerificationAttempt(long actorId, long spotId) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO verification_attempt (
                        actor_id, tourist_spot_id, idempotency_key, result,
                        distance_meters, accuracy_meters, measured_at
                    ) VALUES (?, ?, ?, 'VERIFIED_NEW', ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, actorId);
            statement.setLong(2, spotId);
            statement.setString(3, UUID.randomUUID().toString());
            statement.setBigDecimal(4, new java.math.BigDecimal("10.25"));
            statement.setBigDecimal(5, new java.math.BigDecimal("5.00"));
            statement.setTimestamp(6, Timestamp.from(Instant.parse("2026-09-03T00:00:00Z")));
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private static String cookieValue(String setCookie) {
        assertThat(setCookie).isNotBlank();
        String pair = setCookie.substring(0, setCookie.indexOf(';'));
        return pair.substring(pair.indexOf('=') + 1);
    }

    private static Point point(double longitude, double latitude) {
        return new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(longitude, latitude));
    }
}

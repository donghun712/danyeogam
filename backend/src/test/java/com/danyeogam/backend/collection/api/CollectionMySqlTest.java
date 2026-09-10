package com.danyeogam.backend.collection.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-integration")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_COLLECTION_TESTS", matches = "true")
@Sql(
        scripts = "/sql/clean-database.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class CollectionMySqlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RegionRepository regionRepository;
    @Autowired private TouristSpotRepository spotRepository;

    private Region province;
    private Region district;
    private TouristSpot visitedSpot;

    @BeforeEach
    void setUp() {
        province = regionRepository.save(Region.province("TOUR:AREA:45", "가 지역"));
        district = regionRepository.save(Region.cityCounty(
                "TOUR:AREA:45:1", "가 시군구", province
        ));
        Region secondDistrict = regionRepository.save(Region.cityCounty(
                "TOUR:AREA:45:2", "나 시군구", province
        ));
        regionRepository.save(Region.province("TOUR:AREA:46", "나 빈지역"));
        visitedSpot = spotRepository.save(stampSpot("COLL-1", "가 방문", district, 127.0, 37.0));
        spotRepository.save(stampSpot("COLL-2", "나 미방문", district, 127.001, 37.001));
        spotRepository.save(stampSpot("COLL-3", "다 미방문", secondDistrict, 127.002, 37.002));
        spotRepository.save(TouristSpot.fromTourApi(
                "COLL-GENERAL", "12", "일반 관광지", district,
                point(127.003, 37.003), "e".repeat(64)
        ));
    }

    @Test
    void returnsFilteredCollectionAndZeroSafeRegionalSummary() throws Exception {
        String token = issueSession();
        mockMvc.perform(post("/api/v1/stamp-verifications")
                        .cookie(new Cookie("dg_session", token))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stampBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED_NEW"));

        mockMvc.perform(get("/api/v1/me/collection")
                        .cookie(new Cookie("dg_session", token))
                        .param("regionCode", province.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.count").value(3))
                .andExpect(jsonPath("$.data.items[0].name").value("가 방문"))
                .andExpect(jsonPath("$.data.items[0].visitState").value("VISITED"))
                .andExpect(jsonPath("$.data.items[0].verifiedAt").isString())
                .andExpect(jsonPath("$.data.items[1].visitState").value("NOT_VISITED"));

        mockMvc.perform(get("/api/v1/me/collection")
                        .cookie(new Cookie("dg_session", token))
                        .param("regionCode", province.getCode())
                        .param("status", "VISITED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.count").value(1));
        mockMvc.perform(get("/api/v1/me/collection")
                        .cookie(new Cookie("dg_session", token))
                        .param("regionCode", province.getCode())
                        .param("status", "NOT_VISITED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.count").value(2));

        mockMvc.perform(get("/api/v1/me/collection/summary")
                        .cookie(new Cookie("dg_session", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.regions[0].visitedCount").value(1))
                .andExpect(jsonPath("$.data.regions[0].totalCount").value(3))
                .andExpect(jsonPath("$.data.regions[0].progressPercent").value(33))
                .andExpect(jsonPath("$.data.regions[1].totalCount").value(0))
                .andExpect(jsonPath("$.data.regions[1].progressPercent").value(0));

        mockMvc.perform(get("/api/v1/me/collection/summary")
                        .cookie(new Cookie("dg_session", token))
                        .param("parentRegionCode", province.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.count").value(2))
                .andExpect(jsonPath("$.data.regions[0].code").value(district.getCode()))
                .andExpect(jsonPath("$.data.regions[0].visitedCount").value(1))
                .andExpect(jsonPath("$.data.regions[0].totalCount").value(2))
                .andExpect(jsonPath("$.data.regions[1].totalCount").value(1));

        mockMvc.perform(get("/api/v1/me/collection/summary"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/collection")
                        .cookie(new Cookie("dg_session", token))
                        .param("regionCode", province.getCode())
                        .param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_COLLECTION_STATUS"));
    }

    private String issueSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions/anonymous"))
                .andExpect(status().isOk()).andReturn();
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String pair = header.substring(0, header.indexOf(';'));
        return pair.substring(pair.indexOf('=') + 1);
    }

    private String stampBody() {
        return """
                {"touristSpotId":%d,"position":{"latitude":37.0,"longitude":127.0,
                "accuracyMeters":5,"measuredAt":"%s"}}
                """.formatted(visitedSpot.getId(), Instant.now());
    }

    private static TouristSpot stampSpot(
            String contentId, String name, Region region, double lon, double lat
    ) {
        TouristSpot spot = TouristSpot.fromTourApi(
                contentId, "12", name, region, point(lon, lat), "f".repeat(64)
        );
        ReflectionTestUtils.setField(spot, "spotType", SpotType.STAMP_TARGET);
        ReflectionTestUtils.setField(spot, "stampEnabled", true);
        return spot;
    }

    private static Point point(double longitude, double latitude) {
        return new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(longitude, latitude));
    }
}

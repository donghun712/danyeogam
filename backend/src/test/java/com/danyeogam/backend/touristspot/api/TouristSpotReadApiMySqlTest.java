package com.danyeogam.backend.touristspot.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.domain.TouristSpotImage;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotImageRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-integration")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_READ_API_TESTS", matches = "true")
@Transactional
@Sql(
        scripts = "/sql/clean-database.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class TouristSpotReadApiMySqlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TouristSpotRepository spotRepository;

    @Autowired
    private TouristSpotImageRepository imageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TouristSpot insideSpot;

    @BeforeEach
    void setUp() {
        Region province = regionRepository.saveAndFlush(Region.province("READ-1", "테스트광역시"));
        Region district = regionRepository.saveAndFlush(Region.cityCounty("READ-1-1", "테스트구", province));

        insideSpot = TouristSpot.fromTourApi(
                "READ-CONTENT-1", "12", "영역 안 관광지", district,
                point(127.0000, 37.0000), "a".repeat(64)
        );
        insideSpot.refreshSummary(
                "12", "영역 안 관광지", district, "테스트로 1", "테스트동 1",
                point(127.0000, 37.0000), "https://image.test/thumb.jpg",
                "https://image.test/original.jpg", "02-0000-0000",
                Instant.parse("2026-09-01T00:00:00Z"), "a".repeat(64)
        );
        insideSpot.hydrateDetail(
                "상세 설명", "https://example.com", "02-0000-0000",
                "https://image.test/thumb.jpg", "https://image.test/original.jpg",
                Instant.parse("2026-09-02T00:00:00Z")
        );
        insideSpot.hydrateIntro(
                "09:00~18:00", "연중무휴", "불가능", null,
                "가능", "없음", Instant.parse("2026-09-02T00:00:00Z")
        );
        insideSpot = spotRepository.saveAndFlush(insideSpot);
        imageRepository.saveAndFlush(new TouristSpotImage(
                insideSpot, "https://image.test/original.jpg", "관광지 전경", 0, "Type1", "READ-IMG-1"
        ));

        TouristSpot outsideSpot = TouristSpot.fromTourApi(
                "READ-CONTENT-2", "12", "영역 밖 관광지", district,
                point(128.0000, 38.0000), "b".repeat(64)
        );
        outsideSpot.refreshSummary(
                "12", "영역 밖 관광지", district, null, null,
                point(128.0000, 38.0000), null, null, null,
                Instant.parse("2026-09-01T00:00:00Z"), "b".repeat(64)
        );
        spotRepository.saveAndFlush(outsideSpot);
    }

    @Test
    void regionBoundsAndDetailEndpointsReadActualMySqlData() throws Exception {
        mockMvc.perform(get("/api/v1/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("테스트광역시"))
                .andExpect(jsonPath("$.data.items[0].children[0].name").value("테스트구"));

        mockMvc.perform(get("/api/v1/tourist-spots")
                        .param("northEastLatitude", "37.1")
                        .param("northEastLongitude", "127.1")
                        .param("southWestLatitude", "36.9")
                        .param("southWestLongitude", "126.9")
                        .param("types", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.count").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("영역 안 관광지"))
                .andExpect(jsonPath("$.data.items[0].position.latitude").value(37.0))
                .andExpect(jsonPath("$.data.items[0].position.longitude").value(127.0));

        mockMvc.perform(get("/api/v1/tourist-spots/{spotId}", insideSpot.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overview").value("상세 설명"))
                .andExpect(jsonPath("$.data.images[0].copyrightType").value("Type1"))
                .andExpect(jsonPath("$.data.navigation.coordinateType").value("wgs84"))
                .andExpect(jsonPath("$.data.dataQuality").value("COMPLETE"))
                .andExpect(jsonPath("$.data.operatingInfo.hours").value("09:00~18:00"))
                .andExpect(jsonPath("$.data.operatingInfo.closedDays").value("연중무휴"))
                .andExpect(jsonPath("$.data.facilityInfo.parking.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.facilityInfo.parking.note").value("불가능"))
                .andExpect(jsonPath("$.data.facilityInfo.strollerRental.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.facilityInfo.petAllowed.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.facilityInfo.petAllowed.note").value("없음"));

        List<String> usedKeys = jdbcTemplate.query(
                """
                EXPLAIN SELECT ts.id
                FROM tourist_spot ts FORCE INDEX (sx_tourist_spot_location)
                WHERE ts.active = TRUE
                  AND MBRContains(
                      ST_GeomFromText(?, 4326, 'axis-order=long-lat'),
                      ts.location
                  )
                LIMIT 1001
                """,
                (resultSet, rowNum) -> resultSet.getString("key"),
                "POLYGON((126.9 36.9,127.1 36.9,127.1 37.1,126.9 37.1,126.9 36.9))"
        );
        assertThat(usedKeys).contains("sx_tourist_spot_location");
    }

    private static Point point(double longitude, double latitude) {
        return new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(longitude, latitude));
    }
}

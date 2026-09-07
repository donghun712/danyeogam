package com.danyeogam.backend.touristspot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.domain.TouristSpotImage;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("mysql-integration")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
@Transactional
@Sql(
        scripts = "/sql/clean-database.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class TouristSpotRepositoryMySqlTest {

    private static final String HASH = "b".repeat(64);

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TouristSpotRepository touristSpotRepository;

    @Autowired
    private TouristSpotImageRepository imageRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void flywaySchemaAndJpaSpatialMappingsCanPersistAndReadTouristSpot() {
        Region province = regionRepository.saveAndFlush(Region.province("TEST-1", "테스트광역시"));
        Region district = regionRepository.saveAndFlush(Region.cityCounty("TEST-1-1", "테스트구", province));
        TouristSpot spot = touristSpotRepository.saveAndFlush(TouristSpot.fromTourApi(
                "TEST-CONTENT-1",
                "12",
                "테스트 관광지",
                district,
                point(127.1501, 35.8151),
                HASH
        ));
        imageRepository.saveAndFlush(new TouristSpotImage(
                spot,
                "https://image.test/tourist-spot.jpg",
                "테스트 관광지 전경",
                0,
                "Type1",
                "TEST-IMAGE-1"
        ));

        entityManager.clear();

        TouristSpot loaded = touristSpotRepository
                .findBySourceAndSourceContentId("TOUR_API", "TEST-CONTENT-1")
                .orElseThrow();
        assertThat(loaded.getLocation().getSRID()).isEqualTo(4326);
        assertThat(loaded.getLocation().getX()).isEqualTo(127.1501);
        assertThat(loaded.getLocation().getY()).isEqualTo(35.8151);
        assertThat(loaded.getRegion().getCode()).isEqualTo("TEST-1-1");
        assertThat(imageRepository.findAllByTouristSpotIdOrderBySortOrderAscIdAsc(loaded.getId()))
                .singleElement()
                .satisfies(image -> {
                    assertThat(image.getUrl()).isEqualTo("https://image.test/tourist-spot.jpg");
                    assertThat(image.getCopyrightType()).isEqualTo("Type1");
                });
    }

    private static Point point(double longitude, double latitude) {
        return new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(longitude, latitude));
    }
}

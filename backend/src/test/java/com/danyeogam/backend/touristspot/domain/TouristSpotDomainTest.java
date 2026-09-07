package com.danyeogam.backend.touristspot.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

class TouristSpotDomainTest {

    private static final String HASH = "a".repeat(64);

    @Test
    void createsTourApiSpotWithWgs84Coordinate() {
        Region province = Region.province("1", "서울특별시");
        TouristSpot spot = TouristSpot.fromTourApi(
                "126508",
                "12",
                "경복궁",
                province,
                point(126.9769, 37.5788, 4326),
                HASH
        );

        assertThat(spot.getSource()).isEqualTo("TOUR_API");
        assertThat(spot.getSpotType()).isEqualTo(SpotType.GENERAL);
        assertThat(spot.getCoordinateSource()).isEqualTo(CoordinateSource.TOUR_API);
        assertThat(spot.getLocation().getX()).isEqualTo(126.9769);
        assertThat(spot.getLocation().getY()).isEqualTo(37.5788);
        assertThat(spot.getDataHash()).isEqualTo(HASH);
    }

    @Test
    void rejectsCoordinatesThatAreNotWgs84() {
        Region province = Region.province("1", "서울특별시");

        assertThatThrownBy(() -> TouristSpot.fromTourApi(
                "126508", "12", "경복궁", province, point(126.9769, 37.5788, 0), HASH
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SRID는 4326");
    }

    @Test
    void enablesImportedSpotAsStampTargetWithConfiguredRadius() {
        TouristSpot spot = TouristSpot.fromTourApi(
                "126508", "12", "경복궁", Region.province("1", "서울특별시"),
                point(126.9769, 37.5788, 4326), HASH
        );

        spot.enableStampTarget(100);

        assertThat(spot.getSpotType()).isEqualTo(SpotType.STAMP_TARGET);
        assertThat(spot.isStampEnabled()).isTrue();
        assertThat(spot.getStampRadiusMeters()).isEqualTo(100);
        assertThatThrownBy(() -> spot.enableStampTarget(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enforcesRegionHierarchy() {
        Region province = Region.province("1", "서울특별시");
        Region district = Region.cityCounty("1-23", "종로구", province);

        assertThat(district.getParent()).isSameAs(province);
        assertThat(district.getLevel()).isEqualTo(RegionLevel.CITY_COUNTY);
        assertThatThrownBy(() -> Region.cityCounty("bad", "상위 없음", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void imageRejectsNegativeSortOrder() {
        Region province = Region.province("1", "서울특별시");
        TouristSpot spot = TouristSpot.fromTourApi(
                "126508", "12", "경복궁", province, point(126.9769, 37.5788, 4326), HASH
        );

        assertThatThrownBy(() -> new TouristSpotImage(
                spot, "https://image.test/palace.jpg", "경복궁", -1, "Type1", "123"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 이상");
    }

    private static Point point(double longitude, double latitude, int srid) {
        return new GeometryFactory(new PrecisionModel(), srid)
                .createPoint(new Coordinate(longitude, latitude));
    }
}

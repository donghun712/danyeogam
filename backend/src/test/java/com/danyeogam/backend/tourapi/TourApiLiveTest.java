package com.danyeogam.backend.tourapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_TOUR_API_TESTS", matches = "true")
class TourApiLiveTest {

    @Autowired
    private TourApiClient client;

    @Test
    void issuedKeyCanReadKoreanTouristDataAndImages() {
        TourApiPage<TouristSummary> summaries = client.getAreaBasedList(1, 30, null, "12");
        assertThat(summaries.totalCount()).isPositive();
        assertThat(summaries.items()).isNotEmpty();

        TouristSummary spotWithRepresentativeImage = summaries.items().stream()
                .filter(spot -> hasText(spot.firstImageUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("첫 페이지에서 대표 이미지가 있는 관광지를 찾지 못했습니다."));

        Optional<TouristImage> detailImage = client
                .getImages(spotWithRepresentativeImage.contentId(), 1, 20)
                .items()
                .stream()
                .filter(image -> hasText(image.originalUrl()))
                .findFirst();

        assertThat(detailImage)
                .as("detailImage2가 원본 이미지 URL을 반환해야 합니다")
                .isPresent();
    }

    @Test
    void issuedKeyCanReadTouristAndCultureIntroFields() {
        TouristIntro tourist = client.getIntroDetail("2818242", "12").orElseThrow();
        assertThat(tourist.operatingHours()).isEqualTo("상시 개방");
        assertThat(tourist.closedDays()).isEqualTo("연중무휴");

        TouristIntro culture = client.getIntroDetail("2549836", "14").orElseThrow();
        assertThat(culture.operatingHours()).isEqualTo("10:00~18:00");
        assertThat(culture.closedDays()).isEqualTo("연중무휴");
        assertThat(culture.parkingFeeNote()).isEqualTo("무료");
        assertThat(culture.strollerRentalNote()).isEqualTo("불가");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

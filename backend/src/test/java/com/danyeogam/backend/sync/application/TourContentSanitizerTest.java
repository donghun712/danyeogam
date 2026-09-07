package com.danyeogam.backend.sync.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TourContentSanitizerTest {

    private final TourContentSanitizer sanitizer = new TourContentSanitizer();

    @Test
    void convertsOverviewHtmlToPlainText() {
        assertThat(sanitizer.plainText("<p>경복궁 <script>alert(1)</script><b>소개</b></p>"))
                .isEqualTo("경복궁 소개");
    }

    @Test
    void extractsOnlyHttpHomepageUrl() {
        assertThat(sanitizer.homepageUrl("<a href=\"https://example.com/place\">홈페이지</a>"))
                .isEqualTo("https://example.com/place");
        assertThat(sanitizer.homepageUrl("javascript:alert(1)"))
                .isNull();
    }
}

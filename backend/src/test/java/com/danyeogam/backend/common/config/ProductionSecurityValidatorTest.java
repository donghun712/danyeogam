package com.danyeogam.backend.common.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.danyeogam.backend.common.web.ApiWebProperties;
import com.danyeogam.backend.identity.config.AnonymousSessionProperties;
import com.danyeogam.backend.kakao.KakaoLocalProperties;
import com.danyeogam.backend.tourapi.TourApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class ProductionSecurityValidatorTest {

    @Test
    void acceptsSecureProductionSettings() {
        assertThatCode(() -> validator(true, List.of("https://app.example.com"), false, false)
                .afterPropertiesSet()).doesNotThrowAnyException();
    }

    @Test
    void rejectsInsecureCookieOrCorsOriginOrEnabledDocs() {
        assertThatThrownBy(() -> validator(false, List.of(), false, false).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Secure=true");
        assertThatThrownBy(() -> validator(true, List.of("http://app.example.com"), false, false)
                .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTPS origin");
        assertThatThrownBy(() -> validator(true, List.of(), true, false).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Swagger");
    }

    @Test
    void rejectsExampleApiKeys() {
        TourApiProperties tourProperties = new TourApiProperties();
        tourProperties.setServiceKey("replace-with-data-go-kr-service-key");
        KakaoLocalProperties kakaoProperties = new KakaoLocalProperties();
        kakaoProperties.setRestApiKey("test-kakao-key");
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.datasource.password"))
                .thenReturn("a-long-production-password");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(
                new AnonymousSessionProperties(), new ApiWebProperties(), tourProperties,
                kakaoProperties, environment, false, false
        );
        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TourAPI");
    }

    private static ProductionSecurityValidator validator(
            boolean secureCookie,
            List<String> origins,
            boolean apiDocsEnabled,
            boolean swaggerEnabled
    ) {
        AnonymousSessionProperties session = new AnonymousSessionProperties();
        session.setCookieSecure(secureCookie);
        ApiWebProperties web = new ApiWebProperties();
        web.setAllowedOrigins(origins);
        TourApiProperties tour = new TourApiProperties();
        tour.setServiceKey("test-tour-key");
        KakaoLocalProperties kakao = new KakaoLocalProperties();
        kakao.setRestApiKey("test-kakao-key");
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.datasource.password"))
                .thenReturn("a-long-production-password");
        return new ProductionSecurityValidator(
                session, web, tour, kakao, environment, apiDocsEnabled, swaggerEnabled
        );
    }
}

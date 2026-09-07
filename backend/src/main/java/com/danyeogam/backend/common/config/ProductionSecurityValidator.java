package com.danyeogam.backend.common.config;

import java.net.URI;

import com.danyeogam.backend.common.web.ApiWebProperties;
import com.danyeogam.backend.identity.config.AnonymousSessionProperties;
import com.danyeogam.backend.kakao.KakaoLocalProperties;
import com.danyeogam.backend.tourapi.TourApiProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
public class ProductionSecurityValidator implements InitializingBean {

    private final AnonymousSessionProperties sessionProperties;
    private final ApiWebProperties webProperties;
    private final TourApiProperties tourApiProperties;
    private final KakaoLocalProperties kakaoProperties;
    private final Environment environment;
    private final boolean apiDocsEnabled;
    private final boolean swaggerUiEnabled;

    public ProductionSecurityValidator(
            AnonymousSessionProperties sessionProperties,
            ApiWebProperties webProperties,
            TourApiProperties tourApiProperties,
            KakaoLocalProperties kakaoProperties,
            Environment environment,
            @Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled,
            @Value("${springdoc.swagger-ui.enabled:false}") boolean swaggerUiEnabled
    ) {
        this.sessionProperties = sessionProperties;
        this.webProperties = webProperties;
        this.tourApiProperties = tourApiProperties;
        this.kakaoProperties = kakaoProperties;
        this.environment = environment;
        this.apiDocsEnabled = apiDocsEnabled;
        this.swaggerUiEnabled = swaggerUiEnabled;
    }

    @Override
    public void afterPropertiesSet() {
        require(sessionProperties.isCookieSecure(), "운영 세션 쿠키는 Secure=true여야 합니다.");
        require(webProperties.getAllowedOrigins().stream().allMatch(ProductionSecurityValidator::isHttpsOrigin),
                "운영 CORS origin은 정확한 HTTPS origin만 허용할 수 있습니다.");
        require(!apiDocsEnabled && !swaggerUiEnabled, "운영 환경에서는 Swagger와 OpenAPI 문서를 비활성화해야 합니다.");
        require(validSecret(tourApiProperties.getServiceKey()),
                "운영 TourAPI 서비스 키가 필요하며 예시값을 사용할 수 없습니다.");
        require(validSecret(kakaoProperties.getRestApiKey()),
                "운영 카카오 REST API 키가 필요하며 예시값을 사용할 수 없습니다.");
        require(isHttpsUrl(tourApiProperties.getBaseUrl()), "운영 TourAPI base URL은 HTTPS여야 합니다.");
        require(isHttpsUrl(kakaoProperties.getBaseUrl()), "운영 카카오 base URL은 HTTPS여야 합니다.");
        require(validSecret(environment.getProperty("spring.datasource.password")),
                "운영 데이터베이스 비밀번호가 필요하며 예시값을 사용할 수 없습니다.");
    }

    private static boolean isHttpsOrigin(String value) {
        if (!isHttpsUrl(value)) {
            return false;
        }
        URI uri = URI.create(value);
        return (uri.getPath() == null || uri.getPath().isEmpty())
                && uri.getQuery() == null && uri.getFragment() == null;
    }

    private static boolean isHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean validSecret(String value) {
        return StringUtils.hasText(value)
                && !value.toLowerCase(java.util.Locale.ROOT).contains("replace-with")
                && value.length() >= 12;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

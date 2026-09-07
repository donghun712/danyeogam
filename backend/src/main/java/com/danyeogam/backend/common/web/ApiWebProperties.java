package com.danyeogam.backend.common.web;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.web")
public class ApiWebProperties {

    private List<String> allowedOrigins = new ArrayList<>();
    private Duration hstsMaxAge = Duration.ofDays(365);

    public List<String> getAllowedOrigins() { return List.copyOf(allowedOrigins); }
    public void setAllowedOrigins(List<String> origins) {
        this.allowedOrigins = origins == null
                ? new ArrayList<>()
                : origins.stream().filter(value -> value != null && !value.isBlank())
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    @AssertTrue(message = "CORS 허용 출처는 경로 없는 정확한 HTTP(S) origin이어야 합니다.")
    public boolean isAllowedOriginsValid() {
        return allowedOrigins.stream().allMatch(ApiWebProperties::validOrigin);
    }

    public boolean allows(String origin) {
        return allowedOrigins.stream().anyMatch(value -> value.equalsIgnoreCase(origin));
    }

    public Duration getHstsMaxAge() {
        return hstsMaxAge;
    }

    public void setHstsMaxAge(Duration hstsMaxAge) {
        this.hstsMaxAge = hstsMaxAge;
    }

    @AssertTrue(message = "HSTS max-age는 0 이상이어야 합니다.")
    public boolean isHstsMaxAgeValid() {
        return hstsMaxAge != null && !hstsMaxAge.isNegative();
    }

    private static boolean validOrigin(String value) {
        try {
            URI uri = URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && (uri.getPath() == null || uri.getPath().isEmpty())
                    && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

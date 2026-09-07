package com.danyeogam.backend.identity.config;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.anonymous-session")
public class AnonymousSessionProperties {

    private static final Duration MIN_TTL = Duration.ofHours(1);
    private static final Duration MAX_TTL = Duration.ofDays(365);

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
    private String cookieName = "dg_session";

    private Duration ttl = Duration.ofDays(90);
    private Duration touchInterval = Duration.ofHours(1);
    private boolean cookieSecure = true;
    private String cookiePath = "/api";

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getTouchInterval() {
        return touchInterval;
    }

    public void setTouchInterval(Duration touchInterval) {
        this.touchInterval = touchInterval;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    @AssertTrue(message = "익명 세션 TTL은 1시간 이상 365일 이하여야 합니다.")
    public boolean isTtlValid() {
        return ttl != null && ttl.compareTo(MIN_TTL) >= 0 && ttl.compareTo(MAX_TTL) <= 0;
    }

    @AssertTrue(message = "세션 lastSeen 갱신 간격은 1분 이상이고 TTL보다 짧아야 합니다.")
    public boolean isTouchIntervalValid() {
        return touchInterval != null
                && !touchInterval.minus(Duration.ofMinutes(1)).isNegative()
                && ttl != null
                && touchInterval.compareTo(ttl) < 0;
    }

    @AssertTrue(message = "세션 쿠키 경로는 /api 또는 그 하위의 절대 경로여야 합니다.")
    public boolean isCookiePathValid() {
        return cookiePath != null
                && (cookiePath.equals("/api") || cookiePath.startsWith("/api/"))
                && !cookiePath.contains("..")
                && !cookiePath.contains(";");
    }
}

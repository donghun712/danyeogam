package com.danyeogam.backend.stamp.config;

import java.math.BigDecimal;
import java.time.Duration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.stamp-verification")
public class StampVerificationProperties {

    @NotNull
    @Min(1)
    @Max(10000)
    private Integer defaultRadiusMeters;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("10000.0")
    private BigDecimal maxAccuracyMeters;

    @NotNull
    private Duration maxLocationAge;

    @NotNull
    private Duration maxFutureSkew = Duration.ofSeconds(10);

    @Min(1)
    @Max(60)
    private int maxAttemptsPerMinute = 5;

    public Integer getDefaultRadiusMeters() { return defaultRadiusMeters; }
    public void setDefaultRadiusMeters(Integer value) { this.defaultRadiusMeters = value; }
    public BigDecimal getMaxAccuracyMeters() { return maxAccuracyMeters; }
    public void setMaxAccuracyMeters(BigDecimal value) { this.maxAccuracyMeters = value; }
    public Duration getMaxLocationAge() { return maxLocationAge; }
    public void setMaxLocationAge(Duration value) { this.maxLocationAge = value; }
    public Duration getMaxFutureSkew() { return maxFutureSkew; }
    public void setMaxFutureSkew(Duration value) { this.maxFutureSkew = value; }
    public int getMaxAttemptsPerMinute() { return maxAttemptsPerMinute; }
    public void setMaxAttemptsPerMinute(int value) { this.maxAttemptsPerMinute = value; }

    @AssertTrue(message = "위치 측정 유효시간은 1초 이상 1시간 이하여야 합니다.")
    public boolean isMaxLocationAgeValid() {
        return maxLocationAge != null
                && maxLocationAge.compareTo(Duration.ofSeconds(1)) >= 0
                && maxLocationAge.compareTo(Duration.ofHours(1)) <= 0;
    }

    @AssertTrue(message = "미래 시각 허용치는 0초 이상 1분 이하여야 합니다.")
    public boolean isMaxFutureSkewValid() {
        return maxFutureSkew != null
                && !maxFutureSkew.isNegative()
                && maxFutureSkew.compareTo(Duration.ofMinutes(1)) <= 0;
    }
}

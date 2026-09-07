package com.danyeogam.backend.parking.config;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.parking-query")
public class ParkingQueryProperties {

    @Min(1) @Max(20000)
    private int defaultRadiusMeters = 3000;

    @Min(1) @Max(20000)
    private int maxRadiusMeters = 20000;

    @Min(1) @Max(15)
    private int defaultLimit = 3;

    @Min(1) @Max(15)
    private int maxLimit = 15;

    @NotNull
    private Duration cacheTtl = Duration.ofMinutes(5);

    @Min(1) @Max(100000)
    private int cacheMaxEntries = 5000;

    public int getDefaultRadiusMeters() { return defaultRadiusMeters; }
    public void setDefaultRadiusMeters(int value) { this.defaultRadiusMeters = value; }
    public int getMaxRadiusMeters() { return maxRadiusMeters; }
    public void setMaxRadiusMeters(int value) { this.maxRadiusMeters = value; }
    public int getDefaultLimit() { return defaultLimit; }
    public void setDefaultLimit(int value) { this.defaultLimit = value; }
    public int getMaxLimit() { return maxLimit; }
    public void setMaxLimit(int value) { this.maxLimit = value; }
    public Duration getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(Duration value) { this.cacheTtl = value; }
    public int getCacheMaxEntries() { return cacheMaxEntries; }
    public void setCacheMaxEntries(int value) { this.cacheMaxEntries = value; }
}

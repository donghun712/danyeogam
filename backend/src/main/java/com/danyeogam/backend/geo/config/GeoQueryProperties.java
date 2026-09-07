package com.danyeogam.backend.geo.config;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.geo-query")
public class GeoQueryProperties {

    @NotNull
    private Duration reverseCacheTtl = Duration.ofMinutes(5);

    @Min(1)
    @Max(100000)
    private int reverseCacheMaxEntries = 10000;

    public Duration getReverseCacheTtl() { return reverseCacheTtl; }
    public void setReverseCacheTtl(Duration value) { this.reverseCacheTtl = value; }
    public int getReverseCacheMaxEntries() { return reverseCacheMaxEntries; }
    public void setReverseCacheMaxEntries(int value) { this.reverseCacheMaxEntries = value; }
}

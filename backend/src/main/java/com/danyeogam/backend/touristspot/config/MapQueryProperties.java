package com.danyeogam.backend.touristspot.config;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.map-query")
public class MapQueryProperties {

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("180.0")
    private BigDecimal maxLatitudeSpan = new BigDecimal("1.0");

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("360.0")
    private BigDecimal maxLongitudeSpan = new BigDecimal("1.0");

    @Min(1)
    @Max(5000)
    private int maxResults = 1000;

    public BigDecimal getMaxLatitudeSpan() { return maxLatitudeSpan; }
    public void setMaxLatitudeSpan(BigDecimal maxLatitudeSpan) { this.maxLatitudeSpan = maxLatitudeSpan; }
    public BigDecimal getMaxLongitudeSpan() { return maxLongitudeSpan; }
    public void setMaxLongitudeSpan(BigDecimal maxLongitudeSpan) { this.maxLongitudeSpan = maxLongitudeSpan; }
    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
}

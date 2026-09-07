package com.danyeogam.backend.common.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.api-rate-limit")
public class ApiRateLimitProperties {

    private boolean enabled = true;

    @Min(1) @Max(100000)
    private int requestsPerMinute = 600;

    @Min(1) @Max(10000)
    private int sessionCreatesPerMinute = 20;

    @Min(100) @Max(1000000)
    private int maxTrackedClients = 10000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int value) { this.requestsPerMinute = value; }
    public int getSessionCreatesPerMinute() { return sessionCreatesPerMinute; }
    public void setSessionCreatesPerMinute(int value) { this.sessionCreatesPerMinute = value; }
    public int getMaxTrackedClients() { return maxTrackedClients; }
    public void setMaxTrackedClients(int value) { this.maxTrackedClients = value; }
}

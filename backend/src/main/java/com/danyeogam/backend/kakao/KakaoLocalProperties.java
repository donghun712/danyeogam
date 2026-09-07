package com.danyeogam.backend.kakao;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "external-api.kakao-local")
public class KakaoLocalProperties {

    private String baseUrl = "https://dapi.kakao.com";
    private String restApiKey = "";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String value) { this.baseUrl = value; }
    public String getRestApiKey() { return restApiKey; }
    public void setRestApiKey(String value) { this.restApiKey = value; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration value) { this.connectTimeout = value; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration value) { this.readTimeout = value; }

    public boolean hasRestApiKey() {
        return restApiKey != null && !restApiKey.isBlank();
    }
}

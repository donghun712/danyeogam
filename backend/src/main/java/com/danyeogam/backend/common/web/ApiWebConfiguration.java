package com.danyeogam.backend.common.web;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiWebConfiguration implements WebMvcConfigurer {

    private final ApiWebProperties properties;
    private final WriteOriginInterceptor originInterceptor;
    private final ApiRateLimitInterceptor rateLimitInterceptor;

    public ApiWebConfiguration(
            ApiWebProperties properties,
            WriteOriginInterceptor originInterceptor,
            ApiRateLimitInterceptor rateLimitInterceptor
    ) {
        this.properties = properties;
        this.originInterceptor = originInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(originInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = properties.getAllowedOrigins();
        if (!origins.isEmpty()) {
            registry.addMapping("/api/**")
                    .allowedOrigins(origins.toArray(String[]::new))
                    .allowedMethods("GET", "POST", "OPTIONS")
                    .allowedHeaders("Content-Type", "Idempotency-Key", "X-Request-Id")
                    .exposedHeaders("X-Request-Id", "Retry-After")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }
}

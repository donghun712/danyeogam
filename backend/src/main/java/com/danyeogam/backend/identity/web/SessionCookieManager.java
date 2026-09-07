package com.danyeogam.backend.identity.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.danyeogam.backend.identity.config.AnonymousSessionProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieManager {

    private final AnonymousSessionProperties properties;
    private final Clock clock;

    public SessionCookieManager(AnonymousSessionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public ResponseCookie create(String rawToken, Instant expiresAt) {
        long remainingSeconds = Math.max(
                0,
                Duration.between(clock.instant(), expiresAt).getSeconds()
        );
        return ResponseCookie.from(properties.getCookieName(), rawToken)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(remainingSeconds))
                .build();
    }
}

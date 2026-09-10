package com.danyeogam.backend.common.web;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.danyeogam.backend.common.error.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiRateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_SECONDS = 60;
    private static final String SESSION_PATH = "/api/v1/sessions/anonymous";

    private final ApiRateLimitProperties properties;
    private final Clock clock;
    private final Map<ClientBucket, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    public ApiRateLimitInterceptor(ApiRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled() || "OPTIONS".equals(request.getMethod())) {
            return true;
        }

        long nowEpochSecond = clock.instant().getEpochSecond();
        if ((requestCounter.incrementAndGet() & 255) == 0) {
            windows.entrySet().removeIf(entry -> entry.getValue().expiresAtEpochSecond() <= nowEpochSecond);
        }

        BucketType type = isSessionCreation(request) ? BucketType.SESSION_CREATE : BucketType.GENERAL;
        int limit = type == BucketType.SESSION_CREATE
                ? properties.getSessionCreatesPerMinute()
                : properties.getRequestsPerMinute();
        ClientBucket key = new ClientBucket(normalizeAddress(request.getRemoteAddr()), type);
        AtomicBoolean allowed = new AtomicBoolean(false);
        AtomicLong retryAfterSeconds = new AtomicLong(WINDOW_SECONDS);

        windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAtEpochSecond() <= nowEpochSecond) {
                if (existing == null && windows.size() >= properties.getMaxTrackedClients()) {
                    return null;
                }
                allowed.set(true);
                return new Window(nowEpochSecond + WINDOW_SECONDS, 1);
            }
            if (existing.count() >= limit) {
                retryAfterSeconds.set(Math.max(1, existing.expiresAtEpochSecond() - nowEpochSecond));
                return existing;
            }
            allowed.set(true);
            return new Window(existing.expiresAtEpochSecond(), existing.count() + 1);
        });

        if (!allowed.get()) {
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds.get()));
            throw new RateLimitException(retryAfterSeconds.get());
        }
        return true;
    }

    private static boolean isSessionCreation(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && SESSION_PATH.equals(request.getRequestURI());
    }

    private static String normalizeAddress(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() <= 64 ? value : value.substring(0, 64);
    }

    private enum BucketType { GENERAL, SESSION_CREATE }
    private record ClientBucket(String address, BucketType type) { }
    private record Window(long expiresAtEpochSecond, int count) { }
}

package com.danyeogam.backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiRateLimitInterceptorTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-07T00:00:00Z"), ZoneOffset.UTC
    );

    @Test
    void sessionCreationUsesStricterLimitAndReturnsRetryAfter() {
        ApiRateLimitProperties properties = new ApiRateLimitProperties();
        properties.setRequestsPerMinute(10);
        properties.setSessionCreatesPerMinute(2);
        ApiRateLimitInterceptor interceptor = new ApiRateLimitInterceptor(properties, FIXED_CLOCK);

        MockHttpServletRequest request = request("POST", "/api/v1/sessions/anonymous", "192.0.2.10");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        assertThatThrownBy(() -> interceptor.preHandle(request, blockedResponse, new Object()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
        assertThat(blockedResponse.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
    }

    @Test
    void clientsAndBucketTypesAreIsolatedAndOptionsIsNotLimited() {
        ApiRateLimitProperties properties = new ApiRateLimitProperties();
        properties.setRequestsPerMinute(1);
        properties.setSessionCreatesPerMinute(1);
        ApiRateLimitInterceptor interceptor = new ApiRateLimitInterceptor(properties, FIXED_CLOCK);

        assertThat(interceptor.preHandle(
                request("GET", "/api/v1/regions", "192.0.2.10"),
                new MockHttpServletResponse(), new Object()
        )).isTrue();
        assertThat(interceptor.preHandle(
                request("GET", "/api/v1/regions", "192.0.2.11"),
                new MockHttpServletResponse(), new Object()
        )).isTrue();
        assertThat(interceptor.preHandle(
                request("POST", "/api/v1/sessions/anonymous", "192.0.2.10"),
                new MockHttpServletResponse(), new Object()
        )).isTrue();
        assertThat(interceptor.preHandle(
                request("OPTIONS", "/api/v1/regions", "192.0.2.10"),
                new MockHttpServletResponse(), new Object()
        )).isTrue();
    }

    private static MockHttpServletRequest request(String method, String path, String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}

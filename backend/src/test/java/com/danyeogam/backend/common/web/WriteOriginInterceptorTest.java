package com.danyeogam.backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class WriteOriginInterceptorTest {

    @Test
    void allowsSameOrConfiguredOriginAndRejectsCrossSiteWrite() {
        ApiWebProperties properties = new ApiWebProperties();
        properties.setAllowedOrigins(java.util.List.of("https://front.example"));
        WriteOriginInterceptor interceptor = new WriteOriginInterceptor(properties);
        MockHttpServletRequest same = request("http://localhost");
        assertThat(interceptor.preHandle(same, new MockHttpServletResponse(), new Object())).isTrue();
        MockHttpServletRequest configured = request("https://front.example");
        assertThat(interceptor.preHandle(configured, new MockHttpServletResponse(), new Object())).isTrue();
        MockHttpServletRequest malicious = request("https://evil.example");
        assertThatThrownBy(() -> interceptor.preHandle(
                malicious, new MockHttpServletResponse(), new Object()
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORIGIN_NOT_ALLOWED));
    }

    private static MockHttpServletRequest request(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/stamp-verifications");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(80);
        request.addHeader("Origin", origin);
        return request;
    }
}

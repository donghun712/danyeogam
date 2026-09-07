package com.danyeogam.backend.common.web;

import java.util.Set;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WriteOriginInterceptor implements HandlerInterceptor {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final ApiWebProperties properties;

    public WriteOriginInterceptor(ApiWebProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return true;
        }
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank() || sameOrigin(request, origin) || properties.allows(origin)) {
            return true;
        }
        throw new BusinessException(ErrorCode.ORIGIN_NOT_ALLOWED);
    }

    private static boolean sameOrigin(HttpServletRequest request, String origin) {
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(request.getScheme()) && port == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && port == 443);
        String requestOrigin = request.getScheme() + "://" + request.getServerName()
                + (defaultPort ? "" : ":" + port);
        return requestOrigin.equalsIgnoreCase(origin);
    }
}

package com.danyeogam.backend.common.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final ApiWebProperties properties;

    public SecurityHeadersFilter(ApiWebProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), payment=(), usb=()");
        response.setHeader("X-XSS-Protection", "0");

        String path = request.getRequestURI();
        if (path.startsWith("/api/") || path.startsWith("/actuator/")) {
            response.setHeader(
                    "Content-Security-Policy",
                    "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
            );
        }

        if (request.isSecure() && !properties.getHstsMaxAge().isZero()) {
            response.setHeader(
                    "Strict-Transport-Security",
                    "max-age=" + properties.getHstsMaxAge().toSeconds() + "; includeSubDomains"
            );
        }
        filterChain.doFilter(request, new NoStoreOnErrorResponse(response));
    }

    private static final class NoStoreOnErrorResponse extends HttpServletResponseWrapper {

        private NoStoreOnErrorResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int statusCode) {
            addNoStoreForError(statusCode);
            super.setStatus(statusCode);
        }

        @Override
        public void sendError(int statusCode) throws IOException {
            addNoStoreForError(statusCode);
            super.sendError(statusCode);
        }

        @Override
        public void sendError(int statusCode, String message) throws IOException {
            addNoStoreForError(statusCode);
            super.sendError(statusCode, message);
        }

        private void addNoStoreForError(int statusCode) {
            if (statusCode >= 400 && !containsHeader("Cache-Control")) {
                setHeader("Cache-Control", "no-store");
            }
        }
    }
}

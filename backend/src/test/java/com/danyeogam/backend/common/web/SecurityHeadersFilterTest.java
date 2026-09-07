package com.danyeogam.backend.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class SecurityHeadersFilterTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApiWebProperties properties = new ApiWebProperties();
        properties.setHstsMaxAge(Duration.ofDays(365));
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(new SecurityHeadersFilter(properties))
                .build();
    }

    @Test
    void apiResponseHasBrowserHardeningHeaders() throws Exception {
        mockMvc.perform(get("/api/test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), payment=(), usb=()"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                .andExpect(header().string("X-XSS-Protection", "0"));
    }

    @Test
    void secureResponseHasHstsButPlainHttpDoesNot() throws Exception {
        mockMvc.perform(get("/api/test").secure(true))
                .andExpect(header().string(
                        "Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains"
                ));
        mockMvc.perform(get("/api/test").secure(false))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void errorResponseIsNeverCached() throws Exception {
        mockMvc.perform(get("/api/test/error"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @RestController
    private static class TestController {
        @GetMapping("/api/test")
        String get() { return "ok"; }

        @GetMapping("/api/test/error")
        ResponseEntity<Void> error() { return ResponseEntity.status(403).build(); }
    }
}

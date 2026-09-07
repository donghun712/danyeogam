package com.danyeogam.backend.identity.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.danyeogam.backend.common.api.RequestIdFilter;
import com.danyeogam.backend.common.error.GlobalExceptionHandler;
import com.danyeogam.backend.identity.application.AnonymousSessionService;
import com.danyeogam.backend.identity.application.IssuedAnonymousSession;
import com.danyeogam.backend.identity.config.AnonymousSessionProperties;
import com.danyeogam.backend.identity.domain.ActorType;
import com.danyeogam.backend.identity.web.SessionCookieManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AnonymousSessionControllerTest {

    @Test
    void returnsSecureHttpOnlySameSiteCookieWithoutExposingTokenInJson() throws Exception {
        Instant now = Instant.parse("2026-09-03T00:00:00Z");
        Instant expiresAt = now.plusSeconds(3600);
        String oldToken = "A".repeat(43);
        String issuedToken = "B".repeat(43);
        AnonymousSessionService service = mock(AnonymousSessionService.class);
        when(service.issueOrReuse(oldToken)).thenReturn(new IssuedAnonymousSession(
                issuedToken, ActorType.ANONYMOUS, expiresAt
        ));
        AnonymousSessionProperties properties = new AnonymousSessionProperties();
        properties.setCookieSecure(true);
        SessionCookieManager cookieManager = new SessionCookieManager(
                properties, Clock.fixed(now, ZoneOffset.UTC)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new AnonymousSessionController(service, cookieManager)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        new ObjectMapper()
                                .findAndRegisterModules()
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                ))
                .addFilters(new RequestIdFilter())
                .build();

        mockMvc.perform(post("/api/v1/sessions/anonymous")
                        .cookie(new Cookie("dg_session", oldToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("dg_session=" + issuedToken)))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=3600")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.data.actorType").value("ANONYMOUS"))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-09-03T01:00:00Z"))
                .andExpect(jsonPath("$.data.rawToken").doesNotExist());

        verify(service).issueOrReuse(oldToken);
    }
}

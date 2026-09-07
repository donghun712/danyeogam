package com.danyeogam.backend.stamp.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.common.api.RequestIdFilter;
import com.danyeogam.backend.common.error.GlobalExceptionHandler;
import com.danyeogam.backend.identity.application.SessionActor;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import com.danyeogam.backend.identity.domain.ActorType;
import com.danyeogam.backend.stamp.application.StampVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StampVerificationControllerTest {

    private static final String KEY = "550e8400-e29b-41d4-a716-446655440000";
    private SessionActorResolver actorResolver;
    private StampVerificationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        actorResolver = mock(SessionActorResolver.class);
        service = mock(StampVerificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new StampVerificationController(actorResolver, service)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void requiresAnonymousSession() throws Exception {
        when(actorResolver.resolve(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/stamp-verifications")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void returnsBusinessResultAsHttp200AndPrivateNoStore() throws Exception {
        when(actorResolver.resolve(any())).thenReturn(Optional.of(new SessionActor(
                42L, ActorType.ANONYMOUS, Instant.parse("2026-12-01T00:00:00Z")
        )));
        when(service.verify(eq(42L), eq(KEY), any())).thenReturn(new StampVerificationResponse(
                "VERIFIED_NEW", 7L, new BigDecimal("12.34"),
                Instant.parse("2026-09-03T00:00:01Z"), "VISITED", true, List.of()
        ));

        mockMvc.perform(post("/api/v1/stamp-verifications")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.data.status").value("VERIFIED_NEW"))
                .andExpect(jsonPath("$.data.distanceMeters").value(12.34))
                .andExpect(jsonPath("$.data.visitState").value("VISITED"));
    }

    @Test
    void rejectsMissingHeaderAndInvalidCoordinatesAs400() throws Exception {
        mockMvc.perform(post("/api/v1/stamp-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/v1/stamp-verifications")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody().replace("37.0", "91.0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    private static String validBody() {
        return """
                {
                  "touristSpotId": 7,
                  "position": {
                    "latitude": 37.0,
                    "longitude": 127.0,
                    "accuracyMeters": 5.0,
                    "measuredAt": "2026-09-03T00:00:00Z"
                  }
                }
                """;
    }
}

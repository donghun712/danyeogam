package com.danyeogam.backend.title.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.common.api.RequestIdFilter;
import com.danyeogam.backend.common.error.GlobalExceptionHandler;
import com.danyeogam.backend.identity.application.SessionActor;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import com.danyeogam.backend.identity.domain.ActorType;
import com.danyeogam.backend.title.application.TitleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TitleControllerTest {

    private SessionActorResolver actorResolver;
    private TitleQueryService queryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        actorResolver = mock(SessionActorResolver.class);
        queryService = mock(TitleQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TitleController(actorResolver, queryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void requiresAnonymousSession() throws Exception {
        when(actorResolver.resolve(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/me/titles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void returnsAllTitlesWithPrivateNoStore() throws Exception {
        when(actorResolver.resolve(any())).thenReturn(Optional.of(new SessionActor(
                42L, ActorType.ANONYMOUS, Instant.parse("2026-12-01T00:00:00Z")
        )));
        when(queryService.getTitles(42L)).thenReturn(new TitleListResponse(List.of(
                new TitleItemResponse(
                        1L, "FIRST_STEP", "첫 걸음", "첫 번째 스탬프",
                        true, Instant.parse("2026-09-11T00:00:00Z"),
                        1, 1, "VISITS"
                ),
                new TitleItemResponse(
                        2L, "TRAVEL_RECORDER", "여행 기록가", "스탬프 10개",
                        false, null, 1, 10, "VISITS"
                )
        )));

        mockMvc.perform(get("/api/v1/me/titles"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.meta.count").value(2))
                .andExpect(jsonPath("$.data.titles[0].code").value("FIRST_STEP"))
                .andExpect(jsonPath("$.data.titles[0].earned").value(true))
                .andExpect(jsonPath("$.data.titles[1].currentValue").value(1))
                .andExpect(jsonPath("$.data.titles[1].targetValue").value(10));
    }
}

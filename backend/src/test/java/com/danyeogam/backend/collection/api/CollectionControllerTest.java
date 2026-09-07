package com.danyeogam.backend.collection.api;

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

import com.danyeogam.backend.collection.application.CollectionQueryService;
import com.danyeogam.backend.common.api.RequestIdFilter;
import com.danyeogam.backend.common.error.GlobalExceptionHandler;
import com.danyeogam.backend.identity.application.SessionActor;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import com.danyeogam.backend.identity.domain.ActorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CollectionControllerTest {

    private SessionActorResolver resolver;
    private CollectionQueryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        resolver = mock(SessionActorResolver.class);
        service = mock(CollectionQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CollectionController(resolver, service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void requiresSession() throws Exception {
        when(resolver.resolve(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/me/collection").param("regionCode", "TOUR:AREA:45"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void returnsPrivateCollectionAndCount() throws Exception {
        when(resolver.resolve(any())).thenReturn(Optional.of(new SessionActor(
                42L, ActorType.ANONYMOUS, Instant.parse("2026-12-01T00:00:00Z")
        )));
        when(service.getCollection(42L, "TOUR:AREA:45", "ALL")).thenReturn(new CollectionResponse(
                new CollectionRegionResponse("TOUR:AREA:45", "전북"),
                List.of(new CollectionItemResponse(7L, "경기전", "NOT_VISITED", null, null))
        ));
        mockMvc.perform(get("/api/v1/me/collection")
                        .param("regionCode", "TOUR:AREA:45"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.meta.count").value(1))
                .andExpect(jsonPath("$.data.items[0].visitState").value("NOT_VISITED"));
    }
}

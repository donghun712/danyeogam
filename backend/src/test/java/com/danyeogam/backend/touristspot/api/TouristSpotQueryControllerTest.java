package com.danyeogam.backend.touristspot.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.common.api.RequestIdFilter;
import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.common.error.GlobalExceptionHandler;
import com.danyeogam.backend.identity.application.SessionActor;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import com.danyeogam.backend.identity.domain.ActorType;
import com.danyeogam.backend.touristspot.application.MapBounds;
import com.danyeogam.backend.touristspot.application.TouristSpotQueryService;
import com.danyeogam.backend.touristspot.application.dto.PositionResponse;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotMapData;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotMapItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TouristSpotQueryControllerTest {

    private TouristSpotQueryService queryService;
    private SessionActorResolver actorResolver;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queryService = mock(TouristSpotQueryService.class);
        actorResolver = mock(SessionActorResolver.class);
        when(actorResolver.resolve(any())).thenReturn(Optional.empty());
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TouristSpotQueryController(queryService, actorResolver)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void mapEndpointReturnsLightweightItemsCountAndCacheHeader() throws Exception {
        when(queryService.getMapSpots(any(MapBounds.class), eq("GENERAL"), isNull())).thenReturn(
                new TouristSpotMapData(List.of(new TouristSpotMapItemResponse(
                        7L,
                        "경복궁",
                        new PositionResponse(new BigDecimal("37.5788"), new BigDecimal("126.9769")),
                        "GENERAL",
                        false,
                        "UNKNOWN",
                        "https://image.test/thumb.jpg"
                )))
        );

        mockMvc.perform(get("/api/v1/tourist-spots")
                        .header(RequestIdFilter.HEADER_NAME, "front-map-1")
                        .param("northEastLatitude", "37.7")
                        .param("northEastLongitude", "127.2")
                        .param("southWestLatitude", "37.4")
                        .param("southWestLongitude", "126.8")
                        .param("types", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(jsonPath("$.data.items[0].id").value(7))
                .andExpect(jsonPath("$.data.items[0].position.longitude").value(126.9769))
                .andExpect(jsonPath("$.data.items[0].visitState").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.items[0].overview").doesNotExist())
                .andExpect(jsonPath("$.meta.count").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("front-map-1"));
    }

    @Test
    void authenticatedMapResponseIsPrivateAndContainsVisitState() throws Exception {
        when(actorResolver.resolve(any())).thenReturn(Optional.of(new SessionActor(
                42L, ActorType.ANONYMOUS, Instant.parse("2026-12-01T00:00:00Z")
        )));
        when(queryService.getMapSpots(any(MapBounds.class), isNull(), eq(42L))).thenReturn(
                new TouristSpotMapData(List.of(new TouristSpotMapItemResponse(
                        7L,
                        "경복궁",
                        new PositionResponse(new BigDecimal("37.5788"), new BigDecimal("126.9769")),
                        "STAMP_TARGET",
                        true,
                        "VISITED",
                        null
                )))
        );

        mockMvc.perform(get("/api/v1/tourist-spots")
                        .param("northEastLatitude", "37.7")
                        .param("northEastLongitude", "127.2")
                        .param("southWestLatitude", "37.4")
                        .param("southWestLongitude", "126.8"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(jsonPath("$.data.items[0].visitState").value("VISITED"));

        verify(queryService).getMapSpots(any(MapBounds.class), isNull(), eq(42L));
    }

    @Test
    void missingCoordinateReturnsInvalidRequestInsteadOfServerError() throws Exception {
        mockMvc.perform(get("/api/v1/tourist-spots")
                        .param("northEastLatitude", "37.7")
                        .param("northEastLongitude", "127.2")
                        .param("southWestLatitude", "37.4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void reversedBoundsReturnStableBusinessError() throws Exception {
        mockMvc.perform(get("/api/v1/tourist-spots")
                        .param("northEastLatitude", "37.4")
                        .param("northEastLongitude", "127.2")
                        .param("southWestLatitude", "37.7")
                        .param("southWestLongitude", "126.8"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_BOUNDS"));
    }

    @Test
    void missingSpotReturnsTouristSpotNotFound() throws Exception {
        when(queryService.getDetail(999L, null))
                .thenThrow(new BusinessException(ErrorCode.TOURIST_SPOT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/tourist-spots/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TOURIST_SPOT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.retryable").value(false));
    }
}

package com.danyeogam.backend.parking.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.danyeogam.backend.common.api.RequestIdFilter;
import com.danyeogam.backend.common.error.GlobalExceptionHandler;
import com.danyeogam.backend.parking.application.ParkingQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ParkingControllerTest {

    private ParkingQueryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ParkingQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ParkingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void returnsIndependentUnavailableStateWithoutCachingIt() throws Exception {
        when(service.findNearby(7L, 3000, 3))
                .thenReturn(new ParkingResponse(List.of(), true));

        mockMvc.perform(get("/api/v1/tourist-spots/7/parking")
                        .param("radiusMeters", "3000")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.data.temporarilyUnavailable").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.meta.count").value(0));
    }

    @Test
    void rejectsNonPositiveQueryParameters() throws Exception {
        mockMvc.perform(get("/api/v1/tourist-spots/7/parking")
                        .param("radiusMeters", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}

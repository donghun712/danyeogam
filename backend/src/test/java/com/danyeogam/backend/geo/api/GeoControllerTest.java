package com.danyeogam.backend.geo.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danyeogam.backend.common.api.RequestIdFilter;
import com.danyeogam.backend.common.error.GlobalExceptionHandler;
import com.danyeogam.backend.geo.application.GeoQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GeoControllerTest {

    private GeoQueryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(GeoQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GeoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void returnsAddressWithoutCachingExactGps() throws Exception {
        when(service.reverse(new java.math.BigDecimal("35.8242"), new java.math.BigDecimal("127.1480")))
                .thenReturn(new ReverseGeoResponse(
                        "전북 전주시 완산구", "전북 전주시 완산구 태조로 1",
                        new AddressRegionResponse("전북특별자치도", "전주시 완산구", "풍남동"),
                        "KAKAO_LOCAL"
                ));

        mockMvc.perform(post("/api/v1/geo/reverse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"position":{"latitude":35.8242,"longitude":127.1480}}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.data.region.depth1").value("전북특별자치도"))
                .andExpect(jsonPath("$.data.source").value("KAKAO_LOCAL"));
    }

    @Test
    void rejectsMissingOrOutOfRangePosition() throws Exception {
        mockMvc.perform(post("/api/v1/geo/reverse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post("/api/v1/geo/reverse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"position":{"latitude":91,"longitude":127}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
}

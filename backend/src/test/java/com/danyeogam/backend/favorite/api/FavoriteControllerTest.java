package com.danyeogam.backend.favorite.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danyeogam.backend.common.api.RequestIdFilter;
import com.danyeogam.backend.common.error.GlobalExceptionHandler;
import com.danyeogam.backend.favorite.application.FavoriteService;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FavoriteControllerTest {

    private SessionActorResolver resolver;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        resolver = mock(SessionActorResolver.class);
        FavoriteService service = mock(FavoriteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new FavoriteController(resolver, service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void allEndpointsRequireAnonymousSession() throws Exception {
        when(resolver.resolve(any())).thenReturn(java.util.Optional.empty());

        mockMvc.perform(post("/api/v1/tourist-spots/7/favorite"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(delete("/api/v1/tourist-spots/7/favorite"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/favorites"))
                .andExpect(status().isUnauthorized());
    }
}

package com.danyeogam.backend.favorite.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-integration")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_FAVORITE_TESTS", matches = "true")
@Sql(
        scripts = "/sql/clean-database.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class FavoriteMySqlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RegionRepository regionRepository;
    @Autowired private TouristSpotRepository touristSpotRepository;

    private TouristSpot spot;

    @BeforeEach
    void setUp() {
        Region province = regionRepository.save(Region.province("TOUR:AREA:52", "전북특별자치도"));
        spot = touristSpotRepository.save(TouristSpot.fromTourApi(
                "FAVORITE-1", "12", "경기전", province,
                new GeometryFactory(new PrecisionModel(), 4326)
                        .createPoint(new Coordinate(127.148, 35.814)),
                "a".repeat(64)
        ));
    }

    @Test
    void addsListsReportsDetailStateAndRemovesIdempotently() throws Exception {
        String token = issueSession();
        Cookie cookie = new Cookie("dg_session", token);

        mockMvc.perform(post("/api/v1/tourist-spots/{id}/favorite", spot.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));
        mockMvc.perform(post("/api/v1/tourist-spots/{id}/favorite", spot.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));

        mockMvc.perform(get("/api/v1/me/favorites").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.count").value(1))
                .andExpect(jsonPath("$.data.items[0].touristSpotId").value(spot.getId()))
                .andExpect(jsonPath("$.data.items[0].visitState").value("NOT_VISITED"));
        mockMvc.perform(get("/api/v1/tourist-spots/{id}", spot.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));

        mockMvc.perform(delete("/api/v1/tourist-spots/{id}/favorite", spot.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false));
        mockMvc.perform(delete("/api/v1/tourist-spots/{id}/favorite", spot.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false));
        mockMvc.perform(get("/api/v1/me/favorites").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.count").value(0));
    }

    @Test
    void rejectsMissingSession() throws Exception {
        mockMvc.perform(post("/api/v1/tourist-spots/{id}/favorite", spot.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    private String issueSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions/anonymous"))
                .andExpect(status().isOk()).andReturn();
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String pair = header.substring(0, header.indexOf(';'));
        return pair.substring(pair.indexOf('=') + 1);
    }
}

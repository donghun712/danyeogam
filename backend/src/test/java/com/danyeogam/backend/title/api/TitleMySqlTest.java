package com.danyeogam.backend.title.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.danyeogam.backend.identity.application.SessionTokenCodec;
import com.danyeogam.backend.stamp.domain.VerificationAttempt;
import com.danyeogam.backend.stamp.domain.VerificationResult;
import com.danyeogam.backend.stamp.repository.VerificationAttemptRepository;
import com.danyeogam.backend.title.application.TitleDefinitionSeedService;
import com.danyeogam.backend.title.domain.TitleDefinition;
import com.danyeogam.backend.title.repository.ActorTitleRepository;
import com.danyeogam.backend.title.repository.TitleDefinitionRepository;
import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.RegionLevel;
import com.danyeogam.backend.touristspot.domain.SpotType;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import com.danyeogam.backend.visit.domain.Visit;
import com.danyeogam.backend.visit.repository.VisitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-integration")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TITLE_TESTS", matches = "true")
@Sql(
        scripts = "/sql/clean-database.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class TitleMySqlTest {

    private static final Instant PREVIOUS_VISIT_TIME = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SessionTokenCodec tokenCodec;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RegionRepository regionRepository;
    @Autowired private TouristSpotRepository spotRepository;
    @Autowired private VerificationAttemptRepository attemptRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private TitleDefinitionRepository definitionRepository;
    @Autowired private ActorTitleRepository actorTitleRepository;
    @Autowired private TitleDefinitionSeedService seedService;

    @BeforeEach
    void seedGlobalDefinitions() {
        definitionRepository.saveAllAndFlush(globalDefinitions());
    }

    @Test
    void stampAwardsAllSevenGlobalConditionsAndReplayKeepsNewTitleIds() throws Exception {
        List<Region> provinces = new ArrayList<>();
        List<Region> districts = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            Region province = regionRepository.save(Region.province(
                    "TOUR:AREA:T" + index, "테스트 광역 " + index
            ));
            provinces.add(province);
            districts.add(regionRepository.save(Region.cityCounty(
                    "TOUR:AREA:T" + index + ":A", "테스트 시군구 " + index + "A", province
            )));
        }
        districts.add(regionRepository.save(Region.cityCounty(
                "TOUR:AREA:T0:B", "테스트 시군구 0B", provinces.get(0)
        )));
        districts.add(regionRepository.save(Region.cityCounty(
                "TOUR:AREA:T1:B", "테스트 시군구 1B", provinces.get(1)
        )));

        List<TouristSpot> spots = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            Region district = districts.get(index % districts.size());
            String level2 = index < 14 || index == 49 ? "HS01" : null;
            String level3 = (index >= 14 && index < 21) || index == 49
                    ? "VE070100" : null;
            spots.add(spotRepository.save(stampSpot(
                    "TITLE-GLOBAL-" + index,
                    "칭호 관광지 " + index,
                    district,
                    level2,
                    level3,
                    127.0,
                    37.0
            )));
        }
        String token = issueSession();
        long actorId = actorId(token);
        for (int index = 0; index < 49; index++) {
            recordVisit(actorId, spots.get(index), PREVIOUS_VISIT_TIME.plusSeconds(index));
        }
        seedService.synchronizeRegionTitles();
        Set<Long> globalTitleIds = definitionRepository
                .findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .filter(definition -> !definition.getCode().startsWith("REGION_MASTER:"))
                .map(TitleDefinition::getId)
                .collect(java.util.stream.Collectors.toSet());
        String idempotencyKey = UUID.randomUUID().toString();

        MvcResult firstResult = performStamp(token, idempotencyKey, spots.get(49))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED_NEW"))
                .andReturn();
        Set<Long> firstAwardIds = titleIds(firstResult);

        assertThat(firstAwardIds).containsAll(globalTitleIds);
        assertThat(actorTitleRepository.findTitleDefinitionIdsByActorId(actorId))
                .containsAll(globalTitleIds);

        MvcResult replayResult = performStamp(token, idempotencyKey, spots.get(49))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED_NEW"))
                .andReturn();
        assertThat(titleIds(replayResult)).isEqualTo(firstAwardIds);

        mockMvc.perform(get("/api/v1/me/titles").cookie(new Cookie("dg_session", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titles[?(@.code == 'FIRST_STEP')].earned").value(true))
                .andExpect(jsonPath("$.data.titles[?(@.code == 'DANYEOGAM_RECORDER')].earned").value(true))
                .andExpect(jsonPath("$.data.titles[?(@.code == 'EIGHT_PROVINCE_TRAVELER')].earned").value(true))
                .andExpect(jsonPath("$.data.titles[?(@.code == 'EVERY_CORNER_EXPLORER')].earned").value(true))
                .andExpect(jsonPath("$.data.titles[?(@.code == 'HISTORY_FOOTPRINT')].earned").value(true))
                .andExpect(jsonPath("$.data.titles[?(@.code == 'CULTURE_COLLECTOR')].earned").value(true));
    }

    @Test
    void oneStampCanAwardMultipleProvinceMasterTitles() throws Exception {
        Region firstProvince = regionRepository.save(Region.province("TOUR:AREA:A", "가 광역"));
        Region secondProvince = regionRepository.save(Region.province("TOUR:AREA:B", "나 광역"));
        Region firstDistrict = regionRepository.save(Region.cityCounty(
                "TOUR:AREA:A:1", "가 시군구", firstProvince
        ));
        Region secondDistrict = regionRepository.save(Region.cityCounty(
                "TOUR:AREA:B:1", "나 시군구", secondProvince
        ));
        List<TouristSpot> firstSpots = createFiveSpots("A", firstDistrict);
        List<TouristSpot> secondSpots = createFiveSpots("B", secondDistrict);
        String token = issueSession();
        long actorId = actorId(token);
        recordVisit(actorId, secondSpots.get(0), PREVIOUS_VISIT_TIME);
        seedService.synchronizeRegionTitles();
        Set<Long> expectedRegionalIds = definitionRepository
                .findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .filter(definition -> definition.getCode().equals("REGION_MASTER:TOUR:AREA:A")
                        || definition.getCode().equals("REGION_MASTER:TOUR:AREA:B"))
                .map(TitleDefinition::getId)
                .collect(java.util.stream.Collectors.toSet());

        MvcResult result = performStamp(
                token, UUID.randomUUID().toString(), firstSpots.get(0)
        ).andExpect(status().isOk()).andReturn();

        assertThat(expectedRegionalIds).hasSize(2);
        assertThat(titleIds(result)).containsAll(expectedRegionalIds);
        mockMvc.perform(get("/api/v1/me/titles").cookie(new Cookie("dg_session", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titles[?(@.code == 'REGION_MASTER:TOUR:AREA:A')].earned")
                        .value(true))
                .andExpect(jsonPath("$.data.titles[?(@.code == 'REGION_MASTER:TOUR:AREA:B')].earned")
                        .value(true));
    }

    private List<TouristSpot> createFiveSpots(String prefix, Region region) {
        List<TouristSpot> spots = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            spots.add(spotRepository.save(stampSpot(
                    "TITLE-REGION-" + prefix + index,
                    "지역 관광지 " + prefix + index,
                    region,
                    null,
                    null,
                    127.0,
                    37.0
            )));
        }
        return spots;
    }

    private void recordVisit(long actorId, TouristSpot spot, Instant at) {
        VerificationAttempt attempt = attemptRepository.saveAndFlush(new VerificationAttempt(
                actorId,
                spot.getId(),
                UUID.randomUUID().toString(),
                VerificationResult.VERIFIED_NEW,
                BigDecimal.ZERO,
                new BigDecimal("5.00"),
                at,
                at
        ));
        visitRepository.saveAndFlush(new Visit(
                actorId, spot.getId(), attempt.getId(), at, BigDecimal.ZERO
        ));
    }

    private org.springframework.test.web.servlet.ResultActions performStamp(
            String token, String idempotencyKey, TouristSpot spot
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/stamp-verifications")
                .cookie(new Cookie("dg_session", token))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"touristSpotId":%d,"position":{"latitude":37.0,"longitude":127.0,
                        "accuracyMeters":5,"measuredAt":"%s"}}
                        """.formatted(spot.getId(), Instant.now())));
    }

    private Set<Long> titleIds(MvcResult result) throws Exception {
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("data").path("newTitleIds");
        Set<Long> ids = new HashSet<>();
        items.forEach(item -> ids.add(item.longValue()));
        return ids;
    }

    private String issueSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions/anonymous"))
                .andExpect(status().isOk()).andReturn();
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String pair = header.substring(0, header.indexOf(';'));
        return pair.substring(pair.indexOf('=') + 1);
    }

    private long actorId(String token) {
        return jdbcTemplate.queryForObject(
                "SELECT actor_id FROM anonymous_session WHERE token_hash = ?",
                Long.class,
                tokenCodec.hash(token)
        );
    }

    private static TouristSpot stampSpot(
            String contentId,
            String name,
            Region region,
            String classificationLevel2,
            String classificationLevel3,
            double longitude,
            double latitude
    ) {
        TouristSpot spot = TouristSpot.fromTourApi(
                contentId, "12", name, region,
                point(longitude, latitude), "a".repeat(64)
        );
        spot.refreshClassification(
                "12",
                classificationLevel2 == null && classificationLevel3 != null ? "VE" : "HS",
                classificationLevel2 == null && classificationLevel3 != null
                        ? "VE07" : classificationLevel2,
                classificationLevel3
        );
        ReflectionTestUtils.setField(spot, "spotType", SpotType.STAMP_TARGET);
        ReflectionTestUtils.setField(spot, "stampEnabled", true);
        return spot;
    }

    private static Point point(double longitude, double latitude) {
        return new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(longitude, latitude));
    }

    private static List<TitleDefinition> globalDefinitions() {
        return List.of(
                TitleDefinition.visitCount("FIRST_STEP", "첫 걸음", "첫 번째 스탬프", 1, 1),
                TitleDefinition.visitCount("TRAVEL_RECORDER", "여행 기록가", "스탬프 10개", 10, 2),
                TitleDefinition.visitCount("DANYEOGAM_RECORDER", "다녀감 기록가", "스탬프 50개", 50, 3),
                TitleDefinition.distinctRegionCount(
                        "EIGHT_PROVINCE_TRAVELER", "팔도 여행자", "광역 8곳",
                        RegionLevel.PROVINCE, 8, 4
                ),
                TitleDefinition.distinctRegionCount(
                        "EVERY_CORNER_EXPLORER", "구석구석 탐험가", "시군구 10곳",
                        RegionLevel.CITY_COUNTY, 10, 5
                ),
                TitleDefinition.classificationVisitCount(
                        "HISTORY_FOOTPRINT", "역사 발자국", "역사유적 15곳",
                        2, List.of("HS01"), 15, 6
                ),
                TitleDefinition.classificationVisitCount(
                        "CULTURE_COLLECTOR", "문화 수집가", "문화시설 8곳",
                        3, List.of("VE070100", "VE070200", "VE070600"), 8, 7
                )
        );
    }
}

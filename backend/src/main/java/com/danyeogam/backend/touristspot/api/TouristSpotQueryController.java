package com.danyeogam.backend.touristspot.api;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import com.danyeogam.backend.common.api.ApiResponse;
import com.danyeogam.backend.identity.application.SessionActor;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import com.danyeogam.backend.touristspot.application.MapBounds;
import com.danyeogam.backend.touristspot.application.TouristSpotQueryService;
import com.danyeogam.backend.touristspot.application.dto.RegionListData;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotDetailResponse;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotMapData;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Profile("!test")
@Tag(name = "관광지", description = "지역, 지도 영역 관광지와 관광지 상세 조회")
public class TouristSpotQueryController {

    private final TouristSpotQueryService queryService;
    private final SessionActorResolver actorResolver;

    public TouristSpotQueryController(
            TouristSpotQueryService queryService,
            SessionActorResolver actorResolver
    ) {
        this.queryService = queryService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/regions")
    @Operation(summary = "지역 목록 조회", description = "활성 광역·시군구 지역 계층을 반환합니다.")
    public ResponseEntity<ApiResponse<RegionListData>> getRegions() {
        RegionListData data = queryService.getRegions();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(ApiResponse.success(data, data.items().size()));
    }

    @GetMapping("/tourist-spots")
    @Operation(summary = "지도 영역 관광지 조회", description = "화면 bounds 안의 경량 관광지 마커 데이터를 반환합니다.")
    public ResponseEntity<ApiResponse<TouristSpotMapData>> getMapSpots(
            @RequestParam BigDecimal northEastLatitude,
            @RequestParam BigDecimal northEastLongitude,
            @RequestParam BigDecimal southWestLatitude,
            @RequestParam BigDecimal southWestLongitude,
            @RequestParam(required = false) String types,
            HttpServletRequest request
    ) {
        Optional<SessionActor> actor = actorResolver.resolve(request);
        TouristSpotMapData data = queryService.getMapSpots(
                new MapBounds(
                        northEastLatitude,
                        northEastLongitude,
                        southWestLatitude,
                        southWestLongitude
                ),
                types,
                actor.map(SessionActor::actorId).orElse(null)
        );
        return ResponseEntity.ok()
                .cacheControl(cacheControl(actor, Duration.ofSeconds(60)))
                .body(ApiResponse.success(data, data.items().size()));
    }

    @GetMapping("/tourist-spots/{spotId}")
    @Operation(summary = "관광지 상세 조회", description = "상세 정보, 이미지, 방문 상태와 내비 목적지를 반환합니다.")
    public ResponseEntity<ApiResponse<TouristSpotDetailResponse>> getDetail(
            @PathVariable long spotId,
            HttpServletRequest request
    ) {
        Optional<SessionActor> actor = actorResolver.resolve(request);
        TouristSpotDetailResponse data = queryService.getDetail(
                spotId,
                actor.map(SessionActor::actorId).orElse(null)
        );
        return ResponseEntity.ok()
                .cacheControl(cacheControl(actor, Duration.ofMinutes(5)))
                .body(ApiResponse.success(data));
    }

    private static CacheControl cacheControl(Optional<SessionActor> actor, Duration publicMaxAge) {
        return actor.isPresent()
                ? CacheControl.noStore().cachePrivate()
                : CacheControl.maxAge(publicMaxAge).cachePublic();
    }
}

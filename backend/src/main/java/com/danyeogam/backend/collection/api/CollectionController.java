package com.danyeogam.backend.collection.api;

import com.danyeogam.backend.collection.application.CollectionQueryService;
import com.danyeogam.backend.common.api.ApiResponse;
import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.identity.application.SessionActor;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/collection")
@Profile("!test")
@Tag(name = "도감", description = "익명 사용자별 지역 도감과 진행률")
public class CollectionController {

    private final SessionActorResolver actorResolver;
    private final CollectionQueryService queryService;

    public CollectionController(
            SessionActorResolver actorResolver,
            CollectionQueryService queryService
    ) {
        this.actorResolver = actorResolver;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "지역 도감 조회", security = @SecurityRequirement(name = "anonymousSession"))
    public ResponseEntity<ApiResponse<CollectionResponse>> getCollection(
            @RequestParam String regionCode,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletRequest request
    ) {
        SessionActor actor = requireActor(request);
        CollectionResponse data = queryService.getCollection(actor.actorId(), regionCode, status);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(data, data.items().size()));
    }

    @GetMapping("/summary")
    @Operation(summary = "지역별 도감 진행률 조회", security = @SecurityRequirement(name = "anonymousSession"))
    public ResponseEntity<ApiResponse<CollectionSummaryResponse>> getSummary(
            @RequestParam(required = false) String parentRegionCode,
            HttpServletRequest request
    ) {
        SessionActor actor = requireActor(request);
        CollectionSummaryResponse data = queryService.getSummary(actor.actorId(), parentRegionCode);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(data, data.regions().size()));
    }

    private SessionActor requireActor(HttpServletRequest request) {
        return actorResolver.resolve(request)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED));
    }
}

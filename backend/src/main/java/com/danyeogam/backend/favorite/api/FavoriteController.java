package com.danyeogam.backend.favorite.api;

import com.danyeogam.backend.common.api.ApiResponse;
import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.favorite.application.FavoriteService;
import com.danyeogam.backend.identity.application.SessionActor;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Profile("!test")
@Tag(name = "즐겨찾기", description = "익명 사용자별 관광지 즐겨찾기")
public class FavoriteController {

    private final SessionActorResolver actorResolver;
    private final FavoriteService favoriteService;

    public FavoriteController(SessionActorResolver actorResolver, FavoriteService favoriteService) {
        this.actorResolver = actorResolver;
        this.favoriteService = favoriteService;
    }

    @PostMapping("/tourist-spots/{touristSpotId}/favorite")
    @Operation(summary = "즐겨찾기 추가", security = @SecurityRequirement(name = "anonymousSession"))
    public ResponseEntity<ApiResponse<FavoriteStateResponse>> add(
            @PathVariable long touristSpotId,
            HttpServletRequest request
    ) {
        SessionActor actor = requireActor(request);
        return privateResponse(favoriteService.add(actor.actorId(), touristSpotId));
    }

    @DeleteMapping("/tourist-spots/{touristSpotId}/favorite")
    @Operation(summary = "즐겨찾기 해제", security = @SecurityRequirement(name = "anonymousSession"))
    public ResponseEntity<ApiResponse<FavoriteStateResponse>> remove(
            @PathVariable long touristSpotId,
            HttpServletRequest request
    ) {
        SessionActor actor = requireActor(request);
        return privateResponse(favoriteService.remove(actor.actorId(), touristSpotId));
    }

    @GetMapping("/me/favorites")
    @Operation(summary = "내 즐겨찾기 목록", security = @SecurityRequirement(name = "anonymousSession"))
    public ResponseEntity<ApiResponse<FavoriteListResponse>> getFavorites(HttpServletRequest request) {
        SessionActor actor = requireActor(request);
        FavoriteListResponse data = favoriteService.getFavorites(actor.actorId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(data, data.items().size()));
    }

    private SessionActor requireActor(HttpServletRequest request) {
        return actorResolver.resolve(request)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED));
    }

    private static <T> ResponseEntity<ApiResponse<T>> privateResponse(T data) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(data));
    }
}

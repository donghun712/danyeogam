package com.danyeogam.backend.title.api;

import com.danyeogam.backend.common.api.ApiResponse;
import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.identity.application.SessionActor;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import com.danyeogam.backend.title.application.TitleQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/titles")
@Profile("!test")
@Tag(name = "칭호", description = "익명 사용자별 칭호 목록과 진행 상태")
public class TitleController {

    private final SessionActorResolver actorResolver;
    private final TitleQueryService queryService;

    public TitleController(
            SessionActorResolver actorResolver,
            TitleQueryService queryService
    ) {
        this.actorResolver = actorResolver;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "내 칭호 목록 조회", security = @SecurityRequirement(name = "anonymousSession"))
    public ResponseEntity<ApiResponse<TitleListResponse>> getTitles(HttpServletRequest request) {
        SessionActor actor = actorResolver.resolve(request)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED));
        TitleListResponse data = queryService.getTitles(actor.actorId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(data, data.titles().size()));
    }
}

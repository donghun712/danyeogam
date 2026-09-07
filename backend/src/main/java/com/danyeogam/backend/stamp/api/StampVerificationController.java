package com.danyeogam.backend.stamp.api;

import com.danyeogam.backend.common.api.ApiResponse;
import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.identity.application.SessionActor;
import com.danyeogam.backend.identity.application.SessionActorResolver;
import com.danyeogam.backend.stamp.application.StampVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stamp-verifications")
@Profile("!test")
@Tag(name = "스탬프", description = "GPS 기반 방문 인증과 스탬프 지급")
public class StampVerificationController {

    private final SessionActorResolver actorResolver;
    private final StampVerificationService verificationService;

    public StampVerificationController(
            SessionActorResolver actorResolver,
            StampVerificationService verificationService
    ) {
        this.actorResolver = actorResolver;
        this.verificationService = verificationService;
    }

    @PostMapping
    @Operation(
            summary = "GPS 방문 인증",
            description = "서버에서 거리·정확도·측정 시각을 검증하고 최초 방문을 원자적으로 저장합니다.",
            security = @SecurityRequirement(name = "anonymousSession")
    )
    public ResponseEntity<ApiResponse<StampVerificationResponse>> verify(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody StampVerificationRequest request,
            HttpServletRequest servletRequest
    ) {
        SessionActor actor = actorResolver.resolve(servletRequest)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED));
        StampVerificationResponse data = verificationService.verify(
                actor.actorId(), idempotencyKey, request
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(data));
    }
}

package com.danyeogam.backend.identity.api;

import com.danyeogam.backend.common.api.ApiResponse;
import com.danyeogam.backend.identity.application.AnonymousSessionService;
import com.danyeogam.backend.identity.application.IssuedAnonymousSession;
import com.danyeogam.backend.identity.web.SessionCookieManager;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@Profile("!test")
@Tag(name = "세션", description = "로그인 전 익명 사용자 세션")
public class AnonymousSessionController {

    private final AnonymousSessionService sessionService;
    private final SessionCookieManager cookieManager;

    public AnonymousSessionController(
            AnonymousSessionService sessionService,
            SessionCookieManager cookieManager
    ) {
        this.sessionService = sessionService;
        this.cookieManager = cookieManager;
    }

    @PostMapping("/anonymous")
    @Operation(summary = "익명 세션 생성 또는 재사용", description = "HttpOnly 세션 쿠키를 발급하거나 유효한 기존 세션을 재사용합니다.")
    public ResponseEntity<ApiResponse<AnonymousSessionResponse>> createOrReuse(
            HttpServletRequest request
    ) {
        IssuedAnonymousSession issued = sessionService.issueOrReuse(cookieManager.read(request));
        AnonymousSessionResponse data = new AnonymousSessionResponse(
                issued.actorType().name(),
                issued.expiresAt()
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookieManager.create(
                        issued.rawToken(), issued.expiresAt()
                ).toString())
                .body(ApiResponse.success(data));
    }
}

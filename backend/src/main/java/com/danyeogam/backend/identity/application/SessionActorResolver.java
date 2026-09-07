package com.danyeogam.backend.identity.application;

import java.util.Optional;

import com.danyeogam.backend.identity.web.SessionCookieManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SessionActorResolver {

    private final SessionCookieManager cookieManager;
    private final AnonymousSessionService sessionService;

    public SessionActorResolver(
            SessionCookieManager cookieManager,
            AnonymousSessionService sessionService
    ) {
        this.cookieManager = cookieManager;
        this.sessionService = sessionService;
    }

    public Optional<SessionActor> resolve(HttpServletRequest request) {
        return sessionService.resolve(cookieManager.read(request));
    }
}

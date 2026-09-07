package com.danyeogam.backend.identity.application;

import java.time.Instant;

import com.danyeogam.backend.identity.domain.ActorType;

public record IssuedAnonymousSession(
        String rawToken,
        ActorType actorType,
        Instant expiresAt
) {
}

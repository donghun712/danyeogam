package com.danyeogam.backend.identity.application;

import java.time.Instant;

import com.danyeogam.backend.identity.domain.ActorType;

public record SessionActor(
        long actorId,
        ActorType actorType,
        Instant expiresAt
) {
}

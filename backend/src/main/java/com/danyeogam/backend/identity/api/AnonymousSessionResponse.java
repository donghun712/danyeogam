package com.danyeogam.backend.identity.api;

import java.time.Instant;

public record AnonymousSessionResponse(
        String actorType,
        Instant expiresAt
) {
}

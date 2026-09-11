package com.danyeogam.backend.title.api;

import java.time.Instant;

public record TitleItemResponse(
        long id,
        String code,
        String name,
        String description,
        boolean earned,
        Instant awardedAt,
        long currentValue,
        long targetValue,
        String progressUnit
) {
}

package com.danyeogam.backend.common.api;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMeta(
        String requestId,
        Instant generatedAt,
        Integer count
) {
    public static ApiMeta now() {
        return new ApiMeta(RequestIdContext.current(), Instant.now(), null);
    }

    public static ApiMeta now(int count) {
        return new ApiMeta(RequestIdContext.current(), Instant.now(), count);
    }
}

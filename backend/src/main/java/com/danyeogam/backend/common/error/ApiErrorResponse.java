package com.danyeogam.backend.common.error;

import com.danyeogam.backend.common.api.ApiMeta;

public record ApiErrorResponse(
        ApiError error,
        ApiMeta meta
) {
}

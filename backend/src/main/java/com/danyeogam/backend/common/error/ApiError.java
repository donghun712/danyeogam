package com.danyeogam.backend.common.error;

import java.util.List;

public record ApiError(
        String code,
        String message,
        boolean retryable,
        List<FieldErrorDetail> fieldErrors
) {
    public ApiError {
        fieldErrors = List.copyOf(fieldErrors);
    }
}

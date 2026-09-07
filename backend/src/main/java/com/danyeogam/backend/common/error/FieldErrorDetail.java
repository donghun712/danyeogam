package com.danyeogam.backend.common.error;

public record FieldErrorDetail(
        String field,
        String message
) {
}

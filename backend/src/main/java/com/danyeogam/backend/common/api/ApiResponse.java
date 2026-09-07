package com.danyeogam.backend.common.api;

public record ApiResponse<T>(
        T data,
        ApiMeta meta
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, ApiMeta.now());
    }

    public static <T> ApiResponse<T> success(T data, int count) {
        return new ApiResponse<>(data, ApiMeta.now(count));
    }
}

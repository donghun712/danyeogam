package com.danyeogam.backend.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.", false),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다.", false),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요.", false),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다.", false),
    CONFLICT(HttpStatus.CONFLICT, "현재 상태에서는 요청을 처리할 수 없습니다.", false),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.", true),
    EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "외부 서비스에 일시적으로 연결할 수 없습니다.", true),
    GEO_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "주소 서비스를 잠시 이용할 수 없습니다.", true),
    INVALID_BOUNDS(HttpStatus.BAD_REQUEST, "지도 좌표 범위를 확인해 주세요.", false),
    BOUNDS_TOO_WIDE(HttpStatus.BAD_REQUEST, "지도를 조금 더 확대해 주세요.", false),
    BOUNDS_TOO_DENSE(HttpStatus.BAD_REQUEST, "표시할 관광지가 너무 많습니다. 지도를 확대해 주세요.", false),
    INVALID_SPOT_TYPE(HttpStatus.BAD_REQUEST, "관광지 유형을 확인해 주세요.", false),
    TOURIST_SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "관광지를 찾을 수 없습니다.", false),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "익명 세션이 필요합니다.", false),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "이미 다른 요청에 사용된 멱등 키입니다.", false),
    ORIGIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "허용되지 않은 요청 출처입니다.", false),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다.", false),
    INVALID_COLLECTION_STATUS(HttpStatus.BAD_REQUEST, "도감 조회 상태를 확인해 주세요.", false),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", true);

    private final HttpStatus status;
    private final String defaultMessage;
    private final boolean retryable;

    ErrorCode(HttpStatus status, String defaultMessage, boolean retryable) {
        this.status = status;
        this.defaultMessage = defaultMessage;
        this.retryable = retryable;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public boolean retryable() {
        return retryable;
    }
}

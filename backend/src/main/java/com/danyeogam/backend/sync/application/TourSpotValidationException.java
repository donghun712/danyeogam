package com.danyeogam.backend.sync.application;

class TourSpotValidationException extends RuntimeException {

    private final String code;
    private final boolean retryable;

    TourSpotValidationException(String code, String message) {
        this(code, message, false);
    }

    TourSpotValidationException(String code, String message, boolean retryable) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    String code() {
        return code;
    }

    boolean retryable() {
        return retryable;
    }
}

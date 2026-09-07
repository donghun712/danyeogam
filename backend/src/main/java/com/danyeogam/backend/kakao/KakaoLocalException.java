package com.danyeogam.backend.kakao;

public class KakaoLocalException extends RuntimeException {

    private final boolean retryable;

    KakaoLocalException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}

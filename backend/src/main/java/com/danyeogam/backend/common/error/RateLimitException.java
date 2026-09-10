package com.danyeogam.backend.common.error;

public final class RateLimitException extends BusinessException {

    private final long retryAfterSeconds;

    public RateLimitException(long retryAfterSeconds) {
        super(ErrorCode.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

package com.danyeogam.backend.geo.application;

public class GeoProviderException extends RuntimeException {

    private final boolean retryable;

    public GeoProviderException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}

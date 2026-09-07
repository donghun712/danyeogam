package com.danyeogam.backend.tourapi;

public class TourApiException extends RuntimeException {

    private final String resultCode;

    TourApiException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    TourApiException(String message) {
        super(message);
        this.resultCode = null;
    }

    public String getResultCode() {
        return resultCode;
    }
}

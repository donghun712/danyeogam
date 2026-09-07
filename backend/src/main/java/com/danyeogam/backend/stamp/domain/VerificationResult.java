package com.danyeogam.backend.stamp.domain;

public enum VerificationResult {
    VERIFIED_NEW,
    VERIFIED_ALREADY_ACQUIRED,
    OUT_OF_RANGE,
    GPS_ACCURACY_INSUFFICIENT,
    LOCATION_STALE,
    STAMP_DISABLED
}

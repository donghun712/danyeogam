package com.danyeogam.backend.sync.application;

record StagingResult(
        long stagingId,
        ValidatedTouristSpot validated,
        boolean accepted
) {
    static StagingResult accepted(long stagingId, ValidatedTouristSpot validated) {
        return new StagingResult(stagingId, validated, true);
    }

    static StagingResult rejected(long stagingId) {
        return new StagingResult(stagingId, null, false);
    }
}

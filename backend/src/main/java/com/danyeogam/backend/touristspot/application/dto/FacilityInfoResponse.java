package com.danyeogam.backend.touristspot.application.dto;

public record FacilityInfoResponse(
        FacilityStatusResponse parking,
        String parkingFeeNote,
        FacilityStatusResponse strollerRental,
        FacilityStatusResponse petAllowed
) {
}

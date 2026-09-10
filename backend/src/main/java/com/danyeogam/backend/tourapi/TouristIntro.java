package com.danyeogam.backend.tourapi;

import org.springframework.util.StringUtils;

public record TouristIntro(
        String contentId,
        String contentTypeId,
        String operatingHours,
        String closedDays,
        String parkingNote,
        String parkingFeeNote,
        String strollerRentalNote,
        String petAllowedNote
) {

    public TouristIntro {
        contentId = blankToNull(contentId);
        contentTypeId = blankToNull(contentTypeId);
        operatingHours = blankToNull(operatingHours);
        closedDays = blankToNull(closedDays);
        parkingNote = blankToNull(parkingNote);
        parkingFeeNote = blankToNull(parkingFeeNote);
        strollerRentalNote = blankToNull(strollerRentalNote);
        petAllowedNote = blankToNull(petAllowedNote);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}

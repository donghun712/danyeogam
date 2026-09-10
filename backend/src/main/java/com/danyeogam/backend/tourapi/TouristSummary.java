package com.danyeogam.backend.tourapi;

import com.fasterxml.jackson.databind.JsonNode;

public record TouristSummary(
        String contentId,
        String contentTypeId,
        String title,
        String address,
        String detailAddress,
        String areaCode,
        String districtCode,
        String longitude,
        String latitude,
        String firstImageUrl,
        String firstThumbnailUrl,
        String modifiedTime,
        String telephone,
        String copyrightType,
        String classificationLevel1,
        String classificationLevel2,
        String classificationLevel3,
        JsonNode rawPayload
) {
}

package com.danyeogam.backend.touristspot.application.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record TouristSpotDetailResponse(
        long id,
        String name,
        String type,
        boolean stampEnabled,
        String visitState,
        TouristSpotAddressResponse address,
        PositionResponse position,
        String overview,
        List<TouristSpotImageResponse> images,
        String telephone,
        String homepageUrl,
        NavigationDestinationResponse navigation,
        String dataSource,
        String dataQuality,
        Instant lastSyncedAt,
        @Schema(description = "PROVINCE 레벨 지역 코드. 도감 요약 regions[].code와 연결한다.",
                example = "TOUR:AREA:52", pattern = "^TOUR:AREA:[0-9]+$",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String regionCode,
        OperatingInfoResponse operatingInfo,
        FacilityInfoResponse facilityInfo
) {
    public TouristSpotDetailResponse {
        images = List.copyOf(images);
    }
}

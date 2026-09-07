package com.danyeogam.backend.tourapi;

public record TouristDetail(
        String contentId,
        String title,
        String address,
        String detailAddress,
        String longitude,
        String latitude,
        String overview,
        String homepage,
        String telephone,
        String firstImageUrl,
        String firstThumbnailUrl,
        String modifiedTime,
        String copyrightType
) {
}

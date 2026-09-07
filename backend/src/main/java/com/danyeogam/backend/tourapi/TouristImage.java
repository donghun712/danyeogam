package com.danyeogam.backend.tourapi;

public record TouristImage(
        String contentId,
        String imageName,
        String originalUrl,
        String thumbnailUrl,
        String serialNumber,
        String copyrightType
) {
}

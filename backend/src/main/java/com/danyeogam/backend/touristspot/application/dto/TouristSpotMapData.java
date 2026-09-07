package com.danyeogam.backend.touristspot.application.dto;

import java.util.List;

public record TouristSpotMapData(List<TouristSpotMapItemResponse> items) {
    public TouristSpotMapData {
        items = List.copyOf(items);
    }
}

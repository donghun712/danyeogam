package com.danyeogam.backend.touristspot.application.dto;

import java.util.List;

public record RegionListData(List<RegionResponse> items) {
    public RegionListData {
        items = List.copyOf(items);
    }
}

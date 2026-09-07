package com.danyeogam.backend.touristspot.application.dto;

import java.util.List;

public record RegionResponse(
        long id,
        String code,
        String name,
        String level,
        List<RegionResponse> children
) {
    public RegionResponse {
        children = List.copyOf(children);
    }
}

package com.danyeogam.backend.title.api;

import java.util.List;

public record TitleListResponse(List<TitleItemResponse> titles) {
    public TitleListResponse {
        titles = List.copyOf(titles);
    }
}

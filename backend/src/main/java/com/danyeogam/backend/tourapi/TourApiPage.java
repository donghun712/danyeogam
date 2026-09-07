package com.danyeogam.backend.tourapi;

import java.util.List;

public record TourApiPage<T>(
        int totalCount,
        int pageNo,
        int numOfRows,
        List<T> items
) {
    public TourApiPage {
        items = List.copyOf(items);
    }
}

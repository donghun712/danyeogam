package com.danyeogam.backend.collection.repository;

public interface CollectionSummaryProjection {
    String getCode();
    String getName();
    Long getVisitedCount();
    Long getTotalCount();
}

package com.danyeogam.backend.favorite.repository;

public interface FavoriteItemProjection {
    Long getTouristSpotId();
    String getName();
    String getThumbnailUrl();
    String getVisitState();
}

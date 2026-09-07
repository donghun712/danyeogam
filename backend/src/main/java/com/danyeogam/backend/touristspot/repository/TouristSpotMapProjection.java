package com.danyeogam.backend.touristspot.repository;

import java.math.BigDecimal;

public interface TouristSpotMapProjection {

    Long getId();
    String getName();
    String getSpotType();
    Boolean getStampEnabled();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    String getThumbnailUrl();
}

package com.danyeogam.backend.touristspot.repository;

import java.util.List;

import com.danyeogam.backend.touristspot.domain.TouristSpotImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristSpotImageRepository extends JpaRepository<TouristSpotImage, Long> {

    List<TouristSpotImage> findAllByTouristSpotIdOrderBySortOrderAscIdAsc(Long touristSpotId);

    void deleteAllByTouristSpotId(Long touristSpotId);
}

package com.danyeogam.backend.touristspot.repository;

import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.touristspot.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByCode(String code);

    Optional<Region> findByCodeAndActiveTrue(String code);

    List<Region> findAllByParentIdAndActiveTrueOrderByNameAsc(Long parentId);

    List<Region> findAllByParentIsNullAndActiveTrueOrderByNameAsc();
}

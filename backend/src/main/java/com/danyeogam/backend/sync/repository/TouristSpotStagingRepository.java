package com.danyeogam.backend.sync.repository;

import java.util.Optional;

import com.danyeogam.backend.sync.domain.TouristSpotStaging;
import com.danyeogam.backend.sync.domain.StagingProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristSpotStagingRepository extends JpaRepository<TouristSpotStaging, Long> {

    Optional<TouristSpotStaging> findBySyncRunIdAndSourceAndSourceContentId(
            Long syncRunId,
            String source,
            String sourceContentId
    );

    long countBySyncRunIdAndProcessingStatus(Long syncRunId, StagingProcessingStatus processingStatus);
}

package com.danyeogam.backend.sync.repository;

import com.danyeogam.backend.sync.domain.SyncErrorRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncErrorRepository extends JpaRepository<SyncErrorRecord, Long> {

    long countBySyncRunId(Long syncRunId);
}

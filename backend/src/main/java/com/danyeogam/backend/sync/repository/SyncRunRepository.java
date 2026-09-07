package com.danyeogam.backend.sync.repository;

import com.danyeogam.backend.sync.domain.SyncRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {
}

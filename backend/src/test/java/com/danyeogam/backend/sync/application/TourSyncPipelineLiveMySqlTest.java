package com.danyeogam.backend.sync.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.danyeogam.backend.sync.domain.StagingProcessingStatus;
import com.danyeogam.backend.sync.domain.SyncRunStatus;
import com.danyeogam.backend.sync.repository.SyncErrorRepository;
import com.danyeogam.backend.sync.repository.SyncRunRepository;
import com.danyeogam.backend.sync.repository.TouristSpotStagingRepository;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotImageRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@SpringBootTest
@ActiveProfiles("mysql-integration")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_TOUR_SYNC_TESTS", matches = "true")
@Sql(
        scripts = "/sql/clean-database.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class TourSyncPipelineLiveMySqlTest {

    @Autowired
    private TourSyncService syncService;

    @Autowired
    private SyncRunRepository syncRunRepository;

    @Autowired
    private TouristSpotStagingRepository stagingRepository;

    @Autowired
    private SyncErrorRepository errorRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TouristSpotRepository spotRepository;

    @Autowired
    private TouristSpotImageRepository imageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void realTourApiDataIsStagedPromotedAndIdempotentOnSecondRun() {
        TourSyncCommand command = new TourSyncCommand("1", 1, 1, true);

        TourSyncResult first = syncService.synchronize(command);

        assertThat(first.status()).isEqualTo(SyncRunStatus.SUCCEEDED);
        assertThat(first.requestedCount()).isPositive();
        assertThat(first.processedCount()).isEqualTo(first.requestedCount());
        assertThat(first.insertedCount()).isEqualTo(first.requestedCount());
        assertThat(first.updatedCount()).isZero();
        assertThat(first.failedCount()).isZero();
        int catalogApiRequestCount = first.apiRequestCount() - first.requestedCount() * 3;
        assertThat(catalogApiRequestCount).isGreaterThanOrEqualTo(9);
        assertThat(stagingRepository.countBySyncRunIdAndProcessingStatus(
                first.syncRunId(), StagingProcessingStatus.PROMOTED
        )).isEqualTo(first.requestedCount());
        assertThat(errorRepository.countBySyncRunId(first.syncRunId())).isZero();
        assertThat(spotRepository.countBySource("TOUR_API")).isEqualTo(first.requestedCount());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tourist_spot WHERE spot_type = 'STAMP_TARGET' AND stamp_enabled = TRUE",
                Integer.class
        )).isEqualTo(first.requestedCount());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tourist_spot WHERE intro_hydrated_at IS NOT NULL",
                Integer.class
        )).isEqualTo(first.requestedCount());
        assertThat(imageRepository.count()).isPositive();
        assertThat(regionRepository.count()).isGreaterThan(3);

        TourSyncResult second = syncService.synchronize(command);

        assertThat(second.status()).isEqualTo(SyncRunStatus.SUCCEEDED);
        assertThat(second.requestedCount()).isEqualTo(first.requestedCount());
        assertThat(second.processedCount()).isEqualTo(first.requestedCount());
        assertThat(second.insertedCount()).isZero();
        assertThat(second.updatedCount()).isZero();
        assertThat(second.failedCount()).isZero();
        assertThat(second.apiRequestCount()).isEqualTo(catalogApiRequestCount);
        assertThat(stagingRepository.countBySyncRunIdAndProcessingStatus(
                second.syncRunId(), StagingProcessingStatus.PROMOTED
        )).isEqualTo(first.requestedCount());
        assertThat(errorRepository.countBySyncRunId(second.syncRunId())).isZero();
        assertThat(spotRepository.countBySource("TOUR_API")).isEqualTo(first.requestedCount());

        Long changedSpotId = spotRepository.findAll().get(0).getId();
        jdbcTemplate.update("UPDATE tourist_spot SET data_hash = ? WHERE id = ?", "0".repeat(64), changedSpotId);

        TourSyncResult third = syncService.synchronize(command);

        assertThat(third.status()).isEqualTo(SyncRunStatus.SUCCEEDED);
        assertThat(third.insertedCount()).isZero();
        assertThat(third.updatedCount()).isEqualTo(1);
        assertThat(third.failedCount()).isZero();
        assertThat(third.apiRequestCount()).isEqualTo(catalogApiRequestCount + 3);
        assertThat(spotRepository.countBySource("TOUR_API")).isEqualTo(first.requestedCount());
        assertThat(syncRunRepository.count()).isEqualTo(3);
    }
}

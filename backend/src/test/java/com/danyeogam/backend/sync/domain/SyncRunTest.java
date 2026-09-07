package com.danyeogam.backend.sync.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SyncRunTest {

    @Test
    void promotedItemWithEnrichmentFailureFinishesPartiallySucceeded() {
        SyncRun run = SyncRun.start(SyncJobType.INITIAL_LOAD);
        run.recordRequested(1);
        run.recordProcessed();
        run.recordInserted();
        run.recordFailed();

        run.finish("부분 보강 실패");

        assertThat(run.getStatus()).isEqualTo(SyncRunStatus.PARTIALLY_SUCCEEDED);
    }

    @Test
    void allRejectedItemsFinishFailed() {
        SyncRun run = SyncRun.start(SyncJobType.INITIAL_LOAD);
        run.recordRequested(1);
        run.recordProcessed();
        run.recordFailed();

        run.finish("전체 실패");

        assertThat(run.getStatus()).isEqualTo(SyncRunStatus.FAILED);
    }
}

package com.danyeogam.backend.sync.application;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.danyeogam.backend.sync.domain.SyncJobType;
import com.danyeogam.backend.sync.domain.SyncRun;
import com.danyeogam.backend.sync.repository.SyncRunRepository;
import com.danyeogam.backend.tourapi.TourApiClient;
import com.danyeogam.backend.tourapi.TourApiException;
import com.danyeogam.backend.tourapi.TourApiPage;
import com.danyeogam.backend.tourapi.TouristDetail;
import com.danyeogam.backend.tourapi.TouristImage;
import com.danyeogam.backend.tourapi.TouristSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

@Service
@Profile("!test")
public class TourSyncService {

    private static final Logger log = LoggerFactory.getLogger(TourSyncService.class);
    private final TourApiClient tourApiClient;
    private final TourSyncPersistenceService persistence;
    private final SyncRunRepository syncRunRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    TourSyncService(
            TourApiClient tourApiClient,
            TourSyncPersistenceService persistence,
            SyncRunRepository syncRunRepository
    ) {
        this.tourApiClient = tourApiClient;
        this.persistence = persistence;
        this.syncRunRepository = syncRunRepository;
    }

    public TourSyncResult synchronize(TourSyncCommand command) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("TourAPI 동기화가 이미 실행 중입니다.");
        }

        SyncRun run = syncRunRepository.save(SyncRun.start(SyncJobType.INITIAL_LOAD));
        try {
            Set<String> preparedDistrictAreas = new HashSet<>();
            prepareProvinces(run);
            boolean completeNationalCatalog = command.areaCode() == null;

            for (TourCategorySelection selection : TouristSpotSelectionPolicy.selections()) {
                int pageNo = 1;
                boolean selectionComplete = false;
                while (pageNo <= command.maxPages()) {
                    run.recordApiRequest();
                    TourApiPage<TouristSummary> page = tourApiClient.getAreaBasedList(
                            pageNo,
                            command.pageSize(),
                            command.areaCode(),
                            selection.contentTypeId(),
                            selection.classificationLevel1(),
                            selection.classificationLevel2(),
                            selection.classificationLevel3()
                    );
                    List<TouristSummary> items = page.items();
                    if (items.isEmpty()) {
                        selectionComplete = true;
                        break;
                    }

                    run.recordRequested(items.size());
                    prepareDistricts(run, items, preparedDistrictAreas);
                    for (TouristSummary summary : items) {
                        importOne(run, command, summary);
                    }

                    if ((long) pageNo * command.pageSize() >= page.totalCount()) {
                        selectionComplete = true;
                        break;
                    }
                    pageNo++;
                }
                if (!selectionComplete) {
                    completeNationalCatalog = false;
                }
            }

            if (completeNationalCatalog) {
                run.recordDeactivated(persistence.deactivateOutsideSelectionPolicy());
            }

            run.finish("TourAPI 역사유적·역사유물·박물관·기념관·미술관 동기화 완료");
            syncRunRepository.save(run);
            log.info(
                    "TourAPI sync completed: syncRunId={}, status={}, requested={}, processed={}, inserted={}, updated={}, deactivated={}, failed={}, apiRequests={}",
                    run.getId(), run.getStatus(), run.getRequestedCount(), run.getProcessedCount(),
                    run.getInsertedCount(), run.getUpdatedCount(), run.getDeactivatedCount(),
                    run.getFailedCount(), run.getRequestQuotaCount()
            );
            return result(run);
        } catch (RuntimeException exception) {
            persistence.recordUnstagedError(
                    run.getId(),
                    "SYNC_ABORTED",
                    "TourAPI 동기화가 완료되기 전에 중단되었습니다."
            );
            run.recordFailed();
            run.fail("TourAPI 동기화 중단");
            syncRunRepository.save(run);
            log.error("TourAPI sync aborted: syncRunId={}, exceptionType={}", run.getId(), exception.getClass().getSimpleName());
            throw exception;
        } finally {
            running.set(false);
        }
    }

    private void prepareProvinces(SyncRun run) {
        run.recordApiRequest();
        persistence.upsertProvinces(tourApiClient.getLegalDongCodes(null, 1, 100).items());
    }

    private void prepareDistricts(
            SyncRun run,
            List<TouristSummary> items,
            Set<String> preparedAreas
    ) {
        items.stream()
                .map(TouristSummary::areaCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .filter(preparedAreas::add)
                .forEach(areaCode -> {
                    run.recordApiRequest();
                    persistence.upsertDistricts(
                            areaCode,
                            tourApiClient.getLegalDongCodes(areaCode, 1, 1000).items()
                    );
                });
    }

    private void importOne(SyncRun run, TourSyncCommand command, TouristSummary summary) {
        // 기존 관광지는 좌표나 상세 검증이 실패하더라도 선정 분류를 먼저 기록한다.
        // 그러면 완전한 전국 동기화 뒤 제외 대상을 정리할 때 선정 장소의 기존 좌표를 보존할 수 있다.
        persistence.updateClassificationIfPresent(summary);

        StagingResult staging;
        try {
            staging = persistence.stage(run.getId(), summary);
        } catch (RuntimeException exception) {
            persistence.recordUnstagedError(
                    run.getId(),
                    "STAGING_FAILED",
                    "관광지 원본 데이터를 staging에 저장하지 못했습니다."
            );
            run.recordProcessed();
            run.recordFailed();
            log.warn("TourAPI item staging failed: syncRunId={}, contentIdPresent={}", run.getId(), hasText(summary.contentId()));
            return;
        }

        if (!staging.accepted()) {
            run.recordProcessed();
            run.recordFailed();
            return;
        }

        if (persistence.isUnchanged(summary, staging.validated().dataHash())) {
            persistence.markPromoted(staging.stagingId());
            run.recordProcessed();
            return;
        }

        TouristDetail detail = null;
        List<TouristImage> images = null;
        boolean partialFailure = false;
        if (command.hydrateDetails()) {
            run.recordApiRequest();
            try {
                Optional<TouristDetail> response = tourApiClient.getCommonDetail(summary.contentId());
                if (response.isPresent()) {
                    detail = response.get();
                } else {
                    partialFailure = true;
                    persistence.recordItemError(
                            run.getId(), staging.stagingId(), summary.contentId(),
                            "DETAIL_NOT_FOUND", "TourAPI 상세정보가 없습니다.", false, false
                    );
                }
            } catch (TourApiException exception) {
                partialFailure = true;
                recordExternalError(run, staging, summary, "DETAIL_FETCH_FAILED", exception);
            }

            run.recordApiRequest();
            try {
                images = tourApiClient.getImages(summary.contentId(), 1, 100).items();
            } catch (TourApiException exception) {
                partialFailure = true;
                recordExternalError(run, staging, summary, "IMAGE_FETCH_FAILED", exception);
            }
        }

        try {
            ImportOutcome outcome = persistence.promote(
                    staging.stagingId(),
                    summary,
                    staging.validated(),
                    detail,
                    images
            );
            run.recordProcessed();
            if (outcome == ImportOutcome.INSERTED) {
                run.recordInserted();
            } else if (outcome == ImportOutcome.UPDATED) {
                run.recordUpdated();
            }
            if (partialFailure) {
                run.recordFailed();
            }
        } catch (RuntimeException exception) {
            run.recordProcessed();
            run.recordFailed();
            persistence.recordItemError(
                    run.getId(), staging.stagingId(), summary.contentId(),
                    "PROMOTION_FAILED", "관광지 데이터를 본 테이블에 저장하지 못했습니다.", true, true
            );
            log.warn(
                    "TourAPI item promotion failed: syncRunId={}, contentId={}, exceptionType={}",
                    run.getId(), summary.contentId(), exception.getClass().getSimpleName()
            );
        }
    }

    private void recordExternalError(
            SyncRun run,
            StagingResult staging,
            TouristSummary summary,
            String fallbackCode,
            TourApiException exception
    ) {
        String resultCode = hasText(exception.getResultCode()) ? exception.getResultCode() : fallbackCode;
        boolean retryable = !("10".equals(resultCode) || "20".equals(resultCode) || "30".equals(resultCode));
        persistence.recordItemError(
                run.getId(),
                staging.stagingId(),
                summary.contentId(),
                resultCode,
                fallbackCode.equals("DETAIL_FETCH_FAILED")
                        ? "TourAPI 상세정보 호출에 실패했습니다."
                        : "TourAPI 이미지 호출에 실패했습니다.",
                retryable,
                false
        );
    }

    private static TourSyncResult result(SyncRun run) {
        return new TourSyncResult(
                run.getId(),
                run.getStatus(),
                run.getRequestedCount(),
                run.getProcessedCount(),
                run.getInsertedCount(),
                run.getUpdatedCount(),
                run.getDeactivatedCount(),
                run.getFailedCount(),
                run.getRequestQuotaCount()
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

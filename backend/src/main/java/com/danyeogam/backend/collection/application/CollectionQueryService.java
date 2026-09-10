package com.danyeogam.backend.collection.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import com.danyeogam.backend.collection.api.CollectionItemResponse;
import com.danyeogam.backend.collection.api.CollectionRegionResponse;
import com.danyeogam.backend.collection.api.CollectionResponse;
import com.danyeogam.backend.collection.api.CollectionSummaryItemResponse;
import com.danyeogam.backend.collection.api.CollectionSummaryResponse;
import com.danyeogam.backend.collection.domain.CollectionStatus;
import com.danyeogam.backend.collection.repository.CollectionQueryRepository;
import com.danyeogam.backend.collection.repository.CollectionSummaryProjection;
import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.RegionLevel;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class CollectionQueryService {

    private final RegionRepository regionRepository;
    private final CollectionQueryRepository collectionRepository;

    public CollectionQueryService(
            RegionRepository regionRepository,
            CollectionQueryRepository collectionRepository
    ) {
        this.regionRepository = regionRepository;
        this.collectionRepository = collectionRepository;
    }

    @Transactional(readOnly = true)
    public CollectionResponse getCollection(long actorId, String regionCode, String rawStatus) {
        if (regionCode == null || regionCode.isBlank() || regionCode.length() > 20) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        CollectionStatus status = parseStatus(rawStatus);
        Region region = regionRepository.findByCodeAndActiveTrue(regionCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
        List<CollectionItemResponse> items = collectionRepository
                .findCollectionItems(actorId, region.getId(), status.name())
                .stream()
                .map(item -> new CollectionItemResponse(
                        item.getTouristSpotId(), item.getName(), item.getVisitState(),
                        item.getVerifiedAt(), item.getThumbnailUrl()
                ))
                .toList();
        return new CollectionResponse(
                new CollectionRegionResponse(region.getCode(), region.getName()),
                items
        );
    }

    @Transactional(readOnly = true)
    public CollectionSummaryResponse getSummary(long actorId) {
        return getSummary(actorId, null);
    }

    @Transactional(readOnly = true)
    public CollectionSummaryResponse getSummary(long actorId, String parentRegionCode) {
        List<CollectionSummaryProjection> summary;
        if (parentRegionCode == null || parentRegionCode.isBlank()) {
            summary = collectionRepository.findCollectionSummary(actorId);
        } else {
            if (parentRegionCode.length() > 20) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            Region parent = regionRepository.findByCodeAndActiveTrue(parentRegionCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
            if (parent.getLevel() != RegionLevel.PROVINCE) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            summary = collectionRepository.findCollectionSummaryByParent(actorId, parent.getId());
        }
        List<CollectionSummaryItemResponse> regions = summary
                .stream()
                .map(this::summaryResponse)
                .toList();
        return new CollectionSummaryResponse(regions);
    }

    private CollectionSummaryItemResponse summaryResponse(CollectionSummaryProjection item) {
        long visited = item.getVisitedCount();
        long total = item.getTotalCount();
        int percent = total == 0 ? 0 : BigDecimal.valueOf(visited)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValueExact();
        return new CollectionSummaryItemResponse(
                item.getCode(), item.getName(), visited, total, percent
        );
    }

    private static CollectionStatus parseStatus(String rawStatus) {
        String value = rawStatus == null || rawStatus.isBlank() ? "ALL" : rawStatus;
        try {
            return CollectionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_COLLECTION_STATUS);
        }
    }
}

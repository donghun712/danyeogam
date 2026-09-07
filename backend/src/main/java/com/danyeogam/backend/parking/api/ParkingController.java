package com.danyeogam.backend.parking.api;

import java.time.Duration;

import com.danyeogam.backend.common.api.ApiResponse;
import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.parking.application.ParkingQueryService;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/tourist-spots")
@Profile("!test")
@Tag(name = "주차장", description = "관광지 주변 카카오 PK6 주차장 후보")
public class ParkingController {

    private final ParkingQueryService service;

    public ParkingController(ParkingQueryService service) {
        this.service = service;
    }

    @GetMapping("/{spotId}/parking")
    @Operation(summary = "관광지 주변 주차장 조회", description = "카카오 장소 검색 결과이며 공영 주차장 여부는 보증하지 않습니다.")
    public ResponseEntity<ApiResponse<ParkingResponse>> getParking(
            @PathVariable long spotId,
            @RequestParam(required = false) @Min(1) Integer radiusMeters,
            @RequestParam(required = false) @Min(1) Integer limit
    ) {
        if ((radiusMeters != null && radiusMeters < 1) || (limit != null && limit < 1)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        ParkingResponse data = service.findNearby(spotId, radiusMeters, limit);
        CacheControl cacheControl = data.temporarilyUnavailable()
                ? CacheControl.noStore()
                : CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();
        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(ApiResponse.success(data, data.items().size()));
    }
}

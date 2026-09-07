package com.danyeogam.backend.geo.api;

import com.danyeogam.backend.common.api.ApiResponse;
import com.danyeogam.backend.geo.application.GeoQueryService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/geo")
@Profile("!test")
@Tag(name = "위치", description = "좌표 기반 주소 조회")
public class GeoController {

    private final GeoQueryService service;

    public GeoController(GeoQueryService service) {
        this.service = service;
    }

    @PostMapping("/reverse")
    @Operation(summary = "현재 좌표의 주소 조회", description = "정확한 GPS 좌표를 URL에 남기지 않도록 POST body로 전달합니다.")
    public ResponseEntity<ApiResponse<ReverseGeoResponse>> reverse(
            @Valid @RequestBody ReverseGeoRequest request
    ) {
        ReverseGeoResponse data = service.reverse(
                request.position().latitude(),
                request.position().longitude()
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(data));
    }
}

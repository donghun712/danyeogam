import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { MapView } from "@/components/map/MapView";
import { MapLegendSheet } from "@/components/map/MapLegendSheet";
import { AttractionBottomSheet } from "@/components/attraction/AttractionBottomSheet";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { AppIcon } from "@/constants/icons";
import { ROUTES } from "@/constants/routes";
import { useKakaoMapsSdk } from "@/hooks/useKakaoMapsSdk";
import { useCurrentLocation } from "@/hooks/useCurrentLocation";
import { useCurrentAddress } from "@/hooks/useCurrentAddress";
import { useAttractionsInBounds } from "@/hooks/useAttractionsInBounds";
import type { MapBounds } from "@/api/attractionApi";
import styles from "./MapPage.module.css";

const BOUNDS_ERROR_MESSAGE: Record<string, string> = {
  BOUNDS_TOO_WIDE: "지도를 조금 더 확대해 주세요.",
  BOUNDS_TOO_DENSE: "관광지가 많은 지역이에요. 지도를 확대한 뒤 다시 시도해 주세요.",
  NETWORK_ERROR: "네트워크 연결을 확인해 주세요.",
};

// 후속 요청서 1단계 — 현재 위치 버튼을 ZoomControl 바로 위에 둘 때 쓰는 크기/간격.
// 버튼 자체 크기는 CSS(.recenterButton width/height)와 반드시 같은 값이어야 한다.
const RECENTER_BUTTON_SIZE = 40;
const RECENTER_GAP = 10;

/**
 * SCREEN-MAP — 메인 지도.
 * 확인 대상: SDK 로드 → 지도 렌더링 → 현재 위치 → bounds 조회(디바운스/취소) →
 * 마커/클러스터링 → 마커 선택 → Bottom Sheet, 그리고 Loading/Empty/Error/Partial Error.
 */
export function MapPage() {
  const navigate = useNavigate();
  const sdkStatus = useKakaoMapsSdk();
  const currentLocation = useCurrentLocation();
  const [bounds, setBounds] = useState<MapBounds | null>(null);
  const { status, spots, errorCode, retry } = useAttractionsInBounds(bounds);
  const [selectedSpotId, setSelectedSpotId] = useState<number | null>(null);
  const [legendOpen, setLegendOpen] = useState(false);
  // 요청서 4단계 — "현재 위치" 버튼을 누를 때마다 1씩 증가시켜 MapView에 신호를 보낸다.
  const [recenterSignal, setRecenterSignal] = useState(0);
  // 후속 요청서 1단계 — 카카오 ZoomControl의 실제 렌더링 위치(런타임 측정값). 측정
  // 실패 시 null로 두고 CSS의 고정 폴백 위치를 그대로 쓴다.
  const [zoomControlRect, setZoomControlRect] = useState<{
    right: number;
    top: number;
  } | null>(null);

  const latitude = currentLocation.position?.coords.latitude ?? null;
  const longitude = currentLocation.position?.coords.longitude ?? null;

  // STEP 9에서 실제 브라우저로 확인된 버그: 이 값을 매 렌더마다 새 객체로 만들면
  // useCurrentAddress의 useEffect([position])가 참조 비교로 인해 렌더마다 다시 실행되어
  // 요청이 끝없이 재시작되는 루프가 생긴다. 실제 좌표 값이 바뀔 때만 새 객체를 만든다.
  const currentPosition = useMemo(
    () =>
      currentLocation.status === "success" && latitude !== null && longitude !== null
        ? { latitude, longitude }
        : null,
    [currentLocation.status, latitude, longitude],
  );

  // MAP-02: 현재 주소 — 실패해도 지도 자체는 그대로 사용 가능해야 하는 Partial Error 영역
  const address = useCurrentAddress(currentPosition);

  return (
    <div className={styles.container}>
      <div className={styles.mapArea}>
        <div className={styles.overlayHeader}>
          <div className={styles.brandRow}>
            <img src="/danyeogam-logo.png" alt="다녀감" className={styles.logo} />
            <button
              type="button"
              className={styles.favoritesEntry}
              onClick={() => navigate(ROUTES.favorites)}
              aria-label="즐겨찾기"
            >
              <AppIcon.favorite size={18} strokeWidth={2} aria-hidden="true" />
            </button>
          </div>

          <div className={styles.topControls}>
            <div className={styles.addressBar}>
              <AppIcon.location size={14} strokeWidth={2} aria-hidden="true" />
              <span className={`text-caption ${styles.addressText}`}>
                {address.status === "success" && address.addressName
                  ? address.addressName
                  : currentLocation.status === "permission_denied"
                    ? "위치 권한이 필요해요"
                    : currentLocation.status === "unavailable"
                      ? "위치를 사용할 수 없어요"
                      : currentLocation.status === "timeout"
                        ? "위치 확인 시간이 초과됐어요"
                        : currentLocation.status === "success" && address.status === "error"
                          ? "주소를 불러오지 못했어요" // MAP-02 Partial Error: 위치는 확보했지만 역지오코딩만 실패한 경우
                          : "현재 위치 확인 중..."}
              </span>
            </div>

            {sdkStatus === "ready" && (
              <button
                type="button"
                className={styles.legendButton}
                onClick={() => setLegendOpen(true)}
                aria-label="지도상태 — 마커 안내"
              >
                <AppIcon.mapLegend size={18} strokeWidth={2} aria-hidden="true" />
              </button>
            )}
          </div>
        </div>

        {sdkStatus === "error" && (
          <ErrorState message="카카오맵을 불러오지 못했습니다. VITE_KAKAO_JS_KEY 설정을 확인해 주세요." />
        )}

        {sdkStatus === "loading" && (
          <div className={styles.sdkLoading}>
            <span className="text-body">지도를 불러오는 중...</span>
          </div>
        )}

        {sdkStatus === "ready" && (
          <MapView
            currentPosition={currentPosition}
            spots={spots}
            onBoundsChange={setBounds}
            onSelectSpot={setSelectedSpotId}
            selectedSpotId={selectedSpotId}
            recenterSignal={recenterSignal}
            onZoomControlRect={setZoomControlRect}
          />
        )}

        {/* 요청서 4단계 — 지도 탐색 위치를 유지하게 바꾸면서, 사용자가 원할 때 GPS
            현재 위치로 돌아갈 수 있는 명확한 수단이 필요해져 추가한 버튼.
            후속 요청서 1단계 — 실제 렌더링된 카카오 ZoomControl 바로 위, 같은 우측
            축으로 정렬한다(측정값 있을 때). 측정 실패 시 CSS 기본 위치로 폴백. */}
        {sdkStatus === "ready" && (
          <button
            type="button"
            className={styles.recenterButton}
            style={
              zoomControlRect
                ? {
                    right: `${zoomControlRect.right}px`,
                    top: `${zoomControlRect.top - RECENTER_BUTTON_SIZE - RECENTER_GAP}px`,
                    bottom: "auto",
                  }
                : undefined
            }
            onClick={() => setRecenterSignal((prev) => prev + 1)}
            aria-label="현재 위치로 이동"
            disabled={!currentPosition}
          >
            <AppIcon.currentLocation size={20} strokeWidth={2} aria-hidden="true" />
          </button>
        )}

        {sdkStatus === "ready" && status === "loading" && spots.length === 0 && (
          <div className={styles.overlayBottom}>
            <div className={styles.initialLoading}>
              <span className="text-body">관광지를 불러오는 중...</span>
            </div>
          </div>
        )}

        {sdkStatus === "ready" && status === "empty" && (
          <div className={styles.overlayBottom}>
            <EmptyState
              icon={AppIcon.map}
              message={"주변에 등록된 관광지가 없습니다.\n지도를 이동해서 다른 지역을 탐색해보세요."}
            />
          </div>
        )}

        {sdkStatus === "ready" && status === "error" && (
          <div className={styles.overlayBottom}>
            <ErrorState
              message={
                (errorCode && BOUNDS_ERROR_MESSAGE[errorCode]) ??
                "관광지 정보를 불러오지 못했습니다."
              }
              onRetry={retry}
              compact
            />
          </div>
        )}
      </div>

      <AttractionBottomSheet
        spotId={selectedSpotId}
        onClose={() => setSelectedSpotId(null)}
      />

      <MapLegendSheet open={legendOpen} onClose={() => setLegendOpen(false)} />
    </div>
  );
}

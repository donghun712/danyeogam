import { useCallback, useEffect, useRef, useState } from "react";
import { fetchAttractionsInBounds } from "@/api/attractionApi";
import { ApiError, NetworkError } from "@/api/client";
import { subscribeAttractionVisited } from "@/utils/attractionEvents";
import type { MapBounds } from "@/api/attractionApi";
import type { MapSpot } from "@/types/api";

type FetchStatus = "idle" | "loading" | "success" | "empty" | "error";

interface AttractionsInBoundsState {
  status: FetchStatus;
  spots: MapSpot[];
  errorCode: string | null;
}

/**
 * bounds가 바뀔 때마다 MAP-03(GET /tourist-spots)을 호출한다.
 * bounds 자체의 디바운스(300ms)는 지도 idle 이벤트 쪽(MapView)에서 처리하고,
 * 이 훅은 "새 bounds가 왔을 때 이전 요청을 취소하고 최신 요청만 반영"하는 책임만 진다.
 */
export function useAttractionsInBounds(
  bounds: MapBounds | null,
): AttractionsInBoundsState & { retry: () => void } {
  const [state, setState] = useState<AttractionsInBoundsState>({
    status: "idle",
    spots: [],
    errorCode: null,
  });
  const abortControllerRef = useRef<AbortController | null>(null);
  const [retryToken, setRetryToken] = useState(0);

  const retry = useCallback(() => setRetryToken((token) => token + 1), []);

  useEffect(() => {
    if (!bounds) return;

    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    // 요청 시작을 즉시 반영하는 의도적인 로딩 상태 setState (취소 가능한 데이터 페칭 패턴)
    setState((prev) => ({ ...prev, status: "loading", errorCode: null }));

    fetchAttractionsInBounds(bounds, undefined, controller.signal)
      .then(({ items }) => {
        if (controller.signal.aborted) return;
        setState({
          status: items.length === 0 ? "empty" : "success",
          spots: items,
          errorCode: null,
        });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        // AbortError는 다음 요청으로 대체된 것이므로 오류로 취급하지 않는다.
        if (error instanceof DOMException && error.name === "AbortError") return;

        const code =
          error instanceof ApiError
            ? error.code
            : error instanceof NetworkError
              ? "NETWORK_ERROR"
              : "UNKNOWN_ERROR";
        // 상태명세서 9장: "재시도 시 기존 화면 상태를 최대한 유지한다" — 이전에 성공적으로
        // 불러온 마커를 오류 하나 때문에 지우지 않는다.
        setState((prev) => ({ status: "error", spots: prev.spots, errorCode: code }));
      });

    return () => {
      controller.abort();
    };
  }, [bounds, retryToken]);

  // GPS 인증 성공 후 재조회 없이 해당 마커의 visitState만 즉시 반영한다(STEP 6).
  useEffect(() => {
    return subscribeAttractionVisited((visitedSpotId) => {
      setState((prev) => {
        if (!prev.spots.some((spot) => spot.id === visitedSpotId)) return prev;
        return {
          ...prev,
          spots: prev.spots.map((spot) =>
            spot.id === visitedSpotId
              ? { ...spot, visitState: "VISITED" }
              : spot,
          ),
        };
      });
    });
  }, []);

  return { ...state, retry };
}

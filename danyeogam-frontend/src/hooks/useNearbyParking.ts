import { useCallback, useEffect, useRef, useState } from "react";
import { fetchNearbyParking } from "@/api/parkingApi";
import { ApiError, NetworkError } from "@/api/client";
import type { ParkingItem } from "@/types/api";

type ParkingStatus = "idle" | "loading" | "success" | "empty" | "unavailable" | "error";

interface NearbyParkingState {
  status: ParkingStatus;
  items: ParkingItem[];
  errorCode: string | null;
}

/**
 * PARK-01: 관광지 상세와 독립적으로 실패할 수 있는 영역이라 자체 Loading/Empty/Error를 갖는다.
 * temporarilyUnavailable=true는 HTTP 200으로 오는 "공급자 장애" 상태라 error와 구분해서 다룬다
 * (백엔드 문서 8.5절, 10.2절).
 * limit을 지정하면 바텀시트의 "가까운 주차장" 미리보기처럼 일부만 가져올 수 있다.
 */
export function useNearbyParking(
  spotId: number | null,
  limit?: number,
): NearbyParkingState & { retry: () => void } {
  const [state, setState] = useState<NearbyParkingState>({
    status: "idle",
    items: [],
    errorCode: null,
  });
  const abortControllerRef = useRef<AbortController | null>(null);
  const [retryToken, setRetryToken] = useState(0);

  const retry = useCallback(() => setRetryToken((token) => token + 1), []);

  useEffect(() => {
    if (spotId === null) {
      // spotId가 없을 때 즉시 idle로 되돌리는 의도적인 setState.
      setState({ status: "idle", items: [], errorCode: null });
      return;
    }

    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    // 요청 시작을 즉시 반영하는 의도적인 로딩 상태 setState (취소 가능한 데이터 페칭 패턴)
    setState((prev) => ({ ...prev, status: "loading", errorCode: null }));

    fetchNearbyParking(spotId, { limit }, controller.signal)
      .then(({ items, temporarilyUnavailable }) => {
        if (controller.signal.aborted) return;
        if (temporarilyUnavailable) {
          setState({ status: "unavailable", items: [], errorCode: null });
        } else if (items.length === 0) {
          setState({ status: "empty", items: [], errorCode: null });
        } else {
          setState({ status: "success", items, errorCode: null });
        }
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        if (error instanceof DOMException && error.name === "AbortError") return;

        const code =
          error instanceof ApiError
            ? error.code
            : error instanceof NetworkError
              ? "NETWORK_ERROR"
              : "UNKNOWN_ERROR";
        // 상태명세서 9장: 재시도 시 기존 화면 상태를 최대한 유지한다.
        setState((prev) => ({ status: "error", items: prev.items, errorCode: code }));
      });

    return () => {
      controller.abort();
    };
  }, [spotId, limit, retryToken]);

  return { ...state, retry };
}

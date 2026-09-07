import { useCallback, useEffect, useRef, useState } from "react";
import { fetchAttractionDetail } from "@/api/attractionApi";
import { ApiError, NetworkError } from "@/api/client";
import { subscribeAttractionVisited } from "@/utils/attractionEvents";
import type { TouristSpotDetail } from "@/types/api";

type DetailStatus = "idle" | "loading" | "success" | "error";

interface AttractionDetailState {
  status: DetailStatus;
  detail: TouristSpotDetail | null;
  errorCode: string | null;
}

/** TOUR-02: 지도 목록과 분리된 상세 데이터를 마커/카드 선택 시점에만 조회한다(Lazy Loading). */
export function useAttractionDetail(
  spotId: number | null,
): AttractionDetailState & { retry: () => void } {
  const [state, setState] = useState<AttractionDetailState>({
    status: "idle",
    detail: null,
    errorCode: null,
  });
  const abortControllerRef = useRef<AbortController | null>(null);
  const [retryToken, setRetryToken] = useState(0);
  // 현재 state.detail이 어느 spotId의 데이터인지 기록한다. 다른 관광지로 이동한 경우(마커를
  // 바꿔 클릭하는 등) 이전 관광지의 상세를 잘못 보여주지 않기 위해 필요하다.
  const loadedSpotIdRef = useRef<number | null>(null);

  const retry = useCallback(() => setRetryToken((token) => token + 1), []);

  useEffect(() => {
    if (spotId === null) {
      loadedSpotIdRef.current = null;
      // spotId가 없을 때 즉시 idle로 되돌리는 의도적인 setState.
      setState({ status: "idle", detail: null, errorCode: null });
      return;
    }

    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    // 같은 관광지를 다시 불러오는 경우(백그라운드 재조회/재시도)에만 기존 데이터를 유지하고,
    // 다른 관광지로 이동한 경우에는 이전 관광지 정보를 보여주지 않는다.
    const isRefreshOfSameSpot = loadedSpotIdRef.current === spotId;

    // 요청 시작을 즉시 반영하는 의도적인 로딩 상태 setState (취소 가능한 데이터 페칭 패턴).
    // 상태명세서 9장 "재시도 시 기존 화면 상태를 최대한 유지" — 이전에 성공적으로 불러온
    // detail이 있다면(백그라운드 재조회) 지우지 않고 계속 보여준다.
    setState((prev) => ({
      status: "loading",
      detail: isRefreshOfSameSpot ? prev.detail : null,
      errorCode: null,
    }));

    fetchAttractionDetail(spotId, controller.signal)
      .then((detail) => {
        if (controller.signal.aborted) return;
        loadedSpotIdRef.current = spotId;
        setState({ status: "success", detail, errorCode: null });
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
        // 같은 관광지를 다시 불러오다 실패한 경우에만 이전에 성공한 detail을 유지한다.
        setState((prev) => ({
          status: "error",
          detail: isRefreshOfSameSpot ? prev.detail : null,
          errorCode: code,
        }));
      });

    return () => {
      controller.abort();
    };
  }, [spotId, retryToken]);

  // GPS 인증 성공 후 이 상세가 보여주는 visitState를 최신화한다(STEP 6).
  useEffect(() => {
    if (spotId === null) return;
    return subscribeAttractionVisited((visitedSpotId) => {
      if (visitedSpotId === spotId) retry();
    });
  }, [spotId, retry]);

  return { ...state, retry };
}

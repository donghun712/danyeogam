import { useCallback, useEffect, useRef, useState } from "react";
import { fetchCollectionSummary } from "@/api/collectionApi";
import { ApiError, NetworkError } from "@/api/client";
import { subscribeAttractionVisited } from "@/utils/attractionEvents";
import type { CollectionRegionSummary } from "@/types/api";

type SummaryStatus = "idle" | "loading" | "success" | "empty" | "error";

interface CollectionSummaryState {
  status: SummaryStatus;
  regions: CollectionRegionSummary[];
  errorCode: string | null;
}

/**
 * BOOK-02: GET /me/collection/summary — 16개 광역 지역 전체의 진행률을 한 번에 반환하므로
 * 이 응답을 지역 선택 목록의 출처로도 재사용한다(별도로 GET /regions을 다시 호출하지 않음).
 * parentRegionCode를 주면 그 광역 아래 시군구 단위 진행률을 대신 가져온다(같은 API의 하위
 * 호환 확장). null/undefined면 광역 16개를 그대로 가져온다.
 * STEP 6의 attraction-visited 이벤트(collectionChanged를 유발하는 두 상태에서만 발행됨)를
 * 구독해서 재조회한다 — 새 전역 상태 관리 구조를 추가하지 않고 기존 이벤트를 재사용.
 */
export function useCollectionSummary(
  parentRegionCode?: string | null,
): CollectionSummaryState & {
  retry: () => void;
} {
  const [state, setState] = useState<CollectionSummaryState>({
    status: parentRegionCode === null ? "idle" : "loading",
    regions: [],
    errorCode: null,
  });
  const abortControllerRef = useRef<AbortController | null>(null);
  const [retryToken, setRetryToken] = useState(0);

  const retry = useCallback(() => setRetryToken((token) => token + 1), []);

  useEffect(() => {
    // parentRegionCode를 명시적으로 null로 넘긴 경우(아직 상위 지역이 안 정해짐) — 요청하지
    // 않는다. undefined(인자 생략)는 기존처럼 광역 16개를 그대로 가져온다.
    if (parentRegionCode === null) {
      setState({ status: "idle", regions: [], errorCode: null });
      return;
    }

    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    // 요청 시작을 즉시 반영하는 의도적인 로딩 상태 setState (취소 가능한 데이터 페칭 패턴)
    setState((prev) => ({ ...prev, status: "loading", errorCode: null }));

    fetchCollectionSummary(parentRegionCode)
      .then(({ regions }) => {
        if (controller.signal.aborted) return;
        setState({
          status: regions.length === 0 ? "empty" : "success",
          regions,
          errorCode: null,
        });
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
        // 상태명세서 9장: 재시도 시 기존 화면 상태를 최대한 유지한다. 인증 성공 이벤트로
        // 백그라운드 재조회가 실패해도 이미 표시 중이던 지역 목록/진행률은 지우지 않는다.
        setState((prev) => ({ status: "error", regions: prev.regions, errorCode: code }));
      });

    return () => {
      controller.abort();
    };
  }, [parentRegionCode, retryToken]);

  useEffect(() => subscribeAttractionVisited(() => retry()), [retry]);

  return { ...state, retry };
}

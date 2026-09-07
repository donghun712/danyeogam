import { useCallback, useEffect, useRef, useState } from "react";
import { fetchCollection } from "@/api/collectionApi";
import { ApiError, NetworkError } from "@/api/client";
import { subscribeAttractionVisited } from "@/utils/attractionEvents";
import type { CollectionItem } from "@/types/api";

type ListStatus = "idle" | "loading" | "success" | "empty" | "error";

interface CollectionListState {
  status: ListStatus;
  items: CollectionItem[];
  regionName: string | null;
  errorCode: string | null;
}

/**
 * BOOK-01: GET /me/collection?regionCode=...&status=ALL.
 * regionCode는 GET /me/collection/summary(useCollectionSummary)가 내려준 code를 그대로
 * 전달받아 사용하고, 프론트에서 형식을 바꾸지 않는다("TOUR:AREA:45" 그대로).
 */
export function useCollectionList(
  regionCode: string | null,
): CollectionListState & { retry: () => void } {
  const [state, setState] = useState<CollectionListState>({
    status: "idle",
    items: [],
    regionName: null,
    errorCode: null,
  });
  const abortControllerRef = useRef<AbortController | null>(null);
  const [retryToken, setRetryToken] = useState(0);
  // 현재 state가 어느 regionCode의 데이터인지 기록한다. 지역 선택 드롭다운으로 다른
  // 지역으로 바꾼 경우에는 이전 지역의 목록을 잠깐이라도 보여주면 안 되기 때문에 필요하다.
  const loadedRegionCodeRef = useRef<string | null>(null);

  const retry = useCallback(() => setRetryToken((token) => token + 1), []);

  useEffect(() => {
    if (regionCode === null) {
      loadedRegionCodeRef.current = null;
      // 선택된 지역이 없을 때 즉시 idle로 되돌리는 의도적인 setState.
      setState({ status: "idle", items: [], regionName: null, errorCode: null });
      return;
    }

    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    // 같은 지역을 다시 불러오는 경우(인증 성공 후 백그라운드 재조회, 재시도)에만 기존
    // 목록을 유지하고, 사용자가 드롭다운으로 다른 지역을 선택한 경우에는 그 지역의 이전
    // 목록을 보여주지 않는다.
    const isRefreshOfSameRegion = loadedRegionCodeRef.current === regionCode;

    // 요청 시작을 즉시 반영하는 의도적인 로딩 상태 setState (취소 가능한 데이터 페칭 패턴)
    setState((prev) => ({
      status: "loading",
      items: isRefreshOfSameRegion ? prev.items : [],
      regionName: isRefreshOfSameRegion ? prev.regionName : null,
      errorCode: null,
    }));

    fetchCollection(regionCode, "ALL")
      .then(({ region, items }) => {
        if (controller.signal.aborted) return;
        loadedRegionCodeRef.current = regionCode;
        setState({
          status: items.length === 0 ? "empty" : "success",
          items,
          regionName: region.name,
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
        setState((prev) => ({
          status: "error",
          items: isRefreshOfSameRegion ? prev.items : [],
          regionName: isRefreshOfSameRegion ? prev.regionName : null,
          errorCode: code,
        }));
      });

    return () => {
      controller.abort();
    };
  }, [regionCode, retryToken]);

  useEffect(() => subscribeAttractionVisited(() => retry()), [retry]);

  return { ...state, retry };
}

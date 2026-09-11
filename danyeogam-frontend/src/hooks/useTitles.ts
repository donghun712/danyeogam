import { useCallback, useEffect, useRef, useState } from "react";
import { fetchTitles } from "@/api/titleApi";
import { ApiError, NetworkError } from "@/api/client";
import { subscribeAttractionVisited } from "@/utils/attractionEvents";
import type { Title } from "@/types/api";

type TitlesStatus = "loading" | "success" | "empty" | "error";

interface TitlesState {
  status: TitlesStatus;
  titles: Title[];
  errorCode: string | null;
}

/**
 * GET /me/titles — 활성 칭호 전체(획득 여부 포함)를 한 번에 반환한다.
 * GPS 인증 성공(VERIFIED_NEW/VERIFIED_ALREADY_ACQUIRED) 후 attraction-visited 이벤트를
 * 구독해서 재조회한다 — useCollectionSummary와 동일하게 기존 이벤트를 재사용한다.
 */
export function useTitles(): TitlesState & { retry: () => void } {
  const [state, setState] = useState<TitlesState>({
    status: "loading",
    titles: [],
    errorCode: null,
  });
  const abortControllerRef = useRef<AbortController | null>(null);
  const [retryToken, setRetryToken] = useState(0);

  const retry = useCallback(() => setRetryToken((token) => token + 1), []);

  useEffect(() => {
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    // 취소 가능한 데이터 페칭 시작 시점의 의도적인 loading setState(기존 컨벤션과 동일).
    setState((prev) => ({ ...prev, status: "loading", errorCode: null }));

    fetchTitles()
      .then(({ titles }) => {
        if (controller.signal.aborted) return;
        setState({
          status: titles.length === 0 ? "empty" : "success",
          titles,
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
        // 재시도 시 기존 화면 상태를 최대한 유지(9장 상태 처리 원칙).
        setState((prev) => ({ status: "error", titles: prev.titles, errorCode: code }));
      });

    return () => {
      controller.abort();
    };
  }, [retryToken]);

  useEffect(() => subscribeAttractionVisited(() => retry()), [retry]);

  return { ...state, retry };
}

import { useCallback, useEffect, useState } from "react";
import { fetchFavorites } from "@/api/favoriteApi";
import { ApiError, NetworkError } from "@/api/client";
import type { FavoriteItem } from "@/types/api";

type FavoritesStatus = "idle" | "loading" | "success" | "empty" | "error";

interface FavoritesState {
  status: FavoritesStatus;
  items: FavoriteItem[];
  errorCode: string | null;
}

/**
 * 요청서 2단계 — "즐겨찾기한 곳을 다시 찾아볼 최소한의 재탐색 경로".
 * 백엔드 계약(docs/frontend-favorites-api.md)에 이미 GET /me/favorites가 있고
 * favoriteApi.ts에 fetchFavorites()도 이미 있었는데, 이걸 보여주는 화면이 없었다 —
 * 새 API나 가짜 데이터를 만들지 않고 기존 계약 그대로 쓴다.
 */
export function useFavorites(): FavoritesState & { retry: () => void } {
  const [state, setState] = useState<FavoritesState>({
    status: "loading",
    items: [],
    errorCode: null,
  });
  const [retryToken, setRetryToken] = useState(0);
  const retry = useCallback(() => setRetryToken((token) => token + 1), []);

  useEffect(() => {
    let cancelled = false;
    setState((prev) => ({ ...prev, status: "loading", errorCode: null }));

    fetchFavorites()
      .then(({ items }) => {
        if (cancelled) return;
        setState({
          status: items.length === 0 ? "empty" : "success",
          items,
          errorCode: null,
        });
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        const code =
          error instanceof ApiError
            ? error.code
            : error instanceof NetworkError
              ? "NETWORK_ERROR"
              : "UNKNOWN_ERROR";
        setState((prev) => ({ status: "error", items: prev.items, errorCode: code }));
      });

    return () => {
      cancelled = true;
    };
  }, [retryToken]);

  return { ...state, retry };
}

import { useCallback, useState } from "react";
import { addFavorite, removeFavorite } from "@/api/favoriteApi";

/**
 * 즐겨찾기 토글. detail.favorited를 초기값으로 받아 낙관적으로 바로 반영하고,
 * 실패하면 되돌린다. 다른 관광지로 이동(spotId 변경)하면 그 관광지의 초기값으로 다시 맞춘다
 * (React 공식 패턴: effect 없이 렌더링 중 상태를 보정).
 */
export function useFavorite(spotId: number, initialFavorited: boolean) {
  const [syncedSpotId, setSyncedSpotId] = useState(spotId);
  const [favorited, setFavorited] = useState(initialFavorited);
  const [isPending, setIsPending] = useState(false);

  if (spotId !== syncedSpotId) {
    setSyncedSpotId(spotId);
    setFavorited(initialFavorited);
  }

  const toggle = useCallback(async () => {
    if (isPending) return;
    const next = !favorited;
    setFavorited(next);
    setIsPending(true);
    try {
      await (next ? addFavorite(spotId) : removeFavorite(spotId));
    } catch {
      // 상태명세서 9장 원칙과 동일하게, 실패하면 조용히 이전 상태로 되돌린다.
      setFavorited(!next);
    } finally {
      setIsPending(false);
    }
  }, [favorited, isPending, spotId]);

  return { favorited, isPending, toggle };
}

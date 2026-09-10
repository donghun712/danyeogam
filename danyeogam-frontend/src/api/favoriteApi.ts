import { apiRequest } from "@/api/client";
import type { FavoriteItem, FavoriteState } from "@/types/api";

/** 좋아요/즐겨찾기를 하나로 합친 개인 저장 기능. POST는 멱등 — 이미 추가된 상태면 그대로 favorited:true. */
export function addFavorite(spotId: number): Promise<FavoriteState> {
  return apiRequest<FavoriteState>(`/tourist-spots/${spotId}/favorite`, {
    method: "POST",
  });
}

/** DELETE도 멱등 — 이미 해제된 상태면 그대로 favorited:false. */
export function removeFavorite(spotId: number): Promise<FavoriteState> {
  return apiRequest<FavoriteState>(`/tourist-spots/${spotId}/favorite`, {
    method: "DELETE",
  });
}

export function fetchFavorites(): Promise<{ items: FavoriteItem[] }> {
  return apiRequest<{ items: FavoriteItem[] }>("/me/favorites");
}

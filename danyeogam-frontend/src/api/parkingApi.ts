import { apiRequest } from "@/api/client";
import type { ParkingList } from "@/types/api";

/**
 * 관광지 주변 주차장 조회. 실패해도 관광지 상세 화면 전체를 실패시키지 않고
 * 주차장 영역만 별도로 Loading/Empty/Error 처리한다 (Partial Error).
 */
export function fetchNearbyParking(
  spotId: number,
  options?: { radiusMeters?: number; limit?: number },
  signal?: AbortSignal,
): Promise<ParkingList> {
  return apiRequest<ParkingList>(`/tourist-spots/${spotId}/parking`, {
    query: {
      radiusMeters: options?.radiusMeters,
      limit: options?.limit,
    },
    signal,
  });
}

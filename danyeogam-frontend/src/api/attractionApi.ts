import { apiRequest } from "@/api/client";
import type { MapSpot, SpotType, TouristSpotDetail } from "@/types/api";

export interface MapBounds {
  northEastLatitude: number;
  northEastLongitude: number;
  southWestLatitude: number;
  southWestLongitude: number;
}

/**
 * 현재 지도 영역(Bounding Box) 안의 관광지 경량 목록을 조회한다.
 * 상세 설명/전체 이미지는 포함하지 않는다 — 필요하면 fetchAttractionDetail을 별도 호출한다(Lazy Loading).
 */
export function fetchAttractionsInBounds(
  bounds: MapBounds,
  types?: SpotType[],
  signal?: AbortSignal,
): Promise<{ items: MapSpot[] }> {
  return apiRequest<{ items: MapSpot[] }>("/tourist-spots", {
    query: {
      northEastLatitude: bounds.northEastLatitude,
      northEastLongitude: bounds.northEastLongitude,
      southWestLatitude: bounds.southWestLatitude,
      southWestLongitude: bounds.southWestLongitude,
      types: types?.join(","),
    },
    signal,
  });
}

/** 관광지 상세 조회 (마커/카드 선택 시점에 Lazy Loading). */
export function fetchAttractionDetail(
  spotId: number,
  signal?: AbortSignal,
): Promise<TouristSpotDetail> {
  return apiRequest<TouristSpotDetail>(`/tourist-spots/${spotId}`, { signal });
}

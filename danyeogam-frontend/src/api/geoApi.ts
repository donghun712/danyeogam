import { apiRequest } from "@/api/client";
import type { Position, ReverseGeocodeResult } from "@/types/api";

/**
 * 현재 좌표를 주소로 변환한다. 좌표가 URL(쿼리스트링)에 남지 않도록 POST body로 전송한다.
 * 실패(503 GEO_PROVIDER_UNAVAILABLE)해도 지도/마커 자체는 계속 표시해야 한다 — 호출부에서
 * 이 요청의 실패를 지도 전체 오류로 취급하지 않는다.
 */
export function reverseGeocode(
  position: Position,
): Promise<ReverseGeocodeResult> {
  return apiRequest<ReverseGeocodeResult>("/geo/reverse", {
    method: "POST",
    body: { position },
  });
}

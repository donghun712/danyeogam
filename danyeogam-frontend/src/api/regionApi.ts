import { apiRequest } from "@/api/client";
import type { Region } from "@/types/api";

/** 지역 계층 목록. 도감 화면의 regionCode 선택지로 사용한다 (code를 그대로 사용, 임의 가공 금지). */
export function fetchRegions(): Promise<{ items: Region[] }> {
  return apiRequest<{ items: Region[] }>("/regions");
}

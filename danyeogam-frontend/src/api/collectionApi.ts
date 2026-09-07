import { apiRequest } from "@/api/client";
import type {
  CollectionList,
  CollectionStatusFilter,
  CollectionSummary,
} from "@/types/api";

/** 지역별 방문/미방문 목록. 세션 필요(401 시 로그인 유도 대신 익명 세션 재발급을 먼저 시도). */
export function fetchCollection(
  regionCode: string,
  status: CollectionStatusFilter = "ALL",
): Promise<CollectionList> {
  return apiRequest<CollectionList>("/me/collection", {
    query: { regionCode, status },
  });
}

/** 지역별 진행률 요약. GPS 인증 응답의 collectionChanged=true일 때 다시 호출한다. */
export function fetchCollectionSummary(): Promise<CollectionSummary> {
  return apiRequest<CollectionSummary>("/me/collection/summary");
}

import type { VisitState } from "@/types/api";

/**
 * 요청서 5.1 — "방문 완료"와 "스탬프 가능"이 동시에 뜨면 재인증해야 하는지 혼동된다는
 * 실제 버그 신고에 대한 공통 판정 로직. 배지·CTA를 각 화면(AttractionSummaryBody,
 * AttractionDetailBody, AttractionBottomSheet, AttractionDetailPage)에서 따로
 * `stampEnabled`만 보고 판단하던 걸 여기 하나로 모아서, 네 곳이 항상 같은 규칙을 쓰게
 * 한다. visitState는 백엔드가 이미 계산해서 내려주는 실제 값을 그대로 쓴다.
 */
export type VisitBadgeKind = "visited" | "stampAvailable" | null;

export function getVisitBadgeKind(params: {
  stampEnabled: boolean;
  visitState: VisitState;
}): VisitBadgeKind {
  if (params.visitState === "VISITED") return "visited";
  if (params.stampEnabled) return "stampAvailable";
  return null;
}

/** VISITED가 확인된 경우에만 인증 CTA를 숨긴다 — UNKNOWN(조회 불확실)은 기존처럼 stampEnabled를 따른다. */
export function shouldShowStampCta(params: {
  stampEnabled: boolean;
  visitState: VisitState;
}): boolean {
  return params.stampEnabled && params.visitState !== "VISITED";
}

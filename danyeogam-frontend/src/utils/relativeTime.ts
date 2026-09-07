/**
 * 실제 measuredAt(ISO 문자열) 기준으로 사람이 읽기 쉬운 상대 시각을 만든다.
 * 임의의 값이 아니라 항상 실제로 전달된 시각만 변환한다.
 */
export function formatRelativeTimeKo(isoTimestamp: string): string {
  const measuredMs = new Date(isoTimestamp).getTime();
  if (Number.isNaN(measuredMs)) return "방금 전";

  const elapsedSeconds = Math.max(0, Math.floor((Date.now() - measuredMs) / 1000));

  if (elapsedSeconds < 10) return "방금 전";
  if (elapsedSeconds < 60) return `${elapsedSeconds}초 전`;

  const elapsedMinutes = Math.floor(elapsedSeconds / 60);
  if (elapsedMinutes < 60) return `${elapsedMinutes}분 전`;

  const elapsedHours = Math.floor(elapsedMinutes / 60);
  return `${elapsedHours}시간 전`;
}

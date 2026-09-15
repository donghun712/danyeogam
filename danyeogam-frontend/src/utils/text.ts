/**
 * 요청서 2단계 — Bottom Sheet 소개 미리보기는 line-clamp로 중간에 끊지 않고
 * "첫 번째 완전한 문장만" 보여준다. 원문을 재작성하거나 요약하지 않고, 첫 종결부호
 * (. ! ?) 위치까지 그대로 잘라낸다. 종결부호가 없으면 원문 전체를 그대로 반환하고
 * (fake text를 만들지 않음), 호출부의 CSS line-clamp가 fallback으로 넘친 부분을 가린다.
 */
export function firstSentence(text: string): string {
  const match = /^[^.!?]*[.!?]+/.exec(text);
  return match ? match[0].trim() : text;
}

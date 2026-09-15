/**
 * 요청서 — Bottom Sheet 소개 미리보기는 "첫 번째 완전한 문장만" 보여준다.
 * 이전 버전(단순 첫 .!? 검색)은 "충민사는 마래산(385.2m)에..." 같은 소수점을
 * 문장 종결로 오인해서 "충민사는 마래산(385." 에서 잘리는 버그가 있었다.
 *
 * 1순위: Intl.Segmenter('ko', {granularity:'sentence'}) — 브라우저 표준 API로
 *        언어별 문장 경계 규칙(약어, 소수점 등)을 실제로 고려해서 분리한다.
 * 2순위: Segmenter 미지원/실패 시에만 fallback — 단순 첫 점 검색이 아니라,
 *        마침표 앞뒤가 숫자면(소수점·"3.1운동" 같은 숫자+점) 문장 종결로 보지 않는다.
 * 3순위: 그래도 종결부호를 못 찾으면 원문을 그대로 반환한다(재작성/fake text 금지).
 *        호출부의 CSS line-clamp가 이 경우의 fallback을 담당한다.
 */
export function firstSentence(text: string): string {
  const trimmed = text.trim();
  if (!trimmed) return trimmed;

  if (typeof Intl !== "undefined" && "Segmenter" in Intl) {
    try {
      const segmenter = new Intl.Segmenter("ko", { granularity: "sentence" });
      for (const { segment } of segmenter.segment(trimmed)) {
        const s = segment.trim();
        if (s) return s;
      }
    } catch {
      // 아래 fallback으로
    }
  }

  return fallbackFirstSentence(trimmed);
}

function fallbackFirstSentence(text: string): string {
  for (let i = 0; i < text.length; i += 1) {
    const ch = text[i];
    if (ch === "!" || ch === "?") {
      return text.slice(0, i + 1).trim();
    }
    if (ch === ".") {
      const prev = text[i - 1];
      const next = text[i + 1];
      // 숫자 사이의 점(소수점, "3.1운동" 같은 표기)은 문장 종결이 아니다.
      const touchesDigit = (prev != null && /\d/.test(prev)) || (next != null && /\d/.test(next));
      if (touchesDigit) continue;
      // 진짜 문장 끝은 보통 공백/문자열 끝/닫는 괄호·따옴표 뒤에 온다.
      if (next === undefined || /[\s)"'”’]/.test(next)) {
        return text.slice(0, i + 1).trim();
      }
    }
  }
  // 종결부호를 찾지 못함 — 원문 그대로, 호출부 line-clamp가 넘친 부분을 가린다.
  return text;
}

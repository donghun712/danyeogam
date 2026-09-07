/**
 * 카카오맵 마커/클러스터 스타일은 CSS가 아니라 JS 객체(MarkerImage, Clusterer styles)로
 * 넘겨야 해서 style.css를 직접 참조할 수 없다. 대신 :root에 이미 선언된 디자인 토큰 값을
 * 런타임에 읽어와서 색상 값을 이중 관리하지 않는다 (src/styles/tokens.css가 유일한 출처).
 */
export function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

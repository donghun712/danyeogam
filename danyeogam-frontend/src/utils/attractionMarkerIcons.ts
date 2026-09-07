import { getCssVar } from "./cssVar";

/**
 * 디자인 시스템 11장 Map Marker System 기준:
 * - 스탬프 대상: Heritage Brown 원형 + 아이콘
 * - 일반 관광지: 중립적인 형태(작은 원)
 * - 방문 완료: 기존 마커에 완료 상태 표시 추가 — 단, "방문 완료 마커의 최종 UI는 TBD"라고
 *   문서에 명시되어 있어(11장, 개발 전 5가지 결정 항목 중 하나) 최종 디자인이 아니다.
 *   여기서는 우선 색상을 Success(Sage)로 바꿔 구분하는 임시안으로 구현하고,
 *   실제 완료 마커 디자인이 정해지면 이 파일만 고치면 된다.
 *
 * kakao.maps.MarkerClusterer는 kakao.maps.Marker(+ MarkerImage) 인스턴스를 요구하므로
 * CustomOverlay 대신 SVG data URI 기반 MarkerImage를 사용한다.
 */

const MARKER_SIZE = 30;

function buildCircleMarkerSvg(fillColor: string, strokeColor: string): string {
  const r = MARKER_SIZE / 2 - 2;
  const c = MARKER_SIZE / 2;
  return `
    <svg xmlns="http://www.w3.org/2000/svg" width="${MARKER_SIZE}" height="${MARKER_SIZE}" viewBox="0 0 ${MARKER_SIZE} ${MARKER_SIZE}">
      <circle cx="${c}" cy="${c}" r="${r}" fill="${fillColor}" stroke="${strokeColor}" stroke-width="2" />
    </svg>
  `.trim();
}

function toDataUri(svg: string): string {
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

export type AttractionMarkerVariant = "stamp" | "stamp-visited" | "general";

/**
 * kakao.maps.MarkerImage를 생성한다. 카카오 SDK가 로드된 뒤(useKakaoMapsSdk "ready")에만
 * 호출해야 한다 — kakao.maps 네임스페이스가 그 전에는 존재하지 않는다.
 */
export function createAttractionMarkerImage(
  variant: AttractionMarkerVariant,
): kakao.maps.MarkerImage {
  const heritageBrown = getCssVar("--color-heritage-brown") || "#8A4F2A";
  const success = getCssVar("--color-success") || "#5F7D57";
  const ivory = getCssVar("--color-hanji-ivory") || "#F7F3EA";
  const disabled = getCssVar("--color-disabled") || "#B9B4AA";

  let svg: string;
  switch (variant) {
    case "stamp":
      svg = buildCircleMarkerSvg(heritageBrown, ivory);
      break;
    case "stamp-visited":
      svg = buildCircleMarkerSvg(success, ivory);
      break;
    case "general":
    default:
      svg = buildCircleMarkerSvg(disabled, ivory);
      break;
  }

  return new kakao.maps.MarkerImage(
    toDataUri(svg),
    new kakao.maps.Size(MARKER_SIZE, MARKER_SIZE),
    { offset: new kakao.maps.Point(MARKER_SIZE / 2, MARKER_SIZE / 2) },
  );
}

export function createCurrentLocationMarkerImage(): kakao.maps.MarkerImage {
  const size = 22;
  const info = getCssVar("--color-info") || "#55758A";
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
      <circle cx="${size / 2}" cy="${size / 2}" r="${size / 2 - 2}" fill="${info}" fill-opacity="0.25" />
      <circle cx="${size / 2}" cy="${size / 2}" r="5" fill="${info}" stroke="#fff" stroke-width="2" />
    </svg>
  `.trim();

  return new kakao.maps.MarkerImage(
    toDataUri(svg),
    new kakao.maps.Size(size, size),
    { offset: new kakao.maps.Point(size / 2, size / 2) },
  );
}

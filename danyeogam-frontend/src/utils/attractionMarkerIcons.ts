import { getCssVar } from "./cssVar";

/**
 * 디자인 시스템 11장 Map Marker System 기준, 화면시안 범례("스탬프 대상 역사유적"=진한 핀,
 * "일반 관광지"=옅은 원형 테두리)에 맞춰 두 형태를 구분한다:
 * - 스탬프 대상: 지도 핀(pin) 모양 + Heritage Brown 채움 + 중앙 아이보리 점
 * - 일반 관광지: 작은 테두리 원(채움 없음) — 상대적으로 덜 강조되는 형태
 * - 방문 완료: 기존 마커에 완료 상태 표시 추가 — 단, "방문 완료 마커의 최종 UI는 TBD"라고
 *   문서에 명시되어 있어(11장, 개발 전 5가지 결정 항목 중 하나) 최종 디자인이 아니다.
 *   여기서는 우선 색상을 Success(Sage)로 바꿔 구분하는 임시안으로 구현하고,
 *   실제 완료 마커 디자인이 정해지면 이 파일만 고치면 된다.
 *
 * kakao.maps.MarkerClusterer는 kakao.maps.Marker(+ MarkerImage) 인스턴스를 요구하므로
 * CustomOverlay 대신 SVG data URI 기반 MarkerImage를 사용한다.
 */

const PIN_WIDTH = 28;
const PIN_HEIGHT = 36;
const DOT_SIZE = 18;

/** 지도 핀(teardrop) 모양 — 스탬프 대상 관광지용. 끝부분(하단 뾰족한 점)이 실제 좌표를 가리킨다. */
function buildPinMarkerSvg(fillColor: string, accentColor: string): string {
  return `
    <svg xmlns="http://www.w3.org/2000/svg" width="${PIN_WIDTH}" height="${PIN_HEIGHT}" viewBox="0 0 ${PIN_WIDTH} ${PIN_HEIGHT}">
      <path
        d="M14 0C6.268 0 0 6.268 0 14c0 10.5 14 22 14 22s14-11.5 14-22C28 6.268 21.732 0 14 0z"
        fill="${fillColor}"
      />
      <circle cx="14" cy="14" r="5.5" fill="${accentColor}" />
    </svg>
  `.trim();
}

/** 테두리만 있는 작은 원 — 일반(스탬프 대상 아닌) 관광지용, 화면시안 범례의 옅은 원형과 맞춘다. */
function buildOutlineDotSvg(strokeColor: string, fillColor: string): string {
  const r = DOT_SIZE / 2 - 2;
  const c = DOT_SIZE / 2;
  return `
    <svg xmlns="http://www.w3.org/2000/svg" width="${DOT_SIZE}" height="${DOT_SIZE}" viewBox="0 0 ${DOT_SIZE} ${DOT_SIZE}">
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

  if (variant === "general") {
    return new kakao.maps.MarkerImage(
      toDataUri(buildOutlineDotSvg(disabled, ivory)),
      new kakao.maps.Size(DOT_SIZE, DOT_SIZE),
      { offset: new kakao.maps.Point(DOT_SIZE / 2, DOT_SIZE / 2) },
    );
  }

  const fillColor = variant === "stamp-visited" ? success : heritageBrown;
  return new kakao.maps.MarkerImage(
    toDataUri(buildPinMarkerSvg(fillColor, ivory)),
    new kakao.maps.Size(PIN_WIDTH, PIN_HEIGHT),
    { offset: new kakao.maps.Point(PIN_WIDTH / 2, PIN_HEIGHT) }, // 핀 끝(뾰족한 부분)이 실제 좌표
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

/**
 * kakao.maps.MarkerClusterer의 기본 스타일은 카카오 기본 파란색 말풍선이라 앱의 한지/브랜드
 * 톤과 충돌한다. 디자인 토큰으로 브랜드에 맞는 원형 배지 스타일을 만든다.
 */
export function createClustererStyle(): Record<string, string> {
  const heritageBrown = getCssVar("--color-heritage-brown") || "#8A4F2A";
  const ivory = getCssVar("--color-hanji-ivory") || "#F7F3EA";

  return {
    width: "40px",
    height: "40px",
    lineHeight: "40px",
    borderRadius: "50%",
    background: heritageBrown,
    border: `2px solid ${ivory}`,
    color: ivory,
    textAlign: "center",
    fontFamily: "var(--font-family-base)",
    fontWeight: "700",
    fontSize: "14px",
  };
}

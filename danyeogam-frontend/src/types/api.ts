/**
 * 백엔드 API 타입 정의
 *
 * 기준: danyeogam 저장소 backend/docs/frontend-*.md, backend/docs/openapi.yaml
 * (다녀감_백엔드_설계서.md 초안이 아니라 실제 구현/Swagger를 기준으로 함)
 *
 * TODO: 프로젝트가 커지면 openapi-typescript 등으로 openapi.yaml에서 타입을 직접
 * 생성하는 방식으로 전환하고, 이 수기 타입과 이중 관리하지 않는다.
 */

// ── 공통 ──────────────────────────────────────────────

export interface Position {
  latitude: number;
  longitude: number;
}

export interface ApiMeta {
  requestId: string;
  generatedAt: string;
  count?: number;
}

export interface ApiSuccess<T> {
  data: T;
  meta: ApiMeta;
}

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  retryable: boolean;
  fieldErrors: ApiFieldError[];
}

export interface ApiErrorResponse {
  error: ApiErrorBody;
  meta: ApiMeta;
}

// ── 지역 (GET /api/v1/regions) ──────────────────────────

export type RegionLevel = "PROVINCE" | "CITY_COUNTY";

export interface Region {
  id: number;
  code: string; // 예: "TOUR:AREA:45" — 프론트에서 임의 가공하지 않고 그대로 사용
  name: string;
  level: RegionLevel;
  children: Region[];
}

// ── 관광지 지도 목록 (GET /api/v1/tourist-spots) ─────────

export type SpotType = "GENERAL" | "STAMP_TARGET";
export type VisitState = "UNKNOWN" | "NOT_VISITED" | "VISITED";

export interface MapSpot {
  id: number;
  name: string;
  position: Position;
  type: SpotType;
  stampEnabled: boolean;
  visitState: VisitState;
  thumbnailUrl: string | null;
}

// ── 관광지 상세 (GET /api/v1/tourist-spots/{id}) ─────────

export type DataQuality = "COMPLETE" | "PARTIAL";

export interface SpotAddress {
  road: string | null;
  lot: string | null;
}

export interface SpotImage {
  url: string;
  alt: string;
  copyrightType: string | null;
}

export interface NavigationTarget {
  destinationName: string;
  latitude: number;
  longitude: number;
  coordinateType: "wgs84";
}

/** TourAPI detailIntro2 원문 그대로 — 파싱하지 않고 그대로 표시한다("상시 개방" 같은 값도 있음). */
export interface OperatingInfo {
  hours: string | null;
  closedDays: string | null;
}

export type FacilityStatusValue = "AVAILABLE" | "UNAVAILABLE" | "UNKNOWN";

/**
 * status만으로 판단하지 않는다 — UNKNOWN이어도 note(원문)가 있으면 그 텍스트를 보여줘야 한다.
 * note가 null인 UNKNOWN만 "정보 없음"에 해당한다.
 */
export interface FacilityStatus {
  status: FacilityStatusValue;
  note: string | null;
}

export interface FacilityInfo {
  parking: FacilityStatus;
  parkingFeeNote: string | null;
  strollerRental: FacilityStatus;
  petAllowed: FacilityStatus;
  // 휠체어(무장애) 정보는 TourAPI detailIntro2에 없어 백엔드 응답에도 없다 — 항목 자체를 만들지 않는다.
}

export interface TouristSpotDetail {
  id: number;
  name: string;
  type: SpotType;
  stampEnabled: boolean;
  visitState: VisitState;
  address: SpotAddress;
  position: Position;
  overview: string | null;
  images: SpotImage[];
  telephone: string | null;
  homepageUrl: string | null;
  navigation: NavigationTarget;
  dataSource: string;
  dataQuality: DataQuality;
  lastSyncedAt: string;
  /**
   * PROVINCE 레벨 지역 코드(예: "TOUR:AREA:52"). GET /me/collection/summary의 regions[].code와
   * 정확히 매칭된다 — 시/군/구 소속 관광지도 백엔드가 상위 광역 코드로 정규화해서 내려준다.
   */
  regionCode: string;
  /** detailIntro2로 보강되지 않은 관광지는 null — 아직 전체 3,885건 중 일부만 보강된 상태다. */
  operatingInfo: OperatingInfo | null;
  facilityInfo: FacilityInfo | null;
  /** 세션이 없으면 항상 false — 상세 조회 자체는 세션 없이도 가능하다. */
  favorited: boolean;
}

export interface FavoriteState {
  touristSpotId: number;
  favorited: boolean;
}

export interface FavoriteItem {
  touristSpotId: number;
  name: string;
  thumbnailUrl: string | null;
  visitState: VisitState;
}

// ── 현재 좌표 주소 (POST /api/v1/geo/reverse) ────────────

export interface ReverseGeocodeRegion {
  depth1: string | null;
  depth2: string | null;
  depth3: string | null;
}

export interface ReverseGeocodeResult {
  addressName: string | null;
  roadAddressName: string | null;
  region: ReverseGeocodeRegion | null;
  source: "KAKAO_LOCAL";
}

// ── 주변 주차장 (GET /api/v1/tourist-spots/{id}/parking) ─

export interface ParkingItem {
  id: string;
  name: string;
  address: string;
  position: Position;
  distanceMeters: number;
  /**
   * 현재 데이터 소스(Kakao PK6)는 공영 여부를 보증하지 않아 항상 false다.
   * true가 내려올 가능성을 대비해 분기는 유지하되,
   * 지금 데이터로 "공영주차장" 문구를 표시하지 않는다.
   */
  publicVerified: boolean;
  source: "KAKAO_LOCAL";
  navigation: NavigationTarget;
  fetchedAt: string;
}

export interface ParkingList {
  items: ParkingItem[];
  temporarilyUnavailable: boolean;
}

// ── GPS 스탬프 인증 (POST /api/v1/stamp-verifications) ───

export type StampVerificationStatus =
  | "VERIFIED_NEW"
  | "VERIFIED_ALREADY_ACQUIRED"
  | "OUT_OF_RANGE"
  | "GPS_ACCURACY_INSUFFICIENT"
  | "LOCATION_STALE"
  | "STAMP_DISABLED";

export interface StampVerificationRequestPosition {
  latitude: number;
  longitude: number;
  accuracyMeters: number;
  measuredAt: string;
}

export interface StampVerificationRequest {
  touristSpotId: number;
  position: StampVerificationRequestPosition;
}

export interface StampVerificationResult {
  status: StampVerificationStatus;
  touristSpotId: number;
  distanceMeters: number | null;
  verifiedAt: string | null;
  visitState: VisitState;
  collectionChanged: boolean;
  /** 이번 인증으로 새로 부여된 title_definition.id 목록. GET /me/titles 재조회 후 매칭한다. */
  newTitleIds: number[];
}

// ── 도감 (P1) ─────────────────────────────────────────

export type CollectionStatusFilter = "ALL" | "VISITED" | "NOT_VISITED";

export interface CollectionItem {
  touristSpotId: number;
  name: string;
  visitState: VisitState;
  verifiedAt: string | null;
  thumbnailUrl: string | null;
}

export interface CollectionList {
  region: { code: string; name: string };
  items: CollectionItem[];
}

export interface CollectionRegionSummary {
  code: string;
  name: string;
  visitedCount: number;
  totalCount: number;
  progressPercent: number;
}

export interface CollectionSummary {
  regions: CollectionRegionSummary[];
}

// ── 칭호 (GET /api/v1/me/titles) ─────────────────────────

/**
 * VISITS: 서로 다른 관광지 방문 수, REGIONS: 서로 다른 광역/시군구 방문 수,
 * PERCENT: 해당 광역지역의 현재 도감 진행률. currentValue/targetValue의 단위를 결정한다.
 */
export type TitleProgressUnit = "VISITS" | "REGIONS" | "PERCENT";

export interface Title {
  id: number;
  code: string;
  name: string;
  description: string;
  earned: boolean;
  awardedAt: string | null;
  currentValue: number;
  targetValue: number;
  progressUnit: TitleProgressUnit;
}

export interface TitleList {
  titles: Title[];
}

// ── 익명 세션 (POST /api/v1/sessions/anonymous) ──────────

export interface AnonymousSession {
  actorType: "ANONYMOUS";
  expiresAt: string;
}

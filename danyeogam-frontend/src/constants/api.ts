/** API 기준 경로. 기본값은 동일 출처 /api/v1이며, 분리 배포 시 전체 API prefix만 허용한다. */
export const API_BASE_PATH = resolveApiBasePath(import.meta.env.VITE_API_BASE_URL);

function resolveApiBasePath(configuredValue: string | undefined): string {
  const value = configuredValue?.trim();
  if (!value) return "/api/v1";

  if (value.startsWith("/")) {
    if (value.startsWith("//") || value.includes("?") || value.includes("#")) {
      throw new Error("VITE_API_BASE_URL은 안전한 API 경로여야 합니다.");
    }
    const normalized = value.replace(/\/+$/, "");
    if (!normalized) {
      throw new Error("VITE_API_BASE_URL은 비어 있지 않은 API 경로여야 합니다.");
    }
    return normalized;
  }

  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new Error("VITE_API_BASE_URL 형식이 올바르지 않습니다.");
  }

  const localHttp =
    !import.meta.env.PROD &&
    url.protocol === "http:" &&
    (url.hostname === "localhost" || url.hostname === "127.0.0.1");
  if (
    (url.protocol !== "https:" && !localHttp) ||
    url.username ||
    url.password ||
    url.search ||
    url.hash
  ) {
    throw new Error("운영 API 주소는 인증정보 없는 HTTPS URL이어야 합니다.");
  }
  return value.replace(/\/+$/, "");
}

/** 지도 idle 이벤트 이후 bounds 재조회까지의 디바운스 시간(ms). 백엔드/프론트 명세서 공통 값. */
export const MAP_BOUNDS_DEBOUNCE_MS = 300;

/**
 * 백엔드가 실제로 반환하는 error.code 값들.
 * 프론트는 이 코드로만 분기하고 error.message 문자열은 비교하지 않는다.
 */
export const API_ERROR_CODE = {
  INVALID_REQUEST: "INVALID_REQUEST",
  INVALID_BOUNDS: "INVALID_BOUNDS",
  BOUNDS_TOO_WIDE: "BOUNDS_TOO_WIDE",
  BOUNDS_TOO_DENSE: "BOUNDS_TOO_DENSE",
  INVALID_SPOT_TYPE: "INVALID_SPOT_TYPE",
  VALIDATION_FAILED: "VALIDATION_FAILED",
  TOURIST_SPOT_NOT_FOUND: "TOURIST_SPOT_NOT_FOUND",
  REGION_NOT_FOUND: "REGION_NOT_FOUND",
  INVALID_COLLECTION_STATUS: "INVALID_COLLECTION_STATUS",
  AUTHENTICATION_REQUIRED: "AUTHENTICATION_REQUIRED",
  ORIGIN_NOT_ALLOWED: "ORIGIN_NOT_ALLOWED",
  IDEMPOTENCY_KEY_CONFLICT: "IDEMPOTENCY_KEY_CONFLICT",
  UNSUPPORTED_MEDIA_TYPE: "UNSUPPORTED_MEDIA_TYPE",
  TOO_MANY_REQUESTS: "TOO_MANY_REQUESTS",
  GEO_PROVIDER_UNAVAILABLE: "GEO_PROVIDER_UNAVAILABLE",
  INTERNAL_SERVER_ERROR: "INTERNAL_SERVER_ERROR",
} as const;

export type ApiErrorCode =
  (typeof API_ERROR_CODE)[keyof typeof API_ERROR_CODE];

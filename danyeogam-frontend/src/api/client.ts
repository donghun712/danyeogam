import { API_BASE_PATH } from "@/constants/api";
import type {
  ApiErrorBody,
  ApiErrorResponse,
  ApiSuccess,
} from "@/types/api";

/**
 * 백엔드 오류 응답(error.code 등)을 그대로 담아 던지는 예외.
 * 화면은 error.message 문자열이 아니라 code로 분기해야 한다.
 */
export class ApiError extends Error {
  readonly code: string;
  readonly retryable: boolean;
  readonly fieldErrors: ApiErrorBody["fieldErrors"];
  readonly status: number;
  readonly requestId?: string;
  readonly retryAfterSeconds: number | null;

  constructor(
    status: number,
    body: ApiErrorResponse,
    retryAfterSeconds: number | null = null,
  ) {
    super(body.error.message);
    this.name = "ApiError";
    this.code = body.error.code;
    this.retryable = body.error.retryable;
    this.fieldErrors = body.error.fieldErrors;
    this.status = status;
    this.requestId = body.meta?.requestId;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

function parseRetryAfterSeconds(value: string | null): number | null {
  if (!value) return null;

  const seconds = Number(value);
  if (Number.isFinite(seconds) && seconds >= 0) {
    return Math.ceil(seconds);
  }

  const retryAt = Date.parse(value);
  if (Number.isNaN(retryAt)) return null;
  return Math.max(0, Math.ceil((retryAt - Date.now()) / 1000));
}

/** 네트워크 자체가 끊긴 경우(요청이 서버까지 도달하지 못한 경우)를 구분하기 위한 예외. */
export class NetworkError extends Error {
  constructor(cause: unknown) {
    super("네트워크 연결을 확인해 주세요.");
    this.name = "NetworkError";
    this.cause = cause;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  query?: Record<string, string | number | boolean | undefined>;
  body?: unknown;
  /** POST /stamp-verifications 등 변경 요청에 필요한 재시도 키 */
  idempotencyKey?: string;
  signal?: AbortSignal;
}

function buildQueryString(
  query: RequestOptions["query"],
): string {
  if (!query) return "";
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined) continue;
    params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

/**
 * 인증이 필요한 API를 세션 쿠키 없이 호출했을 때(401)를 구분해서 처리할 수 있도록,
 * 호출부에서 error.code === "AUTHENTICATION_REQUIRED"를 확인하는 걸 권장한다.
 */
export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const url = `${API_BASE_PATH}${path}${buildQueryString(options.query)}`;

  const headers: Record<string, string> = {};
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (options.idempotencyKey) {
    headers["Idempotency-Key"] = options.idempotencyKey;
  }

  let response: Response;
  try {
    response = await fetch(url, {
      method: options.method ?? "GET",
      credentials: "include", // dg_session 쿠키 전달에 필수
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      signal: options.signal,
    });
  } catch (cause) {
    // AbortError는 호출부(useAttractions 등)에서 별도로 다루므로 그대로 전파한다.
    if (cause instanceof DOMException && cause.name === "AbortError") {
      throw cause;
    }
    throw new NetworkError(cause);
  }

  const json = await response.json().catch(() => null);

  if (!response.ok) {
    const retryAfterSeconds = parseRetryAfterSeconds(
      response.headers.get("Retry-After"),
    );
    if (json && typeof json === "object" && "error" in json) {
      throw new ApiError(
        response.status,
        json as ApiErrorResponse,
        retryAfterSeconds,
      );
    }
    throw new ApiError(response.status, {
      error: {
        code: "INTERNAL_SERVER_ERROR",
        message: "알 수 없는 오류가 발생했습니다.",
        retryable: true,
        fieldErrors: [],
      },
      meta: { requestId: "unknown", generatedAt: new Date().toISOString() },
    }, retryAfterSeconds);
  }

  return (json as ApiSuccess<T>).data;
}

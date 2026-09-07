import { apiRequest } from "@/api/client";
import type { AnonymousSession } from "@/types/api";

/**
 * 익명 세션을 발급하거나 재사용한다. 앱 시작 시 한 번 호출한다.
 * 실패해도 지도 조회 자체는 계속할 수 있어야 하므로, 호출부에서
 * 이 함수의 실패를 전체 앱 오류로 취급하지 않는다.
 */
export function createOrReuseAnonymousSession(): Promise<AnonymousSession> {
  return apiRequest<AnonymousSession>("/sessions/anonymous", {
    method: "POST",
  });
}

import { isMobileDevice } from "./device";
import type { NavigationTarget } from "@/types/api";

export type KakaoNaviFailureReason = "not_mobile" | "sdk_not_ready" | "unknown_error";

export type KakaoNaviLaunchResult =
  | { ok: true }
  | { ok: false; reason: KakaoNaviFailureReason };

/**
 * 프론트엔드 구현 명세서 7장: 카카오내비 버튼 선택 시 관광지 또는 주차장을 목적지로 전달한다.
 * 실행에 실패하면 오류 메시지를 보여주되 전체 서비스 오류로 이어지지 않게 한다.
 *
 * 카카오내비 JS SDK는 공식 문서 기준 모바일 기기에서만 동작하고(데스크톱에서는 아무 동작도
 * 하지 않음), 1.41.0 버전부터 앱 미설치 시 웹 대체 없이 설치 페이지로만 이동한다.
 */
export function launchKakaoNavi(
  navigation: NavigationTarget,
): KakaoNaviLaunchResult {
  if (!isMobileDevice()) {
    return { ok: false, reason: "not_mobile" };
  }
  if (!window.Kakao?.isInitialized?.()) {
    return { ok: false, reason: "sdk_not_ready" };
  }

  try {
    window.Kakao.Navi.start({
      name: navigation.destinationName,
      x: navigation.longitude,
      y: navigation.latitude,
      coordType: navigation.coordinateType,
    });
    return { ok: true };
  } catch {
    return { ok: false, reason: "unknown_error" };
  }
}

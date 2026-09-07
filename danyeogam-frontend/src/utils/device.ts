/**
 * 카카오내비 JavaScript SDK는 모바일 기기에서만 동작한다(공식 문서: Kakao.Navi.start/share
 * "모바일 기기에서만 동작"). 또한 SDK 1.41.0부터 앱 미설치 시 웹 길안내로 대체하는 기능은
 * 제공하지 않고 설치 페이지로만 이동한다.
 * 데스크톱에서는 실행을 시도하는 대신 안내 문구를 보여주기 위해 이 함수로 미리 구분한다.
 */
export function isMobileDevice(): boolean {
  if (typeof navigator === "undefined") return false;
  return /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
}

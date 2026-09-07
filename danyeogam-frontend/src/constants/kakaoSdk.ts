/**
 * "Kakao SDK for JavaScript"(카카오내비 등에 쓰이는 범용 SDK, 카카오맵 SDK와 별개) 로드 설정.
 *
 * 기능 호환성을 확인한 2.7.1을 고정하고, 공식 CDN 파일에서 계산한 SHA-384 SRI로
 * 변조를 탐지한다. 버전을 변경할 때는 공식 다운로드 문서의 integrity도 함께 갱신한다.
 */
export const KAKAO_JS_SDK_VERSION = "2.7.1";
export const KAKAO_JS_SDK_INTEGRITY =
  "sha384-kDljxUXHaJ9xAb2AzRd59KxjrFjzHa5TAoFQ6GbYTCAG0bjM55XohjjDT7tDDC01";

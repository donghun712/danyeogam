/**
 * "Kakao SDK for JavaScript"(카카오내비 등에 쓰이는 범용 SDK, 카카오맵 SDK와 별개) 로드 설정.
 *
 * 버전은 공식 다운로드 페이지(https://developers.kakao.com/docs/latest/ko/javascript/download)에서
 * 2026-09-08 기준 최신 버전(2.8.3, 2026.9.3 배포)으로 확인·갱신했다.
 *
 * 공식 CDN의 2.8.3 minified 파일을 내려받아 SHA-384로 계산한 SRI 값을 고정한다.
 * SDK 버전을 바꾸면 파일과 integrity 값을 반드시 함께 갱신해야 한다.
 */
export const KAKAO_JS_SDK_VERSION = "2.8.3";
export const KAKAO_JS_SDK_INTEGRITY =
  "sha384-oroumrnFVE0xtgqyDZJARgERibXg2C28380uaUZz2kHDS5CR7tu20eGiOU6GkTpy";

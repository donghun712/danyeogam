/**
 * "Kakao SDK for JavaScript"(카카오내비 등에 쓰이는 범용 SDK, 카카오맵 SDK와 별개) 로드 설정.
 *
 * 버전은 공식 다운로드 페이지(https://developers.kakao.com/docs/latest/ko/javascript/download)에서
 * 2026-09-08 기준 최신 버전(2.8.3, 2026.9.3 배포)으로 확인·갱신했다.
 *
 * TODO(팀 확인 필요): integrity(SRI) 값은 그 페이지의 "복사" 버튼을 눌러야 정확한 값을 얻을 수
 * 있고, 여기서는 바이트 단위로 100% 신뢰할 수 있는 방법으로 확인하지 못해 비워뒀다. 잘못된
 * integrity 값은 비어있는 것보다 더 나쁘다 — SRI 검증이 실패하면 스크립트 로드 자체가 막힌다.
 * 배포 전 팀이 직접 다운로드 페이지에서 이 버전의 integrity 값을 복사해서 채워야 한다.
 * KAKAO_JS_SDK_INTEGRITY가 비어 있는 동안은 SRI 검증 없이 로드된다(기능은 정상 동작).
 */
export const KAKAO_JS_SDK_VERSION = "2.8.3";
export const KAKAO_JS_SDK_INTEGRITY = ""; // TODO: 다운로드 페이지에서 이 버전의 값을 복사해서 채울 것

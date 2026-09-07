/**
 * 카카오맵 SDK(window.kakao.maps)와는 별개의 "Kakao SDK for JavaScript"(window.Kakao) 타입.
 * 이 프로젝트가 실제로 쓰는 범위(init/isInitialized/Navi.start)만 최소한으로 선언한다.
 * 출처: https://developers.kakao.com/docs/latest/ko/kakaonavi/js
 */
export interface KakaoNaviLaunchOptions {
  name: string;
  x: number;
  y: number;
  coordType?: "wgs84" | "katec";
}

export interface KakaoNaviNamespace {
  start(options: KakaoNaviLaunchOptions): void;
  share(options: KakaoNaviLaunchOptions): void;
}

export interface KakaoSdk {
  init(javascriptKey: string): void;
  isInitialized(): boolean;
  Navi: KakaoNaviNamespace;
}

declare global {
  interface Window {
    Kakao: KakaoSdk;
  }
}

export {};

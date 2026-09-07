import { useEffect, useState } from "react";
import { KAKAO_JS_KEY } from "@/constants/env";
import { KAKAO_JS_SDK_INTEGRITY, KAKAO_JS_SDK_VERSION } from "@/constants/kakaoSdk";

type SdkStatus = "loading" | "ready" | "error";

let sdkPromise: Promise<void> | null = null;

/**
 * 카카오내비는 별도의 "Kakao SDK for JavaScript"(window.Kakao)를 사용한다 — 지도 SDK와는
 * 다른 스크립트다. 같은 JavaScript 키로 Kakao.init()까지 완료한 뒤에만 Kakao.Navi.start()를
 * 호출할 수 있다. REST API 키는 여기서 전혀 쓰지 않는다.
 */
function loadKakaoSdk(): Promise<void> {
  if (window.Kakao?.isInitialized?.()) {
    return Promise.resolve();
  }
  if (sdkPromise) {
    return sdkPromise;
  }
  if (!KAKAO_JS_KEY) {
    return Promise.reject(new Error("VITE_KAKAO_JS_KEY가 설정되지 않았습니다."));
  }

  sdkPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = `https://t1.kakaocdn.net/kakao_js_sdk/${KAKAO_JS_SDK_VERSION}/kakao.min.js`;
    if (KAKAO_JS_SDK_INTEGRITY) {
      script.integrity = KAKAO_JS_SDK_INTEGRITY;
      script.crossOrigin = "anonymous";
    }
    script.async = true;
    script.onload = () => {
      if (!window.Kakao.isInitialized()) {
        window.Kakao.init(KAKAO_JS_KEY);
      }
      resolve();
    };
    script.onerror = () => {
      sdkPromise = null;
      reject(new Error("카카오 JavaScript SDK를 불러오지 못했습니다."));
    };
    document.head.appendChild(script);
  });

  return sdkPromise;
}

/** 카카오내비 버튼이 눌리기 전에 미리 로드해두기 위한 훅. */
export function useKakaoSdk(): SdkStatus {
  const [status, setStatus] = useState<SdkStatus>("loading");

  useEffect(() => {
    let cancelled = false;
    loadKakaoSdk()
      .then(() => {
        if (!cancelled) setStatus("ready");
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return status;
}

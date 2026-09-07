import { useEffect, useState } from "react";
import { KAKAO_JS_KEY } from "@/constants/env";

type SdkStatus = "loading" | "ready" | "error";

let sdkPromise: Promise<void> | null = null;

/**
 * 카카오맵 JS SDK를 문서에 한 번만 삽입하고, kakao.maps.load() 콜백이 끝난 뒤 resolve한다.
 * clusterer 라이브러리를 함께 로드한다(마커 클러스터링에 필요).
 */
function loadKakaoMapsSdk(): Promise<void> {
  if (window.kakao?.maps?.Map) {
    return Promise.resolve();
  }
  if (sdkPromise) {
    return sdkPromise;
  }
  if (!KAKAO_JS_KEY) {
    return Promise.reject(
      new Error("VITE_KAKAO_JS_KEY가 설정되지 않았습니다."),
    );
  }

  sdkPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false&libraries=clusterer`;
    script.async = true;
    script.onload = () => {
      window.kakao.maps.load(() => resolve());
    };
    script.onerror = () => {
      sdkPromise = null; // 실패 시 다음 마운트에서 재시도할 수 있게 초기화
      reject(new Error("카카오맵 SDK를 불러오지 못했습니다."));
    };
    document.head.appendChild(script);
  });

  return sdkPromise;
}

/** 카카오맵 SDK 로드 상태. MapView는 "ready"가 될 때까지 지도를 그리지 않는다. */
export function useKakaoMapsSdk(): SdkStatus {
  const [status, setStatus] = useState<SdkStatus>("loading");

  useEffect(() => {
    let cancelled = false;
    loadKakaoMapsSdk()
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

import { useEffect, useState } from "react";

export type CurrentLocationStatus =
  | "idle"
  | "locating"
  | "success"
  | "permission_denied"
  | "unavailable"
  | "timeout";

export interface CurrentLocationState {
  status: CurrentLocationStatus;
  position: GeolocationPosition | null;
}

/**
 * MAP-01: 브라우저 GPS 권한을 요청하고 현재 좌표를 가져온다.
 * 권한 거부/GPS 불가/타임아웃이 발생해도 지도 자체는 계속 탐색 가능해야 하므로
 * (프론트 구현 명세서 4장), 이 훅은 오류를 던지지 않고 상태로만 노출한다.
 */
export function useCurrentLocation(): CurrentLocationState {
  const [state, setState] = useState<CurrentLocationState>({
    status: "idle",
    position: null,
  });

  useEffect(() => {
    if (!("geolocation" in navigator)) {
      // 브라우저가 Geolocation 자체를 지원하지 않는 즉시 종료 케이스 — 의도적인 setState.
      setState({ status: "unavailable", position: null });
      return;
    }

    setState({ status: "locating", position: null });

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setState({ status: "success", position });
      },
      (error) => {
        if (error.code === error.PERMISSION_DENIED) {
          setState({ status: "permission_denied", position: null });
        } else if (error.code === error.TIMEOUT) {
          setState({ status: "timeout", position: null });
        } else {
          setState({ status: "unavailable", position: null });
        }
      },
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 0 },
    );
  }, []);

  return state;
}

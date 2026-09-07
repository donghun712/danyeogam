import { useCallback, useRef, useState } from "react";
import { verifyStamp } from "@/api/stampApi";
import { ApiError, NetworkError } from "@/api/client";
import { createIdempotencyKey } from "@/utils/idempotencyKey";
import { emitAttractionVisited } from "@/utils/attractionEvents";
import type { StampVerificationResult } from "@/types/api";

/**
 * STAMP-01/02 전체 상태 기계.
 *
 * - "gps_*" 상태: 브라우저에서 좌표 자체를 못 얻은 경우(서버까지 가지 않음).
 * - "measuring"/"verifying": 진행 중 상태.
 * - "result": 서버가 200으로 응답한 경우. result.status가 실제 백엔드 값
 *   (VERIFIED_NEW / VERIFIED_ALREADY_ACQUIRED / OUT_OF_RANGE / GPS_ACCURACY_INSUFFICIENT /
 *   LOCATION_STALE / STAMP_DISABLED) 그대로이며, 여기서 값을 합치거나 새로 만들지 않는다.
 * - "http_error": 서버가 4xx/5xx로 응답했거나 네트워크 자체가 끊긴 경우. errorCode에
 *   실제 error.code(예: AUTHENTICATION_REQUIRED, TOO_MANY_REQUESTS 등)를 그대로 담는다.
 */
export type StampVerificationPhase =
  | "ready"
  | "gps_permission_denied"
  | "gps_unavailable"
  | "gps_timeout"
  | "measuring"
  | "verifying"
  | "result"
  | "http_error";

/** 브라우저 Geolocation이 실제로 반환한 값 그대로 — 화면 표시용으로만 쓰고 서버 판정에는 영향 없음. */
export interface LastMeasurement {
  accuracyMeters: number;
  measuredAt: string;
}

interface StampVerificationState {
  phase: StampVerificationPhase;
  result: StampVerificationResult | null;
  errorCode: string | null;
  lastMeasurement: LastMeasurement | null;
}

export function useStampVerification(touristSpotId: number) {
  const [state, setState] = useState<StampVerificationState>({
    phase: "ready",
    result: null,
    errorCode: null,
    lastMeasurement: null,
  });
  // 중복 클릭 방지: setState는 비동기라 즉시 참조 가능한 ref로도 같이 막는다.
  const isBusyRef = useRef(false);

  const start = useCallback(() => {
    if (isBusyRef.current) return;
    isBusyRef.current = true;

    // 새 시도를 시작하므로 이전 측정값 표시도 지운다(다음 측정이 나올 때까지 낡은 값을 보여주지 않음).
    setState({ phase: "measuring", result: null, errorCode: null, lastMeasurement: null });

    if (!("geolocation" in navigator)) {
      isBusyRef.current = false;
      setState({
        phase: "gps_unavailable",
        result: null,
        errorCode: null,
        lastMeasurement: null,
      });
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lastMeasurement: LastMeasurement = {
          accuracyMeters: position.coords.accuracy,
          measuredAt: new Date(position.timestamp).toISOString(),
        };
        setState((prev) => ({ ...prev, phase: "verifying", lastMeasurement }));

        verifyStamp(
          {
            touristSpotId,
            position: {
              latitude: position.coords.latitude,
              longitude: position.coords.longitude,
              accuracyMeters: position.coords.accuracy,
              measuredAt: lastMeasurement.measuredAt,
            },
          },
          createIdempotencyKey(),
        )
          .then((result) => {
            isBusyRef.current = false;
            setState((prev) => ({
              ...prev,
              phase: "result",
              result,
              errorCode: null,
            }));
            if (
              result.status === "VERIFIED_NEW" ||
              result.status === "VERIFIED_ALREADY_ACQUIRED"
            ) {
              emitAttractionVisited(touristSpotId);
            }
          })
          .catch((error: unknown) => {
            isBusyRef.current = false;
            const code =
              error instanceof ApiError
                ? error.code
                : error instanceof NetworkError
                  ? "NETWORK_ERROR"
                  : "UNKNOWN_ERROR";
            setState((prev) => ({
              ...prev,
              phase: "http_error",
              result: null,
              errorCode: code,
            }));
          });
      },
      (error) => {
        isBusyRef.current = false;
        if (error.code === error.PERMISSION_DENIED) {
          setState({
            phase: "gps_permission_denied",
            result: null,
            errorCode: null,
            lastMeasurement: null,
          });
        } else if (error.code === error.TIMEOUT) {
          setState({
            phase: "gps_timeout",
            result: null,
            errorCode: null,
            lastMeasurement: null,
          });
        } else {
          setState({
            phase: "gps_unavailable",
            result: null,
            errorCode: null,
            lastMeasurement: null,
          });
        }
      },
      // 인증 버튼을 누른 시점의 새 위치가 필요하므로 캐시된 좌표를 재사용하지 않는다.
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 0 },
    );
  }, [touristSpotId]);

  const isBusy = state.phase === "measuring" || state.phase === "verifying";

  return { ...state, isBusy, start };
}

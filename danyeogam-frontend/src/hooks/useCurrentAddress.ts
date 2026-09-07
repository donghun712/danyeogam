import { useEffect, useState } from "react";
import { reverseGeocode } from "@/api/geoApi";

type AddressStatus = "idle" | "loading" | "success" | "error";

interface CurrentAddressState {
  status: AddressStatus;
  addressName: string | null;
}

/**
 * MAP-02: 현재 좌표 → 주소. 503 GEO_PROVIDER_UNAVAILABLE 등으로 실패해도
 * 지도/마커는 계속 표시해야 하므로(백엔드 문서 8.2절) 이 훅의 실패를 화면 전체 오류로 전파하지 않는다.
 */
export function useCurrentAddress(
  position: { latitude: number; longitude: number } | null,
): CurrentAddressState {
  const [state, setState] = useState<CurrentAddressState>({
    status: "idle",
    addressName: null,
  });

  useEffect(() => {
    if (!position) return;

    let cancelled = false;
    // 요청 시작을 즉시 반영하는 의도적인 로딩 상태 setState (취소 가능한 데이터 페칭 패턴)
    setState({ status: "loading", addressName: null });

    reverseGeocode(position)
      .then((result) => {
        if (cancelled) return;
        setState({
          status: "success",
          addressName: result.roadAddressName ?? result.addressName,
        });
      })
      .catch(() => {
        if (cancelled) return;
        setState({ status: "error", addressName: null });
      });

    return () => {
      cancelled = true;
    };
  }, [position]);

  return state;
}

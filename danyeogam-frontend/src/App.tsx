import { useEffect } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { TabLayout } from "@/components/common/TabLayout";
import { MapPage } from "@/pages/MapPage";
import { CollectionPage } from "@/pages/CollectionPage";
import { AttractionDetailPage } from "@/pages/AttractionDetailPage";
import { ParkingListPage } from "@/pages/ParkingListPage";
import { StampVerificationPage } from "@/pages/StampVerificationPage";
import { createOrReuseAnonymousSession } from "@/api/sessionApi";

export function App() {
  useEffect(() => {
    // 세션 문서 권장: 앱 시작 시 한 번 호출. 실패해도 지도 API는 계속 호출 가능해야 하므로
    // (visitState="UNKNOWN"으로 표시됨) 여기서 오류를 앱 전체에 전파하지 않는다.
    createOrReuseAnonymousSession().catch(() => {
      // 세션 발급 실패는 조용히 무시한다 — 이후 개별 화면에서 401을 받으면 재시도할 수 있다.
    });
  }, []);

  return (
    <Routes>
      {/* 하단 탭 네비게이션이 보이는 화면 */}
      <Route element={<TabLayout />}>
        <Route path="/" element={<MapPage />} />
        <Route path="/collection" element={<CollectionPage />} />
      </Route>

      {/* 뒤로가기 상단바를 쓰는 풀페이지 화면 (하단 탭 없음) */}
      <Route path="/attractions/:attractionId" element={<AttractionDetailPage />} />
      <Route
        path="/attractions/:attractionId/parking"
        element={<ParkingListPage />}
      />
      <Route
        path="/attractions/:attractionId/stamp"
        element={<StampVerificationPage />}
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

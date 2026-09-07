import { Outlet } from "react-router-dom";
import { BottomNavigation } from "./BottomNavigation";
import styles from "./TabLayout.module.css";

/**
 * 지도 화면과 도감 화면처럼 하단 탭 네비게이션이 보이는 화면들의 레이아웃.
 * 관광지 상세 풀페이지 / 주차장 목록 화면은 FullPageLayout(뒤로가기 상단바)을 쓰고
 * 이 레이아웃 밖의 라우트로 둔다 — App.tsx 참고.
 */
export function TabLayout() {
  return (
    <div className={styles.container}>
      <main className={styles.main}>
        <Outlet />
      </main>
      <BottomNavigation />
    </div>
  );
}

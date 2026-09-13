import { NavLink } from "react-router-dom";
import { ROUTES } from "@/constants/routes";
import { AppIcon } from "@/constants/icons";
import styles from "./BottomNavigation.module.css";

/** 지도 / 도감 / 칭호 세 탭. 원래 칭호는 도감 하단에 붙어 있었으나, 스크롤해야만 보여서
 * 접근성이 떨어진다는 실제 테스트 피드백을 받아 별도 탭으로 분리했다. */
export function BottomNavigation() {
  return (
    <nav className={styles.nav} aria-label="주요 화면 이동">
      <NavLink
        to={ROUTES.map}
        end
        className={({ isActive }) =>
          isActive ? `${styles.tab} ${styles.active}` : styles.tab
        }
      >
        <AppIcon.map size={22} strokeWidth={2} aria-hidden="true" />
        <span className="text-caption">지도</span>
      </NavLink>
      <NavLink
        to={ROUTES.collection}
        className={({ isActive }) =>
          isActive ? `${styles.tab} ${styles.active}` : styles.tab
        }
      >
        <AppIcon.collection size={22} strokeWidth={2} aria-hidden="true" />
        <span className="text-caption">도감</span>
      </NavLink>
      <NavLink
        to={ROUTES.titles}
        className={({ isActive }) =>
          isActive ? `${styles.tab} ${styles.active}` : styles.tab
        }
      >
        <AppIcon.title size={22} strokeWidth={2} aria-hidden="true" />
        <span className="text-caption">칭호</span>
      </NavLink>
    </nav>
  );
}

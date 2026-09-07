import { NavLink } from "react-router-dom";
import { ROUTES } from "@/constants/routes";
import { AppIcon } from "@/constants/icons";
import styles from "./BottomNavigation.module.css";

/** 디자인 시스템 9장: 하단 네비게이션은 지도 / 도감 두 탭만 존재한다. */
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
    </nav>
  );
}

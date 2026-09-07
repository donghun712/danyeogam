import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { AppIcon } from "@/constants/icons";
import styles from "./FullPageLayout.module.css";

interface FullPageLayoutProps {
  title: string;
  children: ReactNode;
}

/**
 * 화면 시안의 "←" 뒤로가기 상단바를 쓰는 풀페이지 화면 공통 레이아웃.
 * 관광지 상세 페이지, 주변 주차장 목록 화면에서 사용한다.
 */
export function FullPageLayout({ title, children }: FullPageLayoutProps) {
  const navigate = useNavigate();

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <button
          type="button"
          className={styles.backButton}
          onClick={() => navigate(-1)}
          aria-label="뒤로가기"
        >
          <AppIcon.back size={22} strokeWidth={2} />
        </button>
        <h1 className={`text-h2 ${styles.title}`}>{title}</h1>
      </header>
      <main className={styles.main}>{children}</main>
    </div>
  );
}

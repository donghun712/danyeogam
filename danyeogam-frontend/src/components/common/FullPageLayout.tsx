import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { AppIcon } from "@/constants/icons";
import styles from "./FullPageLayout.module.css";

interface FullPageLayoutProps {
  title: string;
  children: ReactNode;
  /** 화면시안 03번 헤더 우측 아이콘(공유 등) 자리. 안 넘기면 기존처럼 뒤로가기+제목만 나온다. */
  headerActions?: ReactNode;
  /**
   * 화면시안 "03 관광지 상세 페이지"는 헤더에 이름 텍스트가 없다(본문 제목에서만 표시).
   * true면 title을 화면에는 안 보이게(스크린리더용으로만) 렌더링한다 — 접근성 상 페이지
   * 제목 자체는 필요해서 완전히 없애지 않는다. 기본값 false는 기존 화면(주차장/스탬프
   * 인증 등)과 동일하게 항상 보이는 제목을 유지한다.
   */
  hideTitle?: boolean;
  /**
   * 요청서 P0-1 — 상세 페이지 Hero 전용 예외. 있으면 기존 아이보리 헤더 바
   * (뒤로가기+제목+headerActions) 대신 이 hero를 최상단에 그대로 배치한다 —
   * 뒤로가기/즐겨찾기/공유는 hero 컴포넌트 내부에서 사진 위 오버레이로 직접 그린다.
   * title/headerActions는 이 경우 스크린리더 접근성 확인용으로만 남기고 화면엔 안 보인다.
   */
  hero?: ReactNode;
}

/**
 * 화면 시안의 "←" 뒤로가기 상단바를 쓰는 풀페이지 화면 공통 레이아웃.
 * 관광지 상세 페이지, 주변 주차장 목록 화면에서 사용한다.
 */
export function FullPageLayout({
  title,
  children,
  headerActions,
  hideTitle = false,
  hero,
}: FullPageLayoutProps) {
  const navigate = useNavigate();

  if (hero) {
    return (
      <div className={styles.container}>
        <h1 className={styles.srOnly}>{title}</h1>
        {hero}
        <main className={styles.main}>{children}</main>
      </div>
    );
  }

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
        <h1
          className={
            hideTitle ? styles.srOnly : `text-h2 ${styles.title}`
          }
        >
          {title}
        </h1>
        {headerActions && (
          <div className={styles.headerActions}>{headerActions}</div>
        )}
      </header>
      <main className={styles.main}>{children}</main>
    </div>
  );
}

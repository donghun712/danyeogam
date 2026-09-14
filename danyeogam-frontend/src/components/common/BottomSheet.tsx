import { useEffect } from "react";
import type { ReactNode } from "react";
import styles from "./BottomSheet.module.css";

interface BottomSheetProps {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
  /**
   * 요청서 5.3 — 인증 CTA를 본문과 분리된 시트 내부 고정 푸터로 둔다. 안 넘기면
   * 기존처럼 children만 있는 시트로 동작한다(다른 BottomSheet 사용처에 영향 없음).
   */
  footer?: ReactNode;
}

/**
 * 디자인 시스템 14장: 마커 선택 시 새 페이지로 이동하지 않고 Bottom Sheet를 띄운다.
 * 상단 손잡이("─────")와 배경 딤 처리, 24px 라운드를 포함한 범용 컨테이너.
 * footer가 있으면 본문만 스크롤되고 footer는 시트 하단에 고정된다.
 */
export function BottomSheet({ open, onClose, children, footer }: BottomSheetProps) {
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div
        className={styles.sheet}
        role="dialog"
        aria-modal="true"
        onClick={(event) => event.stopPropagation()}
      >
        <div className={styles.handle} aria-hidden="true" />
        <div className={styles.scrollArea}>{children}</div>
        {footer && <div className={styles.footer}>{footer}</div>}
      </div>
    </div>
  );
}

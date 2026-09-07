import { useEffect } from "react";
import type { ReactNode } from "react";
import styles from "./BottomSheet.module.css";

interface BottomSheetProps {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
}

/**
 * 디자인 시스템 14장: 마커 선택 시 새 페이지로 이동하지 않고 Bottom Sheet를 띄운다.
 * 상단 손잡이("─────")와 배경 딤 처리, 24px 라운드를 포함한 범용 컨테이너.
 * 실제 내용(이미지/이름/설명 등)은 STEP 4의 AttractionBottomSheet에서 채운다.
 */
export function BottomSheet({ open, onClose, children }: BottomSheetProps) {
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
        {children}
      </div>
    </div>
  );
}

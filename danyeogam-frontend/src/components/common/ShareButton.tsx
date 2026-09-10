import { useState } from "react";
import { AppIcon } from "@/constants/icons";
import styles from "./ShareButton.module.css";

interface ShareButtonProps {
  title: string;
  /** 공유할 URL. 안 넘기면 현재 페이지 주소를 쓴다. */
  url?: string;
}

/**
 * 화면시안 헤더의 공유 아이콘. 새 데이터/저장소가 필요 없어서(즐겨찾기와 달리) 바로 구현했다.
 * Web Share API를 지원하면 OS 공유 시트를, 아니면 링크를 클립보드에 복사한다.
 */
export function ShareButton({ title, url }: ShareButtonProps) {
  const [copied, setCopied] = useState(false);

  const handleClick = async () => {
    const shareUrl = url ?? window.location.href;

    if (navigator.share) {
      try {
        await navigator.share({ title, url: shareUrl });
      } catch {
        // 사용자가 공유 시트를 취소한 경우 등 — 오류로 취급하지 않는다.
      }
      return;
    }

    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      // 클립보드 접근도 실패하면 조용히 무시한다 — 전체 화면을 막을 정도의 기능이 아니다.
    }
  };

  return (
    <button
      type="button"
      className={styles.button}
      onClick={handleClick}
      aria-label="공유하기"
    >
      <AppIcon.share size={20} strokeWidth={2} />
      {copied && <span className={`text-caption ${styles.toast}`}>링크 복사됨</span>}
    </button>
  );
}

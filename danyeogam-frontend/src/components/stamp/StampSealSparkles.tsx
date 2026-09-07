import type { ReactNode } from "react";
import styles from "./StampSealSparkles.module.css";

interface StampSealSparklesProps {
  children: ReactNode;
}

const SPARKLE_POSITIONS = [
  { top: "6%", left: "8%", size: 14, delay: "0s" },
  { top: "4%", left: "78%", size: 10, delay: "0.4s" },
  { top: "78%", left: "4%", size: 10, delay: "0.8s" },
  { top: "82%", left: "82%", size: 16, delay: "0.2s" },
] as const;

function SparkleShape() {
  return (
    <svg viewBox="0 0 24 24" width="100%" height="100%" aria-hidden="true">
      <path
        d="M12 0 L14.2 9.8 L24 12 L14.2 14.2 L12 24 L9.8 14.2 L0 12 L9.8 9.8 Z"
        fill="var(--color-muted-gold)"
      />
    </svg>
  );
}

/**
 * 화면시안 "05 스탬프 획득!" 인장 주변의 반짝임 장식 — 순수 시각 요소.
 * StampSeal 내부(인증 로직·SVG 생성)는 건드리지 않고, 감싸는 래퍼에서 장식만 얹는다.
 */
export function StampSealSparkles({ children }: StampSealSparklesProps) {
  return (
    <div className={styles.wrapper}>
      {SPARKLE_POSITIONS.map((pos) => (
        <span
          key={`${pos.top}-${pos.left}`}
          className={styles.sparkle}
          style={{
            top: pos.top,
            left: pos.left,
            width: pos.size,
            height: pos.size,
            animationDelay: pos.delay,
          }}
        >
          <SparkleShape />
        </span>
      ))}
      {children}
    </div>
  );
}

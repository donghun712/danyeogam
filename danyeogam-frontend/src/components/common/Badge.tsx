import type { ReactNode } from "react";
import styles from "./Badge.module.css";

type BadgeTone = "brand" | "success" | "neutral";

interface BadgeProps {
  tone?: BadgeTone;
  children: ReactNode;
}

/**
 * 디자인 시스템 27장: Badge는 정보를 빠르게 이해시키는 용도로만 사용한다.
 * 예: <Badge tone="brand">스탬프 가능</Badge>, <Badge tone="success">방문 완료</Badge>
 *
 * 색상만으로 상태를 구분하지 않는다는 원칙(3.3장)에 따라, 상태를 나타낼 때는
 * 호출부에서 아이콘이나 텍스트("✓ 방문 완료" 등)를 함께 넣어 사용한다.
 */
export function Badge({ tone = "neutral", children }: BadgeProps) {
  return <span className={`${styles.badge} ${styles[tone]}`}>{children}</span>;
}

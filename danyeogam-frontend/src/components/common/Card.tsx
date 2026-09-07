import type { HTMLAttributes, ReactNode } from "react";
import styles from "./Card.module.css";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  padded?: boolean;
}

/** 관광지 카드, 도감 카드, 주차장 카드 등에서 공통으로 쓰는 컨테이너. */
export function Card({ children, padded = true, className, ...rest }: CardProps) {
  const classNames = [styles.card, padded ? styles.padded : "", className ?? ""]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={classNames} {...rest}>
      {children}
    </div>
  );
}

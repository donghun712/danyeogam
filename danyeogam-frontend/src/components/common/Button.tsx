import type { ButtonHTMLAttributes, ReactNode } from "react";
import styles from "./Button.module.css";

type ButtonVariant = "primary" | "secondary" | "ghost";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  fullWidth?: boolean;
  children: ReactNode;
}

/**
 * 디자인 시스템 26장 Button System.
 * Primary: 브랜드 핵심 행동(예: 스탬프 인증하기) — Heritage Brown
 * Secondary: 보조 행동(예: 주차장 보기) — Sand 계열 배경
 * Ghost: 배경 없는 텍스트 버튼(예: 길찾기)
 * disabled 속성을 주면 자동으로 27장 Disabled 스타일이 적용된다.
 */
export function Button({
  variant = "primary",
  fullWidth = false,
  className,
  children,
  ...rest
}: ButtonProps) {
  const classNames = [
    styles.button,
    styles[variant],
    fullWidth ? styles.fullWidth : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button className={classNames} {...rest}>
      {children}
    </button>
  );
}

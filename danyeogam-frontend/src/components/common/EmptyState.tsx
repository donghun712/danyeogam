import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import styles from "./EmptyState.module.css";

interface EmptyStateProps {
  icon: LucideIcon;
  message: string;
  action?: ReactNode;
}

export function EmptyState({ icon: Icon, message, action }: EmptyStateProps) {
  return (
    <div className={styles.container}>
      <Icon size={32} strokeWidth={1.5} aria-hidden="true" />
      <p className={`text-body ${styles.message}`}>{message}</p>
      {action}
    </div>
  );
}

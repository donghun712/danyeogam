import type { ComponentType } from "react";
import type { FacilityStatus } from "@/types/api";
import styles from "./FacilityBadge.module.css";

interface FacilityBadgeProps {
  icon: ComponentType<{ size?: number; strokeWidth?: number }>;
  label: string;
  status: FacilityStatus;
}

/**
 * status만 보고 표시를 결정하지 않는다 — UNKNOWN이어도 note(원문)가 있으면 그 텍스트를
 * 그대로 보여준다. note가 없는 UNKNOWN만 "정보 없음"에 해당하며, 이 경우 항목 자체를
 * 렌더링하지 않는다(백엔드 문서 참고).
 */
export function FacilityBadge({ icon: Icon, label, status }: FacilityBadgeProps) {
  if (status.status === "UNKNOWN" && !status.note) {
    return null;
  }

  const detailText =
    status.status === "AVAILABLE"
      ? "가능"
      : status.status === "UNAVAILABLE"
        ? "불가"
        : status.note;

  return (
    <div className={`${styles.badge} ${styles[status.status.toLowerCase()]}`}>
      <Icon size={18} strokeWidth={2} />
      <div className={styles.text}>
        <span className="text-caption">{label}</span>
        <span className="text-caption">{detailText}</span>
      </div>
    </div>
  );
}

import type { CollectionRegionSummary } from "@/types/api";
import styles from "./CollectionProgress.module.css";

interface CollectionProgressProps {
  summary: CollectionRegionSummary;
}

/**
 * 디자인 시스템 23장 Progress.
 * visitedCount/totalCount/progressPercent는 모두 서버가 계산해서 내려주는 값을 그대로
 * 표시한다 — 프론트에서 진행률을 다시 계산하거나 추측하지 않는다.
 */
export function CollectionProgress({ summary }: CollectionProgressProps) {
  return (
    <div className={styles.container}>
      <div className={styles.countRow}>
        <span className="text-h3">
          {summary.visitedCount} / {summary.totalCount}
        </span>
        <span className="text-body">{summary.progressPercent}%</span>
      </div>
      <div className={styles.track} role="presentation">
        <div
          className={styles.fill}
          style={{ width: `${summary.progressPercent}%` }}
        />
      </div>
    </div>
  );
}

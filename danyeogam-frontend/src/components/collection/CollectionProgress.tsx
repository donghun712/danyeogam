import type { CollectionRegionSummary } from "@/types/api";
import styles from "./CollectionProgress.module.css";

interface CollectionProgressProps {
  summary: CollectionRegionSummary;
}

/**
 * 디자인 시스템 23장 Progress.
 * visitedCount/totalCount는 서버가 계산해서 내려주는 값을 그대로 쓴다 — 방문/전체
 * 개수 집계 자체는 여기서 다시 계산하지 않는다.
 * 요청서(출시 전) 2단계 — 다만 백엔드 문서(frontend-collection-api.md)에 명시된 대로
 * progressPercent 자체는 서버가 이미 정수로 반올림해서 내려주기 때문에, 전체 대상이
 * 많은 지역(예: 1/296)에서는 실제로 0.3%가 반영됐는데도 화면엔 "0%"로만 보여
 * "기록이 안 됐다"고 오해할 수 있다. visitedCount/totalCount는 정수째 그대로 있으니
 * 그 두 값으로 표시 전용 비율을 다시 계산해 소수점 첫째 자리까지 보여준다 — 칭호
 * 지급 등 판정 로직에는 이 값을 전혀 쓰지 않는다(서버 progressPercent와 별개).
 */
export function CollectionProgress({ summary }: CollectionProgressProps) {
  const displayPercent =
    summary.totalCount > 0
      ? (summary.visitedCount / summary.totalCount) * 100
      : 0;

  return (
    <div className={styles.container}>
      <div className={styles.countRow}>
        <span className="text-h3">
          {summary.visitedCount} / {summary.totalCount}
        </span>
        <span className="text-body">{displayPercent.toFixed(1)}%</span>
      </div>
      <div className={styles.track} role="presentation">
        <div
          className={styles.fill}
          style={{ width: `${displayPercent}%` }}
        />
      </div>
    </div>
  );
}

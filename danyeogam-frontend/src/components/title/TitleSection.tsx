import { AppIcon } from "@/constants/icons";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { Skeleton } from "@/components/common/Skeleton";
import { useTitles } from "@/hooks/useTitles";
import type { Title } from "@/types/api";
import styles from "./TitleSection.module.css";

/**
 * currentValue/targetValue/progressUnit은 전부 서버가 계산해서 내려주는 값을 그대로
 * 표시한다 — CollectionProgress와 동일한 원칙으로 프론트에서 다시 계산하지 않는다.
 */
function formatProgress({ currentValue, targetValue, progressUnit }: Title): string {
  const shown = Math.min(currentValue, targetValue);
  return progressUnit === "PERCENT" ? `${shown}% / ${targetValue}%` : `${shown} / ${targetValue}`;
}

function TitleTile({ title }: { title: Title }) {
  const Icon = title.earned ? AppIcon.title : AppIcon.titleLocked;
  return (
    <div
      className={`${styles.tile} ${title.earned ? styles.earned : styles.locked}`}
      title={title.description}
    >
      <Icon className={styles.icon} size={18} strokeWidth={2} aria-hidden="true" />
      <div className={styles.body}>
        <p className={`text-caption ${styles.name}`}>{title.name}</p>
        <p className={styles.progress}>{formatProgress(title)}</p>
      </div>
    </div>
  );
}

/**
 * 도감 화면 하단의 칭호(업적) 섹션. GET /me/titles가 활성 칭호 전체(획득 여부 포함)를
 * 표시 순서대로 내려주므로, 그 순서를 그대로 쓰고 프론트에서 재정렬하지 않는다.
 * 지역 진행률(CollectionProgress)과 달리 칭호는 광역 선택과 무관한 전역 데이터라
 * 선택된 지역이 바뀌어도 다시 불러오지 않는다.
 */
export function TitleSection() {
  const { status, titles, retry } = useTitles();
  const earnedCount = titles.filter((title) => title.earned).length;

  return (
    <section className={styles.container}>
      <div className={styles.header}>
        <AppIcon.title className={styles.headerIcon} size={18} strokeWidth={2} aria-hidden="true" />
        <span className="text-h3">칭호</span>
        {status === "success" && (
          <span className={`text-caption ${styles.count}`}>
            {earnedCount} / {titles.length}
          </span>
        )}
      </div>

      {status === "loading" && (
        <div className={styles.grid}>
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton
              key={`title-skeleton-${index}`}
              height="52px"
              radius="var(--radius-card)"
            />
          ))}
        </div>
      )}

      {status === "error" && (
        <ErrorState
          message="칭호 정보를 불러오지 못했습니다."
          onRetry={retry}
          compact
        />
      )}

      {status === "empty" && (
        <EmptyState icon={AppIcon.title} message="아직 표시할 칭호가 없습니다." />
      )}

      {status === "success" && (
        <div className={styles.grid}>
          {titles.map((title) => (
            <TitleTile key={title.id} title={title} />
          ))}
        </div>
      )}
    </section>
  );
}

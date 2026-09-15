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
 * 요청서 — 조건 단위를 해석 가능한 형태로: 횟수/지역 수는 "n / 목표단위",
 * 비율은 "현재 n% · 목표 m%".
 */
function formatProgress({ currentValue, targetValue, progressUnit }: Title): string {
  const shown = Math.min(currentValue, targetValue);
  if (progressUnit === "PERCENT") return `현재 ${shown}% · 목표 ${targetValue}%`;
  const unitLabel = progressUnit === "REGIONS" ? "곳" : "회";
  return `${shown} / ${targetValue}${unitLabel}`;
}

/** progress bar 채움 비율(%) — 서버가 내려준 currentValue/targetValue 그대로 계산, 새 데이터 없음 */
function progressPercent({ currentValue, targetValue, progressUnit }: Title): number {
  if (targetValue <= 0) return 0;
  const value = progressUnit === "PERCENT" ? currentValue : (currentValue / targetValue) * 100;
  return Math.max(0, Math.min(100, value));
}

/** 획득일은 실제 값이 있을 때만 "2026.05.12" 형태로 — 시간대 변환 없이 날짜 부분만 그대로 */
function formatAwardedDate(awardedAt: string): string {
  return awardedAt.slice(0, 10).replaceAll("-", ".");
}

/**
 * 요청서 5단계 — 획득/미획득을 미세조정이 아니라 완전히 다른 역할로 재설계.
 * 획득 = "기념패"(특별한 카드), 미획득 = "목표 목록"(간결한 리스트 아이템).
 */
function EarnedTile({ title }: { title: Title }) {
  return (
    <div className={styles.earnedTile}>
      <div className={styles.medal}>
        <AppIcon.title size={20} strokeWidth={2} aria-hidden="true" />
      </div>
      <div className={styles.earnedBody}>
        <div className={styles.earnedTopRow}>
          <p className={styles.earnedName}>{title.name}</p>
          <span className={styles.earnedBadge}>획득</span>
        </div>
        {title.description && (
          <p className={`text-caption ${styles.earnedDescription}`}>{title.description}</p>
        )}
        {title.awardedAt && (
          <p className={`text-caption ${styles.earnedDate}`}>{formatAwardedDate(title.awardedAt)} 획득</p>
        )}
      </div>
    </div>
  );
}

function LockedTile({ title }: { title: Title }) {
  return (
    <div className={styles.lockedTile}>
      <div className={styles.lockedTopRow}>
        <AppIcon.titleLocked size={14} strokeWidth={2} className={styles.lockIcon} aria-hidden="true" />
        <p className={styles.lockedName}>{title.name}</p>
      </div>
      {title.description && (
        <p className={`text-caption ${styles.lockedDescription}`}>{title.description}</p>
      )}
      <div className={styles.progressRow}>
        <div className={styles.progressTrack}>
          <div
            className={styles.progressFill}
            style={{ width: `${progressPercent(title)}%` }}
          />
        </div>
        <span className={styles.progressText}>{formatProgress(title)}</span>
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
        <div className={styles.list}>
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton
              key={`title-skeleton-${index}`}
              height="76px"
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
        <div className={styles.list}>
          {titles.map((title) =>
            title.earned ? (
              <EarnedTile key={title.id} title={title} />
            ) : (
              <LockedTile key={title.id} title={title} />
            ),
          )}
        </div>
      )}
    </section>
  );
}

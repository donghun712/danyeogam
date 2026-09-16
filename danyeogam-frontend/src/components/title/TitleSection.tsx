import { AppIcon } from "@/constants/icons";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { Skeleton } from "@/components/common/Skeleton";
import { useTitles } from "@/hooks/useTitles";
import type { Title } from "@/types/api";
import styles from "./TitleSection.module.css";

/**
 * 방문/지역 수와 칭호 지급 판정은 서버의 currentValue/targetValue를 그대로 따른다.
 * PERCENT의 소수점 표시에만 서버가 제공한 원본 개수를 사용한다.
 * 요청서 — 조건 단위를 해석 가능한 형태로: 횟수/지역 수는 "n / 목표단위",
 * 비율은 "현재 n% · 목표 m%".
 */
function exactProgressPercent({ currentValue, currentCount, targetCount }: Title): number {
  if (currentCount !== null && targetCount !== null && targetCount > 0) {
    return (currentCount / targetCount) * 100;
  }
  return currentValue;
}

function formatProgress(title: Title): string {
  const { currentValue, targetValue, progressUnit } = title;
  if (progressUnit === "PERCENT") {
    const shown = Math.min(exactProgressPercent(title), targetValue);
    return `현재 ${shown.toFixed(1)}% · 목표 ${targetValue}%`;
  }
  const shown = Math.min(currentValue, targetValue);
  const unitLabel = progressUnit === "REGIONS" ? "곳" : "회";
  return `${shown} / ${targetValue}${unitLabel}`;
}

/** progress bar 채움 비율(%) — PERCENT는 원본 개수로 표시 정밀도만 높인다. */
function progressPercent(title: Title): number {
  const { currentValue, targetValue, progressUnit } = title;
  if (targetValue <= 0) return 0;
  const value = progressUnit === "PERCENT"
    ? exactProgressPercent(title)
    : (currentValue / targetValue) * 100;
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
        <img src="/title-medal.png" alt="" className={styles.medalImage} />
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

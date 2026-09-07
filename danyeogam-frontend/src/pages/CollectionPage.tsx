import { useEffect, useState } from "react";
import { CollectionRegionSelector } from "@/components/collection/CollectionRegionSelector";
import { CollectionProgress } from "@/components/collection/CollectionProgress";
import { CollectionCard } from "@/components/collection/CollectionCard";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { Skeleton } from "@/components/common/Skeleton";
import { AppIcon } from "@/constants/icons";
import { useCollectionSummary } from "@/hooks/useCollectionSummary";
import { useCollectionList } from "@/hooks/useCollectionList";
import styles from "./CollectionPage.module.css";

/**
 * SCREEN-COLLECTION — 나만의 팔도 도감 (P1).
 *
 * GET /me/collection/summary로 지역 목록 + 진행률을 한 번에 받고, 사용자가 지역을 고르면
 * GET /me/collection?regionCode=...로 해당 지역의 방문/미방문 목록을 가져온다.
 * regionCode는 백엔드가 내려준 값("TOUR:AREA:45" 형태)을 그대로 사용하고 가공하지 않는다.
 *
 * 칭호(Title)는 백엔드 API가 아직 없어 이 화면에서 다루지 않는다.
 * TODO: GET /me/titles가 추가되면 이 페이지의 CollectionProgress 아래에
 * TitleSection(가칭)을 새로 만들어 연결한다 — 임의의 칭호 데이터/조건을 만들지 않는다.
 */
export function CollectionPage() {
  const summary = useCollectionSummary();
  const [selectedRegionCode, setSelectedRegionCode] = useState<string | null>(
    null,
  );

  // 요약이 로드되면 첫 번째 지역을 기본 선택한다(문서에 별도 기본 지역 규칙이 없어
  // API가 반환한 목록의 첫 항목을 사용한다).
  useEffect(() => {
    if (summary.status === "success" && selectedRegionCode === null) {
      // 최초 진입 시 기본 지역을 정하는 의도적인 setState (문서에 규칙이 없어 첫 항목 사용).
      setSelectedRegionCode(summary.regions[0]?.code ?? null);
    }
  }, [summary.status, summary.regions, selectedRegionCode]);

  const list = useCollectionList(selectedRegionCode);
  const selectedSummary = summary.regions.find(
    (region) => region.code === selectedRegionCode,
  );

  return (
    <div className={styles.container}>
      <h1 className={`text-h1 ${styles.title}`}>나만의 팔도 도감</h1>

      {summary.status === "loading" && summary.regions.length === 0 && (
        <div className={styles.loading}>
          <Skeleton height="40px" />
          <Skeleton height="24px" />
        </div>
      )}

      {summary.status === "error" && summary.regions.length === 0 && (
        <ErrorState
          message="도감 정보를 불러오지 못했습니다."
          onRetry={summary.retry}
        />
      )}

      {summary.status === "empty" && (
        <EmptyState
          icon={AppIcon.collection}
          message={"아직 방문한 관광지가 없습니다.\n첫 번째 스탬프를 획득해보세요."}
        />
      )}

      {summary.regions.length > 0 && selectedRegionCode && (
        <>
          <CollectionRegionSelector
            regions={summary.regions}
            selectedCode={selectedRegionCode}
            onChange={setSelectedRegionCode}
          />

          {selectedSummary && <CollectionProgress summary={selectedSummary} />}

          {list.status === "loading" && list.items.length === 0 && (
            <div className={styles.grid}>
              {Array.from({ length: 4 }).map((_, index) => (
                <Skeleton key={`skeleton-${index}`} height="140px" radius="var(--radius-card)" />
              ))}
            </div>
          )}

          {list.status === "error" && list.items.length === 0 && (
            <ErrorState
              message="도감 정보를 불러오지 못했습니다."
              onRetry={list.retry}
            />
          )}

          {list.status === "empty" && (
            <EmptyState
              icon={AppIcon.collection}
              message="이 지역에는 아직 표시할 관광지가 없습니다."
            />
          )}

          {list.items.length > 0 && (
            <div className={styles.grid}>
              {list.items.map((item) => (
                <CollectionCard key={item.touristSpotId} item={item} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}

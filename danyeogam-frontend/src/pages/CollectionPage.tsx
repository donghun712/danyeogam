import { useState } from "react";
import { CollectionRegionSelector } from "@/components/collection/CollectionRegionSelector";
import { CollectionSubRegionSelector } from "@/components/collection/CollectionSubRegionSelector";
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
 * 광역(도/특별시·광역시)을 고른 뒤, 그 안에 시군구 단위 진행률이 있으면(대부분의 광역이
 * 그렇다 — 세종특별자치시처럼 하위 지역이 없는 경우는 자동으로 숨김) 2단계로 더 좁혀볼 수
 * 있다. `GET /me/collection/summary?parentRegionCode=...`를 재사용한다(백엔드 문서 참고).
 *
 * 칭호(Title)는 백엔드 API가 아직 없어 이 화면에서 다루지 않는다.
 * TODO: GET /me/titles가 추가되면 이 페이지의 CollectionProgress 아래에
 * TitleSection(가칭)을 새로 만들어 연결한다 — 임의의 칭호 데이터/조건을 만들지 않는다.
 */
export function CollectionPage() {
  const summary = useCollectionSummary();
  const [preferredRegionCode, setPreferredRegionCode] = useState<string | null>(
    null,
  );

  // 사용자가 고른 지역이 현재 요약에 있으면 유지하고, 최초 진입 또는 목록 변경 시에는
  // API가 반환한 첫 지역을 렌더링 단계에서 기본값으로 사용한다.
  const selectedRegionCode = summary.regions.some(
    (region) => region.code === preferredRegionCode,
  )
    ? preferredRegionCode
    : (summary.regions[0]?.code ?? null);

  // 시군구 하위 진행률 — 광역이 바뀌면 하위 선택도 같이 초기화한다(effect 없이 렌더링 중 보정).
  const [syncedProvinceCode, setSyncedProvinceCode] = useState(selectedRegionCode);
  const [preferredSubRegionCode, setPreferredSubRegionCode] = useState<
    string | null
  >(null);
  if (selectedRegionCode !== syncedProvinceCode) {
    setSyncedProvinceCode(selectedRegionCode);
    setPreferredSubRegionCode(null);
  }

  const subSummary = useCollectionSummary(selectedRegionCode);
  // 백엔드 문서: "활성 하위 시군구가 없는 광역자치단체는 광역 자체를 단일 항목으로 반환" —
  // 그런 경우 굳이 2단계 선택기를 보여줄 필요가 없어서 항목이 2개 이상일 때만 노출한다.
  const hasSubRegions =
    subSummary.status === "success" && subSummary.regions.length > 1;
  const selectedSubRegionCode =
    hasSubRegions &&
    subSummary.regions.some((region) => region.code === preferredSubRegionCode)
      ? preferredSubRegionCode
      : null;

  const effectiveRegionCode = selectedSubRegionCode ?? selectedRegionCode;
  const list = useCollectionList(effectiveRegionCode);
  const effectiveSummary = selectedSubRegionCode
    ? subSummary.regions.find((region) => region.code === selectedSubRegionCode)
    : summary.regions.find((region) => region.code === selectedRegionCode);

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
          <div className={styles.selectorRow}>
            <CollectionRegionSelector
              regions={summary.regions}
              selectedCode={selectedRegionCode}
              onChange={setPreferredRegionCode}
            />
            {/* 하위 시군구가 없는 광역(세종특별자치시 등)이거나 아직 불러오는 중이어도
                이 자리 자체는 계속 유지하고 비활성화만 한다 — 나타났다 사라졌다 하면서
                레이아웃이 흔들리는 걸 막기 위함. */}
            <CollectionSubRegionSelector
              regions={hasSubRegions ? subSummary.regions : []}
              selectedCode={selectedSubRegionCode}
              onChange={setPreferredSubRegionCode}
              disabled={!hasSubRegions}
            />
          </div>

          {effectiveSummary && <CollectionProgress summary={effectiveSummary} />}

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

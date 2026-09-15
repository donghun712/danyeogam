import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { CollectionRegionSelector } from "@/components/collection/CollectionRegionSelector";
import { CollectionSubRegionSelector } from "@/components/collection/CollectionSubRegionSelector";
import { CollectionProgress } from "@/components/collection/CollectionProgress";
import { CollectionCard } from "@/components/collection/CollectionCard";
import { AttractionBottomSheet } from "@/components/attraction/AttractionBottomSheet";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { Skeleton } from "@/components/common/Skeleton";
import { AppIcon } from "@/constants/icons";
import { useCollectionSummary } from "@/hooks/useCollectionSummary";
import { useCollectionList } from "@/hooks/useCollectionList";
import styles from "./CollectionPage.module.css";

const SCROLL_STORAGE_KEY = "danyeogam:collection-scroll";

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
 * 요청서 P0-2 — 선택한 시·도/시·군·구는 useState가 아니라 URL 쿼리 파라미터
 * (?region=...&subRegion=...)로 관리한다. 관광지 상세로 이동했다가 뒤로가면 React
 * Router가 이전 URL(쿼리 포함)로 정확히 복원해 주므로, 이 페이지가 언마운트/재마운트
 * 되어도 선택 상태가 초기화되지 않는다 — 브라우저 자체 뒤로가기에서도 동일하게 동작.
 * 스크롤 위치도 sessionStorage에 저장해두고 목록이 뜬 뒤 복원한다.
 *
 * 칭호(Title)는 별도 탭(/titles, TitlePage)으로 분리했다 — 원래 이 화면 맨 아래
 * TitleSection으로 붙어 있었는데, 도감 카드 전체를 스크롤해야 보인다는 실제 테스트
 * 피드백을 받아 하단 탭 3번째로 옮겼다.
 */
export function CollectionPage() {
  const summary = useCollectionSummary();
  const [searchParams, setSearchParams] = useSearchParams();
  const preferredRegionCode = searchParams.get("region");
  const preferredSubRegionCode = searchParams.get("subRegion");

  const setPreferredRegionCode = useCallback(
    (code: string) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          next.set("region", code);
          // 광역이 바뀌면 이전 광역의 시군구 선택은 더 이상 유효하지 않다.
          next.delete("subRegion");
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  const setPreferredSubRegionCode = useCallback(
    (code: string | null) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (code) next.set("subRegion", code);
          else next.delete("subRegion");
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  // 요청서 4단계 — 도감 카드 클릭 시 지도 마커 클릭과 같은 관광지 Bottom Sheet를 재사용.
  // 이건 실제 페이지 이동이 아니라 이 화면 위에 뜨는 오버레이라 URL에 남길 필요는 없다.
  const [selectedSpotId, setSelectedSpotId] = useState<number | null>(null);

  // 사용자가 고른 지역이 현재 요약에 있으면 유지하고, 최초 진입 또는 목록 변경 시에는
  // API가 반환한 첫 지역을 렌더링 단계에서 기본값으로 사용한다.
  const selectedRegionCode = summary.regions.some(
    (region) => region.code === preferredRegionCode,
  )
    ? preferredRegionCode
    : (summary.regions[0]?.code ?? null);

  const subSummary = useCollectionSummary(selectedRegionCode);
  // 백엔드 확인: 일부 시군구는 하위 구와 별도의 상위 코드로 DB에 존재하면서 실제 연결된
  // 스탬프 대상은 0건인 경우가 있다(예: "전주시" 자체는 0건, "전주시 덕진구"/"완산구"에
  // 실제 데이터가 있음 — 전국 18개 지역이 이런 케이스). 골라도 빈 화면만 나오므로
  // totalCount가 0인 항목은 선택지에서 제외한다. DB 자체를 건드리는 게 아니라
  // 화면에 표시할 가치가 없는 항목만 프론트에서 숨기는 것 — 백엔드 권장 방식 그대로.
  const visibleSubRegions = subSummary.regions.filter(
    (region) => region.totalCount > 0,
  );
  // 백엔드 문서: "활성 하위 시군구가 없는 광역자치단체는 광역 자체를 단일 항목으로 반환" —
  // 그런 경우 굳이 2단계 선택기를 보여줄 필요가 없어서 항목이 2개 이상일 때만 노출한다.
  const hasSubRegions =
    subSummary.status === "success" && visibleSubRegions.length > 1;
  const selectedSubRegionCode =
    hasSubRegions &&
    visibleSubRegions.some((region) => region.code === preferredSubRegionCode)
      ? preferredSubRegionCode
      : null;

  const effectiveRegionCode = selectedSubRegionCode ?? selectedRegionCode;
  const list = useCollectionList(effectiveRegionCode);
  const effectiveSummary = selectedSubRegionCode
    ? subSummary.regions.find((region) => region.code === selectedSubRegionCode)
    : summary.regions.find((region) => region.code === selectedRegionCode);

  // 요청서 P0-2 — 목록이 실제로 뜬 뒤 스크롤 위치를 복원하고, 화면을 떠날 때 저장한다.
  const scrollRestored = useRef(false);
  useEffect(() => {
    if (list.status !== "success" || scrollRestored.current) return;
    scrollRestored.current = true;
    const main = document.querySelector("main");
    const saved = sessionStorage.getItem(SCROLL_STORAGE_KEY);
    if (main && saved) {
      main.scrollTop = Number(saved);
    }
  }, [list.status]);

  useEffect(() => {
    const main = document.querySelector("main");
    if (!main) return;
    return () => {
      sessionStorage.setItem(SCROLL_STORAGE_KEY, String(main.scrollTop));
    };
  }, []);

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
          <div className={styles.stickyHeader}>
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
                regions={hasSubRegions ? visibleSubRegions : []}
                selectedCode={selectedSubRegionCode}
                onChange={setPreferredSubRegionCode}
                disabled={!hasSubRegions}
              />
            </div>

            {effectiveSummary && <CollectionProgress summary={effectiveSummary} />}
          </div>

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
                <CollectionCard
                  key={item.touristSpotId}
                  item={item}
                  onSelect={setSelectedSpotId}
                />
              ))}
            </div>
          )}
        </>
      )}

      <AttractionBottomSheet
        spotId={selectedSpotId}
        onClose={() => setSelectedSpotId(null)}
      />
    </div>
  );
}

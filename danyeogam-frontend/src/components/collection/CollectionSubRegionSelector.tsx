import { AppIcon } from "@/constants/icons";
import type { CollectionRegionSummary } from "@/types/api";
import styles from "./CollectionSubRegionSelector.module.css";

interface CollectionSubRegionSelectorProps {
  regions: CollectionRegionSummary[];
  /** null이면 "전체"(광역 단위) 선택 상태 */
  selectedCode: string | null;
  onChange: (code: string | null) => void;
  /** 하위 시군구가 없거나(세종특별자치시 등) 아직 불러오는 중일 때 — 자리는 유지하되 비활성화 */
  disabled?: boolean;
}

/**
 * 광역 지역 하나를 고른 뒤 그 안의 시군구 단위로 더 좁혀볼 수 있는 2단계 선택기.
 * "전체"를 고르면 다시 광역 단위 진행률/목록으로 돌아간다.
 *
 * 하위 시군구가 없는 광역(세종특별자치시 등)으로 바꿔도 이 선택기 자체가 화면에서
 * 나타났다 사라졌다 하지 않도록, 호출부(CollectionPage)에서 이 컴포넌트를 계속 렌더링한
 * 채로 disabled만 토글한다 — 레이아웃이 흔들리는 걸 막기 위함.
 */
export function CollectionSubRegionSelector({
  regions,
  selectedCode,
  onChange,
  disabled = false,
}: CollectionSubRegionSelectorProps) {
  return (
    <div className={styles.wrapper}>
      <select
        className={styles.select}
        value={selectedCode ?? ""}
        onChange={(event) => onChange(event.target.value || null)}
        disabled={disabled}
        aria-label="시·군·구 선택"
      >
        <option value="">전체</option>
        {regions.map((region) => (
          <option key={region.code} value={region.code}>
            {region.name}
          </option>
        ))}
      </select>
      <AppIcon.chevronDown
        className={styles.chevron}
        size={16}
        strokeWidth={2}
        aria-hidden="true"
      />
    </div>
  );
}

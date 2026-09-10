import { AppIcon } from "@/constants/icons";
import type { CollectionRegionSummary } from "@/types/api";
import styles from "./CollectionSubRegionSelector.module.css";

interface CollectionSubRegionSelectorProps {
  regions: CollectionRegionSummary[];
  /** null이면 "전체"(광역 단위) 선택 상태 */
  selectedCode: string | null;
  onChange: (code: string | null) => void;
}

/**
 * 광역 지역 하나를 고른 뒤 그 안의 시군구 단위로 더 좁혀볼 수 있는 2단계 선택기.
 * "전체"를 고르면 다시 광역 단위 진행률/목록으로 돌아간다.
 */
export function CollectionSubRegionSelector({
  regions,
  selectedCode,
  onChange,
}: CollectionSubRegionSelectorProps) {
  return (
    <div className={styles.wrapper}>
      <select
        className={styles.select}
        value={selectedCode ?? ""}
        onChange={(event) => onChange(event.target.value || null)}
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

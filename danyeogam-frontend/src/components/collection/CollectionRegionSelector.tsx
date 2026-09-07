import type { CollectionRegionSummary } from "@/types/api";
import styles from "./CollectionRegionSelector.module.css";

interface CollectionRegionSelectorProps {
  regions: CollectionRegionSummary[];
  selectedCode: string;
  onChange: (code: string) => void;
}

/** 디자인 시스템 9장/21장: 도감 화면 상단의 지역 선택 드롭다운. */
export function CollectionRegionSelector({
  regions,
  selectedCode,
  onChange,
}: CollectionRegionSelectorProps) {
  return (
    <select
      className={styles.select}
      value={selectedCode}
      onChange={(event) => onChange(event.target.value)}
      aria-label="지역 선택"
    >
      {regions.map((region) => (
        <option key={region.code} value={region.code}>
          {region.name}
        </option>
      ))}
    </select>
  );
}

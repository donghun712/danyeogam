import { AppIcon } from "@/constants/icons";
import type { CollectionRegionSummary } from "@/types/api";
import styles from "./CollectionRegionSelector.module.css";

interface CollectionRegionSelectorProps {
  regions: CollectionRegionSummary[];
  selectedCode: string;
  onChange: (code: string) => void;
}

/**
 * 디자인 시스템 9장/21장: 도감 화면 상단의 지역 선택 드롭다운.
 * 실제 동작은 표준 <select>를 그대로 쓰고(접근성·키보드 조작 유지), 화면시안의 알약형
 * 드롭다운처럼 보이도록 감싸는 el과 커스텀 화살표 아이콘만 얹었다.
 */
export function CollectionRegionSelector({
  regions,
  selectedCode,
  onChange,
}: CollectionRegionSelectorProps) {
  return (
    <div className={styles.wrapper}>
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
      <AppIcon.chevronDown
        className={styles.chevron}
        size={18}
        strokeWidth={2}
        aria-hidden="true"
      />
    </div>
  );
}

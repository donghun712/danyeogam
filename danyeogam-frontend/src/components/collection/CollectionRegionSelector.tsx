import { AppIcon } from "@/constants/icons";
import type { CollectionRegionSummary } from "@/types/api";
import styles from "./CollectionRegionSelector.module.css";

interface CollectionRegionSelectorProps {
  regions: CollectionRegionSummary[];
  selectedCode: string;
  onChange: (code: string) => void;
}

/**
 * "도"와 "광역시/특별시"를 묶어서 보여준다 — 실제 API가 내려주는 지역명(region.name)
 * 문자열 끝 글자만으로 구분한다(예: "강원특별자치도" → 도, "부산광역시" → 시).
 * 새 데이터를 추가하는 게 아니라 이미 있는 name 값을 그대로 분류에만 쓴다.
 */
function groupByRegionType(
  regions: CollectionRegionSummary[],
): { label: string; items: CollectionRegionSummary[] }[] {
  const provinces = regions.filter((region) => region.name.endsWith("도"));
  const cities = regions.filter((region) => !region.name.endsWith("도"));

  return [
    { label: "도", items: provinces },
    { label: "특별시·광역시", items: cities },
  ].filter((group) => group.items.length > 0);
}

/**
 * 디자인 시스템 9장/21장: 도감 화면 상단의 지역 선택 드롭다운.
 * 실제 동작은 표준 <select>를 그대로 쓰고(접근성·키보드 조작 유지), 화면시안의 알약형
 * 드롭다운처럼 보이도록 감싸는 el과 커스텀 화살표 아이콘만 얹었다.
 * <optgroup>으로 "도"/"특별시·광역시"를 구분해서 목록이 길어도 찾기 쉽게 한다 —
 * option의 value/개수는 그대로라 실제 선택 동작에는 영향이 없다.
 */
export function CollectionRegionSelector({
  regions,
  selectedCode,
  onChange,
}: CollectionRegionSelectorProps) {
  const groups = groupByRegionType(regions);

  return (
    <div className={styles.wrapper}>
      <select
        className={styles.select}
        value={selectedCode}
        onChange={(event) => onChange(event.target.value)}
        aria-label="지역 선택"
      >
        {groups.map((group) => (
          <optgroup key={group.label} label={group.label}>
            {group.items.map((region) => (
              <option key={region.code} value={region.code}>
                {region.name}
              </option>
            ))}
          </optgroup>
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

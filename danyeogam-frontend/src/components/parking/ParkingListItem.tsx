import { Card } from "@/components/common/Card";
import { AppIcon } from "@/constants/icons";
import { NavigationButton } from "@/components/common/NavigationButton";
import type { ParkingItem } from "@/types/api";
import styles from "./ParkingListItem.module.css";

interface ParkingListItemProps {
  item: ParkingItem;
}

/**
 * 디자인 시스템 16장 · 백엔드 문서 8.5절:
 * publicVerified=false → "주변 주차장" / publicVerified=true → "주변 공영주차장"
 * 현재 데이터 소스(Kakao PK6)는 공영 여부를 보증하지 않아 항상 false로 내려오지만,
 * 분기 자체는 유지해서 true가 내려오는 날 코드 수정 없이 바로 반영되게 한다.
 * 요청서 7단계 — 거리를 우측 뱃지로 강조하고, 라벨/이름/주소를 한 덩어리로 묶어
 * 세로로 늘어지던 카드를 더 컴팩트하게 정리한다. API/parking 로직은 그대로.
 */
export function ParkingListItem({ item }: ParkingListItemProps) {
  const label = item.publicVerified ? "주변 공영주차장" : "주변 주차장";

  return (
    <Card className={styles.card}>
      <div className={styles.headerRow}>
        <div className={styles.titleGroup}>
          <span className={`text-caption ${styles.label}`}>{label}</span>
          <h3 className="text-h3">{item.name}</h3>
        </div>
        <span className={styles.distanceBadge}>
          <AppIcon.parking size={12} strokeWidth={2} aria-hidden="true" />
          {item.distanceMeters}m
        </span>
      </div>
      <p className={`text-caption ${styles.meta}`}>{item.address}</p>
      <NavigationButton navigation={item.navigation} />
    </Card>
  );
}

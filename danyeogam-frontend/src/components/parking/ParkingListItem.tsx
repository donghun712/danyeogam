import { Card } from "@/components/common/Card";
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
 */
export function ParkingListItem({ item }: ParkingListItemProps) {
  const label = item.publicVerified ? "주변 공영주차장" : "주변 주차장";

  return (
    <Card className={styles.card}>
      <span className="text-caption">{label}</span>
      <h3 className="text-h3">{item.name}</h3>
      <p className={`text-caption ${styles.meta}`}>{item.address}</p>
      <p className={`text-body ${styles.distance}`}>{item.distanceMeters}m</p>
      <NavigationButton navigation={item.navigation} />
    </Card>
  );
}

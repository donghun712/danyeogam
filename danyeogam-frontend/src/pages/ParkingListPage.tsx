import { useParams } from "react-router-dom";
import { FullPageLayout } from "@/components/common/FullPageLayout";
import { ParkingListItem } from "@/components/parking/ParkingListItem";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { Skeleton } from "@/components/common/Skeleton";
import { AppIcon } from "@/constants/icons";
import { useNearbyParking } from "@/hooks/useNearbyParking";
import styles from "./ParkingListPage.module.css";

/**
 * 화면시안 "주변 주차장 목록" — 관광지 상세의 "주차장 보기"에서 진입하는 풀페이지.
 * PARK-01: 관광지 상세와 독립적으로 실패할 수 있는 영역이라 이 화면 자체가 상세 화면을
 * 막지 않는다(이미 별도 라우트로 분리돼 있어 자연히 만족).
 */
export function ParkingListPage() {
  const { attractionId } = useParams<{ attractionId: string }>();
  const parsedId = attractionId ? Number(attractionId) : NaN;
  const spotId = Number.isFinite(parsedId) ? parsedId : null;
  const { status, items, retry } = useNearbyParking(spotId);

  return (
    <FullPageLayout title="주변 주차장">
      {status === "loading" && items.length === 0 && (
        <div className={styles.loading}>
          <Skeleton height="120px" radius="var(--radius-card)" />
          <Skeleton height="120px" radius="var(--radius-card)" />
          <Skeleton height="120px" radius="var(--radius-card)" />
        </div>
      )}

      {status === "empty" && (
        <EmptyState
          icon={AppIcon.parking}
          message={"주변에서 확인된 주차장이 없어요."}
        />
      )}

      {status === "unavailable" && items.length === 0 && (
        // 백엔드 문서 10.2절: 주차장 공급자 오류 문구를 그대로 사용
        <ErrorState message="주차장 정보를 잠시 불러오지 못했어요." onRetry={retry} />
      )}

      {status === "error" && items.length === 0 && (
        <ErrorState
          message="주차장 정보를 불러오지 못했습니다."
          onRetry={retry}
        />
      )}

      {items.length > 0 && (
        <div className={styles.list}>
          {items.map((item) => (
            <ParkingListItem key={item.id} item={item} />
          ))}
        </div>
      )}
    </FullPageLayout>
  );
}

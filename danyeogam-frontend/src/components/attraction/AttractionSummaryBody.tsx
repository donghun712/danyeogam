import type { ReactNode } from "react";
import { Badge } from "@/components/common/Badge";
import { FavoriteButton } from "@/components/common/FavoriteButton";
import { useNearbyParking } from "@/hooks/useNearbyParking";
import { safeExternalHttpUrl } from "@/utils/safeExternalUrl";
import type { TouristSpotDetail } from "@/types/api";
import styles from "./AttractionSummaryBody.module.css";

interface AttractionSummaryBodyProps {
  detail: TouristSpotDetail;
  actions?: ReactNode;
}

/**
 * 화면시안 "02 관광지 상세(바텀시트)" 전용 — 요약만 보여준다.
 * 운영시간/시설정보/전체 소개글 같은 전체 정보는 "03 관광지 상세 페이지"
 * (AttractionDetailBody)에만 있고 여기는 의도적으로 뺐다 — 시안에도 바텀시트엔 없는 정보다.
 */
export function AttractionSummaryBody({
  detail,
  actions,
}: AttractionSummaryBodyProps) {
  const imageUrl = safeExternalHttpUrl(detail.images[0]?.url);
  // 시안의 "가까운 공영주차장" 미리보기 한 줄 — 이미 있는 주차장 API를 limit=1로 재사용한다.
  const { items: nearbyParking, status: parkingStatus } = useNearbyParking(
    detail.id,
    1,
  );
  const nearestParking =
    parkingStatus === "success" ? (nearbyParking[0] ?? null) : null;

  return (
    <div className={styles.content}>
      {imageUrl ? (
        <img
          src={imageUrl}
          alt={detail.images[0].alt}
          className={styles.image}
        />
      ) : (
        <div className={styles.imageFallback} aria-hidden="true" />
      )}

      <div className={styles.badgeRow}>
        {detail.stampEnabled && <Badge tone="brand">스탬프 가능</Badge>}
        {detail.visitState === "VISITED" && (
          <Badge tone="success">✓ 방문 완료</Badge>
        )}
        <FavoriteButton spotId={detail.id} favorited={detail.favorited} showLabel />
      </div>

      <h2 className="text-h2">{detail.name}</h2>

      {detail.address.road && (
        <p className={`text-caption ${styles.address}`}>{detail.address.road}</p>
      )}

      {detail.overview && (
        <p className={`text-body ${styles.summary}`}>{detail.overview}</p>
      )}

      {nearestParking && (
        <div className={styles.parkingPreview}>
          <span className="text-caption">가까운 주차장</span>
          <span className="text-body">
            {nearestParking.name} {Math.round(nearestParking.distanceMeters)}m
          </span>
        </div>
      )}

      {actions && <div className={styles.actions}>{actions}</div>}
    </div>
  );
}

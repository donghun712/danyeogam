import type { ReactNode } from "react";
import { Badge } from "@/components/common/Badge";
import { FavoriteButton } from "@/components/common/FavoriteButton";
import { AppIcon } from "@/constants/icons";
import { useNearbyParking } from "@/hooks/useNearbyParking";
import { safeExternalHttpUrl } from "@/utils/safeExternalUrl";
import { getVisitBadgeKind } from "@/utils/visitStatus";
import { firstSentence } from "@/utils/text";
import type { TouristSpotDetail } from "@/types/api";
import styles from "./AttractionSummaryBody.module.css";

interface AttractionSummaryBodyProps {
  detail: TouristSpotDetail;
  /**
   * 길찾기/주차장 보기/상세 보기 같은 보조 행동만 여기 전달한다 — 스탬프 인증 같은
   * 주 행동(primary CTA)은 요청서 5.3에 따라 BottomSheet의 고정 footer로 별도 전달한다.
   */
  secondaryActions?: ReactNode;
}

/**
 * 화면시안 "02 관광지 상세(바텀시트)" 전용 — 요약만 보여준다.
 * 운영시간/시설정보/전체 소개글 같은 전체 정보는 "03 관광지 상세 페이지"
 * (AttractionDetailBody)에만 있고 여기는 의도적으로 뺐다 — 시안에도 바텀시트엔 없는 정보다.
 * 순서: 사진 → 제목·상태·주소 → 소개 요약 → 주차장 요약 → 보조 액션 (요청서 5.3).
 */
export function AttractionSummaryBody({
  detail,
  secondaryActions,
}: AttractionSummaryBodyProps) {
  const imageUrl = safeExternalHttpUrl(detail.images[0]?.url);
  const imageSourceUrl = safeExternalHttpUrl(detail.images[0]?.sourcePageUrl);
  const visitBadge = getVisitBadgeKind(detail);
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
      {imageUrl && detail.images[0]?.attribution && (
        <p className={`text-caption ${styles.imageAttribution}`}>
          {imageSourceUrl ? (
            <a href={imageSourceUrl} target="_blank" rel="noreferrer noopener">
              {detail.images[0].attribution}
            </a>
          ) : (
            detail.images[0].attribution
          )}
        </p>
      )}

      <div className={styles.badgeRow}>
        {visitBadge === "visited" && <Badge tone="success">✓ 방문 완료</Badge>}
        {visitBadge === "stampAvailable" && <Badge tone="brand">스탬프 가능</Badge>}
        <FavoriteButton spotId={detail.id} favorited={detail.favorited} showLabel />
      </div>

      <h2 className="text-h2">{detail.name}</h2>

      {detail.address.road && (
        <p className={`text-caption ${styles.address}`}>{detail.address.road}</p>
      )}

      {detail.overview && (
        // 요청서 2단계 — 여러 줄 line-clamp로 문장 중간에서 끊지 않고, 첫 번째
        // 완전한 문장만 보여준다(원문 그대로 자르기, 재작성 없음).
        <p className={`text-body ${styles.summary}`}>{firstSentence(detail.overview)}</p>
      )}

      {nearestParking && (
        // 요청서 6.2 — 강한 황토색 박스 대신 주변과 같은 표면 + 아이콘 + 구분선
        <div className={styles.parkingPreview}>
          <AppIcon.parking size={16} strokeWidth={2} className={styles.parkingIcon} aria-hidden="true" />
          <span className={`text-caption ${styles.parkingLabel}`}>가까운 주차장</span>
          <span className={`text-body ${styles.parkingName}`}>{nearestParking.name}</span>
          <span className="text-caption">{Math.round(nearestParking.distanceMeters)}m</span>
        </div>
      )}

      {secondaryActions && (
        <div className={styles.secondaryActions}>{secondaryActions}</div>
      )}
    </div>
  );
}

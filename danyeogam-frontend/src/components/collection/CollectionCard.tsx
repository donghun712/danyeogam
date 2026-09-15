import { Card } from "@/components/common/Card";
import { AppIcon } from "@/constants/icons";
import type { CollectionItem } from "@/types/api";
import { safeExternalHttpUrl } from "@/utils/safeExternalUrl";
import styles from "./CollectionCard.module.css";

interface CollectionCardProps {
  item: CollectionItem;
  /**
   * 요청서 4단계 — 도감 카드를 눌렀을 때 지도 마커 클릭과 동일한 관광지 정보 흐름
   * (AttractionBottomSheet)을 재사용한다. 새 상세 UI를 만들지 않는다.
   */
  onSelect: (touristSpotId: number) => void;
}

/**
 * 디자인 시스템 22장 Collection Card.
 * visitState는 서버 값을 그대로 사용한다 — "VISITED"가 아니면 전부 미방문으로 취급하고
 * 프론트에서 별도로 방문 여부를 추측하지 않는다.
 * 요청서 P1-6 — 방문완료 카드는 사진이 주인공. 얇은 프레임(collection-card-visited-thin.png)
 * 위에 사진을 꽉 채우고(object-fit: cover), 방문완료 상태는 작은 체크 배지로만 보조한다
 * (기존 두꺼운 액자 구조 폐기). 미방문은 기존 수묵 placeholder 그대로 유지, 두 상태 모두
 * 동일한 카드 shell(같은 aspect-ratio, 같은 클리핑)을 쓴다.
 */
export function CollectionCard({ item, onSelect }: CollectionCardProps) {
  const visited = item.visitState === "VISITED";
  const thumbnailUrl = safeExternalHttpUrl(item.thumbnailUrl);

  return (
    <button
      type="button"
      className={styles.button}
      onClick={() => onSelect(item.touristSpotId)}
    >
      <Card padded={false} className={styles.card}>
        <div
          className={visited ? styles.frameVisited : styles.frameUnvisited}
        >
          {visited && thumbnailUrl ? (
            <>
              <img src={thumbnailUrl} alt={item.name} className={styles.image} />
              <span className={styles.visitedBadge} aria-hidden="true">
                <AppIcon.check size={12} strokeWidth={3} />
              </span>
            </>
          ) : (
            !visited && (
              <span className={styles.placeholderMark} aria-hidden="true">
                ?
              </span>
            )
          )}
        </div>
        <div className={styles.footer}>
          <p className="text-caption">{item.name}</p>
          {visited ? (
            <p className={`text-caption ${styles.visited}`}>✓ 방문완료</p>
          ) : (
            <p className={`text-caption ${styles.unvisitedLabel}`}>아직 미방문</p>
          )}
        </div>
      </Card>
    </button>
  );
}

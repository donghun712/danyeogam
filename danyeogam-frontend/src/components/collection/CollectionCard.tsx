import { Card } from "@/components/common/Card";
import type { CollectionItem } from "@/types/api";
import { safeExternalHttpUrl } from "@/utils/safeExternalUrl";
import styles from "./CollectionCard.module.css";

interface CollectionCardProps {
  item: CollectionItem;
}

/**
 * 디자인 시스템 22장 Collection Card.
 * visitState는 서버 값을 그대로 사용한다 — "VISITED"가 아니면 전부 미방문으로 취급하고
 * 프론트에서 별도로 방문 여부를 추측하지 않는다.
 */
export function CollectionCard({ item }: CollectionCardProps) {
  const visited = item.visitState === "VISITED";
  const thumbnailUrl = safeExternalHttpUrl(item.thumbnailUrl);

  return (
    <Card padded={false} className={styles.card}>
      <div
        className={visited ? styles.frameVisited : styles.frameUnvisited}
      >
        {visited && thumbnailUrl ? (
          <img src={thumbnailUrl} alt={item.name} className={styles.image} />
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
  );
}

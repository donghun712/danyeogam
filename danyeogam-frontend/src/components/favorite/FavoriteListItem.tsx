import { useNavigate } from "react-router-dom";
import { Card } from "@/components/common/Card";
import { safeExternalHttpUrl } from "@/utils/safeExternalUrl";
import { ROUTES } from "@/constants/routes";
import { ImagePlaceholder } from "@/components/attraction/ImagePlaceholder";
import type { FavoriteItem } from "@/types/api";
import styles from "./FavoriteListItem.module.css";

interface FavoriteListItemProps {
  item: FavoriteItem;
}

/**
 * 요청서 P1-9 — 즐겨찾기는 대규모 재설계 없이 이미지/텍스트 계층만 점검.
 * 이미지 없을 때의 빈 박스를 다른 화면과 같은 ImagePlaceholder로 통일했다
 * (요청서 10 — 이미지 URL 없음/로드 실패 모두 같은 영역 크기 유지, fake 이미지 금지).
 */
export function FavoriteListItem({ item }: FavoriteListItemProps) {
  const navigate = useNavigate();
  const thumbnailUrl = safeExternalHttpUrl(item.thumbnailUrl);
  const visited = item.visitState === "VISITED";

  return (
    <button
      type="button"
      className={styles.button}
      onClick={() => navigate(ROUTES.attractionDetail(item.touristSpotId))}
    >
      <Card padded={false} className={styles.card}>
        {thumbnailUrl ? (
          <img src={thumbnailUrl} alt={item.name} className={styles.image} />
        ) : (
          <div className={styles.imageFallback}>
            <ImagePlaceholder compact />
          </div>
        )}
        <div className={styles.body}>
          <p className="text-body">{item.name}</p>
          {visited ? (
            <p className={`text-caption ${styles.visited}`}>✓ 방문완료</p>
          ) : (
            <p className={`text-caption ${styles.unvisited}`}>아직 미방문</p>
          )}
        </div>
      </Card>
    </button>
  );
}

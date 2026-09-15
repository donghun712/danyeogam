import { useNavigate } from "react-router-dom";
import { Card } from "@/components/common/Card";
import { safeExternalHttpUrl } from "@/utils/safeExternalUrl";
import { ROUTES } from "@/constants/routes";
import type { FavoriteItem } from "@/types/api";
import styles from "./FavoriteListItem.module.css";

interface FavoriteListItemProps {
  item: FavoriteItem;
}

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
          <div className={styles.imageFallback} aria-hidden="true" />
        )}
        <div className={styles.body}>
          <p className="text-body">{item.name}</p>
          {visited && (
            <p className={`text-caption ${styles.visited}`}>✓ 방문완료</p>
          )}
        </div>
      </Card>
    </button>
  );
}

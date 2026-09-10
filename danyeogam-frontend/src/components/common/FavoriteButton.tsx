import { AppIcon } from "@/constants/icons";
import { useFavorite } from "@/hooks/useFavorite";
import styles from "./FavoriteButton.module.css";

interface FavoriteButtonProps {
  spotId: number;
  favorited: boolean;
  /** 바텀시트처럼 아이콘 옆에 글자도 보여주고 싶을 때. 상세 페이지 헤더에서는 안 쓴다(아이콘만). */
  showLabel?: boolean;
}

/** 좋아요+즐겨찾기를 하나로 합친 토글 버튼(백엔드 결정 사항, docs/frontend-favorites-api.md 참고). */
export function FavoriteButton({
  spotId,
  favorited: initialFavorited,
  showLabel = false,
}: FavoriteButtonProps) {
  const { favorited, isPending, toggle } = useFavorite(spotId, initialFavorited);

  return (
    <button
      type="button"
      className={`${styles.button} ${showLabel ? styles.withLabel : ""} ${favorited ? styles.active : ""}`}
      onClick={toggle}
      disabled={isPending}
      aria-pressed={favorited}
      aria-label={favorited ? "즐겨찾기 해제" : "즐겨찾기 추가"}
    >
      <AppIcon.favorite
        size={20}
        strokeWidth={2}
        fill={favorited ? "currentColor" : "none"}
      />
      {showLabel && (
        <span className="text-caption">{favorited ? "즐겨찾기 됨" : "즐겨찾기"}</span>
      )}
    </button>
  );
}

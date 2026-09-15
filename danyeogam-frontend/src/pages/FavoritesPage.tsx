import { FullPageLayout } from "@/components/common/FullPageLayout";
import { FavoriteListItem } from "@/components/favorite/FavoriteListItem";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { Skeleton } from "@/components/common/Skeleton";
import { AppIcon } from "@/constants/icons";
import { useFavorites } from "@/hooks/useFavorites";
import styles from "./FavoritesPage.module.css";

/**
 * 요청서 2단계 — 즐겨찾기 재탐색 최소 경로. 기존 GET /me/favorites 계약을 그대로 쓴다.
 */
export function FavoritesPage() {
  const { status, items, errorCode, retry } = useFavorites();

  return (
    <FullPageLayout title="즐겨찾기">
      {status === "loading" && items.length === 0 && (
        <div className={styles.loading}>
          <Skeleton height="80px" radius="var(--radius-card)" />
          <Skeleton height="80px" radius="var(--radius-card)" />
          <Skeleton height="80px" radius="var(--radius-card)" />
        </div>
      )}

      {status === "empty" && (
        <EmptyState
          icon={AppIcon.favorite}
          message={"아직 즐겨찾기한 곳이 없어요.\n관광지 상세에서 별 아이콘을 눌러보세요."}
        />
      )}

      {status === "error" && items.length === 0 && (
        <ErrorState
          message={
            errorCode === "AUTHENTICATION_REQUIRED"
              ? "로그인 세션이 필요해요."
              : "즐겨찾기 목록을 불러오지 못했습니다."
          }
          onRetry={retry}
        />
      )}

      {items.length > 0 && (
        <div className={styles.list}>
          {items.map((item) => (
            <FavoriteListItem key={item.touristSpotId} item={item} />
          ))}
        </div>
      )}
    </FullPageLayout>
  );
}

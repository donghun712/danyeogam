import { useNavigate } from "react-router-dom";
import { AppIcon } from "@/constants/icons";
import { FavoriteButton } from "@/components/common/FavoriteButton";
import { ShareButton } from "@/components/common/ShareButton";
import { ImagePlaceholder } from "./ImagePlaceholder";
import type { SpotImage } from "@/types/api";
import { safeExternalHttpUrl } from "@/utils/safeExternalUrl";
import styles from "./AttractionHero.module.css";

interface AttractionHeroProps {
  name: string;
  image: SpotImage | undefined;
  spotId: number;
  favorited: boolean;
}

/**
 * 요청서 P0-1 — 상세 페이지 상단만 예외적으로 "사진 몰입형 Hero" 구조를 쓴다
 * (제안서 2번 시안). 나머지 본문(AttractionDetailBody)은 1번 시안 그대로 유지.
 * 대표 사진이 화면 최상단부터 시작하고, 그 위에 뒤로가기(좌)·즐겨찾기/공유(우)를
 * 반투명 원형 배지로 오버레이한다 — 관광지명/주소/소개는 이 Hero 아래 본문에서
 * 시작한다(여기서는 그리지 않음).
 */
export function AttractionHero({ name, image, spotId, favorited }: AttractionHeroProps) {
  const navigate = useNavigate();
  const imageUrl = safeExternalHttpUrl(image?.url);
  const imageSourceUrl = safeExternalHttpUrl(image?.sourcePageUrl);

  return (
    <div className={styles.hero}>
      {imageUrl ? (
        <img src={imageUrl} alt={image?.alt ?? name} className={styles.image} />
      ) : (
        <div className={styles.imageFallback}>
          <ImagePlaceholder />
        </div>
      )}

      <div className={styles.overlay}>
        <button
          type="button"
          className={styles.backButton}
          onClick={() => navigate(-1)}
          aria-label="뒤로가기"
        >
          <AppIcon.back size={20} strokeWidth={2} />
        </button>
        <div className={styles.rightIcons}>
          <FavoriteButton spotId={spotId} favorited={favorited} overlay />
          <ShareButton title={name} overlay />
        </div>
      </div>

      {imageUrl && image?.attribution && (
        <p className={styles.attribution}>
          {imageSourceUrl ? (
            <a href={imageSourceUrl} target="_blank" rel="noreferrer noopener">
              {image.attribution}
            </a>
          ) : (
            image.attribution
          )}
        </p>
      )}
    </div>
  );
}

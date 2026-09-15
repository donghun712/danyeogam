import { AppIcon } from "@/constants/icons";
import styles from "./ImagePlaceholder.module.css";

interface ImagePlaceholderProps {
  /** 즐겨찾기 목록의 64px 썸네일처럼 작은 자리에서 쓸 때 — 아이콘만, 문구 없이 */
  compact?: boolean;
}

/**
 * 요청서 — 이미지가 없는 관광지를 열었을 때 빈 베이지 박스만 보이면
 * 로딩 실패처럼 보인다는 지적. 기존 한지 질감 에셋(hanji-texture.png)을 옅게
 * 깔고, 의도된 상태임을 명확히 알 수 있게 아이콘 + "이미지 없음" 문구를 더한다.
 * 새 에셋을 만들지 않고 기존 hanji-texture.png를 재사용한다.
 */
export function ImagePlaceholder({ compact = false }: ImagePlaceholderProps) {
  return (
    <div className={styles.placeholder} aria-hidden="true">
      <AppIcon.noImage size={compact ? 18 : 28} strokeWidth={1.5} className={styles.icon} />
      {!compact && <span className="text-caption">이미지 없음</span>}
    </div>
  );
}

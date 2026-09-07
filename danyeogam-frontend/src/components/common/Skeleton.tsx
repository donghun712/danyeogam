import styles from "./Skeleton.module.css";

interface SkeletonProps {
  width?: string;
  height?: string;
  radius?: string;
}

/**
 * 29장: 지도는 지도 자체를 먼저 보여주고 마커만 로딩, 상세는 Bottom Sheet가 먼저
 * 등장하고 이미지/텍스트가 Skeleton으로 채워지는 방식을 쓴다.
 * 이 컴포넌트는 그 Skeleton 블록 하나를 그린다.
 */
export function Skeleton({
  width = "100%",
  height = "16px",
  radius = "var(--radius-input)",
}: SkeletonProps) {
  return (
    <div
      className={styles.skeleton}
      style={{ width, height, borderRadius: radius }}
      aria-hidden="true"
    />
  );
}

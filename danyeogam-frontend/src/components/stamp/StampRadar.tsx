import type { CSSProperties } from "react";
import { AppIcon } from "@/constants/icons";
import styles from "./StampRadar.module.css";

export type StampRadarTone = "active" | "warning" | "info";

interface StampRadarProps {
  tone: StampRadarTone;
  /** 서버가 실제로 응답한 거리(OUT_OF_RANGE)일 때만 전달한다 — 없으면 숫자를 표시하지 않는다. */
  distanceMeters?: number | null;
  /** 측정/인증이 진행 중일 때만 true — 바깥 원이 은은하게 확장되는 스캔 애니메이션을 켠다. */
  scanning?: boolean;
}

const TONE_VAR: Record<StampRadarTone, string> = {
  active: "var(--color-heritage-brown)",
  warning: "var(--color-warning)",
  info: "var(--color-info)",
};

/**
 * 화면시안 "04 스탬프 인증(GPS 확인)"의 원형 반경 그래픽.
 * 모든 상태(대기/정확도 부족/위치 오래됨/거리 확인)가 같은 손그림 질감 링
 * (public/gps-radar-ring.png)을 쓴다 — CSS mask-image로 이미지의 형태(질감)만
 * 마스크로 재사용하고 실제 색은 semantic 토큰(--radar-color)으로 칠한다. 그래서
 * 상태마다 그래픽이 달라 보이지 않고 색만 바뀐다.
 * 거리·정확도 판정은 서버 책임이라(STAMP-01) 여기서는 아무 값도 계산하지 않고,
 * distanceMeters가 실제로 주어졌을 때만 그 값을 그대로 보여준다.
 */
export function StampRadar({ tone, distanceMeters, scanning }: StampRadarProps) {
  const color = TONE_VAR[tone];
  const radarStyle = { "--radar-color": color } as CSSProperties;

  return (
    <div className={styles.wrapper} style={radarStyle}>
      {scanning && <div className={styles.pulse} aria-hidden="true" />}
      <div className={styles.ring}>
        <div className={styles.ringTexture} aria-hidden="true" />
        <div className={styles.content}>
          {distanceMeters != null ? (
            <span className={styles.distance}>{Math.round(distanceMeters)}m</span>
          ) : (
            <AppIcon.location size={40} strokeWidth={2} aria-hidden="true" />
          )}
        </div>
      </div>
    </div>
  );
}

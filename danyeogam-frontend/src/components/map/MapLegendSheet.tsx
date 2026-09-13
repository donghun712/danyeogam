import { BottomSheet } from "@/components/common/BottomSheet";
import styles from "./MapLegendSheet.module.css";

interface MapLegendSheetProps {
  open: boolean;
  onClose: () => void;
}

/**
 * "지도상태" — 지도에 이미 쓰이고 있는 마커/클러스터 색상과 모양이 각각 뭘 뜻하는지
 * 설명하는 순수 안내 UI. 새 마커 종류나 의미를 만들지 않고, 실제
 * attractionMarkerIcons.ts에 있는 값(Heritage Brown 핀, Sage 핀+체크마크, 테두리 원,
 * Info색 내 위치 점, Heritage Brown 원형 클러스터 배지)을 그대로 재현한다.
 * 지도/마커 동작 자체는 전혀 건드리지 않는다 — 그리기만 하는 화면.
 */
export function MapLegendSheet({ open, onClose }: MapLegendSheetProps) {
  return (
    <BottomSheet open={open} onClose={onClose}>
      <div className={styles.content}>
        <p className="text-h3">지도 마커 안내</p>

        <div className={styles.row}>
          <svg width="28" height="36" viewBox="0 0 28 36" aria-hidden="true">
            <path
              d="M14 0C6.268 0 0 6.268 0 14c0 10.5 14 22 14 22s14-11.5 14-22C28 6.268 21.732 0 14 0z"
              fill="var(--color-heritage-brown)"
            />
            <circle cx="14" cy="14" r="5.5" fill="var(--color-hanji-ivory)" />
          </svg>
          <span className="text-body">스탬프 대상 관광지 (미방문)</span>
        </div>

        <div className={styles.row}>
          <svg width="28" height="36" viewBox="0 0 28 36" aria-hidden="true">
            <path
              d="M14 0C6.268 0 0 6.268 0 14c0 10.5 14 22 14 22s14-11.5 14-22C28 6.268 21.732 0 14 0z"
              fill="var(--color-success)"
            />
            <circle cx="14" cy="14" r="5.5" fill="var(--color-hanji-ivory)" />
            <path
              d="M11 14.2 L13.3 16.5 L17.3 11.2"
              fill="none"
              stroke="var(--color-success)"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          <span className="text-body">스탬프 대상 관광지 (방문 완료)</span>
        </div>

        <div className={styles.row}>
          <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
            <circle
              cx="9"
              cy="9"
              r="7"
              fill="var(--color-hanji-ivory)"
              stroke="var(--color-disabled)"
              strokeWidth="2"
            />
          </svg>
          <span className="text-body">일반 관광지 (스탬프 대상 아님)</span>
        </div>

        <div className={styles.row}>
          <svg width="22" height="22" viewBox="0 0 22 22" aria-hidden="true">
            <circle cx="11" cy="11" r="9" fill="var(--color-info)" fillOpacity="0.25" />
            <circle cx="11" cy="11" r="5" fill="var(--color-info)" stroke="#fff" strokeWidth="2" />
          </svg>
          <span className="text-body">현재 내 위치</span>
        </div>

        <div className={styles.row}>
          <div className={styles.clusterSample} aria-hidden="true">
            12
          </div>
          <span className="text-body">클러스터 — 숫자는 그 지점에 모여 있는 관광지 개수</span>
        </div>
      </div>
    </BottomSheet>
  );
}

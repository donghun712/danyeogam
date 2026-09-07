import { formatRelativeTimeKo } from "@/utils/relativeTime";
import type { LastMeasurement } from "@/hooks/useStampVerification";
import styles from "./StampMeasurementInfo.module.css";

interface StampMeasurementInfoProps {
  measurement: LastMeasurement;
}

/**
 * 화면시안 "GPS 정확도 8m" / "위치 갱신 시간 방금 전" 줄.
 * 브라우저 Geolocation이 실제로 반환한 accuracy/timestamp만 표시하고, 서버 판정에는 관여하지 않는다.
 */
export function StampMeasurementInfo({ measurement }: StampMeasurementInfoProps) {
  return (
    <dl className={styles.list}>
      <div className={styles.row}>
        <dt className="text-caption">GPS 정확도</dt>
        <dd className="text-body">{Math.round(measurement.accuracyMeters)}m</dd>
      </div>
      <div className={styles.row}>
        <dt className="text-caption">위치 갱신 시간</dt>
        <dd className="text-body">{formatRelativeTimeKo(measurement.measuredAt)}</dd>
      </div>
    </dl>
  );
}

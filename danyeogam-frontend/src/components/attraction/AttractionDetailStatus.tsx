import { Skeleton } from "@/components/common/Skeleton";
import { ErrorState } from "@/components/common/ErrorState";
import styles from "./AttractionDetailStatus.module.css";

interface AttractionDetailStatusProps {
  status: "loading" | "error";
  errorCode: string | null;
  onRetry: () => void;
}

/**
 * TOUR-01/02 로딩·오류 상태. 바텀시트/풀페이지가 동일한 데이터·동일한 상태 처리를 쓰도록 공유한다.
 *
 * STEP 8: 상태명세서 4장은 "Error"와 "Not Found"를 별도 상태로 구분한다. 백엔드 문서
 * (frontend-read-api.md)도 404 TOURIST_SPOT_NOT_FOUND에는 "상세 화면을 닫고 목록 갱신"을
 * 권장하며 재시도를 권하지 않는다 — 존재하지 않는 관광지는 다시 요청해도 똑같이 404이므로
 * 재시도 버튼을 보여주지 않는다.
 */
export function AttractionDetailStatus({
  status,
  errorCode,
  onRetry,
}: AttractionDetailStatusProps) {
  if (status === "loading") {
    return (
      <div className={styles.loading}>
        <Skeleton height="160px" radius="var(--radius-image)" />
        <Skeleton width="60%" height="22px" />
        <Skeleton width="40%" height="16px" />
      </div>
    );
  }

  if (errorCode === "TOURIST_SPOT_NOT_FOUND") {
    return <ErrorState message="관광지 정보를 찾을 수 없습니다." />;
  }

  return (
    <div>
      <ErrorState message="관광지 정보를 불러오지 못했습니다." onRetry={onRetry} />
      {errorCode && <p className="text-caption">{errorCode}</p>}
    </div>
  );
}

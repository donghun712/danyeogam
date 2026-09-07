import { useNavigate, useParams } from "react-router-dom";
import { FullPageLayout } from "@/components/common/FullPageLayout";
import { Button } from "@/components/common/Button";
import { StampSeal } from "@/components/stamp/StampSeal";
import { StampSealSparkles } from "@/components/stamp/StampSealSparkles";
import { StampRadar } from "@/components/stamp/StampRadar";
import { StampMeasurementInfo } from "@/components/stamp/StampMeasurementInfo";
import { useStampVerification } from "@/hooks/useStampVerification";
import { useAttractionDetail } from "@/hooks/useAttractionDetail";
import { ROUTES } from "@/constants/routes";
import styles from "./StampVerificationPage.module.css";

const GPS_ERROR_MESSAGE: Record<string, string> = {
  gps_permission_denied: "스탬프 인증을 위해 위치 권한이 필요해요.",
  gps_unavailable: "이 기기에서 위치를 확인할 수 없어요.",
  gps_timeout: "위치 확인 시간이 초과됐어요. 다시 시도해 주세요.",
};

const HTTP_ERROR_MESSAGE: Record<string, string> = {
  NETWORK_ERROR: "네트워크 연결을 확인해 주세요.",
  AUTHENTICATION_REQUIRED: "인증 세션이 만료됐어요. 다시 시도해 주세요.",
  ORIGIN_NOT_ALLOWED: "지금은 인증을 처리할 수 없어요.",
  TOURIST_SPOT_NOT_FOUND: "관광지 정보를 찾을 수 없어요.",
  IDEMPOTENCY_KEY_CONFLICT: "요청이 겹쳤어요. 다시 시도해 주세요.",
  TOO_MANY_REQUESTS: "요청이 너무 많아요. 잠시 후 다시 시도해 주세요.",
  VALIDATION_FAILED: "요청 처리 중 문제가 발생했어요.",
  INVALID_REQUEST: "요청 처리 중 문제가 발생했어요.",
  UNSUPPORTED_MEDIA_TYPE: "요청 처리 중 문제가 발생했어요.",
};

/**
 * 화면시안 "04 스탬프 인증(GPS 확인)" + "05 스탬프 획득" — 같은 화면 안에서 단계별로 전환된다.
 *
 * STAMP-01/02: 거리·정확도·위치 신선도 판정은 전적으로 서버가 하고, 프론트는 좌표/정확도/
 * 측정시각만 전달한다. 반경·정확도·유효시간 수치는 어디에도 하드코딩하지 않는다.
 */
export function StampVerificationPage() {
  const { attractionId } = useParams<{ attractionId: string }>();
  const navigate = useNavigate();
  const parsedId = attractionId ? Number(attractionId) : NaN;
  const spotId = Number.isFinite(parsedId) ? parsedId : null;

  const { phase, result, errorCode, isBusy, start, lastMeasurement } =
    useStampVerification(spotId ?? -1);
  // 인장에 넣을 관광지명만 필요해서 STAMP-01 응답과 별개로 상세를 조회한다(이름 필드가 없음).
  const { detail } = useAttractionDetail(spotId);

  if (spotId === null) {
    return (
      <FullPageLayout title="스탬프 인증">
        <p className="text-body">잘못된 접근이에요.</p>
      </FullPageLayout>
    );
  }

  return (
    <FullPageLayout title="스탬프 인증">
      <div className={styles.container}>
        {phase === "ready" && (
          <div className={styles.center}>
            <StampRadar tone="active" />
            <p className="text-body">
              관광지 현장에서 GPS 위치를 확인해 스탬프를 인증해요.
            </p>
            <Button onClick={start} disabled={isBusy}>
              스탬프 인증하기
            </Button>
          </div>
        )}

        {phase === "measuring" && (
          <div className={styles.center}>
            <StampRadar tone="active" scanning />
            <p className="text-body">현재 위치를 확인하는 중...</p>
          </div>
        )}

        {phase === "verifying" && (
          <div className={styles.center}>
            <StampRadar tone="active" scanning />
            <p className="text-body">인증하는 중...</p>
            {lastMeasurement && <StampMeasurementInfo measurement={lastMeasurement} />}
          </div>
        )}

        {(phase === "gps_permission_denied" ||
          phase === "gps_unavailable" ||
          phase === "gps_timeout") && (
          <div className={styles.center}>
            <p className="text-body">{GPS_ERROR_MESSAGE[phase]}</p>
            <Button variant="secondary" onClick={start} disabled={isBusy}>
              다시 시도하기
            </Button>
          </div>
        )}

        {phase === "http_error" && (
          <div className={styles.center}>
            <p className="text-body">
              {(errorCode && HTTP_ERROR_MESSAGE[errorCode]) ??
                "인증 중 오류가 발생했어요."}
            </p>
            {errorCode && <p className="text-caption">{errorCode}</p>}
            <Button variant="secondary" onClick={start} disabled={isBusy}>
              다시 시도하기
            </Button>
          </div>
        )}

        {phase === "result" && result?.status === "VERIFIED_NEW" && (
          <div className={styles.center}>
            <p className="text-h1">스탬프 획득!</p>
            {detail && (
              <StampSealSparkles>
                <StampSeal name={detail.name} />
              </StampSealSparkles>
            )}
            <p className="text-body">방문 인증을 완료했어요!</p>
            <div className={styles.actions}>
              <Button onClick={() => navigate(ROUTES.collection)}>
                도감에서 확인하기
              </Button>
              <Button variant="ghost" onClick={() => navigate(ROUTES.map)}>
                계속 탐색하기
              </Button>
            </div>
          </div>
        )}

        {phase === "result" && result?.status === "VERIFIED_ALREADY_ACQUIRED" && (
          <div className={styles.center}>
            <p className="text-h2">이미 방문한 관광지예요</p>
            <p className="text-body">이전에 스탬프를 획득했어요.</p>
            <Button variant="secondary" onClick={() => navigate(ROUTES.collection)}>
              도감에서 확인하기
            </Button>
          </div>
        )}

        {phase === "result" && result?.status === "OUT_OF_RANGE" && (
          <div className={styles.center}>
            <StampRadar tone="active" distanceMeters={result.distanceMeters} />
            <p className="text-body">
              {result.distanceMeters !== null
                ? `${detail ? `${detail.name}까지` : "관광지까지"} 남은 거리`
                : "관광지에서 조금 더 가까이 이동해 주세요."}
            </p>
            {lastMeasurement && <StampMeasurementInfo measurement={lastMeasurement} />}
            <p className="text-caption">정확한 인증을 위해 관광지 중앙으로 이동해주세요.</p>
            <Button onClick={start} disabled={isBusy}>
              다시 인증하기
            </Button>
          </div>
        )}

        {phase === "result" && result?.status === "GPS_ACCURACY_INSUFFICIENT" && (
          <div className={styles.center}>
            <StampRadar tone="warning" />
            <p className="text-body">GPS 정확도가 낮아요.</p>
            {lastMeasurement && <StampMeasurementInfo measurement={lastMeasurement} />}
            <p className="text-caption">야외로 이동한 뒤 다시 시도해 주세요.</p>
            <Button onClick={start} disabled={isBusy}>
              다시 측정하기
            </Button>
          </div>
        )}

        {phase === "result" && result?.status === "LOCATION_STALE" && (
          <div className={styles.center}>
            <StampRadar tone="info" />
            <p className="text-body">위치 정보가 오래됐어요.</p>
            {lastMeasurement && <StampMeasurementInfo measurement={lastMeasurement} />}
            <Button onClick={start} disabled={isBusy}>
              위치 새로고침
            </Button>
          </div>
        )}

        {phase === "result" && result?.status === "STAMP_DISABLED" && (
          <div className={styles.center}>
            <p className="text-body">지금은 이 관광지의 스탬프 인증을 이용할 수 없어요.</p>
            <Button
              variant="secondary"
              onClick={() => navigate(ROUTES.attractionDetail(spotId))}
            >
              관광지로 돌아가기
            </Button>
          </div>
        )}
      </div>
    </FullPageLayout>
  );
}

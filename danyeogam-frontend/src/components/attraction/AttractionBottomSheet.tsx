import { useNavigate } from "react-router-dom";
import { BottomSheet } from "@/components/common/BottomSheet";
import { Button } from "@/components/common/Button";
import { NavigationButton } from "@/components/common/NavigationButton";
import { useAttractionDetail } from "@/hooks/useAttractionDetail";
import { shouldShowStampCta } from "@/utils/visitStatus";
import { ROUTES } from "@/constants/routes";
import { AttractionSummaryBody } from "./AttractionSummaryBody";
import { AttractionDetailStatus } from "./AttractionDetailStatus";
import styles from "./AttractionBottomSheet.module.css";

interface AttractionBottomSheetProps {
  spotId: number | null;
  onClose: () => void;
}

/**
 * 화면시안 "02 관광지 상세(바텀시트)".
 * 요청서 5.3 — 순서는 사진→제목·상태·주소→소개 요약→주차장 요약→보조 액션→인증 CTA.
 * 보조 액션(상세 보기 텍스트 링크 + 길찾기/주차장 보기 2열)은 AttractionSummaryBody
 * 안에서 스크롤되고, 인증 CTA는 BottomSheet의 footer로 분리해 시트 하단에 고정한다.
 * 요청서 5.1 — 방문 완료가 확인되면 인증 CTA 자체를 숨긴다(비활성 버튼도 남기지 않음).
 * STEP 8: 이미 보여주고 있던 detail이 있으면(같은 관광지의 백그라운드 재조회/재시도) 그
 * 데이터를 계속 보여주고, detail이 아예 없을 때만 Loading/Error 상태 화면으로 전환한다.
 */
export function AttractionBottomSheet({
  spotId,
  onClose,
}: AttractionBottomSheetProps) {
  const navigate = useNavigate();
  const { status, detail, errorCode, retry } = useAttractionDetail(spotId);

  return (
    <BottomSheet
      open={spotId !== null}
      onClose={onClose}
      footer={
        detail && shouldShowStampCta(detail) ? (
          <Button
            fullWidth
            onClick={() => navigate(ROUTES.attractionStamp(detail.id))}
          >
            스탬프 인증하기
          </Button>
        ) : undefined
      }
    >
      {!detail && (status === "loading" || status === "error") && (
        <AttractionDetailStatus
          status={status}
          errorCode={errorCode}
          onRetry={retry}
        />
      )}

      {detail && (
        <AttractionSummaryBody
          detail={detail}
          secondaryActions={
            <>
              <Button
                variant="ghost"
                fullWidth
                onClick={() => navigate(ROUTES.attractionDetail(detail.id))}
              >
                상세 보기
              </Button>
              <div className={styles.secondaryRow}>
                <NavigationButton navigation={detail.navigation} />
                <Button
                  variant="secondary"
                  onClick={() => navigate(ROUTES.attractionParking(detail.id))}
                >
                  주차장 보기
                </Button>
              </div>
            </>
          }
        />
      )}
    </BottomSheet>
  );
}

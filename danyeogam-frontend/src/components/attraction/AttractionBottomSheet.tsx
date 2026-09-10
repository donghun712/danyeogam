import { useNavigate } from "react-router-dom";
import { BottomSheet } from "@/components/common/BottomSheet";
import { Button } from "@/components/common/Button";
import { NavigationButton } from "@/components/common/NavigationButton";
import { useAttractionDetail } from "@/hooks/useAttractionDetail";
import { ROUTES } from "@/constants/routes";
import { AttractionSummaryBody } from "./AttractionSummaryBody";
import { AttractionDetailStatus } from "./AttractionDetailStatus";

interface AttractionBottomSheetProps {
  spotId: number | null;
  onClose: () => void;
}

/**
 * 화면시안 "02 관광지 상세(바텀시트)".
 * 시안을 다시 확인해보니 바텀시트(02)와 상세 페이지(03)는 정보량이 다르다 — 바텀시트는
 * 요약(이미지/뱃지/이름/한 줄 소개/가까운 주차장), 상세 페이지가 전체 정보(운영시간/시설정보
 * 등)를 담당한다. 그래서 둘이 공유하던 AttractionDetailBody 대신 여기는
 * AttractionSummaryBody를 쓴다.
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
    <BottomSheet open={spotId !== null} onClose={onClose}>
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
          actions={
            <>
              {detail.stampEnabled && (
                <Button
                  fullWidth
                  onClick={() => navigate(ROUTES.attractionStamp(detail.id))}
                >
                  스탬프 인증하기
                </Button>
              )}
              <NavigationButton navigation={detail.navigation} />
              <Button
                variant="secondary"
                onClick={() => navigate(ROUTES.attractionParking(detail.id))}
              >
                주차장 보기
              </Button>
              <Button
                variant="secondary"
                onClick={() => navigate(ROUTES.attractionDetail(detail.id))}
              >
                상세 보기
              </Button>
            </>
          }
        />
      )}
    </BottomSheet>
  );
}

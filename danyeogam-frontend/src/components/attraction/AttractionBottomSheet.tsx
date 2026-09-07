import { useNavigate } from "react-router-dom";
import { BottomSheet } from "@/components/common/BottomSheet";
import { Button } from "@/components/common/Button";
import { NavigationButton } from "@/components/common/NavigationButton";
import { useAttractionDetail } from "@/hooks/useAttractionDetail";
import { ROUTES } from "@/constants/routes";
import { AttractionDetailBody } from "./AttractionDetailBody";
import { AttractionDetailStatus } from "./AttractionDetailStatus";

interface AttractionBottomSheetProps {
  spotId: number | null;
  onClose: () => void;
}

/**
 * 화면시안 "02 관광지 상세(바텀시트)".
 * STEP 4: "03 관광지 상세 페이지"(AttractionDetailPage)와 동일한 상세 데이터를
 * AttractionDetailBody로 공유해서 보여준다 — 두 화면의 정보 차이는 명세서에 정의가 없다.
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
        <AttractionDetailBody
          detail={detail}
          actions={
            <>
              {detail.stampEnabled && (
                <Button
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

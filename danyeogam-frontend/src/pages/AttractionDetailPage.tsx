import { useNavigate, useParams } from "react-router-dom";
import { FullPageLayout } from "@/components/common/FullPageLayout";
import { AttractionDetailBody } from "@/components/attraction/AttractionDetailBody";
import { AttractionDetailStatus } from "@/components/attraction/AttractionDetailStatus";
import { Button } from "@/components/common/Button";
import { NavigationButton } from "@/components/common/NavigationButton";
import { useAttractionDetail } from "@/hooks/useAttractionDetail";
import { ROUTES } from "@/constants/routes";

/**
 * 화면시안 "03 관광지 상세 페이지" — Bottom Sheet의 "상세 보기"에서 진입하는 풀페이지.
 *
 * STEP 4 결정: 바텀시트와 완전히 동일한 상세 데이터(AttractionDetailBody 공유)를 보여준다.
 * 액션은 "스탬프 인증하기"(stampEnabled일 때만), "길찾기", "주차장 보기"만 두고
 * ("상세 보기"는 이미 이 화면이라 의미가 없음).
 * STEP 8: 이미 보여주고 있던 detail이 있으면(같은 관광지의 백그라운드 재조회/재시도) 그
 * 데이터를 계속 보여주고, detail이 아예 없을 때만 Loading/Error 상태 화면으로 전환한다.
 */
export function AttractionDetailPage() {
  const { attractionId } = useParams<{ attractionId: string }>();
  const navigate = useNavigate();
  const parsedId = attractionId ? Number(attractionId) : NaN;
  const spotId = Number.isFinite(parsedId) ? parsedId : null;
  const { status, detail, errorCode, retry } = useAttractionDetail(spotId);

  return (
    <FullPageLayout title={detail?.name ?? "관광지 상세"}>
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
            </>
          }
        />
      )}
    </FullPageLayout>
  );
}

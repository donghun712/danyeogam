import { useNavigate, useParams } from "react-router-dom";
import { FullPageLayout } from "@/components/common/FullPageLayout";
import { AttractionDetailBody } from "@/components/attraction/AttractionDetailBody";
import { AttractionDetailStatus } from "@/components/attraction/AttractionDetailStatus";
import { ErrorState } from "@/components/common/ErrorState";
import { Button } from "@/components/common/Button";
import { NavigationButton } from "@/components/common/NavigationButton";
import { ShareButton } from "@/components/common/ShareButton";
import { FavoriteButton } from "@/components/common/FavoriteButton";
import { useAttractionDetail } from "@/hooks/useAttractionDetail";
import { shouldShowStampCta } from "@/utils/visitStatus";
import { ROUTES } from "@/constants/routes";

/**
 * 화면시안 "03 관광지 상세 페이지" — Bottom Sheet의 "상세 보기"에서 진입하는 풀페이지.
 *
 * 바텀시트(AttractionSummaryBody)와 다르게 여기는 시안 그대로 전체 정보
 * (AttractionDetailBody: 전체 소개글, 운영시간/휴무일, 시설정보 등)를 보여준다.
 * 요청서 5.1/5.3 — 보조 액션(길찾기/주차장 보기)을 먼저, 인증 CTA를 마지막에 두고,
 * 방문 완료가 확인되면 인증 CTA 자체를 숨긴다(shouldShowStampCta로 visitState까지 확인 —
 * stampEnabled만 보던 이전 로직은 방문 완료 후에도 버튼이 남는 버그가 있었다).
 * 헤더 우측엔 시안대로 즐겨찾기·공유 아이콘을 둔다.
 * STEP 8: 이미 보여주고 있던 detail이 있으면(같은 관광지의 백그라운드 재조회/재시도) 그
 * 데이터를 계속 보여주고, detail이 아예 없을 때만 Loading/Error 상태 화면으로 전환한다.
 */
export function AttractionDetailPage() {
  const { attractionId } = useParams<{ attractionId: string }>();
  const navigate = useNavigate();
  const parsedId = attractionId ? Number(attractionId) : NaN;
  const spotId = Number.isFinite(parsedId) ? parsedId : null;
  const { status, detail, errorCode, retry } = useAttractionDetail(spotId);

  // STEP 10 QA: attractionId가 숫자로 파싱되지 않으면 spotId가 null이 되고,
  // useAttractionDetail(null)은 요청 없이 idle 상태로만 머문다 — 방치하면 빈 화면이 되므로
  // StampVerificationPage와 동일한 패턴으로 여기서 명시적으로 오류 상태를 보여준다.
  if (spotId === null) {
    return (
      <FullPageLayout title="관광지 상세">
        <ErrorState message="잘못된 관광지 정보예요." />
      </FullPageLayout>
    );
  }

  return (
    <FullPageLayout
      title={detail?.name ?? "관광지 상세"}
      hideTitle={Boolean(detail)}
      headerActions={
        detail && (
          <>
            <FavoriteButton spotId={detail.id} favorited={detail.favorited} />
            <ShareButton title={detail.name} />
          </>
        )
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
        <AttractionDetailBody
          detail={detail}
          actions={
            <>
              <NavigationButton navigation={detail.navigation} />
              <Button
                variant="secondary"
                onClick={() => navigate(ROUTES.attractionParking(detail.id))}
              >
                주차장 보기
              </Button>
              {shouldShowStampCta(detail) && (
                <Button
                  fullWidth
                  onClick={() => navigate(ROUTES.attractionStamp(detail.id))}
                >
                  스탬프 인증하기
                </Button>
              )}
            </>
          }
        />
      )}
    </FullPageLayout>
  );
}

import type { ReactNode } from "react";
import { Badge } from "@/components/common/Badge";
import type { TouristSpotDetail } from "@/types/api";
import { safeExternalHttpUrl } from "@/utils/safeExternalUrl";
import styles from "./AttractionDetailBody.module.css";

interface AttractionDetailBodyProps {
  detail: TouristSpotDetail;
  /** 바텀시트/풀페이지마다 다른 액션 버튼 구성을 이 슬롯에 전달한다. */
  actions?: ReactNode;
}

/**
 * 화면시안 "02 관광지 상세(바텀시트)"와 "03 관광지 상세 페이지"가 공유하는 본문.
 * STEP 4 결정: 두 화면이 정확히 어떤 정보를 다르게 보여줘야 하는지 명세서에 정의가
 * 없어(개발 보고 D-1), 우선 완전히 동일한 상세 데이터를 보여준다. 액션 버튼 구성만
 * 화면별로 다르게 하고 싶어서 `actions` 슬롯으로 분리했다.
 */
export function AttractionDetailBody({
  detail,
  actions,
}: AttractionDetailBodyProps) {
  const imageUrl = safeExternalHttpUrl(detail.images[0]?.url);
  const homepageUrl = safeExternalHttpUrl(detail.homepageUrl);

  return (
    <div className={styles.content}>
      {imageUrl ? (
        <img
          src={imageUrl}
          alt={detail.images[0].alt}
          className={styles.image}
        />
      ) : (
        // 백엔드는 이미지가 없으면 빈 배열을 반환하므로(8.4절) 프론트가 기본 이미지를 대신 채운다.
        <div className={styles.imageFallback} aria-hidden="true" />
      )}

      <div className={styles.badgeRow}>
        {detail.stampEnabled && <Badge tone="brand">스탬프 가능</Badge>}
        {detail.visitState === "VISITED" && (
          <Badge tone="success">✓ 방문 완료</Badge>
        )}
      </div>

      <h2 className="text-h2">{detail.name}</h2>

      {detail.address.road && (
        <p className={`text-caption ${styles.address}`}>{detail.address.road}</p>
      )}

      {detail.dataQuality === "PARTIAL" && (
        // 디자인 시스템 15장 Partial 상태 문구
        <p className={`text-caption ${styles.partialNotice}`}>
          ⚠ 일부 관광정보를 불러오지 못했습니다.
        </p>
      )}

      {detail.overview && (
        <p className={`text-body ${styles.overview}`}>{detail.overview}</p>
      )}

      {(detail.telephone || homepageUrl) && (
        <dl className={styles.metaList}>
          {detail.telephone && (
            <div className={styles.metaRow}>
              <dt className="text-caption">전화</dt>
              <dd className="text-body">{detail.telephone}</dd>
            </div>
          )}
          {homepageUrl && (
            <div className={styles.metaRow}>
              <dt className="text-caption">홈페이지</dt>
              <dd className="text-body">
                <a
                  href={homepageUrl}
                  target="_blank"
                  rel="noreferrer noopener"
                  className={styles.homepageLink}
                >
                  {homepageUrl}
                </a>
              </dd>
            </div>
          )}
        </dl>
      )}

      {actions && <div className={styles.actions}>{actions}</div>}

      {/* TODO(STEP 6): 스탬프 인증 버튼 — GPS 측정 → POST /stamp-verifications 연동 */}
    </div>
  );
}

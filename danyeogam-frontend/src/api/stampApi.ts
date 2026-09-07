import { apiRequest } from "@/api/client";
import type {
  StampVerificationRequest,
  StampVerificationResult,
} from "@/types/api";

/**
 * GPS 스탬프 인증. 거리 판정은 전적으로 서버가 하며, 프론트는 좌표/정확도/측정시각만 전달한다.
 * idempotencyKey는 호출부(useStampVerification 훅)에서 요청마다 새 UUID를 생성해서 넘기고,
 * 같은 요청을 네트워크 재시도할 때만 동일한 값을 재사용한다.
 */
export function verifyStamp(
  request: StampVerificationRequest,
  idempotencyKey: string,
): Promise<StampVerificationResult> {
  return apiRequest<StampVerificationResult>("/stamp-verifications", {
    method: "POST",
    body: request,
    idempotencyKey,
  });
}

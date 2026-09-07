const target = new EventTarget();
const EVENT_NAME = "attraction-visited";

/**
 * GPS 인증 성공(VERIFIED_NEW/VERIFIED_ALREADY_ACQUIRED) 후 다른 화면(지도 마커, 상세 화면)의
 * visitState를 갱신하기 위한 최소한의 pub/sub. 전역 상태 관리 라이브러리를 새로 들이는 대신,
 * 이 프로젝트 규모에서는 이벤트 하나만 필요해서 EventTarget으로 충분하다고 판단했다.
 *
 * TODO(STEP 7): 도감 진행률(collectionChanged=true)도 이 이벤트를 구독해서 갱신할 수 있다.
 */
export function emitAttractionVisited(spotId: number): void {
  target.dispatchEvent(new CustomEvent<number>(EVENT_NAME, { detail: spotId }));
}

export function subscribeAttractionVisited(
  handler: (spotId: number) => void,
): () => void {
  const listener = (event: Event) => {
    handler((event as CustomEvent<number>).detail);
  };
  target.addEventListener(EVENT_NAME, listener);
  return () => target.removeEventListener(EVENT_NAME, listener);
}

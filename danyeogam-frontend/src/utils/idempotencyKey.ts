/**
 * 스탬프 인증 문서: "Idempotency-Key는 요청마다 새 UUID를 만들고, 네트워크 재시도에는
 * 같은 UUID를 사용한다." 여기서는 사용자가 "인증하기"를 다시 누를 때마다(=새 시도)
 * 새 키를 만든다. crypto.randomUUID를 우선 쓰고, 구형 WebView 등 지원하지 않는
 * 환경을 위한 최소 대체 구현을 둔다.
 */
export function createIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (char) => {
    const random = (Math.random() * 16) | 0;
    const value = char === "x" ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

/** JavaScript 키는 브라우저에 공개되므로 카카오 콘솔의 Web 도메인 제한이 보안 경계다. */
export const KAKAO_JS_KEY = (import.meta.env.VITE_KAKAO_JS_KEY ?? "").trim();
const validKakaoKey = /^[a-fA-F0-9]{32}$/.test(KAKAO_JS_KEY);

if (import.meta.env.PROD && !validKakaoKey) {
  throw new Error("배포용 VITE_KAKAO_JS_KEY가 없거나 형식이 올바르지 않습니다.");
}

if (import.meta.env.DEV && !validKakaoKey) {
  // eslint-disable-next-line no-console
  console.warn(
    "[env] VITE_KAKAO_JS_KEY가 없거나 형식이 올바르지 않습니다. .env.example을 참고해 주세요.",
  );
}

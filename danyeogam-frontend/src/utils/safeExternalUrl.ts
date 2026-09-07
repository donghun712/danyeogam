/** API 데이터에서 받은 외부 URL을 링크나 이미지에 넣기 전에 허용 스킴을 제한한다. */
export function safeExternalHttpUrl(value: string | null | undefined): string | null {
  if (!value) return null;

  try {
    const url = new URL(value);
    if (url.protocol === "http:" && url.hostname === "tong.visitkorea.or.kr") {
      url.protocol = "https:";
    }
    const localHttp =
      !import.meta.env.PROD &&
      url.protocol === "http:" &&
      (url.hostname === "localhost" || url.hostname === "127.0.0.1");
    if (
      (url.protocol !== "https:" && !localHttp) ||
      url.username ||
      url.password
    ) {
      return null;
    }
    return url.href;
  } catch {
    return null;
  }
}

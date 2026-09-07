# 프론트엔드용 익명 세션 계약

## 세션 생성 또는 재사용

```http
POST /api/v1/sessions/anonymous
```

요청 body는 없다. 브라우저에 유효한 `dg_session` 쿠키가 있으면 같은 Actor와 세션을 재사용하고, 없거나 잘못됐거나 만료됐으면 새 세션을 발급한다.

```json
{
  "data": {
    "actorType": "ANONYMOUS",
    "expiresAt": "2026-12-02T00:00:00Z"
  },
  "meta": {
    "requestId": "...",
    "generatedAt": "2026-09-03T00:00:00Z"
  }
}
```

원문 세션 토큰은 JSON에 포함되지 않고 `Set-Cookie`로만 전달된다.

```text
Set-Cookie: dg_session=<opaque>; Path=/api; Max-Age=...; Secure; HttpOnly; SameSite=Lax
Cache-Control: no-store
```

- 운영: `Secure=true`가 기본값이므로 HTTPS가 필요하다.
- `local` 프로필: HTTP 개발을 위해 `Secure=false`가 기본값이다.
- JavaScript에서는 HttpOnly 쿠키 값을 읽거나 별도 저장하지 않는다.
- 기본 만료 기간은 90일이며 고정 만료다. 요청마다 만료가 연장되지는 않는다.
- DB에는 원문이 아닌 SHA-256 해시 32바이트만 저장한다.

## 프론트 호출

프론트와 API를 같은 오리진으로 배포하거나 dev-server에서 `/api`를 백엔드로 프록시하는 구성을 권장한다.

```ts
await fetch("/api/v1/sessions/anonymous", {
  method: "POST",
  credentials: "include",
});

const response = await fetch(
  "/api/v1/tourist-spots?northEastLatitude=37.6&northEastLongitude=127.1&southWestLatitude=37.4&southWestLongitude=126.8",
  { credentials: "include" },
);
```

앱 시작 시 세션 API를 한 번 호출한 다음 지역·지도 API를 호출한다. 세션 생성이 일시적으로 실패해도 지도 API는 호출할 수 있으며 이 경우 `visitState="UNKNOWN"`으로 표시한다.

## 방문 상태

| 세션 상태 | 방문 레코드 | `visitState` | HTTP 캐시 |
|---|---|---|---|
| 없음·잘못됨·만료됨 | 무관 | `UNKNOWN` | `public`, 짧은 max-age |
| 유효함 | 없음 | `NOT_VISITED` | `private, no-store` |
| 유효함 | 있음 | `VISITED` | `private, no-store` |

GPS 스탬프 인증이 성공하면 `visit` 레코드가 생성되며 이후 지도·상세 조회가 `VISITED`를 반환한다. 요청 형식과 상태 코드는 `frontend-stamp-api.md`를 참고한다.

## 환경변수

```text
SESSION_COOKIE_NAME=dg_session
SESSION_TTL=90d
SESSION_TOUCH_INTERVAL=1h
SESSION_COOKIE_SECURE=true
```

운영에서 `SESSION_COOKIE_SECURE=false`를 사용하지 않는다. 프론트와 백엔드를 서로 다른 오리진으로 직접 호출할 경우에는 허용할 정확한 프론트 오리진을 확정한 뒤 credentials 허용 CORS를 추가해야 한다. 와일드카드 오리진과 쿠키 credentials는 함께 사용하지 않는다.

# 프론트엔드용 즐겨찾기 API 계약

좋아요와 즐겨찾기는 별도 집계 기능으로 나누지 않고 개인 저장용 `즐겨찾기` 하나로 통합했다. 모든 쓰기·목록 API는 유효한 `dg_session` 쿠키가 필요하며 `credentials: "include"`로 호출한다. 응답은 `Cache-Control: private, no-store`다.

## 추가

```http
POST /api/v1/tourist-spots/12031/favorite
```

```json
{
  "data": { "touristSpotId": 12031, "favorited": true },
  "meta": { "requestId": "...", "generatedAt": "2026-09-11T00:00:00Z" }
}
```

이미 추가된 상태에서 다시 호출해도 `200`과 같은 상태를 반환한다.

## 목록

```http
GET /api/v1/me/favorites
```

```json
{
  "data": {
    "items": [
      {
        "touristSpotId": 12031,
        "name": "경기전",
        "thumbnailUrl": "https://...",
        "visitState": "NOT_VISITED"
      }
    ]
  },
  "meta": { "count": 1, "requestId": "...", "generatedAt": "2026-09-11T00:00:00Z" }
}
```

최신 즐겨찾기부터 반환한다. `thumbnailUrl`은 사진이 없으면 `null`, `visitState`는 `VISITED` 또는 `NOT_VISITED`다.

## 해제

```http
DELETE /api/v1/tourist-spots/12031/favorite
```

```json
{
  "data": { "touristSpotId": 12031, "favorited": false },
  "meta": { "requestId": "...", "generatedAt": "2026-09-11T00:00:00Z" }
}
```

이미 해제된 상태에서 다시 호출해도 `200`과 같은 상태를 반환한다.

관광지 상세 `GET /api/v1/tourist-spots/{id}`에도 항상 `favorited`가 포함된다. 유효한 세션이 없으면 상세 조회 자체는 가능하며 `favorited=false`다.

## 오류

| HTTP | `error.code` | 의미 |
|---:|---|---|
| 401 | `AUTHENTICATION_REQUIRED` | 세션 없음·만료·변조 |
| 404 | `TOURIST_SPOT_NOT_FOUND` | 존재하지 않거나 비활성인 관광지 |

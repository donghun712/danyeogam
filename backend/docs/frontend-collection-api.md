# 프론트엔드용 지역 도감 API 계약

두 API 모두 유효한 `dg_session` 쿠키가 필요하며 응답은 `Cache-Control: private, no-store`다. `credentials: "include"`를 사용한다.

도감 분모와 목록은 활성화된 역사유적 `HS01`, 역사유물 `HS02`, 박물관 `VE070100`, 기념관 `VE070200`, 미술관·화랑 `VE070600`만 사용한다. 종교성지·안보관광지·랜드마크와 행사·축제·여행코스·레포츠·숙박·쇼핑·음식점은 포함되지 않는다.

## 지역별 도감

```http
GET /api/v1/me/collection?regionCode=TOUR:AREA:45&status=ALL
```

`regionCode`는 `GET /api/v1/regions`에서 받은 값을 그대로 전달한다. `status`는 `ALL`, `VISITED`, `NOT_VISITED`이며 생략하면 `ALL`이다.

```json
{
  "data": {
    "region": {
      "code": "TOUR:AREA:45",
      "name": "전북특별자치도"
    },
    "items": [
      {
        "touristSpotId": 12031,
        "name": "경기전",
        "visitState": "VISITED",
        "verifiedAt": "2026-09-03T04:00:01Z",
        "thumbnailUrl": "https://..."
      }
    ]
  },
  "meta": {
    "count": 1,
    "requestId": "...",
    "generatedAt": "2026-09-03T04:00:01Z"
  }
}
```

미방문 항목은 `verifiedAt=null`이다. 현재 활성화된 `STAMP_TARGET`만 포함하며 일반·비활성 관광지는 제외한다.

## 지역별 진행률

```http
GET /api/v1/me/collection/summary
```

```json
{
  "data": {
    "regions": [
      {
        "code": "TOUR:AREA:45",
        "name": "전북특별자치도",
        "visitedCount": 1,
        "totalCount": 3,
        "progressPercent": 33
      }
    ]
  },
  "meta": {
    "count": 1,
    "requestId": "...",
    "generatedAt": "2026-09-03T04:00:01Z"
  }
}
```

- `progressPercent`는 가장 가까운 정수로 반올림한다.
- 도감 대상이 없는 지역도 `visitedCount=0`, `totalCount=0`, `progressPercent=0`으로 반환한다.
- 분모는 조회 시점의 활성 스탬프 대상 수이므로 대상 관광지가 바뀌면 과거 진행률도 달라질 수 있다.
- GPS 인증에서 `collectionChanged=true`이면 도감 목록과 진행률 쿼리를 다시 가져온다.

## 오류

| HTTP | `error.code` | 의미 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | `regionCode` 누락 또는 형식 오류 |
| 400 | `INVALID_COLLECTION_STATUS` | 지원하지 않는 상태 필터 |
| 401 | `AUTHENTICATION_REQUIRED` | 세션 없음·만료·변조 |
| 404 | `REGION_NOT_FOUND` | 존재하지 않거나 비활성인 지역 |

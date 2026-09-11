# 프론트엔드 칭호 API 계약

## 인증과 호출 규칙

- 모든 호출은 기존 익명 세션 쿠키 `dg_session`을 사용한다.
- 브라우저 요청은 `credentials: "include"`를 지정한다.
- 세션이 없거나 만료되면 HTTP 401과 `AUTHENTICATION_REQUIRED`가 반환된다.
- 응답은 사용자별 상태이므로 `Cache-Control: private, no-store`가 적용된다.

## 내 칭호 목록

```http
GET /api/v1/me/titles
```

활성 칭호 정의 전체를 표시 순서대로 반환한다. `earned=false`인 칭호도 포함하므로 프론트는 별도 정적 목록을 가질 필요가 없다.

```json
{
  "data": {
    "titles": [
      {
        "id": 1,
        "code": "FIRST_STEP",
        "name": "첫 걸음",
        "description": "첫 번째 스탬프를 획득하세요.",
        "earned": false,
        "awardedAt": null,
        "currentValue": 0,
        "targetValue": 1,
        "progressUnit": "VISITS"
      },
      {
        "id": 15,
        "code": "REGION_MASTER:TOUR:AREA:52",
        "name": "전북특별자치도 터줏대감",
        "description": "전북특별자치도 스탬프 진행률 20%를 달성하세요.",
        "earned": true,
        "awardedAt": "2026-09-11T06:00:00Z",
        "currentValue": 24,
        "targetValue": 20,
        "progressUnit": "PERCENT"
      }
    ]
  },
  "meta": {
    "requestId": "cfd8fa69-b642-4a14-984d-1b6c4ecb0b9e",
    "generatedAt": "2026-09-11T06:00:01Z",
    "count": 23
  }
}
```

`progressUnit`은 다음 중 하나다.

- `VISITS`: 서로 다른 관광지 방문 수
- `REGIONS`: 서로 다른 광역 또는 시군구 방문 수
- `PERCENT`: 해당 광역지역의 현재 도감 진행률

## 스탬프 인증에서 신규 칭호 확인

기존 `POST /api/v1/stamp-verifications` 응답의 `newTitleIds`에 이번 인증으로 새로 부여된 `title_definition.id`가 들어간다.

```json
{
  "data": {
    "status": "VERIFIED_NEW",
    "touristSpotId": 34163,
    "distanceMeters": 12.34,
    "verifiedAt": "2026-09-11T06:00:00Z",
    "visitState": "VISITED",
    "collectionChanged": true,
    "newTitleIds": [1, 15]
  }
}
```

프론트는 `newTitleIds`가 비어 있지 않으면 `GET /api/v1/me/titles`를 다시 호출하고, ID가 일치하는 항목의 이름과 설명으로 획득 알림을 표시한다. 동일한 `Idempotency-Key`로 인증을 재호출하면 동일한 `newTitleIds`가 반환된다.

## 판정 기준

| code | 이름 | DB 판정 기준 |
|---|---|---|
| `FIRST_STEP` | 첫 걸음 | Visit 1개 |
| `TRAVEL_RECORDER` | 여행 기록가 | Visit 10개 |
| `DANYEOGAM_RECORDER` | 다녀감 기록가 | Visit 50개 |
| `EIGHT_PROVINCE_TRAVELER` | 팔도 여행자 | 방문한 서로 다른 `PROVINCE` 8곳 |
| `EVERY_CORNER_EXPLORER` | 구석구석 탐험가 | 방문한 서로 다른 `CITY_COUNTY` 10곳 |
| `HISTORY_FOOTPRINT` | 역사 발자국 | `classification_level2=HS01`인 서로 다른 관광지 15곳 |
| `CULTURE_COLLECTOR` | 문화 수집가 | `classification_level3`가 `VE070100`, `VE070200`, `VE070600` 중 하나인 서로 다른 관광지 8곳 |
| `REGION_MASTER:{regionCode}` | `{지역명} 터줏대감` | 해당 광역 도감 진행률 20% 이상 |

터줏대감은 활성 `PROVINCE`마다 별도 정의와 ID를 가진다. 따라서 한 사용자가 여러 지역 터줏대감을 동시에 보유할 수 있다.

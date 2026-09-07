# 프론트엔드용 읽기 API 계약

기준 경로는 `/api/v1`이며 현재 세 API는 세션 없이도 호출할 수 있다. `POST /api/v1/sessions/anonymous`를 먼저 호출하고 쿠키를 포함하면 방문 상태가 함께 반환된다. 성공 응답은 `data`와 `meta`, 실패 응답은 `error`와 `meta`를 사용한다. 프론트는 오류 문구가 아닌 `error.code`로 분기한다.

## 공통

- 좌표계: WGS84, 위도 `latitude`, 경도 `longitude`
- 응답 헤더: `X-Request-Id`
- `meta.count`: 목록 응답의 반환 항목 수
- 세션 없음·잘못됨·만료됨: `visitState="UNKNOWN"`
- 유효한 세션: 방문 기록에 따라 `NOT_VISITED` 또는 `VISITED`
- 세션 포함 요청은 사용자 상태가 섞이므로 `Cache-Control: private, no-store`
- 사진이 없으면 상세 `images`는 빈 배열이며 프론트가 기본 이미지를 표시한다.

## 1. 지역 목록

```http
GET /api/v1/regions
```

```json
{
  "data": {
    "items": [
      {
        "id": 1,
        "code": "1",
        "name": "서울특별시",
        "level": "PROVINCE",
        "children": [
          {
            "id": 2,
            "code": "1-1",
            "name": "강남구",
            "level": "CITY_COUNTY",
            "children": []
          }
        ]
      }
    ]
  },
  "meta": {
    "requestId": "...",
    "generatedAt": "2026-09-03T00:00:00Z",
    "count": 1
  }
}
```

지역 데이터가 없으면 `items: []`, `count: 0`을 반환한다.

## 2. 지도 영역 관광지

```http
GET /api/v1/tourist-spots?northEastLatitude=37.6&northEastLongitude=127.1&southWestLatitude=37.4&southWestLongitude=126.8&types=GENERAL,STAMP_TARGET
```

필수 쿼리 파라미터는 북동쪽과 남서쪽 위·경도 네 개다. `types`는 선택이며 `GENERAL`, `STAMP_TARGET`을 쉼표로 전달한다. 생략하면 모든 유형을 조회한다.

```json
{
  "data": {
    "items": [
      {
        "id": 12031,
        "name": "관광지 이름",
        "position": {
          "latitude": 37.5000000,
          "longitude": 127.0000000
        },
        "type": "GENERAL",
        "stampEnabled": false,
        "visitState": "UNKNOWN",
        "thumbnailUrl": "https://..."
      }
    ]
  },
  "meta": {
    "requestId": "...",
    "generatedAt": "2026-09-03T00:00:00Z",
    "count": 1
  }
}
```

프론트 호출 규칙:

- 카카오맵 `idle` 이벤트 후 약 300ms 디바운스한다.
- 새 bounds 요청을 보낼 때 이전 요청을 취소하고 최신 응답만 반영한다.
- 목록에는 상세 설명과 전체 이미지가 없으므로 마커 선택 시 상세 API를 호출한다.
- 기본 허용 범위는 위도·경도 각각 1도, 최대 결과 수는 1000건이다.
- 세션 쿠키를 보낼 때 직접 교차 출처로 호출한다면 `credentials: "include"`가 필요하다. 로컬 개발은 프론트 dev-server의 `/api` 프록시 사용을 권장한다.

## 3. 관광지 상세

```http
GET /api/v1/tourist-spots/12031
```

```json
{
  "data": {
    "id": 12031,
    "name": "관광지 이름",
    "type": "GENERAL",
    "stampEnabled": false,
    "visitState": "UNKNOWN",
    "address": {
      "road": "도로명 주소",
      "lot": "지번 주소"
    },
    "position": {
      "latitude": 37.5,
      "longitude": 127.0
    },
    "overview": "상세 설명",
    "images": [
      {
        "url": "https://...",
        "alt": "관광지 전경",
        "copyrightType": "Type1"
      }
    ],
    "telephone": "02-0000-0000",
    "homepageUrl": "https://...",
    "navigation": {
      "destinationName": "관광지 이름",
      "latitude": 37.5,
      "longitude": 127.0,
      "coordinateType": "wgs84"
    },
    "dataSource": "한국관광공사 TourAPI",
    "dataQuality": "COMPLETE",
    "lastSyncedAt": "2026-09-03T00:00:00Z"
  },
  "meta": {
    "requestId": "...",
    "generatedAt": "2026-09-03T00:00:00Z"
  }
}
```

`dataQuality`가 `PARTIAL`이면 상세 보강 전 데이터이며 nullable 필드가 있을 수 있다. `navigation` 좌표를 카카오내비 목적지에 전달하되 프론트의 카카오 JavaScript 키와 서버 REST 키를 혼동하지 않는다.

## 오류 코드

| HTTP | `error.code` | 프론트 처리 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | 누락되거나 숫자가 아닌 파라미터 확인 |
| 400 | `INVALID_BOUNDS` | 좌표 범위 및 북동/남서 순서 확인 |
| 400 | `BOUNDS_TOO_WIDE` | 지도를 확대하도록 안내 |
| 400 | `BOUNDS_TOO_DENSE` | 지도를 확대한 뒤 재조회 |
| 400 | `INVALID_SPOT_TYPE` | 지원하는 유형 값으로 수정 |
| 404 | `TOURIST_SPOT_NOT_FOUND` | 상세 화면을 닫고 목록 갱신 |
| 500 | `INTERNAL_SERVER_ERROR` | 재시도 UI와 `X-Request-Id` 보관 |

오류 예시:

```json
{
  "error": {
    "code": "INVALID_BOUNDS",
    "message": "지도 좌표 범위를 확인해 주세요.",
    "retryable": false,
    "fieldErrors": []
  },
  "meta": {
    "requestId": "...",
    "generatedAt": "2026-09-03T00:00:00Z"
  }
}
```

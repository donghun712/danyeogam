# 프론트 위치·주변 주차장 API 계약

## 공통

- Base URL: `/api/v1`
- 좌표계: WGS84
- 카카오 서버 REST 키는 프론트에 전달하거나 번들에 포함하지 않는다.
- 카카오 지도 표시용 JavaScript 키와 서버 REST 키는 서로 다른 키다.

## 현재 좌표의 주소

`POST /api/v1/geo/reverse`

정확한 GPS 좌표가 브라우저·프록시 URL에 남지 않도록 GET 쿼리가 아닌 JSON body를 사용한다.

```json
{
  "position": {
    "latitude": 35.8242,
    "longitude": 127.1480
  }
}
```

성공 응답은 `Cache-Control: private, no-store`다.

```json
{
  "data": {
    "addressName": "전북특별자치도 전주시 완산구 풍남동3가",
    "roadAddressName": "전북특별자치도 전주시 완산구 태조로 1",
    "region": {
      "depth1": "전북특별자치도",
      "depth2": "전주시 완산구",
      "depth3": "풍남동3가"
    },
    "source": "KAKAO_LOCAL"
  },
  "meta": {
    "requestId": "...",
    "generatedAt": "2026-09-03T00:00:00Z"
  }
}
```

카카오에 주소가 없으면 HTTP 200을 유지하고 `addressName`, `roadAddressName`, `region`이 `null`일 수 있다. 키 누락·429·5xx·타임아웃은 `503 GEO_PROVIDER_UNAVAILABLE`이다. 이 오류가 발생해도 프론트 지도와 관광지 마커는 계속 표시한다.

## 관광지 주변 주차장

`GET /api/v1/tourist-spots/{spotId}/parking?radiusMeters=3000&limit=3`

- `radiusMeters` 기본값 3000m, 서버 상한 20000m
- `limit` 기본값 3, 서버 상한 15
- 결과는 거리 오름차순이다.
- 카카오 `PK6`은 주차장 후보이며 공영 여부를 보증하지 않으므로 항상 `publicVerified=false`다.

```json
{
  "data": {
    "items": [
      {
        "id": "KAKAO:123456",
        "name": "한옥마을 주차장",
        "address": "전북특별자치도 전주시 완산구 ...",
        "position": {
          "latitude": 35.816,
          "longitude": 127.151
        },
        "distanceMeters": 260,
        "publicVerified": false,
        "source": "KAKAO_LOCAL",
        "navigation": {
          "destinationName": "한옥마을 주차장",
          "latitude": 35.816,
          "longitude": 127.151,
          "coordinateType": "wgs84"
        },
        "fetchedAt": "2026-09-03T00:00:00Z"
      }
    ],
    "temporarilyUnavailable": false
  },
  "meta": {
    "requestId": "...",
    "generatedAt": "2026-09-03T00:00:00Z",
    "count": 1
  }
}
```

공급자 장애나 카카오 키 미설정 시 관광지 상세 API는 실패하지 않는다. 주차장 API도 HTTP 200으로 다음 부분 실패 상태를 반환한다.

```json
{
  "data": {
    "items": [],
    "temporarilyUnavailable": true
  },
  "meta": {
    "requestId": "...",
    "generatedAt": "2026-09-03T00:00:00Z",
    "count": 0
  }
}
```

프론트 문구는 `주변 공영주차장`이 아니라 `주변 주차장`을 사용한다. `temporarilyUnavailable=true`이면 별도 주차장 영역에 재시도 UI를 표시한다.

## 오류 코드

| HTTP | `error.code` | 의미 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | 반경·개수 또는 요청 형식 오류 |
| 400 | `VALIDATION_FAILED` | 위도·경도 범위 오류 |
| 404 | `TOURIST_SPOT_NOT_FOUND` | 존재하지 않거나 비활성인 관광지 |
| 503 | `GEO_PROVIDER_UNAVAILABLE` | 역지오코딩 공급자 키 누락 또는 일시 장애 |

## 키 입력 후 확인할 항목

`.env`에 `KAKAO_REST_API_KEY`를 추가한 뒤 실제 주소 변환, 역지오코딩, PK6 주차장 결과, 429 및 호출 한도를 확인한다. 키 값은 소스·로그·프론트 응답에 포함하지 않는다.

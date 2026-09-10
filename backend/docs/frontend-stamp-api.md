# 프론트엔드용 GPS 스탬프 인증 계약

## 호출

```http
POST /api/v1/stamp-verifications
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json
Cookie: dg_session=<HttpOnly 쿠키>
```

```json
{
  "touristSpotId": 12031,
  "position": {
    "latitude": 35.8150,
    "longitude": 127.1500,
    "accuracyMeters": 18.4,
    "measuredAt": "2026-09-03T04:00:00Z"
  }
}
```

- 인증 버튼을 누른 시점에 `navigator.geolocation.getCurrentPosition`으로 새 위치를 측정한다.
- 앱 시작 시 받은 과거 좌표를 재사용하지 않는다.
- `Idempotency-Key`는 요청마다 새 UUID를 만들고 네트워크 재시도에는 같은 UUID를 사용한다.
- 클라이언트가 계산한 거리는 보내지 않는다. 서버가 관광지 좌표와 Haversine 거리로 판정한다.
- 요청과 응답에 `credentials: "include"`를 사용한다.

## 응답

정상적인 인증 결과와 업무 실패는 모두 HTTP 200이다.

```json
{
  "data": {
    "status": "VERIFIED_NEW",
    "touristSpotId": 12031,
    "distanceMeters": 22.80,
    "verifiedAt": "2026-09-03T04:00:01Z",
    "visitState": "VISITED",
    "collectionChanged": true,
    "newTitleIds": []
  },
  "meta": {
    "requestId": "...",
    "generatedAt": "2026-09-03T04:00:01Z"
  }
}
```

| `status` | 의미 | 권장 처리 |
|---|---|---|
| `VERIFIED_NEW` | 최초 획득 | 획득 모션 후 지도·상세·도감 갱신 |
| `VERIFIED_ALREADY_ACQUIRED` | 이미 획득 | 기존 방문 완료 안내 |
| `OUT_OF_RANGE` | 허용 반경 밖 | 가까이 이동한 뒤 다시 측정 |
| `GPS_ACCURACY_INSUFFICIENT` | GPS 오차가 큼 | 야외 이동 후 다시 측정 |
| `LOCATION_STALE` | 오래됐거나 미래 시각인 측정 | 새 위치를 요청 |
| `STAMP_DISABLED` | 인증 대상이 아니거나 중단됨 | 인증 버튼 비활성화 및 상세 갱신 |

`distanceMeters`는 거리 계산 전 실패에서는 `null`일 수 있고, `verifiedAt`은 성공 또는 이미 획득 상태에서만 존재한다. 칭호 기능 전까지 `newTitleIds`는 빈 배열이다.

## HTTP 오류

| HTTP | `error.code` | 의미 |
|---:|---|---|
| 400 | `INVALID_REQUEST` | body 또는 멱등 키 형식 오류 |
| 400 | `VALIDATION_FAILED` | 좌표·정확도 필드 범위 오류 |
| 401 | `AUTHENTICATION_REQUIRED` | 세션 없음·만료·변조 |
| 403 | `ORIGIN_NOT_ALLOWED` | 허용되지 않은 브라우저 출처 |
| 404 | `TOURIST_SPOT_NOT_FOUND` | 존재하지 않는 관광지 |
| 409 | `IDEMPOTENCY_KEY_CONFLICT` | 같은 키를 다른 관광지 요청에 사용 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 요청 `Content-Type`이 `application/json`이 아님 |
| 429 | `TOO_MANY_REQUESTS` | Actor 기준 최근 1분의 새 인증 요청 5회 초과 |

429 응답에는 다시 요청할 수 있을 때까지 남은 초가 `Retry-After` 헤더로 제공된다.
프론트는 이 시간 동안 재시도 버튼을 비활성화하고 남은 시간을 안내한다.

## 현재 판정 기준

로컬·테스트의 임시값은 반경 100m, 최대 GPS 정확도 50m, 측정 유효시간 2분, 미래 시각 허용 10초다. 화면 문구에 이 숫자를 고정하지 않는다. 운영에서는 현장 테스트 결과를 다음 환경변수로 반드시 주입해야 하며, 핵심 세 값이 없으면 서버가 시작되지 않는다.

```text
STAMP_DEFAULT_RADIUS_METERS
STAMP_MAX_ACCURACY_METERS
STAMP_MAX_LOCATION_AGE
STAMP_MAX_FUTURE_SKEW
STAMP_MAX_ATTEMPTS_PER_MINUTE
CORS_ALLOWED_ORIGINS
```

관광지별 `stamp_radius_meters`가 있으면 공통 반경보다 우선한다. 정확한 사용자 GPS 좌표는 거리 계산에만 사용하며 DB와 애플리케이션 로그에 저장하지 않는다.

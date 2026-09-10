# 관광지 상세 regionCode 추가 및 검증

검증일: 2026-09-08

## 프론트 업데이트 확인

`bde5eaa..532050d` 기준으로 스탬프 인장·획득 효과, GPS 레이더·측정 정보,
지도 마커·클러스터·확대/축소, 지역 선택 UI, 상세·주차 화면 표시가 개선됐다.
현재 프론트 상세 타입과 획득 화면에는 regionCode 및 지역 진행률 연결이 아직 없다.
프론트 파일은 수정하지 않았으며 `npm run build`는 성공했다.

## 변경 파일

- `src/main/java/com/danyeogam/backend/touristspot/application/dto/TouristSpotDetailResponse.java`
- `src/main/java/com/danyeogam/backend/touristspot/application/TouristSpotQueryService.java`
- `src/test/java/com/danyeogam/backend/touristspot/application/TouristSpotQueryServiceTest.java`
- `docs/frontend-read-api.md`
- `docs/openapi.json`
- `docs/openapi.yaml`
- `docs/region-code-verification.md` (이 보고서)

기존 상세 필드의 이름·타입·순서를 유지하고 맨 뒤에 String regionCode를 추가했다.
다른 API 구현 및 응답 DTO, DB 스키마, 관광지 데이터, 스탬프 정책과 GPS 운영값은 변경하지 않았다.

## 정확한 조회 로직

상세 조회의 read-only 트랜잭션 안에서 `resolveProvinceRegionCode(spot.getRegion())`를 호출한다.
LAZY 부모 관계는 해당 트랜잭션 안에서 조회된다.

```java
private static String resolveProvinceRegionCode(Region region) {
    Set<String> visitedCodes = new HashSet<>();
    for (Region current = region; current != null; current = current.getParent()) {
        if (!visitedCodes.add(current.getCode())) {
            break;
        }
        if (current.getLevel() == RegionLevel.PROVINCE) {
            return current.getCode();
        }
    }
    throw new IllegalStateException("관광지의 상위 광역 지역을 찾을 수 없습니다.");
}
```

요청서의 `Region.parent`와 `RegionLevel.PROVINCE/CITY_COUNTY` 구조는 실제 코드와 일치한다.
광역 지역 직속이면 자신의 코드, 시군구 소속이면 부모를 따라 찾은 광역 코드를 반환한다.
부모 누락·순환 데이터에서는 시군구 코드로 대체하지 않고 실패한다.
현재 DB의 전북 코드는 요청서 예시의 `TOUR:AREA:45`가 아닌 `TOUR:AREA:52`이다.
코드를 자르거나 하드코딩하지 않으므로 실제 데이터의 코드가 반환된다.

## 검증 결과

- `gradlew.bat test bootJar`: 성공. 총 97건 중 89건 통과, 실패 0, 오류 0, 조건부 8건 제외.
  제외된 항목은 환경변수로 활성화하는 MySQL 및 외부 TourAPI/Kakao 테스트다.
- 추가 단위 검증: 광역 직속, 시군구 상위 코드, 로그인 여부와 무관한 동일 코드, 부모 누락·순환 거부.
- 세부 분류 정책 반영 후 MySQL 카탈로그에서 활성 관광지 3,885건 확인: 시군구 3,872건, 광역 직속 13건.
  상위 지역 레벨·활성 상태·코드 형식 이상 0건.
- 실제 서버에서 익명 세션을 발급하고 `/api/v1/me/collection/summary`의 16개 코드를 조회했다.
  16개 광역 지역별 관광지 상세를 하나씩 호출하여 DB 상위 코드와 일치하고,
  `^TOUR:AREA:[0-9]+$` 형식이며 요약 목록에 포함되는 것을 모두 확인했다.
- 광역 직속 관광지 상세도 별도 호출하여 자기 광역 코드 반환을 확인했다.
- OpenAPI에서 기존 상세 properties와 신규 응답의 regionCode 제외 properties를 비교해
  기존 필드 이름·타입·순서가 동일함을 확인했다.

## 실제 응답 예시

경북 지역 관광지 관호산성, `GET /api/v1/tourist-spots/14` 응답 일부:

```json
{
  "id": 14,
  "name": "관호산성",
  "type": "STAMP_TARGET",
  "regionCode": "TOUR:AREA:47"
}
```

`regionCode`는 실제 도감 요약 목록에 포함된다. 관광지의 면적·규모 필드는 현재 API에 없다.

이 보고서 뒤에 적용한 관광지 세부 분류 정책으로 현재 활성 대상은 3,885건이다. `regionCode` 조회 로직과 응답 계약에는 영향이 없다.

## Swagger 및 전달 문서

DTO의 Schema 설명·예시·정규식·required 선언을 추가하고 실행 서버의 `/v3/api-docs` 및
`/v3/api-docs.yaml`로 고정 문서를 갱신했다. 기존 YAML에 저장돼 있던 숫자 배열 문자열을
정상 UTF-8 YAML로 복구했다. 재생성 과정에서 기존 고정 문서에 누락됐던 RegionResponse.children도
현재 서버 스키마대로 반영됐다. `/regions` API 코드는 변경하지 않았다.
`frontend-read-api.md` 상세 응답 예시와 도감 요약 매칭 설명을 갱신했다.

프론트 담당자는 TouristSpotDetail 타입에 `regionCode: string`을 추가한 후
요약의 `regions.find(region => region.code === detail.regionCode)`로 진행률을 연결할 수 있다.

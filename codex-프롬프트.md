# Codex에게 붙여넣을 프롬프트

아래 내용을 그대로 복사해서 Codex(또는 사용 중인 AI 코딩 도구)에 붙여넣으시면 됩니다.
`regionCode-요청서.md` 파일을 저장소 어딘가(예: `backend/docs/` 또는 저장소 루트)에 커밋한
뒤, 그 파일 경로를 프롬프트 맨 위에 채워 넣어 주세요.

---

## 프롬프트 시작

다녀감 백엔드(danyeogam) 저장소에서 작업을 진행해줘.

먼저 `regionCode-요청서.md` 파일을 읽고 요청 내용을 정확히 파악해줘.

## 작업 범위

`TouristSpotDetailResponse`에 `regionCode` 필드를 추가하는 것 **하나만** 진행한다.

## 조건

- `TouristSpotDetailResponse`에 `regionCode` 필드를 추가한다.
- 이 값은 반드시 PROVINCE 레벨 지역 코드여야 한다. `TouristSpot.getRegion()`이 CITY_COUNTY
  레벨일 경우 `getParent()`를 따라 올라가서 PROVINCE 레벨 코드를 찾아 반환한다.
  요청서에 있는 이유를 반드시 읽고 이해한 뒤 구현해줘 — 그냥 `region.getCode()`를 그대로
  반환하면 안 된다.
- 이 값이 실제로 `GET /api/v1/me/collection/summary`가 반환하는 `regions[].code` 중 하나와
  일치하는지 기존 DB 데이터 기준으로 직접 확인해줘(예: 테스트 코드 또는 로컬 조회로).
- 요청서의 코드 스니펫을 그대로 복사하지 말고, 먼저 현재 `Region` 엔티티/연관관계와 기존
  지역 코드 처리 로직(`TourSyncPersistenceService`, `RegionRepository` 등)을 확인한 뒤
  프로젝트의 실제 구조와 코딩 스타일에 맞는 방식으로 구현해줘.
- 특히 `Region`의 `parent` 관계나 `RegionLevel` 구조가 요청서에 적힌 것과 실제 코드가
  다르다면, 임의로 새 구조를 만들지 말고 기존 구조를 활용해줘. 다르다는 걸 발견하면
  그 사실부터 먼저 알려줘.
- 기존 `TouristSpotDetailResponse`의 다른 필드는 이름/타입/순서 전부 그대로 유지한다.
- 다른 API(`/regions`, `/tourist-spots`(목록), `/me/collection` 등)의 응답 구조는 건드리지
  않는다.
- 프론트엔드 코드는 별도 저장소이므로 손대지 않는다.
- `regionCode`와 무관한 리팩터링이나 스타일 변경은 하지 않는다.
- Swagger(`docs/openapi.yaml` 또는 자동 생성 설정)와 `docs/frontend-read-api.md`의 응답
  예시를 실제 변경사항에 맞게 갱신한다.

## 확인해야 할 것

1. 기존 테스트가 깨지지 않는지 (`./gradlew test`)
2. 새로 추가한 `regionCode`가 실제로 PROVINCE 레벨 코드 형식(`TOUR:AREA:숫자`, 콜론 뒤 추가
   세그먼트 없음)으로 나오는지
3. 여러 관광지로 확인했을 때 이 값이 `/me/collection/summary`의 `regions[].code` 목록에
   실제로 포함되는지
4. Swagger 문서가 실제 응답 구조와 일치하는지

## 보고 형식

작업이 끝나면 다음을 알려줘.

1. 변경한 파일 목록
2. `regionCode`를 채우는 정확한 로직(코드 스니펫)
3. 요청서에 적힌 구조(Region.parent/RegionLevel)와 실제 코드가 달랐는지 여부
4. 테스트 결과
5. 실제 응답 예시(관광지 하나 골라서 규모/이름과 함께 `regionCode` 값 보여주기)
6. Swagger/문서 갱신 여부

문제가 발견되거나 기존 데이터 구조상 불가능한 부분이 있으면, 임의로 다른 방식으로 우회하지
말고 먼저 알려줘.

## 프롬프트 끝

---

## 참고

이 프롬프트는 **regionCode 필드 추가 작업 하나만** 다룹니다. 요청서 맨 아래에 적힌
"STAMP_TARGET 정책"과 "GPS 인증 운영값"은 코드 작업이 아니라 팀 논의가 먼저 필요한
항목이라 이 프롬프트에는 일부러 포함하지 않았습니다. 두 항목은 팀에서 방향이 정해지면
그때 별도로 요청서를 만들어 드리겠습니다.

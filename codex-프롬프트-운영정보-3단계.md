# Codex에게 붙여넣을 프롬프트 — 운영정보/시설정보 구현 (3단계)

`운영정보-요청서.md`(1단계 배경)와 `운영정보-2단계-스키마.md`(확정된 스키마)를 저장소에
커밋한 뒤, 아래 경로를 채워서 붙여넣으세요.

---

## 프롬프트 시작

다녀감 백엔드(danyeogam) 저장소에서 작업을 진행해줘.

먼저 다음 두 파일을 순서대로 읽고 파악해줘.
1. `[운영정보-요청서.md]` — 배경과 1단계 조사 결과
2. `[운영정보-2단계-스키마.md]` — 확정된 응답 스키마

이번엔 구현까지 진행해줘 (1, 2단계는 이미 끝났음).

## 조건

- `운영정보-2단계-스키마.md`에 있는 `OperatingInfoResponse`, `FacilityStatusResponse`,
  `FacilityInfoResponse` 구조를 그대로 구현해줘. 필드명/타입은 문서 기준이 맞지만, 실제
  코드 스타일(레코드 패키지 위치, 네이밍 컨벤션)은 프로젝트 기존 관례를 따라줘.
- `TouristSpotDetailResponse`에 `operatingInfo`, `facilityInfo` 두 필드를 추가해줘.
- TourAPI `detailIntro2`를 호출해서 값을 가져오되, **contentTypeId에 따라 필드명이
  다르다**(관광지는 `usetime`/`restdate`/`parking`/`chkbabycarriage`/`chkpet`, 문화시설은
  `usetimeculture`/`restdateculture`/`parkingculture`/`chkbabycarriageculture`/
  `chkpetculture`/`parkingfee`). 이 매핑은 스키마 문서의 표를 따라줘.
- 빈 문자열/공백만 있는 값은 `null`로 변환해줘.
- `status` 분류는 문서에 적힌 대로: 원문이 "가능"으로 시작하면 `AVAILABLE`, "불가능"/"불가"로
  시작하면 `UNAVAILABLE`, 그 외(빈 값 포함)는 `UNKNOWN`.
- **`UNKNOWN`이어도 원문이 있으면 `note`에 반드시 채워줘** — status만 보고 note를 비워두면
  안 돼. 이게 이번 작업에서 제일 중요한 부분이야.
- 휠체어(무장애) 정보는 이번 범위에서 제외야. 필드 자체를 추가하지 마.
- 기존 `TouristSpotDetailResponse`의 다른 필드는 이름/타입/순서 그대로 유지해줘.
- 프론트엔드 코드는 건드리지 마 (별도 저장소).
- Swagger(`docs/openapi.yaml` 등)와 `docs/frontend-read-api.md`의 응답 예시를 갱신해줘.
- `docs/tourapi-field-mapping.md`에 이번에 추가한 `detailIntro2` 매핑을 기존 문서 형식과
  동일하게 추가해줘.

## 확인해야 할 것

1. 기존 테스트가 깨지지 않는지 (`./gradlew test`)
2. 관광지(12)와 문화시설(14) 타입 각각 실제 관광지로 확인했을 때, `operatingInfo`/
   `facilityInfo`가 올바르게 채워지는지
3. `status=UNKNOWN`인데 `note`가 채워진 실제 사례가 있는지 확인 (예: 애매한 값이 있는
   관광지로 테스트)
4. 값이 비어있는 관광지에서 `null` 처리가 제대로 되는지

## 보고 형식

1. 변경한 파일 목록
2. 실제 `detailIntro2` 매핑 코드(핵심 부분)
3. 테스트 결과
4. 실제 응답 예시 — `AVAILABLE`/`UNAVAILABLE`/`UNKNOWN`+note 있는 경우/`UNKNOWN`+note
   없는 경우, 이렇게 4가지 케이스를 실제 관광지로 찾아서 각각 보여줘
5. Swagger/문서 갱신 여부

애매한 부분이 있으면(예: 분류 임계값을 실제 데이터에 적용했을 때 이상한 케이스가 나옴)
임의로 넘기지 말고 먼저 알려줘.

## 프롬프트 끝

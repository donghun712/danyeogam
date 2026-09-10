# TourAPI 국문 서비스 필드 매핑

검증 기준 서비스는 `https://apis.data.go.kr/B551011/KorService2`다. 2026-09-08 실제 응답으로 요청 파라미터와 대분류·중분류·소분류 필드를 확인했다.

## 사용하는 기능

| 기능 | Operation | 기능 파라미터 |
|---|---|---|
| 광역·시군구 법정동 코드 | `ldongCode2` | 하위 지역 조회 시 `lDongRegnCd` |
| 선정 관광지 목록 | `areaBasedList2` | `areaCode`, `contentTypeId`, `lclsSystm1/2/3`, `arrange=C` |
| 관광지 공통 상세 | `detailCommon2` | `contentId` |
| 관광지 소개정보 | `detailIntro2` | `contentId`, `contentTypeId` |
| 관광지 이미지 | `detailImage2` | `contentId` |

모든 요청에는 `serviceKey`, `MobileOS`, `MobileApp`, `_type=json`, `pageNo`, `numOfRows`를 공통으로 전달한다. 현재 `detailImage2`에는 과거 파라미터인 `imageYN`, `subImageYN`을 전달하지 않는다.

## 스탬프 대상 선정 정책

구 `cat1/cat2/cat3`가 아니라 현재 응답의 `lclsSystm1/2/3`를 기준으로 판정한다.

| 서비스 의미 | contentTypeId | 대분류 | 중분류 | 소분류 |
|---|---:|---|---|---|
| 역사유적 | 12 | `HS` | `HS01` | 전체 |
| 역사유물 | 12 | `HS` | `HS02` | 전체 |
| 박물관 | 12, 14 | `VE` | `VE07` | `VE070100` |
| 기념관 | 12, 14 | `VE` | `VE07` | `VE070200` |
| 미술관·화랑 | 12, 14 | `VE` | `VE07` | `VE070600` |

종교성지 `HS03`, 안보관광지 `HS04`, 랜드마크·자연·체험 등 그 밖의 관광지와 다른 문화시설은 제외한다. 행사·축제 `15`, 여행코스 `25`, 레포츠 `28`, 숙박 `32`, 쇼핑 `38`, 음식점 `39`도 요청하지 않는다.

전국 동기화가 모든 선정 조건의 마지막 페이지까지 완료됐을 때만 기존 비선정 TourAPI 장소를 비활성화한다. 특정 지역 동기화나 페이지 수가 부족한 부분 동기화는 전역 비활성화를 수행하지 않는다.

## 관광지 매핑

| TourAPI 필드 | DB 필드 | 처리 |
|---|---|---|
| `contentid` | `source_content_id` | `source=TOUR_API`와 함께 유일 키 |
| `contenttypeid` | `source_content_type_id` | 원본 문자열 유지 |
| `lclsSystm1` | `classification_level1` | 선정 정책 판정 및 원본 분류 보존 |
| `lclsSystm2` | `classification_level2` | 선정 정책 판정 및 원본 분류 보존 |
| `lclsSystm3` | `classification_level3` | 선정 정책 판정 및 원본 분류 보존 |
| `title` | `name` | 필수 |
| `addr1` | `road_address` | 없을 수 있음 |
| `addr2` | `lot_address` | 보조 주소로 저장 |
| `mapx` | `location` 경도 X | 소수 7자리, 대한민국 범위 검증 |
| `mapy` | `location` 위도 Y | 소수 7자리, 대한민국 범위 검증 |
| `lDongRegnCd` | `region.code` | 우선 사용, `TOUR:AREA:{광역 법정동 코드}` |
| `lDongSignguCd` | `region.code` | 우선 사용, `TOUR:AREA:{광역 코드}:{시군구 코드}` |
| `areacode` | `region.code` | 구 응답 호환용 fallback, 법정동 광역 코드로 변환 |
| `sigungucode` | `region.code` | 구 응답 호환용 fallback |
| `firstimage` | `original_image_url` | 대표 원본 이미지 |
| `firstimage2` | `thumbnail_url` | 대표 썸네일 |
| `tel` | `tel` | 상세 응답 값으로 보강 가능 |
| `modifiedtime` | `source_modified_at` | 한국 표준시로 해석 후 UTC 저장 |
| `overview` | `overview` | HTML 제거 후 일반 텍스트 저장 |
| `homepage` | `homepage_url` | HTTP(S) 링크만 추출 |

`KorService2`의 현재 지역 분류는 법정동 코드를 기준으로 한다. 목록에 신·구 필드가 함께 오면 `lDongRegnCd`와 `lDongSignguCd`를 우선 사용한다. 세종시의 전체 법정동 코드 `36110`은 광역 코드 `36`으로 정규화하고 별도 시군구를 만들지 않는다.

## 이미지 매핑

| TourAPI 필드 | DB 필드 |
|---|---|
| `originimgurl` | `tourist_spot_image.url` |
| `imgname` | `alt_text` |
| `serialnum` | `source_image_id` |
| `cpyrhtDivCd` | `copyright_type` |

`detailImage2`가 빈 목록이어도 목록 응답에 `firstimage`가 있으면 대표 이미지를 첫 번째 이미지로 저장한다. 어느 응답에도 이미지가 없으면 관광지는 정상 적재하고 이미지 목록은 비워 둔다.

## 소개정보 매핑

`detailIntro2`는 `contentTypeId`에 따라 필드명이 다르다. 2026-09-10 관광지와 문화시설 각 8건의 실제 응답으로 다음 필드를 확인했다.

| API 응답 의미 | 관광지 12 | 문화시설 14 | DB 필드 |
|---|---|---|---|
| 운영시간 | `usetime` | `usetimeculture` | `operating_hours` |
| 휴무일 | `restdate` | `restdateculture` | `closed_days` |
| 주차 | `parking` | `parkingculture` | `parking_note` |
| 주차요금 | 없음 | `parkingfee` | `parking_fee_note` |
| 유모차 대여 | `chkbabycarriage` | `chkbabycarriageculture` | `stroller_rental_note` |
| 반려동물 | `chkpet` | `chkpetculture` | `pet_allowed_note` |

모든 값은 자유 형식 문자열이며 빈 문자열과 공백만 있는 값은 `null`로 저장한다. `가능`으로 시작하면 `AVAILABLE`, `불가능` 또는 `불가`로 시작하면 `UNAVAILABLE`, 나머지는 `UNKNOWN`으로 응답하되 원문은 항상 `note`에 보존한다. `detailIntro2`에는 휠체어 필드가 없어 현재 범위에서 제외한다. 소개정보를 정상 수집한 시각은 `intro_hydrated_at`에 기록한다.

## staging과 재실행

- 각 실행은 `sync_run` 한 건을 생성한다.
- 목록 원문 JSON은 `tourist_spot_staging.raw_payload`에 저장한다.
- 선정 분류, 필수값, 좌표가 정상인 건만 `PROMOTED` 상태로 승격한다.
- 원문 SHA-256 해시와 저장된 분류가 같더라도 `detail_hydrated_at`이 비어 있으면 공통 상세·이미지를 요청해 목록만 적재된 기존 장소를 보강한다.
- 기존 장소에 공통 상세정보는 있고 `intro_hydrated_at`만 비어 있으면 `detailIntro2`만 요청해 운영·시설정보를 보강한다.
- HTTP 429 또는 TourAPI 제한 초과 코드 `22`가 반환되면 반복 실패를 만들지 않고 동기화를 즉시 중단한다. 제한 해제 후 같은 범위로 재실행하면 `detail_hydrated_at`과 `intro_hydrated_at`이 비어 있는 장소만 다시 보강한다.
- 해시 또는 분류가 달라지면 동일 관광지를 신규 삽입하지 않고 갱신한다.
- 검증·외부 API·승격 오류는 `sync_error`에 기록한다.

# TourAPI 국문 서비스 필드 매핑

검증 기준 서비스는 `https://apis.data.go.kr/B551011/KorService2`다. 2026-09-02 실제 응답으로 요청 파라미터와 필드를 확인했다.

## 사용하는 기능

| 기능 | Operation | 기능 파라미터 |
|---|---|---|
| 광역·시군구 법정동 코드 | `ldongCode2` | 하위 지역 조회 시 `lDongRegnCd` |
| 지역 기반 관광지·문화시설 | `areaBasedList2` | `areaCode`, `contentTypeId`, `arrange=C` |
| 관광지 공통 상세 | `detailCommon2` | `contentId` |
| 관광지 이미지 | `detailImage2` | `contentId` |

모든 요청에는 `serviceKey`, `MobileOS`, `MobileApp`, `_type=json`, `pageNo`, `numOfRows`를 공통으로 전달한다. 현재 `detailImage2`에는 과거 파라미터인 `imageYN`, `subImageYN`을 전달하지 않는다.

스탬프 서비스의 수집 허용 유형은 관광지 `12`와 문화시설 `14`다. 두 유형을 각각 `contentTypeId`로 지정해 조회한다. 행사·축제 `15`, 여행코스 `25`, 레포츠 `28`, 숙박 `32`, 쇼핑 `38`, 음식점 `39`은 요청하지 않으므로 staging과 서비스 테이블에도 새로 유입되지 않는다.

## 관광지 매핑

| TourAPI 필드 | DB 필드 | 처리 |
|---|---|---|
| `contentid` | `source_content_id` | `source=TOUR_API`와 함께 유일 키 |
| `contenttypeid` | `source_content_type_id` | 원본 문자열 유지 |
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

## staging과 재실행

- 각 실행은 `sync_run` 한 건을 생성한다.
- 목록 원문 JSON은 `tourist_spot_staging.raw_payload`에 저장한다.
- 필수값과 좌표가 정상인 건만 `PROMOTED` 상태로 승격한다.
- 원문 SHA-256 해시가 같으면 상세·이미지를 다시 요청하지 않는다.
- 해시가 달라지면 동일 관광지를 신규 삽입하지 않고 갱신한다.
- 검증·외부 API·승격 오류는 `sync_error`에 기록한다.

# 다녀감 백엔드

Spring Boot 기반 다녀감 백엔드 프로젝트다.

## 현재 단계

프론트 연결 전 백엔드 준비를 완료했다. 카카오 Local 실연동, 분리 MySQL 통합 테스트, 세부 분류 기반 전국 TourAPI 카탈로그 적재, Swagger/OpenAPI 계약 생성을 검증했다.

- Spring Boot 3.5.16
- Java 17
- Gradle 8.14.4 Wrapper
- Web, Validation, Actuator
- Spring Data JPA, MySQL Connector/J
- Flyway MySQL 마이그레이션
- 로컬 MySQL 8.4 Docker Compose
- 한국관광공사 국문 관광정보 `KorService2` 클라이언트
- 역사유적·역사유물·선정 문화시설 및 대표 이미지 조회
- 관광지별 운영시간·휴무일·시설정보, 상세 이미지와 저작권 유형 조회
- 공통 성공·오류 JSON 응답과 필드 검증 오류
- 안전한 `X-Request-Id` 전달·생성 및 MDC 정리
- 지역·관광지·이미지 JPA 엔티티와 Repository
- Hibernate Spatial 기반 WGS84 `POINT SRID 4326` 매핑
- TourAPI 지역·관광지·문화시설·상세·이미지 수집 파이프라인
- 원본 JSON staging, SHA-256 변경 감지, 동기화 이력과 오류 기록
- 동일 데이터 건너뛰기와 변경 데이터 갱신
- 지역 계층 목록 API
- MySQL 공간 인덱스를 사용하는 지도 bounds 경량 조회 API
- 관광지 상세·이미지·내비 목적지 조회 API
- 지도 요청 범위·유형·최대 결과 수 검증
- 256비트 난수 기반 익명 세션과 SHA-256 토큰 해시 저장
- HttpOnly·SameSite 쿠키 발급과 유효 세션 재사용
- 지도·상세 응답의 `UNKNOWN`·`NOT_VISITED`·`VISITED` 상태 결합
- 사용자별 응답의 `private, no-store` 캐시 분리
- 서버 Haversine 거리 기반 GPS 스탬프 판정
- 인증 시도와 최초 방문의 원자적 저장
- UUID 멱등 키 재시도 및 Actor 행 잠금 기반 동시성 제어
- Actor 기준 분당 인증 제한과 브라우저 Origin 검증
- 지역별 전체·방문·미방문 도감 조회
- 활성 스탬프 대상 기준 지역 진행률 집계
- 스탬프 대상 0개 지역의 안전한 0% 처리
- Swagger UI와 JSON/YAML OpenAPI 계약
- 2KB 이상 JSON 응답의 서버 압축
- API 보안 헤더, 오류 응답 `no-store`, 익명 세션 발급·API IP 요청 제한
- 운영 Swagger 비활성화와 `prod` 프로필 보안 설정 검증
- 운영 Flyway 마이그레이션 계정과 애플리케이션 런타임 계정 분리
- 로컬 MySQL 포트의 loopback(`127.0.0.1`) 전용 바인딩

설계서의 권장 버전은 Java 21이지만 현재 개발 PC에는 Java 17만 설치되어 있어, 먼저 실행 가능한 Java 17로 구성했다. Java 21 설치 후 `build.gradle`의 toolchain 값을 21로 변경할 수 있다.

## 로컬 실행

Docker Desktop을 실행한 뒤 다음 명령을 사용한다.

```powershell
Copy-Item .env.example .env
docker compose up -d mysql
$env:SPRING_PROFILES_ACTIVE='local'
.\gradlew.bat bootRun
```

Flyway가 최초 실행 시 `src/main/resources/db/migration`의 V1 초기 스키마와 V2 관광 세부 분류 마이그레이션을 순서대로 적용한다.
프로젝트 루트의 `.env`는 Spring 설정에서 선택적으로 불러오며 Git 추적 대상에서 제외된다.

기본 포트는 `8080`이며 헬스체크는 다음 경로에서 확인한다.

```text
GET http://localhost:8080/actuator/health
```

## 테스트

```powershell
.\gradlew.bat test
```

### 2단계 검증 결과

- Gradle 테스트 성공
- 분리된 임시 MySQL 8.0.44에서 Flyway V1·V2 적용 성공
- 애플리케이션 시작 시 14개 서비스 테이블과 `flyway_schema_history` 생성 확인
- `tourist_spot.location`의 SRID 4326 및 SPATIAL 인덱스 확인
- DB 연결 상태에서 `/actuator/health` 응답 `UP` 확인
- Docker Compose 설정 구문 검증 성공

### TourAPI 검증 결과

- 목록·대표 이미지 응답 파싱 계약 테스트 성공
- 상세 이미지·저작권 유형 응답 파싱 계약 테스트 성공
- 빈 이미지 및 TourAPI 오류 응답 처리 테스트 성공
- 실제 발급 키로 `areaBasedList2` 관광지 목록과 대표 이미지 조회 성공
- 실제 발급 키로 `detailImage2` 원본·썸네일 이미지 조회 성공
- 현재 `detailImage2` 명세에 맞춰 `contentId`만 기능 파라미터로 전달

실제 API 테스트는 명시적으로 활성화할 때만 실행한다.

```powershell
$env:RUN_LIVE_TOUR_API_TESTS='true'
.\gradlew.bat test --tests com.danyeogam.backend.tourapi.TourApiLiveTest --rerun-tasks
```

### 프론트 연결 준비 1단계 검증 결과

- 기본 회귀 테스트 19건 중 17건 통과, 외부 연동용 2건은 기본 실행에서 의도적으로 제외
- 제외된 실제 TourAPI 테스트와 실제 MySQL 통합 테스트도 각각 별도 실행해 성공
- 공통 성공 응답의 `data`, `meta.requestId`, `meta.generatedAt` 확인
- 오류 응답의 고정 `error.code`, 재시도 여부, 필드 오류 확인
- 안전하지 않은 요청 ID 교체 및 응답 헤더·본문 일치 확인
- 잘못된 JSON과 미존재 리소스의 400·404 처리 확인
- 지역 계층, 데이터 해시, 이미지 순서, WGS84 SRID 도메인 규칙 확인
- 임시 MySQL 8.0.44에서 Flyway 14개 테이블 및 JPA 스키마 검증 성공
- 실제 MySQL에서 관광지 `POINT SRID 4326`와 이미지 Repository 저장·조회 성공
- `sx_tourist_spot_location` 공간 인덱스 확인
- 실행 가능한 Spring Boot JAR 생성 성공
- 외부 HTTP 오류의 원본 URL·서비스 키가 상위 예외에 남지 않도록 차단

### 프론트 연결 준비 2단계 검증 결과

- 기본 회귀 테스트 29건 중 26건 통과, 실패 0건, 외부 연동 3건은 기본 실행에서 의도적으로 제외
- 실제 TourAPI와 임시 MySQL을 함께 사용한 적재 파이프라인 테스트 별도 성공
- 서울 관광지·문화시설 12건의 원본 JSON, 좌표, 상세정보와 사용 가능한 이미지 적재 확인
- 첫 실행: 신규 12건, 처리 실패 0건
- 동일한 두 번째 실행: 신규·갱신 0건, 상세·이미지 재호출 생략 확인
- 해시를 변경한 세 번째 실행: 신규 0건, 해당 관광지 1건만 갱신 확인
- 세 번의 실행에서 staging 36건 모두 `PROMOTED`, JSON 유효성 통과, `sync_error` 0건
- 상세 이미지가 비어 있을 때 대표 이미지 fallback 저장 확인
- 사진이 전혀 없는 관광지는 오류 없이 빈 이미지 상태로 적재
- 상세 설명의 HTML 제거와 HTTP(S) 홈페이지 주소만 저장하는 보안 테스트 통과

### 프론트 연결 준비 3단계 검증 결과

- `GET /api/v1/regions` 지역 계층 목록 구현
- `GET /api/v1/tourist-spots` 지도 bounds 경량 조회와 유형 필터 구현
- `GET /api/v1/tourist-spots/{spotId}` 관광지 상세·이미지·내비 좌표 구현
- 잘못된 좌표, 과도한 범위·밀집도, 잘못된 유형, 미존재 관광지 오류 코드 검증
- 임시 MySQL 8.0.44에서 실제 HTTP 응답과 WGS84 좌표축 검증 성공
- `EXPLAIN`으로 `sx_tourist_spot_location` 공간 인덱스 사용 확인
- 지도 응답에서 MySQL 지리좌표의 부동소수 오차를 소수 7자리로 정규화

프론트 전달용 요청·응답 계약은 `docs/frontend-read-api.md`에 정리했다.

### 프론트 연결 준비 4단계 검증 결과

- `POST /api/v1/sessions/anonymous` 구현
- 원문 토큰은 쿠키로만 전달하고 DB에는 SHA-256 해시 32바이트만 저장
- 유효한 쿠키 재호출 시 기존 Actor·세션 재사용 및 중복 생성 방지 확인
- 만료·변조·형식 오류 토큰은 인증에 사용하지 않음
- 세션 쿠키에 `HttpOnly`, `SameSite=Lax`, `Path=/api`, `Max-Age` 적용
- 운영 기본 `Secure=true`, 로컬 프로필만 `Secure=false`
- 세션 없는 조회는 `UNKNOWN`, 유효 세션은 방문 기록에 따라 `NOT_VISITED`·`VISITED` 반환
- 방문 상태가 포함된 지도·상세 응답은 `private, no-store`로 공개 캐시와 분리
- 임시 MySQL 8.0.44에서 발급·재사용·방문 전후·만료 시나리오 통합 검증 성공

세션 연동 계약은 `docs/frontend-session-api.md`에 정리했다.

### 프론트 연결 준비 5단계 검증 결과

- `POST /api/v1/stamp-verifications` 구현
- 최초 성공, 이미 획득, 반경 밖, GPS 정확도 부족, 오래된 위치, 인증 중단 상태 구현
- 정확한 GPS 원 좌표를 DB나 로그에 저장하지 않고 거리·정확도·판정 결과만 저장
- 동일 멱등 키 재전송 시 기존 결과 반환 및 중복 시도·방문 생성 방지
- 같은 Actor의 동시 요청을 DB 비관적 잠금으로 직렬화
- `UNIQUE(actor_id, tourist_spot_id)`와 함께 최초 방문 1회 보장
- Actor 기준 기본 분당 5회 제한 및 기존 멱등 재시도는 제한 전에 반환
- 변경 요청 Origin 검증과 정확한 허용 오리진 credentials CORS 지원
- 임시 MySQL 8.0.44에서 전체 상태·멱등성·동시 요청 통합 검증 성공

GPS 인증 계약은 `docs/frontend-stamp-api.md`에 정리했다.

### 프론트 연결 준비 6단계 검증 결과

- `GET /api/v1/me/collection` 지역 도감과 `ALL`·`VISITED`·`NOT_VISITED` 필터 구현
- `GET /api/v1/me/collection/summary` 광역 지역별 진행률 구현
- 광역 지역에 직접 연결된 관광지와 하위 시군구 관광지를 함께 집계
- 일반·비활성·스탬프 중단 관광지를 현재 분모에서 제외
- 미방문 항목의 `verifiedAt=null`과 방문 항목의 최초 인증 시각 확인
- 대상 0개 지역도 `0/0`, 진행률 0%로 안정적으로 반환
- 모든 도감 응답을 `private, no-store`로 설정하고 세션 없이는 401 반환
- 임시 MySQL 8.0.44에서 GPS 획득 후 도감·필터·진행률 통합 검증 성공

도감 계약은 `docs/frontend-collection-api.md`에 정리했다.

### 프론트 연결 준비 7단계 검증 결과

- TourAPI 좌표가 정상일 때 그대로 유지하고 좌표가 누락·이상인 경우에만 주소 기반 카카오 보정 시도
- 선정 정책과 좌표 검증을 모두 통과한 TourAPI 장소만 `STAMP_TARGET`으로 활성화하고 운영 GPS 기본 반경 적용
- 보정 좌표 출처를 `KAKAO_GEOCODE`로 분리 저장하는 staging·관광지 승격 경로 구현
- `POST /api/v1/geo/reverse` 현재 위치 역지오코딩 구현
- `GET /api/v1/tourist-spots/{spotId}/parking` 카카오 `PK6` 주변 주차장 후보 조회 구현
- 카카오 주차장의 `publicVerified=false`, WGS84 내비 좌표, 조회 시각 반환
- 키 누락·외부 장애 시 역지오코딩은 `503 GEO_PROVIDER_UNAVAILABLE`, 주차장은 `temporarilyUnavailable=true`로 상세 화면과 장애 격리
- 키 없이 서버가 시작되며 정상 TourAPI 좌표 데이터 동기화는 카카오와 무관하게 계속 진행
- 근접 역지오코딩과 동일 관광지 주차장 결과의 짧은 인메모리 캐시 적용
- 실제 키 대신 가짜 HTTP 응답으로 인증 헤더·x/y 좌표축·빈 결과·장애 상태 자동 검증

위치·주차장 계약은 `docs/frontend-geo-parking-api.md`에 정리했다.

실제 카카오 키를 입력한 뒤 다음 선택 테스트를 실행한다.

```powershell
$env:RUN_LIVE_KAKAO_API_TESTS='true'
.\gradlew.bat test --tests com.danyeogam.backend.kakao.KakaoLocalLiveTest --rerun-tasks
```

검증용 서버와 임시 DB는 확인 후 종료·삭제했다.

2026-09-05 카카오 REST API 키를 로컬 `.env`에 적용하고 실연동 테스트를 완료했다.
주소→좌표 변환, 좌표→주소 변환, `PK6` 주변 주차장 검색이 모두 정상 동작했으며
키 값은 소스, 문서, 테스트 결과에 기록하지 않는다.

### 최종 백엔드 단독 검증 결과

- 2026-09-09 기본 회귀 테스트 총 102건 중 92건 통과, 실패·오류 0건, 외부 선택 테스트 10건 제외
- 실제 MySQL 관광지 선정·비활성화 통합 테스트 2건 별도 통과
- 실제 MySQL에서 KST·UTC가 섞인 과거 인증 시도가 1분 제한에 영구 포함되지 않는 회귀 테스트 통과
- Flyway V1·V2, Repository, 공간 인덱스, 세션, GPS 인증, 도감, 실제 TourAPI·카카오 호출 검증
- TourAPI 세부 분류에서 역사유적 `HS01`, 역사유물 `HS02`, 박물관 `VE070100`, 기념관 `VE070200`, 미술관·화랑 `VE070600`만 선정
- 지역 284건, 활성 스탬프 대상 3,885건, 대표 원본·썸네일 보유 3,629건 확인
- TourAPI 좌표 3,884건, 카카오 주소 보정 좌표 1건 확인
- 기존 비선정 11,450건은 삭제하지 않고 비활성화하고, 활성 대상만 담은 독립 복원용 데이터 덤프를 생성
- `/v3/api-docs`, `/v3/api-docs.yaml`, `/swagger-ui.html` 응답 및 9개 API 경로 검증
- 동시 요청 20개 부하, GPS 멱등 동시성, 전국 16개 광역 도감 합계와 gzip 응답 압축 검증

전국 적재의 제외 사유와 범위는 `docs/tourapi-full-load-report.md`, Swagger 사용법은 `docs/openapi-guide.md`, 프론트 연결 전 최종 검증은 `docs/pre-frontend-validation-report.md`에 정리했다.

## TourAPI 적재 실행

`.env`에서 실행 범위를 지정한다. `TOUR_SYNC_ENABLED=true`로 서버를 한 번 실행한 뒤 다시 `false`로 되돌려 일반 실행마다 작업이 시작되지 않게 한다. 동기화 대상은 코드 정책으로 역사유적 `HS01`, 역사유물 `HS02`, 박물관 `VE070100`, 기념관 `VE070200`, 미술관·화랑 `VE070600`만 허용한다.

```text
TOUR_SYNC_ENABLED=true
TOUR_SYNC_AREA_CODE=
TOUR_SYNC_PAGE_SIZE=1000
TOUR_SYNC_MAX_PAGES=3
TOUR_SYNC_HYDRATE_DETAILS=false
```

```powershell
$env:SPRING_PROFILES_ACTIVE='local'
.\gradlew.bat bootRun
```

위 값은 전국 목록만 갱신하는 권장 예다. 빈 `TOUR_SYNC_AREA_CODE`는 전국을 뜻하며, 모든 선정 조건의 마지막 페이지까지 읽었을 때만 기존 비선정 TourAPI 행을 비활성화한다. 특정 지역이나 제한된 페이지로 실행하면 전역 비활성화는 하지 않는다. 장소별 상세 설명, 소개정보와 추가 이미지 전체 수집은 호출량을 확인한 뒤 `TOUR_SYNC_HYDRATE_DETAILS=true`로 증분 실행한다. 목록 원문이 변경되지 않았더라도 소개정보 미수집 장소에는 `detailIntro2`만 호출해 보강한다. 선정 정책은 `docs/tourist-spot-selection-policy.md`, 필드 매핑은 `docs/tourapi-field-mapping.md`에 기록했다.

## 다음 단계

백엔드 단독 검증과 로컬 프론트 연결 준비를 완료했다. 이후에는 실제 화면에서 지도·도감 흐름을 확인하고, 배포 환경의 MySQL·HTTPS·CORS·Secret을 설정한 뒤 실제 기기 GPS 인증을 점검한다.

## 운영 환경변수

운영에서는 `application-local.yml` 값을 사용하지 않고 `.env.production.example`을 기준으로 환경변수를 주입한다. 비밀번호와 API 키는 Secret으로 관리한다.
반드시 `SPRING_PROFILES_ACTIVE=prod`로 실행하며, 운영 보안 검증에 실패하면 서버가 시작되지 않는다.

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
FLYWAY_DATABASE_USERNAME
FLYWAY_DATABASE_PASSWORD
DATABASE_POOL_MAX_SIZE
DATABASE_POOL_MIN_IDLE
SERVER_PORT
SERVER_COMPRESSION_ENABLED
SERVER_COMPRESSION_MIN_RESPONSE_SIZE
SERVER_MAX_HTTP_REQUEST_HEADER_SIZE
TOUR_API_BASE_URL
TOUR_API_SERVICE_KEY
TOUR_API_MOBILE_OS
TOUR_API_MOBILE_APP
TOUR_API_CONNECT_TIMEOUT
TOUR_API_READ_TIMEOUT
KAKAO_REST_API_KEY
KAKAO_LOCAL_BASE_URL
KAKAO_CONNECT_TIMEOUT
KAKAO_READ_TIMEOUT
MAP_QUERY_MAX_LATITUDE_SPAN
MAP_QUERY_MAX_LONGITUDE_SPAN
MAP_QUERY_MAX_RESULTS
GEO_REVERSE_CACHE_TTL
GEO_REVERSE_CACHE_MAX_ENTRIES
PARKING_DEFAULT_RADIUS_METERS
PARKING_MAX_RADIUS_METERS
PARKING_DEFAULT_LIMIT
PARKING_MAX_LIMIT
PARKING_CACHE_TTL
PARKING_CACHE_MAX_ENTRIES
SESSION_COOKIE_NAME
SESSION_COOKIE_PATH
SESSION_TTL
SESSION_TOUCH_INTERVAL
SESSION_COOKIE_SECURE
STAMP_DEFAULT_RADIUS_METERS
STAMP_MAX_ACCURACY_METERS
STAMP_MAX_LOCATION_AGE
STAMP_MAX_FUTURE_SKEW
STAMP_MAX_ATTEMPTS_PER_MINUTE
CORS_ALLOWED_ORIGINS
HSTS_MAX_AGE
API_RATE_LIMIT_ENABLED
API_RATE_LIMIT_REQUESTS_PER_MINUTE
API_RATE_LIMIT_SESSION_CREATES_PER_MINUTE
API_RATE_LIMIT_MAX_TRACKED_CLIENTS
```

운영 DB 계정에는 애플리케이션 실행에 필요한 최소 권한만 부여하고, `prod` 프로필의 별도 Flyway 계정으로 스키마를 변경한다.
구체적인 배포 보안 체크리스트와 프록시 헤더 예시는 `docs/security-hardening.md`를 따른다.

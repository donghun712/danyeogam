# 다녀감 DB

## 파일

- `danyeogam_schema.sql`: MySQL 8.4용 독립 실행형 초기 스키마
- `danyeogam_tour_seed.sql`: 2026-09-08 역사·문화 스탬프 카탈로그 데이터 덤프

`danyeogam_tour_seed.sql` SHA-256: `fde41f01e830da7409e03edb7e7c991541145a7a084b6993e2a47a52dd5fe520`

## 생성되는 범위

- 지역과 관광지 공간 데이터
- 익명 세션과 향후 회원 연결 구조
- GPS 인증 시도와 최초 방문 스탬프
- P1 도감 집계 기반과 칭호
- TourAPI staging, 동기화 실행 이력, 오류 이력

두 SQL 파일 모두 API 키를 포함하지 않는다. 데이터 덤프에는 `region`과 `tourist_spot` 데이터가 포함되며 대표 이미지 URL과 스탬프 대상 설정은 관광지 행에 저장되어 있다. 세션, 방문, 인증 시도, staging, 동기화 이력은 포함하지 않는다.

## 백엔드에서 사용하는 권장 복원 순서

빈 `danyeogam` 데이터베이스를 만든 뒤 백엔드를 한 번 실행해 Flyway V1·V2 스키마를 적용한다. 백엔드를 종료하거나 외부 요청을 받지 않는 상태에서 데이터 덤프를 넣는다.

```powershell
mysql --default-character-set=utf8mb4 -u root -p --database=danyeogam -e "SOURCE C:/관광데이터/db/danyeogam_tour_seed.sql"
```

이 순서로 복원하면 `flyway_schema_history`가 유지되어 백엔드가 다음 실행에서도 정상 기동한다. 이미 `danyeogam_schema.sql`로 만든 비어 있지 않은 DB에는 Flyway 이력이 없으므로 백엔드를 바로 연결하지 않는다.

`danyeogam_schema.sql`은 백엔드 없이 DB 구조만 독립적으로 만들 때 사용한다. 이 파일은 `danyeogam` 데이터베이스를 생성한다. 운영 DB 계정에 `CREATE DATABASE` 권한이 없다면 관리자가 데이터베이스를 먼저 만들고 DDL을 배포 환경에 맞게 분리한다.

## 확인

```sql
USE danyeogam;

SELECT version, description, applied_at
FROM schema_metadata;

SHOW TABLES;

SHOW INDEX FROM tourist_spot;
```

`tourist_spot`의 `sx_tourist_spot_location`이 `SPATIAL` 인덱스로 생성되어야 한다.

## 검증 결과

2026-08-29에 기존 사용자 DB와 분리한 임시 MySQL 8.0.44 인스턴스에서 전체 SQL을 실행해 다음을 확인했다. 스키마의 운영 목표 버전은 MySQL 8.4다.

- 14개 테이블 생성 성공
- `tourist_spot.location`: `POINT`, SRID 4326, `NOT NULL`
- `sx_tourist_spot_location`: `SPATIAL` 인덱스 생성
- WGS84 테스트 관광지의 Bounding Box 조회 성공
- `ST_Distance_Sphere` 거리 계산 성공
- 같은 Actor와 관광지의 방문 재삽입 시 `visit`이 1건으로 유지됨
- `verification_attempt`에 위도·경도·POINT 컬럼이 존재하지 않음

2026-09-05에는 별도 검증 DB에 스키마와 당시 전국 원본 데이터 덤프를 순서대로 복원했다.

2026-09-08에는 세부 분류 정책에 맞춰 역사유적 2,376건, 역사유물 398건, 박물관 594건, 기념관 155건, 미술관·화랑 362건을 담은 덤프를 새로 만들었다. 별도 검증 DB에 V1·V2를 적용한 다음 덤프를 복원했으며 지역 284건, 장소 3,885건, 정책 위반 0건, 스탬프 상태 오류 0건, 공간 인덱스 1개를 확인했다.

## 중요한 정책

- `tourist_spot.location`은 WGS84, SRID 4326, `NOT NULL`이다.
- 스탬프 대상은 역사유적 `HS01`, 역사유물 `HS02`, 박물관 `VE070100`, 기념관 `VE070200`, 미술관·화랑 `VE070600`만 허용한다.
- 종교성지 `HS03`, 안보관광지 `HS04`, 랜드마크와 그 밖의 관광·문화 분류는 적재하지 않는다.
- 행사·축제 `15`, 여행코스 `25`, 레포츠 `28`, 숙박 `32`, 쇼핑 `38`, 음식점 `39`도 적재하지 않는다.
- 좌표가 없거나 의심스러운 원본은 `tourist_spot_staging`에서 정제한 뒤 승격한다.
- `visit(actor_id, tourist_spot_id)` 유니크 제약으로 장소당 최초 스탬프 한 번만 저장한다.
- `verification_attempt(actor_id, idempotency_key)` 유니크 제약으로 재전송을 안전하게 처리한다.
- 인증 요청의 정확한 GPS 좌표는 테이블에 저장하지 않는다. 계산된 거리, 기기 정확도, 결과, 측정 시각만 기록한다.
- 이동 경로 테이블은 개인정보 정책이 확정되지 않아 생성하지 않았다.
- GPS 반경, 허용 정확도, 측정 유효시간은 아직 TBD이므로 DB 기본값으로 고정하지 않는다.

## 배포 시 필요한 외부 정보

운영 배포 시 아래 값은 환경변수 Secret과 정책 설정으로 전달해야 한다.

- 한국관광공사 TourAPI 서비스 키
- 카카오 REST API 키
- 확정된 GPS 인증 반경·허용 정확도·측정 유효시간
- 필요하면 공영주차장 공공데이터 출처

키 원문은 SQL 파일이나 DB 일반 테이블에 저장하지 않고 배포 환경의 Secret으로 주입한다.

백엔드에서는 `V1__init_schema.sql`과 `V2__add_tour_classification.sql`을 사용한다. 운영 환경에서는 `CREATE DATABASE`와 `USE`를 인프라 설정과 분리한다.

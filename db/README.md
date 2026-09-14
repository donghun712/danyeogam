# 다녀감 DB

## 파일

- `danyeogam_schema.sql`: MySQL 8.4용 독립 실행형 초기 스키마
- `danyeogam_tour_seed.sql`: 2026-09-13 역사·문화 스탬프 카탈로그·상세정보 데이터 덤프

`danyeogam_tour_seed.sql` SHA-256: `0f9c3f0b5e16fcd7194e9b3bccc43580f0e377c8ed22a4f8f8f32b6ed3d9e6dc`

## 생성되는 범위

- 지역과 관광지 공간 데이터
- 익명 세션과 향후 회원 연결 구조
- GPS 인증 시도와 최초 방문 스탬프
- 익명 Actor별 관광지 즐겨찾기
- P1 도감 집계 기반과 칭호
- TourAPI staging, 동기화 실행 이력, 오류 이력

두 SQL 파일 모두 API 키를 포함하지 않는다. 데이터 덤프에는 활성 `region`, 활성 `tourist_spot`, 해당 장소의 `tourist_spot_image`, 지역별 `title_definition` 데이터가 포함된다. 관광지 3,885건 모두 공통 상세정보와 소개정보 수집 완료 시각이 저장되어 있으며 TourAPI 원본이 제공한 설명·운영시간·휴무일·시설정보와 상세 이미지가 포함된다. 전역 칭호 7건은 Flyway V5가 생성하므로 덤프에는 지역 칭호 16건만 포함한다. 세션, 방문, 인증 시도, 즐겨찾기, 칭호 획득, staging, 동기화 실행·오류 이력은 포함하지 않는다.

## 백엔드에서 사용하는 권장 복원 순서

빈 `danyeogam` 데이터베이스를 만든 뒤 백엔드를 한 번 실행해 Flyway V1~V6 스키마를 적용한다. 백엔드를 종료하거나 외부 요청을 받지 않는 상태에서 데이터 덤프를 넣는다.

Windows의 MySQL 클라이언트는 `SOURCE` 대상에 한글 경로가 있으면 파일을 열지 못할 수 있다. 아래처럼 영문 임시 경로를 사용하면 저장소 위치와 관계없이 안전하게 복원할 수 있다.

```powershell
$seedSourcePath = (Resolve-Path -LiteralPath '.\db\danyeogam_tour_seed.sql').Path
$seedImportPath = Join-Path ([IO.Path]::GetTempPath()) 'danyeogam_tour_seed.sql'
Copy-Item -LiteralPath $seedSourcePath -Destination $seedImportPath -Force
try {
    $mysqlSourcePath = $seedImportPath.Replace('\', '/')
    mysql --default-character-set=utf8mb4 -u root -p --database=danyeogam -e "SOURCE $mysqlSourcePath"
} finally {
    Remove-Item -LiteralPath $seedImportPath -Force -ErrorAction SilentlyContinue
}
```

Linux 서버에서는 저장소 루트에서 다음과 같이 복원할 수 있다.

```bash
mysql --default-character-set=utf8mb4 -u root -p danyeogam < db/danyeogam_tour_seed.sql
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

- 15개 테이블 생성 성공(즐겨찾기 테이블 포함)
- `tourist_spot.location`: `POINT`, SRID 4326, `NOT NULL`
- `sx_tourist_spot_location`: `SPATIAL` 인덱스 생성
- WGS84 테스트 관광지의 Bounding Box 조회 성공
- `ST_Distance_Sphere` 거리 계산 성공
- 같은 Actor와 관광지의 방문 재삽입 시 `visit`이 1건으로 유지됨
- `verification_attempt`에 위도·경도·POINT 컬럼이 존재하지 않음

2026-09-05에는 별도 검증 DB에 스키마와 당시 전국 원본 데이터 덤프를 순서대로 복원했다.

2026-09-08에는 세부 분류 정책에 맞춰 역사유적 2,376건, 역사유물 398건, 박물관 594건, 기념관 155건, 미술관·화랑 362건을 담은 덤프를 새로 만들었다. 별도 검증 DB에 V1·V2를 적용한 다음 덤프를 복원했으며 지역 284건, 장소 3,885건, 정책 위반 0건, 스탬프 상태 오류 0건, 공간 인덱스 1개를 확인했다.

2026-09-13에는 남은 상세정보 증분 수집을 완료한 뒤 덤프를 갱신했다. 활성 관광지 3,885건 모두 `detail_hydrated_at`과 `intro_hydrated_at`이 채워졌고 상세 설명 3,885건, 운영시간 3,712건, 휴무일 3,679건, 상세 이미지 25,835건을 확인했다. 운영시간·휴무일 건수 차이는 호출 실패가 아니라 TourAPI 원본의 미제공 항목이다. 새 빈 DB에 Flyway V1~V6를 적용한 후 이 덤프를 복원했으며 공간 인덱스, 칭호 23건, 사용자 데이터 미포함과 실제 상세·도감·칭호 API 기동을 확인했다.

## 중요한 정책

- `tourist_spot.location`은 WGS84, SRID 4326, `NOT NULL`이다.
- 스탬프 대상은 역사유적 `HS01`, 역사유물 `HS02`, 박물관 `VE070100`, 기념관 `VE070200`, 미술관·화랑 `VE070600`만 허용한다.
- 종교성지 `HS03`, 안보관광지 `HS04`, 랜드마크와 그 밖의 관광·문화 분류는 적재하지 않는다.
- 행사·축제 `15`, 여행코스 `25`, 레포츠 `28`, 숙박 `32`, 쇼핑 `38`, 음식점 `39`도 적재하지 않는다.
- 좌표가 없거나 의심스러운 원본은 `tourist_spot_staging`에서 정제한 뒤 승격한다.
- `visit(actor_id, tourist_spot_id)` 유니크 제약으로 장소당 최초 스탬프 한 번만 저장한다.
- `tourist_spot_favorite(actor_id, tourist_spot_id)` 유니크 제약으로 중복 즐겨찾기를 막는다.
- `verification_attempt(actor_id, idempotency_key)` 유니크 제약으로 재전송을 안전하게 처리한다.
- 칭호 조건은 `title_definition`에 저장하고 획득 결과는 `actor_title(actor_id, title_definition_id)` 유니크 제약으로 한 번만 부여한다.
- 문화 수집가는 실제 TourAPI 구조에 맞춰 `classification_level3`의 `VE070100`, `VE070200`, `VE070600`을 합산한다.
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

백엔드에서는 `V1__init_schema.sql`부터 `V6__update_title_schema_metadata.sql`까지 순서대로 사용한다. 운영 환경에서는 `CREATE DATABASE`와 `USE`를 인프라 설정과 분리한다.

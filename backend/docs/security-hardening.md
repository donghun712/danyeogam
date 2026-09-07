# 다녀감 운영 보안 설정

## 적용된 보호 장치

- 운영 기본값에서 Swagger UI와 OpenAPI HTTP 엔드포인트 비활성화
- Actuator는 상세정보 없는 `health`만 노출 (`local`에서만 `info` 추가)
- API 응답에 `nosniff`, 클릭재킹 차단, Referrer/Permissions Policy, API 전용 CSP 적용
- HTTPS 요청에는 1년 HSTS 적용 (`local`은 비활성화)
- 모든 API 오류 응답에 `Cache-Control: no-store` 적용
- 세션 쿠키는 `HttpOnly`, `Secure`(운영), `SameSite=Lax`, `Path=/api`
- 변경 요청은 동일 출처 또는 정확히 허용한 Origin만 수락
- IP별 일반 API 분당 600회, 익명 세션 발급 분당 20회 기본 제한
- 요청 ID는 허용 문자 64자까지만 수락하고 그 외 값은 서버 UUID로 교체
- 운영 시작 시 HTTPS 외부 API, Secure 쿠키, Swagger 차단, API 키와 DB 비밀번호 검증
- 로컬 Compose MySQL은 `127.0.0.1`에만 공개
- 로컬 백엔드와 Vite 개발 서버도 loopback에만 바인딩
- 운영 Flyway 계정과 런타임 DB 계정 분리 지원

## 운영 실행 원칙

운영 서버에는 `.env` 파일을 복사하지 않는다. 배포 플랫폼의 Secret 저장소로
`.env.production.example`의 값을 주입하고 `SPRING_PROFILES_ACTIVE=prod`로 실행한다.
프론트와 API는 가능하면 같은 사이트에서 `/api` reverse proxy로 제공한다. 분리 배포 시
`CORS_ALLOWED_ORIGINS`에는 쉼표로 구분한 정확한 HTTPS origin만 적고 와일드카드를 쓰지 않는다.

TLS 종료 프록시는 아래 헤더를 직접 설정한다. 애플리케이션의 HSTS는 `request.isSecure()`가
참일 때만 추가되므로, 프록시에서 TLS가 끝나는 구조에서는 프록시 HSTS가 기준이다.

```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "camera=(), microphone=(), payment=(), usb=()" always;
```

프론트 CSP는 카카오 지도 동적 리소스 때문에 처음에는 `Content-Security-Policy-Report-Only`로
실제 기기에서 점검한 뒤 강제한다. 시작 정책은 다음과 같다.

```text
default-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none';
script-src 'self' https://dapi.kakao.com https://*.kakaocdn.net https://*.daumcdn.net;
style-src 'self' 'unsafe-inline'; img-src 'self' data: blob: https:;
font-src 'self' data:; connect-src 'self' https://*.kakao.com https://*.kakaocdn.net;
form-action 'self'; upgrade-insecure-requests
```

애플리케이션의 IP 제한은 한 서버 프로세스 안에서만 공유되고, 보안상 `X-Forwarded-For`를
신뢰하지 않는다. 운영 프록시 또는 CDN에서도 세션 생성, 역지오코딩, 주차장 검색에 IP 제한을
추가하고 백엔드 포트를 인터넷에 직접 노출하지 않는다.

## 키와 데이터베이스

- 테스트에 사용한 TourAPI·카카오 REST 키는 배포 전에 재발급한다.
- 카카오 JavaScript 키는 브라우저에 공개되는 값이므로 운영 Web 도메인을 반드시 제한한다.
- `VITE_*` 변수에는 서버 비밀키를 넣지 않는다.
- 런타임 DB 계정은 서비스 테이블의 필요한 `SELECT`, `INSERT`, `UPDATE`, `DELETE`만 부여한다.
- Flyway 계정에만 스키마 변경 권한을 부여하고 두 계정의 비밀번호를 다르게 설정한다.
- 정기 백업, 복원 훈련, 보존 기간과 접근 로그 정책은 배포 플랫폼에서 별도로 설정한다.

## 배포 직전 확인

1. 새 키 발급 및 이전 테스트 키 폐기
2. 운영 도메인·HTTPS와 카카오 Web 도메인 등록
3. `prod` 프로필 시작 성공, `/swagger-ui.html`과 `/v3/api-docs`가 404인지 확인
4. 세션 쿠키의 `Secure`, `HttpOnly`, `SameSite=Lax`, `Path=/api` 확인
5. 허용되지 않은 Origin의 POST가 403인지 확인
6. 보안 헤더와 프록시 요청 크기·속도 제한 확인
7. 실제 휴대폰 GPS 스탬프와 카카오내비 확인
8. DB 백업에서 별도 환경 복원 확인

## 의존성 점검 기록 (2026-09-07)

- Spring Boot `3.5.7`에서 3.5 계열 최종 OSS 패치인 `3.5.16`으로 갱신
- jsoup `1.21.2`에서 `1.23.2`, springdoc `2.8.14`에서 `2.8.17`로 갱신
- 프론트 npm 운영·개발 의존성 audit 결과 취약점 0건
- 카카오 JavaScript SDK 2.7.1 파일에 SHA-384 SRI 적용

Spring Boot 3.5.x는 OSS 지원이 끝났으므로 다음 정기 작업에서 Boot 4.1.x 전환을 별도 브랜치로
검증한다. 현재 알려진 Spring Data JPA Sort 검증 우회 조건과 달리 이 서비스는 외부 입력
`Sort`/`Pageable`을 native query에 전달하지 않으며, 외부 입력을 SpEL 표현식으로 실행하지
않는다. 이 판단은 버전 고정을 대신하지 않으므로 배포 때마다 공식 보안 공지를 다시 확인한다.

# 프론트 전달용 OpenAPI 안내

백엔드 실행 기준 주소는 다음과 같다.

| 용도 | 주소 |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| OpenAPI YAML | `http://localhost:8080/v3/api-docs.yaml` |

서버를 실행하지 않고도 프론트 코드 생성과 계약 확인에 사용할 수 있도록 고정 산출물을 함께 제공한다.

- `docs/openapi.json`
- `docs/openapi.yaml`

문서에는 현재 구현된 12개 API 경로와 45개 스키마가 포함되어 있다. 익명 세션이 필요한 GPS 인증·도감·즐겨찾기·칭호 API는 `anonymousSession` 쿠키 보안 스키마로 표시했다. 브라우저에서는 먼저 `POST /api/v1/sessions/anonymous`을 호출하고 이후 요청에 credentials를 포함해야 한다.

프론트가 다른 포트에서 실행되면 운영 환경변수 `CORS_ALLOWED_ORIGINS`에 정확한 Origin을 등록한다. 쿠키 인증 요청에는 프론트의 `fetch` 기준 `credentials: 'include'`가 필요하다.

## 검증 결과

2026-09-11 로컬 MySQL에 연결한 실제 Spring Boot 서버에서 다음을 확인했다.

- `/swagger-ui.html`: HTTP 200
- `/v3/api-docs`: HTTP 200
- `/v3/api-docs.yaml`: HTTP 200
- 요구 API 경로 12개와 스키마 45개 노출
- 익명 세션 쿠키 보안 스키마 노출
- 즐겨찾기 경로와 도감 `parentRegionCode` 파라미터 노출
- 칭호 경로, 진행 상태 스키마와 스탬프 `newTitleIds` 노출

API 구현이 변경되면 서버의 `/v3/api-docs`와 `/v3/api-docs.yaml` 응답으로 두 고정 파일도 다시 생성해야 한다.

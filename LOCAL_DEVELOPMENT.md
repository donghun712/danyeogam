# 다녀감 로컬 개발 환경

이 저장소는 Docker 없이 로컬 MySQL 8.0을 사용하도록 준비되어 있다. 기존 3306 MySQL과 충돌하지 않도록 프로젝트 전용 DB는 `127.0.0.1:3307`에서 실행하며 데이터는 `.tmp/local-mysql-data`에 저장한다.

## 최초 실행

PowerShell 터미널 3개를 연다.

1. 프로젝트 전용 MySQL 시작

   ```powershell
   .\scripts\start-local-mysql.ps1
   ```

2. 백엔드 시작

   ```powershell
   .\scripts\start-backend.ps1
   ```

   처음 시작할 때 Flyway V1~V6가 자동 적용된다. `Started DanyeogamApplication` 로그가 나온 후 최초 한 번만 다른 터미널에서 시드를 넣는다.

   ```powershell
   .\scripts\import-local-seed.ps1
   ```

3. 프론트엔드 시작

   ```powershell
   .\scripts\start-frontend.ps1
   ```

브라우저 주소는 `http://localhost:5173`이며 Vite가 `/api` 요청을 `http://localhost:8080`으로 프록시한다.

## 점검 주소

- 프론트: `http://localhost:5173`
- 백엔드 헬스: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 테스트

```powershell
Set-Location backend
.\gradlew.bat test --rerun-tasks

Set-Location ..\danyeogam-frontend
npm run build
npm run lint
```

기본 테스트는 실제 TourAPI·Kakao 호출과 별도 MySQL 통합 테스트를 제외한다. 이 선택 테스트들은 API 호출량과 테스트 DB 초기화 영향을 확인한 뒤 명시적으로 실행한다.

## 종료

프론트와 백엔드는 각각 `Ctrl+C`로 종료한다. 이후 프로젝트 전용 MySQL을 종료한다.

```powershell
.\scripts\stop-local-mysql.ps1
```

기존 Windows 서비스 MySQL(3306)은 이 로컬 환경에서 변경하거나 중지하지 않는다.

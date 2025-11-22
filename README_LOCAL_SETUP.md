# 로컬 개발 환경 설정 가이드

## 📋 필수 사항

### 1. PostgreSQL 설치 및 실행
- PostgreSQL이 로컬에 설치되어 있어야 합니다.
- 기본 포트: `5432`
- 데이터베이스: `finalproject`
- 사용자: `postgres`
- 비밀번호: `rootroot` (또는 본인의 설정)

### 2. 환경변수 설정 (선택사항)
기본값이 설정되어 있으므로 별도 설정 없이도 작동합니다.

**Windows (PowerShell):**
```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="finalproject"
$env:DB_USER="postgres"
$env:DB_PASSWORD="rootroot"
```

**Windows (CMD):**
```cmd
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=finalproject
set DB_USER=postgres
set DB_PASSWORD=rootroot
```

**Linux/Mac:**
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=finalproject
export DB_USER=postgres
export DB_PASSWORD=rootroot
```

### 3. Spring Boot 실행
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

또는 IDE에서 `DementiaCareApplication.java` 실행 시:
- Active Profile: `dev`

## 🔧 작동 원리

### DataSource 자동 구성
`DataSourceConfig.java`가 환경에 따라 자동으로 DataSource를 구성합니다:

1. **로컬 환경** (`DB_HOST=localhost` 또는 기본값):
   - 일반 TCP 연결 사용
   - JDBC URL: `jdbc:postgresql://localhost:5432/finalproject`

2. **프로덕션 환경** (`DB_HOST=/cloudsql/...`):
   - Cloud SQL Unix Socket 연결 사용
   - Cloud SQL Socket Factory 자동 사용

### 환경별 설정
- **로컬**: `application-dev.properties` (HTTPS, localhost:5173)
- **프로덕션**: `application.properties` (HTTP, 환경변수 기반)

## ⚠️ 주의사항

1. **Cloud SQL Socket Factory 의존성**
   - `pom.xml`에 포함되어 있지만, 로컬에서는 사용되지 않습니다.
   - 로컬에서도 정상 작동합니다.

2. **환경변수 우선순위**
   - 환경변수 > `application-dev.properties` > `application.properties` > 기본값

3. **프론트엔드 설정**
   - 프론트엔드는 별도로 `VITE_API_BASE_URL` 환경변수 설정 필요
   - 로컬: `http://localhost:8080` (또는 `https://localhost:8080`)

## 🐛 문제 해결

### 데이터베이스 연결 실패
1. PostgreSQL이 실행 중인지 확인
2. 포트 5432가 사용 가능한지 확인
3. 데이터베이스 `finalproject`가 존재하는지 확인
4. 사용자 `postgres`의 비밀번호 확인

### 포트 충돌
- 기본 포트 8080이 사용 중이면 환경변수로 변경:
  ```bash
  export SERVER_PORT=8081
  ```

## 📝 체크리스트

- [ ] PostgreSQL 설치 및 실행
- [ ] 데이터베이스 `finalproject` 생성
- [ ] 사용자 `postgres` 비밀번호 확인
- [ ] 환경변수 설정 (선택사항)
- [ ] Spring Boot 실행 (`dev` 프로필)
- [ ] 프론트엔드 환경변수 설정


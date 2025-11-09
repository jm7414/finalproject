# 배포 가이드

## 📋 목차
1. [로컬 개발 환경 설정](#로컬-개발-환경-설정)
2. [서버 배포 준비](#서버-배포-준비)
3. [GitHub Secrets 설정](#github-secrets-설정)
4. [서버 초기 설정](#서버-초기-설정)
5. [배포 프로세스](#배포-프로세스)

---

## 로컬 개발 환경 설정

### 1. Backend 설정

#### `backend/src/main/resources/application-local.properties` 파일 생성
```properties
# Gemini API Key (로컬 개발용)
GEMINI_API_KEY=AIzaSyBX2ET5avOYYNDQgYcmCD_UBOL1vloK5HY

# 데이터베이스 (로컬 PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finalproject
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=rootroot

# 서버 포트
SPRING_SERVER_PORT=8080

# 도메인 (로컬 개발)
DOMAIN=localhost:5173

# Tmap API (로컬 개발용)
TMAP_API_KEY=3EkdQeGupP3Y0mJO1SuVC31mEYHXemG4hJMIET47
TMAP_API_BASE_URL=https://apis.openapi.sk.com/tmap
```

#### IntelliJ IDEA에서 실행 시
```
Run → Edit Configurations
→ Environment variables 추가:
  - Active profiles: local
  - Environment variables: GEMINI_API_KEY=AIzaSyBX2ET5avOYYNDQgYcmCD_UBOL1vloK5HY
```

### 2. Frontend 설정

#### `frontend/.env.development` 파일 생성
```bash
VITE_API_BASE_URL=https://localhost:8080
VITE_FASTAPI_URL=http://localhost:8000
VITE_GEMINI_API_KEY=AIzaSyBX2ET5avOYYNDQgYcmCD_UBOL1vloK5HY
VITE_TMAP_API_KEY=pu1CWi6rz48GHLWhk7NI239il6I2j9fHaSLFeYoi
VITE_VWORLD_API_KEY=6A0CFFEF-45CF-3426-882D-44A63B5A5289
VITE_KAKAO_JS_KEY=7e0332c38832a4584b3335bed6ae30d8
```

---

## 서버 배포 준비

### 1. Oracle Cloud 서버 준비
- Ubuntu 22.04 LTS
- Docker & Docker Compose 설치
- 포트 개방: 80, 443, 22

### 2. 도메인 DNS 설정
- `lx12mammamia.xyz` → 서버 IP
- `www.lx12mammamia.xyz` → 서버 IP

---

## GitHub Secrets 설정

### 필수 Secrets

1. **SERVER_HOST**: Oracle Cloud 서버 IP 주소
   ```
   예: 123.45.67.89
   ```

2. **SERVER_USER**: 서버 사용자명
   ```
   ubuntu
   ```

3. **SSH_PRIVATE_KEY**: SSH 개인키 전체 내용
   ```
   -----BEGIN OPENSSH PRIVATE KEY-----
   ...
   -----END OPENSSH PRIVATE KEY-----
   ```

4. **DOMAIN**: 도메인 이름
   ```
   lx12mammamia.xyz
   ```

5. **POSTGRES_PASSWORD**: 데이터베이스 비밀번호 (강력한 비밀번호)
   ```
   예: YourSecurePassword123!@#
   ```

6. **GEMINI_API_KEY**: Gemini API 키
   ```
   AIzaSyBX2ET5avOYYNDQgYcmCD_UBOL1vloK5HY
   ```

7. **TMAP_API_KEY**: Tmap API 키
   ```
   pu1CWi6rz48GHLWhk7NI239il6I2j9fHaSLFeYoi
   ```

8. **VWORLD_API_KEY**: VWorld API 키
   ```
   6A0CFFEF-45CF-3426-882D-44A63B5A5289
   ```

9. **KAKAO_JS_KEY**: Kakao JavaScript 키
   ```
   7e0332c38832a4584b3335bed6ae30d8
   ```

### GitHub Secrets 등록 방법
1. GitHub 저장소 → Settings → Secrets and variables → Actions
2. "New repository secret" 클릭
3. 위의 모든 Secrets 추가

---

## 서버 초기 설정

### 1. 서버 접속
```bash
ssh -i ~/.ssh/oracle-cloud-key.key ubuntu@<서버IP>
```

### 2. Docker & Docker Compose 설치
```bash
# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 재로그인
exit
ssh -i ~/.ssh/oracle-cloud-key.key ubuntu@<서버IP>
```

### 3. 작업 디렉토리 생성
```bash
mkdir -p /home/ubuntu/final-project
cd /home/ubuntu/final-project
```

### 4. SSL 인증서 초기 발급 (Let's Encrypt)
```bash
# Certbot 설치
sudo apt update
sudo apt install certbot -y

# 인증서 발급 (Nginx 없이 standalone 모드)
sudo certbot certonly --standalone -d lx12mammamia.xyz -d www.lx12mammamia.xyz

# 인증서 위치 확인
sudo ls -la /etc/letsencrypt/live/lx12mammamia.xyz/
```

---

## 배포 프로세스

### 자동 배포 (CI/CD)
1. `main` 브랜치에 `push`
2. GitHub Actions가 자동으로:
   - Frontend 빌드
   - 코드를 서버에 배포
   - Docker Compose로 서비스 재시작

### 수동 배포
```bash
# 서버 접속
ssh -i ~/.ssh/oracle-cloud-key.key ubuntu@<서버IP>
cd /home/ubuntu/final-project

# 코드 업데이트
git pull origin main

# .env 파일 확인/수정
nano .env

# Docker Compose 재시작
docker-compose down
docker-compose build --no-cache
docker-compose up -d

# 로그 확인
docker-compose logs -f
```

---

## 문제 해결

### 배포 실패 시
```bash
# 서버 로그 확인
docker-compose logs backend
docker-compose logs fastapi
docker-compose logs nginx

# 컨테이너 상태 확인
docker-compose ps

# 재시작
docker-compose restart backend
```

### SSL 인증서 갱신
```bash
# Certbot 컨테이너가 자동으로 갱신
# 수동 갱신이 필요한 경우:
docker-compose exec certbot certbot renew
docker-compose restart nginx
```

---

## 주의사항

1. **Gemini API 키**: 절대 Git에 올리지 마세요. Gemini에서 키를 차단합니다.
2. **.env 파일**: 서버에만 존재하며 Git에 올라가지 않습니다.
3. **application-local.properties**: 로컬 개발용이며 Git에 올라가지 않습니다.



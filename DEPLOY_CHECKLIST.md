# 🚀 배포 체크리스트

## ✅ 준비 완료 항목
- [x] GitHub Secrets 등록 완료
- [x] Docker & Docker Compose 설치 완료
- [x] SSL 인증서 발급 완료
- [x] 작업 디렉토리 생성 완료

## 📝 배포 전 최종 확인

### 1. 코드 커밋 및 Push
```bash
# 현재 브랜치 확인
git branch

# main 브랜치로 전환
git checkout main

# 변경사항 확인
git status

# 모든 변경사항 추가
git add .

# 커밋
git commit -m "feat: 배포 설정 완료 및 환경변수화"

# GitHub에 Push
git push origin main
```

### 2. GitHub Actions 확인
1. GitHub 저장소 → Actions 탭 이동
2. "Deploy to Oracle Cloud" 워크플로우 실행 확인
3. 각 단계별 성공 여부 확인:
   - ✅ Checkout code
   - ✅ Setup Node.js
   - ✅ Setup Java
   - ✅ Install Frontend dependencies
   - ✅ Build Frontend
   - ✅ Deploy to server
   - ✅ Run deployment script

### 3. 배포 실패 시 확인 사항

#### 서버 로그 확인
```bash
ssh -i ~/.ssh/oracle-cloud-key.key ubuntu@<서버IP>
cd /home/ubuntu/final-project

# Docker 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs backend
docker-compose logs fastapi
docker-compose logs nginx
docker-compose logs postgres
```

#### 일반적인 문제 해결

**문제 1: Backend가 시작되지 않음**
```bash
# 로그 확인
docker-compose logs backend

# 환경변수 확인
cat .env

# 수동 재시작
docker-compose restart backend
```

**문제 2: Frontend가 표시되지 않음**
```bash
# Nginx 로그 확인
docker-compose logs nginx

# Frontend 빌드 파일 확인
ls -la frontend/dist/

# Nginx 재시작
docker-compose restart nginx
```

**문제 3: 데이터베이스 연결 실패**
```bash
# PostgreSQL 로그 확인
docker-compose logs postgres

# 데이터베이스 연결 테스트
docker-compose exec postgres psql -U postgres -d finalproject -c "SELECT 1;"
```

**문제 4: SSL 인증서 문제**
```bash
# 인증서 확인
sudo ls -la /etc/letsencrypt/live/lx12mammamia.xyz/

# Nginx 설정 확인
docker-compose exec nginx nginx -t
```

### 4. 배포 성공 확인

#### 웹사이트 접속 테스트
1. **HTTPS 접속**: https://lx12mammamia.xyz
2. **www 접속**: https://www.lx12mammamia.xyz
3. **로그인 테스트**: 실제 계정으로 로그인 시도
4. **API 테스트**: 브라우저 개발자 도구에서 네트워크 요청 확인

#### 서비스 상태 확인
```bash
# 모든 서비스 실행 중인지 확인
docker-compose ps

# 예상 출력:
# NAME                STATUS          PORTS
# dementia_backend    Up X minutes    0.0.0.0:8080->8080/tcp
# dementia_fastapi    Up X minutes    0.0.0.0:8000->8000/tcp
# dementia_nginx      Up X minutes    0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
# dementia_postgres   Up X minutes    0.0.0.0:5432->5432/tcp
```

### 5. 배포 후 작업

#### 초기 데이터 확인
```bash
# 데이터베이스 연결
docker-compose exec postgres psql -U postgres -d finalproject

# 샘플 데이터 확인
SELECT * FROM users LIMIT 5;
SELECT * FROM role;
```

#### 업로드 디렉토리 권한 확인
```bash
# 업로드 디렉토리 확인
ls -la backend/uploads/

# 권한 수정 (필요시)
chmod -R 755 backend/uploads/
```

---

## 🎉 배포 완료!

배포가 성공적으로 완료되면:
- ✅ https://lx12mammamia.xyz 접속 가능
- ✅ 모든 서비스 정상 작동
- ✅ 자동 배포 파이프라인 작동 중

이제 `main` 브랜치에 push할 때마다 자동으로 배포됩니다!

---

## 📞 문제 발생 시

1. GitHub Actions 로그 확인
2. 서버 로그 확인 (`docker-compose logs`)
3. README_DEPLOY.md의 문제 해결 섹션 참고


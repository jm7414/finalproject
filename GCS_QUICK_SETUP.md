# 🚨 배포 환경 이미지 업로드 문제 해결 가이드

## 문제 상황
배포 환경에서 이미지 업로드 시 **500 에러**가 발생합니다.

## 원인
- 배포 환경에서 GCS(Google Cloud Storage)를 사용하도록 설정되어 있음
- 하지만 **GCS 버킷이 아직 생성되지 않았거나 권한이 설정되지 않음**

## ✅ 해결 방법 (5분 소요)

### 1단계: GCS 버킷 생성

**로컬 터미널 또는 Cloud Shell에서 실행:**

```bash
# 버킷 생성
gsutil mb -l asia-northeast3 gs://dementia-care-uploads

# 공개 읽기 권한 부여 (이미지 접근 가능하도록)
gsutil iam ch allUsers:objectViewer gs://dementia-care-uploads
```

### 2단계: Cloud Run Service Account에 GCS 권한 부여

**먼저 실제 Service Account 확인:**
```powershell
# Cloud Run 서비스가 사용하는 Service Account 확인
gcloud run services describe dementia-care-backend --region asia-northeast3 --format="value(spec.template.spec.serviceAccountName)"
```

**결과에 따라 다음 중 하나 실행:**

**방법 1: Service Account가 표시된 경우 (예: 123456789-compute@developer.gserviceaccount.com)**
```powershell
gcloud projects add-iam-policy-binding dementia-care-project --member="serviceAccount:실제_SERVICE_ACCOUNT_이메일" --role="roles/storage.objectAdmin"
```

**방법 2: Service Account가 없거나 기본값인 경우 (더 간단한 방법)**
```powershell
# 프로젝트 번호 확인
gcloud projects describe dementia-care-project --format="value(projectNumber)"

# 프로젝트 번호를 사용하여 기본 Compute Engine Service Account에 권한 부여
# (예: 프로젝트 번호가 123456789인 경우)
gcloud projects add-iam-policy-binding dementia-care-project --member="serviceAccount:123456789-compute@developer.gserviceaccount.com" --role="roles/storage.objectAdmin"
```

**방법 3: 가장 간단한 방법 - 버킷에 직접 권한 부여 (권장)**
```powershell
# 버킷에 Cloud Run의 기본 Service Account 권한 부여
# 먼저 프로젝트 번호 확인
$PROJECT_NUMBER = gcloud projects describe dementia-care-project --format="value(projectNumber)"
gcloud storage buckets add-iam-policy-binding gs://dementia-care-uploads --member="serviceAccount:$PROJECT_NUMBER-compute@developer.gserviceaccount.com" --role="roles/storage.objectAdmin"
```

### 3단계: 확인

```bash
# 버킷이 생성되었는지 확인
gsutil ls gs://dementia-care-uploads

# 결과: gs://dementia-care-uploads/ (빈 버킷)
```

### 4단계: 재배포 (자동)

다음 배포 시 자동으로 GCS를 사용합니다. 수동으로 재배포하려면:

```bash
# GitHub에서 main 브랜치에 push하면 자동 재배포
git push origin main
```

또는 GitHub Actions에서 수동 실행:
1. GitHub Repository → Actions 탭
2. "Deploy to Cloud Run" 워크플로우 선택
3. "Run workflow" 클릭

## 🧪 테스트

재배포 후:
1. 게시판에서 사진 업로드 테스트
2. 프로필 사진 변경 테스트
3. 업로드된 이미지가 정상 표시되는지 확인

## 📋 체크리스트

- [ ] GCS 버킷 생성 완료
- [ ] 공개 읽기 권한 설정 완료
- [ ] Cloud Run Service Account 권한 부여 완료
- [ ] 재배포 완료
- [ ] 이미지 업로드 테스트 성공

## ⚠️ 주의사항

- **버킷 이름**: `dementia-care-uploads` (변경 불가, 코드에 하드코딩됨)
- **리전**: `asia-northeast3` (서울)
- **권한**: 모든 사용자가 읽을 수 있도록 설정됨 (이미지 URL 접근용)

## 🐛 여전히 안 되면?

1. **Cloud Run 로그 확인:**
   ```bash
   gcloud run services logs read dementia-care-backend --region asia-northeast3 --limit 50
   ```

2. **에러 메시지 확인:**
   - "Bucket not found" → 버킷 생성 확인
   - "Permission denied" → Service Account 권한 확인
   - "Project not found" → 프로젝트 ID 확인

3. **환경변수 확인:**
   ```bash
   gcloud run services describe dementia-care-backend \
     --region asia-northeast3 \
     --format="value(spec.template.spec.containers[0].env)"
   ```

## 💡 참고

- **로컬 개발**: 여전히 로컬 파일 시스템 사용 (변경 없음)
- **배포 환경**: GCS 사용 (버킷 생성 후 자동 적용)
- **비용**: 월 5GB까지 무료, 초과 시 매우 저렴한 요금


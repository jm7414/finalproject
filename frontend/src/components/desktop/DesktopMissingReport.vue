<template>
  <div class="desktop-container">
    <div class="report-card">
      <div class="card-header">
        <div class="header-content">
          <h2 class="title">🚨 긴급 실종 신고</h2>
          <p class="subtitle">
            실종자의 정보를 상세하게 입력해주세요. 입력된 정보는 경찰 및 이웃에게 공유됩니다.
          </p>
        </div>
      </div>

      <div class="card-body">
        <div class="left-column">
          <div class="section-group">
            <label class="form-label">실종자 사진 <span class="required">*</span></label>
            
            <input 
              type="file" 
              ref="fileInput" 
              @change="handleFileChange" 
              accept="image/*" 
              style="display: none;" 
            />

            <div v-if="imagePreviewUrl" class="preview-container">
              <img :src="imagePreviewUrl" alt="미리보기" class="image-preview" />
              <button @click="removeImage" class="remove-btn">
                <i class="bi bi-x-lg"></i> 삭제
              </button>
            </div>
            
            <div v-else class="upload-box" @click="triggerFileInput">
              <div class="upload-content">
                <i class="bi bi-camera-fill upload-icon"></i>
                <span class="upload-text">사진 업로드</span>
                <span class="upload-subtext">클릭하여 선택하세요</span>
              </div>
            </div>
          </div>

          <div class="section-group">
            <div class="row">
              <div class="col-6">
                <label class="form-label">실종 날짜 <span class="required">*</span></label>
                <input type="date" v-model="missingDate" class="form-control" />
              </div>
              <div class="col-6">
                <label class="form-label">실종 시간 <span class="required">*</span></label>
                <input type="time" v-model="missingTime" class="form-control" />
              </div>
            </div>
          </div>

          <div class="section-group">
            <label class="form-label">신고자 연락처 <span class="required">*</span></label>
            <input type="tel" v-model="reporterContact" placeholder="010-1234-5678" class="form-control" />
          </div>

          <div class="info-box">
            <i class="bi bi-info-circle-fill"></i>
            <div>
              <strong>안내사항</strong>
              <p>입력된 실종 시간과 마지막 위치를 기반으로 AI가 예상 이동 경로를 분석합니다.</p>
            </div>
          </div>
        </div>

        <div class="right-column">
          <h3 class="column-title">상세 특징 정보</h3>
          
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">인상착의 (상의, 하의, 신발)</label>
              <input type="text" v-model="descAppearance" placeholder="예: 파란색 패딩, 검정 바지, 흰색 운동화" class="form-control" />
            </div>

            <div class="form-group">
              <label class="form-label">두발상태</label>
              <input type="text" v-model="descHair" placeholder="예: 백발, 짧은 스포츠 머리, 모자 착용" class="form-control" />
            </div>

            <div class="form-group">
              <label class="form-label">건강상태 / 병력</label>
              <input type="text" v-model="descHealth" placeholder="예: 치매 초기, 거동 불편, 지팡이 사용" class="form-control" />
            </div>

            <div class="form-group">
              <label class="form-label">소지품</label>
              <input type="text" v-model="descItems" placeholder="예: 검정 가방, 빨간색 지갑" class="form-control" />
            </div>

            <div class="form-group full-width">
              <label class="form-label">기타 특이사항</label>
              <textarea 
                v-model="descOther" 
                placeholder="자주 가는 장소나 평소 습관 등을 적어주세요." 
                class="form-control text-area"
              ></textarea>
            </div>
          </div>
        </div>
      </div>

      <div class="card-footer">
        <button @click="goBack" class="btn btn-secondary btn-lg">취소</button>
        <button 
          @click="submitReport" 
          class="btn btn-danger btn-lg submit-btn" 
          :disabled="isUploading"
        >
          <span v-if="isUploading" class="spinner-border spinner-border-sm me-2"></span>
          {{ isUploading ? '신고 접수 중...' : '🚨 실종 신고 완료' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import { usePostImageUpload } from '@/composables/usePostImageUpload';
import ConfirmModal from '@/components/ConfirmModal.vue'

const router = useRouter();
const route = useRoute();

// === 데이터 변수 ===
const patientNo = ref(null);
const missingDate = ref('');
const missingTime = ref('');
const reporterContact = ref('');
const photoFile = ref(null);
const imagePreviewUrl = ref(null);
const isUploading = ref(false);

// 특이사항 데이터
const descAppearance = ref('');
const descHair = ref('');
const descHealth = ref('');
const descItems = ref('');
const descOther = ref('');

// 이미지 업로드 composable
const { upload } = usePostImageUpload();
const fileInput = ref(null);

// === 초기화 ===
onMounted(() => {
  patientNo.value = route.params.id;
  if (!patientNo.value) {
    alert('신고 대상 환자 정보가 올바르지 않습니다.');
    router.back();
  } else {
    console.log('실종 신고 페이지 로드. 대상 환자 ID:', patientNo.value);
  }

  // 현재 날짜/시간 초기화
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');

  missingDate.value = `${year}-${month}-${day}`;
  missingTime.value = `${hours}:${minutes}`;
});

// === 이미지 관련 함수 ===
function triggerFileInput() {
  fileInput.value?.click();
}

function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;
  
  photoFile.value = file;
  if (imagePreviewUrl.value) {
    URL.revokeObjectURL(imagePreviewUrl.value);
  }
  imagePreviewUrl.value = URL.createObjectURL(file);
}

function removeImage() {
  photoFile.value = null;
  imagePreviewUrl.value = null;
  if (fileInput.value) fileInput.value.value = '';
}

// === 신고 제출 함수 ===
async function submitReport() {
  if (!patientNo.value) {
    alert('신고 대상 환자 정보가 없습니다.'); return;
  }
  
  if (!missingDate.value || !missingTime.value || !reporterContact.value) {
    alert('실종 날짜, 시간, 연락처는 필수 입력 항목입니다.'); return;
  }

  // JSON 데이터 구성
  const descriptionData = {
    appearance: descAppearance.value,
    hair: descHair.value,
    health: descHealth.value,
    items: descItems.value,
    other: descOther.value,
  };
  const descriptionString = JSON.stringify(descriptionData);

  isUploading.value = true;
  let uploadedImageUrl = null;

  try {
    // 1. 이미지 업로드
    if (photoFile.value) {
      uploadedImageUrl = await upload(photoFile.value);
    }
    
    // 2. 데이터 준비
    const reportData = {
      patientUserNo: patientNo.value,
      photoPath: uploadedImageUrl, 
      description: descriptionString,
      status: "실종",
      // reporterContact 등의 필드는 API 스펙에 맞춰 추가 전송 필요할 수 있음
    };

    // 3. API 호출
    const response = await axios.post('/api/missing-persons/report', reportData, {
      withCredentials: true
    });

    // ⭐ [핵심 수정] 백엔드에서 생성된 ID를 받아서 예상 위치 페이지로 이동
    // 백엔드 응답이 { missingPostId: 123 } 형태라고 가정 (안되면 response.data 확인 필요)
    const newMissingId = response.data.missingPostId || response.data; 

    alert('실종 신고가 성공적으로 접수되었습니다.\n예상 위치 분석 페이지로 이동합니다.');
    
    // 4. 웹용 예상 위치 페이지로 이동
    if (newMissingId) {
        router.replace({ 
            name: 'desktop-predict', 
            params: { id: newMissingId } 
        });
    } else {
        // ID를 못 받았을 경우 폴백 (보통 목록으로)
        router.push('/CommunityMissing'); 
    }

  } catch (error) {
    console.error("실종 신고 처리 중 오류 발생:", error);
    alert("실종 신고에 실패했습니다. 다시 시도해주세요.");
  } finally {
    isUploading.value = false;
  }
}

function goBack() {
  router.back();
}
</script>

<style scoped>
.desktop-container {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
  background-color: #f3f4f6;
  min-height: 100vh;
  font-family: 'Pretendard', sans-serif;
}

.report-card {
  width: 100%;
  max-width: 1000px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 헤더 */
.card-header {
  background-color: #fee2e2; /* 연한 빨강 */
  padding: 30px 40px;
  border-bottom: 1px solid #fecaca;
}

.title {
  font-size: 24px;
  font-weight: 800;
  color: #dc2626;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 15px;
  color: #7f1d1d;
  margin: 0;
}

/* 바디 (2단 레이아웃) */
.card-body {
  display: flex;
  padding: 40px;
  gap: 50px;
}

.left-column {
  flex: 1;
  min-width: 300px;
  border-right: 1px solid #e5e7eb;
  padding-right: 40px;
}

.right-column {
  flex: 1.5;
}

/* 공통 폼 스타일 */
.section-group {
  margin-bottom: 24px;
}

.column-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 24px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e5e7eb;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.required {
  color: #dc2626;
}

.form-control {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: #8E97FD;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

/* 사진 업로드 박스 */
.upload-box {
  width: 100%;
  aspect-ratio: 4/3;
  background-color: #f9fafb;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-box:hover {
  background-color: #f3f4f6;
  border-color: #9ca3af;
}

.upload-content {
  text-align: center;
  color: #6b7280;
}

.upload-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 8px;
}

.preview-container {
  position: relative;
  width: 100%;
  aspect-ratio: 4/3;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
}

/* 우측 그리드 폼 */
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.text-area {
  min-height: 120px;
  resize: vertical;
}

/* 안내 박스 */
.info-box {
  background-color: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.info-box i {
  color: #2563eb;
  font-size: 18px;
}

.info-box strong {
  display: block;
  color: #1e40af;
  font-size: 14px;
  margin-bottom: 4px;
}

.info-box p {
  margin: 0;
  color: #3b82f6;
  font-size: 13px;
}

/* 푸터 버튼 */
.card-footer {
  padding: 30px 40px;
  background-color: #f9fafb;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-lg {
  padding: 12px 30px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
}

.submit-btn {
  min-width: 200px;
}

/* 유틸리티 */
.row {
  display: flex;
  gap: 12px;
}
.col-6 {
  flex: 1;
}
.right-area-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}
.right-area-header h2 {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0;
}
.close-btn {
  background: none;
  border: none;
  font-size: 1.25rem;
  color: #6b7280;
  cursor: pointer;
  padding: 4px;
  line-height: 1;
}
.modern-btn {
  /* 상단 여백 및 크기 */
  margin-top: 8px;
  padding: 8px 16px;
  
  /* 색상 및 테두리 (기본 보라색 계열) */
  background: #667eea;
  border: none;
  border-radius: 20px;
  color: white;
  
  /* 폰트 설정 */
  font-weight: 600;
  font-size: 13px;
  
  /* 배치 (아이콘과 텍스트 간격) */
  align-items: center;
  gap: 6px;
  
  /* 동작 및 효과 */
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  
  /* 레이아웃 (파일 맨 아래에 정의됨 - 버튼이 꽉 차게 늘어남) */
  flex-grow: 1;
}
.modern-btn.active {
    /* 보라색 -> 연보라색 그라데이션 배경 */
    background: linear-gradient(135deg, #667eea 0%, #ae8ad1 100%);
    color: white;
    /* 그림자가 보라색 계열로 변경 */
    box-shadow: 0 4px 15px rgba(118, 75, 162, 0.3);
    border: none;
}
.modern-btn.active {
    /* 보라색 -> 연보라색 그라데이션 배경 */
    background: linear-gradient(135deg, #667eea 0%, #ae8ad1 100%);
    color: white;
    /* 그림자가 보라색 계열로 변경 */
    box-shadow: 0 4px 15px rgba(118, 75, 162, 0.3);
    border: none;
}
</style>
<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios'; // API 호출을 위해 추가

// 이미지 업로드 심부름꾼 불러오기 (경로는 실제 파일 위치에 맞게 수정)
import { usePostImageUpload } from '@/composables/usePostImageUpload'; 

const router = useRouter();
const { upload } = usePostImageUpload();

// --- 폼 데이터 ---
// 실종자 정보 (실제로는 이전 페이지에서 받아오거나 API로 조회)
const missingPerson = ref({
  name: '김○○',
  age: 75
});

const missingDateTime = ref(''); // 실종 일시
const description = ref('');    // 특이사항
const reporterContact = ref(''); // 신고자 연락처

// --- 이미지 관련 데이터 ---
const fileInput = ref(null);
const imagePreviewUrl = ref(null);
const uploadedImageUrl = ref(null);
const isUploading = ref(false);

// --- UI 액션 함수 ---
function goBack() {
  router.back();
}

function triggerFileInput() {
  fileInput.value.click();
}

async function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;

  imagePreviewUrl.value = URL.createObjectURL(file);
  isUploading.value = true;

  try {
    const imageUrl = await upload(file);
    uploadedImageUrl.value = imageUrl;
    console.log("실종자 사진 업로드 성공! 경로:", imageUrl);
  } catch (error) {
    alert("사진 업로드에 실패했습니다.");
    removeImage();
  } finally {
    isUploading.value = false;
  }
}

function removeImage() {
  imagePreviewUrl.value = null;
  uploadedImageUrl.value = null;
  if(fileInput.value) fileInput.value.value = '';
}

// --- 폼 제출 함수 ---
async function submitReport() {
  // 간단한 유효성 검사 (필요에 따라 추가)
  if (!missingDateTime.value || !description.value || !reporterContact.value) {
    alert('모든 필수 정보를 입력해주세요.');
    return;
  }

  // 백엔드로 보낼 데이터 준비
  const reportData = {
    // patientUserNo: ???, // 실제 실종자 userNo 필요 (API 설계 필요)
    // reporterUserNo: ???, // 실제 신고자 userNo 필요 (현재 로그인 사용자)
    photoPath: uploadedImageUrl.value,
    description: description.value,
    reportedAt: missingDateTime.value, // 실종 일시 필드명 확인 필요
    contact: reporterContact.value // 신고자 연락처 필드명 확인 필요
    // status: '실종' // 기본값은 서버에서 처리 가능
  };

  console.log('서버로 전송할 데이터:', reportData);

  try {
    isUploading.value = true; // 제출 중 상태 표시 (옵션)
    
    // 🚨 중요: 실제 백엔드 API 주소로 변경해야 함!
    // const response = await axios.post('/api/missing-posts', reportData, {
    //   withCredentials: true
    // });
    
    alert('실종 신고가 접수되었습니다.'); // 임시 알림
    // router.push('/communityMissing'); // 성공 시 목록 페이지 등으로 이동

  } catch (error) {
    console.error('실종 신고 처리 중 오류 발생:', error);
    alert('신고 접수에 실패했습니다.');
  } finally {
     isUploading.value = false;
  }
}

</script>

<template>
  <div class="report-container">
    <div class="header">
      <button @click="goBack" class="back-button">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M10.707 13.293a1 1 0 0 1-1.414 1.414l-6-6a1 1 0 0 1 0-1.414l6-6a1 1 0 0 1 1.414 1.414L5.414 8l5.293 5.293z" fill="#525252"/>
        </svg>
      </button>
      <h1 class="title">실종 신고</h1>
    </div>

    <div class="form-content">
      <section class="info-section">
        <h2>실종자 정보</h2>
        <div class="info-box">
          <div class="info-row">
            <span>이름</span>
            <span>{{ missingPerson.name }}</span>
          </div>
          <div class="info-row">
            <span>나이</span>
            <span>{{ missingPerson.age }}세</span>
          </div>
        </div>
      </section>

      <section class="form-section">
        <h2>실종자 사진</h2>
        <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*" style="display: none;" />

        <div v-if="imagePreviewUrl" class="preview-area">
          <img :src="imagePreviewUrl" alt="미리보기" class="image-preview" />
          <button @click="removeImage" class="remove-image-button">X</button>
        </div>
        <div v-else class="photo-uploader" @click="triggerFileInput">
          <div class="upload-icon">📷</div>
          <div class="upload-text-main">최근 사진을 업로드해주세요</div>
          <button class="upload-button">사진 선택</button>
        </div>
      </section>

      <section class="form-section"> 
        <label for="missing-datetime">실종 일시</label>
        <div class="input-with-icon">
          <input 
            id="missing-datetime"
            type="datetime-local" 
            v-model="missingDateTime"
            class="form-input" 
          />
          <span class="icon-calendar">📅</span> 
        </div>
      </section>

      <div class="info-guide">
        <span>ℹ️</span>
        <div>
          <strong>실종장소 안내</strong>
          <p>실종시간을 기반으로 예상위치 페이지에서 표시됩니다.</p>
        </div>
      </div>

      <section class="form-section">
        <label for="description">특이사항</label>
        <textarea 
          id="description"
          v-model="description" 
          placeholder="실종자를 찾는데 도움이 될 수 있는 모든 정보를 입력해주세요" 
          class="form-textarea"
        ></textarea>
      </section>

      <section class="form-section">
        <label for="contact">신고자 연락처</label>
        <input 
          id="contact"
          type="tel" 
          v-model="reporterContact" 
          placeholder="010-0000-0000" 
          class="form-input"
        />
      </section>

    </div>

    <div class="footer-buttons">
      <button @click="submitReport" class="submit-button" :disabled="isUploading">
        <span>⚠️</span> 긴급 실종 신고
      </button>
      </div>

  </div>
</template>

<style scoped>
/* 전체 컨테이너 */
.report-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 500px; /* 최대 너비 제한 */
  min-height: 100vh;
  margin: 0 auto;
  background-color: #FFFFFF;
  border: 1px solid #E5E5E5; /* 테두리 추가 (선택사항) */
}

/* 헤더 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #E5E5E5;
  background-color: #FFFFFF;
  position: sticky; /* 상단 고정 */
  top: 0;
  z-index: 10;
}
.back-button {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
}
.title {
  font-size: 18px;
  font-weight: 600; /* 조금 더 굵게 */
  color: #171717;
  margin: 0;
  position: absolute; /* 중앙 정렬 트릭 */
  left: 50%;
  transform: translateX(-50%);
}

/* 메인 폼 영역 */
.form-content {
  padding: 24px 16px 80px 16px; /* 하단 버튼 영역 확보 */
  flex-grow: 1; /* 남은 공간 채우기 */
}

/* 섹션 공통 스타일 */
.form-section {
  margin-bottom: 32px; /* 섹션 간 간격 */
}
.form-section h2 { /* 실종자 정보, 사진 섹션 제목 */
  font-size: 16px;
  font-weight: 600; /* 굵게 */
  color: #171717;
  margin-bottom: 12px;
}
.form-section label { /* 나머지 섹션 라벨 */
  display: block;
  font-size: 14px;
  font-weight: 500; /* 살짝 굵게 */
  color: #404040;
  margin-bottom: 8px;
}

/* 실종자 정보 박스 */
.info-section .info-box {
  background: #FAFAFA;
  border-radius: 6px;
  padding: 16px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #525252;
}
.info-row span:last-child {
  color: #171717;
  font-weight: 500;
}
.info-row:not(:last-child) {
  margin-bottom: 8px;
}

/* 사진 업로더 */
.photo-uploader {
  border: 2px dashed #D4D4D4;
  border-radius: 8px;
  padding: 26px 20px;
  text-align: center;
  cursor: pointer;
  background-color: #FFFFFF; /* 배경 흰색 */
}
.upload-icon { 
  font-size: 24px; /* 아이콘 크기 */
  color: #A3A3A3; /* 아이콘 색상 */
  background-color: #E5E5E5; /* 아이콘 배경 */
  width: 64px;
  height: 64px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
} 
.upload-text-main { 
  font-size: 14px;
  color: #525252; 
  margin-bottom: 8px;
}
.upload-button { /* 사진 선택 버튼 */
  background: #171717;
  color: #FFFFFF;
  border: none;
  padding: 7px 16px;
  font-size: 14px;
  border-radius: 6px;
  cursor: pointer;
}

/* 이미지 미리보기 */
.preview-area {
  position: relative;
  width: 100px; /* 크기 조정 */
  height: 100px; /* 크기 조정 */
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}
.image-preview {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.remove-image-button {
  position: absolute;
  top: 5px;
  right: 5px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-radius: 50%;
  width: 20px; /* 크기 살짝 줄임 */
  height: 20px; /* 크기 살짝 줄임 */
  border: none;
  cursor: pointer;
  font-weight: bold;
  font-size: 12px; /* X 크기 조정 */
  line-height: 20px;
  text-align: center;
  padding: 0;
}

/* 입력 필드 공통 */
.form-input, .form-textarea {
  box-sizing: border-box;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #D4D4D4;
  border-radius: 6px;
  font-size: 16px;
  background: #FFFFFF;
}
.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: #8E97FD; /* 포커스 색상 */
}
.form-textarea {
  min-height: 96px; /* 높이 */
  resize: vertical;
}

/* 실종 일시 입력 필드 (아이콘 포함) */
.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}
.input-with-icon .form-input {
  padding-right: 40px; /* 아이콘 공간 확보 */
}
.input-with-icon .icon-calendar {
  position: absolute;
  right: 12px;
  color: #555; /* 아이콘 색상 */
  font-size: 20px; /* 아이콘 크기 */
}

/* 실종장소 안내 박스 */
.info-guide {
  display: flex;
  gap: 12px;
  background: #FAFAFA;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 32px;
}
.info-guide span:first-child { /* 아이콘 */
  color: #737373;
  font-size: 16px;
  margin-top: 2px;
}
.info-guide strong {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 4px;
}
.info-guide p {
  font-size: 11px;
  color: #525252;
  margin: 0;
  line-height: 1.4;
}

/* 하단 버튼 영역 */
.footer-buttons {
  padding: 16px;
  border-top: 1px solid #E5E5E5;
  background-color: #FFFFFF;
  position: sticky; /* 하단 고정 */
  bottom: 0;
  z-index: 10;
}
.submit-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  background: #525252;
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500; /* 살짝 얇게 */
  cursor: pointer;
}
.submit-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
.submit-button span { /* 아이콘 */
  font-size: 16px;
}

</style>
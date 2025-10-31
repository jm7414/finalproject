<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import { usePostImageUpload } from '@/composables/usePostImageUpload';


const router = useRouter();
const route = useRoute();

// 날짜와 시간 분리
const patientNo = ref(null);
const missingDate = ref(''); // 날짜 입력용
const missingTime = ref(''); // 시간 입력용
const description = ref('');
const reporterContact = ref('');
const photoFile = ref(null);
const imagePreviewUrl = ref(null);
const isUploading = ref(false);

// 이미지 업로드 관련
const { upload } = usePostImageUpload();
const fileInput = ref(null); // <input type="file"> 참조


onMounted(() => {
  patientNo.value = route.params.id;
  if (!patientNo.value) {
    alert('신고 대상 환자 정보가 올바르지 않습니다.');
  } else {
    console.log('실종 신고 페이지 로드. 대상 환자 ID (params):', patientNo.value);
  }

  // 기능 12: 현재 날짜 및 시간으로 초기값 설정
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0'); // 월 (0부터 시작하므로 +1)
  const day = String(now.getDate()).padStart(2, '0');      // 일
  const hours = String(now.getHours()).padStart(2, '0');    // 시
  const minutes = String(now.getMinutes()).padStart(2, '0');  // 분

  missingDate.value = `${year}-${month}-${day}`; // YYYY-MM-DD 형식
  missingTime.value = `${hours}:${minutes}`;     // HH:mm 형식
});

// 사진넣기
function triggerFileInput() {
  fileInput.value?.click();
}

// 파일 선택 시 미리보기 업데이트
async function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;
  photoFile.value = file;
  if (imagePreviewUrl.value) {
    URL.revokeObjectURL(imagePreviewUrl.value);
  }
  imagePreviewUrl.value = URL.createObjectURL(file);
}

// 기능 9: 폼 제출 (실종 신고 API 호출)
async function submitReport() {
  if (!patientNo.value) {
    alert('신고 대상 환자 정보가 없습니다.'); return;
  }
  if (!missingDate.value || !missingTime.value || !description.value || !reporterContact.value) {
    alert('실종 날짜, 실종 시간, 특이사항, 신고자 연락처를 모두 입력해주세요.'); return;
  }

  // 날짜와 시간을 ISO 8601 형식으로 결합
  const reportedAtValue = `${missingDate.value}T${missingTime.value}`;

  isUploading.value = true;
  let uploadedImageUrl = null;

  try {
    // 이미지 업로드
    if (photoFile.value) {
      uploadedImageUrl = await upload(photoFile.value);
    }

    // API 전송 데이터 준비
    const reportData = {
      patientUserNo: patientNo.value,
      photoPath: uploadedImageUrl,
      description: description.value,
      status: "실종",
    };

    // 실종 신고 API 호출
    const response = await axios.post('/api/missing-persons/report', reportData, {
      withCredentials: true
    });

    alert('실종 신고가 성공적으로 접수되었습니다.');
    // 성공 시 페이지 이동
    router.push('/CommunityMissing');

  } catch (error) {
    console.error("실종 신고 처리 중 오류 발생:", error);
    alert("실종 신고에 실패했습니다. 프론트");
  } finally {
    isUploading.value = false;
  }
}

// 기능 10: 취소 (뒤로 가기)
function goBack() {
  router.go(-1);
}
</script>

<template>
  <div class="page-container">
    <div class="form-wrapper">
      <section class="form-section">
        <label class="form-label small mb-1">실종자 사진</label>
        <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*" style="display: none;" />

        <div v-if="imagePreviewUrl" class="preview-area">
          <img :src="imagePreviewUrl" alt="미리보기" class="image-preview" />
          <button @click="photoFile = null; imagePreviewUrl = null; if (fileInput) fileInput.value = '';"
            class="remove-image-button">X</button>
        </div>
        <div v-else class="photo-uploader" @click="triggerFileInput">
          <div class="upload-icon">📷</div>
          <div class="upload-text-main">사진을 추가해보세요</div>
          <div class="upload-text-sub">사진 선택하기</div>
        </div>
      </section>

      <section class="form-section">
        <label for="missing-date" class="form-label small mb-1">실종 날짜</label>
        <input id="missing-date" type="date" v-model="missingDate" class="form-control" />
      </section>

      <section class="form-section">
        <label for="missing-time" class="form-label small mb-1">실종 시간</label>
        <input id="missing-time" type="time" v-model="missingTime" class="form-control" />
      </section>

      <div class="guide-box">
        <span>ℹ️</span>
        <div>
          <strong>실종장소 안내</strong>
          <p>실종시간을 기반으로 예상위치 페이지에서 표시됩니다.</p>
        </div>
      </div>

      <section class="form-section">
        <label for="description" class="form-label small mb-1">특이사항</label>
        <textarea id="description" v-model="description" placeholder="실종자를 찾는데 도움이 될 수 있는 모든 정보를 입력해주세요"
          class="form-control content-textarea" maxlength="1000" rows="4"></textarea>
      </section>

      <section class="form-section">
        <label for="contact" class="form-label small mb-1">신고자 연락처</label>
        <input id="contact" type="tel" v-model="reporterContact" placeholder="010-1234-1234" class="form-control" />
      </section>
      <div class="btns">
        <button @click="submitReport" class="submit-btn" :disabled="isUploading">
          {{ isUploading ? '신고 중...' : '긴급 실종 신고' }}
        </button>
        <button @click="goBack" class="cancel-btn">취소하기</button>
      </div>
    </div>


  </div>
</template>

<style scoped>
/* 전체 페이지 레이아웃 */
.page-container {
  width: 100%;
  margin-top: 80px;
  background: #FAFAFA;
  font-family: 'Inter', sans-serif;
  height: calc(100vh - 90px - 90px);
  overflow-y: auto;
  box-sizing: border-box;
}

/* 폼 영역 */
.form-wrapper {
  flex-grow: 1;
  padding: 17px 16px;
  padding-bottom: 200px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

/* 폼 섹션 공통 */
.form-section {
  margin-bottom: 0;
}

/* 사진 첨부 섹션 */
.photo-uploader {
  border: 2px dashed #D4D4D4;
  border-radius: 8px;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  background-color: #f9f9f9;
}

.upload-icon {
  font-size: 32px;
  color: #aaa;
  margin-bottom: 8px;
}

.upload-text-main {
  font-weight: 600;
  color: #555;
}

.upload-text-sub {
  font-size: 14px;
  color: #888;
}

.preview-area {
  position: relative;
  width: 120px;
  height: 120px;
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
  width: 24px;
  height: 24px;
  border: none;
  cursor: pointer;
  font-weight: bold;
  font-size: 14px;
  line-height: 24px;
  text-align: center;
  padding: 0;
}

/* 입력 필드 공통 */
.form-control {
  font-size: 14px;
}

.form-control::placeholder {
  color: #ADAEBC;
}

.form-control:focus {
  border-color: #8E97FD;
  box-shadow: 0 0 0 0.2rem rgba(142, 151, 253, 0.25);
}

.small {
  font-size: .875em;
}

.mb-1 {
  margin-bottom: 0.25rem !important;
}

/* 내용 입력 */
.content-textarea {
  min-height: 120px;
  resize: vertical;
}

/* 안내 박스 */
.guide-box {
  display: flex;
  gap: 12px;
  background: #FAFAFA;
  border-radius: 6px;
  padding: 12px;
}

.guide-box span:first-child {
  color: #737373;
  font-size: 16px;
  margin-top: 2px;
}

.guide-box strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
}

.guide-box p {
  font-size: 11px;
  color: #525252;
  margin: 0;
  line-height: 1.4;
}



/* 버튼 스타일 (이전과 동일) */

.btns{
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.submit-btn,
.cancel-btn {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
}

.submit-btn:hover,
.cancel-btn:hover {
  opacity: 0.8;
}

.submit-btn {
  background: #8E97FD;
  color: #FFFFFF;
}

.cancel-btn {
  background: #FFFFFF;
  color: #404040;
  border: 1px solid #D4D4D4;
}

.submit-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.7;
}
</style>

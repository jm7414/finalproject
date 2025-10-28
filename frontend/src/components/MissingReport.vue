<script setup>
// 실종상태변환 기능 1: 필요한 모듈 import (ref, onMounted, useRouter, useRoute, axios 등)
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router'; // ★ useRoute 추가
import axios from 'axios';

// 실종상태변환 기능 2: 이미지 업로드 composable import
import { usePostImageUpload } from '@/composables/usePostImageUpload'; // 경로 확인!

// 실종상태변환 기능 3: 라우터 인스턴스 생성
const router = useRouter();
// 실종상태변환 기능 4: 현재 라우트 정보 접근
const route = useRoute();

// 실종상태변환 기능 5: 신고 대상 환자 정보 ref
const patientNo = ref(null);
const patientName = ref(''); // (선택) 환자 이름 표시용

// 실종상태변환 기능 6: 폼 데이터 ref (기존 모달 코드와 동일)
const missingDateTime = ref('');
const description = ref('');
const reporterContact = ref('');
const photoFile = ref(null);
const imagePreviewUrl = ref(null);
const isUploading = ref(false);

// 실종상태변환 기능 7: 이미지 업로드 함수 가져오기
const { upload } = usePostImageUpload();

// 실종상태변환 기능 8: 파일 입력(<input type="file">) 요소 참조
const fileInput = ref(null);

// 실종상태변환 기능 9: 페이지 로드 시 실행될 로직
onMounted(() => {
  // 실종상태변환 기능 10: URL 쿼리 파라미터에서 patientNo 읽기
  patientNo.value = route.query.patientNo;
  if (!patientNo.value) {
    console.error('신고 대상 환자 번호(patientNo)가 URL에 없습니다.');
    alert('신고 대상 환자 정보가 올바르지 않습니다.');
    // 이전 페이지로 돌려보내거나 홈으로 보낼 수 있습니다.
    // router.go(-1);
  } else {
    console.log('실종 신고 페이지 로드. 대상 환자 No:', patientNo.value);
    // (선택) patientNo로 환자 이름 등 추가 정보 API 조회
    // fetchPatientDetails(patientNo.value);
  }
});

// 실종상태변환 기능 11: 파일 선택창 열기 함수 (기존 모달 코드와 동일)
function triggerFileInput() {
  fileInput.value.click();
}

// 실종상태변환 기능 12: 파일 선택 시 처리 함수 (기존 모달 코드와 동일)
async function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;
  photoFile.value = file;
  imagePreviewUrl.value = URL.createObjectURL(file);
}

// 실종상태변환 기능 13: '긴급 실종 신고' 버튼 클릭 시 실행될 메인 함수
async function submitReport() {
  // 실종상태변환 기능 14: 필수 입력값 검증 (patientNo 추가)
  if (!patientNo.value) {
    alert('신고 대상 환자 정보가 없습니다.');
    return;
  }
  // (주의) datetime-local은 값이 비어있어도 빈 문자열("")이므로, !missingDateTime.value 만으로는 부족할 수 있습니다.
  // 필요시 더 엄격한 유효성 검사 추가 (예: 날짜 형식 확인)
  if (!missingDateTime.value || !description.value || !reporterContact.value) {
    alert('실종 일시, 특이사항, 신고자 연락처를 모두 입력해주세요.');
    return;
  }

  isUploading.value = true;
  let uploadedImageUrl = null;

  try {
    // 실종상태변환 기능 15: 이미지 업로드 (기존 모달 코드와 동일)
    if (photoFile.value) {
      uploadedImageUrl = await upload(photoFile.value);
    }

    // 실종상태변환 기능 16: 백엔드 API로 보낼 데이터 준비 (patientUserNo 값 설정)
    const reportData = {
      patientUserNo: patientNo.value, // ★ props 대신 ref 사용
      photoPath: uploadedImageUrl,
      description: description.value,
      reportedAt: missingDateTime.value,
      status: "실종",
      // reporterContact 처리 방식 확인 필요 (description에 포함 또는 DTO 수정)
    };

    // 실종상태변환 기능 17: 백엔드 실종 신고 API 호출 (기존 모달 코드와 동일)
    const response = await axios.post('/api/missing-persons/report', reportData, {
      withCredentials: true
    });

    alert('실종 신고가 성공적으로 접수되었습니다.');
    // 실종상태변환 기능 18: 성공 후 페이지 이동 (예: 커뮤니티 실종 목록)
    router.push('/CommunityMissing'); // ★ 성공 시 이동할 경로

  } catch (error) {
    // 실종상태변환 기능 19: API 호출 실패 처리
    console.error("실종 신고 처리 중 오류 발생:", error);
    alert("실종 신고에 실패했습니다.");
  } finally {
    // 실종상태변환 기능 20: 로딩 상태 해제
    isUploading.value = false;
  }
}

// 실종상태변환 기능 21: 뒤로가기 함수
function goBack() {
  router.go(-1); // 브라우저의 뒤로가기 기능 사용
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <button @click="goBack" class="back-button">←</button>
      <h1 class="title">실종 신고</h1>
    </div>

    <div class="form-wrapper">
      <section class="form-section">
        <!-- 스타일 수정 1: Report.vue와 동일하게 label 클래스 적용 -->
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
        <!-- 스타일 수정 2: Report.vue와 동일하게 label 클래스 적용 -->
        <label for="missing-datetime" class="form-label small mb-1">실종 일시</label>
        <!-- 스타일 수정 3: 가짜 placeholder 구조 제거, form-control 클래스 적용 -->
        <input id="missing-datetime" type="datetime-local" v-model="missingDateTime" class="form-control" <!--
          form-control 클래스 사용 -->
        />
        <!-- (icon-calendar span 제거됨) -->
      </section>

      <div class="guide-box">
        <span>ℹ️</span>
        <div>
          <strong>실종장소 안내</strong>
          <p>실종시간을 기반으로 예상위치 페이지에서 표시됩니다.</p>
        </div>
      </div>

      <section class="form-section">
        <!-- 스타일 수정 4: Report.vue와 동일하게 label 클래스 적용 -->
        <label for="description" class="form-label small mb-1">특이사항</label>
        <!-- 스타일 수정 5: form-control 클래스 적용 (textarea에도 적용 가능) -->
        <textarea id="description" v-model="description" placeholder="실종자를 찾는데 도움이 될 수 있는 모든 정보를 입력해주세요"
          class="form-control content-textarea" <!-- form-control 추가 -->
          maxlength="1000"
          rows="4" <!-- 높이 조절 (선택 사항) -->
        ></textarea>
      </section>

      <section class="form-section">
        <!-- 스타일 수정 6: Report.vue와 동일하게 label 클래스 적용 -->
        <label for="contact" class="form-label small mb-1">신고자 연락처</label>
        <!-- 스타일 수정 7: form-control 클래스 적용 -->
        <input id="contact" type="tel" v-model="reporterContact" placeholder="010-1234-1234" class="form-control" <!--
          form-control 클래스 사용 -->
        />
      </section>
    </div>

    <div class="footer-buttons">
      <button @click="submitReport" class="submit-btn" :disabled="isUploading">
        {{ isUploading ? '신고 중...' : '긴급 실종 신고' }}
      </button>
      <button @click="goBack" class="cancel-btn">취소하기</button>
    </div>
  </div>
</template>

<style scoped>
/* 전체 페이지 레이아웃 */
.page-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 465px;
  /* 최대 너비 일치 */
  min-height: 100vh;
  margin: 0 auto;
  background-color: #FAFAFA;
  /* 배경색 일치 */
  font-family: 'Inter', sans-serif;
}

/* 페이지 헤더 (새로 추가) */
.page-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  border-bottom: 1px solid #E5E5E5;
  position: relative;
  flex-shrink: 0;
  background-color: #FFFFFF;
  /* 헤더 배경색 */
}

.back-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  position: absolute;
  left: 16px;
  color: #333;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #171717;
  margin: 0;
}

/* 폼 영역 */
.form-wrapper {
  flex-grow: 1;
  /* 내용 영역이 남은 공간 차지 */
  padding: 17px 16px;
  /* 패딩 일치 */
  display: flex;
  flex-direction: column;
  gap: 24px;
  /* 섹션 간격 일치 */
  overflow-y: auto;
  /* 내용 길어지면 스크롤 */
}

/* 폼 섹션 공통 */
.form-section {
  margin-bottom: 0;
  /* form-wrapper의 gap으로 대체 */
}

/* 스타일 수정 8: Report.vue의 label 스타일 적용 (form-label, small, mb-1 클래스는 Bootstrap이 처리) */
/* .form-section label { ... } <- 기존 스타일 제거 또는 Bootstrap 스타일로 대체 */


/* 사진 첨부 섹션 */
.photo-uploader {
  border: 2px dashed #D4D4D4;
  /* 스타일 일치 */
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
  /* 미리보기 크기 조정 (선택 사항) */
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

/* 입력 필드 공통 (Bootstrap .form-control 사용) */
/* 스타일 수정 9: 기존 .form-input, .title-input 스타일 제거 또는 Bootstrap 재정의 */
/* .form-input, .title-input { ... } */
/* .form-input::placeholder, .title-input::placeholder { ... } */
/* .form-input:focus, .title-input:focus { ... } */

/* 내용 입력 (Bootstrap .form-control 사용) */
/* 스타일 수정 10: 기존 .textarea-container, .content-textarea, .char-counter 스타일 제거 또는 Bootstrap 재정의 */
/* .textarea-container { ... } */
.content-textarea {
  min-height: 120px;
  /* Bootstrap 기본 높이 조절 (선택 사항) */
  resize: vertical;
  /* 세로 크기 조절 허용 */
}

/* .content-textarea::placeholder { ... } */
/* .content-textarea:focus { ... } */
/* .char-counter { ... } */


/* 실종 일시 (아이콘 포함) - 아이콘 제거되었으므로 관련 스타일 불필요 */
/* 스타일 수정 11: .input-with-icon, .icon-calendar 스타일 제거 */
/* .input-with-icon { ... } */
/* .input-with-icon .icon-calendar { ... } */


/* 안내 박스 - 기존 스타일 유지 */
.guide-box {
  display: flex;
  gap: 12px;
  background: #FAFAFA;
  border-radius: 6px;
  padding: 12px;
  /* margin-bottom 제거됨 (form-wrapper gap 사용) */
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

/* 하단 버튼 영역 */
.footer-buttons {
  background: #FFFFFF;
  padding: 17px 16px;
  /* 패딩 일치 */
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  /* 그림자 일치 */
  display: flex;
  flex-direction: column;
  gap: 12px;
  /* 버튼 간격 일치 */
  flex-shrink: 0;
  /* 높이 고정 */
}

.submit-btn,
.cancel-btn {
  width: 100%;
  height: 44px;
  /* 높이 일치 */
  border-radius: 8px;
  /* 둥근 모서리 일치 */
  font-size: 14px;
  /* 폰트 크기 일치 */
  font-weight: 500;
  /* 폰트 굵기 일치 */
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
  /* 호버 효과 일치 */
}

.submit-btn:hover,
.cancel-btn:hover {
  opacity: 0.8;
  /* 호버 효과 일치 */
}

.submit-btn {
  background: #8E97FD;
  /* 제출 버튼 색상 일치 (조정 필요시 #525252) */
  color: #FFFFFF;
}

.cancel-btn {
  background: #FFFFFF;
  color: #404040;
  /* 취소 버튼 스타일 일치 */
  border: 1px solid #D4D4D4;
}

/* 로딩 상태 버튼 스타일 */
.submit-btn:disabled {
  background-color: #ccc;
  /* 비활성화 색상 */
  cursor: not-allowed;
  opacity: 0.7;
}
</style>

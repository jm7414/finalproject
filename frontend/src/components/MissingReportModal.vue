<script setup>
import { ref } from 'vue';
import axios from 'axios';

// 부모 컴포넌트(Pr.vue)로부터 전달받을 props 정의
const props = defineProps({
  show: Boolean, // 모달을 보여줄지 여부
  patient: Object // 신고할 환자 정보 (name, userNo)
});

// 부모 컴포넌트(Pr.vue)로 이벤트를 보내기 위한 emit 정의
const emit = defineEmits(['close', 'reportSuccess']);

// 폼 데이터
const missingDateTime = ref('');
const description = ref('');
const reporterContact = ref('');
const photoFile = ref(null);
const imagePreviewUrl = ref(null);
const isUploading = ref(false);

// 이미지 업로드 심부름꾼 (경로 확인!)
import { usePostImageUpload } from '@/composables/usePostImageUpload';
const { upload } = usePostImageUpload();

// 파일 선택창 열기
const fileInput = ref(null);
function triggerFileInput() {
  fileInput.value.click();
}

// 파일 선택 시 처리
async function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;
  photoFile.value = file;
  imagePreviewUrl.value = URL.createObjectURL(file);
}

// '긴급 실종 신고' 버튼 클릭
async function submitReport() {
  if (!missingDateTime.value || !description.value || !reporterContact.value) {
    alert('모든 필수 항목을 입력해주세요.');
    return;
  }
  
  isUploading.value = true;
  let uploadedImageUrl = null;

  try {
    // 1. 이미지가 있으면 먼저 업로드
    if (photoFile.value) {
      uploadedImageUrl = await upload(photoFile.value);
    }

    // 2. 백엔드에 보낼 데이터 준비
    const reportData = {
      patientUserNo: props.patient.userNo,
      photoPath: uploadedImageUrl,
      description: description.value,
      reportedAt: missingDateTime.value, // 프론트에서 보낸 시간을 사용
      status: "실종",
      // (추가) 신고자 연락처는 description에 포함하거나 DTO 필드 추가 필요
      // description: `특이사항: ${description.value}\n신고자 연락처: ${reporterContact.value}`
    };

    // 3. 실종 신고 API 호출
    const response = await axios.post('/api/missing-persons/report', reportData, {
      withCredentials: true
    });
    
    alert('실종 신고가 성공적으로 접수되었습니다.');
    emit('reportSuccess', response.data); // 성공 이벤트 부모에게 전달
    closeModal(); // 모달 닫기

  } catch (error) {
    console.error("실종 신고 처리 중 오류 발생:", error);
    alert("실종 신고에 실패했습니다.");
  } finally {
    isUploading.value = false;
  }
}

// 모달 닫기
function closeModal() {
  // 폼 초기화 (선택사항)
  missingDateTime.value = '';
  description.value = '';
  reporterContact.value = '';
  photoFile.value = null;
  imagePreviewUrl.value = null;
  
  emit('close'); // 닫기 이벤트 부모에게 전달
}
</script>

<template>
  <div v-if="show" class="modal-backdrop" @click.self="closeModal">
    <div class="modal-container">
      
      <div class="modal-header">
        <button @click="closeModal" class="back-button">←</button>
        <h1 class="title">실종 신고</h1>
      </div>

      <div class="modal-content">
        <section class="form-section">
          <h2>실종자 사진</h2>
          <label>최근 사진을 업로드해주세요</label>
          <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*" style="display: none;" />
          
          <div v-if="imagePreviewUrl" class="preview-area">
            <img :src="imagePreviewUrl" alt="미리보기" class="image-preview" />
            <button @click="photoFile = null; imagePreviewUrl = null;" class="remove-image-button">X</button>
          </div>
          <div v-else class="photo-uploader" @click="triggerFileInput">
            <div class="upload-icon">📷</div>
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

        <div class="guide-box">
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
            placeholder="010-1234-1234" 
            class="form-input"
          />
        </section>
        <div class="modal-footer">
        <button @click="submitReport" class="action-button primary" :disabled="isUploading">
          <span>⚠️</span> 
          {{ isUploading ? '신고 중...' : '긴급 실종 신고' }}
        </button>
      </div>
      </div>



    </div>
  </div>
</template>

<style scoped>
/* 모달 배경 */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

/* 모달 컨테이너 (디자인 적용) */
.modal-container {
  width: 375px;
  max-height: 90vh; /* 화면 높이의 90%를 넘지 않도록 */
  background: #FFFFFF;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 내부 스크롤을 위해 */
}

/* 헤더 */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: center; /* 제목 중앙 정렬 */
  padding: 12px 16px;
  border-bottom: 1px solid #E5E5E5;
  position: relative; /* 뒤로가기 버튼 위치 기준 */
  flex-shrink: 0; /* 높이 고정 */
}
.back-button {
  background: none; border: none; font-size: 24px; cursor: pointer;
  position: absolute; /* 헤더 왼쪽에 배치 */
  left: 16px;
}
.title {
  font-size: 18px; font-weight: 600; color: #171717; margin: 0;
}

/* 폼 콘텐츠 (스크롤 가능 영역) */
.modal-content {
  padding: 24px 16px;
  overflow-y: auto; /* 내용 길어지면 스크롤 */
  flex-grow: 1; /* 남은 공간 차지 */
}

/* 폼 섹션 공통 */
.form-section {
  margin-bottom: 24px;
}
.form-section h2 { /* 실종자 사진 제목 */
  font-size: 16px; font-weight: 600; color: #171717; margin-bottom: 4px;
}
.form-section label { /* 나머지 라벨 */
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #404040;
  margin-bottom: 8px;
}

/* 사진 업로더 */
.photo-uploader {
  border: 2px dashed #D4D4D4;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  background-color: #f9f9f9;
}
.upload-icon { 
  font-size: 24px; color: #A3A3A3; background-color: #E5E5E5;
  width: 64px; height: 64px; border-radius: 8px;
  display: inline-flex; align-items: center; justify-content: center;
  margin-bottom: 12px;
} 
/* 미리보기 영역 */
.preview-area {
  position: relative; width: 80px; height: 80px; /* 크기 조정 */
}
.image-preview {
  width: 100%; height: 100%; object-fit: cover; border-radius: 8px;
}
.remove-image-button {
  position: absolute; top: -5px; right: -5px; background: black;
  color: white; border-radius: 50%; width: 20px; height: 20px;
  border: none; cursor: pointer; font-size: 12px;
}


/* 입력 필드 공통 */
.form-input, .form-textarea {
  box-sizing: border-box; width: 100%; padding: 10px 12px;
  border: 1px solid #D4D4D4; border-radius: 6px; font-size: 16px;
}
.form-textarea { min-height: 96px; resize: vertical; }

/* 실종 일시 (아이콘 포함) */
.input-with-icon { position: relative; display: flex; align-items: center; }
.input-with-icon .form-input { padding-right: 40px; }
.input-with-icon .icon-calendar { position: absolute; right: 12px; font-size: 20px; }

/* 안내 박스 */
.guide-box {
  display: flex; gap: 12px; background: #FAFAFA;
  border-radius: 6px; padding: 12px; margin-bottom: 24px;
}
.guide-box span:first-child { color: #737373; font-size: 16px; margin-top: 2px; }
.guide-box strong { display: block; font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 4px; }
.guide-box p { font-size: 11px; color: #525252; margin: 0; line-height: 1.4; }

/* 푸터 (버튼) */
.action-button {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  width: 100%; margin-bottom: 90px; padding: 12px; background: #525252; color: #FFFFFF;
  border: none; border-radius: 8px; font-size: 16px; font-weight: 500;
  cursor: pointer;
}
.action-button:disabled { background-color: #ccc; }
</style>
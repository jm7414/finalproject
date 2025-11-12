<template>
  <div class="page-container">
    <div class="form-wrapper">

      <!-- '제목' 섹션은 제보에 필요 없으므로 삭제됨 -->

      <section class="form-section">
        <label for="content-textarea">내용</label>
        <div class="textarea-container">
          <textarea 
            id="content-textarea"
            class="content-textarea"
            placeholder="제보 내용을 입력해주세요 (발견 시간, 장소, 인상착의 등)"
            v-model="content"
            maxlength="1000"
          ></textarea>
          <span class="char-counter">{{ contentLength }} / 1000자</span>
        </div>
      </section>

      <section class="form-section">
        <label>사진 첨부</label>
        <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*" style="display: none;" />
        
        <div v-if="imagePreviewUrl" class="preview-area">
          <img :src="imagePreviewUrl" alt="미리보기" class="image-preview" />
          <button @click="removeImage" class="remove-image-button">X</button>
        </div>
        
        <div v-else class="photo-uploader" @click="triggerFileInput">
          <div class="upload-icon">📷</div>
          <div class="upload-text-main">사진을 추가해보세요</div>
          <div class="upload-text-sub">사진 선택하기</div>
        </div>
      </section>
    </div>

    <div class="footer-buttons">
      <button @click="submitReport" class="submit-btn" :disabled="isUploading">
        {{ isUploading ? '이미지 업로드 중...' : (isEditMode ? '제보 수정하기' : '제보 작성하기') }}
      </button>
      <button @click="cancel" class="cancel-btn">취소하기</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import { usePostImageUpload } from '@/composables/usePostImageUpload';

const route = useRoute();
const router = useRouter();
const { upload } = usePostImageUpload();

// '수정'과 '생성' 모드의 URL 파라미터를 명확히 분리합니다.
const isEditMode = computed(() => route.name === 'ReportEdit');
// 수정 모드일 때만 '제보 ID'를 가져옵니다.
const reportId = ref(isEditMode.value ? route.params.id : null);
// 생성 모드일 때만 '실종 ID'를 가져옵니다.
const missingPostId = ref(isEditMode.value ? null : route.params.id);

const content = ref('');
const contentLength = computed(() => content.value.length);

const fileInput = ref(null);
const imagePreviewUrl = ref(null);
const uploadedImageUrl = ref(null);
const isUploading = ref(false);

onMounted(() => {
  if (isEditMode.value) {
    // 수정 모드일 때만 기존 제보 내용을 불러옵니다.
    fetchReportForEdit();
  } else if (!missingPostId.value) {
    // 생성 모드인데 missingPostId가 URL에 없는 경우
    console.error("제보 생성 오류: missingPostId가 URL 파라미터에 없습니다.");
    alert("잘못된 접근입니다. 제보할 실종 게시물 페이지에서 다시 시도해주세요.");
    router.back();
  }
});

// '제보' 수정 데이터를 불러오는 함수
async function fetchReportForEdit() {
  try {
    const response = await axios.get(`/api/sighting-reports/${reportId.value}`, {
      withCredentials: true
    });
    content.value = response.data.content;
    
    if (response.data.imagePath) {
      imagePreviewUrl.value = response.data.imagePath;
      uploadedImageUrl.value = response.data.imagePath;
    }
    
    // (중요) 수정 모드에서는 missingPostId를 DB에서 가져와야 합니다.
    // (SightingReportResponseDto에 missingPostId가 포함되어 있다고 가정)
    if (response.data.missingPostId) {
        missingPostId.value = response.data.missingPostId;
    }

  } catch (error) {
    console.error('수정할 제보 정보를 불러오는 데 실패했습니다:', error);
    alert('제보 정보를 불러올 수 없습니다.');
    router.back();
  }
}

// --- 이미지 업로드 함수들 ---
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
  } catch (error) {
    alert("이미지 업로드에 실패했습니다. 파일을 다시 선택해주세요.");
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

// 제보를 작성하거나 수정하는 함수
async function submitReport() {
  if (!content.value.trim()) {
    alert('내용을 입력해주세요.');
    return;
  }

  try {
    const reportData = {
      content: content.value,
      imagePath: uploadedImageUrl.value 
    };

    if (isEditMode.value) {
      // --- 1. 제보 수정 ---
      await axios.put(`/api/sighting-reports/${reportId.value}`, reportData, {
        withCredentials: true
      });
      alert('제보가 성공적으로 수정되었습니다!');
      router.push(`/SightingReport/${reportId.value}`); 
    } else {
      // --- 2. 새 제보 작성 --
      reportData.missingPostId = missingPostId.value;

      if (!reportData.missingPostId) {
          alert("오류: 실종 게시물 ID가 없습니다. 다시 시도해주세요.");
          return;
      }
      const response = await axios.post(`/api/sighting-reports`, reportData, {
        withCredentials: true
      });
      const newReportId = response.data; 
      alert('제보가 성공적으로 작성되었습니다!');
      router.push(`/SightingReportBoard/${missingPostId.value}`); 
    }
  } catch (error) {
    console.error('제보 처리 중 오류가 발생했습니다:', error);
    alert('작업에 실패했습니다. 다시 시도해주세요.');
  }
}

function cancel() {
  router.back();
}
</script>

<style scoped>

.page-container {
  width: 100%;
  margin-top: 70px;
  background: #FAFAFA;
  font-family: 'Inter', sans-serif;
  height: calc(100vh - 90px - 90px);
  overflow-y: auto;
  box-sizing: border-box;
  padding-bottom: 60px;
}
.form-wrapper {
  flex-grow: 1;
  padding: 17px 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.form-section {
  margin-bottom: 24px;
}
.form-section label {
  display: block;
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 14px;
  color: #404040;
  font-weight: 500;
}
.photo-uploader {
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 30px 20px; 
  text-align: center;
  cursor: pointer;
  background-color: #f9f9f9;
}
.upload-icon { font-size: 32px; color: #aaa; margin-bottom: 8px;}
.upload-text-main { font-weight: 600; color: #555;}
.upload-text-sub { font-size: 14px; color: #888; }
.preview-area {
  position: relative;
  width: 100%; 
  height: 100%;
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
.textarea-container {
  position: relative;
}
.content-textarea {
  width: 100%;
  height: 186px;
  border: 1px solid #D4D4D4;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  resize: none;
}
.content-textarea::placeholder {
  color: #ADAEBC;
}
.content-textarea:focus {
  outline: 1px solid #8E97FD;
}
.char-counter {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 12px;
  color: #737373;
}
.photo-uploader {
  width: 100%;
  height: 132px;
  border: 2px dashed #D4D4D4;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  gap: 6px;
}
.upload-icon {
  font-size: 24px;
}
.upload-text-main {
  font-size: 14px;
  color: #737373;
}
.upload-text-sub {
  font-size: 14px;
  color: #525252;
  font-weight: 500;
}
.footer-buttons {
  background: #FFFFFF;
  padding: 17px 16px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.submit-btn, .cancel-btn {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
}   
.submit-btn:hover, .cancel-btn:hover {
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
</style>
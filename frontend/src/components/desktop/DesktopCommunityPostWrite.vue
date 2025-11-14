<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

// 이미지 업로드
import { usePostImageUpload } from '@/composables/usePostImageUpload';

const route = useRoute();
const router = useRouter();
const { upload } = usePostImageUpload();

const isEditMode = computed(() => !!route.params.id);
const postId = ref(route.params.id || null);

const title = ref('');
const content = ref('');
const contentLength = computed(() => content.value.length);

// 이미지 관련
const fileInput = ref(null);
const imagePreviewUrl = ref(null);
const uploadedImageUrl = ref(null);
const isUploading = ref(false);

// [수정] 현재 사용자 정보 ref
const currentUser = ref(null);

onMounted(() => {
  // [수정] 현재 사용자 정보 로드
  fetchCurrentUser();
  
  if (isEditMode.value) {
    fetchPostForEdit();
  }
});

// [수정] 현재 사용자 정보 로드 함수
async function fetchCurrentUser() {
  try {
    const response = await axios.get(`/api/user/me`, {
      withCredentials: true
    });
    currentUser.value = response.data;
  } catch (error) {
    console.error("현재 사용자 정보를 가져오는데 실패했습니다.", error);
    currentUser.value = null;
    if (!isEditMode.value) {
      alert("사용자 정보를 불러올 수 없습니다. 다시 로그인해주세요.");
      router.back();
    }
  }
}

async function fetchPostForEdit() {
  try {
    const response = await axios.get(`/api/posts/${postId.value}`, {
      withCredentials: true
    });
    title.value = response.data.title;
    content.value = response.data.content;
    if (response.data.image) {
      imagePreviewUrl.value = response.data.image;
      uploadedImageUrl.value = response.data.image;
    }
  } catch (error) {
    console.error('수정할 게시물 정보를 불러오는 데 실패했습니다:', error);
    alert('게시물 정보를 불러올 수 없습니다.');
    router.back();
  }
}

// --- 이미지 처리 함수 (동일) ---
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
// --- 이미지 처리 끝 ---


// [수정] submitPost 함수 (디버깅 코드 제거)
async function submitPost() {
  if (!title.value.trim() || !content.value.trim()) {
    alert('제목과 내용을 모두 입력해주세요.');
    return;
  }

  try {
    if (isEditMode.value) {
      // 1. [수정 시] - userId가 필요 없습니다.
      const postData = {
        title: title.value,
        content: content.value,
        image: uploadedImageUrl.value
      };
      await axios.put(`/api/posts/${postId.value}`, postData, {
        withCredentials: true
      });
      alert('게시글이 성공적으로 수정되었습니다!');
      router.push(`/desktop/communityPost/${postId.value}`);

    } else {
      // 2. [새 글 작성 시] - userId가 필수입니다.
      if (!currentUser.value) {
        alert("사용자 정보가 로드되지 않았습니다. 잠시 후 다시 시도해주세요.");
        return;
      }
      const postData = {
        title: title.value,
        content: content.value,
        image: uploadedImageUrl.value,
        userId: currentUser.value.userNo // (로그에서 '1'이 확인됨)
      };

      // [수정] 실제 서버로 전송합니다.
      const response = await axios.post(`/api/posts`, postData, {
        withCredentials: true
      });
      
      const newPostId = response.data; 
      alert('게시글이 성공적으로 작성되었습니다!');
      router.push(`/desktop/communityPost/${newPostId}`); 
    }
  } catch (error) {
    console.error('게시글 처리 중 오류가 발생했습니다:', error);
    alert('작업에 실패했습니다. 다시 시도해주세요.');
  }
}

function cancel() {
  router.back();
}
</script>

<template>
  <div class="page-container">
    <div class="form-wrapper">
      <section class="form-section">
        <label for="title-input">제목</label>
        <input 
          id="title-input" 
          type="text" 
          class="title-input" 
          placeholder="제목을 입력해주세요"
          v-model="title"
        >
      </section>

      <section class="form-section">
        <label for="content-textarea">내용</label>
        <div class="textarea-container">
          <textarea 
            id="content-textarea"
            class="content-textarea"
            placeholder="내용을 입력해주세요"
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
      <button @click="submitPost" class="submit-btn" :disabled="isUploading">
        {{ isUploading ? '이미지 업로드 중...' : (isEditMode ? '수정하기' : '게시물 작성하기') }}
      </button>
      <button @click="cancel" class="cancel-btn">취소하기</button>
    </div>
  </div>
</template>


<style scoped>
/* 전체 페이지 레이아웃 */
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

/* 사진 첨부 섹션 */
.form-section {
  margin-bottom: 24px;
}
.form-section label {
  display: block;
  font-weight: 600;
  margin-bottom: 8px;
}

/* 사진 선택 영역 스타일 */
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
  /* 크기를 원하는 대로 조절 */
  width: 40%; 
  height: 40%;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}
.image-preview {
  display: block;
  width: 50%;
  height: 50%;
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

.form-section label {
  font-size: 14px;
  color: #404040;
  font-weight: 500;
}

/* 제목 입력 */
.title-input {
  width: 100%;
  height: 46px;
  border: 1px solid #D4D4D4;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
}
.title-input::placeholder {
  color: #ADAEBC;
}
.title-input:focus {
  outline: 1px solid #8E97FD;
}

/* 내용 입력 */
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

/* 사진 첨부 */
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

.preview-area {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
/* (이미지 미리보기 관련 스타일은 추후 추가) */


/* 하단 버튼 */
.footer-buttons {
  background: #FFFFFF;
  padding: 0px 16px;
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
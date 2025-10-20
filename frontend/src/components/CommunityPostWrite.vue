<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

// 이미지 업로드 가이드 1번
import { usePostImageUpload } from '@/composables/usePostImageUpload';

const route = useRoute();
const router = useRouter();

// 이미지 업로드 가이드 2번
const { upload } = usePostImageUpload();

const isEditMode = computed(() => !!route.params.id);
const postId = ref(route.params.id || null);

const title = ref('');
const content = ref('');
const contentLength = computed(() => content.value.length);

// 이미지 업로드 가이드 3번 - 이미지 관련 상태를 관리할 변수들을 추가
const fileInput = ref(null);        // 숨겨진 <input type="file">에 접근하기 위한 변수
const imagePreviewUrl = ref(null);  // 이미지 미리보기 URL
const uploadedImageUrl = ref(null); // 서버에 업로드 후 받은 최종 이미지 경로
const isUploading = ref(false);     // 업로드 중인지 상태를 관리

onMounted(() => {
  if (isEditMode.value) {
    fetchPostForEdit();
  }
});

async function fetchPostForEdit() {
  try {
    const response = await axios.get(`/api/posts/${postId.value}`, {
      withCredentials: true
    });
    title.value = response.data.title;
    content.value = response.data.content;

    // 이미지 업로드 가이드 4번 - 수정 모드일 때, 기존 이미지가 있다면 미리보기에 표시
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

// 이미지 업로드 가이드 5번 - '사진 추가' 버튼을 누르면 숨겨진 파일 입력창을 클릭시키는 함수
function triggerFileInput() {
  fileInput.value.click();
}

// 이미지 업로드 가이드 6번 - 파일이 선택되면 이미지를 서버에 업로드하고 미리보기를 보여주는 함수
async function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;

  imagePreviewUrl.value = URL.createObjectURL(file);
  isUploading.value = true;

  try {
    const imageUrl = await upload(file);
    uploadedImageUrl.value = imageUrl;
    console.log("이미지 업로드 성공! 경로:", imageUrl);
  } catch (error) {
    alert("이미지 업로드에 실패했습니다. 파일을 다시 선택해주세요.");
    removeImage();
  } finally {
    isUploading.value = false;
  }
}

// 이미지 업로드 가이드 7번 - 선택된 이미지 미리보기를 제거하는 함수
function removeImage() {
  imagePreviewUrl.value = null;
  uploadedImageUrl.value = null;
  if(fileInput.value) fileInput.value.value = '';
}

async function submitPost() {
  if (!title.value.trim() || !content.value.trim()) {
    alert('제목과 내용을 모두 입력해주세요.');
    return;
  }

  try {
    // 이미지 업로드 가이드 8번 - 최종 전송할 데이터에 이미지 경로 포함
    const postData = {
      title: title.value,
      content: content.value,
      image: uploadedImageUrl.value
    };

    if (isEditMode.value) {
      // 2. 글 수정할 때
      await axios.put(`/api/posts/${postId.value}`, postData, {
        withCredentials: true
      });
      alert('게시글이 성공적으로 수정되었습니다!');
      router.push(`/post/${postId.value}`);
    } else {
      // 3. 새 글 작성할 때
      const response = await axios.post(`/api/posts`, postData, {
        withCredentials: true
      });
      // createPost API는 이제 생성된 postId를 숫자(Integer)로 반환합니다.
      const newPostId = response.data; 
      alert('게시글이 성공적으로 작성되었습니다!');
      router.push(`/post/${newPostId}`); 
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
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 465px;
  min-height: 100vh;
  margin: 0 auto;
  background-color: #FAFAFA;
  font-family: 'Inter', sans-serif;
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
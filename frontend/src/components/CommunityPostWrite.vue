<template>
  <div class="page-container">
    <div class="form-wrapper">
      <section class="form-section">
        <label for="title-input">제목</label>
        <input id="title-input" type="text" class="title-input" placeholder="제목을 입력해주세요" v-model="title">
      </section>
      <section class="form-section">
        <label for="content-textarea">내용</label>
        <div class="textarea-container">
          <textarea id="content-textarea" class="content-textarea" placeholder="내용을 입력해주세요" v-model="content"
            maxlength="1000"></textarea>
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

    <!-- Alert 모달 -->
    <div v-if="modal.show" class="modal-overlay">
      <div class="modal-box">
        <div class="modal-title">{{ modal.title }}</div>
        <div class="modal-message">{{ modal.message }}</div>
        <button class="modal-btn" @click="handleModalConfirm">확인</button>
      </div>
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
const isEditMode = computed(() => !!route.params.id);
const postId = ref(route.params.id || null);

const title = ref('');
const content = ref('');
const contentLength = computed(() => content.value.length);

const fileInput = ref(null);
const imagePreviewUrl = ref(null);
const uploadedImageUrl = ref(null);
const isUploading = ref(false);

const modal = ref({
  show: false,
  title: '',
  message: '',
  onConfirm: null
});

function showModal(title, message, onConfirm = null) {
  modal.value = { show: true, title, message, onConfirm };
}
function closeModal() { modal.value.show = false; }
function handleModalConfirm() {
  closeModal();
  if (typeof modal.value.onConfirm === 'function') modal.value.onConfirm();
}

onMounted(() => {
  if (isEditMode.value) fetchPostForEdit();
});

async function fetchPostForEdit() {
  try {
    const response = await axios.get(`/api/posts/${postId.value}`, { withCredentials: true });
    title.value = response.data.title;
    content.value = response.data.content;
    if (response.data.image) {
      imagePreviewUrl.value = response.data.image;
      uploadedImageUrl.value = response.data.image;
    }
  } catch (error) {
    console.error('수정할 게시물 정보를 불러오는 데 실패했습니다:', error);
    showModal('게시물 정보를 불러올 수 없습니다.', '', () => router.back());
  }
}

function triggerFileInput() {
  if (fileInput.value) fileInput.value.click();
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
    showModal("이미지 업로드에 실패했습니다. 파일을 다시 선택해주세요.");
    removeImage();
  } finally {
    isUploading.value = false;
  }
}
function removeImage() {
  imagePreviewUrl.value = null;
  uploadedImageUrl.value = null;
  if (fileInput.value) fileInput.value.value = '';
}
async function submitPost() {
  if (!title.value.trim() || !content.value.trim()) {
    showModal('제목과 내용을 모두 입력해주세요.');
    return;
  }
  try {
    const postData = { title: title.value, content: content.value, image: uploadedImageUrl.value };
    if (isEditMode.value) {
      await axios.put(`/api/posts/${postId.value}`, postData, { withCredentials: true });
      showModal('게시글이 성공적으로 수정되었습니다!', '', () => router.push(`/post/${postId.value}`));
    } else {
      const response = await axios.post(`/api/posts`, postData, { withCredentials: true });
      const newPostId = response.data;
      showModal('게시글이 성공적으로 작성되었습니다!', '', () => router.push(`/post/${newPostId}`));
    }
  } catch (error) {
    console.error('게시글 처리 중 오류가 발생했습니다:', error);
    showModal('작업에 실패했습니다. 다시 시도해주세요.');
  }
}
function cancel() { router.back(); }
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
  padding-bottom: 100px;
}

.page-container::-webkit-scrollbar {
  display: none;
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
}

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
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  background-color: #f9f9f9;
  width: 100%;
  height: 132px;
  gap: 6px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
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
  width: 100%;
  height: 100%;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  gap: 8px;
  margin-top: 12px;
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

.footer-buttons {
  background: #FFFFFF;
  padding: 0px 16px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
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

/* 모달 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(34, 55, 133, 0.06);
  /* 거의 투명 반투명 회색방향 */
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-box {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 32px rgba(142, 151, 253, 0.15);
  padding: 24px 16px 14px 16px;
  min-width: 240px;
  max-width: 340px;
  width: 88%;
  text-align: center;
  border: 2px solid #e8edfb;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.modal-title {
  font-size: 14px;
  font-weight: 500;
  color: #404040;
  margin-bottom: 8px;
}

.modal-message {
  font-size: 14px;
  color: #444;
  margin-bottom: 18px;
  white-space: pre-line;
}

.modal-btn {
  background: #8E97FD;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  border: none;
  border-radius: 8px;
  padding: 9px 0;
  width: 92%;
  cursor: pointer;
  transition: background 0.21s;
  margin-bottom: 2px;
}

.modal-btn:hover {
  background: #5f70d3;
}
</style>

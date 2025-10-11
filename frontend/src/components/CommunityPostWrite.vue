<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

const route = useRoute();
const router = useRouter();

// URL에 id가 있으면 수정 모드, 없으면 생성 모드
const isEditMode = computed(() => !!route.params.id);
const postId = ref(route.params.id || null);

const title = ref('');
const content = ref('');
const contentLength = computed(() => content.value.length);
const attachedFile = ref(null);

// 컴포넌트가 마운트될 때 '수정 모드'인지 확인
onMounted(() => {
  if (isEditMode.value) {
    // 수정 모드이면, 기존 게시물 데이터를 불러와서 폼에 채워넣습니다.
    fetchPostForEdit();
  }
});

// 수정할 게시물 데이터를 불러오는 함수
async function fetchPostForEdit() {
  try {
    const response = await axios.get(`http://localhost:8080/api/posts/${postId.value}`);
    title.value = response.data.title;
    content.value = response.data.content;
  } catch (error) {
    console.error('수정할 게시물 정보를 불러오는 데 실패했습니다:', error);
    alert('게시물 정보를 불러올 수 없습니다.');
    router.push('/community'); // 실패 시 목록으로
  }
}

// '글 작성하기' 또는 '수정하기' 버튼 클릭 시 실행될 함수
async function submitPost() {
  if (!title.value.trim() || !content.value.trim()) {
    alert('제목과 내용을 모두 입력해주세요.');
    return;
  }

  try {
    const postData = {
      title: title.value,
      content: content.value,
    };

    if (isEditMode.value) { // 이해필요
      // 수정 모드일 경우: PUT 요청
      await axios.put(`http://localhost:8080/api/posts/${postId.value}`, postData);
      alert('게시글이 성공적으로 수정되었습니다!');
      router.push(`/post/${postId.value}`); // 수정된 글로 이동
    } else {
      // 생성 모드일 경우: POST 요청
      const response = await axios.post('http://localhost:8080/api/posts', postData);
      const newPostId = response.data;
      router.push(`/post/${newPostId}`); 
    }

  } catch (error) {
    console.error('게시글 처리 중 오류가 발생했습니다:', error);
    alert('작업에 실패했습니다. 다시 시도해주세요.');
  }
}

// 사진
function handleFileClick() {
  alert('미완성 기능');
}
</script>

<template>
  <div class="form-container">
    <div class="form-group">
      <label for="title">제목 <span class="required">*</span></label>
      <input 
        id="title"
        type="text" 
        v-model="title" 
        placeholder="제목을 입력해 주세요" 
        class="form-input"
      />
    </div>

    <div class="form-group">
      <label for="content">내용 <span class="required">*</span></label>
      <div class="textarea-wrapper">
        <textarea 
          id="content"
          v-model="content" 
          placeholder="내용을 입력해 주세요" 
          class="form-textarea"
          maxlength="500"
        ></textarea>
        <span class="char-counter">{{ contentLength }}/500</span>
      </div>
    </div>
    
    <div class="form-group">
      <label for="photo">사진 추가</label>
      <button 
        id="photo"
        @click="handleFileClick" 
        class="file-input-button"
      >
      </button>
    </div>

    <button @click="submitPost" class="submit-button">
      {{ isEditMode ? '수정하기' : '글 작성하기' }}
    </button>
  </div>
</template>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  max-width: 500px;
  margin: 24px auto;
  padding: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 700;
  font-size: 18px;
}

.required {
  color: #8E97FD;
}

.form-input, .file-input-button, .form-textarea {
  width: 100%;
  background-color: #F2F3F7;
  border-radius: 15px;
  border: 1px solid transparent;
  padding: 50px;
  font-size: 16px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.form-input::placeholder, .file-input-button, .form-textarea::placeholder {
  color: #A1A4B2;
}
.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: #8E97FD;
  box-shadow: 0 0 0 3px rgba(142, 151, 253, 0.2);
}

.file-input-button {
  text-align: left;
  cursor: pointer;
}

.textarea-wrapper {
  position: relative;
}

.form-textarea {
  height: 280px;
  resize: none; /* 사용자가 크기 조절 못하게 */
}

.char-counter {
  position: absolute;
  bottom: 12px;
  right: 16px;
  color: #A1A4B2;
  font-size: 14px;
}

.submit-button {
  width: 100%;
  padding: 18px;
  border-radius: 38px;
  background-color: #8E97FD;
  color: white;
  font-size: 20px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease;
}
.submit-button:hover {
  background-color: #7b85f8;
  transform: translateY(-2px);
}
</style>


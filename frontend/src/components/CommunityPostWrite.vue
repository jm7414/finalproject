<script setup>
import { ref, computed } from 'vue';
import image1 from '@/assets/images/Missing.jpg';

// 입력된 제목, 내용을 저장하기 위한 ref 변수
const title = ref('');
const content = ref('');
const attachedFile = ref(null);

// 내용의 글자 수를 계산하는 computed 속성
const contentLength = computed(() => content.value.length);

// '사진 추가' 버튼을 클릭했을 때 실행될 함수
function handleFileClick() {
  alert('미완성 기능');
  // 실제 구현 시에는 파일 입력(input type="file")을 트리거하는 코드가 들어갑니다.
}

// '글 작성하기' 버튼을 클릭했을 때 실행될 함수
function submitPost() {
  if (!title.value || !content.value) {
    alert('제목과 내용을 모두 입력해주세요.');
    return;
  }

  // 콘솔에 입력된 데이터를 객체 형태로 출력
  console.log('--- 새로운 게시물 데이터 ---');
  console.log({
    title: title.value,
    content: content.value,
    // attachedFile: attachedFile.value // 실제 파일 데이터
  });
  alert('게시글이 작성되었습니다. (콘솔을 확인해보세요)');
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
        image: image1
      >
      </button>
    </div>

    <button @click="submitPost" class="submit-button">글 작성하기</button>
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
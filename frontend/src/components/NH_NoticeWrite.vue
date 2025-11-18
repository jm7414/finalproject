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
      </div>
  
      <div class="footer-buttons">
        <button @click="submitNotice" class="submit-btn" :disabled="loading">
          {{ loading ? '작성 중...' : '공지 작성하기' }}
        </button>
        <button @click="cancel" class="cancel-btn">취소하기</button>
      </div>
    </div>
  </template>
  
<script setup>
import { ref, computed } from 'vue';
import axios from 'axios';

const emit = defineEmits(['notice-created', 'cancel']);

const title = ref('');
const content = ref('');
const contentLength = computed(() => content.value.length);
const loading = ref(false);

// 공지 기능으로 인한 수정: 새로운 API 사용
async function submitNotice() {
  if (!title.value.trim() || !content.value.trim()) {
    alert('제목과 내용을 모두 입력해주세요.');
    return;
  }

  loading.value = true;

  try {
    await axios.post('/NH/api/neighbor/notices', {
      title: title.value,
      content: content.value
    });

    alert('공지가 성공적으로 작성되었습니다!');
    emit('notice-created');
  } catch (error) {
    console.error('공지 작성 중 오류가 발생했습니다:', error);
    
    if (error.response && error.response.status === 403) {
      alert('방장만 공지를 작성할 수 있습니다.');
    } else {
      alert('작업에 실패했습니다. 다시 시도해주세요.');
    }
  } finally {
    loading.value = false;
  }
}

function cancel() {
  if (title.value || content.value) {
    if (confirm('작성 중인 공지를 포기하시겠습니까?')) {
      emit('cancel');
    }
  } else {
    emit('cancel');
  }
}
</script>


<style scoped>
.page-container {
  width: 100%;
  margin-top: 0px;
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
}

.form-section label {
  font-size: 14px;
  color: #404040;
  font-weight: 500;
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
  outline: 2px solid #a7cc10; /* 아보카도 색상 */
  border-color: #a7cc10;
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
  outline: 2px solid #a7cc10; /* 아보카도 색상 */
  border-color: #a7cc10;
}

.char-counter {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 12px;
  color: #737373;
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

/* 아보카도 색상으로 변경 */
.submit-btn {
  background: #a7cc10; /* 아보카도 색상 */
  color: #FFFFFF;
}
.submit-btn:disabled {
  background: #d4e487; /* 아보카도 색상 비활성화 */
  cursor: not-allowed;
}

.cancel-btn {
  background: #FFFFFF;
  color: #404040;
  border: 1px solid #D4D4D4;
}
</style>

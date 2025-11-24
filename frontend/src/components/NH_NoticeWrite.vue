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
        {{ loading ? (isEdit ? '수정 중...' : '작성 중...') : (isEdit ? '공지 수정하기' : '공지 작성하기') }}
      </button>
      <button @click="cancel" class="cancel-btn">취소하기</button>
    </div>

    <!-- (지현 수정) AlertModal -->
    <div v-if="showAlert" class="modal-overlay" @click.self="closeAlert">
      <div class="modal-content">
        <div class="modal-icon" :class="`icon-${alertType}`">
          <i :class="alertIcon"></i>
        </div>
        <h3 class="modal-title">{{ alertTitle }}</h3>
        <p class="modal-message">{{ alertMessage }}</p>
        <button class="modal-btn" :class="`btn-${alertType}`" @click="handleAlertConfirm">
          확인
        </button>
      </div>
    </div>

    <!-- (지현 수정) ConfirmModal -->
    <div v-if="showConfirm" class="modal-overlay" @click.self="closeConfirm">
      <div class="modal-content">
        <h3 class="modal-title">취소 확인</h3>
        <p class="modal-message">공지 작성을 취소하시겠습니까?</p>
        <div class="modal-buttons">
          <button class="modal-btn btn-cancel" @click="handleConfirmCancel">
            취소
          </button>
          <button class="modal-btn btn-continue" @click="closeConfirm">
            계속 작성
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

const props = defineProps({
  notice: {
    type: Object,
    default: null
  },
  isEdit: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['notice-created', 'notice-updated', 'cancel']);

const title = ref('');
const content = ref('');
const contentLength = computed(() => content.value.length);
const loading = ref(false);

// (지현 수정) AlertModal 상태
const showAlert = ref(false);
const alertTitle = ref('');
const alertMessage = ref('');
const alertType = ref('info');
let alertCallback = null;

const alertIcon = computed(() => {
  const icons = {
    success: 'bi bi-check-circle-fill',
    error: 'bi bi-x-circle-fill',
    warning: 'bi bi-exclamation-triangle-fill',
    info: 'bi bi-info-circle-fill'
  };
  return icons[alertType.value];
});

// (지현 수정) ConfirmModal 상태
const showConfirm = ref(false);

// (지현 수정) 모달 표시 함수
function showAlertModal(type, titleText, messageText, callback = null) {
  alertType.value = type;
  alertTitle.value = titleText;
  alertMessage.value = messageText;
  alertCallback = callback;
  showAlert.value = true;
}

function closeAlert() {
  showAlert.value = false;
}

function handleAlertConfirm() {
  closeAlert();
  if (alertCallback) {
    alertCallback();
    alertCallback = null;
  }
}

function closeConfirm() {
  showConfirm.value = false;
}

function handleConfirmCancel() {
  closeConfirm();
  emit('cancel');
}

onMounted(() => {
  if (props.isEdit && props.notice) {
    title.value = props.notice.title || '';
    content.value = props.notice.content || '';
  }
});

async function submitNotice() {
  // (지현 수정) alert -> 모달로 변경
  if (!title.value.trim() || !content.value.trim()) {
    showAlertModal('warning', '입력 오류', '제목과 내용을 모두 입력해주세요.');
    return;
  }

  loading.value = true;

  try {
    if (props.isEdit) {
      await axios.put(`/NH/api/neighbor/notices/${props.notice.noticeNo}`, {
        title: title.value,
        content: content.value
      });
      // (지현 수정) alert -> 모달로 변경
      showAlertModal('success', '수정 완료', '공지가 성공적으로 수정되었습니다!', () => {
        emit('notice-updated');
      });
    } else {
      await axios.post('/NH/api/neighbor/notices', {
        title: title.value,
        content: content.value
      });
      // (지현 수정) alert -> 모달로 변경
      showAlertModal('success', '작성 완료', '공지가 성공적으로 작성되었습니다!', () => {
        emit('notice-created');
      });
    }
  } catch (error) {
    console.error('공지 처리 중 오류가 발생했습니다:', error);
    
    // (지현 수정) alert -> 모달로 변경
    if (error.response && error.response.status === 403) {
      showAlertModal('error', '권한 오류', '방장만 공지를 작성/수정할 수 있습니다.');
    } else {
      showAlertModal('error', '오류 발생', '작업에 실패했습니다. 다시 시도해주세요.');
    }
  } finally {
    loading.value = false;
  }
}

// (지현 수정) confirm -> 모달로 변경
function cancel() {
  if (title.value || content.value) {
    showConfirm.value = true;
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
  outline: 2px solid #a7cc10;
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
  outline: 2px solid #a7cc10;
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

.submit-btn {
  background: #a7cc10;
  color: #FFFFFF;
}

.submit-btn:disabled {
  background: #d4e487;
  cursor: not-allowed;
}

.cancel-btn {
  background: #FFFFFF;
  color: #404040;
  border: 1px solid #D4D4D4;
}

/* (지현 수정) 모달 스타일 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 320px;
  text-align: center;
}

.modal-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.icon-success i { color: #a7cc10; }
.icon-error i { color: #e74c3c; }
.icon-warning i { color: #f39c12; }
.icon-info i { color: #3498db; }

.modal-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 12px;
  color: #333;
}

.modal-message {
  font-size: 15px;
  color: #555;
  margin: 0 0 20px;
  line-height: 1.5;
}

.modal-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.modal-btn:hover {
  opacity: 0.9;
}

.btn-success { background: #a7cc10; color: white; }
.btn-error { background: #e74c3c; color: white; }
.btn-warning { background: #f39c12; color: white; }
.btn-info { background: #3498db; color: white; }

.modal-buttons {
  display: flex;
  gap: 10px;
}

.btn-cancel {
  background: #e74c3c;
  color: white;
}

.btn-continue {
  background: #a7cc10;
  color: white;
}
</style>

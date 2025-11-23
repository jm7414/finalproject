<!-- components/predict_split_Modal.vue -->
<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="handleCancel">
    <div class="modal-content">
      <p>{{ message }}</p>
      <div class="modal-buttons">
        <button @click="confirm" class="btn-confirm">확인</button>
        <button v-if="showCancel" @click="cancel" class="btn-cancel">취소</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const isOpen = ref(false)
const message = ref('')
const showCancel = ref(true) // 취소 버튼 표시 여부
let resolvePromise = null

const show = (msg, options = {}) => {
  message.value = msg
  showCancel.value = options.showCancel !== false // 기본값 true
  isOpen.value = true
  return new Promise((resolve) => {
    resolvePromise = resolve
  })
}

const confirm = () => {
  isOpen.value = false
  resolvePromise(true)
}

const cancel = () => {
  isOpen.value = false
  resolvePromise(false)
}

// 취소 버튼이 없을 때 오버레이 클릭 방지
const handleCancel = () => {
  if (showCancel.value) {
    cancel()
  }
}

defineExpose({ show })
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 375px;
  height: 812px;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10000;
  border-radius: 16px;
  overflow: hidden;
}

.modal-content {
  background: white;
  padding: 28px 24px;
  border-radius: 12px;
  min-width: 280px;
  max-width: 320px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.modal-content p {
  margin: 0 0 24px 0;
  line-height: 1.7;
  font-size: 15px;
  color: #333;
  font-weight: 400;
  letter-spacing: -0.3px; /* 한글 자간 조정 */
  word-break: keep-all; /* 단어 단위로 줄바꿈 */
  font-family: -apple-system, BlinkMacSystemFont, "Apple SD Gothic Neo", "Pretendard", "Noto Sans KR", sans-serif;
}

.modal-buttons {
  display: flex;
  gap: 10px;
  margin-top: 0;
  justify-content: flex-end;
}

.btn-confirm,
.btn-cancel {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: -0.3px;
  transition: all 0.2s ease;
  font-family: -apple-system, BlinkMacSystemFont, "Apple SD Gothic Neo", "Pretendard", "Noto Sans KR", sans-serif;
  min-width: 70px;
}

.btn-confirm {
  background: #4CAF50;
  color: white;
  box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
}

.btn-confirm:hover {
  background: #45a049;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.4);
}

.btn-confirm:active {
  transform: translateY(0);
}

.btn-cancel {
  background: #FF4433;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.btn-cancel:hover {
  background: #FF4433;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.btn-cancel:active {
  transform: translateY(0);
}

/* 버튼이 하나일 때 중앙 정렬 */
.modal-buttons:has(.btn-confirm:only-child) {
  justify-content: center;
}
</style>

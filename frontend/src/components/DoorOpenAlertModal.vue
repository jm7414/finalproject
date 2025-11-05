<template>
  <!-- 모달 배경 -->
  <div v-if="show" class="modal-backdrop" @click="closeModal">
    <!-- 모달 컨테이너 -->
    <div class="modal-container" @click.stop>
      <!-- 모달 헤더 -->
      <div class="modal-header">
        <div class="alert-icon">
          <i class="bi bi-exclamation-triangle-fill siren-icon"></i>
        </div>
        <h5 class="modal-title">현관문 감지 알림</h5>
      </div>

      <!-- 모달 내용 -->
      <div class="modal-body">
        <p class="alert-message">
          현관문이 열렸지만<br>핸드폰 움직임은 감지되지 않았습니다.
        </p>
      </div>

      <!-- 모달 버튼 -->
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="closeModal">
          닫기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

function closeModal() {
  emit('close')
}
</script>

<style scoped>
/* 모달 배경 */
.modal-backdrop {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  background-color: rgba(0, 0, 0, 0.5) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  z-index: 999999 !important;
  padding: 20px !important;
  width: 100vw !important;
  height: 100vh !important;
  margin: 0 !important;
}

/* 모달 컨테이너 */
.modal-container {
  background: white !important;
  border-radius: 16px !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2) !important;
  max-width: 320px !important;
  width: 320px !important;
  max-height: 90vh !important;
  overflow: hidden !important;
  animation: modalSlideIn 0.3s ease-out !important;
  position: relative !important;
  z-index: 1000000 !important;
  margin: 0 !important;
  transform: translateX(-10px) !important;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 모달 헤더 */
.modal-header {
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  color: white;
  padding: 12px 24px 10px;
  text-align: center;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.alert-icon {
  margin-bottom: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.alert-icon i {
  font-size: 24px;
  color: #FEF3C7;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.siren-icon {
  animation: sirenBlink 1s infinite;
}

@keyframes sirenBlink {
  0%, 50% {
    opacity: 1;
    transform: scale(1);
  }
  25%, 75% {
    opacity: 0.7;
    transform: scale(1.1);
  }
}

.modal-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
  color: white;
  text-align: center;
  width: 100%;
}

/* 모달 내용 */
.modal-body {
  padding: 24px;
  text-align: center;
}

.alert-message {
  font-size: 1rem;
  line-height: 1.7;
  color: #333;
  margin-bottom: 0;
  text-align: center;
  font-weight: 500;
  word-break: keep-all;
  white-space: pre-line;
}

/* 모달 버튼 */
.modal-footer {
  padding: 16px 24px 24px;
  display: flex;
  gap: 12px;
}

.btn {
  flex: 1;
  padding: 12px 16px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 0.95rem;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary {
  background: #F8F9FA;
  color: #666;
  border: 1px solid #E9ECEF;
}

.btn-secondary:hover {
  background: #E9ECEF;
  color: #555;
  transform: translateY(-1px);
}

.btn:active {
  transform: translateY(0);
}

/* 반응형 */
@media (max-width: 375px) {
  .modal-backdrop {
    padding: 16px;
  }
  
  .modal-container {
    max-width: 100%;
  }
  
  .modal-header {
    padding: 16px 20px 12px;
  }
  
  .modal-body {
    padding: 20px;
  }
  
  .modal-footer {
    padding: 12px 20px 20px;
  }
}
</style>


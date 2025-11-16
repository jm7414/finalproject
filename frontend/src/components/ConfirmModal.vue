<template>
  <div v-if="show" class="modal-backdrop" @click="handleOverlayClick">
    <div class="modal-container" @click.stop>
      <!-- 모달 헤더 -->
      <div class="modal-header">
        <div class="alert-icon">
          <i class="bi bi-exclamation-triangle-fill siren-icon"></i>
        </div>
        <h5 class="modal-title">{{ title }}</h5>
      </div>

      <!-- 모달 내용 -->
      <div class="modal-body">
        <p class="modal-message">{{ message }}</p>
      </div>

      <!-- 모달 버튼 -->
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="handleCancel">
          {{ cancelText }}
        </button>
        <button type="button" class="btn btn-primary" @click="handleConfirm">
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '확인'
  },
  message: {
    type: String,
    required: true
  },
  confirmText: {
    type: String,
    default: '확인'
  },
  cancelText: {
    type: String,
    default: '취소'
  },
  closeOnOverlay: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['confirm', 'cancel', 'close'])

function handleConfirm() {
  emit('confirm')
  emit('close')
}

function handleCancel() {
  emit('cancel')
  emit('close')
}

function handleOverlayClick() {
  if (props.closeOnOverlay) {
    emit('close')
  }
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
  max-width: 340px !important;
  width: 340px !important;
  max-height: 90vh !important;
  overflow: hidden !important;
  animation: modalSlideIn 0.3s ease-out !important;
  position: relative !important;
  z-index: 1000000 !important;
  margin: 0 !important;
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
  background: linear-gradient(135deg, #da9a39 0%, #c88a2f 100%);
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
  color: #FFF7ED;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 사이렌 애니메이션 */
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

.modal-message {
  font-size: 1rem;
  line-height: 1.5;
  color: #333;
  margin-bottom: 0;
  text-align: center;
  white-space: pre-wrap;
}

.modal-message strong {
  color: #da9a39;
  font-weight: 700;
}

/* 모달 버튼 */
.modal-footer {
  padding: 16px 20px 24px;
  display: flex;
  gap: 10px;
}

.btn {
  flex: 1;
  padding: 12px 8px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 0.875rem;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.btn-primary {
  background: linear-gradient(135deg, #da9a39 0%, #c88a2f 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(218, 154, 57, 0.3);
}

.btn-primary:hover {
  background: linear-gradient(135deg, #c88a2f 0%, #b67a25 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(218, 154, 57, 0.4);
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

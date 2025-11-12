<template>
  <div v-if="show" class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-container" @click.stop>
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">{{ title }}</h3>
        </div>
        <div class="modal-body">
          <p class="modal-message">{{ message }}</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel-btn" @click="handleCancel">
            {{ cancelText }}
          </button>
          <button class="modal-btn confirm-btn" @click="handleConfirm">
            {{ confirmText }}
          </button>
        </div>
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
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-container {
  width: 100%;
  max-width: 320px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.modal-content {
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 20px 20px 0 20px;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  text-align: center;
}

.modal-body {
  padding: 16px 20px 20px 20px;
}

.modal-message {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
  text-align: center;
  line-height: 1.5;
  white-space: pre-wrap;
}

.modal-footer {
  display: flex;
  gap: 8px;
  padding: 0 20px 20px 20px;
}

.modal-btn {
  flex: 1;
  padding: 12px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #ffffff;
  color: #a7cc10;
  border: 1px solid #a7cc10;
}

.cancel-btn:hover {
  background: #f9fafb;
  border-color: #a0c60c;;
}

.confirm-btn {
  background: #a7cc10;
  color: #ffffff;
}

.confirm-btn:hover {
  background: #a0c60c;
}
</style>

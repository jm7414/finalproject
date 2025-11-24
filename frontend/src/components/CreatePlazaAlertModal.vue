<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div v-if="showIcon" class="modal-icon" :class="`icon-${type}`">
        <i :class="iconClass"></i>
      </div>
      <h3 class="modal-title">{{ title }}</h3>
      <p class="modal-message">{{ message }}</p>
      <div v-if="mode==='confirm'" class="modal-buttons">
        <button class="modal-btn btn-cancel" @click="$emit('cancel')">취소</button>
        <button class="modal-btn" :class="`btn-${type}`" @click="$emit('confirm')">확인</button>
      </div>
      <button v-else class="modal-btn" :class="`btn-${type}`" @click="$emit('confirm')">확인</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  show: Boolean,
  title: { type: String, default: '알림' },
  message: String,
  type: { type: String, default: 'info' },
  mode: { type: String, default: 'alert' }, // 'confirm' or 'alert'
  showIcon: { type: Boolean, default: true }
})
const iconClass = computed(() => {
  const icons = {
    success: 'bi bi-check-circle-fill',
    error: 'bi bi-x-circle-fill',
    warning: 'bi bi-exclamation-triangle-fill',
    info: 'bi bi-info-circle-fill'
  }
  return icons[props.type]
})
</script>

<style scoped>
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 350px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  text-align: center;
  padding: 24px;
}

.modal-icon { font-size: 48px; margin-bottom: 12px; }
.icon-success i { color: #a7cc10; }
.icon-error i { color: #e74c3c; }
.icon-warning i { color: #f39c12; }
.icon-info i { color: #3498db; }
.modal-title { font-size: 18px; font-weight: 700; margin: 0 0 12px; color: #333; }
.modal-message { font-size: 15px; color: #555; margin: 0 0 20px; line-height: 1.5; white-space: pre-line; }
.modal-buttons { display: flex; gap: 10px; justify-content: center; }
.modal-btn { flex: 1; padding: 12px 0; border: none; border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; transition: opacity 0.2s; }
.modal-btn:hover { opacity: 0.9; }
.btn-cancel { background: #e74c3c; color: white; }
.btn-success { background: #a7cc10; color: white; }
.btn-error { background: #e74c3c; color: white; }
.btn-warning { background: #f39c12; color: white; }
.btn-info { background: #3498db; color: white; }
</style>

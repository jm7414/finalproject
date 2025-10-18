<template>
  <Teleport to="body">
    <div v-if="isVisible" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span>날짜 선택</span>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <input type="date" v-model="selectedDate" class="date-input" />
        </div>
        <div class="modal-footer">
          <button class="confirm-btn" @click="confirmDate">확인</button>
          <button class="cancel-btn" @click="closeModal">취소</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
const props = defineProps({ isVisible: Boolean })
const emit = defineEmits(['close', 'confirm'])
const selectedDate = ref('')

function closeModal() { emit('close') }
function confirmDate() {
  if (selectedDate.value) emit('confirm', selectedDate.value)
  else alert('날짜를 선택해주세요')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background: rgba(0,0,0,0.5);
  display: flex; justify-content: center; align-items: center;
  z-index: 9999;
}
.modal-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: 90vw; max-width: 340px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  position: relative;
  z-index: 10000;
}
.close-btn { /* 상단 x 버튼 */
  position: absolute; top: 20px; right: 24px;
  background: none; border: none; font-size: 24px; cursor: pointer;
}
.date-input {
  width: 100%;
  font-size: 20px;
  padding: 12px;
  margin-bottom: 20px;
  border-radius: 8px;
  border: 1px solid #e3e3e3;
  color: #555;
}
.confirm-btn, .cancel-btn {
  padding: 10px 22px; border-radius: 8px; font-size: 16px; border: none;
}
.confirm-btn { background: #4A62DD; color: #fff; }
.cancel-btn { background: #eee; color: #444; margin-left: 8px; }
</style>
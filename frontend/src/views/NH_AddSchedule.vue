<template>
  <div class="add-schedule-page">
    <!-- 폼 섹션 -->
    <div class="form-section">
      <!-- 일정 제목 -->
      <div class="form-group">
        <label class="form-label">제목 *</label>
        <input 
          v-model="scheduleForm.title"
          type="text" 
          class="form-input"
          placeholder="일정 제목을 입력하세요"
          required
        />
      </div>
      <!-- 일정 내용 -->
      <div class="form-group">
        <label class="form-label">내용</label>
        <textarea 
          v-model="scheduleForm.content"
          class="form-textarea"
          placeholder="일정 내용을 입력하세요 (선택사항)"
          rows="4"
        ></textarea>
      </div>
      <!-- 날짜 선택 -->
      <div class="form-group">
        <label class="form-label">날짜 *</label>
        <input 
          v-model="scheduleForm.date"
          type="date" 
          class="form-input"
          required
        />
      </div>
      <!-- 액션 버튼 (폼 내부에 배치, margin-top만 조금) -->
      <div class="action-buttons">
        <button class="save-btn" @click="saveNeighborSchedule" :disabled="!isFormValid || loading">
          {{ loading ? '저장 중...' : '저장' }}
        </button>
        <button class="cancel-btn" @click="cancelSchedule">
          취소
        </button>
      </div>
    </div>
    <!-- 확인 모달 -->
    <ConfirmModal
      :show="showConfirmModal"
      :title="confirmModalConfig.title"
      :message="confirmModalConfig.message"
      :confirm-text="confirmModalConfig.confirmText"
      :cancel-text="confirmModalConfig.cancelText"
      @confirm="handleConfirmCancel"
      @cancel="handleCancelCancel"
      @close="closeConfirmModal"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import ConfirmModal from '../components/ConfirmModal.vue'

const router = useRouter()

const scheduleForm = ref({
  title: '',
  content: '',
  date: ''
})
const loading = ref(false)
const showConfirmModal = ref(false)
const confirmModalConfig = ref({
  title: '확인',
  message: '',
  confirmText: '확인',
  cancelText: '취소'
})

// 필수값 검사
const isFormValid = computed(() => {
  return scheduleForm.value.title.trim() !== '' && scheduleForm.value.date !== ''
})

function resetScheduleForm() {
  scheduleForm.value = { title: '', content: '', date: '' }
  sessionStorage.removeItem('scheduleFormData')
  sessionStorage.removeItem('selectedDate')
}

function saveNeighborSchedule() {
  if (!isFormValid.value) {
    alert('제목과 날짜를 입력해주세요.')
    return
  }
  loading.value = true
  axios.post('/NH/api/neighbor/schedules', {
    title: scheduleForm.value.title,
    content: scheduleForm.value.content,
    scheduleDate: scheduleForm.value.date
  }).then(() => {
    resetScheduleForm()
    alert('일정이 저장되었습니다.')
    router.push({ name: 'NH_Calender' })
  }).catch(error => {
    if (error.response && error.response.status === 403) {
      alert('광장 멤버만 일정을 작성할 수 있습니다.')
    } else {
      alert(error.response?.data?.message || '일정 저장 중 오류가 발생했습니다.')
    }
  }).finally(() => {
    loading.value = false
  })
}

function cancelSchedule() {
  if (scheduleForm.value.title || scheduleForm.value.content) {
    confirmModalConfig.value = {
      title: '일정 취소',
      message: '작성 중인 내용이 사라집니다. 정말 취소하시겠습니까?',
      confirmText: '취소',
      cancelText: '계속 작성'
    }
    showConfirmModal.value = true
  } else {
    resetScheduleForm()
    router.go(-1)
  }
}

function handleConfirmCancel() {
  resetScheduleForm()
  closeConfirmModal()
  router.go(-1)
}
function handleCancelCancel() { closeConfirmModal() }
function closeConfirmModal() { showConfirmModal.value = false }

onMounted(() => {
  const selectedDate = sessionStorage.getItem('selectedDate')
  if (selectedDate) {
    scheduleForm.value.date = selectedDate
    sessionStorage.removeItem('selectedDate')
  } else {
    const today = new Date()
    const year = today.getFullYear()
    const month = String(today.getMonth() + 1).padStart(2, '0')
    const day = String(today.getDate()).padStart(2, '0')
    scheduleForm.value.date = `${year}-${month}-${day}`
  }
  const savedFormData = sessionStorage.getItem('scheduleFormData')
  if (savedFormData) {
    try {
      const parsedData = JSON.parse(savedFormData)
      scheduleForm.value = { ...scheduleForm.value, ...parsedData }
    } catch (error) {
      console.error('폼 데이터 파싱 오류:', error)
    }
  }
})
</script>

<style scoped>
.add-schedule-page {
  width: 100%;
  max-width: 420px;
  max-height: 700px;
  background: #fff;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  height: calc(100vh - 70px); /* 하단 네비/푸터 공간 빼고, 화면 내에 꽉 맞게 */
  overflow-y: auto;
}

.form-section {
  width: 100%;
  padding: 24px 16px 0 16px;
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.form-group {
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 3px;
  color: #555;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 10px 12px;
  font-size: 15px;
  border: 1px solid #a7cc10;
  border-radius: 8px;
  background: #fafafa;
  margin-bottom: 2px;
  box-sizing: border-box;
}

.form-textarea {
  resize: vertical;
  min-height: 60px;
  font-family: inherit;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.save-btn, .cancel-btn {
  width: 100%;
  padding: 13px 0;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
}

.save-btn {
  background: #a7cc10;
  color: #fff;
  border: none;
}

.save-btn:disabled {
  background: #d6e8ad;
  color: #fff;
  cursor: not-allowed;
}

.save-btn:not(:disabled):hover {
  background: #92bb11;
}

.cancel-btn {
  background: #fff;
  color: #a7cc10;
  border: 1px solid #a7cc10;
}

.cancel-btn:hover {
  background: #f6f7ed;
}

@media (max-width: 420px) {
  .form-section { padding-left: 9px; padding-right: 9px; }
}
</style>

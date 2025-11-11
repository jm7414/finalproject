<template>
  <div class="add-schedule-page">
    <!-- 헤더 -->
    <div class="header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <h1 class="page-title">일정 추가</h1>
      </div>
    </div>

    <!-- 폼 섹션 -->
    <div class="form-section">
      <!-- 일정 제목 -->
      <div class="form-group">
        <input 
          v-model="scheduleForm.title"
          type="text" 
          class="form-input"
          placeholder="일정 제목을 입력하세요*"
          required
        />
      </div>

      <!-- 일정 내용 -->
      <div class="form-group">
        <textarea 
          v-model="scheduleForm.content"
          class="form-textarea"
          placeholder="일정 내용을 입력하세요"
          rows="4"
        ></textarea>
      </div>

      <!-- 날짜 선택 -->
      <div class="form-group">
        <input 
          v-model="scheduleForm.date"
          type="date" 
          class="form-input"
          required
        />
      </div>

      <!-- 일정 위치 추가 (선택사항) -->
      <div class="form-group">
        <!-- 위치가 설정되지 않은 경우 -->
        <div v-if="!hasLocationData" class="location-input" @click="goToSearchRoute">
          <div class="location-content">
            <svg class="location-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg class="plus-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="location-text">일정 위치 추가 (선택)</span>
          </div>
        </div>
        
        <!-- 위치가 설정된 경우 -->
        <div v-else class="location-display">
          <div class="location-header">
            <svg class="location-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="location-title">일정 경로</span>
            <button class="edit-location-btn" @click="goToSearchRoute">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="m18.5 2.5 3 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
          <div class="location-route">
            <div v-for="(location, index) in locationData" :key="index" class="location-item">
              <div class="location-step">
                <div class="step-number">{{ index + 1 }}</div>
                <div class="step-content">
                  <div class="step-name">{{ location.name }}</div>
                </div>
              </div>
              <div v-if="index < locationData.length - 1" class="route-arrow">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M5 12h14M12 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 액션 버튼 -->
    <div class="action-buttons">
      <button class="save-btn" @click="saveNeighborSchedule" :disabled="!isFormValid">
        저장
      </button>
      <button class="cancel-btn" @click="cancelSchedule">
        취소
      </button>
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
import ConfirmModal from '../components/ConfirmModal.vue'

const router = useRouter()

// 폼 데이터
const scheduleForm = ref({
  title: '',
  content: '',
  date: ''
})

// 위치 데이터
const locationData = ref([])

// 위치 데이터가 있는지 확인
const hasLocationData = computed(() => locationData.value.length > 0)

// 확인 모달 관련
const showConfirmModal = ref(false)
const confirmModalConfig = ref({
  title: '확인',
  message: '',
  confirmText: '확인',
  cancelText: '취소'
})

// 폼 유효성 검사 - 제목과 날짜만 필수
const isFormValid = computed(() => {
  return scheduleForm.value.title.trim() !== '' &&
         scheduleForm.value.date !== ''
})

// 폼 초기화 함수
function resetScheduleForm() {
  scheduleForm.value = {
    title: '',
    content: '',
    date: ''
  }
  locationData.value = []
  sessionStorage.removeItem('scheduleLocations')
}

// 뒤로 가기
function goBack() {
  router.go(-1)
}

// 경로 검색 페이지로 이동
function goToSearchRoute() {
  sessionStorage.setItem('scheduleFormData', JSON.stringify(scheduleForm.value))
  sessionStorage.setItem('isScheduleInProgress', 'true')
  router.push({ name: 'search-route' })
}
// 일정 저장
async function saveNeighborSchedule() {
  if (!isFormValid.value) {
    alert('제목과 날짜를 입력해주세요.')
    return
  }

  try {
    const requestData = {
      title: scheduleForm.value.title,
      content: scheduleForm.value.content,
      date: scheduleForm.value.date
    }

    console.log('일정 저장 요청:', requestData)

    // /create-NH로 호출
    const response = await fetch(`/NH/api/create-NH`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify(requestData)
    })

    if (!response.ok) {
      const error = await response.json()
      throw new Error(error.message || '일정 저장에 실패했습니다.')
    }

    const result = await response.json()
    console.log('일정 저장 성공:', result)

    // 저장 성공 후 session 삭제하기
    resetScheduleForm()
    sessionStorage.removeItem('isScheduleInProgress')
    sessionStorage.removeItem('scheduleFormData')
    sessionStorage.removeItem('scheduleLocations')
    sessionStorage.removeItem('routeCoordinates')
    sessionStorage.removeItem('bufferCoordinates')

    resetScheduleForm()
    alert('일정이 저장되었습니다.')
    
    // NH_Calender로 이동
    router.push({ name: 'NH_Calender' })

  } catch (error) {
    console.error('일정 저장 오류:', error)
    alert(error.message || '일정 저장 중 오류가 발생했습니다.')
  }
}

// 일정 취소
function cancelSchedule() {
  confirmModalConfig.value = {
    title: '일정 취소',
    message: '작성 중인 내용이 사라집니다. 정말 취소하시겠습니까?',
    confirmText: '취소',
    cancelText: '계속 작성'
  }
  showConfirmModal.value = true
}

// 확인 모달에서 확인 버튼 클릭 시
function handleConfirmCancel() {
  resetScheduleForm()
  router.go(-1)
}

// 확인 모달에서 취소 버튼 클릭 시
function handleCancelCancel() {
  // 아무것도 하지 않음
}

// 확인 모달 닫기
function closeConfirmModal() {
  showConfirmModal.value = false
}

onMounted(() => {
  // 기본값으로 오늘 날짜 설정
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  scheduleForm.value.date = `${year}-${month}-${day}`

  // 진행 중인 경우 폼 데이터 복원
  const isInProgress = sessionStorage.getItem('isScheduleInProgress')
  if (isInProgress) {
    const savedFormData = sessionStorage.getItem('scheduleFormData')
    if (savedFormData) {
      const parsedData = JSON.parse(savedFormData)
      scheduleForm.value = { ...scheduleForm.value, ...parsedData }
    }

    const scheduleLocations = sessionStorage.getItem('scheduleLocations')
    if (scheduleLocations) {
      try {
        locationData.value = JSON.parse(scheduleLocations)
      } catch (error) {
        console.error('위치 데이터 파싱 오류:', error)
        locationData.value = []
      }
    }
  }
})
</script>

<style scoped>
/* (CSS는 이전과 동일하게 유지) */
.add-schedule-page {
  min-height: 70vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  padding-bottom: 220px;
  position: relative;
}

.header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  background: none;
  border: none;
  color: #111827;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background: #f9fafb;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.form-section {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #b5c4a3;
  border-radius: 12px;
  background: #fafafa;
  font-size: 14px;
  color: #111827;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: #a7cc10;
  background: #fafaf3;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
}

.location-input {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
}

.location-input:hover {
  border-color: #a7cc10;
  background: #fafaf3;;
}

.location-content {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
}

.location-icon {
  color: #9ca3af;
}

.plus-icon {
  color: #a7cc10;
}

.location-text {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
}

.location-display {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
  overflow: hidden;
}

.location-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f5f7ff;
  border-bottom: 1px solid #e5e7eb;
}

.location-title {
  font-size: 14px;
  color: #111827;
  font-weight: 600;
  flex: 1;
}

.edit-location-btn {
  background: none;
  border: none;
  color: #6366f1;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.edit-location-btn:hover {
  background: #e0e7ff;
}

.location-route {
  padding: 16px;
}

.location-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.location-step {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.step-number {
  width: 24px;
  height: 24px;
  background: #6366f1;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.step-name {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
}

.route-arrow {
  color: #9ca3af;
  margin: 0 8px;
}

.action-buttons {
  position: absolute;
  bottom: 70px;
  left: 0;
  right: 0;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #ffffff;
  border-top: 1px solid #f1f5f9;
  z-index: 1001;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
}

.save-btn {
  width: 100%;
  padding: 12px 20px;
  background: #a7cc10;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
}

.save-btn:hover:not(:disabled) {
  background: #90b403;;
}

.save-btn:disabled {
  background: #c2d477;
  cursor: not-allowed;
}

.cancel-btn {
  width: 100%;
  padding: 12px 20px;
  background: #ffffff;
  color: #a7cc10;
  border: 1px solid #a7cc10;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 500;
  cursor: pointer;
}

.cancel-btn:hover {
  background: #f9fafb;
}
</style>

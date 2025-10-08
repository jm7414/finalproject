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
      <div class="header-right">
        <button class="profile-btn" @click="goToMypage">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
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

      <!-- 시간 선택 -->
      <div class="form-group">
        <div class="time-inputs">
          <div class="time-input-wrapper" @click="openTimePicker('start')">
            <input 
              v-model="displayStartTime"
              type="text" 
              class="form-input time-input"
              placeholder="시작 시간"
              readonly
              required
            />
            <svg class="time-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <polyline points="12,6 12,12 16,14" stroke="currentColor" stroke-width="2"/>
            </svg>
          </div>
          <div class="time-input-wrapper" @click="openTimePicker('end')">
            <input 
              v-model="displayEndTime"
              type="text" 
              class="form-input time-input"
              placeholder="종료 시간"
              readonly
              required
            />
            <svg class="time-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <polyline points="12,6 12,12 16,14" stroke="currentColor" stroke-width="2"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- 일정 위치 추가 -->
      <div class="form-group">
        <div class="location-input" @click="goToSearchRoute">
          <div class="location-content">
            <svg class="location-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg class="plus-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="location-text">일정 위치 추가</span>
          </div>
        </div>
        <div class="location-hint">
          # 해당 위치를 기반으로 안심존이 활성화 됩니다.
        </div>
      </div>
    </div>

    <!-- 액션 버튼 -->
    <div class="action-buttons">
      <button class="save-btn" @click="saveSchedule" :disabled="!isFormValid">
        저장
      </button>
      <button class="cancel-btn" @click="cancelSchedule">
        취소
      </button>
    </div>

    <!-- 시간 선택 모달 -->
    <div v-if="showTimePicker" class="time-picker-overlay" @click="closeTimePicker">
      <div class="time-picker-modal" @click.stop>
        <div class="time-picker-header">
          <h3>{{ timePickerType === 'start' ? '시작 시간 선택' : '종료 시간 선택' }}</h3>
          <button class="close-btn" @click="closeTimePicker">✕</button>
        </div>
        <div class="time-picker-body">
          <div class="time-picker-scrolls">
            <!-- 오전/오후 -->
            <div class="time-scroll">
              <div 
                v-for="period in periods" 
                :key="period"
                class="scroll-item"
                :class="{ active: selectedPeriod === period }"
                @click="selectedPeriod = period"
              >
                {{ period }}
              </div>
            </div>
            <!-- 시 -->
            <div class="time-scroll">
              <div 
                v-for="hour in hours" 
                :key="hour"
                class="scroll-item"
                :class="{ active: selectedHour === hour }"
                @click="selectedHour = hour"
              >
                {{ hour }}
              </div>
            </div>
            <!-- 분 -->
            <div class="time-scroll">
              <div 
                v-for="minute in minutes" 
                :key="minute"
                class="scroll-item"
                :class="{ active: selectedMinute === minute }"
                @click="selectedMinute = minute"
              >
                {{ minute }}
              </div>
            </div>
          </div>
        </div>
        <div class="time-picker-footer">
          <button class="confirm-btn" @click="confirmTime">확인</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 폼 데이터
const scheduleForm = ref({
  title: '',
  content: '',
  date: '',
  startTime: '',
  endTime: '',
  location: null
})

// 시간 선택기 관련
const showTimePicker = ref(false)
const timePickerType = ref('start')
const selectedPeriod = ref('오전')
const selectedHour = ref('01')
const selectedMinute = ref('00')

// 시간 옵션 (반복되지 않도록 수정)
const periods = ['오전', '오후']
const hours = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))
const minutes = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'))

// 표시용 시간 (placeholder 대체)
const displayStartTime = computed(() => scheduleForm.value.startTime || '')
const displayEndTime = computed(() => scheduleForm.value.endTime || '')

// 폼 유효성 검사
const isFormValid = computed(() => {
  return scheduleForm.value.title.trim() !== '' &&
         scheduleForm.value.date !== '' &&
         scheduleForm.value.startTime !== '' &&
         scheduleForm.value.endTime !== ''
})

// 뒤로 가기
function goBack() {
  router.go(-1)
}

// 마이페이지로 이동
function goToMypage() {
  router.push({ name: 'gdmypage' })
}

// 시간 선택기 열기
function openTimePicker(type) {
  timePickerType.value = type
  
  // 기존 시간이 있으면 파싱
  const currentTime = type === 'start' ? scheduleForm.value.startTime : scheduleForm.value.endTime
  if (currentTime) {
    const parts = currentTime.split(' ')
    selectedPeriod.value = parts[0]
    const [hour, minute] = parts[1].split(':')
    selectedHour.value = hour
    selectedMinute.value = minute
  } else {
    selectedPeriod.value = '오전'
    selectedHour.value = '01'
    selectedMinute.value = '00'
  }
  
  showTimePicker.value = true
}

// 시간 선택기 닫기
function closeTimePicker() {
  showTimePicker.value = false
}

// 시간 확인
function confirmTime() {
  const timeString = `${selectedPeriod.value} ${selectedHour.value}:${selectedMinute.value}`
  
  if (timePickerType.value === 'start') {
    scheduleForm.value.startTime = timeString
  } else {
    scheduleForm.value.endTime = timeString
  }
  
  closeTimePicker()
}

// 경로 검색 페이지로 이동
function goToSearchRoute() {
  sessionStorage.setItem('scheduleFormData', JSON.stringify(scheduleForm.value))
  router.push({ name: 'search-route' })
}

// 일정 저장
function saveSchedule() {
  if (!isFormValid.value) {
    alert('필수 항목을 모두 입력해주세요.')
    return
  }

  console.log('일정 저장:', scheduleForm.value)
  router.push({ name: 'calendar' })
}

// 일정 취소
function cancelSchedule() {
  if (confirm('작성 중인 내용이 사라집니다. 정말 취소하시겠습니까?')) {
    router.push({ name: 'calendar' })
  }
}

onMounted(() => {
  // 캘린더에서 선택된 날짜가 있는지 확인
  const selectedDate = sessionStorage.getItem('selectedDate')
  if (selectedDate) {
    // 캘린더에서 선택된 날짜 사용
    scheduleForm.value.date = selectedDate
    sessionStorage.removeItem('selectedDate')
  } else {
    // 기본값으로 오늘 날짜 설정
    const today = new Date()
    const year = today.getFullYear()
    const month = String(today.getMonth() + 1).padStart(2, '0')
    const day = String(today.getDate()).padStart(2, '0')
    scheduleForm.value.date = `${year}-${month}-${day}`
  }
  
  // 경로 검색에서 돌아온 경우 폼 데이터 복원
  const savedFormData = sessionStorage.getItem('scheduleFormData')
  if (savedFormData) {
    const parsedData = JSON.parse(savedFormData)
    scheduleForm.value = { ...scheduleForm.value, ...parsedData }
    sessionStorage.removeItem('scheduleFormData')
  }
})
</script>

<style scoped>
.add-schedule-page {
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  padding-bottom: 80px;
}

/* 헤더 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  width: 40px;
  display: flex;
  justify-content: center;
}

.back-btn, .profile-btn {
  background: none;
  border: none;
  color: #111827;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.back-btn:hover, .profile-btn:hover {
  background: #f9fafb;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

/* 폼 */
.form-section {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
  font-size: 14px;
  color: #111827;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: #6366f1;
  background: #f5f7ff;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
}

/* 시간 입력 */
.time-inputs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.time-input-wrapper {
  position: relative;
  cursor: pointer;
}

.time-input {
  cursor: pointer;
  padding-right: 40px;
}

.time-input::placeholder {
  color: #9ca3af;
}

.time-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}

.time-input-wrapper:hover .form-input {
  border-color: #6366f1;
  background: #f5f7ff;
}

/* 시간 선택 모달 */
.time-picker-overlay {
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
}

.time-picker-modal {
  background: #ffffff;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
}

.time-picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.time-picker-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #6b7280;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.time-picker-body {
  padding: 20px;
}

.time-picker-scrolls {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.time-scroll {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
}

.scroll-item {
  padding: 12px;
  text-align: center;
  font-size: 16px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.scroll-item:hover {
  background: #f3f4f6;
}

.scroll-item.active {
  background: #6366f1;
  color: #ffffff;
  font-weight: 600;
}

.time-picker-footer {
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}

.confirm-btn {
  width: 100%;
  padding: 12px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.confirm-btn:hover {
  background: #4f46e5;
}

/* 위치 입력 */
.location-input {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
}

.location-input:hover {
  border-color: #6366f1;
  background: #f5f7ff;
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
  color: #6366f1;
}

.location-text {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
}

.location-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
  padding: 0 16px;
}

/* 액션 버튼 */
.action-buttons {
  padding: 0 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.save-btn {
  width: 100%;
  padding: 12px 20px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.save-btn:hover:not(:disabled) {
  background: #4f46e5;
}

.save-btn:disabled {
  background: #a5b4fc;
  cursor: not-allowed;
}

.cancel-btn {
  width: 100%;
  padding: 12px 20px;
  background: #ffffff;
  color: #6366f1;
  border: 1px solid #6366f1;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #f9fafb;
  border-color: #4f46e5;
}

</style>

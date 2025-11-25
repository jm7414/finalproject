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
        <h1 class="page-title">{{ isEditMode ? '일정 수정' : '일정 추가' }}</h1>
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

    <!-- 토스트 알림 -->
    <div
      v-if="notification.show"
      :class="['toast', notification.type === 'error' ? 'toast-error' : 'toast-success']"
    >
      {{ notification.message }}
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
            <span class="location-text">일정 위치 추가</span>
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
        
        <div class="location-hint">
          # 해당 위치를 기반으로 안심존이 활성화 됩니다.
        </div>
      </div>

      <!-- 액션 버튼 -->
      <div class="action-buttons">
        <button class="save-btn" @click="saveSchedule" :disabled="!isFormValid">
          {{ isEditMode ? '수정 완료' : '저장' }}
        </button>
        <button class="cancel-btn" @click="cancelSchedule">
          취소
        </button>
      </div>
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

    <!-- 확인 모달 (취소용) -->
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

// 수정 모드 여부
const isEditMode = ref(false)
const editScheduleNo = ref(null)

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

// 시간 옵션
const periods = ['오전', '오후']
const hours = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))
const minutes = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'))

// 표시용 시간
const displayStartTime = computed(() => scheduleForm.value.startTime || '')
const displayEndTime = computed(() => scheduleForm.value.endTime || '')

// 위치 데이터 관리
const locationData = ref([])

// 위치 데이터가 있는지 확인
const hasLocationData = computed(() => locationData.value.length > 0)

// 확인 모달 (취소 확인용)
const showConfirmModal = ref(false)
const confirmModalConfig = ref({
  title: '확인',
  message: '',
  confirmText: '확인',
  cancelText: '취소'
})

// 토스트 알림 상태
const notification = ref({
  show: false,
  message: '',
  type: 'info' // 'info' | 'success' | 'error'
})

function showNotification(message, type = 'info') {
  notification.value.message = message
  notification.value.type = type
  notification.value.show = true

  // 2초 후 자동 숨김
  setTimeout(() => {
    notification.value.show = false
  }, 2000)
}

// 폼 유효성 검사
const isFormValid = computed(() => {
  return scheduleForm.value.title.trim() !== '' &&
         scheduleForm.value.date !== '' &&
         scheduleForm.value.startTime !== '' &&
         scheduleForm.value.endTime !== '' &&
         hasLocationData.value
})

// 폼 초기화 함수
function resetScheduleForm() {
  scheduleForm.value = {
    title: '',
    content: '',
    date: '',
    startTime: '',
    endTime: '',
    location: null
  }
  
  locationData.value = []
  isEditMode.value = false
  editScheduleNo.value = null
  
  sessionStorage.removeItem('scheduleFormData')
  sessionStorage.removeItem('editScheduleNo')
  sessionStorage.removeItem('routeCoordinates')
  sessionStorage.removeItem('bufferCoordinates')
  sessionStorage.removeItem('scheduleLocations')
  sessionStorage.removeItem('routeBufferPolygon')
  sessionStorage.removeItem('isScheduleInProgress')
}

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
  sessionStorage.setItem('isScheduleInProgress', 'true')
  router.push({ name: 'search-route' })
}

// 일정 저장
async function saveSchedule() {
  if (!isFormValid.value) {
    showNotification('필수 항목을 모두 입력해주세요.', 'error')
    return
  }

  try {
    const routeCoordinates = sessionStorage.getItem('routeCoordinates')
    const bufferCoordinates = sessionStorage.getItem('bufferCoordinates')
    const scheduleLocations = sessionStorage.getItem('scheduleLocations')

    if (!routeCoordinates || !bufferCoordinates || !scheduleLocations) {
      showNotification('경로 및 안심존 설정이 필요합니다. "일정 위치 추가" 버튼을 눌러 설정해주세요.', 'error')
      return
    }

    const bufferCoordinatesData = JSON.parse(bufferCoordinates)
    let bufferCoordinatesArray
    let bufferLevel = 1
    
    if (Array.isArray(bufferCoordinatesData)) {
      bufferCoordinatesArray = bufferCoordinatesData
    } else if (bufferCoordinatesData.coordinates) {
      bufferCoordinatesArray = bufferCoordinatesData.coordinates
      bufferLevel = bufferCoordinatesData.level || 1
    } else {
      throw new Error('지원하지 않는 bufferCoordinates 형식입니다.')
    }

    const requestData = {
      title: scheduleForm.value.title,
      content: scheduleForm.value.content,
      date: scheduleForm.value.date,
      startTime: scheduleForm.value.startTime,
      endTime: scheduleForm.value.endTime,
      locations: JSON.parse(scheduleLocations),
      routeCoordinates: JSON.parse(routeCoordinates),
      bufferCoordinates: bufferCoordinatesArray,
      bufferLevel: bufferLevel
    }

    const url = isEditMode.value
      ? `/api/schedule/update/${editScheduleNo.value}`
      : `/api/schedule/create`
    
    const successMessage = isEditMode.value ? '일정이 성공적으로 수정되었습니다.' : '일정이 성공적으로 저장되었습니다.'

    console.log(`일정 ${isEditMode.value ? '수정' : '저장'} 요청:`, requestData)

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify(requestData)
    })

    if (!response.ok) {
      let errorMessage = `일정 ${isEditMode.value ? '수정' : '저장'}에 실패했습니다.`
      try {
        const error = await response.json()
        if (error && error.message) {
          errorMessage = error.message
        }
      } catch (_) {
        // JSON 파싱 실패 시 기본 메시지 사용
      }
      throw new Error(errorMessage)
    }

    const result = await response.json()
    console.log(`일정 ${isEditMode.value ? '수정' : '저장'} 성공:`, result)

    resetScheduleForm()
    showNotification(successMessage, 'success')

    // 살짝 딜레이 후 이동하면 토스트가 잠깐 보였다가 사라짐
    setTimeout(() => {
      router.push({ name: 'calendar' })
    }, 300)

  } catch (error) {
    console.error(`일정 ${isEditMode.value ? '수정' : '저장'} 오류:`, error)
    showNotification(
      error.message || `일정 ${isEditMode.value ? '수정' : '저장'} 중 오류가 발생했습니다.`,
      'error'
    )
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
  router.push({ name: 'calendar' })
}

// 확인 모달에서 취소 버튼 클릭 시
function handleCancelCancel() {
  // 그대로 작성 계속
}

// 확인 모달 닫기
function closeConfirmModal() {
  showConfirmModal.value = false
}

// 시간을 "오전/오후 HH:MM" 형식으로 변환
function formatTimeForDisplay(timeString) {
  if (!timeString) return ''
  
  const [hour, minute] = timeString.split(':')
  const hourNum = parseInt(hour)
  
  if (hourNum === 0) {
    return `오전 12:${minute}`
  } else if (hourNum < 12) {
    return `오전 ${String(hourNum).padStart(2, '0')}:${minute}`
  } else if (hourNum === 12) {
    return `오후 12:${minute}`
  } else {
    return `오후 ${String(hourNum - 12).padStart(2, '0')}:${minute}`
  }
}

// 수정할 일정 데이터 불러오기
async function loadScheduleForEdit(scheduleNo) {
  try {
    const scheduleResponse = await fetch(`/api/schedule/${scheduleNo}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!scheduleResponse.ok) {
      throw new Error('일정 정보를 불러올 수 없습니다.')
    }
    
    const schedule = await scheduleResponse.json()
    
    scheduleForm.value.title = schedule.scheduleTitle
    scheduleForm.value.content = schedule.content || ''
    scheduleForm.value.date = schedule.scheduleDate
    scheduleForm.value.startTime = formatTimeForDisplay(schedule.startTime)
    scheduleForm.value.endTime = formatTimeForDisplay(schedule.endTime)
    
    const locationsResponse = await fetch(`/api/schedule/${scheduleNo}/locations`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (locationsResponse.ok) {
      const locations = await locationsResponse.json()
      locationData.value = locations.map((loc, index) => ({
        name: loc.locationName,
        latitude: parseFloat(loc.latitude),
        longitude: parseFloat(loc.longitude),
        sequenceOrder: loc.sequenceOrder || index
      }))
      
      sessionStorage.setItem('scheduleLocations', JSON.stringify(locationData.value))
    }
    
    const routeResponse = await fetch(`/api/schedule/${scheduleNo}/route`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (routeResponse.ok) {
      const route = await routeResponse.json()
      sessionStorage.setItem('routeCoordinates', route.routeCoordinates)
      sessionStorage.setItem('bufferCoordinates', route.bufferCoordinates)
    }
    
  } catch (error) {
    console.error('일정 불러오기 오류:', error)
    showNotification(error.message || '일정을 불러오는 중 오류가 발생했습니다.', 'error')
    router.push({ name: 'calendar' })
  }
}

onMounted(async () => {
  const editScheduleNoFromStorage = sessionStorage.getItem('editScheduleNo')
  if (editScheduleNoFromStorage) {
    isEditMode.value = true
    editScheduleNo.value = parseInt(editScheduleNoFromStorage)
    sessionStorage.setItem('isScheduleInProgress', 'true')
    await loadScheduleForEdit(editScheduleNo.value)
    return
  }
  
  const isInProgress = sessionStorage.getItem('isScheduleInProgress')
  
  if (!isInProgress) {
    resetScheduleForm()
    
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
    
    sessionStorage.setItem('isScheduleInProgress', 'true')
  }
  
  if (isInProgress) {
    const savedFormData = sessionStorage.getItem('scheduleFormData')
    if (savedFormData) {
      const parsedData = JSON.parse(savedFormData)
      scheduleForm.value = { ...scheduleForm.value, ...parsedData }
      sessionStorage.removeItem('scheduleFormData')
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
.add-schedule-page {
  height: 100%;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
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

/* 토스트 알림 */
.toast {
  position: fixed;
  top: 72px; /* 헤더 바로 아래 */
  left: 50%;
  transform: translateX(-50%);
  max-width: 90%;
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  text-align: center;
  z-index: 1100;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.15);
}

.toast-success {
  background: #ecfdf5;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.toast-error {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
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

/* 위치 표시 스타일 */
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
  transition: background-color 0.2s;
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

.location-item:last-child {
  margin-bottom: 0;
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
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-name {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
  margin-bottom: 2px;
}

.step-address {
  font-size: 12px;
  color: #9ca3af;
}

.route-arrow {
  color: #9ca3af;
  margin: 0 8px;
  flex-shrink: 0;
}

/* 액션 버튼 */
.action-buttons {
  margin-top: 20px;
  padding: 0;
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

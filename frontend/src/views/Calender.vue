<template>
  <div class="calendar-page">

    <!-- 캘린더 섹션 -->
    <div class="calendar-section">
      <!-- 월/년도 표시 및 네비게이션 -->
      <div class="calendar-header">
        <div class="nav-arrows">
          <button class="nav-btn" @click="previousMonth">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div class="month-year-center">
          <span class="month-year-text">{{ currentYear }}년 {{ currentMonth }}</span>
        </div>
        <div class="nav-arrows">
          <button class="nav-btn" @click="nextMonth">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M9 18L15 12L9 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 요일 헤더 -->
      <div class="weekdays">
        <div class="weekday" v-for="day in weekdays" :key="day">{{ day }}</div>
      </div>

      <!-- 캘린더 그리드 -->
      <div class="calendar-grid">
        <div 
          v-for="date in calendarDates" 
          :key="date.key"
          class="calendar-day"
          :class="{
            'other-month': !date.isCurrentMonth,
            'today': date.isToday,
            'selected': date.isSelected,
            'isEmpty': date.isEmpty
          }"
          @click="selectDate(date)"
        >
          <span class="day-number">{{ date.day }}</span>
          <div class="event-dots" v-if="date.events && date.events.length > 0">
            <div 
              v-for="(event, index) in date.events.slice(0, 3)" 
              :key="index"
              class="event-dot"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 일정 목록 섹션 -->
    <div class="schedule-section">
      <!-- 오늘 일정 -->
      <div class="schedule-group">
        <h3 class="schedule-title">오늘 일정</h3>
        <div class="schedule-list">
          <div 
            v-for="schedule in todaySchedules" 
            :key="schedule.id" 
            class="schedule-item"
            @click="openScheduleDetailFromList(schedule.id)"
          >
            <div class="schedule-content">
              <div class="schedule-time">{{ schedule.time }}</div>
              <div class="schedule-title">{{ schedule.title }}</div>
              <div class="schedule-location">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>{{ schedule.location }}</span>
              </div>
            </div>
          </div>
          <div v-if="todaySchedules.length === 0" class="no-schedule">
            오늘 일정이 없습니다.
          </div>
        </div>
      </div>

      <!-- 내일 일정 -->
      <div class="schedule-group">
        <h3 class="schedule-title">내일 일정</h3>
        <div class="schedule-list">
          <div 
            v-for="schedule in tomorrowSchedules" 
            :key="schedule.id" 
            class="schedule-item"
            @click="openScheduleDetailFromList(schedule.id)"
          >
            <div class="schedule-content">
              <div class="schedule-time">{{ schedule.time }}</div>
              <div class="schedule-title">{{ schedule.title }}</div>
              <div class="schedule-location">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>{{ schedule.location }}</span>
              </div>
            </div>
          </div>
          <div v-if="tomorrowSchedules.length === 0" class="no-schedule">
            내일 일정이 없습니다.
          </div>
        </div>
      </div>
    </div>

    <!-- 플로팅 액션 버튼 -->
    <button class="fab" @click="goToAddSchedule">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>

    <!-- 날짜별 일정 목록 모달 -->
    <div v-if="showDateSchedulesModal" class="modal-overlay" @click="closeDateSchedulesModal">
      <div class="modal-container schedule-list-modal" @click.stop>
        <div class="modal-header">
          <h3>{{ selectedDateFormatted }}</h3>
          <button class="close-btn" @click="closeDateSchedulesModal">✕</button>
        </div>
        
        <div class="modal-body">
          <div 
            v-for="schedule in selectedDateSchedules" 
            :key="schedule.scheduleNo"
            class="schedule-card"
            @click="openScheduleDetail(schedule)"
          >
            <div class="schedule-card-title">{{ schedule.scheduleTitle }}</div>
            <div class="schedule-card-time">{{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}</div>
            <div class="schedule-card-location">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>{{ formatLocation(schedule.scheduleNo) }}</span>
            </div>
          </div>

          <div v-if="selectedDateSchedules.length === 0" class="no-schedule-modal">
            이 날짜에 일정이 없습니다.
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="addScheduleForDate" class="add-schedule-btn">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            이 날짜에 일정 추가
          </button>
        </div>
      </div>
    </div>

    <!-- 일정 상세 모달 -->
    <div v-if="showScheduleDetailModal" class="modal-overlay" @click="closeScheduleDetailModal">
      <div class="modal-container schedule-detail-modal" @click.stop>
        <div class="modal-header">
          <h3>일정 상세</h3>
          <button class="close-btn" @click="closeScheduleDetailModal">✕</button>
        </div>
        
        <div class="modal-body detail-body">
          <div class="detail-row">
            <label>제목</label>
            <div class="detail-value">{{ selectedSchedule?.scheduleTitle }}</div>
          </div>
          <div class="detail-row">
            <label>내용</label>
            <div class="detail-value">{{ selectedSchedule?.content || '내용 없음' }}</div>
          </div>
          <div class="detail-row">
            <label>날짜</label>
            <div class="detail-value">{{ selectedSchedule?.scheduleDate }}</div>
          </div>
          <div class="detail-row">
            <label>시간</label>
            <div class="detail-value">
              {{ formatTime(selectedSchedule?.startTime) }} - {{ formatTime(selectedSchedule?.endTime) }}
            </div>
          </div>
          <div class="detail-row">
            <label>경로</label>
            <div class="detail-value">{{ formatLocation(selectedSchedule?.scheduleNo) || '위치 없음' }}</div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="editSchedule" class="edit-btn">수정하기</button>
          <button @click="confirmDeleteSchedule" class="delete-btn">삭제하기</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 현재 날짜 정보
const currentDate = ref(new Date())
const selectedDate = ref(new Date())

// 환자 및 일정 데이터
const patientUserNo = ref(null)
const allSchedules = ref([])
const scheduleLocations = ref({}) // scheduleNo를 키로 하는 위치 정보 맵

// 모달 관련 상태
const showDateSchedulesModal = ref(false)
const showScheduleDetailModal = ref(false)
const selectedDateForModal = ref(null)
const selectedSchedule = ref(null)

// 요일 배열 (한글)
const weekdays = ['월', '화', '수', '목', '금', '토', '일']

// 월 이름 배열
const monthNames = [
  '1월', '2월', '3월', '4월', '5월', '6월',
  '7월', '8월', '9월', '10월', '11월', '12월'
]

// 현재 월/년도
const currentMonth = computed(() => monthNames[currentDate.value.getMonth()])
const currentYear = computed(() => currentDate.value.getFullYear())

// 선택된 날짜의 일정 목록
const selectedDateSchedules = computed(() => {
  if (!selectedDateForModal.value) return []
  
  const dateKey = selectedDateForModal.value
  return allSchedules.value.filter(schedule => schedule.scheduleDate === dateKey)
})

// 선택된 날짜 포맷팅
const selectedDateFormatted = computed(() => {
  if (!selectedDateForModal.value) return ''
  
  const [year, month, day] = selectedDateForModal.value.split('-')
  return `${year}년 ${parseInt(month)}월 ${parseInt(day)}일`
})

// 캘린더 날짜 생성
const calendarDates = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  
  // 이번 달 첫째 날
  const firstDay = new Date(year, month, 1)
  // 이번 달 마지막 날
  const lastDay = new Date(year, month + 1, 0)
  // 이번 달 첫째 날의 요일 (0=일요일, 1=월요일...)
  const firstDayOfWeek = (firstDay.getDay() + 6) % 7 // 월요일을 0으로 변환
  
  const dates = []
  
  // 이전 달의 빈 셀 (첫 주를 채우기 위해)
  for (let i = 0; i < firstDayOfWeek; i++) {
    dates.push({
      day: '',
      date: null,
      isCurrentMonth: false,
      isToday: false,
      isSelected: false,
      key: `prev-${i}`,
      events: [],
      isEmpty: true
    })
  }
  
  // 이번 달의 날들
  for (let day = 1; day <= lastDay.getDate(); day++) {
    const date = new Date(year, month, day)
    const isToday = isSameDay(date, new Date())
    const isSelected = isSameDay(date, selectedDate.value)
    
    dates.push({
      day,
      date,
      isCurrentMonth: true,
      isToday,
      isSelected,
      key: `current-${day}`,
      events: getEventsForDate(date)
    })
  }
  
  // 마지막 주를 완성하기 위한 빈 셀만 추가 (7의 배수로 맞추기)
  const remainingCells = (7 - (dates.length % 7)) % 7
  for (let i = 1; i <= remainingCells; i++) {
    dates.push({
      day: '',
      date: null,
      isCurrentMonth: false,
      isToday: false,
      isSelected: false,
      key: `next-${i}`,
      events: [],
      isEmpty: true
    })
  }
  
  return dates
})

// 날짜 비교 함수
function isSameDay(date1, date2) {
  return date1.getFullYear() === date2.getFullYear() &&
         date1.getMonth() === date2.getMonth() &&
         date1.getDate() === date2.getDate()
}

// 시간을 12시간 형식으로 변환 (오전/오후 포함)
function formatTime(timeString) {
  if (!timeString) return ''
  
  // timeString: "09:00:00" or "14:30:00"
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

// 위치 정보를 화살표 형식으로 포맷팅
function formatLocation(scheduleNo) {
  const locations = scheduleLocations.value[scheduleNo]
  if (!locations || locations.length === 0) return ''
  
  // sequence_order 순서대로 정렬
  const sortedLocations = [...locations].sort((a, b) => a.sequenceOrder - b.sequenceOrder)
  
  // 위치명을 화살표로 연결
  return sortedLocations.map(loc => loc.locationName).join(' → ')
}

// 특정 날짜의 이벤트 가져오기
function getEventsForDate(date) {
  // 로컬 시간대 기준으로 YYYY-MM-DD 형식 생성 (UTC 변환 없이)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dateKey = `${year}-${month}-${day}`
  
  // 해당 날짜의 일정 필터링
  const daySchedules = allSchedules.value.filter(schedule => {
    return schedule.scheduleDate === dateKey
  })
  
  // 일정 개수만큼 이벤트 반환 (최대 3개 표시)
  return daySchedules.slice(0, 3).map(schedule => ({
    id: schedule.scheduleNo,
    title: schedule.scheduleTitle
  }))
}

// 오늘 일정
const todaySchedules = computed(() => {
  const today = new Date()
  // 로컬 시간대 기준으로 YYYY-MM-DD 형식 생성 (UTC 변환 없이)
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  const todayKey = `${year}-${month}-${day}`
  
  return allSchedules.value
    .filter(schedule => schedule.scheduleDate === todayKey)
    .map(schedule => ({
      id: schedule.scheduleNo,
      time: `${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}`,
      title: schedule.scheduleTitle,
      location: formatLocation(schedule.scheduleNo),
      completed: false
    }))
})

// 내일 일정
const tomorrowSchedules = computed(() => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  // 로컬 시간대 기준으로 YYYY-MM-DD 형식 생성 (UTC 변환 없이)
  const year = tomorrow.getFullYear()
  const month = String(tomorrow.getMonth() + 1).padStart(2, '0')
  const day = String(tomorrow.getDate()).padStart(2, '0')
  const tomorrowKey = `${year}-${month}-${day}`
  
  return allSchedules.value
    .filter(schedule => schedule.scheduleDate === tomorrowKey)
    .map(schedule => ({
      id: schedule.scheduleNo,
      time: `${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}`,
      title: schedule.scheduleTitle,
      location: formatLocation(schedule.scheduleNo),
      completed: false
    }))
})

// 이전 달로 이동
function previousMonth() {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() - 1, 1)
}

// 다음 달로 이동
function nextMonth() {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() + 1, 1)
}

// 날짜 선택
function selectDate(date) {
  if (date.isCurrentMonth && date.date) {
    selectedDate.value = date.date
    
    // 날짜를 YYYY-MM-DD 형식으로 변환
    const year = date.date.getFullYear()
    const month = String(date.date.getMonth() + 1).padStart(2, '0')
    const day = String(date.date.getDate()).padStart(2, '0')
    const dateString = `${year}-${month}-${day}`
    
    // 해당 날짜에 일정이 있는지 확인
    const hasSchedules = allSchedules.value.some(schedule => schedule.scheduleDate === dateString)
    
    if (hasSchedules) {
      // 일정이 있으면 모달 표시
      selectedDateForModal.value = dateString
      showDateSchedulesModal.value = true
    } else {
      // 일정이 없으면 바로 추가 페이지로 이동
      goToAddScheduleWithDate(date.date)
    }
  }
}

// 특정 날짜로 일정 추가 페이지 이동
function goToAddScheduleWithDate(date) {
  // 선택된 날짜를 YYYY-MM-DD 형식으로 변환
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dateString = `${year}-${month}-${day}`
  
  // 세션 스토리지에 선택된 날짜 저장
  sessionStorage.setItem('selectedDate', dateString)
  
  // 일정 추가 페이지로 이동
  router.push({ name: 'add-schedule' })
}


// 일정 추가 페이지로 이동
function goToAddSchedule() {
  router.push({ name: 'add-schedule' })
}

// 날짜별 일정 모달 닫기
function closeDateSchedulesModal() {
  showDateSchedulesModal.value = false
  selectedDateForModal.value = null
}

// 선택된 날짜에 일정 추가
function addScheduleForDate() {
  if (!selectedDateForModal.value) return
  
  // 선택된 날짜를 세션 스토리지에 저장
  sessionStorage.setItem('selectedDate', selectedDateForModal.value)
  
  // 모달 닫기
  closeDateSchedulesModal()
  
  // 일정 추가 페이지로 이동
  router.push({ name: 'add-schedule' })
}

// 일정 상세 모달 열기
function openScheduleDetail(schedule) {
  selectedSchedule.value = schedule
  showScheduleDetailModal.value = true
  // 날짜 목록 모달은 닫지 않음 (뒤로가기 느낌을 위해)
}

// 일정 상세 모달 닫기
function closeScheduleDetailModal() {
  showScheduleDetailModal.value = false
  selectedSchedule.value = null
}

// 일정 수정
function editSchedule() {
  if (!selectedSchedule.value) return
  
  // 수정할 일정 번호를 세션 스토리지에 저장
  sessionStorage.setItem('editScheduleNo', selectedSchedule.value.scheduleNo)
  
  // 모달 닫기
  closeScheduleDetailModal()
  closeDateSchedulesModal()
  
  // 일정 추가 페이지로 이동 (수정 모드)
  router.push({ name: 'add-schedule' })
}

// 일정 삭제 확인
async function confirmDeleteSchedule() {
  if (!selectedSchedule.value) return
  
  if (!confirm('정말로 이 일정을 삭제하시겠습니까?')) {
    return
  }
  
  try {
    const response = await fetch(`/api/schedule/delete/${selectedSchedule.value.scheduleNo}`, {
      method: 'POST',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('일정 삭제에 실패했습니다.')
    }
    
    alert('일정이 삭제되었습니다.')
    
    // 모달 닫기
    closeScheduleDetailModal()
    closeDateSchedulesModal()
    
    // 일정 목록 새로고침
    await loadAllData()
    
  } catch (error) {
    console.error('일정 삭제 오류:', error)
    alert(error.message || '일정 삭제 중 오류가 발생했습니다.')
  }
}

// 오늘/내일 일정 아이템 클릭 시 상세 모달 열기
function openScheduleDetailFromList(scheduleNo) {
  const schedule = allSchedules.value.find(s => s.scheduleNo === scheduleNo)
  if (schedule) {
    openScheduleDetail(schedule)
  }
}

// 보호자가 관리하는 환자 정보 가져오기
async function fetchPatientInfo() {
  try {
    const response = await fetch(`/api/user/my-patient`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('환자 정보를 가져올 수 없습니다.')
    }
    
    const patient = await response.json()
    
    // 메시지만 있는 경우 (환자가 없는 경우)
    if (patient.message) {
      console.warn(patient.message)
      return null
    }
    
    return patient.userNo
  } catch (error) {
    console.error('환자 정보 조회 오류:', error)
    return null
  }
}

// 일정 목록 가져오기
async function fetchSchedules(userNo) {
  try {
    const response = await fetch(`/api/schedule/list/${userNo}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('일정 목록을 가져올 수 없습니다.')
    }
    
    const schedules = await response.json()
    return schedules
  } catch (error) {
    console.error('일정 목록 조회 오류:', error)
    return []
  }
}

// 특정 일정의 위치 목록 가져오기
async function fetchScheduleLocations(scheduleNo) {
  try {
    const response = await fetch(`/api/schedule/${scheduleNo}/locations`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('위치 정보를 가져올 수 없습니다.')
    }
    
    const locations = await response.json()
    return locations
  } catch (error) {
    console.error('위치 정보 조회 오류:', error)
    return []
  }
}

// 모든 데이터 로드
async function loadAllData() {
  // 1. 환자 정보 조회
  const userNo = await fetchPatientInfo()
  if (!userNo) {
    console.warn('관리하는 환자가 없습니다.')
    return
  }
  
  patientUserNo.value = userNo
  
  // 2. 일정 목록 조회
  const schedules = await fetchSchedules(userNo)
  allSchedules.value = schedules
  
  // 3. 각 일정의 위치 정보 조회
  for (const schedule of schedules) {
    const locations = await fetchScheduleLocations(schedule.scheduleNo)
    scheduleLocations.value[schedule.scheduleNo] = locations
  }
}

onMounted(() => {
  // 컴포넌트 마운트 시 데이터 로드
  loadAllData()
})
</script>

<style scoped>
.calendar-page {
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  padding-bottom: 80px; /* 하단 네비게이션 공간 확보 */
}


/* 캘린더 섹션 */
.calendar-section {
  padding: 20px;
  background: #ffffff;
}

.calendar-header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.month-year-center {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.month-year-text {
  font-size: 22px;
  font-weight: 700;
  color: #111827;
}

.nav-arrows {
  display: flex;
  gap: 8px;
  width: 40px;
  justify-content: center;
}

.nav-btn {
  background: none;
  border: none;
  color: #6366f1;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.nav-btn:hover {
  background: #f5f7ff;
}

/* 요일 헤더 */
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 1px;
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 14px;
  color: #9ca3af;
  padding: 8px 0;
  font-weight: 600;
}

/* 캘린더 그리드 */
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 1px;
  background: #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.calendar-day {
  background: #ffffff;
  min-height: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: background-color 0.2s;
}

.calendar-day:hover {
  background: #f9fafb;
}

.calendar-day.other-month {
  color: #d1d5db;
}

.calendar-day.isEmpty {
  cursor: default;
  pointer-events: none;
}

.calendar-day.today {
  background: #f5f7ff;
}

.calendar-day.today .day-number {
  background: #6366f1;
  color: #ffffff;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.calendar-day.selected {
  background: #f5f7ff;
}

.day-number {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 4px;
}

.event-dots {
  display: flex;
  gap: 2px;
  justify-content: center;
}

.event-dot {
  width: 4px;
  height: 4px;
  background: #6366f1;
  border-radius: 50%;
}

/* 일정 섹션 */
.schedule-section {
  flex: 1;
  padding: 0 20px;
  background: #ffffff;
}

.schedule-group {
  margin-bottom: 24px;
}

.schedule-title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px 0;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.schedule-item {
  padding: 12px;
  background: #f9fafb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.schedule-item:hover {
  background: #f3f4f6;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.schedule-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.schedule-time {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
}

.schedule-title {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
}

.schedule-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.no-schedule {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 20px;
}

/* 플로팅 액션 버튼 */
.fab {
  position: fixed;
  bottom: 100px;
  right: calc((100vw - 375px) / 2 + 20px);
  width: 56px;
  height: 56px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
  transition: all 0.2s;
  z-index: 1001;
}

/* 작은 화면에서는 화면 오른쪽에 고정 */
@media (max-width: 415px) {
  .fab {
    right: 20px;
  }
}

.fab:hover {
  background: #4f46e5;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.5);
}


/* 모달 오버레이 */
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

/* 모달 컨테이너 */
.modal-container {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 500px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

/* 모달 헤더 */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
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
  border-radius: 8px;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background: #f3f4f6;
}

/* 모달 바디 */
.modal-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

/* 일정 목록 모달 - 일정 카드 */
.schedule-card {
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.schedule-card:hover {
  background: #f3f4f6;
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.schedule-card:last-child {
  margin-bottom: 0;
}

.schedule-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.schedule-card-time {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.schedule-card-location {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #9ca3af;
}

.schedule-card-location svg {
  flex-shrink: 0;
}

.no-schedule-modal {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 40px 20px;
}

/* 모달 푸터 */
.modal-footer {
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}

.add-schedule-btn {
  width: 100%;
  padding: 12px 20px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.add-schedule-btn:hover {
  background: #4f46e5;
}

/* 일정 상세 모달 */
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-row label {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
}

.detail-value {
  font-size: 15px;
  color: #111827;
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 8px;
  word-break: break-word;
}

.edit-btn, .delete-btn {
  width: 100%;
  padding: 12px 20px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.edit-btn {
  background: #6366f1;
  color: #ffffff;
}

.edit-btn:hover {
  background: #4f46e5;
}

.delete-btn {
  background: #ffffff;
  color: #ef4444;
  border: 1px solid #ef4444;
}

.delete-btn:hover {
  background: #fef2f2;
}

.delete-btn:last-child {
  margin-bottom: 0;
}

/* 반응형 디자인 */
@media (max-width: 480px) {
  .calendar-day {
    min-height: 50px;
  }
  
  .day-number {
    font-size: 12px;
  }
  
  .schedule-item {
    padding: 10px;
  }
  
  .schedule-time {
    min-width: 80px;
    font-size: 11px;
  }

  .modal-overlay {
    padding: 10px;
  }

  .modal-container {
    max-height: 90vh;
  }

  .modal-header, .modal-body, .modal-footer {
    padding: 16px;
  }
}
</style>
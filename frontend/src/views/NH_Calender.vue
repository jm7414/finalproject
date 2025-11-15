<template>
  <div class="calendar-page neighbor-page-container">

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
        <h3 class="schedule-title-header">오늘 일정</h3>
        <div class="schedule-list">
          <div 
            v-for="schedule in todaySchedules" 
            :key="schedule.id" 
            class="schedule-item"
            @click="openScheduleDetailFromList(schedule.id)"
          >
            <div class="schedule-content">
              <div class="schedule-title">{{ schedule.title }}</div>
              <div class="schedule-author">작성자: {{ schedule.author }}</div>
            </div>
          </div>
          <div v-if="todaySchedules.length === 0" class="no-schedule">
            오늘 일정이 없습니다.
          </div>
        </div>
      </div>

      <!-- 내일 일정 -->
      <div class="schedule-group">
        <h3 class="schedule-title-header">내일 일정</h3>
        <div class="schedule-list">
          <div 
            v-for="schedule in tomorrowSchedules" 
            :key="schedule.id" 
            class="schedule-item"
            @click="openScheduleDetailFromList(schedule.id)"
          >
            <div class="schedule-content">
              <div class="schedule-title">{{ schedule.title }}</div>
              <div class="schedule-author">작성자: {{ schedule.author }}</div>
            </div>
          </div>
          <div v-if="tomorrowSchedules.length === 0" class="no-schedule">
            내일 일정이 없습니다.
          </div>
        </div>
      </div>
    </div>

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
            <div class="schedule-card-author">작성자: {{ schedule.authorName }}</div>
            <div class="schedule-card-content" v-if="schedule.content">{{ schedule.content }}</div>
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
            <div class="detail-value">{{ formatDate(selectedSchedule?.scheduleDate) }}</div>
          </div>
          <div class="detail-row">
            <label>작성자</label>
            <div class="detail-value">{{ selectedSchedule?.authorName }}</div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="confirmDeleteSchedule" class="delete-btn">삭제하기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

// 현재 날짜 정보
const currentDate = ref(new Date())
const selectedDate = ref(new Date())

// 일정 데이터
const allSchedules = ref([])

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

// 날짜 포맷팅
function formatDate(dateString) {
  if (!dateString) return ''
  const [year, month, day] = dateString.split('-')
  return `${year}년 ${parseInt(month)}월 ${parseInt(day)}일`
}

// 캘린더 날짜 생성
const calendarDates = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const firstDayOfWeek = (firstDay.getDay() + 6) % 7
  
  const dates = []
  
  // 이전 달의 빈 셀
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
  
  // 마지막 주 완성
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

// 특정 날짜의 이벤트 가져오기
function getEventsForDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dateKey = `${year}-${month}-${day}`
  
  const daySchedules = allSchedules.value.filter(schedule => {
    return schedule.scheduleDate === dateKey
  })
  
  return daySchedules.slice(0, 3).map(schedule => ({
    id: schedule.scheduleNo,
    title: schedule.scheduleTitle
  }))
}

// 오늘 일정
const todaySchedules = computed(() => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  const todayKey = `${year}-${month}-${day}`
  
  return allSchedules.value
    .filter(schedule => schedule.scheduleDate === todayKey)
    .map(schedule => ({
      id: schedule.scheduleNo,
      title: schedule.scheduleTitle,
      author: schedule.authorName
    }))
})

// 내일 일정
const tomorrowSchedules = computed(() => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  const year = tomorrow.getFullYear()
  const month = String(tomorrow.getMonth() + 1).padStart(2, '0')
  const day = String(tomorrow.getDate()).padStart(2, '0')
  const tomorrowKey = `${year}-${month}-${day}`
  
  return allSchedules.value
    .filter(schedule => schedule.scheduleDate === tomorrowKey)
    .map(schedule => ({
      id: schedule.scheduleNo,
      title: schedule.scheduleTitle,
      author: schedule.authorName
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
    
    const year = date.date.getFullYear()
    const month = String(date.date.getMonth() + 1).padStart(2, '0')
    const day = String(date.date.getDate()).padStart(2, '0')
    const dateString = `${year}-${month}-${day}`
    
    const hasSchedules = allSchedules.value.some(schedule => schedule.scheduleDate === dateString)
    
    if (hasSchedules) {
      selectedDateForModal.value = dateString
      showDateSchedulesModal.value = true
    } else {
      goToAddScheduleWithDate(date.date)
    }
  }
}

// 특정 날짜로 일정 추가 페이지 이동
function goToAddScheduleWithDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dateString = `${year}-${month}-${day}`
  
  sessionStorage.setItem('selectedDate', dateString)
  router.push({ name: 'NH_AddSchedule' })
}

// 일정 추가 페이지로 이동
function goToAddSchedule() {
  router.push({ name: 'NH_AddSchedule' })
}

// 날짜별 일정 모달 닫기
function closeDateSchedulesModal() {
  showDateSchedulesModal.value = false
  selectedDateForModal.value = null
}

// 선택된 날짜에 일정 추가
function addScheduleForDate() {
  if (!selectedDateForModal.value) return
  
  sessionStorage.setItem('selectedDate', selectedDateForModal.value)
  closeDateSchedulesModal()
  router.push({ name: 'NH_AddSchedule' })
}

// 일정 상세 모달 열기
function openScheduleDetail(schedule) {
  selectedSchedule.value = schedule
  showScheduleDetailModal.value = true
}

// 일정 상세 모달 닫기
function closeScheduleDetailModal() {
  showScheduleDetailModal.value = false
  selectedSchedule.value = null
}

// 일정 삭제 확인
async function confirmDeleteSchedule() {
  if (!selectedSchedule.value) return
  
  if (!confirm('정말로 이 일정을 삭제하시겠습니까?')) {
    return
  }
  
  try {
    await axios.delete(`/NH/api/neighbor/schedules/${selectedSchedule.value.scheduleNo}`)
    
    alert('일정이 삭제되었습니다.')
    
    closeScheduleDetailModal()
    closeDateSchedulesModal()
    
    await loadAllData()
    
  } catch (error) {
    console.error('일정 삭제 오류:', error)
    if (error.response && error.response.status === 403) {
      alert('작성자만 일정을 삭제할 수 있습니다.')
    } else {
      alert('일정 삭제 중 오류가 발생했습니다.')
    }
  }
}

// 오늘/내일 일정 아이템 클릭 시 상세 모달 열기
function openScheduleDetailFromList(scheduleNo) {
  const schedule = allSchedules.value.find(s => s.scheduleNo === scheduleNo)
  if (schedule) {
    openScheduleDetail(schedule)
  }
}

// 모든 데이터 로드 (일정 기능으로 인한 수정)
async function loadAllData() {
  try {
    const response = await axios.get('/NH/api/neighbor/schedules')
    allSchedules.value = response.data
    console.log('광장 일정 로드 성공:', response.data)
  } catch (error) {
    console.error('광장 일정 조회 오류:', error)
    allSchedules.value = []
  }
}

onMounted(() => {
  loadAllData()
})
</script>

<style scoped>
/* 기존 스타일 유지 + 일부 수정 */
.calendar-page {
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  padding-bottom: 80px;
  position: relative;
}

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
  color: #a7cc10;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.nav-btn:hover {
  background: #f5f7ff;
}

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
  background: #a7cc10;
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
  background: #a7cc10;
  border-radius: 50%;
}

.schedule-section {
  flex: 1;
  padding: 0 20px;
  background: #ffffff;
}

.schedule-group {
  margin-bottom: 24px;
}

.schedule-title-header {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 12px;
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

.schedule-title {
  font-size: 14px;
  color: #111827;
  font-weight: 600;
}

.schedule-author {
  font-size: 12px;
  color: #9ca3af;
}

.no-schedule {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 20px;
}

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

.modal-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

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

.schedule-card-author {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.schedule-card-content {
  font-size: 14px;
  color: #9ca3af;
}

.no-schedule-modal {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 40px 20px;
}

.modal-footer {
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}

.add-schedule-btn {
  width: 100%;
  padding: 12px 20px;
  background: #a7cc10;
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
  background: #9bbe0f;
}

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

.delete-btn {
  width: 100%;
  padding: 12px 20px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background: #ffffff;
  color: #ef4444;
  border: 1px solid #ef4444;
}

.delete-btn:hover {
  background: #fef2f2;
}

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

  .modal-overlay {
    padding: 10px;
  }

  .modal-container {
    max-height: 90vh;
  }

  .modal-header, .modal-body, .modal-footer {
    padding: 16px;
  }

  .neighbor-page-container {
    padding-bottom: 100px;
    margin-bottom: 30px;
  }
}
</style>

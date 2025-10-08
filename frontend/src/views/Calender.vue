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
          <div v-for="schedule in todaySchedules" :key="schedule.id" class="schedule-item">
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
          <div v-for="schedule in tomorrowSchedules" :key="schedule.id" class="schedule-item">
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

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 현재 날짜 정보
const currentDate = ref(new Date())
const selectedDate = ref(new Date())

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

// 특정 날짜의 이벤트 가져오기 (임시 데이터)
function getEventsForDate(date) {
  // 임시 이벤트 데이터
  const events = {
    '2025-01-13': [{ id: 1, title: '이벤트 1' }, { id: 2, title: '이벤트 2' }, { id: 3, title: '이벤트 3' }],
    '2025-01-28': [{ id: 4, title: '이벤트 4' }],
    '2025-01-29': [{ id: 5, title: '이벤트 5' }, { id: 6, title: '이벤트 6' }]
  }
  
  const dateKey = date.toISOString().split('T')[0]
  return events[dateKey] || []
}

// 오늘 일정 (임시 데이터)
const todaySchedules = ref([
  {
    id: 1,
    time: '11:30 AM - 12:30 PM',
    title: '치매 센터 정기 검진일',
    location: '집 → 구로 고대 병원',
    completed: false
  }
])

// 내일 일정 (임시 데이터)
const tomorrowSchedules = ref([
  {
    id: 2,
    time: '11:30 AM - 12:30 PM',
    title: '마트 들려서 식재료 사오기',
    location: '집 → 구로 한마음마트 → 다이소 구로점',
    completed: false
  },
  {
    id: 3,
    time: '11:30 AM - 12:30 PM',
    title: '다이소에서 수선 재료 구매',
    location: '집 → 구로 한마음마트 → 다이소 구로점',
    completed: false
  }
])

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
  if (date.isCurrentMonth) {
    selectedDate.value = date.date
    // 날짜를 더블클릭하면 일정 추가 페이지로 이동
    goToAddScheduleWithDate(date.date)
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

onMounted(() => {
  // 컴포넌트 마운트 시 초기화
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
  right: 20px;
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
  z-index: 10;
}

.fab:hover {
  background: #4f46e5;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.5);
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
}
</style>
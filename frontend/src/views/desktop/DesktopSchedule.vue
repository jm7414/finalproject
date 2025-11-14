<template>
  <div class="schedule-page">
    <!-- 중앙 영역: 캘린더 + 오늘/내일 일정 -->
    <main class="center-area">
      <!-- 월별 캘린더 -->
      <div class="calendar-container">
        <div class="calendar-header">
          <button class="nav-btn" @click="previousMonth">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <h2 class="month-title">{{ currentYear }}년 {{ currentMonth }}</h2>
          <button class="nav-btn" @click="nextMonth">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M9 18L15 12L9 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <div class="weekdays">
          <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
        </div>

        <div class="calendar-grid">
          <div
            v-for="date in calendarDates"
            :key="date.key"
            class="calendar-day"
            :class="{
              'other-month': !date.isCurrentMonth,
              'today': date.isToday,
              'selected': date.isSelected,
              'has-events': date.events && date.events.length > 0,
              'empty': date.isEmpty
            }"
            @click="selectDate(date)"
          >
            <span class="day-number">{{ date.day }}</span>
            <div v-if="date.events && date.events.length > 0" class="event-indicator">
              {{ date.events.length }}
            </div>
          </div>
        </div>
      </div>

      <!-- 오늘/내일 일정 -->
      <div class="quick-schedules">
        <div class="quick-schedule-section">
          <h3 class="quick-title">오늘 일정</h3>
          <div v-if="todaySchedules.length > 0" class="quick-list">
            <div
              v-for="schedule in todaySchedules"
              :key="schedule.id"
              class="quick-item"
              @click="openScheduleDetail(schedule)"
            >
              <div class="quick-time">{{ schedule.time }}</div>
              <div class="quick-text">{{ schedule.title }}</div>
            </div>
          </div>
          <div v-else class="no-schedule">일정 없음</div>
        </div>

        <div class="quick-schedule-section">
          <h3 class="quick-title">내일 일정</h3>
          <div v-if="tomorrowSchedules.length > 0" class="quick-list">
            <div
              v-for="schedule in tomorrowSchedules"
              :key="schedule.id"
              class="quick-item"
              @click="openScheduleDetail(schedule)"
            >
              <div class="quick-time">{{ schedule.time }}</div>
              <div class="quick-text">{{ schedule.title }}</div>
            </div>
          </div>
          <div v-else class="no-schedule">일정 없음</div>
        </div>
      </div>
    </main>

    <!-- 우측 영역: 선택된 날짜의 일정 목록 -->
    <aside class="right-area">
      <div class="right-header">
        <h2>{{ selectedDateFormatted }}</h2>
        <button class="add-btn" @click="openAddSchedule">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          일정 추가
        </button>
      </div>

      <div v-if="selectedDateSchedules.length > 0" class="schedule-list">
        <div
          v-for="schedule in selectedDateSchedules"
          :key="schedule.scheduleNo"
          class="schedule-card"
          @click="openScheduleDetail(schedule)"
        >
          <h3 class="schedule-title">{{ schedule.scheduleTitle }}</h3>
          <div class="schedule-time">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <polyline points="12,6 12,12 16,14" stroke="currentColor" stroke-width="2"/>
            </svg>
            {{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}
          </div>
          <div v-if="schedule.content" class="schedule-content">
            {{ schedule.content }}
          </div>
          <div class="schedule-location">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2"/>
              <circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2"/>
            </svg>
            {{ formatLocation(schedule.scheduleNo) || '위치 없음' }}
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <svg width="80" height="80" viewBox="0 0 24 24" fill="none">
          <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
          <line x1="3" y1="9" x2="21" y2="9" stroke="currentColor" stroke-width="2"/>
        </svg>
        <p>이 날짜에 일정이 없습니다</p>
      </div>
    </aside>

    <!-- 일정 상세보기 모달 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="detail-modal" @click.stop>
        <div class="modal-header">
          <h2>일정 상세</h2>
          <button class="close-btn" @click="closeDetailModal">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2"/>
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <div class="detail-section">
            <h3>{{ selectedSchedule?.scheduleTitle }}</h3>
            <div class="detail-row">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                <polyline points="12,6 12,12 16,14" stroke="currentColor" stroke-width="2"/>
              </svg>
              <span>{{ formatTime(selectedSchedule?.startTime) }} - {{ formatTime(selectedSchedule?.endTime) }}</span>
            </div>
            <div class="detail-row">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                <line x1="3" y1="9" x2="21" y2="9" stroke="currentColor" stroke-width="2"/>
              </svg>
              <span>{{ selectedSchedule?.scheduleDate }}</span>
            </div>
            <div v-if="selectedSchedule?.content" class="detail-content">
              <strong>내용</strong>
              <p>{{ selectedSchedule.content }}</p>
            </div>
          </div>

          <div v-if="detailRouteCoordinates.length > 0" class="detail-map">
            <h4>경로 지도</h4>
            <div ref="detailMapEl" class="map-view"></div>
          </div>

          <div class="detail-actions">
            <button class="edit-btn" @click="editScheduleFromDetail">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" stroke="currentColor" stroke-width="2"/>
                <path d="m18.5 2.5 3 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="2"/>
              </svg>
              수정하기
            </button>
            <button class="delete-btn" @click="confirmDeleteSchedule">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2"/>
              </svg>
              삭제하기
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 우측 슬라이드 패널: 일정 추가/수정 -->
    <div class="slide-panel" :class="{ open: isPanelOpen }">
      <div class="panel-header">
        <h2>{{ panelMode === 'add' ? '새 일정 추가' : '일정 수정' }}</h2>
        <button class="close-btn" @click="closePanel">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2"/>
          </svg>
        </button>
      </div>

      <div class="panel-body">
        <!-- Step 1: 기본 정보 -->
        <div v-show="currentStep === 1" class="form-step">
          <div class="form-group">
            <label>일정 제목 *</label>
            <input
              v-model="scheduleForm.title"
              type="text"
              class="form-input"
              placeholder="일정 제목을 입력하세요"
            />
          </div>

          <div class="form-group">
            <label>일정 내용</label>
            <textarea
              v-model="scheduleForm.content"
              class="form-textarea"
              placeholder="일정 내용을 입력하세요"
              rows="4"
            ></textarea>
          </div>

          <div class="form-group">
            <label>날짜 *</label>
            <input
              v-model="scheduleForm.date"
              type="date"
              class="form-input"
            />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>시작 시간 *</label>
              <input
                v-model="scheduleForm.startTime"
                type="text"
                class="form-input"
                placeholder="오전 09:00"
                readonly
                @click="openTimePicker('start')"
              />
            </div>
            <div class="form-group">
              <label>종료 시간 *</label>
              <input
                v-model="scheduleForm.endTime"
                type="text"
                class="form-input"
                placeholder="오후 02:00"
                readonly
                @click="openTimePicker('end')"
              />
            </div>
          </div>

          <button class="next-btn" @click="goToStep(2)">
            다음: 위치 설정
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2"/>
            </svg>
          </button>
        </div>

        <!-- Step 2: 경로 검색 -->
        <div v-show="currentStep === 2" class="form-step">
          <div class="step-indicator">Step 2 / 3</div>
          
          <div class="location-search-card">
            <button class="swap-btn" @click="swapLocations" title="출발지/도착지 교체">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M7 16V4M7 4L3 8M7 4l4 4M17 8v12m0 0l4-4m-4 4l-4-4" stroke="currentColor" stroke-width="2"/>
              </svg>
            </button>

            <div class="location-input-group">
              <div class="location-input" :class="{ active: activeLocationField === 'start' }">
                <label>출발지</label>
                <input
                  v-model="startQuery"
                  type="text"
                  placeholder="예) 구로구청"
                  @focus="setActiveLocation('start')"
                  @keyup.enter="searchLocation"
                />
              </div>
              <div class="location-input" :class="{ active: activeLocationField === 'end' }">
                <label>도착지</label>
                <input
                  v-model="endQuery"
                  type="text"
                  placeholder="예) 고려대 구로병원"
                  @focus="setActiveLocation('end')"
                  @keyup.enter="searchLocation"
                />
              </div>
            </div>

            <div v-if="showLocationResults && locationResults.length > 0" class="location-results">
              <div
                v-for="place in locationResults"
                :key="place.id"
                class="location-result-item"
                @click="selectLocation(place)"
              >
                <div class="place-name">{{ place.place_name }}</div>
                <div class="place-address">{{ place.road_address_name || place.address_name }}</div>
              </div>
            </div>
          </div>

          <div class="step-actions">
            <button class="back-btn" @click="goToStep(1)">이전</button>
            <button class="next-btn" :disabled="!canProceedToStep3" @click="goToStep(3)">
              다음: 안심존 설정
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Step 3: 안심존 설정 -->
        <div v-show="currentStep === 3" class="form-step">
          <div class="step-indicator">Step 3 / 3</div>
          
          <div class="map-container">
            <div ref="mapEl" class="map-view"></div>
          </div>

          <div class="buffer-controls">
            <h4>안심존 범위 설정</h4>
            <div class="buffer-levels">
              <button
                v-for="level in bufferLevels"
                :key="level.value"
                class="buffer-level-btn"
                :class="{ active: selectedBufferLevel === level.value }"
                @click="selectBufferLevel(level.value)"
              >
                <div class="level-name">{{ level.name }}</div>
                <div class="level-desc">{{ level.desc }}</div>
              </button>
            </div>
          </div>

          <div class="step-actions">
            <button class="back-btn" @click="goToStep(2)">이전</button>
            <button class="save-btn" @click="saveSchedule">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <polyline points="20 6 9 17 4 12" stroke="currentColor" stroke-width="2"/>
              </svg>
              저장하기
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 시간 선택 모달 -->
    <div v-if="showTimePicker" class="modal-overlay" @click="closeTimePicker">
      <div class="time-picker-modal" @click.stop>
        <div class="modal-header">
          <h3>{{ timePickerType === 'start' ? '시작 시간' : '종료 시간' }}</h3>
          <button class="close-btn" @click="closeTimePicker">✕</button>
        </div>
        <div class="time-picker-body">
          <div class="time-scrolls">
            <div class="time-scroll">
              <div
                v-for="period in ['오전', '오후']"
                :key="period"
                class="time-item"
                :class="{ active: selectedPeriod === period }"
                @click="selectedPeriod = period"
              >
                {{ period }}
              </div>
            </div>
            <div class="time-scroll">
              <div
                v-for="hour in hours"
                :key="hour"
                class="time-item"
                :class="{ active: selectedHour === hour }"
                @click="selectedHour = hour"
              >
                {{ hour }}
              </div>
            </div>
            <div class="time-scroll">
              <div
                v-for="minute in minutes"
                :key="minute"
                class="time-item"
                :class="{ active: selectedMinute === minute }"
                @click="selectedMinute = minute"
              >
                {{ minute }}
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="confirm-btn" @click="confirmTime">확인</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { lineString, buffer } from '@turf/turf'

// 캘린더 관련
const currentDate = ref(new Date())
const selectedDate = ref(new Date())
const weekdays = ['월', '화', '수', '목', '금', '토', '일']
const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']

// 환자 및 일정 데이터
const patientUserNo = ref(null)
const allSchedules = ref([])
const scheduleLocations = ref({})

// 상세보기 모달
const showDetailModal = ref(false)
const selectedSchedule = ref(null)
const detailMapEl = ref(null)
let detailMapInstance = null
const detailRouteCoordinates = ref([])

// 우측 패널 관련
const isPanelOpen = ref(false)
const panelMode = ref('add')
const currentStep = ref(1)
const editingSchedule = ref(null)

// 폼 데이터
const scheduleForm = ref({
  title: '',
  content: '',
  date: '',
  startTime: '',
  endTime: ''
})

// 시간 선택기
const showTimePicker = ref(false)
const timePickerType = ref('start')
const selectedPeriod = ref('오전')
const selectedHour = ref('09')
const selectedMinute = ref('00')
const hours = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))
const minutes = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'))

// 위치 검색 관련
const startQuery = ref('')
const endQuery = ref('')
const selectedStart = ref(null)
const selectedDest = ref(null)
const activeLocationField = ref('start')
const showLocationResults = ref(false)
const locationResults = ref([])
let placesService = null

// 지도 관련
const mapEl = ref(null)
let mapInstance = null
let currentPolyline = null
let currentBuffer = null
let startMarker = null
let endMarker = null
const selectedBufferLevel = ref(1)
const bufferLevels = [
  { value: 1, name: '1단계', desc: '30m' },
  { value: 2, name: '2단계', desc: '60m' },
  { value: 3, name: '3단계', desc: '100m' }
]

// 경로 좌표
const routeCoordinates = ref([])
const bufferCoordinates = ref([])

// Computed
const currentMonth = computed(() => monthNames[currentDate.value.getMonth()])
const currentYear = computed(() => currentDate.value.getFullYear())

const selectedDateFormatted = computed(() => {
  const date = selectedDate.value
  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`
})

const selectedDateSchedules = computed(() => {
  const dateKey = formatDateToKey(selectedDate.value)
  return allSchedules.value.filter(schedule => schedule.scheduleDate === dateKey)
})

const todaySchedules = computed(() => {
  const todayKey = formatDateToKey(new Date())
  return allSchedules.value
    .filter(schedule => schedule.scheduleDate === todayKey)
    .map(schedule => ({
      id: schedule.scheduleNo,
      time: `${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}`,
      title: schedule.scheduleTitle,
      ...schedule
    }))
})

const tomorrowSchedules = computed(() => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  const tomorrowKey = formatDateToKey(tomorrow)
  return allSchedules.value
    .filter(schedule => schedule.scheduleDate === tomorrowKey)
    .map(schedule => ({
      id: schedule.scheduleNo,
      time: `${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}`,
      title: schedule.scheduleTitle,
      ...schedule
    }))
})

const calendarDates = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const firstDayOfWeek = (firstDay.getDay() + 6) % 7
  
  const dates = []
  
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

const canProceedToStep3 = computed(() => {
  return selectedStart.value && selectedDest.value
})

// Methods
function isSameDay(date1, date2) {
  return date1.getFullYear() === date2.getFullYear() &&
         date1.getMonth() === date2.getMonth() &&
         date1.getDate() === date2.getDate()
}

function formatDateToKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getEventsForDate(date) {
  const dateKey = formatDateToKey(date)
  return allSchedules.value
    .filter(schedule => schedule.scheduleDate === dateKey)
    .map(schedule => ({ id: schedule.scheduleNo, title: schedule.scheduleTitle }))
}

function formatTime(timeString) {
  if (!timeString) return ''
  const [hour, minute] = timeString.split(':')
  const hourNum = parseInt(hour)
  
  if (hourNum === 0) return `오전 12:${minute}`
  else if (hourNum < 12) return `오전 ${String(hourNum).padStart(2, '0')}:${minute}`
  else if (hourNum === 12) return `오후 12:${minute}`
  else return `오후 ${String(hourNum - 12).padStart(2, '0')}:${minute}`
}

function formatLocation(scheduleNo) {
  const locations = scheduleLocations.value[scheduleNo]
  if (!locations || locations.length === 0) return ''
  const sortedLocations = [...locations].sort((a, b) => a.sequenceOrder - b.sequenceOrder)
  return sortedLocations.map(loc => loc.locationName).join(' → ')
}

function previousMonth() {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() - 1, 1)
}

function nextMonth() {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() + 1, 1)
}

function selectDate(date) {
  if (date.isCurrentMonth && date.date) {
    selectedDate.value = date.date
  }
}

// 상세보기 모달
async function openScheduleDetail(schedule) {
  selectedSchedule.value = schedule
  showDetailModal.value = true
  detailRouteCoordinates.value = []
  
  // 경로 정보 로드
  try {
    const routeResponse = await fetch(`/api/schedule/${schedule.scheduleNo}/route`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (routeResponse.ok) {
      const route = await routeResponse.json()
      if (route.routeCoordinates) {
        detailRouteCoordinates.value = JSON.parse(route.routeCoordinates)
        
        // 지도 초기화
        await nextTick()
        if (detailMapEl.value) {
          initDetailMap()
        }
      }
    }
  } catch (error) {
    console.error('경로 정보 로드 오류:', error)
  }
}

function closeDetailModal() {
  showDetailModal.value = false
  selectedSchedule.value = null
  detailRouteCoordinates.value = []
  if (detailMapInstance) {
    detailMapInstance = null
  }
}

function initDetailMap() {
  if (!window.kakao || !window.kakao.maps) {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=52b0ab3fbb35c5b7adc31c9772065891&libraries=services&autoload=false`
    document.head.appendChild(script)
    script.onload = () => {
      window.kakao.maps.load(() => {
        createDetailMap()
      })
    }
  } else {
    createDetailMap()
  }
}

function createDetailMap() {
  if (!detailMapEl.value || detailRouteCoordinates.value.length < 2) return
  
  const options = {
    center: new window.kakao.maps.LatLng(
      detailRouteCoordinates.value[0].latitude,
      detailRouteCoordinates.value[0].longitude
    ),
    level: 4
  }
  detailMapInstance = new window.kakao.maps.Map(detailMapEl.value, options)
  
  // 경로 그리기
  const path = detailRouteCoordinates.value.map(c => 
    new window.kakao.maps.LatLng(c.latitude, c.longitude)
  )
  
  const polyline = new window.kakao.maps.Polyline({
    path,
    strokeWeight: 5,
    strokeColor: '#8B5CF6',
    strokeOpacity: 0.9,
    strokeStyle: 'solid'
  })
  polyline.setMap(detailMapInstance)
  
  // 마커
  const startMarker = new window.kakao.maps.Marker({
    position: path[0],
    map: detailMapInstance
  })
  
  const endMarker = new window.kakao.maps.Marker({
    position: path[path.length - 1],
    map: detailMapInstance
  })
  
  // 지도 범위 조정
  const bounds = new window.kakao.maps.LatLngBounds()
  path.forEach(latLng => bounds.extend(latLng))
  detailMapInstance.setBounds(bounds)
}

function editScheduleFromDetail() {
  const schedule = selectedSchedule.value
  closeDetailModal()
  if (schedule) {
    editSchedule(schedule)
  }
}

async function confirmDeleteSchedule() {
  if (!confirm('정말로 이 일정을 삭제하시겠습니까?')) return
  
  try {
    const response = await fetch(`/api/schedule/delete/${selectedSchedule.value.scheduleNo}`, {
      method: 'POST',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('일정 삭제 실패')
    }
    
    alert('일정이 삭제되었습니다.')
    closeDetailModal()
    await loadAllData()
  } catch (error) {
    console.error('일정 삭제 오류:', error)
    alert('일정 삭제 중 오류가 발생했습니다.')
  }
}

// 일정 추가/수정
function openAddSchedule() {
  panelMode.value = 'add'
  currentStep.value = 1
  editingSchedule.value = null
  scheduleForm.value = {
    title: '',
    content: '',
    date: formatDateToKey(selectedDate.value),
    startTime: '',
    endTime: ''
  }
  startQuery.value = ''
  endQuery.value = ''
  selectedStart.value = null
  selectedDest.value = null
  routeCoordinates.value = []
  bufferCoordinates.value = []
  isPanelOpen.value = true
}

async function editSchedule(schedule) {
  panelMode.value = 'edit'
  currentStep.value = 1
  editingSchedule.value = schedule
  
  scheduleForm.value = {
    title: schedule.scheduleTitle,
    content: schedule.content || '',
    date: schedule.scheduleDate,
    startTime: formatTime(schedule.startTime),
    endTime: formatTime(schedule.endTime)
  }
  
  try {
    const locationsResponse = await fetch(`/api/schedule/${schedule.scheduleNo}/locations`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (locationsResponse.ok) {
      const locations = await locationsResponse.json()
      if (locations.length >= 2) {
        selectedStart.value = {
          place_name: locations[0].locationName,
          x: locations[0].longitude,
          y: locations[0].latitude
        }
        selectedDest.value = {
          place_name: locations[1].locationName,
          x: locations[1].longitude,
          y: locations[1].latitude
        }
        startQuery.value = locations[0].locationName
        endQuery.value = locations[1].locationName
      }
    }
    
    const routeResponse = await fetch(`/api/schedule/${schedule.scheduleNo}/route`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (routeResponse.ok) {
      const route = await routeResponse.json()
      if (route.routeCoordinates) {
        routeCoordinates.value = JSON.parse(route.routeCoordinates)
      }
      if (route.bufferCoordinates) {
        const parsed = JSON.parse(route.bufferCoordinates)
        if (parsed.level) {
          selectedBufferLevel.value = parsed.level
          bufferCoordinates.value = parsed.coordinates
        } else {
          bufferCoordinates.value = parsed
        }
      }
    }
  } catch (error) {
    console.error('일정 정보 로드 오류:', error)
  }
  
  isPanelOpen.value = true
}

function closePanel() {
  isPanelOpen.value = false
  currentStep.value = 1
}

function goToStep(step) {
  if (step === 3 && !canProceedToStep3.value) {
    alert('출발지와 도착지를 모두 선택해주세요.')
    return
  }
  
  if (step === 3) {
    requestRoute()
  }
  
  currentStep.value = step
  
  if (step === 3) {
    nextTick(() => {
      if (mapEl.value && !mapInstance) {
        initMap()
      }
    })
  }
}

// 시간 선택
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
    selectedHour.value = '09'
    selectedMinute.value = '00'
  }
  
  showTimePicker.value = true
}

function closeTimePicker() {
  showTimePicker.value = false
}

function confirmTime() {
  const timeString = `${selectedPeriod.value} ${selectedHour.value}:${selectedMinute.value}`
  if (timePickerType.value === 'start') {
    scheduleForm.value.startTime = timeString
  } else {
    scheduleForm.value.endTime = timeString
  }
  closeTimePicker()
}

// 위치 검색
function ensureKakaoPlaces() {
  if (window.kakao && window.kakao.maps && window.kakao.maps.services) {
    placesService = new window.kakao.maps.services.Places()
    return
  }
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=52b0ab3fbb35c5b7adc31c9772065891&libraries=services&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      placesService = new window.kakao.maps.services.Places()
    })
  }
}

function setActiveLocation(field) {
  activeLocationField.value = field
}

function searchLocation() {
  const query = activeLocationField.value === 'start' ? startQuery.value : endQuery.value
  if (!query || !placesService) return
  
  placesService.keywordSearch(query, (data, status) => {
    if (status !== window.kakao.maps.services.Status.OK) {
      locationResults.value = []
      showLocationResults.value = false
      return
    }
    locationResults.value = (data || []).slice(0, 5)
    showLocationResults.value = true
  })
}

function selectLocation(place) {
  if (activeLocationField.value === 'start') {
    selectedStart.value = place
    startQuery.value = place.place_name
  } else {
    selectedDest.value = place
    endQuery.value = place.place_name
  }
  showLocationResults.value = false
}

function swapLocations() {
  const tempQuery = startQuery.value
  startQuery.value = endQuery.value
  endQuery.value = tempQuery
  
  const tempSelected = selectedStart.value
  selectedStart.value = selectedDest.value
  selectedDest.value = tempSelected
}

// 경로 요청
async function requestRoute() {
  if (!selectedStart.value || !selectedDest.value) return
  
  try {
    const body = {
      startName: selectedStart.value.place_name,
      startX: Number(selectedStart.value.x),
      startY: Number(selectedStart.value.y),
      endName: selectedDest.value.place_name,
      endX: Number(selectedDest.value.x),
      endY: Number(selectedDest.value.y),
      reqCoordType: 'WGS84GEO',
      resCoordType: 'WGS84GEO',
      searchOption: '0'
    }
    
    const response = await fetch(`/api/route/pedestrian`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    
    if (!response.ok) {
      throw new Error('경로 요청 실패')
    }
    
    const data = await response.json()
    if (!data || !Array.isArray(data.coordinates) || data.coordinates.length < 2) {
      throw new Error('유효한 경로 없음')
    }
    
    routeCoordinates.value = data.coordinates
    
    if (mapInstance) {
      drawRouteOnMap()
    }
  } catch (error) {
    console.error('경로 요청 오류:', error)
    alert('경로를 찾을 수 없습니다.')
  }
}

// 지도 초기화
function initMap() {
  if (!window.kakao || !window.kakao.maps) {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=52b0ab3fbb35c5b7adc31c9772065891&libraries=services&autoload=false`
    document.head.appendChild(script)
    script.onload = () => {
      window.kakao.maps.load(() => {
        createMap()
      })
    }
  } else {
    createMap()
  }
}

function createMap() {
  if (!mapEl.value) return
  
  const options = {
    center: new window.kakao.maps.LatLng(37.495488, 126.887642),
    level: 4
  }
  mapInstance = new window.kakao.maps.Map(mapEl.value, options)
  
  if (routeCoordinates.value.length > 0) {
    drawRouteOnMap()
  }
}

function drawRouteOnMap() {
  if (!mapInstance || routeCoordinates.value.length < 2) return
  
  const path = routeCoordinates.value.map(c => 
    new window.kakao.maps.LatLng(c.latitude, c.longitude)
  )
  
  if (currentPolyline) currentPolyline.setMap(null)
  if (currentBuffer) currentBuffer.setMap(null)
  if (startMarker) startMarker.setMap(null)
  if (endMarker) endMarker.setMap(null)
  
  currentPolyline = new window.kakao.maps.Polyline({
    path,
    strokeWeight: 5,
    strokeColor: '#8B5CF6',
    strokeOpacity: 0.9,
    strokeStyle: 'solid'
  })
  currentPolyline.setMap(mapInstance)
  
  const startPoint = routeCoordinates.value[0]
  const endPoint = routeCoordinates.value[routeCoordinates.value.length - 1]
  
  startMarker = new window.kakao.maps.Marker({
    position: new window.kakao.maps.LatLng(startPoint.latitude, startPoint.longitude),
    map: mapInstance
  })
  
  endMarker = new window.kakao.maps.Marker({
    position: new window.kakao.maps.LatLng(endPoint.latitude, endPoint.longitude),
    map: mapInstance
  })
  
  createBuffer()
  
  const bounds = new window.kakao.maps.LatLngBounds()
  path.forEach(latLng => bounds.extend(latLng))
  mapInstance.setBounds(bounds)
}

function createBuffer() {
  if (!mapInstance || routeCoordinates.value.length < 2) return
  
  try {
    const turfCoords = routeCoordinates.value.map(c => [c.longitude, c.latitude])
    const line = lineString(turfCoords)
    const bufferSizes = { 1: 30, 2: 60, 3: 100 }
    const bufferSize = bufferSizes[selectedBufferLevel.value] || 30
    const buffered = buffer(line, bufferSize, { units: 'meters' })
    
    if (currentBuffer) currentBuffer.setMap(null)
    
    const coordinates = buffered.geometry.coordinates[0]
    const kakaoPath = coordinates.map(coord => 
      new window.kakao.maps.LatLng(coord[1], coord[0])
    )
    
    currentBuffer = new window.kakao.maps.Polygon({
      path: kakaoPath,
      strokeWeight: 2,
      strokeColor: '#EF4444',
      strokeOpacity: 0.8,
      fillColor: '#EF4444',
      fillOpacity: 0.3
    })
    currentBuffer.setMap(mapInstance)
    
    bufferCoordinates.value = coordinates.map(coord => ({
      latitude: coord[1],
      longitude: coord[0]
    }))
  } catch (error) {
    console.error('버퍼 생성 오류:', error)
  }
}

function selectBufferLevel(level) {
  selectedBufferLevel.value = level
  createBuffer()
}

// 일정 저장
async function saveSchedule() {
  if (!scheduleForm.value.title || !scheduleForm.value.date || 
      !scheduleForm.value.startTime || !scheduleForm.value.endTime) {
    alert('필수 항목을 모두 입력해주세요.')
    return
  }
  
  if (!selectedStart.value || !selectedDest.value) {
    alert('출발지와 도착지를 선택해주세요.')
    return
  }
  
  if (routeCoordinates.value.length < 2 || bufferCoordinates.value.length === 0) {
    alert('경로 및 안심존 설정이 필요합니다.')
    return
  }
  
  try {
    const requestData = {
      title: scheduleForm.value.title,
      content: scheduleForm.value.content,
      date: scheduleForm.value.date,
      startTime: scheduleForm.value.startTime,
      endTime: scheduleForm.value.endTime,
      locations: [
        {
          name: selectedStart.value.place_name,
          latitude: Number(selectedStart.value.y),
          longitude: Number(selectedStart.value.x),
          sequenceOrder: 1
        },
        {
          name: selectedDest.value.place_name,
          latitude: Number(selectedDest.value.y),
          longitude: Number(selectedDest.value.x),
          sequenceOrder: 2
        }
      ],
      routeCoordinates: routeCoordinates.value,
      bufferCoordinates: bufferCoordinates.value,
      bufferLevel: selectedBufferLevel.value
    }
    
    const url = panelMode.value === 'edit' 
      ? `/api/schedule/update/${editingSchedule.value.scheduleNo}`
      : `/api/schedule/create`
    
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(requestData)
    })
    
    if (!response.ok) {
      throw new Error('일정 저장 실패')
    }
    
    alert(panelMode.value === 'edit' ? '일정이 수정되었습니다.' : '일정이 추가되었습니다.')
    closePanel()
    await loadAllData()
  } catch (error) {
    console.error('일정 저장 오류:', error)
    alert('일정 저장 중 오류가 발생했습니다.')
  }
}

// 데이터 로드
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

async function fetchSchedules(userNo) {
  try {
    const response = await fetch(`/api/schedule/list/${userNo}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('일정 목록을 가져올 수 없습니다.')
    }
    
    return await response.json()
  } catch (error) {
    console.error('일정 목록 조회 오류:', error)
    return []
  }
}

async function fetchScheduleLocations(scheduleNo) {
  try {
    const response = await fetch(`/api/schedule/${scheduleNo}/locations`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('위치 정보를 가져올 수 없습니다.')
    }
    
    return await response.json()
  } catch (error) {
    console.error('위치 정보 조회 오류:', error)
    return []
  }
}

async function loadAllData() {
  const userNo = await fetchPatientInfo()
  if (!userNo) {
    console.warn('관리하는 환자가 없습니다.')
    return
  }
  
  patientUserNo.value = userNo
  const schedules = await fetchSchedules(userNo)
  allSchedules.value = schedules
  
  for (const schedule of schedules) {
    const locations = await fetchScheduleLocations(schedule.scheduleNo)
    scheduleLocations.value[schedule.scheduleNo] = locations
  }
}

onMounted(async () => {
  ensureKakaoPlaces()
  await loadAllData()
})
</script>

<style scoped>
.schedule-page {
  display: flex;
  min-height: calc(100vh - 48px - 32px);
  max-height: calc(100vh - 48px - 32px);
  background: #ffffff;
  overflow: hidden;
}

/* 중앙 영역 */
.center-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  background: #f9fafb;
  border-radius: 12px;
  overflow: hidden;
  min-width: 0;
  height: 100%;
}

.calendar-container {
  background: #ffffff;
  border-radius: 12px;
  padding: 10px 16px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  flex: 0 0 600px;
  max-height: 600px;
  height: 600px;
  width: 100%;
  margin: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.month-title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.nav-btn {
  background: none;
  border: none;
  color: #6366f1;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-btn:hover {
  background: #f5f7ff;
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0;
  margin-bottom: 4px;
  flex: 0 0 auto;
}

.weekday {
  text-align: center;
  font-size: 11px;
  color: #6b7280;
  font-weight: 600;
  padding: 6px 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  grid-auto-rows: 1fr;
  gap: 0;
  flex: 1 1 auto;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.calendar-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: background 0.2s, color 0.2s;
  padding: 10px 6px;
  min-height: 52px;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.calendar-day:nth-child(7n) {
  border-right: none;
}

.calendar-day:nth-last-child(-n + 7) {
  border-bottom: none;
}

.calendar-day:hover:not(.empty) {
  background: #f5f7ff;
}

.calendar-day.other-month {
  color: #d1d5db;
  cursor: default;
  background: #fafafa;
}

.calendar-day.empty {
  cursor: default;
  pointer-events: none;
  background: #fafafa;
}

.calendar-day.today {
  background: #f9fafc;
  box-shadow: inset 0 0 0 2px #6366f1;
}

.calendar-day.selected {
  background: #4f46e5;
  color: #ffffff;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.6);
}

.calendar-day.selected .day-number {
  color: #ffffff;
  font-weight: 700;
}

.day-number {
  font-size: 14px;
  font-weight: 600;
}

.event-indicator {
  position: absolute;
  top: 6px;
  right: 6px;
  background: #ef4444;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  border-radius: 999px;
  min-width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

.quick-schedules {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  flex: 1 1 0;
  min-height: 0;
  align-items: stretch;
}

.quick-schedule-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.quick-title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  overflow: hidden;
}

.quick-item {
  padding: 8px;
  background: #f9fafb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-item:hover {
  background: #f3f4f6;
  transform: translateX(4px);
}

.quick-time {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 3px;
}

.quick-text {
  font-size: 12px;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.no-schedule {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
  padding: 10px 0;
}

/* 우측 영역 */
.right-area {
  width: 350px;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
  overflow-y: auto;
}

.right-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.right-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.add-btn:hover {
  background: #4f46e5;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
  justify-content: flex-start;
}

.schedule-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.schedule-card:hover {
  background: #f3f4f6;
  border-color: #6366f1;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
}

.schedule-title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 10px 0;
}

.schedule-time,
.schedule-location {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.schedule-content {
  font-size: 14px;
  color: #4b5563;
  margin-bottom: 8px;
  line-height: 1.5;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  color: #9ca3af;
  gap: 16px;
}

.empty-state svg {
  color: #d1d5db;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
  color: #6b7280;
}

.empty-add-btn {
  padding: 10px 20px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.empty-add-btn:hover {
  background: #4f46e5;
}

/* 상세보기 모달 */
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

.detail-modal {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 700px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
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
  color: #6b7280;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #111827;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h3 {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 16px 0;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  color: #4b5563;
  margin-bottom: 10px;
}

.detail-content {
  margin-top: 16px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.detail-content strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.detail-content p {
  font-size: 15px;
  color: #4b5563;
  line-height: 1.6;
  margin: 0;
}

.detail-map {
  margin-bottom: 24px;
}

.detail-map h4 {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px 0;
}

.map-view {
  width: 100%;
  height: 300px;
  border-radius: 12px;
  overflow: hidden;
}

.detail-actions {
  display: flex;
  gap: 12px;
}

.edit-btn,
.delete-btn {
  flex: 1;
  padding: 12px 20px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
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

/* 우측 슬라이드 패널 */
.slide-panel {
  position: fixed;
  top: 48px;
  right: 0;
  bottom: 0;
  width: 350px;
  background: #ffffff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.1);
  transform: translateX(100%);
  transition: transform 0.3s ease;
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.slide-panel.open {
  transform: translateX(0);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.panel-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.form-step {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.step-indicator {
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #6366f1;
  margin-bottom: 8px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.form-input,
.form-textarea {
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  color: #111827;
  transition: all 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.form-textarea {
  resize: vertical;
  font-family: inherit;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.next-btn,
.back-btn,
.save-btn {
  padding: 12px 20px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.next-btn {
  background: #6366f1;
  color: #ffffff;
  width: 100%;
}

.next-btn:hover:not(:disabled) {
  background: #4f46e5;
}

.next-btn:disabled {
  background: #a5b4fc;
  cursor: not-allowed;
}

.back-btn {
  background: #ffffff;
  color: #6b7280;
  border: 1px solid #d1d5db;
  flex: 1;
}

.back-btn:hover {
  background: #f9fafb;
}

.save-btn {
  background: #10b981;
  color: #ffffff;
  flex: 2;
}

.save-btn:hover {
  background: #059669;
}

.step-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

/* Step 2: 경로 검색 */
.location-search-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  position: relative;
}

.swap-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
  z-index: 10;
  transition: all 0.2s;
}

.swap-btn:hover {
  background: #4f46e5;
  transform: translate(-50%, -50%) scale(1.1);
}

.location-input-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.location-input {
  background: #ffffff;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.2s;
}

.location-input.active {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.location-input label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 6px;
}

.location-input input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  color: #111827;
}

.location-results {
  margin-top: 12px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  max-height: 200px;
  overflow-y: auto;
}

.location-result-item {
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.2s;
}

.location-result-item:last-child {
  border-bottom: none;
}

.location-result-item:hover {
  background: #f9fafb;
}

.place-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.place-address {
  font-size: 12px;
  color: #6b7280;
}

/* Step 3: 지도 및 안심존 */
.map-container {
  width: 100%;
  height: 300px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.buffer-controls {
  margin-top: 20px;
}

.buffer-controls h4 {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px 0;
}

.buffer-levels {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.buffer-level-btn {
  padding: 14px 12px;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.buffer-level-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.buffer-level-btn.active {
  background: #eef2ff;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.level-name {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.buffer-level-btn.active .level-name {
  color: #6366f1;
}

.level-desc {
  font-size: 12px;
  color: #6b7280;
}

.buffer-level-btn.active .level-desc {
  color: #4f46e5;
}

/* 시간 선택 모달 */
.time-picker-modal {
  background: #ffffff;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.time-picker-body {
  padding: 20px;
}

.time-scrolls {
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

.time-item {
  padding: 12px;
  text-align: center;
  font-size: 16px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.time-item:hover {
  background: #f3f4f6;
}

.time-item.active {
  background: #6366f1;
  color: #ffffff;
  font-weight: 600;
}

.modal-footer {
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}

.confirm-btn {
  width: 100%;
  padding: 12px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.confirm-btn:hover {
  background: #4f46e5;
}
</style>

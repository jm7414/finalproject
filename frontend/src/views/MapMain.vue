<!-- src/views/GD_main.vue -->
<template>
  <!-- 지도 컨테이너 -->
  <div class="cg-wrap position-relative bg-white" style="margin-top: 0;">
    <!-- 지도 -->
    <div ref="mapEl" class="w-100" style="height:100%;"></div>

    <!-- 하단 흰 영역 채우기(디자인 유지) -->
    <div class="position-absolute start-0 end-0 bg-white" style="height:176px; bottom:0;"></div>

    <!-- (에러 표시) -->
    <div v-if="err" class="position-absolute top-0 start-0 w-100 text-center p-2"
      style="background:rgba(255,255,255,.92);">
      {{ err }}
    </div>

    <!-- 지도 컨트롤 버튼들 -->
    <!-- 왼쪽 상단: 안심존 버튼들 -->
    <div class="map-controls-left">
      <button class="map-btn-text">
        안심존 범위 설정
      </button>
      <button class="map-btn-text">
        안심존 해제
      </button>
    </div>

    <!-- 오른쪽: 줌 및 위치 버튼들 -->
    <div class="map-controls-right">
      <button class="map-btn-square" @click="zoomIn">
        <i class="bi bi-plus-lg"></i>
      </button>
      <button class="map-btn-square" @click="zoomOut">
        <i class="bi bi-dash-lg"></i>
      </button>
    </div>

    <!-- 현위치 버튼 (바텀시트와 연동) -->
    <div class="map-controls-location" :style="{ bottom: locationBtnBottom + 'px' }">
      <button class="map-btn-circle">
        <i class="bi bi-crosshair"></i>
      </button>
    </div>
  </div>

  <!-- ================== Bottom Sheet ================== -->
  <div class="bs-backdrop"
    :style="{ opacity: backdropOpacity, pointerEvents: sheetHeight > collapsedH + 1 ? 'auto' : 'none' }"
    @click="toCollapsed"></div>

  <div ref="sheetEl" class="bs-sheet card rounded-top-4 shadow-lg" :style="sheetStyle" @pointerdown="onPointerDown">
    <div class="d-flex justify-content-center pt-2 pb-1">
      <div class="bs-handle"></div>
    </div>

    <!-- 상단 영역: 환자 정보 카드 -->
    <div class="px-4 pt-3 pb-2" style="background: #EEF3F8;">
      <div class="card border-0 rounded-3 bg-white shadow-sm">
        <div class="card-body px-3 py-2">
          <div class="d-flex align-items-center gap-2">
            <!-- 아바타 아이콘 -->
            <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0" 
                 style="width: 48px; height: 48px; background: #E5E7EB; border: 4px solid #94FFA1;">
              <svg width="26" height="26" fill="#6B7280" viewBox="0 0 16 16">
                <path d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm2-3a2 2 0 1 1-4 0 2 2 0 0 1 4 0zm4 8c0 1-1 1-1 1H3s-1 0-1-1 1-4 6-4 6 3 6 4zm-1-.004c-.001-.246-.154-.986-.832-1.664C11.516 10.68 10.289 10 8 10c-2.29 0-3.516.68-4.168 1.332-.678.678-.83 1.418-.832 1.664h10z"/>
              </svg>
            </div>
            <!-- 텍스트 정보 -->
            <div class="flex-grow-1" style="min-width: 0;">
              <div class="fw-bold text-dark" style="font-size: 1.05rem; line-height: 1.3;">할머니 김순자</div>
              <div class="text-muted" style="font-size: 0.8125rem; line-height: 1.3;">온라인 • 2분 전</div>
            </div>
            <!-- 아이콘 -->
            <div class="d-flex align-items-center gap-2 flex-shrink-0">
              <i class="bi bi-bell-fill" style="font-size: 20px; color: #6B7280; cursor: pointer;"></i>
              <i class="bi bi-gear-fill" style="font-size: 20px; color: #6B7280; cursor: pointer;"></i>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 중간 회색 영역: 안심존 & 환자 걸음수 -->
    <div class="px-4 py-2 pb-3" style="background: #EEF3F8;" ref="topTilesRow">
      <div class="row g-2 mb-0">
        <!-- 안심존 카드 -->
        <div class="col-6">
          <div class="card border-0 rounded-3 d-flex flex-column" style="background: #DCFCE7; height: 85px;">
            <div class="card-body p-2 d-flex flex-column justify-content-between">
              <div class="d-flex align-items-center gap-1">
                <i class="bi bi-shield" style="font-size: 20px; color: #16A34A;"></i>
                <span class="fw-bold text-dark" style="font-size: 0.85rem;">안심존</span>
              </div>
              <div style="line-height: 1.4;">
                <div class="text-muted" style="font-size: 0.75rem;">현재 위치</div>
                <div class="fw-semibold" style="font-size: 0.85rem; color: #16A34A;">안전</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 환자 걸음수 카드 -->
        <div class="col-6">
          <div class="card border-0 rounded-3 d-flex flex-column bg-white" style="height: 85px;">
            <div class="card-body p-2 d-flex flex-column justify-content-between">
              <div class="d-flex align-items-center gap-1">
                <i class="bi bi-person-walking" style="font-size: 20px; color: #9CA3AF;"></i>
                <span class="fw-bold text-dark" style="font-size: 0.85rem;">환자 걸음수</span>
              </div>
              <div class="text-muted" style="font-size: 0.75rem;">1,057 걸음</div>
            </div>
          </div>
        </div>
        </div>
      </div>

    <!-- 🔽 접힘 기준 앵커 -->
      <div ref="foldAnchor" style="height:0; margin:0; padding:0;"></div>

    <!-- 하단 영역: 오늘의 일정 -->
    <div class="px-4 pt-3 pb-4" style="background: #EEF3F8;">
      <div class="d-flex align-items-center justify-content-between mb-3">
        <div class="fw-bold text-dark fs-5">오늘의 일정</div>
        <button @click="goToCalendar" class="btn btn-link btn-sm text-decoration-none p-0 text-primary fw-semibold">
          + 더보기
        </button>
      </div>

      <!-- 일정 목록 -->
      <div 
        class="schedule-list d-flex flex-column gap-3"
        :class="{ 'schedule-scrollable': sheetHeight >= openH() - 10 }">
        <!-- 일정이 없을 때 -->
        <div v-if="todaySchedules.length === 0" class="card border-0 rounded-4 bg-white">
          <div class="card-body p-4 text-center text-muted">
            오늘 예정된 일정이 없습니다.
          </div>
        </div>

        <!-- 일정 카드들 -->
        <div 
          v-for="schedule in todaySchedules"
          :key="schedule.scheduleNo"
          @click="selectSchedule(schedule.scheduleNo)"
          :class="['schedule-card', 'card', 'rounded-4', { 'schedule-active': getScheduleStatus(schedule) === 'active' }]"
          :style="{
            cursor: 'pointer',
            ...getScheduleCardStyle(schedule),
            minHeight: '140px'
          }">
          <div class="card-body p-3">
            <div class="d-flex align-items-start gap-2 position-relative">
              <!-- 왼쪽 아이콘 -->
              <div class="d-flex align-items-center flex-shrink-0" style="padding-top: 4px;">
                <div 
                  class="rounded-circle" 
                  :style="{
                    width: '12px',
                    height: '12px',
                    background: getScheduleStatus(schedule) === 'active' ? '#3B82F6' : '#9CA3AF'
                  }">
                </div>
              </div>
              
              <!-- 일정 정보 -->
              <div class="flex-grow-1">
                <div class="fw-semibold text-muted mb-2" style="font-size: 1.0625rem;">
                  {{ schedule.scheduleTitle }}
                </div>
                <div class="text-muted mb-3" style="font-size: 0.9375rem;">
                  {{ formatLocation(schedule.scheduleNo) || '위치 정보 없음' }}
                </div>
                <div class="d-flex align-items-center gap-2">
                  <svg width="14" height="14" fill="#9CA3AF" viewBox="0 0 16 16">
                    <path d="M8 3.5a.5.5 0 0 0-1 0V9a.5.5 0 0 0 .252.434l3.5 2a.5.5 0 0 0 .496-.868L8 8.71V3.5z"/>
                    <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm7-8A7 7 0 1 1 1 8a7 7 0 0 1 14 0z"/>
                  </svg>
                  <span class="text-muted" style="font-size: 0.8125rem;">
                    {{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}
                  </span>
                </div>
              </div>
              
              <!-- 상태 배지 -->
              <div class="text-end flex-shrink-0">
                <span 
                  class="badge rounded-pill px-3 py-1" 
                  :style="{
                    background: getScheduleStatus(schedule) === 'active' ? '#3B82F6' : '#9CA3AF',
                    color: 'white',
                    fontSize: '0.75rem',
                    fontWeight: '600'
                  }">
                  {{ getScheduleStatus(schedule) === 'active' ? '이동중' : '대기중' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps, computed, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 일정 관련 데이터
const patientUserNo = ref(null)
const allSchedules = ref([])
const scheduleLocations = ref({}) // scheduleNo를 키로 하는 위치 정보 맵

// 안심존 관련
let currentSafeZone = null // 현재 표시 중인 안심존 폴리곤/원형

// 일정 선택 상태 관리
const selectedScheduleIndex = ref(null)

// 일정 선택 함수
const selectSchedule = (scheduleNo) => {
  // 같은 일정을 다시 클릭하면 선택 해제
  if (selectedScheduleIndex.value === scheduleNo) {
    selectedScheduleIndex.value = null
  } else {
    selectedScheduleIndex.value = scheduleNo
  }
  // TODO: 나중에 여기서 해당 일정의 안심존을 지도에 표시하는 로직 추가
}

// 오늘의 일정 계산 (종료되지 않은 일정만)
const todaySchedules = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const todayKey = `${year}-${month}-${day}`
  
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()
  const currentTimeInMinutes = currentHour * 60 + currentMinute
  
  return allSchedules.value
    .filter(schedule => {
      // 오늘 일정만
      if (schedule.scheduleDate !== todayKey) return false
      
      // 종료 시간 체크
      const [endHour, endMinute] = schedule.endTime.split(':').map(Number)
      const endTimeInMinutes = endHour * 60 + endMinute
      
      // 종료되지 않은 일정만 (현재 시간 <= 종료 시간)
      return currentTimeInMinutes <= endTimeInMinutes
    })
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
})

// 일정의 상태 판단 (대기중/이동중)
const getScheduleStatus = (schedule) => {
  const now = new Date()
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()
  const currentTimeInMinutes = currentHour * 60 + currentMinute
  
  const [startHour, startMinute] = schedule.startTime.split(':').map(Number)
  const [endHour, endMinute] = schedule.endTime.split(':').map(Number)
  
  const startTimeInMinutes = startHour * 60 + startMinute
  const endTimeInMinutes = endHour * 60 + endMinute
  
  // 시작 시간 이전: 대기중
  if (currentTimeInMinutes < startTimeInMinutes) {
    return 'waiting'
  }
  
  // 시작~종료 시간 사이: 이동중
  if (currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes <= endTimeInMinutes) {
    return 'active'
  }
  
  // 종료 후 (이 경우는 todaySchedules에서 필터링되어 나타나지 않음)
  return 'finished'
}

// 일정 카드 스타일 가져오기
const getScheduleCardStyle = (schedule) => {
  const status = getScheduleStatus(schedule)
  
  if (status === 'active') {
    return {
      background: 'rgba(191, 219, 254, 0.5)',
      border: selectedScheduleIndex.value === schedule.scheduleNo ? '3px solid #000' : '1px solid rgba(191, 219, 254, 0.8)'
    }
  } else {
    return {
      background: 'white',
      border: selectedScheduleIndex.value === schedule.scheduleNo ? '3px solid #000' : '1px solid #E5E7EB'
    }
  }
}

// 캘린더 페이지로 이동하는 함수
const goToCalendar = () => {
  router.push('/calendar')
}

/* ===== 기존 지도/카드 props ===== */
const props = defineProps({
  kakaoKey: { type: String, default: '' },
  foldNudge: { type: Number, default: 10 },
  center: { type: Object, default: () => ({ lat: 37.4943524920695, lng: 126.88767655688868 }) },
  patient: {
    type: Object,
    default: () => ({
      name: '홍길동', taken: 1, total: 3, safe: true,
      avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=256&auto=format&fit=crop'
    })
  }
})

/* ===== Kakao Map Loader ===== */
const mapEl = ref(null)
const err = ref('')
let mapInstance = null // 지도 인스턴스 저장

function loadKakao(key) {
  return new Promise((resolve, reject) => {
    if (!key) return reject(new Error('Kakao JavaScript 키가 비어 있습니다. (.env 또는 prop 확인)'))
    if (window.kakao?.maps) return resolve(window.kakao)
    let s = document.querySelector('script[data-kakao-sdk]')
    if (!s) {
      s = document.createElement('script')
      s.setAttribute('data-kakao-sdk', 'true')
      s.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${key}&autoload=false&libraries=services`
      s.async = true
      s.onerror = () => reject(new Error('Kakao SDK 로드 실패(도메인/키 확인)'))
      document.head.appendChild(s)
    }
    s.addEventListener('load', () => {
      if (!window.kakao?.maps) return reject(new Error('kakao 객체 미탑재'))
      window.kakao.maps.load(() => resolve(window.kakao))
    }, { once: true })
  })
}

onMounted(async () => {
  // 일정 데이터 로드
  await loadScheduleData()
  
  try {
    const key = props.kakaoKey || import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'
    const kakao = await loadKakao(key)
    const center = new kakao.maps.LatLng(props.center.lat, props.center.lng)
    const map = new kakao.maps.Map(mapEl.value, { center, level: 3 })
    mapInstance = map // 지도 인스턴스 저장
    
    new kakao.maps.Marker({ position: center }).setMap(map)
    await nextTick()
    map.relayout(); map.setCenter(center)
    
    // 안심존 표시
    await updateSafeZone(map)
    
    window.addEventListener('resize', onResize)
  } catch (e) { console.error(e); err.value = e.message }
  await nextTick()
  measureCollapsed()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})

/* ===== Bottom Sheet: 드래그로만 열기 (collapsed ↔ 80% open) ===== */
const sheetEl = ref(null)
const topTilesRow = ref(null)
const foldAnchor = ref(null)

const vh = () => window.innerHeight
const openH = () => Math.round(vh() * 0.8)
const collapsedH = ref(Math.round(vh() * 0.28) || 280) // 초기값(대략), 곧 재계산됨
const sheetHeight = ref(collapsedH.value)

let startY = 0
let startH = collapsedH.value
let dragging = false

const sheetStyle = computed(() => ({
  height: sheetHeight.value + 'px',
}))
const backdropOpacity = computed(() => {
  const t = Math.max(0, Math.min(1, (sheetHeight.value - collapsedH.value) / (openH() - collapsedH.value)))
  return (0.6 * t).toFixed(2)
})

// 현위치 버튼의 bottom 위치 계산 (바텀시트 기본 높이 이하로 내려가면 따라감)
const locationBtnBottom = computed(() => {
  const btnOffset = 20 // 바텀시트 위로 20px 여백
  return Math.min(sheetHeight.value, collapsedH.value) + btnOffset
})

function onPointerDown(e) {
  dragging = true
  startY = e.clientY || (e.touches && e.touches[0].clientY)
  startH = sheetHeight.value
  window.addEventListener('pointermove', onPointerMove, { passive: false })
  window.addEventListener('pointerup', onPointerUp, { once: true })
}
function onPointerMove(e) {
  if (!dragging) return
  e.preventDefault()
  const y = e.clientY || (e.touches && e.touches[0].clientY)
  const delta = startY - y
  const minHeight = 100 // 손잡이가 보이도록 최소 높이 설정
  const next = Math.max(minHeight, Math.min(openH(), startH + delta))
  sheetHeight.value = next
}
function onPointerUp() {
  dragging = false
  // 3단계 스냅: 최소 높이(손잡이만 보임) / 기본 높이(collapsedH) / 완전히 열린 상태(openH)
  const minH = 100 // 손잡이가 보이는 최소 높이
  const midH = collapsedH.value
  const maxH = openH()
  
  const current = sheetHeight.value
  const toMin = Math.abs(current - minH)
  const toMid = Math.abs(current - midH)
  const toMax = Math.abs(current - maxH)
  
  // 가장 가까운 높이로 스냅
  if (toMin < toMid && toMin < toMax) {
    sheetHeight.value = minH
  } else if (toMid < toMax) {
    sheetHeight.value = midH
  } else {
    sheetHeight.value = maxH
  }
  
  window.removeEventListener('pointermove', onPointerMove)
}
function toCollapsed() { sheetHeight.value = collapsedH.value }

/* ✅ 접힘 높이 자동 계산 (환자 정보 + 안심존/걸음수 카드까지 보이게) */
function measureCollapsed() {
  try {
    if (!sheetEl.value || !foldAnchor.value) return
    const sheetRect = sheetEl.value.getBoundingClientRect()
    const anchorRect = foldAnchor.value.getBoundingClientRect()
    // 앵커의 bottom이 시트 상단에서 얼마나 떨어져 있는지 + 여백
    const desired = Math.ceil(anchorRect.bottom - sheetRect.top + 50 + (props.foldNudge || 0))
    const clamped = Math.max(200, Math.min(desired, openH() - 8))
    collapsedH.value = clamped
    // 드래그 중이 아니면 접힘 높이로 설정
    if (!dragging) {
      sheetHeight.value = collapsedH.value
    }
  } catch (e) {
    console.warn('measureCollapsed failed', e)
  }
}

function onResize() {
  // 뷰포트 변할 때 오픈/접힘 기준 갱신
  const wasOpen = sheetHeight.value > (collapsedH.value + openH()) / 2
  measureCollapsed()
  sheetHeight.value = wasOpen ? openH() : collapsedH.value
}

/* ===== 지도 줌 컨트롤 ===== */
function zoomIn() {
  if (!mapInstance) return
  const level = mapInstance.getLevel()
  mapInstance.setLevel(level - 1) // 레벨 감소 = 확대
}

function zoomOut() {
  if (!mapInstance) return
  const level = mapInstance.getLevel()
  mapInstance.setLevel(level + 1) // 레벨 증가 = 축소
}

/* ===== 일정 관련 함수 ===== */
// 시간을 12시간 형식으로 변환 (오전/오후 포함)
function formatTime(timeString) {
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

// 위치 정보를 화살표 형식으로 포맷팅
function formatLocation(scheduleNo) {
  const locations = scheduleLocations.value[scheduleNo]
  if (!locations || locations.length === 0) return ''
  
  // sequence_order 순서대로 정렬
  const sortedLocations = [...locations].sort((a, b) => a.sequenceOrder - b.sequenceOrder)
  
  // 위치명을 화살표로 연결
  return sortedLocations.map(loc => loc.locationName).join(' → ')
}

// 보호자가 관리하는 환자 정보 가져오기
async function fetchPatientInfo() {
  try {
    const response = await fetch('http://localhost:8080/api/user/my-patient', {
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
    const response = await fetch(`http://localhost:8080/api/schedule/list/${userNo}`, {
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
    const response = await fetch(`http://localhost:8080/api/schedule/${scheduleNo}/locations`, {
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

// 모든 일정 데이터 로드
async function loadScheduleData() {
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

// 현재 진행 중인 일정 찾기
function getCurrentSchedule() {
  const now = new Date()
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  const todayKey = `${year}-${month}-${day}`
  
  // 오늘 일정만 필터링
  const todaySchedules = allSchedules.value.filter(schedule => schedule.scheduleDate === todayKey)
  
  // 현재 시간
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()
  const currentTimeInMinutes = currentHour * 60 + currentMinute
  
  // 현재 시간에 해당하는 일정들 모두 찾기
  const currentSchedules = []
  
  for (const schedule of todaySchedules) {
    const [startHour, startMinute] = schedule.startTime.split(':').map(Number)
    const [endHour, endMinute] = schedule.endTime.split(':').map(Number)
    
    const startTimeInMinutes = startHour * 60 + startMinute
    const endTimeInMinutes = endHour * 60 + endMinute
    
    // 현재 시간이 일정 시간 범위 안에 있는지 확인
    if (currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes <= endTimeInMinutes) {
      currentSchedules.push(schedule)
    }
  }
  
  // 일정이 없으면 null 반환
  if (currentSchedules.length === 0) return null
  
  // 일정이 여러 개 겹치면 시작 시간이 가장 빠른 것 선택
  if (currentSchedules.length > 1) {
    console.warn(`⚠️ ${currentSchedules.length}개의 일정이 현재 시간에 겹칩니다. 가장 먼저 시작된 일정을 표시합니다.`)
    currentSchedules.forEach(s => {
      console.log(`  - ${s.scheduleTitle} (${s.startTime} ~ ${s.endTime})`)
    })
  }
  
  // 시작 시간 기준으로 정렬 후 첫 번째 반환
  return currentSchedules.sort((a, b) => 
    a.startTime.localeCompare(b.startTime)
  )[0]
}

// 일정의 안심존(버퍼) 가져오기
async function fetchScheduleSafeZone(scheduleNo) {
  try {
    const response = await fetch(`http://localhost:8080/api/schedule/${scheduleNo}/route`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('경로 정보를 가져올 수 없습니다.')
    }
    
    const route = await response.json()
    return route.bufferCoordinates ? JSON.parse(route.bufferCoordinates) : null
  } catch (error) {
    console.error('일정 안심존 조회 오류:', error)
    return null
  }
}

// 기본 안심존 가져오기
async function fetchBasicSafeZone(userNo) {
  try {
    const response = await fetch(`http://localhost:8080/api/schedule/basic-safe-zone/${userNo}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('기본 안심존을 가져올 수 없습니다.')
    }
    
    const result = await response.json()
    
    // 메시지만 있는 경우 (기본 안심존이 설정되지 않은 경우)
    if (result.message) {
      console.warn(result.message)
      return null
    }
    
    return result.boundaryCoordinates ? JSON.parse(result.boundaryCoordinates) : null
  } catch (error) {
    console.error('기본 안심존 조회 오류:', error)
    return null
  }
}

// 지도에 경로형 안심존(버퍼 폴리곤) 그리기
function drawScheduleSafeZone(map, bufferCoordinates) {
  if (!map || !bufferCoordinates) return
  
  try {
    // 기존 안심존 제거
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
    }
    
    // bufferCoordinates는 [{ latitude, longitude }, ...] 형식
    const kakaoPath = bufferCoordinates.map(coord => 
      new window.kakao.maps.LatLng(coord.latitude, coord.longitude)
    )
    
    // 폴리곤 생성
    currentSafeZone = new window.kakao.maps.Polygon({
      path: kakaoPath,
      strokeWeight: 2,
      strokeColor: '#EF4444',
      strokeOpacity: 0.8,
      fillColor: '#EF4444',
      fillOpacity: 0.3
    })
    
    currentSafeZone.setMap(map)
    
    // 안심존이 보이도록 지도 범위 조정
    const bounds = new window.kakao.maps.LatLngBounds()
    kakaoPath.forEach(latLng => bounds.extend(latLng))
    map.setBounds(bounds)
    
    console.log('경로형 안심존 표시 완료')
  } catch (error) {
    console.error('경로형 안심존 표시 오류:', error)
  }
}

// 지도에 기본형 안심존(원형) 그리기
function drawBasicSafeZone(map, boundaryData) {
  if (!map || !boundaryData) return
  
  try {
    // 기존 안심존 제거
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
    }
    
    // boundaryData 구조: { type: 'Circle', center: { lat, lng }, radius, ... }
    if (boundaryData.type === 'Circle') {
      const center = new window.kakao.maps.LatLng(boundaryData.center.lat, boundaryData.center.lng)
      const radius = boundaryData.radius
      
      // 원형 폴리곤 생성 (Turf.js 없이 직접 계산)
      const circlePoints = []
      const steps = 64
      const earthRadius = 6371000 // 지구 반경 (미터)
      
      for (let i = 0; i < steps; i++) {
        const angle = (Math.PI * 2 * i) / steps
        const dx = radius * Math.cos(angle)
        const dy = radius * Math.sin(angle)
        
        const lat = boundaryData.center.lat + (dy / earthRadius) * (180 / Math.PI)
        const lng = boundaryData.center.lng + (dx / earthRadius) * (180 / Math.PI) / Math.cos(boundaryData.center.lat * Math.PI / 180)
        
        circlePoints.push(new window.kakao.maps.LatLng(lat, lng))
      }
      
      // 폴리곤 생성
      currentSafeZone = new window.kakao.maps.Polygon({
        path: circlePoints,
        strokeWeight: 3,
        strokeColor: '#6366f1',
        strokeOpacity: 0.8,
        fillColor: '#6366f1',
        fillOpacity: 0.2
      })
      
      currentSafeZone.setMap(map)
      
      // 지도 레벨 조정
      const bounds = new window.kakao.maps.LatLngBounds()
      circlePoints.forEach(point => bounds.extend(point))
      map.setBounds(bounds)
      
      console.log('기본형 안심존 표시 완료')
    }
  } catch (error) {
    console.error('기본형 안심존 표시 오류:', error)
  }
}

// 안심존 업데이트 (현재 일정에 따라)
async function updateSafeZone(map) {
  if (!map || !patientUserNo.value) return
  
  try {
    // 1. 현재 진행 중인 일정 찾기
    const currentSchedule = getCurrentSchedule()
    
    if (currentSchedule) {
      // 2. 진행 중인 일정이 있으면 해당 일정의 안심존 표시
      console.log('현재 진행 중인 일정:', currentSchedule.scheduleTitle)
      const bufferCoordinates = await fetchScheduleSafeZone(currentSchedule.scheduleNo)
      
      if (bufferCoordinates && bufferCoordinates.length > 0) {
        drawScheduleSafeZone(map, bufferCoordinates)
        return
      }
    }
    
    // 3. 진행 중인 일정이 없거나 안심존이 없으면 기본 안심존 표시
    console.log('기본 안심존 표시')
    const basicSafeZone = await fetchBasicSafeZone(patientUserNo.value)
    
    if (basicSafeZone) {
      drawBasicSafeZone(map, basicSafeZone)
    } else {
      console.warn('표시할 안심존이 없습니다.')
    }
  } catch (error) {
    console.error('안심존 업데이트 오류:', error)
  }
}
</script>

<style scoped>
/* ===== 전체 프레임: 화면 가득 채우기 ===== */
.cg-wrap {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* ===== Bottom Sheet ===== */
.bs-backdrop {
  position: fixed;
  inset: 0;
  background: #000;
  transition: opacity .15s ease;
  z-index: 998;
}

.bs-sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  max-width: 100%;
  background: #EEF3F8;
  border: 0;
  z-index: 999;
  touch-action: none;
}

.bs-handle {
  width: 36px;
  height: 4px;
  border-radius: 100px;
  background: #000000;
  opacity: 1;
}

.card {
  box-shadow: 0 1px 2px rgba(0, 0, 0, .05);
}

/* 일정 카드 스타일 */
.schedule-card {
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .08);
}

.schedule-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, .12);
}

/* 일정 목록 스크롤 */
.schedule-list {
  max-height: none;
  overflow: visible;
}

.schedule-list.schedule-scrollable {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

/* 스크롤바 스타일링 */
.schedule-list.schedule-scrollable::-webkit-scrollbar {
  width: 6px;
}

.schedule-list.schedule-scrollable::-webkit-scrollbar-track {
  background: #E5E7EB;
  border-radius: 3px;
}

.schedule-list.schedule-scrollable::-webkit-scrollbar-thumb {
  background: #9CA3AF;
  border-radius: 3px;
}

.schedule-list.schedule-scrollable::-webkit-scrollbar-thumb:hover {
  background: #6B7280;
}

/* ===== 지도 컨트롤 버튼들 ===== */
.map-controls-left {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.map-controls-right {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.map-controls-location {
  position: fixed;
  right: 20px;
  z-index: 100;
  transition: bottom 0.2s ease;
}

/* 텍스트 버튼 (안심존 관련) */
.map-btn-text {
  padding: 8px 12px;
  border-radius: 6px;
  background: white;
  border: 1px solid #D1D5DB;
  font-size: 12px;
  font-weight: 600;
  color: #1F2937;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
  white-space: nowrap;
}

.map-btn-text:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  transform: translateY(-1px);
}

.map-btn-text:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

/* 네모난 아이콘 버튼 (+, -) */
.map-btn-square {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
}

.map-btn-square:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  transform: translateY(-1px);
}

.map-btn-square:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.map-btn-square i {
  font-size: 20px;
  color: #1F2937;
}

/* 원형 버튼 (현위치) */
.map-btn-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
}

.map-btn-circle:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  transform: translateY(-1px);
}

.map-btn-circle:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.map-btn-circle i {
  font-size: 20px;
  color: #1F2937;
}
</style>


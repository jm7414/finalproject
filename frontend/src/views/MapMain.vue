<!-- src/views/GD_main.vue -->
<template>
  <!-- 헤더 높이만큼 위로 당김(지도 딱 붙게) -->
  <div class="cg-wrap position-relative bg-white" :style="wrapStyle">
    <!-- 지도 -->
    <div ref="mapEl" class="w-100" style="height:900px;"></div>

    <!-- 하단 흰 영역 채우기(디자인 유지) -->
    <div class="position-absolute start-0 end-0 bg-white" style="height:176px; bottom:0;"></div>

    <!-- (에러 표시) -->
    <div v-if="err" class="position-absolute top-0 start-0 w-100 text-center p-2"
      style="background:rgba(255,255,255,.92);">
      {{ err }}
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

    <!-- 상단 흰색 영역: 환자 정보 헤더 -->
    <div class="bg-white px-4 pt-3 pb-3">
      <div class="d-flex align-items-center gap-3">
        <!-- 아바타 아이콘 -->
        <div class="d-flex align-items-center justify-content-center rounded-circle" 
             style="width: 56px; height: 56px; background: #4DB6AC;">
          <svg width="32" height="32" fill="white" viewBox="0 0 16 16">
            <path d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm2-3a2 2 0 1 1-4 0 2 2 0 0 1 4 0zm4 8c0 1-1 1-1 1H3s-1 0-1-1 1-4 6-4 6 3 6 4zm-1-.004c-.001-.246-.154-.986-.832-1.664C11.516 10.68 10.289 10 8 10c-2.29 0-3.516.68-4.168 1.332-.678.678-.83 1.418-.832 1.664h10z"/>
          </svg>
        </div>
        <!-- 텍스트 정보 -->
        <div class="flex-grow-1">
          <div class="fw-bold text-dark fs-5">할머니 김순자</div>
          <div class="text-muted" style="font-size: 0.875rem;">온라인 • 2분 전</div>
          </div>
        </div>
      </div>

    <!-- 중간 회색 영역: 안심존 & 환자 걸음수 -->
    <div class="px-4 py-3" style="background: #F5F5F5;" ref="topTilesRow">
      <div class="row g-3 mb-0">
        <!-- 안심존 카드 -->
        <div class="col-6">
          <div class="card border-0 rounded-3 d-flex flex-column" style="background: #DCFCE7; height: 110px;">
            <div class="card-body p-3 d-flex flex-column justify-content-between">
              <div class="d-flex align-items-center gap-2">
                <i class="bi bi-shield" style="font-size: 26px; color: #16A34A;"></i>
                <span class="fw-bold text-dark" style="font-size: 0.95rem;">안심존</span>
              </div>
              <div class="text-muted fw-semibold" style="font-size: 0.95rem; line-height: 1.3;">
                현재 위치<br>안전
              </div>
            </div>
          </div>
        </div>
        
        <!-- 환자 걸음수 카드 -->
        <div class="col-6">
          <div class="card border-0 rounded-3 d-flex flex-column bg-white" style="height: 110px;">
            <div class="card-body p-3 d-flex flex-column justify-content-between">
              <div class="d-flex align-items-center gap-2">
                <i class="bi bi-person-walking" style="font-size: 26px; color: #3B82F6;"></i>
                <span class="fw-bold text-dark" style="font-size: 0.95rem;">환자 걸음수</span>
              </div>
              <div class="text-dark fw-bold" style="font-size: 1.15rem;">1,057 걸음</div>
            </div>
          </div>
        </div>
        </div>
      </div>

    <!-- 🔽 접힘 기준 앵커 -->
      <div ref="foldAnchor" style="height:0; margin:0; padding:0;"></div>

    <!-- 하단 흰색 영역: 오늘의 일정 -->
    <div class="bg-white px-4 pt-3 pb-4">
      <div class="d-flex align-items-center justify-content-between mb-3">
        <div class="fw-bold text-dark fs-5">오늘의 일정</div>
        <button @click="goToCalendar" class="btn btn-link btn-sm text-decoration-none p-0 text-primary fw-semibold">
          + 더보기
        </button>
      </div>

      <!-- 일정 목록 -->
      <div class="d-flex flex-column gap-3">
        <!-- 약국 방문 (진행중) -->
        <div 
          @click="selectSchedule(1)"
          :class="['schedule-card', 'card', 'rounded-4', 'schedule-active', { 'schedule-selected': selectedScheduleIndex === 1 }]"
          :style="{
            cursor: 'pointer',
            background: 'rgba(191, 219, 254, 0.5)',
            border: selectedScheduleIndex === 1 ? '3px solid #000' : '1px solid rgba(191, 219, 254, 0.8)',
            minHeight: '140px'
          }">
          <div class="card-body p-3">
            <div class="d-flex align-items-start gap-2 position-relative">
              <!-- 왼쪽 아이콘 -->
              <div class="d-flex align-items-center flex-shrink-0" style="padding-top: 4px;">
                <div class="rounded-circle" style="width: 12px; height: 12px; background: #3B82F6;"></div>
              </div>
              <!-- 일정 정보 -->
              <div class="flex-grow-1">
                <div class="fw-semibold text-muted mb-2" style="font-size: 1.0625rem;">약국 방문</div>
                <div class="text-muted mb-3" style="font-size: 0.9375rem;">온누리약국</div>
                <div class="d-flex align-items-center gap-2 mb-1">
                  <svg width="14" height="14" fill="#9CA3AF" viewBox="0 0 16 16">
                    <path d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/>
                  </svg>
                  <span class="text-muted" style="font-size: 0.8125rem;">시작 시간: 15:00</span>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <svg width="14" height="14" fill="#9CA3AF" viewBox="0 0 16 16">
                    <path d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/>
                  </svg>
                  <span class="text-muted" style="font-size: 0.8125rem;">종료 시간: 16:00</span>
                </div>
              </div>
              <!-- 이동중 배지 -->
              <div class="text-end flex-shrink-0">
                <span class="badge rounded-pill px-3 py-1" style="background: #3B82F6; color: white; font-size: 0.75rem; font-weight: 600;">이동중</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 병원 방문 (선택된 상태) -->
        <div 
          @click="selectSchedule(0)"
          :class="['schedule-card', 'card', 'rounded-4', { 'schedule-selected': selectedScheduleIndex === 0 }]"
          :style="{
            cursor: 'pointer',
            border: selectedScheduleIndex === 0 ? '3px solid #000' : '1px solid #E5E7EB',
            minHeight: '140px'
          }">
          <div class="card-body p-3">
            <div class="d-flex align-items-start gap-2">
              <!-- 아이콘 -->
              <div class="d-flex align-items-center flex-shrink-0" style="padding-top: 4px;">
                <div class="rounded-circle" style="width: 12px; height: 12px; background: #9CA3AF;"></div>
              </div>
              <!-- 일정 정보 -->
              <div class="flex-grow-1">
                <div class="fw-semibold text-muted mb-2" style="font-size: 1.0625rem;">병원 방문</div>
                <div class="text-muted mb-3" style="font-size: 0.9375rem;">구로구청 -> 구로 고려대병원</div>
                <div class="d-flex align-items-center gap-2 mb-1">
                  <svg width="14" height="14" fill="#9CA3AF" viewBox="0 0 16 16">
                    <path d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/>
                  </svg>
                  <span class="text-muted" style="font-size: 0.8125rem;">시작 시간: 14:00</span>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <svg width="14" height="14" fill="#9CA3AF" viewBox="0 0 16 16">
                    <path d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/>
                  </svg>
                  <span class="text-muted" style="font-size: 0.8125rem;">종료 시간: 15:30</span>
                </div>
              </div>
              <!-- 대기중 배지 -->
              <div class="text-end flex-shrink-0">
                <span class="badge rounded-pill px-3 py-1" style="background: #9CA3AF; color: white; font-size: 0.75rem; font-weight: 600;">대기중</span>
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
const selectSchedule = (index) => {
  // 현재 진행중인 일정(1번 인덱스)이 아닌 다른 일정을 선택할 때만 선택 상태 변경
  if (selectedScheduleIndex.value === index) {
    selectedScheduleIndex.value = null // 같은 것을 다시 클릭하면 선택 해제
  } else {
    selectedScheduleIndex.value = index
  }
  // TODO: 나중에 여기서 해당 일정의 안심존을 지도에 표시하는 로직 추가
}

// 캘린더 페이지로 이동하는 함수
const goToCalendar = () => {
  router.push('/calendar')
}

/* ===== 기존 지도/카드 props ===== */
const props = defineProps({
  kakaoKey: { type: String, default: '' },
  foldNudge: { type: Number, default: -20 },
  center: { type: Object, default: () => ({ lat: 37.4943524920695, lng: 126.88767655688868 }) },
  patient: {
    type: Object,
    default: () => ({
      name: '홍길동', taken: 1, total: 3, safe: true,
      avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=256&auto=format&fit=crop'
    })
  },
  pullUp: { type: Number, default: 70 },
  pullUpDesktop: { type: Number, default: 80 },
  /* 💡 지도 위 카드 위치(조절 가능) */
  infoTop: { type: Number, default: 124 },
  infoTopDesktop: { type: Number, default: 132 }
})

/* ===== 지도 쪽: 헤더 딱 붙이기 + 카드 위치 변수 주입 ===== */
const wrapStyle = computed(() => ({
  marginTop: `-${props.pullUp}px`,
  '--pullUpDesktop': `-${props.pullUpDesktop}px`,
  '--infoTop': `${props.infoTop}px`,
  '--infoTopDesktop': `${props.infoTopDesktop}px`,
}))

/* ===== 일정 관련 함수 (나중에 백엔드 연결 시 사용) ===== */
// TODO: 백엔드 연결 시 실제 일정 데이터를 가져오는 함수들 추가 예정
// TODO: 안심존 업데이트 함수들도 여기에 추가 예정

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
  try {
    const key = props.kakaoKey || import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'
    const kakao = await loadKakao(key)
    const center = new kakao.maps.LatLng(props.center.lat, props.center.lng)
    const map = new kakao.maps.Map(mapEl.value, { center, level: 3 })
    mapInstance = map // 지도 인스턴스 저장
    
    new kakao.maps.Marker({ position: center }).setMap(map)
    await nextTick()
    map.relayout(); map.setCenter(center)
    
    window.addEventListener('resize', onResize)
  } catch (e) { console.error(e); err.value = e.message }
  await nextTick()
  measureCollapsed()  // ✅ 처음 로드 시 접힘 높이 자동 계산
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
  const next = Math.max(collapsedH.value, Math.min(openH(), startH + delta))
  sheetHeight.value = next
}
function onPointerUp() {
  dragging = false
  const mid = (collapsedH.value + openH()) / 2
  sheetHeight.value = sheetHeight.value > mid ? openH() : collapsedH.value
  window.removeEventListener('pointermove', onPointerMove)
}
function toCollapsed() { sheetHeight.value = collapsedH.value }

/* ✅ 접힘 높이 자동 계산 (일지매 + 상단 두 타일까지 보이게) */
function measureCollapsed() {
  try {
    if (!sheetEl.value || !foldAnchor.value) return
    const sheetRect = sheetEl.value.getBoundingClientRect()
    const anchorRect = foldAnchor.value.getBoundingClientRect()
    // 앵커의 bottom이 시트 상단에서 얼마나 떨어져 있는지 + 약간의 여백
    const desired = Math.ceil(anchorRect.bottom - sheetRect.top + 12 + (props.foldNudge || 0))
    const clamped = Math.max(240, Math.min(desired, openH() - 8))
    collapsedH.value = clamped
    // 열려있지 않다면, 현재 높이를 접힘값으로 맞춰준다
    const mid = (startH + openH()) / 2
    if (!dragging && sheetHeight.value <= mid) sheetHeight.value = collapsedH.value
  } catch (e) {
    // 실패시 기존 비율 유지
    console.warn('measureCollapsed failed', e)
  }
}

function onResize() {
  // 뷰포트 변할 때 오픈/접힘 기준 갱신
  const wasOpen = sheetHeight.value > (collapsedH.value + openH()) / 2
  measureCollapsed()
  sheetHeight.value = wasOpen ? openH() : collapsedH.value
}
</script>

<style scoped>
/* ===== 전체 프레임: 화면 가득 채우기 ===== */
.cg-wrap {
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
}

@media (min-width:768px) {
  .cg-wrap {
    margin-top: var(--pullUpDesktop, -80px) !important;
  }
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
  background: #F8F9FA;
  border: 0;
  z-index: 999;
  touch-action: none;
}

.bs-handle {
  width: 51px;
  height: 4px;
  border-radius: 100px;
  background: #79747E;
  opacity: .9;
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
</style>


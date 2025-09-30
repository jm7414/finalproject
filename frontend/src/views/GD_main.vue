<!-- src/views/GD_main.vue -->
<template>
  <!-- 헤더 높이만큼 위로 당김(지도 딱 붙게) -->
  <div class="cg-wrap position-relative bg-white" :style="wrapStyle">
    <!-- 지도 -->
    <div ref="mapEl" class="w-100" style="height:900px;"></div>

    <!-- 지도 위 정보 카드 -->
    <div class="card cg-glass shadow-sm position-absolute cg-info">
      <div class="card-body py-2 px-3 d-flex align-items-center gap-3">
        <div class="symbol symbol-60px rounded-circle overflow-hidden flex-shrink-0">
          <img :src="patient.avatarUrl" alt="avatar" class="w-100 h-100" style="object-fit:cover;" />
        </div>
        <div class="flex-grow-1 lh-sm">
          <div class="fw-semibold cg-name">{{ patient.name }}님</div>
          <div class="small text-muted">13:00 | 마트 들려서 양..</div>
          <div class="small" :class="patient.safe ? 'text-success' : 'text-danger'">
            현재 위치: {{ patient.safe ? '안심존' : '주의구역' }}
          </div>
        </div>
        <button type="button" class="cg-bell btn p-0 d-inline-flex align-items-center justify-content-center"
          @click="$emit('click-notify')" aria-label="알림">
          <svg width="22" height="22" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M8 16a2 2 0 0 0 1.985-1.75H6.015A2 2 0 0 0 8 16z" />
            <path
              d="M8 1.918a3 3 0 0 0-3 3v1.098c0 .502-.145.993-.417 1.41L3.1 8.908c-.533.8-.106 1.892.83 2.086A31 31 0 0 0 8 11.5c1.394 0 2.788-.11 4.07-.506.937-.194 1.364-1.286.83-2.086l-1.483-2.082a2.5 2.5 0 0 1-.417-1.41V4.918a3 3 0 0 0-3-3z" />
          </svg>
        </button>
      </div>
    </div>

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

    <div class="card-body pt-2 pb-3 px-3">
      <div class="d-flex align-items-center justify-content-between mb-3">
        <div class="d-flex align-items-center gap-2">
          <img :src="patient.avatarUrl" class="rounded-circle" width="40" height="40" alt="avatar" />
          <div class="fw-semibold text-dark">안녕하세요. {{ patient.name }} 보호자님</div>
        </div>
        <span class="badge text-dark border bg-danger-subtle px-3 py-2 rounded-2">환자 상태 변환</span>
      </div>

      <!-- 위험 알림 카드 -->
      <div class="card border-0 rounded-3 mb-3 alert-soft">
        <div class="card-body d-flex align-items-center gap-3 py-3">
          <div class="d-flex align-items-center justify-content-center rounded-3 bg-white bg-opacity-10"
            style="width:70px;height:70px; overflow:hidden;">
            <img :src="patient.avatarUrl" alt="avatar" style="width:100%;height:100%;object-fit:cover;" />
          </div>
          <div class="text-light">
            <div class="fw-semibold" style="color:#000000;">일 지 매 (71)</div>
            <div style="color:#000000;">실종 위치 : 청주 동남지구</div>
            <div style="color:#000000;">실종 시간 : 2025/09/30</div>
          </div>
        </div>
      </div>

      <!-- 상단 두 타일 -->
      <div class="row g-2 mb-3" ref="topTilesRow">
        <div class="col-6">
          <div class="card border-0 rounded-4" style="background:#DCFCE7; cursor: pointer;" @click="goToGeoFencing">
            <div class="card-body py-3 d-flex align-items-center gap-2">
              <img :src="cir" alt="안심 아이콘" aria-hidden="true" class="icon-44" />
              <div class="fw-semibold">안심 zone</div>
            </div>
          </div>
        </div>
        <div class="col-6">
          <div class="card border-0 rounded-4" style="background:#F0F7FF; cursor: pointer;" @click="goToPredictLocation">
            <div class="card-body py-3 d-flex align-items-center gap-2">
              <img :src="cirr" alt="예상 위치 아이콘" aria-hidden="true" class="icon-44" />
              <div class="fw-semibold">예상 위치</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 🔽 접힘 기준 앵커: 이 아래는 접힘 상태에서 안 보여도 됨 -->
      <div ref="foldAnchor" style="height:0; margin:0; padding:0;"></div>

      <!-- 중간 타일 묶음 -->
      <div class="row g-2 mb-3">
        <!-- 좌측 큰: 커뮤니티 -->
        <div class="col-6">
          <div class="card border-0 rounded-3 text-light d-flex justify-content-end"
            style="background:rgba(74,98,221,0.85);height:212px;position:relative;cursor:pointer;"
            @click="goToCommunity">
            <img :src="cur" alt="커뮤니티 아이콘" aria-hidden="true"
              class="position-absolute top-0 end-0 p-0 icon-88 opacity-75" />
            <div class="p-3 fw-semibold fs-4" style="color:#FFECCC;">커뮤니티</div>
          </div>
        </div>

        <!-- 우측 2단: 종합 지원 / 건강 리포트 -->
        <div class="col-6 d-flex flex-column gap-2">
          <!-- 종합 지원 -->
          <div class="card border-0 rounded-3"
            style="background:rgba(170,194,254,0.91);height:100px;position:relative;cursor:pointer;"
            @click="goToTotalSupport">
            <img :src="supportAgent" alt="지원 상담 아이콘" aria-hidden="true"
              class="position-absolute top-0 end-0 p-0 icon-64 opacity-75" />
            <div class="p-3 fw-semibold text-dark fs-5">종합 지원</div>
          </div>

          <!-- 건강 리포트 -->
          <div class="card border-0 rounded-3" style="background:#FFECCC;height:100px;position:relative;cursor:pointer;"
            @click="goToReport">
            <img :src="bookOpen" alt="건강 리포트 아이콘" aria-hidden="true"
              class="position-absolute end-0 bottom-0 p-2 icon-80 opacity-25" />
            <div class="p-3 fw-semibold fs-5" style="color:rgba(74,98,221,0.85);">건강 리포트</div>
          </div>
        </div>
      </div>

      <!-- 오늘의 일정 -->
      <div class="mb-2 fw-bold fs-6 text-dark">오늘의 일정</div>
      <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body">
          <div class="text-muted small mb-1">오늘 일정</div>
          <div class="fw-semibold">11:30 AM - 12:30 PM</div>
          <div class="text-secondary">치매 센터 정기 검진일</div>
          <div class="text-muted small mt-1">집 → 구로 고대 병원</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps, computed, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import supportAgent from '@/assets/images/support_agent.svg'
import cir from '@/assets/images/cir.svg'
import cirr from '@/assets/images/cirr.svg'
import cur from '@/assets/images/cur.svg'
import bookOpen from '@/assets/images/Book open.svg'

const router = useRouter()

// 예상 위치 페이지로 이동하는 함수
const goToPredictLocation = () => {
  router.push('/predict-location')
}

// 안심존 페이지로 이동하는 함수
const goToGeoFencing = () => {
  router.push('/geo-fencing')
}

// 종합 지원 페이지로 이동하는 함수
const goToTotalSupport = () => {
  router.push('/total-support')
}

// 건강 리포트 페이지로 이동하는 함수
const goToReport = () => {
  router.push('/report')
}

// 커뮤니티 페이지로 이동하는 함수
const goToCommunity = () => {
  router.push('/CommunityView')
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

/* ===== Kakao Map Loader ===== */
const mapEl = ref(null)
const err = ref('')

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

/* 지도 카드 (위치: 변수화) */
.cg-info {
  left: 50%;
  transform: translateX(-50%);
  top: var(--infoTop, 124px);
  width: calc(100% - 32px);
  max-width: 500px;
  height: 82px;
  border-radius: 8px;
  z-index: 10;
}

@media (min-width:768px) {
  .cg-info {
    top: var(--infoTopDesktop, 132px);
  }
}

.cg-glass {
  background: rgba(251, 252, 255, 0.7);
  backdrop-filter: saturate(140%) blur(6px);
}

.cg-name {
  color: #3F414E;
}

.cg-bell {
  width: 56px;
  height: 52px;
  border-radius: 999px;
  background: rgba(74, 98, 221, 0.85);
  border: 4px solid #FFECCC;
  color: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, .15);
}

.symbol-60px {
  width: 60px;
  height: 60px;
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
  background: #F0EFEF;
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

/* 아이콘 크기 유틸 */
.icon-44 {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

/* 상단 타일 */
.icon-64 {
  width: 64px;
  height: 64px;
  object-fit: contain;
}

/* 종합 지원 */
.icon-80 {
  width: 80px;
  height: 80px;
  object-fit: contain;
}

/* 건강 리포트 */
.icon-88 {
  width: 88px;
  height: 88px;
  object-fit: contain;
}

/* 커뮤니티 */

.card {
  box-shadow: 0 1px 2px rgba(0, 0, 0, .05);
}

.alert-soft{
  /* 같은 레드(#F86969) 계열, 투명도만 낮춰서 덜 눈에 띄게 */
  background: linear-gradient(180deg,
              rgba(248,105,105, .18),
              rgba(248,105,105, .12));
  border: 1px solid rgba(248,105,105, .28);
  box-shadow: 0 4px 14px rgba(248,105,105,.08); /* 살짝만 */
  border-radius: 12px;
}
.alert-soft:hover{
  background: linear-gradient(180deg,
              rgba(248,105,105, .22),
              rgba(248,105,105, .16));
}

</style>

<template>
  <div class="container-sm py-3 neighbor-page-container" style="max-width:414px; position:relative;">

    <!-- 상단 문구 -->
    <div class="my-3">
      <div class="d-flex align-items-center justify-content-between">
        <div class="text-start flex-grow-1">
          <div class="safety-status-text">
            현재 만남의 광장에 3명이 있습니다
          </div>
        </div>
        <!-- 임시 로그아웃 버튼 -->
        <button @click="handleLogout" class="btn btn-sm btn-outline-secondary" 
          style="font-size: 0.75rem; padding: 4px 8px;">
          <i class="bi bi-box-arrow-right"></i>
        </button>
      </div>
    </div>

    <!-- Kakao 지도 프리뷰 -->
    <div class="card border-0 shadow-sm position-relative overflow-hidden mb-4 rounded-4">
      <div ref="mapEl" class="w-100" style="height:280px;"></div>
      <!-- 항상 노출 -->
      <button class="btn btn-light rounded-pill position-absolute start-50 translate-middle-x map-detail-btn"
        style="bottom:12px; z-index:10; pointer-events:auto">
        지도 자세히 보기
      </button>
    </div>

    <!-- 걸음 수 -->
    <div class="card border-0 shadow-sm rounded-4 mb-2">
      <div class="card-body text-muted d-flex">
        <div class="map-detail-btn" style="width:50px; height:30px;">아이콘</div>
        <div style="width:180px; height:30px;">오늘의 활동량</div>
        <span>0000걸음</span>
      </div>
    </div>

    <!-- 가장 빠른 일정 2개 -->
    <h6 class="fw-bold mb-2">모임 일정</h6>
    
    <!-- 일정 2개 -->
    <div v-if="upcomingSchedules.length > 0">
      <div v-for="(schedule, index) in upcomingSchedules"  
        :key="index" 
        class="card border-2 rounded-3 p-3 mb-2" 
        style="border-color:#e9ecef">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <span class="small text-secondary mb-1">{{ formatDate(schedule.scheduleDate) }}</span>
          <div class="d-flex align-items-center gap-2">
            <span class="fw-semibold">{{ schedule.scheduleTitle }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 일정 없음 -->
    <div v-else class="card border-0 shadow-sm rounded-4 mb-2">
      <div class="card-body text-center text-muted">
        <p>일정이 없습니다.</p>
        <p>일정을 입력해보세요!</p>
      </div>
    </div>

    <button class="btn btn-outline-dark w-100 rounded-pill mb-3" @click="router.push('/calendar')">
      일정 자세히 보기
    </button>

    <!-- 기능 타일 -->
    <div class="row g-3 align-items-stretch mb-4">
      <!-- 1) 기본 안심존 설정 -->
      <div class="col-6">
        <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
          style="height:220px;
                       background-image:
                         linear-gradient(0deg, rgba(255,255,255,.38) 0%, rgba(255,255,255,.18) 45%, rgba(255,255,255,0) 75%),
                         linear-gradient(135deg,#6f82ff 0%,#576cff 55%,#475cff 100%);">
          <div class="position-absolute top-0 start-0 end-0 d-flex align-items-center justify-content-center"
            style="bottom:44px">
            <img :src="zone1" alt="" draggable="false"
              style="height:100%;max-height:100%;width:auto;object-fit:contain;transform:scale(1.14) translateY(2%);">
          </div>
          <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold text-white"
            style="height:44px">기본 안심존 설정</div>
        </button>
      </div>

      <!-- 2) 예상 위치 -->
      <div class="col-6">
        <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
          style="height:196px;
                       background-image:
                         linear-gradient(0deg, rgba(255,255,255,.35) 0%, rgba(255,255,255,.16) 45%, rgba(255,255,255,0) 75%),
                         linear-gradient(135deg,#ff7b64 0%,#ff5a42 60%,#ff3f2e 100%);">
          <div class="position-absolute top-0 start-0 end-0 d-flex align-items-center justify-content-center"
            style="bottom:40px">
            <img :src="locationIcon" alt="" draggable="false"
              style="height:65%;max-height:100%;width:auto;object-fit:contain;transform:scale(1.12);">
          </div>
          <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold text-white"
            style="height:40px">예상 위치</div>
        </button>
      </div>

      <!-- 3) AI 보고서 -->
      <div class="col-6">
        <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
          style="height:196px;
                       background-image:
                         linear-gradient(0deg, rgba(255,255,255,.32) 0%, rgba(255,255,255,.14) 45%, rgba(255,255,255,0) 75%),
                         linear-gradient(135deg,#ffd6b9 0%,#ffb487 62%,#ff965f 100%);">
          <div class="position-absolute top-0 start-0 end-0 d-flex align-items-center justify-content-center"
            style="bottom:40px">
            <img :src="report2" alt="" draggable="false" class="position-absolute top-50 start-50 translate-middle"
              style="height:132%;max-height:none;width:auto;object-fit:contain;transform:translate(-50%,-56%);">
          </div>
          <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold"
            style="height:40px;color:#232323">AI 보고서</div>
        </button>
      </div>

      <!-- 4) 환자 연결 관리 -->
      <div class="col-6" style="margin-top:-8px">
        <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
          style="height:220px;
                       background-image:
                         linear-gradient(0deg, rgba(255,255,255,.34) 0%, rgba(255,255,255,.16) 45%, rgba(255,255,255,0) 75%),
                         linear-gradient(135deg,#ffe08f 0%,#ffc050 60%,#ffae2a 100%);">
          <div class="position-absolute top-0 start-0 end-0 d-flex align-items-center justify-content-center"
            style="bottom:44px">
            <img :src="connectIcon" alt="" draggable="false"
              style="height:100%;max-height:100%;width:auto;object-fit:contain;transform:scale(1.14) translateY(6%);">
          </div>
          <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold"
            style="height:44px;color:#353535">환자 연결 관리</div>
        </button>
      </div>
    </div>

    <!-- 이웃 전용 푸터 -->
    <NeighborFooter />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useKakaoMap } from '@/composables/useKakaoMap'
import { logout } from '@/utils/auth'
import NeighborFooter from '@/components/NeighborFooter.vue'

import zone1 from '@/assets/images/zone 1.svg'
import locationIcon from '@/assets/images/location.svg'
import report2 from '@/assets/images/report2.png'
import connectIcon from '@/assets/images/connect.svg'

const router = useRouter()

// ✅ nextSchedule → upcomingSchedules 배열로 변경
const upcomingSchedules = ref([])

/* ===== Kakao Map Loader ===== */
const {
  mapEl,
  initMap
} = useKakaoMap({
  kakaoKey: import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891',
  center: { lat: 37.4943524920695, lng: 126.88767655688868 },
  defaultLevel: 3,
  enableControls: false,
  enableTracking: false
})

/* ===== 날짜 포맷팅 ===== */
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString + 'T00:00:00') // ISO 형식 보정
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}/${day}`
}

/* ===== 로그아웃 처리 ===== */
const handleLogout = async () => {
  const success = await logout()
  if (success) {
    router.push('/login')
  } else {
    alert('로그아웃에 실패했습니다.')
  }
}

/* ===== 오늘, 내일 일정 조회 ===== */
const fetchUpcomingSchedules = async () => {
  try {
    const response = await fetch('/NH/api/schedule/upcoming')
    if (!response.ok) {
      throw new Error(`API 오류: ${response.status}`)
    }

    const data = await response.json()
    upcomingSchedules.value = data || []
  } catch (error) {
    console.error('일정 조회 실패:', error)
    upcomingSchedules.value = []
  }
}

/* ===== 초기화 ===== */
onMounted(async () => {
  try {
    // DOM이 완전히 마운트될 때까지 대기
    await nextTick()
    
    // 지도 초기화 (빈 지도만 표시)
    await initMap()
    
    // ✅ 오늘, 내일 일정 2개 조회
    await fetchUpcomingSchedules()
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
/* 폰트 설정 */
@import url('https://cdn.jsdelivr.net/gh/sunn-us/SUIT/fonts/static/woff2/SUIT.css');

/* 안전 상태 텍스트 */
.safety-status-text {
  font-family: 'SUIT', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 1.2rem;
  font-weight: 400;
}

/* 지도 자세히 보기 버튼 */
.map-detail-btn {
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  background-color: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(4px);
}

/* 이웃 페이지 컨테이너 - 하단 푸터 공간 확보 */
.neighbor-page-container {
  padding-bottom: 100px;
  margin-bottom: 30px;
}
</style>

<!-- src/views/Pr.vue -->
<template>
  <div class="container-sm py-3" style="max-width:414px; position:relative;">

    <!-- (요청) 실종 제보 카드: 상태 문구보다 위로 이동 -->
    <div v-if="missingEvent" class="card border-0 shadow-sm mb-3">
      <div class="row g-3 align-items-center p-3">
        <div class="col-auto">
          <img v-if="missingEvent.avatarUrl" :src="missingEvent.avatarUrl" alt="face" class="rounded"
            style="width:56px;height:56px;object-fit:cover">
          <div v-else class="rounded-circle d-flex align-items-center justify-content-center bg-light border"
            style="width:56px;height:56px;font-size:28px;line-height:1">👤</div>
        </div>
        <div class="col">
          <div class="small fw-semibold">
            {{ missingEvent.name }} <span v-if="missingEvent.age">({{ missingEvent.age }})</span>
          </div>
          <div class="small text-secondary" v-if="missingEvent.location">실종 위치 : {{ missingEvent.location }}</div>
          <div class="small text-secondary" v-if="missingEvent.time">실종 시간 : {{ missingEvent.time }}</div>
        </div>
        <div class="col-12">
          <button class="btn btn-outline-dark w-100 rounded-pill" @click="goToMapMain">지도에서 보기</button>
        </div>
      </div>
    </div>

    <!-- 상단 헤더: 이벤트 발생시에만 노출 -->
    <div class="d-flex align-items-center mb-2" v-if="missingEvent">
      <h5 class="fw-bold m-0 me-auto">
        <span class="text-dark">{{ patient.name || '환자' }}</span>의 이웃을 찾아요
      </h5>
    </div>

    <!-- 상태 문구 -->
    <div class="my-3">
      <template v-if="connected">
        <div class="d-flex align-items-center justify-content-between">
          <div class="text-start flex-grow-1">
            <div class="patient-name-text">
              <span class="patient-name-bold">{{ patient.name }}</span>님은
            </div>
            <div class="d-flex align-items-center gap-2">
              <div 
                :class="safeZoneStatus.isInside ? 'status-indicator-safe' : 'status-indicator-danger'"
                class="status-indicator"
              ></div>
              <div :class="safeZoneStatus.isInside ? 'text-success' : 'text-danger'" class="safety-status-text">
                {{ safeZoneStatus.isInside ? '안전한 위치에 있습니다' : '안심존을 벗어났습니다' }}
              </div>
            </div>
          </div>
          <div class="activity-status-card">
            <div class="activity-icon">
              <img v-if="patientLocation" src="/figma/walk-person.gif" alt="활동중" class="activity-gif" />
              <img v-else src="/figma/stand-person.gif" alt="미접속" class="activity-gif" />
            </div>
            <div class="activity-text text-muted">
              {{ patientLocation ? '활동 중' : '미접속' }}
            </div>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="d-flex align-items-center justify-content-between">
          <div class="text-start flex-grow-1">
            <div class="d-flex align-items-center gap-2">
              <div class="status-indicator status-indicator-disconnected"></div>
              <div class="safety-status-text text-muted">연결한 환자가 없습니다</div>
            </div>
          </div>
          <div class="activity-status-card">
            <div class="activity-icon">
              <img src="/figma/stand-person.gif" alt="연결 없음" class="activity-gif" />
            </div>
            <div class="activity-text text-muted">연결 없음</div>
          </div>
        </div>
      </template>
    </div>

    <!-- Kakao 지도 프리뷰 -->
    <div class="card border-0 shadow-sm position-relative overflow-hidden mb-4 rounded-4">
      <div ref="mapEl" class="w-100" style="height:280px;"></div>
      <!-- 항상 노출 -->
      <button class="btn btn-light rounded-pill position-absolute start-50 translate-middle-x map-detail-btn"
        style="bottom:12px; z-index:10; pointer-events:auto" @click="goToMapMain">
        지도 자세히 보기
      </button>
    </div>

    <!-- 가장 빠른 일정 -->
    <h6 class="fw-bold mb-2">오늘의 일정</h6>
    <div v-if="nextSchedule" class="card border-2 rounded-3 p-3 mb-2" style="border-color:#e9ecef">
      <div class="d-flex justify-content-between align-items-center mb-1">
        <div class="d-flex align-items-center gap-2">
          <span class="d-inline-block rounded-circle" style="width:10px;height:10px;background:#6c757d"></span>
          <span class="fw-semibold">{{ nextSchedule.title }}</span>
        </div>
        <span class="text-secondary">{{ nextSchedule.time }}</span>
      </div>
      <div class="small text-secondary mb-1">{{ nextSchedule.location || '위치 정보 없음' }}</div>
      <div class="small text-secondary" v-if="nextSchedule.depart">예상 출발: {{ nextSchedule.depart }}</div>
      <div class="small text-secondary" v-if="nextSchedule.arrive">예상 도착: {{ nextSchedule.arrive }}</div>
    </div>
    <div v-else class="card border-0 shadow-sm rounded-4 mb-2">
      <div class="card-body text-center text-muted">오늘 남은 일정이 없습니다.</div>
    </div>

    <button class="btn btn-outline-dark w-100 rounded-pill mb-3" @click="router.push('/calendar')">
      일정 자세히 보기
    </button>

    <!-- 기능 타일 -->
    <div class="row g-3 align-items-stretch">
      <!-- 1) 기본 안심존 설정 -->
      <div class="col-6">
        <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
          @click="router.push({ name: 'basic-safe-zone-location' })" style="height:220px;
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
          @click="router.push('/predict-location')" style="height:196px;
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
          @click="router.push('/report')" style="height:196px;
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
          @click="router.push('/gdc')" style="height:220px;
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

    <!-- 에러 토스트 -->
    <div v-if="err" class="alert alert-warning mt-3" role="alert" style="white-space:pre-wrap">
      {{ err }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useKakaoMap } from '@/composables/useKakaoMap'
import { useSchedule } from '@/composables/useSchedule'
import { usePatientLocation } from '@/composables/usePatientLocation'

import zone1 from '@/assets/images/zone 1.svg'
import locationIcon from '@/assets/images/location.svg'
import report2 from '@/assets/images/report2.png'
import connectIcon from '@/assets/images/connect.svg'
const router = useRouter()

/* ===== API ===== */
const ENDPOINTS = {
  myPatient: '/api/user/my-patient',
  patientByNo: (no) => `/api/user/${no}`,
  lastRecord: (no) => `/api/record/last/${no}`,
  schedules: (no) => `/api/schedule/list/${no}`,
  scheduleLocations: (sn) => `/api/schedule/${sn}/locations`,
  basicSafeZone: (no) => `/api/schedule/basic-safe-zone/${no}`,
  activeMissing: (no) => `/api/missing/active/${no}`,
}

/* ===== 상태 ===== */
const connected = ref(false)
const err = ref('')

const patient = ref({ userNo: null, name: '', avatarUrl: null })
const missingEvent = ref(null)
const isReportModalVisible = ref(false)

// 안심존 상태 관련
const safeZoneStatus = ref({
  isInside: true,
  status: '연결 필요',
  color: '#9CA3AF',
  bgColor: '#F3F4F6'
})

// 안심존 관련
let currentSafeZone = null // 현재 표시 중인 안심존 폴리곤/원형

// 시연용 시뮬레이션 상태 관리
const simulationState = ref({
  isSimulating: true, // 시뮬레이션 모드 활성화
  locationState: 'center', // 'center' | 'away' | 'disconnected'
  currentPosition: { lat: 37.494381, lng: 126.887690 }, // 현재 시뮬레이션 위치
  targetPosition: { lat: 37.493677, lng: 126.885879 }, // 이탈 목표 위치
  centerPosition: { lat: 37.494381, lng: 126.887690 }, // 복귀 위치
  isMoving: false, // 이동 중 여부
  moveInterval: null // 이동 인터벌
})

/* ===== Kakao Map Loader ===== */
const {
  mapEl,
  mapInstance,
  initMap,
  setCenter,
  setBounds
} = useKakaoMap({
  kakaoKey: import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891',
  center: { lat: 37.4943524920695, lng: 126.88767655688868 },
  defaultLevel: 3,
  enableControls: false,
  enableTracking: false
})

/* ===== 일정 관련 composable ===== */
const {
  patientUserNo,
  allSchedules,
  scheduleLocations,
  todaySchedules,
  formatTime,
  formatLocation,
  getScheduleStatus,
  getScheduleCardStyle,
  loadScheduleData,
  getCurrentSchedule
} = useSchedule({
  fetchPatientInfo,
  onScheduleLoaded: async () => {
    // 일정 로드 완료 후 안심존 상태 다시 확인
    await checkPatientInSafeZone()
  }
})

// 환자 위치 추적 composable
const {
  patientLocation,
  patientMarker,
  startPatientLocationTracking,
  stopPatientLocationTracking,
  fetchPatientLocation,
  updatePatientMarker
} = usePatientLocation({
  patientUserNo,
  patientInfo: patient,
  mapInstance,
  simulationState, // 시뮬레이션 상태 전달
  onLocationUpdate: async (location) => {
    // 위치 업데이트 시 안심존 상태 확인
    await checkPatientInSafeZone()
  },
  onPatientInfoUpdate: (info) => {
    // 환자 정보 업데이트
  },
  onError: (error) => {
    console.error('환자 위치 추적 오류:', error)
  }
})

/* ===== 유틸 ===== */
function fmtTime(hhmm) {
  if (!hhmm) return ''
  const [h, m] = hhmm.split(':').map(Number)
  const isPM = h >= 12
  const hour12 = h % 12 || 12
  return `${isPM ? '오후' : '오전'} ${String(hour12).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}
function timeToMin(hhmm) { const [h, m] = hhmm.split(':').map(Number); return h * 60 + m }
function tsToLocal(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}
function formatTimestamp(timestamp) {
  if (!timestamp) return ''
  const d = new Date(timestamp)
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mi}`
}
async function fetchJSON(url) {
  const res = await fetch(url, { method: 'GET', credentials: 'include' })
  if (!res.ok) throw new Error(`${url} → ${res.status}`)
  return res.json()
}

/* ===== 환자 정보 관리 ===== */
// 보호자가 관리하는 환자 정보 가져오기
async function fetchPatientInfo() {
  try {
    const response = await fetch(`/api/user/my-patient`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      connected.value = false
      throw new Error('환자 정보를 가져올 수 없습니다.')
    }
    
    const patientData = await response.json()
    
    // 메시지만 있는 경우 (환자가 없는 경우)
    if (patientData.message) {
      console.warn(patientData.message)
      connected.value = false
      return null
    }
    
    // 환자 연결 상태 업데이트
    connected.value = true
    
    // 환자 정보 업데이트
    patient.value = {
      userNo: patientData.userNo,
      name: patientData.name || '',
      avatarUrl: patientData.profilePhoto || null
    }
    
    // 연결된 경우 위치 추적 시작
    if (connected.value && patientData.userNo && mapInstance.value) {
      startPatientLocationTracking()
    }
    
    return patientData.userNo
  } catch (error) {
    console.error('환자 정보 조회 오류:', error)
    connected.value = false
    return null
  }
}

async function getActiveMissing(no) {
  try {
    const r = await fetchJSON(ENDPOINTS.activeMissing(no))
    if (r?.message) { missingEvent.value = null }
    else if (r) {
      missingEvent.value = {
        name: r.name ?? patient.value.name ?? '환자',
        age: r.age ?? '',
        location: r.location ?? '',
        time: r.time ?? tsToLocal(r.ts) ?? '',
        avatarUrl: r.avatarUrl ?? null
      }
    }
  } catch { missingEvent.value = null }
}

/* ===== 안심존 데이터 관리 ===== */

// 일정의 안심존(버퍼) 가져오기
async function fetchScheduleSafeZone(scheduleNo) {
  try {
    const response = await fetch(`/api/schedule/${scheduleNo}/route`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('경로 정보를 가져올 수 없습니다.')
    }
    
    const route = await response.json()
    if (!route.bufferCoordinates) return null
    
    const bufferCoordinates = JSON.parse(route.bufferCoordinates)
    
    // 기존 데이터 호환성 처리
    if (Array.isArray(bufferCoordinates)) {
      return {
        level: 1,
        coordinates: bufferCoordinates
      }
    }
    
    return bufferCoordinates
  } catch (error) {
    console.error('일정 안심존 조회 오류:', error)
    return null
  }
}

// 기본 안심존 가져오기
async function fetchBasicSafeZone(userNo) {
  try {
    const response = await fetch(`/api/schedule/basic-safe-zone/${userNo}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('기본 안심존을 가져올 수 없습니다.')
    }
    
    const result = await response.json()
    
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

/* ===== 안심존 시각화 ===== */

// 지도에 경로형 안심존(버퍼 폴리곤) 그리기
function drawScheduleSafeZone(map, bufferCoordinates) {
  if (!map || !bufferCoordinates) return
  
  try {
    // 기존 안심존 제거
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
    }
    
    // bufferCoordinates 형식 처리
    let coordinates
    if (Array.isArray(bufferCoordinates)) {
      coordinates = bufferCoordinates
    } else if (bufferCoordinates.coordinates) {
      coordinates = bufferCoordinates.coordinates
    } else {
      console.error('지원하지 않는 bufferCoordinates 형식:', bufferCoordinates)
      return
    }
    
    const kakaoPath = coordinates.map(coord => 
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
    
    if (boundaryData.type === 'Circle') {
      const center = new window.kakao.maps.LatLng(boundaryData.center.lat, boundaryData.center.lng)
      const radius = boundaryData.radius
      
      // 원형 폴리곤 생성
      const circlePoints = []
      const steps = 64
      const earthRadius = 6371000
      
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
      
      if (bufferCoordinates && (
        (Array.isArray(bufferCoordinates) && bufferCoordinates.length > 0) ||
        (typeof bufferCoordinates === 'object' && bufferCoordinates.coordinates && bufferCoordinates.coordinates.length > 0)
      )) {
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

/* ===== 안심존 상태 확인 및 판단 ===== */

// 환자 위치가 안심존 내부에 있는지 판단
async function checkPatientInSafeZone() {
  // 환자와 연결되지 않은 경우
  if (!patientUserNo.value) {
    safeZoneStatus.value = {
      isInside: false,
      status: '연결 필요',
      color: '#9CA3AF',
      bgColor: '#F3F4F6'
    }
    return
  }
  
  // 환자 위치가 없는 경우
  if (!patientLocation.value) {
    safeZoneStatus.value = {
      isInside: false,
      status: '위치 확인 중',
      color: '#F59E0B',
      bgColor: '#FEF3C7'
    }
    return
  }
  
  try {
    const patientLat = patientLocation.value.latitude
    const patientLng = patientLocation.value.longitude
    
    let isInside = false
    
    // 현재 활성화된 안심존 정보 가져오기
    const currentSchedule = getCurrentSchedule()
    let safeZoneData = null
    
    if (currentSchedule) {
      // 현재 일정의 안심존 데이터 가져오기
      safeZoneData = await fetchScheduleSafeZone(currentSchedule.scheduleNo)
    } else {
      // 기본 안심존 데이터 가져오기
      safeZoneData = await fetchBasicSafeZone(patientUserNo.value)
    }
    
    if (safeZoneData) {
      if (currentSchedule && safeZoneData.coordinates) {
        // 경로형 안심존 (폴리곤) - 점이 폴리곤 내부에 있는지 판단
        const coordinates = Array.isArray(safeZoneData) ? safeZoneData : safeZoneData.coordinates
        isInside = isPointInPolygon(patientLat, patientLng, coordinates)
      } else if (safeZoneData.type === 'Circle') {
        // 기본형 안심존 (원형) - 중심점과의 거리 계산
        const centerLat = safeZoneData.center.lat
        const centerLng = safeZoneData.center.lng
        const radius = safeZoneData.radius
        
        const distance = calculateDistance(patientLat, patientLng, centerLat, centerLng)
        isInside = distance <= radius
      }
    }
    
    // 안심존 상태 업데이트
    if (isInside) {
      safeZoneStatus.value = {
        isInside: true,
        status: '안전',
        color: '#16A34A',
        bgColor: '#DCFCE7'
      }
    } else {
      safeZoneStatus.value = {
        isInside: false,
        status: '벗어남',
        color: '#EF4444',
        bgColor: '#FEE2E2'
      }
    }
    
    console.log(`안심존 상태: ${isInside ? '안전' : '벗어남'} (환자 위치: ${patientLat}, ${patientLng})`)
    
  } catch (error) {
    console.error('안심존 상태 확인 오류:', error)
    safeZoneStatus.value = {
      isInside: false,
      status: '위치 확인 중',
      color: '#F59E0B',
      bgColor: '#FEF3C7'
    }
  }
}

/* ===== 안심존 계산 유틸리티 ===== */

// 두 점 간의 거리 계산 (Haversine 공식)
function calculateDistance(lat1, lng1, lat2, lng2) {
  const R = 6371000 // 지구 반지름 (미터)
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLng/2) * Math.sin(dLng/2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
  return R * c
}

// 점이 폴리곤 내부에 있는지 판단 (Ray casting 알고리즘)
function isPointInPolygon(lat, lng, polygon) {
  let inside = false
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const xi = polygon[i].longitude
    const yi = polygon[i].latitude
    const xj = polygon[j].longitude
    const yj = polygon[j].latitude
    
    if (((yi > lng) !== (yj > lng)) && (lat < (xj - xi) * (lng - yi) / (yj - yi) + xi)) {
      inside = !inside
    }
  }
  return inside
}

/* ===== 기타 ===== */
const nextSchedule = computed(() => {
  const now = new Date()
  const yyyy = now.getFullYear()
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  const todayKey = `${yyyy}-${mm}-${dd}`

  const todayList = allSchedules.value
    .filter(s => s.scheduleDate === todayKey)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))

  const nowMin = now.getHours() * 60 + now.getMinutes()
  const upcoming = todayList.find(s => timeToMin(s.startTime) >= nowMin)
  if (!upcoming) return null

  const t = `${fmtTime(upcoming.startTime)} - ${fmtTime(upcoming.endTime)}`
  const loc = formatLocation(upcoming.scheduleNo)
  return { id: upcoming.scheduleNo, title: upcoming.scheduleTitle, time: t, location: loc, depart: null, arrive: null }
})
function openReportModal() {    // 실종 모달 함수
  if (!connected.value || !patient.value?.userNo) {
    alert("먼저 환자와 연결해야 합니다.");
    return;
  }
  isReportModalVisible.value = true; // 모달 온
}
async function onReportSuccess(reportData) {  // 실종 모달 확인용
  console.log("신고 접수 완료 (메인에서 받음):", reportData);
  await getActiveMissing(patient.value.userNo);
}

function toggleTestEvent() {
  if (missingEvent.value) { missingEvent.value = null }
  else {
    // 이벤트 발생 시 이름은 연결된 환자명 우선 사용
    missingEvent.value = {
      name: (patient.value.name || '환자'),
      age: 71,
      location: '청주 동남지구',
      time: tsToLocal(new Date().toISOString()),
      avatarUrl: null
    }
  }
}
function goToMapMain() { router.push('/map-main') }

/* ===== 연결 상태 변경 감지 ===== */
// 연결 상태 변경 시 위치 추적 시작/중지
watch([connected, patientUserNo], ([newConnected, newPatientUserNo]) => {
  if (newConnected && newPatientUserNo && mapInstance.value) {
    // 연결된 경우 위치 추적 시작
    startPatientLocationTracking()
  } else {
    // 연결 끊긴 경우 위치 추적 중지 및 마커 제거
    stopPatientLocationTracking()
    if (patientMarker.value) {
      patientMarker.value.setMap(null)
    }
    patientLocation.value = null
  }
}, { immediate: false })

/* ===== 초기화 ===== */
onMounted(async () => {
  try {
    // DOM이 완전히 마운트될 때까지 대기
    await nextTick()
    
    // 시뮬레이션 상태 초기화 및 localStorage에 저장 (기본값: 라크라센타)
    const savedSimState = localStorage.getItem('simulationState')
    if (!savedSimState) {
      // 시뮬레이션 상태가 없으면 초기화하여 저장
      simulationState.value = {
        isSimulating: true,
        locationState: 'center',
        currentPosition: { lat: 37.494381, lng: 126.887690 },
        targetPosition: { lat: 37.493677, lng: 126.885879 },
        centerPosition: { lat: 37.494381, lng: 126.887690 },
        isMoving: false,
        moveInterval: null
      }
      localStorage.setItem('simulationState', JSON.stringify({
        locationState: simulationState.value.locationState,
        currentPosition: simulationState.value.currentPosition
      }))
    } else {
      // localStorage에서 시뮬레이션 상태 복원
      try {
        const parsed = JSON.parse(savedSimState)
        if (parsed.currentPosition) {
          simulationState.value.currentPosition = parsed.currentPosition
          simulationState.value.locationState = parsed.locationState || 'center'
        }
      } catch (e) {
        console.error('시뮬레이션 상태 복원 오류:', e)
      }
    }
    
    // 일정 데이터 로드
    await loadScheduleData()
    
    // 지도 초기화 (DOM 요소가 준비된 후)
    await initMap()
    
    // 안심존 표시
    await updateSafeZone(mapInstance.value)
    
    // 환자 위치 추적 시작 (연결된 경우에만)
    if (connected.value && patientUserNo.value) {
      startPatientLocationTracking()
    }
    
    // 초기 안심존 상태 확인
    await checkPatientInSafeZone()
    
    // 실종 이벤트 확인
    if (patientUserNo.value) {
      await getActiveMissing(patientUserNo.value)
    }
  } catch (e) {
    console.error(e)
    err.value = `[메인 초기화 오류]\n${e?.message || e}`
  }
})

onBeforeUnmount(() => {
  // 환자 위치 추적 중지
  stopPatientLocationTracking()
})
</script>

<style scoped>
/* 상태 표시 인디케이터 */
.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-indicator-safe {
  background-color: #16A34A;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
}

.status-indicator-danger {
  background-color: #EF4444;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2);
}

.status-indicator-disconnected {
  background-color: #9CA3AF;
  box-shadow: 0 0 0 2px rgba(156, 163, 175, 0.2);
}

/* 폰트 설정 */
@import url('https://cdn.jsdelivr.net/gh/sunn-us/SUIT/fonts/static/woff2/SUIT.css');

/* 환자명 텍스트 */
.patient-name-text {
  font-family: 'SUIT', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 1.3rem;
  font-weight: 400;
  color: #333;
  margin-bottom: 4px;
}

.patient-name-bold {
  font-weight: 700;
}

/* 안전 상태 텍스트 */
.safety-status-text {
  font-family: 'SUIT', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 1.2rem;
  font-weight: 400;
}

/* 활동 상태 카드 */
.activity-status-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 6px 6px 8px 6px;
  background-color: #E7FDEE;
  border-radius: 6px;
  width: 50px;
  height: 60px;
}

.activity-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
  flex: 0 0 auto;
}

.activity-text {
  font-family: 'SUIT', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 0.7rem;
  font-weight: 400;
  text-align: center;
  color: #666;
  line-height: 1;
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 2px;
}

/* 활동 GIF 스타일 */
.activity-gif {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

/* 지도 자세히 보기 버튼 */
.map-detail-btn {
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  background-color: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(4px);
}
</style>

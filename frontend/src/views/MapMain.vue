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

    <!-- 지도 컨트롤 -->
    <MapControls 
      :is-patient-connected="isPatientConnected"
      :is-safe-zone-enabled="isSafeZoneEnabled"
      :is-safe-zone-control-expanded="isSafeZoneControlExpanded"
      :current-active-zone="currentActiveZone"
      :selected-level="selectedLevel"
      :buffer-levels="bufferLevels"
      :location-btn-bottom="locationBtnBottom"
      @toggle-safe-zone="toggleSafeZone"
      @toggle-safe-zone-control="toggleSafeZoneControl"
      @select-level="selectLevel"
      @zoom-in="zoomIn"
      @zoom-out="zoomOut"
      @move-to-patient-location="moveToPatientLocation"
    />
  </div>

  <!-- ================== Bottom Sheet ================== -->
  <div class="bs-backdrop"
    :style="{ opacity: backdropOpacity, pointerEvents: sheetHeight > collapsedH + 1 ? 'auto' : 'none' }"
    @click="toCollapsed"></div>

  <div ref="sheetEl" class="bs-sheet card rounded-top-4 shadow-lg" :style="sheetStyle" @pointerdown="onPointerDown">
    <div class="d-flex justify-content-center pt-2 pb-1">
      <div class="bs-handle"></div>
    </div>

    <!-- 환자 정보 카드 -->
    <PatientInfoCard 
      :patient-info="patientInfo"
      @go-to-my-page="goToMyPage"
      @report-missing="reportMissing"
      @go-to-connect="goToConnect"
      @statusUpdated="fetchPatientInfo"
    />

    <!-- 안심존 상태 카드 -->
    <SafeZoneStatusCard 
      :safe-zone-status="safeZoneStatus"
      :patient-steps="'1,057'"
      ref="safeZoneStatusCardRef"
    />

    <!-- 🔽 접힘 기준 앵커 -->
      <div ref="foldAnchor" style="height:0; margin:0; padding:0;"></div>

    <!-- 일정 목록 -->
    <ScheduleList 
      :today-schedules="todaySchedules"
      :format-time="formatTime"
      :format-location="formatLocation"
      :get-schedule-status="getScheduleStatus"
      :get-schedule-card-style="getScheduleCardStyle"
      :is-scrollable="sheetHeight >= openH() - 10"
      @go-to-calendar="goToCalendar"
      @select-schedule="selectSchedule"
    />
  </div>

</template>

<script setup>
import { ref, onMounted, defineProps, computed, nextTick, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { lineString, buffer, circle } from '@turf/turf'
import { useBottomSheet } from '@/composables/useBottomSheet'
import { useKakaoMap } from '@/composables/useKakaoMap'
import { useSchedule } from '@/composables/useSchedule'
import { usePatientLocation } from '@/composables/usePatientLocation'
import PatientInfoCard from '@/components/PatientInfoCard.vue'
import SafeZoneStatusCard from '@/components/SafeZoneStatusCard.vue'
import ScheduleList from '@/components/ScheduleList.vue'
import MapControls from '@/components/MapControls.vue'

const router = useRouter()

// 환자 정보 데이터
const patientInfo = ref({
  name: '',
  userNo: null,
  isOnline: false,
  lastActivity: null,
  user_status: 0 // 환자 상태변경 기본값
})


// 환자 위치 관련 변수들은 usePatientLocation composable에서 관리

// 안심존 상태 관련
const safeZoneStatus = ref({
  isInside: true,
  status: '연결 필요',
  color: '#9CA3AF',
  bgColor: '#F3F4F6'
})

// 안심존 관련
let currentSafeZone = null // 현재 표시 중인 안심존 폴리곤/원형
let previewSafeZone = null // 미리보기 안심존

// 안심존 활성화 상태 관리
const isSafeZoneEnabled = ref(true) // 안심존 활성화 상태
const isPatientConnected = ref(false) // 환자 연결 상태

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

// 안심존 범위 설정 컨트롤 상태
const isSafeZoneControlExpanded = ref(false)
const selectedLevel = ref(1) // 현재 선택된 단계
const currentActiveZone = ref(null) // 현재 활성화된 안심존 정보 { type, data, scheduleNo?, level }
const originalLevel = ref(1) // 원래 단계 (취소 시 복원용)

// 일정 관련 composable
const {
  patientUserNo,
  allSchedules,
  scheduleLocations,
  selectedScheduleIndex,
  todaySchedules,
  formatTime,
  formatLocation,
  getScheduleStatus,
  getScheduleCardStyle,
  loadScheduleData,
  getCurrentSchedule,
  selectSchedule
} = useSchedule({
  fetchPatientInfo,
  onScheduleLoaded: () => {
    // 일정 로드 완료 후 안심존 상태 다시 확인
    checkPatientInSafeZone()
  }
})

// 버퍼 레벨 설정
const bufferLevels = [
  { value: 1, name: '1단계', desc: '30m' },
  { value: 2, name: '2단계', desc: '60m' },
  { value: 3, name: '3단계', desc: '100m' }
]

// 일정 관련 함수들은 useSchedule composable에서 가져옴

// 캘린더 페이지로 이동하는 함수
const goToCalendar = () => {
  router.push('/calendar')
}

// 연결 페이지로 이동하는 함수
const goToConnect = () => {
  router.push('/gdc')
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
const {
  mapEl,
  mapInstance,
  err,
  initMap,
  zoomIn,
  zoomOut,
  setCenter,
  setBounds,
  moveToPatientLocation: moveToPatientLocationMap,
  relayout
} = useKakaoMap({
  kakaoKey: props.kakaoKey,
  center: props.center,
  defaultLevel: 3,
  enableControls: true,
  enableTracking: true
})

// 환자 위치 추적 composable (mapInstance 초기화 후)
const {
  patientLocation,
  patientMarker,
  
  startPatientLocationTracking,
  stopPatientLocationTracking,
  fetchPatientLocation,
  updatePatientMarker
} = usePatientLocation({
  patientUserNo,
  patientInfo,
  mapInstance,
  simulationState, // 시뮬레이션 상태 전달
  onLocationUpdate: (location) => {
    // 위치 업데이트 시 안심존 상태 확인
    checkPatientInSafeZone()
  },
  onPatientInfoUpdate: (info) => {
    // 환자 정보 업데이트 (필요시 추가 로직)
  },
  onError: (error) => {
    console.error('환자 위치 추적 오류:', error)
  }
})

onMounted(async () => {
  // 키보드 이벤트 리스너 등록 (시뮬레이션용)
  window.addEventListener('keydown', handleKeyDown)
  
  // localStorage에서 안심존 상태 불러오기
  const saved = localStorage.getItem('safeZoneEnabled')
  if (saved !== null) {
    isSafeZoneEnabled.value = JSON.parse(saved)
  }
  
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
  }
  
  // 일정 데이터 로드
  await loadScheduleData()
  
  try {
    // 지도 초기화
    await initMap()
    
    // 안심존 표시
    await updateSafeZone(mapInstance.value)
    
    // 환자 위치 추적 시작
    startPatientLocationTracking()
    
    // 초기 안심존 상태 확인
    checkPatientInSafeZone()
    
    // 바텀시트 초기화
    initBottomSheet()
  } catch (e) { console.error(e); err.value = e.message }
  await nextTick()
  // measureCollapsed는 컴포넌트가 마운트된 후 호출
  nextTick(() => {
    if (safeZoneStatusCardRef.value?.topTilesRow) {
      measureCollapsedWithRef(safeZoneStatusCardRef.value.topTilesRow)
    }
  })
})

onBeforeUnmount(() => {
  // 키보드 이벤트 리스너 제거
  window.removeEventListener('keydown', handleKeyDown)
  // 이동 인터벌 정리
  if (simulationState.value.moveInterval) {
    clearInterval(simulationState.value.moveInterval)
  }
  // 바텀시트 정리
  cleanupBottomSheet()
  // 환자 위치 추적 중지 (usePatientLocation에서 자동으로 처리됨)
})

/* ===== Bottom Sheet 관리 ===== */
/* ===== Bottom Sheet: 드래그로만 열기 (collapsed ↔ 80% open) ===== */
const {
  sheetEl,
  foldAnchor,
  sheetHeight,
  collapsedH,
  sheetStyle,
  backdropOpacity,
  locationBtnBottom,
  openH,
  onPointerDown,
  measureCollapsed,
  toCollapsed,
  init: initBottomSheet,
  cleanup: cleanupBottomSheet
} = useBottomSheet({ foldNudge: props.foldNudge })

// 컴포넌트 참조
const safeZoneStatusCardRef = ref(null)

// measureCollapsed 함수를 컴포넌트 ref와 함께 사용하는 래퍼 함수
function measureCollapsedWithRef(topTilesRowRef) {
  try {
    if (!sheetEl.value || !foldAnchor.value || !topTilesRowRef) return
    const sheetRect = sheetEl.value.getBoundingClientRect()
    const anchorRect = foldAnchor.value.getBoundingClientRect()
    // 앵커의 bottom이 시트 상단에서 얼마나 떨어져 있는지 + 여백
    const desired = Math.ceil(anchorRect.bottom - sheetRect.top + 50 + (props.foldNudge || 0))
    const clamped = Math.max(200, Math.min(desired, openH() - 8))
    collapsedH.value = clamped
    // 드래그 중이 아니면 접힘 높이로 설정
    sheetHeight.value = collapsedH.value
  } catch (e) {
    console.warn('measureCollapsedWithRef failed', e)
  }
}

/* ===== 지도 컨트롤 ===== */
// zoomIn, zoomOut 함수는 useKakaoMap에서 가져옴

/* ===== 일정 관련 함수 ===== */
// formatLastActivity 함수는 PatientInfoCard 컴포넌트로 이동

/* ===== 환자 정보 관리 ===== */

// 보호자가 관리하는 환자 정보 가져오기
async function fetchPatientInfo() {
  try {
    const response = await fetch(`/api/user/my-patient`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      isPatientConnected.value = false
      throw new Error('환자 정보를 가져올 수 없습니다.')
    }
    
    const patient = await response.json()
    
// 메시지만 있는 경우 (환자가 없는 경우)
    if (patient.message) {
      console.warn(patient.message)
      isPatientConnected.value = false
      patientInfo.value = { name: '', userNo: null, isOnline: false, lastActivity: null, user_status: 0 }; 
      return null // 환자가 없으므로 함수 종료
    } 
    else { 
      console.log("API 응답 (patient 객체):", JSON.stringify(patient, null, 2)); // 응답 데이터 확인용 로그

      // 환자 연결 상태 업데이트
      isPatientConnected.value = true
      
      // 환자 정보 업데이트
      patientInfo.value = {
        name: patient.name || '', 
        userNo: patient.userNo, 
        isOnline: patient.isOnline ?? false, 
        lastActivity: patient.lastActivity || null, 
        user_status: patient.userStatus === 1 ? 1 : 0 
      }
      console.log("업데이트된 patientInfo.value:", JSON.stringify(patientInfo.value, null, 2));
      
      return patient.userNo // 환자 번호 반환
    }
    
    return patient.userNo
  } catch (error) {
    console.error('환자 정보 조회 오류:', error)
    isPatientConnected.value = false
    return null
  }
}

/* ===== 일정 관련 함수 ===== */
// 일정 관련 함수들은 useSchedule composable에서 가져옴

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
    // bufferCoordinates가 배열인 경우 (기존 형식) level 정보 추가
    if (Array.isArray(bufferCoordinates)) {
      return {
        level: 1, // 기본값
        coordinates: bufferCoordinates
      }
    }
    
    // bufferCoordinates가 객체인 경우 (새 형식) 그대로 반환
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
      // 기존 형식: [{ latitude, longitude }, ...]
      coordinates = bufferCoordinates
    } else if (bufferCoordinates.coordinates) {
      // 새 형식: { level: 2, coordinates: [{ latitude, longitude }, ...] }
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

/* ===== 안심존 업데이트 및 관리 ===== */

// 안심존 업데이트 (현재 일정에 따라)
async function updateSafeZone(map) {
  if (!map || !patientUserNo.value) return
  
  // 안심존이 꺼져있으면 표시하지 않음
  if (!isSafeZoneEnabled.value) {
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
      currentSafeZone = null
      currentActiveZone.value = null
    }
    return
  }
  
  try {
    // 1. 현재 진행 중인 일정 찾기
    const currentSchedule = getCurrentSchedule()
    
    if (currentSchedule) {
      // 2. 진행 중인 일정이 있으면 해당 일정의 안심존 표시
      console.log('현재 진행 중인 일정:', currentSchedule.scheduleTitle)
      const bufferCoordinates = await fetchScheduleSafeZone(currentSchedule.scheduleNo)
      
      if (bufferCoordinates && (
        // 배열 형식 (기존 데이터)
        (Array.isArray(bufferCoordinates) && bufferCoordinates.length > 0) ||
        // 객체 형식 (새 데이터)
        (typeof bufferCoordinates === 'object' && bufferCoordinates.coordinates && bufferCoordinates.coordinates.length > 0)
      )) {
        // 경로형 안심존 단계 파악
        const level = detectRouteSafeZoneLevel(bufferCoordinates)
        currentActiveZone.value = {
          type: '경로형',
          data: bufferCoordinates,
          scheduleNo: currentSchedule.scheduleNo,
          level: level
        }
        selectedLevel.value = level
        originalLevel.value = level
        
        drawScheduleSafeZone(map, bufferCoordinates)
        return
      }
    }
    
    // 3. 진행 중인 일정이 없거나 안심존이 없으면 기본 안심존 표시
    console.log('기본 안심존 표시')
    const basicSafeZone = await fetchBasicSafeZone(patientUserNo.value)
    
    if (basicSafeZone) {
      // 기본형 안심존 단계 파악
      const level = detectBasicSafeZoneLevel(basicSafeZone)
      currentActiveZone.value = {
        type: '기본형',
        data: basicSafeZone,
        level: level
      }
      selectedLevel.value = level
      originalLevel.value = level
      
      drawBasicSafeZone(map, basicSafeZone)
    } else {
      console.warn('표시할 안심존이 없습니다.')
      currentActiveZone.value = null
    }
  } catch (error) {
    console.error('안심존 업데이트 오류:', error)
    currentActiveZone.value = null
  }
}

/* ===== 안심존 레벨 감지 ===== */

// 기본형 안심존의 단계 파악
function detectBasicSafeZoneLevel(boundaryData) {
  if (!boundaryData || boundaryData.type !== 'Circle') return 1
  
  // level이 직접 저장되어 있는 경우
  if (boundaryData.level) {
    return boundaryData.level
  }
  
  // radius로부터 역산
  const radius = boundaryData.radius
  if (radius <= 30) return 1
  if (radius <= 60) return 2
  if (radius <= 100) return 3
  return 1
}

// 경로형 안심존의 단계 파악
function detectRouteSafeZoneLevel(bufferCoordinates) {
  if (!bufferCoordinates || bufferCoordinates.length === 0) return 1
  
  // bufferCoordinates가 객체인 경우 (메타데이터 포함)
  if (typeof bufferCoordinates === 'object' && !Array.isArray(bufferCoordinates)) {
    // level 정보가 직접 저장되어 있는 경우
    if (bufferCoordinates.level) {
      return bufferCoordinates.level
    }
    
    // coordinates 배열에서 level 정보 확인
    if (bufferCoordinates.coordinates && Array.isArray(bufferCoordinates.coordinates)) {
      // 첫 번째 좌표에 level 정보가 있을 수 있음
      const firstCoord = bufferCoordinates.coordinates[0]
      if (firstCoord && firstCoord.level) {
        return firstCoord.level
      }
    }
  }
  
  // bufferCoordinates가 배열인 경우, 첫 번째 요소에 level 정보가 있을 수 있음
  if (Array.isArray(bufferCoordinates) && bufferCoordinates.length > 0) {
    const firstCoord = bufferCoordinates[0]
    if (firstCoord && typeof firstCoord === 'object' && firstCoord.level) {
      return firstCoord.level
    }
  }
  
  // level 정보를 찾을 수 없는 경우 기본값 1 반환
  return 1
}

/* ===== 안심존 설정 및 컨트롤 ===== */

// 안심존 범위 설정 컨트롤 토글
function toggleSafeZoneControl() {
  if (!currentActiveZone.value || !isSafeZoneEnabled.value) return
  
  if (isSafeZoneControlExpanded.value) {
    // 확인 버튼 클릭 - 저장
    saveSafeZoneLevel()
  } else {
    // 컨트롤 열기
    isSafeZoneControlExpanded.value = true
  }
}

// 단계 선택
function selectLevel(level) {
  selectedLevel.value = level
  
  // 실시간 미리보기 업데이트
  updatePreviewSafeZone()
}

// 미리보기 안심존 업데이트
function updatePreviewSafeZone() {
  if (!mapInstance.value || !currentActiveZone.value) return
  
  // 기존 미리보기 제거
  if (previewSafeZone) {
    previewSafeZone.setMap(null)
  }
  
  const level = selectedLevel.value
  const radiusMap = { 1: 30, 2: 60, 3: 100 }
  const radius = radiusMap[level]
  
  try {
    if (currentActiveZone.value.type === '기본형') {
      // 기본형 안심존 미리보기
      const boundaryData = currentActiveZone.value.data
      const center = [boundaryData.center.lng, boundaryData.center.lat]
      const options = { steps: 64, units: 'meters' }
      const circleGeoJSON = circle(center, radius, options)
      
      const coordinates = circleGeoJSON.geometry.coordinates[0]
      const kakaoPath = coordinates.map(coord => 
        new window.kakao.maps.LatLng(coord[1], coord[0])
      )
      
      previewSafeZone = new window.kakao.maps.Polygon({
        path: kakaoPath,
        strokeWeight: 2,
        strokeColor: '#6366f1',
        strokeOpacity: 0.4,
        fillColor: '#6366f1',
        fillOpacity: 0.1
      })
      
      previewSafeZone.setMap(mapInstance.value)
      
    } else if (currentActiveZone.value.type === '경로형') {
      // 경로형 안심존 미리보기
      // 원본 경로 좌표를 가져와야 함
      const scheduleNo = currentActiveZone.value.scheduleNo
      fetchRouteCoordinates(scheduleNo).then(routeCoordinates => {
        if (!routeCoordinates || routeCoordinates.length < 2) return
        
        // Turf.js로 버퍼 생성
        const turfCoords = routeCoordinates.map(c => [c.longitude, c.latitude])
        const line = lineString(turfCoords)
        const buffered = buffer(line, radius, { units: 'meters' })
        
        const coordinates = buffered.geometry.coordinates[0]
        const kakaoPath = coordinates.map(coord => 
          new window.kakao.maps.LatLng(coord[1], coord[0])
        )
        
        previewSafeZone = new window.kakao.maps.Polygon({
          path: kakaoPath,
          strokeWeight: 2,
          strokeColor: '#EF4444',
          strokeOpacity: 0.4,
          fillColor: '#EF4444',
          fillOpacity: 0.1
        })
        
        previewSafeZone.setMap(mapInstance.value)
      })
    }
  } catch (error) {
    console.error('미리보기 안심존 생성 오류:', error)
  }
}

/* ===== 안심존 미리보기 및 저장 ===== */

// 경로 좌표 가져오기
async function fetchRouteCoordinates(scheduleNo) {
  try {
    const response = await fetch(`/api/schedule/${scheduleNo}/route`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) return null
    
    const route = await response.json()
    return route.routeCoordinates ? JSON.parse(route.routeCoordinates) : null
  } catch (error) {
    console.error('경로 좌표 조회 오류:', error)
    return null
  }
}

 // 안심존 단계 저장
 async function saveSafeZoneLevel() {
   if (!currentActiveZone.value) return
   
   try {
     const level = selectedLevel.value
     const radiusMap = { 1: 30, 2: 60, 3: 100 }
     const radius = radiusMap[level]
     
     if (currentActiveZone.value.type === '기본형') {
       // 기본형 안심존 업데이트
       const boundaryData = currentActiveZone.value.data
       const updatedBoundary = {
         ...boundaryData,
         radius: radius,
         level: level
       }
       
       const response = await fetch(`/api/schedule/basic-safe-zone`, {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         credentials: 'include',
         body: JSON.stringify({
           boundaryCoordinates: JSON.stringify(updatedBoundary)
         })
       })
       
       if (!response.ok) {
         throw new Error('기본 안심존 저장에 실패했습니다.')
       }
       
     } else if (currentActiveZone.value.type === '경로형') {
       // 경로형 안심존 업데이트
       const scheduleNo = currentActiveZone.value.scheduleNo
       const routeCoordinates = await fetchRouteCoordinates(scheduleNo)
       
       if (!routeCoordinates || routeCoordinates.length < 2) {
         throw new Error('경로 정보를 찾을 수 없습니다.')
       }
       
       // 새로운 버퍼 생성
       const turfCoords = routeCoordinates.map(c => [c.longitude, c.latitude])
       const line = lineString(turfCoords)
       const buffered = buffer(line, radius, { units: 'meters' })
       
       // level 정보를 포함한 bufferCoordinates 생성
       const coordinates = buffered.geometry.coordinates[0].map(coord => ({
         latitude: coord[1],
         longitude: coord[0]
       }))
       
       // level 정보를 메타데이터로 포함
       const bufferCoordinates = {
         level: level,
         coordinates: coordinates
       }
       
       const response = await fetch(`/api/schedule/route-safe-zone/${scheduleNo}`, {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         credentials: 'include',
         body: JSON.stringify({
           bufferCoordinates: JSON.stringify(bufferCoordinates),
           level: level
         })
       })
       
       if (!response.ok) {
         throw new Error('경로형 안심존 저장에 실패했습니다.')
       }
     }
     
     // 미리보기 제거 (저장 전에 미리 제거)
     if (previewSafeZone) {
       previewSafeZone.setMap(null)
       previewSafeZone = null
     }
     
     // 안심존 다시 로드
     await updateSafeZone(mapInstance.value)
     
     // 안심존 상태 다시 확인
     checkPatientInSafeZone()
     
     // 저장 성공 - UI 업데이트 (모든 비동기 작업 완료 후)
     isSafeZoneControlExpanded.value = false
     originalLevel.value = level
     
   } catch (error) {
     console.error('안심존 저장 오류:', error)
     alert('안심존 저장에 실패했습니다.')
   }
 }

// 컨트롤이 닫힐 때 미리보기 제거 (뒤로가기, 홈 버튼 등)
watch(isSafeZoneControlExpanded, (newVal) => {
  if (!newVal) {
    // 컨트롤이 닫힐 때 미리보기 제거
    if (previewSafeZone) {
      previewSafeZone.setMap(null)
      previewSafeZone = null
    }
    // 선택 단계를 원래대로 복원
    selectedLevel.value = originalLevel.value
  }
})

// 페이지 떠날 때 미리보기 제거
onBeforeUnmount(() => {
  if (previewSafeZone) {
    previewSafeZone.setMap(null)
  }
})

/* ===== 환자 위치 추적 관련 함수 ===== */
// 환자 위치 추적 관련 함수들은 usePatientLocation composable에서 관리

/* ===== 안심존 상태 확인 및 판단 ===== */

// 환자 위치가 안심존 내부에 있는지 판단
function checkPatientInSafeZone() {
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
  
  // 안심존이 꺼져있는 경우
  if (!isSafeZoneEnabled.value) {
    safeZoneStatus.value = {
      isInside: false,
      status: '안심존 비활성화',
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
  
  // 안심존이 없는 경우 (환자 위치는 있음)
  if (!currentActiveZone.value) {
    safeZoneStatus.value = {
      isInside: false,
      status: '안심존 미설정',
      color: '#9CA3AF',
      bgColor: '#F3F4F6'
    }
    return
  }
  
  try {
    const patientLat = patientLocation.value.latitude
    const patientLng = patientLocation.value.longitude
    
    let isInside = false
    
    if (currentActiveZone.value.type === '기본형') {
      // 기본형 안심존 (원형) - 중심점과의 거리 계산
      const boundaryData = currentActiveZone.value.data
      const centerLat = boundaryData.center.lat
      const centerLng = boundaryData.center.lng
      const radius = boundaryData.radius
      
      // 두 점 간의 거리 계산 (미터 단위)
      const distance = calculateDistance(patientLat, patientLng, centerLat, centerLng)
      isInside = distance <= radius
      
    } else if (currentActiveZone.value.type === '경로형') {
      // 경로형 안심존 (폴리곤) - 점이 폴리곤 내부에 있는지 판단
      const bufferCoordinates = currentActiveZone.value.data
      let coordinates
      
      if (Array.isArray(bufferCoordinates)) {
        coordinates = bufferCoordinates
      } else if (bufferCoordinates.coordinates) {
        coordinates = bufferCoordinates.coordinates
      } else {
        isInside = true // 좌표를 찾을 수 없으면 안전으로 처리
      }
      
      if (coordinates) {
        isInside = isPointInPolygon(patientLat, patientLng, coordinates)
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
    // 오류 발생 시 위치 확인 중 상태로 설정
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

/* ===== 안심존 토글 및 활성화 관리 ===== */

// 안심존 토글 함수
function toggleSafeZone() {
  if (!isPatientConnected.value) return
  
  isSafeZoneEnabled.value = !isSafeZoneEnabled.value
  localStorage.setItem('safeZoneEnabled', JSON.stringify(isSafeZoneEnabled.value))
  
  if (isSafeZoneEnabled.value) {
    // 안심존 켜기 - 다시 표시
    updateSafeZone(mapInstance.value)
    checkPatientInSafeZone()
  } else {
    // 안심존 끄기 - 완전히 제거
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
      currentSafeZone = null
      currentActiveZone.value = null
    }
    // 안심존 범위 설정 컨트롤도 닫기
    isSafeZoneControlExpanded.value = false
    checkPatientInSafeZone()
  }
}

/* ===== 지도 컨트롤 및 네비게이션 ===== */

// 현위치 버튼 클릭 시 환자 위치로 이동
async function moveToPatientLocation() {
   await moveToPatientLocationMap(
     patientLocation.value, 
     patientUserNo.value, 
     fetchPatientLocation, 
     goToConnect
   )
 }

/* ===== 페이지 네비게이션 ===== */

// 실종신고 버튼 클릭 (기능 구현 예정)
function reportMissing() {
   console.log('실종신고 기능 - 추후 구현 예정')
   // TODO: 실종신고 기능 구현
 }

 // 마이페이지로 이동
 function goToMyPage() {
   router.push('/gdmypage')
 }

/* ===== 시연용 시뮬레이션 기능 ===== */

// 두 점 간의 거리 계산 (미터)
function calculateDistanceMeters(lat1, lng1, lat2, lng2) {
  const R = 6371000 // 지구 반지름 (미터)
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLng/2) * Math.sin(dLng/2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
  return R * c
}

// 두 점 사이의 각도 계산 (라디안)
function calculateBearing(lat1, lng1, lat2, lng2) {
  const dLng = (lng2 - lng1) * Math.PI / 180
  const lat1Rad = lat1 * Math.PI / 180
  const lat2Rad = lat2 * Math.PI / 180
  const y = Math.sin(dLng) * Math.cos(lat2Rad)
  const x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLng)
  return Math.atan2(y, x)
}

// 목표 위치로 이동 시작 (사람 속도: 1.4m/s, 3초마다 업데이트)
function startMovingToTarget() {
  if (simulationState.value.isMoving) return
  
  const speedMps = 1.4 // 1.4m/s
  const updateInterval = 3000 // 3초마다 업데이트
  const distancePerUpdate = speedMps * (updateInterval / 1000) // 3초 동안 이동 거리 (미터)
  
  const startPos = { ...simulationState.value.currentPosition }
  const target = simulationState.value.locationState === 'center' 
    ? simulationState.value.targetPosition 
    : simulationState.value.centerPosition
  
  console.log('[시연] 환자 이동 시작:', {
    시작위치: startPos,
    목표위치: target,
    속도: `${speedMps}m/s`,
    업데이트간격: `${updateInterval}ms`
  })
  
  const totalDistance = calculateDistanceMeters(
    startPos.lat, startPos.lng, 
    target.lat, target.lng
  )
  
  if (totalDistance < 1) {
    // 목표에 거의 도달했으면 즉시 이동
    simulationState.value.currentPosition = { ...target }
    simulationState.value.locationState = simulationState.value.locationState === 'center' ? 'away' : 'center'
    // localStorage에 상태 저장 (GD_main.vue와 공유)
    localStorage.setItem('simulationState', JSON.stringify({
      locationState: simulationState.value.locationState,
      currentPosition: simulationState.value.currentPosition
    }))
    // 환자 위치 강제 업데이트
    if (fetchPatientLocation) {
      fetchPatientLocation()
    }
    return
  }
  
  const bearing = calculateBearing(startPos.lat, startPos.lng, target.lat, target.lng)
  const totalSteps = Math.ceil(totalDistance / distancePerUpdate)
  let currentStep = 0
  
  simulationState.value.isMoving = true
  
  simulationState.value.moveInterval = setInterval(() => {
    currentStep++
    const progress = currentStep / totalSteps
    
    if (progress >= 1) {
      // 목표 도달
      simulationState.value.currentPosition = { ...target }
      const newState = simulationState.value.locationState === 'center' ? 'away' : 'center'
      simulationState.value.locationState = newState
      console.log(`[시연] 환자 이동 완료: ${newState === 'away' ? '안심존 이탈 위치 도달' : '라크라센타 복귀 완료'}`)
      // localStorage에 상태 저장 (GD_main.vue와 공유)
      localStorage.setItem('simulationState', JSON.stringify({
        locationState: simulationState.value.locationState,
        currentPosition: simulationState.value.currentPosition
      }))
      clearInterval(simulationState.value.moveInterval)
      simulationState.value.moveInterval = null
      simulationState.value.isMoving = false
      
      // 안심존 상태 즉시 확인 (이탈 시 alert 표시를 위해)
      if (fetchPatientLocation) {
        fetchPatientLocation().then(() => {
          setTimeout(async () => {
            checkPatientInSafeZone()
            // App.vue의 안심존 모니터링 함수 호출 (전역적으로 접근 가능하도록)
            if (window.triggerSafeZoneCheck) {
              await window.triggerSafeZoneCheck()
            }
          }, 200)
        })
      }
    } else {
      console.log(`[시연] 환자 이동 중... (${Math.round(progress * 100)}%)`, {
        현재위치: simulationState.value.currentPosition,
        목표위치: target
      })
      // 중간 위치 계산
      const currentDistance = totalDistance * progress
      const R = 6371000
      const lat1Rad = startPos.lat * Math.PI / 180
      const lng1Rad = startPos.lng * Math.PI / 180
      
      const newLat = Math.asin(
        Math.sin(lat1Rad) * Math.cos(currentDistance / R) +
        Math.cos(lat1Rad) * Math.sin(currentDistance / R) * Math.cos(bearing)
      ) * 180 / Math.PI
      
      const newLng = (lng1Rad + Math.atan2(
        Math.sin(bearing) * Math.sin(currentDistance / R) * Math.cos(lat1Rad),
        Math.cos(currentDistance / R) - Math.sin(lat1Rad) * Math.sin(newLat * Math.PI / 180)
      )) * 180 / Math.PI
      
      simulationState.value.currentPosition = { lat: newLat, lng: newLng }
      // localStorage에 상태 저장 (GD_main.vue와 공유) - 이동 중에도 업데이트
      localStorage.setItem('simulationState', JSON.stringify({
        locationState: simulationState.value.locationState,
        currentPosition: simulationState.value.currentPosition
      }))
      
      // 이동 중에도 안심존 상태 확인 (이탈 시 즉시 alert 표시를 위해)
      if (fetchPatientLocation) {
        fetchPatientLocation().then(() => {
          setTimeout(async () => {
            checkPatientInSafeZone()
            // App.vue의 안심존 모니터링 함수 호출
            if (window.triggerSafeZoneCheck) {
              await window.triggerSafeZoneCheck()
            }
          }, 100)
        })
      }
    }
  }, updateInterval)
}

// Ctrl + 1: 위치 이동/복귀 토글
function handleSimulationToggle() {
  if (!simulationState.value.isSimulating || !patientUserNo.value) return
  
  if (simulationState.value.locationState === 'center') {
    // 이탈 위치로 이동 시작
    console.log('[시연] Ctrl+1: 환자 이탈 위치로 이동 시작')
    startMovingToTarget()
  } else {
    // 즉시 복귀
    console.log('[시연] Ctrl+1: 환자 복귀 (라크라센타로 즉시 이동)')
    if (simulationState.value.moveInterval) {
      clearInterval(simulationState.value.moveInterval)
      simulationState.value.moveInterval = null
      simulationState.value.isMoving = false
    }
    simulationState.value.currentPosition = { ...simulationState.value.centerPosition }
    simulationState.value.locationState = 'center'
    // localStorage에 상태 저장 (GD_main.vue와 공유)
    localStorage.setItem('simulationState', JSON.stringify({
      locationState: simulationState.value.locationState,
      currentPosition: simulationState.value.currentPosition
    }))
    // 환자 위치 강제 업데이트 및 안심존 상태 확인
    if (fetchPatientLocation) {
      fetchPatientLocation().then(() => {
        // 위치 업데이트 후 안심존 상태 확인
        setTimeout(() => {
          checkPatientInSafeZone()
        }, 100)
      })
    }
  }
}

// Ctrl + 2: 연결 끊김/복구 토글
function handleDisconnectToggle() {
  if (!simulationState.value.isSimulating || !patientUserNo.value) return
  
  if (simulationState.value.locationState === 'disconnected') {
    // 연결 복구: 라크라센타 위치로 복귀
    console.log('[시연] Ctrl+2: 환자 연결 복구')
    simulationState.value.locationState = 'center'
    simulationState.value.currentPosition = { ...simulationState.value.centerPosition }
    // localStorage에 상태 저장 (GD_main.vue와 공유)
    localStorage.setItem('simulationState', JSON.stringify({
      locationState: simulationState.value.locationState,
      currentPosition: simulationState.value.currentPosition
    }))
    // 이동 중지
    if (simulationState.value.moveInterval) {
      clearInterval(simulationState.value.moveInterval)
      simulationState.value.moveInterval = null
      simulationState.value.isMoving = false
    }
    // 환자 정보를 온라인으로 복구
    patientInfo.value.isOnline = true
    patientInfo.value.lastActivity = new Date()
    // 환자 위치 강제 업데이트
    if (fetchPatientLocation) {
      fetchPatientLocation()
    }
  } else {
    // 연결 끊김: 즉시 오프라인 처리
    console.log('[시연] Ctrl+2: 환자 연결 끊김')
    simulationState.value.locationState = 'disconnected'
    // localStorage에 상태 저장 (GD_main.vue와 공유)
    localStorage.setItem('simulationState', JSON.stringify({
      locationState: simulationState.value.locationState,
      currentPosition: simulationState.value.currentPosition
    }))
    // 이동 중지
    if (simulationState.value.moveInterval) {
      clearInterval(simulationState.value.moveInterval)
      simulationState.value.moveInterval = null
      simulationState.value.isMoving = false
    }
    // 환자 정보를 오프라인으로 설정 (즉시, 시간 대기 없이)
    patientInfo.value.isOnline = false
    patientInfo.value.lastActivity = null
    // 환자 위치를 null로 설정 (지도에서 사라짐)
    patientLocation.value = null
    if (patientMarker.value) {
      patientMarker.value.setMap(null)
    }
    // 모달 표시 (App.vue의 전역 함수 호출)
    setTimeout(() => {
      if (window.showDisconnectionModal) {
        window.showDisconnectionModal(patientInfo.value.name || '환자')
      }
    }, 100)
  }
}

// Ctrl + 3: 현관문 열림 감지 알림
function handleDoorOpenAlert() {
  console.log('[시연] Ctrl+3: 현관문 열림 감지 알림')
  // 모달 표시 (App.vue의 전역 함수 호출)
  setTimeout(() => {
    if (window.showDoorOpenModal) {
      window.showDoorOpenModal()
    }
  }, 100)
}

// 키보드 이벤트 리스너
function handleKeyDown(event) {
  // Ctrl + 1: 위치 이동/복귀 토글
  if (event.ctrlKey && event.key === '1') {
    event.preventDefault()
    handleSimulationToggle()
  }
  // Ctrl + 2: 연결 끊김/복구 토글
  if (event.ctrlKey && event.key === '2') {
    event.preventDefault()
    handleDisconnectToggle()
  }
  // Ctrl + 3: 현관문 열림 감지 알림
  if (event.ctrlKey && event.key === '3') {
    event.preventDefault()
    handleDoorOpenAlert()
  }
}

// 키보드 이벤트 등록/해제는 기존 onMounted에 추가됨

</script>

<style scoped>
/* ===== 전체 프레임: 화면 가득 채우기 ===== */
.cg-wrap {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* ===== 스타일 ===== */

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

/* ===== 기타 스타일 ===== */

/* 일정 관련 스타일은 ScheduleList 컴포넌트로 이동 */

/* 지도 컨트롤 관련 스타일은 MapControls 컴포넌트로 이동 */

 /* 상태 변경 버튼 */
 .missing-report-btn {
   display: flex;
   align-items: center;
   gap: 6px;
   padding: 8px 12px;
   background: linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%);
   border: 1px solid #CBD5E1;
   border-radius: 8px;
   color: #475569;
   font-size: 0.75rem;
   font-weight: 600;
   cursor: pointer;
   transition: all 0.2s ease;
   box-shadow: 0 1px 3px rgba(71, 85, 105, 0.1);
 }

 .missing-report-btn:hover {
   background: linear-gradient(135deg, #F1F5F9 0%, #E2E8F0 100%);
   border-color: #94A3B8;
   transform: translateY(-1px);
   box-shadow: 0 2px 6px rgba(71, 85, 105, 0.15);
 }

 .missing-report-btn:active {
   transform: translateY(0);
   box-shadow: 0 1px 3px rgba(71, 85, 105, 0.1);
 }

 .missing-report-btn i {
   font-size: 14px;
   color: #475569;
 }

 .missing-report-btn span {
   white-space: nowrap;
   font-weight: 600;
 }

 /* 아바타 클릭 가능 스타일 */
 .avatar-clickable {
   cursor: pointer;
   transition: all 0.2s ease;
 }

 .avatar-clickable:hover {
   transform: scale(1.05);
   box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
 }

 .avatar-clickable:active {
   transform: scale(0.98);
 }
</style>


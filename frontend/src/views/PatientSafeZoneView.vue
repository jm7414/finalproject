<!-- 환자용 안심존 모달 -->
<template>
  <div class="safe-zone-modal-backdrop" @click.self="closeModal">
    <div class="safe-zone-modal" role="dialog" aria-modal="true">
      <button type="button" class="safe-zone-close" @click="closeModal" aria-label="닫기">
        ×
      </button>

      <div class="safe-zone-content">
        <!-- 지도 영역 -->
        <div class="map-wrapper">
          <div ref="mapEl" class="map-container"></div>

          <!-- 상단: 안심존 상태 카드 (지도 위) -->
          <div class="status-card-overlay" :style="{ backgroundColor: safeZoneStatus.bgColor }">
            <div class="status-icon">
              <span v-if="safeZoneStatus.isInside" class="icon-check">✓</span>
              <span v-else-if="safeZoneStatus.status === '위치 확인 중'" class="icon-loading">⟳</span>
              <span v-else class="icon-warning">⚠</span>
            </div>
            <div class="status-text-group">
              <div class="status-text" :style="{ color: safeZoneStatus.color }">
                {{ safeZoneStatus.status }}
              </div>
              <div class="status-subtitle" v-if="safeZoneStatus.subtitle">
                {{ safeZoneStatus.subtitle }}
              </div>
            </div>
          </div>
        </div>

        <!-- 하단: 현재 일정 정보 -->
        <div class="schedule-section">
          <div class="schedule-card" v-if="currentSchedule">
            <div class="schedule-label">현재 일정</div>
            <div class="schedule-title">{{ currentSchedule.scheduleTitle }}</div>
            <div class="schedule-time">
              {{ formatTime(currentSchedule.startTime) }} ~ {{ formatTime(currentSchedule.endTime) }}
            </div>
          </div>
          <div class="schedule-card no-schedule" v-else>
            <div class="schedule-label">현재 일정</div>
            <div class="schedule-title">진행중인 일정이 없습니다</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useKakaoMap } from '@/composables/useKakaoMap'

const emit = defineEmits(['close'])

// 사용자 정보
const userNo = ref(null)
const userName = ref('')

// 현재 위치
const currentLocation = ref(null)
let locationUpdateInterval = null

// 안심존 관련
let currentSafeZone = null // 현재 표시 중인 안심존 폴리곤/원형
const currentActiveZone = ref(null) // 현재 활성화된 안심존 정보 { type, data, scheduleNo?, level }

// 안심존 상태
const safeZoneStatus = ref({
  isInside: false,
  status: '위치 확인 중',
  subtitle: '',
  color: '#F59E0B',
  bgColor: '#FEF3C7'
})

// 일정 관련
const allSchedules = ref([])
const currentSchedule = computed(() => {
  if (!allSchedules.value.length) return null
  
  const now = new Date()
  const today = now.toISOString().split('T')[0]
  const nowMin = now.getHours() * 60 + now.getMinutes()
  
  const todaySchedules = allSchedules.value
    .filter(s => s.scheduleDate === today)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
  
  for (const s of todaySchedules) {
    const st = hmToMinutes(s.startTime)
    const et = hmToMinutes(s.endTime)
    if (nowMin >= st && nowMin <= et) {
      return s
    }
  }
  return null
})

// 카카오 지도
const {
  mapEl,
  mapInstance,
  err,
  initMap,
  setCenter,
  setBounds
} = useKakaoMap({
  kakaoKey: import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891',
  center: { lat: 37.4943524920695, lng: 126.88767655688868 },
  defaultLevel: 3,
  enableControls: false
})

// 지도 드래그 비활성화
function disableMapDragging() {
  if (mapInstance.value && window.kakao?.maps) {
    mapInstance.value.setDraggable(false)
    mapInstance.value.setZoomable(false)
  }
}

// 현재 위치 마커
let currentLocationMarker = null

function createCurrentLocationMarkerImage() {
  if (!window.kakao?.maps) return null
  return new window.kakao.maps.MarkerImage(
    'data:image/svg+xml;base64,' + btoa(`
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <circle cx="16" cy="16" r="14" fill="#EF4444" stroke="white" stroke-width="2"/>
        <circle cx="16" cy="16" r="5" fill="white"/>
      </svg>
    `),
    new window.kakao.maps.Size(32, 32),
    { offset: new window.kakao.maps.Point(16, 16) }
  )
}

/* ===== API 호출 ===== */

// 사용자 정보 가져오기
async function fetchMe() {
  try {
    const response = await fetch('/api/user/me', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('사용자 정보를 가져올 수 없습니다.')
    }
    
    const user = await response.json()
    userNo.value = user.userNo
    userName.value = user.name || '사용자'
    return user
  } catch (error) {
    console.error('사용자 정보 조회 오류:', error)
    return null
  }
}

// 일정 목록 가져오기
async function fetchSchedules(userNo) {
  try {
    const response = await fetch(`/api/schedule/list/${encodeURIComponent(userNo)}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('일정 정보를 가져올 수 없습니다.')
    }
    
    const schedules = await response.json()
    allSchedules.value = Array.isArray(schedules) ? schedules : []
  } catch (error) {
    console.error('일정 조회 오류:', error)
    allSchedules.value = []
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
      return null
    }
    
    const result = await response.json()
    
    if (result.message) {
      return null
    }
    
    return result.boundaryCoordinates ? JSON.parse(result.boundaryCoordinates) : null
  } catch (error) {
    console.error('기본 안심존 조회 오류:', error)
    return null
  }
}

// 경로형 안심존 가져오기
async function fetchScheduleSafeZone(scheduleNo) {
  try {
    const response = await fetch(`/api/schedule/${scheduleNo}/route`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      return null
    }
    
    const route = await response.json()
    if (!route.bufferCoordinates) return null
    
    const bufferCoordinates = JSON.parse(route.bufferCoordinates)
    
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

/* ===== 위치 관리 ===== */

// 현재 위치 가져오기 (브라우저 geolocation)
function getCurrentLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('이 브라우저는 위치 서비스를 지원하지 않습니다.'))
      return
    }
    
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const location = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          timestamp: Date.now()
        }
        resolve(location)
      },
      (error) => {
        reject(error)
      },
      {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0
      }
    )
  })
}

// 위치 업데이트 및 안심존 상태 확인
async function updateLocationAndStatus() {
  try {
    // 현재 위치 가져오기
    const location = await getCurrentLocation()
    currentLocation.value = location
    
    // 지도에 현재 위치 마커 표시
    updateCurrentLocationMarker(location)
    
    // 안심존 상태 확인
    checkSafeZoneStatus()
    
    // 서버에 위치 전송 (선택사항)
    if (userNo.value) {
      try {
        await fetch('/api/location/update', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({
            userNo: userNo.value,
            latitude: location.latitude,
            longitude: location.longitude,
            timestamp: location.timestamp
          })
        })
      } catch (error) {
        console.warn('위치 전송 실패:', error)
      }
    }
  } catch (error) {
    console.error('위치 업데이트 오류:', error)
    safeZoneStatus.value = {
      isInside: false,
      status: '위치 확인 실패',
      subtitle: '위치 권한을 허용해주세요',
      color: '#EF4444',
      bgColor: '#FEE2E2'
    }
  }
}

// 위치 추적 시작 (2초마다 - 시연용 최적화)
function startLocationTracking() {
  updateLocationAndStatus()
  locationUpdateInterval = setInterval(() => {
    updateLocationAndStatus()
  }, 2000) // 2초
}

// 위치 추적 중지
function stopLocationTracking() {
  if (locationUpdateInterval) {
    clearInterval(locationUpdateInterval)
    locationUpdateInterval = null
  }
}

/* ===== 안심존 관리 ===== */

// 안심존 업데이트 (현재 일정에 따라)
async function updateSafeZone(map) {
  if (!map || !userNo.value) return
  
  try {
    // 1. 현재 진행 중인 일정 찾기
    const schedule = currentSchedule.value
    
    if (schedule) {
      // 2. 진행 중인 일정이 있으면 해당 일정의 안심존 표시
      const bufferCoordinates = await fetchScheduleSafeZone(schedule.scheduleNo)
      
      if (bufferCoordinates && (
        (Array.isArray(bufferCoordinates) && bufferCoordinates.length > 0) ||
        (typeof bufferCoordinates === 'object' && bufferCoordinates.coordinates && bufferCoordinates.coordinates.length > 0)
      )) {
        currentActiveZone.value = {
          type: '경로형',
          data: bufferCoordinates,
          scheduleNo: schedule.scheduleNo
        }
        drawScheduleSafeZone(map, bufferCoordinates)
        return
      }
    }
    
    // 3. 진행 중인 일정이 없거나 안심존이 없으면 기본 안심존 표시
    const basicSafeZone = await fetchBasicSafeZone(userNo.value)
    
    if (basicSafeZone) {
      currentActiveZone.value = {
        type: '기본형',
        data: basicSafeZone
      }
      drawBasicSafeZone(map, basicSafeZone)
    } else {
      currentActiveZone.value = null
    }
  } catch (error) {
    console.error('안심존 업데이트 오류:', error)
    currentActiveZone.value = null
  }
}

// 경로형 안심존 그리기
function drawScheduleSafeZone(map, bufferCoordinates) {
  if (!map || !bufferCoordinates) return
  
  try {
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
    }
    
    let coordinates
    if (Array.isArray(bufferCoordinates)) {
      coordinates = bufferCoordinates
    } else if (bufferCoordinates.coordinates) {
      coordinates = bufferCoordinates.coordinates
    } else {
      return
    }
    
    const kakaoPath = coordinates.map(coord => 
      new window.kakao.maps.LatLng(coord.latitude, coord.longitude)
    )
    
    currentSafeZone = new window.kakao.maps.Polygon({
      path: kakaoPath,
      strokeWeight: 3,
      strokeColor: '#6366f1',
      strokeOpacity: 0.8,
      fillColor: '#6366f1',
      fillOpacity: 0.2
    })
    
    currentSafeZone.setMap(map)
    
    // 안심존이 보이도록 지도 범위 조정
    const bounds = new window.kakao.maps.LatLngBounds()
    kakaoPath.forEach(latLng => bounds.extend(latLng))
    if (currentLocation.value) {
      bounds.extend(new window.kakao.maps.LatLng(
        currentLocation.value.latitude,
        currentLocation.value.longitude
      ))
    }
    map.setBounds(bounds)
  } catch (error) {
    console.error('경로형 안심존 표시 오류:', error)
  }
}

// 기본형 안심존 그리기
function drawBasicSafeZone(map, boundaryData) {
  if (!map || !boundaryData) return
  
  try {
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
    }
    
    if (boundaryData.type === 'Circle') {
      const center = new window.kakao.maps.LatLng(boundaryData.center.lat, boundaryData.center.lng)
      const radius = boundaryData.radius
      // 시연용: 30m만 임시로 5m로 적용 (60m, 100m는 그대로)
      const effectiveRadius = radius === 30 ? 5 : radius
      
      // 원형 폴리곤 생성
      const circlePoints = []
      const steps = 64
      const earthRadius = 6371000
      
      for (let i = 0; i < steps; i++) {
        const angle = (Math.PI * 2 * i) / steps
        const dx = effectiveRadius * Math.cos(angle)
        const dy = effectiveRadius * Math.sin(angle)
        
        const lat = boundaryData.center.lat + (dy / earthRadius) * (180 / Math.PI)
        const lng = boundaryData.center.lng + (dx / earthRadius) * (180 / Math.PI) / Math.cos(boundaryData.center.lat * Math.PI / 180)
        
        circlePoints.push(new window.kakao.maps.LatLng(lat, lng))
      }
      
      currentSafeZone = new window.kakao.maps.Polygon({
        path: circlePoints,
        strokeWeight: 3,
        strokeColor: '#6366f1',
        strokeOpacity: 0.8,
        fillColor: '#6366f1',
        fillOpacity: 0.2
      })
      
      currentSafeZone.setMap(map)
      
      // 지도 범위 조정
      const bounds = new window.kakao.maps.LatLngBounds()
      circlePoints.forEach(point => bounds.extend(point))
      if (currentLocation.value) {
        bounds.extend(new window.kakao.maps.LatLng(
          currentLocation.value.latitude,
          currentLocation.value.longitude
        ))
      }
      map.setBounds(bounds)
    }
  } catch (error) {
    console.error('기본형 안심존 표시 오류:', error)
  }
}

// 현재 위치 마커 업데이트
function updateCurrentLocationMarker(location) {
  if (!mapInstance.value || !window.kakao?.maps || !location) return
  
  try {
    // 기존 마커 제거
    if (currentLocationMarker) {
      currentLocationMarker.setMap(null)
    }
    
    // 새 마커 생성
    const position = new window.kakao.maps.LatLng(location.latitude, location.longitude)
    const markerImage = createCurrentLocationMarkerImage()
    
    currentLocationMarker = new window.kakao.maps.Marker({
      position: position,
      image: markerImage || undefined,
      map: mapInstance.value
    })
    
    // 지도 중심을 현재 위치로 이동
    mapInstance.value.setCenter(position)
  } catch (error) {
    console.error('현재 위치 마커 업데이트 오류:', error)
  }
}

/* ===== 안심존 상태 확인 ===== */

// 안심존 상태 확인
function checkSafeZoneStatus() {
  if (!userNo.value) {
    safeZoneStatus.value = {
      isInside: false,
      status: '연결 필요',
      subtitle: '',
      color: '#9CA3AF',
      bgColor: '#F3F4F6'
    }
    return
  }
  
  if (!currentLocation.value || !currentActiveZone.value) {
    safeZoneStatus.value = {
      isInside: false,
      status: '위치 확인 중',
      subtitle: '',
      color: '#F59E0B',
      bgColor: '#FEF3C7'
    }
    return
  }
  
  try {
    const lat = currentLocation.value.latitude
    const lng = currentLocation.value.longitude
    
    let isInside = false
    
    if (currentActiveZone.value.type === '기본형') {
      const boundaryData = currentActiveZone.value.data
      const centerLat = boundaryData.center.lat
      const centerLng = boundaryData.center.lng
      const radius = boundaryData.radius
      // 시연용: 30m만 임시로 5m로 적용 (60m, 100m는 그대로)
      const effectiveRadius = radius === 30 ? 5 : radius
      
      const distance = calculateDistance(lat, lng, centerLat, centerLng)
      isInside = distance <= effectiveRadius
      
    } else if (currentActiveZone.value.type === '경로형') {
      const bufferCoordinates = currentActiveZone.value.data
      
      let coordinates
      if (Array.isArray(bufferCoordinates)) {
        coordinates = bufferCoordinates
      } else if (bufferCoordinates.coordinates) {
        coordinates = bufferCoordinates.coordinates
      } else {
        isInside = true
      }
      
      if (coordinates) {
        isInside = isPointInPolygon(lat, lng, coordinates)
      }
    }
    
    // 안심존 상태 업데이트
    if (isInside) {
      safeZoneStatus.value = {
        isInside: true,
        status: '안전합니다',
        subtitle: '안심존 안에 있습니다',
        color: '#16A34A',
        bgColor: '#DCFCE7'
      }
    } else {
      safeZoneStatus.value = {
        isInside: false,
        status: '안심존 이탈',
        subtitle: '안심존으로 돌아가주세요',
        color: '#EF4444',
        bgColor: '#FEE2E2'
      }
    }
  } catch (error) {
    console.error('안심존 상태 확인 오류:', error)
    safeZoneStatus.value = {
      isInside: false,
      status: '위치 확인 중',
      subtitle: '',
      color: '#F59E0B',
      bgColor: '#FEF3C7'
    }
  }
}

// 두 점 간의 거리 계산 (Haversine 공식)
function calculateDistance(lat1, lng1, lat2, lng2) {
  const R = 6371000
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
    
    if (((yi > lat) !== (yj > lat)) && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)) {
      inside = !inside
    }
  }
  return inside
}

/* ===== 유틸리티 ===== */

// 시간 포맷 (HH:mm)
function formatTime(hm) {
  return String(hm).slice(0, 5)
}

// 시간을 분으로 변환
function hmToMinutes(hm) {
  const [h, m] = String(hm).split(':').map(n => parseInt(n, 10))
  return (h || 0) * 60 + (m || 0)
}

function closeModal() {
  emit('close')
}

/* ===== 초기화 ===== */

onMounted(async () => {
  try {
    // 사용자 정보 가져오기
    await fetchMe()
    
    if (!userNo.value) {
      alert('로그인이 필요합니다.')
      closeModal()
      return
    }
    
    // 일정 목록 가져오기
    await fetchSchedules(userNo.value)
    
    // 지도 초기화
    await initMap()
    
    // 지도 드래그 비활성화
    disableMapDragging()
    
    // 안심존 표시
    await updateSafeZone(mapInstance.value)
    
    // 위치 추적 시작
    startLocationTracking()
  } catch (error) {
    console.error('초기화 오류:', error)
  }
})

onBeforeUnmount(() => {
  stopLocationTracking()
  
  if (currentSafeZone) {
    currentSafeZone.setMap(null)
  }
  
  if (currentLocationMarker) {
    currentLocationMarker.setMap(null)
  }
})
</script>

<style scoped>
.safe-zone-modal-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 200;
}

.safe-zone-modal {
  position: relative;
  width: clamp(320px, 90%, 420px);
  max-height: min(620px, 85%);
  background: #ffffff;
  border-radius: 28px;
  box-shadow: 0 25px 60px rgba(15, 23, 42, 0.25);
  overflow: hidden;
}

.safe-zone-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.05);
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  cursor: pointer;
  z-index: 5;
  transition: background 0.2s;
}

.safe-zone-close:hover {
  background: rgba(15, 23, 42, 0.12);
}

.safe-zone-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 18px 18px;
}

/* ===== 지도 영역 ===== */
.map-wrapper {
  position: relative;
  height: min(360px, 55vh);
  border-radius: 20px;
  overflow: hidden;
  background: #e5e7eb;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.05);
}

.map-container {
  width: 100%;
  height: 100%;
}

.status-card-overlay {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.2);
  backdrop-filter: blur(8px);
  z-index: 10;
  min-width: 240px;
  max-width: 90%;
}

.status-icon {
  font-size: 26px;
  font-weight: bold;
  line-height: 1;
}

.icon-check { color: #16a34a; }
.icon-warning { color: #ef4444; }
.icon-loading {
  color: #f59e0b;
  animation: spin 2s linear infinite;
}

.status-text-group {
  flex: 1;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-text {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}

.status-subtitle {
  font-size: 13px;
  color: #4b5563;
  font-weight: 500;
}

/* ===== 일정 영역 ===== */
.schedule-section {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.schedule-card {
  width: 100%;
  background: #f9fafb;
  padding: 18px 22px;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}

.schedule-card.no-schedule {
  text-align: center;
}

.schedule-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 600;
  margin-bottom: 8px;
}

.schedule-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
  line-height: 1.3;
}

.schedule-time {
  font-size: 16px;
  color: #4b5563;
  font-weight: 500;
}

/* 반응형 */
@media (max-width: 480px) {
  .safe-zone-modal {
    width: 92vw;
    max-height: 82vh;
  }

  .safe-zone-content {
    padding: 20px;
    gap: 12px;
  }

  .status-card-overlay {
    padding: 10px 14px;
    min-width: 180px;
  }

  .status-icon {
    font-size: 22px;
  }

  .status-text {
    font-size: 16px;
  }

  .schedule-card {
    padding: 16px;
  }
}
</style>


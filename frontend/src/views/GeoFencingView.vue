<template>
  <div class="page">
    <div ref="mapContainer" class="map-box"></div>
    
    <!-- 안전존 범위 설정 UI -->
    <div class="controls">
      <div class="control-header">
        <h3>안전존 범위 설정</h3>
        <p>범위를 선택하면 지도에 안전존이 표시됩니다</p>
      </div>
      
      <div class="control-row">
        <div class="level-options">
          <button 
            v-for="level in bufferLevels" 
            :key="level.value"
            class="level-btn"
            :class="{ active: bufferLevel === level.value }"
            @click="selectBufferLevel(level.value)"
          >
            <div class="level-name">{{ level.name }}</div>
            <div class="level-desc">{{ level.desc }}</div>
          </button>
        </div>
      </div>
      
      <div class="action-row">
        <button class="set-btn" @click="saveSettings">설정</button>
        <button class="cancel-btn" @click="cancelSettings">취소</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { lineString, buffer, simplify } from '@turf/turf'

const router = useRouter()
const mapContainer = ref(null)
const bufferLevel = ref('1')
let mapInstance = null
let currentPolyline = null
let currentBuffer = null
let currentSafetyZone = null
let startMarker = null
let endMarker = null

const bufferLevels = [
  { value: '1', name: '1단계', desc: '30m 범위' },
  { value: '2', name: '2단계', desc: '60m 범위' },
  { value: '3', name: '3단계', desc: '100m 범위' }
]

/* 카카오 지도 */
const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
onMounted(() => {
  loadKakaoMap(mapContainer.value)
})
function loadKakaoMap(container) {
  const script = document.createElement('script')
  // service, cluster, drawing 라이브러리 활성화
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services,clusterer,drawing&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      const options = {
        center: new window.kakao.maps.LatLng(37.495488, 126.887642),
        level: 4,
        maxLevel: 14,
      }
      mapInstance = new window.kakao.maps.Map(container, options)

      // 백엔드(Tmap)로부터 좌표를 받아 폴리라인 그리기
      // - 지도 로드가 끝나면 자동으로 요청을 보냄
      fetchRouteAndDraw(mapInstance)
    })
  }
}

// 안전존 범위 변경 감지
watch(bufferLevel, () => {
  if (mapInstance && currentPolyline) {
    updateSafetyZone()
  }
})

/**
 * 안전존 범위에 따른 폴리곤 생성 및 그리기 (Turf.js 사용)
 */
function updateSafetyZone() {
  if (!mapInstance || !currentPolyline) return

  // 기존 안전존 제거
  if (currentSafetyZone) {
    currentSafetyZone.setMap(null)
  }

  // 폴리라인 경로를 좌표 배열로 변환
  const path = currentPolyline.getPath()
  if (path.length < 2) return

  const coords = path.map(latLng => ({
    latitude: latLng.getLat(),
    longitude: latLng.getLng()
  }))

  // Turf.js를 사용한 정교한 버퍼 생성
  try {
    // 1) 좌표를 Turf 형식으로 변환 [lng, lat]
    const turfCoords = coords.map(c => [c.longitude, c.latitude])
    
    // 2) LineString 생성
    const line = lineString(turfCoords)
    
    // 3) 단계별 버퍼 크기 설정 (미터 단위)
    const bufferSizes = {
      '1': 30,   // 30m
      '2': 60,   // 60m  
      '3': 100   // 100m
    }
    
    const bufferSize = bufferSizes[bufferLevel.value] || 30
    
    // 4) 버퍼 생성
    const buffered = buffer(line, bufferSize, { units: 'meters' })
    
    // 5) GeoJSON Polygon을 카카오맵 폴리곤으로 변환
    const coordinates = buffered.geometry.coordinates[0] // 외곽 링
    const kakaoPath = coordinates.map(coord => 
      new window.kakao.maps.LatLng(coord[1], coord[0]) // [lng, lat] -> (lat, lng)
    )
    
    // 6) 카카오맵 폴리곤 생성
    currentSafetyZone = new window.kakao.maps.Polygon({
      path: kakaoPath,
      strokeWeight: 2,
      strokeColor: '#FF3B30',
      strokeOpacity: 0.8,
      fillColor: '#FF3B30',
      fillOpacity: 0.2
    })
    
    // 7) 지도에 표시
    currentSafetyZone.setMap(mapInstance)
    
    // 8) 전체 영역이 보이도록 지도 범위 조정
    const bounds = new window.kakao.maps.LatLngBounds()
    kakaoPath.forEach(latLng => bounds.extend(latLng))
    mapInstance.setBounds(bounds)
    
    console.log(`안전존 생성 완료: ${bufferSize}m (${bufferLevel.value}단계)`)
    
  } catch (error) {
    console.error('안전존 생성 중 오류:', error)
  }
}

/**
 * Kakao 지도에 좌표 배열로 Polyline(경로)을 그려주는 함수
 * - coords 형식: [{ latitude: number, longitude: number }, ...] 이거라서
 * - 보통 백엔드(Tmap) 응답의 coordinates 배열을 그대로 사용하면 될 거 같아욤
 */
function drawRouteOnKakaoMap(map, coords) {
  // 유효한 좌표 배열인지 확인
  if (!Array.isArray(coords) || coords.length < 2) {
    console.warn('경로를 그리려면 2개 이상의 좌표가 필요합니다.')
    return
  }

  // Kakao LatLng 배열로 변환 (new kakao.maps.LatLng(위도, 경도))
  const path = coords.map((c) => new kakao.maps.LatLng(c.latitude, c.longitude))

  // 기존 경로/버퍼/마커 제거
  if (currentPolyline) currentPolyline.setMap(null)
  if (currentBuffer) currentBuffer.setMap(null)
  if (startMarker) startMarker.setMap(null)
  if (endMarker) endMarker.setMap(null)

  // Polyline 생성
  currentPolyline = new kakao.maps.Polyline({
    path,
    strokeWeight: 5,
    strokeColor: '#8B5CF6', // 보라색으로 변경
    strokeOpacity: 0.9,
    strokeStyle: 'solid',
  })

  // 기존 폴리라인 제거
  if (currentPolyline) {
    currentPolyline.setMap(null)
  }
  
  // 지도에 Polyline 표시
  currentPolyline.setMap(map)

  // 시작점과 끝점에 마커 추가
  const startPoint = coords[0]
  const endPoint = coords[coords.length - 1]

  startMarker = new kakao.maps.Marker({
    position: new kakao.maps.LatLng(startPoint.latitude, startPoint.longitude),
    map: map,
    image: new kakao.maps.MarkerImage(
      'data:image/svg+xml;base64,' + btoa(`
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#EF4444"/>
          <circle cx="12" cy="9" r="3" fill="white"/>
        </svg>
      `),
      new kakao.maps.Size(40, 40),
      { offset: new kakao.maps.Point(12, 12) }
    )
  })

  endMarker = new kakao.maps.Marker({
    position: new kakao.maps.LatLng(endPoint.latitude, endPoint.longitude),
    map: map,
    image: new kakao.maps.MarkerImage(
      'data:image/svg+xml;base64,' + btoa(`
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#EF4444"/>
          <circle cx="12" cy="9" r="3" fill="white"/>
        </svg>
      `),
      new kakao.maps.Size(40, 40),
      { offset: new kakao.maps.Point(12, 12) }
    )
  })

  // 버퍼 생성
  createBuffer(map, coords)

  // 경로 전체가 한 화면에 보이도록 지도 범위 맞추기
  const bounds = new window.kakao.maps.LatLngBounds()
  path.forEach((latLng) => bounds.extend(latLng))
  map.setBounds(bounds)
  
  // 안전존 폴리곤 생성
  updateSafetyZone()
}

/**
 * SearchRouteView에서 전달된 coordinates로 지도에 경로를 그립니다.
 * - 전달 데이터가 없으면 아무 작업도 하지 않고 경고 출력
 */
async function fetchRouteAndDraw(map) {
  try {
    // SearchRouteView에서 전달된 좌표 사용
    const stored = sessionStorage.getItem('routeCoordinates')
    if (stored) {
      const coords = JSON.parse(stored)
      if (Array.isArray(coords) && coords.length >= 2) {
        drawRouteOnKakaoMap(map, coords)
        return
      }
    }
    console.warn('routeCoordinates가 없어 경로를 표시하지 않습니다.')
  } catch (err) {
    console.error('경로 요청 중 오류:', err)
  }
}

/**
 * Turf.js를 사용해 경로 주변에 버퍼를 생성하고 카카오맵에 폴리곤으로 그립니다.
 */
function createBuffer(map, coords) {
  try {
    // 1) 좌표를 Turf 형식으로 변환 [lng, lat]
    const turfCoords = coords.map(c => [c.longitude, c.latitude])
    
    // 2) LineString 생성
    const line = lineString(turfCoords)
    
    // 3) 단계별 버퍼 크기 설정 (미터 단위)
    const bufferSizes = {
      '1': 30,   // 30m
      '2': 60,   // 60m  
      '3': 100   // 100m
    }
    
    const bufferSize = bufferSizes[bufferLevel.value] || 30
    
    // 4) 버퍼 생성
    const buffered = buffer(line, bufferSize, { units: 'meters' })
    
    // 5) 기존 버퍼 제거
    if (currentBuffer) {
      currentBuffer.setMap(null)
    }
    
    // 6) GeoJSON Polygon을 카카오맵 폴리곤으로 변환
    const coordinates = buffered.geometry.coordinates[0] // 외곽 링
    const kakaoPath = coordinates.map(coord => 
      new kakao.maps.LatLng(coord[1], coord[0]) // [lng, lat] -> (lat, lng)
    )
    
    // 7) 카카오맵 폴리곤 생성
    currentBuffer = new kakao.maps.Polygon({
      path: kakaoPath,
      strokeWeight: 2,
      strokeColor: '#EF4444',
      strokeOpacity: 0.8,
      fillColor: '#EF4444',
      fillOpacity: 0.4
    })
    
    // 8) 지도에 표시
    currentBuffer.setMap(map)
    
    // 9) 버퍼 좌표를 저장 (나중에 위치 판정용)
    sessionStorage.setItem('routeBufferPolygon', JSON.stringify(coordinates))
    
    console.log(`버퍼 생성 완료: ${bufferSize}m (${bufferLevel.value}단계)`)
    
  } catch (error) {
    console.error('버퍼 생성 중 오류:', error)
  }
}

/**
 * 버퍼 레벨 선택 시 호출
 */
function selectBufferLevel(level) {
  bufferLevel.value = level
  updateBuffer()
}

/**
 * 버퍼 레벨 변경 시 호출
 */
function updateBuffer() {
  const stored = sessionStorage.getItem('routeCoordinates')
  if (stored && mapInstance) {
    const coords = JSON.parse(stored)
    if (Array.isArray(coords) && coords.length >= 2) {
      createBuffer(mapInstance, coords)
    }
  }
}

/**
 * 설정 저장 - AddSchedule.vue로 돌아가기
 */
function saveSettings() {
  console.log('설정 저장:', { bufferLevel: bufferLevel.value })
  
  // 버퍼 좌표를 sessionStorage에서 가져와서 형식 변환
  const bufferCoordsRaw = sessionStorage.getItem('routeBufferPolygon')
  if (bufferCoordsRaw) {
    try {
      // Turf.js 형식: [[lng, lat], ...] → { latitude, longitude } 형식으로 변환
      const bufferArray = JSON.parse(bufferCoordsRaw)
      const convertedBuffer = bufferArray.map(coord => ({
        latitude: coord[1],   // 배열의 두 번째 값이 latitude
        longitude: coord[0]   // 배열의 첫 번째 값이 longitude
      }))
      
      // level 정보를 포함한 bufferCoordinates 생성
      const bufferCoordinates = {
        level: parseInt(bufferLevel.value), // 선택된 단계 (1, 2, 3)
        coordinates: convertedBuffer
      }
      
      sessionStorage.setItem('bufferCoordinates', JSON.stringify(bufferCoordinates))
    } catch (error) {
      console.error('버퍼 좌표 변환 중 오류:', error)
    }
  }
  
  // 일정 진행 상태 유지 (AddSchedule.vue에서 데이터 복원을 위해)
  // isScheduleInProgress 플래그는 유지됨
  
  // AddSchedule.vue로 돌아가기
  router.push({ name: 'add-schedule' })
}

/**
 * 설정 취소 - SearchRouteView로 돌아가기
 */
function cancelSettings() {
  console.log('설정 취소')
  
  // sessionStorage 정리 (경로 관련 데이터만 삭제)
  sessionStorage.removeItem('routeCoordinates')
  sessionStorage.removeItem('routeBufferPolygon')
  sessionStorage.removeItem('bufferCoordinates')
  sessionStorage.removeItem('scheduleLocations')
  
  // 일정 진행 상태는 유지 (SearchRouteView에서 다시 설정할 수 있도록)
  
  // SearchRouteView로 돌아가기
  router.push({ name: 'search-route' })
}
</script>

<style scoped>
.page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1;
}

.map-box {
  width: 100%;
  flex: 1;
  min-height: 300px;
  max-height: 55vh; /* 지도 최대 높이를 화면의 50%로 제한 */
}

/* 컨트롤 패널 */
.controls {
  flex-shrink: 0;
  background: #ffffff;
  border-top: 1px solid #e5e7eb;
  padding: 24px 20px;
  overflow-y: auto;
  margin-bottom: 60px; /* 하단 네비게이션 여유 공간 */
}

.control-header {
  margin-bottom: 20px;
}

.control-header h3 {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
}

.control-header p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.control-row {
  margin-bottom: 20px;
}

/* 범위 선택 버튼 */
.level-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.level-btn {
  padding: 16px 12px;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.level-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.level-btn.active {
  background: #eef2ff;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.level-name {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.level-btn.active .level-name {
  color: #6366f1;
}

.level-desc {
  font-size: 13px;
  color: #6b7280;
}

.level-btn.active .level-desc {
  color: #4f46e5;
}

/* 액션 버튼 */
.action-row {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.set-btn, .cancel-btn {
  flex: 1;
  padding: 14px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.set-btn {
  background: #6366f1;
  color: #ffffff;
  border: none;
}

.set-btn:hover {
  background: #4f46e5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.cancel-btn {
  background: #ffffff;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.cancel-btn:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

/* 반응형 */
@media (max-width: 480px) {
  .map-box {
    max-height: 40vh; /* 모바일에서는 지도 높이를 더 줄임 */
  }

  .controls {
    padding: 20px 16px;
    margin-bottom: 60px;
  }

  .level-options {
    grid-template-columns: 1fr;
  }

  .level-btn {
    padding: 14px 12px;
  }

  .control-header h3 {
    font-size: 18px;
  }

  .control-header p {
    font-size: 13px;
  }
}
</style>


<template>
  <div class="page">
    <div ref="mapContainer" class="map-box"></div>
    
    <!-- 안전존 범위 설정 UI -->
    <div class="controls">
      <div class="control-row">
        <span class="label">안전존 범위</span>
        <select v-model="bufferLevel" @change="updateBuffer" class="level-select">
          <option value="1">1단계</option>
          <option value="2">2단계</option>
          <option value="3">3단계</option>
        </select>
        <button class="confirm-btn" @click="updateBuffer">확인</button>
      </div>
      <div class="action-row">
        <button class="set-btn" @click="saveSettings">설정</button>
        <button class="cancel-btn" @click="cancelSettings">취소</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { lineString, buffer, simplify } from '@turf/turf'

const mapContainer = ref(null)
const bufferLevel = ref('1')
let mapInstance = null
let currentPolyline = null
let currentBuffer = null
let startMarker = null
let endMarker = null

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
  const bounds = new kakao.maps.LatLngBounds()
  path.forEach((latLng) => bounds.extend(latLng))
  map.setBounds(bounds)
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
 * 설정 저장
 */
function saveSettings() {
  console.log('설정 저장:', { bufferLevel: bufferLevel.value })
  // 여기에 실제 설정 저장 로직 추가
}

/**
 * 설정 취소
 */
function cancelSettings() {
  console.log('설정 취소')
  // 여기에 취소 로직 추가
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.map-box {
  width: 100%;
  height: calc(100vh - 180px); /* 컨트롤 영역 높이만큼 제외 */
}

.controls {
  background: #fff;
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  height: 180px; /* 고정 높이 설정 */
  flex-shrink: 0; /* 축소 방지 */
}

.control-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.label {
  font-size: 16px;
  font-weight: 500;
  color: #374151;
  min-width: 100px;
}

.level-select {
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  font-size: 16px;
  color: #374151;
  cursor: pointer;
  min-width: 120px;
  height: 48px;
}

.confirm-btn {
  padding: 12px 20px;
  background: #6366f1;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  height: 48px;
  transition: background-color 0.2s;
}

.confirm-btn:hover {
  background: #4f46e5;
}

.action-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.set-btn {
  width: 100%;
  padding: 12px 20px;
  background: #6366f1;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  height: 48px;
  transition: background-color 0.2s;
}

.set-btn:hover {
  background: #4f46e5;
}

.cancel-btn {
  width: 100%;
  padding: 12px 20px;
  background: #fff;
  color: #6B7280;
  border: 1px solid #6366f1;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 500;
  cursor: pointer;
  height: 48px;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #f9fafb;
  border-color: #4f46e5;
}
</style>


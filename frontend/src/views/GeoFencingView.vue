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

// 안전존 범위 변경 감지
watch(selectedRange, () => {
  if (mapInstance && currentPolyline) {
    updateSafetyZone()
  }
})

/**
 * 안전존 범위에 따른 폴리곤 생성 및 그리기
 */
function updateSafetyZone() {
  if (!mapInstance || !currentPolyline) return

  // 기존 안전존 제거
  if (currentSafetyZone) {
    currentSafetyZone.setMap(null)
  }

  // 안전존 범위 설정 (미터 단위)
  const rangeSettings = {
    '1': 30,   // 30m
    '2': 60,   // 60m  
    '3': 100   // 100m
  }

  const bufferDistance = rangeSettings[selectedRange.value] || 30
  
  // 폴리라인 경로 가져오기
  const path = currentPolyline.getPath()
  if (path.length < 2) return

  // 경로를 따라 버퍼 폴리곤 생성
  const bufferPolygon = createBufferPolygon(path, bufferDistance)
  
  if (bufferPolygon && bufferPolygon.length > 0) {
    // 카카오맵 폴리곤으로 변환
    const kakaoPath = bufferPolygon.map(coord => 
      new window.kakao.maps.LatLng(coord.lat, coord.lng)
    )
    
    // 폴리곤 생성 및 지도에 표시
    currentSafetyZone = new window.kakao.maps.Polygon({
      path: kakaoPath,
      strokeWeight: 2,
      strokeColor: '#FF3B30',
      strokeOpacity: 0.8,
      fillColor: '#FF3B30',
      fillOpacity: 0.2
    })
    
    currentSafetyZone.setMap(mapInstance)
    
    // 전체 영역이 보이도록 지도 범위 조정
    const bounds = new window.kakao.maps.LatLngBounds()
    kakaoPath.forEach(latLng => bounds.extend(latLng))
    mapInstance.setBounds(bounds)
  }
}

/**
 * 경로를 따라 버퍼 폴리곤 생성 (경로 모양을 정확히 따라가는 알고리즘)
 */
function createBufferPolygon(path, bufferDistance) {
  if (path.length < 2) return []
  
  const leftPoints = []
  const rightPoints = []
  const metersPerDegree = 111320 // 대략적인 미터당 경도 변환
  
  // 각 점에 대해 양쪽 버퍼 점 생성
  for (let i = 0; i < path.length; i++) {
    const point = path[i]
    const lat = point.getLat()
    const lng = point.getLng()
    
    // 이전/다음 점과의 방향 계산
    let perpX = 0, perpY = 0
    
    if (i === 0) {
      // 첫 번째 점: 다음 점 방향 사용
      const next = path[i + 1]
      const dx = next.getLng() - lng
      const dy = next.getLat() - lat
      const length = Math.sqrt(dx * dx + dy * dy)
      if (length > 0) {
        perpX = -dy / length
        perpY = dx / length
      }
    } else if (i === path.length - 1) {
      // 마지막 점: 이전 점 방향 사용
      const prev = path[i - 1]
      const dx = lng - prev.getLng()
      const dy = lat - prev.getLat()
      const length = Math.sqrt(dx * dx + dy * dy)
      if (length > 0) {
        perpX = -dy / length
        perpY = dx / length
      }
    } else {
      // 중간 점: 이전과 다음 점의 평균 방향 사용
      const prev = path[i - 1]
      const next = path[i + 1]
      
      const dx1 = lng - prev.getLng()
      const dy1 = lat - prev.getLat()
      const dx2 = next.getLng() - lng
      const dy2 = next.getLat() - lat
      
      const dx = dx1 + dx2
      const dy = dy1 + dy2
      const length = Math.sqrt(dx * dx + dy * dy)
      
      if (length > 0) {
        perpX = -dy / length
        perpY = dx / length
      }
    }
    
    // 버퍼 거리를 경도/위도 단위로 변환
    const latOffset = bufferDistance / metersPerDegree
    const lngOffset = bufferDistance / (metersPerDegree * Math.cos(lat * Math.PI / 180))
    
    // 양쪽 버퍼 점 생성
    const leftLat = lat + perpY * latOffset
    const leftLng = lng + perpX * lngOffset
    const rightLat = lat - perpY * latOffset
    const rightLng = lng - perpX * lngOffset
    
    leftPoints.push({ lat: leftLat, lng: leftLng })
    rightPoints.push({ lat: rightLat, lng: rightLng })
  }
  
  // 왼쪽과 오른쪽 점들을 연결하여 폴리곤 생성
  const polygonPoints = []
  
  // 왼쪽 점들을 순서대로 추가
  for (let i = 0; i < leftPoints.length; i++) {
    polygonPoints.push(leftPoints[i])
  }
  
  // 오른쪽 점들을 역순으로 추가
  for (let i = rightPoints.length - 1; i >= 0; i--) {
    polygonPoints.push(rightPoints[i])
  }
  
  return polygonPoints
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
  min-height: 100vh;
  overflow: hidden;
}

.map-box {
  flex: 1;
  width: 100%;
  min-height: 0;
}

.control-panel {
  background: white;
  padding: 16px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
}

.safety-zone-control {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}

.dropdown-container {
  position: relative;
  flex: 1;
  max-width: 120px;
}

.range-selector {
  width: 100%;
  height: 800px; /* 지도 길이 조정 */
}

.controls {
  background: #fff;
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
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


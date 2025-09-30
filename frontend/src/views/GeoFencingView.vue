<template>
  <div class="geo-fencing-page">
    <!-- 지도 영역 -->
    <div ref="mapContainer" class="map-box"></div>
    
    <!-- 하단 컨트롤 영역 -->
    <div class="control-panel">
      <div class="safety-zone-control">
        <span class="label">안전존 범위</span>
        <div class="dropdown-container">
          <select v-model="selectedRange" class="range-selector">
            <option value="1">1단계</option>
            <option value="2">2단계</option>
            <option value="3">3단계</option>
          </select>
          <div class="dropdown-arrow">▼</div>
        </div>
        <button class="confirm-btn" @click="updateSafetyZone">확인</button>
      </div>
      
      <div class="action-buttons">
        <button class="primary-btn">설정</button>
        <button class="secondary-btn">취소</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

const mapContainer = ref(null)
const selectedRange = ref('1')
let mapInstance = null
let currentPolyline = null
let currentSafetyZone = null

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

  // Polyline 생성
  const polyline = new kakao.maps.Polyline({
    path,
    strokeWeight: 5,
    strokeColor: '#FF3B30',
    strokeOpacity: 0.9,
    strokeStyle: 'solid',
  })

  // 기존 폴리라인 제거
  if (currentPolyline) {
    currentPolyline.setMap(null)
  }
  
  // 지도에 Polyline 표시
  polyline.setMap(map)
  currentPolyline = polyline

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
</script>

<style scoped>
.geo-fencing-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
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
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  font-size: 14px;
  appearance: none;
  cursor: pointer;
}

.dropdown-arrow {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  font-size: 12px;
  color: #666;
}

.confirm-btn {
  padding: 8px 16px;
  background: #9c27b0;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

.confirm-btn:hover {
  background: #8e24aa;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.primary-btn {
  flex: 1;
  padding: 12px;
  background: #9c27b0;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
}

.primary-btn:hover {
  background: #8e24aa;
}

.secondary-btn {
  flex: 1;
  padding: 12px;
  background: white;
  color: #9c27b0;
  border: 1px solid #9c27b0;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
}

.secondary-btn:hover {
  background: #f3e5f5;
}
</style>


<template>
  <div class="page">
    <div ref="mapContainer" class="map-box"></div>
    
    <!-- 안심존 반경 설정 UI -->
    <div class="controls">
      <div class="control-header">
        <h3>안심존 반경 설정</h3>
        <p>반경을 선택하면 지도에 안심존이 표시됩니다</p>
      </div>
      
      <div class="control-row">
        <div class="level-options">
          <button 
            v-for="level in radiusLevels" 
            :key="level.value"
            class="level-btn"
            :class="{ active: radiusLevel === level.value }"
            @click="selectRadius(level.value)"
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { circle } from '@turf/turf'

const router = useRouter()
const mapContainer = ref(null)
const radiusLevel = ref(1)

let mapInstance = null
let centerMarker = null
let currentCircle = null

const radiusLevels = [
  { value: 1, name: '1단계', desc: '100m 반경' },
  { value: 2, name: '2단계', desc: '200m 반경' },
  { value: 3, name: '3단계', desc: '500m 반경' }
]

// 반경 설정 (미터)
const radiusSettings = {
  1: 100,
  2: 200,
  3: 500
}

const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
let locationData = null

onMounted(() => {
  // sessionStorage에서 위치 데이터 가져오기
  const savedLocation = sessionStorage.getItem('basicSafeZoneLocation')
  if (!savedLocation) {
    alert('위치 정보가 없습니다. 이전 단계로 돌아갑니다.')
    router.push({ name: 'basic-safe-zone-location' })
    return
  }

  locationData = JSON.parse(savedLocation)
  loadKakaoMap()
})

function loadKakaoMap() {
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services,clusterer,drawing&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      const options = {
        center: new window.kakao.maps.LatLng(locationData.latitude, locationData.longitude),
        level: 5
      }
      mapInstance = new window.kakao.maps.Map(mapContainer.value, options)

      // 중심 마커 표시
      addCenterMarker()
      
      // 초기 안심존 표시
      updateCircle()
    })
  }
}

function addCenterMarker() {
  if (!mapInstance || !locationData) return

  const markerPosition = new window.kakao.maps.LatLng(locationData.latitude, locationData.longitude)
  
  centerMarker = new window.kakao.maps.Marker({
    position: markerPosition,
    map: mapInstance
  })

  // 인포윈도우 추가
  const infowindow = new window.kakao.maps.InfoWindow({
    content: `<div style="padding:8px 12px;font-size:13px;font-weight:600;color:#111827;">${locationData.name}</div>`
  })
  infowindow.open(mapInstance, centerMarker)
}

function selectRadius(level) {
  radiusLevel.value = level
  updateCircle()
}

function updateCircle() {
  if (!mapInstance || !locationData) return

  // 기존 원 제거
  if (currentCircle) {
    currentCircle.setMap(null)
  }

  const radius = radiusSettings[radiusLevel.value]

  // Turf.js로 원형 폴리곤 생성
  const center = [locationData.longitude, locationData.latitude]
  const options = { steps: 64, units: 'meters' }
  const circleGeoJSON = circle(center, radius, options)

  // GeoJSON 좌표를 Kakao 좌표로 변환
  const coordinates = circleGeoJSON.geometry.coordinates[0]
  const kakaoPath = coordinates.map(coord => {
    return new window.kakao.maps.LatLng(coord[1], coord[0])
  })

  // 카카오맵 원 그리기
  currentCircle = new window.kakao.maps.Polygon({
    path: kakaoPath,
    strokeWeight: 3,
    strokeColor: '#6366f1',
    strokeOpacity: 0.8,
    fillColor: '#6366f1',
    fillOpacity: 0.2
  })

  currentCircle.setMap(mapInstance)

  // 지도 레벨 조정
  const bounds = new window.kakao.maps.LatLngBounds()
  kakaoPath.forEach(point => bounds.extend(point))
  mapInstance.setBounds(bounds)
}

async function saveSettings() {
  if (!locationData) {
    alert('위치 정보가 없습니다.')
    return
  }

  try {
    const radius = radiusSettings[radiusLevel.value]

    // 안심존 데이터 구조
    const boundaryData = {
      type: 'Circle',
      center: {
        lat: locationData.latitude,
        lng: locationData.longitude
      },
      radius: radius,
      level: radiusLevel.value,
      locationName: locationData.name,
      locationAddress: locationData.address
    }

    // 백엔드 API 호출
    const response = await fetch(`/api/schedule/basic-safe-zone`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({
        boundaryCoordinates: JSON.stringify(boundaryData)
      })
    })

    if (!response.ok) {
      const error = await response.json()
      throw new Error(error.message || '기본 안심존 저장에 실패했습니다.')
    }

    const result = await response.json()
    console.log('기본 안심존 저장 성공:', result)

    // sessionStorage 정리
    sessionStorage.removeItem('basicSafeZoneLocation')
    sessionStorage.removeItem('existingBasicSafeZone')

    alert('기본 안심존이 성공적으로 설정되었습니다.')
    router.push({ name: 'gdmypage' })

  } catch (error) {
    console.error('기본 안심존 저장 오류:', error)
    alert(error.message || '기본 안심존 저장 중 오류가 발생했습니다.')
  }
}

function cancelSettings() {
  if (confirm('작성 중인 내용이 사라집니다. 정말 취소하시겠습니까?')) {
    sessionStorage.removeItem('basicSafeZoneLocation')
    sessionStorage.removeItem('existingBasicSafeZone')
    router.push({ name: 'gdmypage' })
  }
}
</script>

<style scoped>
.page {
  position: relative;
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.map-box {
  width: 100%;
  flex: 1;
  min-height: 300px;
  max-height: 50vh; /* 지도 최대 높이를 화면의 50%로 제한 */
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

/* 반경 선택 버튼 */
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


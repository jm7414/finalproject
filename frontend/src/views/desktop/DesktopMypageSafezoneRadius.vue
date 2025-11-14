<template>
  <div class="desktop-mypage-safezone-radius">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">안심존 반경 설정</h1>
        <p class="page-subtitle">반경을 선택하면 지도에 안심존이 표시됩니다</p>
      </div>

      <div class="content-split">
        <!-- 좌측: 지도 -->
        <div class="map-section">
          <div ref="mapContainer" class="map-view"></div>
        </div>

        <!-- 우측: 컨트롤 -->
        <div class="control-section">
          <div class="control-group">
            <h3 class="control-title">안심존 반경 설정</h3>
            <p class="control-desc">반경을 선택하면 지도에 안심존이 표시됩니다</p>
            
            <div class="radius-options">
              <button 
                v-for="level in radiusLevels" 
                :key="level.value"
                class="radius-btn"
                :class="{ active: radiusLevel === level.value }"
                @click="selectRadius(level.value)"
              >
                <div class="radius-name">{{ level.name }}</div>
                <div class="radius-desc">{{ level.desc }}</div>
              </button>
            </div>
          </div>

          <!-- 선택된 위치 정보 -->
          <div v-if="locationData" class="location-info">
            <div class="location-label">선택된 위치</div>
            <div class="location-name">{{ locationData.name }}</div>
            <div class="location-address">{{ locationData.address }}</div>
          </div>

          <!-- 액션 버튼 -->
          <div class="actions">
            <button class="btn-save" @click="saveSettings">설정</button>
            <button class="btn-cancel" @click="cancelSettings">취소</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { circle } from '@turf/turf'

const router = useRouter()
const mapContainer = ref(null)
const radiusLevel = ref(1)

let mapInstance = null
let centerMarker = null
let currentCircle = null

const radiusLevels = [
  { value: 1, name: '1단계', desc: '30m 반경' },
  { value: 2, name: '2단계', desc: '60m 반경' },
  { value: 3, name: '3단계', desc: '100m 반경' }
]

const radiusSettings = {
  1: 30,
  2: 60,
  3: 100
}

const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
const locationData = ref(null)

onMounted(() => {
  const savedLocation = sessionStorage.getItem('basicSafeZoneLocation')
  if (!savedLocation) {
    alert('위치 정보가 없습니다. 이전 단계로 돌아갑니다.')
    router.push('/desktop/mypage/safezone')
    return
  }

  locationData.value = JSON.parse(savedLocation)
  loadKakaoMap()
})

function loadKakaoMap() {
  if (!window.kakao || !window.kakao.maps) {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services,clusterer,drawing&autoload=false`
    document.head.appendChild(script)
    script.onload = () => {
      window.kakao.maps.load(() => {
        initMap()
      })
    }
  } else {
    if (window.kakao.maps.load) {
      window.kakao.maps.load(() => {
        initMap()
      })
    } else {
      initMap()
    }
  }
}

function initMap() {
  if (!mapContainer.value || !locationData.value) return

  const options = {
    center: new window.kakao.maps.LatLng(locationData.value.latitude, locationData.value.longitude),
    level: 5
  }
  mapInstance = new window.kakao.maps.Map(mapContainer.value, options)

  addCenterMarker()
  updateCircle()
}

function addCenterMarker() {
  if (!mapInstance || !locationData.value) return

  const markerPosition = new window.kakao.maps.LatLng(locationData.value.latitude, locationData.value.longitude)
  
  centerMarker = new window.kakao.maps.Marker({
    position: markerPosition,
    map: mapInstance
  })

  const infowindow = new window.kakao.maps.InfoWindow({
    content: `<div style="padding:8px 12px;font-size:13px;font-weight:600;color:#111827;">${locationData.value.name}</div>`
  })
  infowindow.open(mapInstance, centerMarker)
}

function selectRadius(level) {
  radiusLevel.value = level
  updateCircle()
}

function updateCircle() {
  if (!mapInstance || !locationData.value) return

  if (currentCircle) {
    currentCircle.setMap(null)
  }

  const radius = radiusSettings[radiusLevel.value]

  const center = [locationData.value.longitude, locationData.value.latitude]
  const options = { steps: 64, units: 'meters' }
  const circleGeoJSON = circle(center, radius, options)

  const coordinates = circleGeoJSON.geometry.coordinates[0]
  const kakaoPath = coordinates.map(coord => {
    return new window.kakao.maps.LatLng(coord[1], coord[0])
  })

  currentCircle = new window.kakao.maps.Polygon({
    path: kakaoPath,
    strokeWeight: 3,
    strokeColor: '#6366f1',
    strokeOpacity: 0.8,
    fillColor: '#6366f1',
    fillOpacity: 0.2
  })

  currentCircle.setMap(mapInstance)

  const bounds = new window.kakao.maps.LatLngBounds()
  kakaoPath.forEach(point => bounds.extend(point))
  mapInstance.setBounds(bounds)
}

async function saveSettings() {
  if (!locationData.value) {
    alert('위치 정보가 없습니다.')
    return
  }

  try {
    const radius = radiusSettings[radiusLevel.value]

    const boundaryData = {
      type: 'Circle',
      center: {
        lat: locationData.value.latitude,
        lng: locationData.value.longitude
      },
      radius: radius,
      level: radiusLevel.value,
      locationName: locationData.value.name,
      locationAddress: locationData.value.address
    }

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

    sessionStorage.removeItem('basicSafeZoneLocation')
    sessionStorage.removeItem('existingBasicSafeZone')

    alert('기본 안심존이 성공적으로 설정되었습니다.')
    router.push('/desktop/mypage')

  } catch (error) {
    console.error('기본 안심존 저장 오류:', error)
    alert(error.message || '기본 안심존 저장 중 오류가 발생했습니다.')
  }
}

function cancelSettings() {
  if (confirm('작성 중인 내용이 사라집니다. 정말 취소하시겠습니까?')) {
    sessionStorage.removeItem('basicSafeZoneLocation')
    sessionStorage.removeItem('existingBasicSafeZone')
    router.push('/desktop/mypage')
  }
}

onBeforeUnmount(() => {
  if (currentCircle) {
    currentCircle.setMap(null)
  }
  if (centerMarker) {
    centerMarker.setMap(null)
  }
})
</script>

<style scoped>
.desktop-mypage-safezone-radius {
  width: 100%;
  min-height: calc(100vh - 80px);
  padding: 0;
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #171717;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #737373;
  margin: 0;
}

.content-split {
  display: flex;
  gap: 24px;
  min-height: 600px;
}

.map-section {
  flex: 1 1 55%;
  min-width: 0;
  border-radius: 12px;
  overflow: hidden;
  background: #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.map-view {
  width: 100%;
  height: 100%;
  min-height: 600px;
}

.control-section {
  flex: 1 1 45%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.control-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.control-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.radius-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.radius-btn {
  padding: 20px 16px;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.radius-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.radius-btn.active {
  background: #eef2ff;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.radius-name {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.radius-btn.active .radius-name {
  color: #6366f1;
}

.radius-desc {
  font-size: 14px;
  color: #6b7280;
}

.radius-btn.active .radius-desc {
  color: #4f46e5;
}

.location-info {
  padding: 20px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
}

.location-label {
  font-size: 12px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 8px;
}

.location-name {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.location-address {
  font-size: 14px;
  color: #6b7280;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.btn-save,
.btn-cancel {
  flex: 1;
  padding: 14px 24px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-save {
  background: #6366f1;
  color: #ffffff;
  border: none;
}

.btn-save:hover {
  background: #4f46e5;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.btn-cancel {
  background: #ffffff;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.btn-cancel:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

@media (max-width: 1024px) {
  .content-split {
    flex-direction: column;
    min-height: auto;
  }

  .map-section {
    flex: 0 0 400px;
  }

  .control-section {
    flex: 1;
  }

  .radius-options {
    grid-template-columns: 1fr;
  }
}
</style>


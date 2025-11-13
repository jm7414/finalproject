<template>
  <div class="desktop-page">
    <section class="main-split">
      <div class="map-column">
        <div class="map-header">
          <div>
            <h1>안심존 관리</h1>
            <p class="subtitle">환자의 현재 위치와 안심존을 모니터링하세요.</p>
          </div>
          <button type="button" class="create-zone-btn" @click="openBasicSafeZoneModal">기본 안심존 변경</button>
        </div>

        <!-- Kakao Map 영역 -->
        <div class="map-surface">
          <div ref="mapEl" class="map-view"></div>
          
          <!-- 지도 컨트롤 (지도 영역 내부) -->
          <MapControls 
            :desktop="true"
            :is-patient-connected="isPatientConnected"
            :is-safe-zone-enabled="isSafeZoneEnabled"
            :is-safe-zone-control-expanded="isSafeZoneControlExpanded"
            :current-active-zone="currentActiveZone"
            :selected-level="selectedLevel"
            :buffer-levels="bufferLevels"
            :location-btn-bottom="20"
            @toggle-safe-zone="toggleSafeZone"
            @toggle-safe-zone-control="toggleSafeZoneControl"
            @select-level="selectLevel"
            @zoom-in="zoomIn"
            @zoom-out="zoomOut"
            @move-to-patient-location="moveToPatientLocation"
          />
        </div>
      </div>

      <aside class="info-column">
        <section class="panel patient-card">
          <header class="panel-header">
            <h2>환자 정보</h2>
            <button type="button" class="panel-action">상세 보기</button>
          </header>
          <div class="patient-body">
            <div class="patient-avatar">
              <img 
                v-if="patientInfo.profileImageUrl || defaultProfileImage"
                :src="patientInfo.profileImageUrl || defaultProfileImage" 
                alt="프로필"
                @error="handleImageError"
              />
              <div v-else class="avatar-placeholder">🙂</div>
            </div>
            <div class="patient-meta">
              <strong>{{ patientInfo.name || patient.name }}</strong>
              <span>{{ patient.age }}세 · {{ patient.gender }}</span>
              <span>등록일 {{ patient.registeredAt }}</span>
            </div>
            <ul class="patient-stats">
              <li>
                <span class="label">현재 상태</span>
                <span class="value" :class="{ alert: !safeZoneStatus.isInside, inside: safeZoneStatus.isInside }">
                  {{ safeZoneStatus.isInside ? '안심존 내부' : safeZoneStatus.status }}
                </span>
              </li>
              <li>
                <span class="label">마지막 업데이트</span>
                <span class="value">{{ safeZoneStatus.lastUpdated || '2분 전' }}</span>
              </li>
              <li>
                <span class="label">현재 위치</span>
                <span class="value">{{ safeZoneStatus.currentArea || '서울시 강남구' }}</span>
              </li>
            </ul>
          </div>
        </section>

        <section class="panel schedule-panel">
          <header class="panel-header">
            <h2>오늘의 일정</h2>
            <button type="button" class="panel-action more-btn" @click="goToSchedule">
              더보기
            </button>
          </header>
          <div v-if="todayScheduleCards.length > 0" class="today-schedule-list">
            <div
              v-for="schedule in todayScheduleCards"
              :key="schedule.scheduleNo"
              class="today-schedule-item"
            >
              <div class="today-schedule-header">
                <span class="today-schedule-time">
                  {{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}
                </span>
                <span
                  :class="['today-schedule-status', scheduleStatusMeta(schedule).class]"
                >
                  {{ scheduleStatusMeta(schedule).label }}
                </span>
              </div>
              <strong class="today-schedule-title">{{ schedule.scheduleTitle }}</strong>
              <div v-if="formatLocation(schedule.scheduleNo)" class="today-schedule-route">
                {{ formatLocation(schedule.scheduleNo) }}
              </div>
            </div>
          </div>
          <div v-else class="today-schedule-empty">
            오늘 예정된 일정이 없습니다.
          </div>
        </section>

        <section class="panel community-panel">
          <header class="panel-header">
            <h2>최신 게시글</h2>
            <button type="button" class="panel-action more-btn" @click="goToCommunityBoard">
              더보기
            </button>
          </header>
          
          <!-- <div v-if="recentPosts.length > 0" class="recent-post-list">
            <div v-for="post in recentPosts":key="post.id" class="recent-post-item">
              <div class="recent-post-header">
                <span class="post-category">{{ post.category }}</span> 
                <span class="post-time">{{ post.time }}</span>
              </div>
              <strong class="recent-post-title">{{ post.title }}</strong>
              <div class="recent-post-author">{{ post.author }}</div>
            </div>
          </div>
          
          <div v-else class="recent-post-empty">
            최신 게시글이 없습니다.
          </div> -->
        </section>
      </aside>
    </section>

    <!-- 기본 안심존 변경 모달 -->
    <div v-if="showBasicSafeZoneModal" class="modal-overlay" @click="closeBasicSafeZoneModal">
      <div class="basic-safe-zone-modal" @click.stop>
        <div class="modal-header">
          <h2>기본 안심존 변경</h2>
          <button class="close-btn" @click="closeBasicSafeZoneModal">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2"/>
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <div class="modal-content-split">
            <!-- 좌측: 지도 -->
            <div class="modal-map-section">
              <div ref="modalMapEl" class="modal-map-view"></div>
            </div>

            <!-- 우측: 컨트롤 -->
            <div class="modal-control-section">
              <!-- 위치 선택 -->
              <div class="control-group">
                <h3 class="control-title">위치 선택</h3>
                
                <!-- 방법 선택 -->
                <div class="method-tabs">
                  <button 
                    class="method-tab" 
                    :class="{ active: selectedLocationMethod === 'search' }"
                    @click="selectedLocationMethod = 'search'"
                  >
                    직접 검색
                  </button>
                  <button 
                    class="method-tab" 
                    :class="{ active: selectedLocationMethod === 'current' }"
                    @click="selectedLocationMethod = 'current'"
                  >
                    환자 현위치
                  </button>
                </div>

                <!-- 검색 입력 -->
                <div v-if="selectedLocationMethod === 'search'" class="search-input-wrapper">
                  <input
                    v-model="locationQuery"
                    type="text"
                    placeholder="예) 서울시 강남구 역삼동"
                    class="search-input"
                    @keyup.enter="searchLocation"
                    @input="onLocationQueryInput"
                  />
                  <button class="search-btn" @click="searchLocation">검색</button>
                </div>

                <!-- 환자 현위치 버튼 -->
                <div v-if="selectedLocationMethod === 'current'" class="current-location-wrapper">
                  <button 
                    class="current-location-btn" 
                    @click="usePatientLocationForModal"
                    :disabled="isLoadingPatientLocation"
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                      <circle cx="12" cy="12" r="3" fill="currentColor"/>
                    </svg>
                    {{ isLoadingPatientLocation ? '위치 조회 중...' : '환자 현위치로 설정' }}
                  </button>
                </div>

                <!-- 검색 결과 -->
                <div v-if="showLocationResults && locationSearchResults.length > 0" class="search-results">
                  <div 
                    v-for="place in locationSearchResults" 
                    :key="place.id"
                    class="search-result-item"
                    @click="selectLocationFromSearch(place)"
                  >
                    <div class="result-info">
                      <div class="result-name">{{ place.place_name }}</div>
                      <div class="result-address">{{ place.road_address_name || place.address_name }}</div>
                    </div>
                    <button class="select-location-btn">선택</button>
                  </div>
                </div>

                <!-- 선택된 위치 표시 -->
                <div v-if="selectedLocationData" class="selected-location-display">
                  <div class="selected-location-label">선택된 위치</div>
                  <div class="selected-location-name">{{ selectedLocationData.name }}</div>
                  <div class="selected-location-address">{{ selectedLocationData.address }}</div>
                </div>
              </div>

              <!-- 반경 설정 -->
              <div class="control-group">
                <h3 class="control-title">안심존 반경 설정</h3>
                <p class="control-desc">반경을 선택하면 지도에 안심존이 표시됩니다</p>
                
                <div class="radius-options">
                  <button 
                    v-for="level in bufferLevels" 
                    :key="level.value"
                    class="radius-btn"
                    :class="{ active: modalRadiusLevel === level.value }"
                    @click="selectModalRadius(level.value)"
                  >
                    <div class="radius-name">{{ level.name }}</div>
                    <div class="radius-desc">{{ level.desc }}</div>
                  </button>
                </div>
              </div>

              <!-- 액션 버튼 -->
              <div class="modal-actions">
                <button class="save-btn" @click="saveBasicSafeZone" :disabled="!selectedLocationData">
                  저장
                </button>
                <button class="cancel-modal-btn" @click="closeBasicSafeZoneModal">취소</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onBeforeUnmount, ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { lineString, buffer, circle } from '@turf/turf'
import { useKakaoMap } from '@/composables/useKakaoMap'
import { useSchedule } from '@/composables/useSchedule'
import { usePatientLocation } from '@/composables/usePatientLocation'
import MapControls from '@/components/MapControls.vue'
import defaultProfileImage from '@/assets/default-profile.png'

const router = useRouter()

// 더미 데이터 (나중에 실제 API로 교체)
const patient = {
  name: '김영희',
  age: 78,
  gender: '여성',
  registeredAt: '2025.01.15'
}

// 환자 정보 데이터
const patientInfo = ref({
  name: '',
  userNo: null,
  isOnline: false,
  lastActivity: null,
  user_status: 0,
  profileImageUrl: '',
  imageError: false
})

// 안심존 상태 관련
const safeZoneStatus = ref({
  isInside: true,
  status: '연결 필요',
  color: '#9CA3AF',
  bgColor: '#F3F4F6',
  lastUpdated: '',
  currentArea: ''
})

// 안심존 관련
let currentSafeZone = null // 현재 표시 중인 안심존 폴리곤/원형
let previewSafeZone = null // 미리보기 안심존

// 안심존 활성화 상태 관리
const isSafeZoneEnabled = ref(true)
const isPatientConnected = ref(false)

// 안심존 범위 설정 컨트롤 상태
const isSafeZoneControlExpanded = ref(false)
const selectedLevel = ref(1)
const currentActiveZone = ref(null)
const originalLevel = ref(1)

// 버퍼 레벨 설정
const bufferLevels = [
  { value: 1, name: '1단계', desc: '30m' },
  { value: 2, name: '2단계', desc: '60m' },
  { value: 3, name: '3단계', desc: '100m' }
]

/* ===== Kakao Map 초기화 ===== */
const {
  mapEl,
  mapInstance,
  initMap,
  zoomIn,
  zoomOut,
  setCenter,
  setBounds,
  moveToPatientLocation: moveToPatientLocationMap
} = useKakaoMap({
  kakaoKey: import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891',
  center: { lat: 37.4943524920695, lng: 126.88767655688868 },
  defaultLevel: 3,
  enableControls: true,
  enableTracking: true
})

/* ===== 환자 정보 관리 ===== */
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
    
    if (patient.message) {
      console.warn(patient.message)
      isPatientConnected.value = false
      patientInfo.value = { name: '', userNo: null, isOnline: false, lastActivity: null, user_status: 0, profileImageUrl: null, imageError: false }
      return null
    } else {
      isPatientConnected.value = true
      const imageUrl =
        patient.profilePhoto ||
        patient.imageUrl ||
        patient.photoPath ||
        patient.profileImageUrl ||
        patient.profileImgUrl ||
        ''

      patientInfo.value = {
        name: patient.name || '',
        userNo: patient.userNo,
        isOnline: patient.isOnline ?? false,
        lastActivity: patient.lastActivity || null,
        user_status: patient.userStatus === 1 ? 1 : 0,
        profileImageUrl: imageUrl,
        imageError: false
      }
      return patient.userNo
    }
  } catch (error) {
    console.error('환자 정보 조회 오류:', error)
    isPatientConnected.value = false
    return null
  }
}

/* ===== 일정 관련 composable ===== */
const {
  patientUserNo,
  allSchedules,
  scheduleLocations,
  formatTime,
  formatLocation,
  getScheduleStatus,
  loadScheduleData,
  getCurrentSchedule
} = useSchedule({
  fetchPatientInfo,
  onScheduleLoaded: () => {
    checkPatientInSafeZone()
  }
})

const todayScheduleCards = computed(() => {
  const now = new Date()
  const key = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  return allSchedules.value
    .filter(schedule => schedule.scheduleDate === key)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
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
  patientInfo,
  mapInstance,
  onLocationUpdate: (location) => {
    checkPatientInSafeZone()
  },
  onPatientInfoUpdate: (info) => {
    // 환자 정보 업데이트
  },
  onError: (error) => {
    console.error('환자 위치 추적 오류:', error)
  }
})

/* ===== 안심존 데이터 관리 ===== */
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
      console.error('지원하지 않는 bufferCoordinates 형식:', bufferCoordinates)
      return
    }
    
    const kakaoPath = coordinates.map(coord => 
      new window.kakao.maps.LatLng(coord.latitude, coord.longitude)
    )
    
    currentSafeZone = new window.kakao.maps.Polygon({
      path: kakaoPath,
      strokeWeight: 2,
      strokeColor: '#EF4444',
      strokeOpacity: 0.8,
      fillColor: '#EF4444',
      fillOpacity: 0.3
    })
    
    currentSafeZone.setMap(map)
    
    const bounds = new window.kakao.maps.LatLngBounds()
    kakaoPath.forEach(latLng => bounds.extend(latLng))
    map.setBounds(bounds)
    
    console.log('경로형 안심존 표시 완료')
  } catch (error) {
    console.error('경로형 안심존 표시 오류:', error)
  }
}

function drawBasicSafeZone(map, boundaryData) {
  if (!map || !boundaryData) return
  
  try {
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
    }
    
    if (boundaryData.type === 'Circle') {
      const center = new window.kakao.maps.LatLng(boundaryData.center.lat, boundaryData.center.lng)
      const radius = boundaryData.radius
      
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
      
      currentSafeZone = new window.kakao.maps.Polygon({
        path: circlePoints,
        strokeWeight: 3,
        strokeColor: '#6366f1',
        strokeOpacity: 0.8,
        fillColor: '#6366f1',
        fillOpacity: 0.2
      })
      
      currentSafeZone.setMap(map)
      
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
async function updateSafeZone(map) {
  if (!map || !patientUserNo.value) return
  
  if (!isSafeZoneEnabled.value) {
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
      currentSafeZone = null
      currentActiveZone.value = null
    }
    return
  }
  
  try {
    const currentSchedule = getCurrentSchedule()
    
    if (currentSchedule) {
      console.log('현재 진행 중인 일정:', currentSchedule.scheduleTitle)
      const bufferCoordinates = await fetchScheduleSafeZone(currentSchedule.scheduleNo)
      
      if (bufferCoordinates && (
        (Array.isArray(bufferCoordinates) && bufferCoordinates.length > 0) ||
        (typeof bufferCoordinates === 'object' && bufferCoordinates.coordinates && bufferCoordinates.coordinates.length > 0)
      )) {
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
    
    console.log('기본 안심존 표시')
    const basicSafeZone = await fetchBasicSafeZone(patientUserNo.value)
    
    if (basicSafeZone) {
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
function detectBasicSafeZoneLevel(boundaryData) {
  if (!boundaryData || boundaryData.type !== 'Circle') return 1
  
  if (boundaryData.level) {
    return boundaryData.level
  }
  
  const radius = boundaryData.radius
  if (radius <= 30) return 1
  if (radius <= 60) return 2
  if (radius <= 100) return 3
  return 1
}

function detectRouteSafeZoneLevel(bufferCoordinates) {
  if (!bufferCoordinates || bufferCoordinates.length === 0) return 1
  
  if (typeof bufferCoordinates === 'object' && !Array.isArray(bufferCoordinates)) {
    if (bufferCoordinates.level) {
      return bufferCoordinates.level
    }
    
    if (bufferCoordinates.coordinates && Array.isArray(bufferCoordinates.coordinates)) {
      const firstCoord = bufferCoordinates.coordinates[0]
      if (firstCoord && firstCoord.level) {
        return firstCoord.level
      }
    }
  }
  
  if (Array.isArray(bufferCoordinates) && bufferCoordinates.length > 0) {
    const firstCoord = bufferCoordinates[0]
    if (firstCoord && typeof firstCoord === 'object' && firstCoord.level) {
      return firstCoord.level
    }
  }
  
  return 1
}

/* ===== 안심존 설정 및 컨트롤 ===== */
function toggleSafeZoneControl() {
  if (!currentActiveZone.value || !isSafeZoneEnabled.value) return
  
  if (isSafeZoneControlExpanded.value) {
    saveSafeZoneLevel()
  } else {
    isSafeZoneControlExpanded.value = true
  }
}

function selectLevel(level) {
  selectedLevel.value = level
  updatePreviewSafeZone()
}

function updatePreviewSafeZone() {
  if (!mapInstance.value || !currentActiveZone.value) return
  
  if (previewSafeZone) {
    previewSafeZone.setMap(null)
  }
  
  const level = selectedLevel.value
  const radiusMap = { 1: 30, 2: 60, 3: 100 }
  const radius = radiusMap[level]
  
  try {
    if (currentActiveZone.value.type === '기본형') {
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
      const scheduleNo = currentActiveZone.value.scheduleNo
      fetchRouteCoordinates(scheduleNo).then(routeCoordinates => {
        if (!routeCoordinates || routeCoordinates.length < 2) return
        
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

async function saveSafeZoneLevel() {
  if (!currentActiveZone.value) return
  
  try {
    const level = selectedLevel.value
    const radiusMap = { 1: 30, 2: 60, 3: 100 }
    const radius = radiusMap[level]
    
    if (currentActiveZone.value.type === '기본형') {
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
      const scheduleNo = currentActiveZone.value.scheduleNo
      const routeCoordinates = await fetchRouteCoordinates(scheduleNo)
      
      if (!routeCoordinates || routeCoordinates.length < 2) {
        throw new Error('경로 정보를 찾을 수 없습니다.')
      }
      
      const turfCoords = routeCoordinates.map(c => [c.longitude, c.latitude])
      const line = lineString(turfCoords)
      const buffered = buffer(line, radius, { units: 'meters' })
      
      const coordinates = buffered.geometry.coordinates[0].map(coord => ({
        latitude: coord[1],
        longitude: coord[0]
      }))
      
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
    
    if (previewSafeZone) {
      previewSafeZone.setMap(null)
      previewSafeZone = null
    }
    
    await updateSafeZone(mapInstance.value)
    checkPatientInSafeZone()
    
    isSafeZoneControlExpanded.value = false
    originalLevel.value = level
    
  } catch (error) {
    console.error('안심존 저장 오류:', error)
    alert('안심존 저장에 실패했습니다.')
  }
}

watch(isSafeZoneControlExpanded, (newVal) => {
  if (!newVal) {
    if (previewSafeZone) {
      previewSafeZone.setMap(null)
      previewSafeZone = null
    }
    selectedLevel.value = originalLevel.value
  }
})

/* ===== 안심존 상태 확인 및 판단 ===== */
function checkPatientInSafeZone() {
  if (!patientUserNo.value) {
    safeZoneStatus.value = {
      isInside: false,
      status: '연결 필요',
      color: '#9CA3AF',
      bgColor: '#F3F4F6',
      lastUpdated: '',
      currentArea: ''
    }
    return
  }
  
  if (!isSafeZoneEnabled.value) {
    safeZoneStatus.value = {
      isInside: false,
      status: '안심존 비활성화',
      color: '#9CA3AF',
      bgColor: '#F3F4F6',
      lastUpdated: '',
      currentArea: ''
    }
    return
  }
  
  if (!patientLocation.value || !currentActiveZone.value) {
    safeZoneStatus.value = {
      isInside: false,
      status: '위치 확인 중',
      color: '#F59E0B',
      bgColor: '#FEF3C7',
      lastUpdated: '',
      currentArea: ''
    }
    return
  }
  
  try {
    const patientLat = patientLocation.value.latitude
    const patientLng = patientLocation.value.longitude
    
    let isInside = false
    
    if (currentActiveZone.value.type === '기본형') {
      const boundaryData = currentActiveZone.value.data
      const centerLat = boundaryData.center.lat
      const centerLng = boundaryData.center.lng
      const radius = boundaryData.radius
      
      const distance = calculateDistance(patientLat, patientLng, centerLat, centerLng)
      isInside = distance <= radius
      
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
        isInside = isPointInPolygon(patientLat, patientLng, coordinates)
      }
    }
    
    if (isInside) {
      safeZoneStatus.value = {
        isInside: true,
        status: '안전',
        color: '#16A34A',
        bgColor: '#DCFCE7',
        lastUpdated: '방금 전',
        currentArea: '서울시 강남구'
      }
    } else {
      safeZoneStatus.value = {
        isInside: false,
        status: '벗어남',
        color: '#EF4444',
        bgColor: '#FEE2E2',
        lastUpdated: '방금 전',
        currentArea: '서울시 강남구'
      }
    }
    
    console.log(`안심존 상태: ${isInside ? '안전' : '벗어남'} (환자 위치: ${patientLat}, ${patientLng})`)
    
  } catch (error) {
    console.error('안심존 상태 확인 오류:', error)
    safeZoneStatus.value = {
      isInside: false,
      status: '위치 확인 중',
      color: '#F59E0B',
      bgColor: '#FEF3C7',
      lastUpdated: '',
      currentArea: ''
    }
  }
}

/* ===== 안심존 계산 유틸리티 ===== */
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
function toggleSafeZone() {
  if (!isPatientConnected.value) return
  
  isSafeZoneEnabled.value = !isSafeZoneEnabled.value
  localStorage.setItem('safeZoneEnabled', JSON.stringify(isSafeZoneEnabled.value))
  
  if (isSafeZoneEnabled.value) {
    updateSafeZone(mapInstance.value)
    checkPatientInSafeZone()
  } else {
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
      currentSafeZone = null
      currentActiveZone.value = null
    }
    isSafeZoneControlExpanded.value = false
    checkPatientInSafeZone()
  }
}

/* ===== 이미지 에러 핸들링 ===== */
function handleImageError(event) {
  console.warn('프로필 이미지 로드 실패:', event.target.src)
  patientInfo.value.imageError = true
  // 기본 이미지로 대체 시도
  if (event.target.src !== defaultProfileImage) {
    event.target.src = defaultProfileImage
  } else {
    // 기본 이미지도 실패하면 이모지 표시
    event.target.style.display = 'none'
  }
}

/* ===== 지도 컨트롤 및 네비게이션 ===== */
function goToConnect() {
  router.push('/gdc')
}

async function moveToPatientLocation() {
  await moveToPatientLocationMap(
    patientLocation.value,
    patientUserNo.value,
    fetchPatientLocation,
    goToConnect
  )
}

// 일정 상태 메타 정보
function scheduleStatusMeta(schedule) {
  const status = getScheduleStatus(schedule)
  if (status === 'active') {
    return { label: '진행 중', class: 'active' }
  }
  if (status === 'waiting') {
    return { label: '대기 중', class: 'waiting' }
  }
  return { label: '종료', class: 'finished' }
}

function goToSchedule() {
  router.push('/desktop/schedule')
}

function goToCommunityBoard() {
  router.push('/desktop/communityBoard') 
}

/* ===== 기본 안심존 변경 모달 ===== */
const showBasicSafeZoneModal = ref(false)
const modalMapEl = ref(null)
let modalMapInstance = null
let modalCenterMarker = null
let modalPreviewCircle = null

// 위치 선택 관련
const selectedLocationMethod = ref('search')
const locationQuery = ref('')
const locationSearchResults = ref([])
const showLocationResults = ref(false)
const selectedLocationData = ref(null)
const isLoadingPatientLocation = ref(false)
let modalPlacesService = null

// 반경 설정
const modalRadiusLevel = ref(1)

const modalRadiusSettings = {
  1: 30,
  2: 60,
  3: 100
}

// 모달 열림 시 지도 초기화 (변수 선언 이후에 watch 설정)
watch(showBasicSafeZoneModal, async (isOpen) => {
  if (isOpen) {
    await nextTick()
    if (modalMapEl.value) {
      if (!modalMapInstance) {
        await initModalMap()
      }
      // 기존 안심존 정보 로드
      await loadExistingBasicSafeZone()
    }
  }
})

// 모달 열기
function openBasicSafeZoneModal() {
  if (!isPatientConnected.value) {
    alert('환자 연결이 필요합니다.')
    return
  }
  
  showBasicSafeZoneModal.value = true
  
  // Kakao Places 서비스 초기화
  ensureModalKakaoPlaces()
}

// 모달 닫기
function closeBasicSafeZoneModal() {
  showBasicSafeZoneModal.value = false
  selectedLocationData.value = null
  locationQuery.value = ''
  locationSearchResults.value = []
  showLocationResults.value = false
  modalRadiusLevel.value = 1
  selectedLocationMethod.value = 'search'
  
  // 모달 지도 정리
  if (modalMapInstance) {
    if (modalCenterMarker) {
      modalCenterMarker.setMap(null)
      modalCenterMarker = null
    }
    if (modalPreviewCircle) {
      modalPreviewCircle.setMap(null)
      modalPreviewCircle = null
    }
    modalMapInstance = null
  }
}

// 모달 지도 초기화
async function initModalMap() {
  if (!modalMapEl.value) return
  
  // Kakao Maps API가 로드되지 않은 경우
  if (!window.kakao || !window.kakao.maps) {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'}&libraries=services&autoload=false`
    document.head.appendChild(script)
    await new Promise((resolve, reject) => {
      script.onload = () => {
        window.kakao.maps.load(() => {
          createModalMap()
          resolve()
        })
      }
      script.onerror = () => {
        reject(new Error('Kakao Maps 스크립트 로드 실패'))
      }
    })
  } else {
    // 이미 로드된 경우에도 load() 완료를 기다림
    await new Promise((resolve) => {
      if (window.kakao.maps.load) {
        window.kakao.maps.load(() => {
          createModalMap()
          resolve()
        })
      } else {
        // 이미 완전히 로드된 경우
        createModalMap()
        resolve()
      }
    })
  }
}

function createModalMap() {
  if (!modalMapEl.value) return
  
  const defaultCenter = selectedLocationData.value 
    ? new window.kakao.maps.LatLng(selectedLocationData.value.latitude, selectedLocationData.value.longitude)
    : new window.kakao.maps.LatLng(37.4943524920695, 126.88767655688868)
  
  modalMapInstance = new window.kakao.maps.Map(modalMapEl.value, {
    center: defaultCenter,
    level: 5
  })
  
  // 선택된 위치가 있으면 표시
  if (selectedLocationData.value) {
    updateModalMapWithLocation(selectedLocationData.value)
  }
}

// Kakao Places 서비스 초기화
function ensureModalKakaoPlaces() {
  if (window.kakao && window.kakao.maps && window.kakao.maps.services) {
    modalPlacesService = new window.kakao.maps.services.Places()
    return
  }
  
  // 스크립트가 이미 추가되어 있는지 확인
  const existingScript = document.querySelector('script[src*="dapi.kakao.com/v2/maps/sdk.js"]')
  if (existingScript) {
    // 스크립트가 이미 있으면 로드 완료를 기다림
    if (window.kakao && window.kakao.maps && window.kakao.maps.load) {
      window.kakao.maps.load(() => {
        if (window.kakao.maps.services) {
          modalPlacesService = new window.kakao.maps.services.Places()
        }
      })
    }
    return
  }
  
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'}&libraries=services&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      modalPlacesService = new window.kakao.maps.services.Places()
    })
  }
}

// 위치 검색
function searchLocation() {
  if (!locationQuery.value || !modalPlacesService) return
  
  modalPlacesService.keywordSearch(locationQuery.value, (data, status) => {
    if (status !== window.kakao.maps.services.Status.OK) {
      locationSearchResults.value = []
      showLocationResults.value = false
      return
    }
    locationSearchResults.value = (data || []).slice(0, 10)
    showLocationResults.value = true
  })
}

// 검색어 입력 시
function onLocationQueryInput() {
  if (!locationQuery.value) {
    showLocationResults.value = false
    locationSearchResults.value = []
  }
}

// 검색 결과에서 위치 선택
function selectLocationFromSearch(place) {
  const locationData = {
    name: place.place_name,
    address: place.road_address_name || place.address_name,
    latitude: parseFloat(place.y),
    longitude: parseFloat(place.x)
  }
  
  selectedLocationData.value = locationData
  locationQuery.value = place.place_name
  showLocationResults.value = false
  locationSearchResults.value = []
  
  // 지도에 위치 표시
  updateModalMapWithLocation(locationData)
}

// 환자 현위치 사용 (모달용)
async function usePatientLocationForModal() {
  if (isLoadingPatientLocation.value || !patientUserNo.value) return
  
  isLoadingPatientLocation.value = true
  
  try {
    const response = await fetch(`/api/location/patient/${patientUserNo.value}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      alert('환자의 현재 위치를 확인할 수 없습니다.')
      return
    }
    
    const location = await response.json()
    
    if (!location || !location.latitude || !location.longitude) {
      alert('환자의 현재 위치를 확인할 수 없습니다.')
      return
    }
    
    // 역지오코딩으로 주소 변환
    const addressInfo = await reverseGeocode(location.latitude, location.longitude)
    
    const locationData = {
      name: addressInfo.name,
      address: addressInfo.address,
      latitude: location.latitude,
      longitude: location.longitude
    }
    
    selectedLocationData.value = locationData
    locationQuery.value = addressInfo.name
    
    // 지도에 위치 표시
    updateModalMapWithLocation(locationData)
    
  } catch (error) {
    console.error('환자 위치 조회 오류:', error)
    alert('환자의 현재 위치를 확인할 수 없습니다.')
  } finally {
    isLoadingPatientLocation.value = false
  }
}

// 역지오코딩
function reverseGeocode(latitude, longitude) {
  return new Promise((resolve) => {
    if (!window.kakao || !window.kakao.maps || !window.kakao.maps.services) {
      const fallbackName = `위도: ${latitude.toFixed(6)}, 경도: ${longitude.toFixed(6)}`
      resolve({
        name: fallbackName,
        address: fallbackName
      })
      return
    }
    
    const geocoder = new window.kakao.maps.services.Geocoder()
    geocoder.coord2Address(longitude, latitude, (result, status) => {
      if (status === window.kakao.maps.services.Status.OK && result && result[0]) {
        const address = result[0].address
        const addressName = address.address_name || `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`
        resolve({
          name: addressName,
          address: addressName
        })
      } else {
        const fallbackName = `위도: ${latitude.toFixed(6)}, 경도: ${longitude.toFixed(6)}`
        resolve({
          name: fallbackName,
          address: fallbackName
        })
      }
    })
  })
}

// 지도에 위치 및 반경 표시
function updateModalMapWithLocation(locationData) {
  if (!modalMapInstance || !locationData) return
  
  // 기존 마커 제거
  if (modalCenterMarker) {
    modalCenterMarker.setMap(null)
  }
  
  // 중심 마커 추가
  const markerPosition = new window.kakao.maps.LatLng(locationData.latitude, locationData.longitude)
  modalCenterMarker = new window.kakao.maps.Marker({
    position: markerPosition,
    map: modalMapInstance
  })
  
  // 인포윈도우
  const infowindow = new window.kakao.maps.InfoWindow({
    content: `<div style="padding:8px 12px;font-size:13px;font-weight:600;color:#111827;">${locationData.name}</div>`
  })
  infowindow.open(modalMapInstance, modalCenterMarker)
  
  // 지도 중심 이동
  modalMapInstance.setCenter(markerPosition)
  
  // 반경 원 표시
  updateModalRadiusCircle()
}

// 반경 선택
function selectModalRadius(level) {
  modalRadiusLevel.value = level
  updateModalRadiusCircle()
}

// 모달 지도에 반경 원 업데이트
function updateModalRadiusCircle() {
  if (!modalMapInstance || !selectedLocationData.value) return
  
  // 기존 원 제거
  if (modalPreviewCircle) {
    modalPreviewCircle.setMap(null)
  }
  
  const radius = modalRadiusSettings[modalRadiusLevel.value]
  const center = [selectedLocationData.value.longitude, selectedLocationData.value.latitude]
  const options = { steps: 64, units: 'meters' }
  const circleGeoJSON = circle(center, radius, options)
  
  const coordinates = circleGeoJSON.geometry.coordinates[0]
  const kakaoPath = coordinates.map(coord => 
    new window.kakao.maps.LatLng(coord[1], coord[0])
  )
  
  modalPreviewCircle = new window.kakao.maps.Polygon({
    path: kakaoPath,
    strokeWeight: 3,
    strokeColor: '#6366f1',
    strokeOpacity: 0.8,
    fillColor: '#6366f1',
    fillOpacity: 0.2
  })
  
  modalPreviewCircle.setMap(modalMapInstance)
  
  // 지도 레벨 조정
  const bounds = new window.kakao.maps.LatLngBounds()
  kakaoPath.forEach(point => bounds.extend(point))
  modalMapInstance.setBounds(bounds)
}

// 기존 기본 안심존 로드
async function loadExistingBasicSafeZone() {
  if (!patientUserNo.value) return
  
  try {
    const basicSafeZone = await fetchBasicSafeZone(patientUserNo.value)
    if (basicSafeZone && basicSafeZone.center) {
      selectedLocationData.value = {
        name: basicSafeZone.locationName || '기존 설정 위치',
        address: basicSafeZone.locationAddress || basicSafeZone.address || '기존 안심존 위치',
        latitude: basicSafeZone.center.lat,
        longitude: basicSafeZone.center.lng
      }
      modalRadiusLevel.value = basicSafeZone.level || 1
      
      // 지도 업데이트
      if (modalMapInstance) {
        updateModalMapWithLocation(selectedLocationData.value)
      }
    }
  } catch (error) {
    console.error('기존 기본 안심존 로드 오류:', error)
  }
}

// 기본 안심존 저장
async function saveBasicSafeZone() {
  if (!selectedLocationData.value || !patientUserNo.value) {
    alert('위치를 선택해주세요.')
    return
  }
  
  try {
    const radius = modalRadiusSettings[modalRadiusLevel.value]
    
    const boundaryData = {
      type: 'Circle',
      center: {
        lat: selectedLocationData.value.latitude,
        lng: selectedLocationData.value.longitude
      },
      radius: radius,
      level: modalRadiusLevel.value,
      locationName: selectedLocationData.value.name,
      locationAddress: selectedLocationData.value.address
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
    
    // 모달 닫기
    closeBasicSafeZoneModal()
    
    // 메인 지도 안심존 업데이트
    await updateSafeZone(mapInstance.value)
    checkPatientInSafeZone()
    
    alert('기본 안심존이 성공적으로 설정되었습니다.')
    
  } catch (error) {
    console.error('기본 안심존 저장 오류:', error)
    alert(error.message || '기본 안심존 저장 중 오류가 발생했습니다.')
  }
}

/* ===== 초기화 ===== */
onMounted(async () => {
  const saved = localStorage.getItem('safeZoneEnabled')
  if (saved !== null) {
    isSafeZoneEnabled.value = JSON.parse(saved)
  }
  
  await loadScheduleData()
  
  try {
    await nextTick()
    await initMap()
    
    await updateSafeZone(mapInstance.value)
    
    startPatientLocationTracking()
    
    checkPatientInSafeZone()
    
  } catch (e) {
    console.error(e)
  }
})

onBeforeUnmount(() => {
  stopPatientLocationTracking()
  if (previewSafeZone) {
    previewSafeZone.setMap(null)
  }
})
</script>

<style scoped>
.desktop-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  color: #111827;
  background: transparent;
}

.main-split {
  display: flex;
  flex: 1;
  gap: 16px;
  min-height: 0;
}

.map-column {
  flex: 1 1 60%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.map-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.create-zone-btn {
  height: 32px;
  border-radius: 16px;
  border: 0;
  background: #6366f1;
  color: #ffffff;
  font-weight: 600;
  font-size: 13px;
  padding: 0 16px;
  cursor: pointer;
  transition: filter 0.2s ease;
}

.create-zone-btn:hover {
  filter: brightness(0.95);
}

.map-surface {
  flex: 1;
  min-height: 400px;
  max-height: calc(100vh - 48px - 32px - 80px);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  background: #e5e7eb;
  position: relative;
}

.map-view {
  width: 100%;
  height: 100%;
}

.info-column {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex-shrink: 0;
  overflow: hidden;
  height: 100%;
  align-self: stretch;
}

.panel {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  flex-shrink: 0;
}

.patient-card {
  flex: 1 1 50%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.schedule-panel {
  flex: 1 1 50%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.panel-action {
  border: 0;
  background: transparent;
  color: #6366f1;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.more-btn {
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid rgba(99, 102, 241, 0.3);
  background: transparent;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s;
}

.more-btn:hover {
  background: rgba(99, 102, 241, 0.08);
}

.today-schedule-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.today-schedule-item {
  padding: 14px;
  border-radius: 12px;
  background: rgba(238, 242, 255, 0.5);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.today-schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.today-schedule-time {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}

.today-schedule-status {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
}

.today-schedule-status.waiting {
  background: #fef3c7;
  color: #b45309;
}

.today-schedule-status.active {
  background: #d1fae5;
  color: #047857;
}

.today-schedule-status.finished {
  background: #e5e7eb;
  color: #4b5563;
}

.today-schedule-title {
  font-size: 15px;
  color: #111827;
  font-weight: 700;
}

.today-schedule-route {
  font-size: 12px;
  color: #6366f1;
  font-weight: 500;
  word-break: break-word;
}

.today-schedule-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 13px;
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
}

.patient-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  overflow: hidden;
}

.patient-avatar {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: #eef2ff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.patient-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 16px;
  display: block;
}

.patient-avatar .avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  line-height: 1;
}

.patient-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #4b5563;
  word-break: break-word;
  overflow-wrap: break-word;
}

.patient-meta strong {
  font-size: 16px;
  color: #111827;
  word-break: break-word;
  overflow-wrap: break-word;
}

.patient-meta span {
  font-size: 14px;
  word-break: break-word;
  overflow-wrap: break-word;
}

.patient-stats {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 25px;
  /* 고정 스크롤 제거 */
  overflow: visible;
  min-height: 0;
}

.patient-stats li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  line-height: 1.4;
  color: #4b5563;
  padding: 6px 0;
  word-break: break-word;
  overflow-wrap: break-word;
  flex-shrink: 0;
  min-height: 20px;
  height: 20px;
}

.patient-stats .label {
  flex-shrink: 0;
  margin-right: 8px;
  line-height: 1.4;
  height: 20px;
  display: flex;
  align-items: center;
}

.patient-stats .value {
  font-weight: 600;
  color: #111827;
  text-align: right;
  word-break: break-word;
  overflow-wrap: break-word;
  flex: 1;
  min-width: 0;
  line-height: 1.4;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.patient-stats .value.alert {
  color: #ef4444;
}
.patient-stats .value.inside {
  color: #3b82f6;
}

/* 데스크탑용 지도 컨트롤 위치 조정 */
.map-surface :deep(.map-controls-left) {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 100;
}

.map-surface :deep(.map-controls-location-group) {
  position: absolute;
  right: 20px;
  bottom: 20px;
  z-index: 100;
}

@media (max-width: 1440px) {
  .map-surface {
    min-height: 380px;
  }

  .info-column {
    width: 300px;
  }
}

/* 기본 안심존 변경 모달 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.basic-safe-zone-modal {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 1000px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.basic-safe-zone-modal .modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.basic-safe-zone-modal .modal-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.basic-safe-zone-modal .close-btn {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.basic-safe-zone-modal .close-btn:hover {
  background: #f3f4f6;
  color: #111827;
}

.basic-safe-zone-modal .modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.modal-content-split {
  display: flex;
  gap: 24px;
  height: 100%;
  min-height: 500px;
}

.modal-map-section {
  flex: 1 1 55%;
  min-width: 0;
  border-radius: 12px;
  overflow: hidden;
  background: #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.modal-map-view {
  width: 100%;
  height: 100%;
  min-height: 500px;
}

.modal-control-section {
  flex: 1 1 45%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
  overflow-y: auto;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.control-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.control-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* 방법 선택 탭 */
.method-tabs {
  display: flex;
  gap: 8px;
  background: #f3f4f6;
  padding: 4px;
  border-radius: 8px;
}

.method-tab {
  flex: 1;
  padding: 10px 16px;
  border: none;
  background: transparent;
  color: #6b7280;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.method-tab:hover {
  background: rgba(255, 255, 255, 0.5);
}

.method-tab.active {
  background: #ffffff;
  color: #6366f1;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 검색 입력 */
.search-input-wrapper {
  display: flex;
  gap: 8px;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-btn {
  padding: 12px 20px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.search-btn:hover {
  background: #4f46e5;
}

/* 환자 현위치 버튼 */
.current-location-wrapper {
  width: 100%;
}

.current-location-btn {
  width: 100%;
  padding: 12px 16px;
  background: #16a34a;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.current-location-btn:hover:not(:disabled) {
  background: #15803d;
}

.current-location-btn:disabled {
  background: #e5e7eb;
  color: #6b7280;
  cursor: not-allowed;
}

/* 검색 결과 */
.search-results {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.search-result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background-color 0.2s;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background: #f9fafb;
}

.result-info {
  flex: 1;
  min-width: 0;
}

.result-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.result-address {
  font-size: 12px;
  color: #6b7280;
}

.select-location-btn {
  padding: 6px 16px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.select-location-btn:hover {
  background: #4f46e5;
}

/* 선택된 위치 표시 */
.selected-location-display {
  padding: 16px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
}

.selected-location-label {
  font-size: 12px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 8px;
}

.selected-location-name {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.selected-location-address {
  font-size: 14px;
  color: #6b7280;
}

/* 반경 선택 */
.radius-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.radius-btn {
  padding: 16px 12px;
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
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.radius-btn.active .radius-name {
  color: #6366f1;
}

.radius-desc {
  font-size: 13px;
  color: #6b7280;
}

.radius-btn.active .radius-desc {
  color: #4f46e5;
}

/* 액션 버튼 */
.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.save-btn,
.cancel-modal-btn {
  flex: 1;
  padding: 14px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.save-btn {
  background: #6366f1;
  color: #ffffff;
  border: none;
}

.save-btn:hover:not(:disabled) {
  background: #4f46e5;
}

.save-btn:disabled {
  background: #d1d5db;
  color: #9ca3af;
  cursor: not-allowed;
}

.cancel-modal-btn {
  background: #ffffff;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.cancel-modal-btn:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

@media (max-width: 1024px) {
  .modal-content-split {
    flex-direction: column;
    min-height: auto;
  }

  .modal-map-section {
    flex: 0 0 400px;
  }

  .modal-control-section {
    flex: 1;
  }

  .radius-options {
    grid-template-columns: 1fr;
  }
}
</style>


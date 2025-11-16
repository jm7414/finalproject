<template>
  <div class="page">

    <!-- 설정된 위치 카드 -->
    <div v-if="hasExistingLocation" class="location-preview">
      <div class="preview-header">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="#6366f1" stroke-width="2"/>
          <circle cx="12" cy="10" r="3" stroke="#6366f1" stroke-width="2"/>
        </svg>
        <span>설정된 위치</span>
        <button class="change-location-btn" @click="showMethodSelection">변경</button>
      </div>
      <div class="preview-content">
        <div class="preview-name">{{ selectedLocation.place_name }}</div>
        <div class="preview-address">{{ selectedLocation.road_address_name || selectedLocation.address_name }}</div>
      </div>
    </div>

    <!-- 기본 위치가 없는 경우 -->
    <div v-else class="no-location-card">
      <div class="no-location-header">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="#6b7280" stroke-width="2"/>
          <circle cx="12" cy="10" r="3" stroke="#6b7280" stroke-width="2"/>
        </svg>
        <span>설정된 위치</span>
        <button class="add-location-btn" @click="showMethodSelection">추가하기</button>
      </div>
      <div class="no-location-content">
        <div class="no-location-message">설정된 기본 위치가 없습니다</div>
      </div>
    </div>

    <!-- 위치 설정 방법 선택 -->
    <div v-if="showMethods" class="method-selection">
      <h3 class="method-title">위치 설정 방법을 선택하세요</h3>
      
      <!-- 방법 1: 직접 검색 -->
      <div class="method-card" :class="{ active: selectedMethod === 'search' }" @click="selectMethod('search')">
        <div class="method-header">
          <div class="method-icon search-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="2"/>
              <path d="m21 21-4.35-4.35" stroke="currentColor" stroke-width="2"/>
            </svg>
          </div>
          <div class="method-info">
            <h4 class="method-name">직접 검색</h4>
            <p class="method-desc">주소나 장소명을 입력하여 검색</p>
          </div>
        </div>
        
        <div v-if="selectedMethod === 'search'" class="method-content">
          <div class="input-item">
            <input
              v-model="locationQuery"
              type="text"
              placeholder="예) 서울시 강남구 역삼동"
              @focus="showResults = true"
              @keyup.enter="searchLocation"
            />
          </div>
        </div>
      </div>

      <!-- 방법 2: 환자 현위치 -->
      <div class="method-card" :class="{ active: selectedMethod === 'current' }" @click="selectMethod('current')">
        <div class="method-header">
          <div class="method-icon current-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <circle cx="12" cy="12" r="3" fill="currentColor"/>
            </svg>
          </div>
          <div class="method-info">
            <h4 class="method-name">{{ patientName || '환자' }}님의 현 위치</h4>
            <p class="method-desc">환자의 마지막 위치를 기준으로 설정</p>
          </div>
        </div>
        
        <div v-if="selectedMethod === 'current'" class="method-content">
          <button class="current-location-btn" @click="usePatientLocation" :disabled="isLoadingLocation">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <circle cx="12" cy="12" r="3" fill="currentColor"/>
            </svg>
            {{ isLoadingLocation ? '위치 조회 중...' : '현 위치로 설정' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 장소 검색 결과 -->
    <div class="section-title" v-if="showResults && searchResults.length > 0">장소 결과</div>
    <ul class="results" v-show="showResults && searchResults.length > 0">
      <li v-for="place in searchResults" :key="place.id" @click="selectLocation(place)">
        <div class="item-left">
          <div class="name">{{ place.place_name }}</div>
          <div class="addr">{{ place.road_address_name || place.address_name }}</div>
        </div>
        <div class="item-right">
          <button class="select-btn">선택</button>
        </div>
      </li>
    </ul>

    <!-- 설명 -->
    <div class="description-card">
      <p class="description-text">
        <strong>{{ isEditMode ? '기본 안심존 수정' : '기본 안심존 생성' }}</strong><br/>
        환자가 평소 머무는 위치를 설정하여 기본 안심존을 만듭니다.<br/>
        일정이 없을 때 이 안심존이 활성화됩니다.
      </p>
    </div>

    <!-- 액션 버튼 -->
    <div class="actions">
      <button class="primary" :disabled="!selectedLocation" @click="goToRadiusSetting">다음</button>
      <button class="ghost" @click="goBack">취소</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
const locationQuery = ref('')
const searchResults = ref([])
const selectedLocation = ref(null)
const showResults = ref(false)
const isEditMode = ref(false)
const isLoadingLocation = ref(false)
const patientName = ref('')
const selectedMethod = ref('')
const showMethods = ref(false)
const hasExistingLocation = ref(false)

let placesService = null

onMounted(async () => {
  ensureKakaoPlaces()
  
  // 환자 정보 가져오기
  const patient = await fetchPatientInfo()
  
  // 기존 기본 안심존 조회 (환자 정보가 있을 때만)
  if (patient) {
    await checkExistingBasicSafeZone(patient)
  }
})

// 환자 정보 가져오기
async function fetchPatientInfo() {
  try {
    const response = await fetch(`/api/user/my-patient`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      console.error('환자 정보 조회 실패')
      return null
    }
    
    const patient = await response.json()
    if (patient.message) {
      console.warn('관리하는 환자가 없습니다.')
      return null
    }
    
    patientName.value = patient.name || '환자'
    return patient
  } catch (error) {
    console.error('환자 정보 조회 오류:', error)
    return null
  }
}

// 기존 기본 안심존 확인
async function checkExistingBasicSafeZone(patient) {
  try {
    if (!patient) {
      hasExistingLocation.value = false
      return
    }
    
    // 기본 안심존 조회
    const zoneResponse = await fetch(`/api/schedule/basic-safe-zone/${patient.userNo}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!zoneResponse.ok) {
      console.error('기본 안심존 조회 실패')
      return
    }
    
    const zoneData = await zoneResponse.json()
    
    // 기존 안심존이 있으면 수정 모드
    if (zoneData.safeZoneNo) {
      isEditMode.value = true
      hasExistingLocation.value = true
      
      // 기존 데이터를 sessionStorage에 저장
      sessionStorage.setItem('existingBasicSafeZone', JSON.stringify(zoneData))
      
      // 기존 위치 정보 파싱 및 표시
      try {
        const boundary = JSON.parse(zoneData.boundaryCoordinates)
        if (boundary.center) {
          selectedLocation.value = {
            place_name: boundary.locationName || '기존 설정 위치',
            address_name: boundary.address || boundary.locationName || '기존 안심존 위치',
            road_address_name: boundary.roadAddress || boundary.address || boundary.locationName,
            x: boundary.center.lng,
            y: boundary.center.lat
          }
        }
      } catch (e) {
        console.error('기존 안심존 데이터 파싱 오류:', e)
      }
    } else {
      hasExistingLocation.value = false
    }
  } catch (error) {
    console.error('기본 안심존 확인 오류:', error)
    hasExistingLocation.value = false
  }
}

// 환자 위치로 설정하기
async function usePatientLocation() {
  if (isLoadingLocation.value) return
  
  isLoadingLocation.value = true
  
  try {
    // 환자 정보 가져오기
    const patient = await fetchPatientInfo()
    if (!patient) {
      alert(`${patientName.value}님의 현재 위치를 확인할 수 없습니다.`)
      return
    }
    
    // 환자 위치 조회
    const response = await fetch(`/api/location/patient/${patient.userNo}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      alert(`${patientName.value}님의 현재 위치를 확인할 수 없습니다.`)
      return
    }
    
    const location = await response.json()
    
    // [시연용] 고정 좌표로 덮어쓰기
    const fixedLocation = {
      ...location,
      latitude: 37.244364,
      longitude: 126.876748
    }
    
    if (!fixedLocation || !fixedLocation.latitude || !fixedLocation.longitude) {
      alert(`${patientName.value}님의 현재 위치를 확인할 수 없습니다.`)
      return
    }
    
    // 역지오코딩으로 주소 변환
    const addressInfo = await reverseGeocode(fixedLocation.latitude, fixedLocation.longitude)
    
    // 위치 정보를 sessionStorage에 저장
    sessionStorage.setItem('basicSafeZoneLocation', JSON.stringify({
      name: addressInfo.name,
      address: addressInfo.address,
      latitude: fixedLocation.latitude,
      longitude: fixedLocation.longitude
    }))
    
    // 위치 선택 완료 처리
    onLocationSelected()
    
    // 반경 설정 페이지로 이동
    router.push({ name: 'basic-safe-zone-radius' })
    
  } catch (error) {
    console.error('환자 위치 조회 오류:', error)
    alert(`${patientName.value}님의 현재 위치를 확인할 수 없습니다.`)
  } finally {
    isLoadingLocation.value = false
  }
}

// 역지오코딩 함수
function reverseGeocode(latitude, longitude) {
  return new Promise((resolve) => {
    if (!window.kakao || !window.kakao.maps || !window.kakao.maps.services) {
      // 카카오맵이 로드되지 않은 경우 기본값 반환
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
        // 실패 시 기본값
        const fallbackName = `위도: ${latitude.toFixed(6)}, 경도: ${longitude.toFixed(6)}`
        resolve({
          name: fallbackName,
          address: fallbackName
        })
      }
    })
  })
}

function ensureKakaoPlaces() {
  if (window.kakao && window.kakao.maps && window.kakao.maps.services) {
    placesService = new window.kakao.maps.services.Places()
    return
  }
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      placesService = new window.kakao.maps.services.Places()
    })
  }
}

function searchLocation() {
  if (!locationQuery.value || !placesService) return

  placesService.keywordSearch(locationQuery.value, (data, status) => {
    if (status !== window.kakao.maps.services.Status.OK) {
      searchResults.value = []
      return
    }
    searchResults.value = data.slice(0, 10)
    showResults.value = true
  })
}

// 방법 선택 표시
function showMethodSelection() {
  showMethods.value = true
  selectedMethod.value = ''
  locationQuery.value = ''
  showResults.value = false
  searchResults.value = []
}

// 방법 선택
function selectMethod(method) {
  selectedMethod.value = method
  if (method === 'search') {
    showResults.value = false
    searchResults.value = []
    // 직접 검색 선택 시 현재 주소 지우고 예시 문구만 표시
    locationQuery.value = ''
  }
}

// 위치 선택
function selectLocation(place) {
  selectedLocation.value = place
  locationQuery.value = place.place_name
  showResults.value = false
  searchResults.value = []
  onLocationSelected()
}

// 위치 선택 완료 후 처리
function onLocationSelected() {
  showMethods.value = false
  selectedMethod.value = ''
  hasExistingLocation.value = true
}

function goToRadiusSetting() {
  if (!selectedLocation.value) {
    alert('위치를 선택해주세요.')
    return
  }

  // 선택된 위치를 sessionStorage에 저장
  sessionStorage.setItem('basicSafeZoneLocation', JSON.stringify({
    name: selectedLocation.value.place_name,
    address: selectedLocation.value.road_address_name || selectedLocation.value.address_name,
    latitude: parseFloat(selectedLocation.value.y),
    longitude: parseFloat(selectedLocation.value.x)
  }))

  // 위치 선택 완료 처리
  onLocationSelected()

  // 반경 설정 페이지로 이동
  router.push({ name: 'basic-safe-zone-radius' })
}

function goBack() {
  // sessionStorage 정리
  sessionStorage.removeItem('basicSafeZoneLocation')
  sessionStorage.removeItem('existingBasicSafeZone')
  router.go(-1)
}
</script>

<style scoped>
.page {
  height: 100%;
  background: #f9fafb;
  padding: 20px 0 20px 0;
  position: relative;
  z-index: 1;
  overflow-y: auto;
  overflow-x: hidden;
}


/* 설명 카드 */
.description-card {
  margin: 20px;
  padding: 16px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  position: relative;
  z-index: 2;
}

.description-text {
  font-size: 14px;
  color: #1e40af;
  line-height: 1.6;
  margin: 0;
}

/* 카드 */
.card {
  margin: 20px;
  padding: 20px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.input-item {
  margin-bottom: 16px;
}

.label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s;
  box-sizing: border-box;
}

input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

/* 방법 선택 섹션 */
.method-selection {
  margin: 20px;
  position: relative;
  z-index: 2;
}

.method-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 16px 0;
}

.method-card {
  background: #ffffff;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
}

.method-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.method-card.active {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.method-header {
  display: flex;
  align-items: center;
  padding: 16px;
  gap: 12px;
}

.method-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.search-icon {
  background: #f0f9ff;
  color: #0369a1;
}

.current-icon {
  background: #f0fdf4;
  color: #16a34a;
}

.method-info {
  flex: 1;
}

.method-name {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
}

.method-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.method-content {
  padding: 0 16px 16px 16px;
}

.current-location-btn {
  width: 100%;
  padding: 12px 16px;
  background: #16a34a;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
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

/* 섹션 타이틀 */
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin: 0 20px 12px 20px;
}

/* 검색 결과 */
.results {
  list-style: none;
  padding: 0;
  margin: 0 20px 20px 20px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.results li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background-color 0.2s;
}

.results li:last-child {
  border-bottom: none;
}

.results li:hover {
  background: #f9fafb;
}

.item-left {
  flex: 1;
}

.name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.addr {
  font-size: 13px;
  color: #6b7280;
}

.select-btn {
  padding: 6px 16px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.select-btn:hover {
  background: #4f46e5;
}

/* 위치 미리보기 */
.location-preview {
  margin: 20px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  position: relative;
  z-index: 2;
}

/* 기본 위치가 없는 경우 */
.no-location-card {
  margin: 20px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  position: relative;
  z-index: 2;
}

.no-location-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.no-location-header > div {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
}

.add-location-btn {
  padding: 4px 12px;
  background: #16a34a;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.add-location-btn:hover {
  background: #15803d;
}

.no-location-content {
  padding-left: 28px;
}

.no-location-message {
  font-size: 14px;
  color: #6b7280;
  font-style: italic;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.preview-header > div {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
}

.change-location-btn {
  padding: 4px 12px;
  background: #ffffff;
  color: #6366f1;
  border: 1px solid #6366f1;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.change-location-btn:hover {
  background: #6366f1;
  color: #ffffff;
}

.preview-content {
  padding-left: 28px;
}

.preview-name {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.preview-address {
  font-size: 14px;
  color: #6b7280;
}


/* 액션 버튼 */
.actions {
  margin: 20px;
  padding: 16px 20px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  gap: 12px;
  margin-bottom: 20px; /* 적절한 여백만 */
}

.primary, .ghost {
  flex: 1;
  padding: 14px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.primary {
  background: #6366f1;
  color: #ffffff;
  border: none;
}

.primary:hover:not(:disabled) {
  background: #4f46e5;
}

.primary:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.ghost {
  background: #ffffff;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.ghost:hover {
  background: #f9fafb;
}
</style>


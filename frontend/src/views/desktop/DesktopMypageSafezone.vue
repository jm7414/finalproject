<template>
  <div class="desktop-mypage-safezone">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">기본 안심존 설정</h1>
        <p class="page-subtitle">환자가 평소 머무는 위치를 설정하여 기본 안심존을 만듭니다</p>
      </div>

      <!-- 설정된 위치 카드 -->
      <div v-if="hasExistingLocation" class="location-card">
        <div class="card-header">
          <div class="header-left">
            <i class="bi bi-geo-alt-fill"></i>
            <span>설정된 위치</span>
          </div>
          <button class="btn-change" @click="showMethodSelection">변경</button>
        </div>
        <div class="card-body">
          <div class="location-name">{{ selectedLocation.place_name }}</div>
          <div class="location-address">{{ selectedLocation.road_address_name || selectedLocation.address_name }}</div>
        </div>
      </div>

      <!-- 기본 위치가 없는 경우 -->
      <div v-else class="location-card empty">
        <div class="card-header">
          <div class="header-left">
            <i class="bi bi-geo-alt"></i>
            <span>설정된 위치</span>
          </div>
          <button class="btn-add" @click="showMethodSelection">추가하기</button>
        </div>
        <div class="card-body">
          <div class="empty-message">설정된 기본 위치가 없습니다</div>
        </div>
      </div>

      <!-- 위치 설정 방법 선택 -->
      <div v-if="showMethods" class="methods-section">
        <h3 class="methods-title">위치 설정 방법을 선택하세요</h3>
        
        <!-- 방법 1: 직접 검색 -->
        <div class="method-card" :class="{ active: selectedMethod === 'search' }" @click="selectMethod('search')">
          <div class="method-header">
            <div class="method-icon search">
              <i class="bi bi-search"></i>
            </div>
            <div class="method-info">
              <h4 class="method-name">직접 검색</h4>
              <p class="method-desc">주소나 장소명을 입력하여 검색</p>
            </div>
          </div>
          
          <div v-if="selectedMethod === 'search'" class="method-content">
            <div class="search-input-wrapper">
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
            <div class="method-icon current">
              <i class="bi bi-geo-alt-fill"></i>
            </div>
            <div class="method-info">
              <h4 class="method-name">{{ patientName || '환자' }}님의 현 위치</h4>
              <p class="method-desc">환자의 마지막 위치를 기준으로 설정</p>
            </div>
          </div>
          
          <div v-if="selectedMethod === 'current'" class="method-content">
            <button class="btn-current-location" @click="usePatientLocation" :disabled="isLoadingLocation">
              <i class="bi bi-geo-alt-fill me-2"></i>
              {{ isLoadingLocation ? '위치 조회 중...' : '현 위치로 설정' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 장소 검색 결과 -->
      <div v-if="showResults && searchResults.length > 0" class="results-section">
        <h3 class="results-title">장소 결과</h3>
        <div class="results-list">
          <div 
            v-for="place in searchResults" 
            :key="place.id" 
            class="result-item"
            @click="selectLocation(place)"
          >
            <div class="result-info">
              <div class="result-name">{{ place.place_name }}</div>
              <div class="result-address">{{ place.road_address_name || place.address_name }}</div>
            </div>
            <button class="btn-select">선택</button>
          </div>
        </div>
      </div>

      <!-- 설명 카드 -->
      <div class="description-card">
        <p class="description-text">
          <strong>{{ isEditMode ? '기본 안심존 수정' : '기본 안심존 생성' }}</strong><br/>
          환자가 평소 머무는 위치를 설정하여 기본 안심존을 만듭니다.<br/>
          일정이 없을 때 이 안심존이 활성화됩니다.
        </p>
      </div>

      <!-- 액션 버튼 -->
      <div class="actions">
        <button class="btn-primary" :disabled="!selectedLocation" @click="goToRadiusSetting">다음</button>
        <button class="btn-ghost" @click="goBack">취소</button>
      </div>
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
  
  const patient = await fetchPatientInfo()
  
  if (patient) {
    await checkExistingBasicSafeZone(patient)
  }
})

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

async function checkExistingBasicSafeZone(patient) {
  try {
    if (!patient) {
      hasExistingLocation.value = false
      return
    }
    
    const zoneResponse = await fetch(`/api/schedule/basic-safe-zone/${patient.userNo}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!zoneResponse.ok) {
      console.error('기본 안심존 조회 실패')
      return
    }
    
    const zoneData = await zoneResponse.json()
    
    if (zoneData.safeZoneNo) {
      isEditMode.value = true
      hasExistingLocation.value = true
      
      sessionStorage.setItem('existingBasicSafeZone', JSON.stringify(zoneData))
      
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

async function usePatientLocation() {
  if (isLoadingLocation.value) return
  
  isLoadingLocation.value = true
  
  try {
    const patient = await fetchPatientInfo()
    if (!patient) {
      alert(`${patientName.value}님의 현재 위치를 확인할 수 없습니다.`)
      return
    }
    
    const response = await fetch(`/api/location/patient/${patient.userNo}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      alert(`${patientName.value}님의 현재 위치를 확인할 수 없습니다.`)
      return
    }
    
    const location = await response.json()
    
    if (!location || !location.latitude || !location.longitude) {
      alert(`${patientName.value}님의 현재 위치를 확인할 수 없습니다.`)
      return
    }
    
    const addressInfo = await reverseGeocode(location.latitude, location.longitude)
    
    sessionStorage.setItem('basicSafeZoneLocation', JSON.stringify({
      name: addressInfo.name,
      address: addressInfo.address,
      latitude: location.latitude,
      longitude: location.longitude
    }))
    
    onLocationSelected()
    router.push('/desktop/mypage/safezone/radius')
    
  } catch (error) {
    console.error('환자 위치 조회 오류:', error)
    alert(`${patientName.value}님의 현재 위치를 확인할 수 없습니다.`)
  } finally {
    isLoadingLocation.value = false
  }
}

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

function showMethodSelection() {
  showMethods.value = true
  selectedMethod.value = ''
  locationQuery.value = ''
  showResults.value = false
  searchResults.value = []
}

function selectMethod(method) {
  selectedMethod.value = method
  if (method === 'search') {
    showResults.value = false
    searchResults.value = []
    locationQuery.value = ''
  }
}

function selectLocation(place) {
  selectedLocation.value = place
  locationQuery.value = place.place_name
  showResults.value = false
  searchResults.value = []
  onLocationSelected()
}

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

  sessionStorage.setItem('basicSafeZoneLocation', JSON.stringify({
    name: selectedLocation.value.place_name,
    address: selectedLocation.value.road_address_name || selectedLocation.value.address_name,
    latitude: parseFloat(selectedLocation.value.y),
    longitude: parseFloat(selectedLocation.value.x)
  }))

  onLocationSelected()
  router.push('/desktop/mypage/safezone/radius')
}

function goBack() {
  sessionStorage.removeItem('basicSafeZoneLocation')
  sessionStorage.removeItem('existingBasicSafeZone')
  router.push('/desktop/mypage')
}
</script>

<style scoped>
.desktop-mypage-safezone {
  width: 100%;
  min-height: calc(100vh - 80px);
  padding: 0;
}

.page-container {
  max-width: 900px;
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

.location-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.location-card.empty {
  border: 2px dashed #d1d5db;
  background: #fafafa;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #475569;
}

.header-left i {
  font-size: 20px;
  color: #6366f1;
}

.btn-change,
.btn-add {
  padding: 6px 16px;
  background: #ffffff;
  color: #6366f1;
  border: 1px solid #6366f1;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-change:hover,
.btn-add:hover {
  background: #6366f1;
  color: #ffffff;
}

.btn-add {
  background: #16a34a;
  color: #ffffff;
  border-color: #16a34a;
}

.btn-add:hover {
  background: #15803d;
}

.card-body {
  padding: 24px;
}

.location-name {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
}

.location-address {
  font-size: 15px;
  color: #6b7280;
}

.empty-message {
  font-size: 15px;
  color: #6b7280;
  font-style: italic;
  padding-left: 28px;
}

.methods-section {
  margin-bottom: 24px;
}

.methods-title {
  font-size: 20px;
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
  padding: 20px;
  gap: 16px;
}

.method-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 24px;
}

.method-icon.search {
  background: #f0f9ff;
  color: #0369a1;
}

.method-icon.current {
  background: #f0fdf4;
  color: #16a34a;
}

.method-info {
  flex: 1;
}

.method-name {
  font-size: 18px;
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
  padding: 0 20px 20px 20px;
}

.search-input-wrapper input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s;
}

.search-input-wrapper input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.btn-current-location {
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

.btn-current-location:hover:not(:disabled) {
  background: #15803d;
}

.btn-current-location:disabled {
  background: #e5e7eb;
  color: #6b7280;
  cursor: not-allowed;
}

.results-section {
  margin-bottom: 24px;
}

.results-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px 0;
}

.results-list {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background-color 0.2s;
}

.result-item:last-child {
  border-bottom: none;
}

.result-item:hover {
  background: #f9fafb;
}

.result-info {
  flex: 1;
}

.result-name {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.result-address {
  font-size: 14px;
  color: #6b7280;
}

.btn-select {
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

.btn-select:hover {
  background: #4f46e5;
}

.description-card {
  margin: 24px 0;
  padding: 20px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
}

.description-text {
  font-size: 14px;
  color: #1e40af;
  line-height: 1.6;
  margin: 0;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
}

.btn-primary,
.btn-ghost {
  flex: 1;
  padding: 14px 24px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #6366f1;
  color: #ffffff;
  border: none;
}

.btn-primary:hover:not(:disabled) {
  background: #4f46e5;
}

.btn-primary:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.btn-ghost {
  background: #ffffff;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.btn-ghost:hover {
  background: #f9fafb;
}
</style>

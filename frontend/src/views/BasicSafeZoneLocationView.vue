<template>
  <div class="page">
    <!-- 헤더 -->
    <div class="header">
      <button class="back-btn" @click="goBack">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <h1 class="page-title">기본 안심존 설정</h1>
    </div>

    <!-- 설명 -->
    <div class="description-card">
      <p class="description-text">
        <strong>{{ isEditMode ? '기본 안심존 수정' : '기본 안심존 생성' }}</strong><br/>
        환자가 평소 머무는 위치를 설정하여 기본 안심존을 만듭니다.<br/>
        일정이 없을 때 이 안심존이 활성화됩니다.
      </p>
    </div>

    <!-- 장소 검색 카드 -->
    <div class="card">
      <div class="input-item">
        <div class="label">기본 위치</div>
        <input
          v-model="locationQuery"
          type="text"
          placeholder="예) 서울시 강남구 역삼동"
          @focus="showResults = true"
          @keyup.enter="searchLocation"
        />
      </div>

      <!-- 내 위치로 설정 버튼 (미구현) -->
      <button class="my-location-btn" disabled title="환자 위치 추적 기능 구현 후 사용 가능">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
          <circle cx="12" cy="12" r="3" fill="currentColor"/>
        </svg>
        내 위치로 설정 (준비 중)
      </button>
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

    <!-- 선택된 위치 표시 -->
    <div v-if="selectedLocation" class="selected-location-card">
      <div class="selected-header">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="#6366f1" stroke-width="2"/>
          <circle cx="12" cy="10" r="3" stroke="#6366f1" stroke-width="2"/>
        </svg>
        <span>선택된 위치</span>
      </div>
      <div class="selected-name">{{ selectedLocation.place_name }}</div>
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

let placesService = null

onMounted(async () => {
  ensureKakaoPlaces()
  
  // 기존 기본 안심존 조회
  await checkExistingBasicSafeZone()
})

// 기존 기본 안심존 확인
async function checkExistingBasicSafeZone() {
  try {
    // 환자 번호 가져오기
    const patientResponse = await fetch(`/api/user/my-patient`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!patientResponse.ok) {
      console.error('환자 정보 조회 실패')
      return
    }
    
    const patient = await patientResponse.json()
    if (patient.message) {
      console.warn('관리하는 환자가 없습니다.')
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
          locationQuery.value = selectedLocation.value.place_name
        }
      } catch (e) {
        console.error('기존 안심존 데이터 파싱 오류:', e)
      }
    }
  } catch (error) {
    console.error('기본 안심존 확인 오류:', error)
  }
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

function selectLocation(place) {
  selectedLocation.value = place
  locationQuery.value = place.place_name
  showResults.value = false
  searchResults.value = []
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

  // 반경 설정 페이지로 이동
  router.push({ name: 'basic-safe-zone-radius' })
}

function goBack() {
  // sessionStorage 정리
  sessionStorage.removeItem('basicSafeZoneLocation')
  sessionStorage.removeItem('existingBasicSafeZone')
  router.push({ name: 'gdmypage' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f9fafb;
  padding-bottom: 100px;
}

/* 헤더 */
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.back-btn {
  background: none;
  border: none;
  color: #111827;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background: #f3f4f6;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

/* 설명 카드 */
.description-card {
  margin: 20px;
  padding: 16px;
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

/* 내 위치 버튼 */
.my-location-btn {
  width: 100%;
  padding: 12px 16px;
  background: #e5e7eb;
  color: #6b7280;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
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

/* 선택된 위치 카드 */
.selected-location-card {
  margin: 20px;
  padding: 16px;
  background: #ffffff;
  border: 2px solid #6366f1;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
}

.selected-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #6366f1;
  margin-bottom: 12px;
}

.selected-name {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
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
  margin-bottom: 100px; /* 하단 네비게이션 여유 공간 */
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


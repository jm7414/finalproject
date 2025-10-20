<template>
  <div class="page-container">
    
    <!-- 지도 영역 -->
    <div ref="mapContainer" class="map-area"></div>
    
    <!-- 토글 버튼 영역 -->
    <div class="toggle-button-wrapper">
      <button class="toggle-button" :class="{ active: selectedType === 'info' }" @click="mapOrInfo('info')">
        <i class="bi bi-person-fill"></i>
        <span class="button-text">실종자 정보</span>
      </button>

      <button class="toggle-button" :class="{ active: selectedType === 'map' }" @click="mapOrInfo('map')">
        <i class="bi bi-eye-fill"></i>
        <span class="button-text">예상위치</span>
      </button>
    </div>

    <!-- 실종자 정보 헤더 -->
    <div class="info-header" v-if="selectedType === 'map'">
      <h2 class="info-title">실종자 정보</h2>
      <div class="info-meta">
        <span class="missing-date">실종일자: 2025-01-15 14:30</span>
        <span class="elapsed-info">실종 경과 후 <strong>{{ Math.floor(elapsedMinutes / 60) }}시간 {{ elapsedMinutes % 60 }}분
            경과</strong></span>
      </div>
    </div>

    <!-- 시간 선택 버튼 -->
    <div class="time-selector" v-if="selectedType === 'map'">
      <button class="time-button" :class="{ active: selectedTime === 30 }" @click="timeStamp(30)">
        <span class="time-text">실종~30분</span>
      </button>
      <button class="time-button" :class="{ active: selectedTime === 60 }" @click="timeStamp(60)">
        <span class="time-text">30분~60분</span>
      </button>
      <button class="time-button" :class="{ active: selectedTime === 90 }" @click="timeStamp(90)">
        <span class="time-text">60분~90분</span>
      </button>
      <button class="time-button" :class="{ active: selectedTime === 120 }" @click="timeStamp(120)">
        <span class="time-text">실종~90분</span>
      </button>
    </div>

    <!-- 컨텐츠 영역 -->
    <div class="content-section">
      <!-- 실종자 정보 -->
      <div v-if="selectedType === 'info'" class="missing-person-info">
        <!-- 상단: 프로필 이미지 + 기본 정보 -->
        <div class="info-header-section">
          <div class="profile-image-wrapper">
            <img class="profile-image" src="../assets/logo.svg" alt="실종자 사진" />
          </div>

          <div class="basic-info-wrapper">
            <div class="name-age-row">
              <h2 class="person-name">김○○ (78세)</h2>
            </div>
            <p class="age-info">{{ Math.floor(elapsedMinutes / 60) }}시간 전</p>
            <p class="missing-datetime">실종일시: {{ missingTime }}</p>
            <p class="missing-location">실종장소: 서울 강남구 역삼동</p>
          </div>
        </div>

        <!-- 세부 정보 섹션들 -->
        <div class="detail-sections">
          <!-- 신체 특징 -->
          <div class="info-item">
            <div class="info-badge">
              <span class="badge-label">신체 특징</span>
            </div>
            <span class="info-content">키 160cm, 지적장애</span>
          </div>

          <!-- 착의사항 -->
          <div class="info-item">
            <div class="info-badge">
              <span class="badge-label">착의사항</span>
            </div>
            <span class="info-content">회색 티셔츠, 흰색 운동화</span>
          </div>

          <!-- 함께하는 이웃 -->
          <div class="info-item">
            <div class="info-badge">
              <span class="badge-label">함께하는 이웃</span>
            </div>
            <span class="info-content">3명</span>
          </div>

          <!-- 특이사항 -->
          <div class="info-item">
            <div class="info-badge">
              <span class="badge-label">특이사항</span>
            </div>
            <span class="info-content">키 160cm, 지적장애</span>
          </div>
        </div>
      </div>

      <!-- 예상 위치 카드 리스트 -->
      <div v-if="selectedType === 'map'" class="prediction-list">
        <div class="prediction-card" v-for="(loc, index) in location" :key="index">
          <div class="card-icon-wrapper">
            <div class="location-icon">
              <i class="bi bi-geo-alt-fill"></i>
            </div>
          </div>

          <div class="card-content">
            <div class="location-header">
              <h4 class="location-name">{{ loc.name }}</h4>
              <span class="probability-badge">높은 확률</span>
            </div>
            <p class="location-distance">{{ loc.time }}</p>
            <div class="action-button-wrapper">
              <button class="view-probability-btn">높은 확률</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const user_no = ref('')
const selectedTime = ref(30)
const selectedType = ref('info')
const missingTime = ref('2025-10-17 08:40')

const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
const mapContainer = ref(null)
let mapInstance = null
let geocoder = null

const fullLocation = ref(null)
const location = [
  {
    name: '서울시 양천구 목1동',
    time: '도보 10분 거리',
    prob: '높은 확률'
  },
  {
    name: '서울시 양천구 신월동',
    time: '도보 10분 거리',
    prob: '높은 확률'
  },
  {
    name: '서울시 양천구 신월동',
    time: '도보 10분 거리',
    prob: '높은 확률'
  }
]

// 시간 계산 함수
function parseDate(str) {
  const [datePart, timePart] = str.split(' ')
  const [y, m, d] = datePart.split('-').map(Number)
  const [h, min] = timePart.split(':').map(Number)
  return new Date(y, m - 1, d, h, min)
}

const now = new Date()
const start = parseDate(missingTime.value)
const diffMs = now - start
const elapsedMinutes = Math.floor(diffMs / (1000 * 60))

onMounted(() => {
  // 경과 시간에 따라 초기 시간대 설정
  if (elapsedMinutes < 60) {
    selectedTime.value = 30
  } else if (elapsedMinutes < 90) {
    selectedTime.value = 60
  } else {
    selectedTime.value = 90
  }

  // 카카오맵 스크립트 로드
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services&autoload=false`
  document.head.appendChild(script)

  script.onload = () => {
    window.kakao.maps.load(() => {
      geocoder = new window.kakao.maps.services.Geocoder()
      mapInstance = new window.kakao.maps.Map(mapContainer.value, {
        center: new window.kakao.maps.LatLng(37.537248, 126.857187),
        level: 6
      })
    })
  }
})

// API에서 예측 위치 데이터 가져오기
async function getPrediction(userLocData) {
  const params = { userLocData }
  try {
    const response = await axios.post(`http://localhost:8000/pred`, {
      params: params
    })
    console.log(`응답 -> ${JSON.stringify(response)}`)
    fullLocation.value = response.data.data
  } catch (err) {
    console.log(`예측위치 로딩 중 에러발생 -> ${err}`)
  }
}

// 시간대 선택 함수
function timeStamp(time) {
  selectedTime.value = time
  console.log(`선택된 시간: ${time}분`)
  // 여기서 선택된 시간에 따라 예측 위치를 업데이트할 수 있음
}

// 지도/실종자 정보 토글 함수
function mapOrInfo(what) {
  selectedType.value = what
  console.log(`선택된 타입: ${what}`)
}
</script>

<style scoped>
/* ============ 전역 스타일 ============ */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

/* ============ 페이지 컨테이너 ============ */
.page-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 375px;
  min-height: 100vh;
  margin-top: -20px;
  background: #ffffff;
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Apple SD Gothic Neo", sans-serif;
}


/* ============ 지도 영역 ============ */
.map-area {
  position: relative;
  width: 100%;
  height: 320px;
  background: #f5f5f5;
  top:0;
  left:0;
  border: 1px solid #e5e5e5;
}

/* ============ 토글 버튼 래퍼 ============ */
.toggle-button-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 150px;
  height: 59px;
  padding: 12px 16px;
  background: #fafafa;
  border-top: 1px solid #e5e5e5;
  border-bottom: 1px solid #e5e5e5;
  gap: 10px;

}

.toggle-button {
  flex: 1;
  height: 40px;
  background: rgba(0, 0, 0, 0);
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 13px;
  gap:8px;
}

.toggle-button i {
  font-size: 16px;
  color: #404040;
}

.toggle-button.active {
  background: rgba(74, 98, 221, 0.85);
  border-color: rgba(74, 98, 221, 0.85);
}

.button-text {
  color: #404040;
  font-size: 14px;
  font-weight: 400;
  line-height: 16.943px;
  white-space: nowrap;
}

.toggle-button.active .button-text {
  color: #ffffff;
}

.toggle-button.active i {
  color: #ffffff;
}

/* ============ 실종자 정보 헤더 ============ */
.info-header {
  padding: 16px;
  background: #F8F9FA;
  border-bottom: 1px solid #E9ECEF;
}

.info-title {
  font-size: 16px;
  font-weight: 700;
  color: #212529;
  margin-bottom: 8px;
}

.info-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.missing-date {
  font-size: 13px;
  color: #6C757D;
}

.elapsed-info {
  font-size: 13px;
  color: #6C757D;
}

.elapsed-info strong {
  color: #DC3545;
  font-weight: 600;
}

/* ============ 시간 선택 버튼 ============ */
.time-selector {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #E9ECEF;
}

.time-button {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 16px;
  width: 80px;
  background: #ffffff;
  border: 1px solid #d4d4d4;
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.3s ease;
  gap: 8px;
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.time-button.active {
  background: rgba(74, 98, 221, 0.85);
  border-color: rgba(74, 98, 221, 0.85);
}

.time-text {
  color: #3f414e;
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  white-space: nowrap;
}

.time-button.active .time-text {
  color: #ffffff;
}

/* ============ 컨텐츠 섹션 ============ */
.content-section {
  flex: 1;
  overflow-y: auto;
  background: #ffffff;
  padding: 0;
}

/* ============ 실종자 정보 카드 ============ */
.missing-person-info {
  width: 100%;
  background: #ffffff;
  padding: 20px 16px;
}

/* 상단: 프로필 + 기본 정보 */
.info-header-section {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  align-items: flex-start;
}

.profile-image-wrapper {
  flex-shrink: 0;
}

.profile-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
  border: 1px solid #e5e5e5;
}

.basic-info-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 4px;
}

.name-age-row {
  margin-bottom: 4px;
}

.person-name {
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #171717;
  line-height: 1.3;
  margin: 0;
}

.age-info {
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 13px;
  font-weight: 400;
  color: #737373;
  line-height: 1.4;
  margin: 0;
}

.missing-datetime {
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 13px;
  font-weight: 400;
  color: #525252;
  line-height: 1.5;
  margin: 0;
}

.missing-location {
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 13px;
  font-weight: 400;
  color: #525252;
  line-height: 1.5;
  margin: 0;
}

/* 세부 정보 섹션 */
.detail-sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  gap: 8px;
}

.info-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 4px 10px;
  background: rgba(170, 193, 253, 0.3);
  border-radius: 16px;
}

.badge-label {
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #4a62dd;
  line-height: 1.2;
  white-space: nowrap;
}

.info-content {
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 14px;
  font-weight: 400;
  color: #525252;
  line-height: 1.6;
  padding-left: 2px;
}

/* ============ 예상 위치 카드 리스트 ============ */
.prediction-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 100%;
  padding: 8px 0;
}

.prediction-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  width: 100%;
  padding: 16px;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
}

.prediction-card:last-child {
  border-bottom: none;
}

.card-icon-wrapper {
  flex-shrink: 0;
  padding-top: 2px;
}

.location-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff5e7e 100%);
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(255, 107, 157, 0.25);
}

.location-icon i {
  font-size: 22px;
  color: #ffffff;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.location-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.location-name {
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 15px;
  font-weight: 500;
  color: #171717;
  line-height: 1.4;
  margin: 0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.probability-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 10px;
  background: #e3f2fd;
  border-radius: 12px;
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 11px;
  font-weight: 500;
  color: #1976d2;
  line-height: 1.2;
  white-space: nowrap;
  flex-shrink: 0;
}

.location-distance {
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 13px;
  font-weight: 400;
  color: #737373;
  line-height: 1.4;
  margin: 0;
}

.action-button-wrapper {
  margin-top: 4px;
}

.view-probability-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 14px;
  background: #f5f5f5;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #525252;
  cursor: pointer;
  transition: all 0.2s ease;
}

.view-probability-btn:hover {
  background: #eeeeee;
  border-color: #d4d4d4;
}

.view-probability-btn:active {
  background: #e5e5e5;
  transform: scale(0.98);
}

/* ============ 스크롤바 스타일 ============ */
.content-section::-webkit-scrollbar {
  width: 4px;
}

.content-section::-webkit-scrollbar-track {
  background: #fafafa;
}

.content-section::-webkit-scrollbar-thumb {
  background: #d4d4d4;
  border-radius: 2px;
}

.content-section::-webkit-scrollbar-thumb:hover {
  background: #a3a3a3;
}

/* ============ 반응형 ============ */
@media (max-width: 375px) {
  .page-container {
    max-width: 100%;
  }

  .missing-person-info {
    padding: 16px 12px;
  }

  .info-header-section {
    gap: 12px;
  }

  .profile-image {
    width: 80px;
    height: 80px;
  }
}
</style>

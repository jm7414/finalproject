<!-- src/views/HospitalCare.vue -->
<template>
  <!-- 메인 컨테이너 -->
  <div class="main-container">
    <!-- 지도 섹션 -->
    <div class="map-section">
      <!-- 지도 컨트롤 버튼 (현위치, 확대, 축소) -->
      <div class="map-controls">
        <button class="current-location-btn" @click="moveToCurrentLocation">
          <div class="icon-wrapper">
            <div class="current-location-icon"></div>
          </div>
        </button>
        <button class="zoom-in-btn" @click="zoomIn">
          <div class="icon-wrapper">
            <div class="zoom-in-icon"></div>
          </div>
        </button>
        <button class="zoom-out-btn" @click="zoomOut">
          <div class="icon-wrapper">
            <div class="zoom-out-icon"></div>
          </div>
        </button>
      </div>

      <!-- 카카오맵 컨테이너 -->
      <div ref="mapContainer" class="map-container"></div>

      <!-- 검색 입력창 (지도 안쪽, 결과 섹션과 안 겹치게) -->
      <div class="search-wrapper">
        <input
          class="search-input"
          placeholder="주변의 병원을 찾아보세요"
          v-model="searchKeyword"
        />
        <div class="search-icon-wrapper">
          <div class="search-icon"></div>
        </div>
      </div>
    </div>

    <!-- 검색 결과 / 요약 섹션 -->
    <div class="results-section">
      <!-- 상단 타이틀 & 요약 -->
      <div class="header-summary">
        <h2 class="page-title">라크라센터 주변 병원</h2>
        <p class="page-subtitle">
          라크라센터를 기준으로 반경 1km 안에 있는 병원을 한 번에 모아서 보여드려요.
        </p>

        <div class="summary-box">
          <div class="summary-main">
            <span class="summary-label">1km 이내 병원 수</span>
            <span class="summary-count">{{ filteredHospitals.length }}곳</span>
          </div>
          <button class="summary-refresh" @click="reload">
            🔄 다시 불러오기
          </button>
        </div>

        <!-- 병원 / 전체 카테고리 -->
        <div class="category-filter-row">
        </div>

        <!-- 전체 / 큰 병원만 토글 (약국 화면에서는 의미 없으니 병원일 때만 사용) -->
        <div class="big-filter-row" v-if="category !== 'pharmacy'">
          <button
            class="big-filter-btn"
            :class="{ active: !onlyBig }"
            @click="onlyBig = false"
          >
            전체 병원
          </button>
          <button
            class="big-filter-btn"
            :class="{ active: onlyBig }"
            @click="onlyBig = true"
          >
            큰 병원만
          </button>
        </div>

        <p v-if="loading" class="info-text">
          병원 위치를 불러오는 중입니다...
        </p>
        <p v-if="errorMessage" class="error-text">
          {{ errorMessage }}
        </p>

        <div
          v-if="!loading && !errorMessage && filteredHospitals.length === 0"
          class="fallback-box"
        >
          <p class="fallback-main">
            조건에 맞는 병원·약국 정보를 찾지 못했어요.
          </p>
          <p class="fallback-sub">
            잠시 후 다시 시도해 보거나, 검색어 / 필터를 바꿔서 다시 확인해 주세요.
          </p>
        </div>
      </div>

      <!-- 병원 리스트 -->
      <div v-if="paginatedHospitals.length > 0" class="cards-wrapper">
        <div
          class="counseling-card"
          v-for="(h, index) in paginatedHospitals"
          :key="h.name + '-' + h.address + '-' + index"
        >
          <div class="card-header">
            <div class="card-info">
              <h3 class="center-name">{{ h.name }}</h3>
              <p class="center-address">
                {{ h.address }}
                <span class="location-pin-icon"></span>
                <span class="distance-text">
                  {{ h.distanceKm.toFixed(2) }}km
                </span>
              </p>
            </div>
          </div>

          <div class="action-buttons">
            <button
              class="phone-button"
              @click="call(h.tel)"
              :disabled="!h.tel"
            >
              <div class="phone-button-content">
                <div class="phone-icon-wrapper">
                  <div class="phone-icon"></div>
                </div>
                <span class="phone-text">
                  {{ h.tel ? '전화' : '전화정보 없음' }}
                </span>
              </div>
            </button>

            <button
              class="directions-button"
              @click="openKakaoMap(h)"
            >
              <div class="directions-button-content">
                <div class="directions-icon-wrapper">
                  <div class="directions-icon"></div>
                </div>
                <span class="directions-text">길찾기</span>
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 페이지네이션 (심플 버전) -->
    <div class="pagination" v-if="totalPages > 1">
      <button
        class="page-btn"
        :disabled="currentPage === 1"
        @click="currentPage--"
      >
        이전
      </button>

      <span class="page-info">
        {{ currentPage }} / {{ totalPages }}
      </span>

      <button
        class="page-btn"
        :disabled="currentPage === totalPages"
        @click="currentPage++"
      >
        다음
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import axios from 'axios'

const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'
const mapContainer = ref(null)

// 병원 데이터
const allHospitals = ref([])

// 상태
const loading = ref(false)
const errorMessage = ref('')

// 검색 키워드 (이름 + 주소)
const searchKeyword = ref('')

// 탭: 전체 / 병원 / 약국
const category = ref('all')

// "큰 병원만 보기" 토글
const onlyBig = ref(false)

// 지도 관련
let map = null
let markers = []
let infowindow = null

// 라크라센터 좌표 (구로구청 앞 건물)
const DEFAULT_CENTER_LAT = 37.4942627
const DEFAULT_CENTER_LNG = 126.8873901

// 페이지네이션
const currentPage = ref(1)
const itemsPerPage = 5

// === 마운트 시 카카오맵 & 병원 데이터 로드 ===
onMounted(() => {
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false`
  document.head.appendChild(script)

  script.onload = () => {
    window.kakao.maps.load(() => {
      map = new window.kakao.maps.Map(mapContainer.value, {
        center: new window.kakao.maps.LatLng(DEFAULT_CENTER_LAT, DEFAULT_CENTER_LNG),
        level: 3
      })
      infowindow = new window.kakao.maps.InfoWindow({ zIndex: 1 })
      loadHospitals()
    })
  }
})

// === 병원 정보 로드 (백엔드 호출) ===
const loadHospitals = async () => {
  loading.value = true
  errorMessage.value = ''
  allHospitals.value = []
  currentPage.value = 1

  try {
    const res = await axios.get('/api/hospital/near')
    const raw = Array.isArray(res.data) ? res.data : []
    allHospitals.value = raw
  } catch (err) {
    console.error('병원 정보 조회 실패:', err)
    errorMessage.value =
      err.response?.data?.message ||
      err.message ||
      '병원 정보를 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

// === 약국 여부 ===
function isPharmacy(hospital) {
  const name = (hospital.name || '').trim()
  return !!name && name.includes('약국')
}

// === "큰 병원"인지 이름 기준으로 판단 ===
function isBigHospitalByName(hospital) {
  const name = (hospital.name || '').trim()
  if (!name) return false

  // 작은 규모로 많이 쓰이는 패턴은 제외
  if (
    name.includes('의원') ||
    name.includes('치과') ||
    name.includes('한의원') ||
    name.includes('약국')
  ) {
    return false
  }

  // 대학병원, 의료원, 병원 등은 큰 병원으로 취급
  if (name.includes('대학병원')) return true
  if (name.includes('의료원')) return true
  if (name.includes('병원')) return true

  return false
}

// === 필터링된 병원 목록 (1km + 카테고리 + 큰 병원 토글 + 검색어) ===
const filteredHospitals = computed(() => {
  // 1km 이내 먼저 필터
  let list = allHospitals.value.filter(h => {
    const d = Number(h.distanceKm)
    return !Number.isNaN(d) && d <= 1
  })

  // 병원/약국 카테고리 필터
  if (category.value === 'hospital') {
    list = list.filter(h => !isPharmacy(h))
  } else if (category.value === 'pharmacy') {
    list = list.filter(h => isPharmacy(h))
  }

  // 큰 병원만 보기 (약국 탭에서는 적용 X)
  if (onlyBig.value && category.value !== 'pharmacy') {
    list = list.filter(h => isBigHospitalByName(h))
  }

  // 검색어 필터
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim()
    list = list.filter(
      h =>
        (h.name && h.name.includes(kw)) ||
        (h.address && h.address.includes(kw))
    )
  }

  return list
})

// === 페이지네이션 계산 ===
const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredHospitals.value.length / itemsPerPage))
)

const paginatedHospitals = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredHospitals.value.slice(start, start + itemsPerPage)
})

// totalPages 줄어들 때 현재 페이지 보정
watch(totalPages, newTotal => {
  if (currentPage.value > newTotal) {
    currentPage.value = newTotal
  }
})

// 필터된 병원 목록이 바뀔 때마다 지도 마커 다시 그림
watch(
  filteredHospitals,
  newList => {
    renderMarkers(newList)
  },
  { immediate: true }
)

// === 지도에 마커 표시 ===
function renderMarkers(list) {
  if (!map || !window.kakao) return

  // 기존 마커 제거
  markers.forEach(m => m.setMap(null))
  markers = []

  if (!list || list.length === 0) {
    // 병원이 하나도 없어도 중심은 라크라센터로
    const center = new window.kakao.maps.LatLng(DEFAULT_CENTER_LAT, DEFAULT_CENTER_LNG)
    map.setCenter(center)
    map.setLevel(3)
    return
  }

  list.forEach(h => {
    if (Number.isNaN(h.lat) || Number.isNaN(h.lng)) return

    const pos = new window.kakao.maps.LatLng(h.lat, h.lng)
    const marker = new window.kakao.maps.Marker({
      map,
      position: pos
    })
    markers.push(marker)

    window.kakao.maps.event.addListener(marker, 'click', () => {
      infowindow.setContent(
        `<div style="padding:5px;font-size:12px;">${h.name}</div>`
      )
      infowindow.open(map, marker)
    })
  })

  // ★ 항상 라크라센터 기준 + 레벨 3으로 고정
  const center = new window.kakao.maps.LatLng(DEFAULT_CENTER_LAT, DEFAULT_CENTER_LNG)
  map.setCenter(center)
  map.setLevel(3)
}

// === 현재 위치로 이동 (백엔드 재호출 없이 지도만 이동) ===
function moveToCurrentLocation() {
  if (!navigator.geolocation || !map) return

  navigator.geolocation.getCurrentPosition(
    pos => {
      const lat = pos.coords.latitude
      const lng = pos.coords.longitude
      const loc = new window.kakao.maps.LatLng(lat, lng)
      map.setCenter(loc)
    },
    () => {
      // 실패 시 그냥 기본 위치 유지
    }
  )
}

// === 줌 컨트롤 ===
function zoomIn() {
  if (!map) return
  const level = map.getLevel()
  if (level > 1) {
    map.setLevel(level - 1)
  }
}

function zoomOut() {
  if (!map) return
  const level = map.getLevel()
  map.setLevel(level + 1)
}

// === 전화 & 길찾기 ===
function call(phone) {
  if (!phone) return
  window.open(`tel:${phone}`)
}

function openKakaoMap(h) {
  const q = encodeURIComponent(`${h.name} ${h.address}`)
  window.open(`https://map.kakao.com/?q=${q}`, '_blank')
}

// === 다시 불러오기 ===
function reload() {
  loadHospitals()
}
</script>

<style scoped>
/* 기본 설정 */
:root {
  --default-font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    Ubuntu, "Helvetica Neue", Helvetica, Arial, "PingFang SC",
    "Hiragino Sans GB", "Microsoft Yahei UI", "Microsoft Yahei",
    "Source Han Sans CN", sans-serif;
}

* {
  box-sizing: border-box;
}

input,
select,
textarea,
button {
  outline: 0;
}

/* 메인 컨테이너 */
.main-container {
  position: relative;
  width: 375px;
  min-height: 100vh;
  margin: 0 auto;
  background: #ffffff;
  overflow: hidden;
}

/* 지도 섹션 */
.map-section {
  position: relative;
  right: 7px;
  width: 375px;
  height: 360px; /* 조금 더 크게 */
  background: #f5f5f5;
}

.map-container {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  background: #d4d4d4;
  z-index: 1;
}

/* 지도 컨트롤 버튼 */
.map-controls {
  position: absolute;
  width: 40px;
  height: 136px;
  top: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 10;
}

.current-location-btn,
.zoom-in-btn,
.zoom-out-btn {
  width: 40px;
  height: 40px;
  background: #ffffff;
  border: none;
  border-radius: 8px;
  box-shadow: 0 4px 6px 0 rgba(0, 0, 0, 0.1);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.current-location-btn:hover,
.zoom-in-btn:hover,
.zoom-out-btn:hover {
  box-shadow: 0 6px 8px 0 rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

.current-location-btn:active,
.zoom-in-btn:active,
.zoom-out-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.1);
}

.icon-wrapper {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.current-location-icon {
  width: 16px;
  height: 16px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/YGSmfjJuiN.png')
    no-repeat center;
  background-size: cover;
}

.zoom-in-icon {
  width: 14px;
  height: 16px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/sxkSvTkgyU.png')
    no-repeat center;
  background-size: cover;
}

.zoom-out-icon {
  width: 14px;
  height: 16px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/2TXTq4KJ99.png')
    no-repeat center;
  background-size: cover;
}

/* 검색 입력창 (지도 안쪽, 결과와 겹치지 않게) */
.search-wrapper {
  position: absolute;
  bottom: 12px;
  left: 16px;
  width: calc(100% - 32px);
  height: 46px;
  z-index: 10;
}

.search-input {
  width: 100%;
  height: 100%;
  padding: 10px 12px 10px 44px;
  background: #fafafa;
  border: 1px solid #d4d4d4;
  border-radius: 8px;
  font-family: var(--default-font-family);
  font-size: 15px;
  font-weight: 400;
  line-height: 22px;
  color: #262626;
}

.search-input::placeholder {
  color: #a3a3a3;
}

.search-input:focus {
  border-color: rgba(170, 193, 253, 0.91);
  background: #ffffff;
}

.search-icon-wrapper {
  position: absolute;
  top: 50%;
  left: 14px;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  pointer-events: none;
}

.search-icon {
  width: 16px;
  height: 16px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/bjkU3DLsqM.png')
    no-repeat center;
  background-size: cover;
}

/* 결과 섹션 */
.results-section {
  position: relative;
  top: 0;
  right: 7px;
  width: 375px;
  min-height: 300px;
  padding: 16px;
  background: #ffffff;
}

.header-summary {
  margin-bottom: 12px;
}

.page-title {
  margin: 0;
  font-family: Pretendard, var(--default-font-family);
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.page-subtitle {
  margin: 6px 0 10px 0;
  font-family: Pretendard, var(--default-font-family);
  font-size: 13px;
  color: #6b7280;
  line-height: 1.4;
}

/* 요약 박스 */
.summary-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  margin-bottom: 8px;
}

.summary-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 12px;
  color: #6b7280;
}

.summary-count {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.summary-refresh {
  border: none;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  background: #eef2ff;
  color: #4f46e5;
  cursor: pointer;
}

/* 카테고리 (병원/약국/전체) */
.category-filter-row {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.category-btn {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  font-size: 11px;
  cursor: pointer;
  color: #4b5563;
}

.category-btn.active {
  background: #4a62dd;
  color: #ffffff;
  border-color: #4a62dd;
}

/* 전체/큰 병원 토글 */
.big-filter-row {
  margin-top: 6px;
  display: flex;
  gap: 8px;
}

.big-filter-btn {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  font-size: 11px;
  cursor: pointer;
  color: #4b5563;
}

.big-filter-btn.active {
  background: #4a62dd;
  color: #ffffff;
  border-color: #4a62dd;
}

.info-text {
  margin-top: 8px;
  font-size: 12px;
  color: #4b5563;
}

.error-text {
  margin-top: 8px;
  font-size: 12px;
  color: #b91c1c;
}

.fallback-box {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fef9c3;
  border: 1px solid #facc15;
}

.fallback-main {
  font-size: 13px;
  font-weight: 600;
  color: #854d0e;
  margin: 0 0 4px 0;
}

.fallback-sub {
  font-size: 12px;
  color: #92400e;
  margin: 0;
}

/* 카드 */
.cards-wrapper {
  margin-top: 6px;
}

.counseling-card {
  width: 100%;
  padding: 20px;
  background: #ffffff;
  border: 1px solid #f3f4f6;
  border-radius: 16px;
  box-shadow: 2px 2px 3px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-top: 8px;
  gap: 12px;
  position: relative;
}

.card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.center-name {
  color: #000000;
  font-family: Pretendard, var(--default-font-family);
  font-size: 18px;
  font-weight: 500;
  line-height: 24px;
  margin: 0;
}

.center-address {
  color: #7e7e7e;
  font-family: Pretendard, var(--default-font-family);
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.location-pin-icon {
  display: inline-block;
  width: 9px;
  height: 12px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/OeCRPTBYB4.png')
    no-repeat center;
  background-size: cover;
}

.distance-text {
  color: #6b7280;
  font-family: Pretendard, var(--default-font-family);
  font-size: 12px;
  font-weight: 400;
  line-height: 16px;
  white-space: nowrap;
}

/* 액션 버튼 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
}

.phone-button {
  flex: 1;
  height: 40px;
  padding: 8px 16px;
  background: rgba(74, 98, 221, 0.85);
  border: none;
  border-radius: 8px;
  box-shadow: 2px 2px 4px 0 rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
}

.phone-button[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}

.phone-button:hover:not([disabled]) {
  background: rgba(74, 98, 221, 1);
  transform: translateY(-1px);
  box-shadow: 2px 4px 6px 0 rgba(0, 0, 0, 0.1);
}

.phone-button:active:not([disabled]) {
  transform: translateY(0);
}

.phone-button-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
}

.phone-icon-wrapper {
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.phone-icon {
  width: 14px;
  height: 14px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/pceOwzmAMJ.png')
    no-repeat center;
  background-size: cover;
}

.phone-text {
  color: #ffffff;
  font-family: Pretendard, var(--default-font-family);
  font-size: 16px;
  font-weight: 400;
  line-height: 19px;
  white-space: nowrap;
}

.directions-button {
  flex: 1;
  height: 40px;
  padding: 8px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  box-shadow: 2px 2px 4px 0 rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
}

.directions-button:hover {
  background: #e5e7eb;
  transform: translateY(-1px);
  box-shadow: 2px 4px 6px 0 rgba(0, 0, 0, 0.1);
}

.directions-button:active {
  transform: translateY(0);
}

.directions-button-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
}

.directions-icon-wrapper {
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.directions-icon {
  width: 14px;
  height: 14px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/UHPKx6GoL9.png')
    no-repeat center;
  background-size: cover;
}

.directions-text {
  color: #374151;
  font-family: Pretendard, var(--default-font-family);
  font-size: 14px;
  font-weight: 400;
  line-height: 17px;
  white-space: nowrap;
}

/* 페이지네이션 스타일 (심플) */
.pagination {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  top: 15px;
  gap: 10px;
  margin: 12px 0 28px; /* 아래 여유 조금 더 */
}

.page-btn {
  padding: 6px 12px;
  background: #f3f4f6;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  font-family: var(--default-font-family);
  font-size: 13px;
  color: #374151;
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
}

/* 반응형 */
@media (max-width: 375px) {
  .main-container {
    width: 100%;
  }

  .map-section,
  .results-section {
    width: 100%;
  }

  .search-wrapper {
    left: 16px;
    width: calc(100% - 32px);
  }
}
</style>

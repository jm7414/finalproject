<template>
  <!-- 메인 컨테이너 -->
  <div class="main-container">

    <!-- 위치기반 서비스 허용 알림 -->
    <div v-if="!isAllowed" class="location-permission-alert">
      <div class="alert-content">
        <div class="location-icon-wrapper">
          <div class="location-icon"></div>
        </div>
        <span class="permission-text">위치 기반 서비스를 위해 위치 권한이 필요 합니다.</span>
        <button class="allow-button" @click="allow">
          <span class="allow-button-text">허용</span>
        </button>
      </div>
    </div>

    <!-- 지도 섹션 -->
    <div class="map-section">
      <!-- 지도 컨트롤 버튼 (현위치, 확대, 축소) -->
      <div class="map-controls">
        <button class="current-location-btn">
          <div class="icon-wrapper">
            <div class="current-location-icon"></div>
          </div>
        </button>
        <button class="zoom-in-btn">
          <div class="icon-wrapper">
            <div class="zoom-in-icon"></div>
          </div>
        </button>
        <button class="zoom-out-btn">
          <div class="icon-wrapper">
            <div class="zoom-out-icon"></div>
          </div>
        </button>
      </div>

      <!-- 카카오맵 컨테이너 -->
      <div ref="mapContainer" class="map-container"></div>

      <!-- 검색 입력창 -->
      <div class="search-wrapper">
        <input class="search-input" placeholder="주변의 상담소를 찾아보세요" v-model="content" @keyup.enter="searchNearby" />
        <div class="search-icon-wrapper">
          <div class="search-icon"></div>
        </div>
      </div>
    </div>

    <!-- 검색 결과 섹션 -->
    <div class="results-section">
      <!-- places 배열에 대한 반복 렌더링 -->
      <div class="counseling-card" v-for="(place, index) in paginatedPlaces" :key="place.id || index">
        <div class="card-image"></div>

        <div class="card-header">
          <div class="card-info">
            <h3 class="center-name">{{ place.place_name }}</h3>
            <p class="center-address">
              {{ place.road_address_name || place.address_name }}
              <span class="location-pin-icon"></span>
              <span class="distance-text">
                {{ (place.distance * 0.001).toFixed(2) }}km
              </span>
            </p>
          </div>
        </div>

        <div class="action-buttons">
          <button class="phone-button" @click="call(place.phone)">
            <div class="phone-button-content">
              <div class="phone-icon-wrapper">
                <div class="phone-icon"></div>
              </div>
              <span class="phone-text">전화</span>
            </div>
          </button>
          <button class="directions-button" @click="findRoute(place.place_name)">
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
    <div class="pagination">
      <button class="page-btn" :disabled="currentPage === 1" @click="currentPage--">
        이전
      </button>

      <button v-for="page in totalPages" :key="page" class="page-btn" :class="{ active: page === currentPage }"
        @click="currentPage = page">
        {{ page }}
      </button>

      <button class="page-btn" :disabled="currentPage === totalPages" @click="currentPage++">
        다음
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891';
const mapContainer = ref(null);
const isAllowed = ref(false);
let map;
let geocoder = null;
let markers = [];
let infowindow = null;
let myMarker = null; // 내 기준점 마커

const content = ref('');
const places = ref([]);

const BASE_LAT = 37.494364;
const BASE_LNG = 126.887661;
const BASE_ZOOM = 9;

// ---------- 지도 및 데이터 초기화 ----------
onMounted(() => {
  const script = document.createElement('script');
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services&autoload=false`;
  document.head.appendChild(script);

  script.onload = () => {
    window.kakao.maps.load(() => {
      geocoder = new window.kakao.maps.services.Geocoder();
      map = new window.kakao.maps.Map(mapContainer.value, {
        center: new window.kakao.maps.LatLng(BASE_LAT, BASE_LNG),
        level: BASE_ZOOM
      });
      infowindow = new window.kakao.maps.InfoWindow({ zIndex: 1 });

      // ✅ (3) 기준점 마커 강조
      if (myMarker) myMarker.setMap(null); // 중복방지
      myMarker = new window.kakao.maps.Marker({
        map,
        position: new window.kakao.maps.LatLng(BASE_LAT, BASE_LNG),
        image: new window.kakao.maps.MarkerImage(
          'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png', // 눈에 띄는 아이콘(스타)
          new window.kakao.maps.Size(24, 35)
        )
      });

      loadSeoulAndGyeonggi();
    });
  };
});

// ---------- 검색 -----------
function searchNearby() {
  markers.forEach(marker => marker.setMap(null));
  markers = [];
  if (content.value.trim()) {
    loadPublicData(content.value);
  } else {
    loadSeoulAndGyeonggi();
  }
}

// ---------- 서울+경기 통합 요청 ----------
async function loadSeoulAndGyeonggi() {
  try {
    const [gyeonggiResp, seoulResp] = await Promise.all([
      axios.get('/api/heart-care/centers', { params: { page: 1, perPage: 30, keyword: '경기도' } }),
      axios.get('/api/heart-care/centers', { params: { page: 1, perPage: 30, keyword: '서울' } })
    ]);
    const merged = [...(gyeonggiResp.data.data || []), ...(seoulResp.data.data || [])];
    const uniqueMap = new Map();
    merged.forEach(center => {
      const key = `${center["주소"]}_${center["기관명"]}`;
      uniqueMap.set(key, center);
    });
    const uniqueCenters = Array.from(uniqueMap.values());

    places.value = uniqueCenters.map(center => ({
      place_name: center["기관명"],
      address_name: center["주소"],
      road_address_name: center["주소"],
      phone: center["전화번호"] || '-',
      category_name: center["기관구분"],
      x: 0,
      y: 0,
      distance: 0
    }));
    convertAddressesToCoordinates();
  } catch (error) {
    console.error('공공데이터 조회 실패:', error);
    alert('상담소 정보를 불러오는데 실패했습니다.');
  }
}

async function loadPublicData(keyword) {
  try {
    const response = await axios.get('/api/heart-care/centers', {
      params: { page: 1, perPage: 100, keyword: keyword }
    });
    if (response.data.success) {
      places.value = response.data.data.map(center => ({
        place_name: center["기관명"],
        address_name: center["주소"],
        road_address_name: center["주소"],
        phone: center["전화번호"] || '-',
        category_name: center["기관구분"],
        x: 0,
        y: 0,
        distance: 0
      }));
      convertAddressesToCoordinates();
    }
  } catch (error) {
    console.error('공공데이터 조회 실패:', error);
    alert('상담소 정보를 불러오는데 실패했습니다.');
  }
}

// ---------- 좌표변환/거리 정렬(모두 변환 후) ----------
function convertAddressesToCoordinates() {
  if (!window.kakao || !window.kakao.maps) return;
  const geocoder = new window.kakao.maps.services.Geocoder();
  let processedCount = 0;

  markers.forEach(marker => marker.setMap(null)); // 마커 초기화
  markers = [];

  places.value.forEach((place) => {
    if (!place.address_name || !place.address_name.trim()) {
      console.warn('빈 주소로 변환 시도:', place);
      return;
    }
    geocoder.addressSearch(place.address_name, (result, status) => {
      processedCount++;
      if (status === window.kakao.maps.services.Status.OK) {
        place.x = result[0].x;
        place.y = result[0].y;
        // (1,2) 고정 기준점에서 거리계산
        const baseCenter = new window.kakao.maps.LatLng(BASE_LAT, BASE_LNG);
        const placePosition = new window.kakao.maps.LatLng(place.y, place.x);
        const polyline = new window.kakao.maps.Polyline({ path: [baseCenter, placePosition] });
        place.distance = polyline.getLength();
        displayMarker(place);
      } else {
        console.warn(`주소 변환 실패: ${place.address_name}`);
      }
      if (processedCount === places.value.length) {
        // (2) 지도 비율 자동 조정(setBounds) 대신
        map.setCenter(new window.kakao.maps.LatLng(BASE_LAT, BASE_LNG));
        map.setLevel(BASE_ZOOM);

        places.value.sort((a, b) => a.distance - b.distance); // 가까운순
      }
    });
  });
}

// ---------- 마커 표시 ----------
function displayMarker(place) {
  if (!place.x || !place.y) return;
  const marker = new window.kakao.maps.Marker({
    map,
    position: new window.kakao.maps.LatLng(place.y, place.x)
  });
  markers.push(marker);
  window.kakao.maps.event.addListener(marker, 'click', () => {
    infowindow.setContent(
      `<div style="padding:5px;font-size:12px;">${place.place_name}</div>`
    );
    infowindow.open(map, marker);
  });
}

function allow() { isAllowed.value = true; }
function call(phone) {
  if (phone && phone !== '-') window.open(`tel:${phone}`);
  else alert('전화번호 정보가 없습니다.');
}
function findRoute(placeName) {
  window.open(`https://map.kakao.com/link/search/${placeName}`);
}

const currentPage = ref(1);
const itemsPerPage = 5;
const totalPages = computed(() => Math.ceil(places.value.length / itemsPerPage));
const paginatedPlaces = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  return places.value.slice(start, start + itemsPerPage);
});
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
  margin-top: -15px;
  background: #ffffff;
  overflow: hidden;
}

/* ============================================
   위치 권한 알림 섹션
============================================ */
.location-permission-alert {
  position: relative;
  width: 375px;
  height: 65px;
  right: 10px;
  background: #fafafa;
  border-top: 1px solid #f5f5f5;
  border-bottom: 1px solid #f5f5f5;
  z-index: 100;
}

.alert-content {
  position: absolute;
  width: 351px;
  height: 40px;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.location-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 12px;
  height: 16px;
  flex-shrink: 0;
}

.location-icon {
  width: 12px;
  height: 16px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/CCVEhnFpko.png') no-repeat center;
  background-size: cover;
}

.permission-text {
  flex: 1;
  color: #262626;
  font-family: var(--default-font-family);
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  text-align: left;
}

.allow-button {
  width: 48px;
  height: 24px;
  background: rgba(170, 193, 253, 0.91);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.allow-button:hover {
  background: rgba(170, 193, 253, 1);
}

.allow-button-text {
  color: #ffffff;
  font-family: var(--default-font-family);
  font-size: 12px;
  font-weight: 400;
  line-height: 14.523px;
  text-align: center;
  white-space: nowrap;
}

/* ============================================
   지도 섹션
============================================ */
.map-section {
  position: relative;
  right: 7px;
  width: 375px;
  height: 320px;
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
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/YGSmfjJuiN.png') no-repeat center;
  background-size: cover;
}

.zoom-in-icon {
  width: 14px;
  height: 16px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/sxkSvTkgyU.png') no-repeat center;
  background-size: cover;
}

.zoom-out-icon {
  width: 14px;
  height: 16px;
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/2TXTq4KJ99.png') no-repeat center;
  background-size: cover;
}

/* 검색 입력창 */
.search-wrapper {
  position: absolute;
  bottom: -55px;
  left: 6px;
  width: 343px;
  height: 50px;
  z-index: 10;
}

.search-input {
  width: 100%;
  height: 100%;
  padding: 12px 12px 12px 48px;
  background: #fafafa;
  border: 1px solid #d4d4d4;
  border-radius: 8px;
  font-family: var(--default-font-family);
  font-size: 16px;
  font-weight: 400;
  line-height: 24px;
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
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/bjkU3DLsqM.png') no-repeat center;
  background-size: cover;
}

/* ============================================
   검색 결과 섹션
============================================ */
.results-section {
  position: relative;
  top: 30px;
  right: 7px;
  width: 375px;
  min-height: 300px;
  padding: 16px;
  background: #ffffff;
}

/* 상담소 카드 */
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
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/OeCRPTBYB4.png') no-repeat center;
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

.status-badge {
  flex-shrink: 0;
  padding: 5px 12px;
  background: #dcfce7;
  border-radius: 9999px;
  height: fit-content;
}

.status-text {
  color: #15803d;
  font-family: Pretendard, var(--default-font-family);
  font-size: 12px;
  font-weight: 400;
  line-height: 14px;
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

.phone-button:hover {
  background: rgba(74, 98, 221, 1);
  transform: translateY(-1px);
  box-shadow: 2px 4px 6px 0 rgba(0, 0, 0, 0.1);
}

.phone-button:active {
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
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/pceOwzmAMJ.png') no-repeat center;
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
  background: url('https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-17/UHPKx6GoL9.png') no-repeat center;
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

/* 반응형 디자인 (선택사항) */
@media (max-width: 375px) {
  .main-container {
    width: 100%;
  }

  .location-permission-alert,
  .map-section,
  .results-section {
    width: 100%;
  }

  .search-wrapper {
    width: calc(100% - 32px);
  }
}

/* 페이지네이션 스타일 */
.pagination {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  top:5px;
  gap: 8px;
  margin-top: 16px;
}

.page-btn {
  padding: 6px 12px;
  background: #f3f4f6;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-family: var(--default-font-family);
  font-size: 14px;
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.page-btn.active {
  background: #4a62dd;
  color: #ffffff;
}
</style>

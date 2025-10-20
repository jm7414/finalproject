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

const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891';
const mapContainer = ref(null);

// 맨 위에 위치정보 허용누르면 true로 바뀌고 없어지게 할 거
const isAllowed = ref(false)

// 여기는 기본적으로 검색했을떄 지도에 마커뜨고 infowindow는 마커 클릭하면 가게이름 나오는거
let map;
let geocoder = null;
let markers = [];
let infowindow = null;

// 이거로 검색 키워드 넣을 수 있게 할거임
const content = ref('');

// 검색결과 나타내기
const places = ref([])

// 처음 들어오면 지도부터 불러와
onMounted(() => {
  const script = document.createElement('script');
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services&autoload=false`;
  document.head.appendChild(script);

  script.onload = () => {
    window.kakao.maps.load(() => {
      geocoder = new window.kakao.maps.services.Geocoder();
      map = new window.kakao.maps.Map(mapContainer.value, {
        center: new window.kakao.maps.LatLng(37.494382, 126.887690),
        level: 6
      });
      infowindow = new window.kakao.maps.InfoWindow({ zIndex: 1 });
      content.value = '상담소'
      searchNearby(content.value)
      content.value = ''
    });
  };
});

function searchNearby() {
  markers.forEach(marker => marker.setMap(null));
  markers = [];

  const ps = new window.kakao.maps.services.Places();
  const center = map.getCenter();
  const options = {
    location: center,
    radius: 1000,
    sort: window.kakao.maps.services.SortBy.DISTANCE
  };


  ps.keywordSearch(content.value, (data, status) => {
    console.log(`응답 -> ${JSON.stringify(data)}`)
    if (status === window.kakao.maps.services.Status.OK) {
      const bounds = new window.kakao.maps.LatLngBounds();
      // 반환된 데이터로 배열 초기화
      places.value = data.map(place => ({
        ...place
      }));

      // 마커 표시
      data.forEach(place => {
        displayMarker(place);
        bounds.extend(new window.kakao.maps.LatLng(place.y, place.x));
      });
      map.setBounds(bounds);
    }
  }, options);
}


function displayMarker(place) {
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


function allow() {
  isAllowed.value = true
}

function call(phone) {
  window.open(`tel:${phone}`);
}

function findRoute(x) {
  // 카카오 길찾기 페이지로 이동 예시
  window.open(`https://map.kakao.com/link/search/${x}`);
}


// pagination
const currentPage = ref(1);
const itemsPerPage = 5;

const totalPages = computed(() => {
  return Math.ceil(places.value.length / itemsPerPage);
});

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
  margin: 0 auto;
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

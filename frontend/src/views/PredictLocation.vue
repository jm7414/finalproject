<template>
  <div class="map-wrapper">
    <div ref="mapContainer" class="map-box"></div>
    <div class="button-box">
      <button class="btn btn-info" >랜덤마커 찍기</button>
    </div>

    <!-- Detail Driver 레이어 -->
    <div class="detail-driver">
      <!-- 토글 바 -->
      <div class="toggle-bar">
        <button class="btn-confirm" @click="randomMarker()">
          <div class="icon-confirm"></div>
          <span class="text-confirm">확인하기</span>
        </button>
        <div class="btn-time">
          <div class="icon-time"></div>
          <span class="text-time">{{ currentTime }}</span>
        </div>
      </div>

      <!-- 위치 아이템 예시 -->
      <!-- 실제 좌표 기반으로 v-for 등으로 동적 생성도 가능 -->
      <div class="location-item item1">
        <div class="icon"></div>
        <div class="label-number">1번위치</div>
        <div class="label-desc">구로구 짜장동</div>
      </div>
      <div class="location-item item2">
        <div class="icon"></div>
        <div class="label-number">2번위치</div>
        <div class="label-desc">구로구 마라동</div>
      </div>
      <div class="location-item item3">
        <div class="icon"></div>
        <div class="label-number">3번위치</div>
        <div class="label-desc">구로구 마라동</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'

const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'

const mapContainer = ref(null)
let mapInstance = null
const markers = []
const circles = []

// 실시간 현재시간
const currentTime = ref('')

let timerId = null
function updateTime() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${hh}:${mm}:${ss}`
}

onMounted(() => {
  // 시간 갱신 시작
  updateTime()
  timerId = setInterval(updateTime, 1000)

  // Kakao map 로드
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      const options = {
        center: new window.kakao.maps.LatLng(37.537248, 126.857187),
        level: 6
      }
      mapInstance = new window.kakao.maps.Map(mapContainer.value, options)
      const initMarker = new kakao.maps.Marker({
        position: mapInstance.getCenter(),
        map: mapInstance
      })
      markers.push(initMarker)
    })
  }
})

onBeforeUnmount(() => {
  clearInterval(timerId)
})

async function randomMarker(count = 3) {
  try {
    const { data: coords } = await axios.get('http://localhost:8001/random_coords', {
      params: { count }
    })
    markers.forEach(m => m.setMap(null))
    markers.length = 0
    circles.forEach(c => c.setMap(null))
    circles.length = 0

    let totalLat = 0
    let totalLon = 0

    coords.forEach(item => {
      totalLat += item.latitude
      totalLon += item.longitude

      const position = new kakao.maps.LatLng(item.latitude, item.longitude)
      const marker = new kakao.maps.Marker({ position, map: mapInstance })
      markers.push(marker)

      const circle = new kakao.maps.Circle({
        center: position,
        radius: 300,
        strokeWeight: 5,
        strokeColor: '#75B8FA',
        strokeOpacity: 1,
        strokeStyle: 'dashed',
        fillColor: '#CFE7FF',
        fillOpacity: 0.7
      })
      circle.setMap(mapInstance)
      circles.push(circle)
    })

    if (coords.length) {
      const avgLat = totalLat / coords.length
      const avgLon = totalLon / coords.length
      mapInstance.setCenter(new kakao.maps.LatLng(avgLat, avgLon))
    }
  } catch (err) {
    console.error('API 요청 실패', err)
  }
}

// 확인하기 버튼 핸들러
function onConfirm() {
  console.log('확인하기 버튼 클릭!')
}
</script>

<style>
.map-wrapper {
  position: relative;
  width: 414px;
  height: 800px;
}

/* 지도 박스 */
.map-box {
  width: 100%;
  height: 400px;
}

/* 버튼 영역 */
.button-box {
  position: absolute;
  top: 410px;
  width: 100%;
  text-align: center;
}

/* Detail Driver 레이어 */
.detail-driver {
  position: absolute;
  width: 414px;
  height: 320px;
  left: 0;
  top: 460px;
  background: #FFFFFF;
  border-radius: 24px 24px 0 0;
}

/* 토글 바 */
.toggle-bar {
  position: absolute;
  width: 184px;
  height: 36px;
  left: 115px;
  top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #DBEAFE;
  border-radius: 9999px;
  padding: 0 4px;
}
.btn-confirm {
  display: flex;
  align-items: center;
  background: #F8FFC2;
  border: none;
  border-radius: 15px;
  padding: 0 8px;
  height: 36px;
  cursor: pointer;
}
.icon-confirm {
  width: 22px;
  height: 26px;
  background: #7D5260;
  transform: rotate(-180deg);
}
.text-confirm {
  font-family: 'Inter';
  font-weight: 500;
  font-size: 17px;
  color: #5C5A18;
  margin-left: 4px;
}
.btn-time {
  display: flex;
  align-items: center;
  padding: 0 8px;
}
.icon-time {
  width: 22px;
  height: 26px;
  background: #2563EB;
}
.text-time {
  font-family: 'Inter';
  font-weight: 500;
  font-size: 17px;
  color: #1D4ED8;
  margin-left: 4px;
}

/* 위치 아이템 공통 */
.location-item {
  position: absolute;
  width: 348px;
  height: 52px;
  display: flex;
  align-items: center;
}
.location-item .icon {
  width: 24px;
  height: 24px;
  background: #FF0404;
  margin-right: 16px;
  border-radius: 50%;
}
.location-item .label-number {
  font-family: 'ABeeZee';
  font-size: 20px;
  color: #D51E1E;
  margin-right: 8px;
}
.location-item .label-desc {
  font-family: 'ABeeZee';
  font-size: 17px;
  color: #868282;
  border: 0.5px solid #FF0000;
  text-shadow: 0px 4px 4px rgba(0,0,0,0.25);
  padding: 2px 4px;
}

/* 각 아이템 위치 */
.item1 { top: 90px; left: 80px; }
.item2 { top: 159px; left: 80px; }
.item3 { top: 229px; left: 80px; }
</style>

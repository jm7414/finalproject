<template>
  <div ref="mapContainer" class="map-box"></div>
  <div class="row g-3 mt-4"></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const mapContainer = ref(null)

/* 카카오 지도 */
const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
onMounted(() => {
  loadKakaoMap(mapContainer.value)
})
function loadKakaoMap(container) {
  const script = document.createElement('script')
  // service, cluster, drawing 라이브러리 활성화
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services,clusterer,drawing&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      const options = {
        center: new window.kakao.maps.LatLng(37.495488, 126.887642),
        level: 4,
      }
      const mapInstance = new window.kakao.maps.Map(container, options)

      // 백엔드(Tmap)로부터 좌표를 받아 폴리라인 그리기
      // - 지도 로드가 끝나면 자동으로 요청을 보냅니다.
      fetchRouteAndDraw(mapInstance)
    })
  }
}

/**
 * Kakao 지도에 좌표 배열로 Polyline(경로)을 그려주는 함수
 * - coords 형식: [{ latitude: number, longitude: number }, ...]
 * - 보통 백엔드(Tmap) 응답의 coordinates 배열을 그대로 사용하면 됩니다.
 */
function drawRouteOnKakaoMap(map, coords) {
  // 유효한 좌표 배열인지 확인
  if (!Array.isArray(coords) || coords.length < 2) {
    console.warn('경로를 그리려면 2개 이상의 좌표가 필요합니다.')
    return
  }

  // Kakao LatLng 배열로 변환 (new kakao.maps.LatLng(위도, 경도))
  const path = coords.map((c) => new kakao.maps.LatLng(c.latitude, c.longitude))

  // Polyline 생성
  const polyline = new kakao.maps.Polyline({
    path,
    strokeWeight: 5,
    strokeColor: '#FF3B30',
    strokeOpacity: 0.9,
    strokeStyle: 'solid',
  })

  // 지도에 Polyline 표시
  polyline.setMap(map)

  // 경로 전체가 한 화면에 보이도록 지도 범위 맞추기
  const bounds = new kakao.maps.LatLngBounds()
  path.forEach((latLng) => bounds.extend(latLng))
  map.setBounds(bounds)
}

/**
 * SearchRouteView에서 전달된 coordinates로 지도에 경로를 그립니다.
 * - 전달 데이터가 없으면 아무 작업도 하지 않고 경고만 출력합니다.
 */
async function fetchRouteAndDraw(map) {
  try {
    // SearchRouteView에서 전달된 좌표 사용
    const stored = sessionStorage.getItem('routeCoordinates')
    if (stored) {
      const coords = JSON.parse(stored)
      if (Array.isArray(coords) && coords.length >= 2) {
        drawRouteOnKakaoMap(map, coords)
        return
      }
    }
    console.warn('routeCoordinates가 없어 경로를 표시하지 않습니다.')
  } catch (err) {
    console.error('경로 요청 중 오류:', err)
  }
}
</script>

<style scoped>
.map-box {
  width: 100%;
  height: 700px;
}
</style>


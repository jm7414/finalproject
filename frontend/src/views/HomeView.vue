<template>
  <div ref="mapContainer" class="map-box"></div>
  <div class="row g-3 mt-4"></div>


</template>

<script setup>
import { ref, onMounted } from 'vue'

const mapContainer = ref(null)

/* 카카오 지도 (그대로) */
const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
onMounted(() => {
  loadKakaoMap(mapContainer.value)
})
function loadKakaoMap(container) {
  const script = document.createElement('script')
  // 현재 service, cluster, drawing 라이브러리를 모두 활성화 시킴
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services,clusterer,drawing&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      const options = {
        center: new window.kakao.maps.LatLng(33.450701, 126.570667),
        level: 4,
        maxLevel: 5,
      }
      const mapInstance = new window.kakao.maps.Map(container, options)
      // 지도 중앙에 마커 생성
      const marker = new kakao.maps.Marker({
        position: mapInstance.getCenter(),
        map: mapInstance,
      })

      // =============================
      // 임의의 좌표값으로 Polyline(경로) 그리기
      // - 실제 백엔드(Tmap) 연동 전, 프런트에서 좌표 배열을 넣었을 때 경로가 정상적으로 그려지는지 확인하기 위한 테스트 코드에용
      // =============================

      // 1) 샘플 좌표 데이터 (latitude: 위도, longitude: 경도)
      //    - 지도 초기 중심(제주) 근처로 3개의 점을 찍어 가상의 경로를 만듭니다.
      const sampleCoordinates = [
        { latitude: 33.450701, longitude: 126.570667 },
        { latitude: 33.451701, longitude: 126.561667 },
        { latitude: 33.452701, longitude: 126.572667 },
      ]

      // 2) 헬퍼 함수를 사용해 경로를 지도 위에 그립니다.
      drawRouteOnKakaoMap(mapInstance, sampleCoordinates)
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

  // Kakao LatLng 배열로 변환
  // - Kakao 지도는 new kakao.maps.LatLng(위도, 경도) 순서를 사용합니다.
  const path = coords.map((c) => new kakao.maps.LatLng(c.latitude, c.longitude))

  // Polyline 생성
  // - strokeWeight: 선 두께(px)
  // - strokeColor: 선 색상(hex)
  // - strokeOpacity: 선 투명도(0~1)
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

  // setBounds(남서-북동) + 패딩(px) 값으로 여백을 둡니다.

  map.setBounds(bounds)
}
</script>

<style scoped>
.map-box {
  width: 100%;
  height: 700px;
}
</style>

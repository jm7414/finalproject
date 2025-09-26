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
    })
  }
}
</script>

<style scoped>
.map-box {
  width: 100%;
  height: 700px;
}
</style>

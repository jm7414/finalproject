<template>
  <!-- 지도 + 하위 UI -->
  <div class="form-row">
    <!-- 지도 -->
    <div ref="mapContainer" class="map-box"></div>
    <div class="row g-3 mt-4">
      <div class="col-4">
        <button type="button" class="btn btn-light w-100 py-6 fw-semibold shadow-sm rounded-3 minh-84">
          📜 리포트
        </button>
      </div>
      <div class="col-4">
        <button type="button" class="btn btn-light w-100 py-6 fw-semibold shadow-sm rounded-3 minh-84">
          🗺️ 지도 미리보기
        </button>
      </div>
      <div class="col-4">
        <button type="button" class="btn btn-light w-100 py-6 fw-semibold shadow-sm rounded-3 minh-84">
          🆘 종합지원
        </button>
      </div>
    </div>

    <!-- 오늘의 일정 -->
    <section class="card mt-4">
      <div class="card-header d-flex align-items-center justify-content-between py-3">
        <h3 class="card-title m-0 fs-5 fw-bold">오늘의 일정</h3>
        <div class="card-toolbar">
          <button type="button" class="btn btn-sm btn-light border-dashed">+ 일정 추가</button>
        </div>
      </div>

      <div class="card-body py-3">
        <ul class="list-unstyled m-0">
          <li class="d-flex align-items-center gap-3 py-2">
            <span class="dot bg-success"></span>
            <div>
              <div class="fw-semibold">약 먹기</div>
              <small class="text-muted">08:30 - 아침 식후</small>
            </div>
          </li>
          <li class="d-flex align-items-center gap-3 py-2">
            <span class="dot bg-success"></span>
            <div>
              <div class="fw-semibold">출근</div>
              <small class="text-muted">15:00 - 도보 30분</small>
            </div>
          </li>
          <li class="d-flex align-items-center gap-3 py-2">
            <span class="dot bg-success"></span>
            <div>
              <div class="fw-semibold">퇴근</div>
              <small class="text-muted">22:30 - 도보 30분</small>
            </div>
          </li>
        </ul>
      </div>
    </section>

    <!-- 실종자 -->
    <section class="missing bg-danger-subtle">
      <div class="missing-font d-flex align-items-center justify-content-between gap-3">
        <div>
          <strong class="d-block fs-5">실종자 알림</strong>
          <p class="m-0">이름: 홍길동 (남, 78) · 마지막 위치: 청주</p>
        </div>
        <button type="button" class="btn btn-danger fw-semibold rounded-3">자세히 보기</button>
      </div>
    </section>
  </div>
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
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      const options = {
        center: new window.kakao.maps.LatLng(33.450701, 126.570667),
        level: 3,
        maxLevel: 5,
      }
      const mapInstance = new window.kakao.maps.Map(container, options)
      const marker = new kakao.maps.Marker({
        position: mapInstance.getCenter(),
        map: mapInstance,
      })
    })
  }
}
</script>

<style scoped>
/* 지도 영역*/
.map-box {
  width: 100%;
  height: 200px;
  border: 1.5px solid rgba(18, 232, 216, .4);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 8px 18px rgba(0, 0, 0, .05);
}


/* 일정에 있는 초록색 점 */
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}


.missing {
  width: 100vw;
  margin-left: calc(50% - 50vw);
  margin-right: calc(50% - 50vw);
  margin-top: 18px;
}

.missing-font {
  padding: 14px 16px;
}
</style>
 
<script setup>
import { ref, watch, onUnmounted } from 'vue';
import axios from 'axios';
// (필요하다면 카카오맵 마커 이미지 생성 함수도 가져옵니다)

// 1. 부모(PredictLocation)로부터 '지도 객체'와 '실종 ID'를 받습니다.
const props = defineProps({
  map: { type: Object, required: true },
  missingPostId: { type: Number, required: true }
});

const participantMarkers = ref([]); // 이 컴포넌트가 관리할 마커 목록
let pollingTimer = null; // 5초마다 위치를 물어볼 타이머

// 2. 지도(map)가 준비되면, 실시간 위치 조회를 시작합니다.
watch(() => props.map, (newMap) => {
  if (newMap) {
    console.log("ParticipantsLayer: 지도 객체를 받았습니다. 폴링을 시작합니다.");
    startPolling();
  }
}, { immediate: true });

// 3. API로 '함께하는 사람' 위치 목록을 가져오는 함수
async function fetchParticipantLocations() {
  if (!props.missingPostId) return;

  try {
    // (예시 API) /api/missing-persons/{id}/participants/locations
    const response = await axios.get(
      `/api/missing-persons/${props.missingPostId}/participants/locations`,
      { withCredentials: true }
    );
    
    // (이전 마커는 모두 지웁니다)
    clearMarkers();
    
    // (새 마커를 그립니다)
    response.data.forEach(person => {
      const position = new window.kakao.maps.LatLng(person.lat, person.lon);
      const marker = new window.kakao.maps.Marker({
        map: props.map, // ⭐ 부모에게 받은 지도에 그립니다.
        position: position,
        // (image: ... 이웃용 마커 이미지)
      });
      participantMarkers.value.push(marker);
    });

  } catch (err) {
    console.error("참여자 위치 조회 실패:", err);
  }
}

// 4. 폴링 (5초마다 반복 실행)
function startPolling() {
  stopPolling(); // 혹시 모를 타이머 중복 방지
  fetchParticipantLocations(); // 일단 1회 즉시 실행
  
  pollingTimer = setInterval(() => {
    fetchParticipantLocations();
  }, 5000); // 5초마다 반복
}

function stopPolling() {
  if (pollingTimer) clearInterval(pollingTimer);
}

// 5. 마커 지우기
function clearMarkers() {
  participantMarkers.value.forEach(marker => marker.setMap(null));
  participantMarkers.value = [];
}

// 6. 이 컴포넌트가 사라질 때(예: v-if=false) 타이머와 마커를 모두 정리
onUnmounted(() => {
  console.log("ParticipantsLayer: 컴포넌트 정리. 폴링 중지 및 마커 삭제.");
  stopPolling();
  clearMarkers();
});

</script>

<template>
  <div class="participant-layer-logic-only"></div>
</template>
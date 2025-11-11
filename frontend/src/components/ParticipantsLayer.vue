<script setup>
import { ref, watch, onUnmounted } from 'vue';
import axios from 'axios';

// 1. 부모(PredictLocation)로부터 '지도 객체'와 '실종 ID'를 받습니다.
const props = defineProps({
  map: { type: Object, required: true },
  missingPostId: { type: Number, required: true }
});

const participantMarkers = ref([]); // 이 컴포넌트가 관리할 마커 목록
let pollingTimer = null; // 5초마다 위치를 물어볼 타이머

// 2. 지도(map)와 ID(missingPostId)가 모두 준비되면 폴링을 시작합니다.
watch(() => [props.map, props.missingPostId], ([newMap, newId]) => {
  if (newMap && newId) {
    console.log(`ParticipantsLayer: 지도와 ID(${newId})를 받았습니다. 폴링을 시작합니다.`);
    startPolling();
  }
}, { immediate: true }); // 즉시 실행

// 3. API로 '함께하는 사람' 위치 목록을 가져오는 함수
async function fetchParticipantLocations() {
  if (!props.missingPostId) return;

  try {
    // (예시 API) /api/missing-persons/{id}/participants/locations
    const response = await axios.get(
      `/api/missing-persons/${props.missingPostId}/participants/locations`,
      { withCredentials: true }
    );

    // 이전 마커는 모두 지웁니다
    clearMarkers();

    // 새 마커를 그립니다
    response.data.forEach(person => {
      const position = new window.kakao.maps.LatLng(person.latitude, person.longitude);
      const marker = new window.kakao.maps.Marker({
        map: props.map, // ⭐ 부모에게 받은 지도에 그립니다.
        position: position,
        // (image: ... 이웃용 마커 이미지)
      });
      participantMarkers.value.push(marker);
    });

  } catch (err) {
    console.error("참여자 위치 조회 실패:", err);
    // (404는 백엔드 API가 아직 없다는 뜻)
  }
}

// 4. 폴링 시작 함수
function startPolling() {
  stopPolling(); // 중복 방지
  fetchParticipantLocations(); // 1회 즉시 실행
  pollingTimer = setInterval(fetchParticipantLocations, 5000); // 5초마다 반복
}

// 5. 폴링 중지 함수
function stopPolling() {
  if (pollingTimer) clearInterval(pollingTimer);
  pollingTimer = null;
}

// 6. 마커 지우기
function clearMarkers() {
  participantMarkers.value.forEach(marker => marker.setMap(null));
  participantMarkers.value = [];
}

// 7. 이 컴포넌트가 사라질 때 (v-if=false) 모든 것을 정리합니다.
onUnmounted(() => {
  console.log("ParticipantsLayer: 컴포넌트 정리 (폴링 중지, 마커 삭제)");
  stopPolling();
  clearMarkers();
});

</script>

<template>
  <div class="participant-layer-logic-only"></div>
</template>
import { ref, onUnmounted, watch } from 'vue';
import axios from 'axios';

// props: 부모(PredictLocation)로부터 'map'과 'missingPostId'를 받음
// (ref 객체를 받기 위해 props 대신 구조화된 객체를 인수로 받음)
export function useParticipantLocations({ map, missingPostId }) {
    
    // 1. 상태: 마커 배열과 타이머
    const participantMarkers = ref([]);
    let pollingTimer = null;

    // 2. API 호출: '함께하는 사람들'의 위치를 가져옴
    async function fetchParticipantLocations() {
        // (지도와 ID가 ref.value로 준비되었는지 확인)
        if (!map.value || !missingPostId.value) {
            console.warn("참여자 위치 조회: 지도 또는 ID가 준비되지 않음");
            return;
        }

        try {
            // (404가 났던 바로 그 API 호출)
            const response = await axios.get(
                `/api/missing-persons/${missingPostId.value}/participants/locations`,
                { withCredentials: true }
            );
            console.log("백엔드로부터 받은 참여자 데이터:", response.data);
            
            // (이전 마커 모두 삭제)
            clearMarkers();
            
            // (N명의 마커 새로 그리기)
            response.data.forEach(person => {
                // (백엔드가 LocationResponseDTO를 반환한다고 가정)
                const position = new window.kakao.maps.LatLng(person.latitude, person.longitude);
                
                // (TODO: 이웃용 마커 아이콘 생성 로직 추가)
                const marker = new window.kakao.maps.Marker({
                    map: map.value, // 부모의 지도 사용
                    position: position,
                });
                participantMarkers.value.push(marker); // 마커 배열에 추가
            });
            console.log(`[useParticipantLocations] 참여자 ${response.data.length}명의 위치를 표시합니다.`);

        } catch (err) {
            console.error("[useParticipantLocations] 참여자 위치 조회 실패:", err);
            // (404는 백엔드 API가 아직 없다는 뜻)
            stopParticipantTracking(); // 오류 발생 시 폴링 중지
        }
    }

    // 3. 마커 지우기
    function clearMarkers() {
        participantMarkers.value.forEach(marker => marker.setMap(null));
        participantMarkers.value = [];
    }

    // 4. 추적 시작 (wherePeople 함수가 호출)
    function startParticipantTracking() {
        console.log("▶️ '함께하는 사람' 위치 추적 시작");
        stopParticipantTracking(); // 중복 방지
        
        // 지도와 ID가 준비될 때까지 잠시 기다렸다가 실행
        watch([map, missingPostId], ([newMap, newId]) => {
            if (newMap && newId) {
                fetchParticipantLocations(); // 1회 즉시 실행
                // 타이머 재설정
                if (pollingTimer) clearInterval(pollingTimer);
                pollingTimer = setInterval(fetchParticipantLocations, 5000); // 5초마다 반복
            }
        }, { immediate: true }); // 즉시 실행
    }

    // 5. 추적 중지 (wherePeople 함수가 다시 호출되거나, 페이지 나갈 때)
    function stopParticipantTracking() {
        console.log("⏹️ '함께하는 사람' 위치 추적 중지");
        if (pollingTimer) clearInterval(pollingTimer);
        pollingTimer = null;
        clearMarkers(); // 추적 중지 시 마커도 삭제
    }

    // 6. PredictLocation.vue가 언마운트될 때 정리
    onUnmounted(() => {
        stopParticipantTracking();
    });

    // PredictLocation.vue가 사용할 함수들을 반환
    return {
        startParticipantTracking,
        stopParticipantTracking
    };
}
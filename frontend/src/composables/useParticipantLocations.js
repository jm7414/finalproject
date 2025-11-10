import { ref, onUnmounted } from 'vue';
import axios from 'axios';

// props: 부모(PredictLocation)로부터 'map'과 'missingPostId'를 받음
export function useParticipantLocations(props) {
    
    // 1. 상태: 마커 배열과 타이머
    const participantMarkers = ref([]);
    let pollingTimer = null;

    // 2. API 호출: '함께하는 사람들'의 위치를 가져옴
    async function fetchParticipantLocations() {
        if (!props.map.value || !props.missingPostId.value) {
            console.warn("참여자 위치 조회: 지도 또는 ID가 준비되지 않음");
            return;
        }

        try {
            // ⭐ [로직 변경] API 주소 변경
            const response = await axios.get(
                `/api/missing-persons/${props.missingPostId.value}/participants/locations`,
                { withCredentials: true }
            );

            // ⭐ [로직 변경] 이전 마커 모두 삭제
            clearMarkers();

            // ⭐ [로직 변경] N명의 마커 새로 그리기
            response.data.forEach(person => {
                const position = new window.kakao.maps.LatLng(person.lat, person.lon);
                
                // (이웃용 아이콘이 있다면 여기서 image: ... 로 수정)
                const marker = new window.kakao.maps.Marker({
                    map: props.map.value, // 부모의 지도 사용
                    position: position,
                });
                participantMarkers.value.push(marker); // 마커 배열에 추가
            });

        } catch (err) {
            console.error("참여자 위치 조회 실패:", err);
            // (404는 백엔드 API가 아직 없다는 뜻이므로 정상)
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
        fetchParticipantLocations(); // 1회 즉시 실행
        pollingTimer = setInterval(fetchParticipantLocations, 5000); // 5초마다 반복
    }

    // 5. 추적 중지 (wherePeople 함수가 다시 호출되거나, 페이지 나갈 때)
    function stopParticipantTracking() {
        console.log("⏹️ '함께하는 사람' 위치 추적 중지");
        if (pollingTimer) clearInterval(pollingTimer);
        pollingTimer = null;
        clearMarkers(); // 추적 중지 시 마커도 삭제
    }

    // 6. 컴포넌트 소멸 시 정리
    onUnmounted(() => {
        stopParticipantTracking();
    });

    // 부모(PredictLocation)가 사용할 함수들을 반환
    return {
        startParticipantTracking,
        stopParticipantTracking
    };
}
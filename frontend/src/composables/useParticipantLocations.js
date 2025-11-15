import { ref, onUnmounted, watch } from 'vue';
import axios from 'axios';

// ✅ 수정: props에서 'map' 제거, 'missingPostId'만 받음
export function useParticipantLocations({ missingPostId }) {
    
    const participantMarkers = ref([]);
    let pollingTimer = null;
    
    // ✅ 1. 내부에서 사용할 'map' 변수 선언 (ref 아님)
    let internalMap = null;

    // 2. API 호출
    async function fetchParticipantLocations() {
        const postId = missingPostId.value; // (missingPostId는 ref가 맞습니다)

        // 'internalMap'이 주입되었는지, 'postId'가 있는지 확인
        if (!internalMap || !postId) {
            console.warn("참여자 위치 조회: 지도(internalMap) 또는 ID가 준비되지 않음");
            return;
        }

        try {
            const response = await axios.get(
                `/api/missing-persons/${postId}/participants/locations`,
                { withCredentials: true }
            );
            
            // 기존 콘솔 로그 (데이터 원본)
            console.log("백엔드로부터 받은 참여자 데이터:", response.data);
            
            clearMarkers(); // 이전 마커 삭제
            
            response.data.forEach((person, index) => {
                // ✅ 2. (요청하신 부분) 참여자 정보와 위도/경도 콘솔에 출력
                console.log(`[참여자 ${index + 1} 정보]:`, person);
                console.log(`[참여자 ${index + 1} 위치]: Lat ${person.latitude}, Lon ${person.longitude}`);

                // 위도, 경도 값이 유효한지 확인 (마커가 안 뜨는 2차 원인일 수 있음)
                if (!person.latitude || !person.longitude) {
                    console.warn(`[참여자 ${index + 1}]의 위치 정보가 유효하지 않습니다.`);
                    return; // 위치 없으면 마커 생성 건너뛰기
                }

                const position = new window.kakao.maps.LatLng(person.latitude, person.longitude);
                
                // ✅ 3. 'map.value' 대신 'internalMap' 변수 사용
                const marker = new window.kakao.maps.Marker({
                    map: internalMap, 
                    position: position,
                    // (TODO: 이웃용 마커 아이콘 생성 로직)
                });
                participantMarkers.value.push(marker);
            });
            console.log(`[useParticipantLocations] 참여자 ${response.data.length}명의 위치를 표시합니다.`);

        } catch (err) {
            console.error("[useParticipantLocations] 참여자 위치 조회 실패:", err);
            stopParticipantTracking();
        }
    }

    // 3. 마커 지우기
    function clearMarkers() {
        participantMarkers.value.forEach(marker => marker.setMap(null));
        participantMarkers.value = [];
    }

    // 4. 추적 시작
    function startParticipantTracking() {
        console.log("▶️ '함께하는 사람' 위치 추적 시작");
        stopParticipantTracking(); // 중복 타이머 방지
        
        // ✅ 4. 'map' 감시 제거, 'missingPostId'만 감시
        watch(missingPostId, (newId) => {
            // 'internalMap'이 준비되었는지 수동으로 확인
            if (internalMap && newId) {
                fetchParticipantLocations(); // 1회 즉시 실행
                if (pollingTimer) clearInterval(pollingTimer);
                pollingTimer = setInterval(fetchParticipantLocations, 5000); // 5초마다 반복
            }
        }, { immediate: true }); // 즉시 실행
    }

    // 5. 추적 중지
    function stopParticipantTracking() {
        console.log("⏹️ '함께하는 사람' 위치 추적 중지");
        if (pollingTimer) clearInterval(pollingTimer);
        pollingTimer = null;
        clearMarkers();
    }

    // ✅ 5. (가장 중요) 부모에서 map 객체를 주입할 함수 (NEW)
    const setMap = (mapObject) => {
        console.log('useParticipantLocations: Map 객체를 주입받았습니다.');
        internalMap = mapObject; // 내부 변수에 지도 할당

        // 지도를 받았는데, 만약 ID도 이미 준비된 상태고 타이머가 없다면 추적 시작
        if (internalMap && missingPostId.value && !pollingTimer) {
             console.log('useParticipantLocations: Map과 ID가 모두 준비되어 추적을 시작합니다.');
             fetchParticipantLocations(); // 즉시 1회 실행
             if (pollingTimer) clearInterval(pollingTimer);
             pollingTimer = setInterval(fetchParticipantLocations, 5000);
        }
    };

    // 6. 컴포넌트 언마운트 시 정리
    onUnmounted(() => {
        stopParticipantTracking();
    });

    // ✅ 6. 'setMap' 함수를 반환하여 부모가 사용할 수 있게 함
    return {
        startParticipantTracking,
        stopParticipantTracking,
        setMap 
    };
}
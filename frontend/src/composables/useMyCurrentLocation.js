import { onUnmounted, watch, ref, computed } from 'vue';
import axios from 'axios';
import { useSearchStore } from '@/stores/useSearchStore';

/**
 * '함께 찾기' 스위치를 감시하고,
 * 'isSearching'이 true일 때 5초마다 '내 위치'를 POST로 전송하는 엔진
 *
 * @param userNoRef (App.vue로부터 userNo를 받음)
 */
export function useMyCurrentLocation(userNoRef) {

    const searchStore = useSearchStore();
    let pollingTimer = null;

    // isSearching 뿐만 아니라 userNo가 준비되었는지도 함께 감시
    const isReady = computed(() => searchStore.isSearching && userNoRef.value > 0); // 0보다 큰지 확인


    // ===================================================================
    // ⭐ 1. 함수 선언을 모두 위로 올립니다.
    // ===================================================================

    // 위치 전송 시작
    function startSendingLocation() {
        stopSendingLocation(); // 중복 방지
        console.log("[useMyCurrentLocation] '함께 찾기' 참여자 위치 전송을 시작합니다.");
        sendLocation(); // 즉시 1회 실행
        pollingTimer = setInterval(sendLocation, 5000); // 5초마다 반복
    }

    // 위치 전송 중지
    function stopSendingLocation() {
        if (pollingTimer) {
            console.log("[useMyCurrentLocation] 위치 전송을 중지합니다.");
            clearInterval(pollingTimer);
            pollingTimer = null;
        }
    }

    // 실제 위치를 서버로 POST하는 함수
    async function sendLocation() {
        const myUserNo = userNoRef.value;

        console.log(`[MyLoc Debug] @@@@@@@@@@ sendLocation 함수 실행 시도. userNo: ${myUserNo}`);

        if (!myUserNo) {
            console.warn("[MyLoc] 로그인 ID(userNo)가 없어 위치 전송 불가.");
            stopSendingLocation();
            return;
        }

        try {
            const position = await new Promise((resolve, reject) => {
                navigator.geolocation.getCurrentPosition(resolve, reject, {
                    enableHighAccuracy: true, timeout: 5000, maximumAge: 0
                });
            });

            const { latitude, longitude } = position.coords;

            const locationData = {
                userNo: myUserNo,
                latitude: latitude,
                longitude: longitude,
                timestamp: Date.now()
            };

            await axios.post('/api/location/update', locationData, {
                withCredentials: true
            });

            console.log(`[MyLoc] ✅ 참여자(ID:${myUserNo}) 위치 전송 성공.`);

        } catch (err) {
            console.error("[MyLoc] ❌ 위치 전송/조회 실패:", err);
        }
    }

    // ===================================================================
    // ⭐ 2. watch와 onUnmounted를 함수 선언 *아래*로 내립니다.
    // ===================================================================

    // isReady (isSearching && userNo) 상태를 감시
    watch(isReady, (isReadyNow) => {
        console.log(`[MyLoc Debug] @@@@@@@@@@ isReady 상태 변경 감지: ${isReadyNow}`);
        if (isReadyNow) {
            startSendingLocation(); // (이제 이 함수는 위에 선언되어 있음)
        } else {
            stopSendingLocation(); // (이제 이 함수는 위에 선언되어 있음)
        }
    }, { immediate: true }); // 즉시 1회 체크

    onUnmounted(() => {
        stopSendingLocation();
    });
}
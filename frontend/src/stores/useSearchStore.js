import { defineStore } from 'pinia';
import { ref } from 'vue';

/**
 * '함께 찾기' 상태를 전역으로 관리하는 스토어
 * (이웃 앱이 자신의 위치를 전송해야 하는지 여부를 결정)
 */
export const useSearchStore = defineStore('search', () => {
    
    // 1. 내가 현재 '함께 찾기'에 참여 중인지 여부 (true/false)
    const isSearching = ref(false);
    
    // 2. 내가 참여 중인 실종 신고의 ID (예: 2)
    const activeMissingPostId = ref(null);

    /**
     * '함께 찾기' 시작 (MissingDetailModal에서 호출)
     */
    function startSearch(missingPostId) {
        console.log(`[SearchStore] '함께 찾기' 시작: ID ${missingPostId}`);
        isSearching.value = true;
        activeMissingPostId.value = missingPostId;
        // (참고: 새로고침해도 유지하려면 localStorage에 저장하는 로직 추가)
    }

    /**
     * '함께 찾기' 중지 (예: 실종이 해제되었을 때)
     */
    function stopSearch() {
        console.log(`[SearchStore] '함께 찾기' 중지.`);
        isSearching.value = false;
        activeMissingPostId.value = null;
    }

    return { isSearching, activeMissingPostId, startSearch, stopSearch };
});
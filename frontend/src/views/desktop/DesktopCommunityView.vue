<template>
  <div class="desktop-community-page">
    <!-- 1. 상단 가로 네비게이션 메뉴 (기존 sub-nav 수정) -->
    <nav class="top-nav">
      <button 
        type="button"
        class="top-nav-item"
        :class="{ active: activeTab === 'Post' }"
        @click="changeTab('Post')"
      >
        <!-- (아이콘 예시) -->
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H3a2 2 0 0 0-2 2v12c0 1.1.9 2 2 2h9z"></path><polyline points="16 2 16 6 8 6 8 2"></polyline><line x1="4" x2="20" y1="10" y2="10"></line></svg>
        <span>게시글</span>
      </button>
      <button 
        type="button"
        class="top-nav-item"
        :class="{ active: activeTab === 'Missing' }"
        @click="changeTab('Missing')"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" x2="16.65" y1="21" y2="16.65"></line><line x1="11" x2="11" y1="8" y2="14"></line><line x1="8" x2="14" y1="11" y2="11"></line></svg>
        <span>실종</span>
      </button>
      <button 
        type="button"
        class="top-nav-item"
        :class="{ active: activeTab === 'Event' }"
        @click="changeTab('Event')"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 11c0-3.866 3.134-7 7-7s7 3.134 7 7v5c0 3.866-3.134 7-7 7s-7-3.134-7-7v-5z"></path><path d="M9 11c0-1.657 1.343-3 3-3s3 1.343 3 3v5c0 1.657-1.343 3-3 3s-3-1.343-3-3v-5z"></path><line x1="12" x2="12" y1="19" y2="22"></line></svg>
        <span>동네 소식</span>
      </button>
    </nav>

    <!-- 2. 메인 콘텐츠 영역 (기존 content-area) -->
    <main class="content-area">
      <!-- 
        v-if를 사용해 선택된 탭의 컴포넌트 '하나만' 렌더링합니다.
        (모바일 CommunityView.vue와 동일한 로직)
      -->
      <CommunityBoard v-if="activeTab === 'Post'" />
      <CommunityMissing v-if="activeTab === 'Missing'" />
      <CommunityEvent v-if="activeTab === 'Event'" />
    </main>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

// 3개의 '채널' 컴포넌트를 모두 불러옵니다.
import CommunityMissing from '@/components/CommunityMissing.vue';
import CommunityBoard from '@/components/CommunityBoard.vue';
import CommunityEvent from '@/components/CommunityEvent.vue';

const route = useRoute();
const router = useRouter();

// URL 쿼리 파라미터(?tab=...)를 읽어 현재 탭을 결정합니다.
// '게시글(Post)'을 기본값으로 설정합니다.
const activeTab = ref(route.query.tab || 'Post'); 

// 탭 변경 시 URL도 /desktop/community?tab=Missing 처럼 변경합니다.
// (새로고침, URL 공유, 뒤로가기 버튼 대응)
function changeTab(tabName) {
  activeTab.value = tabName;
  router.push({ query: { tab: tabName } });
}

// 브라우저 뒤로가기/앞으로가기 버튼을 눌렀을 때(URL 쿼리가 바뀔 때)
// activeTab을 동기화합니다.
watch(() => route.query.tab, (newTab) => {
  if (newTab && newTab !== activeTab.value) {
    activeTab.value = newTab;
  } else if (!newTab) {
    // 쿼리 파라미터가 없으면 기본값('Post')으로 설정
    activeTab.value = 'Post';
  }
});
</script>

<style scoped>
.desktop-community-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}


.top-nav {
  display: flex;
  flex-direction: row; /* 가로 방향 배치 */
  flex-shrink: 0;
  border-bottom: 2px solid #e5e7eb; /* 하단 구분선 */
  margin-bottom: 24px; /* 콘텐츠 영역과의 간격 */
}

.top-nav-item {
  display: flex;
  align-items: center;
  gap: 8px; /* 아이콘과 텍스트 간격 */
  padding: 16px 4px; /* 상하 여백, 좌우는 약간만 */
  margin-right: 28px; /* 탭 간의 간격 */
  border: none;
  background: transparent;
  font-size: 16px; /* 글씨 크기 약간 키움 */
  font-weight: 600;
  text-align: left;
  color: #374151; /* 기본 텍스트 색상 */
  cursor: pointer;
  transition: all 0.2s ease;
  
  border-bottom: 3px solid transparent;
  margin-bottom: -2px; /* 하단 구분선과 겹치도록 */
}

.top-nav-item svg {
  color: #6b7280;
  transition: all 0.2s ease;
}

.top-nav-item:hover {
  background: none; /* 배경색 대신 하단 보더 색상 변경 */
  border-bottom-color: #d1d5db; /* 호버 시 연한 회색 보더 */
  color: #111827;
}

.top-nav-item.active {
  background: none; /* 배경색 제거 */
  color: #6366f1; /* 메인 보라색 */
  border-bottom-color: #6366f1; /* 활성화 시 보라색 보더 */
}

.top-nav-item.active svg {
  color: #6366f1;
}

.content-area {
  flex: 1; /* 남은 세로 공간 모두 차지 */
  min-width: 0;
  min-height: 0; /* flex 자식 요소가 스크롤되도록 */
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  overflow: hidden;
  
  display: flex;
  flex-direction: column;
}

/* v-if로 렌더링되는 자식 컴포넌트들이 
  이 영역을 꽉 채우고 스스로 스크롤하도록
*/
.content-area > :deep(div) {
  height: 100%;
}
</style>
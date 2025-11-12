<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router'; 

// 3개의 '채널' 컴포넌트를 모두 불러옵니다. (경로는 실제 파일 위치에 맞게 조정하세요)
import CommunityMissing from '@/components/CommunityMissing.vue';
import CommunityBoard from '@/components/CommunityBoard.vue';
import CommunityEvent from '@/components/CommunityEvent.vue';

const route = useRoute();
const router = useRouter();

// 기본으로 보여줄 탭(채널)을 요청이 있는지 살펴본 후 'Post'로 이동하게 설정
const activeTab = ref(route.query.tab || 'Post'); 

// URL도 /communityView?tab=Missing 처럼 변경해서, 새로고침해도 탭이 유지되도록 함
function changeTab(tabName) {
  activeTab.value = tabName;
  router.push({ query: { tab: tabName } });
}

watch(() => route.query.tab, (newTab) => {
  if (newTab && newTab !== activeTab.value) {
    activeTab.value = newTab;
  }
});

</script>

<template>
  <div class="community-view-container">
    <div class="main-nav">
      <div class="main-div-8">
        <div class="main-button" 
             :class="{ active: activeTab === 'Post' }" 
             @click="changeTab('Post')">
          <span class="post-button">게시글</span>
        </div>

        <div class="missing-button" 
             :class="{ active: activeTab === 'Missing' }" 
             @click="changeTab('Missing')">
          <span class="event-button">실종</span>
        </div>

        <div class="main-div-9" 
             :class="{ active: activeTab === 'Event' }" 
             @click="changeTab('Event')">
          <span class="main-div-a">동네소식</span>
        </div>
      </div>
    </div>

    <div class="content-area">
      <CommunityMissing v-if="activeTab === 'Missing'" />
      <CommunityBoard v-if="activeTab === 'Post'" />
      <CommunityEvent v-if="activeTab === 'Event'" />
    </div>
  </div>
</template>

<style scoped>
/* 전체 레이아웃 */
.community-view-container {
  padding: 10px;
  margin-top: -30px;
  background: #ffffff;
}

/* 탭 네비게이션 스타일은 이전에 완성했던 
  디자인의 스타일을 그대로 가져옵니다. 
*/
.main-nav {
  width: 100%;
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;
  background: #ffffff;
  border-top: 1px solid #e5e5e5;
  border-bottom: 1px solid #e5e5e5; /* 탭과 콘텐츠 구분선 */
}
.main-div-8 {
  display: flex;
  height: 46px;
}
.main-button, .missing-button, .main-div-9 {
  flex: 1;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  border-bottom: 2px solid transparent; 
  cursor: pointer;
  transition: all 0.2s ease;
}
.post-button, .event-button, .main-div-a {
  font-family: Inter, var(--default-font-family);
  font-size: 14px;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
  color: #737373; /* 비활성화 시 텍스트 색상 */
}

/* 'active' 클래스가 붙었을 때의 스타일 */
.main-button.active,
.missing-button.active,
.main-div-9.active {
  border-bottom-color: #8e97fd;
}
.main-button.active .post-button,
.missing-button.active .event-button,
.main-div-9.active .main-div-a {
  color: #171717; /* 활성화 시 텍스트 색상 */
  font-weight: 700;
}

/* 콘텐츠가 표시될 영역 */
.content-area {
  width: 100%;
}
</style>
<script setup>
import { ref } from 'vue';

// 3개의 '부품' 컴포넌트를 모두 불러옵니다. (경로는 실제 파일 위치에 맞게 조정하세요)
import CommunityMissing from '@/components/CommunityMissing.vue';
import CommunityPost from '@/components/CommunityBoard.vue';
import CommunityEvent from '@/components/CommunityEvent.vue';

// 기본으로 보여줄 탭을 'Missing'으로 설정합니다.
const activeTab = ref('Missing'); 

// 탭을 변경하는 함수입니다.
function changeTab(tabName) {
  activeTab.value = tabName;
}
</script>

<template>
  <div class="community-container">
    <div class="filter-buttons">
       <button 
         class="filter-button"
         :class="{ active: activeTab === 'Missing' }"
         @click="changeTab('Missing')">실종</button>
       <button 
         class="filter-button"
         :class="{ active: activeTab === 'Post' }"
         @click="changeTab('Post')">게시글</button>
       <button 
         class="filter-button"
         :class="{ active: activeTab === 'Event' }"
         @click="changeTab('Event')">이벤트</button>
    </div>

    <div class="content-area">
      <CommunityMissing v-if="activeTab === 'Missing'" />
      <CommunityPost v-if="activeTab === 'Post'" />
      <CommunityEvent v-if="activeTab === 'Event'" />
    </div>
  </div>
</template>

<style scoped>
.community-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 50px;
  gap: 20px;
  width: 100%;
  max-width: 500px; /* 최대 너비는 자유롭게 조절하세요 */
  margin: 0 auto;
}

.filter-buttons {
  display: flex;
  flex-direction: row;
  gap: 12px;
  width: 100%;
}

.filter-button {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 38px; /* 버튼 높이를 살짝 키웠습니다 */
  padding: 0 16px; 
  border-radius: 19px; /* 높이에 맞춰 둥글게 */
  border: 1px solid #E0E0E0;
  background: #FFFFFF;
  font-weight: 600;
  font-size: 15px;
  color: #A0A0A0;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
}

.filter-button.active {
  background: #808AFF;
  color: #FFFFFF;
  border-color: #808AFF;
  box-shadow: 0px 4px 10px rgba(128, 138, 255, 0.4);
}

.content-area {
  width: 100%;
}
</style>
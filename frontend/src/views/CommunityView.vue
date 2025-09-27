<script setup>

import { ref } from 'vue';

import CommunityMissingList from '@/components/CommunityMissing.vue';
import CommunityPostList from '@/components/CommunityPost.vue';
import CommunityEventList from '@/components/CommunityEvent.vue';

const activeTab = ref('Missing');

function changeTab(tabName) {
  activeTab.value = tabName;
}

</script>


<template>
  <div class="post-container">
    <div class="content-area">
      <CommunityMissing v-if="activeTab === 'Missing'" />
      <CommunityPost v-if="activeTab === 'Post'" />
      <CommunityEvent v-if="activeTab === 'Event'" />
    </div>
  </div>
</template>



<style scoped>
/* 전체적인 폰트 스타일 및 박스 크기 계산법 설정 */
* {
  box-sizing: border-box;
  font-family: 'Quicksand', sans-serif;
  /* 'Quicksand' 폰트가 시스템에 설치되어 있어야 합니다. */
}

/* 가장 바깥쪽 흰색 카드 컨테이너 (수정됨) */
.post-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  gap: 20px;

  /* --- 수정된 부분 --- */
  width: 100%;
  /* 너비를 100%로 채움 */
  max-width: 500px;
  /* 최대 너비는 375px로 제한 */
  margin: 20px auto;
  /* 페이지 중앙에 보이도록 자동 마진 추가 */
  height: 800px;
  background: #FFFFFF;
  box-shadow: 0px 0px 40px rgba(0, 0, 0, 0.15);
  border-radius: 32px;
}

/* 필터 버튼들을 감싸는 컨테이너 */
.filter-buttons {
  display: flex;
  flex-direction: row;
  gap: 12px;
  width: 100%;
}

/* 기본 필터 버튼 스타일 (수정됨) */
.filter-button {
  /* --- 추가된 부분 --- */
  flex: 1;
  /* 사용 가능한 공간을 1:1:1 비율로 나눠가짐 */

  display: flex;
  justify-content: center;
  align-items: center;

  height: 30px;
  padding: 0 16px;
  border-radius: 15px;
  border: 1px solid #40B59F;
  background: #FFFFFF;

  font-weight: 700;
  font-size: 15px;
  color: #40B59F;
  cursor: pointer;
  white-space: nowrap;
}

/* 활성화된 필터 버튼 스타일 */
.filter-button.active {
  background: #40B59F;
  color: #FFFFFF;
  border: none;
}

/* 정보가 담긴 콘텐츠 카드 */
.content-card {
  display: flex;
  flex-direction: row;
  align-items: center;

  width: 100%;
  padding: 12px;
  gap: 16px;

  background: #FFFFFF;
  border: 1px solid #40B59F;
  box-shadow: 0px 4px 15px rgba(0, 0, 0, 0.1);
  border-radius: 15px;
}

/* 프로필 이미지 (자리만 잡아둠) */
.profile-image {
  width: 70px;
  height: 70px;
  border-radius: 15px;
  background-color: #f0f0f0;
  flex-shrink: 0;
}

/* 사용자 정보 텍스트 영역 */
.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 유저 상세 정보 (이름/성별/나이) */
.user-details {
  margin: 0;
  font-weight: 700;
  font-size: 16px;
  color: #3E4958;
}

/* 포스팅 시간 */
.post-time {
  margin: 0;
  font-size: 13px;
  color: #97ADB6;
}
</style>
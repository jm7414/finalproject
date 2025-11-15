<template>
  <div class="community-view-container">
    <div class="main-nav">
      <div class="main-div-8">
        <div class="main-button" 
             :class="{ active: activeTab === 'Post' }" 
             @click="changeTab('Post')">
          <span class="post-button">공지</span>
        </div>

        <div class="missing-button" 
             :class="{ active: activeTab === 'Missing' }" 
             @click="goToMissing">
          <span class="event-button">실종</span>
        </div>

        <div class="main-div-9" 
             :class="{ active: activeTab === 'Event' }" 
             @click="goToEvent">
          <span class="main-div-a">이벤트</span>
        </div>
      </div>
    </div>

    <div class="content-area">
      <!-- 공지 탭: 목록 또는 작성 -->
      <div v-if="activeTab === 'Post'">
        <NH_NoticeBoard 
          v-if="noticeView === 'list'"
          :isPlazaMaster="isPlazaMaster"
          @write-notice="goToNoticeWrite"
        />
        <NH_NoticeWrite 
          v-if="noticeView === 'write'"
          @notice-created="handleNoticeCreated"
          @cancel="goToNoticeList"
        />
      </div>

      <CommunityMissing v-if="activeTab === 'Missing'" />
      <CommunityEvent v-if="activeTab === 'Event'" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import NH_NoticeBoard from '@/components/NH_NoticeBoard.vue';
import NH_NoticeWrite from '@/components/NH_NoticeWrite.vue';
import CommunityMissing from '@/components/CommunityMissing.vue';
import CommunityEvent from '@/components/CommunityEvent.vue';
import axios from 'axios';

const router = useRouter();

// Props (제거 - plazaId, userId는 내부에서 조회)
// 기본으로 보여줄 탭(채널)을 'Post'로 설정
const activeTab = ref('Post');
const noticeView = ref('list'); // 공지 탭 내부 상태: 'list' 또는 'write'
const isPlazaMaster = ref(false); // 공지 기능으로 인한 추가

// 마운트 시 방장 정보 조회
onMounted(async () => {
  await fetchMyPlazaRole();
});

// 공지 기능으로 인한 추가: 내 역할 조회
async function fetchMyPlazaRole() {
  try {
    const response = await axios.get('/NH/api/neighbor/plazas/my/role');
    isPlazaMaster.value = response.data.isOwner;
  } catch (error) {
    console.error('역할 조회 실패:', error);
  }
}

// 탭(채널)을 변경하는 함수
function changeTab(tabName) {
  activeTab.value = tabName;
  noticeView.value = 'list'; // 탭 변경 시 공지 리스트로 초기화
}

// 공지 작성 페이지로 이동
function goToNoticeWrite() {
  noticeView.value = 'write';
}

// 공지 리스트로 돌아가기
function goToNoticeList() {
  noticeView.value = 'list';
}

// 공지 작성 완료
function handleNoticeCreated() {
  goToNoticeList();
}

// 실종자 페이지로 이동
function goToMissing() {
  router.push({
    name: 'CommunityMissing'
  });
}

// 이벤트 페이지로 이동
function goToEvent() {
  router.push({
    name: 'CommunityEvent'
  });
}
</script>


<style scoped>
/* 전체 레이아웃 */
.community-view-container {
  min-height: 120vh;
  padding: 10px;
  margin-top: -30px;
  background: #fff;
  display: flex;
  flex-direction: column;
}
.main-nav {
  width: 100%;
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;
  background: #ffffff;
  border-top: 1px solid #e5e5e5;
  border-bottom: 1px solid #e5e5e5;
}
.main-div-8 {
  display: flex;
  min-height: 46px;
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
  color: #737373;
}

.main-button.active,
.missing-button.active,
.main-div-9.active {
  border-bottom-color: #a7cc10;
}
.main-button.active .post-button,
.missing-button.active .event-button,
.main-div-9.active .main-div-a {
  color: #171717;
  font-weight: 700;
}

.content-area {
  width: 100%;
  flex-grow: 1;
  overflow-y: auto;
}
</style>

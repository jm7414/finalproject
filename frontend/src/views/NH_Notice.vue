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
             @click="changeTab('Missing')">
          <span class="event-button">실종</span>
        </div>

        <div class="main-div-9" 
             :class="{ active: activeTab === 'Event' }" 
             @click="changeTab('Event')">
          <span class="main-div-a">이벤트</span>
        </div>
      </div>
    </div>

    <div class="content-area">
      <!-- 공지 탭: 목록, 작성, 수정 -->
      <div v-if="activeTab === 'Post'">
        <!-- 공지 목록 -->
        <NH_NoticeBoard 
          v-if="noticeView === 'list'"
          :isPlazaMaster="isPlazaMaster"
          @write-notice="goToNoticeWrite"
          @edit-notice="goToNoticeEdit"
        />
        
        <!-- 공지 작성 -->
        <NH_NoticeWrite 
          v-if="noticeView === 'write'"
          @notice-created="handleNoticeCreated"
          @cancel="goToNoticeList"
        />
        
        <!-- 공지 수정 -->
        <NH_NoticeWrite 
          v-if="noticeView === 'edit'"
          :notice="editingNotice"
          :isEdit="true"
          @notice-updated="handleNoticeUpdated"
          @cancel="goToNoticeList"
        />
      </div>

      <!-- 실종 탭 -->
      <CommunityMissing v-if="activeTab === 'Missing'" />
      
      <!-- 이벤트 탭 -->
      <NH_CommunityEvent v-if="activeTab === 'Event'" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import NH_NoticeBoard from '@/components/NH_NoticeBoard.vue';
import NH_NoticeWrite from '@/components/NH_NoticeWrite.vue';
import CommunityMissing from '@/components/CommunityMissing.vue';
import NH_CommunityEvent from '@/components/NH_CommunityEvent.vue';
import axios from 'axios';

const activeTab = ref('Post');
const noticeView = ref('list'); // 'list', 'write', 'edit'
const isPlazaMaster = ref(false);
const editingNotice = ref(null); // 수정할 공지 데이터

onMounted(async () => {
  await fetchMyPlazaRole();
});

async function fetchMyPlazaRole() {
  try {
    const response = await axios.get('/NH/api/neighbor/plazas/my/role');
    isPlazaMaster.value = response.data.isOwner;
  } catch (error) {
    console.error('역할 조회 실패:', error);
  }
}

function changeTab(tabName) {
  activeTab.value = tabName;
  if (tabName === 'Post') {
    noticeView.value = 'list';
    editingNotice.value = null;
  }
}

function goToNoticeWrite() {
  noticeView.value = 'write';
  editingNotice.value = null;
}

function goToNoticeEdit(notice) {
  console.log('수정할 공지:', notice);
  editingNotice.value = notice;
  noticeView.value = 'edit';
}

function goToNoticeList() {
  noticeView.value = 'list';
  editingNotice.value = null;
}

function handleNoticeCreated() {
  goToNoticeList();
}

function handleNoticeUpdated() {
  goToNoticeList();
}
</script>

<style scoped>
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

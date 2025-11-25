<template>
  <div class="community-view-container">
    <div class="main-nav">
      <div class="main-div-8">
        <div class="main-button" 
             :class="{ active: activeTab === 'Post' }" 
             :style="activeTab === 'Post' ? { borderBottomColor: themeColors.primary } : {}"
             @click="changeTab('Post')">
          <span class="post-button"
                :style="{ 
                  color: activeTab === 'Post' ? themeColors.activeText : themeColors.inactiveText,
                  fontWeight: activeTab === 'Post' ? '700' : '500'
                }">
            공지
          </span>
        </div>

        <div class="missing-button" 
             :class="{ active: activeTab === 'Missing' }" 
             :style="activeTab === 'Missing' ? { borderBottomColor: themeColors.primary } : {}"
             @click="changeTab('Missing')">
          <span class="event-button"
                :style="{ 
                  color: activeTab === 'Missing' ? themeColors.activeText : themeColors.inactiveText,
                  fontWeight: activeTab === 'Missing' ? '700' : '500'
                }">
            실종
          </span>
        </div>

        <div class="main-div-9" 
             :class="{ active: activeTab === 'Event' }" 
             :style="activeTab === 'Event' ? { borderBottomColor: themeColors.primary } : {}"
             @click="changeTab('Event')">
          <span class="main-div-a"
                :style="{ 
                  color: activeTab === 'Event' ? themeColors.activeText : themeColors.inactiveText,
                  fontWeight: activeTab === 'Event' ? '700' : '500'
                }">
            이벤트
          </span>
        </div>
      </div>
    </div>

    <div class="content-area">
      <div v-if="activeTab === 'Post'">
        <NH_NoticeBoard 
          v-if="noticeView === 'list'"
          :isPlazaMaster="isPlazaMaster"
          :theme="theme"
          @write-notice="goToNoticeWrite"
          @edit-notice="goToNoticeEdit"
        />
        
        <!-- key 추가로 강제 재렌더링 -->
        <NH_NoticeWrite 
          v-if="noticeView === 'write'"
          :key="'write-' + noticeViewKey"
          :theme="theme"
          @notice-created="handleNoticeCreated"
          @cancel="goToNoticeList"
        />
        
        <NH_NoticeWrite 
          v-if="noticeView === 'edit'"
          :key="'edit-' + noticeViewKey"
          :notice="editingNotice"
          :isEdit="true"
          :theme="theme"
          @notice-updated="handleNoticeUpdated"
          @cancel="goToNoticeList"
        />
      </div>

      <CommunityMissing v-if="activeTab === 'Missing'" :theme="theme" />
      <NH_CommunityEvent v-if="activeTab === 'Event'" :theme="theme" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import NH_NoticeBoard from '@/components/NH_NoticeBoard.vue';
import NH_NoticeWrite from '@/components/NH_NoticeWrite.vue';
import CommunityMissing from '@/components/CommunityMissing.vue';
import NH_CommunityEvent from '@/components/NH_CommunityEvent.vue';
import axios from 'axios';

const props = defineProps({
  theme: {
    type: String,
    default: 'neighbor',
    validator: (value) => ['guardian', 'neighbor'].includes(value)
  }
});

const themeColors = computed(() => {
  return props.theme === 'neighbor' 
    ? {
        primary: '#a7cc10',
        activeText: '#171717',
        inactiveText: '#737373'
      }
    : {
        primary: '#4A62DD',
        activeText: '#171717',
        inactiveText: '#737373'
      };
});

const activeTab = ref('Post');
const noticeView = ref('list');
const isPlazaMaster = ref(false);
const editingNotice = ref(null);
const noticeViewKey = ref(0); // key 카운터 추가

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
  noticeViewKey.value++; // key 변경으로 강제 재렌더링
}

function goToNoticeEdit(notice) {
  console.log('수정할 공지:', notice);
  editingNotice.value = notice;
  noticeView.value = 'edit';
  noticeViewKey.value++; // key 변경으로 강제 재렌더링
}

function goToNoticeList() {
  noticeView.value = 'list';
  editingNotice.value = null;
  noticeViewKey.value++; // key 변경
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
  text-align: center;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.content-area {
  width: 100%;
  flex-grow: 1;
  overflow-y: auto;
}
</style>

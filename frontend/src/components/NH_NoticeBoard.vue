<template>
  <div v-if="loading" class="loading-state">
    공지를 불러오는 중입니다...
  </div>

  <div v-else-if="error" class="error-state">
    {{ error }}
  </div>

  <div v-else class="board-container">
    <div v-if="notices.length === 0" class="empty-state">
      <p>공지사항이 없습니다.</p>
    </div>

    <div v-else class="post-list">
      <div v-for="notice in notices" :key="notice.post_id" class="post-card">
        
        <div class="card-header">
          <div class="author-info">
            <div class="author-details">
              <span class="author-name">{{ getAuthorName(notice.user_id) }}</span>
              <span class="post-time">{{ formatTimeAgo(notice.created_at) }}</span>
            </div>
          </div>
          <!-- 방장만 삭제 버튼 표시 -->
          <button 
            v-if="isPlazaMaster && notice.user_id === userId"
            class="options-button"
            @click.stop="deleteNotice(notice.post_id)">
            ⋮
          </button>
        </div>

        <div class="card-body">
          <h3 class="post-title">{{ notice.title }}</h3>
          <p class="post-content">{{ notice.content }}</p>
        </div>

        <img v-if="notice.image_path" :src="notice.image_path" alt="공지 이미지" class="post-image">

      </div>
    </div>

    <!-- ✨ [추가] 방장만 공지 작성 FAB 버튼 표시 -->
    <button v-if="isPlazaMaster" class="fab" @click="emit('write-notice')">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const props = defineProps({
  plazaId: {
    type: Number,
    required: true
  },
  userId: {
    type: Number,
    required: true
  },
  isPlazaMaster: {
    type: Boolean,
    required: true
  }
});

const emit = defineEmits(['write-notice']);

const notices = ref([]);
const loading = ref(true);
const error = ref(null);

onMounted(() => {
  fetchNotices();
});

// 공지 목록 조회
async function fetchNotices() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`/api/posts?noticeCheck=공지&plazaId=${props.plazaId}`, {
      withCredentials: true
    });
    notices.value = response.data;
  } catch (err) {
    console.error("공지 목록을 불러오는 데 실패했습니다:", err);
    error.value = "데이터를 불러올 수 없습니다.";
  } finally {
    loading.value = false;
  }
}

// 공지 삭제
async function deleteNotice(postId) {
  if (!confirm('이 공지를 삭제하시겠습니까?')) return;

  try {
    await axios.delete(`/api/posts/${postId}`, {
      withCredentials: true
    });
    await fetchNotices();
  } catch (error) {
    console.error('공지 삭제 실패:', error);
    alert('공지 삭제에 실패했습니다.');
  }
}

// 시간 계산 함수
function formatTimeAgo(dateString) {
  const now = new Date();
  const postDate = new Date(dateString);
  const seconds = Math.floor((now - postDate) / 1000);
  let interval = seconds / 31536000;
  if (interval > 1) return Math.floor(interval) + "년 전";
  interval = seconds / 2592000;
  if (interval > 1) return Math.floor(interval) + "달 전";
  interval = seconds / 86400;
  if (interval > 1) return Math.floor(interval) + "일 전";
  interval = seconds / 3600;
  if (interval > 1) return Math.floor(interval) + "시간 전";
  interval = seconds / 60;
  if (interval > 1) return Math.floor(interval) + "분 전";
  return "방금 전";
}

// 작성자 이름 조회
function getAuthorName(userId) {
  return `사용자 #${userId}`;
}
</script>

<style scoped>
/* 로딩 및 에러 상태 */
.loading-state, .error-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}
.error-state {
  color: red;
}

/* 게시글 목록 */
.post-list {
  display: flex;
  flex-direction: column;
  margin-top: 10px;
  gap: 12px;
  padding: 16px;
}

/* 게시글 카드 */
.post-card {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.05);
  padding: 16px;
  cursor: pointer;
  transition: transform 0.2s ease;
}
.post-card:hover {
  transform: translateY(-2px);
}

/* 카드 헤더 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.author-details {
  display: flex;
  flex-direction: column;
}
.author-name {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}
.post-time {
  font-size: 12px;
  color: #888;
}
.options-button {
  background: none;
  border: none;
  font-size: 20px;
  font-weight: bold;
  color: #aaa;
  cursor: pointer;
}

/* 카드 본문 */
.card-body {
  margin-bottom: 12px;
}
.post-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.post-content {
  font-size: 14px;
  color: #555;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  min-height: calc(1.5em * 1);
}

/* 게시글 이미지 */
.post-image {
  width: 100%;
  height: auto;
  max-height: 250px;
  object-fit: cover;
  border-radius: 8px;
  margin-top: 12px;
  display: block;
}

/* ✨ [추가] 글쓰기 버튼 (FAB) */
.fab {
  position: fixed;
  bottom: 100px;
  right: 20px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: #8e97fd;
  box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  justify-content: center;
  align-items: center;
  border: none;
  cursor: pointer;
  z-index: 1000;
  transition: all 0.2s ease;
}
.fab:hover {
  background-color: #7a82e0;
  transform: translateY(-2px);
  box-shadow: 0px 6px 16px rgba(0, 0, 0, 0.2);
}
.fab:active {
  transform: translateY(0);
}

.fab svg {
  color: white;
}
</style>

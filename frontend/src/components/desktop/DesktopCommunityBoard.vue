<template>
    <div class="board-container-web">
    
    <header class="board-header">
      <h1>게시글</h1>
      <button class="create-post-btn" @click="goToPostWrite">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M13 21h8"/><path d="M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z"/></svg>
        <span>새 글 작성</span>
      </button>
    </header>

    <div v-if="loading" class="state-container loading-state">
      <p>게시물 목록을 불러오는 중입니다...</p>
    </div>

    <div v-else-if="error" class="state-container error-state">
      <p>게시물을 불러오는 중 오류가 발생했습니다.</p>
      <span>{{ error }}</span>
    </div>

    <div v-else-if="posts.length === 0" class="state-container empty-state">
      <p>아직 게시글이 없습니다. 첫 글을 작성해보세요!</p>
    </div>

        <div v-else class="post-list-table">
      
      <div class="post-list-header">
        <div class="post-cell cell-title">제목</div>
        <div class="post-cell cell-thumbnail-header">사진</div>
        <div class="post-cell cell-author">작성자</div>
        <div class="post-cell cell-date">작성일</div>
        <div class="post-cell cell-stats">조회수</div>
        <div class="post-cell cell-stats">좋아요</div>
      </div>

      <div v-for="post in posts" :key="post.postId" class="post-list-row" @click="goToPost(post.postId)">
        <div class="post-cell cell-title">
          <span class="title-text">{{ post.title }}</span>
          <span v-if="post.comments > 0" class="comment-count">[{{ post.comments }}]</span>
        </div>

        <div class="post-cell cell-thumbnail">
          <img v-if="post.image" :src="post.image" alt="첨부 이미지" class="post-thumbnail-img">
        </div>

        <div class="post-cell cell-author">
          <img :src="post.authorProfileImage || defaultProfileImage" alt="프로필" class="author-img-sm">
          <span class="author-name-sm">{{ post.author }}</span>
        </div>
        
        <div class="post-cell cell-date">
          {{ formatTimeAgo(post.createdAt) }}
        </div>
        
        <div class="post-cell cell-stats">
          {{ post.views }}
        </div>
        
        <div class="post-cell cell-stats">
          {{ post.likes }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

// assets 폴더의 기본 프로필 이미지를 불러옵니다.
import defaultProfileImage from '@/assets/default-profile.png';

const posts = ref([]);
const loading = ref(true);
const error = ref(null);
const router = useRouter();

onMounted(() => {
  fetchPosts();
});

async function fetchPosts() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`/api/posts`, {
      withCredentials: true 
    });
    posts.value = response.data;
  } catch (err) {
    console.error("게시물 목록을 불러오는 데 실패했습니다:", err);
    error.value = "데이터를 불러올 수 없습니다.";
  } finally {
    loading.value = false;
  }
}

// 시간 계산 함수 (이전과 동일)
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

// 게시글 상세 페이지로 이동
function goToPost(postId) {
  router.push(`/desktop/communityPost/${postId}`);
}

// 글쓰기 페이지로 이동
function goToPostWrite() {
  router.push(`/desktop/communityPostWrite`);
}
</script>

<style scoped>
.board-container-web {
  width: 100%;
  height: 100%; /* 부모(content-area)의 높이를 100% 사용 */
  padding: 24px 32px;
  background-color: #f9fafb; /* DesktopCommunityView의 흰색 배경과 구분 */
  overflow-y: auto; /* 이 컴포넌트 자체가 스크롤되도록 */
  display: flex;
  flex-direction: column;
}

.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.board-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.create-post-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 10px;
  border: 0;
  background: #6366f1;
  color: #ffffff;
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
}

.create-post-btn:hover {
  transform: translateY(-2px);
  background: #4f46e5;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.3);
}

.state-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  padding: 80px 20px;
}

.state-container p {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}
.error-state span {
  font-size: 14px;
  color: #ef4444;
}

.post-list-table {
  flex: 1; /* 남은 공간을 채움 (게시글이 많을 때 스크롤되도록) */
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  overflow: hidden; /* 테두리 밖으로 나가지 않게 */
  display: flex;
  flex-direction: column;
}

.post-list-header {
  display: grid;
  /* 컬럼 비율: 제목(4fr), 사진(0.5fr), 작성자(1.5fr), 날짜(1.5fr), 조회(1fr), 좋아요(1fr) */
  grid-template-columns: 3.2fr 1.3fr 1.1fr 0.7fr 0.5fr 0.4fr;
  gap: 16px;
  width: 100%;
  padding: 16px 24px;
  border-bottom: 2px solid #e5e7eb;
  background: #f9fafb;
  flex-shrink: 0;
}

.cell-thumbnail-header {
  color: #6b7280;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
}

.post-list-row {
  display: grid;
  /* 헤더와 동일한 컬럼 비율 */
  grid-template-columns: 4fr 1fr 1.5fr 1fr 0.5fr 0.5fr;
  gap: 16px;
  width: 100%;
  padding: 18px 24px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.post-list-row:hover {
  background-color: #f9fafb;
}

.post-list-row:last-child {
  border-bottom: none;
}

/* 테이블의 각 셀 공통 스타일 */
.post-cell {
  display: flex;
  align-items: center;
  min-width: 0; /* flex/grid 아이템이 줄어들 수 있도록 */
  font-size: 15px;
  color: #374151;
}

/* 헤더 셀 스타일 */
.post-list-header .post-cell {
  color: #6b7280;
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
}

/* 셀 유형별 개별 스타일 */

/* 제목 셀 */
.cell-title {
  font-weight: 600;
  color: #111827;
  gap: 8px;
}
.title-text {
  /* 긴 제목 ... 처리 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.comment-count {
  font-size: 14px;
  font-weight: 700;
  color: #6366f1; /* 메인 색상 */
  flex-shrink: 0;
}

/* 썸네일 이미지 셀 */
.cell-thumbnail {
  display: flex;
  justify-content: center;
  align-items: center;
}

.post-thumbnail-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

/* 작성자 셀 */
.cell-author {
  gap: 15px;
}
.author-img-sm {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.author-name-sm {
  font-size: 14px;
  color: #4b5563;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 날짜 셀 */
.cell-date {
  font-size: 14px;
  color: #6b7280;
}

/* 통계 셀 (조회, 좋아요) */
.cell-stats {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  justify-content: center;
}

.post-list-header .cell-stats {
  justify-content: center;
}

</style>
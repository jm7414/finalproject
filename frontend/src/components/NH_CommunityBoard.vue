<template>
  <div v-if="loading" class="loading-state">
    게시물 목록을 불러오는 중입니다...
  </div>

  <div v-else-if="error" class="error-state">
    {{ error }}
  </div>

  <div v-else class="board-container">


    <div v-if="posts.length === 0" class="empty-state">
      <p>아직 게시글이 없습니다. 첫 글을 작성해보세요!</p>
    </div>

<div v-else class="post-list">
  <div v-for="post in posts" :key="post.postId" class="post-card" @click="goToPost(post.postId)">
    
    <div class="card-header">
      <div class="author-info">
        <img :src="post.authorProfileImage || defaultProfileImage" alt="프로필" class="profile-img">
        <div class="author-details">
          <span class="author-name">{{ post.author }}</span>
          <span class="post-time">{{ formatTimeAgo(post.createdAt) }}</span>
        </div>
      </div>
      <button class="options-button">⋮</button>
    </div>

    <div class="card-body">
      <h3 class="post-title">{{ post.title }}</h3>
      <p class="post-content">{{ post.content }}</p>
    </div>

    <img v-if="post.image" :src="post.image" alt="게시물 이미지" class="post-image">

    <div class="card-footer">
      <div class="post-stats">
        <span>❤️ {{ post.likes }}</span>
        <span>💬 {{ post.comments }}</span>
        <span>👁️ {{ post.views }}</span>
      </div>
    </div>
    
  </div>
  </div>

    <button class="create-post-fab" @click="goToPostWrite">
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M13 21h8"/>
        <path d="M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z"/>
      </svg>
    </button>
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
  router.push(`/post/${postId}`);
}

// 글쓰기 페이지로 이동
function goToPostWrite() {
  router.push(`/CommunityPostWrite`);
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

/* 정렬/필터 버튼 */
.filter-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  background-color: #f9f9f9;
  position: sticky; /* 스크롤 시 상단에 고정 */
  top: 0; /* 헤더 바로 아래 */
  z-index: 5;
}
.sort-buttons {
  display: flex;
  gap: 8px;
}
.sort-button {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #e0e0e0;
  background-color: #fff;
  color: #555;
  font-size: 14px;
  cursor: pointer;
}
.sort-button.active {
  background-color: #8E97FD; /* 활성 버튼 색상 */
  color: #fff;
  border-color: #8E97FD;
}
.filter-button {
  background: none;
  border: none;
  padding: 8px;
  color: #555;
  cursor: pointer;
}

/* 게시글 목록 */
.post-list {
  display: flex;
  flex-direction: column;
  margin-top: 10px;
  gap: 12px; /* 카드 사이 간격 */
}
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
  /* 내용이 짧아도 최소 높이를 확보하려면 아래 주석 해제 */
  /* min-height: calc(1.5em * 1); */ 
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
.profile-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #eee;
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
  /* 제목이 길 경우 ... 처리 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.post-content {
  font-size: 14px;
  color: #555;
  line-height: 1.5;
  /* ✨ 친구 요청: 4줄까지만 보이도록 설정 ✨ */
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 4; 
  -webkit-box-orient: vertical;
  min-height: calc(1.5em * 1); /* 최소 1줄 높이 확보 */
}

/* 게시글 이미지 */
.post-image {
  width: 100%;
  height: auto; /* 비율 유지 */
  max-height: 250px; /* 최대 높이 제한 (선택사항) */
  object-fit: cover;
  border-radius: 8px;
  margin-top: 12px;
}

/* 카드 푸터 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.post-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #777;
}
.post-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 글쓰기 버튼 (FAB) */
.create-post-fab {
  position: fixed;
  bottom: 80px; /* 하단 탭 위에 위치 (탭 높이에 따라 조정) */
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
}
.create-post-fab:hover {
  background-color: #7a82e0;
}

</style>
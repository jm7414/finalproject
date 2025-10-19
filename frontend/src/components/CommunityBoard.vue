<script setup>
import { ref, onMounted } from 'vue'; // onMounted 추가
import { useRouter } from 'vue-router';
import axios from 'axios'; // axios 추가

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
    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    const response = await axios.get(`${API_BASE_URL}/api/posts`, {
      withCredentials: true // 여기에 입장권(쿠키)을 챙겨달라는 옵션 추가. 없으면 프론트 - 백 연결 불가. 시큐리티와 인덱스에서 롤 설정으로 막혀있는듯
    });
    posts.value = response.data;
  } catch (err) {
    console.error("게시물 목록을 불러오는 데 실패했습니다:", err);
    error.value = "데이터를 불러올 수 없습니다.";
  } finally {
    loading.value = false;
  }
}

// 시간을 계산하는 함수
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


function goToPost(postId) {
  router.push(`/post/${postId}`);
}

function goToPostWrite() {
  router.push(`/CommunityPostWrite`);
}
</script>

<template>
  <div class="post-list-container">
    <div class="sort-selector-wrapper">
      <select class="sort-selector">
        <option>최신순</option>
        <option>인기순</option>
      </select>
    </div>

    <!-- 로딩 중일 때 보이는 메시지임 -->
    <div v-if="loading">게시물 목록을 불러오는 중입니다...</div>
    
    <div v-else-if="error">{{ error }}</div>

    <div 
      v-else
      v-for="post in posts" 
      :key="post.postId" class="post-card"
      @click="goToPost(post.postId)" >
      
      <div class="card-header">
        <h3 class="post-title">{{ post.title }}</h3>
        <span class="post-author">작성자 : {{ post.author }}</span>
      </div>

      <!-- 이미지는 DB에 경로가 있을 때만 표시 -->
      <img v-if="post.image" :src="post.image" alt="게시물 이미지" class="post-image">
      
      <div class="card-footer">
        <div class="post-stats">
          
          <span>💬 {{ post.comments }}</span>
          <span>❤️ {{ post.likes }}</span>
          <span>👁️ {{ post.views }}</span>
        </div>
        
        <span class="post-time">{{ formatTimeAgo(post.createdAt) }}</span>
      </div>
    </div>
    
    <button class="fab-button" @click="goToPostWrite()">+ 글 작성</button>
  </div>
</template>

<style scoped>
.post-list-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}

.sort-selector-wrapper {
  display: flex;
  justify-content: flex-end;
}
.sort-selector {
  border: none;
  background: transparent;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
}

.post-card {
  display: flex;
  flex-direction: column; /* 카드 내부 요소를 세로로 배치 */
  width: 100%;
  background: #FFFFFF;
  border: 1px solid #808AFF;
  box-shadow: 0px 4px S20px rgba(0, 0, 0, 0.1);
  border-radius: 15px;
  cursor: pointer;
  overflow: hidden; /* 이미지가 둥근 모서리를 넘어가지 않도록 */
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
}

.post-title {
  margin: 0;
  font-weight: 700;
  font-size: 18px;
  color: #3F414E;
}

.post-author {
  font-size: 14px;
  color: #555;
}

.post-image {
  display: block;
  width: 100%;
  height: auto;
  padding-top: 10px;
  padding-bottom: 10px;
  padding-left: 30px;
  padding-right: 30px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background-color: #fafbff; /* 푸터 영역 배경색 살짝 추가 */
}

.post-stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #555;
}
.post-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.post-time {
  font-size: 14px;
  color: #a0a0a0;
}

/* 글 작성 버튼 (Floating Action Button) */
.fab-button {
  position: fixed;
  bottom: 100px;
  right: 24px;
  padding: 12px 24px;
  border-radius: 30px;
  background-color: #8E97FD;
  color: white;
  font-size: 18px;
  font-weight: 700;
  border: none;
  box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  z-index: 900;
  transition: transform 0.2s ease;
}
.fab-button:hover {
  transform: scale(1.05);
}
</style>
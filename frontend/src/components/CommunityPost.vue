<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router'; // useRouter 추가
import axios from 'axios';

const route = useRoute();
const router = useRouter(); // 라우터 인스턴스
const postId = ref(route.params.id || 1);
const post = ref(null);
const loading = ref(true);
const error = ref(null);

// '더보기' 메뉴의 보임/숨김 상태를 관리할 ref
const isOptionsMenuVisible = ref(false);

onMounted(() => {
  fetchPost();
});

async function fetchPost() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`http://localhost:8080/api/posts/${postId.value}`);
    post.value = response.data;
  } catch (err) {
    console.error('게시물 데이터를 불러오는 데 실패했습니다:', err);
    error.value = '데이터를 불러올 수 없습니다.';
  } finally {
    loading.value = false;
  }
}

// 더보기 메뉴를 토글하는 함수
function toggleOptionsMenu() {
  isOptionsMenuVisible.value = !isOptionsMenuVisible.value;
}

function editPost() {
  router.push(`/post/edit/${postId.value}`);
}

async function deletePost() {
  if (confirm('정말로 이 게시물을 삭제하시겠습니까?')) {
    try {
      await axios.delete(`http://localhost:8080/api/posts/${postId.value}`);
      alert('게시물이 삭제되었습니다.');
      router.push('/community'); 
    } catch (err) {
      console.error('게시물 삭제 중 오류 발생:', err);
      alert('게시물 삭제에 실패했습니다.');
    }
  }
}

function sharePost() {
  alert('이 게시물을 공유합니다!');
  isOptionsMenuVisible.value = false;
}

// 좋아요, 댓글 관련 함수 (기능 구현 시 사용)
function toggleLike() { alert('좋아요 API 연동 필요'); }
function addComment() { alert('댓글 작성 API 연동 필요'); }
function likeComment() { alert('댓글 좋아요!'); }
</script>

<template>
  <div v-if="loading" class="loading-container">
    <p>데이터를 불러오는 중입니다...</p>
  </div>

  <div v-else-if="error" class="error-container">
    <p>{{ error }}</p>
  </div>

  <div v-else-if="post" class="post-detail-container">
    <div class="post-header-top">
      <div class="author-info">
        <img :src="post.authorProfileImage" alt="작성자 프로필" class="author-profile-img">
        <div>
          <span class="author-name">{{ post.author }}</span>
          <span class="post-date">{{ new Date(post.date).toLocaleDateString('ko-KR') }}</span>
        </div>
      </div>

      <!-- '더보기' 메뉴 컨테이너 -->
      <div class="options-container">
        <button @click="toggleOptionsMenu" class="options-button">⋮</button>
        <div v-if="isOptionsMenuVisible" class="options-menu">
          <div @click="editPost" class="options-item">수정</div>
          <div @click="deletePost" class="options-item">삭제</div>
          <div @click="sharePost" class="options-item">공유</div>
        </div>
      </div>
    </div>

    <div class="post-content">
      <h1 class="post-title">{{ post.title }}</h1>
      <p class="post-text">{{ post.content }}</p>
      <img v-if="post.image" :src="post.image" alt="게시물 이미지" class="post-main-image">
    </div>

    <div class="post-footer-stats">
      <span class="like-button">
        <span>❤️</span> {{ post.likes }}
      </span>
      <span>👁️ {{ post.views }}</span>
    </div>

    <!-- 댓글 -->
    <div class="comments-section">
    </div>
  </div>
</template>

<style scoped>
.post-detail-container {
  padding: 24px;
  width: 100%;
  max-width: 500px; /* 전체 너비 제한 */
  margin: 0 auto;
  background-color: #FFFFFF;
}

.loading-container, .error-container {
  text-align: center;
  padding: 50px;
  font-size: 1.2em;
  color: #555;
}

/* 게시물 헤더 */
.post-header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-profile-img {
  width: 56px;
  height: 56px;
  border-radius: 50%; /* 원형 프로필 이미지 */
  object-fit: cover;
  flex-shrink: 0;
}

.author-name {
  display: block;
  font-weight: 700;
  font-size: 18px;
  color: #3F414E;
}

.post-date {
  display: block;
  font-size: 14px;
  color: #555;
}

/* '더보기' 메뉴 관련 스타일 */
.options-container {
  position: relative; /* 드롭다운 메뉴의 위치 기준점 */
}

.options-button {
  background: none;
  border: none;
  font-size: 28px;
  font-weight: bold;
  color: #888;
  cursor: pointer;
  padding: 0 8px;
}

.options-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: white;
  border: 1px solid #eee;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 10;
  width: 120px;
  overflow: hidden;
}

.options-item {
  padding: 12px 16px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.options-item:hover {
  background-color: #f5f5f5;
}


/* 게시물 본문 */
.post-content {
  margin-bottom: 20px;
}

.post-title {
  font-weight: 700;
  font-size: 28px;
  margin-top: 0;
  margin-bottom: 12px;
  color: #3F414E;
}

.post-text {
  font-weight: 500;
  font-size: 18px;
  line-height: 1.5;
  color: #3F414E;
  white-space: pre-wrap;
  margin-bottom: 20px;
}

.post-main-image {
  width: 100%;
  height: auto;
  border-radius: 10px;
  object-fit: cover;
  margin-top: 10px;
}

/* 게시물 푸터 (좋아요, 조회수) */
.post-footer-stats {
  display: flex;
  gap: 20px;
  padding: 15px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
  font-size: 16px;
  color: #555;
}

.like-button {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}
</style>


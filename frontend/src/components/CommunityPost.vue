<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

const route = useRoute();
const router = useRouter();
const postId = ref(route.params.id || 1);
const post = ref(null);
const loading = ref(true);
const error = ref(null);

const isOptionsMenuVisible = ref(false);
const currentUser = ref(null);

onMounted(() => {
  fetchCurrentUser();
  fetchPost();
});

// 본인 확인 함수
async function fetchCurrentUser() {
  try {
    const response = await axios.get('http://localhost:8080/api/user/me', {
      withCredentials: true // 로그인 인증
    });
    // 자신의 계정정보를 currentUser에 저장
    currentUser.value = response.data;
  } catch (error) {
    console.error("현재 사용자 정보를 가져오는데 실패했습니다.", error);
    // 로그인 안한 상태라면
    currentUser.value = null;
  }
}

async function fetchPost() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`http://localhost:8080/api/posts/${postId.value}`, {
      withCredentials: true
    });
    post.value = response.data;
  } catch (err) {
    console.error('게시물 데이터를 불러오는 데 실패했습니다:', err);
    error.value = '데이터를 불러올 수 없습니다.';
  } finally {
    loading.value = false;
  }
}

function toggleOptionsMenu() {
  isOptionsMenuVisible.value = !isOptionsMenuVisible.value;
}

function editPost() {
  router.push(`/post/edit/${postId.value}`);
}

async function deletePost() {
  if (confirm('정말로 이 게시물을 삭제하시겠습니까?')) {
    try {
      await axios.delete(`http://localhost:8080/api/posts/${postId.value}`, {
        withCredentials: true
      });
      alert('게시물이 삭제되었습니다.');
      router.push('/CommunityView');
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
          <span class="post-date">{{ new Date(post.createdAt).toLocaleDateString('ko-KR') }}</span>
        </div>
      </div>

      <div class="post-header-top">
        <div class="author-info">
          </div>

        <div class="post-actions-container">

          <button @click="sharePost" class="action-button share-button" title="공유하기">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"></path><polyline points="16 6 12 2 8 6"></polyline><line x1="12" y1="2" x2="12" y2="15"></line></svg>
          </button>

          <div v-if="post && currentUser && (post.userId === currentUser.userNo || currentUser.userNo === 1)" class="more-options-group">
            <button @click="toggleOptionsMenu" class="action-button options-button" title="더보기">⋮</button>
            <div v-if="isOptionsMenuVisible" class="options-menu">
              <div @click="editPost" class="options-item">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                <span>수정</span>
              </div>
              <div @click="deletePost" class="options-item delete">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
                <span>삭제</span>
              </div>
            </div>
          </div>

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

    <div class="comments-section">
    </div>
  </div>
</template>

<style scoped>
.post-detail-container {
  padding: 24px;
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  background-color: #FFFFFF;
}
.loading-container, .error-container {
  text-align: center;
  padding: 50px;
  font-size: 1.2em;
  color: #555;
}
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
  border-radius: 50%;
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
.post-actions-container {
  display: flex;
  align-items: center;
  gap: 8px;
}
.action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background-color: #f0f2f5;
  color: #555;
  cursor: pointer;
  transition: background-color 0.2s;
}
.action-button:hover {
  background-color: #e4e6eb;
}
.action-button svg {
  stroke: #3F414E;
}
.options-button {
  font-size: 24px;
  font-weight: bold;
  padding-bottom: 4px;
  background-color: transparent;
}
.options-button:hover {
  background-color: #f0f2f5;
}
.more-options-group {
  position: relative;
}
.options-menu {
  position: absolute;
  top: 110%;
  right: 0;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.1);
  z-index: 10;
  width: 130px;
  overflow: hidden;
  padding: 6px;
  border: 1px solid #eee;
}
.options-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  font-size: 16px;
  font-weight: 500;
  color: #3F414E;
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 8px;
}
.options-item:hover {
  background-color: #f5f5f5;
}
.options-item svg {
  stroke: #555;
}
.options-item.delete {
  color: #e53e3e;
}
.options-item.delete:hover {
  background-color: #fed7d7;
}
.options-item.delete svg {
  stroke: #e53e3e;
}

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
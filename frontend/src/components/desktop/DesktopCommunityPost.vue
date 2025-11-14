<template>
    <div v-if="loading" class="status-container-web">
    <p>데이터를 불러오는 중입니다...</p>
  </div>

  <div v-else-if="error" class="status-container-web">
    <p>{{ error }}</p>
  </div>

    <div v-else-if="post" class="page-container-web">
        <div class="post-detail-wrapper">

            <section class="post-section">
        <div class="post-header">
          <img :src="post.authorProfileImage || defaultProfileImage" alt="프로필" class="author-profile-img">
          <div class="author-details">
            <div class="author-name">{{ post.author }}</div>
            <div class="post-time">{{ formatRelativeTime(post.createdAt) }}</div>
          </div>
          <div class="post-actions">
            <button @click="sharePost" class="action-btn">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.72"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path></svg>
            </button>
            <div v-if="currentUser && (post.userId === currentUser.userNo || currentUser.userNo === 1)" class="more-options-group">
              <button @click="toggleOptionsMenu" class="action-btn">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"></circle><circle cx="12" cy="5" r="1"></circle><circle cx="12" cy="19" r="1"></circle></svg>
              </button>
              <div v-if="isOptionsMenuVisible" class="options-menu">
                <div @click="editPost">수정</div>
                <div @click="deletePost" class="delete">삭제</div>
              </div>
            </div>
          </div>
        </div>

        <h1 class="post-title">{{ post.title }}</h1>
        <p class="post-content">{{ post.content }}</p>
        
        <div v-if="post.image" class="post-image-container">
          <img :src="post.image" alt="게시물 이미지" class="post-image">
        </div>
        
        <div class="post-footer">
          <div class="stats">
                        <span @click="toggleLike" class="like-btn">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"></path></svg>
              <span>{{ post.likes }}</span>
            </span>
            <span>👁️‍🗨️ {{ post.views }}</span>
          </div>
          <span class="comment-count">댓글 {{ comments.length }}개</span>
        </div>
      </section>

            <section class="comment-input-section">
                <img 
          v-if="currentUser" 
          :src="currentUser.profileImage || defaultProfileImage" 
          alt="내 프로필" 
          class="author-profile-img-sm"
        >
        <input 
          type="text" 
          class="comment-input" 
          :placeholder="currentUser ? '댓글을 입력하세요...' : '로그인 후 댓글을 작성할 수 있습니다.'"
          v-model="newCommentContent"
          @keyup.enter="submitComment"
          :disabled="!currentUser"
        >
        <button @click="submitComment" class="submit-btn" :disabled="!currentUser">게시</button>
      </section>

            <section class="comment-list-section">
        <div v-if="comments.length > 0">
          <div v-for="comment in comments" :key="comment.commentId" class="comment-item">
            <img :src="comment.authorProfileImage || defaultProfileImage" alt="프로필" class="author-profile-img-sm">
            <div class="comment-body">
              <div class="comment-header">
                <span class="author-name-sm">{{ comment.author }}</span>
                <span class="post-time-sm">{{ formatRelativeTime(comment.createdAt) }}</span>
              </div>
              <p class="comment-content">{{ comment.content }}</p>
            </div>
                        <button 
              v-if="currentUser && (comment.userId === currentUser.userNo || currentUser.userNo === 1)"
              @click="deleteComment(comment.commentId)"
              class="comment-delete-btn"
            >
              &times;             </button>
          </div>
        </div>
        <div v-else class="no-comments">
          <p>첫 댓글을 남겨보세요.</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

import defaultProfileImage from '@/assets/default-profile.png';

const route = useRoute();
const router = useRouter();
const postId = ref(route.params.id);
const post = ref(null);
const loading = ref(true);
const error = ref(null);
const isOptionsMenuVisible = ref(false);
const currentUser = ref(null);

// --- 댓글 관련 변수 ---
const comments = ref([]);
const newCommentContent = ref('');

// --- 시간 포맷팅 유틸리티 함수 ---
function formatRelativeTime(dateString) {
  const now = new Date();
  const date = new Date(dateString);
  const diffInSeconds = Math.floor((now - date) / 1000);

  const minutes = Math.floor(diffInSeconds / 60);
  if (minutes < 1) return '방금 전'; // [개선] 0분 전 대신 방금 전
  if (minutes < 60) return `${minutes}분 전`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;

  // [개선] 7일 이내는 'n일 전'으로 표시
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}일 전`;

  // 7일이 넘으면 날짜 표시
  return new Intl.DateTimeFormat('ko-KR', { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric' 
  }).format(date);
}


// --- 컴포넌트가 시작될 때 실행될 함수들 ---
onMounted(() => {
  console.log("컴포넌트 마운트됨. 데이터 로딩 시작..."); // 시작 로그

  // [수정] 중복 실행되던 Promise.all 하나를 제거했습니다. (로직 오류 수정)
  // 3가지 요청을 동시에 보냅니다.
  Promise.all([
    fetchCurrentUser(),
    fetchPost(),
    fetchComments()
  ])
  .then(() => {
    // 모든 요청이 성공적으로 완료된 후에 이 부분이 실행됩니다.
    console.log("✅ 모든 초기 데이터 로딩 완료:");
    console.log("   - 현재 사용자(currentUser):", currentUser.value);
    console.log("   - 게시물(post):", post.value);
    console.log("   - 댓글(comments):", comments.value);
  })
  .catch(error => {
    // Promise.all 내의 요청 중 하나라도 실패하면 이 부분이 실행됩니다.
    console.error("❌ 초기 데이터 로딩 중 오류 발생:", error);
    console.log("   - 현재 사용자(currentUser) 상태:", currentUser.value);
    console.log("   - 게시물(post) 상태:", post.value);
    console.log("   - 댓글(comments) 상태:", comments.value);
  });

  console.log("onMounted 훅 실행 완료.");
});

// --- 데이터 로딩 함수들 ---
async function fetchCurrentUser() {
  try {
    const response = await axios.get(`/api/user/me`, {
      withCredentials: true
    });
    currentUser.value = response.data;
  } catch (error) {
    console.error("현재 사용자 정보를 가져오는데 실패했습니다.", error);
    currentUser.value = null;
  }
}

async function fetchPost() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`/api/posts/${postId.value}`, {
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

async function fetchComments() {
  try {
    const response = await axios.get(`/api/posts/${postId.value}/comments`, {
      withCredentials: true
    });
    comments.value = response.data;
  } catch (error) {
    console.error("댓글 목록을 불러오는 데 실패했습니다:", error);
  }
}

// --- 게시물 관련 액션 함수들 ---
function toggleOptionsMenu() {
  isOptionsMenuVisible.value = !isOptionsMenuVisible.value;
}

function editPost() {
  router.push(`/desktop/post/edit/${postId.value}`);
}

async function deletePost() {
  if (confirm('정말로 이 게시물을 삭제하시겠습니까?')) {
    try {
      await axios.delete(`/api/posts/${postId.value}`, {
        withCredentials: true
      });
      alert('게시물이 삭제되었습니다.');
      router.push('/desktop/communityView');
    } catch (err) {
      console.error('게시물 삭제 중 오류 발생:', err);
      alert('게시물 삭제에 실패했습니다.');
    }
  }
}

function sharePost() {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(window.location.href)
      .then(() => alert('게시물 링크가 복사되었습니다.'))
      .catch(err => alert('링크 복사에 실패했습니다.'));
  } else {
    const textArea = document.createElement("textarea");
    textArea.value = window.location.href;
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
      document.execCommand('copy');
      alert('게시물 링크가 복사되었습니다.');
    } catch (err) {
      alert('링크 복사에 실패했습니다.');
    }
    document.body.removeChild(textArea);
  }
  isOptionsMenuVisible.value = false;
}

async function toggleLike() {
  if (!currentUser.value) {
    alert("좋아요를 누르려면 로그인이 필요합니다.");
    return;
  }
  if (!post.value) return;
  
  try {
    // [개선] 템플릿에서 post.value.postId 대신 postId.value 사용
    const response = await axios.post(
      `/api/posts/${postId.value}/like`, // post.value.postId 대신 postId.value 사용
      null,
      { withCredentials: true }
    );
    
    // [개선] 백엔드에서 객체를 반환한다고 가정 (좋아요 수, 좋아요 여부)
    // { likesCount: 10, isLiked: true }
    if (typeof response.data === 'object') {
      post.value.likes = response.data.likesCount;
      post.value.isLiked = response.data.isLiked; // 좋아요 상태 UI 반영을 위해
    } else {
      // 기존 로직 (숫자만 반환)
      post.value.likes = response.data;
    }
    
  } catch (error) {
    console.error("좋아요 처리 중 오류가 발생했습니다:", error);
    if (error.response && error.response.status === 401) {
      alert("좋아요를 누르려면 로그인이 필요합니다.");
    } else {
      alert("좋아요 처리에 실패했습니다. 다시 시도해주세요.");
    }
  }
}

// --- 댓글 관련 액션 함수들 ---
async function submitComment() {
  if (!currentUser.value) {
    alert("로그인이 필요합니다.");
    return;
  }
  if (!newCommentContent.value.trim()) {
    alert("댓글 내용을 입력해주세요.");
    return;
  }
  try {
    const response = await axios.post(
      `/api/posts/${postId.value}/comments`,
      { content: newCommentContent.value },
      { withCredentials: true }
    );
    comments.value.unshift(response.data);
    newCommentContent.value = '';
    if (post.value) {
      // post.value.comments++ 대신 comments.length를 직접 사용
      // post.value.comments = comments.value.length; (더 정확)
    }
  } catch (error) {
    console.error("댓글 작성 중 오류 발생:", error);
    alert("댓글 작성에 실패했습니다.");
  }
}

async function deleteComment(commentId) {
  if (confirm("정말로 이 댓글을 삭제하시겠습니까?")) {
    try {
      await axios.delete(`/api/comments/${commentId}`, {
        withCredentials: true
      });
      comments.value = comments.value.filter(comment => comment.commentId !== commentId);
      if (post.value) {
        // post.value.comments-- 대신 comments.length 사용
        // post.value.comments = comments.value.length; (더 정확)
      }
      alert("댓글이 삭제되었습니다.");
    } catch (error) {
      console.error("댓글 삭제 중 오류 발생:", error);
      alert("댓글 삭제에 실패했습니다.");
    }
  }
}
</script>

<style scoped>
/* [수정] 전체 페이지 컨테이너: PC 웹 스타일 */
.page-container-web {
  width: 100%;
  min-height: 100%;
  padding: 32px;
  background-color: #f9fafb; /* 목록과 동일한 배경색 */
  box-sizing: border-box;
}

/* [신규] 콘텐츠 중앙 정렬 래퍼 */
.post-detail-wrapper {
  max-width: 900px; /* 콘텐츠 최대 너비 설정 */
  margin: 0 auto; /* 중앙 정렬 */
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px; /* 섹션(카드) 간의 간격 */
}

/* [수정] 로딩/에러 상태: PC용 중앙 정렬 */
.status-container-web {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  font-size: 18px;
  color: #6b7280;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  margin: 32px;
}

/* [신규] 카드 공통 스타일 */
.post-section,
.comment-input-section,
.comment-list-section {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  border: 1px solid #f3f4f6;
}

/* [수정] 게시글 섹션 */
.post-section {
  padding: 32px;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.author-profile-img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 16px;
}

.author-details {
  flex-grow: 1;
}

.author-name {
  font-size: 16px;
  color: #111827;
  font-weight: 600;
}

.post-time {
  font-size: 14px;
  color: #6b7280;
}

.post-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  border: none;
  background: none;
  cursor: pointer;
  color: #6b7280;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s, color 0.2s;
}

.action-btn:hover {
  background-color: #f3f4f6;
  color: #111827;
}

.more-options-group {
  position: relative;
}

.options-menu {
  position: absolute;
  top: 110%;
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  padding: 8px;
  z-index: 10;
  width: 120px;
}

.options-menu div {
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
}
.options-menu div:hover {
  background-color: #f9fafb;
}
.options-menu .delete {
  color: #ef4444;
}
.options-menu .delete:hover {
  background-color: #fef2f2;
}

.post-title {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 24px;
  line-height: 1.4;
}

.post-content {
  font-size: 16px;
  line-height: 1.7;
  color: #374151;
  white-space: pre-wrap; /* 줄바꿈 유지 */
  margin-bottom: 24px;
  word-break: break-word; /* 긴 단어 줄바꿈 */
}

.post-image-container {
  width: 70%;
  border-radius: 12px;
  margin: 24px 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.post-image {
  /*width: 70%;*/
  height: auto;
  display: block;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

.stats {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 15px;
  color: #6b7280;
  font-weight: 500;
}

.stats span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.like-btn {
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 20px;
  transition: background-color 0.2s, color 0.2s;
}
.like-btn:hover {
  background-color: #fef2f2;
  color: #ef4444;
}

.comment-count {
  font-size: 15px;
  color: #374151;
  font-weight: 500;
}

/* [수정] 댓글 입력 섹션 */
.comment-input-section {
  display: flex;
  padding: 24px;
  gap: 16px;
  align-items: center;
}

/* 댓글 입력창 앞의 프로필 이미지 (작은 버전) */
.author-profile-img-sm {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.comment-input {
  flex-grow: 1;
  height: 44px;
  border: 1px solid #d1d5db;
  border-radius: 8px; /* 둥근 사각형으로 변경 */
  padding: 0 16px;
  font-size: 15px;
  background-color: #f9fafb;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.comment-input::placeholder {
  color: #9ca3af;
}
.comment-input:focus {
  outline: none;
  border-color: #6366f1;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}
.comment-input:disabled {
  background-color: #f3f4f6;
  cursor: not-allowed;
}

.submit-btn {
  flex-shrink: 0;
  height: 44px;
  padding: 0 20px;
  background: #6366f1; /* 메인 색상 */
  color: white;
  border: none;
  border-radius: 8px; /* 둥근 사각형으로 변경 */
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}
.submit-btn:hover {
  background: #4f46e5;
}
.submit-btn:disabled {
  background: #e0e7ff;
  cursor: not-allowed;
}

/* [수정] 댓글 목록 섹션 */
.comment-list-section {
  overflow: hidden; /* 내부 border-radius 적용 */
}

.comment-item {
  display: flex;
  padding: 24px;
  border-bottom: 1px solid #f3f4f6;
  gap: 16px;
  position: relative;
}
.comment-item:last-child {
  border-bottom: none;
}

.comment-body {
  flex-grow: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.author-name-sm {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.post-time-sm {
  font-size: 13px;
  color: #6b7280;
}

.comment-content {
  font-size: 15px;
  line-height: 1.6;
  color: #374151;
  word-break: break-word;
}

.comment-delete-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  border: none;
  background: none;
  color: #9ca3af;
  cursor: pointer;
  font-size: 20px; /* 아이콘 크기 */
  line-height: 1;
  padding: 4px;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s, color 0.2s;
}
.comment-delete-btn:hover {
  background-color: #fef2f2;
  color: #ef4444;
}

.no-comments {
  padding: 60px;
  text-align: center;
  color: #6b7280;
  font-size: 15px;
}
</style>
<template>
  <div v-if="loading" class="status-container">
    <p>데이터를 불러오는 중입니다...</p>
  </div>

  <div v-else-if="error" class="status-container">
    <p>{{ error }}</p>
  </div>

  <div v-else-if="post" class="page-container">
    <section class="post-section">
      <div class="post-header">
        <img :src="post.authorProfileImage || defaultProfileImage" alt="프로필" class="author-profile-img" />
        <div class="author-details">
          <div class="author-name">{{ post.author }}</div>
          <div class="post-time">{{ formatRelativeTime(post.createdAt) }}</div>
        </div>
        <div class="post-actions">
          <button @click="sharePost" class="action-btn">🔗</button>
          <div
            v-if="currentUser && (post.userId === currentUser.userNo || currentUser.userNo === 1)"
            class="more-options-group"
          >
            <button @click="toggleOptionsMenu" class="action-btn">⋮</button>
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
        <img :src="post.image" alt="게시물 이미지" class="post-image" />
      </div>
      <div class="post-footer">
        <div class="stats">
          <span @click="toggleLike" class="like-btn">❤️ {{ post.likes }}</span>
          <span>👁️‍🗨️ {{ post.views }}</span>
        </div>
        <span class="comment-count">댓글 {{ comments.length }}개</span>
      </div>
    </section>

    <section class="comment-input-section">
      <input
        type="text"
        class="comment-input"
        placeholder="댓글을 입력하세요..."
        v-model="newCommentContent"
        @keyup.enter="submitComment"
      />
      <button @click="submitComment" class="submit-btn">게시</button>
    </section>

    <section class="comment-list-section">
      <div v-if="comments.length > 0">
        <div v-for="comment in comments" :key="comment.commentId" class="comment-item">
          <img :src="comment.authorProfileImage || defaultProfileImage" alt="프로필" class="author-profile-img-sm" />
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
            ✕
          </button>
        </div>
      </div>
      <div v-else class="no-comments">
        <p>첫 댓글을 남겨보세요.</p>
      </div>
    </section>
  </div>

  <!-- 모달 (삭제일 때만 삭제/취소 두 버튼) -->
  <div v-if="modal.show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-box">
      <div class="modal-message">{{ modal.message }}</div>
      <div class="modal-btns">
        <button
          v-if="modal.isDelete"
          class="modal-btn delete"
          @click="handleDeleteConfirm"
        >삭제</button>
        <button
          class="modal-btn"
          @click="closeModal"
        >{{ modal.isDelete ? '취소' : '확인' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
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

const comments = ref([]);
const newCommentContent = ref('');

const modal = ref({
  show: false,
  message: '',
  onDelete: null,
  isDelete: false,
});

// 삭제/취소 모달. 삭제 아닐 땐 확인만 보여주기
function showModal(message, onDelete = null, isDelete = false) {
  modal.value = { show: true, message, onDelete, isDelete };
}
function closeModal() {
  modal.value.show = false;
}
function handleDeleteConfirm() {
  closeModal();
  if (typeof modal.value.onDelete === 'function') modal.value.onDelete();
}

function formatRelativeTime(dateString) {
  const now = new Date();
  const date = new Date(dateString);
  const diffInSeconds = Math.floor((now - date) / 1000);

  const minutes = Math.floor(diffInSeconds / 60);
  if (minutes < 60) return `${minutes}분 전`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;

  return new Intl.DateTimeFormat('ko-KR').format(date);
}

onMounted(() => {
  Promise.all([fetchCurrentUser(), fetchPost(), fetchComments()])
    .then(() => console.log('✅ 데이터 로딩 완료'))
    .catch(err => console.error('❌ 데이터 로딩 중 오류:', err));
});

async function fetchCurrentUser() {
  try {
    const response = await axios.get(`/api/user/me`, { withCredentials: true });
    currentUser.value = response.data;
  } catch (err) {
    console.error('현재 사용자 정보를 가져오는데 실패했습니다.', err);
    currentUser.value = null;
  }
}

async function fetchPost() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`/api/posts/${postId.value}`, { withCredentials: true });
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
    const response = await axios.get(`/api/posts/${postId.value}/comments`, { withCredentials: true });
    comments.value = response.data;
  } catch (err) {
    console.error('댓글 목록을 불러오는 데 실패했습니다:', err);
  }
}

function toggleOptionsMenu() {
  isOptionsMenuVisible.value = !isOptionsMenuVisible.value;
}

function editPost() {
  router.push(`/post/edit/${postId.value}`);
}

// 삭제: 삭제 모달은 삭제/취소 두 버튼, 삭제 아닌 경우는 확인 버튼만
async function deletePost() {
  showModal('정말로 이 게시물을 삭제하시겠습니까?', async () => {
    try {
      await axios.delete(`/api/posts/${postId.value}`, { withCredentials: true });
      showModal('게시물이 삭제되었습니다.', null, false);
      router.push('/CommunityView');
    } catch (err) {
      console.error('게시물 삭제 중 오류 발생:', err);
      showModal('게시물 삭제에 실패했습니다.', null, false);
    }
  }, true);
}

function sharePost() {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(window.location.href)
      .then(() => showModal('게시물 링크가 복사되었습니다.', null, false))
      .catch(() => showModal('링크 복사에 실패했습니다.', null, false));
  } else {
    showModal('이 게시물을 공유합니다!', null, false);
  }
  isOptionsMenuVisible.value = false;
}

async function toggleLike() {
  if (!post.value) return;
  try {
    const response = await axios.post(`/api/posts/${post.value.postId}/like`, null, { withCredentials: true });
    post.value.likes = response.data;
  } catch (err) {
    console.error('좋아요 처리 중 오류가 발생했습니다:', err);
    if (err.response && err.response.status === 401) {
      showModal('좋아요를 누르려면 로그인이 필요합니다.', null, false);
    } else {
      showModal('좋아요 처리에 실패했습니다. 다시 시도해주세요.', null, false);
    }
  }
}

async function submitComment() {
  if (!newCommentContent.value.trim()) {
    showModal('댓글 내용을 입력해주세요.', null, false);
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
    if (post.value) post.value.comments++;
  } catch (err) {
    console.error('댓글 작성 중 오류 발생:', err);
    showModal('댓글 작성에 실패했습니다.', null, false);
  }
}

async function deleteComment(commentId) {
  showModal('정말로 이 댓글을 삭제하시겠습니까?', async () => {
    try {
      await axios.delete(`/api/comments/${commentId}`, { withCredentials: true });
      comments.value = comments.value.filter(comment => comment.commentId !== commentId);
      if (post.value) post.value.comments--;
      showModal('댓글이 삭제되었습니다.', null, false);
    } catch (err) {
      console.error('댓글 삭제 중 오류 발생:', err);
      showModal('댓글 삭제에 실패했습니다.', null, false);
    }
  }, true);
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap');
.page-container {
  width: 100%;
  margin-top: 70px;
  background: #FAFAFA;
  font-family: 'Inter', sans-serif;
  height: calc(100vh - 90px - 90px);
  overflow-y: auto;
  box-sizing: border-box;
  padding-bottom: 60px;
}

.status-container {
  padding: 50px;
  text-align: center;
  color: #525252;
}

.post-section {
  background: #FFFFFF;
  padding: 16px;
  margin-right: 10px;
  border-bottom: 1px solid #F5F5F5;
}
.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.author-profile-img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
}
.author-details {
  flex-grow: 1;
}
.author-name {
  font-size: 16px;
  color: #171717;
  font-weight: 500;
}
.post-time {
  font-size: 14px;
  color: #737373;
}
.post-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 1px;
}
.action-btn {
  border: none;
  background: none;
  font-size: 20px;
  cursor: pointer;
  color: #525252;
}
.more-options-group {
  position: relative;
}
.options-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 8px;
  z-index: 10;
  width: 100px;
  text-align: center;
}
.options-menu div {
  padding: 8px;
  cursor: pointer;
  border-radius: 4px;
}
.options-menu div:hover {
  background-color: #f5f5f5;
}
.options-menu .delete {
  color: #E81224;
}
.post-title {
  font-size: 20px;
  font-weight: bold;
  color: #171717;
  margin: 16px 0;
}
.post-content {
  font-size: 16px;
  line-height: 1.6;
  color: #171717;
  white-space: pre-wrap;
  margin-bottom: 16px;
}
.post-image-container {
  width: 100%;
  background: #D4D4D4;
  border-radius: 8px;
  margin: 16px 0;
  overflow: hidden;
}
.post-image {
  width: 100%;
  height: auto;
  display: block;
}
.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid #F5F5F5;
}
.stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #525252;
}
.like-btn {
  cursor: pointer;
}
.comment-count {
  font-size: 14px;
  color: #737373;
}
.comment-input-section {
  display: flex;
  padding: 17px 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #F5F5F5;
  gap: 8px;
}
.comment-input {
  flex-grow: 1;
  height: 38px;
  border: 1px solid #D4D4D4;
  border-radius: 9999px;
  padding: 0 16px;
  font-size: 14px;
  font-family: 'Inter', sans-serif;
}
.comment-input::placeholder {
  color: #ADAEBC;
}
.comment-input:focus {
  outline: 1px solid #8E97FD;
}
.submit-btn {
  width: 60px;
  height: 36px;
  background: #8E97FD;
  color: white;
  border: none;
  border-radius: 9999px;
  font-size: 14px;
  cursor: pointer;
  font-family: 'Inter', sans-serif;
}
.comment-list-section {
  background: #FFFFFF;
}
.comment-item {
  display: flex;
  padding: 16px;
  border-bottom: 1px solid #F5F5F5;
  gap: 12px;
  position: relative;
}
.author-profile-img-sm {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}
.comment-body {
  flex-grow: 1;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.author-name-sm {
  font-size: 14px;
  font-weight: 500;
  color: #171717;
  font-family: 'Inter', sans-serif;
}
.post-time-sm {
  font-size: 12px;
  color: #737373;
  font-family: 'Inter', sans-serif;
}
.comment-content {
  font-size: 14px;
  line-height: 1.6;
  color: #404040;
  font-family: 'Inter', sans-serif;
}
.comment-delete-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  border: none;
  background: none;
  color: #A3A3A3;
  cursor: pointer;
  font-weight: bold;
}
.no-comments {
  padding: 40px;
  text-align: center;
  color: #737373;
}

/* 모달 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.modal-box {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 32px rgba(142, 151, 253, 0.15);
  padding: 24px 16px 14px 16px;
  min-width: 240px;
  max-width: 340px;
  width: 88%;
  border: 2px solid #e8edfb;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.modal-message {
  font-size: 14px;
  color: #444;
  margin-bottom: 18px;
  font-family: 'Inter', sans-serif;
  white-space: pre-line;
}
/* 버튼 그룹 가로 정렬 */
.modal-btns {
  display: flex;
  gap: 10px;
  width: 100%;
  justify-content: center;
}
.modal-btn {
  background: #8E97FD;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  border: none;
  border-radius: 8px;
  padding: 9px 0;
  width: 92px;
  cursor: pointer;
  transition: background 0.21s;
  font-family: 'Inter', sans-serif;
}
.modal-btn:hover {
  background: #5f70d3;
}
.modal-btn.delete {
  background: #E81224;
}
.modal-btn.delete:hover {
  background: #B2151D;
  color: #fff;
}
</style>

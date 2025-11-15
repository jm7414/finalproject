<template>
  <div v-if="loading" class="status-container">
    <p>데이터를 불러오는 중입니다...</p>
  </div>

  <div v-else-if="error" class="status-container">
    <p>{{ error }}</p>
  </div>

  <div v-else-if="report" class="page-container">
    <!-- 1. 제보 본문 섹션 (이전과 동일) -->
    <section class="post-section">
      <div class="post-header">
        <img :src="report.authorProfileImage || defaultProfileImage" alt="프로필" class="author-profile-img">
        <div class="author-details">
          <div class="author-name">{{ report.author }}</div>
          <div class="post-time">{{ formatRelativeTime(report.createdAt) }}</div>
        </div>
        <div class="post-actions">
          <button @click="sharePost" class="action-btn">🔗</button>
          <div v-if="currentUser && (report.userNo === currentUser.userNo || currentUser.userNo === 1)"
            class="more-options-group">
            <button @click="toggleOptionsMenu" class="action-btn">⋮</button>
            <div v-if="isOptionsMenuVisible" class="options-menu">
              <div @click="editReport">수정</div>
              <div @click="deleteReport" class="delete">삭제</div>
            </div>
          </div>
        </div>
      </div>

      <p class="post-content">{{ report.content }}</p>

      <div v-if="report.imagePath" class="post-image-container">
        <img :src="report.imagePath" alt="제보 이미지" class="post-image">
      </div>

      <div class="post-footer">
        <div></div> <!-- '좋아요'/'조회수'가 있던 자리 (레이아웃 유지) -->
        <span class="comment-count">댓글 {{ comments.length }}개</span>
      </div>
    </section>

    <!-- ⭐ 2. [수정] 댓글 입력창 추가 -->
    <section class="comment-input-section">
      <input 
        type="text" 
        class="comment-input" 
        placeholder="댓글을 입력하세요..." 
        v-model="newCommentContent"
        @keyup.enter="submitComment"
      >
      <button @click="submitComment" class="submit-btn">게시</button>
    </section>

    <!-- ⭐ 3. [수정] 댓글 목록 추가 -->
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
          <!-- (댓글 DTO에 userNo가 포함되어 있다고 가정) -->
          <button 
            v-if="currentUser && (comment.userNo === currentUser.userNo || currentUser.userNo === 1)"
            @click="deleteComment(comment.commentId)"
            class="comment-delete-btn">
            X
          </button>
        </div>
      </div>
      <div v-else class="no-comments">
        <p>첫 댓글을 남겨보세요.</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import defaultProfileImage from '@/assets/default-profile.png';

const route = useRoute();
const router = useRouter();

// 'post' -> 'report'로 변수명 변경 (제보)
const reportId = ref(route.params.id); 
const report = ref(null);
const loading = ref(true);
const error = ref(null);
const isOptionsMenuVisible = ref(false);
const currentUser = ref(null);
const missingPostId = ref(route.params.id); 

// 댓글 관련 변수 (기존 CommunityPost.vue와 동일)
const comments = ref([]);
const newCommentContent = ref('');

// --- 시간 포맷팅 (동일) ---
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

// --- onMounted (API 주소 수정) ---
onMounted(() => {
  Promise.all([
    fetchCurrentUser(),
    fetchReport(), // 제보 내용
    fetchComments() // 제보의 댓글
  ])
    .then(() => {
      console.log("✅ 모든 제보 상세 데이터 로딩 완료");
    })
    .catch(error => {
      console.error("❌ 제보 상세 데이터 로딩 중 오류 발생:", error);
    });
});

// --- 데이터 로딩 함수 (API 주소 수정) ---
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

// [API 1] 제보 본문 1건 조회
async function fetchReport() {
  loading.value = true;
  error.value = null;
  try {
    // API: /api/sighting-reports/{id}
    const response = await axios.get(`/api/sighting-reports/${reportId.value}`, {
      withCredentials: true
    });
    report.value = response.data;
  } catch (err) {
    console.error('제보 데이터를 불러오는 데 실패했습니다:', err);
    error.value = '제보를 불러올 수 없습니다.';
  } finally {
    loading.value = false;
  }
}

// [API 2] 제보에 달린 '댓글 목록' 조회
async function fetchComments() {
  try {
    // API: /api/sighting-reports/{id}/comments
    const response = await axios.get(`/api/sighting-reports/${reportId.value}/comments`, {
      withCredentials: true
    });
    comments.value = response.data;
  } catch (error) {
    console.error("제보 댓글 목록을 불러오는 데 실패했습니다:", error);
  }
}

// --- 제보 본문 액션 (API 주소 수정) ---
function toggleOptionsMenu() {
  isOptionsMenuVisible.value = !isOptionsMenuVisible.value;
}

function editReport() {
  // SightingReportWrite.vue (수정 모드)로 이동
  router.push({ name: 'ReportEdit', params: { id: reportId.value } });
}

async function deleteReport() {
  if (confirm('정말로 이 제보를 삭제하시겠습니까?')) {
    try {
      await axios.delete(`/api/sighting-reports/${reportId.value}`, {
        withCredentials: true
      });
      alert('제보가 삭제되었습니다.');
      const boardId = missingPostId.value?.missingPostId; 
      if (boardId) {
        router.push({ 
          name: 'SightingReportBoard',
          params: { id: missingPostId.value } 
        });
      } else {
        console.error("삭제 후 목록 ID(missingPostId)를 찾지 못해 홈으로 이동합니다.");
        router.push('/');
      }
    } catch (err) {
      console.error('제보 삭제 중 오류 발생:', err);
      alert('제보 삭제에 실패했습니다.');
    }
  }
}

function sharePost() {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(window.location.href)
      .then(() => alert('게시물 링크가 복사되었습니다.'))
      .catch(err => alert('링크 복사에 실패했습니다.'));
  } else {
    alert('이 게시물을 공유합니다!');
  }
  isOptionsMenuVisible.value = false;
}

// --- ⭐ [신규] 댓글 액션 함수 (백엔드 연결) ---

// [API 3] 제보에 '댓글 쓰기'
async function submitComment() {
  if (!newCommentContent.value.trim()) {
    alert("댓글 내용을 입력해주세요.");
    return;
  }
  try {
    // API: POST /api/sighting-reports/{id}/comments
    const response = await axios.post(
      `/api/sighting-reports/${reportId.value}/comments`,
      { content: newCommentContent.value },
      { withCredentials: true }
    );
    // (백엔드가 새로 생성된 댓글 객체를 반환한다고 가정)
    comments.value.unshift(response.data); 
    newCommentContent.value = '';
  } catch (error) {
    console.error("댓글 작성 중 오류 발생:", error);
    alert("댓글 작성에 실패했습니다.");
  }
}

// [API 4] 제보 '댓글 삭제'
async function deleteComment(commentId) {
  if (confirm("정말로 이 댓글을 삭제하시겠습니까?")) {
    try {
      // (이 API는 /api/sighting-report-comments/{commentId} 처럼
      //  별도의 컨트롤러가 필요할 수 있습니다. 백엔드 설계에 맞춰 수정하세요.)
      await axios.delete(`/api/sighting-report-comments/${commentId}`, {
        withCredentials: true
      });
      comments.value = comments.value.filter(comment => comment.commentId !== commentId);
      alert("댓글이 삭제되었습니다.");
    } catch (error) {
      console.error("댓글 삭제 중 오류 발생:", error);
      alert("댓글 삭제에 실패했습니다.");
    }
  }
}
</script>

<style scoped>
/* 전체 페이지 컨테이너 */
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

/* 게시글 섹션 */
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
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
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

/* 댓글 입력 섹션 */
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
}

/* 댓글 목록 섹션 */
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
}

.post-time-sm {
  font-size: 12px;
  color: #737373;
}

.comment-content {
  font-size: 14px;
  line-height: 1.6;
  color: #404040;
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
</style>
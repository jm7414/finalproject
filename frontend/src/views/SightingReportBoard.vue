<template>
  <div class="board-container">
    <div class="board-header">
      <h1 class="board-title">제보 게시판</h1>
      <button @click="goToCreateReport" class="write-btn">
        <i class="bi bi-pencil-fill"></i> 제보 등록
      </button>
    </div>

        <div v-if="loading" class="status-container">
      <p>제보 목록을 불러오는 중...</p>
    </div>
    <div v-else-if="error" class="status-container">
      <p>{{ error }}</p>
    </div>

        <div v-else-if="reports.length > 0" class="report-list">
      <div v-for="report in reports" :key="report.sightingReportId" class="report-card" @click="goToReportDetail(report.sightingReportId)">
        
                <div class="card-header">
          <img :src="report.authorProfileImage || defaultProfileImage" alt="프로필" class="author-profile-img">
          <div class="author-details">
            <div class="author-name">{{ report.author }}</div>
            <div class="report-time">{{ formatRelativeTime(report.createdAt) }}</div>
          </div>
        </div>

                <div class="card-body">
          <p class="report-content">{{ truncateContent(report.content) }}</p>
          <img v-if="report.imagePath" :src="report.imagePath" alt="제보 이미지" class="report-thumbnail">
        </div>

                <div class="card-footer">
          <span><i class="bi bi-chat-dots-fill"></i> 댓글 {{ report.commentCount || 0 }}개</span>
        </div>

      </div>
    </div>

        <div v-else class="status-container empty">
      <p>아직 등록된 제보가 없습니다.<br>첫 번째 제보를 등록해보세요.</p>
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

// PredictLocation.vue에서 '제보하기'를 누를 때,
// /report-board/{missingPostId} 처럼 URL에 missingPostId가 넘어와야 합니다.
const missingPostId = ref(route.params.id); 

const reports = ref([]);
const loading = ref(true);
const error = ref(null);

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

// --- 내용 요약 ---
function truncateContent(content) {
  if (content.length > 100) {
    return content.substring(0, 100) + '...';
  }
  return content;
}

// --- onMounted: 제보 목록 불러오기 ---
onMounted(() => {
  fetchReports();
});

// 특정 실종 건(missingPostId)에 대한 '제보 목록' 불러오기
async function fetchReports() {
  loading.value = true;
  error.value = null;
  try {
    // 이 API는 백엔드에서 새로 만들어야 합니다.
    // (예: /api/missing-persons/{id}/sighting-reports)
    const response = await axios.get(`/api/sighting-reports/missing/${missingPostId.value}`, {
      withCredentials: true
    });
    reports.value = response.data;
  } catch (err) {
    console.error('제보 목록을 불러오는 데 실패했습니다:', err);
    error.value = '제보 목록을 불러올 수 없습니다.';
  } finally {
    loading.value = false;
  }
}

// --- 페이지 이동 함수 ---

// '제보 상세' 페이지(MissingSightingReport.vue)로 이동
function goToReportDetail(reportId) {
  router.push(`/SightingReport/${reportId}`); 
}

// '제보 작성' 페이지로 이동
function goToCreateReport() {
  router.push({ 
    name: 'SightingReportWrite',
    params: { id: missingPostId.value } 
  });
}
</script>

<style scoped>
.board-container {
  width: 100%;
  max-width: 600px; /* PC에서도 너무 넓어지지 않게 */
  margin: 0 auto;
  padding-top: 70px; /* 헤더 높이만큼 띄우기 */
  padding-bottom: 70px; /* 푸터 높이만큼 띄우기 */
  background-color: #f4f6f8;
  min-height: 100vh;
}

.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
}

.board-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.write-btn {
  background-color: #667eea; /* 메인 색상 */
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}
.write-btn i {
  font-size: 12px;
}

.status-container {
  padding: 60px 20px;
  text-align: center;
  color: #555;
}
.status-container.empty {
  color: #888;
  line-height: 1.6;
}

.report-list {
  display: flex;
  flex-direction: column;
  gap: 10px; /* 카드 사이 간격 */
  padding: 10px;
}

/* 제보 카드 스타일 */
.report-card {
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  padding: 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.report-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.author-profile-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
}

.author-details {
  flex-grow: 1;
}

.author-name {
  font-size: 15px;
  color: #171717;
  font-weight: 600;
}

.report-time {
  font-size: 13px;
  color: #737373;
}

.card-body {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.report-content {
  font-size: 15px;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
  word-break: break-all;
  flex: 1;
}

.report-thumbnail {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eee;
}

.card-footer {
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
}
.card-footer i {
  margin-right: 4px;
}
</style>
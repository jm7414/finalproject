<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const missingPeople = ref([]);
const loading = ref(true);
const error = ref(null);

onMounted(() => {
  fetchMissingPeople();
});

async function fetchMissingPeople() {
  loading.value = true;
  error.value = null;
  try {
    // API 엔드포인트는 실제 서버 주소에 맞게 조정해야 합니다.
    const response = await axios.get('http://localhost:8080/api/missing-posts');
    missingPeople.value = response.data;
  } catch (err) {
    console.error("실종자 목록을 불러오는 데 실패했습니다:", err);
    error.value = "데이터를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.";
  } finally {
    loading.value = false;
  }
}


// 상세 정보 페이지로 이동하는 함수
function goToDetail(personId) {
    // 실제 실종자 상세 페이지의 경로로 수정해야 합니다.
    router.push(`/missing/${personId}`);
}

// 상대 시간 계산 함수
function formatTimeAgo(dateString) {
  const now = new Date();
  const postDate = new Date(dateString);
  const seconds = Math.floor((now - postDate) / 1000);

  const hours = Math.floor(seconds / 3600);
  if (hours > 0) return `${hours}시간 전`;
  
  const minutes = Math.floor(seconds / 60);
  if (minutes > 0) return `${minutes}분 전`;
  
  return "방금 전";
}

// 날짜와 시간을 포맷팅하는 함수
function formatDateTime(dateString) {
    if (!dateString) return '정보 없음';
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}
</script>

<template>
  <div class="missing-page-container">
    <section class="urgent-notice">
      <div class="notice-icon">📢</div>
      <div class="notice-text">
        <p class="main-text">긴급 실종신고</p>
        <p class="sub-text">실종신고 {{ missingPeople.length }}건이 등록되었습니다</p>
      </div>

    </section>

    <main class="missing-list">
      <div v-if="loading" class="status-message">실종자 목록을 불러오는 중입니다...</div>
      <div v-else-if="error" class="status-message error">{{ error }}</div>
      
      <div v-else-if="missingPeople.length === 0" class="status-message">등록된 실종신고가 없습니다.</div>

      <div v-else v-for="person in missingPeople" :key="person.missingId" class="card" @click="goToDetail(person.missingId)">
        <div class="card-main-info">
          <img :src="person.image || '/default-person.png'" :alt="person.name" class="person-image">
          <div class="person-details">
            <h3>{{ person.name }} ({{ person.age }}세)</h3>
            <span>{{ formatTimeAgo(person.missingTime) }}</span>
            <p>실종일시: {{ formatDateTime(person.missingTime) }}</p>
            <p>실종장소: {{ person.missingLocation }}</p>
          </div>
        </div>
        <div class="card-extra-info">
          <div class="info-item">
            <span class="tag">신체 특징</span>
            <p>{{ person.physicalFeatures }}</p>
          </div>
          <div class="info-item">
            <span class="tag">착의사항</span>
            <p>{{ person.clothing }}</p>
          </div>
        </div>
        <button class="map-button" @click.stop="goToDetail(person.missingId)">
          📍 마지막 위치 보기
        </button>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* 전체 레이아웃 */
.missing-page-container {
  width: 100%;
  background: #FAFAFA;
  font-family: 'Inter', sans-serif;
}

/* 긴급 알림 섹션 */
.urgent-notice {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #F5F5F5;
  border-bottom: 1px solid #E5E5E5;
}
.notice-icon {
  font-size: 20px;
}
.notice-text {
  flex-grow: 1;
}
.notice-text p {
  margin: 0;
}
.main-text {
  font-size: 14px;
  color: #262626;
  font-weight: bold;
}
.sub-text {
  font-size: 12px;
  color: #525252;
}

/* 실종자 목록 */
.missing-list {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.status-message {
  padding: 40px;
  text-align: center;
  color: #737373;
}
.error {
  color: red;
}

/* 실종자 카드 */
.card {
  display: flex;
  flex-direction: column;
  padding: 17px;
  background: #FFFFFF;
  border: 1px solid #E5E5E5;
  box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.card:hover {
  transform: translateY(-4px);
  box-shadow: 0px 4px 15px rgba(0, 0, 0, 0.1);
}

.card-main-info {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.person-image {
  width: 106px;
  height: 106px;
  border-radius: 8px;
  object-fit: cover;
  background-color: #D4D4D4;
  flex-shrink: 0;
}
.person-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.person-details h3 {
  font-size: 16px;
  font-weight: bold;
  color: #171717;
  margin: 0;
}
.person-details span {
  font-size: 14px;
  color: #525252;
}
.person-details p {
  font-size: 14px;
  color: #525252;
  margin: 0;
}

.card-extra-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tag {
  display: inline-block;
  padding: 2px 8px;
  background: #DCDCDC;
  border-radius: 9999px;
  font-size: 12px;
  color: #262626;
  flex-shrink: 0;
}
.info-item p {
  margin: 0;
  font-size: 14px;
  color: #525252;
}

.map-button {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  width: 100%;
  height: 36px;
  background: #8E97FD;
  border-radius: 8px;
  border: none;
  color: #FFFFFF;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
}
</style>
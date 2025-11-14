<template>
  <div class="missing-page-container-wall-poster">
    <header class="page-title-section">
      <h1>실종자 현황</h1>
    </header>

    <div v-if="loading" class="status-message">
      <p>실종자 정보를 불러오는 중입니다...</p>
    </div>
    <div v-else-if="error" class="status-message error">
      <p>{{ error }}</p>
    </div>
    <div v-else-if="missingPeople.length === 0" class="status-message">
      <p>현재 등록된 실종자 정보가 없습니다.</p>
    </div>

    <div v-else class="content-area">
      <section class="highlight-poster-section" v-if="missingPeople.length > 0">
        <div v-for="person in missingPeople" :key="person.missingPostId" class="highlight-poster" @click="openMissingDetailModal(person)">
          <img :src="person.photoPath || defaultPersonImage" :alt="person.patientName" class="poster-image">
          <div class="poster-details">
            <h2>실종자를 찾습니다</h2>
            <p><strong>이름:</strong> {{ person.patientName || '이름 없음' }}</p>
            <p><strong>나이:</strong> {{ calculateAge(person.patientBirthDate) }}세</p>
            <p><strong>실종일시:</strong> {{ formatDateTime(person.reportedAt) }}</p>
            <p class="description-summary"><strong>특징:<br></strong>{{ formatDescription(person.description) }}</p>
            <div class="poster-footer">
              <span class="search-count">함께 찾는 이웃: {{ person.searchTogetherCount || 0 }}명</span>
              <button @click.stop="openMissingDetailModal(person)" class="action-button">상세 정보 보기</button>
            </div>
          </div>
        </div>
      </section>
    </div>
    <MissingDetailModal v-if="isModalVisible" :person="selectedPerson" @close="closeMissingDetailModal"@join-search="navigateToPredictLocation" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import MissingDetailModal from '../MissingDetailModal.vue';

// 기존 로직은 여기에 그대로 유지합니다.
// 스크립트 내용은 변경하지 않고, 템플릿과 스타일만 변경되었습니다.

const router = useRouter();
const missingPeople = ref([]);
const loading = ref(true);
const error = ref(null);

// 상세정보 출력용
const descriptionLabels = {
  appearance: "인상착의",
  hair: "두발상태",
  health: "건강상태/병력",
  items: "소지품",
  other: "기타 특이사항"
};

onMounted(() => {
  fetchMissingPeople();
});

async function fetchMissingPeople() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`/api/missing-persons`, {
      withCredentials: true
    });
    missingPeople.value = response.data;
  } catch (err) {
    console.error("실종자 목록을 불러오는 데 실패했습니다:", err);
    error.value = "데이터를 불러올 수 없습니다.";
  } finally {
    loading.value = false;
  }
}

const selectedPerson = ref(null);
const isModalVisible = ref(false);

function openMissingDetailModal(person) {
  selectedPerson.value = person;
  isModalVisible.value = true;
}

function closeMissingDetailModal() {
  isModalVisible.value = false;
  selectedPerson.value = null;
}

async function navigateToPredictLocation(missingPostId) {
  if (missingPostId === null || missingPostId === undefined) {
    console.error("ID가 없어 '함께 찾기' 및 이동을 할 수 없습니다.");
    alert("오류: 처리에 필요한 ID가 없습니다.");
    return;
  }
  try {
    const response = await axios.post(
      `/api/missing-persons/${missingPostId}/join`,
      {},
      { withCredentials: true }
    );
    if (response.data && response.data.success) {
      console.log(response.data.message);
    } else {
      throw new Error(response.data.message || "함께 찾기에 실패했습니다.");
    }
    router.push(`/predict-location/${missingPostId}`);
    closeMissingDetailModal();
  } catch (err) {
    console.error("'함께 찾기' 처리 중 오류 발생:", err);
    if (err.response && err.response.status === 401) {
      alert("로그인이 필요합니다.");
    } else {
      alert(err.message || "처리 중 오류가 발생했습니다.");
    }
  }
}

function formatTimeAgo(dateString) {
  if (!dateString) return '';
  try {
    const now = new Date();
    const postDate = new Date(dateString);
    if (isNaN(postDate)) return '시간 정보 없음';
    const seconds = Math.floor((now - postDate) / 1000);
    if (isNaN(seconds) || seconds < 0) return '시간 정보 없음';
    const hours = Math.floor(seconds / 3600);
    if (hours > 0) return `${hours}시간 전`;
    const minutes = Math.floor(seconds / 60);
    if (minutes > 0) return `${minutes}분 전`;
    return "방금 전";
  } catch (e) {
    console.error("시간 계산 오류:", e, dateString);
    return '시간 계산 오류';
  }
}

function formatDateTime(dateString) {
  if (!dateString) return '정보 없음';
  try {
    const date = new Date(dateString);
    if (isNaN(date)) return '날짜 형식 오류';
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'numeric', day: 'numeric',
      hour: 'numeric', minute: '2-digit', hour12: true
    });
  } catch (e) {
    console.error("날짜 포맷 오류:", e, dateString);
    return '날짜 형식 오류';
  }
}

function calculateAge(birthDateString) {
  if (!birthDateString) return '?';
  try {
    const birthDate = new Date(birthDateString);
    if (isNaN(birthDate)) return '?';
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age >= 0 ? age : '?';
  } catch (e) {
    console.error("나이 계산 오류:", e, birthDateString);
    return '?';
  }
}

const defaultPersonImage = '/default-person.png';

// 실종자 특징
function formatDescription(jsonString) {
  if (!jsonString) return '정보 없음';

  try {
    // 1. JSON 문자열을 실제 객체로 변환합니다.
    const data = JSON.parse(jsonString);
    const parts = []; // 한글 라인들을 저장할 배열

    // 2. 라벨 순서대로(appearance, hair 등) 값을 확인하고 배열에 추가합니다.
    for (const key in descriptionLabels) {
      const value = data[key];
      
      // 3. 값이 비어있지 않은 항목만 추가합니다.
      if (value) {
        const label = descriptionLabels[key]; // 한글 라벨 (예: "인상착의")
        parts.push(`- ${label}: ${value}`);
      }
    }

    if (parts.length === 0) return '상세 정보가 없습니다.';
    
    // 4. 모든 항목을 "줄바꿈(\n)" 문자로 합쳐서 반환합니다.
    return parts.join('\n');

  } catch (e) {
    // 5. 만약 JSON이 아닌 일반 텍스트가 들어오면, 그냥 \n만 처리해서 반환합니다.
    console.error("설명란(description) JSON 파싱 실패:", e, jsonString);
    return jsonString.replace(/\\n/g, '\n');
  }
}

</script>

<style scoped>
/* 전체 컨테이너: 사이드바 영역 고려 */
.missing-page-container-wall-poster {
  width: 100%;
  max-width: 1200px; /* 전체 페이지 최대 너비 지정 */
  margin: 0 auto; /* 중앙 정렬 */
  padding: 24px 20px;
  background-color: #f0f2f5; /* 페이지 배경색 */
  font-family: 'Noto Sans KR', sans-serif;
  box-sizing: border-box;
}

/* 페이지 제목 */
.page-title-section {
  margin-bottom: 25px;
  border-bottom: 2px solid #ddd;
  padding-bottom: 15px;
}

.page-title-section h1 {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

/* 로딩/에러/빈 메시지 */
.status-message {
  text-align: center;
  padding: 50px;
  font-size: 18px;
  color: #666;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.status-message.error {
  color: #e74c3c;
  font-weight: bold;
}

/* 주요 콘텐츠 영역 (강조 포스터 + 목록) */
.content-area {
  display: flex;
  flex-direction: column;
  gap: 30px; /* 섹션 간 간격 */
}

.highlight-poster-section {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.highlight-poster-section:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.highlight-poster {
  display: flex; /* 사진과 디테일 분리 */
  flex-direction: column; /* 기본은 세로 (모바일 유사) */
  align-items: center;
  padding: 25px;
}

.poster-image {
  width: 100%; /* 너비를 꽉 채움 */
  max-width: 350px; /* 최대 너비 제한 */
  height: auto;
  aspect-ratio: 3 / 4; /* 벽보처럼 세로로 긴 이미지 비율 */
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 4px solid #f8d7da; /* 강조 테두리 */
}

.poster-details {
  flex-grow: 1;
  text-align: center;
}

.poster-details h2 {
  font-size: 32px;
  font-weight: 800;
  color: #c0392b; /* 강조 색상 */
  margin-bottom: 15px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.poster-details p {
  font-size: 18px;
  line-height: 1.6;
  color: #444;
  margin-bottom: 10px;
}

.poster-details strong {
  color: #2c3e50;
  font-weight: 600;
}

.description-summary {
  font-size: 16px;
  color: #555;
  margin-top: 20px;
  border-top: 1px dashed #eee;
  padding-top: 15px;
  white-space: pre-wrap;
}

.poster-footer {
  margin-top: 25px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.search-count {
  font-size: 16px;
  color: #7f8c8d;
  background-color: #ecf0f1;
  padding: 8px 15px;
  border-radius: 5px;
}

.action-button {
  background-color: #8E97FD; /* 액션 버튼 색상 */
  color: white;
  padding: 12px 25px;
  border: none;
  border-radius: 8px;
  font-size: 17px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.3s ease;
  width: 100%;
  max-width: 250px;
}

.action-button:hover {
  background-color: #3498db;
}

.card-image {
  width: 100%;
  aspect-ratio: 16 / 9; /* 와이드 이미지 비율 */
  object-fit: cover;
  background-color: #e0e0e0;
}

.card-info-summary {
  padding: 15px;
  flex-grow: 1;
}

.card-info-summary h4 {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin: 0 0 5px 0;
}

.card-reported-at {
  font-size: 14px;
  color: #777;
  margin-bottom: 10px;
}

.card-description {
  font-size: 14px;
  color: #555;
  line-height: 1.5;
  margin-bottom: 10px;
  /* 2줄 말줄임 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 42px; /* 2줄 높이 확보 */
}

.card-search-count {
  font-size: 13px;
  color: #888;
  background-color: #f5f5f5;
  padding: 5px 10px;
  border-radius: 3px;
  display: inline-block;
  margin-top: auto; /* 하단 정렬 */
}

/* --- 반응형 디자인 --- */
/* PC (사이드바 있어도 여유로운 경우) */
@media (min-width: 992px) {
  .highlight-poster {
    flex-direction: row; /* PC에서는 사진-디테일 가로 배열 */
    align-items: flex-start; /* 상단 정렬 */
    text-align: left; /* 텍스트 왼쪽 정렬 */
    gap: 30px;
  }
  .poster-image {
    width: 300px; /* PC에서 고정 너비 */
    margin-bottom: 0; /* 세로 마진 제거 */
    flex-shrink: 0;
  }
  .poster-details h2,
  .poster-details p {
    text-align: left;
  }
  .poster-footer {
    flex-direction: row; /* 버튼, 카운트 가로 배열 */
    justify-content: flex-start;
    gap: 20px;
    width: 100%;
    margin-top: 30px;
  }
  .action-button {
    max-width: 200px; /* 버튼 너비 조정 */
  }
}

</style>
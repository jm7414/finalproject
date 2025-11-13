<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import MissingDetailModal from './MissingDetailModal.vue'; // 1. 모달 컴포넌트 import

const router = useRouter();
const missingPeople = ref([]);
const loading = ref(true);
const error = ref(null);

onMounted(() => {
  fetchMissingPeople();
});

// 백엔드 API 호출 함수 (커뮤니티 미싱에서 실종자 가져오는 API)
async function fetchMissingPeople() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`/api/missing-persons`, {
      withCredentials: true
    });
    console.log("서버 응답:", JSON.stringify(response.data, null, 2));
    missingPeople.value = response.data;
  } catch (err) {
    console.error("실종자 목록을 불러오는 데 실패했습니다:", err);
    error.value = "데이터를 불러올 수 없습니다.";
  } finally {
    loading.value = false;
  }
}

// 2. 모달 상태 및 선택된 사람 정보
const selectedPerson = ref(null);
const isModalVisible = ref(false);

// 3. 모달 열기 함수
function openMissingDetailModal(person) {
  selectedPerson.value = person;
  isModalVisible.value = true;
}

// 4. 모달 닫기 함수
function closeMissingDetailModal() {
  isModalVisible.value = false;
  selectedPerson.value = null;
}

// 5. 모달의 '함께 찾기' 이벤트 처리 -> PredictLocation으로 이동
// 5. 모달의 '함께 찾기' 이벤트 처리 -> API 호출 후 PredictLocation으로 이동
async function navigateToPredictLocation(missingPostId) {

  // 1. ID가 있는지 다시 한번 확인
  if (missingPostId === null || missingPostId === undefined) {
    console.error("ID가 없어 '함께 찾기' 및 이동을 할 수 없습니다.");
    alert("오류: 처리에 필요한 ID가 없습니다.");
    return; // ID가 없으면 함수 종료
  }

  try {
    // --- [추가된 부분] "함께 찾기" API 호출 ---
    // 컨트롤러의 POST /api/missing-persons/{missingPostId}/join 를 호출
    const response = await axios.post(
      `/api/missing-persons/${missingPostId}/join`,
      {}, // POST 본문이 비어있으면 빈 객체 전송
      { withCredentials: true } // 로그인 세션(인증)을 함께 보냄
    );

    // API가 성공적으로 처리되었는지 확인
    if (response.data && response.data.success) {
      console.log(response.data.message); // "함께 찾기에 참여했습니다." 또는 "이미 참여 중입니다."
    } else {
      // API가 성공(2xx)은 했지만, success: false를 반환한 경우 (예: 404는 catch로 감)
      throw new Error(response.data.message || "함께 찾기에 실패했습니다.");
    }

    // --- [수정된 부분] ID를 가지고 페이지 이동 ---
    router.push(`/predict-location/${missingPostId}`);

    closeMissingDetailModal(); // 모달 닫기

  } catch (err) {
    // axios 호출이 실패했거나(401, 404, 500 등) 위에서 throw new Error를 던진 경우
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
    if (isNaN(postDate)) return '시간 정보 없음'; // 유효하지 않은 날짜 처리

    const seconds = Math.floor((now - postDate) / 1000);
    if (isNaN(seconds) || seconds < 0) return '시간 정보 없음'; // 계산 오류 방지

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

// 날짜/시간 표시 함수 (예: "10월 29일 오후 5:48")
function formatDateTime(dateString) {
  if (!dateString) return '정보 없음';
  try {
    const date = new Date(dateString);
    if (isNaN(date)) return '날짜 형식 오류';
    return date.toLocaleString('ko-KR', {
      year: 'numeric', // 년도 제외
      month: 'numeric', day: 'numeric',
      hour: 'numeric', minute: '2-digit', hour12: true
    });
  } catch (e) {
    console.error("날짜 포맷 오류:", e, dateString);
    return '날짜 형식 오류';
  }
}

// 나이 계산 함수
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
    return age >= 0 ? age : '?'; // 계산 결과가 음수면 '?' 반환
  } catch (e) {
    console.error("나이 계산 오류:", e, birthDateString);
    return '?';
  }
}

const defaultPersonImage = '/default-person.png';

</script>

<template>
  <div class="missing-page-container">

    <main class="missing-list">
      <div v-if="loading" class="status-message">...</div>
      <div v-else-if="error" class="status-message error">{{ error }}</div>
      <div v-else-if="missingPeople.length === 0" class="status-message">
        <p>실종자 없음!</p>맘마미아!
      </div>


      <div v-else v-for="person in missingPeople" :key="person.missingPostId" class="card"
        @click="openMissingDetailModal(person)">
        <div class="card-main-info">
          <img :src="person.photoPath || defaultPersonImage" :alt="person.patientName" class="person-image">
          <div class="person-details">
            <h3>{{ person.patientName || '이름 없음' }} ({{ calculateAge(person.patientBirthDate) }}세)</h3>
            <span>{{ formatTimeAgo(person.reportedAt) }}</span>
            <span>실종일시: </span>
            <p>{{ formatDateTime(person.reportedAt) }}</p>
          </div>
        </div>
        <div class="card-extra-info">
          <div class="info-item full-description">
            <span class="tag">상세정보</span>
            <p>{{ person.description?.trim().replace(/\\n/g, '\n') || '정보 없음' }}</p>
          </div>
          <div class="info-item">
            <span class="tag">함께하는 이웃</span>
            <p>{{ person.searchTogetherCount || 0 }}명</p>
          </div>
        </div>
        <button class="map-button" @click.stop="openMissingDetailModal(person)">
          📍 상세 정보 보기
        </button>
      </div>
    </main>

    <MissingDetailModal v-if="isModalVisible" :person="selectedPerson" @close="closeMissingDetailModal"
      @join-search="navigateToPredictLocation" />

  </div>
</template>

<style scoped>
/* 전체 레이아웃 */
.missing-page-container {
  width: 100%;
  max-width: 500px;
  /* 최대 너비 제한 (선택사항) */
  margin: 0 auto;
  /* 가운데 정렬 (선택사항) */
  background: #FAFAFA;
  font-family: 'Inter', sans-serif;
  padding-bottom: 20px;
  /* 하단 여백 추가 */
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
  padding: 15px;
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
  flex: 1;
  /* 남는 공간 차지 */
  min-width: 0;
  /* 내용 넘침 방지 */
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
  gap: 10px;
  /* 간격 조정 */
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  gap: 8px;
}

.tag {
  padding: 3px 10px;
  /* 패딩 조정 */
  background: #EEEEEE;
  /* 배경색 변경 */
  border-radius: 12px;
  /* 더 둥글게 */
  font-size: 12px;
  color: #333333;
  /* 글자색 변경 */
  flex-shrink: 0;
  height: fit-content;
  /* 내용 높이에 맞춤 */
}

.info-item p {
  margin: 0;
  font-size: 14px;
  color: #525252;
  line-height: 1.6;
  /* 줄간격 */
  white-space: pre-wrap;
  /* \n 줄바꿈 처리 */
  flex: 1;
  /* 남는 공간 차지 */
  min-width: 0;
  /* 내용 넘침 방지 */
  word-break: break-all;
  /* 긴 단어 줄바꿈 처리 */
}

/* 상세정보는 태그를 위쪽에 맞춤 */
.full-description {
  align-items: flex-start;
}

.full-description .tag {
  margin-top: 2px;
  /* 위치 미세 조정 */
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
<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

// 기본 프로필/사진 경로 (public 폴더에 이미지 필요)
const defaultPersonImage = '/default-person.png'; // 기본 이미지 경로

const route = useRoute();
const router = useRouter();
const missingPostId = ref(route.params.id); // URL에서 /:id 값을 가져옴

const detail = ref(null); // API로부터 받을 상세 정보 (MissingPersonDto)
const loading = ref(true);
const error = ref(null);

// 컴포넌트 마운트 시 데이터 로드
onMounted(() => {
  fetchMissingPersonDetail();
});

// 백엔드 API 호출 함수
async function fetchMissingPersonDetail() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`/api/missing-persons/${missingPostId.value}`, {
      withCredentials: true
    });
    detail.value = response.data;
  } catch (err) {
    console.error("실종자 상세 정보를 불러오는 데 실패했습니다:", err);
    if (err.response && err.response.status === 404) {
      error.value = "해당 실종자 정보를 찾을 수 없습니다.";
    } else {
      error.value = "정보를 불러오는 데 실패했습니다.";
    }
  } finally {
    loading.value = false;
  }
}

// 뒤로가기 함수
function goBack() {
  router.back();
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
    return age >= 0 ? age : '?';
  } catch(e) { return '?'; }
}

// description 파싱 및 표시 함수 (개선된 버전, \n 처리 포함)
function formatDescription(desc) {
  if (!desc) return '상세 정보 없음';
  // 서버에서 받은 '\n'을 실제 줄바꿈으로 변경
  return String(desc).replace(/\\n/g, '\n'); 
}

// '지도에서 확인' 버튼 클릭 시 동작 (실제 지도 연동 필요)
function checkMap() {
  // TODO: 지도 API 연동 로직 구현
  alert('지도 연동 기능은 준비 중입니다.');
}

// '함께하기' 버튼 클릭 시 동작 (실제 기능 구현 필요)
function joinSearch() {
  // TODO: 함께하기 API 연동 로직 구현
  alert('함께하기 기능은 준비 중입니다.');
}

</script>

<template>
  <div class="detail-container">
    <div class="header">
      <button @click="goBack" class="back-button">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M10.707 13.293a1 1 0 0 1-1.414 1.414l-6-6a1 1 0 0 1 0-1.414l6-6a1 1 0 0 1 1.414 1.414L5.414 8l5.293 5.293z" fill="#525252"/></svg>
      </button>
      <h1 class="title">실종자 상세정보</h1>
      </div>

    <div v-if="loading" class="status-message">정보를 불러오는 중...</div>
    <div v-else-if="error" class="status-message error">{{ error }}</div>

    <div v-else-if="detail" class="content">
      <section class="info-section">
        <h2>실종자 정보</h2>
        <div class="info-box">
          <div class="info-row">
            <span>이름</span>
            <span>{{ detail.patientName || '정보 없음' }}</span>
          </div>
          <div class="info-row">
            <span>나이</span>
            <span>{{ calculateAge(detail.patientBirthDate) }}세</span>
          </div>
          <div class="info-row">
            <span>보호자 전화번호</span>
            <span>{{ '010-0000-0000' }}</span> </div>
          <div class="info-row detail-description">
            <span>상세정보</span>
            <p>{{ formatDescription(detail.description) }}</p>
          </div>
        </div>
      </section>

      <section class="info-section">
        <h2>실종자 사진</h2>
        <div class="photo-container">
          <img :src="detail.photoPath || defaultPersonImage" :alt="detail.patientName" class="missing-photo">
        </div>
      </section>

      <div class="guide-box">
        <span class="guide-icon">ℹ️</span>
        <div class="guide-text">
          <strong>함께하기란?</strong>
          <p>함께하기 버튼을 눌러서 자신의 위치를 실시간으로 공유하며, 실종자의 예상위치와 또다른 함께하기 중인 이웃의 위치를 확인하실 수 있습니다.</p>
        </div>
      </div>

      <div class="button-section">
        <button @click="checkMap" class="action-button primary">지도에서 확인</button>
        <button @click="joinSearch" class="action-button secondary">함께하기</button>
      </div>
    </div>
    
    <div v-else class="status-message">해당 실종자 정보를 찾을 수 없습니다.</div>

  </div>
</template>

<style scoped>
/* 전체 컨테이너 */
.detail-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 500px;
  min-height: 100vh;
  margin: 0 auto;
  background-color: #FFFFFF;
}

/* 헤더 */
.header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #E5E5E5;
  background-color: #FFFFFF;
  position: sticky; top: 0; z-index: 10;
}
.back-button {
  background: none; border: none; padding: 8px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.title {
  font-size: 18px; font-weight: 600; color: #171717; margin: 0;
  position: absolute; left: 50%; transform: translateX(-50%);
}

/* 로딩/에러 메시지 */
.status-message { padding: 50px; text-align: center; color: #737373; }
.error { color: red; }

/* 메인 컨텐츠 영역 */
.content {
  padding: 24px 16px; /* 좌우 패딩 */
  flex-grow: 1;
}

/* 정보 섹션 (실종자 정보, 사진) */
.info-section {
  margin-bottom: 32px;
}
.info-section h2 {
  font-size: 16px;
  font-weight: 600;
  color: #171717;
  margin: 0 0 12px 0; /* 제목 아래 여백 */
}

/* 실종자 정보 박스 */
.info-box {
  background: #FAFAFA;
  border-radius: 6px;
  padding: 16px;
  font-size: 14px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}
.info-row:last-child {
  margin-bottom: 0;
}
.info-row span:first-child { /* 라벨 (이름, 나이 등) */
  color: #525252;
  flex-shrink: 0; /* 줄어들지 않게 */
  margin-right: 16px; /* 값과의 간격 */
}
.info-row span:last-child { /* 값 (김OO, 75세 등) */
  color: #171717;
  font-weight: 500;
  text-align: right; /* 오른쪽 정렬 */
}
/* 상세정보는 여러 줄 가능하도록 */
.info-row.detail-description {
  flex-direction: column; /* 라벨과 내용을 위아래로 */
  align-items: flex-start; /* 왼쪽 정렬 */
}
.info-row.detail-description span:first-child {
  margin-bottom: 6px; /* 라벨과 내용 사이 간격 */
}
.info-row.detail-description p {
  margin: 0;
  color: #525252; /* 내용 글자색 */
  white-space: pre-wrap; /* 줄바꿈 처리 */
  line-height: 1.6;
  text-align: left; /* 왼쪽 정렬 */
}

/* 실종자 사진 */
.photo-container {
  width: 100%;
  border-radius: 8px;
  overflow: hidden; /* 이미지가 밖으로 나가지 않게 */
  border: 1px solid #eee; /* 테두리 (선택사항) */
}
.missing-photo {
  display: block; /* 이미지 아래 불필요한 공백 제거 */
  width: 100%;
  height: auto; /* 비율 유지 */
  object-fit: contain; /* 이미지가 잘리지 않게 */
  background-color: #f0f0f0; /* 이미지 없을 때 배경색 */
}

/* 함께하기 안내 박스 */
.guide-box {
  display: flex;
  gap: 12px;
  background: #FAFAFA;
  border-radius: 6px;
  padding: 16px; /* 패딩 늘림 */
  margin-bottom: 32px; /* 아래 버튼과의 간격 */
}
.guide-icon { color: #737373; font-size: 18px; margin-top: 2px; }
.guide-text strong {
  display: block; font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 6px;
}
.guide-text p { font-size: 12px; color: #525252; margin: 0; line-height: 1.5; }

/* 하단 버튼 섹션 */
.button-section {
  display: flex;
  flex-direction: column; /* 버튼 위아래 배치 */
  gap: 12px; /* 버튼 사이 간격 */
  padding-bottom: 16px; /* 하단 여백 */
}
.action-button {
  width: 100%;
  padding: 14px; /* 버튼 크기 키움 */
  border-radius: 8px;
  border: none;
  font-size: 16px;
  font-weight: 600; /* 굵게 */
  cursor: pointer;
  text-align: center;
}
.action-button.primary { /* 지도에서 확인 */
  background: #525252;
  color: #FFFFFF;
}
.action-button.secondary { /* 함께하기 */
  background: #E5E5E5;
  color: #404040;
}
</style>
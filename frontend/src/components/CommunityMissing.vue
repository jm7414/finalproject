<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const missingPeople = ref([]);
const loading = ref(true);
const error = ref(null);

// 모달(팝업) 관련 상태
const isModalVisible = ref(false);
const selectedPerson = ref(null);

onMounted(() => {
  fetchMissingPeople();
});

async function fetchMissingPeople() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`/api/missing-posts`);
    missingPeople.value = response.data;
  } catch (err) {
    console.error("실종자 목록을 불러오는 데 실패했습니다:", err);
    error.value = "데이터를 불러올 수 없습니다.";
  } finally {
    loading.value = false;
  }
}

// 지도 버튼 클릭 시 모달 열기
function openMap(person) {
  selectedPerson.value = person;
  isModalVisible.value = true;
}

// 모달 닫기
function closeModal() {
  isModalVisible.value = false;
  selectedPerson.value = null;
}

// 함께하기 버튼 클릭
async function joinSearch(person) {
  try {
    await axios.post(`/api/missing-posts/${person.id}/join`);
    alert(person.name + ' 님 찾기에 함께합니다!');
  } catch (err) {
    alert('참여 처리 중 오류가 발생했습니다.');
  }
}

// 상대 시간 계산 함수
function formatTimeAgo(dateString) {
  const now = new Date();
  const postDate = new Date(dateString);
  const seconds = Math.floor((now - postDate) / 1000);

  let interval = seconds / 3600;
  if (interval > 1) return Math.floor(interval) + "시간 전";
  interval = seconds / 60;
  if (interval > 1) return Math.floor(interval) + "분 전";
  return "방금 전";
}
</script>

<template>
  <div class="list-container">
    <div v-if="loading">실종자 목록을 불러오는 중입니다...</div>
    <div v-else-if="error">{{ error }}</div>

    <div v-else v-for="person in missingPeople" :key="person.id" class="detail-card">
      <div class="card-header">
        <img v-if="person.photoPath" :src="person.photoPath" :alt="person.name" class="profile-image">
        <div v-else class="profile-image-placeholder">사진</div>
        <div class="info-summary">
          <p><strong>성함 :</strong> {{ person.name }}</p>
          <p><strong>나이 :</strong> {{ person.age }}</p>
          <p><strong>실종 시간 :</strong> {{ formatTimeAgo(person.reportedAt) }}</p>
          <p class="timestamp">{{ new Date(person.reportedAt).toLocaleString('ko-KR') }}</p>
        </div>
        <div class="view-count">
          <span class="eye-icon">👁️</span> {{ person.viewCount }}
        </div>
      </div>

      <div class="card-actions">
        <button class="action-button" @click="openMap(person)">지도</button>
        <button class="action-button primary" @click="joinSearch(person)">함께하기</button>
      </div>
    </div>
  </div>

  <div v-if="isModalVisible" class="modal-overlay" @click="closeModal">
    <div class="modal-content" @click.stop>
      <button class="modal-close-button" @click="closeModal">X</button>
      <h2 v-if="selectedPerson">{{ selectedPerson.name }}님 상세 정보</h2>
      <div v-if="selectedPerson">
        <p><strong>나이:</strong> {{ selectedPerson.age }}</p>
        <p><strong>실종 시각:</strong> {{ new Date(selectedPerson.reportedAt).toLocaleString('ko-KR') }}</p>
        <p><strong>특이사항:</strong> 실종자 특이사항</p>
        <button class="action-button primary" style="width:100%; margin-top:20px;">지도로 이동</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.list-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  /* 카드 사이 간격 */
}

.sort-selector-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.sort-selector {
  border: none;
  background: transparent;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
}

.detail-card {
  background: #FFFFFF;
  border: 1px solid #808AFF;
  box-shadow: 0px 4px 20px rgba(0, 0, 0, 0.1);
  border-radius: 20px;
  padding: 24px;
}

.card-header {
  display: flex;
  gap: 20px;
  position: relative;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.profile-image,
.profile-image-placeholder {
  width: 135px;
  height: 135px;
  border-radius: 15px;
  object-fit: cover;
  flex-shrink: 0;
}

.profile-image-placeholder {
  background-color: #f0f0f0;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #ccc;
  font-weight: bold;
}

.info-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 18px;
}

.info-summary p {
  margin: 0;
}

.info-summary .timestamp {
  font-size: 16px;
  color: #888;
}

.view-count {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
}

.card-body {
  padding: 24px 0;
}

.section-title {
  font-size: 30px;
  margin-top: 0;
  margin-bottom: 16px;
  
}

.detail-item {
  display: flex;
  margin-bottom: 12px;
  font-size: 16px;
}

.detail-key {
  width: 200px;
  font-weight: 600;
  font-size: 20px;
  color: #555;
  flex-shrink: 0;
}

.detail-value {
  color: #333;
  font-weight: 600;
  font-size: 18px;
  margin: 4px;
}

.card-actions {
  display: flex;
  gap: 16px;
  margin-top: 24px;
}

.action-button {
  flex: 1;
  padding: 16px;
  border-radius: 30px;
  border: none;
  background-color: #f0f2ff;
  color: #808AFF;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-button.primary {
  background-color: #808AFF;
  color: #FFFFFF;
  box-shadow: 0px 4px 10px rgba(128, 138, 255, 0.4);
}

.action-button:hover {
  transform: translateY(-2px);
}

/* 모달(팝업) 스타일 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  padding: 30px;
  border-radius: 15px;
  width: 90%;
  max-width: 500px;
  position: relative;
}
.modal-close-button {
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
}

</style>
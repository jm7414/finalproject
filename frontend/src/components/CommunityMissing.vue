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
            <p>{{ formatDescription(person.description) }}</p>
          </div>
          <div class="info-item">
            <span class="tag">함께하는 이웃</span>
            <p>{{ person.searchTogetherCount || 0 }}명</p>
          </div>
        </div>
        <button class="map-button" 
                :style="{ background: themeColors.primary }"
                @click.stop="openMissingDetailModal(person)">
          📍 상세 정보 보기
        </button>
      </div>
    </main>

    <MissingDetailModal 
      v-if="isModalVisible" 
      :person="selectedPerson" 
      :theme="theme"
      @close="closeMissingDetailModal"
      @join-search="navigateToPredictLocation" 
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import MissingDetailModal from './MissingDetailModal.vue';

// ✅ props 추가
const props = defineProps({
  theme: {
    type: String,
    default: 'guardian',
    validator: (value) => ['guardian', 'neighbor'].includes(value)
  }
});

// ✅ 테마별 색상
const themeColors = computed(() => {
  return props.theme === 'neighbor' 
    ? {
        primary: '#a7cc10',
        activeText: '#171717',
        inactiveText: '#737373'
      }
    : {
        primary: '#8E97FD',
        activeText: '#171717',
        inactiveText: '#737373'
      };
});

const router = useRouter();
const missingPeople = ref([]);
const loading = ref(true);
const error = ref(null);

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

function formatDescription(jsonString) {
  if (!jsonString) return '정보 없음';

  try {
    const data = JSON.parse(jsonString);
    const parts = [];

    for (const key in descriptionLabels) {
      const value = data[key];
      
      if (value) {
        const label = descriptionLabels[key];
        parts.push(`- ${label}: ${value}`);
      }
    }

    if (parts.length === 0) return '상세 정보가 없습니다.';
    
    return parts.join('\n');

  } catch (e) {
    console.error("설명란(description) JSON 파싱 실패:", e, jsonString);
    return jsonString.replace(/\\n/g, '\n');
  }
}

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

    // (지현)query 파라미터로 source 추가
    router.push({
      path: `/predict-location/${missingPostId}`,
      query: { source: 'neighbor' }
    });
    
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
</script>

<style scoped>
.missing-page-container {
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  background: #FAFAFA;
  font-family: 'Inter', sans-serif;
  padding-bottom: 20px;
}

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
  min-width: 0;
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
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  gap: 8px;
}

.tag {
  padding: 3px 10px;
  background: #EEEEEE;
  border-radius: 12px;
  font-size: 12px;
  color: #333333;
  flex-shrink: 0;
  height: fit-content;
}

.info-item p {
  margin: 0;
  font-size: 14px;
  color: #525252;
  line-height: 1.6;
  white-space: pre-wrap;
  flex: 1;
  min-width: 0;
  word-break: break-all;
}

.full-description {
  align-items: flex-start;
}

.full-description .tag {
  margin-top: 2px;
}

.map-button {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  width: 100%;
  height: 36px;
  border-radius: 8px;
  border: none;
  color: #FFFFFF;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.map-button:hover {
  opacity: 0.9;
}
</style>

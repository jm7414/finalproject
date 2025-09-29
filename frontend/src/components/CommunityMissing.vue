<script setup>
import { ref } from 'vue';
import image1 from '@/assets/images/Missing.jpg';

// 상세 정보가 포함된 실종자 목록 데이터 예시
const missingPeople = ref([
  {
    id: 1,
    name: '오일남',
    age: 79,
    timeSinceMissing: '3 시간',
    missingTimestamp: '2025.09.28 13:35',
    viewCount: 23,
    image: image1 , // 예시 이미지
    details: {
      '실종 복장': '초록색 트레이닝세트',
      '예측 위치': '전주 청주 진주',
      '함께하는 이웃': '3 명',
    },
  },
  {
    id: 2,
    name: '김철수',
    age: 78,
    timeSinceMissing: '12 시간',
    missingTimestamp: '2025.09.28 04:30',
    viewCount: 45,
    image: image1, // 이미지가 없는 경우
    details: {
      '실종 복장': '회색 점퍼와 검은 바지',
      '예측 위치': '서울역 인근',
      '함께하는 이웃': '5 명',
    },
  },
]);

function openMap(person) {
  alert(person.name + ' 님의 위치를 지도로 봅니다.');
}

function joinSearch(person) {
  alert(person.name + ' 님 찾기에 함께합니다.');
}
</script>

<template>
  <div class="list-container">
    <div class="sort-selector-wrapper">
      <select class="sort-selector">
        <option>최근순</option>
        <option>조회순</option>
      </select>
    </div>

    <div v-for="person in missingPeople" :key="person.id" class="detail-card">
      <div class="card-header">
        <img v-if="person.image" :src="person.image" :alt="person.name" class="profile-image">
        <div v-else class="profile-image-placeholder">사진</div>

        <div class="info-summary">
          <p><strong>성함 :</strong> {{ person.name }}</p>
          <p><strong>나이 :</strong> {{ person.age }}</p>
          <p><strong>실종 시간 :</strong> {{ person.timeSinceMissing }}</p>
          <p class="timestamp">{{ person.missingTimestamp }}</p>
        </div>
        <div class="view-count">
          <span class="eye-icon">👁️</span> {{ person.viewCount }}
        </div>
      </div>

      <div class="card-body">
        <h3 class="section-title">상세정보</h3>
        <div v-for="(value, key) in person.details" :key="key" class="detail-item">
          <span class="detail-key">{{ key }}</span>
          <span class="detail-value">{{ value }}</span>
        </div>
      </div>

      <div class="card-actions">
        <button class="action-button" @click="openMap(person)">지도</button>
        <button class="action-button primary" @click="joinSearch(person)">함께하기</button>
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
  width: 120px;
  height: 120px;
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
  font-size: 16px;
}

.info-summary p {
  margin: 0;
}

.info-summary .timestamp {
  font-size: 14px;
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
  font-size: 20px;
  margin-top: 0;
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  margin-bottom: 12px;
  font-size: 16px;
}

.detail-key {
  width: 100px;
  font-weight: 600;
  color: #555;
  flex-shrink: 0;
}

.detail-value {
  color: #333;
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
</style>
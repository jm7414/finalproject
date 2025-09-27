<script setup>
import { ref } from 'vue';

import image1 from '@/assets/images/Missing.jpg';


// '실종자 목록'에 해당하는 데이터 예시입니다.
const missingPeople = ref([
  { id: 1, image: image1, name: '오일남', age: 76, gender: '남', time: '12 시간 전' },
  { id: 2, image: '', name: '이영희', age: 82, gender: '여', time: '2 일 전' },
  // ... 나중에 실제 데이터가 이 배열에 들어옵니다.
]);
</script>

<template>
  <div class="list-container">
    <div 
      v-for="person in missingPeople" 
      :key="person.id" 
      class="list-card"
      @click="handleCardClick(person.id)"
    >
      <div class="image-placeholder">
        <span class="image-text">사진</span>
      </div>
      <div class="info-area">
        <p class="name-details">{{ person.name }} / {{ person.age }}세 / {{ person.gender }}</p>
        <p class="time-stamp">{{ person.time }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.list-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

/* 카드 전체 스타일, 클릭 가능하도록 cursor: pointer 추가 */
.list-card {
  display: flex;
  flex-direction: row; /* 이미지와 텍스트를 가로로 배치 */
  align-items: center; /* 세로 중앙 정렬 */
  padding: 16px;
  width: 100%;
  
  background: #FFFFFF;
  border: 1px solid #808AFF; /* Figma 디자인의 보라색 테두리 */
  box-shadow: 0px 4px 15px rgba(0, 0, 0, 0.1);
  border-radius: 15px;
  cursor: pointer;
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

/* 마우스를 올렸을 때 살짝 커지는 효과 */
.list-card:hover {
  transform: translateY(-3px);
  box-shadow: 0px 6px 20px rgba(0, 0, 0, 0.12);
}

.image-placeholder {
  width: 90px;
  height: 90px;
  border-radius: 12px;
  background-color: #f0f0f0;
  flex-shrink: 0;
  margin-right: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.image-text {
  font-weight: 700;
  color: #a0a0a0;
}

.info-area {
  flex-grow: 1; /* 남는 공간을 모두 차지 */
  display: flex;
  flex-direction: column; /* 텍스트들을 세로로 배치 */
  justify-content: space-between; /* 위아래로 텍스트를 떨어트림 */
  align-self: stretch; /* info-area가 카드 높이를 꽉 채우도록 설정 */
  padding: 4px 0; /* 위아래 약간의 여백 */
}

.name-details {
  margin: 0;
  font-weight: 700;
  font-size: 18px;
  color: #3F414E;
}

.time-stamp {
  margin: 0;
  font-size: 14px;
  color: #a0a0a0;
  text-align: right; /* 시간 텍스트를 오른쪽으로 정렬 */
}
</style>
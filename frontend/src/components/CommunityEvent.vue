<script setup>
import { ref } from 'vue';

import image1 from '@/assets/images/Event.jpg';
import image2 from '@/assets/images/Event2.jpg';
import image3 from '@/assets/images/Event3.jpg';
import image4 from '@/assets/images/Event4.png';

// '이벤트 목록'에 해당하는 데이터 예시입니다.
// 각 이벤트는 고유 id, 이미지 경로(image), 대체 텍스트(alt)를 가집니다.
const events = ref([
  { id: 1, image: image1, alt: '가을맞이 쇼핑 이벤트' },
  { id: 3, image: image2, alt: '가을 하늘처럼 높은 할인율' },
  { id: 4, image: image3, alt: '특별한 가을 쇼핑' },
  { id: 5, image: image4, alt: '가을 선물 무료 할인' },
]);
</script>

<template>
  <div class="event-list-container">
    <a 
      href="#" 
      v-for="event in events" 
      :key="event.id" 
      class="event-banner"
    >
      <img :src="event.image" :alt="event.alt" class="banner-image">
    </a>
  </div>
</template>

<style scoped>
.event-list-container {
  display: flex;
  flex-direction: column; /* 아이템들을 세로로 쌓습니다 */
  gap: 16px; /* 아이템 사이의 간격 */
  width: 100%;
}

.event-banner {
  display: block; /* a 태그의 기본 속성 변경 */
  width: 100%;
  border-radius: 15px;
  overflow: hidden; /* 이미지가 둥근 모서리를 넘어가지 않도록 */
  box-shadow: 0px 4px 15px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

/* 마우스를 올렸을 때 살짝 커지는 효과 */
.event-banner:hover {
  transform: translateY(-3px);
  box-shadow: 0px 6px 20px rgba(0, 0, 0, 0.12);
}

.banner-image {
  display: block; /* 이미지 아래의 불필요한 여백 제거 */
  width: 100%;
  height: auto; /* 이미지 비율 유지 */
}
</style>
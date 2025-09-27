<script setup>
import { ref } from 'vue';

import image1 from '@/assets/images/Post.jpg';

// '게시글 목록'에 해당하는 데이터 예시입니다.
const posts = ref([
  { 
    id: 1, 
    title: '오목 초보만', 
    content: '13시 점심먹고 ㄱㄱ', 
    time: '1 시간 전' 
  },
  { 
    id: 2, 
    title: '꽃이.예쁘네요...', 
    content: '꽃게.입니다.', 
    time: '3 시간 전',
    image: image1
  },
  // ... 나중에 실제 데이터가 이 배열에 들어옵니다.
]);
</script>

<template>
  <div class="post-list-container">
    <button class="fab-button">+</button>

    <div 
      v-for="post in posts" 
      :key="post.id" 
      class="post-card"
    >
      <div class="card-content">
        <h3 class="post-title">{{ post.title }}</h3>
        <p class="post-body">{{ post.content }}</p>
        
        <img v-if="post.image" :src="post.image" alt="게시물 이미지" class="post-image">

        <span class="post-time">{{ post.time }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.post-list-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  position: relative; /* FAB 버튼의 기준점이 되기 위해 필요 */
}

/* 카드 전체 스타일 */
.post-card {
  padding: 20px;
  width: 100%;
  background: #FFFFFF;
  border: 1px solid #E0E0E0; /* 연한 회색 테두리 */
  box-shadow: 0px 4px 15px rgba(0, 0, 0, 0.07);
  border-radius: 15px;
  cursor: pointer;
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

/* 마우스를 올렸을 때 살짝 커지는 효과 */
.post-card:hover {
  transform: translateY(-3px);
  box-shadow: 0px 6px 20px rgba(0, 0, 0, 0.1);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 8px; /* 내부 요소들 사이의 간격 */
}

.post-title {
  margin: 0;
  font-weight: 700;
  font-size: 16px;
  color: #3F414E;
}

.post-body {
  margin: 0;
  font-size: 14px;
  color: #555761;
  line-height: 1.5;
}

.post-image {
  width: 100%;
  height: auto; /* 이미지 비율 유지 */
  border-radius: 10px;
  margin-top: 8px;
}

.post-time {
  margin-top: 12px;
  font-size: 12px;
  color: #a0a0a0;
  text-align: right; /* 시간 텍스트를 오른쪽으로 정렬 */
}

/* 글 작성 버튼 (Floating Action Button) */
.fab-button {
  position: fixed; /* 화면에 고정 */
  bottom: 100px; /* 푸터 높이를 고려한 위치 */
  right: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%; /* 원형 버튼 */
  background-color: #808AFF; /* 보라색 배경 */
  color: white;
  font-size: 32px;
  line-height: 56px;
  text-align: center;
  border: none;
  box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  z-index: 900;
  transition: transform 0.2s ease;
}
.fab-button:hover {
  transform: scale(1.05); /* 마우스 올리면 살짝 커짐 */
}
</style>
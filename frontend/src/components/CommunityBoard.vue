<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router'; // 1. 라우터 import
import image1 from '@/assets/images/Post.jpg';

const posts = ref([
  { 
    id: 1, 
    author: '오일남',
    title: '치매에 좋은 음식', 
    stats: { comments: 3, likes: 10, views: 23 },
    time: '1 시간 전' 
  },
  { 
    id: 2, 
    author: '오일남',
    title: '꽃이 이쁘네요', 
    image: image1,
    stats: { comments: 3, likes: 10, views: 23 },
    time: '3 시간 전'
  },
]);

const router = useRouter(); // 2. 라우터 인스턴스 생성

// 3. 클릭 시 실행될 함수 정의
function goToPostDetail(postId) {
  // '/post/1', '/post/2' 와 같은 경로로 페이지를 이동시킵니다.
  router.push(`/post/${postId}`);
}

function goToPostWrite() {
  router.push(`/CommunityPostWrite`);
}

</script>

<template>
  <div class="post-list-container">
    <div class="sort-selector-wrapper">
      <select class="sort-selector">
        <option>게시일</option>
        <option>인기순</option>
      </select>
    </div>

    <div 
      v-for="post in posts" 
      :key="post.id" 
      class="post-card"
      @click="goToPostDetail(post.id)"
    >
      <div class="card-header">
        <h3 class="post-title">{{ post.title }}</h3>
        <span class="post-author">작성자 : {{ post.author }}</span>
      </div>

      <img v-if="post.image" :src="post.image" alt="게시물 이미지" class="post-image">
      
      <div class="card-footer">
        <div class="post-stats">
          <span>💬 {{ post.stats.comments }}</span>
          <span>❤️ {{ post.stats.likes }}</span>
          <span>👁️ {{ post.stats.views }}</span>
        </div>
        <span class="post-time">{{ post.time }}</span>
      </div>
    </div>
    
    <button class="fab-button" @click="goToPostWrite()">+ 글 작성</button>
  </div>
</template>

<style scoped>
.post-list-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}

.sort-selector-wrapper {
  display: flex;
  justify-content: flex-end;
}
.sort-selector {
  border: none;
  background: transparent;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
}

.post-card {
  display: flex;
  flex-direction: column; /* 카드 내부 요소를 세로로 배치 */
  width: 100%;
  background: #FFFFFF;
  border: 1px solid #808AFF;
  box-shadow: 0px 4px S20px rgba(0, 0, 0, 0.1);
  border-radius: 15px;
  cursor: pointer;
  overflow: hidden; /* 이미지가 둥근 모서리를 넘어가지 않도록 */
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
}

.post-title {
  margin: 0;
  font-weight: 700;
  font-size: 18px;
  color: #3F414E;
}

.post-author {
  font-size: 14px;
  color: #555;
}

.post-image {
  display: block;
  width: 100%;
  height: auto;
  padding-top: 10px;
  padding-bottom: 10px;
  padding-left: 30px;
  padding-right: 30px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background-color: #fafbff; /* 푸터 영역 배경색 살짝 추가 */
}

.post-stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #555;
}
.post-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.post-time {
  font-size: 14px;
  color: #a0a0a0;
}

/* 글 작성 버튼 (Floating Action Button) */
.fab-button {
  position: fixed;
  bottom: 100px;
  right: 24px;
  padding: 12px 24px;
  border-radius: 30px;
  background-color: #8E97FD;
  color: white;
  font-size: 18px;
  font-weight: 700;
  border: none;
  box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  z-index: 900;
  transition: transform 0.2s ease;
}
.fab-button:hover {
  transform: scale(1.05);
}
</style>
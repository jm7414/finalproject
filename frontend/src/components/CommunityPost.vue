<script setup>
import { ref } from 'vue';

// 실제로는 서버에서 받아올 게시물 데이터 예시입니다.
// 이 배열의 내용이 바뀌면 화면도 자동으로 바뀝니다.
const post = ref([
  { id: 3, title: '게시물 3', time: 'N 시간 전' },
  { id: 2, title: '게시물 2', time: 'N 시간 전' },
  { id: 1, title: '게시물 1', time: 'N 시간 전' },
  // { id: 4, title: '새 게시물', time: '방금 전' }, // <- 이 줄의 주석을 풀면 아이템이 추가됩니다.
]);
</script>

<template>
  <div class="post-list-container">
    <div v-for="post in post" :key="post.id" class="post-card">
      <div class="post-image-placeholder"></div>
      <div class="post-info">
        <p class="post-title">{{ post.title }}</p>
      </div>
      <div class="post-time">
        <span>{{ post.time }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 게시물 목록 전체를 감싸는 컨테이너 */
.post-list-container {
  display: flex;
  flex-direction: column; /* 아이템들을 세로로 쌓습니다 */
  gap: 16px; /* 아이템 사이의 간격 */
  width: 100%;
}

/* 개별 게시물 카드 */
.post-card {
  display: flex;
  flex-direction: row; /* 카드 내부 요소들을 가로로 배치 */
  align-items: center; /* 세로 중앙 정렬 */
  width: 100%;
  padding: 12px;
  
  background: #FFFFFF;
  border: 1px solid #40B59F;
  box-shadow: 0px 4px 15px rgba(0, 0, 0, 0.1);
  border-radius: 15px;
}

/* 게시물 이미지 (자리만 잡아둠) */
.post-image-placeholder {
  width: 70px;
  height: 50px;
  border-radius: 10px;
  background-color: #f0f0f0; /* 이미지가 없을 때 회색으로 표시 */
  flex-shrink: 0; /* 크기가 줄어들지 않도록 설정 */
  margin-right: 16px; /* 오른쪽 텍스트와의 간격 */
}

/* 게시물 정보 (제목) */
.post-info {
  flex-grow: 1; /* 남는 공간을 모두 차지하도록 설정 */
}

.post-title {
  margin: 0;
  font-family: 'Quicksand', sans-serif;
  font-weight: 700;
  font-size: 18px;
  color: #40B59F;
}

/* 게시물 시간 */
.post-time {
  font-family: 'Quicksand', sans-serif;
  font-weight: 700;
  font-size: 15px;
  color: #40B59F;
  white-space: nowrap; /* 시간이 줄바꿈되지 않도록 설정 */
}
</style>
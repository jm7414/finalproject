<script setup>
import { ref } from 'vue';
import { useRoute } from 'vue-router';
import profileImage from '@/assets/images/Missing1.jpg';
import postContentImage from '@/assets/images/Post.jpg';
import profileImage2 from '@/assets/images/Missing2.jpg';
const route = useRoute();
const postId = ref(route.params.id || 1); // 라우터에서 게시물 ID 가져오기, 없으면 1번 게시물

// 실제로는 postId를 이용해 서버에서 게시물 데이터를 가져올 것입니다.
// 여기서는 예시 데이터를 사용합니다.
const post = ref({
  id: postId.value,
  author: '오일남',
  authorProfileImage: profileImage, // 프로필 이미지
  date: '2025.09.28',
  title: '꽃이 이쁘네요',
  content: '점심먹고 13시 쯤 길가다가 찍은 사진입니다.\n행복하세요',
  image: postContentImage, // 게시물 본문 이미지
  likes: 10,
  views: 23,
  comments: [
    { id: 1, author: '오일남', authorProfileImage: profileImage, time: '3분 전', text: '다들 한 번 가보세요.' },
    { id: 2, author: '김철수', authorProfileImage: profileImage2, time: '방금 전', text: '지금 어디신가요?' },
  ],
});

const isLiked = ref(false); // 좋아요 상태
const newCommentText = ref(''); // 새로운 댓글 입력창

function toggleLike() {
  isLiked.value = !isLiked.value;
  if (isLiked.value) {
    post.value.likes++;
  } else {
    post.value.likes--;
  }
}

function sharePost() {
  alert('이 게시물을 공유합니다!');
}

function addComment() {
/*  if (newCommentText.value.trim() === '') {
    alert('댓글 내용을 입력해주세요.');
    return;
  }
  const newComment = {
    id: post.value.comments.length + 1,
    author: '현재 사용자 (임시)', // 실제로는 로그인한 사용자 이름
    authorProfileImage: profileImage, // 실제로는 로그인한 사용자 프로필
    time: '방금 전',
    text: newCommentText.value,
  };
  post.value.comments.push(newComment);
  newCommentText.value = ''; // 입력창 비우기 */
  alert('댓글이 작성되었습니다.');
} 


function toggleOptions() {
    alert('댓글 옵션 토글 (수정/삭제 등)');
}

function likeComment() {
    alert('댓글 좋아요!');
}
</script>

<template>
  <div class="post-detail-container">
    <div class="post-header-top">
      <div class="author-info">
        <img :src="post.authorProfileImage" alt="작성자 프로필" class="author-profile-img">
        <div>
          <span class="author-name">{{ post.author }}</span>
          <span class="post-date">{{ post.date }}</span>
        </div>
      </div>
      <button @click="sharePost" class="share-button">
        <span class="material-icons-outlined"></span>🔗 공유
      </button>   
    </div>

    <div class="post-content">
      <h1 class="post-title">{{ post.title }}</h1>
      <p class="post-text">{{ post.content }}</p>
      <img v-if="post.image" :src="post.image" alt="게시물 이미지" class="post-main-image">
    </div>

    <div class="post-footer-stats">
      <span @click="toggleLike" class="like-button">
        <span :class="['material-icons-outlined', isLiked ? 'liked' : '']">❤️</span> {{ post.likes }}
      </span>
      <span>👁️ {{ post.views }}</span>
    </div>

    <div class="comments-section">
      <div class="comment-input-area">
        <input 
          type="text" 
          v-model="newCommentText" 
          placeholder="댓글을 입력해 주세요" 
          class="comment-input"
        />
        <button @click="addComment" class="comment-submit-button">입력</button>
      </div>

      <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
        <div class="comment-author-info">
          <img :src="comment.authorProfileImage" alt="댓글 작성자" class="comment-profile-img">
          <div>
            <span class="comment-author-name">{{ comment.author }}</span>
            <span class="comment-time">{{ comment.time }}</span>
          </div>
        </div>
        <p class="comment-text">{{ comment.text }}</p>
        <div class="comment-actions">
            <span @click="likeComment" class="comment-like-button">👍</span>
            <span @click="toggleOptions" class="comment-options-button">⋮</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.post-detail-container {
  padding: 24px;
  width: 100%;
  max-width: 500px; /* 전체 너비 제한 */
  margin: 0 auto;
  background-color: #FFFFFF;
}

/* 게시물 헤더 */
.post-header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-profile-img {
  width: 56px;
  height: 56px;
  border-radius: 50%; /* 원형 프로필 이미지 */
  object-fit: cover;
  flex-shrink: 0;
}

.author-name {
  display: block;
  font-weight: 700;
  font-size: 18px;
  color: #3F414E;
}

.post-date {
  display: block;
  font-size: 14px;
  color: #555;
}

.share-button {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  border: 1px solid #808AFF;
  border-radius: 15px;
  background-color: #FFFFFF;
  color: #808AFF;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s;
}

/* 게시물 본문 */
.post-content {
  margin-bottom: 20px;
}

.post-title {
  font-weight: 700;
  font-size: 28px;
  margin-top: 0;
  margin-bottom: 12px;
  color: #3F414E;
}

.post-text {
  font-weight: 500;
  font-size: 18px;
  line-height: 1.5;
  color: #3F414E;
  white-space: pre-wrap; /* 줄바꿈 유지 */
  margin-bottom: 20px;
}

.post-main-image {
  width: 100%;
  height: auto;
  border-radius: 10px;
  object-fit: cover;
  margin-top: 10px;
}

/* 게시물 푸터 (좋아요, 조회수) */
.post-footer-stats {
  display: flex;
  gap: 20px;
  padding: 15px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
  font-size: 16px;
  color: #555;
}

.like-button {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}
.like-button .material-icons-outlined {
  color: #FF84A2; /* 하트 기본 색상 */
  font-size: 20px;
}
.like-button .material-icons-outlined.liked {
  animation: pop 0.3s ease-out; /* 좋아요 눌렀을 때 애니메이션 */
}
@keyframes pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}


/* 댓글 섹션 */
.comments-section {
  margin-top: 20px;
}

.comment-input-area {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.comment-input {
  flex-grow: 1;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 16px;
}
.comment-input:focus {
  outline: none;
  border-color: #808AFF;
}

.comment-submit-button {
  padding: 10px 20px;
  border: none;
  border-radius: 20px;
  background-color: #8E97FD;
  color: white;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s;
}
.comment-submit-button:hover {
  background-color: #7b85f8;
}

.comment-item {
  display: flex;
  flex-direction: column;
  background-color: #f9f9f9;
  border-radius: 10px;
  padding: 15px;
  margin-bottom: 15px;
  position: relative;
}

.comment-author-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.comment-profile-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}
.comment-author-name {
  font-weight: 700;
  font-size: 16px;
  color: #151516;
}
.comment-time {
  font-size: 13px;
  color: #A0A3B1;
  margin-left: 8px;
}
.comment-text {
  margin: 0;
  font-size: 16px;
  color: #3F414E;
  margin-left: 50px; /* 프로필 이미지와 맞춰서 들여쓰기 */
}

.comment-actions {
    position: absolute;
    right: 15px;
    top: 15px;
    display: flex;
    gap: 10px;
}
.comment-like-button, .comment-options-button {
    cursor: pointer;
    font-size: 18px;
    color: #888;
}
.comment-like-button:hover, .comment-options-button:hover {
    color: #555;
}
</style>
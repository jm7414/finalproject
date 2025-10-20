<template>
  <div v-if="loading" style="text-align: center; padding: 50px;">
    게시물 목록을 불러오는 중입니다...
  </div>

  <div v-else-if="error" style="text-align: center; padding: 50px; color: red;">
    {{ error }}
  </div>

  <div v-else class="main-container">
    <div class="main-body">
      <div class="main-div">

        <div class="main-header">
        </div>

        <div class="main-div-b">
          <div class="main-div-c">
            <div class="main-div-d">
              <div class="main-button-e">
                <span class="view-count">조회수</span>
                <div class="icon-div-f">
                  <div class="svg-div-10">
                    <div class="main-frame-11"></div>
                  </div>
                </div>
              </div>
              <div class="main-button-12">
                <span class="latest-sort">최신순</span>
                <div class="icon-div-13">
                  <div class="svg-div-14">
                    <div class="main-frame-15"></div>
                  </div>
                </div>
              </div>
            </div>
            <div class="main-button-16" @click="goToPostWrite" style="cursor: pointer;">
              <div class="icon-div-17">
                <div class="svg"><div class="frame"></div></div>
              </div>
            </div>
          </div>
        </div>

        <div class="main">
          <div v-if="posts.length === 0" style="text-align: center; padding: 50px;">
            <p>아직 게시글이 없습니다.</p>
          </div>
          
          <div v-for="post in posts" :key="post.postId" class="article" @click="goToPost(post.postId)" style="cursor: pointer;">
            <div class="div">
              <div class="img">
                 <img :src="post.authorProfileImage || defaultProfileImage" style="width: 100%; height: 100%; object-fit: cover;">
              </div>
              <div class="div-19">
                <div class="div-1a">
                  <div class="h"><span class="span">{{ post.author }}</span></div>
                  <div class="span-1b"></div>
                </div>
                <div class="p"><span class="span-1c">{{ formatTimeAgo(post.createdAt) }}</span></div>
              </div>
              <div class="button">
                <div class="i">
                  <div class="svg-1d"><div class="frame-1e"></div></div>
                </div>
              </div>
            </div>
            
            <h3 style="font-size: 16px; font-weight: bold; margin-bottom: 8px;">{{ post.title }}</h3>
            <div class="p-1f">
              <span>{{ post.content }}</span>
            </div>

            <div v-if="post.image" class="post">
              <img :src="post.image" alt="게시물 이미지" style="width: 100%; height: auto; display: block; border-radius: 11px;">
            </div>

            <div class="div-26">
              <div class="div-27">
                <div class="button-28">
                  <div class="i-29"><div class="svg-2a"><div class="like-btn">❤️</div></div></div>
                  <div class="span-2c"><span class="span-2d">{{ post.likes }}</span></div>
                </div>
                <div class="button-2e">
                  <div class="i-2f"><div class="svg-30"><div class="frame-31"></div></div></div>
                  <div class="span-32"><span class="span-33">{{ post.comments }}</span></div>
                </div>
                <div class="main-div-34">
                  <div class="italic-div"><div class="svg-div-35"><div class="frame-div"></div></div></div>
                  <div class="span-div"><span class="number-span">{{ post.views }}</span></div>
                </div>
              </div>
              <div class="button-div">
                <div class="icon-div-36"><div class="svg-div-37"><div class="frame-div-38"></div></div></div>
              </div>
            </div>
          </div>
          
          </div>

        <div class="line"></div>
      </div>
    </div>
<div class="community-view-container">
    <button class="create-post-fab main__C" @click="goToPostWrite">
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M13 21h8"/>
        <path d="M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z"/>
      </svg>
    </button>
    </div>
  </div>
  
</template>

<script setup>
// 두 번째 코드의 script 로직을 그대로 가져왔습니다.
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

import defaultProfileImage from '@/assets/default-profile.png';

const posts = ref([]);
const loading = ref(true);
const error = ref(null);
const router = useRouter();

onMounted(() => {
  fetchPosts();
});

async function fetchPosts() {
  loading.value = true;
  error.value = null;
  try {
    const response = await axios.get(`/api/posts`, {
      withCredentials: true // 여기에 입장권(쿠키)을 챙겨달라는 옵션 추가. 없으면 프론트 - 백 연결 불가. 시큐리티와 인덱스에서 롤 설정으로 막혀있는듯
    });
    posts.value = response.data;
  } catch (err) {
    console.error("게시물 목록을 불러오는 데 실패했습니다:", err);
    error.value = "데이터를 불러올 수 없습니다.";
  } finally {
    loading.value = false;
  }
}

function formatTimeAgo(dateString) {
  const now = new Date();
  const postDate = new Date(dateString);
  const seconds = Math.floor((now - postDate) / 1000);

  let interval = seconds / 31536000;
  if (interval > 1) return Math.floor(interval) + "년 전";
  interval = seconds / 2592000;
  if (interval > 1) return Math.floor(interval) + "달 전";
  interval = seconds / 86400;
  if (interval > 1) return Math.floor(interval) + "일 전";
  interval = seconds / 3600;
  if (interval > 1) return Math.floor(interval) + "시간 전";
  interval = seconds / 60;
  if (interval > 1) return Math.floor(interval) + "분 전";
  return "방금 전";
}

function goToPost(postId) {
  router.push(`/post/${postId}`);
}

function goToPostWrite() {
  router.push(`/CommunityPostWrite`);
}
</script>

<style scoped>

/* 첫 번째 코드의 스타일을 그대로 유지합니다. */
/* 기본 설정 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
/* ... (이하 모든 스타일은 첫 번째 코드와 동일) ... */
:root {
  --default-font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Ubuntu, "Helvetica Neue", Helvetica, Arial, "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei UI", "Microsoft Yahei", "Source Han Sans CN", sans-serif;
}
input, select, textarea, button {
  outline: 0;
}

.main-body {
  position: relative;
  width: 375px;
}
.main-div {
  width: 375px;
  background: #ffffff;
}

.main-div-1 {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.main-div-2 {
  display: flex;
  align-items: center;
  gap: 12px;
}
.icon-div, .svg-div {
  width: 24px;
  height: 24px;
}
.main-frame {
  width: 24px;
  height: 24px;
  background: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/sLcKWcAAzA.png) no-repeat center;
  background-size: cover;
}
.main-heading {
  width: 72px;
  height: 28px;
}
.dementia-care {
  color: #171717;
  font-family: Inter, var(--default-font-family);
  font-size: 18px;
  font-weight: 400;
  line-height: 21.784px;
}
.main-div-3 {
  display: flex;
  align-items: center;
  gap: 12px;
}
.icon-div-4, .svg-div-5 {
  width: 17.5px;
  height: 20px;
}
.main-frame-6 {
  width: 17.5px;
  height: 20px;
  background: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/EDwFOrbkmo.png) no-repeat center;
  background-size: cover;
}
.main-img {
  width: 32px;
  height: 32px;
}
.main-frame-7 {
  width: 32px;
  height: 32px;
  background: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/1BikGkoVwi.png) no-repeat center;
  background-size: cover;
  border-radius: 9999px;
}
.main-nav {
  width: 375px;
  padding: 0 16px;
  background: #ffffff;
  border-top: 1px solid #e5e5e5;
}
.main-div-8 {
  display: flex;
  height: 46px;
}
.main-button, .missing-button, .main-div-9 {
  flex: 1;
  position: relative;
  border-top: 2px solid transparent;
}
.main-button {
  border-bottom-color: #8e97fd;
}
.post-button, .event-button, .main-div-a {
  position: absolute;
  top: 11px;
  left: 50%;
  transform: translateX(-50%);
  color: #171717;
  font-family: Inter, var(--default-font-family);
  font-size: 14px;
  text-align: center;
  white-space: nowrap;
}
.event-button, .main-div-a {
  color: #737373;
}
.main-div-b {
  width: 465px;
  padding: 12px 16px;
  background: #fafafa;
  border-top: 1px solid #e5e5e5;
}
.main-div-c {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.main-div-d {
  display: flex;
  gap: 8px;
}
.main-button-e, .main-button-12 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 5px 12px;
  border-radius: 9999px;
  font-size: 14px;
  white-space: nowrap;
}
.main-button-e {
  background: #ffffff;
  border: 1px solid #d4d4d4;
  color: #404040;
}
.main-button-12 {
  background: #8e97fd;
  color: #ffffff;
}
.view-count, .latest-sort {
  font-family: Inter, var(--default-font-family);
  font-size: 14px;
}
.icon-div-f, .icon-div-13 {
  width: 13.5px;
  height: 12px;
}
.main-frame-11 {
  width: 13.5px;
  height: 12px;
  background: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/hA6SdnJ2ba.png) no-repeat center;
  background-size: cover;
}
.main-frame-15 {
  width: 12px;
  height: 12px;
  background: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/v0Ra1X9eGc.png) no-repeat center;
  background-size: cover;
}
.frame {
  width: 16px;
  height: 16px;
  background: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/kTG1m93k0k.png) no-repeat center;
  background-size: cover;
}
.article, .article-div, .article-52 {
  padding: 10px;
  width: 465px;
  background: #ffffff;
  border-top: 1px solid #f5f5f5;
}
.div, .content-div, .div-53 {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.img, .image-div, .img-54 {
  width: 40px;
  height: 40px;
  border-radius: 9999px;
  overflow: hidden;
}
.frame-18, .frame-div-39, .frame-55 {
  width: 40px;
  height: 40px;
  background-size: cover;
}
.frame-18 {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/3uNJTSRfPL.png);
}
.frame-div-39 {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/h3d8pS3Yzn.png);
}
.frame-55 {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/0Th7bmAkEs.png);
}
.div-19, .inner-div, .div-56 {
  flex: 1;
}
.span, .name-span, .text-18 {
  color: #171717;
  font-family: Inter, var(--default-font-family);
  font-size: 14px;
}
.span-1c, .time-span, .span-1-day-ago {
  color: #737373;
  font-family: Inter, var(--default-font-family);
  font-size: 12px;
}
.p-1f, .text-div, .div-5f {
  margin-bottom: 12px;
  color: #171717;
  font-family: Inter, var(--default-font-family);
  font-size: 14px;
  line-height: 1.5;
  /* 목록에서 내용이 너무 길 경우를 대비한 스타일 추가 */
  word-break: break-all;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3; /* 3줄까지만 보이도록 설정 */
  -webkit-box-orient: vertical;
}
.post {
  margin: 12px 0;
  border-radius: 11px;
  overflow: hidden;
}
.div-23 {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 192px;
  background: #d4d4d4;
  border-radius: 8px;
}
.span-25, .span-cooking-photo {
  color: #ffffff;
  font-family: Inter, var(--default-font-family);
  font-size: 14px;
}
.div-26, .inner-div-3d, .div-66 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}
.div-27, .inner-div-3e, .div-67 {
  display: flex;
  align-items: center;
  gap: 16px;
}
.button-28, .button-2e, .main-div-34,
.action-button-3f, .action-button-45, .div-49,
.button-68, .button-6d, .div-72 {
  display: flex;
  align-items: center;
  gap: 4px;
}
.span-2d, .span-33, .number-span,
.number-span-44, .span-15, .span-298 {
  color: #737373;
  font-family: Inter, var(--default-font-family);
  font-size: 12px;
  display: flex;
  align-items: center;  
}
.frame-2b, .frame-31, .frame-div,
.frame-div-42, .frame-48, .frame-4c {
  width: 12px;
  height: 12px;
  background-size: cover;
}
.like-btn {
display: flex;
  align-items: center;
  font-size: 0.8rem;
}
.frame-31 {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/fnd981fBj3.png);
}
.frame-div {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/cYs5PcthrV.png);
}
.frame-div-42 {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/kSrhmJXZN4.png);
}
.frame-48 {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/65B4hhZs4R.png);
}
.frame-4c {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/mgK9F0Z1kW.png);
}
.button-div, .button-4e, .button-77 {
  width: 12px;
  height: 12px;
}
.frame-div-38, .frame-51 {
  width: 9px;
  height: 12px;
  background-size: cover;
}
.frame-div-38 {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/ANRiangAmy.png);
}
.frame-51 {
  background-image: url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/8KrFw0kNGu.png);
}
.flex-row-fea {
  display: flex;
  gap: 8px;
  margin: 12px 0;
}
.kakaotalk {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
  background: #d4d4d4;
  border-radius: 8px;
  flex: 1;
}
.flex-row-e {
  display: flex;
  justify-content: space-around;
  padding: 12px 16px;
  background: #ffffff;
  border-top: 1px solid #e5e5e5;
}
.home, .schedule, .record, .emergency, .comprehensive-support {
  color: #737373;
  font-family: Inter, var(--default-font-family);
  font-size: 12px;
  text-align: center;
}
.line {
  width: 465px;
  height: 1px;
  background: #e5e5e5;
}
.rectangle-7b {
  width: 375px;
  height: 20px;
  background: #f5f5f5;
}

/* 게시물 작성 플로팅 버튼 스타일 */
.create-post-fab {
  position: fixed; /* 스크롤과 상관없이 화면에 고정 */
  bottom: 80px;    /* 푸터 위 (임시값, 푸터 높이에 따라 조절) */
  right: 20px;     /* 오른쪽에서 20px 떨어지게 */
  
  width: 56px;
  height: 56px;
  border-radius: 50%; /* 원형 버튼 */
  background-color: #8e97fd; /* 사진의 보라색 */
  box-shadow: 0px 4px 12px rgba(0, 0, 0, 0.15); /* 그림자 효과 */
  
  display: flex;
  justify-content: center;
  align-items: center;
  
  border: none;
  cursor: pointer;
  transition: background-color 0.2s ease-in-out;
  z-index: 1000; /* 다른 요소들 위에 보이도록 z-index 설정 */
}

.create-post-fab:hover {
  background-color: #7a82e0; /* 호버 시 약간 어두워지게 */
}
</style>
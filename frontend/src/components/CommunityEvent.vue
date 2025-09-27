<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

// 웹 화면용 내비게이션 아이템
const navItems = ['일정', '기록', '홈', '긴급', '종합지원'];

// 헤더가 보이는지 여부를 저장하는 변수. 기본값은 true (보임)
const isHeaderVisible = ref(true);
// 마지막 스크롤 위치를 저장하는 변수
const lastScrollY = ref(0);

// 스크롤 이벤트를 감지하는 함수
const handleScroll = () => {
  const currentScrollY = window.scrollY;

  // 페이지 최상단에서는 항상 헤더를 보여줌
  if (currentScrollY < 50) {
    isHeaderVisible.value = true;
    lastScrollY.value = currentScrollY;
    return;
  }
  
  // 스크롤을 내릴 때 (현재 위치 > 이전 위치) 헤더를 숨김
  if (currentScrollY > lastScrollY.value) {
    isHeaderVisible.value = false;
  } 
  // 스크롤을 올릴 때 (현재 위치 < 이전 위치) 헤더를 보여줌
  else {
    isHeaderVisible.value = true;
  }

  // 마지막 스크롤 위치를 현재 위치로 업데이트
  lastScrollY.value = currentScrollY;
};

// 컴포넌트가 화면에 나타나면 스크롤 이벤트를 감지하기 시작
onMounted(() => {
  window.addEventListener('scroll', handleScroll);
});

// 컴포넌트가 화면에서 사라지면 이벤트 감지를 중단
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <header class="app-header" :class="{ 'header-hidden': !isHeaderVisible }">
    <div class="icon-wrapper mobile-only">
      <svg class="icon" viewBox="0 0 24 24"><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"></path></svg>
    </div>

    <div class="app-title">치매케어</div>

    <nav class="header-desktop-nav">
      <a href="#" v-for="item in navItems" :key="item">{{ item }}</a>
    </nav>
    
    <div class="icon-wrapper">
      <svg class="icon" viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"></path></svg>
    </div>
  </header>
</template>

<style scoped>
/* --- 기본 (보이는 상태) 스타일 --- */
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 80px; /* 기본 헤더 높이 */
  padding: 0 24px;
  background-color: rgba(255, 255, 255, 0.85);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  
  /* 부드러운 전환 효과 (transform에 적용) */
  transition: transform 0.4s ease-in-out;
}

/* --- 헤더를 숨기는 스타일 --- */
.app-header.header-hidden {
  /* transform을 사용해 헤더를 자신의 높이(-100%)만큼 위로 밀어올림 */
  transform: translateY(-100%);
}

.app-title { font-size: 18px; font-weight: 700; color: #333; }
.icon-wrapper .icon { width: 24px; height: 24px; fill: #000000; }
.header-desktop-nav { display: none; }


/* --- 화면이 768px 이상일 때 (웹) 스타일 --- */
@media (min-width: 768px) {
  /* 웹, 보이는 상태 */
  .app-header {
    height: 90px;
    background-color: rgba(142, 151, 253, 0.95);
    box-shadow: 0 5px 15px rgba(45, 55, 158, 0.2);
    padding: 0 40px;
  }
  
  /* ... (나머지 웹 스타일은 이전과 동일) ... */
  .app-title { color: #FFFFFF; }
  .icon-wrapper.mobile-only { display: none; }
  .icon-wrapper .icon { fill: #FFFFFF; }
  .header-desktop-nav {
    display: flex; gap: 30px; position: absolute; left: 50%; transform: translateX(-50%);
  }
  .header-desktop-nav a { color: #FFFFFF; text-decoration: none; font-weight: 600; font-size: 16px; }
}
</style>
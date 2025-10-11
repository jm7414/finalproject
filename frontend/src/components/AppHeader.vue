<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';

// 웹 화면용 내비게이션 아이템
const navItems = ['일정', '기록', '홈', '긴급', '종합지원'];

// 스크롤 상태를 저장할 변수. 기본값은 false (스크롤 안 됨)
const isScrolled = ref(false);

const router = useRouter();
const route = useRoute();

// GD_main 페이지인지 확인하는 computed 속성
const isGDMainPage = computed(() => {
  return route.name === 'GD';
});

// 스크롤 위치를 감지해서 isScrolled 값을 변경하는 함수
const handleScroll = () => {
  // window.scrollY는 페이지 최상단으로부터 얼마나 스크롤됐는지를 나타냅니다.
  // 50px보다 더 많이 스크롤되면 isScrolled를 true로 설정합니다.
  isScrolled.value = window.scrollY > 50;
};

// 프로필 아이콘 클릭 시 마이페이지로 이동
const goToMyPage = () => {
  router.push('/gdmypage');
};

// 컴포넌트가 화면에 나타나면 스크롤 이벤트를 감지하기 시작합니다.
onMounted(() => {
  window.addEventListener('scroll', handleScroll);
});

// 컴포넌트가 화면에서 사라지면 이벤트 감지를 중단합니다. (메모리 절약)
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <header class="app-header" :class="{ scrolled: isScrolled, 'gd-main-header': isGDMainPage }">
    <div class="icon-wrapper mobile-only" :class="{ 'gd-main-icon': isGDMainPage }">
      <svg class="icon" viewBox="0 0 24 24"><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"></path></svg>
    </div>

    <div class="app-title" v-if="!isGDMainPage">치매케어</div>

    <nav class="header-desktop-nav">
      <a href="#" v-for="item in navItems" :key="item">{{ item }}</a>
    </nav>
    
    <div class="icon-wrapper" :class="{ 'gd-main-icon': isGDMainPage }" @click="goToMyPage">
      <svg class="icon" viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"></path></svg>
    </div>
  </header>
</template>

<style scoped>
/* --- 기본 (스크롤 전) 스타일 --- */
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 70px; /* 기본 헤더 높이 */
  padding: 0 24px;
  background-color: rgba(255, 255, 255, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;

  
  /* 부드러운 전환 효과 */
  transition: height 0.3s ease, background-color 0.3s ease, box-shadow 0.3s ease;
}

/* --- 스크롤 후 (.scrolled 클래스가 붙었을 때) 스타일 --- */
.app-header.scrolled {
  height: 80px; /* 헤더 높이 증가 */
  background-color: rgba(255, 255, 255, 0.85); /* 배경 더 진하게 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); /* 그림자 효과 추가 */
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.app-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.icon-wrapper {
  cursor: pointer;
}

.icon-wrapper .icon {
  width: 24px;
  height: 24px;
  fill: #000000;
}

.header-desktop-nav {
  display: none;
}

/* GD_main 페이지: 헤더 투명하게 */
.app-header.gd-main-header {
  background-color: transparent;
  box-shadow: none;
}

/* GD_main 페이지: 버튼들 아래로 이동 */
.icon-wrapper.gd-main-icon {
  margin-top: 50px;
}

/* --- 화면이 768px 이상일 때 (웹) 스타일 --- */
@media (min-width: 768px) {
  /* 웹, 스크롤 전 */
  .app-header {
    background-color: rgba(142, 151, 253, 0.8);
    height: 80px;
    padding: 0 40px;
  }

  /* 웹, 스크롤 후 */
  .app-header.scrolled {
    height: 90px; /* 웹 헤더 높이 증가 */
    background-color: rgba(142, 151, 253, 0.95); /* 배경 더 진하게 */
    box-shadow: 0 5px 15px rgba(45, 55, 158, 0.2); /* 그림자 효과 추가 */
  }

  /* ... (나머지 웹 스타일은 이전과 거의 동일) ... */
  .app-title { color: #FFFFFF; }
  .icon-wrapper.mobile-only { display: none; }
  .icon-wrapper .icon { fill: #FFFFFF; }
  .header-desktop-nav {
    display: flex;
    gap: 30px;
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
  }
  .header-desktop-nav a {
    color: #FFFFFF;
    text-decoration: none;
    font-weight: 600;
    font-size: 16px;
  }

  /* 웹 환경: GD_main 페이지에서도 버튼 아래로 */
  .icon-wrapper.gd-main-icon {
    margin-top: 60px;
  }
}
</style>
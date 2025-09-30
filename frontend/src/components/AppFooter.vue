<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

// ★★★ SVG 데이터에서 색상/필터 관련 속성을 모두 제거하고 모양(path)만 남겼습니다. ★★★
const navItems = ref([
  { name: '일정', path: '/schedule', isStroke: true, svg: `<path d="M20.25 9.33366H0.75M14.8333 0.666992V5.00033M6.16667 0.666992V5.00033M8.875 13.667L10.5 12.5837V18.0003M9.14583 18.0003H11.8542M5.95 22.3337H15.05C16.8702 22.3337 17.7803 22.3337 18.4755 21.9794C19.087 21.6678 19.5842 21.1707 19.8958 20.5591C20.25 19.8639 20.25 18.9538 20.25 17.1337V8.03366C20.25 6.21349 20.25 5.3034 19.8958 4.60819C19.5842 3.99666 19.087 3.49948 18.4755 3.18789C17.7803 2.83366 16.8702 2.83366 15.05 2.83366H5.95C4.12983 2.83366 3.21974 2.83366 2.52453 3.18789C1.913 3.49948 1.41582 3.99666 1.10423 4.60819C0.75 5.3034 0.75 6.21349 0.75 8.03366V17.1337C0.75 18.9538 0.75 19.8639 1.10423 20.5591C1.41582 21.1707 1.913 21.6678 2.52453 21.9794C3.21974 22.3337 4.12983 22.3337 5.95 22.3337Z" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />` },
  { name: '기록', path: '/history', svg: `<path d="M17.5505 9.44L13.613 5.69L4.1 14.75V18.5H8.0375L17.5505 9.44ZM6.2 16.5V15.58L13.613 8.52L14.579 9.44L7.166 16.5H6.2Z" /><path d="M20.5955 6.54C21.005 6.15 21.005 5.52 20.5955 5.13L18.1385 2.79C17.9285 2.59 17.666 2.5 17.393 2.5C17.1305 2.5 16.8575 2.6 16.658 2.79L14.7365 4.62L18.674 8.37L20.5955 6.54Z" /><path d="M23 20.5H2V24.5H23V20.5Z" />` },
  { name: '홈', path: '/', svg: `<path d="M24.5489 8.87066L15.3769 0.968066C14.0666 -0.313721 11.9334 -0.313721 10.6772 0.9151L1.39692 8.92362C0.498126 9.80287 0 10.9787 0 12.2287V22.8114C0 24.5699 1.46189 26 3.25948 26H22.7514C24.5381 26 26 24.5699 26 22.8114V12.2287C26 10.9787 25.5019 9.80287 24.5489 8.87066ZM23.8342 22.8114C23.8342 23.4046 23.3469 23.8813 22.7514 23.8813H15.8859V17.0593C15.8859 16.4449 15.3878 15.9576 14.7597 15.9576H11.2403C10.6231 15.9576 10.1141 16.4449 10.1141 17.0593V23.8813H3.25948C2.65306 23.8813 2.16576 23.4046 2.16576 22.8114V12.2287C2.16576 11.5614 2.44731 10.894 2.88047 10.4597L12.1608 2.46172C12.3882 2.23926 12.6805 2.12273 12.9838 2.12273C13.3086 2.12273 13.6335 2.24985 13.8934 2.50409L23.0654 10.4173C23.5635 10.9046 23.8342 11.5508 23.8342 12.2287V22.8114Z"/>` },
  { name: '커뮤니티', path: '/community', svg: `<path d="M6.04545 13.0731C7.13636 13.0944 8.14773 13.6044 8.82955 14.4969C9.65909 15.5913 11.0341 16.25 12.5 16.25C13.9659 16.25 15.3409 15.5912 16.1705 14.4862C16.8523 13.5938 17.8636 13.0837 18.9545 13.0625C18.1364 11.7662 14.8636 10.9375 12.5 10.9375C10.1477 10.9375 6.86364 11.7662 6.04545 13.0731Z" /><path d="M3.40909 13.0625C5.29545 13.0625 6.81818 11.6388 6.81818 9.875C6.81818 8.11125 5.29545 6.6875 3.40909 6.6875C1.52273 6.6875 0 8.11125 0 9.875C0 11.6388 1.52273 13.0625 3.40909 13.0625Z" /><path d="M21.5909 13.0625C23.4773 13.0625 25 11.6388 25 9.875C25 8.11125 23.4773 6.6875 21.5909 6.6875C19.7045 6.6875 18.1818 8.11125 18.1818 9.875C18.1818 11.6388 19.7045 13.0625 21.5909 13.0625Z" /><path d="M12.5 9.875C14.3864 9.875 15.9091 8.45125 15.9091 6.6875C15.9091 4.92375 14.3864 3.5 12.5 3.5C10.6136 3.5 9.09091 4.92375 9.09091 6.6875C9.09091 8.45125 10.6136 9.875 12.5 9.875Z" /><path d="M22.7273 14.125H19.0114C18.1364 14.125 17.4773 14.6031 17.1023 15.1025C17.0568 15.1662 15.5568 17.3125 12.5 17.3125C10.875 17.3125 9.05682 16.6325 7.89773 15.1025C7.45455 14.5181 6.76136 14.125 5.98864 14.125H2.27273C1.02273 14.125 0 15.0812 0 16.25V20.5H7.95455V18.0987C9.26136 18.9487 10.8409 19.4375 12.5 19.4375C14.1591 19.4375 15.7386 18.9487 17.0455 18.0987V20.5H25V16.25C25 15.0812 23.9773 14.125 22.7273 14.125Z" />` },
  { name: '종합지원', path: '/support', svg: `<path d="M22.7493 13.2383C22.7493 7.29083 18.1343 3.25 12.9993 3.25C7.91852 3.25 3.24935 7.20417 3.24935 13.3033C2.59935 13.6717 2.16602 14.365 2.16602 15.1667V17.3333C2.16602 18.525 3.14102 19.5 4.33268 19.5H5.41602V12.8917C5.41602 8.69917 8.80685 5.30833 12.9993 5.30833C17.1918 5.30833 20.5827 8.69917 20.5827 12.8917V20.5833H11.916V22.75H20.5827C21.7743 22.75 22.7493 21.775 22.7493 20.5833V19.2617C23.3885 18.9258 23.8327 18.265 23.8327 17.485V14.9933C23.8327 14.235 23.3885 13.5742 22.7493 13.2383Z" /><path d="M9.74935 15.1667C10.3477 15.1667 10.8327 14.6816 10.8327 14.0833C10.8327 13.485 10.3477 13 9.74935 13C9.15104 13 8.66602 13.485 8.66602 14.0833C8.66602 14.6816 9.15104 15.1667 9.74935 15.1667Z" /><path d="M16.2493 15.1667C16.8477 15.1667 17.3327 14.6816 17.3327 14.0833C17.3327 13.485 16.8477 13 16.2493 13C15.651 13 15.166 13.485 15.166 14.0833C15.166 14.6816 15.651 15.1667 16.2493 15.1667Z" /><path d="M19.4993 11.9492C18.9793 8.86167 16.2927 6.5 13.0535 6.5C9.77102 6.5 6.23935 9.21917 6.52102 13.4875C9.19685 12.3933 11.2118 10.01 11.786 7.10667C13.2052 9.95583 16.1193 11.9167 19.4993 11.9492Z" />` },
]);

const activeItem = ref('홈');
const router = useRouter();

function navigate(item) {
  activeItem.value = item.name;
  router.push(item.path);
}
</script>

<template>
  <footer class="app-footer">
    <nav class="nav-container">
      <!-- v-for 루프를 사용해 5개의 아이템을 모두 렌더링 -->
      <div
        v-for="item in navItems"
        :key="item.name"
        class="nav-item"
        @click="navigate(item)"
      >
        <!-- '홈' 아이템일 경우 특별한 튀어나온 구조 렌더링 -->
        <div v-if="item.name === '홈'" class="home-button-wrapper">
          <div class="home-button" :class="{ active: activeItem === '홈' }">
            <svg class="nav-icon" viewBox="0 0 26 26" v-html="item.svg"></svg>
          </div>
        </div>
        
        <!-- 나머지 아이템일 경우 일반 구조 렌더링 -->
        <div v-else class="icon-wrapper">
          <div class="icon-background" :class="{ active: activeItem === item.name }">
            <!-- ★★★ v-html과 viewBox를 이용해 SVG를 유연하게 렌더링 ★★★ -->
            <svg class="nav-icon" :class="{ active: activeItem === item.name }" viewBox="0 0 28 28" v-html="item.svg"></svg>
          </div>
        </div>
        
        <span class="nav-text" :class="{ active: activeItem === item.name }">{{ item.name }}</span>
      </div>
    </nav>
  </footer>
</template>

<style scoped>
.app-footer {
  width: 100%;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 95px; /* 푸터 전체 높이 증가 (튀어나온 부분 포함) */
  z-index: 1000;
}
.nav-container {
  display: flex;
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 85px; /* 하단 바 높이 증가 */
  background-color: #FFFFFF;
  box-shadow: 0 -2px 10px rgba(84, 87, 92, 0.1);
}
.nav-item {
  flex: 1; /* ★★★ 5개 아이템이 공간을 1:1:1:1:1로 나눠가짐 ★★★ */
  display: flex;
  flex-direction: column;
  justify-content: flex-start; /* 아이콘을 위쪽으로 정렬 */
  align-items: center;
  height: 100%;
  padding-top: 12px; /* 아이콘 위쪽 여백 */
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  position: relative;
}

/* --- 일반 아이콘 스타일 (홈 제외) --- */
.icon-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}
.icon-background {
  width: 52px; /* 아이콘 배경 크기 증가 */
  height: 52px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: background-color 0.3s ease;
}
.icon-background.active {
  background-color: #8E97FD;
}
.nav-icon {
  width: 30px; /* 아이콘 크기 증가 */
  height: 30px;
  fill: #A9ACB8;
  stroke: #A9ACB8; /* '일정' 아이콘용 */
  transition: all 0.3s ease;
}
.icon-background.active .nav-icon {
  fill: #FFFFFF;
  stroke: #FFFFFF;
}

/* --- 텍스트 스타일 --- */
.nav-text {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #A0A3B1;
  transition: all 0.3s ease;
  position: absolute; /* 모든 텍스트를 하단에 고정 */
  bottom: 10px;
}
.nav-text.active {
  color: #8E97FD;
  font-weight: 600;
}

/* --- 홈 버튼 스타일 (홈만 특별 취급) --- */
.home-button-wrapper {
  position: absolute;
  top: -25px; /* 위로 튀어나오도록 */
}
.home-button {
  width: 64px;
  height: 64px;
  border-radius: 22px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2ff;
  transition: all 0.3s ease;
}
.home-button.active {
  background: #8E97FD;
  box-shadow: 0 4px 10px rgba(142, 151, 253, 0.5);
}
.home-button .nav-icon {
  width: 32px; /* 홈 아이콘 크기 조정 */
  height: 32px;
  stroke: none; /* 홈 아이콘은 stroke 없음 */
}
.home-button.active .nav-icon {
  fill: #FFFFFF;
}

@media (min-width: 768px) {
  .app-footer {
    display: none;
  }
}
</style>
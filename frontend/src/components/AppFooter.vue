<script setup>
import { ref } from 'vue';

// script에서 nav 아이템들을 관리하여 v-for로 쉽게 렌더링
// 홈 버튼은 레이아웃 구조상 따로 배치합니다.
const leftItems = ref([
  { name: '일정', icon: '...' },
  { name: '기록', icon: '...' },
]);
const rightItems = ref([
  { name: '긴급', icon: '...' },
  { name: '종합지원', icon: '...' },
]);

// 현재 활성화된 메뉴를 저장하는 상태 변수. 기본값은 '홈'
const activeItem = ref('홈');

// 메뉴를 클릭했을 때 activeItem 값을 변경하는 함수
function setActive(itemName) {
  activeItem.value = itemName;
}
</script>

<template>
  <footer class="app-footer">
    <div 
      class="nav-item home-button-wrapper" 
      @click="setActive('홈')" 
      :class="{ active: activeItem === '홈' }"
    >
      <div class="home-button">
        <svg class="nav-icon" viewBox="0 0 24 24"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"></path></svg>
      </div>
      <span class="nav-text">홈</span>
    </div>

    <nav class="nav-container">
      <div 
        v-for="item in leftItems" 
        :key="item.name" 
        class="nav-item"
        @click="setActive(item.name)"
        :class="{ active: activeItem === item.name }"
      >
        <div class="nav-icon-placeholder"></div>
        <span class="nav-text">{{ item.name }}</span>
      </div>
      
      <div class="nav-spacer"></div>

      <div 
        v-for="item in rightItems" 
        :key="item.name" 
        class="nav-item"
        @click="setActive(item.name)"
        :class="{ active: activeItem === item.name }"
      >
        <div class="nav-icon-placeholder"></div>
        <span class="nav-text">{{ item.name }}</span>
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
  height: 70px; /* 네비게이션 바의 실제 높이 */
  z-index: 1000;
}

.nav-container {
  display: flex;
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 100%;
  background-color: #FFFFFF;
  box-shadow: 0 -2px 10px rgba(84, 87, 92, 0.1);
}

.nav-item {
  flex: 1; /* 각 아이템이 공간을 균등하게 나눠가짐 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 4px;
  height: 100%;
  cursor: pointer;
  transition: background-color 0.2s ease;
  -webkit-tap-highlight-color: transparent; /* 모바일 클릭 시 파란색 하이라이트 제거 */
}

/* 마우스를 올렸을 때의 스타일 */
.nav-item:hover {
  background-color: #f5f5f5;
}

.nav-icon-placeholder {
  width: 26px;
  height: 26px;
  border-radius: 4px;
  background-color: #A9ACB8; /* 비활성 아이콘 색 */
  transition: background-color 0.2s ease;
}

.nav-text {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  color: #A0A3B1; /* 비활성 텍스트 색 */
  transition: color 0.2s ease;
}

/* 활성화 상태 스타일 */
.nav-item.active .nav-icon-placeholder {
  background-color: #8E97FD; /* 활성 아이콘 색 */
}
.nav-item.active .nav-text {
  color: #8E97FD; /* 활성 텍스트 색 */
  font-weight: 600;
}


/* --- 홈 버튼 관련 스타일 --- */
.home-button-wrapper {
  width: 20%; /* 전체 너비의 1/5 차지 */
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  height: 100%;
  z-index: 1;
}
.home-button-wrapper .nav-text {
  position: absolute;
  bottom: 8px;
}
.home-button-wrapper:hover {
  background-color: transparent; /* 홈 버튼 배경은 hover 안되게 */
}


.home-button {
  width: 56px;
  height: 56px;
  background: #f0f2ff;
  border-radius: 18px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  bottom: 25px;
  left: 50%;
  transform: translateX(-50%);
  transition: all 0.2s ease;
}
.home-button-wrapper.active .home-button {
  background: #8E97FD;
  box-shadow: 0 4px 10px rgba(142, 151, 253, 0.5);
}

.home-button .nav-icon {
  width: 28px;
  height: 28px;
  fill: #A9ACB8; /* 비활성 홈 아이콘 색 */
  transition: fill 0.2s ease;
}

.home-button-wrapper.active .home-button .nav-icon {
  fill: #FFFFFF; /* 활성 홈 아이콘 색 */
}
.home-button-wrapper.active .nav-text {
  color: #8E97FD;
}

/* 홈 버튼이 차지하는 공간을 위한 스페이서 */
.nav-spacer {
  flex: 1; /* 홈 버튼과 동일한 너비를 차지 */
}

/* 화면이 768px 이상일 때 (웹) 푸터 전체를 숨김 */
@media (min-width: 768px) {
  .app-footer {
    display: none;
  }
}

</style>
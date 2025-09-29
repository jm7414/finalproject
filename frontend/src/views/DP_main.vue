<template>
  <div class="patient-main-container">
    <div class="background-decorations"></div>

    <div class="schedule-card">
      <div 
        v-for="item in scheduleItems" 
        :key="item.time" 
        :class="['schedule-item', { 'is-current': item.isCurrent, 'is-past': item.isPast }]"
      >
        <span class="time">{{ item.time }}</span> - <span class="task">{{ item.task }}</span>
      </div>
    </div>

    <div class="menu-grid">
      <button 
        v-for="item in menuItems" 
        :key="item.id" 
        :class="['menu-item', `menu-item-${item.id}`]"
        @click="handleMenuClick(item)"
      >
        <div v-if="item.icon" class="icon-wrapper">
          <img :src="item.icon" :alt="item.title" class="icon" />
        </div>
        <span class="menu-title">{{ item.title }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// 상단 스케줄 데이터
const scheduleItems = ref([
  { time: '8 : 30', task: '아침약 복용', isCurrent: false, isPast: true },
  { time: '12 : 00', task: '점심식사', isCurrent: true, isPast: false },
  { time: '14 : 00', task: '산책', isCurrent: false, isPast: false },
]);

// 메인 메뉴 데이터 (나중에 경로지정 path에서 하시면 됩니다.)
const menuItems = ref([
  { id: 'ai-chatbot', title: 'AI 챗봇', icon: null, path: '/chatbot' },
  { id: 'emergency-contact', title: '응급 연락망', icon: 'heart_icon_path.svg', path: '/emergency' },
  { id: 'daily-log', title: '오늘의 기록', icon: null, path: '/log' },
  { id: 'my-info', title: '내 정보', icon: null, path: '/my-info' },
]);

const handleMenuClick = (menuItem) => {
  console.log(`${menuItem.title} 버튼 클릭!`);

};
</script>

<style scoped>
/* 전체 컨테이너 스타일 */
.patient-main-container {
  position: relative;
  background-color: #F2F2F2;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 15px;
  box-sizing: border-box;
  
}

/* 배경 장식용 도형들 */
.background-decorations {
  position: absolute;
  width: 100%;
  height: 300px;
  top: 0;
  left: 0;
  background: linear-gradient(180deg, #FAF8F5 4.24%, rgba(255, 255, 255, 0) 72.25%);
  z-index: 0;
}

/* 상단 스케줄 카드 */
.schedule-card {
  position: relative;
  width: 100%;
  max-width: 399px;
  background: #F5F5F9;
  border-radius: 20px;
  padding: 29px 48px;
  margin-bottom: 40px;
  box-sizing: border-box;
  z-index: 1;
}

.schedule-item {
  font-size: 20px;
  color: #686868;
  line-height: 1.5;
  margin-bottom: 10px;
}

.schedule-item:last-child { margin-bottom: 0; }
.schedule-item.is-past { color: #C0C0C0; }
.schedule-item.is-current {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 34px;
  color: #000000;
}

/* 메인 메뉴 그리드 */
.menu-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: auto auto;
  gap: 20px;
  z-index: 1;
}

.menu-item {
  position: relative;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  box-sizing: border-box;
  min-height: 230px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  color: #FEF9F3;
  overflow: hidden;
  cursor: pointer;
  border: none;
  text-align: left; /* button 태그의 기본 중앙 정렬을 초기화 */
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

/* [추가] 마우스 올렸을 때(hover) 입체감 효과 */
.menu-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

/* [추가] 클릭했을 때(active) 눌리는 효과 */
.menu-item:active {
  transform: translateY(-2px) scale(0.98);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
}

.menu-title { z-index: 2; }

/* 각 메뉴 아이템의 배경색과 텍스트 색상 */
.menu-item-ai-chatbot {
  background-color: #8E97FD;
  grid-row: 1 / 3;
  min-height: 300px;
  color: #FFECCC;
}
.menu-item-emergency-contact { background-color: #FA6E5A; }
.menu-item-daily-log {
  background-color: #FEB18F;
  color: #3F414E;
}
.menu-item-my-info {
  background-color: #FFCF86;
  grid-row: 2 / 4;
  min-height: 270px; /* 높이 조절 */
  color: #3F414E;
}
</style>
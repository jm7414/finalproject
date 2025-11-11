<script setup>
import { ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';


// 이웃용 푸터: 일정, 홈, 공지 3개 탭
const navItems = ref([
  { name: '일정', path: '/nhCalender', svg: `<path d="M20 5H19V3H17V5H9V3H7V5H6C4.9 5 4 5.9 4 7L4 21C4 22.1 4.9 23 6 23H20C21.1 23 22 22.1 22 21V7C22 5.9 21.1 5 20 5ZM20 21H6V11H20V21ZM20 9H6V7H20V9ZM10 15H8V13H10V15ZM14 15H12V13H14V15ZM18 15H16V13H18V15ZM10 19H8V17H10V19ZM14 19H12V17H14V19ZM18 19H16V17H18V19Z" />` },
  { name: '홈', path: '/NH', svg: `<path d="M24.5489 8.87066L15.3769 0.968066C14.0666 -0.313721 11.9334 -0.313721 10.6772 0.9151L1.39692 8.92362C0.498126 9.80287 0 10.9787 0 12.2287V22.8114C0 24.5699 1.46189 26 3.25948 26H22.7514C24.5381 26 26 24.5699 26 22.8114V12.2287C26 10.9787 25.5019 9.80287 24.5489 8.87066ZM23.8342 22.8114C23.8342 23.4046 23.3469 23.8813 22.7514 23.8813H15.8859V17.0593C15.8859 16.4449 15.3878 15.9576 14.7597 15.9576H11.2403C10.6231 15.9576 10.1141 16.4449 10.1141 17.0593V23.8813H3.25948C2.65306 23.8813 2.16576 23.4046 2.16576 22.8114V12.2287C2.16576 11.5614 2.44731 10.894 2.88047 10.4597L12.1608 2.46172C12.3882 2.23926 12.6805 2.12273 12.9838 2.12273C13.3086 2.12273 13.6335 2.24985 13.8934 2.50409L23.0654 10.4173C23.5635 10.9046 23.8342 11.5508 23.8342 12.2287V22.8114Z"/>` },
  { name: '공지', path: '/nhNotice', svg: `<path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM13 17H11V15H13V17ZM13 13H11V7H13V13Z" />` },
]);


const router = useRouter();
const route = useRoute();


// 현재 경로에 따라 활성 아이템 결정
const activeItem = computed(() => {
  const currentPath = route.path;
  if (currentPath === '/NH') {
    return '홈';
  } else if (currentPath === '/nhCalender') {
    return '일정';
  } else if (currentPath === '/nhNotice') {
    return '공지';
  }
  return null;
});


function navigate(item) {
  // path가 null이면 동작하지 않음
  if (item.path) {
    router.push(item.path);
  }
  // path가 null이면 아무 동작도 하지 않음
}
</script>


<template>
  <footer class="neighbor-footer">
    <nav class="nav-container">
      <div
        v-for="item in navItems"
        :key="item.name"
        class="nav-item"
        @click="navigate(item)"
      >
        <div class="icon-wrapper">
          <div class="icon-background" :class="{ active: activeItem === item.name }">
            <svg class="nav-icon" :class="{ active: activeItem === item.name }" viewBox="0 0 28 28" v-html="item.svg"></svg>
          </div>
        </div>
        
        <span class="nav-text" :class="{ active: activeItem === item.name }">{{ item.name }}</span>
      </div>
    </nav>
  </footer>
</template>


<style scoped>
.neighbor-footer {
  width: 100%;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 70px;
  z-index: 1000;
}


.nav-container {
  display: flex;
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 70px;
  background-color: #FFFFFF;
  box-shadow: 0 -2px 10px rgba(84, 87, 92, 0.1);
}


.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 8px 0 12px 0;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  position: relative;
}


.icon-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}


.icon-background {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: background-color 0.3s ease;
}


.icon-background.active {
  background-color: #a7cc10;
}


.nav-icon {
  width: 24px;
  height: 24px;
  fill: #A9ACB8;
  transition: all 0.3s ease;
}


.icon-background.active .nav-icon {
  fill: #FFFFFF;
}


.nav-text {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #A0A3B1;
  transition: all 0.3s ease;
  margin-top: 4px;
}


.nav-text.active {
  color: #a7cc10;
  font-weight: 600;
}
</style>

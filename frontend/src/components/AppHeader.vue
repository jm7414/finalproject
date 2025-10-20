<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

// 뒤로가기
function goBack() {
  router.go(-1)
}

// 프로필 아이콘 클릭 시 마이페이지로 이동
const goToMyPage = () => {
  router.push('/gdmypage');
};
</script>

<template>
  <header class="app-header">
    <div class="icon-wrapper" @click="goBack">
      <i class="icon bi bi-arrow-left"></i>
    </div>

    <div class="app-title">치매케어</div>
    
    <div class="icon-wrapper" @click="goToMyPage">
      <svg class="icon" viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"></path></svg>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 70px;
  padding: 0 24px;
  background-color: #FFFFFF;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
</style>
<script setup>
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
</script>

<template>
  <div class="app-layout">
    <AppHeader />
    <main class="main-content">
      <RouterView /> </main>
    <AppFooter />
  </div>
</template>

<style>
/* 앱 전체에 적용될 스타일 */
.app-layout {
  padding-top: 60px; /* 헤더 높이만큼 안쪽으로 밀어넣기 */
  padding-bottom: 90px; /* 푸터 높이만큼 안쪽으로 밀어넣기 */
}
.main-content {
  /* 페이지 내용이 들어갈 공간 스타일 */
}
</style>
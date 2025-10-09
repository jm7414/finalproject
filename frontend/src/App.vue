<template>
  <div class="app-layout" :class="{ 'gd-main-layout': isGDMainPage, 'dp-main-layout': isDPMainPage }">
    <AppHeader v-if="!shouldHideHeader" />
    <main class="main-content" :class="{ 'gd-main-page': isGDMainPage, 'dp-main-page': isDPMainPage }">
      <RouterView /> </main>
    <AppFooter v-if="!isGDMainPage && !isDPMainPage" />
    </div>
</template>

<script setup>
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { computed } from 'vue'

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-3/dist/bootstrap-vue-3.css'

const route = useRoute()

// GD_main 페이지인지 확인하는 computed 속성
const isGDMainPage = computed(() => {
  return route.name === 'GD'
})

// DP_main 페이지인지 확인하는 computed 속성
const isDPMainPage = computed(() => {
  return route.name === 'DP'
})

// 헤더를 숨겨야 하는 페이지들 확인
const shouldHideHeader = computed(() => {
  return route.name === 'add-schedule' || route.name === 'DP'
})
</script>

<style>
/* GD_main 페이지일 때 #app의 padding 제거 (전역 스타일 덮어쓰기) */
.app-layout.gd-main-layout {
  margin-left: -2rem;
  margin-right: -2rem;
  width: calc(100% + 4rem);
}

/* DP_main 페이지일 때 #app의 padding 제거 (전역 스타일 덮어쓰기) */
.app-layout.dp-main-layout {
  margin-left: -2rem;
  margin-right: -2rem;
  width: calc(100% + 4rem);
}

@media (min-width: 1024px) {
  .app-layout.gd-main-layout {
    margin-left: -2rem;
    margin-right: -2rem;
    width: calc(100% + 4rem);
  }
  
  .app-layout.dp-main-layout {
    margin-left: -2rem;
    margin-right: -2rem;
    width: calc(100% + 4rem);
  }
}
</style>

<style scoped>
.main-content{
  padding-top: 30px;
  padding-bottom: 100px;
}

/* GD_main 페이지에서는 padding 제거 */
.main-content.gd-main-page {
  padding: 0;
  overflow-x: hidden;
}

/* DP_main 페이지에서는 padding 제거 */
.main-content.dp-main-page {
  padding: 0;
  overflow-x: hidden;
}
</style>
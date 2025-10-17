<template>
  <div class="mobile-frame">
    <div class="app-layout">
      <AppHeader v-if="!(isAddSchedulePage || isDPMainPage || isGDMainPage || isMapMainPage || isLoginPage || isSignUpPage)" />
      <main class="main-content" :class="{ 'no-padding': isGDMainPage || isDPMainPage || isMapMainPage || isLoginPage || isSignUpPage }">
        <RouterView />
      </main>
      <AppFooter v-if="!(isDPMainPage || isGDMainPage || isLoginPage || isSignUpPage)" />
    </div>
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

// AddSchedule 페이지인지 확인하는 computed 속성
const isAddSchedulePage = computed(() => {
  return route.name === 'add-schedule'
})

// GD_main 페이지인지 확인하는 computed 속성
const isGDMainPage = computed(() => {
  return route.name === 'GD'
})

// DP_main 페이지인지 확인하는 computed 속성
const isDPMainPage = computed(() => {
  return route.name === 'DP'
})

// MapMain 페이지인지 확인하는 computed 속성
const isMapMainPage = computed(() => {
  return route.name === 'map-main'
})

// Login 페이지인지 확인하는 computed 속성
const isLoginPage = computed(() => {
  return route.name === 'login'
})

// SignUp 페이지인지 확인하는 computed 속성
const isSignUpPage = computed(() => {
  return route.name === 'SignUp'
})
</script>

<style>
/* 모바일 프레임 외부 (회색 배경) */
body {
  margin: 0;
  padding: 0;
  background-color: #808080;
  overflow: hidden;
}

/* 375px × 812px 고정 프레임 */
.mobile-frame {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 375px;
  height: 812px;
  background-color: #FFFFFF;
  overflow: hidden;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.3);
}

.app-layout {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}
</style>

<style scoped>
.main-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 90px 5px 80px 5px;
}

/* padding 제거가 필요한 페이지 */
.main-content.no-padding {
  padding: 0;
  overflow: hidden;
}
</style>
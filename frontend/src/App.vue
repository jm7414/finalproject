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


<script setup>
import { RouterLink, RouterView } from 'vue-router'
import HelloWorld from './components/HelloWorld.vue'

import { createApp } from 'vue'
import App from './App.vue'

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-3/dist/bootstrap-vue-3.css'
import { useRouter } from 'vue-router'

import { ref } from 'vue'

const router = useRouter()
const drawer = ref(false)
const tab = ref('home')

// 하단바와 드로어가 같은 소스
const items = [
  { value: 'map',      label: '지도',   icon: 'mdi-map' },
  { value: 'report',   label: '리포트', icon: 'mdi-file-chart-outline' },
  { value: 'home',     label: '홈',     icon: 'mdi-home' },
  { value: 'schedule', label: '일정',   icon: 'mdi-calendar-month' },
  { value: 'support',  label: '종합지원', icon: 'mdi-lifebuoy' },
]

// 드로어에서 항목 클릭하면 하단 탭도 같이 변경 + 드로어 닫기
function select(val) {
  tab.value = val
  drawer.value = false
  switch (val) {
    case 'home':    router.push({ name: 'GD' }); break
    case 'report':  router.push({ name: 'DP' }); break
    case 'map':     router.push({ name: 'map' }); break
    case 'schedule':router.push({ name: 'schedule' }); break
    case 'support': router.push({ name: 'support' }); break
  }
}
>>>>>>>>> Temporary merge branch 2
</script>

<style scoped>

</style>
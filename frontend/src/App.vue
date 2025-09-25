<template>
  <v-app>
    <v-app-bar flat color="surface">
      <v-app-bar-nav-icon @click="drawer = !drawer" />

      <v-toolbar-title>침침해</v-toolbar-title>

      <v-spacer />

      <!-- 마페 -->
      <v-btn variant="text" icon aria-label="마이페이지">
        <span>👤</span>
      </v-btn>
    </v-app-bar>

    <!-- 사이드바 드로어 -->
    <v-navigation-drawer
      v-model="drawer"
      temporary
      location="start"
    >
      <v-list nav>
        <v-list-subheader>메뉴</v-list-subheader>

        <v-list-item
          v-for="it in items"
          :key="it.value"
          :value="it.value"
          @click="select(it.value)"
        >
          <template #prepend>
            <v-icon :icon="it.icon" />
          </template>
          <v-list-item-title>{{ it.label }}</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>

    <!-- 본문 -->
    <v-main>
      <!-- 예시 컨텐츠 -->
      <div>
        <RouterView />
      </div>

      <!-- 하단 바 (5개 탭) -->
      <v-bottom-navigation
        v-model="tab"
        mode="shift"
        elevation="8"
        height="64"
        style="position: fixed; left: 0; right: 0; bottom: 0;"
      >
        <v-btn value="map">
          <v-icon>mdi-map</v-icon>
          <span>지도</span>
        </v-btn>

        <v-btn value="report">
          <v-icon>mdi-file-chart-outline</v-icon>
          <span>리포트</span>
        </v-btn>

        <v-btn value="home">
          <v-icon>mdi-home</v-icon>
          <span>홈</span>
        </v-btn>

        <v-btn value="schedule">
          <v-icon>mdi-calendar-month</v-icon>
          <span>일정</span>
        </v-btn>

        <v-btn value="support">
          <v-icon>mdi-lifebuoy</v-icon>
          <span>종합지원</span>
        </v-btn>
      </v-bottom-navigation>
    </v-main>
  </v-app>
</template>


<script setup>
import { RouterLink, RouterView } from 'vue-router'
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
</script>

<style scoped>

</style>

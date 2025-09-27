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
import HelloWorld from './components/HelloWorld.vue'

import { createApp } from 'vue'
import App from './App.vue'

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-3/dist/bootstrap-vue-3.css'
</script>

<style scoped>

</style>

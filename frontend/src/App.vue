<template>
  <div class="app-layout">
    <AppHeader />
    <main class="main-content">
      <RouterView /> </main>
    <AppFooter />
    </div>
</template>

<script setup>
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
import { RouterLink, RouterView } from 'vue-router'

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-3/dist/bootstrap-vue-3.css'
</script>

<style scoped>
.main-content{
  padding-top: 30px;
  padding-bottom: 100px;
}
</style>
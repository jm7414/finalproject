<template>
  <header class="app-header">
    <div class="icon-wrapper" @click="handleBack">
      <i class="icon bi bi-arrow-left icon-bold"></i>
    </div>

    <div class="app-title">
      <img src="/mammamialogo.png" alt="Mamma Mia Logo" class="logo-image">
    </div>

    <div class="icon-wrapper" @click="goHome">
      <i class="icon bi bi-house icon-bold"></i>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  showBackConfirm: {
    type: Boolean,
    default: false
  },
  backConfirmMessage: {
    type: String,
    default: '이전 페이지로 돌아가시겠습니까?'
  },
  homeRoute: {
    type: String,
    default: '/NH'
  }
})

const emit = defineEmits(['back-click'])

const router = useRouter()

function handleBack() {
  if (props.showBackConfirm) {
    emit('back-click')
  } else {
    router.go(-1)
  }
}

function goHome() {
  router.push(props.homeRoute)
}
</script>

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
  padding: 8px;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.icon-wrapper:hover {
  background-color: #f5f5f5;
}

.icon-wrapper .icon {
  width: 24px;
  height: 24px;
  fill: #000000;
}

.icon-bold {
  font-size: 1.3rem;
  -webkit-text-stroke: 0.8px currentColor;
}

.logo-image {
  height: 32px;
  object-fit: contain;
}
</style>

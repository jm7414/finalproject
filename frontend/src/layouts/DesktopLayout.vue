<template>
  <div class="desktop-layout">
    <header class="desktop-header">
      <div class="brand">
        <span class="brand-name">맘마미아 케어</span>
        <span class="brand-tagline">보호자 전용 대시보드</span>
      </div>
      <div v-if="showHeaderActions" class="header-actions">
        <button type="button" class="ghost-btn">알림</button>
        <button
          type="button"
          class="primary-btn"
          :disabled="isProcessing"
          @click="handleLogout"
        >
          {{ isProcessing ? '로그아웃 중...' : '로그아웃' }}
        </button>
      </div>
    </header>

    <main class="desktop-main">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logout } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const isProcessing = ref(false)

const showHeaderActions = computed(() => route.meta.requiresAuth)

async function handleLogout() {
  if (isProcessing.value) return
  isProcessing.value = true

  try {
    const success = await logout()
    if (success) {
      await router.push('/desktop/login')
    } else {
      alert('로그아웃에 실패했습니다. 잠시 후 다시 시도해주세요.')
    }
  } catch (error) {
    console.error('데스크탑 레이아웃 로그아웃 오류:', error)
    alert('로그아웃 처리 중 오류가 발생했습니다.')
  } finally {
    isProcessing.value = false
  }
}
</script>

<style scoped>
.desktop-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
}

.desktop-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.brand {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.brand-tagline {
  font-size: 12px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ghost-btn,
.primary-btn {
  height: 32px;
  padding: 0 16px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid transparent;
  cursor: pointer;
  background: transparent;
  color: #374151;
}

.ghost-btn:hover {
  background: rgba(55, 65, 81, 0.08);
}

.primary-btn {
  background: #6366f1;
  color: #ffffff;
  border-color: #6366f1;
}

.primary-btn:hover {
  filter: brightness(0.95);
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: default;
  filter: none;
}

.desktop-main {
  flex: 1;
  padding: 24px 32px;
  display: flex;
  flex-direction: column;
}
</style>


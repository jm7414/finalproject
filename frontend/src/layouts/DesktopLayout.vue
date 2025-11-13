<template>
  <div class="desktop-layout">
    <header class="desktop-header">
      <div class="brand">
        <span class="brand-name">맘마미아</span>
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

    <div class="desktop-body" :class="{ 'no-sidebar': !showSidebar }">
      <aside v-if="showSidebar" class="sidebar">
        <div class="sidebar-header">
          <div class="avatar">👤</div>
          <div class="caretaker">
            <span class="label">보호자</span>
            <span class="name">{{ guardianName }}님</span>
          </div>
        </div>

        <nav class="menu">
          <button
            v-for="(item, idx) in menuItems"
            :key="idx"
            type="button"
            class="menu-item"
            :class="{ active: activeMenu === item.route }"
            @click="navigateToMenu(item.route)"
          >
            <span>{{ item.name }}</span>
          </button>
        </nav>

        <div class="sidebar-footer">
          <p class="support-text">궁금한 점이 있으신가요?</p>
          <button type="button" class="support-btn">고객센터 연결</button>
        </div>
      </aside>

      <main class="desktop-main">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentUser, logout } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const isProcessing = ref(false)
const guardianName = ref('보호자')

const menuItems = [
  { name: '안심존', route: '/desktop/main' },
  { name: '예상 위치', route: '/desktop/predict' },
  { name: 'AI보고서', route: null },
  { name: '일정', route: '/desktop/schedule' },
  { name: '커뮤니티', route: null },
  { name: '마이페이지', route: null }
]

const activeMenu = computed(() => {
  const currentPath = route.path
  const matched = menuItems.find(item => item.route && currentPath.startsWith(item.route))
  return matched?.route ?? ''
})

const showHeaderActions = computed(() => route.meta.requiresAuth)
const showSidebar = computed(() => route.meta.requiresAuth)

function navigateToMenu(targetRoute) {
  if (!targetRoute || targetRoute === route.path) return
  router.push(targetRoute)
}

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

onMounted(async () => {
  try {
    const user = await getCurrentUser()
    if (user?.name) {
      guardianName.value = user.name
    }
  } catch (error) {
    console.error('보호자 정보 조회 오류:', error)
  }
})
</script>

<style scoped>
.desktop-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
}

.desktop-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
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
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.brand-tagline {
  font-size: 11px;
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

.desktop-body {
  display: flex;
  flex: 1;
  padding: 16px 20px;
  gap: 16px;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.desktop-body.no-sidebar {
  justify-content: center;
  padding: 32px 20px;
}

.desktop-body.no-sidebar .desktop-main {
  max-width: 560px;
  width: 100%;
}

.sidebar {
  width: 280px;
  background: #111827;
  color: #f9fafb;
  display: flex;
  flex-direction: column;
  padding: 16px 14px;
  border-radius: 12px;
  margin-right: 16px;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #1f2937;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.caretaker {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.caretaker .label {
  font-size: 12px;
  color: #9ca3af;
}

.caretaker .name {
  font-weight: 700;
  font-size: 15px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: auto;
}

.menu-item {
  width: 100%;
  height: 36px;
  border-radius: 8px;
  border: 0;
  background: rgba(255, 255, 255, 0.06);
  color: inherit;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  padding: 0 12px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(3px);
}

.menu-item.active {
  background: rgba(99, 102, 241, 0.2);
  color: #ffffff;
  font-weight: 700;
  border-left: 3px solid #6366f1;
}

.sidebar-footer {
  margin-top: 16px;
  padding: 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  text-align: center;
}

.support-text {
  font-size: 11px;
  margin-bottom: 8px;
}

.support-btn {
  width: 100%;
  height: 32px;
  border-radius: 8px;
  border: 0;
  background: #f59e0b;
  color: #111827;
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
}

.desktop-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

@media (max-width: 1440px) {
  .sidebar {
    width: 260px;
  }
}
</style>


<template>
  <div class="page">
    <div class="header">
      <button @click="goBack" class="btn-back">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h1>내 만남의 광장</h1>
    </div>

    <!-- 로딩 중 -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>광장 정보를 불러오는 중...</p>
    </div>

    <!-- 광장 없음 -->
    <div v-else-if="!myPlaza" class="empty-state-full">
      <div class="empty-icon">
        <i class="bi bi-geo-alt"></i>
      </div>
      <h3>속한 광장이 없습니다</h3>
      <p>새로운 광장을 만들거나 친구의 초대를 기다려보세요!</p>
      <button class="btn-create-large" @click="goToCreatePlaza">
        <i class="bi bi-plus-circle"></i>
        광장 만들기
      </button>
    </div>

    <!-- 광장 있음 (1개만 표시) -->
    <div v-else class="plaza-container">
      <div class="section">
        <h2 class="section-title">
          <i class="bi bi-star-fill" v-if="myPlaza.isOwner" style="color:#fbbf24"></i>
          <i class="bi bi-people-fill" v-else style="color:#a7cc10"></i>
          {{ myPlaza.isOwner ? '내가 만든 광장' : '내가 속한 광장' }}
        </h2>

        <!-- 광장 카드 -->
        <div 
          class="plaza-card" 
          :class="{ owner: myPlaza.isOwner }"
          @click="goToPlaza(myPlaza.plazaNo)">
          <div class="plaza-icon" :class="{ 'owner-icon': myPlaza.isOwner }">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" 
                :fill="myPlaza.isOwner ? '#fbbf24' : '#a7cc10'" 
                stroke="#fff" 
                stroke-width="2"/>
              <circle cx="12" cy="10" r="3" fill="#fff"/>
            </svg>
          </div>
          <div class="plaza-info">
            <div class="plaza-name">{{ myPlaza.plazaName }}</div>
            <div class="plaza-meta">
              <span class="plaza-badge" :class="{ owner: myPlaza.isOwner }">
                {{ myPlaza.memberName }}
              </span>
              <span class="plaza-members">멤버 {{ myPlaza.memberCount }}명</span>
            </div>
          </div>
          <i class="bi bi-chevron-right"></i>
        </div>
      </div>

      <!-- 안내 메시지 -->
      <div class="info-message">
        <i class="bi bi-info-circle"></i>
        <span>광장은 1개만 가입할 수 있습니다. 다른 광장에 가입하려면 먼저 현재 광장에서 탈퇴해야 합니다.</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const loading = ref(true)
const myPlaza = ref(null)

onMounted(async () => {
  await loadMyPlaza()
})

// 내가 속한 광장 조회 (1개)
async function loadMyPlaza() {
  loading.value = true
  try {
    const response = await axios.get('/NH/api/neighbor/plazas/my')
    
    // 응답 데이터 확인
    if (response.data && response.data.plazaNo) {
      myPlaza.value = response.data
    } else {
      myPlaza.value = null
    }
  } catch (error) {
    console.error('광장 조회 실패:', error)
    myPlaza.value = null
  } finally {
    loading.value = false
  }
}

function goToPlaza(plazaNo) {
  if (plazaNo) {
    router.push(`/plazaDetail/${plazaNo}`)
  }
}

function goToCreatePlaza() {
  router.push('/createPlaza')
}

function goBack() {
  router.back()
}
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/gh/sunn-us/SUIT/fonts/static/woff2/SUIT.css');

* {
  font-family: 'SUIT', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.page {
  min-height: auto;
  max-height: 100vh;
  background: #f9fafb;
  position: relative;
  z-index: 1;
  margin-top: -20px;
  overflow-y: auto;
}

/* 헤더 */
.header {
  background: linear-gradient(135deg, #a7cc10 0%, #8fb80e 100%);
  color: white;
  padding: 20px;
  position: relative;
  text-align: center;
}

.btn-back {
  position: absolute;
  left: 15px;
  top: 15px;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);
}

.btn-back:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.8);
}

.header h1 {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

/* 로딩 */
.loading-container {
  text-align: center;
  padding: 100px 20px;
  color: #6b7280;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #c2d477;
  border-top-color: #a7cc10;
  border-radius: 50%;
  margin: 0 auto 15px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 광장 없음 (전체 화면) */
.empty-state-full {
  text-align: center;
  padding: 80px 20px;
  color: #6b7280;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: #f5f9e8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon i {
  font-size: 40px;
  color: #a7cc10;
}

.empty-state-full h3 {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 10px 0;
}

.empty-state-full p {
  font-size: 15px;
  color: #6b7280;
  margin: 0 0 30px 0;
}

.btn-create-large {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  background: #a7cc10;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(167, 204, 16, 0.3);
}

.btn-create-large:hover {
  background: #8fb80e;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(167, 204, 16, 0.4);
}

/* 광장 컨테이너 */
.plaza-container {
  padding: 20px;
}

.section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 17px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 광장 카드 */
.plaza-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.plaza-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.plaza-card.owner {
  border-color: #fbbf24;
  background: linear-gradient(135deg, #fffbeb 0%, #ffffff 100%);
}

.plaza-icon {
  width: 52px;
  height: 52px;
  background: #f5f9e8;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.plaza-icon.owner-icon {
  background: #fef3c7;
}

.plaza-info {
  flex: 1;
}

.plaza-name {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 6px;
}

.plaza-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #6b7280;
}

.plaza-badge {
  display: inline-block;
  padding: 3px 10px;
  background: #a7cc10;
  color: white;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.plaza-badge.owner {
  background: #fbbf24;
}

.plaza-card i.bi-chevron-right {
  color: #9ca3af;
  font-size: 18px;
}

/* 안내 메시지 */
.info-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 10px;
  font-size: 13px;
  color: #0369a1;
  line-height: 1.5;
}

.info-message i {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 2px;
}
</style>

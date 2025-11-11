<template>
  <div class="page">
    <div class="header">
      <button @click="goBack" class="btn-back">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h1>내 만남의 광장</h1>
    </div>

    <!-- 내가 방장인 광장 -->
    <div class="section">
      <h2 class="section-title">
        <i class="bi bi-star-fill" style="color:#fbbf24"></i>
        내가 만든 광장
      </h2>

      <div v-if="loadingOwned" class="loading-state">
        <div class="spinner"></div>
        <p>광장 조회 중...</p>
      </div>

      <div v-else-if="!ownedPlaza" class="empty-state">
        <i class="bi bi-map" style="font-size:3rem; color:#c2d477"></i>
        <p>아직 만든 광장이 없습니다</p>
        <button @click="goToCreatePlaza" class="btn-create">
          <i class="bi bi-plus-circle"></i>
          광장 만들기
        </button>
      </div>

      <div v-else class="plaza-card owner" @click="goToPlaza(ownedPlaza.plazaNo)">
        <div class="plaza-icon owner-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" fill="#fbbf24" stroke="#fff" stroke-width="2"/>
            <circle cx="12" cy="10" r="3" fill="#fff"/>
          </svg>
        </div>
        <div class="plaza-info">
          <div class="plaza-name">{{ ownedPlaza.plazaName }}</div>
          <div class="plaza-meta">
            <span class="plaza-badge owner">방장</span>
            <span class="plaza-members">멤버 {{ ownedPlaza.memberCount }}명</span>
          </div>
        </div>
        <i class="bi bi-chevron-right"></i>
      </div>
    </div>

    <!-- 내가 속한 광장 -->
    <div class="section">
      <h2 class="section-title">
        <i class="bi bi-people-fill" style="color:#a7cc10"></i>
        내가 속한 광장
      </h2>

      <div v-if="loadingJoined" class="loading-state">
        <div class="spinner"></div>
        <p>광장 조회 중...</p>
      </div>

      <div v-else-if="joinedPlazas.length === 0" class="empty-state">
        <i class="bi bi-inbox" style="font-size:2.5rem; color:#c2d477"></i>
        <p>참여 중인 광장이 없습니다</p>
        <p class="empty-hint">친구의 광장 초대를 기다려보세요!</p>
      </div>

      <div v-else class="plaza-list">
        <div
          v-for="plaza in joinedPlazas"
          :key="plaza.plazaNo"
          class="plaza-card"
          @click="goToPlaza(plaza.plazaNo)"
        >
          <div class="plaza-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" fill="#a7cc10" stroke="#fff" stroke-width="2"/>
              <circle cx="12" cy="10" r="3" fill="#fff"/>
            </svg>
          </div>
          <div class="plaza-info">
            <div class="plaza-name">{{ plaza.plazaName }}</div>
            <div class="plaza-meta">
              <span class="plaza-badge">이웃</span>
              <span class="plaza-members">멤버 {{ plaza.memberCount }}명</span>
            </div>
          </div>
          <i class="bi bi-chevron-right"></i>
        </div>
      </div>
    </div>

    <!-- 광장 만들기 버튼 (방장인 광장이 없을 때만) -->
    <div v-if="!ownedPlaza && !loadingOwned" class="floating-action">
      <button @click="goToCreatePlaza" class="btn-floating">
        <i class="bi bi-plus-lg"></i>
        <span>광장 만들기</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const ownedPlaza = ref(null)
const joinedPlazas = ref([])
const loadingOwned = ref(false)
const loadingJoined = ref(false)

onMounted(async () => {
  await Promise.all([
    loadOwnedPlaza(),
    loadJoinedPlazas()
  ])
})

// 내가 방장인 광장 조회
async function loadOwnedPlaza() {
  loadingOwned.value = true
  try {
    const response = await axios.get('/NH/api/neighbor/plazas/my-owned')
    ownedPlaza.value = response.data
  } catch (error) {
    console.error('방장 광장 조회 실패:', error)
  } finally {
    loadingOwned.value = false
  }
}

// 내가 속한 광장 조회 (이웃으로 참여 중)
async function loadJoinedPlazas() {
  loadingJoined.value = true
  try {
    const response = await axios.get('/NH/api/neighbor/plazas/my-joined')
    joinedPlazas.value = response.data
  } catch (error) {
    console.error('참여 광장 조회 실패:', error)
  } finally {
    loadingJoined.value = false
  }
}

function goToPlaza(plazaNo) {
  router.push(`/plazaDetail/${plazaNo}`)
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
  min-height: 600px;
  overflow-y: auto; 
  background: #f9fafb;
  padding: 0 0 20px 0;  
  margin-top: -20px;   
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

/* 섹션 */
.section {
  margin: 20px;
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

/* 로딩/빈 상태 */
.loading-state, .empty-state {
  text-align: center;
  padding: 50px 20px;
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

.empty-state p {
  font-size: 15px;
  margin: 12px 0;
}

.empty-hint {
  font-size: 13px;
  color: #9ca3af;
  font-style: italic;
}

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #a7cc10;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 12px;
  transition: all 0.2s;
}

.btn-create:hover {
  background: #8fb80e;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(167, 204, 16, 0.3);
}

/* 광장 카드 */
.plaza-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

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

/* 플로팅 버튼 */
.floating-action {
  position: relative; 
  bottom: auto;       
  left: auto;         
  transform: none;     
  z-index: 100;
  text-align: center;  
  margin: 20px 0; 
}

.btn-floating {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 24px;
  background: #a7cc10;
  color: white;
  border: none;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(167, 204, 16, 0.4);
  transition: all 0.3s;
}

.btn-floating:hover {
  background: #8fb80e;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(167, 204, 16, 0.5);
}

.btn-floating i {
  font-size: 20px;
}
</style>

<template>
  <div class="page">
    <!-- 헤더 -->
    <div class="header-section">
      <button @click="goBack" class="btn-back">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h2 class="header-title">{{ plazaInfo.plazaName || '광장' }}</h2>
      <p class="header-subtitle">현재 {{ activeMemberCount }}명이 광장에 있습니다</p>
    </div>

    <!-- 카카오 지도 -->
    <div class="map-container">
      <div ref="mapEl" class="map"></div>
    </div>

    <!-- 광장 정보 카드 -->
    <div class="info-card">
      <div class="info-header">
        <div class="info-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="#a7cc10" stroke-width="2"/>
            <circle cx="12" cy="10" r="3" stroke="#a7cc10" stroke-width="2"/>
          </svg>
        </div>
        <div class="info-details">
          <div class="info-name">{{ plazaInfo.plazaName }}</div>
          <div class="info-desc">반경 50m 이내 활동 범위</div>
        </div>
        <div v-if="isOwner" class="owner-badge">방장</div>
      </div>
    </div>

    <!-- 활성 멤버 목록 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">
          <i class="bi bi-geo-alt-fill" style="color:#a7cc10"></i>
          광장 안에 있는 이웃 ({{ activeMembers.length }})
        </h3>
        <button @click="refreshActiveMembers" class="btn-refresh">
          <i class="bi bi-arrow-clockwise"></i>
        </button>
      </div>

      <div v-if="loadingActive" class="loading-state">
        <div class="spinner"></div>
        <p>위치 조회 중...</p>
      </div>

      <div v-else-if="activeMembers.length === 0" class="empty-state">
        <i class="bi bi-person-x" style="font-size:2.5rem; color:#c2d477"></i>
        <p>현재 광장 안에 있는 이웃이 없습니다</p>
      </div>

      <div v-else class="member-list">
        <div v-for="member in activeMembers" :key="member.userNo" class="member-item active">
          <div class="member-avatar">
            <img v-if="member.profilePhoto" :src="member.profilePhoto" alt="프로필" />
            <div v-else class="avatar-placeholder">
              {{ member.name.charAt(0) }}
            </div>
          </div>
          <div class="member-info">
            <div class="member-name">
              {{ member.name }}
              <span class="active-badge">활동 중</span>
            </div>
            <div class="member-distance">중심으로부터 {{ Math.round(member.distanceFromCenter) }}m</div>
          </div>
          <div class="member-marker">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z" fill="#16a34a" stroke="#fff" stroke-width="2"/>
              <circle cx="12" cy="10" r="3" fill="#fff"/>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 전체 멤버 목록 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">
          <i class="bi bi-people-fill" style="color:#a7cc10"></i>
          광장 멤버 ({{ allMembers.length }})
        </h3>
      </div>

      <div v-if="loadingMembers" class="loading-state">
        <div class="spinner"></div>
        <p>멤버 조회 중...</p>
      </div>

      <div v-else class="member-list">
        <div v-for="member in allMembers" :key="member.userNo" class="member-item">
          <div class="member-avatar">
            <img v-if="member.profilePhoto" :src="member.profilePhoto" alt="프로필" />
            <div v-else class="avatar-placeholder">
              {{ member.userName.charAt(0) }}
            </div>
          </div>
          <div class="member-info">
            <div class="member-name">{{ member.userName }}</div>
            <div class="member-role">{{ member.memberName }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 액션 버튼 -->
    <div class="actions">
      <button v-if="isOwner" class="btn-invite" @click="openInviteModal">
        <i class="bi bi-person-plus-fill"></i>
        초대하기
      </button>
      <button class="btn-leave" @click="leavePlaza">
        <i class="bi bi-box-arrow-right"></i>
        {{ isOwner ? '광장 삭제' : '탈퇴하기' }}
      </button>
    </div>

    <!-- 지현 수정: 초대 모달 - 친구 목록 제거, 사용자 ID 입력으로 변경 -->
    <!-- 초대 모달 -->
    <div v-if="showInviteModal" class="modal-overlay" @click="showInviteModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>사용자 초대</h3>
          <button @click="showInviteModal = false" class="btn-close">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <div class="modal-body">
          <div class="invite-form">
            <label class="form-label">초대할 사용자 ID</label>
            <input 
              v-model="inviteUserId" 
              type="text" 
              placeholder="사용자 ID를 입력하세요"
              class="form-input"
              @keyup.enter="inviteUser"
            />
            <p class="form-hint">
              <i class="bi bi-info-circle-fill me-1"></i>
              초대하려는 사용자의 ID를 입력해주세요
            </p>
            <button @click="inviteUser" class="btn-invite-submit">
              <i class="bi bi-person-plus-fill me-1"></i>
              초대하기
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'
const mapEl = ref(null)
const plazaNo = ref(parseInt(route.params.plazaNo))

const plazaInfo = ref({
  plazaNo: 0,
  plazaName: '',
  centerLat: 0,
  centerLng: 0,
  radius: 50
})

const allMembers = ref([])
const activeMembers = ref([])
// 지현 수정: myFriends 제거, inviteUserId 추가
const inviteUserId = ref('')
const isOwner = ref(false)
const loadingMembers = ref(false)
const loadingActive = ref(false)
const showInviteModal = ref(false)

let map = null
let circle = null
let markers = []
let refreshInterval = null
let locationInterval = null

const activeMemberCount = computed(() => activeMembers.value.length)

onMounted(async () => {
  await initMap()
  await loadPlazaInfo()
  await loadAllMembers()
  await loadActiveMembers()
  
  // 10초마다 활성 멤버 자동 갱신
  refreshInterval = setInterval(() => {
    loadActiveMembers()
  }, 10000)
  
  // 실시간 위치 전송 시작
  startLocationTracking()
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
  
  // 위치 전송 중지
  stopLocationTracking()
})

// 실시간 위치 추적 시작
function startLocationTracking() {
  sendCurrentLocation()
  locationInterval = setInterval(() => {
    sendCurrentLocation()
  }, 30000)
}

// 위치 추적 중지
function stopLocationTracking() {
  if (locationInterval) {
    clearInterval(locationInterval)
    locationInterval = null
  }
}

// 현재 위치를 서버로 전송
async function sendCurrentLocation() {
  if (!navigator.geolocation) {
    console.warn('이 브라우저는 위치 서비스를 지원하지 않습니다.')
    return
  }
  
  navigator.geolocation.getCurrentPosition(
    async (position) => {
      const latitude = position.coords.latitude
      const longitude = position.coords.longitude
      
      console.log('현재 위치:', latitude, longitude)
      
      try {
        await axios.post('/NH/api/neighbor/location/update', {
          latitude: latitude,
          longitude: longitude
        })
        console.log('위치 전송 성공')
      } catch (error) {
        console.error('위치 전송 실패:', error)
      }
    },
    (error) => {
      console.error('위치 조회 오류:', error.message)
      if (error.code === error.PERMISSION_DENIED) {
        console.warn('위치 권한이 거부되었습니다.')
      }
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0
    }
  )
}

// 카카오맵 초기화
async function initMap() {
  if (!window.kakao || !window.kakao.maps) {
    await loadKakaoMap()
  }

  const container = mapEl.value
  const options = {
    center: new window.kakao.maps.LatLng(37.5665, 126.9780),
    level: 3
  }

  map = new window.kakao.maps.Map(container, options)
}

function loadKakaoMap() {
  return new Promise((resolve) => {
    if (window.kakao && window.kakao.maps) {
      resolve()
      return
    }

    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false`
    document.head.appendChild(script)

    script.onload = () => {
      window.kakao.maps.load(() => {
        resolve()
      })
    }
  })
}

// 광장 정보 조회
async function loadPlazaInfo() {
  try {
    const response = await axios.get(`/NH/api/neighbor/plazas/${plazaNo.value}`)
    plazaInfo.value = response.data

    const center = new window.kakao.maps.LatLng(plazaInfo.value.centerLat, plazaInfo.value.centerLng)
    map.setCenter(center)

    circle = new window.kakao.maps.Circle({
      center: center,
      radius: plazaInfo.value.radius,
      strokeWeight: 3,
      strokeColor: '#a7cc10',
      strokeOpacity: 0.8,
      strokeStyle: 'solid',
      fillColor: '#a7cc10',
      fillOpacity: 0.2
    })

    circle.setMap(map)
  } catch (error) {
    console.error('광장 정보 조회 실패:', error)
    alert('광장 정보를 불러올 수 없습니다.')
  }
}

// 전체 멤버 조회
async function loadAllMembers() {
  loadingMembers.value = true
  try {
    const response = await axios.get(`/NH/api/neighbor/plazas/${plazaNo.value}/members`)
    allMembers.value = response.data

    const myUser = allMembers.value.find(m => m.memberName === '방장')
    if (myUser) {
      isOwner.value = true
    }
  } catch (error) {
    console.error('멤버 조회 실패:', error)
  } finally {
    loadingMembers.value = false
  }
}

// 활성 멤버 조회
async function loadActiveMembers() {
  loadingActive.value = true
  try {
    const response = await axios.get(`/NH/api/neighbor/plazas/${plazaNo.value}/active-members`)
    activeMembers.value = response.data

    markers.forEach(marker => marker.setMap(null))
    markers = []

    activeMembers.value.forEach(member => {
      const position = new window.kakao.maps.LatLng(member.latitude, member.longitude)
      
      const marker = new window.kakao.maps.Marker({
        position: position,
        map: map,
        title: member.name
      })

      markers.push(marker)
    })
  } catch (error) {
    console.error('활성 멤버 조회 실패:', error)
  } finally {
    loadingActive.value = false
  }
}

// 활성 멤버 수동 새로고침
async function refreshActiveMembers() {
  await loadActiveMembers()
}

// 지현 수정: 초대 모달 열기 - 친구 목록 조회 제거
// 초대 모달 열기
function openInviteModal() {
  showInviteModal.value = true
  inviteUserId.value = ''
}

// 지현 수정: 사용자 ID로 초대하는 함수로 변경
// 사용자 초대
async function inviteUser() {
  if (!inviteUserId.value.trim()) {
    alert('사용자 ID를 입력해주세요.')
    return
  }

  try {
    await axios.post(`/NH/api/neighbor/plazas/${plazaNo.value}/invite/${inviteUserId.value}`)
    alert('초대가 완료되었습니다! 🎉')
    showInviteModal.value = false
    inviteUserId.value = ''
    await loadAllMembers()
  } catch (error) {
    console.error('초대 실패:', error)
    alert(error.response?.data?.message || '초대에 실패했습니다.')
  }
}

// 광장 탈퇴/삭제
async function leavePlaza() {
  const message = isOwner.value 
    ? '광장을 삭제하시겠습니까? 모든 멤버가 나가게 됩니다.' 
    : '광장에서 탈퇴하시겠습니까?'
  
  if (!confirm(message)) return

  try {
    if (isOwner.value) {
      await axios.delete(`/NH/api/neighbor/plazas/${plazaNo.value}`)
      alert('광장이 삭제되었습니다.')
    } else {
      await axios.post(`/NH/api/neighbor/plazas/${plazaNo.value}/leave`)
      alert('광장에서 탈퇴했습니다.')
    }
    router.push('/myPlaza')
  } catch (error) {
    console.error('탈퇴 실패:', error)
    alert(error.response?.data?.message || '처리에 실패했습니다.')
  }
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
  min-height: 100vh;
  max-height: 100vh; 
  overflow-y: auto;   
  background: #f9fafb;
  padding: 0;
  padding-bottom: 70px; 
  margin-top: -15px;
  scrollbar-width: none;
}

/* 헤더 */
.header-section {
  background: linear-gradient(135deg, #a7cc10 0%, #8fb80e 100%);
  color: white;
  padding: 20px 20px 25px 20px;
  position: relative;
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

.header-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px 0;
  text-align: center;
}

.header-subtitle {
  font-size: 14px;
  opacity: 0.95;
  margin: 0;
  text-align: center;
}

/* 지도 */
.map-container {
  width: 100%;
  height: 300px;
  background: #e5e7eb;
  margin-top: -100px;
}

.map {
  width: 100%;
  height: 100%;
}

/* 광장 정보 카드 */
.info-card {
  margin: 20px;
  padding: 16px;
  background: white;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  border: 2px solid #c2d477;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.info-icon {
  width: 44px;
  height: 44px;
  background: #f5f9e8;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-details {
  flex: 1;
}

.info-name {
  font-size: 17px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.info-desc {
  font-size: 13px;
  color: #6b7280;
}

.owner-badge {
  background: #a7cc10;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

/* 섹션 */
.section {
  margin: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-refresh {
  background: white;
  border: 2px solid #c2d477;
  border-radius: 10px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a7cc10;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-refresh:hover {
  background: #f5f9e8;
  border-color: #a7cc10;
  transform: rotate(180deg);
}

/* 로딩/빈 상태 */
.loading-state, .empty-state {
  text-align: center;
  padding: 40px 20px;
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

/* 멤버 리스트 */
.member-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.member-item.active {
  border-color: #a7cc10;
  background: #f5f9e8;
}

.member-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  overflow: hidden;
  background: #a7cc10;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.member-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  color: white;
  font-size: 20px;
  font-weight: 700;
}

.avatar-placeholder.small {
  font-size: 16px;
}

.member-info {
  flex: 1;
}

.member-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-role, .member-distance {
  font-size: 13px;
  color: #6b7280;
}

.active-badge {
  display: inline-block;
  background: #16a34a;
  color: white;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
}

.member-marker {
  flex-shrink: 0;
}

/* 액션 버튼 */
.actions {
  position: relative;
  margin: 20px;
  margin-bottom: 10px;
  padding: 16px 20px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  display: flex;
  gap: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.btn-invite, .btn-leave {
  flex: 1;
  padding: 14px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
}

.btn-invite {
  background: #a7cc10;
  color: white;
}

.btn-invite:hover {
  background: #8fb80e;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(167, 204, 16, 0.3);
}

.btn-leave {
  background: white;
  color: #ef4444;
  border: 2px solid #ef4444;
}

.btn-leave:hover {
  background: #fef2f2;
}

/* 모달 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 350px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.btn-close {
  background: transparent;
  border: none;
  color: #6b7280;
  font-size: 18px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #f3f4f6;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
}

/* 지현 수정: 초대 폼 스타일 추가 */
.invite-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: -8px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #c2d477;
  border-radius: 10px;
  font-size: 15px;
  transition: all 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #a7cc10;
  box-shadow: 0 0 0 3px rgba(167, 204, 16, 0.1);
}

.form-hint {
  font-size: 13px;
  color: #6b7280;
  margin: -8px 0 0 0;
  display: flex;
  align-items: center;
}

.btn-invite-submit {
  width: 100%;
  padding: 14px;
  background: #a7cc10;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-invite-submit:hover {
  background: #8fb80e;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(167, 204, 16, 0.3);
}
</style>

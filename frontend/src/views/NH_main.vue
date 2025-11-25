<template>
  <div class="container-sm py-3 neighbor-page-container" style="max-width:414px; position:relative;">
    <!-- 상단 문구 -->
    <div class="my-3">
      <div class="d-flex align-items-center justify-content-between">
        <div class="text-start flex-grow-1">
          <div class="safety-status-text">
            현재 만남의 광장에 {{ activeMemberCount }}명이 있습니다
          </div>
        </div>
      </div>
    </div>

    <!-- 경로당 배경 이미지 + 활성 멤버 -->
    <div class="card border-0 shadow-sm position-relative overflow-hidden mb-4 rounded-4">
      <div class="plaza-background" style="height:280px;">
        <div class="active-members-container">
          <div v-for="member in activeMembers" :key="member.userNo" class="member-avatar" :style="member.position">
            <img :src="member.profilePhoto || '/default-avatar.png'" :alt="member.name" class="member-photo" />
            <span class="member-name">{{ member.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 지도 아래 버튼 -->
    <div class="d-flex justify-content-between mb-3 px-2">
      <button @click="showEditProfileModal = true" class="btn btn-outline-success flex-fill me-2">
        내 정보 수정
      </button>
      <button @click="handleLogout" class="btn btn-outline-danger flex-fill ms-2">
        로그아웃
      </button>
    </div>

    <!-- 환자에서 들어왔을 때만 보이는 버튼 (로컬스토리지/쿼리값 기반) -->
    <div v-if="showPatientReturnBtn" class="mb-3">
      <button class="btn w-100 rounded-pill patient-return-btn" @click="goToDP">
        <i class="bi bi-person-heart me-1"></i>
        내 첫화면으로 돌아가기
      </button>
    </div>

    <!-- 모임 일정 -->
    <h6 class="fw-bold mb-2">모임 일정</h6>
    <div v-if="upcomingSchedules.length > 0">
      <div v-for="(schedule, index) in upcomingSchedules" :key="index" class="card border-2 rounded-3 p-3 mb-2" style="border-color:#e9ecef">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <span class="small text-secondary mb-1">{{ formatDate(schedule.scheduleDate) }}</span>
          <div class="d-flex align-items-center gap-2">
            <span class="fw-semibold">{{ schedule.scheduleTitle }}</span>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="card border-0 shadow-sm rounded-4 mb-2">
      <div class="card-body text-center text-muted">
        <p>일정이 없습니다.</p>
        <p>일정을 입력해보세요!</p>
      </div>
    </div>

    <!-- 내 만남의 광장 버튼 -->
    <div class="mb-3 mt-4">
      <button class="btn w-100 rounded-pill neighbor-btn-plaza" @click="router.push('/myPlaza')">
        <i class="bi bi-geo-fill me-2"></i>
        내 만남의 광장
      </button>
    </div>

    <div class="row g-2 mb-3">
      <div class="col-6">
        <button class="btn w-100 rounded-pill neighbor-btn-primary" @click="router.push('/makeFriends')">
          <i class="bi bi-person-plus-fill me-1"></i>
          친구 추가
        </button>
      </div>
      <div class="col-6">
        <button class="btn w-100 rounded-pill neighbor-btn-secondary" @click="router.push('/createPlaza')">
          <i class="bi bi-geo-alt-fill me-1"></i>
          광장 만들기
        </button>
      </div>
    </div>

    <!-- AI 챗봇 버튼 -->
    <button class="fab-ai" @click="router.push('/chat')">
      <span class="fab-ai-label">AI챗봇</span>
    </button>

    <NH_ModifyProfileModal 
      :show="showEditProfileModal" 
      @close="showEditProfileModal = false" 
      @saved="reloadUserProfile" 
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout } from '@/utils/auth'
import NH_ModifyProfileModal from '@/components/NH_ModifyProfileModal.vue'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

const showEditProfileModal = ref(false)
const activeMembers = ref([])
const myPlazaNo = ref(null)
const upcomingSchedules = ref([])

const activeMemberCount = computed(() => activeMembers.value.length)

// ** 로컬 스토리지 (sessionStorage) 로 쿼리값 보존하여 버튼 활성화 유지 **
const fromPatientFlag = ref(false)

onMounted(() => {
  if (route.query.fromPatient === '1') {
    fromPatientFlag.value = true
    sessionStorage.setItem('fromPatient', '1')
  } else if (sessionStorage.getItem('fromPatient') === '1') {
    fromPatientFlag.value = true
  }
})

const showPatientReturnBtn = computed(() => fromPatientFlag.value)

function goToDP() {
  router.push('/DP')
}

let locationInterval = null
function startLocationTracking() {
  sendCurrentLocation()
  locationInterval = setInterval(() => { sendCurrentLocation() }, 30000)
}
function stopLocationTracking() {
  if (locationInterval) { clearInterval(locationInterval); locationInterval = null }
}
async function sendCurrentLocation() {
  if (!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(
    async (position) => {
      const { latitude, longitude } = position.coords
      try {
        await axios.post('/NH/api/neighbor/location/update', { latitude, longitude }, { withCredentials: true })
      } catch (error) { }
    },
    () => { },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
  )
}

async function fetchActiveMembers() {
  if (!myPlazaNo.value) return
  try {
    const response = await axios.get(`/NH/api/neighbor/plazas/${myPlazaNo.value}/active-members`, { withCredentials: true })
    const members = response.data || []
    activeMembers.value = members.map(member => ({
      ...member,
      position: getRandomPosition()
    }))
  } catch { activeMembers.value = [] }
}

async function fetchMyPlaza() {
  try {
    const response = await axios.get('/NH/api/neighbor/plazas/my', { withCredentials: true })
    if (response.data && response.data.plazaNo) {
      myPlazaNo.value = response.data.plazaNo
      await fetchActiveMembers()
    }
  } catch { }
}

function getRandomPosition() {
  const top = Math.random() * 35 + 40
  const left = Math.random() * 80 + 10
  return { top: `${top}%`, left: `${left}%` }
}
let movementInterval = null
function startRandomMovement() {
  movementInterval = setInterval(() => {
    activeMembers.value = activeMembers.value.map(member => ({
      ...member,
      position: getRandomPosition()
    }))
  }, 5000)
}

function stopRandomMovement() { if (movementInterval) { clearInterval(movementInterval); movementInterval = null } }
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString + 'T00:00:00')
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const handleLogout = async () => {
  const success = await logout()
  if (success) {
    // 환자 상태 제거
    sessionStorage.removeItem('fromPatient')
    router.push('/login')
  } else {
    alert('로그아웃에 실패했습니다.')
  }
}

const fetchUpcomingSchedules = async () => {
  try {
    const response = await fetch('/NH/api/schedule/upcoming')
    if (!response.ok) throw new Error(`API 오류: ${response.status}`)
    const data = await response.json()
    upcomingSchedules.value = data || []
  } catch { upcomingSchedules.value = [] }
}
const reloadUserProfile = () => { }

onMounted(async () => {
  try {
    await nextTick()
    await fetchUpcomingSchedules()
    await fetchMyPlaza()
    startLocationTracking()
    startRandomMovement()
    setInterval(() => { fetchActiveMembers() }, 10000)
  } catch (e) { console.error(e) }
})

onUnmounted(() => { stopLocationTracking(); stopRandomMovement() })
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/gh/sunn-us/SUIT/fonts/static/woff2/SUIT.css');

.plaza-background {
  width: 100%;
  background-image: url('/NeighborPlaza.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}

.active-members-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.member-avatar {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 5;
  transition: top 2s ease-in-out, left 2s ease-in-out;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {

  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-10px);
  }
}

.member-photo {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 3px solid #a7cc10;
  object-fit: cover;
  background: #fff;
}

.member-name {
  margin-top: 4px;
  font-size: 0.7rem;
  font-weight: 700;
  color: #fff;
  background: rgba(0, 0, 0, 0.7);
  padding: 2px 8px;
  border-radius: 8px;
  white-space: nowrap;
}

.safety-status-text {
  font-family: 'SUIT', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 1.2rem;
  font-weight: 400;
}

.neighbor-page-container {
  padding-bottom: 100px;
  margin-bottom: 70px;
  margin-top: -25px;
}

.patient-return-btn {
  background: linear-gradient(135deg, #fb7185 0%, #f43f5e 100%);
  color: #fff;
  font-family: 'SUIT', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 1.05rem;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(251, 113, 133, 0.18);
  border: none;
  padding: 14px 0;
  transition: background 0.3s, box-shadow 0.2s, transform 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.patient-return-btn i {
  font-size: 1.2rem;
  margin-right: 6px;
  vertical-align: middle;
}

.patient-return-btn:hover,
.patient-return-btn:focus {
  background: linear-gradient(135deg, #f43f5e 0%, #e11d48 100%);
  box-shadow: 0 8px 24px rgba(251, 113, 133, 0.28);
  transform: translateY(-2px) scale(1.03);
  color: #fff;
  outline: none;
}

.neighbor-btn-plaza {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: white;
  font-weight: 700;
  font-size: 1.1rem;
  padding: 14px;
  border: none;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.4);
  transition: all 0.3s ease;
}

.neighbor-btn-plaza:hover {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(251, 191, 36, 0.5);
}

.neighbor-btn-primary {
  background: linear-gradient(135deg, #a7cc10 0%, #8fb80e 100%);
  color: white;
  font-weight: 600;
  border: none;
  box-shadow: 0 2px 8px rgba(167, 204, 16, 0.25);
  transition: all 0.3s ease;
}

.neighbor-btn-primary:hover {
  background: linear-gradient(135deg, #8fb80e 0%, #7aa00c 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(167, 204, 16, 0.35);
}

.neighbor-btn-secondary {
  background: white;
  color: #a7cc10;
  font-weight: 600;
  border: 2px solid #a7cc10;
  transition: all 0.3s ease;
}

.neighbor-btn-secondary:hover {
  background: #f5f9e8;
  border-color: #8fb80e;
  color: #8fb80e;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(167, 204, 16, 0.2);
}

.fab-ai {
  position: fixed;
  bottom: 150px;
  right: calc((100vw - 414px) / 2 + 16px);
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 18px;
  border-radius: 999px;
  border: none;
  background: #A7CC10;
  color: #ffffff;
  font-size: 0.9rem;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.18);
  cursor: pointer;
  z-index: 9999;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.fab-ai-label {
  white-space: nowrap;
}

.fab-ai:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.25);
  opacity: 0.96;
}
</style>

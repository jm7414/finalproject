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
        <!-- 활성 멤버 프로필 표시 영역 -->
        <div class="active-members-container">
          <div
            v-for="member in activeMembers"
            :key="member.userNo"
            class="member-avatar"
            :style="member.position"
          >
            <img 
              :src="member.profilePhoto || '/default-avatar.png'" 
              :alt="member.name"
              class="member-photo"
            />
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

    <!-- 모임 일정 -->
    <h6 class="fw-bold mb-2">모임 일정</h6>

    <!-- 일정 2개 -->
    <div v-if="upcomingSchedules.length > 0">
      <div v-for="(schedule, index) in upcomingSchedules" :key="index" class="card border-2 rounded-3 p-3 mb-2"
        style="border-color:#e9ecef">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <span class="small text-secondary mb-1">{{ formatDate(schedule.scheduleDate) }}</span>
          <div class="d-flex align-items-center gap-2">
            <span class="fw-semibold">{{ schedule.scheduleTitle }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 일정 없음 -->
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

    <!-- 친구 추가 & 광장 만들기 버튼 -->
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

    <!-- 내정보 수정 모달 -->
    <NH_ModifyProfileModal :show="showEditProfileModal" @close="showEditProfileModal = false"
      @saved="reloadUserProfile" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '@/utils/auth'
import NH_ModifyProfileModal from '@/components/NH_ModifyProfileModal.vue'
import axios from 'axios'

const router = useRouter()
const upcomingSchedules = ref([])
const showEditProfileModal = ref(false)
const activeMembers = ref([])
const myPlazaNo = ref(null)

// 활성 멤버 수 계산
const activeMemberCount = computed(() => activeMembers.value.length)

// ===== 실시간 위치 전송 로직 =====
let locationInterval = null

function startLocationTracking() {
  sendCurrentLocation()
  locationInterval = setInterval(() => {
    sendCurrentLocation()
  }, 30000)
}

function stopLocationTracking() {
  if (locationInterval) {
    clearInterval(locationInterval)
    locationInterval = null
  }
}

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
        }, {
          withCredentials: true  // 추가
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

// ===== 활성 멤버 조회 =====
async function fetchActiveMembers() {
  if (!myPlazaNo.value) return
  
  try {
    const response = await axios.get(
      `/NH/api/neighbor/plazas/${myPlazaNo.value}/active-members`,
      { withCredentials: true }  // ← 추가
    )
    const members = response.data || []
    
    // 각 멤버에 초기 위치 할당
    activeMembers.value = members.map(member => ({
      ...member,
      position: getRandomPosition()
    }))
    
    console.log('활성 멤버:', activeMembers.value)
  } catch (error) {
    console.error('활성 멤버 조회 실패:', error)
    activeMembers.value = []
  }
}


// ===== 내 광장 번호 조회 =====
async function fetchMyPlaza() {
  try {
    const response = await axios.get(
      '/NH/api/neighbor/plazas/my',
      { withCredentials: true }  // 추가
    )
    
    if (response.data && response.data.plazaNo) {
      myPlazaNo.value = response.data.plazaNo
      await fetchActiveMembers()
    } else {
      console.log('속한 광장이 없습니다.')
    }
  } catch (error) {
    console.error('내 광장 조회 실패:', error)
  }
}


// ===== 랜덤 위치 생성 (TV 아래 영역, 하단 가려짐 방지) =====
function getRandomPosition() {
  // TV 아래 영역: 상단 40% ~ 하단 75% 사이 (프로필이 가려지지 않도록 조정)
  const top = Math.random() * 35 + 40 // 40% ~ 75%
  const left = Math.random() * 80 + 10 // 10% ~ 90%
  
  return {
    top: `${top}%`,
    left: `${left}%`
  }
}

// ===== 랜덤 이동 애니메이션 시작 =====
let movementInterval = null

function startRandomMovement() {
  movementInterval = setInterval(() => {
    activeMembers.value = activeMembers.value.map(member => ({
      ...member,
      position: getRandomPosition()
    }))
  }, 5000) // 5초마다 위치 변경
}

function stopRandomMovement() {
  if (movementInterval) {
    clearInterval(movementInterval)
    movementInterval = null
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString + 'T00:00:00')
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}/${day}`
}

const handleLogout = async () => {
  const success = await logout()
  if (success) {
    router.push('/login')
  } else {
    alert('로그아웃에 실패했습니다.')
  }
}

const fetchUpcomingSchedules = async () => {
  try {
    const response = await fetch('/NH/api/schedule/upcoming')
    if (!response.ok) {
      throw new Error(`API 오류: ${response.status}`)
    }
    const data = await response.json()
    upcomingSchedules.value = data || []
  } catch (error) {
    console.error('일정 조회 실패:', error)
    upcomingSchedules.value = []
  }
}

const reloadUserProfile = () => {
  // 내정보 수정 후, 필요한 동작
}

onMounted(async () => {
  try {
    await nextTick()
    await fetchUpcomingSchedules()
    await fetchMyPlaza()
    
    // 위치 전송 시작
    startLocationTracking()
    
    // 랜덤 이동 애니메이션 시작
    startRandomMovement()
    
    // 10초마다 활성 멤버 갱신
    setInterval(() => {
      fetchActiveMembers()
    }, 10000)
  } catch (e) {
    console.error(e)
  }
})

onUnmounted(() => {
  stopLocationTracking()
  stopRandomMovement()
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/gh/sunn-us/SUIT/fonts/static/woff2/SUIT.css');

/* 경로당 배경 이미지 */
.plaza-background {
  width: 100%;
  background-image: url('/NeighborPlaza.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}

/* 활성 멤버 컨테이너 */
.active-members-container {
  width: 100%;
  height: 100%;
  position: relative;
}

/* 멤버 아바타 */
.member-avatar {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 5;
  transition: top 2s ease-in-out, left 2s ease-in-out;
  animation: bounce 2s ease-in-out infinite;
}

/* 바운스 애니메이션 */
@keyframes bounce {
  0%, 100% {
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

/* 내 만남의 광장 버튼 */
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

/* 기존 버튼 스타일 */
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
  right: calc((100vw - 414px) / 2 + 16px);  /* 카드 오른쪽 안쪽 */
  width: 64px;                           /* 동그라미 크기 */
  height: 64px;                          /* 동그라미 크기 */
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

<!-- src/views/PatientHome.vue -->
<template>
  <div class="dp-main-page bg-light">
    <div class="container-sm py-3" style="max-width:414px">
      <!-- 메인 카드 -->
      <div class="card border-0 shadow-sm rounded-3 overflow-hidden">
        <div class="card-body p-3">
          <!-- 인사 -->
          <h3 class="fw-bold mb-3" style="letter-spacing:-.2px">
            {{ userName || '사용자' }} 님 안녕하세요!
          </h3>

          <!-- 오늘 일정 카드 -->
          <div class="border rounded-4 shadow-sm p-3 mb-3 bg-white position-relative" style="min-height:106px">
            <div v-if="loading" class="text-secondary small">일정을 불러오는 중...</div>

            <template v-else>
              <div v-if="currentSchedule" class="d-flex justify-content-center align-items-baseline gap-2 mb-2">
                <div class="display-6 fw-semibold mb-0">
                  {{ formatHm(currentSchedule.startTime) }} {{ trimTitle(currentSchedule.scheduleTitle) }}
                </div>
              </div>
              <div v-else class="text-center fw-semibold mb-2">오늘 진행 중인 일정이 없어요</div>

              <div class="rounded-pill mb-2" style="height:6px;background:#e5e7eb"></div>

              <div class="text-secondary text-center">
                <template v-if="nextSchedule">
                  다음: {{ formatHm(nextSchedule.startTime) }} - {{ trimTitle(nextSchedule.scheduleTitle) }}
                </template>
                <template v-else>다음 일정이 없습니다</template>
              </div>
            </template>
          </div>

          <!-- 기능 타일 (교차 배치) -->
          <div class="row g-3 align-items-stretch">
            <!-- 1) 내 안심존 : 큰 -->
            <div class="col-6">
              <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
                style="height:220px;background-image:linear-gradient(135deg,#9CA7FF 0%,#7E89FF 55%,#6D7BFF 100%);
                     background-size:100% 100%;background-repeat:no-repeat;box-shadow:0 8px 20px rgba(16,24,40,.08);"
                @click="go('/geo-fencing')">
                <!-- 이미지 영역(라벨 높이만큼 아래 비움) -->
                <div class="position-absolute top-0 start-0 end-0" style="bottom:44px">
                  <div class="h-100 d-flex align-items-center justify-content-center">
                    <img :src="imgZone" alt="" class="d-block"
                      style="width:125%; max-width:none; height:auto; transform:translateY(3%); filter:drop-shadow(0 8px 12px rgba(0,0,0,.10)); opacity:.97;" />
                  </div>
                </div>
                <!-- 라벨 -->
                <div
                  class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold fs-5 text-white"
                  style="height:44px">
                  내 안심존
                </div>
              </button>
            </div>

            <!-- 2) 내 정보 : 작 (모달로 변경) -->
            <div class="col-6">
              <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
                style="height:180px;background-image:linear-gradient(135deg,#FF9C86 0%, #FF7A63 60%, #FF5C46 100%);
                     background-size:100% 100%;background-repeat:no-repeat;box-shadow:0 8px 20px rgba(16,24,40,.08);"
                @click="openMyInfoModal">
                <div class="position-absolute top-0 start-0 end-0" style="bottom:40px">
                  <div class="h-100 d-flex align-items-center justify-content-center">
                    <img :src="imgInfo" alt="" class="img-fluid"
                      style="max-height:100%;object-fit:contain;transform: scale(1.18) translateY(3%);filter:drop-shadow(0 8px 12px rgba(0,0,0,.10));opacity:.97" />
                  </div>
                </div>
                <div
                  class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold fs-5 text-white"
                  style="height:40px">
                  내 정보
                </div>
              </button>
            </div>

            <!-- 3) 두뇌 게임 : 작 -->
            <div class="col-6" style="margin-top:30px">
              <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
                style="height:180px;background-image:linear-gradient(135deg,#FFD9C3 0%, #FFC19E 60%, #FFAB83 100%);
                     background-size:100% 100%;background-repeat:no-repeat;box-shadow:0 8px 20px rgba(16,24,40,.08);"
                @click="go('/game')">
                <div class="position-absolute top-0 start-0 end-0" style="bottom:40px">
                  <div class="h-100 d-flex align-items-center justify-content-center">
                    <img :src="imgGame" alt="" class="img-fluid"
                      style="max-height:100%;object-fit:contain;filter:drop-shadow(0 8px 12px rgba(0,0,0,.10));opacity:.97" />
                  </div>
                </div>
                <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold"
                  style="height:40px;color:#232323;font-size:1.25rem">
                  두뇌 게임
                </div>
              </button>
            </div>

            <!-- 4) AI 챗봇 : 큰 -->
            <div class="col-6" style="margin-top:-10px">
              <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
                style="height:220px;background-image:linear-gradient(135deg,#FFE8B5 0%, #FFD27E 60%, #FFC35D 100%);
                     background-size:100% 100%;background-repeat:no-repeat;box-shadow:0 8px 20px rgba(16,24,40,.08);"
                @click="go('/chatbot')">
                <div class="position-absolute top-0 start-0 end-0" style="bottom:44px">
                  <div class="h-100 d-flex align-items-center justify-content-center">
                    <img :src="imgAi" alt="" class="img-fluid"
                      style="max-height:100%;object-fit:contain;filter:drop-shadow(0 8px 12px rgba(0,0,0,.10));opacity:.97" />
                  </div>
                </div>
                <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold"
                  style="height:44px;color:#232323;font-size:1.25rem">
                  AI 챗봇
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="py-3"></div>

      <!-- 내 정보 모달 -->
      <MyInfoModal v-model="isMyInfoModalOpen" :user-name="userName" @close="handleMyInfoModalClose" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import MyInfoModal from '@/components/MyinfoModal.vue'

import imgZone from '@/assets/images/My zone.svg'
import imgInfo from '@/assets/images/Myinfo.svg'
import imgGame from '@/assets/images/game.svg'
import imgAi from '@/assets/images/Ai.svg'

const router = useRouter()

/** ====== 상태 ====== */
const loading = ref(false)
const err = ref('')
const userName = ref('')
const patientUserNo = ref(null)
const allSchedules = ref([])
const isMyInfoModalOpen = ref(false)

// 위치 추적 관련
const locationUpdateInterval = ref(null)
const locationPermissionGranted = ref(false)

/** ====== 공통 fetch ====== */
async function request(url, options = {}) {
  const res = await fetch(url, { credentials: 'include', ...options })
  const ct = (res.headers.get('content-type') || '')
  const isJson = ct.includes('application/json')
  const body = isJson ? await res.json().catch(() => ({})) : await res.text().catch(() => '')
  if (!res.ok) throw new Error(typeof body === 'string' ? body : (body.message || `${res.status}`))
  return body
}

/** ====== API: 내 정보 / 일정 ====== */
async function fetchMe() {
  const me = await request('/api/user/me')
  userName.value = me?.name || me?.username || '사용자'
  patientUserNo.value = me?.userNo ?? me?.id ?? null
}
async function fetchSchedules(userNo) {
  return await request(`/api/schedule/list/${encodeURIComponent(userNo)}`)
}

/** ====== 유틸 ====== */
function todayKey() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}
function hmToMinutes(hm) {
  const [h, m] = String(hm).split(':').map(n => parseInt(n, 10))
  return (h || 0) * 60 + (m || 0)
}
function formatHm(hm) {
  return String(hm).slice(0, 5)
}
function trimTitle(t = '') {
  return t.length > 14 ? t.slice(0, 12) + '…' : t
}

/** ====== 파생: 오늘/현재/다음 일정 ====== */
const todaySchedules = computed(() => {
  const key = todayKey()
  return allSchedules.value
    .filter(s => s.scheduleDate === key)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
})

const currentSchedule = computed(() => {
  const now = new Date()
  const nowMin = now.getHours() * 60 + now.getMinutes()
  for (const s of todaySchedules.value) {
    const st = hmToMinutes(s.startTime)
    const et = hmToMinutes(s.endTime)
    if (nowMin >= st && nowMin <= et) return s
  }
  return null
})

const nextSchedule = computed(() => {
  const now = new Date()
  const nowMin = now.getHours() * 60 + now.getMinutes()
  return todaySchedules.value.find(s => hmToMinutes(s.startTime) > nowMin) || null
})

/** ====== 네비게이션 ====== */
function go(path) {
  router.push(path)
}

/** ====== 모달 관리 ====== */
function openMyInfoModal() {
  isMyInfoModalOpen.value = true
}

function handleMyInfoModalClose() {
  isMyInfoModalOpen.value = false
}

/** ====== 위치 추적 관련 함수 ====== */
// 위치 권한 요청 및 추적 시작
async function startLocationTracking() {
  try {
    // 위치 권한 요청
    const permission = await navigator.permissions.query({ name: 'geolocation' })
    if (permission.state === 'denied') {
      console.warn('위치 권한이 거부되었습니다.')
      return
    }

    // 위치 추적 시작
    locationPermissionGranted.value = true
    updateLocation() // 즉시 한 번 실행
    
    // 30초마다 위치 업데이트
    locationUpdateInterval.value = setInterval(() => {
      updateLocation()
    }, 30000) // 30초
  } catch (error) {
    console.error('위치 추적 시작 실패:', error)
  }
}

// 위치 추적 중지
function stopLocationTracking() {
  if (locationUpdateInterval.value) {
    clearInterval(locationUpdateInterval.value)
    locationUpdateInterval.value = null
  }
}

// 현재 위치 업데이트
function updateLocation() {
  if (!navigator.geolocation) {
    console.error('Geolocation이 지원되지 않습니다.')
    return
  }
  
  navigator.geolocation.getCurrentPosition(
    async (position) => {
      try {
        const locationData = {
          userNo: patientUserNo.value,
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          timestamp: Date.now()
        }

        // 서버에 위치 전송
        const response = await fetch('/api/location/update', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify(locationData)
        })

        if (!response.ok) {
          console.error('위치 업데이트 실패:', response.status)
        }
      } catch (error) {
        console.error('위치 전송 오류:', error)
      }
    },
    (error) => {
      console.error('위치 조회 실패:', error.message)
    },
    {
      enableHighAccuracy: true,
      timeout: 15000,        // 타임아웃 증가 (정확한 위치를 위해)
      maximumAge: 0          // 캐시 사용 안함 (항상 새로운 위치 요청)
    }
  )
}

/** ====== 초기 로드 ====== */
onMounted(async () => {
  loading.value = true
  err.value = ''
  try {
    await fetchMe()
    if (patientUserNo.value) {
      const list = await fetchSchedules(patientUserNo.value)
      allSchedules.value = Array.isArray(list) ? list : []
      
      // 환자 로그인 시 위치 추적 시작
      await startLocationTracking()
    }
  } catch (e) {
    err.value = String(e?.message || e)
    console.warn('[PatientHome] init error:', err.value)
  } finally {
    loading.value = false
  }
})

// 페이지 떠날 때 위치 추적 중지
onBeforeUnmount(() => {
  stopLocationTracking()
})
</script>

<style scoped>
.dp-main-page {
  height: 600px;
  max-width: 480px;
}
</style>
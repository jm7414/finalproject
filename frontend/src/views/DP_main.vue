<template>
  <div class="dp-main-page bg-light">
    <div class="container-sm py-3 dp-container" style="max-width:414px; margin-top: -20px;">
      <!-- 메인 카드 -->
      <div class="card border-0 shadow-sm rounded-3 overflow-hidden">
        <div class="card-body p-3">
          <p class="fs-3 fw-bold mb-1 text-center">{{ currentDate }}</p>
          <p class="fs-1 fw-bold mb-4 text-center">{{ currentClock }}</p>
          <div class="border rounded-4 shadow-sm p-3 mb-4 bg-white position-relative" style="min-height:106px;">
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
          <div class="row g-3 align-items-stretch">
            <div class="col-6">
              <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
                style="height:220px;background-image:linear-gradient(135deg,#9CA7FF 0%,#7E89FF 55%,#6D7BFF 100%);" @click="openSafeZoneModal">
                <div class="position-absolute top-0 start-0 end-0" style="bottom:44px">
                  <div class="h-100 d-flex align-items-center justify-content-center">
                    <img :src="imgZone" alt="" class="d-block" style="width:125%; max-width:none; height:auto; transform:translateY(3%); filter:drop-shadow(0 8px 12px rgba(0,0,0,.10)); opacity:.97;" />
                  </div>
                </div>
                <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold fs-5 text-white"
                  style="height:44px">내 안심존</div>
              </button>
            </div>
            <div class="col-6">
              <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
                style="height:180px;background-image:linear-gradient(135deg,#FF9C86 0%, #FF7A63 60%, #FF5C46 100%);" @click="openMyInfoModal">
                <div class="position-absolute top-0 start-0 end-0" style="bottom:40px">
                  <div class="h-100 d-flex align-items-center justify-content-center">
                    <img :src="imgInfo" alt="" class="img-fluid" style="max-height:100%;object-fit:contain;transform: scale(1.18) translateY(3%);filter:drop-shadow(0 8px 12px rgba(0,0,0,.10));opacity:.97" />
                  </div>
                </div>
                <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold fs-5 text-white"
                  style="height:40px">내 정보</div>
              </button>
            </div>
            <div class="col-6" style="margin-top:30px">
              <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
                style="height:180px;background-image:linear-gradient(135deg,#FFD9C3 0%, #FFC19E 60%, #FFAB83 100%);" @click="go('/game')">
                <div class="position-absolute top-0 start-0 end-0" style="bottom:40px">
                  <div class="h-100 d-flex align-items-center justify-content-center">
                    <img :src="imgGame" alt="" class="img-fluid" style="max-height:100%;object-fit:contain;filter:drop-shadow(0 8px 12px rgba(0,0,0,.10));opacity:.97" />
                  </div>
                </div>
                <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold"
                  style="height:40px;color:#232323;font-size:1.25rem">두뇌 게임</div>
              </button>
            </div>
            <div class="col-6" style="margin-top:-10px">
              <button type="button"
                class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
                style="height:220px;background-image:linear-gradient(135deg,#FFE8B5 0%, #FFD27E 60%, #FFC35D 100%);" @click="goToNeighborMain">
                <div class="position-absolute top-0 start-0 end-0" style="bottom:44px">
                  <div class="h-100 d-flex align-items-center justify-content-center">
                    <img :src="imgAi" alt="" class="img-fluid" style="width:130%;max-width:none;height:auto;object-fit:contain; transform:scale(1.05) translateY(15%); filter:drop-shadow(0 8px 12px rgba(0,0,0,.10));opacity:.97" />
                  </div>
                </div>
                <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold"
                  style="height:44px;color:#232323;font-size:1.25rem">
                  만남의광장
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>
      <div class="py-3"></div>
      <MyInfoModal v-model="isMyInfoModalOpen" :user-name="userName" @close="handleMyInfoModalClose" />
      <div v-if="isSafeZoneModalOpen" class="safe-zone-modal-host">
        <PatientSafeZoneModal @close="closeSafeZoneModal" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import MyInfoModal from '@/components/MyinfoModal.vue'
import PatientSafeZoneModal from '@/views/PatientSafeZoneView.vue'

import imgZone from '@/assets/images/My zone.svg'
import imgInfo from '@/assets/images/Myinfo.svg'
import imgGame from '@/assets/images/game.svg'
import imgAi from '@/assets/images/man1.png'

const router = useRouter()
const loading = ref(false)
const err = ref('')
const userName = ref('')
const patientUserNo = ref(null)
const allSchedules = ref([])
const isMyInfoModalOpen = ref(false)
const isSafeZoneModalOpen = ref(false)

let locationUpdateInterval = null
let timeInterval = null

async function request(url, options = {}) {
  const res = await fetch(url, { credentials: 'include', ...options })
  const ct = (res.headers.get('content-type') || '')
  const isJson = ct.includes('application/json')
  const body = isJson ? await res.json().catch(() => ({})) : await res.text().catch(() => '')
  if (!res.ok) throw new Error(typeof body === 'string' ? body : (body.message || `${res.status}`))
  return body
}

async function fetchMe() {
  const me = await request('/api/user/me')
  userName.value = me?.name || me?.username || '사용자'
  patientUserNo.value = me?.userNo ?? me?.id ?? null
}
async function fetchSchedules(userNo) {
  return await request(`/api/schedule/list/${encodeURIComponent(userNo)}`)
}

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

// 모달 관리
function openMyInfoModal() { isMyInfoModalOpen.value = true }
function handleMyInfoModalClose() { isMyInfoModalOpen.value = false }
function openSafeZoneModal() { isSafeZoneModalOpen.value = true }
function closeSafeZoneModal() { isSafeZoneModalOpen.value = false }

// 만남의광장 이동(쿼리 포함, NH에서 환자복귀버튼 표시 위해)
function goToNeighborMain() {
  router.push({ path: '/NH', query: { fromPatient: '1' } })
}
function go(path) { router.push(path) }

// 위치 추적 시작/중지
async function startLocationTracking() {
  try {
    // 위치 추적 시작
    updateLocation() // 즉시 한 번 실행
    locationUpdateInterval = setInterval(() => { updateLocation() }, 30000)
  } catch (error) { console.error(error) }
}

function stopLocationTracking() {
  if (locationUpdateInterval) {
    clearInterval(locationUpdateInterval)
    locationUpdateInterval = null
  }
}

// 현재 위치 업데이트
function updateLocation() {
  if (!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(
    async (position) => {
      try {
        const locationData = {
          userNo: patientUserNo.value,
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          timestamp: Date.now()
        }
        await fetch('/api/location/update', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify(locationData)
        })
      } catch (error) { console.error(error) }
    },
    (error) => { console.error(error.message) },
    { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
  )
}

// 날짜/시간
const currentDate = ref('');
const currentClock = ref('');
function updateTime() {
  const now = new Date();
  const month = now.getMonth() + 1;
  const date = now.getDate();
  const hours = now.getHours();
  const minutes = now.getMinutes();
  const ampm = hours >= 12 ? '오후' : '오전';
  let displayHours = hours % 12;
  if (displayHours === 0) displayHours = 12;
  const paddedHours = String(displayHours).padStart(2, '0');
  const paddedMinutes = String(minutes).padStart(2, '0');
  currentDate.value = `${month}월 ${date}일`;
  currentClock.value = `${ampm} ${paddedHours}시 ${paddedMinutes}분`;
}

onMounted(async () => {
  loading.value = true
  err.value = ''
  try {
    await fetchMe()
    if (patientUserNo.value) {
      const list = await fetchSchedules(patientUserNo.value)
      allSchedules.value = Array.isArray(list) ? list : []
      await startLocationTracking()
    }
  } catch (e) {
    err.value = String(e?.message || e)
    console.warn('[PatientHome] init error:', err.value)
  } finally {
    loading.value = false
  }
  updateTime();
  timeInterval = setInterval(updateTime, 1000);
})

onBeforeUnmount(() => {
  stopLocationTracking()
  if (timeInterval) {
    clearInterval(timeInterval)
    timeInterval = null
  }
})
</script>

<style scoped>
.dp-main-page { height: 600px; max-width: 480px; position: relative; margin-top: -70px; }
.dp-container { position: relative; }
.safe-zone-modal-host { position: absolute; inset: 0; z-index: 100; }
</style>

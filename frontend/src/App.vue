<template>
  <DesktopLayout v-if="isDesktopLayout">
    <RouterView />
  </DesktopLayout>

  <div v-else class="mobile-shell">
    <div class="app-layout">
      <!-- 조건 수정 -->
      <AppHeader v-if="showMobileHeader" />
      <NeighborHeader v-if="isNeighborPage || isPredictLocationFromNeighbor" />
      
      <main class="main-content" :class="mobileMainContentClass">
        <RouterView />
      </main>
      
      <AppFooter v-if="showMobileFooter" />
      <NeighborFooter v-if="isNeighborPage || isPredictLocationFromNeighbor" />
    </div>
  </div>

  <!-- 안심존 이탈 알림 모달 -->
  <SafeZoneAlertModal
    :show="showSafeZoneAlert"
    :patient-name="alertPatientName"
    @close="closeSafeZoneAlert"
  />

  <!-- 문열림 감지 알림 모달 -->
  <DoorOpenAlertModal
    :show="doorOpenAlert"
    :patient-name="alertPatientName"
    @close="closeDoorOpenAlert"
  />

  <!-- 실종자 알림 모달 -->
  <ConfirmModal
    :show="showMissingAlert"
    title="긴급 실종 알림"
    :message="alertMessage"
    confirmText="지금 확인하기"
    cancelText="나중에 확인하기"
    @close="handleCloseAlert"
    @confirm="handleConfirmAndNavigate"
    @cancel="handleCloseAlert"
  />
</template>

<script setup>
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
import NeighborHeader from './components/NeighborHeader.vue';
import NeighborFooter from './components/NeighborFooter.vue';
import SafeZoneAlertModal from './components/SafeZoneAlertModal.vue';
import DoorOpenAlertModal from './components/DoorOpenAlertModal.vue'; // ⭐ 문열림 모달 추가
import { RouterView, useRoute } from 'vue-router'
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useMyCurrentLocation } from '@/composables/useMyCurrentLocation';
import DesktopLayout from './layouts/DesktopLayout.vue'

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-3/dist/bootstrap-vue-3.css'

// ====================
// 실종자 알림 추가 시작
// ====================

import { showMissingAlert, alertMessage, handleConfirmAlert, handleCloseAlert, startAlertPolling } from './composables/useAlertPolling';
import ConfirmModal from './components/ConfirmModal.vue';
import { useRouter } from 'vue-router'

const router = useRouter();

function handleConfirmAndNavigate() {
  handleConfirmAlert();
  router.push({ path: '/communityView', query: { tab: 'Missing' } });
}

// ====================
// 실종자 알림 추가 끝
// ====================
const currentUser = ref(null);
useMyCurrentLocation(computed(() => currentUser.value?.userNo));

const route = useRoute()
const isDesktopLayout = computed(() => route.meta.layout === 'desktop')

// AddSchedule 페이지인지 확인하는 computed 속성
const isAddSchedulePage = computed(() => {
  return route.name === 'add-schedule'
})

// DP_main 페이지인지 확인하는 computed 속성
const isDPMainPage = computed(() => {
  return route.name === 'DP'
})

// MapMain 페이지인지 확인하는 computed 속성
const isMapMainPage = computed(() => {
  return route.name === 'map-main'
})

// Login 페이지인지 확인하는 computed 속성
const isLoginPage = computed(() => {
  return route.name === 'login'
})

// SignUp 페이지인지 확인하는 computed 속성
const isSignUpPage = computed(() => {
  return route.name === 'SignUp'
})

const isDpMypage = computed(() => {
  return route.name === 'dpmypage'
})

const isDpSchedule = computed(() => {
  return route.name === 'DP_schedule'
})


const isDpConnect = computed(() => {
  return route.name === 'dpc'
})

// 이웃 페이지인지 확인하는 computed 속성
const isNeighborPage = computed(() => {
  return route.name?.startsWith('NH')
})

// 실종자 상세정보 입력 페이지
const MissingReport = computed(() => {
  return route.name === 'MissingReport'
})

// 실종자 상세정보(예상위치) 페이지
const PredictLocation = computed(() => {
  return route.name === 'predict-location'
})

// 게시물 페이지
const CommunityPost = computed(() => {
  return route.name === 'CommunityPost'
})

// 게시물 페이지
const CommunityPostWrite = computed(() => {
  return route.name === 'CommunityPostWrite'
})

const PostEdit = computed(() => {
  return route.name === 'PostEdit'
})

const SightingReportBoard = computed(() => {
  return route.name === 'SightingReportBoard'
})

const SightingReport = computed(() => {
  return route.name === 'SightingReport'
})

const SightingReportWrite = computed(() => {
  return route.name === 'SightingReportWrite'
})

const ReportEdit = computed(() => {
  return route.name === 'ReportEdit'
})

// 두뇌 게임
const isGame = computed(() => {
  return route.name === 'game'
})

// GeoFencing 페이지인지 확인하는 computed 속성
const isGeoFencingPage = computed(() => {
  return route.name === 'geo-fencing'
})

// PredictLocation 페이지에서 이웃에서 왔는지 확인
const isPredictLocationFromNeighbor = computed(() => {
  return route.name === 'predict-location' && route.query.source === 'neighbor'
})

const showMobileHeader = computed(() => {
  if (isDesktopLayout.value) return false
  return !(isAddSchedulePage.value ||
    isDPMainPage.value ||
    isMapMainPage.value ||
    isLoginPage.value ||
    isSignUpPage.value ||
    isDpMypage.value ||
    isDpSchedule.value ||
    isDpConnect.value ||
    isNeighborPage.value ||
    isPredictLocationFromNeighbor.value ||
    isGame.value)
})

const showMobileFooter = computed(() => {
  if (isDesktopLayout.value) return false
  return !(isDPMainPage.value ||
    isLoginPage.value ||
    isSignUpPage.value ||
    isDpMypage.value ||
    isDpSchedule.value ||
    isDpConnect.value ||
    isNeighborPage.value ||
    isPredictLocationFromNeighbor.value ||
    isGame.value)
})


const mobileMainContentClass = computed(() => {
  return {
    'no-padding': isMapMainPage.value ||
      isLoginPage.value ||
      isSignUpPage.value ||
      isAddSchedulePage.value ||
      MissingReport.value ||
      PredictLocation.value ||
      CommunityPost.value ||
      CommunityPostWrite.value ||
      PostEdit.value ||
      SightingReportBoard.value ||
      SightingReport.value ||
      SightingReportWrite.value ||
      ReportEdit.value ||
      isGeoFencingPage.value,
    'neighbor-page': isNeighborPage.value
  }
})

/* ===== 안심존 이탈 알림 시스템 ===== */

// 알림 모달 상태
const showSafeZoneAlert = ref(false)
const doorOpenAlert = ref(false)      // ⭐ 문열림 모달 상태 추가
const alertPatientName = ref('')

// 안심존 모니터링 상태
const monitoringInterval = ref(null)
const connectedPatientNo = ref(null)
const patientName = ref('')
const lastSafeZoneStatus = ref(null) // 'inside' | 'outside' | null
const lastSafeZoneData = ref(null) // 이전 안심존 데이터 (변경 감지용)

// 안심존 이탈 알림 닫기
function closeSafeZoneAlert() {
  showSafeZoneAlert.value = false
  alertPatientName.value = ''
}

// 문열림 알림 닫기 ⭐
function closeDoorOpenAlert() {
  doorOpenAlert.value = false
  alertPatientName.value = ''
}

// 현재 로그인한 사용자 정보 가져오기
async function getCurrentUser() {
  try {
    const response = await fetch('/api/user/me', {
      method: 'GET',
      credentials: 'include'
    })

    if (!response.ok) {
      return null
    }

    const user = await response.json()
    return user
  } catch (error) {
    console.error('사용자 정보 조회 오류:', error)
    return null
  }
}

// 환자 정보 확인 (보호자: 연결된 환자, 환자: 자기 자신)
async function checkGuardianPatientConnection(userNo) {
  try {
    const response = await fetch(`/api/user/my-patient`, {
      method: 'GET',
      credentials: 'include'
    })

    if (!response.ok) {
      return null
    }

    const result = await response.json()

    // 메시지만 있는 경우 (보호자인데 연결된 환자가 없는 경우)
    if (result.message) {
      return null
    }

    return {
      patientNo: result.userNo,
      patientName: result.name
    }
  } catch (error) {
    console.error('환자 정보 확인 오류:', error)
    return null
  }
}

// 환자 위치 조회
async function fetchPatientLocation(patientNo) {
  try {
    const response = await fetch(`/api/location/patient/${patientNo}`, {
      method: 'GET',
      credentials: 'include'
    })

    if (!response.ok) {
      return null
    }

    const location = await response.json()

    if (location && location.latitude && location.longitude) {
      return location
    }

    return null
  } catch (error) {
    console.error('환자 위치 조회 오류:', error)
    return null
  }
}

// 일정의 안심존(버퍼) 가져오기
async function fetchScheduleSafeZone(scheduleNo) {
  try {
    const response = await fetch(`/api/schedule/${scheduleNo}/route`, {
      method: 'GET',
      credentials: 'include'
    })

    if (!response.ok) {
      return null
    }

    const route = await response.json()
    if (!route.bufferCoordinates) return null

    const bufferCoordinates = JSON.parse(route.bufferCoordinates)

    // 기존 데이터 호환성 처리
    if (Array.isArray(bufferCoordinates)) {
      return {
        level: 1,
        coordinates: bufferCoordinates
      }
    }

    return bufferCoordinates
  } catch (error) {
    console.error('일정 안심존 조회 오류:', error)
    return null
  }
}

// 기본 안심존 가져오기
async function fetchBasicSafeZone(userNo) {
  try {
    const response = await fetch(`/api/schedule/basic-safe-zone/${userNo}`, {
      method: 'GET',
      credentials: 'include'
    })

    if (!response.ok) {
      return null
    }

    const result = await response.json()

    if (result.message) {
      return null
    }

    return result.boundaryCoordinates ? JSON.parse(result.boundaryCoordinates) : null
  } catch (error) {
    console.error('기본 안심존 조회 오류:', error)
    return null
  }
}

// 현재 진행 중인 일정 찾기
async function getCurrentSchedule(patientNo) {
  try {
    const response = await fetch(`/api/schedule/list/${patientNo}`, {
      method: 'GET',
      credentials: 'include'
    })

    if (!response.ok) {
      return null
    }

    const schedules = await response.json()

    const now = new Date()
    const today = now.toISOString().split('T')[0]
    const currentTime = now.getHours() * 60 + now.getMinutes()

    // 오늘 일정 중 현재 시간에 해당하는 일정 찾기
    const currentSchedule = schedules.find(schedule => {
      if (schedule.scheduleDate !== today) return false

      const startTime = schedule.startTime.split(':').map(Number)
      const endTime = schedule.endTime.split(':').map(Number)
      const startMinutes = startTime[0] * 60 + startTime[1]
      const endMinutes = endTime[0] * 60 + endTime[1]

      return currentTime >= startMinutes && currentTime <= endMinutes
    })

    return currentSchedule
  } catch (error) {
    console.error('현재 일정 조회 오류:', error)
    return null
  }
}

// 두 점 간의 거리 계산 (Haversine 공식)
function calculateDistance(lat1, lng1, lat2, lng2) {
  const R = 6371000 // 지구 반지름 (미터)
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

// 점이 폴리곤 내부에 있는지 판단 (Ray casting 알고리즘)
function isPointInPolygon(lat, lng, polygon) {
  let inside = false
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const xi = polygon[i].longitude
    const yi = polygon[i].latitude
    const xj = polygon[j].longitude
    const yj = polygon[j].latitude

    // Ray casting: 점의 y좌표(위도)가 선분의 y좌표 범위 안에 있고,
    // 점의 x좌표(경도)가 교차점의 x좌표보다 왼쪽에 있으면 교차
    if (((yi > lat) !== (yj > lat)) && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)) {
      inside = !inside
    }
  }
  return inside
}

// 안심존 데이터 비교 (변경 감지용)
function isSafeZoneChanged(oldData, newData) {
  if (!oldData || !newData) return true

  // 기본형 안심존 (Circle) 비교
  if (oldData.type === 'Circle' && newData.type === 'Circle') {
    return (
      oldData.center?.lat !== newData.center?.lat ||
      oldData.center?.lng !== newData.center?.lng ||
      oldData.radius !== newData.radius
    )
  }

  // 경로형 안심존 (폴리곤) 비교 - 좌표 배열의 길이나 첫 번째 좌표 비교
  if (oldData.coordinates && newData.coordinates) {
    const oldCoords = Array.isArray(oldData) ? oldData : oldData.coordinates
    const newCoords = Array.isArray(newData) ? newData : newData.coordinates
    if (oldCoords.length !== newCoords.length) return true
    if (oldCoords.length > 0 && newCoords.length > 0) {
      return (
        oldCoords[0].latitude !== newCoords[0].latitude ||
        oldCoords[0].longitude !== newCoords[0].longitude
      )
    }
  }

  return true // 다른 타입이면 변경된 것으로 간주
}

// 안심존 상태 확인
async function checkSafeZoneStatus(patientNo, patientLocation) {
  if (!patientLocation) {
    return null
  }

  try {
    const patientLat = patientLocation.latitude
    const patientLng = patientLocation.longitude

    // 현재 진행 중인 일정 확인
    const currentSchedule = await getCurrentSchedule(patientNo)
    let safeZoneData = null

    if (currentSchedule) {
      // 현재 일정의 안심존 데이터 가져오기
      safeZoneData = await fetchScheduleSafeZone(currentSchedule.scheduleNo)
    } else {
      // 기본 안심존 데이터 가져오기
      safeZoneData = await fetchBasicSafeZone(patientNo)
    }

    if (!safeZoneData) {
      return null
    }

    // 안심존 데이터 변경 감지
    if (isSafeZoneChanged(lastSafeZoneData.value, safeZoneData)) {
      // 안심존이 변경되었으면 이전 알림 시간 초기화
      const alertKey = `safeZoneAlert_${patientNo}`
      localStorage.removeItem(alertKey)
      lastSafeZoneStatus.value = null // 상태도 초기화하여 첫 감지 시 알림 가능하도록
    }

    // 현재 안심존 데이터 저장
    lastSafeZoneData.value = safeZoneData

    let isInside = false

    if (currentSchedule && safeZoneData.coordinates) {
      // 경로형 안심존 (폴리곤) - 점이 폴리곤 내부에 있는지 판단
      console.log('[경로형 안심존] 판단 시작')
      console.log('- 환자 위치:', { lat: patientLat, lng: patientLng })
      console.log('- 안심존 데이터:', safeZoneData)

      const coordinates = Array.isArray(safeZoneData) ? safeZoneData : safeZoneData.coordinates
      console.log('- coordinates 개수:', coordinates ? coordinates.length : 0)

      isInside = isPointInPolygon(patientLat, patientLng, coordinates)
      console.log('- 판단 결과:', isInside ? '안심존 내부' : '안심존 외부')
    } else if (safeZoneData.type === 'Circle') {
      // 기본형 안심존 (원형) - 중심점과의 거리 계산
      console.log('[원형 안심존] 판단 시작')
      const centerLat = safeZoneData.center.lat
      const centerLng = safeZoneData.center.lng
      const radius = safeZoneData.radius

      const distance = calculateDistance(patientLat, patientLng, centerLat, centerLng)
      isInside = distance <= radius
      console.log(`- 중심점 거리: ${distance.toFixed(2)}m, 반경: ${radius}m, 결과: ${isInside ? '내부' : '외부'}`)
    }

    return isInside ? 'inside' : 'outside'
  } catch (error) {
    console.error('안심존 상태 확인 오류:', error)
    return null
  }
}

// 안심존 모니터링 시작
async function startSafeZoneMonitoring() {
  if (monitoringInterval.value) {
    return
  }

  try {
    console.log('안심존 모니터링 시작 시도...')

    // 현재 로그인한 사용자 정보 가져오기
    const currentUser = await getCurrentUser()
    if (!currentUser) {
      console.warn('[안심존] 사용자 정보를 가져올 수 없습니다.')
      return
    }

    console.log('현재 사용자:', currentUser)

    // 모니터링할 환자 정보 확인 (보호자: 연결된 환자, 환자: 자기 자신)
    const connection = await checkGuardianPatientConnection(currentUser.userNo)
    if (!connection) {
      console.warn('[안심존] 모니터링할 환자 정보를 가져올 수 없습니다.')
      return
    }

    console.log('모니터링 대상 환자:', connection)
    connectedPatientNo.value = connection.patientNo
    patientName.value = connection.patientName

    console.log(`안심존 모니터링 시작: ${patientName.value} (${connectedPatientNo.value})`)

    // 즉시 한 번 확인
    await checkAndAlert()

    // 20초마다 확인
    monitoringInterval.value = setInterval(async () => {
      await checkAndAlert()
    }, 20000) // 20초

  } catch (error) {
    console.error('[안심존] 모니터링 시작 오류:', error)
  }
}

// 안심존 상태 확인 및 알림 처리
async function checkAndAlert() {
  if (!connectedPatientNo.value) {
    stopSafeZoneMonitoring()
    return
  }

  try {
    console.log('환자 위치 조회 중...', connectedPatientNo.value)

    // 환자 위치 조회
    const patientLocation = await fetchPatientLocation(connectedPatientNo.value)
    if (!patientLocation) {
      return
    }

    console.log('환자 위치:', patientLocation)

    // 안심존 상태 확인
    const currentStatus = await checkSafeZoneStatus(connectedPatientNo.value, patientLocation)
    if (currentStatus === null) {
      return
    }

    console.log('현재 안심존 상태:', currentStatus, '이전 상태:', lastSafeZoneStatus.value)

    // 상태 변화 감지
    if (lastSafeZoneStatus.value !== null) {
      // 안심존에 들어왔을 때 상태 초기화
      if (lastSafeZoneStatus.value === 'outside' && currentStatus === 'inside') {
        console.log(`${patientName.value}님이 안심존에 들어왔습니다.`)
        lastSafeZoneStatus.value = 'inside'
        return
      }

      // 안심존을 벗어났을 때 알림
      if (lastSafeZoneStatus.value === 'inside' && currentStatus === 'outside') {
        console.log(`${patientName.value}님이 안심존을 벗어났습니다.`)

        // localStorage에서 이전 알림 상태 확인
        const alertKey = `safeZoneAlert_${connectedPatientNo.value}`
        const lastAlertTime = localStorage.getItem(alertKey)
        const now = Date.now()

        // 5분 이내에 이미 알림을 보냈다면 중복 방지
        if (lastAlertTime && (now - parseInt(lastAlertTime)) < 300000) {
          lastSafeZoneStatus.value = 'outside'
          return
        }

        // 알림 표시
        alertPatientName.value = patientName.value
        showSafeZoneAlert.value = true

        // 알림 시간 저장
        localStorage.setItem(alertKey, now.toString())

        lastSafeZoneStatus.value = 'outside'
        return
      }
    }

    // 첫 번째 상태 설정
    lastSafeZoneStatus.value = currentStatus

  } catch (error) {
    console.error('[안심존] 상태 확인 오류:', error)
  }
}

// 안심존 모니터링 중지
function stopSafeZoneMonitoring() {
  if (monitoringInterval.value) {
    clearInterval(monitoringInterval.value)
    monitoringInterval.value = null
  }

  connectedPatientNo.value = null
  patientName.value = ''
  lastSafeZoneStatus.value = null
  lastSafeZoneData.value = null
}

// ==========================================================
// [[주형]] 아두이노 움직임 센서 + 키보드 단축키
// ==========================================================
let intervalId = null

// Alt + 1 → 문열림 모달, Alt + 2 → 안심존 이탈 모달
function handleKeydown(event) {
  const isOneKey =
    event.key === '1' ||
    event.code === 'Digit1' ||
    event.code === 'Numpad1'

  const isTwoKey =
    event.key === '2' ||
    event.code === 'Digit2' ||
    event.code === 'Numpad2'

  // Alt 키 아니면 무시
  if (!event.altKey) return

  event.preventDefault()

  // 환자 이름 세팅 (없으면 빈 문자열)
  alertPatientName.value = patientName.value || ''

  if (isOneKey) {
    // 문열림 감지 모달 열기
    doorOpenAlert.value = true
  } else if (isTwoKey) {
    // 안심존 이탈 알림 모달 열기
    showSafeZoneAlert.value = true
  }
}

const checkMovement = async () => {
  try {
    if (!connectedPatientNo.value) {
      return
    }
    const res = await fetch(`${import.meta.env.VITE_FASTAPI_URL || 'http://localhost:8000'}/sensor`)
    const data = await res.json()
    if (data.pir === 1) {
      console.log(`움직임 감지됨`)
      alertPatientName.value = patientName.value
      doorOpenAlert.value = true;
      return
    }
  } catch (e) {
    console.error('Error fetching sensor data:', e)
  }
}

// 컴포넌트 마운트 시 모니터링 시작
onMounted(async () => {
  // 키보드 단축키 등록 (Alt+1 / Alt+2)
  window.addEventListener('keydown', handleKeydown)

  // 로그인 페이지가 아닐 때만 모니터링 시작
  if (!isLoginPage.value && !isSignUpPage.value) {
    // 약간의 지연을 주어 인증 상태가 완전히 설정되도록 함
    setTimeout(async () => {
      // ⭐ 4. (중요) 안심존 모니터링 전에 사용자 정보부터 가져와서 ref에 채웁니다.
      currentUser.value = await getCurrentUser();
      await startSafeZoneMonitoring()
      startAlertPolling(currentUser.value?.role); // 실종알림
    }, 500)
  }
  checkMovement()
  intervalId = setInterval(checkMovement, 1500000000000000000000000)
})

// 컴포넌트 언마운트 시 모니터링 중지
onBeforeUnmount(() => {
  stopSafeZoneMonitoring()

  // 종료시 움직임센서도 죽어
  clearInterval(intervalId)
  document.body.classList.remove('desktop-mode', 'mobile-mode')

  // 키보드 단축키 제거
  window.removeEventListener('keydown', handleKeydown)
})

// 라우트 변경 감지
const prevRoute = ref(null)
watch(route, async (newRoute, oldRoute) => {
  // 로그인/회원가입 페이지로 이동할 때 모니터링 중지
  if (newRoute.name === 'login' || newRoute.name === 'SignUp') {
    stopSafeZoneMonitoring()
    prevRoute.value = newRoute
    return
  }

  // 로그인/회원가입 페이지에서 다른 페이지로 이동할 때 모니터링 시작 및 currentUser 갱신
  if (oldRoute && (oldRoute.name === 'login' || oldRoute.name === 'SignUp')) {
    setTimeout(async () => {
      // ⭐ currentUser를 먼저 갱신 (useMyCurrentLocation이 올바른 userNo를 받도록)
      currentUser.value = await getCurrentUser();
      console.log('[App.vue] 로그인 후 currentUser 갱신:', currentUser.value);
      
      await startSafeZoneMonitoring()
      startAlertPolling(currentUser.value?.role);

    }, 500)
    prevRoute.value = newRoute
    return
  }

  // 다른 경우에도 모니터링이 중지되어 있다면 재시작 시도
  // (예: API 오류로 중단되었거나, 수동으로 중지된 경우)
  if (!monitoringInterval.value && newRoute.name !== 'login' && newRoute.name !== 'SignUp') {
    setTimeout(async () => {
      await startSafeZoneMonitoring()
    }, 1000) // 1초 지연
  }

  prevRoute.value = newRoute
}, { immediate: false })

watch(isDesktopLayout, (isDesktop) => {
  document.body.classList.toggle('desktop-mode', isDesktop)
  document.body.classList.toggle('mobile-mode', !isDesktop)
}, { immediate: true })
</script>

<style>
body {
  margin: 0;
  padding: 0;
}

body.mobile-mode {
  background-color: #808080;
  overflow: hidden;
}

body.desktop-mode {
  background-color: #f5f6f8;
  overflow: auto;
}
</style>

<style scoped>
.mobile-shell {
  min-height: 100vh;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #808080;
  padding: 20px 0;
}

.app-layout {
  width: 375px;
  height: 812px;
  background-color: #FFFFFF;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.3);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 90px 5px 80px 5px;
  scrollbar-width: none;
  /* 스크롤바 숨기기 */
}

/* padding 제거가 필요한 페이지 */
.main-content.no-padding {
  padding: 0;
  overflow: hidden;
}

/* 이웃 페이지 - 하단 padding 제거 */
.main-content.neighbor-page {
  padding: 90px 5px 5px 5px;
}
</style>

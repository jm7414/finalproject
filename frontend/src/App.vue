<template>
  <div class="mobile-frame">
    <div class="app-layout">
      <AppHeader v-if="!(isAddSchedulePage || isDPMainPage || isMapMainPage || isLoginPage || isSignUpPage || isDpMypage || isDpSchedule || isDpConnect)" />
      <main class="main-content" :class="{ 'no-padding': isMapMainPage || isLoginPage || isSignUpPage || isBasicSafeZoneLocationPage || isAddSchedulePage}">
        <RouterView />
      </main>
      <AppFooter v-if="!(isDPMainPage || isLoginPage || isSignUpPage || isDpMypage || isDpSchedule || isDpConnect)" />
    </div>
    
  </div>
  
  <!-- 안심존 이탈 알림 모달 (mobile-frame 밖으로 이동) -->
  <SafeZoneAlertModal 
    :show="showSafeZoneAlert"
    :patient-name="alertPatientName"
    @close="closeSafeZoneAlert"
  />
</template>

<script setup>
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
import SafeZoneAlertModal from './components/SafeZoneAlertModal.vue';
import { RouterView, useRoute } from 'vue-router'
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-3/dist/bootstrap-vue-3.css'

const route = useRoute()

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

const isBasicSafeZoneLocationPage = computed(() => {
  return route.name === 'basic-safe-zone-location'
})

const isDpConnect = computed(() => {
  return route.name === 'dpc'
})

/* ===== 안심존 이탈 알림 시스템 ===== */

// 알림 모달 상태
const showSafeZoneAlert = ref(false)
const alertPatientName = ref('')

// 안심존 모니터링 상태
const monitoringInterval = ref(null)
const connectedPatientNo = ref(null)
const patientName = ref('')
const lastSafeZoneStatus = ref(null) // 'inside' | 'outside' | null

// 안심존 이탈 알림 닫기
function closeSafeZoneAlert() {
  showSafeZoneAlert.value = false
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

// 보호자-환자 연결 관계 확인
async function checkGuardianPatientConnection(guardianNo) {
  try {
    const response = await fetch(`/api/user/my-patient`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      return null
    }
    
    const result = await response.json()
    
    // 메시지만 있는 경우 (환자가 없는 경우)
    if (result.message) {
      return null
    }
    
    return {
      patientNo: result.userNo,
      patientName: result.name
    }
  } catch (error) {
    console.error('환자 연결 관계 확인 오류:', error)
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
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLng/2) * Math.sin(dLng/2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
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
    
    if (((yi > lng) !== (yj > lng)) && (lat < (xj - xi) * (lng - yi) / (yj - yi) + xi)) {
      inside = !inside
    }
  }
  return inside
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
    
    let isInside = false
    
    if (currentSchedule && safeZoneData.coordinates) {
      // 경로형 안심존 (폴리곤) - 점이 폴리곤 내부에 있는지 판단
      const coordinates = Array.isArray(safeZoneData) ? safeZoneData : safeZoneData.coordinates
      isInside = isPointInPolygon(patientLat, patientLng, coordinates)
    } else if (safeZoneData.type === 'Circle') {
      // 기본형 안심존 (원형) - 중심점과의 거리 계산
      const centerLat = safeZoneData.center.lat
      const centerLng = safeZoneData.center.lng
      const radius = safeZoneData.radius
      
      const distance = calculateDistance(patientLat, patientLng, centerLat, centerLng)
      isInside = distance <= radius
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
      return
    }
    
    console.log('현재 사용자:', currentUser)
    
    // 보호자-환자 연결 관계 확인
    const connection = await checkGuardianPatientConnection(currentUser.userNo)
    if (!connection) {
      return
    }
    
    console.log('환자 연결 정보:', connection)
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
    console.error('안심존 모니터링 시작 오류:', error)
  }
}

// 안심존 상태 확인 및 알림 처리
async function checkAndAlert() {
  if (!connectedPatientNo.value) {
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
    console.error('안심존 상태 확인 및 알림 처리 오류:', error)
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
}

// 컴포넌트 마운트 시 모니터링 시작
onMounted(async () => {
  // 로그인 페이지가 아닐 때만 모니터링 시작
  if (!isLoginPage.value && !isSignUpPage.value) {
    await startSafeZoneMonitoring()
  }
})

// 컴포넌트 언마운트 시 모니터링 중지
onBeforeUnmount(() => {
  stopSafeZoneMonitoring()
})

// 라우트 변경 감지
const prevRoute = ref(null)
watch(route, (newRoute, oldRoute) => {
  // 로그인/회원가입 페이지에서 다른 페이지로 이동할 때 모니터링 시작
  if (oldRoute && (oldRoute.name === 'login' || oldRoute.name === 'SignUp')) {
    if (newRoute.name !== 'login' && newRoute.name !== 'SignUp') {
      startSafeZoneMonitoring()
    }
  }
  
  // 다른 페이지에서 로그인/회원가입 페이지로 이동할 때 모니터링 중지
  if (oldRoute && oldRoute.name !== 'login' && oldRoute.name !== 'SignUp') {
    if (newRoute.name === 'login' || newRoute.name === 'SignUp') {
      stopSafeZoneMonitoring()
    }
  }
  
  prevRoute.value = newRoute
})
</script>

<style>
/* 모바일 프레임 외부 (회색 배경) */
body {
  margin: 0;
  padding: 0;
  background-color: #808080;
  overflow: hidden;
}

/* 375px × 812px 고정 프레임 */
.mobile-frame {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 375px;
  height: 812px;
  background-color: #FFFFFF;
  overflow: hidden;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.3);
}

.app-layout {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}
</style>

<style scoped>
.main-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 90px 5px 80px 5px;
}

/* padding 제거가 필요한 페이지 */
.main-content.no-padding {
  padding: 0;
  overflow: hidden;
}
</style>
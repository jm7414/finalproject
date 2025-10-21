<!-- src/views/Pr.vue -->
<template>
  <div class="container-sm py-3" style="max-width:414px; position:relative;">

    <!-- 🔴 히든 토글: 아주 작은 동그라미(우상단) -->
    <button class="position-absolute rounded-circle border-0" @click="toggleTestEvent" aria-label="테스트 이벤트 토글"
      title="테스트 이벤트" style="
        top:6px; right:6px; width:18px; height:18px;
        background:#ff4d4f; opacity:.65; z-index:50;
        box-shadow:0 0 0 1px rgba(0,0,0,.08);
      ">
    </button>

    <!-- (요청) 실종 제보 카드: 상태 문구보다 위로 이동 -->
    <div v-if="missingEvent" class="card border-0 shadow-sm mb-3">
      <div class="row g-3 align-items-center p-3">
        <div class="col-auto">
          <img v-if="missingEvent.avatarUrl" :src="missingEvent.avatarUrl" alt="face" class="rounded"
            style="width:56px;height:56px;object-fit:cover">
          <div v-else class="rounded-circle d-flex align-items-center justify-content-center bg-light border"
            style="width:56px;height:56px;font-size:28px;line-height:1">👤</div>
        </div>
        <div class="col">
          <div class="small fw-semibold">
            {{ missingEvent.name }} <span v-if="missingEvent.age">({{ missingEvent.age }})</span>
          </div>
          <div class="small text-secondary" v-if="missingEvent.location">실종 위치 : {{ missingEvent.location }}</div>
          <div class="small text-secondary" v-if="missingEvent.time">실종 시간 : {{ missingEvent.time }}</div>
        </div>
        <div class="col-12">
          <button class="btn btn-outline-dark w-100 rounded-pill" @click="goToMapMain">지도에서 보기</button>
        </div>
      </div>
    </div>

    <!-- 상단 헤더: 이벤트 발생시에만 노출 -->
    <div class="d-flex align-items-center mb-2" v-if="missingEvent">
      <h5 class="fw-bold m-0 me-auto">
        <span class="text-dark">{{ patient.name || '환자' }}</span>의 이웃을 찾아요
      </h5>
    </div>

    <!-- 상태 문구 -->
    <div class="my-3">
      <template v-if="connected">
        <div class="fs-5 fw-semibold mb-1">
          <span class="text-success fw-semibold">{{ patient.name }}</span>님은
        </div>
        <div class="fs-5 fw-semibold">
          <span :class="safeStatus.safe ? 'text-body' : 'text-danger'">
            {{ safeStatus.safe ? '안전한 위치에 있습니다.' : '주의 구역에 있습니다.' }}
          </span>
          <span v-if="safeStatus.checkedAt" class="text-secondary small ms-1">({{ safeStatus.checkedAt }} 기준)</span>
        </div>
      </template>
      <template v-else>
        <div class="fs-5 fw-semibold">연결한 환자가 없습니다.</div>
      </template>
    </div>

    <!-- Kakao 지도 프리뷰 -->
    <div class="card border-0 shadow-sm position-relative overflow-hidden mb-4 rounded-4">
      <div ref="mapEl" class="w-100" style="height:280px;"></div>
      <!-- 항상 노출 -->
      <button class="btn btn-light rounded-pill position-absolute start-50 translate-middle-x"
        style="bottom:12px; z-index:10; pointer-events:auto" @click="goToMapMain">
        지도 자세히 보기
      </button>
    </div>

    <!-- 가장 빠른 일정 -->
    <h6 class="fw-bold mb-2">가장 빠른 일정</h6>
    <div v-if="nextSchedule" class="card border-2 rounded-3 p-3 mb-2" style="border-color:#e9ecef">
      <div class="d-flex justify-content-between align-items-center mb-1">
        <div class="d-flex align-items-center gap-2">
          <span class="d-inline-block rounded-circle" style="width:10px;height:10px;background:#6c757d"></span>
          <span class="fw-semibold">{{ nextSchedule.title }}</span>
        </div>
        <span class="text-secondary">{{ nextSchedule.time }}</span>
      </div>
      <div class="small text-secondary mb-1">{{ nextSchedule.location || '위치 정보 없음' }}</div>
      <div class="small text-secondary" v-if="nextSchedule.depart">예상 출발: {{ nextSchedule.depart }}</div>
      <div class="small text-secondary" v-if="nextSchedule.arrive">예상 도착: {{ nextSchedule.arrive }}</div>
    </div>
    <div v-else class="card border-0 shadow-sm rounded-4 mb-2">
      <div class="card-body text-center text-muted">오늘 남은 일정이 없습니다.</div>
    </div>

    <button class="btn btn-outline-dark w-100 rounded-pill mb-3" @click="router.push('/calendar')">
      일정 자세히 보기
    </button>

    <!-- 기능 타일 -->
    <div class="row g-3 align-items-stretch">
      <!-- 1) 기본 안심존 설정 -->
      <div class="col-6">
        <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
          @click="router.push({ name: 'basic-safe-zone-location' })" style="height:220px;
                       background-image:
                         linear-gradient(0deg, rgba(255,255,255,.38) 0%, rgba(255,255,255,.18) 45%, rgba(255,255,255,0) 75%),
                         linear-gradient(135deg,#6f82ff 0%,#576cff 55%,#475cff 100%);">
          <div class="position-absolute top-0 start-0 end-0 d-flex align-items-center justify-content-center"
            style="bottom:44px">
            <img :src="zone1" alt="" draggable="false"
              style="height:100%;max-height:100%;width:auto;object-fit:contain;transform:scale(1.14) translateY(2%);">
          </div>
          <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold text-white"
            style="height:44px">기본 안심존 설정</div>
        </button>
      </div>

      <!-- 2) 예상 위치 -->
      <div class="col-6">
        <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
          @click="router.push('/predict-location')" style="height:196px;
                       background-image:
                         linear-gradient(0deg, rgba(255,255,255,.35) 0%, rgba(255,255,255,.16) 45%, rgba(255,255,255,0) 75%),
                         linear-gradient(135deg,#ff7b64 0%,#ff5a42 60%,#ff3f2e 100%);">
          <div class="position-absolute top-0 start-0 end-0 d-flex align-items-center justify-content-center"
            style="bottom:40px">
            <img :src="locationIcon" alt="" draggable="false"
              style="height:65%;max-height:100%;width:auto;object-fit:contain;transform:scale(1.12);">
          </div>
          <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold text-white"
            style="height:40px">예상 위치</div>
        </button>
      </div>

      <!-- 3) AI 보고서 -->
      <div class="col-6">
        <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
          @click="router.push('/report')" style="height:196px;
                       background-image:
                         linear-gradient(0deg, rgba(255,255,255,.32) 0%, rgba(255,255,255,.14) 45%, rgba(255,255,255,0) 75%),
                         linear-gradient(135deg,#ffd6b9 0%,#ffb487 62%,#ff965f 100%);">
          <div class="position-absolute top-0 start-0 end-0 d-flex align-items-center justify-content-center"
            style="bottom:40px">
            <img :src="report2" alt="" draggable="false" class="position-absolute top-50 start-50 translate-middle"
              style="height:132%;max-height:none;width:auto;object-fit:contain;transform:translate(-50%,-56%);">
          </div>
          <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold"
            style="height:40px;color:#232323">AI 보고서</div>
        </button>
      </div>

      <!-- 4) 환자 연결 관리 -->
      <div class="col-6" style="margin-top:-8px">
        <button type="button" class="btn p-0 w-100 border-0 rounded-4 shadow-sm position-relative overflow-hidden"
          @click="router.push('/gdc')" style="height:220px;
                       background-image:
                         linear-gradient(0deg, rgba(255,255,255,.34) 0%, rgba(255,255,255,.16) 45%, rgba(255,255,255,0) 75%),
                         linear-gradient(135deg,#ffe08f 0%,#ffc050 60%,#ffae2a 100%);">
          <div class="position-absolute top-0 start-0 end-0 d-flex align-items-center justify-content-center"
            style="bottom:44px">
            <img :src="connectIcon" alt="" draggable="false"
              style="height:100%;max-height:100%;width:auto;object-fit:contain;transform:scale(1.14) translateY(6%);">
          </div>
          <div class="position-absolute bottom-0 start-0 end-0 d-flex align-items-end px-3 pb-2 fw-bold"
            style="height:44px;color:#353535">환자 연결 관리</div>
        </button>
      </div>
    </div>

    <!-- 에러 토스트 -->
    <div v-if="err" class="alert alert-warning mt-3" role="alert" style="white-space:pre-wrap">
      {{ err }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

import zone1 from '@/assets/images/zone 1.svg'
import locationIcon from '@/assets/images/location.svg'
import report2 from '@/assets/images/report2.png'
import connectIcon from '@/assets/images/connect.svg'

const router = useRouter()

/* ===== API ===== */
const ENDPOINTS = {
  myPatient: '/api/user/my-patient',
  patientByNo: (no) => `/api/user/${no}`,
  lastRecord: (no) => `/api/record/last/${no}`,
  schedules: (no) => `/api/schedule/list/${no}`,
  scheduleLocations: (sn) => `/api/schedule/${sn}/locations`,
  basicSafeZone: (no) => `/api/schedule/basic-safe-zone/${no}`,
  activeMissing: (no) => `/api/missing/active/${no}`,
}

/* ===== 상태 ===== */
const connected = ref(false)
const err = ref('')
const mapEl = ref(null)

const patient = ref({ userNo: null, name: '', avatarUrl: null })
const missingEvent = ref(null)

const safeStatus = ref({ safe: true, checkedAt: '' })
const lastLocation = ref(null)
const allSchedules = ref([])
const scheduleLocationsMap = ref({})

// 안심존 관련
let currentSafeZone = null // 현재 표시 중인 안심존 폴리곤/원형

/* ===== 유틸 ===== */
function fmtTime(hhmm) {
  if (!hhmm) return ''
  const [h, m] = hhmm.split(':').map(Number)
  const isPM = h >= 12
  const hour12 = h % 12 || 12
  return `${isPM ? '오후' : '오전'} ${String(hour12).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}
function timeToMin(hhmm) { const [h, m] = hhmm.split(':').map(Number); return h * 60 + m }
function formatLocation(scheduleNo) {
  const arr = scheduleLocationsMap.value[scheduleNo]
  if (!arr || arr.length === 0) return ''
  const sorted = [...arr].sort((a, b) => a.sequenceOrder - b.sequenceOrder)
  return sorted.map(v => v.locationName).join(' → ')
}
function tsToLocal(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}
function haversine(lat1, lon1, lat2, lon2) {
  const R = 6371000
  const toRad = d => d * Math.PI / 180
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLon / 2) ** 2
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}
async function fetchJSON(url) {
  const res = await fetch(url, { method: 'GET', credentials: 'include' })
  if (!res.ok) throw new Error(`${url} → ${res.status}`)
  return res.json()
}

/* ===== 데이터 로딩 ===== */
async function getMyPatientNoAndProfile() {
  const r = await fetchJSON(ENDPOINTS.myPatient).catch(() => ({}))
  if (r?.message) { connected.value = false; return null }
  const userNo = r?.userNo ?? r?.id ?? r
  connected.value = !!userNo
  if (!connected.value) return null

  if (!r?.name) {
    try {
      const u = await fetchJSON(ENDPOINTS.patientByNo(userNo))
      patient.value = { userNo, name: u?.name || '', avatarUrl: u?.profilePhoto || null }
    } catch {
      patient.value = { userNo, name: '', avatarUrl: null }
    }
  } else {
    patient.value = { userNo, name: r.name || '', avatarUrl: r.profilePhoto || null }
  }
  return userNo
}
async function getLastLocation(no) {
  try {
    const r = await fetchJSON(ENDPOINTS.lastRecord(no))
    if (r?.lat && r?.lng) lastLocation.value = { lat: r.lat, lng: r.lng, ts: r.ts }
  } catch { }
}
async function getSchedules(no) {
  const list = await fetchJSON(ENDPOINTS.schedules(no)).catch(() => [])
  allSchedules.value = Array.isArray(list) ? list : []
  for (const s of allSchedules.value) {
    try {
      const locs = await fetchJSON(ENDPOINTS.scheduleLocations(s.scheduleNo))
      scheduleLocationsMap.value[s.scheduleNo] = Array.isArray(locs) ? locs : []
    } catch {
      scheduleLocationsMap.value[s.scheduleNo] = []
    }
  }
}
async function getBasicSafeCheck(no) {
  try {
    const r = await fetchJSON(ENDPOINTS.basicSafeZone(no))
    if (r?.message) return
    const data = r?.boundaryCoordinates ? JSON.parse(r.boundaryCoordinates) : r
    if (data?.type === 'Circle' && lastLocation.value) {
      const d = haversine(lastLocation.value.lat, lastLocation.value.lng, data.center.lat, data.center.lng)
      safeStatus.value.safe = d <= (data.radius || 0)
      safeStatus.value.checkedAt = tsToLocal(lastLocation.value.ts) || tsToLocal(new Date().toISOString())
    }
  } catch { }
}
async function getActiveMissing(no) {
  try {
    const r = await fetchJSON(ENDPOINTS.activeMissing(no))
    if (r?.message) { missingEvent.value = null }
    else if (r) {
      missingEvent.value = {
        name: r.name ?? patient.value.name ?? '환자',
        age: r.age ?? '',
        location: r.location ?? '',
        time: r.time ?? tsToLocal(r.ts) ?? '',
        avatarUrl: r.avatarUrl ?? null
      }
    }
  } catch { missingEvent.value = null }
}

/* ===== Kakao Map ===== */
const kakaoKey = import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'
const defaultCenter = { lat: 37.4943524920695, lng: 126.88767655688868 }
let map, marker
function loadKakao(key) {
  return new Promise((resolve, reject) => {
    if (!key) return reject(new Error('Kakao JavaScript 키가 비어 있습니다.'))
    if (window.kakao?.maps) return resolve(window.kakao)
    let s = document.querySelector('script[data-kakao-sdk]')
    if (!s) {
      s = document.createElement('script')
      s.setAttribute('data-kakao-sdk', 'true')
      s.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${key}&autoload=false&libraries=services`
      s.async = true
      s.onerror = () => reject(new Error('Kakao SDK 로드 실패'))
      document.head.appendChild(s)
    }
    s.addEventListener('load', () => {
      if (!window.kakao?.maps) return reject(new Error('kakao 객체 미탑재'))
      window.kakao.maps.load(() => resolve(window.kakao))
    }, { once: true })
  })
}
async function initMap() {
  try {
    const kakao = await loadKakao(kakaoKey)
    const c = new kakao.maps.LatLng(
      lastLocation.value?.lat ?? defaultCenter.lat,
      lastLocation.value?.lng ?? defaultCenter.lng
    )
    map = new kakao.maps.Map(mapEl.value, { center: c, level: 3 })
    marker = new kakao.maps.Marker({ position: c })
    marker.setMap(map)
    
    // 안심존 표시
    await updateSafeZone(map)
  } catch (e) { console.error('[Pr] Kakao Map Error:', e?.message || e) }
}
function updateMarker() {
  if (!map || !marker || !lastLocation.value) return
  const kakao = window.kakao
  const c = new kakao.maps.LatLng(lastLocation.value.lat, lastLocation.value.lng)
  marker.setPosition(c); map.setCenter(c)
  
  // 마커 업데이트 시 안심존도 함께 업데이트
  updateSafeZone(map)
}

/* ===== 안심존 관련 함수들 ===== */
// 현재 진행 중인 일정 찾기
function getCurrentSchedule() {
  const now = new Date()
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  const todayKey = `${year}-${month}-${day}`
  
  // 오늘 일정만 필터링
  const todaySchedules = allSchedules.value.filter(schedule => schedule.scheduleDate === todayKey)
  
  // 현재 시간
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()
  const currentTimeInMinutes = currentHour * 60 + currentMinute
  
  // 현재 시간에 해당하는 일정들 모두 찾기
  const currentSchedules = []
  
  for (const schedule of todaySchedules) {
    const [startHour, startMinute] = schedule.startTime.split(':').map(Number)
    const [endHour, endMinute] = schedule.endTime.split(':').map(Number)
    
    const startTimeInMinutes = startHour * 60 + startMinute
    const endTimeInMinutes = endHour * 60 + endMinute
    
    // 현재 시간이 일정 시간 범위 안에 있는지 확인
    if (currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes <= endTimeInMinutes) {
      currentSchedules.push(schedule)
    }
  }
  
  // 일정이 없으면 null 반환
  if (currentSchedules.length === 0) return null
  
  // 일정이 여러 개 겹치면 시작 시간이 가장 빠른 것 선택
  if (currentSchedules.length > 1) {
    console.warn(`⚠️ ${currentSchedules.length}개의 일정이 현재 시간에 겹칩니다. 가장 먼저 시작된 일정을 표시합니다.`)
    currentSchedules.forEach(s => {
      console.log(`  - ${s.scheduleTitle} (${s.startTime} ~ ${s.endTime})`)
    })
  }
  
  // 시작 시간 기준으로 정렬 후 첫 번째 반환
  return currentSchedules.sort((a, b) => 
    a.startTime.localeCompare(b.startTime)
  )[0]
}

// 일정의 안심존(버퍼) 가져오기
async function fetchScheduleSafeZone(scheduleNo) {
  try {
    const response = await fetch(`/api/schedule/${scheduleNo}/route`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (!response.ok) {
      throw new Error('경로 정보를 가져올 수 없습니다.')
    }
    
    const route = await response.json()
    return route.bufferCoordinates ? JSON.parse(route.bufferCoordinates) : null
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
      throw new Error('기본 안심존을 가져올 수 없습니다.')
    }
    
    const result = await response.json()
    
    // 메시지만 있는 경우 (기본 안심존이 설정되지 않은 경우)
    if (result.message) {
      console.warn(result.message)
      return null
    }
    
    return result.boundaryCoordinates ? JSON.parse(result.boundaryCoordinates) : null
  } catch (error) {
    console.error('기본 안심존 조회 오류:', error)
    return null
  }
}

// 지도에 경로형 안심존(버퍼 폴리곤) 그리기
function drawScheduleSafeZone(map, bufferCoordinates) {
  if (!map || !bufferCoordinates) return
  
  try {
    // 기존 안심존 제거
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
    }
    
    // bufferCoordinates는 [{ latitude, longitude }, ...] 형식
    const kakaoPath = bufferCoordinates.map(coord => 
      new window.kakao.maps.LatLng(coord.latitude, coord.longitude)
    )
    
    // 폴리곤 생성
    currentSafeZone = new window.kakao.maps.Polygon({
      path: kakaoPath,
      strokeWeight: 2,
      strokeColor: '#EF4444',
      strokeOpacity: 0.8,
      fillColor: '#EF4444',
      fillOpacity: 0.3
    })
    
    currentSafeZone.setMap(map)
    
    // 안심존이 보이도록 지도 범위 조정
    const bounds = new window.kakao.maps.LatLngBounds()
    kakaoPath.forEach(latLng => bounds.extend(latLng))
    map.setBounds(bounds)
    
    console.log('경로형 안심존 표시 완료')
  } catch (error) {
    console.error('경로형 안심존 표시 오류:', error)
  }
}

// 지도에 기본형 안심존(원형) 그리기
function drawBasicSafeZone(map, boundaryData) {
  if (!map || !boundaryData) return
  
  try {
    // 기존 안심존 제거
    if (currentSafeZone) {
      currentSafeZone.setMap(null)
    }
    
    // boundaryData 구조: { type: 'Circle', center: { lat, lng }, radius, ... }
    if (boundaryData.type === 'Circle') {
      const center = new window.kakao.maps.LatLng(boundaryData.center.lat, boundaryData.center.lng)
      const radius = boundaryData.radius
      
      // 원형 폴리곤 생성 (Turf.js 없이 직접 계산)
      const circlePoints = []
      const steps = 64
      const earthRadius = 6371000 // 지구 반경 (미터)
      
      for (let i = 0; i < steps; i++) {
        const angle = (Math.PI * 2 * i) / steps
        const dx = radius * Math.cos(angle)
        const dy = radius * Math.sin(angle)
        
        const lat = boundaryData.center.lat + (dy / earthRadius) * (180 / Math.PI)
        const lng = boundaryData.center.lng + (dx / earthRadius) * (180 / Math.PI) / Math.cos(boundaryData.center.lat * Math.PI / 180)
        
        circlePoints.push(new window.kakao.maps.LatLng(lat, lng))
      }
      
      // 폴리곤 생성
      currentSafeZone = new window.kakao.maps.Polygon({
        path: circlePoints,
        strokeWeight: 3,
        strokeColor: '#6366f1',
        strokeOpacity: 0.8,
        fillColor: '#6366f1',
        fillOpacity: 0.2
      })
      
      currentSafeZone.setMap(map)
      
      // 지도 레벨 조정
      const bounds = new window.kakao.maps.LatLngBounds()
      circlePoints.forEach(point => bounds.extend(point))
      map.setBounds(bounds)
      
      console.log('기본형 안심존 표시 완료')
    }
  } catch (error) {
    console.error('기본형 안심존 표시 오류:', error)
  }
}

// 안심존 업데이트 (현재 일정에 따라)
async function updateSafeZone(map) {
  if (!map || !patient.value.userNo) return
  
  try {
    // 1. 현재 진행 중인 일정 찾기
    const currentSchedule = getCurrentSchedule()
    
    if (currentSchedule) {
      // 2. 진행 중인 일정이 있으면 해당 일정의 안심존 표시
      console.log('현재 진행 중인 일정:', currentSchedule.scheduleTitle)
      const bufferCoordinates = await fetchScheduleSafeZone(currentSchedule.scheduleNo)
      
      if (bufferCoordinates && bufferCoordinates.length > 0) {
        drawScheduleSafeZone(map, bufferCoordinates)
        return
      }
    }
    
    // 3. 진행 중인 일정이 없거나 안심존이 없으면 기본 안심존 표시
    console.log('기본 안심존 표시')
    const basicSafeZone = await fetchBasicSafeZone(patient.value.userNo)
    
    if (basicSafeZone) {
      drawBasicSafeZone(map, basicSafeZone)
    } else {
      console.warn('표시할 안심존이 없습니다.')
    }
  } catch (error) {
    console.error('안심존 업데이트 오류:', error)
  }
}

/* ===== 기타 ===== */
const nextSchedule = computed(() => {
  const now = new Date()
  const yyyy = now.getFullYear()
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  const todayKey = `${yyyy}-${mm}-${dd}`

  const todayList = allSchedules.value
    .filter(s => s.scheduleDate === todayKey)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))

  const nowMin = now.getHours() * 60 + now.getMinutes()
  const upcoming = todayList.find(s => timeToMin(s.startTime) >= nowMin)
  if (!upcoming) return null

  const t = `${fmtTime(upcoming.startTime)} - ${fmtTime(upcoming.endTime)}`
  const loc = formatLocation(upcoming.scheduleNo)
  return { id: upcoming.scheduleNo, title: upcoming.scheduleTitle, time: t, location: loc, depart: null, arrive: null }
})

function toggleTestEvent() {
  if (missingEvent.value) { missingEvent.value = null }
  else {
    // 이벤트 발생 시 이름은 연결된 환자명 우선 사용
    missingEvent.value = {
      name: (patient.value.name || '환자'),
      age: 71,
      location: '청주 동남지구',
      time: tsToLocal(new Date().toISOString()),
      avatarUrl: null
    }
  }
}
function goToMapMain() { router.push('/map-main') }

/* ===== 초기화 ===== */
onMounted(async () => {
  try {
    const userNo = await getMyPatientNoAndProfile()
    if (!userNo) {
      await initMap()
      return
    }
    await Promise.all([
      getLastLocation(userNo),
      getSchedules(userNo),
      getActiveMissing(userNo)
    ])
    await initMap()
    updateMarker()
    await getBasicSafeCheck(userNo)
  } catch (e) {
    console.error(e)
    err.value = `[메인 초기화 오류]\n${e?.message || e}`
  }
})
</script>

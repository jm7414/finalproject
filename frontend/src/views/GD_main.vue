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

    <!--  여기에 임시 '상태변환' 버튼 추가  -->
    <button class="position-absolute btn btn-primary btn-sm" @click="reportMissing()"
      title="상태변환 (임시)" style="
        top:6px; right:30px; z-index:50;
        font-size: 10px; padding: 2px 4px;
      ">
      변환
    </button>
    <!--  버튼 추가 끝  -->

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
          @click="router.push('/geo-fencing')" style="height:220px;
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
  <!-- 실종 모달 -->
  <MissingReportModal
    :show="isReportModalVisible"
    :patient="patient"
    @close="isReportModalVisible = false"
    @reportSuccess="onReportSuccess"
  />
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios';

import zone1 from '@/assets/images/zone 1.svg'
import locationIcon from '@/assets/images/location.svg'
import report2 from '@/assets/images/report2.png'
import connectIcon from '@/assets/images/connect.svg'
import MissingReportModal from '@/components/MissingReportModal.vue'; // 실종 임포트
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
  reportMissing: '/api/missing-persons/report' // 실종 API
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
const isReportModalVisible = ref(false); // 실종 모달 오프

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
  } catch (e) { console.error('[Pr] Kakao Map Error:', e?.message || e) }
}
function updateMarker() {
  if (!map || !marker || !lastLocation.value) return
  const kakao = window.kakao
  const c = new kakao.maps.LatLng(lastLocation.value.lat, lastLocation.value.lng)
  marker.setPosition(c); map.setCenter(c)
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
function openReportModal() {    // 실종 모달 함수
  if (!connected.value || !patient.value?.userNo) {
    alert("먼저 환자와 연결해야 합니다.");
    return;
  }
  isReportModalVisible.value = true; // 모달 온
}
async function onReportSuccess(reportData) {  // 실종 모달 확인용
  console.log("신고 접수 완료 (메인에서 받음):", reportData);
  await getActiveMissing(patient.value.userNo);
}

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

async function reportMissing() {
  // 1. 연결된 환자가 있는지 확인합니다.
  if (!connected.value || !patient.value?.userNo) {
    alert("먼저 환자와 연결해야 합니다.");
    return;
  }

  // 2. 사용자에게 재확인 받습니다.
  if (!confirm(`${patient.value.name || '환자'}님을(를) '실종' 상태로 신고하시겠습니까?`)) {
    return;
  }

  try {
    // 3. 백엔드에 보낼 신고 데이터를 준비합니다.
    const reportData = {
      patientUserNo: patient.value.userNo, // 신고할 환자 ID
      description: "긴급 신고: 메인 화면에서 '상태변환' 버튼을 통해 신고됨", // 임시 상세정보
      status: "실종"
    };

    // 4. 우리가 만든 '실종 신고' API를 호출합니다.
    const response = await axios.post(ENDPOINTS.reportMissing, reportData, {
      withCredentials: true
    });
    
    alert('실종 신고가 성공적으로 접수되었습니다.');
    console.log("신고 접수 완료:", response.data);
    
    // 5. 신고 접수 후, 화면의 실종 이벤트 카드(missingEvent)를 즉시 업데이트합니다.
    await getActiveMissing(patient.value.userNo);
    
  } catch (error) {
    console.error("실종 신고 처리 중 오류 발생:", error);
    alert("실종 신고에 실패했습니다.");
  }
}

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

<!-- src/views/GDC.vue -->
<template>
  <div class="container-sm d-flex flex-column py-3" style="max-width:414px; height:100svh; overflow:hidden">
    <!-- 상단 헤더 -->
    <div class="d-flex align-items-center justify-content-between mb-2">
      <button class="btn btn-link text-body p-0" @click="goBack" :disabled="busy">
        <i class="bi bi-chevron-left fs-5"></i>
      </button>
      <div class="text-center">
        <h6 class="mb-0 fw-semibold">보호자-환자 연결</h6>
        <small class="text-secondary" v-if="connected">연결 유지 중</small>
        <small class="text-secondary" v-else>아직 연결되지 않음</small>
      </div>
      <button class="btn btn-link text-body p-0" :disabled="busy">
        <i class="bi bi-three-dots-vertical fs-5"></i>
      </button>
    </div>

    <!-- 본문 -->
    <div class="flex-grow-1 d-flex flex-column overflow-hidden">
      <!-- [A] 초대코드 입력: 연결 전 전용 -->
      <div v-if="!connected" class="card border-0 shadow-sm mb-3">
        <div class="card-body">
          <h5 class="fw-bold mb-2">초대코드로 연결</h5>
          <p class="text-secondary small mb-3">
            환자 또는 다른 보호자에게 받은 <span class="fw-semibold">초대코드</span>를 입력해 연결을 완료하세요.
          </p>

          <form class="input-group mb-2" @submit.prevent="connectWithCode">
            <input v-model.trim="inviteCode" class="form-control" type="text" inputmode="text"
              autocomplete="one-time-code" placeholder="예: A1B2C3" :disabled="busy" />
            <button class="btn btn-primary" type="submit" :disabled="busy || !inviteCode">
              {{ busy ? '연결 중...' : '연결하기' }}
            </button>
          </form>

          <div class="small text-secondary">
            연결이 완료되면 환자 정보와 다른 보호자들이 아래에 표시됩니다.
          </div>
        </div>
      </div>

      <!-- [B] 연결된 환자 카드: 연결 후 전용 -->
      <div v-if="connected" class="card border-0 shadow-sm mb-3">
        <div class="card-body">
          <div class="d-flex align-items-center justify-content-between mb-2">
            <div class="fw-semibold d-flex align-items-center gap-2">
              <span class="rounded-circle d-inline-flex align-items-center justify-content-center"
                style="width:40px;height:40px;background:#f1f3f5">
                <i class="bi bi-person"></i>
              </span>
              연결된 환자
            </div>

            <button class="btn btn-danger btn-sm" :disabled="busy" @click="disconnect">
              <i class="bi bi-link-slash me-1"></i>
              {{ busy ? '해제 중...' : '연결 해제' }}
            </button>
          </div>

          <h4 class="fw-bold mb-3">환자 정보</h4>

          <div class="border-top">
            <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
              <!-- 라벨/값 색상 고정 -->
              <div style="color:#495057;">환자 번호</div>
              <div class="fw-semibold" style="color:#212529;">#{{ patient.id || '-' }}</div>
            </div>
            <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
              <div style="color:#495057;">이름</div>
              <div class="fw-semibold" style="color:#212529;">{{ patient.name || '-' }}</div>
            </div>
            <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
              <div style="color:#495057;">생년월일</div>
              <div class="fw-semibold" style="color:#212529;">{{ formatBirth(patient.birth) }}</div>
            </div>
            <div class="d-flex justify-content-between align-items-center py-2">
              <div style="color:#495057;">연락처</div>
              <div class="fw-semibold" style="color:#212529;">{{ patient.phone || '-' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- [C] 연결된 보호자: 연결 후 전용 -->
      <template v-if="connected">
        <div class="d-flex align-items-center justify-content-between mb-2">
          <div class="fw-semibold"><i class="bi bi-people me-1"></i>연결된 보호자</div>
          <small class="text-secondary">최대 {{ subscriptionSlots }}명</small>
        </div>

        <div class="row row-cols-3 g-2 mb-3">
          <div v-for="(g, idx) in guardianSlots" :key="idx" class="col">
            <!-- 채워진 슬롯 -->
            <div v-if="g" class="card h-100 border">
              <div class="card-body text-center p-2">
                <div class="rounded-circle d-inline-flex align-items-center justify-content-center bg-light mb-1"
                  style="width:38px;height:38px">
                  <i class="bi bi-person-circle"></i>
                </div>
                <div class="small fw-semibold text-truncate" :title="g.name">{{ g.name }}</div>
                <div class="text-secondary small text-truncate" :title="g.phone">{{ g.phone }}</div>
                <span class="badge bg-light text-secondary mt-1">
                  <i class="bi bi-shield-check me-1"></i>{{ g.role || '보호자' }}
                </span>
              </div>
            </div>

            <!-- 빈 슬롯 -->
            <div v-else class="card h-100 border bg-light-subtle">
              <div class="card-body text-center p-2 d-flex flex-column align-items-center justify-content-center">
                <div class="rounded-circle d-inline-flex align-items-center justify-content-center mb-1 bg-white border"
                  style="width:38px;height:38px">
                  <i class="bi bi-person-plus"></i>
                </div>
                <div class="small text-secondary">빈 슬롯</div>
                <button class="btn btn-sm btn-outline-secondary mt-1" @click="openInviteTip"
                  :disabled="busy">초대</button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 안내/에러 -->
      <p v-if="msg" class="text-success small mb-1">{{ msg }}</p>
      <div v-if="err" class="alert alert-warning small" style="white-space:pre-wrap">{{ err }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

/* =========================
   0) 엔드포인트 (백엔드에 맞춤)
========================= */
const ENDPOINTS = {
  me: '/api/user/me',
  myPatient: '/api/user/my-patient',
  patientBasic: (no) => `/api/connect/patient/${encodeURIComponent(no)}/basic`,
  patientGuardians: (no) => `/api/connect/patient/${encodeURIComponent(no)}/guardians`,
  createConnection: '/api/connect/connections',
  cancelConnection: (pno, gno) =>
    `/api/connect/connections?patientNo=${encodeURIComponent(pno)}&guardianNo=${encodeURIComponent(gno)}`,
  resolveInvite: (code) => `/api/connect/resolve-invite?code=${encodeURIComponent(code)}`
}

/* =========================
   1) 상태
========================= */
const router = useRouter()
const busy = ref(false)
const msg = ref('')
const err = ref('')

const subscriptionSlots = 3

const guardianNo = ref(null)
const connected = ref(false)

const inviteCode = ref('')

const patient = ref({
  id: null, name: '', birth: '', phone: '', profilePhoto: null
})

const guardianSlots = ref([null, null, null])

/* =========================
   2) 공통 fetch
========================= */
async function request(url, options = {}) {
  const res = await fetch(url, { credentials: 'include', ...options })
  const ct = res.headers.get('content-type') || ''
  const isJson = ct.includes('application/json')
  const body = isJson ? await res.json().catch(() => ({})) : await res.text().catch(() => '')
  if (!res.ok) throw new Error(typeof body === 'string' ? body : body.message || `${res.status}`)
  return body
}

/* =========================
   3) 유틸
========================= */
function pickId(obj) {
  if (obj == null) return null
  if (typeof obj === 'number') return obj
  if (typeof obj === 'string') return obj.trim() || null
  if (typeof obj === 'object') {
    const cand = obj.userNo ?? obj.patientNo ?? obj.id ?? obj.userId ?? null
    if (typeof cand === 'number') return cand
    if (typeof cand === 'string') return cand.trim() || null
  }
  return null
}
function formatBirth(d) {
  if (!d) return '-'
  return String(d).split('T')[0]
}
function toPhone(p) {
  if (!p) return ''
  const digits = String(p).replace(/\D/g, '')
  if (digits.length === 11) return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
  if (digits.length === 10) return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`
  return p
}
function mapGuardiansToSlots(list) {
  const mapped = (list || []).map((g) => ({
    name: g.name ?? g.guardianName ?? '(이름 없음)',
    phone: toPhone(g.phoneNumber ?? g.phone ?? ''),
    role: g.relation ?? g.role ?? '보호자'
  }))
  const slots = [null, null, null]
  for (let i = 0; i < Math.min(mapped.length, 3); i++) slots[i] = mapped[i]
  return slots
}

/* =========================
   4) API 호출
========================= */
async function fetchMe() {
  const me = await request(ENDPOINTS.me)
  guardianNo.value = pickId(me)
  if (!guardianNo.value) throw new Error('내 guardianNo를 확인할 수 없습니다.')
}

async function fetchMyPatient() {
  const r = await request(ENDPOINTS.myPatient).catch(() => ({}))
  const userNo = pickId(r)
  connected.value = !!userNo
  return userNo || null
}

async function fetchPatientDetail(userNo) {
  try {
    const u = await request(ENDPOINTS.patientBasic(userNo))
    patient.value = {
      id: pickId(u) || userNo,
      name: u?.name ?? '',
      birth: u?.birth ?? '',
      phone: toPhone(u?.phoneNumber ?? ''),
      profilePhoto: u?.profilePhoto ?? null
    }
  } catch {
    const u = await request(`/api/user/${encodeURIComponent(userNo)}`)
    patient.value = {
      id: pickId(u) || userNo,
      name: u?.name ?? '',
      birth: u?.birthDate ?? u?.birth ?? '',
      phone: toPhone(u?.phoneNumber ?? u?.phone ?? ''),
      profilePhoto: u?.profilePhoto ?? null
    }
  }
}

async function fetchGuardians(patientNo) {
  if (!patientNo) {
    guardianSlots.value = [null, null, null]
    return
  }
  const list = await request(ENDPOINTS.patientGuardians(patientNo))
  guardianSlots.value = mapGuardiansToSlots(list)
}

/* 초대코드로 연결 */
async function connectWithCode() {
  if (!inviteCode.value) return
  busy.value = true
  msg.value = ''
  err.value = ''
  try {
    const info = await request(ENDPOINTS.resolveInvite(inviteCode.value))
    const pno = pickId(info?.patientNo ?? info)
    if (!pno) throw new Error('유효하지 않은 초대코드입니다.')

    await request(ENDPOINTS.createConnection, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ patientNo: pno, guardian1No: guardianNo.value })
    })

    const confirmedPno = await fetchMyPatient()
    if (!confirmedPno) throw new Error('연결 확인에 실패했습니다.')
    await fetchPatientDetail(confirmedPno)
    await fetchGuardians(confirmedPno)

    inviteCode.value = ''
    msg.value = '연결이 완료되었습니다.'
  } catch (e) {
    err.value = '초대코드 연결에 실패했습니다.\n' + (e?.message || '')
  } finally {
    busy.value = false
  }
}

/* 연결 해제 */
async function disconnect() {
  msg.value = ''
  err.value = ''
  if (!connected.value || !patient.value.id || !guardianNo.value) return
  busy.value = true
  try {
    await request(ENDPOINTS.cancelConnection(patient.value.id, guardianNo.value), { method: 'DELETE' })
    connected.value = false
    guardianSlots.value = [null, null, null]
    patient.value = { id: null, name: '', birth: '', phone: '', profilePhoto: null }
    msg.value = '연결이 해제되었습니다.'
  } catch (e) {
    err.value = '연결 해제 중 오류가 발생했습니다.\n' + (e?.message || '')
  } finally {
    busy.value = false
  }
}

/* =========================
   5) 네비 / 기타
========================= */
function goBack() {
  history.length > 1 ? router.back() : router.push('/')
}
function openInviteTip() {
  msg.value = '빈 슬롯을 채우려면 다른 보호자에게 초대코드를 요청하세요.'
}

/* =========================
   6) 초기화
========================= */
onMounted(async () => {
  try {
    await fetchMe()
    const pno = await fetchMyPatient()
    if (!pno) {
      patient.value = { id: null, name: '', birth: '', phone: '', profilePhoto: null }
      guardianSlots.value = [null, null, null]
      msg.value = ''
      return
    }
    await fetchPatientDetail(pno)
    await fetchGuardians(pno)
    msg.value = '저장된 연결을 불러왔습니다.'
  } catch (e) {
    err.value = '초기화 중 오류가 발생했습니다.\n' + (e?.message || '')
  }
})
</script>

<style>
html,
body,
#app {
  height: 100%;
  overflow: hidden;
}
</style>
<style scoped>
/* 필요 시 추가 */
</style>

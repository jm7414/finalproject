<!-- src/views/GD_Connect.vue -->
<template>
  <div class="container-sm px-3">

    <!-- 연결 전: 완전 중앙 & 스크롤 없음 -->
    <div
      v-if="!connected"
      class="overflow-hidden"
      :style="centerBoxStyle"
    >
      <div class="w-100" style="max-width: 26rem; margin: 0 auto;">
        <div class="card border-0 shadow-sm">
          <div class="card-body">
            <h5 class="fw-bold mb-2">초대코드로 연결</h5>
            <p class="text-secondary small mb-3">
              환자 또는 다른 보호자에게 받은 <span class="fw-semibold">초대코드</span>를 입력해 연결을 완료하세요.
            </p>

            <form class="input-group mb-2" @submit.prevent="connectWithCode">
              <input
                v-model.trim="inviteCode"
                class="form-control"
                type="text"
                inputmode="text"
                autocomplete="one-time-code"
                placeholder="예: A1B2C3"
                :disabled="busy"
              />
              <button class="btn btn-primary" type="submit" :disabled="busy || !inviteCode">
                <span v-if="busy">연결 중...</span>
                <span v-else>연결하기</span>
              </button>
            </form>

            <div class="small text-secondary">
              연결이 완료되면 환자 정보와 다른 보호자들이 아래에 표시됩니다.
            </div>

            <!-- 메시지를 카드 안에 넣어 높이 초과 방지 -->
            <p v-if="msg" class="text-success small mt-3 mb-0">{{ msg }}</p>
            <div v-if="err" class="alert alert-warning small mt-2 mb-0" style="white-space:pre-wrap">
              {{ err }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 연결 후: 중앙보다 살짝 위 & 스크롤 없음 -->
    <div
      v-else
      class="d-flex justify-content-center align-items-start overflow-hidden pt-2"
      :style="connectedBoxStyle"
    >
      <div class="w-100 col-12 mx-auto" style="max-width: 28rem">
        <!-- 환자 카드 -->
        <div class="card border-0 shadow-sm mb-2">
          <div class="card-body">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <div class="fw-semibold d-flex align-items-center gap-2">
                <i class="bi bi-person fs-5"></i>
                <span>연결된 환자</span>
              </div>
              <button class="btn btn-danger btn-sm" :disabled="busy" @click="disconnect">
                <i class="bi bi-link-slash me-1"></i>
                <span v-if="busy">해제 중...</span>
                <span v-else>연결 해제</span>
              </button>
            </div>

            <h4 class="fw-bold mb-3">환자 정보</h4>

            <div class="border-top">
              <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
                <div class="text-secondary">환자 번호</div>
                <div class="fw-semibold">#{{ patient.id || '-' }}</div>
              </div>
              <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
                <div class="text-secondary">이름</div>
                <div class="fw-semibold">{{ patient.name || '-' }}</div>
              </div>
              <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
                <div class="text-secondary">생년월일</div>
                <div class="fw-semibold">{{ formatBirth(patient.birth) }}</div>
              </div>
              <div class="d-flex justify-content-between align-items-center py-2">
                <div class="text-secondary">연락처</div>
                <div class="fw-semibold">{{ patient.phone || '-' }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 연결된 보호자(빈 슬롯 제거) -->
        <template v-if="filledGuardians.length">
          <div class="d-flex align-items-center justify-content-between mb-2">
            <div class="fw-semibold"><i class="bi bi-people me-1"></i>연결된 보호자</div>
            <small class="text-secondary">최대 {{ subscriptionSlots }}명</small>
          </div>

          <div class="row row-cols-3 g-2">
            <div v-for="(g, idx) in filledGuardians" :key="idx" class="col">
              <div class="card h-100 border">
                <div class="card-body text-center p-2">
                  <i class="bi bi-person-circle fs-4 d-block mb-1"></i>
                  <div class="small fw-semibold text-truncate" :title="g.name">{{ g.name }}</div>
                  <div class="text-secondary small text-truncate" :title="g.phone">{{ g.phone }}</div>
                  <span class="badge bg-light text-secondary mt-1">
                    <i class="bi bi-shield-check me-1"></i>{{ g.role || '보호자' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </template>

        <p v-if="msg" class="text-success small mt-3 mb-0">{{ msg }}</p>
        <div v-if="err" class="alert alert-warning small mt-2 mb-0" style="white-space:pre-wrap">
          {{ err }}
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

/* 헤더+푸터 총 높이(프로젝트에 맞게 숫자만 조정) */
const CHROME = 400

/* ===== 스타일 계산 (스크롤 제거 + 정렬) ===== */
const centerBoxStyle = computed(() => ({
  height: `calc(100dvh - ${CHROME}px)`,
  display: 'grid',
  placeItems: 'center',
  overflow: 'hidden'
}))
const connectedBoxStyle = computed(() => ({
  height: `calc(100dvh - ${CHROME}px)`,
  overflow: 'hidden'
}))

/* ===== 기존 로직 그대로 ===== */
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

const router = useRouter()
const busy = ref(false)
const msg = ref('')
const err = ref('')

const subscriptionSlots = 3
const guardianNo = ref(null)
const connected = ref(false)
const inviteCode = ref('')

const patient = ref({ id: null, name: '', birth: '', phone: '', profilePhoto: null })
const guardianSlots = ref([null, null, null])
const filledGuardians = computed(() => guardianSlots.value.filter(Boolean))

async function request(url, options = {}) {
  const res = await fetch(url, { credentials: 'include', ...options })
  const ct = res.headers.get('content-type') || ''
  const isJson = ct.includes('application/json')
  const body = isJson ? await res.json().catch(() => ({})) : await res.text().catch(() => '')
  if (!res.ok) throw new Error(typeof body === 'string' ? body : body.message || `${res.status}`)
  return body
}
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
function formatBirth(d) { if (!d) return '-'; return String(d).split('T')[0] }
function toPhone(p) {
  if (!p) return ''
  const digits = String(p).replace(/\D/g, '')
  if (digits.length === 11) return `${digits.slice(0,3)}-${digits.slice(3,7)}-${digits.slice(7)}`
  if (digits.length === 10) return `${digits.slice(0,3)}-${digits.slice(3,6)}-${digits.slice(6)}`
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
  if (!patientNo) { guardianSlots.value = [null, null, null]; return }
  const list = await request(ENDPOINTS.patientGuardians(patientNo))
  guardianSlots.value = mapGuardiansToSlots(list)
}

async function connectWithCode() {
  if (!inviteCode.value) return
  busy.value = true; msg.value = ''; err.value = ''
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
  } finally { busy.value = false }
}

async function disconnect() {
  msg.value = ''; err.value = ''
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
  } finally { busy.value = false }
}

function goBack() { history.length > 1 ? router.back() : router.push('/') }
function openInviteTip() { msg.value = '빈 슬롯을 채우려면 다른 보호자에게 초대코드를 요청하세요.' }

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

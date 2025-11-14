<template>
  <div class="desktop-mypage-connect">
    <div class="page-container">
      <!-- 연결 전 -->
      <div v-if="!connected" class="connect-section">
        <div class="connect-card">
          <h2 class="card-title">초대코드로 연결</h2>
          <p class="card-description">
            환자 또는 다른 보호자에게 받은 <strong>초대코드</strong>를 입력해 연결을 완료하세요.
          </p>

          <form class="connect-form" @submit.prevent="connectWithCode">
            <div class="input-group">
              <input
                v-model.trim="inviteCode"
                class="form-control"
                type="text"
                placeholder="예: A1B2C3"
                :disabled="busy"
              />
              <button class="btn-primary" type="submit" :disabled="busy || !inviteCode">
                <span v-if="busy">연결 중...</span>
                <span v-else>연결하기</span>
              </button>
            </div>
          </form>

          <p class="form-hint">
            연결이 완료되면 환자 정보와 다른 보호자들이 표시됩니다.
          </p>

          <div v-if="msg" class="alert alert-success">{{ msg }}</div>
          <div v-if="err" class="alert alert-danger" style="white-space:pre-wrap">{{ err }}</div>
        </div>
      </div>

      <!-- 연결 후 -->
      <div v-else class="connected-section">
        <!-- 환자 카드 -->
        <div class="patient-card">
          <div class="card-header">
            <div class="header-title">
              <i class="bi bi-person"></i>
              <span>연결된 환자</span>
            </div>
            <button class="btn-danger" :disabled="busy" @click="disconnect">
              <i class="bi bi-link-slash me-1"></i>
              <span v-if="busy">해제 중...</span>
              <span v-else>연결 해제</span>
            </button>
          </div>
          
          <div class="patient-info">
            <h3 class="info-title">환자 정보</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">환자 번호</span>
                <span class="info-value">#{{ patient.id || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">이름</span>
                <span class="info-value">{{ patient.name || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">생년월일</span>
                <span class="info-value">{{ formatBirth(patient.birth) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">연락처</span>
                <span class="info-value">{{ patient.phone || '-' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 연결된 보호자 -->
        <div v-if="filledGuardians.length" class="guardians-section">
          <div class="section-header">
            <h3 class="section-title">
              <i class="bi bi-people me-2"></i>연결된 보호자
            </h3>
            <span class="section-badge">최대 {{ subscriptionSlots }}명</span>
          </div>
          
          <div class="guardians-grid">
            <div v-for="(g, idx) in filledGuardians" :key="idx" class="guardian-card">
              <i class="bi bi-person-circle guardian-icon"></i>
              <div class="guardian-name">{{ g.name }}</div>
              <div class="guardian-phone">{{ g.phone }}</div>
              <span class="guardian-badge">
                <i class="bi bi-shield-check me-1"></i>{{ g.role || '보호자' }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="msg" class="alert alert-success">{{ msg }}</div>
        <div v-if="err" class="alert alert-danger" style="white-space:pre-wrap">{{ err }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
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

<style scoped>
.desktop-mypage-connect {
  width: 100%;
  min-height: calc(100vh - 80px);
  padding: 0;
}

.page-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* 연결 전 */
.connect-section {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 500px;
}

.connect-card {
  width: 100%;
  max-width: 600px;
  background: #ffffff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-size: 24px;
  font-weight: 700;
  color: #171717;
  margin: 0 0 12px 0;
}

.card-description {
  font-size: 15px;
  color: #6b7280;
  margin: 0 0 24px 0;
  line-height: 1.6;
}

.connect-form {
  margin-bottom: 16px;
}

.input-group {
  display: flex;
  gap: 12px;
}

.form-control {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #d1d5db;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: rgba(74, 98, 221, 0.5);
  box-shadow: 0 0 0 3px rgba(74, 98, 221, 0.1);
}

.btn-primary {
  padding: 12px 24px;
  background: rgba(74, 98, 221, 0.85);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-primary:hover:not(:disabled) {
  background: rgba(74, 98, 221, 1);
}

.btn-primary:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.form-hint {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

/* 연결 후 */
.connected-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.patient-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #171717;
}

.header-title i {
  font-size: 20px;
  color: rgba(74, 98, 221, 0.85);
}

.btn-danger {
  padding: 8px 16px;
  background: #ef4444;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-danger:hover:not(:disabled) {
  background: #dc2626;
}

.btn-danger:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.info-title {
  font-size: 20px;
  font-weight: 600;
  color: #171717;
  margin: 0 0 20px 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.info-label {
  font-size: 13px;
  color: #6b7280;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #171717;
}

.guardians-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #171717;
  margin: 0;
  display: flex;
  align-items: center;
}

.section-badge {
  font-size: 13px;
  color: #6b7280;
}

.guardians-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.guardian-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.guardian-icon {
  font-size: 48px;
  color: #9ca3af;
  margin-bottom: 12px;
  display: block;
}

.guardian-name {
  font-size: 15px;
  font-weight: 600;
  color: #171717;
  margin-bottom: 4px;
}

.guardian-phone {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.guardian-badge {
  display: inline-block;
  padding: 4px 8px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 12px;
  color: #6b7280;
}

.alert {
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 16px;
}

.alert-success {
  background: #d1fae5;
  color: #065f46;
}

.alert-danger {
  background: #fee2e2;
  color: #991b1b;
}
</style>

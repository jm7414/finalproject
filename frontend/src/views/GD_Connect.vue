<template>
  <main class="gdc-wrap">
    <!-- 상단 상태/액션 바로가기 바 -->
    <div class="topbar">
      <div class="left">
        <strong>보호자-환자 연결</strong>
        <span class="muted" v-if="stage === 'connected'">연결 유지 중</span>
      </div>
    </div>

    <!-- 연결 폼: 연결 전만 노출 -->
    <div v-if="stage === 'form'" class="card">
      <h2 class="title">초대코드로 연결</h2>

      <div class="field">
        <label class="label">내 보호자 번호</label>
        <input class="input" :value="guardianNo ?? ''" disabled />
      </div>

      <div class="field">
        <label class="label" for="invite">환자 초대코드</label>
        <input id="invite" class="input" type="text" v-model.trim="inviteCode" placeholder="예: 8자리 코드" />
      </div>

      <div class="field" v-if="resolvedPreview">
        <label class="label">확인된 환자</label>
        <div class="kv-inline">
          <span class="code">#{{ resolvedPreview.patientNo }}</span>
          <span class="dot"></span>
          <span>{{ resolvedPreview.name }}</span>
        </div>
      </div>

      <div class="row">
        <button class="btn ghost" :disabled="busy || !inviteCode" @click="previewInvite">
          {{ busy && previewing ? '확인 중...' : '코드 확인' }}
        </button>
        <button class="btn" :disabled="!canSubmit || busy" @click="handleConnect">
          {{ busy && !previewing ? '연결 중...' : '연결' }}
        </button>
      </div>

      <p v-if="msg" class="msg ok">{{ msg }}</p>
      <p v-if="err" class="msg err">{{ err }}</p>
    </div>

    <!-- 연결 이후 보여줄 요약 카드 -->
    <div v-else-if="stage === 'connected'" class="card">
      <div class="flex-row">
        <h2 class="title">연결된 환자</h2>
        <div class="gap8">
          <!-- 다환자 금지: 새 연결 버튼 제거 -->
          <button class="btn danger" :disabled="busy" @click="disconnect">
            {{ busy ? '해제 중...' : '연결 해제' }}
          </button>
        </div>
      </div>

      <section class="section">
        <h3 class="section-title">환자 정보</h3>
        <div class="kv">
          <div class="k">환자 번호</div>
          <div class="v">#{{ patient?.userNo ?? '(알 수 없음)' }}</div>
        </div>
        <div class="kv">
          <div class="k">이름</div>
          <div class="v">{{ patient?.name || '(이름 없음)' }}</div>
        </div>
        <div class="kv">
          <div class="k">생년월일</div>
          <div class="v">{{ formatBirth(patient?.birth) }}</div>
        </div>
        <div class="kv">
          <div class="k">연락처</div>
          <div class="v">{{ patient?.phoneNumber || '-' }}</div>
        </div>
      </section>

      <section class="section">
        <div class="flex-row">
          <h3 class="section-title">연결된 보호자</h3>
          <span class="muted">최대 3명</span>
        </div>

        <div v-if="guardians.length === 0" class="muted">아직 다른 보호자 연결이 없습니다.</div>

        <ul v-else class="list">
          <li v-for="g in guardians" :key="g.guardianId" class="item">
            <div class="item-primary">
              <div class="item-title">{{ g.name ?? '(이름 없음)' }}</div>
              <div class="item-sub">
                <span class="chip">{{ g.relation ?? '보호자' }}</span>
                <span v-if="g.guardianId" class="code">#{{ g.guardianId }}</span>
              </div>
            </div>
          </li>
        </ul>
      </section>

      <p v-if="msg" class="msg ok">{{ msg }}</p>
      <p v-if="err" class="msg err">{{ err }}</p>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'

const stage = ref('form')

// 핵심 상태
const guardianNo = ref(null)
const inviteCode = ref('')
const resolvedPreview = ref(null) // { patientNo, name }

// 연결 후 표시 상태
const patient = ref(null)
const guardians = ref([])

// UX 상태
const msg = ref('')
const err = ref('')
const busy = ref(false)
const previewing = ref(false)

// 버튼 활성화 조건
const canSubmit = computed(() => !!guardianNo.value && !!inviteCode.value)

// 로컬 저장 키
const LS_KEY = 'gdc:lastConnection'

// 공통 fetch
async function request(url, options = {}) {
  const res = await fetch(url, { credentials: 'include', ...options })
  const isJson = (res.headers.get('content-type') || '').includes('application/json')
  const body = isJson ? await res.json().catch(() => ({})) : await res.text().catch(() => '')
  if (!res.ok) throw new Error(typeof body === 'string' ? body : (body.message || `${res.status}`))
  return body
}

// API
async function fetchMe() {
  return await request('/api/user/me')
}

async function apiResolveInvite(code) {
  return await request(`/api/connect/resolve-invite?code=${encodeURIComponent(code)}`)
}

async function apiCreateConnection({ patientNo, guardianNo }) {
  return await request('/api/connect/connections', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      patientNo: Number(patientNo),
      guardian1No: Number(guardianNo)
    })
  })
}

async function apiCancelConnection({ patientNo, guardianNo }) {
  const url = `/api/connect/connections?patientNo=${encodeURIComponent(patientNo)}&guardianNo=${encodeURIComponent(guardianNo)}`
  const res = await fetch(url, { method: 'DELETE', credentials: 'include' })
  const isJson = (res.headers.get('content-type') || '').includes('application/json')
  const body = isJson ? await res.json().catch(() => ({})) : await res.text().catch(() => '')
  if (!res.ok) throw new Error(typeof body === 'string' ? body : (body.message || `해제 실패 (${res.status})`))
  return body
}

// 로컬 저장
function saveLastConnection(payload) {
  localStorage.setItem(LS_KEY, JSON.stringify(payload))
}
function loadLastConnection() {
  const raw = localStorage.getItem(LS_KEY)
  if (!raw) return null
  try { return JSON.parse(raw) } catch { return null }
}
function clearLastConnection() {
  localStorage.removeItem(LS_KEY)
}

// 날짜 포맷
function formatBirth(d) {
  if (!d) return '-'
  const s = String(d).split('T')[0]
  return s
}

// 초기 진입
onMounted(async () => {
  try {
    const me = await fetchMe()
    guardianNo.value = me.userNo ?? me.userId ?? me.id

    const last = loadLastConnection()
    if (last && Number(last.guardianNo) === Number(guardianNo.value)) {
      stage.value = 'connected'
      patient.value = last.patient || { userNo: last.patientNo }
      guardians.value = Array.isArray(last.guardians) ? last.guardians : []
      msg.value = '저장된 연결을 불러왔습니다.'
    } else {
      stage.value = 'form'
    }
  } catch {
    err.value = '내 정보 조회 실패. 다시 로그인 해주세요.'
  }
})

// 초대코드 미리 확인
async function previewInvite() {
  msg.value = ''
  err.value = ''
  resolvedPreview.value = null
  previewing.value = true
  try {
    const data = await apiResolveInvite(inviteCode.value)
    resolvedPreview.value = { patientNo: data.patientNo, name: data.name || data.patientName || '(이름 없음)' }
    msg.value = '초대코드 확인 완료'
  } catch {
    err.value = '유효하지 않은 초대코드입니다.'
  } finally {
    previewing.value = false
  }
}

// 실제 연결
async function handleConnect() {
  if (stage.value === 'connected') {
    err.value = '이미 다른 보호자와 연결되어 있습니다. 구독해주세요.!'
    return
  }
  msg.value = ''
  err.value = ''
  busy.value = true
  try {
    const info = await apiResolveInvite(inviteCode.value)
    const pno = info.patientNo
    const res = await apiCreateConnection({ patientNo: pno, guardianNo: guardianNo.value })

    // 서버가 ok=false로 보낼 수도 있으므로 확인
    if (res && res.ok === false) {
      throw new Error(res.message || '연결 실패')
    }

    stage.value = 'connected'
    patient.value = res.patient || { userNo: pno, name: info.name || info.patientName }
    guardians.value = Array.isArray(res.guardians) ? res.guardians : []
    msg.value = '연결이 완료되었습니다.'

    saveLastConnection({
      patientNo: pno,
      guardianNo: guardianNo.value,
      patient: patient.value,
      guardians: guardians.value
    })

    inviteCode.value = ''
    resolvedPreview.value = null
  } catch (e) {
    const t = String(e.message || e)
    if (t.includes('이미 다른 환자에 연결된 보호자')) {
      err.value = '이미 다른 환자에 연결된 보호자입니다. 기존 연결을 해제한 뒤 다시 시도하세요.'
    } else if (t.includes('필수')) {
      err.value = '연결 실패: 필수 값 누락'
    } else {
      err.value = '연결 처리 중 오류가 발생했습니다.'
    }
  } finally {
    busy.value = false
  }
}

// 연결 해제
async function disconnect() {
  msg.value = ''
  err.value = ''
  busy.value = true
  try {
    const pno = patient.value?.userNo || patient.value?.patientNo
    if (!pno || !guardianNo.value) throw new Error('대상 없음')

    const res = await apiCancelConnection({ patientNo: pno, guardianNo: Number(guardianNo.value) })

    patient.value = res.patient || { userNo: pno }
    guardians.value = Array.isArray(res.guardians) ? res.guardians : []

    if (!guardians.value || guardians.value.length === 0) {
      clearLastConnection()
      stage.value = 'form'
      msg.value = '연결이 해제되었습니다.'
    } else {
      saveLastConnection({
        patientNo: pno,
        guardianNo: guardianNo.value,
        patient: patient.value,
        guardians: guardians.value
      })
      msg.value = '연결이 해제되었습니다. (다른 보호자 연결은 유지됨)'
    }
  } catch {
    err.value = '연결 해제 중 오류가 발생했습니다.'
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.gdc-wrap {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  background: #f6f7fb;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.left {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.card {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, .08);
  padding: 20px;
}

.title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 800;
}

.field {
  margin-bottom: 12px;
}

.label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #444;
}

.input {
  width: 100%;
  height: 40px;
  border: 1px solid #e4e6ef;
  border-radius: 10px;
  padding: 0 12px;
  outline: none;
}

.input:focus {
  border-color: #4f46e5;
}

.row {
  display: flex;
  gap: 8px;
}

.btn {
  height: 40px;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  background: #4f46e5;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.btn.ghost {
  background: #eef2ff;
  color: #4f46e5;
}

.btn.danger {
  background: #ef4444;
}

.btn:disabled {
  opacity: .5;
  cursor: not-allowed;
}

.msg {
  margin-top: 10px;
  font-size: 14px;
  white-space: pre-wrap;
}

.ok {
  color: #16a34a;
}

.err {
  color: #dc2626;
}

.section {
  margin-top: 8px;
  padding-top: 8px;
}

.section-title {
  font-weight: 700;
  margin: 10px 0;
}

.kv {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
}

.k {
  color: #666;
}

.v {
  font-weight: 600;
}

.kv-inline {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.dot {
  width: 4px;
  height: 4px;
  background: #999;
  border-radius: 50%;
  display: inline-block;
}

.list {
  margin-top: 8px;
  display: grid;
  gap: 10px;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
}

.item-title {
  font-weight: 700;
}

.item-sub {
  margin-top: 4px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.chip {
  font-size: 12px;
  background: #eef2ff;
  color: #4f46e5;
  padding: 2px 8px;
  border-radius: 999px;
}

.code {
  font-size: 12px;
  color: #666;
}

.muted {
  color: #6b7280;
  font-size: 13px;
}

.flex-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.gap8 {
  display: flex;
  gap: 8px;
}
</style>

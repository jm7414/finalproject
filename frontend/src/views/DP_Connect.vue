<!-- src/components/InviteCodeModal.vue -->
<template>
  <div class="py-2">
    <!-- 코드 카드 -->
    <div class="border rounded-3 p-3 mb-2">
      <div class="text-center">
        <div class="rounded-circle d-inline-flex align-items-center justify-content-center mb-2"
          style="width:56px;height:56px;background:#f1f3f5">
          <i class="bi bi-qr-code fs-4 text-secondary"></i>
        </div>
        <div class="fw-semibold">초대코드</div>
        <div class="small text-secondary mb-2">
          보호자와 연결하기 위한 코드입니다
          <span v-if="expiresAt" class="ms-1">(만료: {{ expiresAt }})</span>
        </div>
      </div>

      <div class="bg-light border rounded-3 px-3 py-2 text-center mb-2">
        <div v-if="loading">불러오는 중...</div>
        <div v-else-if="code" class="fw-bold"
          style="letter-spacing:.2rem;font-family:ui-monospace,SFMono-Regular,Menlo,monospace">
          {{ code }}
        </div>
        <div v-else class="text-secondary small">코드가 없습니다. 재발급을 눌러주세요.</div>
      </div>

      <div class="d-grid gap-2">
        <button type="button" class="btn btn-outline-secondary btn-sm" @click="copyCode"
          :disabled="!code || copied || loading">
          <i class="bi bi-clipboard me-1"></i>{{ copied ? '복사됨' : '코드 복사' }}
        </button>
        <button type="button" class="btn btn-outline-dark btn-sm" @click="regenerate" :disabled="loading">
          <i class="bi bi-arrow-clockwise me-1"></i>재발급
        </button>
      </div>
    </div>

    <!-- 보호자 정보 입력 (가로 배치) -->
    <div class="mb-1 fw-semibold">보호자 정보 입력</div>
    <div class="row g-2 align-items-end mb-2">
      <div class="col-5">
        <label class="form-label small text-secondary mb-1">보호자 이름</label>
        <input type="text" class="form-control form-control-sm" v-model.trim="guardianName" placeholder="이름">
      </div>
      <div class="col-7">
        <label class="form-label small text-secondary mb-1">보호자 번호</label>
        <input type="tel" class="form-control form-control-sm" v-model.trim="guardianPhone" placeholder="010-0000-0000"
          inputmode="tel">
      </div>
    </div>

    <!-- 전송 방법 -->
    <div class="mb-1 fw-semibold">전송 방법 선택</div>
    <div class="row g-2 mb-2">
      <div class="col-6">
        <button type="button" class="btn w-100 border rounded-3 py-2 btn-outline-secondary"
          :class="{ active: method === 'sms' }" @click="method = 'sms'">
          <i class="bi bi-chat-dots d-block mb-1"></i>문자 전송
        </button>
      </div>
      <div class="col-6">
        <button type="button" class="btn w-100 border rounded-3 py-2 btn-outline-secondary"
          :class="{ active: method === 'share' }" @click="method = 'share'">
          <i class="bi bi-share d-block mb-1"></i>직접 공유
        </button>
      </div>
    </div>

    <!-- 전송 버튼 -->
    <div class="d-grid mb-2">
      <button type="button" class="btn btn-dark py-2" :disabled="!code || sending" @click="sendInvite">
        <span v-if="sending">전송 중...</span>
        <span v-else>초대코드 전송하기</span>
      </button>
    </div>

    <!-- 안내/에러 -->
    <div v-if="hint" class="border rounded-3 p-2 small text-secondary mb-2">
      <div class="d-flex">
        <i class="bi bi-info-circle me-2"></i>
        <div>{{ hint }}</div>
      </div>
    </div>
    <div v-if="error" class="alert alert-warning small" style="white-space:pre-wrap">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'

/** ✅ 백엔드 엔드포인트: 프로젝트에 맞게 여기만 수정하면 끝 */
const ENDPOINTS = {
  me: '/api/user/me',                        // GET → { invitationCode, invitationExpireAt?, name? ... }
  createInvite: '/api/connect/invites',      // POST → { code, expireAt? }  (없으면 404/405 날 수도 있음)
  sendSms: '/api/notify/sms'                 // POST → { to, text }         (없으면 바로 SMS 앱으로 우회)
}

const loading = ref(false)
const sending = ref(false)
const copied = ref(false)
const error = ref('')
const hint = ref('')

const code = ref('')
const expiresAt = ref('')
const meName = ref('')

const guardianName = ref('')
const guardianPhone = ref('')
const method = ref('sms')

/* =========================
   공통
========================= */
async function request(url, options = {}) {
  const res = await fetch(url, { credentials: 'include', ...options })
  const ct = (res.headers.get('content-type') || '')
  const isJson = ct.includes('application/json')
  const body = isJson ? await res.json().catch(() => ({})) : await res.text().catch(() => '')
  if (!res.ok) throw new Error(typeof body === 'string' ? body : (body.message || `${res.status}`))
  return body
}
function onlyDigits(s) { return String(s || '').replace(/\D/g, '') }
function toKoreanPhone(p) {
  const d = onlyDigits(p)
  if (d.length === 11) return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`
  if (d.length === 10) return `${d.slice(0, 3)}-${d.slice(3, 6)}-${d.slice(6)}`
  return p
}
const inviteText = computed(() => {
  const who = meName.value ? `${meName.value}님` : '환자'
  const exp = expiresAt.value ? ` (만료: ${expiresAt.value})` : ' (유효기간 24시간)'
  return `[맘마미아] ${who} 초대코드: ${code.value}\n앱에서 "초대코드로 연결"에 입력해 주세요.${exp}`
})

/* =========================
   1) 코드 불러오기
========================= */
async function loadInvite() {
  loading.value = true
  error.value = ''; hint.value = ''
  try {
    const me = await request(ENDPOINTS.me)
    code.value = me?.invitationCode || ''
    meName.value = me?.name || ''
    // 만료시각 필드가 있으면 보여주기(예: ISO 또는 'YYYY-MM-DD HH:mm')
    const raw = me?.invitationExpireAt || me?.invitationExpiredAt || ''
    expiresAt.value = raw ? toLocalDisplay(raw) : ''
    if (!code.value) hint.value = '코드가 없는 계정입니다. 재발급을 눌러주세요.'
  } catch (e) {
    error.value = `코드 불러오기 실패: ${e?.message || e}`
  } finally {
    loading.value = false
  }
}
function toLocalDisplay(isoOrStr) {
  try {
    const d = new Date(isoOrStr)
    if (!isFinite(d)) return isoOrStr
    const yy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const hh = String(d.getHours()).padStart(2, '0')
    const mi = String(d.getMinutes()).padStart(2, '0')
    return `${yy}-${mm}-${dd} ${hh}:${mi}`
  } catch { return isoOrStr }
}

/* =========================
   2) 재발급(선택)
   - 서버에 없으면 에러 나와도 OK. 안내만 보여줌.
========================= */
async function regenerate() {
  error.value = ''; hint.value = ''
  loading.value = true
  try {
    const r = await request(ENDPOINTS.createInvite, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({})
    })
    code.value = r?.code || r?.invitationCode || code.value
    const raw = r?.expireAt || r?.invitationExpireAt
    expiresAt.value = raw ? toLocalDisplay(raw) : expiresAt.value
    hint.value = '새 초대코드가 발급되었습니다.'
  } catch (e) {
    hint.value = '서버 재발급 API가 없어 복사/공유로 진행해 주세요.'
  } finally {
    loading.value = false
  }
}

/* =========================
   3) 복사 / 전송
========================= */
async function copyCode() {
  error.value = ''; hint.value = ''
  if (!code.value) { hint.value = '먼저 코드를 발급/불러오세요.'; return }
  try {
    await navigator.clipboard.writeText(code.value)
    copied.value = true
    setTimeout(() => copied.value = false, 1500)
  } catch {
    // fallback
    const t = document.createElement('textarea')
    t.value = code.value; document.body.appendChild(t)
    t.select(); document.execCommand('copy'); document.body.removeChild(t)
    copied.value = true
    setTimeout(() => copied.value = false, 1500)
  }
}

async function sendInvite() {
  error.value = ''; hint.value = ''
  if (!code.value) { hint.value = '먼저 코드를 발급/불러오세요.'; return }
  const msg = inviteText.value

  sending.value = true
  try {
    if (method.value === 'sms') {
      // 1) 백엔드 있으면 우선 사용
      try {
        await request(ENDPOINTS.sendSms, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ to: onlyDigits(guardianPhone.value), text: msg })
        })
        hint.value = '문자 전송 요청 완료!'
        return
      } catch {
        // 2) 기기 SMS 앱으로 우회
        const digits = onlyDigits(guardianPhone.value)
        const href = `sms:${digits}?&body=${encodeURIComponent(msg)}`
        window.location.href = href
        hint.value = '기기의 문자앱으로 이동합니다.'
      }
    } else {
      if (navigator.share) {
        await navigator.share({ title: '초대코드', text: msg })
        hint.value = '공유 완료!'
      } else {
        await copyCode()
        hint.value = '이 브라우저는 공유를 지원하지 않아 코드만 복사했습니다.'
      }
    }
  } catch (e) {
    error.value = `전송 실패: ${e?.message || e}`
  } finally {
    sending.value = false
  }
}

onMounted(loadInvite)
</script>

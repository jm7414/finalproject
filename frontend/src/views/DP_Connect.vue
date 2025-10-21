<template>
  <!-- 환자 헤더 -->
  <header class="app-header">
    <div class="icon-wrapper" @click="goBack">
      <i class="icon bi bi-arrow-left icon-bold"></i>
    </div>

    <div class="app-title">
      <img src="/mammamialogo.png" alt="Mamma Mia Logo" class="logo-image">
    </div>

    <div class="icon-wrapper" @click="goHome">
      <i class="icon bi bi-house icon-bold"></i>
    </div>
  </header>

  <div class="invite-code-content">
    <!-- 코드 카드 -->
    <div class="code-card bg-white rounded-4 shadow-sm p-4 mb-3">
      <div class="text-center">
        <div class="icon-circle rounded-circle d-inline-flex align-items-center justify-content-center mb-3">
          <i class="bi bi-link-45deg"></i>
        </div>
        <h5 class="card-title fw-bold mb-2">연결코드</h5>
        <p class="card-subtitle text-secondary small mb-3">
          보호자와 연결하기 위한 코드입니다
          <span v-if="expiresAt" class="d-block mt-1 text-muted" style="font-size: 0.85rem">
            만료: {{ expiresAt }}
          </span>
        </p>
      </div>

      <!-- 코드 표시 영역 -->
      <div class="code-display rounded-3 px-4 py-3 text-center mb-3">
        <div v-if="loading" class="text-secondary">불러오는 중...</div>
        <div v-else-if="code" class="code-text fw-bold">
          {{ code }}
        </div>
        <div v-else class="text-secondary small">코드를 불러올 수 없습니다</div>
      </div>

      <!-- 코드 복사 버튼 -->
      <div class="d-grid">
        <button type="button" class="btn btn-action" @click="copyCode" :disabled="!code || copied || loading">
          <i class="bi bi-clipboard me-2"></i>
          {{ copied ? '복사됨 ✓' : '코드 복사' }}
        </button>
      </div>
    </div>

    <!-- 보호자 정보 입력 -->
    <div class="info-card bg-white rounded-4 shadow-sm p-4 mb-3">
      <h6 class="section-title fw-bold mb-3">
        <i class="bi bi-person-fill-add me-2" style="color: rgba(74, 98, 221, 0.85);"></i>
        보호자 정보 입력
      </h6>
      <div class="row g-3">
        <div class="col-5">
          <label class="form-label small text-secondary mb-2"> 보호자 이름</label>
          <input type="text" class="form-control custom-input" v-model.trim="guardianName" placeholder="이름">
        </div>
        <div class="col-7">
          <label class="form-label small text-secondary mb-2"> 보호자 번호</label>
          <input type="tel" class="form-control custom-input" v-model.trim="guardianPhone" placeholder="010-0000-0000"
            inputmode="tel">
        </div>
      </div>
    </div>

    <!-- 전송 버튼 -->
    <div class="d-grid mb-3">
      <button type="button" class="btn btn-send" :disabled="!code || sending" @click="sendInvite">
        <span v-if="sending">
          <span class="spinner-border spinner-border-sm me-2"></span>
          전송 중...
        </span>
        <span v-else>
          <i class="bi bi-send-fill me-2"></i>
          전송하기
        </span>
      </button>
    </div>

    <!-- 경고 메시지 (보호자 정보 미입력) -->
    <div v-if="showWarning" class="alert alert-warning-custom rounded-3 d-flex align-items-center small">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      <strong>보호자 정보를 입력하세요</strong>
    </div>

    <!-- 성공 메시지 (클릭하여 뒤로가기) -->
    <div v-if="sendSuccess"
      class="alert alert-success-custom rounded-3 d-flex align-items-center justify-content-between small"
      @click="goBack" style="cursor: pointer;">
      <div class="d-flex align-items-center">
        <i class="bi bi-check-circle-fill me-2"></i>
        <strong>전송 완료되었습니다. <br>클릭하여 돌아가기</strong>
      </div>
      <i class="bi bi-arrow-left"></i>
    </div>

    <!-- 에러 메시지 -->
    <div v-if="error" class="alert alert-danger rounded-3 d-flex align-items-start small">
      <i class="bi bi-exclamation-triangle-fill me-2 mt-1"></i>
      <div>{{ error }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

/** Props & Emits */
const emit = defineEmits(['close'])

/** API 엔드포인트 */
const ENDPOINTS = {
  me: '/api/user/me'
}

/** 상태 관리 */
const loading = ref(false)
const sending = ref(false)
const copied = ref(false)
const error = ref('')
const sendSuccess = ref(false)
const showWarning = ref(false)

const code = ref('')
const expiresAt = ref('')
const guardianName = ref('')
const guardianPhone = ref('')

/* =========================
   API 호출
========================= */
async function request(url, options = {}) {
  const res = await fetch(url, { credentials: 'include', ...options })
  const ct = res.headers.get('content-type') || ''
  const isJson = ct.includes('application/json')
  const body = isJson ? await res.json().catch(() => ({})) : await res.text().catch(() => '')
  if (!res.ok) throw new Error(typeof body === 'string' ? body : (body.message || `${res.status}`))
  return body
}

/* =========================
   코드 불러오기
========================= */
async function loadInvite() {
  loading.value = true
  error.value = ''
  try {
    const me = await request(ENDPOINTS.me)
    code.value = me?.invitationCode || ''
    const raw = me?.invitationExpireAt || me?.invitationExpiredAt || ''
    expiresAt.value = raw ? formatDate(raw) : ''
  } catch (e) {
    error.value = `코드 불러오기 실패: ${e?.message || e}`
  } finally {
    loading.value = false
  }
}

function formatDate(isoStr) {
  try {
    const d = new Date(isoStr)
    if (!isFinite(d)) return isoStr
    const yy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const hh = String(d.getHours()).padStart(2, '0')
    const mi = String(d.getMinutes()).padStart(2, '0')
    return `${yy}-${mm}-${dd} ${hh}:${mi}`
  } catch {
    return isoStr
  }
}

/* =========================
   코드 복사
========================= */
async function copyCode() {
  if (!code.value) return

  error.value = ''
  sendSuccess.value = false
  showWarning.value = false

  try {
    await navigator.clipboard.writeText(code.value)
    copied.value = true
    setTimeout(() => copied.value = false, 1500)
  } catch {
    // 클립보드 API 실패 시 fallback
    const textarea = document.createElement('textarea')
    textarea.value = code.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    copied.value = true
    setTimeout(() => copied.value = false, 1500)
  }
}

/* =========================
   전송하기
========================= */
async function sendInvite() {
  // 모든 메시지 초기화
  error.value = ''
  sendSuccess.value = false
  showWarning.value = false

  if (!code.value) {
    error.value = '코드가 없습니다.'
    return
  }

  // 보호자 정보 입력 확인
  if (!guardianName.value || !guardianPhone.value) {
    showWarning.value = true
    return
  }

  sending.value = true

  try {
    // 1초 대기 후 완료 메시지 표시
    await new Promise(resolve => setTimeout(resolve, 1000))
    sendSuccess.value = true
  } catch (e) {
    error.value = `전송 실패: ${e?.message || e}`
  } finally {
    sending.value = false
  }
}

/* =========================
   뒤로 가기
========================= */
function goBack() {
  router.go(-1)
}

function goHome() {
    router.push('/DP');
}

/* =========================
   초기화
========================= */
onMounted(loadInvite)
</script>

<style scoped>
/* 전체 컨테이너 */
.invite-code-content {
  padding: 0.5rem;
  color: #171717;
}

/* 카드 공통 스타일 */
.code-card,
.info-card {
  border: 1px solid #E5E5E5;
  transition: transform 0.2s, box-shadow 0.2s;
}

.code-card:hover,
.info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
}

/* 아이콘 원형 배경 */
.icon-circle {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, rgba(74, 98, 221, 0.1) 0%, rgba(74, 98, 221, 0.2) 100%);
  border: 2px solid rgba(74, 98, 221, 0.2);
}

.icon-circle i {
  font-size: 32px;
  color: rgba(74, 98, 221, 0.85);
}

/* 카드 제목 */
.card-title {
  color: #171717;
  font-size: 1.25rem;
}

.card-subtitle {
  line-height: 1.6;
   color: #171717 !important;
}

/* 코드 표시 영역 */
.code-display {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: 2px dashed rgba(74, 98, 221, 0.3);
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.code-text {
  font-size: 1.5rem;
  letter-spacing: 0.3rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, 'Courier New', monospace;
  color: rgba(74, 98, 221, 0.95);
}

.form-label{
   color: #171717 !important;
}

/* 코드 복사 버튼 */
.btn-action {
  background: linear-gradient(135deg, rgba(74, 98, 221, 0.9) 0%, rgba(74, 98, 221, 0.8) 100%);
  border: 1px solid rgba(74, 98, 221, 0.3);
  color: white;
  padding: 0.75rem;
  font-weight: 500;
  border-radius: 12px;
  transition: all 0.2s;
}

.btn-action:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(74, 98, 221, 1) 0%, rgba(74, 98, 221, 0.9) 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(74, 98, 221, 0.3);
  color: white;
}

.btn-action:disabled {
  background: #e9ecef;
  border-color: #dee2e6;
  color: #6c757d;
  cursor: not-allowed;
}

/* 섹션 제목 */
.section-title {
  color: #171717;
  font-size: 1rem;
  margin-bottom: 0;
}

/* 커스텀 인풋 */
.custom-input {
  border: 2px solid #E5E5E5;
  border-radius: 10px;
  padding: 0.65rem 0.85rem;
  transition: all 0.2s;
  font-size: 0.95rem;
}

.custom-input:focus {
  border-color: rgba(74, 98, 221, 0.5);
  box-shadow: 0 0 0 0.2rem rgba(74, 98, 221, 0.1);
  outline: none;
}

.custom-input::placeholder {
  color: #adb5bd;
}

/* 전송 버튼 */
.btn-send {
  background: linear-gradient(135deg, #4A62DD 0%, #3D51C8 100%);
  border: none;
  color: white;
  padding: 1rem;
  font-weight: 600;
  font-size: 1.05rem;
  border-radius: 12px;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(74, 98, 221, 0.3);
}

.btn-send:hover:not(:disabled) {
  background: linear-gradient(135deg, #3D51C8 0%, #2E3FA3 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(74, 98, 221, 0.4);
  color: white;
}

.btn-send:disabled {
  background: linear-gradient(135deg, #adb5bd 0%, #868e96 100%);
  cursor: not-allowed;
  box-shadow: none;
}

/* 알림 스타일 */
.alert {
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.alert-danger {
  background: linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%);
  color: #721c24;
}

/* 경고 메시지 (보호자 정보 미입력) */
.alert-warning-custom {
  background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);
  color: #856404;
  border: 2px solid rgba(255, 193, 7, 0.4);
  padding: 0.75rem 1rem;
  animation: shake 0.5s;
}

.alert-warning-custom i {
  color: #ffc107;
  font-size: 1.1rem;
}

@keyframes shake {

  0%,
  100% {
    transform: translateX(0);
  }

  10%,
  30%,
  50%,
  70%,
  90% {
    transform: translateX(-5px);
  }

  20%,
  40%,
  60%,
  80% {
    transform: translateX(5px);
  }
}

/* 성공 메시지 (클릭하여 뒤로가기) */
.alert-success-custom {
  background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
  color: #155724;
  border: 2px solid rgba(40, 167, 69, 0.3);
  transition: all 0.2s;
  animation: slideIn 0.3s ease-out;
  padding: 0.75rem 1rem;
}

.alert-success-custom:hover {
  background: linear-gradient(135deg, #c3e6cb 0%, #b1dfbb 100%);
  transform: translateX(-3px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.2);
}

.alert-success-custom i.bi-check-circle-fill {
  color: #28a745;
  font-size: 1.1rem;
}

.alert-success-custom i.bi-arrow-left {
  color: #28a745;
  font-size: 1.3rem;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 스피너 */
.spinner-border-sm {
  width: 1rem;
  height: 1rem;
  border-width: 0.15rem;
}

/* 환자 헤더 */
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 70px;
  padding: 0 24px;
  background-color: #FFFFFF;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.icon-wrapper {
  cursor: pointer;
}

.icon-wrapper .icon {
  width: 24px;
  height: 24px;
  fill: #000000;
}

.icon-bold {
  font-size: 1.3rem;
  -webkit-text-stroke: 0.8px currentColor;
}

.logo-image {
  height: 32px;
  /* 또는 원하는 크기로 조정 */
  object-fit: contain;
}

/* 반응형 */
@media (max-width: 414px) {
  .code-text {
    font-size: 1.2rem;
    letter-spacing: 0.2rem;
  }
}
</style>

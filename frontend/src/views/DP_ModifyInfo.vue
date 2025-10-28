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

  <div class="content-section">
    <div class="container py-2">
      <!-- 프로필 이미지 섹션 -->
      <div class="profile-image-section text-center mb-5">
        <div class="profile-image-wrapper">
          <!-- 지현 수정: 프로필 사진 표시 -->
          <div class="profile-image">
            <img v-if="formData.profilePhoto" :src="formData.profilePhoto" alt="Profile" class="profile-photo-img" />
            <i v-else class="bi bi-person-circle"></i>
          </div>
          <!-- 지현 수정: 파일 input 추가 -->
          <input type="file" ref="fileInput" accept="image/*" @change="handlePhotoChange" style="display: none" />
          <button class="profile-image-edit-btn" @click="triggerFileInput">
            <i class="bi bi-camera-fill"></i>
          </button>
        </div>
        <div class="profile-image-label mt-3">사진 변경</div>
      </div>

      <!-- 입력 폼 섹션 -->
      <div class="form-section">
        <!-- 이름 -->
        <div class="form-group mb-4">
          <label class="form-label">이름</label>
          <input type="text" class="form-control custom-input" v-model="formData.name" placeholder="이름을 입력하세요">
        </div>

        <!-- 아이디 (읽기 전용) -->
        <div class="form-group mb-4">
          <label class="form-label">아이디</label>
          <input type="text" class="form-control custom-input-disabled" v-model="formData.userId" disabled readonly>
          <div class="form-hint">아이디는 변경할 수 없습니다</div>
        </div>

        <!-- 비밀번호 -->
        <div class="form-group mb-4">
          <label class="form-label">비밀번호</label>
          <div class="password-input-wrapper">
            <input :type="showPassword ? 'text' : 'password'" class="form-control custom-input"
              v-model="formData.password" placeholder="비밀번호">
            <button class="password-toggle-btn" @click="togglePassword" type="button">
              <i :class="showPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>
        </div>

        <!-- 생년월일 -->
        <div class="form-group mb-4">
          <label class="form-label">생년월일</label>
          <div class="date-input-wrapper">
            <input type="date" class="form-control custom-input" v-model="formData.birthDate">
            <i class="bi bi-calendar3 calendar-icon"></i>
          </div>
        </div>

        <!-- 전화번호 -->
        <div class="form-group mb-5">
          <label class="form-label">전화번호</label>
          <input type="tel" class="form-control custom-input" v-model="formData.phone" placeholder="010-0000-0000"
            inputmode="tel">
        </div>
      </div>

      <!-- 버튼 그룹 -->
      <div class="button-group">
        <button class="btn btn-save w-100 mb-3" @click="handleSave" :disabled="saving">
          <span v-if="saving">
            <span class="spinner-border spinner-border-sm me-2"></span>
            저장 중...
          </span>
          <span v-else>수정 완료</span>
        </button>

        <button class="btn btn-cancel w-100" @click="handleCancel">
          취소
        </button>
      </div>

      <!-- 성공 메시지 -->
      <div v-if="saveSuccess"
        class="alert alert-success-custom rounded-3 d-flex align-items-center justify-content-between small mt-3"
        @click="goBack" style="cursor: pointer;">
        <div class="d-flex align-items-center">
          <i class="bi bi-check-circle-fill me-2"></i>
          <strong>수정이 완료되었습니다<br>클릭하여 돌아가기</strong>
        </div>
        <i class="bi bi-arrow-left"></i>
      </div>

      <!-- 에러 메시지 -->
      <div v-if="error" class="alert alert-danger rounded-3 d-flex align-items-start small mt-3">
        <i class="bi bi-exclamation-triangle-fill me-2 mt-1"></i>
        <div>{{ error }}</div>
      </div>
    </div>
  </div>

</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const API_BASE = 'https://localhost:8080'

/** 상태 관리 */
const saving = ref(false)
const saveSuccess = ref(false)
const error = ref('')
const showPassword = ref(false)
const fileInput = ref(null) // ✅ 추가

/** 폼 데이터 (UI 바인딩) */
const formData = ref({
  name: '',
  userId: '',
  birthDate: '',
  phone: '',
  profilePhoto: ''
})

/** API 엔드포인트 (백엔드 규약) */
const ENDPOINTS = {
  me: `${API_BASE}/api/user/me`,
  update: `${API_BASE}/api/user/update`,
  uploadPhoto: `${API_BASE}/api/upload/profile-photo` // 지현 추가
}

/* ============ 유틸 ============ */
function toYYYYMMDD(d) {
  if (!d) return ''
  const date = new Date(d)
  if (Number.isNaN(date.getTime())) {
    if (/^\d{4}-\d{2}-\d{2}$/.test(String(d))) return String(d)
    return ''
  }
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function normalizePhone(p) {
  if (!p) return ''
  const digits = String(p).replace(/\D/g, '')
  if (digits.length === 11) return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
  if (digits.length === 10) return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`
  return p
}

async function request(url, options = {}) {
  const res = await fetch(url, { credentials: 'include', ...options })
  const ct = res.headers.get('content-type') || ''
  const isJson = ct.includes('application/json')
  const body = isJson ? await res.json().catch(() => ({})) : await res.text().catch(() => '')
  if (!res.ok) throw new Error(typeof body === 'string' ? body : (body.message || `${res.status}`))
  return body
}

/* ============ 초기 데이터 로드 ============ */
async function loadUserData() {
  error.value = ''
  try {
    const user = await request(ENDPOINTS.me)

    if (user?.roleNo !== 2 && user?.roleNo !== 3) {
      router.replace('/GD')
      return
    }

    const name = user?.name ?? ''
    const userId = user?.userId ?? user?.user_id ?? ''
    const birthRaw = user?.birthDate ?? user?.birth_date ?? null
    const phoneRaw = user?.phoneNumber ?? user?.phone_number ?? user?.phone ?? ''
    const photo = user?.profilePhoto ?? user?.profile_photo ?? ''

    formData.value = {
      name: name || '',
      userId: userId || '',
      birthDate: toYYYYMMDD(birthRaw) || '',
      phone: normalizePhone(phoneRaw) || '',
      profilePhoto: photo || ''
    }
  } catch (e) {
    error.value = `사용자 정보 불러오기 실패: ${e?.message || e}`
  }
}

/* ============ 프로필 이미지 변경 ============ */
// 파일 선택 트리거
function triggerFileInput() {
  fileInput.value?.click()
}

// 사진 변경 처리
async function handlePhotoChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  // 파일 크기 체크 (10MB 제한)
  if (file.size > 10 * 1024 * 1024) {
    alert('파일 크기는 2MB 이하여야 합니다.')
    event.target.value = ''
    return
  }

  // 이미지 파일인지 체크
  if (!file.type.startsWith('image/')) {
    alert('이미지 파일만 업로드 가능합니다.')
    event.target.value = ''
    return
  }

  try {
    const formDataUpload = new FormData()
    formDataUpload.append('file', file)

    const response = await fetch(ENDPOINTS.uploadPhoto, {
      method: 'POST',
      credentials: 'include',
      body: formDataUpload
    })

    const result = await response.json()

    if (!response.ok) {
      throw new Error(result.message || '업로드 실패')
    }

    if (result.success && result.imageUrl) {
      formData.value.profilePhoto = result.imageUrl
      event.target.value = ''
    }
  } catch (e) {
    alert(`프로필 사진 업로드 실패: ${e?.message || e}`)
  }
}


/* ============ 비밀번호 표시/숨김 ============ */
function togglePassword() {
  showPassword.value = !showPassword.value
}

/* ============ 저장(업데이트) ============ */
async function handleSave() {
  error.value = ''
  saveSuccess.value = false

  // 간단 유효성
  if (!formData.value.name || !formData.value.phone) {
    error.value = '필수 항목을 모두 입력해주세요.'
    return
  }
  if (formData.value.birthDate && !/^\d{4}-\d{2}-\d{2}$/.test(formData.value.birthDate)) {
    error.value = '생년월일 형식이 올바르지 않습니다. (예: 1970-01-31)'
    return
  }

  const payload = {
    name: formData.value.name,
    birthDate: formData.value.birthDate || null,
    phoneNumber: formData.value.phone || null,
    profilePhoto: formData.value.profilePhoto || null
  }

  saving.value = true
  try {
    await request(ENDPOINTS.update, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    saveSuccess.value = true
  } catch (e) {
    error.value = `저장 실패: ${e?.message || e}`
  } finally {
    saving.value = false
  }
}

/* ============ 취소/네비 ============ */
function handleCancel() {
  router.go(-1)
}
function goHome() {
  router.push('/DP')
}

function goBack() {
  router.go(-1)
}

/* ============ 초기화 ============ */
onMounted(loadUserData)
</script>

<style scoped>
.profile-edit-page {
  min-height: 100vh;
  background: #f8f9fa;
  color: #171717;
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

/* 컨테이너 */
.container {
  max-width: 420px;
  padding-left: 1.5rem;
  padding-right: 1.5rem;
}

/* 프로필 이미지 섹션 */
.profile-image-section {
  padding: 2rem 0; 
}

.profile-image-wrapper {
  position: relative;
  display: inline-block;
}

.profile-image {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: white;
  border: 4px solid #E5E5E5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.profile-image i {
  font-size: 140px;
  color: #d4d4d4;
}

.profile-image-edit-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 40px;  /* 32px → 40px */
  height: 40px; /* 32px → 40px */
  background: rgba(74, 98, 221, 1);
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.profile-image-edit-btn:hover {
  transform: scale(1.1);
}

.profile-image-edit-btn i {
  color: white;
  font-size: 1rem; /* 0.875rem → 1rem */
}

.profile-image-label {
  color: #525252;
  font-size: 1.125rem;
  -webkit-text-stroke: 0.5px currentColor;
}

.profile-photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top; /* 상단 중앙 정렬 */
  border-radius: 50%;
}


/* 폼 섹션 */
.form-section {
  margin-top: -40px;
  margin-bottom: 2rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  color: #171717;
  font-size: 1.25rem;
  font-weight: 400;
  margin-bottom: 0.75rem;
}

.custom-input {
  width: 100%;
  padding: 1.25rem 1.5rem;
  font-size: 1.25rem;
  border: 2px solid #D4D4D4;
  border-radius: 8px;
  transition: all 0.2s;
  background: white;
  color: #171717;
}

.custom-input:focus {
  border-color: rgba(74, 98, 221, 0.5);
  box-shadow: 0 0 0 0.2rem rgba(74, 98, 221, 0.1);
  outline: none;
}

.custom-input-disabled {
  width: 100%;
  padding: 1.25rem 1.5rem;
  font-size: 1.25rem;
  border: 2px solid #D4D4D4;
  border-radius: 8px;
  background: #F5F5F5;
  color: #171717;
  cursor: not-allowed;
}

.form-hint {
  margin-top: 0.5rem;
  color: #737373;
  font-size: 1rem;
  line-height: 1.5;
}

/* 비밀번호 입력 */
.password-input-wrapper {
  position: relative;
}

.password-toggle-btn {
  position: absolute;
  right: 1.5rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  transition: all 0.2s;
}

.password-toggle-btn i {
  font-size: 1.25rem;
  color: #737373;
}

.password-toggle-btn:hover i {
  color: #404040;
}

/* 날짜 입력 */
.date-input-wrapper {
  position: relative;
}

.calendar-icon {
  position: absolute;
  right: 1.5rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1.25rem;
  color: #171717;
  pointer-events: none;
}

/* 날짜 입력 필드의 기본 아이콘 숨기기 */
.custom-input[type="date"]::-webkit-calendar-picker-indicator {
  opacity: 0;
  position: absolute;
  right: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

/* 버튼 그룹 */
.button-group {
  margin-top: 3rem;
}

.btn-save {
  background: rgba(74, 98, 221, 1);
  border: none;
  color: white;
  padding: 1.375rem;
  font-size: 1.25rem;
  font-weight: 400;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  color: white;
}

.btn-save:disabled {
  background: #737373;
  cursor: not-allowed;
}

.btn-cancel {
  background: white;
  border: 2px solid #D4D4D4;
  color: #404040;
  padding: 1.375rem;
  font-size: 1.25rem;
  font-weight: 400;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f5f5f5;
  border-color: #a3a3a3;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 성공 메시지 */
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

/* 에러 메시지 */
.alert-danger {
  background: linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%);
  color: #721c24;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 애니메이션 */
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

/* 반응형 */
@media (max-width: 414px) {
  .container {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .form-label {
    font-size: 1.125rem;
  }

  .custom-input,
  .custom-input-disabled {
    font-size: 1.125rem;
    padding: 1rem 1.25rem;
  }

  .btn-save,
  .btn-cancel {
    font-size: 1.125rem;
    padding: 1.25rem;
  }
}

/* 반응형 */
@media (max-width: 414px) {
  .code-text {
    font-size: 1.2rem;
    letter-spacing: 0.2rem;
  }
}
</style>

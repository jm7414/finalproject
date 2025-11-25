<template>
  <div class="guardian-profile-edit-page">
    <!-- 메인 컨텐츠 -->
    <div class="content-section">
      <div class="container py-3">
        <!-- 프로필 이미지 섹션 -->
        <div class="profile-image-section text-center mb-4">
          <div class="profile-image-wrapper">
            <!-- 지현수정: 프로필 사진 표시 추가 -->
            <div class="profile-image">
              <img v-if="formData.profilePhoto" :src="formData.profilePhoto" alt="Profile" class="profile-photo-img" />
              <i v-else class="bi bi-person-circle"></i>
            </div>
            <!-- 지현수정: 파일 input 추가 -->
            <input 
              type="file" 
              ref="fileInput" 
              accept="image/*" 
              @change="handlePhotoChange"
              style="display: none"
            />
            <!-- 지현수정: triggerFileInput으로 함수명 변경 -->
            <button class="profile-image-edit-btn" @click="triggerFileInput">
              <i class="bi bi-camera-fill"></i>
            </button>
          </div>
          <div class="profile-image-label mt-2">사진 변경</div>
        </div>

        <!-- 입력 폼 섹션 -->
        <div class="form-section">
          <!-- 이름 -->
          <div class="form-group mb-3">
            <label class="form-label">이름</label>
            <input 
              type="text" 
              class="form-control custom-input" 
              v-model="formData.name"
              placeholder="이름을 입력하세요"
            >
          </div>

          <!-- 아이디 (읽기 전용) -->
          <div class="form-group mb-3">
            <label class="form-label">아이디</label>
            <input 
              type="text" 
              class="form-control custom-input-disabled" 
              v-model="formData.userId"
              disabled
              readonly
            >
            <div class="form-hint">아이디는 변경할 수 없습니다</div>
          </div>

          <!-- 비밀번호 -->
          <div class="form-group mb-3">
            <label class="form-label">비밀번호</label>
            <div class="password-input-wrapper">
              <input 
                :type="showPassword ? 'text' : 'password'" 
                class="form-control custom-input" 
                v-model="formData.password"
                placeholder="비밀번호"
              >
              <button 
                class="password-toggle-btn" 
                @click="togglePassword"
                type="button"
              >
                <i :class="showPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
              </button>
            </div>
          </div>

          <!-- 생년월일 -->
          <div class="form-group mb-3">
            <label class="form-label">생년월일</label>
            <div class="date-input-wrapper">
              <input 
                type="date" 
                class="form-control custom-input" 
                v-model="formData.birthDate"
              >
              <i class="bi bi-calendar3 calendar-icon"></i>
            </div>
          </div>

          <!-- 전화번호 -->
          <div class="form-group mb-4">
            <label class="form-label">전화번호</label>
            <input 
              type="tel" 
              class="form-control custom-input" 
              v-model="formData.phone"
              placeholder="010-0000-0000"
              inputmode="tel"
            >
          </div>
        </div>

        <!-- 버튼 그룹 -->
        <div class="button-group">
          <button 
            class="btn btn-save w-100 mb-2" 
            @click="handleSave"
            :disabled="saving"
          >
            <span v-if="saving">
              <span class="spinner-border spinner-border-sm me-2"></span>
              저장 중...
            </span>
            <span v-else>수정 완료</span>
          </button>
          
          <button 
            class="btn btn-cancel w-100" 
            @click="handleCancel"
          >
            취소
          </button>
        </div>

        <!-- 성공 메시지 -->
        <div 
          v-if="saveSuccess" 
          class="alert alert-success-custom rounded-3 d-flex align-items-center justify-content-between small mt-3"
          @click="goBack"
          style="cursor: pointer;"
        >
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 상태 관리
const saving = ref(false)
const saveSuccess = ref(false)
const error = ref('')
const showPassword = ref(false)
const fileInput = ref(null) // 지현수정: 파일 input ref 추가

// 폼 데이터
const formData = ref({
  name: '',
  userId: '',
  birthDate: '',
  phone: '',
  profilePhoto: '' // 지현수정: 프로필 사진 URL 추가
})

// API 엔드포인트
// 상대 경로를 사용하여 Vite 프록시를 통해 백엔드로 요청
const ENDPOINTS = {
  me: '/api/user/me',
  update: '/api/user/update',
  uploadPhoto: '/api/upload/profile-photo' // 지현수정: 프로필 사진 업로드 API 추가
}

// 유틸 함수
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
  if (digits.length === 11) return `${digits.slice(0,3)}-${digits.slice(3,7)}-${digits.slice(7)}`
  if (digits.length === 10) return `${digits.slice(0,3)}-${digits.slice(3,6)}-${digits.slice(6)}`
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

// 초기 데이터 로드
async function loadUserData() {
  error.value = ''
  try {
    const user = await request(ENDPOINTS.me)

    if (user?.roleNo !== 1) {
      router.replace('/GD')
      return
    }

    const name = user?.name ?? ''
    const userId = user?.userId ?? user?.user_id ?? ''
    const birthRaw = user?.birthDate ?? user?.birth_date ?? null
    const phoneRaw = user?.phoneNumber ?? user?.phone_number ?? ''
    const photo = user?.profilePhoto ?? user?.profile_photo ?? '' // 지현수정: 프로필 사진 URL 로드

    formData.value = {
      name: name || '',
      userId: userId || '',
      birthDate: toYYYYMMDD(birthRaw) || '',
      phone: normalizePhone(phoneRaw) || '',
      profilePhoto: photo || '' // 지현수정: 프로필 사진 URL 설정
    }
  } catch (e) {
    error.value = `사용자 정보 불러오기 실패: ${e?.message || e}`
  }
}

// 지현수정: 파일 선택 트리거 함수 추가
function triggerFileInput() {
  fileInput.value?.click()
}

// 지현수정: 사진 변경 처리 함수 추가
async function handlePhotoChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    alert('파일 크기는 10MB 이하여야 합니다.')
    event.target.value = ''
    return
  }

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

// 비밀번호 표시/숨김
function togglePassword() {
  showPassword.value = !showPassword.value
}

// 저장
async function handleSave() {
  error.value = ''
  saveSuccess.value = false

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
    profilePhoto: formData.value.profilePhoto || null // 지현수정: 프로필 사진 URL 포함
  }

  saving.value = true
  try {
    await request(ENDPOINTS.update, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    saveSuccess.value = true
    await loadUserData()
  } catch (e) {
    error.value = `저장 실패: ${e?.message || e}`
  } finally {
    saving.value = false
  }
}

// 취소/네비
function handleCancel() {
  router.go(-1)
}

function goBack() {
  router.go(-1)
}

// 초기화
onMounted(loadUserData)
</script>

<style scoped>
.guardian-profile-edit-page {
  min-height: 80vh;
  background: #f8f9fa;
  color: #171717;
  margin-top: -15px;
}

.container {
  max-width: 420px;
  padding-left: 1.5rem;
  padding-right: 1.5rem;
}

/* 프로필 이미지 섹션 */
.profile-image-section {
  padding: 1rem 0;
}

.profile-image-wrapper {
  position: relative;
  display: inline-block;
}

.profile-image {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: white;
  border: 3px solid #E5E5E5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.profile-image i {
  font-size: 80px;
  color: #d4d4d4;
}

/* 지현수정: 프로필 사진 이미지 스타일 추가 */
.profile-photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  border-radius: 50%;
}

.profile-image-edit-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 28px;
  height: 28px;
  background: rgba(74, 98, 221, 0.8);
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.profile-image-edit-btn:hover {
  background: rgba(74, 98, 221, 1);
  transform: scale(1.1);
}

.profile-image-edit-btn i {
  color: white;
  font-size: 0.75rem;
}

.profile-image-label {
  color: #525252;
  font-size: 1rem;
}

/* 폼 섹션 */
.form-section {
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  color: #171717;
  font-size: 1rem;
  font-weight: 400;
  margin-bottom: 0.5rem;
}

.custom-input {
  width: 100%;
  padding: 0.875rem 1rem;
  font-size: 1rem;
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
  padding: 0.875rem 1rem;
  font-size: 1rem;
  border: 2px solid #D4D4D4;
  border-radius: 8px;
  background: #F5F5F5;
  color: #171717;
  cursor: not-allowed;
}

.form-hint {
  margin-top: 0.375rem;
  color: #737373;
  font-size: 0.875rem;
}

/* 비밀번호 입력 */
.password-input-wrapper {
  position: relative;
}

.password-toggle-btn {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.375rem;
}

.password-toggle-btn i {
  font-size: 1.125rem;
  color: #737373;
}

/* 날짜 입력 */
.date-input-wrapper {
  position: relative;
}

.calendar-icon {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1.125rem;
  color: #171717;
  pointer-events: none;
}

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
  margin-top: 2rem;
}

.btn-save {
  background: rgba(74, 98, 221, 0.8);
  border: none;
  color: white;
  padding: 1rem;
  font-size: 1rem;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-save:hover:not(:disabled) {
  background: rgba(74, 98, 221, 1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.btn-save:disabled {
  background: #737373;
  cursor: not-allowed;
}

.btn-cancel {
  background: white;
  border: 2px solid #D4D4D4;
  color: #404040;
  padding: 1rem;
  font-size: 1rem;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f5f5f5;
  transform: translateY(-1px);
}

/* 성공 메시지 */
.alert-success-custom {
  background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
  color: #155724;
  border: 2px solid rgba(40, 167, 69, 0.3);
  padding: 0.75rem 1rem;
}

.alert-success-custom i.bi-check-circle-fill {
  color: #28a745;
}

/* 에러 메시지 */
.alert-danger {
  background: linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%);
  color: #721c24;
}

.spinner-border-sm {
  width: 0.875rem;
  height: 0.875rem;
  border-width: 0.125rem;
}
</style>

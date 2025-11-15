<template>
  <div v-if="show" class="modal-overlay" @click.self="handleCancel">
    <div class="my-info-modal">
      <div class="modal-header">
        <span>내 정보 수정</span>
        <button class="close-btn" @click="handleCancel">✕</button>
      </div>
      <div class="modal-content">
        <div class="profile-image-section">
          <div class="profile-image-wrapper">
            <div class="profile-image">
              <img v-if="form.profilePhoto" :src="form.profilePhoto" alt="프로필" />
              <i v-else class="bi bi-person-circle"></i>
            </div>
            <input type="file" ref="fileInput" accept="image/*" @change="handlePhotoChange" style="display:none" />
            <button class="profile-image-edit-btn" @click="triggerFileInput">
              <i class="bi bi-camera-fill"></i>
            </button>
          </div>
          <div class="profile-image-label">사진 변경</div>
        </div>

        <form @submit.prevent="handleSave">
          <label class="form-label">이름</label>
          <input class="form-input" v-model="form.name" disabled readonly />

          <label class="form-label">아이디</label>
          <input class="form-input" v-model="form.userId" disabled readonly />
          <div class="form-hint">아이디는 변경할 수 없습니다</div>

          <label class="form-label">비밀번호</label>
          <div class="password-row">
            <input :type="showPassword ? 'text' : 'password'" class="form-input" v-model="form.password" autocomplete="new-password" placeholder="비밀번호" />
            <button type="button" @click="showPassword = !showPassword" class="password-toggle-btn">
              <i :class="showPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>

          <label class="form-label">생년월일</label>
          <input type="date" class="form-input" v-model="form.birthDate" />

          <label class="form-label">전화번호</label>
          <input class="form-input" v-model="form.phoneNumber" inputmode="tel" placeholder="010-0000-0000" />
        </form>
        <div v-if="error" class="error-msg mt-1">{{ error }}</div>
      </div>
      <div class="modal-footer">
        <button v-if="saveSuccess" class="save-success-btn" @click="handleCancel">
          수정 완료
        </button>
        <template v-else>
          <button class="save-btn" :disabled="saving" @click="handleSave">
            {{ saving ? '저장 중...' : '저장' }}
          </button>
          <button class="cancel-btn" :disabled="saving" @click="handleCancel">
            취소
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import axios from 'axios'

const props = defineProps({ show: Boolean })
const emit = defineEmits(['close', 'saved'])

const fileInput = ref(null)
const saving = ref(false)
const saveSuccess = ref(false)
const error = ref('')
const showPassword = ref(false)

const form = reactive({
  profilePhoto: '',
  name: '',
  userId: '',
  password: '',
  birthDate: '',
  phoneNumber: ''
})

async function loadUser() {
  error.value = ''
  saveSuccess.value = false
  try {
    const res = await axios.get('/api/user/me', { withCredentials: true })
    Object.assign(form, {
      profilePhoto: res.data.profilePhoto || '',
      name: res.data.name || '',
      userId: res.data.userId || res.data.userid || '',
      birthDate: formatBirth(res.data.birthDate || res.data.birth_date),
      phoneNumber: res.data.phoneNumber || res.data.phone_number || '',
      password: ''
    })
  } catch {
    error.value = '사용자 정보 불러오기 실패'
  }
}

function formatBirth(val) {
  if (!val) return ''
  if (/^\d{4}-\d{2}-\d{2}$/.test(val)) return val
  try {
    return new Date(val).toISOString().slice(0, 10)
  } catch {
    return ''
  }
}

function triggerFileInput() {
  fileInput.value?.click()
}

async function handlePhotoChange(ev) {
  const file = ev.target.files[0]
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    alert('10MB 이하 이미지만 업로드')
    ev.target.value = ''
    return
  }
  if (!file.type.startsWith('image/')) {
    alert('이미지 파일만 가능')
    ev.target.value = ''
    return
  }
  try {
    const formDataUpload = new FormData()
    formDataUpload.append('file', file)
    const res = await axios.post('/api/upload/profile-photo', formDataUpload, {
      withCredentials: true,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.success && res.data.imageUrl) {
      form.profilePhoto = res.data.imageUrl
    } else {
      throw new Error('업로드 실패')
    }
  } catch (e) {
    alert('사진 업로드 실패: ' + (e.message || ''))
  }
}

async function handleSave() {
  error.value = ''
  saveSuccess.value = false
  if (!form.name || !form.userId) {
    error.value = '이름/아이디 없음'
    return
  }
  saving.value = true
  try {
    await axios.post(
      '/api/user/update',
      {
        name: form.name,
        profilePhoto: form.profilePhoto,
        birthDate: form.birthDate,
        phoneNumber: form.phoneNumber,
        ...(form.password ? { password: form.password } : {})
      },
      { withCredentials: true }
    )
    saveSuccess.value = true
    emit('saved')
  } catch (e) {
    error.value = e.response?.data?.message || '저장 오류'
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  emit('close')
}

watch(() => props.show, (v) => {
  if (v) loadUser()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.my-info-modal {
  position: relative;
  height: 672px;
  max-width: 380px;
  width: 96%;
  font-size: 1rem;
  margin: 0 auto;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  color: #171717;
  overflow: hidden;
  transform: scale(0.9);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 1.15rem;
  font-weight: 700;
  padding: 16px 16px 8px 20px;
}

.close-btn {
  font-size: 22px;
  background: none;
  border: none;
  color: #a7cc10;
  cursor: pointer;
  font-weight: 700;
}

.modal-content {
  padding: 0 14px 0 14px;
  flex: 1 1 auto;
  overflow-y: auto;
}

.profile-image-section {
  text-align: center;
  margin-bottom: 5px;
}

.profile-image-wrapper {
  position: relative;
  display: inline-block;
}

.profile-image {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: #fff;
  border: 3px solid #e5e5e5;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.profile-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
}

.profile-image i {
  font-size: 60px;
  color: #dadada;
}

.profile-image-edit-btn {
  position: absolute;
  bottom: 4px;
  right: 2px;
  width: 22px;
  height: 22px;
  background: #a7cc10;
  border: none;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  font-size: 1em;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-image-edit-btn:hover {
  background: #8fb80e;
}

.profile-image-label {
  font-size: 0.83rem;
  color: #859848;
  margin-top: 2px;
}

.form-label {
  display: block;
  margin: 9px 0 4px 2px;
  font-weight: 700;
  color: #29370d;
}

.form-input {
  width: 100%;
  padding: 7px 10px;
  font-size: 1rem;
  border-radius: 8px;
  border: 1.5px solid #a7cc10;
  background: #fafbf3;
  color: #1a1a1a;
  margin-bottom: 2px;
  box-sizing: border-box;
}

.form-input[disabled] {
  color: #aaa;
  background: #f3f3f3;
}

.form-hint {
  margin: 4px 0 0 2px;
  color: #b5b5b5;
  font-size: 0.85rem;
}

.password-row {
  display: flex;
  align-items: center;
  position: relative;
  width: 100%;
  min-width: 0;
}

.password-row .form-input {
  flex: 1 1 0%;
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.password-toggle-btn {
  margin-left: -30px;
  border: none;
  background: none;
  font-size: 1.05em;
  color: #90b403;
  cursor: pointer;
  flex-shrink: 0;
  z-index: 2;
}

.modal-footer {
  margin: 12px 0 7px 0;
  margin-bottom: 20px;
  display: flex;
  gap: 15px;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
}

.save-btn,
.cancel-btn {
  flex: 1;
  font-size: 1.05rem;
  border-radius: 9px;
  font-weight: 700;
  padding: 11px 0;
  transition: background 0.18s, border-color 0.18s;
}

.save-btn {
  background: #a7cc10;
  color: #fff;
  border: none;
}

.save-btn:disabled {
  background: #c2d477;
  cursor: not-allowed;
}

.cancel-btn {
  background: #fff;
  border: 2px solid #a7cc10;
  color: #a7cc10;
}

.cancel-btn:hover {
  background: #f5fbe8;
  border-color: #90b403;
}

.error-msg {
  color: #d9534f;
  font-size: 0.93rem;
  margin-top: 7px;
}

.success-msg {
  color: #28a745;
  font-size: 0.98rem;
  margin-top: 6px;
  font-weight: 700;
}

.save-success-btn {
  background: #8fb80e;
  color: white;
  border: none;
  padding: 11px 0;
  font-size: 1.05rem;
  font-weight: 700;
  border-radius: 9px;
  width: 100%;
  cursor: pointer;
  transition: background 0.18s ease;
}

.save-success-btn:hover {
  background: #7aa00c;
}

</style>

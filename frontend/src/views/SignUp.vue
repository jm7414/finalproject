<!-- src/views/SignUp.vue -->
<template>
  <div class="sg-wrap position-relative mx-auto bg-white">
    <!-- 상단 웨이브: 화면 폭 끝까지 -->
    <svg class="deco-top" viewBox="0 0 414 160" preserveAspectRatio="none" aria-hidden="true">
      <path d="M0,0 H414 V95 C330,145 230,84 120,102 C70,110 28,132 0,160 Z" fill="rgba(74,98,221,0.85)" />
    </svg>

    <!-- 좌하단 블랍 (조금 더 보이게, 크게) -->
    <svg class="deco-blob-left" viewBox="0 0 400 320" preserveAspectRatio="xMinYMax meet" aria-hidden="true">
      <path d="M 0 40
           C 65 0, 135 25, 175 60
           C 215 95, 208 145, 206 170
           C 203 210, 245 235, 302 260
           C 350 282, 400 300, 400 320
           L 0 320 Z" fill="var(--brand-blob, rgba(126,136,255,.90))" />
    </svg>

    <!-- 타이틀 -->
    <div class="text-center">
      <h1 class="fw-bold sg-title">회원 가입</h1>
    </div>

    <!-- 폼 -->
    <form class="px-4 mt-2 form-offset" @submit.prevent="onSubmit">
      <!-- 성함 -->
      <div class="mb-4">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="8" r="4.5" stroke="#9A9A9A" stroke-width="1.6" />
              <path d="M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6" stroke="#9A9A9A" stroke-width="1.6" />
            </svg>
          </span>
          <input v-model.trim="form.name" type="text" class="form-control border-0 sg-pill-end" placeholder="성함"
            autocomplete="name" required />
        </div>
      </div>

      <!-- 아이디 + 중복체크 칩 -->
      <div class="mb-4 position-relative">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="8" r="4.5" stroke="#9A9A9A" stroke-width="1.6" />
              <path d="M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6" stroke="#9A9A9A" stroke-width="1.6" />
            </svg>
          </span>
          <input v-model.trim="form.username" type="text" class="form-control border-0 sg-pill-end" placeholder="아이디"
            autocomplete="username" @input="onUsernameChange" required />
        </div>
        <button type="button" class="btn rounded-pill id-chip shadow-sm" @click="checkDuplicate">
          중복체크
        </button>
      </div>

      <!-- 비밀번호 + 보기 토글 -->
      <div class="mb-4">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <rect x="4" y="10" width="16" height="10" rx="2" stroke="#9A9A9A" stroke-width="1.6" />
              <path d="M8 10V7a4 4 0 0 1 8 0v3" stroke="#9A9A9A" stroke-width="1.6" />
            </svg>
          </span>
          <input :type="showPw ? 'text' : 'password'" v-model="form.password" class="form-control border-0"
            placeholder="비밀번호" autocomplete="new-password" required />
          <button type="button" class="btn btn-white border-0 pe-3" @click="showPw = !showPw" aria-label="비밀번호 보기">
            <svg v-if="showPw" width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M2 12s4-6 10-6 10 6 10 6-4 6-10 6-10-6-10-6z" stroke="#6c757d" stroke-width="1.6" />
              <circle cx="12" cy="12" r="3" stroke="#6c757d" stroke-width="1.6" />
            </svg>
            <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M3 3l18 18" stroke="#6c757d" stroke-width="1.6" />
              <path d="M2 12s4-6 10-6a9.6 9.6 0 0 1 6 2" stroke="#6c757d" stroke-width="1.6" />
              <path d="M20.7 15.5C18.7 17.6 15.7 18 12 18 6 18 2 12 2 12a19 19 0 0 1 4-4" stroke="#6c757d"
                stroke-width="1.6" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 생년월일 -->
      <div class="mb-4">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <rect x="3" y="5" width="18" height="16" rx="2" stroke="#9A9A9A" stroke-width="1.6" />
              <path d="M3 9h18" stroke="#9A9A9A" stroke-width="1.6" />
              <path d="M8 3v4M16 3v4" stroke="#9A9A9A" stroke-width="1.6" />
            </svg>
          </span>
          <input v-model="form.birth" type="date" class="form-control border-0 sg-pill-end" placeholder="연도-월-일"
            required />
        </div>
      </div>

      <!-- 전화번호 -->
      <div class="mb-3">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M6 2h4l2 5-3 2a12 12 0 0 0 6 6l2-3 5 2v4a3 3 0 0 1-3 3A17 17 0 0 1 3 5a3 3 0 0 1 3-3z"
                stroke="#9A9A9A" stroke-width="1.6" />
            </svg>
          </span>
          <input v-model.trim="form.phone" type="tel" inputmode="tel" class="form-control border-0 sg-pill-end"
            placeholder="전화번호" required />
        </div>
      </div>

      <!-- 보호자 체크: 전화번호 바로 아래로 붙임 -->
      <div class="form-check guardian-row ms-1">
        <input class="form-check-input" type="checkbox" id="isGuardian" v-model="form.isGuardian" />
        <label class="form-check-label" for="isGuardian">보호자일 경우 체크</label>
      </div>

      <!-- 하단 텍스트와 버튼 -->
      <div class="mt-4 text-center">
        <button type="submit" class="btn btn-primary btn-lg rounded-pill px-5">회원가입</button>
        <p class="mt-3">
          이미 계정이 있으신가요? 
          <router-link to="/login" class="text-decoration-none text-primary">로그인</router-link>
        </p>
      </div>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const form = reactive({
  name: '',
  username: '',
  password: '',
  birth: '',
  phone: '',
  isGuardian: false
})

const showPw = ref(false)
const isIdChecked = ref(false)
const isIdAvailable = ref(false)

// 아이디 입력 시 중복 확인 상태 초기화
function onUsernameChange() {
  isIdChecked.value = false
  isIdAvailable.value = false
}

async function checkDuplicate() {
  if (!form.username) {
    alert('아이디를 입력하세요.')
    return
  }

  try {
    const response = await fetch(`http://localhost:8080/api/user/check-duplicate?userId=${form.username}`, {
      credentials: 'include'
    })
    const data = await response.json()

    if (response.ok) {
      isIdChecked.value = true
      if (data.isDuplicate) {
        isIdAvailable.value = false
        alert('이미 사용 중인 아이디입니다.')
      } else {
        isIdAvailable.value = true
        alert('사용 가능한 아이디입니다.')
      }
    } else {
      alert('중복 확인 중 오류가 발생했습니다.')
    }
  } catch (error) {
    console.error('중복 확인 중 오류 발생:', error)
    alert('중복 확인 중 오류가 발생했습니다.')
  }
}

async function onSubmit() {
  if (!form.name || !form.username || !form.password || !form.birth || !form.phone) {
    alert('필수 항목을 입력해주세요.')
    return
  }

  // 중복 확인 필수
  if (!isIdChecked.value || !isIdAvailable.value) {
    alert('아이디 중복 확인을 해주세요.')
    return
  }

  try {
    const response = await fetch('http://localhost:8080/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        userId: form.username,
        userPw: form.password,
        name: form.name,
        birthDate: form.birth,
        phoneNumber: form.phone,
        roleNo: form.isGuardian ? 1 : 2, // 보호자: 1, 환자: 2
      }),
    })

    const data = await response.json()

    if (response.ok) {
      alert('회원가입이 완료되었습니다.')
      router.push('/login')
    } else {
      alert(data.message || '회원가입에 실패했습니다.')
    }
  } catch (error) {
    console.error('회원가입 중 오류 발생:', error)
    alert('회원가입 처리 중 오류가 발생했습니다.')
  }
}
</script>

<style scoped>
/* 기존 스타일 유지 */
.sg-wrap {
  width: 414px;
  height: 896px;
  border-radius: 8px;
  overflow-x: hidden;
  overflow-y: auto;
  --brand-blob: rgba(126, 136, 255, .9);
}

.deco-top {
  position: absolute;
  left: 0;
  top: -10px;
  width: 414px;
  height: 136px;
  z-index: 0;
  filter: drop-shadow(0 6px 12px rgba(0, 0, 0, .12));
  pointer-events: none;
}

.deco-blob-left {
  position: absolute;
  left: -340px;
  bottom: 105px;
  width: 680px;
  height: 440px;
  z-index: 0;
  filter: drop-shadow(0 10px 22px rgba(0, 0, 0, .09));
  pointer-events: none;
}

.sg-title {
  margin-top: 118px;
  margin-bottom: 24px;
  color: #262626;
  position: relative;
  z-index: 1;
}

.form-offset {
  margin-top: 12px;
}

.sg-pill {
  border-radius: 40px;
  overflow: hidden;
}

.sg-pill-start {
  border-radius: 40px 0 0 40px !important;
}

.sg-pill-end {
  border-radius: 0 40px 40px 0 !important;
}

.sg-shadow {
  box-shadow: 0 16px 28px rgba(0, 0, 0, .08);
}

.input-group-text,
.form-control {
  background: #fff;
}

.id-chip {
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%) scale(.96);
  padding: .3rem .72rem;
  background: #f5f6fb;
  border: 1px solid rgba(0, 0, 0, .06);
  color: #6b7280;
  font-weight: 600;
  z-index: 10;
  pointer-events: auto;
}

.guardian-row {
  margin-top: 4px;
  margin-bottom: 8px;
}

form {
  position: relative;
  z-index: 1;
  padding-bottom: 40px;
}
</style>
<!-- src/views/Signup.vue -->
<template>
  <div class="sg-wrap position-relative mx-auto bg-white">
    <!-- 장식: 상단 웨이브 -->
    <svg class="deco-top" viewBox="0 0 414 150" preserveAspectRatio="none" aria-hidden="true">
      <path d="M0,0 H414 V90 C320,140 230,80 110,100 C60,108 20,128 0,150 Z" fill="rgba(74,98,221,0.85)" />
    </svg>

    <!-- 장식: 하단 블랍 -->
    <div class="deco-bottom" aria-hidden="true"></div>

    <!-- 타이틀 -->
    <div class="text-center pt-5 mt-3">
      <h1 class="fw-bold sg-title">회원 가입</h1>
    </div>

    <!-- 폼 -->
    <form class="px-4 mt-2" @submit.prevent="onSubmit">
      <!-- 성함 -->
      <div class="mb-4">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="8" r="4.5" stroke="#9A9A9A" stroke-width="1.6" />
              <path d="M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6" stroke="#9A9A9A" stroke-width="1.6" />
            </svg>
          </span>
          <input v-model.trim="form.name" type="text" class="form-control border-0 sg-pill-end" placeholder="성함"
            autocomplete="name" required />
        </div>
      </div>

      <!-- 아이디 + 중복체크 칩(오른쪽에 떠있게) -->
      <div class="mb-4 position-relative">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="8" r="4.5" stroke="#9A9A9A" stroke-width="1.6" />
              <path d="M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6" stroke="#9A9A9A" stroke-width="1.6" />
            </svg>
          </span>
          <input v-model.trim="form.username" type="text" class="form-control border-0 sg-pill-end" placeholder="아이디"
            autocomplete="username" required />
        </div>

        <button type="button" class="btn btn-light shadow-sm rounded-pill sg-id-chip" @click="checkDuplicate">
          중복체크
        </button>
      </div>

      <!-- 비밀번호 + 보기 토글 -->
      <div class="mb-4">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
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
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
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
      <div class="mb-4">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M6 2h4l2 5-3 2a12 12 0 0 0 6 6l2-3 5 2v4a3 3 0 0 1-3 3A17 17 0 0 1 3 5a3 3 0 0 1 3-3z"
                stroke="#9A9A9A" stroke-width="1.6" />
            </svg>
          </span>
          <input v-model.trim="form.phone" type="tel" inputmode="tel" class="form-control border-0 sg-pill-end"
            placeholder="전화번호" required />
        </div>
      </div>

      <!-- 주소 -->
      <div class="mb-4">
        <div class="input-group input-group-lg sg-pill sg-shadow">
          <span class="input-group-text bg-white border-0 sg-pill-start">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M12 22s8-7.1 8-12a8 8 0 1 0-16 0c0 4.9 8 12 8 12z" stroke="#9A9A9A" stroke-width="1.6" />
              <circle cx="12" cy="10" r="2.5" stroke="#9A9A9A" stroke-width="1.6" />
            </svg>
          </span>
          <input v-model.trim="form.address" type="text" class="form-control border-0 sg-pill-end" placeholder="주소"
            autocomplete="street-address" />
        </div>
      </div>

      <!-- 보호자 체크 -->
      <div class="form-check mb-5 ms-1">
        <input class="form-check-input" type="checkbox" id="isGuardian" v-model="form.isGuardian">
        <label class="form-check-label" for="isGuardian">보호자일 경우 체크</label>
      </div>

      <!-- CTA -->
      <div class="d-grid gap-2 mb-5">
        <button type="submit" class="btn btn-primary btn-lg rounded-pill sg-cta shadow">
          회원가입
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'

const form = reactive({
  name: '',
  username: '',
  password: '',
  birth: '',
  phone: '',
  address: '',
  isGuardian: false,
})

const showPw = ref(false)

function checkDuplicate() {
  if (!form.username) return alert('아이디를 입력하세요.')
  // TODO: 실제 API 연동
  alert(`'${form.username}' 중복여부를 확인했습니다(데모).`)
}

function onSubmit() {
  if (!form.name || !form.username || !form.password || !form.birth || !form.phone) {
    alert('필수 항목을 입력해주세요.')
    return
  }
  console.log('submit', { ...form })
  alert('회원가입 요청을 전송했습니다. (데모)')
}
</script>

<style scoped>
/* 고정 캔버스(요구 해상도) */
.sg-wrap {
  width: 414px;
  height: 896px;
  border-radius: 8px;
  overflow: auto;
}

/* 타이틀 톤/간격 */
.sg-title {
  color: #262626;
}

/* 상단 웨이브 */
.deco-top {
  position: absolute;
  left: 0;
  top: 0;
  width: 414px;
  height: 150px;
  filter: drop-shadow(0 6px 12px rgba(0, 0, 0, .12));
  z-index: 0;
}

/* 하단 블랍 */
.deco-bottom {
  position: absolute;
  left: -120px;
  bottom: 70px;
  width: 360px;
  height: 260px;
  background: rgba(126, 136, 255, .90);
  border-top-right-radius: 140px;
  border-bottom-left-radius: 120px;
  transform: rotate(-10deg);
  filter: drop-shadow(0 8px 18px rgba(0, 0, 0, .10));
  z-index: 0;
}

/* 둥근 인풋 형태 + 그림자(스크린샷 톤) */
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
  box-shadow: 0 10px 24px rgba(0, 0, 0, .10);
}

/* 중복체크 칩: 오른쪽에 떠 있도록 */
.sg-id-chip {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  background: #fff;
  border: 0;
  padding: .5rem 1rem;
  font-weight: 500;
}

/* CTA 버튼 색/그림자 */
.sg-cta {
  background: #4A62DD;
  border-color: #4A62DD;
}

/* 폼 엘리먼트 배경 흰색 유지 */
.input-group-text,
.form-control {
  background: #fff;
}

/* 스크롤 콘텐츠가 웨이브/블랍 위로 오도록 */
form,
.sg-title {
  position: relative;
  z-index: 1;
}
</style>

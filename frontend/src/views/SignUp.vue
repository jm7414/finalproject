<!-- src/views/Signup.vue -->
<template>
  <div class="sg-wrap position-relative mx-auto bg-white" style="
      /* 데코 컨트롤 */
      --topWaveH: 184px;   /* 상단 웨이브 높이 */
      --topWaveX: 0px;     /* 상단 웨이브 좌우 오프셋 */
      --leftBlobW: 150px;  /* 왼쪽 블랍 너비 */
      --rightBlobW: 96px;  /* 오른쪽 블랍 너비 */
      --decoEdgeGap: 0px;  /* 데코 좌/우 여백(0이면 화면 끝에 붙음) */

      /* 레이아웃 미세조정 */
      --sgFormLift: -32px;  /* 폼을 위로 당김(음수=위) */
      --bottomSafe: 116px;  /* 폼 아래 안전 여백(블랍이 '밖'에 보이도록) */
    ">
    <!-- 배경 데코 -->
    <img class="deco-top-img" :src="topWave" alt="" aria-hidden="true" />
    <img class="deco-blob-left-img" :src="blobLeft" alt="" aria-hidden="true" />
    <img class="deco-blob-right-img" :src="blobRight" alt="" aria-hidden="true" />

    <!-- 폼 카드 -->
    <section class="form-surface" :style="{ marginTop: `calc(var(--topWaveH) + 18px + var(--sgFormLift))` }">
      <header class="form-head">
        <h1>회원 가입</h1>
        <p>필수 정보를 입력해 주세요.</p>
      </header>

      <form @submit.prevent="onSubmit">
        <!-- 성함 -->
        <label class="field">
          <span class="field-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
              <circle cx="12" cy="8" r="4.5" />
              <path d="M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6" />
            </svg>
          </span>
          <input v-model.trim="form.name" type="text" placeholder="성함" autocomplete="name" required />
        </label>

        <!-- 아이디 + 중복체크 -->
        <div class="field with-action">
          <span class="field-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
              <circle cx="12" cy="8" r="4.5" />
              <path d="M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6" />
            </svg>
          </span>
          <input v-model.trim="form.username" type="text" placeholder="아이디" autocomplete="username" required />
          <button type="button" class="chip" @click="checkDuplicate">중복체크</button>
        </div>

        <!-- 비밀번호 -->
        <label class="field">
          <span class="field-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
              <rect x="4" y="10" width="16" height="10" rx="2" />
              <path d="M8 10V7a4 4 0 0 1 8 0v3" />
            </svg>
          </span>
          <input :type="showPw ? 'text' : 'password'" v-model="form.password" placeholder="비밀번호"
            autocomplete="new-password" required />
          <button type="button" class="icon-btn" @click="showPw = !showPw" aria-label="비밀번호 보기">
            <svg v-if="showPw" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.8">
              <path d="M2 12s4-6 10-6 10 6 10 6-4 6-10 6-10-6-10-6z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M3 3l18 18" />
              <path d="M2 12s4-6 10-6a9.6 9.6 0 0 1 6 2" />
              <path d="M20.7 15.5C18.7 17.6 15.7 18 12 18 6 18 2 12 2 12a19 19 0 0 1 4-4" />
            </svg>
          </button>
        </label>

        <!-- 생년월일 -->
        <label class="field">
          <span class="field-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
              <rect x="3" y="5" width="18" height="16" rx="2" />
              <path d="M3 9h18" />
              <path d="M8 3v4M16 3v4" />
            </svg>
          </span>
          <input v-model="form.birth" type="date" placeholder="연도-월-일" required />
        </label>

        <!-- 전화번호 -->
        <label class="field">
          <span class="field-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
              <path d="M6 2h4l2 5-3 2a12 12 0 0 0 6 6l2-3 5 2v4a3 3 0 0 1-3 3A17 17 0 0 1 3 5a3 3 0 0 1 3-3z" />
            </svg>
          </span>
          <input v-model.trim="form.phone" type="tel" inputmode="tel" placeholder="전화번호" required />
        </label>

        <!-- 보호자 체크 -->
        <label class="form-check">
          <input type="checkbox" v-model="form.isGuardian" />
          <span>보호자일 경우 체크</span>
        </label>

        <!-- CTA 버튼 -->
        <button class="cta" type="submit" aria-label="회원가입">회원가입</button>
      </form>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'

import topWave from '@/assets/images/qwe3.svg'
import blobLeft from '@/assets/images/qwe2.svg'
import blobRight from '@/assets/images/qwe1.svg'

const form = reactive({
  name: '',
  username: '',
  password: '',
  birth: '',
  phone: '',
  isGuardian: false
})
const showPw = ref(false)

function checkDuplicate() {
  if (!form.username) return alert('아이디를 입력하세요.')
  alert(`'${form.username}' 중복 확인(데모)`)
}
function onSubmit() {
  if (!form.name || !form.username || !form.password || !form.birth || !form.phone) {
    alert('필수 항목을 입력해주세요.')
    return
  }
  console.log('submit', { ...form })
  alert('회원가입 요청 전송(데모)')
}
</script>

<style scoped>
/* ===== Theme ===== */
.sg-wrap {
  --brand: #5A6BEE;
  --brand-2: #7E88FF;
  --ink: #1f2937;
  --sub-ink: #6b7280;

  --field-bg: rgba(255, 255, 255, 0.9);
  --field-stroke: rgba(93, 106, 238, 0.25);
  --field-stroke-focus: rgba(93, 106, 238, 0.55);
  --field-ring: rgba(93, 106, 238, 0.18);
  --placeholder: #9aa0b5;

  width: 414px;
  height: 896px;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

/* ===== 배경 데코 ===== */
.deco-top-img {
  position: absolute;
  left: 0;
  top: 0;
  width: 414px;
  height: var(--topWaveH, 180px);
  object-fit: cover;
  object-position: var(--topWaveX, 0px) top;
  filter: drop-shadow(0 6px 12px rgba(0, 0, 0, .12));
  z-index: 0;
  pointer-events: none;
}

.deco-blob-left-img {
  position: absolute;
  left: var(--decoEdgeGap, 0);
  /* 화면 왼쪽 끝에 맞춤 */
  bottom: 12px;
  width: var(--leftBlobW, 150px);
  /* 카드 여백 계산 제거 */
  height: auto;
  object-fit: contain;
  object-position: left bottom;
  border-radius: 12px;
  filter: drop-shadow(0 10px 22px rgba(0, 0, 0, .08));
  z-index: 0;
  pointer-events: none;
}

.deco-blob-right-img {
  position: absolute;
  right: var(--decoEdgeGap, 0);
  /* 화면 오른쪽 끝에 맞춤 */
  bottom: 16px;
  width: var(--rightBlobW, 96px);
  height: auto;
  object-fit: contain;
  filter: drop-shadow(0 8px 18px rgba(0, 0, 0, .08));
  z-index: 0;
  pointer-events: none;
}

/* ===== 폼 카드 ===== */
.form-surface {
  position: relative;
  z-index: 1;
  margin: 0 16px;
  padding: 20px 16px 24px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, .72) 0%, rgba(248, 250, 255, .88) 100%);
  border: 1px solid rgba(93, 106, 238, .18);
  box-shadow: 0 16px 36px rgba(22, 36, 86, .10), 0 2px 6px rgba(0, 0, 0, .04);
  backdrop-filter: blur(8px) saturate(120%);
  margin-bottom: var(--bottomSafe, 116px);
  /* ▼ 폼 아래 공간 확보 */
}

.form-head {
  text-align: center;
  margin-bottom: 14px;
}

.form-head h1 {
  margin: 0;
  font-size: 28px;
  color: var(--ink);
  font-weight: 800;
  letter-spacing: -0.02em;
}

.form-head p {
  margin: 6px 0 0;
  color: var(--sub-ink);
  font-size: 14px;
}

/* ===== 필드 공통 ===== */
.field,
.with-action {
  position: relative;
  display: grid;
  grid-template-columns: 40px 1fr;
  align-items: center;
  background: var(--field-bg);
  border: 1px solid var(--field-stroke);
  border-radius: 14px;
  padding: 10px 12px;
  margin-bottom: 12px;
  transition: box-shadow .18s ease, border-color .18s ease, transform .06s ease;
}

.field:focus-within,
.with-action:focus-within {
  border-color: var(--field-stroke-focus);
  box-shadow:
    0 0 0 6px var(--field-ring),
    0 12px 28px rgba(22, 36, 86, .08);
  transform: translateY(-1px);
}

.field-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #8a90a6;
}

/* 입력 요소 */
input {
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 16px;
  color: var(--ink);
}

input::placeholder {
  color: var(--placeholder);
}

/* 액션칩/아이콘 버튼 */
.with-action {
  grid-template-columns: 40px 1fr auto;
  column-gap: 8px;
}

.chip {
  border: 0;
  padding: .4rem .8rem;
  border-radius: 999px;
  font-weight: 700;
  font-size: .9rem;
  color: #fff;
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-2) 100%);
  box-shadow: 0 10px 18px rgba(90, 107, 238, .25);
}

.chip:hover {
  filter: brightness(1.05);
}

.icon-btn {
  border: 0;
  background: transparent;
  color: #6c7280;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 체크박스 */
.form-check {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 2px 10px;
  color: var(--sub-ink);
  font-size: 14px;
}

.form-check input {
  width: 18px;
  height: 18px;
}

/* CTA 버튼 */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 52px;
  margin-top: 6px;
  border: 0;
  border-radius: 14px;
  font-weight: 800;
  letter-spacing: .02em;
  color: #fff;
  font-size: 18px;
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-2) 100%);
  box-shadow: 0 18px 36px rgba(90, 107, 238, .28), 0 4px 10px rgba(0, 0, 0, .06);
  transition: transform .12s ease, box-shadow .12s ease, filter .12s ease;
}

.cta:hover {
  transform: translateY(-1px);
  filter: saturate(1.05);
}

.cta:active {
  transform: translateY(0);
}
</style>

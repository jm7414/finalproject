<!-- src/views/Login.vue -->
<template>
  <div class="lg-wrap position-relative mx-auto bg-white" style="
      --topWaveH: 180px;    /* 상단 블랍 높이 */
      --topWaveX: 0px;      /* 상단 블랍 좌우 오프셋 */

      /* 왼쪽/오른쪽 블랍 크기 */
      --leftBlobW: 230px;   /* 왼쪽 블랍: 통째로 보이게 */
      --rightBlobW: 48px;   /* 오른쪽 블랍: 포인트만 */
      --decoEdgeGap: 0px;   /* 데코 좌/우 여백(0이면 화면 끝에 맞춤) */

      /* 로그인 버튼/아이콘 크기 */
      --go-h: 48px;
      --go-minw: 64px;
      --go-icon: 40px;
      --go-radius: 16px;
    ">
    <!-- 상단 블랍 -->
    <img class="deco-top-img" :src="topWave" alt="" aria-hidden="true" />

    <!-- 콘텐츠(폼/텍스트) -->
    <div class="lg-content">
      <!-- 타이틀 -->
      <div class="text-center">
        <h1 class="fw-bold lg-title">환영합니다</h1>
        <p class="text-secondary lg-sub">로그인 해주세요</p>
      </div>

      <!-- 폼 -->
      <form class="px-4 mt-3" @submit.prevent="onLogin">
        <!-- 아이디 -->
        <div class="mb-4">
          <div class="input-group input-group-lg lg-pill lg-shadow">
            <span class="input-group-text bg-white border-0 lg-pill-start">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <circle cx="12" cy="8" r="4.5" stroke="#9A9A9A" stroke-width="1.6" />
                <path d="M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6" stroke="#9A9A9A" stroke-width="1.6" />
              </svg>
            </span>
            <input v-model.trim="form.username" type="text" class="form-control border-0 lg-pill-end" placeholder="아이디"
              autocomplete="username" required />
          </div>
        </div>

        <!-- 비밀번호 -->
        <div class="mb-2">
          <div class="input-group input-group-lg lg-pill lg-shadow">
            <span class="input-group-text bg-white border-0 lg-pill-start">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <rect x="4" y="10" width="16" height="10" rx="2" stroke="#9A9A9A" stroke-width="1.6" />
                <path d="M8 10V7a4 4 0 0 1 8 0v3" stroke="#9A9A9A" stroke-width="1.6" />
              </svg>
            </span>
            <input :type="showPw ? 'text' : 'password'" v-model="form.password" class="form-control border-0"
              placeholder="비밀번호" autocomplete="current-password" required />
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

        <p class="text-center text-secondary small mb-5">
          <a href="#" class="text-decoration-none text-secondary" @click.prevent="goToForgotPassword">비밀번호가 기억나지 않으신가요?</a>
        </p>

        <!-- CTA: 가운데 정렬 -->
        <div class="cta-row mb-4">
          <div class="cta-title">로그인</div>
          <button type="submit" class="lg-go shadow" aria-label="로그인">
            <svg class="lg-go-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M8 12h8M13 7l5 5-5 5" stroke="#fff" stroke-width="2" stroke-linecap="round"
                stroke-linejoin="round" />
            </svg>
          </button>
        </div>

        <p class="text-center text-secondary">
          회원이 아니신가요? 
          <router-link to="/SignUp" class="text-decoration-none text-primary">회원가입</router-link>
        </p>
      </form>
    </div>

    <!-- 하단 블랍들 -->
    <img class="deco-blob-left-img" :src="leftBlob" alt="" aria-hidden="true" />
    <img class="deco-blob-right-img" :src="rightBlob" alt="" aria-hidden="true" />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import topWave from '@/assets/images/qwe.svg'
import leftBlob from '@/assets/images/asd.svg'
import rightBlob from '@/assets/images/zxc.svg'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const showPw = ref(false)

async function onLogin() {
  if (!form.username || !form.password) {
    alert('아이디와 비밀번호를 입력하세요.')
    return
  }

  try {
    const response = await fetch('http://localhost:8080/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        username: form.username,
        password: form.password,
      }),
      credentials: 'include',
    })

    if (response.ok) {
      // 로그인 성공 시 사용자 정보 조회
      const userResponse = await fetch('http://localhost:8080/api/user/me', {
        credentials: 'include',
      })
      
      if (userResponse.ok) {
        const userData = await userResponse.json()
        // 역할에 따라 다른 페이지로 이동
        if (userData.roleNo === 1 || userData.roleNo === 3) {
          // 보호자(1) 또는 구독자(3) -> MapMain 페이지
          router.push('/map-main')
        } else if (userData.roleNo === 2) {
          // 환자(2) -> DP 페이지
          router.push('/DP')
        } else {
          // 기본값 (예외 처리)
          router.push('/login')
        }
      }
    } else {
      alert('로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.')
    }
  } catch (error) {
    console.error('로그인 중 오류 발생:', error)
    alert('로그인 처리 중 오류가 발생했습니다.')
  }
}

function goToForgotPassword() {
  alert('준비 중인 기능입니다.') // 비밀번호 찾기 기능은 아직 구현하지 않음
}
</script>

<style scoped>
/* 기존 스타일 유지 */
.lg-wrap {
  width: 414px;
  height: 896px;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

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

.lg-content {
  position: relative;
  z-index: 1;
}

.lg-title {
  margin-top: calc(var(--topWaveH, 180px) + 36px);
  margin-bottom: 8px;
  font-size: 48px;
  line-height: 1.1;
  color: #262626;
}

.lg-sub {
  color: #6b7280;
}

.lg-pill {
  border-radius: 40px;
  overflow: hidden;
}

.lg-pill-start {
  border-radius: 40px 0 0 40px !important;
}

.lg-pill-end {
  border-radius: 0 40px 40px 0 !important;
}

.lg-shadow {
  box-shadow: 0 16px 28px rgba(0, 0, 0, .08);
}

.input-group-text,
.form-control {
  background: #fff;
}

/* CTA 행 */
.cta-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 0 8px;
}

.cta-title {
  font-weight: 700;
  font-size: 32px;
  color: #262626;
  line-height: 1;
}

/* 로그인 버튼 */
.lg-go {
  height: var(--go-h);
  min-width: var(--go-minw);
  padding: 0 12px;
  border-radius: var(--go-radius);
  border: 0;
  background: linear-gradient(135deg, #5A6BEE 0%, #7E88FF 100%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 12px 24px rgba(90, 107, 238, .22), 0 2px 6px rgba(0, 0, 0, .06);
  transition: transform .15s ease, box-shadow .15s ease;
}

.lg-go:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(90, 107, 238, .26), 0 3px 8px rgba(0, 0, 0, .08);
}

.lg-go:active {
  transform: translateY(0);
}

/* 아이콘 크기 */
.lg-go-icon {
  width: var(--go-icon);
  height: var(--go-icon);
  display: block;
}

/* ===== 하단 블랍들: 좌/우 화면 끝에 맞춤 ===== */
.deco-blob-left-img {
  position: absolute;
  left: var(--decoEdgeGap, 0);
  /* ← 화면 왼쪽 끝 */
  bottom: 24px;
  width: var(--leftBlobW, 230px);
  /* 카드 여백 계산 제거 */
  height: auto;
  object-fit: contain;
  object-position: left bottom;
  border-radius: 12px;
  filter: drop-shadow(0 10px 22px rgba(0, 0, 0, .09));
  z-index: 0;
  pointer-events: none;
}

.deco-blob-right-img {
  position: absolute;
  right: var(--decoEdgeGap, 0);
  /* → 화면 오른쪽 끝 */
  bottom: 24px;
  width: var(--rightBlobW, 48px);
  height: auto;
  filter: drop-shadow(0 8px 18px rgba(0, 0, 0, .08));
  z-index: 0;
  pointer-events: none;
}
</style>
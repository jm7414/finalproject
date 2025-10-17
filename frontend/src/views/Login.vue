<!-- src/views/Login.vue -->
<template>
    <div class="container-sm py-3" style="max-width:414px">
        <!-- 헤더 -->
        <header class="border-bottom pb-2 mb-3 position-relative">
            <button type="button" class="btn btn-link text-body p-0 position-absolute start-0 top-0">
                <i class="bi bi-arrow-left fs-5"></i>
            </button>
            <h5 class="text-center m-0">로그인</h5>
        </header>

        <!-- 브랜드 -->
        <section class="text-center mb-4">
            <div class="d-inline-flex align-items-center justify-content-center rounded-4 mb-2"
                style="width:72px;height:72px;background:#E7EAFD">
                <img :src="mamaLogo" alt="맘마미아 로고" style="width:44px;height:44px;object-fit:contain" />
            </div>
            <h4 class="fw-semibold mb-1">맘마미아</h4>
            <div class="text-secondary">기술로 연결하고, 마음으로 돌보다</div>
        </section>

        <!-- 폼 (정적) -->
        <form novalidate>
            <!-- 아이디 -->
            <div class="mb-3">
                <label class="form-label">아이디</label>
                <div class="input-group">
                    <input type="text" class="form-control rounded-start-3 border-end-0" placeholder="아이디를 입력하세요"
                        style="height:48px">
                    <span class="input-group-text bg-white rounded-end-3 border-start-0" style="height:48px">
                        <i class="bi bi-person"></i>
                    </span>
                </div>
            </div>

            <!-- 비밀번호 -->
            <div class="mb-4">
                <label class="form-label">비밀번호</label>
                <div class="input-group">
                    <input type="password" class="form-control rounded-start-3 border-end-0" placeholder="비밀번호를 입력하세요"
                        style="height:48px">
                    <span class="input-group-text bg-white rounded-end-3 border-start-0" style="height:48px">
                        <i class="bi bi-eye-slash"></i>
                    </span>
                </div>
            </div>

            <!-- 로그인 버튼 -->
            <button type="button" class="btn w-100 rounded-3 py-3 mb-3"
                style="background:#6F74E9;border-color:#6F74E9;color:#fff">
                로그인
            </button>

            <!-- 찾기 링크 -->
            <div class="d-flex justify-content-center align-items-center text-secondary mb-3">
                <a href="#" class="text-secondary text-decoration-none">아이디 찾기</a>
                <span class="mx-3" style="color:#d0d0d0">|</span>
                <a href="#" class="text-secondary text-decoration-none">비밀번호 찾기</a>
            </div>

            <!-- 또는 -->
            <div class="d-flex align-items-center text-secondary mb-3">
                <div class="flex-grow-1 border-top"></div>
                <span class="px-3">또는</span>
                <div class="flex-grow-1 border-top"></div>
            </div>

            <!-- 회원가입 -->
            <p class="text-center mb-3">아직 회원이 아니신가요?</p>
            <button type="button" class="btn btn-outline-secondary w-100 rounded-3 py-3">회원가입</button>
        </form>

        <!-- 푸터 -->
        <div class="text-center mt-4">
            <div class="small text-secondary d-flex justify-content-center align-items-center">
                <a href="#" class="text-secondary text-decoration-none">이용약관</a>
                <span class="mx-3" style="color:#d0d0d0">|</span>
                <a href="#" class="text-secondary text-decoration-none">개인정보처리방침</a>
                <span class="mx-3" style="color:#d0d0d0">|</span>
                <a href="#" class="text-secondary text-decoration-none">고객센터</a>
            </div>
            <div class="small text-muted mt-2">© 2025 Mamamia. All rights reserved.</div>
        </div>
    </div>
</template>

<script setup>
import mamaLogo from '@/assets/images/mama.png'
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
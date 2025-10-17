<!-- src/views/Login.vue -->
<template>
    <div class="login-container">
        <!-- 헤더 -->
        <header class="border-bottom position-relative" style="padding: 12px 0 8px;">
            <h5 class="text-center m-0">로그인</h5>
        </header>

        <!-- 메인 컨텐츠 영역 -->
        <div class="login-content">
            <!-- 브랜드 -->
            <section class="text-center" style="margin-bottom: 16px;">
                <div class="d-inline-flex align-items-center justify-content-center rounded-4"
                    style="width:64px;height:64px;background:#E7EAFD;margin-bottom:8px;">
                    <img :src="mamaLogo" alt="맘마미아 로고" style="width:40px;height:40px;object-fit:contain" />
                </div>
                <h4 class="fw-semibold" style="font-size: 1.3rem;margin-bottom:4px;">맘마미아</h4>
                <div class="text-secondary" style="font-size: 0.85rem;">기술로 연결하고, 마음으로 돌보다</div>
            </section>

            <!-- 폼 -->
            <form @submit.prevent="onLogin">
                <!-- 아이디 -->
                <div style="margin-bottom: 12px;">
                    <label class="form-label" style="font-size: 0.85rem; margin-bottom: 4px;">아이디</label>
                    <div class="input-group">
                        <input v-model.trim="form.username" type="text" class="form-control rounded-start-3 border-end-0" 
                            placeholder="아이디를 입력하세요" autocomplete="username" required style="height:44px">
                        <span class="input-group-text bg-white rounded-end-3 border-start-0" style="height:44px">
                            <i class="bi bi-person"></i>
                        </span>
                    </div>
                </div>

                <!-- 비밀번호 -->
                <div style="margin-bottom: 12px;">
                    <label class="form-label" style="font-size: 0.85rem; margin-bottom: 4px;">비밀번호</label>
                    <div class="input-group">
                        <input :type="showPw ? 'text' : 'password'" v-model="form.password" 
                            class="form-control rounded-start-3 border-end-0" placeholder="비밀번호를 입력하세요"
                            autocomplete="current-password" required style="height:44px">
                        <button type="button" class="input-group-text bg-white rounded-end-3 border-start-0" 
                            style="height:44px" @click="showPw = !showPw" aria-label="비밀번호 보기">
                            <i :class="showPw ? 'bi bi-eye' : 'bi bi-eye-slash'"></i>
                        </button>
                    </div>
                </div>

                <!-- 로그인 버튼 -->
                <button type="submit" class="btn w-100 rounded-3"
                    style="background:#6F74E9;border-color:#6F74E9;color:#fff;padding:10px;margin-bottom:10px;">
                    로그인
                </button>

                <!-- 찾기 링크 -->
                <div class="d-flex justify-content-center align-items-center text-secondary" style="margin-bottom:10px;">
                    <a href="#" class="text-secondary text-decoration-none" style="font-size: 0.8rem;">아이디 찾기</a>
                    <span class="mx-2" style="color:#d0d0d0">|</span>
                    <a href="#" class="text-secondary text-decoration-none" style="font-size: 0.8rem;">비밀번호 찾기</a>
                </div>

                <!-- 또는 -->
                <div class="d-flex align-items-center" style="margin-bottom:10px;">
                    <div class="flex-grow-1 border-top"></div>
                    <span class="px-2" style="font-size: 0.8rem; color: #000;">또는</span>
                    <div class="flex-grow-1 border-top"></div>
                </div>

                <!-- 회원가입 -->
                <p class="text-center" style="font-size: 0.85rem;margin-bottom:8px;">아직 회원이 아니신가요?</p>
                <router-link to="/SignUp" class="btn btn-outline-secondary w-100 rounded-3 text-decoration-none"
                    style="padding:10px;">
                    회원가입
                </router-link>
            </form>
        </div>

        <!-- 푸터 -->
        <div class="login-footer">
            <div class="small d-flex justify-content-center align-items-center">
                <a href="#" class="text-decoration-none" style="font-size: 0.75rem; color: #000;">이용약관</a>
                <span class="mx-2" style="color:#d0d0d0">|</span>
                <a href="#" class="text-decoration-none" style="font-size: 0.75rem; color: #000;">개인정보처리방침</a>
                <span class="mx-2" style="color:#d0d0d0">|</span>
                <a href="#" class="text-decoration-none" style="font-size: 0.75rem; color: #000;">고객센터</a>
            </div>
            <div class="small text-muted" style="font-size: 0.7rem; margin-top: 0.25rem;">© 2025 Mamamia. All rights reserved.</div>
        </div>
    </div>
</template>

<script setup>
import mamaLogo from '@/assets/images/mama.png'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

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

</script>

<style scoped>
.login-container {
  width: 100%;
  height: 812px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.login-content {
  flex: 1;
  padding: 0 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-footer {
  padding: 8px 20px 12px;
  text-align: center;
}
</style>
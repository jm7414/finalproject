<template>
  <div class="desktop-auth">
    <div class="auth-card">
      <section class="hero">
        <h1>맘마미아 보호자 센터</h1>
        <p>PC 전용 화면에서 더 넓은 시야로 가족을 돌보세요.</p>
      </section>

      <form class="auth-form" @submit.prevent="onLogin">
        <label>
          <span>아이디</span>
          <input
            v-model.trim="form.username"
            type="text"
            placeholder="아이디를 입력하세요"
            autocomplete="username"
            required
          />
        </label>

        <label>
          <span>비밀번호</span>
          <div class="password-field">
            <input
              :type="showPw ? 'text' : 'password'"
              v-model="form.password"
              placeholder="비밀번호를 입력하세요"
              autocomplete="current-password"
              required
            />
            <button type="button" class="toggle" @click="showPw = !showPw">
              {{ showPw ? '숨김' : '보기' }}
            </button>
          </div>
        </label>

        <button type="submit" class="submit-btn">로그인</button>
      </form>

      <div class="auth-links">
        <RouterLink to="/login">모바일 버전으로 돌아가기</RouterLink>
        <RouterLink to="/SignUp">회원가입</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const showPw = ref(false)

async function onLogin() {
  if (!form.username || !form.password) {
    alert('아이디와 비밀번호를 입력하세요.')
    return
  }

  try {
    const response = await axios.post(
      `/api/login`,
      new URLSearchParams({
        username: form.username,
        password: form.password
      }),
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        withCredentials: true
      }
    )

    if (response.status === 200) {
      const userResponse = await axios.get(`/api/user/me`, {
        withCredentials: true
      })

      if (userResponse.status === 200) {
        const userData = userResponse.data
        if (userData.roleNo === 1) {
          router.push('/desktop/main')
        } else {
          router.push('/')
        }
      }
    }
  } catch (error) {
    console.error('로그인 중 오류:', error)
    if (error.response && error.response.status === 401) {
      alert('로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.')
    } else {
      alert('로그인 처리 중 오류가 발생했습니다.')
    }
  }
}
</script>

<style scoped>
.desktop-auth {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.auth-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.94);
  padding: 36px;
  border-radius: 20px;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  color: #1f2937;
}

.hero p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-form label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  color: #4b5563;
  font-weight: 600;
}

.auth-form input {
  height: 44px;
  border-radius: 12px;
  border: 1px solid #d1d5db;
  padding: 0 14px;
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.auth-form input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.password-field {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #d1d5db;
  overflow: hidden;
}

.password-field input {
  flex: 1;
  border: 0;
  padding-right: 0;
}

.password-field .toggle {
  width: 72px;
  height: 44px;
  border: 0;
  border-left: 1px solid #e5e7eb;
  background: #f3f4f6;
  color: #4b5563;
  font-weight: 600;
  cursor: pointer;
}

.submit-btn {
  height: 46px;
  border-radius: 14px;
  border: 0;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #ffffff;
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(79, 70, 229, 0.25);
}

.auth-links {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #4f46e5;
  font-weight: 600;
}

.auth-links a {
  text-decoration: none;
  color: inherit;
}
</style>


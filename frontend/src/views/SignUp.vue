<!-- src/views/Signup.vue -->
<template>
    <div class="signup-container">
        <!-- 상단 헤더 (동작 없음) -->
        <header class="border-bottom pb-2 mb-3 d-flex align-items-center">
            <button type="button" class="btn btn-link text-body p-0 me-2" @click="goBack">
                <i class="bi bi-arrow-left fs-5"></i>
            </button>
            <h5 class="fw-semibold mb-0">회원가입</h5>
        </header>

        <!-- 로고/브랜드 (mama.png 표시) -->
        <section class="text-center mb-3">
            <div class="rounded-circle bg-primary-subtle d-inline-flex align-items-center justify-content-center mb-2"
                style="width:80px;height:80px">
                <img :src="mamaLogo" alt="맘마미아 로고" style="width:56px;height:56px;object-fit:contain" />
            </div>
            <h5 class="mb-1">맘마미아</h5>
            <div class="text-secondary small">안전하고 편리한 실버 관리 서비스</div>
        </section>

        <!-- 폼 -->
        <form @submit.prevent="onSubmit">
            <!-- 이름 -->
            <div class="mb-3">
                <label class="form-label">이름</label>
                <input v-model.trim="form.name" type="text" class="form-control" placeholder="이름을 입력해주세요" required>
            </div>

            <!-- 아이디 + 중복확인 -->
            <div class="mb-3">
                <label class="form-label">아이디</label>
                <div class="input-group">
                    <input v-model.trim="form.username" @input="onUsernameChange" type="text" class="form-control" placeholder="아이디를 입력해주세요" required>
                    <button type="button" class="btn btn-outline-primary" @click="checkDuplicate">중복확인</button>
                </div>
            </div>

            <!-- 비밀번호 -->
            <div class="mb-1">
                <label class="form-label">비밀번호</label>
                <input v-model="form.password" type="password" class="form-control" placeholder="비밀번호를 입력해주세요" required>
            </div>
            <div class="form-text mb-3">8자 이상, 영문+숫자 조합으로 입력해주세요</div>

            <!-- 생년월일 -->
            <div class="mb-3">
                <label class="form-label">생년월일</label>
                <div class="input-group">
                    <span class="input-group-text bg-white"><i class="bi bi-calendar3"></i></span>
                    <input v-model="form.birth" type="date" class="form-control" required>
                </div>
            </div>

            <!-- 전화번호 -->
            <div class="mb-3">
                <label class="form-label">전화번호</label>
                <input v-model.trim="form.phone" type="tel" class="form-control" placeholder="010-0000-0000" required>
            </div>

            <!-- 사용자 구분 -->
            <div class="mb-3">
                <label class="form-label">사용자 구분</label>

                <label class="form-check border rounded-3 ps-5 px-3 py-2 d-flex align-items-center gap-2 mb-2">
                    <input v-model="form.isGuardian" :value="true" class="form-check-input me-2" type="radio" name="role" @change="form.isGuardian = true">
                    <i class="bi bi-people"></i>
                    <span>보호자/가족입니다</span>
                </label>

                <label class="form-check border rounded-3 ps-5 px-3 py-2 d-flex align-items-center gap-2">
                    <input v-model="form.isGuardian" :value="false" class="form-check-input me-2" type="radio" name="role" @change="form.isGuardian = false">
                    <i class="bi bi-person"></i>
                    <span>환자입니다</span>
                </label>
            </div>

            <!-- 약관 -->
            <div class="mb-3">
                <div class="form-check mb-2">
                    <input v-model="agreeAll" class="form-check-input" type="checkbox" id="agreeAll" @change="onAgreeAllChange">
                    <label class="form-check-label" for="agreeAll">전체 동의</label>
                </div>

                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div class="form-check">
                        <input v-model="agreeTos" class="form-check-input" type="checkbox" id="tos" @change="onIndividualAgreeChange">
                        <label class="form-check-label" for="tos">서비스 이용약관 (필수)</label>
                    </div>
                    <button type="button" class="btn btn-link p-0 text-decoration-underline">보기</button>
                </div>

                <div class="d-flex justify-content-between align-items-center">
                    <div class="form-check">
                        <input v-model="agreePrivacy" class="form-check-input" type="checkbox" id="privacy" @change="onIndividualAgreeChange">
                        <label class="form-check-label" for="privacy">개인정보 처리방침 (필수)</label>
                    </div>
                    <button type="button" class="btn btn-link p-0 text-decoration-underline">보기</button>
                </div>
            </div>

            <!-- 제출 -->
            <button type="submit" class="btn btn-primary w-100 rounded-3 py-3"
                style="background:#4A62DD;border-color:#4A62DD">
                회원가입
            </button>
        </form>

        <!-- 하단 링크 -->
        <p class="text-center mt-3 mb-0">
            <span class="text-secondary">이미 계정이 있으신가요?</span>
            <router-link to="/login" class="text-body text-decoration-underline ms-1">로그인</router-link>
        </p>
    </div>
</template>

<script setup>
import mamaLogo from '@/assets/images/mama.png'
import { useRouter } from 'vue-router'
import { reactive, ref } from 'vue'

const router = useRouter()
const form = reactive({
  name: '',
  username: '',
  password: '',
  birth: '',
  phone: '',
  isGuardian: null
})

const isIdChecked = ref(false)
const isIdAvailable = ref(false)

// 약관 동의 관련 변수들
const agreeAll = ref(false)
const agreeTos = ref(false)
const agreePrivacy = ref(false)

// 아이디 입력 시 중복 확인 상태 초기화
function onUsernameChange() {
  isIdChecked.value = false
  isIdAvailable.value = false
}

// 전체 동의 체크박스 변경 시
function onAgreeAllChange() {
  if (agreeAll.value) {
    agreeTos.value = true
    agreePrivacy.value = true
  } else {
    agreeTos.value = false
    agreePrivacy.value = false
  }
}

// 개별 약관 체크박스 변경 시
function onIndividualAgreeChange() {
  agreeAll.value = agreeTos.value && agreePrivacy.value
}

// 뒤로가기 버튼 클릭 시 로그인 페이지로 이동
function goBack() {
  router.push('/login')
}

async function checkDuplicate() {
  if (!form.username) {
    alert('아이디를 입력하세요.')
    return
  }

  try {
    const response = await fetch(`/api/user/check-duplicate?userId=${form.username}`, {
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

  // 사용자 구분 필수
  if (form.isGuardian === null) {
    alert('사용자 구분을 선택해주세요.')
    return
  }

  // 중복 확인 필수
  if (!isIdChecked.value || !isIdAvailable.value) {
    alert('아이디 중복 확인을 해주세요.')
    return
  }

  // 약관 동의 필수
  if (!agreeTos.value || !agreePrivacy.value) {
    alert('필수 약관에 동의해주세요.')
    return
  }

  try {
    const requestData = {
      userId: form.username,
      userPw: form.password,
      name: form.name,
      birthDate: form.birth,
      phoneNumber: form.phone,
      roleNo: form.isGuardian ? 1 : 2, // 보호자: 1, 환자: 2
    }
    
    console.log('회원가입 요청 데이터:', requestData)
    console.log('form.isGuardian 값:', form.isGuardian, '타입:', typeof form.isGuardian)

    const response = await fetch(`/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestData),
      credentials: 'include'
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
.signup-container {
  width: 100%;
  max-width: 414px;
  height: 812px;
  margin: 0 auto;
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
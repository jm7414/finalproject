<!-- src/views/Signup.vue -->
<template>
    <div class="container-sm py-3" style="max-width:414px">
        <!-- 상단 헤더 (동작 없음) -->
        <header class="border-bottom pb-2 mb-3 d-flex align-items-center">
            <button type="button" class="btn btn-link text-body p-0 me-2">
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

        <!-- 폼 (정적, 이벤트/검증 없음) -->
        <form novalidate>
            <!-- 이름 -->
            <div class="mb-3">
                <label class="form-label">이름</label>
                <input type="text" class="form-control" placeholder="이름을 입력해주세요">
            </div>

            <!-- 아이디 + 중복확인 -->
            <div class="mb-3">
                <label class="form-label">아이디</label>
                <div class="input-group">
                    <input type="text" class="form-control" placeholder="아이디를 입력해주세요">
                    <button type="button" class="btn btn-outline-primary">중복확인</button>
                </div>
            </div>

            <!-- 비밀번호 -->
            <div class="mb-1">
                <label class="form-label">비밀번호</label>
                <input type="password" class="form-control" placeholder="비밀번호를 입력해주세요">
            </div>
            <div class="form-text mb-3">8자 이상, 영문+숫자 조합으로 입력해주세요</div>

            <!-- 생년월일 -->
            <div class="mb-3">
                <label class="form-label">생년월일</label>
                <div class="input-group">
                    <span class="input-group-text bg-white"><i class="bi bi-calendar3"></i></span>
                    <input type="date" class="form-control">
                </div>
            </div>

            <!-- 전화번호 -->
            <div class="mb-3">
                <label class="form-label">전화번호</label>
                <input type="tel" class="form-control" placeholder="010-0000-0000">
            </div>

            <!-- 사용자 구분 -->
            <div class="mb-3">
                <label class="form-label">사용자 구분</label>

                <label class="form-check border rounded-3 ps-5 px-3 py-2 d-flex align-items-center gap-2 mb-2">
                    <input class="form-check-input me-2" type="radio" name="role" value="patient">
                    <i class="bi bi-person"></i>
                    <span>환자입니다</span>
                </label>

                <label class="form-check border rounded-3 ps-5 px-3 py-2 d-flex align-items-center gap-2">
                    <input class="form-check-input me-2" type="radio" name="role" value="guardian">
                    <i class="bi bi-people"></i>
                    <span>보호자/가족입니다</span>
                </label>
            </div>

            <!-- 약관 -->
            <div class="mb-3">
                <div class="form-check mb-2">
                    <input class="form-check-input" type="checkbox" id="agreeAll">
                    <label class="form-check-label" for="agreeAll">전체 동의</label>
                </div>

                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="tos">
                        <label class="form-check-label" for="tos">서비스 이용약관 (필수)</label>
                    </div>
                    <button type="button" class="btn btn-link p-0 text-decoration-underline">보기</button>
                </div>

                <div class="d-flex justify-content-between align-items-center">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="privacy">
                        <label class="form-check-label" for="privacy">개인정보 처리방침 (필수)</label>
                    </div>
                    <button type="button" class="btn btn-link p-0 text-decoration-underline">보기</button>
                </div>
            </div>

            <!-- 제출 (동작 없음) -->
            <button type="button" class="btn btn-primary w-100 rounded-3 py-3"
                style="background:#4A62DD;border-color:#4A62DD">
                회원가입
            </button>
        </form>

        <!-- 하단 링크 (정적) -->
        <p class="text-center mt-3 mb-0">
            <span class="text-secondary">이미 계정이 있으신가요?</span>
            <a href="#" class="text-body text-decoration-underline ms-1">로그인</a>
        </p>
    </div>
</template>

<script setup>
import mamaLogo from '@/assets/images/mama.png'
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
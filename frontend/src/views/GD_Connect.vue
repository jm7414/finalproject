<template>
  <main class="gdc-wrap">
    <div class="card">
      <h2 class="title">보호자-환자 연결 (초대코드)</h2>

      <!-- 보호자(로그인 사용자) -->
      <div class="field">
        <label class="label">보호자 번호 (자동)</label>
        <input class="input" :value="guardianNo ?? ''" disabled />
      </div>

      <!-- 초대코드 입력 -->
      <div class="field">
        <label class="label" for="invite">환자 초대코드</label>
        <input id="invite" class="input" type="text" v-model.trim="inviteCode" placeholder="예: 8자리 코드" />
      </div>

      <!-- (선택) 확인된 환자번호 미리보기 -->
      <div class="field" v-if="resolvedPatientNo">
        <label class="label">확인된 환자번호</label>
        <input class="input" :value="resolvedPatientNo" disabled />
      </div>

      <button class="btn" :disabled="!canSubmit || busy" @click="connectByInvite">
        {{ busy ? '처리 중...' : '연결 생성' }}
      </button>

      <p v-if="msg" class="msg ok">{{ msg }}</p>
      <p v-if="err" class="msg err">{{ err }}</p>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const guardianNo = ref(null)        // 로그인 사용자 번호 자동 세팅
const inviteCode = ref('')          // 초대코드
const resolvedPatientNo = ref(null) // 초대코드로 확인된 환자번호

const msg = ref('')
const err = ref('')
const busy = ref(false)

const canSubmit = computed(() => !!guardianNo.value && !!inviteCode.value)

onMounted(async () => {
  try {
    const res = await fetch('/api/user/me', { credentials: 'include' })
    if (res.status === 401) {
      router.push('/login')
      return
    }
    const me = await res.json()
    // 백엔드 UserVO의 필드명에 맞춰주세요. (userNo가 맞다면 그대로)
    guardianNo.value = me.userNo
  } catch (e) {
    err.value = '내 정보 조회 실패: ' + String(e)
  }
})

/** 초대코드 → 환자번호 해석 */
async function resolveInvite(code) {
  const url = `/api/connect/resolve-invite?code=${encodeURIComponent(code)}`
  const res = await fetch(url, { credentials: 'include' })
  if (!res.ok) {
    const t = await res.text().catch(() => '')
    throw new Error(t || `초대코드 확인 실패 (${res.status})`)
  }
  const data = await res.json()
  // 백 응답 예시: { patientNo, userId }
  return data.patientNo
}

/** 연결 생성(초대코드 해석 → POST 연결) */
async function connectByInvite() {
  msg.value = ''
  err.value = ''
  resolvedPatientNo.value = null
  busy.value = true
  try {
    const patientNo = await resolveInvite(inviteCode.value)
    resolvedPatientNo.value = patientNo

    const res = await fetch('/api/connect/connections', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        patientNo: Number(patientNo),
        guardian1No: Number(guardianNo.value),
        guardian2No: null,
        guardian3No: null,
      }),
    })

    if (!res.ok) {
      const t = await res.text().catch(() => '')
      throw new Error(t || `연결 실패 (${res.status})`)
    }
    const data = await res.json().catch(() => ({}))
    msg.value = data.message || '연결 생성 완료'
  } catch (e) {
    err.value = String(e)
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.gdc-wrap {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f6f7fb;
}

.card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, .08);
  padding: 24px;
}

.title {
  margin: 0 0 16px;
  text-align: center;
  font-size: 20px;
  font-weight: 700;
}

.field {
  margin-bottom: 12px;
}

.label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #444;
}

.input {
  width: 100%;
  height: 40px;
  border: 1px solid #e4e6ef;
  border-radius: 10px;
  padding: 0 12px;
  outline: none;
}

.input:focus {
  border-color: #4f46e5;
}

.btn {
  width: 100%;
  height: 44px;
  margin-top: 8px;
  border: none;
  border-radius: 10px;
  background: #4f46e5;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.btn:disabled {
  opacity: .5;
  cursor: not-allowed;
}

.msg {
  margin-top: 12px;
  font-size: 14px;
  white-space: pre-wrap;
}

.ok {
  color: #16a34a;
}

.err {
  color: #dc2626;
}
</style>

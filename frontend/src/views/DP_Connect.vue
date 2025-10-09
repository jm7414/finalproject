<template>
  <!-- 헤더 안 가리게 간단 여백 -->
  <div><br /><br /><br /><br /><br /><br /></div>

  <!-- 중앙 정렬(스타일 없이 align) -->
  <div align="center">
    <main>
      <header>
        <h1>내 초대코드</h1>
      </header>

      <div><br /></div>

      <!-- 1) 코드 불러오기 -->
      <section>
        <button @click="loadInvite">코드 불러오기</button>
        <div v-if="loading">불러오는 중...</div>
        <div v-if="error" style="white-space:pre-wrap">{{ error }}</div>

        <div v-if="code">
          <p>아래 코드를 보호자에게 전달하세요.</p>
          <p><strong>{{ code }}</strong></p>

          <div>
            <button @click="copyCode">복사</button>
            <button @click="shareCode">공유</button>
          </div>

          <div v-if="hint">{{ hint }}</div>
        </div>

        <div v-else-if="loaded && !error">
          아직 초대코드가 없습니다. (환자 계정에만 코드가 있습니다)
        </div>
      </section>

      <div><br /><br /></div>

      <!-- 2) 안내 -->
      <section>
        <h2>도움말</h2>
        <ul>
          <li>“코드 불러오기”는 현재 로그인한 내 초대코드를 가져옵니다.</li>
          <li>회원 유형이 환자가 아니면 코드가 없을 수 있습니다.</li>
          <li>재발급 기능은 나중에 API가 생기면 버튼을 추가하세요.</li>
        </ul>
      </section>

      <div><br /><br /><br /></div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const code = ref('')
const loading = ref(false)
const error = ref('')
const hint = ref('')
const loaded = ref(false)

/** 1) 코드 불러오기: GET /api/user/me -> { invitationCode: "A1B2C3D4", ... } */
async function loadInvite() {
  loading.value = true
  error.value = ''
  hint.value = ''
  loaded.value = false
  code.value = ''

  try {
    const res = await fetch('/api/user/me', { method: 'GET' })
    if (!res.ok) {
      const msg = await safeErr(res)
      throw new Error(msg)
    }
    const data = await res.json()
    code.value = data?.invitationCode || ''
    loaded.value = true
  } catch (e) {
    error.value = String(e?.message || e)
  } finally {
    loading.value = false
  }
}

/** 2) 코드 복사 */
async function copyCode() {
  error.value = ''
  hint.value = ''
  if (!code.value) {
    hint.value = '불러온 코드가 없습니다. 먼저 “코드 불러오기”를 눌러 주세요.'
    return
  }
  try {
    await navigator.clipboard.writeText(code.value)
    hint.value = '복사 완료!'
  } catch {
    error.value = '클립보드 복사에 실패했습니다.'
  }
}

/** 3) 코드 공유 */
async function shareCode() {
  error.value = ''
  hint.value = ''
  if (!code.value) {
    hint.value = '불러온 코드가 없습니다. 먼저 “코드 불러오기”를 눌러 주세요.'
    return
  }
  try {
    if (navigator.share) {
      await navigator.share({ title: '초대코드', text: `내 초대코드: ${code.value}` })
    } else {
      hint.value = '이 브라우저는 공유를 지원하지 않습니다. 복사를 사용해 주세요.'
    }
  } catch {
    hint.value = '공유가 취소되었거나 지원하지 않습니다.'
  }
}

/** 공통: 에러 메시지 추출(간단 버전) */
async function safeErr(res) {
  try {
    const j = await res.json()
    return j?.message || `오류 (${res.status})`
  } catch {
    try { return await res.text() } catch { return `오류 (${res.status})` }
  }
}
</script>
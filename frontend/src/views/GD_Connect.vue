<template>
  <main>
    <h1>보호자 연결(초간단)</h1>

    <!-- 연결 생성 -->
    <section>
      <h2>1) 환자 연결 생성</h2>
      <div>
        <label for="patientNo">환자번호 (필수)</label>
        <input id="patientNo" type="number" v-model.number="patientNo" placeholder="예: 101" />
      </div>
      <div>
        <label for="guardian1No">보호자번호 (guardian1, 필수)</label>
        <input id="guardian1No" type="number" v-model.number="guardian1No" placeholder="예: 201" />
      </div>
      <button @click="createConnection">연결 생성</button>
      <p v-if="createMsg">{{ createMsg }}</p>
    </section>

    <hr />

    <!-- guardian1 교체 -->
    <section>
      <h2>2) guardian1 교체</h2>
      <div>
        <label for="patientNo2">환자번호</label>
        <input id="patientNo2" type="number" v-model.number="patientNoForPatch" placeholder="예: 101" />
      </div>
      <div>
        <label for="newGuardianNo">새 보호자번호</label>
        <input id="newGuardianNo" type="number" v-model.number="newGuardianNo" placeholder="예: 202" />
      </div>
      <button @click="patchGuardian1">guardian1 교체</button>
      <p v-if="patchMsg">{{ patchMsg }}</p>
    </section>

    <hr />

    <!-- 연결 해제 -->
    <section>
      <h2>3) 연결 해제</h2>
      <div>
        <label for="patientNo3">환자번호</label>
        <input id="patientNo3" type="number" v-model.number="patientNoForDelete" placeholder="예: 101" />
      </div>
      <button @click="deleteConnection">연결 해제</button>
      <p v-if="deleteMsg">{{ deleteMsg }}</p>
    </section>

    <p v-if="lastError" style="white-space:pre-wrap">{{ lastError }}</p>
  </main>
</template>

<script setup>
import { ref } from 'vue'

// 입력값
const patientNo = ref()
const guardian1No = ref()

const patientNoForPatch = ref()
const newGuardianNo = ref()

const patientNoForDelete = ref()

// 메시지
const createMsg = ref('')
const patchMsg = ref('')
const deleteMsg = ref('')
const lastError = ref('')

// 1) 연결 생성 : POST /api/connect/connections
async function createConnection() {
  resetMsgs()
  if (!patientNo.value || !guardian1No.value) {
    createMsg.value = 'patientNo, guardian1No는 필수입니다.'
    return
  }
  try {
    const res = await fetch('/api/connect/connections', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        patientNo: Number(patientNo.value),
        guardian1No: Number(guardian1No.value),
        guardian2No: null,
        guardian3No: null,
      }),
    })
    if (res.ok) {
      const data = await res.json().catch(() => ({}))
      createMsg.value = data?.message || '연결 생성 완료'
    } else {
      createMsg.value = await safeErr(res)
    }
  } catch (e) {
    lastError.value = String(e)
  }
}

// 2) guardian1 교체 : PATCH /api/connect/connections/{patientNo}/guardian1?guardianNo=...
async function patchGuardian1() {
  resetMsgs()
  if (!patientNoForPatch.value || !newGuardianNo.value) {
    patchMsg.value = '환자번호와 새 보호자번호를 입력하세요.'
    return
  }
  try {
    const url = `/api/connect/connections/${Number(patientNoForPatch.value)}/guardian1?guardianNo=${encodeURIComponent(
      Number(newGuardianNo.value)
    )}`
    const res = await fetch(url, { method: 'PATCH' })
    if (res.ok) {
      const data = await res.json().catch(() => ({}))
      patchMsg.value = data?.message || 'guardian1 교체 완료'
    } else {
      patchMsg.value = await safeErr(res)
    }
  } catch (e) {
    lastError.value = String(e)
  }
}

// 3) 연결 해제 : DELETE /api/connect/connections/{patientNo}
async function deleteConnection() {
  resetMsgs()
  if (!patientNoForDelete.value) {
    deleteMsg.value = '환자번호를 입력하세요.'
    return
  }
  try {
    const res = await fetch(`/api/connect/connections/${Number(patientNoForDelete.value)}`, {
      method: 'DELETE',
    })
    if (res.ok) {
      const data = await res.json().catch(() => ({}))
      deleteMsg.value = data?.message || '연결 해제 완료'
    } else {
      deleteMsg.value = await safeErr(res)
    }
  } catch (e) {
    lastError.value = String(e)
  }
}

// 공통: 에러 안전 추출
async function safeErr(res) {
  try {
    const j = await res.json()
    return j?.message || `오류 (${res.status})`
  } catch {
    try {
      return await res.text()
    } catch {
      return `오류 (${res.status})`
    }
  }
}

function resetMsgs() {
  createMsg.value = ''
  patchMsg.value = ''
  deleteMsg.value = ''
  lastError.value = ''
}
</script>
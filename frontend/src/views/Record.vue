<!-- src/views/DailyRecord.vue -->
<template>
  <div class="container-sm" style="max-width:414px;">

    <!-- ✅ 이미 오늘 기록이 있을 때 뜨는 안내 모달 -->
    <div v-if="showAlreadyModal" class="record-modal-backdrop">
      <div class="record-modal">
        <!-- 상단 파란 그라데이션 헤더 -->
        <div class="record-modal-header">
          <div class="record-modal-icon">
            <span>📝</span>
          </div>
          <div class="record-modal-title">오늘의 기록이 이미 있습니다</div>
          <div class="record-modal-sub">
            수정이 필요하면 <b>수정하기</b> 버튼을 눌러주세요.
          </div>
        </div>

        <!-- 본문 + 버튼 -->
        <div class="record-modal-body">
          <div class="d-flex gap-2">
            <button
              type="button"
              class="btn flex-fill record-btn-secondary"
              @click="cancelEdit"
            >
              뒤로가기
            </button>
            <button
              type="button"
              class="btn flex-fill record-btn-primary"
              @click="confirmEdit"
            >
              수정하기
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ✅ 오늘 기록 완료 모달 -->
    <div v-if="showCompleteModal" class="record-modal-backdrop">
      <div class="record-modal">
        <div class="record-modal-header">
          <div class="record-modal-icon">
            <span>✅</span>
          </div>
          <div class="record-modal-title">오늘의 기록이 완료되었습니다</div>
          <div class="record-modal-sub">
            남긴 내용을 바탕으로<br />
            AI 보고서가 업데이트됩니다.
          </div>
        </div>

        <div class="record-modal-body">
          <div class="d-flex gap-2">
            <button
              type="button"
              class="btn flex-fill record-btn-secondary"
              @click="closeCompleteModal"
            >
              수정하기
            </button>
            <button
              type="button"
              class="btn flex-fill record-btn-primary"
              @click="goHomeAfterComplete"
            >
              홈으로 가기
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 진행률 -->
    <div class="px-1">
      <div class="d-flex justify-content-between align-items-center mb-1">
        <span class="small text-dark">진행률</span>
        <span class="small text-dark">{{ stepCompleted }}/5 완료</span>
      </div>
      <div class="progress" style="height:6px;">
        <div
          class="progress-bar"
          role="progressbar"
          :style="{ width: progressPct + '%', backgroundColor: team }"
        ></div>
      </div>
    </div>

    <!-- 탭(스텝 라벨) -->
    <div class="d-flex gap-3 px-2 mt-3 border-bottom">
      <button
        v-for="(tab, i) in tabs"
        :key="tab.key"
        class="btn btn-link px-0 pb-2 text-body fw-normal"
        :class="{ 'fw-semibold': step === i }"
        @click="goStep(i)"
        style="text-decoration:none;"
      >
        <div class="d-flex flex-column align-items-center">
          <span>{{ tab.label }}</span>
          <span
            v-if="step === i"
            class="w-100"
            :style="{ height: '2px', backgroundColor: team, marginTop: '8px' }"
          ></span>
        </div>
      </button>
    </div>

    <!-- 질문 카드 -->
    <div class="card border-0 shadow-sm mt-3">
      <div class="card-body">
        <template v-for="q in currentQuestions" :key="q.id">
          <div class="mb-2 fw-semibold" style="line-height:1.35">
            {{ q.text }}
          </div>

          <!-- 예/아니오 -->
          <div v-if="q.type === 'yesno'" class="mb-2 d-flex flex-wrap gap-2">
            <button
              class="btn btn-sm rounded-pill"
              :class="q.value === true ? 'btn-primary' : 'btn-outline-secondary'"
              :style="q.value === true ? primaryStyle : null"
              @click="setAnswer(q, true)"
            >
              예
            </button>
            <button
              class="btn btn-sm rounded-pill"
              :class="q.value === false ? 'btn-primary' : 'btn-outline-secondary'"
              :style="q.value === false ? primaryStyle : null"
              @click="setAnswer(q, false)"
            >
              아니오
            </button>
          </div>

          <!-- 예/아니오 + 상세입력 -->
          <div
            v-if="q.type === 'yesno' && q.value === true && q.detail !== undefined"
            class="mb-3"
          >
            <input
              v-if="q.detailKind === 'number'"
              class="form-control"
              type="number"
              step="0.1"
              :placeholder="q.detailPlaceholder || '입력'"
              v-model.trim="q.detail"
            />
            <input
              v-else-if="q.detailKind === 'textline'"
              class="form-control"
              type="text"
              :placeholder="q.detailPlaceholder || '입력'"
              v-model.trim="q.detail"
            />
            <textarea
              v-else
              class="form-control"
              rows="3"
              :placeholder="q.detailPlaceholder || '상세 내용을 입력하세요'"
              v-model.trim="q.detail"
            ></textarea>
          </div>

          <!-- 단일선택 -->
          <div v-else-if="q.type === 'single'" class="mb-3 d-flex flex-wrap gap-2">
            <button
              v-for="opt in q.options"
              :key="opt"
              class="btn btn-sm rounded-pill"
              :class="q.value === opt ? 'btn-primary' : 'btn-outline-secondary'"
              :style="q.value === opt ? primaryStyle : null"
              @click="setAnswer(q, opt)"
            >
              {{ opt }}
            </button>
          </div>

          <!-- 복수선택 -->
          <div v-else-if="q.type === 'multi'" class="mb-3 d-flex flex-wrap gap-2">
            <button
              v-for="opt in q.options"
              :key="opt"
              class="btn btn-sm rounded-pill"
              :class="q.value?.includes(opt) ? 'btn-primary' : 'btn-outline-secondary'"
              :style="q.value?.includes(opt) ? primaryStyle : null"
              @click="toggleMulti(q, opt)"
            >
              {{ opt }}
            </button>
          </div>

          <!-- 1~5 척도 -->
          <div v-else-if="q.type === 'scale5'" class="mb-3 d-flex gap-2">
            <button
              v-for="n in [1, 2, 3, 4, 5]"
              :key="n"
              class="btn btn-sm rounded-pill"
              :class="q.value === n ? 'btn-primary' : 'btn-outline-secondary'"
              :style="q.value === n ? primaryStyle : null"
              @click="setAnswer(q, n)"
            >
              {{ n }}
            </button>
          </div>

          <!-- 텍스트 -->
          <div v-else-if="q.type === 'text'" class="mb-3">
            <textarea
              class="form-control"
              rows="3"
              :placeholder="q.placeholder || '내용을 입력하세요'"
              v-model.trim="q.value"
            ></textarea>
          </div>
        </template>
      </div>
    </div>

    <!-- 하단 네비게이션 -->
    <div class="d-flex gap-2 mt-3 mb-4">
      <button
        class="btn btn-outline-secondary flex-grow-1"
        :disabled="step === 0"
        @click="prevStep"
      >
        이전
      </button>
      <button
        class="btn flex-grow-1 text-white"
        :style="primaryStyle"
        :disabled="!isCurrentStepComplete"
        @click="nextOrSubmit"
      >
        {{ step === tabs.length - 1 ? '완료' : '다음' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

/* 팀 컬러 */
const team = '#657AE2'
const primaryStyle = { background: team, borderColor: team }

/* 탭 */
const tabs = [
  { key: 'meal', label: '식사' },
  { key: 'med', label: '약 복용' },
  { key: 'act', label: '행동' },
  { key: 'feel', label: '감정' },
  { key: 'note', label: '특이사항' }
]

/* 질문 세트 */
const form = reactive({
  meal: [
    {
      id: 'skipMeal',
      type: 'single',
      text: '식사를 거르신 적이 있나요?',
      options: ['아침', '점심', '저녁', '없음'],
      value: null
    },
    { id: 'snack', type: 'yesno', text: '오늘 간식을 드셨나요?', value: null },
    {
      id: 'water',
      type: 'single',
      text: '오늘 물을 충분히 마셨나요?',
      options: ['충분', '보통', '적음'],
      value: null
    }
  ],
  med: [
    {
      id: 'medMiss',
      type: 'multi',
      text: '약 복용을 잊으신 적이 있나요?',
      options: ['아침', '점심', '저녁'],
      value: []
    },
    {
      id: 'extraMed',
      type: 'yesno',
      text: '추가로 복용한 약(수면제·진통제 등)이 있나요?',
      value: null,
      detail: '',
      detailPlaceholder: '약 종류를 입력하세요',
      detailKind: 'textline'
    }
  ],
  act: [
    {
      id: 'missAppt',
      type: 'single',
      text: '잊은 약속 횟수는 몇 번인가요?',
      options: ['0', '1–2회', '3회 이상'],
      value: null
    },
    {
      id: 'lostItems',
      type: 'single',
      text: '물건을 잃어버린 횟수는 몇 번인가요?',
      options: ['0', '1–2회', '3회 이상'],
      value: null
    },
    {
      id: 'lostWay',
      type: 'yesno',
      text: '외출 중 길을 잃은 적이 있나요?',
      value: null
    },
    {
      id: 'knowDate',
      type: 'yesno',
      text: '날짜를 올바르게 알고 있나요?',
      value: null
    },
    {
      id: 'toilet',
      type: 'single',
      text: '화장실을 혼자 사용하셨나요?',
      options: ['혼자', '도움 필요'],
      value: null
    },
    {
      id: 'dress',
      type: 'single',
      text: '옷 입기를 혼자 하셨나요?',
      options: ['혼자', '도움 필요'],
      value: null
    },
    { id: 'fall', type: 'yesno', text: '넘어지거나 비틀거린 적이 있나요?', value: null },
    {
      id: 'appliance',
      type: 'single',
      text: '스토브·전기제품 등을 사용할 때 위험한 상황은 없었나요?',
      options: ['문제 없음', '주의 필요'],
      value: null
    }
  ],
  feel: [
    {
      id: 'moodScore',
      type: 'scale5',
      text: '전반적인 기분 상태는 어떠셨나요? (1 매우 나쁨 ~ 5 매우 좋음)',
      value: null
    },
    {
      id: 'depress',
      type: 'single',
      text: '우울감을 느끼셨나요?',
      options: ['없음', '경미', '심함'],
      value: null
    },
    {
      id: 'anxiety',
      type: 'single',
      text: '불안감을 느끼셨나요?',
      options: ['없음', '경미', '심함'],
      value: null
    },
    {
      id: 'irritate',
      type: 'single',
      text: '초조하거나 짜증을 느끼셨나요?',
      options: ['없음', '경미', '심함'],
      value: null
    },
    {
      id: 'apathy',
      type: 'single',
      text: '무관심한 기분이 들었나요?',
      options: ['없음', '경미', '심함'],
      value: null
    },
    {
      id: 'conversation',
      type: 'single',
      text: '대화에 얼마나 적극적으로 참여하셨나요?',
      options: ['적극', '보통', '소극'],
      value: null
    }
  ],
  note: [
    {
      id: 'nightWander',
      type: 'yesno',
      text: '밤중에 배회하거나 돌아다닌 적이 있나요?',
      value: null
    },
    {
      id: 'repeatAct',
      type: 'yesno',
      text: '같은 행동을 반복한 적이 있나요?',
      value: null
    },
    {
      id: 'pain',
      type: 'yesno',
      text: '통증을 호소한 부분이 있나요?',
      value: null,
      detail: '',
      detailPlaceholder: '통증 부위를 입력하세요',
      detailKind: 'textline'
    },
    {
      id: 'temp',
      type: 'yesno',
      text: '체온이 평소와 달랐나요?',
      value: null,
      detail: '',
      detailPlaceholder: '체온 수치를 입력하세요 (예: 37.8)',
      detailKind: 'number'
    },
    {
      id: 'emergency',
      type: 'yesno',
      text: '응급 상황이 발생한 적이 있나요?',
      value: null,
      detail: '',
      detailPlaceholder: '무슨 상황이었는지 입력하세요',
      detailKind: 'text'
    },
    {
      id: 'support',
      type: 'yesno',
      text: '특별히 지원이 필요했던 상황이 있나요?',
      value: null,
      detail: '',
      detailPlaceholder: '내용을 입력하세요',
      detailKind: 'text'
    }
  ]
})

/* 스텝/진행률 */
const step = ref(0)
const currentKey = computed(() => tabs[step.value].key)
const currentQuestions = computed(() => form[currentKey.value])

const stepCompleted = computed(() => {
  let done = 0
  for (const t of tabs) {
    const arr = form[t.key]
    const hasAny = arr.some(q => {
      if (q.type === 'text') return !!q.value
      if (q.type === 'multi') return Array.isArray(q.value) && q.value.length > 0
      return q.value !== null && q.value !== undefined
    })
    if (hasAny) done++
  }
  return done
})
const progressPct = computed(() => (stepCompleted.value / 5) * 100)

/* 현재 스텝이 모든 질문에 응답됐는지 */
const isCurrentStepComplete = computed(() => {
  return currentQuestions.value.every(q => {
    if (q.type === 'text') {
      // 텍스트는 선택적으로 두고 싶으면 true 유지, 필수로 만들고 싶으면 !!q.value.trim()
      return true
    }
    if (q.type === 'multi') {
      return Array.isArray(q.value) && q.value.length > 0
    }
    return q.value !== null && q.value !== undefined
  })
})

/* 이미 오늘 기록이 있는지 관련 상태 */
const showAlreadyModal = ref(false)
/* 오늘 기록 완료 모달 상태 */
const showCompleteModal = ref(false)

/* helpers */
function setAnswer(q, v) {
  q.value = v
}
function toggleMulti(q, opt) {
  if (!Array.isArray(q.value)) q.value = []
  const i = q.value.indexOf(opt)
  if (i >= 0) q.value.splice(i, 1)
  else q.value.push(opt)
}

/* 기존 content를 폼에 반영 */
function applyContentToForm(savedContent) {
  for (const t of tabs) {
    const section = savedContent?.[t.key]
    if (!section) continue
    for (const q of form[t.key]) {
      const val = section[q.id]
      if (val !== undefined) {
        if (q.type === 'multi') {
          q.value = Array.isArray(val) ? [...val] : []
        } else if (q.type === 'scale5') {
          q.value = typeof val === 'number' ? val : null
        } else if (q.type === 'yesno') {
          q.value = val === true || val === false ? val : null
        } else {
          q.value = val ?? null
        }
      }
      if (q.type === 'yesno' && q.detail !== undefined) {
        const detailKey = `${q.id}Detail`
        const dval = section[detailKey]
        if (q.value === true) {
          q.detail = dval ?? ''
        } else {
          q.detail = ''
        }
      }
    }
  }
}

/* 네비게이션 */
function goStep(i) {
  step.value = i
}
function prevStep() {
  if (step.value > 0) step.value--
}
async function nextOrSubmit() {
  if (!isCurrentStepComplete.value) {
    alert('현재 항목을 모두 선택해 주세요.')
    return
  }
  if (step.value < tabs.length - 1) {
    step.value++
    return
  }
  await submit()
}

/* 제출 */
async function submit() {
  const content = {}
  for (const t of tabs) {
    content[t.key] = {}
    for (const q of form[t.key]) {
      content[t.key][q.id] = q.value
      if (q.type === 'yesno' && q.detail !== undefined) {
        content[t.key][`${q.id}Detail`] = q.value === true ? q.detail ?? '' : ''
      }
    }
  }

  try {
    const me = await fetch('/api/user/my-patient', { credentials: 'include' })
      .then(r => (r.ok ? r.json() : null))
      .catch(() => null)
    const userId = me?.userNo ?? me?.id ?? me
    if (!userId) throw new Error('연결된 환자 없음')

    const body = {
      userId,
      recordDate: new Date().toISOString().slice(0, 10),
      content
    }
    const res = await fetch('/api/record/upsert', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(body)
    })
    if (!res.ok) throw new Error(`저장 실패(${res.status})`)

    // ✅ alert 대신 완료 모달 표시
    showCompleteModal.value = true
  } catch (e) {
    alert('[저장 오류] ' + (e?.message || e))
  }
}

/* 오늘 기록이 있으면 자동 프리필 + 모달 표시 */
onMounted(async () => {
  try {
    const me = await fetch('/api/user/my-patient', { credentials: 'include' })
      .then(r => (r.ok ? r.json() : null))
      .catch(() => null)
    const userId = me?.userNo ?? me?.id ?? me
    if (!userId) return
    const today = new Date().toISOString().slice(0, 10)

    const res = await fetch(`/api/record/user/${userId}?date=${today}`, {
      credentials: 'include'
    })
    if (!res.ok) return
    const data = await res.json()
    const saved =
      typeof data.content === 'string' ? JSON.parse(data.content) : data.content

    if (saved) {
      applyContentToForm(saved)
      // 기록이 있으면 모달 띄우기
      showAlreadyModal.value = true
    }
  } catch (e) {
    console.warn('기존 기록 사전 로드 실패:', e)
  }
})

/* 모달 버튼 동작 (이미 기록 있음) */
function cancelEdit() {
  showAlreadyModal.value = false
  router.back() // 필요하면 router.push('/') 로 바꿔도 됨
}
function confirmEdit() {
  showAlreadyModal.value = false
  // 프리필된 상태에서 그냥 계속 작성/수정
}

/* 모달 버튼 동작 (기록 완료) */
function closeCompleteModal() {
  showCompleteModal.value = false
}
function goHomeAfterComplete() {
  showCompleteModal.value = false
  router.push('/') // 기존 동작과 동일하게 홈으로 이동
}
</script>

<style scoped>
/* ✅ 오늘 기록 있음/완료 모달 공통 스타일 */
.record-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.38);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.record-modal {
  width: 320px;
  max-width: 90%;
  border-radius: 18px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.3);
}

/* 상단 파란 그라데이션 헤더 */
.record-modal-header {
  padding: 16px 18px 14px;
  text-align: center;
  color: #ffffff;
  background: linear-gradient(135deg, #7fa9ff 0%, #5c7df5 50%, #4556d9 100%);
}

.record-modal-icon {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  margin: 0 auto 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
  font-size: 20px;
}

.record-modal-title {
  font-weight: 700;
  font-size: 0.95rem;
}

.record-modal-sub {
  margin-top: 4px;
  font-size: 0.78rem;
  opacity: 0.9;
}

/* 본문 + 버튼 영역 */
.record-modal-body {
  padding: 14px 16px 16px;
  background: #f8f9ff;
}

/* 버튼들 */
.record-btn-secondary {
  border-radius: 999px;
  font-size: 0.85rem;
  background: #ffffff;
  border: 1px solid #d6d9f3;
  color: #4b4f68;
}

.record-btn-primary {
  border-radius: 999px;
  font-size: 0.85rem;
  border: none;
  color: #ffffff;
  background: linear-gradient(135deg, #5f80ff 0%, #4457d9 100%);
  box-shadow: 0 6px 14px rgba(68, 87, 217, 0.45);
}
</style>

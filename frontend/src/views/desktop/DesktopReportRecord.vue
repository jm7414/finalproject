<template>
  <div class="desktop-record-container">
    <!-- 진행률 -->
    <div class="progress-section">
      <div class="progress-header">
        <span class="progress-label">진행률</span>
        <span class="progress-count">{{ stepCompleted }}/5 완료</span>
      </div>
      <div class="progress-bar-wrapper">
        <div
          class="progress-bar-fill"
          :style="{ width: progressPct + '%', backgroundColor: team }"
        ></div>
      </div>
    </div>

    <!-- 탭(스텝 라벨) -->
    <div class="step-tabs">
      <button
        v-for="(tab, i) in tabs"
        :key="tab.key"
        class="step-tab"
        :class="{ active: step === i }"
        @click="goStep(i)"
      >
        <span>{{ tab.label }}</span>
        <span
          v-if="step === i"
          class="step-indicator"
          :style="{ backgroundColor: team }"
        ></span>
      </button>
    </div>

    <!-- 질문 카드 -->
    <div class="question-card">
      <template v-for="q in currentQuestions" :key="q.id">
        <div class="question-text">{{ q.text }}</div>

        <!-- 예/아니오 -->
        <div v-if="q.type === 'yesno'" class="answer-buttons">
          <button
            class="answer-btn"
            :class="q.value === true ? 'active' : ''"
            :style="q.value === true ? primaryStyle : null"
            @click="setAnswer(q, true)"
          >
            예
          </button>
          <button
            class="answer-btn"
            :class="q.value === false ? 'active' : ''"
            :style="q.value === false ? primaryStyle : null"
            @click="setAnswer(q, false)"
          >
            아니오
          </button>
        </div>

        <!-- 예/아니오 + 상세입력 -->
        <div
          v-if="q.type === 'yesno' && q.value === true && q.detail !== undefined"
          class="detail-input"
        >
          <input
            v-if="q.detailKind === 'number'"
            class="form-input"
            type="number"
            step="0.1"
            :placeholder="q.detailPlaceholder || '입력'"
            v-model.trim="q.detail"
          />
          <input
            v-else-if="q.detailKind === 'textline'"
            class="form-input"
            type="text"
            :placeholder="q.detailPlaceholder || '입력'"
            v-model.trim="q.detail"
          />
          <textarea
            v-else
            class="form-textarea"
            rows="3"
            :placeholder="q.detailPlaceholder || '상세 내용을 입력하세요'"
            v-model.trim="q.detail"
          ></textarea>
        </div>

        <!-- 단일선택 -->
        <div v-else-if="q.type === 'single'" class="answer-buttons">
          <button
            v-for="opt in q.options"
            :key="opt"
            class="answer-btn"
            :class="q.value === opt ? 'active' : ''"
            :style="q.value === opt ? primaryStyle : null"
            @click="setAnswer(q, opt)"
          >
            {{ opt }}
          </button>
        </div>

        <!-- 복수선택 -->
        <div v-else-if="q.type === 'multi'" class="answer-buttons">
          <button
            v-for="opt in q.options"
            :key="opt"
            class="answer-btn"
            :class="q.value?.includes(opt) ? 'active' : ''"
            :style="q.value?.includes(opt) ? primaryStyle : null"
            @click="toggleMulti(q, opt)"
          >
            {{ opt }}
          </button>
        </div>

        <!-- 1~5 척도 -->
        <div v-else-if="q.type === 'scale5'" class="answer-buttons">
          <button
            v-for="n in [1, 2, 3, 4, 5]"
            :key="n"
            class="answer-btn"
            :class="q.value === n ? 'active' : ''"
            :style="q.value === n ? primaryStyle : null"
            @click="setAnswer(q, n)"
          >
            {{ n }}
          </button>
        </div>

        <!-- 텍스트 -->
        <div v-else-if="q.type === 'text'" class="text-input">
          <textarea
            class="form-textarea"
            rows="3"
            :placeholder="q.placeholder || '내용을 입력하세요'"
            v-model.trim="q.value"
          ></textarea>
        </div>
      </template>
    </div>

    <!-- 하단 네비게이션 -->
    <div class="navigation-buttons">
      <button
        class="nav-btn prev-btn"
        :disabled="step === 0"
        @click="prevStep"
      >
        이전
      </button>
      <button class="nav-btn next-btn" :style="primaryStyle" @click="nextOrSubmit">
        {{ step === tabs.length - 1 ? '완료' : '다음' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'

const emit = defineEmits(['saved'])

/* 팀 컬러 */
const team = '#657AE2'
const primaryStyle = { background: team, borderColor: team }

/* 탭 */
const tabs = [
  { key: 'meal', label: '식사' },
  { key: 'med', label: '약 복용' },
  { key: 'act', label: '행동' },
  { key: 'feel', label: '감정' },
  { key: 'note', label: '특이사항' },
]

/* 질문 세트 */
const form = reactive({
  meal: [
    {
      id: 'skipMeal',
      type: 'single',
      text: '식사를 거르신 적이 있나요?',
      options: ['아침', '점심', '저녁', '없음'],
      value: null,
    },
    {
      id: 'snack',
      type: 'yesno',
      text: '오늘 간식을 드셨나요?',
      value: null,
    },
    {
      id: 'water',
      type: 'single',
      text: '오늘 물을 충분히 마셨나요?',
      options: ['충분', '보통', '적음'],
      value: null,
    },
  ],
  med: [
    {
      id: 'medMiss',
      type: 'multi',
      text: '약 복용을 잊으신 적이 있나요?',
      options: ['아침', '점심', '저녁'],
      value: [],
    },
    {
      id: 'extraMed',
      type: 'yesno',
      text: '추가로 복용한 약(수면제·진통제 등)이 있나요?',
      value: null,
      detail: '',
      detailPlaceholder: '약 종류를 입력하세요',
      detailKind: 'textline',
    },
  ],
  act: [
    {
      id: 'missAppt',
      type: 'single',
      text: '잊은 약속 횟수는 몇 번인가요?',
      options: ['0', '1–2회', '3회 이상'],
      value: null,
    },
    {
      id: 'lostItems',
      type: 'single',
      text: '물건을 잃어버린 횟수는 몇 번인가요?',
      options: ['0', '1–2회', '3회 이상'],
      value: null,
    },
    {
      id: 'lostWay',
      type: 'yesno',
      text: '외출 중 길을 잃은 적이 있나요?',
      value: null,
    },
    {
      id: 'knowDate',
      type: 'yesno',
      text: '날짜를 올바르게 알고 있나요?',
      value: null,
    },
    {
      id: 'toilet',
      type: 'single',
      text: '화장실을 혼자 사용하셨나요?',
      options: ['혼자', '도움 필요'],
      value: null,
    },
    {
      id: 'dress',
      type: 'single',
      text: '옷 입기를 혼자 하셨나요?',
      options: ['혼자', '도움 필요'],
      value: null,
    },
    {
      id: 'fall',
      type: 'yesno',
      text: '넘어지거나 비틀거린 적이 있나요?',
      value: null,
    },
    {
      id: 'appliance',
      type: 'single',
      text: '스토브·전기제품 등을 사용할 때 위험한 상황은 없었나요?',
      options: ['문제 없음', '주의 필요'],
      value: null,
    },
  ],
  feel: [
    {
      id: 'moodScore',
      type: 'scale5',
      text: '전반적인 기분 상태는 어떠셨나요? (1 매우 나쁨 ~ 5 매우 좋음)',
      value: null,
    },
    {
      id: 'depress',
      type: 'single',
      text: '우울감을 느끼셨나요?',
      options: ['없음', '경미', '심함'],
      value: null,
    },
    {
      id: 'anxiety',
      type: 'single',
      text: '불안감을 느끼셨나요?',
      options: ['없음', '경미', '심함'],
      value: null,
    },
    {
      id: 'irritate',
      type: 'single',
      text: '초조하거나 짜증을 느끼셨나요?',
      options: ['없음', '경미', '심함'],
      value: null,
    },
    {
      id: 'apathy',
      type: 'single',
      text: '무관심한 기분이 들었나요?',
      options: ['없음', '경미', '심함'],
      value: null,
    },
    {
      id: 'conversation',
      type: 'single',
      text: '대화에 얼마나 적극적으로 참여하셨나요?',
      options: ['적극', '보통', '소극'],
      value: null,
    },
  ],
  note: [
    {
      id: 'nightWander',
      type: 'yesno',
      text: '밤중에 배회하거나 돌아다닌 적이 있나요?',
      value: null,
    },
    {
      id: 'repeatAct',
      type: 'yesno',
      text: '같은 행동을 반복한 적이 있나요?',
      value: null,
    },
    {
      id: 'pain',
      type: 'yesno',
      text: '통증을 호소한 부분이 있나요?',
      value: null,
      detail: '',
      detailPlaceholder: '통증 부위를 입력하세요',
      detailKind: 'textline',
    },
    {
      id: 'temp',
      type: 'yesno',
      text: '체온이 평소와 달랐나요?',
      value: null,
      detail: '',
      detailPlaceholder: '체온 수치를 입력하세요 (예: 37.8)',
      detailKind: 'number',
    },
    {
      id: 'emergency',
      type: 'yesno',
      text: '응급 상황이 발생한 적이 있나요?',
      value: null,
      detail: '',
      detailPlaceholder: '무슨 상황이었는지 입력하세요',
      detailKind: 'text',
    },
    {
      id: 'support',
      type: 'yesno',
      text: '특별히 지원이 필요했던 상황이 있나요?',
      value: null,
      detail: '',
      detailPlaceholder: '내용을 입력하세요',
      detailKind: 'text',
    },
  ],
})

/* 스텝/진행률 */
const step = ref(0)
const currentKey = computed(() => tabs[step.value].key)
const currentQuestions = computed(() => form[currentKey.value])

const stepCompleted = computed(() => {
  let done = 0
  for (const t of tabs) {
    const arr = form[t.key]
    const hasAny = arr.some((q) => {
      if (q.type === 'text') return !!q.value
      if (q.type === 'multi') return Array.isArray(q.value) && q.value.length > 0
      return q.value !== null
    })
    if (hasAny) done++
  }
  return done
})
const progressPct = computed(() => (stepCompleted.value / 5) * 100)

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
      .then((r) => (r.ok ? r.json() : null))
      .catch(() => null)
    const userId = me?.userNo ?? me?.id ?? me
    if (!userId) throw new Error('연결된 환자 없음')

    const body = {
      userId,
      recordDate: new Date().toISOString().slice(0, 10),
      content,
    }
    const res = await fetch('/api/record/upsert', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error(`저장 실패(${res.status})`)
    alert('오늘의 기록이 저장되었습니다.')

    // 저장 성공 시 부모 컴포넌트에 알림
    emit('saved')
  } catch (e) {
    alert('[저장 오류] ' + (e?.message || e))
  }
}

/* 오늘 기록이 있으면 자동 프리필 */
onMounted(async () => {
  try {
    const me = await fetch('/api/user/my-patient', { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : null))
      .catch(() => null)
    const userId = me?.userNo ?? me?.id ?? me
    if (!userId) return
    const today = new Date().toISOString().slice(0, 10)

    const res = await fetch(`/api/record/user/${userId}?date=${today}`, {
      credentials: 'include',
    })
    if (!res.ok) return
    const data = await res.json()
    const saved =
      typeof data.content === 'string' ? JSON.parse(data.content) : data.content
    if (saved) applyContentToForm(saved)
  } catch (e) {
    console.warn('기존 기록 사전 로드 실패:', e)
  }
})
</script>

<style scoped>
.desktop-record-container {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}

.progress-section {
  margin-bottom: 24px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #171717;
}

.progress-bar-wrapper {
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  transition: width 0.3s ease;
}

.step-tabs {
  display: flex;
  gap: 24px;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 24px;
}

.step-tab {
  padding: 12px 0;
  background: none;
  border: none;
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.step-tab:hover {
  color: #171717;
}

.step-tab.active {
  color: #171717;
  font-weight: 600;
}

.step-indicator {
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 2px;
}

.question-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 24px;
  margin-bottom: 24px;
}

.question-text {
  font-weight: 600;
  margin-bottom: 16px;
  line-height: 1.5;
  color: #171717;
}

.answer-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.answer-btn {
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.answer-btn:hover {
  border-color: #657AE2;
  color: #657AE2;
}

.answer-btn.active {
  background: #657AE2;
  border-color: #657AE2;
  color: #ffffff;
}

.detail-input,
.text-input {
  margin-bottom: 24px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #657AE2;
}

.navigation-buttons {
  display: flex;
  gap: 12px;
  margin-top: 32px;
}

.nav-btn {
  flex: 1;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.prev-btn {
  background: #ffffff;
  border: 1px solid #d1d5db;
  color: #6b7280;
}

.prev-btn:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #9ca3af;
}

.prev-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.next-btn {
  background: #657AE2;
  border: 1px solid #657AE2;
  color: #ffffff;
}

.next-btn:hover {
  background: #5568d3;
  border-color: #5568d3;
}
</style>






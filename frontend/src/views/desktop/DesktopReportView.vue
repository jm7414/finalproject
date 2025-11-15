<template>
  <div class="desktop-report-view">
    <!-- 상단: 기간 선택 -->
    <div class="period-selector-card">
      <div class="period-buttons">
        <button
          v-for="p in ['day', 'week', 'month', 'year']"
          :key="p"
          class="period-btn"
          :class="{ active: period === p }"
          @click="setPeriod(p)"
        >
          {{ periodLabel(p) }}
        </button>
      </div>

      <div class="period-inputs">
        <!-- 오늘 기분 -->
        <template v-if="period === 'day'">
          <div class="info-text">
            오늘 상태를 자동으로 업데이트해요. (약 {{ Math.round(POLL_MS / 1000) }}초 간격)
          </div>
        </template>

        <!-- WEEK -->
        <template v-else-if="period === 'week'">
          <div class="input-group">
            <label class="input-label">주(임의의 하루)</label>
            <input type="date" class="form-input" v-model="inputs.anyDayInWeek" />
          </div>
          <div class="range-display">
            <div class="range-label">주간 범위</div>
            <div class="range-value">{{ weekRangeLabelText }}</div>
          </div>
        </template>

        <!-- MONTH -->
        <template v-else-if="period === 'month'">
          <div class="input-row">
            <div class="input-group">
              <label class="input-label">연도</label>
              <input
                type="number"
                class="form-input"
                v-model.number="inputs.year"
                min="2000"
                max="2100"
              />
            </div>
            <div class="input-group">
              <label class="input-label">월</label>
              <input
                type="number"
                class="form-input"
                v-model.number="inputs.month"
                min="1"
                max="12"
              />
            </div>
          </div>
        </template>

        <!-- YEAR -->
        <template v-else>
          <div class="input-group">
            <label class="input-label">연도</label>
            <input
              type="number"
              class="form-input"
              v-model.number="inputs.year"
              min="2000"
              max="2100"
            />
          </div>
        </template>
      </div>

      <div v-if="rangeLabel && period !== 'day'" class="range-info">
        기간: {{ rangeLabel }}
      </div>
    </div>

    <!-- 로딩: 단계형 애니메이션 -->
    <div v-if="loading" class="loading-card">
      <div class="loading-header">
        <div class="loading-title">AI 리포트를 준비하고 있어요</div>
        <div class="loading-percent">{{ yearPercent }}%</div>
      </div>

      <div class="progress-bar-wrapper">
        <div
          class="progress-bar-fill animated"
          :style="{ width: yearPercent + '%' }"
        ></div>
      </div>

      <ul class="loading-steps">
        <li
          v-for="(s, i) in yearSteps"
          :key="i"
          class="loading-step"
        >
          <span
            class="step-dot"
            :class="{
              done: i < yearStepIndex,
              active: i === yearStepIndex,
            }"
          ></span>
          <div class="step-content">
            <span :class="{ 'text-secondary': i > yearStepIndex }">
              {{ s.label }}<span v-if="i === yearStepIndex" class="dots">{{ dots }}</span>
            </span>
            <div v-if="i === yearStepIndex" class="shimmer"></div>
          </div>
          <span v-if="i < yearStepIndex" class="step-check">✔</span>
        </li>
      </ul>
    </div>

    <!-- 에러 -->
    <div v-else-if="error" class="error-alert">{{ error }}</div>

    <!-- 기록 부족 안내 -->
    <div
      v-else-if="insufficient.flag && period !== 'day'"
      class="warning-alert"
    >
      <div class="alert-title">기록이 부족합니다</div>
      <div class="alert-text">
        이 보고서는 <b>주 5일</b> 또는 <b>월·연 70% 이상</b> 기록 시 열립니다.
        <template v-if="insufficient.required !== null">
          (필요: ≥{{ insufficient.required }}일/총 {{ insufficient.total }}일,
          현재: {{ insufficient.covered ?? '—' }}일
          <template v-if="insufficient.covered != null">
            , 남은: {{ Math.max(0, insufficient.required - insufficient.covered) }}일
          </template>
          )
        </template>
      </div>
    </div>

    <!-- 오늘 기분 -->
    <div
      v-if="!loading && !error && period === 'day'"
      class="mood-card"
    >
      <div class="mood-title">오늘 기분</div>
      <div class="mood-emoji">
        <template v-if="dailyResp && (dailyResp.coveredDays === 0 || dailyResp.level === 'none')">
          😴
        </template>
        <template v-else-if="dailyResp && dailyResp.emoji">
          {{ dailyResp.emoji }}
        </template>
        <template v-else>😴</template>
      </div>
      <div v-if="dailyMessage" class="mood-message">
        {{ dailyMessage }}
      </div>
      <div v-if="lastFetchedAt" class="mood-update">
        마지막 업데이트: {{ lastFetchedAt }}
      </div>
    </div>

    <!-- CARE-5 레이더 (주/월) -->
    <div
      v-if="!loading && !insufficient.flag && (period === 'week' || period === 'month')"
      class="chart-card"
    >
      <div class="chart-header">
        <div class="chart-title">CARE-5 레이더</div>
        <div class="chart-subtitle">0–20점</div>
      </div>
      <template v-if="hasScores && !allZero">
        <div class="chart-container">
          <canvas ref="radarRef"></canvas>
        </div>
      </template>
      <template v-else>
        <div class="chart-empty">표시할 점수가 없어요. 기간/데이터를 확인하세요.</div>
      </template>
    </div>

    <!-- 연간 라인 -->
    <div
      v-if="!loading && !insufficient.flag && period === 'year'"
      class="chart-card"
    >
      <div class="chart-header">
        <div class="chart-title">연간 추이 (월별 총점)</div>
        <div class="chart-subtitle">40–70점</div>
      </div>
      <template v-if="yearTotals && yearTotals.length">
        <div class="chart-container">
          <canvas ref="lineRef"></canvas>
        </div>
      </template>
      <template v-else>
        <div class="chart-empty">연간 데이터가 없어요.</div>
      </template>
    </div>

    <!-- 점수 카드 (주/월) -->
    <div
      v-if="!loading && !insufficient.flag && report && (period === 'week' || period === 'month')"
      class="score-card"
    >
      <div class="score-info">
        <div class="score-label">
          {{ report.range?.label || '' }} ({{ periodLabel(report.periodType || period) }})
        </div>
        <div class="score-title">AI 점수</div>
      </div>
      <div class="score-value">
        <div class="score-number">{{ totalScore0to100 }}</div>
        <div class="score-delta">이전 대비 {{ signed(deltaScore) }}</div>
      </div>
    </div>

    <!-- 주간 '한 줄 작업' -->
    <div
      v-if="!loading && !insufficient.flag && period === 'week' && quickActionText"
      class="action-alert"
    >
      <div class="action-title">이번 주 한 줄 작업</div>
      <div class="action-text">{{ quickActionText }}</div>
    </div>

    <!-- 항목별 자세히 보기 -->
    <div
      v-if="!loading && !insufficient.flag && hybridDetailItems.length && period !== 'day'"
      class="detail-card"
    >
      <div class="detail-header">
        <div class="detail-title">
          항목별 자세히 보기
          <span v-if="aiMeta.status === 'ok'" class="badge badge-light">AI 코멘트</span>
          <span v-else-if="aiMeta.status === 'partial-fallback'" class="badge badge-warning">일부 자동 가이드</span>
          <span v-else class="badge badge-danger">생성 실패</span>
        </div>
        <button class="toggle-btn" @click="detailOpen = !detailOpen">
          {{ detailOpen ? '닫기' : '열기' }}
        </button>
      </div>

      <transition name="fade">
        <div v-if="detailOpen" class="detail-content">
          <ul class="detail-list">
            <li v-for="item in hybridDetailItems" :key="item.key" class="detail-item">
              <div class="detail-item-header">
                <div class="detail-item-title">
                  {{ item.label }}
                  <span
                    class="badge"
                    :class="item.source === 'ai' ? 'badge-light' : 'badge-secondary'"
                  >
                    {{ item.source === 'ai' ? 'AI' : '가이드' }}
                  </span>
                </div>
                <div class="detail-item-score">{{ item.value }} / 20</div>
              </div>

              <div class="detail-progress">
                <div
                  class="detail-progress-bar"
                  :style="{
                    width: Math.round((item.value || 0) / 20 * 100) + '%',
                    backgroundColor: team,
                  }"
                ></div>
              </div>

              <div v-if="validText(item.text)" class="detail-text">
                {{ item.text }}
              </div>
              <div v-else class="detail-text-empty">(설명 없음)</div>
            </li>
          </ul>
        </div>
      </transition>
    </div>

    <!-- 랜덤 한 줄 팁 -->
    <div v-if="!loading && tipText && period !== 'day'" class="tip-alert">
      <div class="tip-title">오늘의 작은 팁</div>
      <div class="tip-text">{{ tipText }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import axios from 'axios'
import {
  Chart,
  RadarController,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
  LineController,
  LinearScale,
  CategoryScale,
} from 'chart.js'

Chart.register(
  RadarController,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
  LineController,
  LinearScale,
  CategoryScale
)

const team = '#657AE2'

/* ====== 자동 갱신 설정 (오늘 기분) ====== */
const POLL_MS = 90 * 1000
let pollTimer = null
let midnightTimer = null

/* ---------- 상태 ---------- */
const userId = ref(null)
const period = ref('day')
const inputs = ref({
  anyDayInWeek: todayStr(),
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
})
const report = ref(null)
const yearResp = ref(null)
const dailyResp = ref(null)

const loading = ref(false)
const error = ref('')
const detailOpen = ref(false)
const lastFetchedAt = ref('')

/* "기록 부족" 상태 */
const insufficient = ref({ flag: false, required: null, covered: null, total: null })

/* 랜덤 팁 */
const tips = [
  '하루 물 6~8잔을 목표로 해요. 화장실 가까운 자리의 작은 물병이 도움돼요.',
  '일정은 크게 한 장에! 오늘 해야 할 일 3가지만 적어 같이 확인해요.',
  '낙상 예방을 위해 잘 다니는 길목의 매트를 정리하고, 밤에는 작은 조명을 켜요.',
  '짧은 산책 후 사진 한 장 남기고, 저녁에 함께 보며 대화를 이어가요.',
  '복약은 식사와 묶어 습관화하고, 약 상자는 요일별로 미리 채워두세요.',
]
const tipText = ref('')
function pickTip() {
  tipText.value = tips[Math.floor(Math.random() * tips.length)]
}

/* 차트 refs */
const radarRef = ref(null)
let radarChart = null
const lineRef = ref(null)
let lineChart = null

/* ---------- 연간 로더(단계형) ---------- */
const yearSteps = ref([
  { label: 'AI가 오늘의 기록을 모으는 중' },
  { label: 'AI가 기록을 분석하는 중' },
  { label: 'AI가 그래프를 그리는 중' },
  { label: 'AI가 항목을 정리하는 중' },
])
const yearStepIndex = ref(0)
const yearPercent = ref(0)
const dots = ref('')

let stepTimer = null
let percentTimer = null
let dotTimer = null

function startYearLoader() {
  stopYearLoader()
  yearStepIndex.value = 0
  yearPercent.value = 0
  dots.value = ''
  dotTimer = setInterval(() => {
    dots.value = dots.value.length >= 3 ? '' : dots.value + '.'
  }, 400)
  stepTimer = setInterval(() => {
    if (yearStepIndex.value < yearSteps.value.length - 1) {
      yearStepIndex.value += 1
    }
  }, 2200)
  percentTimer = setInterval(() => {
    const cap = 95
    if (yearPercent.value < cap) {
      yearPercent.value += Math.max(1, Math.round((cap - yearPercent.value) * 0.07))
    }
  }, 180)
}
function finishYearLoader() {
  yearPercent.value = 100
  dots.value = ''
}
function stopYearLoader() {
  if (stepTimer) {
    clearInterval(stepTimer)
    stepTimer = null
  }
  if (percentTimer) {
    clearInterval(percentTimer)
    percentTimer = null
  }
  if (dotTimer) {
    clearInterval(dotTimer)
    dotTimer = null
  }
}

/* ---------- 헬퍼 ---------- */
function periodLabel(p) {
  return p === 'day' ? '오늘 기분' : p === 'week' ? '주간' : p === 'month' ? '월간' : '연간'
}
function signed(n) {
  return (n >= 0 ? '+' : '') + n
}
function pad2(n) {
  return String(n).padStart(2, '0')
}
function todayStr() {
  return fmtLocal(new Date())
}
function fmtLocal(d) {
  const y = d.getFullYear(),
    m = pad2(d.getMonth() + 1),
    day = pad2(d.getDate())
  return `${y}-${m}-${day}`
}
function addDays(d, n) {
  const x = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  x.setDate(x.getDate() + n)
  return x
}
function nowTimeLabel() {
  const d = new Date()
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

/* 기간 계산 */
function weekRange(anyIso) {
  const d = new Date((anyIso || todayStr()) + 'T00:00:00')
  const dow = d.getDay() || 7
  const mon = addDays(d, -(dow - 1))
  const nextMon = addDays(mon, 7)
  return { start: fmtLocal(mon), end: fmtLocal(nextMon) }
}
function monthRange(y, m) {
  return { start: fmtLocal(new Date(y, m - 1, 1)), end: fmtLocal(new Date(y, m, 1)) }
}
function yearRange(y) {
  return { start: fmtLocal(new Date(y, 0, 1)), end: fmtLocal(new Date(y + 1, 0, 1)) }
}

/* 상단 라벨 */
const rangeLabel = computed(() => {
  if (report.value?.range?.start && report.value?.range?.end && period.value !== 'day') {
    return `${report.value.range.start} ~ ${report.value.range.end} (${report.value.range.label})`
  }
  if (period.value === 'year') {
    const y = inputs.value.year
    return `${y}-01-01 ~ ${y + 1}-01-01 (${y}년)`
  }
  return ''
})
const weekRangeLabelText = computed(() => {
  const r = weekRange(inputs.value.anyDayInWeek || todayStr())
  const s = new Date(r.start + 'T00:00:00')
  const e = addDays(new Date(r.end + 'T00:00:00'), -1)
  return `${s.getFullYear()}.${pad2(s.getMonth() + 1)}.${pad2(s.getDate())} ~ ${pad2(e.getMonth() + 1)}.${pad2(e.getDate())}`
})

/* -------- JSON 파서 -------- */
function parseJsonMaybeTwice(x) {
  if (x == null) return null
  if (typeof x !== 'string') return x
  try {
    const a = JSON.parse(x)
    if (typeof a === 'string') {
      try {
        return JSON.parse(a)
      } catch {
        return a
      }
    }
    return a
  } catch {
    return null
  }
}
function getMetrics(rep) {
  return parseJsonMaybeTwice(rep?.metrics)
}
function getSections(rep) {
  return parseJsonMaybeTwice(rep?.sections)
}

/* 점수 배열/상태 (주/월 전용) */
function clamp20(v) {
  const n = Number(v)
  if (!Number.isFinite(n)) return 0
  return Math.max(0, Math.min(20, n))
}
function toScoreArrayFromReport(rep) {
  const s = getMetrics(rep)?.scores
  if (!s) return null
  return [
    clamp20(s.memory_short),
    clamp20(s.memory_long),
    clamp20(s.orientation),
    clamp20(s.adl),
    clamp20(s.behavior_safety),
  ]
}
const currentArray = computed(() =>
  period.value === 'year' || period.value === 'day'
    ? null
    : toScoreArrayFromReport(report.value)
)
const hasScores = computed(() => Array.isArray(currentArray.value) && currentArray.value.length === 5)
const allZero = computed(() => hasScores.value && currentArray.value.every((n) => n === 0))

/* 총점/델타 (주/월) */
const totalScore0to100 = computed(() => {
  const s = getMetrics(report.value)?.scores
  if (!s) return '-'
  const sum = ['memory_short', 'memory_long', 'orientation', 'adl', 'behavior_safety']
    .map((k) => Number(s[k] || 0))
    .reduce((a, b) => a + b, 0)
  return Math.max(0, Math.min(100, Math.round(sum)))
})
const deltaScore = computed(() => {
  const d = report.value?.score?.delta
  return typeof d === 'number' ? d : '-'
})

/* AI 메타 */
const aiMeta = computed(() => {
  if (period.value === 'year') {
    const meta = yearResp.value?.ai
    return { status: meta?.status ?? (yearResp.value?.details?.length ? 'ok' : 'failed') }
  }
  if (period.value === 'day') return { status: 'n/a' }
  const sec = getSections(report.value)
  const meta = sec?.ai
  return { status: meta?.status ?? (sec?.details?.length ? 'ok' : 'failed') }
})

/* 항목별 자세히 보기 (연간 대응) */
const hybridDetailItems = computed(() => {
  if (period.value === 'day') return []
  const details =
    period.value === 'year'
      ? Array.isArray(yearResp.value?.details)
        ? yearResp.value.details
        : []
      : getSections(report.value)?.details || []

  if (Array.isArray(details) && details.length) {
    return details.map((d) => ({
      key: d.key,
      label: d.label,
      value: Number(d.value ?? 0),
      text: d.text ?? '',
      source: d.source === 'rule' ? 'rule' : 'ai',
      aiStatus: d.aiStatus ?? (d.source === 'rule' ? 'failed' : 'ok'),
    }))
  }
  return []
})

/* 텍스트 유효성 */
function validText(t) {
  if (t == null) return false
  const s = String(t).trim()
  return s.length > 0 && s !== '__AI_FAILED__'
}

/* 주간 quick_action */
const quickActionText = computed(() => {
  if (period.value !== 'week' || !report.value) return ''
  const q = getSections(report.value)?.quick_action
  return typeof q === 'string' ? q.trim() : ''
})

/* 연간 보조 계산 */
const yearTotals = computed(() => (Array.isArray(yearResp.value?.totals) ? yearResp.value.totals : []))

/* ---------- 오늘 기분: 한줄 메시지 ---------- */
const dailyMessage = computed(() => {
  if (!dailyResp.value) return ''
  if (dailyResp.value.coveredDays === 0 || dailyResp.value.level === 'none') {
    return '오늘은 기록이 없어요. 편히 쉬고, 내일 한 항목만 체크해볼까요?'
  }
  const score = Number(dailyResp.value.score0to100 ?? 0)
  const lvl = (dailyResp.value.level || inferLevel(score)).toLowerCase()
  if (lvl === 'good') return '오늘은 기분과 컨디션이 좋아 보여요. 행복한 하루 보내세요!'
  if (lvl === 'mid') return '오늘은 무난한 컨디션이에요. 한 가지씩 천천히 해보면 충분해요.'
  return '오늘은 조금 지치실 수 있어요. 천천히 쉬고 안전을 먼저 챙겨요.'
})
function inferLevel(score) {
  if (score >= 67) return 'good'
  if (score >= 34) return 'mid'
  return 'low'
}

/* ---------- 차트 ---------- */
async function renderRadar() {
  const ctx = radarRef.value?.getContext?.('2d')
  if (!ctx) return
  if (!hasScores.value || allZero.value) {
    if (radarChart) {
      radarChart.destroy()
      radarChart = null
    }
    return
  }
  if (radarChart) {
    radarChart.destroy()
    radarChart = null
  }
  radarChart = new Chart(ctx, {
    type: 'radar',
    data: {
      labels: ['단기·작업기억', '장기기억', '지남력', '일상기능', '행동·기분·안전'],
      datasets: [
        {
          label: '이번 기간',
          data: currentArray.value,
          fill: true,
          backgroundColor: 'rgba(101,122,226,0.20)',
          borderColor: team,
          pointBackgroundColor: team,
          pointRadius: 3,
          borderWidth: 2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        r: {
          min: 0,
          max: 20,
          ticks: { stepSize: 5 },
          grid: { circular: true, lineWidth: 1 },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (c) => `${c.dataset.label}: ${c.parsed.r}`,
          },
        },
      },
      elements: { line: { tension: 0.2 } },
    },
  })
}
async function renderYearLine() {
  const ctx = lineRef.value?.getContext?.('2d')
  if (!ctx) return
  const totals = yearTotals.value || []
  if (lineChart) {
    lineChart.destroy()
    lineChart = null
  }
  const labels = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']
  lineChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '월별 총점(20–80)',
          data: totals,
          fill: false,
          borderColor: team,
          pointBackgroundColor: team,
          pointRadius: 3,
          borderWidth: 2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: { type: 'category' },
        y: { type: 'linear', min: 40, max: 70, ticks: { stepSize: 10 } },
      },
      plugins: {
        legend: { display: false },
        tooltip: { enabled: true },
      },
      elements: { line: { tension: 0.2 } },
    },
  })
}

/* ---------- 요구일수 계산 (주 5일 / 월·연 70%) ---------- */
function computeRequirement(periodKind, startIso, endIso) {
  if (periodKind === 'week') return { total: 7, required: 5 }
  const d0 = new Date(startIso + 'T00:00:00')
  const d1 = new Date(endIso + 'T00:00:00')
  const total = Math.max(0, Math.round((d1 - d0) / 86400000))
  const required = Math.ceil(total * 0.7)
  return { total, required }
}

/* ---------- API ---------- */
function computeRange() {
  if (period.value === 'day') {
    return { date: todayStr(), apiPeriod: 'daily' }
  }
  if (period.value === 'week') {
    const r = weekRange(inputs.value.anyDayInWeek)
    return { start: r.start, end: r.end, apiPeriod: 'weekly' }
  }
  if (period.value === 'month') {
    const r = monthRange(inputs.value.year, inputs.value.month)
    return { start: r.start, end: r.end, apiPeriod: 'monthly' }
  }
  const r = yearRange(inputs.value.year)
  return { start: r.start, end: r.end, apiPeriod: 'yearly' }
}

async function fetchTodayMoodOnce() {
  if (!userId.value) return
  try {
    const { date } = computeRange()
    const { data } = await axios.get('/api/ai/report', {
      params: { userId: userId.value, period: 'daily', date },
    })
    dailyResp.value = data || null
    lastFetchedAt.value = nowTimeLabel()
  } catch (e) {
    console.error(e)
  }
}

/* 메인 호출 (주/월/연 자동 갱신 대상) */
async function loadOrCreate() {
  if (!userId.value) {
    error.value = '환자 연결 정보가 없습니다. 먼저 환자를 연결해 주세요.'
    report.value = null
    yearResp.value = null
    insufficient.value = { flag: true, required: null, covered: null, total: null }
    return
  }

  error.value = ''
  insufficient.value = { flag: false, required: null, covered: null, total: null }
  report.value = null
  yearResp.value = null

  if (period.value === 'day') {
    await fetchTodayMoodOnce()
    return
  }

  const { start, end, apiPeriod } = computeRange()
  try {
    loading.value = true
    if (apiPeriod === 'yearly') startYearLoader()

    const { data } = await axios.get('/api/ai/report', {
      params: { userId: userId.value, period: apiPeriod, start, end, mode: 'loadOrCreate' },
    })

    if (data && data.eligibility === 'INSUFFICIENT') {
      const { total, required } = computeRequirement(period.value, start, end)
      insufficient.value = {
        flag: true,
        required,
        covered: data.coveredDays ?? null,
        total,
      }
      return
    }

    if (
      period.value === 'year' &&
      (Array.isArray(data?.totals) || Array.isArray(data?.series) || Array.isArray(data?.details))
    ) {
      const totals = Array.isArray(data?.totals) ? data.totals : []
      const hasMeaningful =
        (totals.length && totals.some((v) => (v ?? 0) > 0)) ||
        (Array.isArray(data?.details) && data.details.length > 0)
      if (!hasMeaningful) {
        const { total, required } = computeRequirement('year', start, end)
        insufficient.value = { flag: true, required, covered: 0, total }
        return
      }
      yearResp.value = data
      return
    }

    report.value = data || null
    if (!data) {
      const { total, required } = computeRequirement(period.value, start, end)
      insufficient.value = { flag: true, required, covered: null, total }
    }
    if (period.value !== 'day') pickTip()
  } catch (e) {
    console.error(e)
    error.value = `응답 오류(${e?.response?.status ?? '???'})`
  } finally {
    if (period.value === 'year') {
      finishYearLoader()
      stopYearLoader()
    }
    loading.value = false
    await nextTick()
    if (period.value === 'year') renderYearLine()
    else if (period.value === 'week' || period.value === 'month') {
      if (!insufficient.value.flag) renderRadar()
    }
  }
}

/* ====== 오늘 기분 자동 갱신 제어 ====== */
function startDailyAutoRefresh() {
  stopDailyAutoRefresh()
  fetchTodayMoodOnce()
  if (!document.hidden) {
    pollTimer = setInterval(() => {
      if (!document.hidden && period.value === 'day') fetchTodayMoodOnce()
    }, POLL_MS)
  }
  scheduleMidnightRefresh()
}
function stopDailyAutoRefresh() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (midnightTimer) {
    clearTimeout(midnightTimer)
    midnightTimer = null
  }
}
function scheduleMidnightRefresh() {
  if (midnightTimer) {
    clearTimeout(midnightTimer)
    midnightTimer = null
  }
  const now = new Date()
  const midnight = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, 5)
  const ms = Math.max(0, midnight.getTime() - now.getTime())
  midnightTimer = setTimeout(async () => {
    await fetchTodayMoodOnce()
    scheduleMidnightRefresh()
  }, ms)
}
function onVisibilityChange() {
  if (period.value !== 'day') return
  if (document.hidden) {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  } else {
    fetchTodayMoodOnce()
    if (!pollTimer) {
      pollTimer = setInterval(() => {
        if (!document.hidden && period.value === 'day') fetchTodayMoodOnce()
      }, POLL_MS)
    }
  }
}

/* ====== 디바운스 ====== */
function debounce(fn, ms = 400) {
  let t = null
  return (...args) => {
    clearTimeout(t)
    t = setTimeout(() => fn(...args), ms)
  }
}
const loadOrCreateDebounced = debounce(() => {
  if (period.value !== 'day') loadOrCreate()
}, 400)

/* 초기화 & 워처 */
function setPeriod(p) {
  period.value = p
  detailOpen.value = false
  if (p === 'week') {
    inputs.value.anyDayInWeek = todayStr()
  } else if (p === 'month') {
    const now = new Date()
    inputs.value.year = now.getFullYear()
    inputs.value.month = now.getMonth() + 1
  } else if (p === 'year') {
    inputs.value.year = new Date().getFullYear()
  }

  if (p === 'day') {
    startDailyAutoRefresh()
  } else {
    stopDailyAutoRefresh()
    loadOrCreateDebounced()
    pickTip()
  }
}

onMounted(async () => {
  try {
    const me = await fetch('/api/user/my-patient', { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : null))
      .catch(() => null)
    userId.value = me?.userNo ?? me?.id ?? null
  } finally {
    startDailyAutoRefresh()
    document.addEventListener('visibilitychange', onVisibilityChange)
    await nextTick()
  }
})

watch(
  () => inputs.value.anyDayInWeek,
  () => {
    if (period.value === 'week') loadOrCreateDebounced()
  }
)
watch(
  () => inputs.value.month,
  () => {
    if (period.value === 'month') loadOrCreateDebounced()
  }
)
watch(
  () => inputs.value.year,
  () => {
    if (period.value === 'month' || period.value === 'year') loadOrCreateDebounced()
  }
)
watch(
  [report, period, yearResp, insufficient],
  async () => {
    await nextTick()
    if (period.value === 'year') renderYearLine()
    else if ((period.value === 'week' || period.value === 'month') && !insufficient.value.flag)
      renderRadar()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  stopDailyAutoRefresh()
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (radarChart) {
    radarChart.destroy()
    radarChart = null
  }
  if (lineChart) {
    lineChart.destroy()
    lineChart = null
  }
  stopYearLoader()
})
</script>

<style scoped>
.desktop-report-view {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
}

.period-selector-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 24px;
  margin-bottom: 24px;
}

.period-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.period-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.period-btn:hover {
  border-color: #657AE2;
  color: #657AE2;
}

.period-btn.active {
  background: #657AE2;
  border-color: #657AE2;
  color: #ffffff;
}

.period-inputs {
  margin-bottom: 16px;
}

.input-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.form-input {
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #657AE2;
}

.range-display {
  margin-top: 16px;
}

.range-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.range-value {
  font-size: 20px;
  font-weight: 600;
  color: #171717;
}

.range-info {
  font-size: 14px;
  color: #6b7280;
  margin-top: 12px;
}

.info-text {
  font-size: 14px;
  color: #6b7280;
}

.loading-card,
.mood-card,
.chart-card,
.score-card,
.detail-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 24px;
  margin-bottom: 24px;
}

.loading-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.loading-title {
  font-weight: 600;
  color: #171717;
}

.loading-percent {
  font-size: 14px;
  color: #6b7280;
}

.progress-bar-wrapper {
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 16px;
}

.progress-bar-fill {
  height: 100%;
  background: #657AE2;
  transition: width 0.3s ease;
}

.progress-bar-fill.animated {
  background: linear-gradient(90deg, #657AE2 0%, #7c8ef0 50%, #657AE2 100%);
  background-size: 200% 100%;
  animation: progress-animate 1.5s linear infinite;
}

@keyframes progress-animate {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.loading-steps {
  list-style: none;
  padding: 0;
  margin: 0;
}

.loading-step {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
}

.step-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d0d4e4;
  margin-top: 6px;
  margin-right: 12px;
  flex-shrink: 0;
}

.step-dot.active {
  background: #657AE2;
  box-shadow: 0 0 0 0 rgba(101, 122, 226, 0.6);
  animation: pulse 1.4s infinite;
}

.step-dot.done {
  background: #20c997;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(101, 122, 226, 0.6);
  }
  70% {
    box-shadow: 0 0 0 12px rgba(101, 122, 226, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(101, 122, 226, 0);
  }
}

.step-content {
  flex: 1;
  font-size: 14px;
  color: #171717;
}

.text-secondary {
  color: #6b7280;
}

.dots {
  display: inline-block;
  width: 16px;
  text-align: left;
}

.shimmer {
  height: 8px;
  border-radius: 6px;
  background: linear-gradient(90deg, #f2f4ff 0%, #e9ecff 30%, #f2f4ff 60%);
  background-size: 200% 100%;
  animation: shimmer 1.2s linear infinite;
  margin-top: 8px;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.step-check {
  margin-left: 12px;
  color: #20c997;
}

.error-alert,
.warning-alert {
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 24px;
}

.error-alert {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.warning-alert {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

.alert-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.alert-text {
  font-size: 14px;
}

.mood-card {
  text-align: center;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.mood-title {
  font-weight: 600;
  margin-bottom: 16px;
  color: #171717;
}

.mood-emoji {
  font-size: 72px;
  line-height: 1;
  margin-bottom: 16px;
}

.mood-message {
  margin-top: 12px;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

.mood-update {
  margin-top: 8px;
  font-size: 14px;
  color: #6b7280;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-title {
  font-weight: 600;
  color: #171717;
}

.chart-subtitle {
  font-size: 14px;
  color: #6b7280;
}

.chart-container {
  position: relative;
  width: 100%;
  height: 280px;
}

.chart-empty {
  padding: 32px;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

.score-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score-info {
  flex: 1;
}

.score-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.score-title {
  font-weight: 600;
  font-size: 20px;
  color: #171717;
  margin-top: 4px;
}

.score-value {
  text-align: right;
}

.score-number {
  font-size: 48px;
  font-weight: 700;
  color: #171717;
  line-height: 1;
}

.score-delta {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.action-alert {
  padding: 16px;
  background: #dbeafe;
  border: 1px solid #93c5fd;
  border-radius: 12px;
  margin-bottom: 24px;
}

.action-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #171717;
}

.action-text {
  color: #171717;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-title {
  font-weight: 600;
  color: #171717;
}

.badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  margin-left: 8px;
}

.badge-light {
  background: #f3f4f6;
  color: #171717;
}

.badge-warning {
  background: #fef3c7;
  color: #92400e;
}

.badge-danger {
  background: #fee2e2;
  color: #991b1b;
}

.badge-secondary {
  background: #e5e7eb;
  color: #6b7280;
}

.toggle-btn {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #ffffff;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-btn:hover {
  border-color: #657AE2;
  color: #657AE2;
}

.detail-content {
  margin-top: 16px;
}

.detail-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.detail-item {
  margin-bottom: 24px;
}

.detail-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.detail-item-title {
  font-weight: 600;
  color: #171717;
}

.detail-item-score {
  font-size: 14px;
  color: #6b7280;
}

.detail-progress {
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
  margin: 8px 0;
}

.detail-progress-bar {
  height: 100%;
  transition: width 0.3s ease;
}

.detail-text {
  font-size: 14px;
  color: #6b7280;
}

.detail-text-empty {
  font-size: 14px;
  color: #9ca3af;
  font-style: italic;
}

.tip-alert {
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  margin-bottom: 24px;
}

.tip-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #171717;
}

.tip-text {
  color: #171717;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>


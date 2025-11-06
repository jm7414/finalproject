<!-- src/views/Report.vue -->
<template>
  <div class="container-sm" style="max-width:414px;">
    <!-- 기간 선택 -->
    <div class="card border-0 shadow-sm mb-3">
      <div class="card-body">
        <div class="d-flex flex-wrap gap-2 mb-3">
          <button
            v-for="p in ['day','week','month','year']"
            :key="p"
            class="btn btn-sm"
            :class="period === p ? 'btn-primary' : 'btn-outline-secondary'"
            @click="setPeriod(p)"
          >
            {{ periodLabel(p) }}
          </button>
        </div>

        <div class="row g-2 align-items-end">
          <!-- DAY -->
          <template v-if="period === 'day'">
            <div class="col-12">
              <label class="form-label small mb-1">날짜</label>
              <input type="date" class="form-control" v-model="inputs.onlyThisDay" />
            </div>
          </template>

          <!-- WEEK -->
          <template v-else-if="period === 'week'">
            <div class="col-12 mb-2">
              <label class="form-label small mb-1">주(임의의 하루)</label>
              <input type="date" class="form-control" v-model="inputs.anyDayInWeek" />
            </div>
            <div class="col-12">
              <div class="small text-secondary">주간 범위</div>
              <div class="fs-5 fw-semibold">{{ weekRangeLabelText }}</div>
            </div>
          </template>

          <!-- MONTH -->
          <template v-else-if="period === 'month'">
            <div class="col-6">
              <label class="form-label small mb-1">연도</label>
              <input type="number" class="form-control" v-model.number="inputs.year" min="2000" max="2100" />
            </div>
            <div class="col-6">
              <label class="form-label small mb-1">월</label>
              <input type="number" class="form-control" v-model.number="inputs.month" min="1" max="12" />
            </div>
          </template>

          <!-- YEAR -->
          <template v-else>
            <div class="col-6">
              <label class="form-label small mb-1">연도</label>
              <input type="number" class="form-control" v-model.number="inputs.year" min="2000" max="2100" />
            </div>
          </template>

          <!-- 액션 -->
          <div class="col-auto d-flex gap-2">
            <button class="btn btn-primary" @click="loadOrCreate">
              {{ period === 'day' ? '오늘의 기록' : 'AI건강체크' }}
            </button>
          </div>
        </div>

        <div v-if="rangeLabel" class="text-secondary small mt-2">기간: {{ rangeLabel }}</div>
      </div>
    </div>

    <!-- 로딩 / 에러 -->
    <div v-if="loading" class="text-center py-5 text-secondary">처리 중…</div>
    <div v-else-if="error" class="alert alert-danger mb-3">{{ error }}</div>

    <!-- 기록 부족 안내 (주/월/연 전용) -->
    <div v-else-if="insufficient.flag && period !== 'day'" class="alert alert-warning border-0 mb-3">
      <div class="fw-semibold mb-1">기록이 부족합니다</div>
      <div class="small">
        해당 기간 전체 기록이 필요해요.
        <template v-if="insufficient.expected !== null">
          (필요: {{ insufficient.expected }}일, 현재: {{ insufficient.covered }}일)
        </template>
      </div>
    </div>

    <!-- ===================== 일간: 이모티콘 + 한줄 메시지 ===================== -->
    <div v-if="!loading && !error && period === 'day'" class="card border-0 shadow-sm mb-3">
      <div class="card-body d-flex flex-column align-items-center justify-content-center" style="min-height:180px;">
        <div v-if="dailyResp && dailyResp.emoji" style="font-size:72px; line-height:1;">
          {{ dailyResp.emoji }}
        </div>
        <div v-else class="text-secondary small">데이터가 없어요.</div>

        <!-- 한줄 메시지 -->
        <div v-if="dailyMessage" class="mt-3 text-center small text-secondary">
          {{ dailyMessage }}
        </div>
      </div>
    </div>
    <!-- ===================================================================== -->

    <!-- CARE-5 레이더 (주/월) -->
    <div
      class="card border-0 shadow-sm mb-3"
      v-if="!loading && !insufficient.flag && (period === 'week' || period === 'month')"
    >
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <div class="fw-semibold">CARE-5 레이더</div>
          <div class="small text-secondary">0–20점</div>
        </div>
        <template v-if="hasScores && !allZero">
          <div class="radar-wrap"><canvas ref="radarRef" /></div>
        </template>
        <template v-else>
          <div class="text-secondary small py-4 text-center">표시할 점수가 없어요. 기간/데이터를 확인하세요.</div>
        </template>
      </div>
    </div>

    <!-- 연간 라인 -->
    <div class="card border-0 shadow-sm mb-3" v-if="!loading && !insufficient.flag && period === 'year'">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <div class="fw-semibold">연간 추이 (월별 총점)</div>
          <div class="small text-secondary">20–80점</div>
        </div>
        <template v-if="yearTotals && yearTotals.length">
          <div class="line-wrap"><canvas ref="lineRef" /></div>
        </template>
        <template v-else>
          <div class="text-secondary small py-4 text-center">연간 데이터가 없어요.</div>
        </template>
      </div>
    </div>

    <!-- 점수 카드 (주/월) -->
    <template v-if="!loading && !insufficient.flag && report && (period === 'week' || period === 'month')">
      <div class="card border-0 shadow-sm mb-3">
        <div class="card-body d-flex justify-content-between align-items-center">
          <div>
            <div class="text-secondary small">
              {{ report.range?.label || '' }} ({{ periodLabel(report.periodType || period) }})
            </div>
            <div class="fw-semibold fs-5 mt-1">AI 점수</div>
          </div>
          <div class="text-end">
            <div class="display-6 fw-bold">{{ totalScore0to100 }}</div>
            <div class="small text-secondary">이전 대비 {{ signed(deltaScore) }}</div>
          </div>
        </div>
      </div>
    </template>

    <!-- 주간 ‘한 줄 작업’ -->
    <div
      class="alert alert-primary border-0"
      v-if="!loading && !insufficient.flag && period === 'week' && quickActionText"
    >
      <div class="fw-semibold mb-1">이번 주 한 줄 작업</div>
      <div>{{ quickActionText }}</div>
    </div>

    <!-- 항목별 자세히 보기 (AI + 규칙 하이브리드 그대로 표시) -->
    <div
      class="card border-0 shadow-sm mb-3"
      v-if="!loading && !insufficient.flag && hybridDetailItems.length && (period === 'week' || period === 'month')"
    >
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center">
          <div class="fw-semibold">
            항목별 자세히 보기
            <span v-if="aiMeta.status === 'ok'" class="badge text-bg-light ms-2">AI 코멘트</span>
            <span v-else-if="aiMeta.status === 'partial-fallback'" class="badge text-bg-warning ms-2">일부 자동 가이드</span>
            <span v-else class="badge text-bg-danger ms-2">생성 실패</span>
          </div>
          <button class="btn btn-sm btn-outline-secondary" @click="detailOpen = !detailOpen">
            {{ detailOpen ? '닫기' : '열기' }}
          </button>
        </div>

        <transition name="fade">
          <div v-if="detailOpen" class="mt-3 small">
            <ul class="list-unstyled mb-0">
              <li v-for="item in hybridDetailItems" :key="item.key" class="mb-3">
                <div class="d-flex justify-content-between">
                  <div class="fw-semibold">
                    {{ item.label }}
                    <span
                      class="badge ms-1"
                      :class="item.source === 'ai' ? 'text-bg-light' : 'text-bg-secondary'"
                    >{{ item.source === 'ai' ? 'AI' : '가이드' }}</span>
                  </div>
                  <div class="text-secondary">{{ item.value }} / 20</div>
                </div>

                <div class="progress my-1" style="height:6px;">
                  <div
                    class="progress-bar"
                    role="progressbar"
                    :style="{ width: Math.round((item.value || 0) / 20 * 100) + '%', backgroundColor: team }"
                  ></div>
                </div>

                <div class="text-secondary" v-if="validText(item.text)">{{ item.text }}</div>
                <div class="text-secondary" v-else>(설명 없음)</div>
              </li>
            </ul>
          </div>
        </transition>
      </div>
    </div>

    <!-- 랜덤 한 줄 팁 (보조) — 일간에는 노출 안 함 -->
    <div class="alert alert-light border" v-if="!loading && tipText && period !== 'day'">
      <div class="fw-semibold mb-1">오늘의 작은 팁</div>
      <div>{{ tipText }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import axios from 'axios'
import {
  Chart, RadarController, RadialLinearScale, PointElement, LineElement, Filler, Tooltip, Legend,
  LineController, LinearScale, CategoryScale
} from 'chart.js'

Chart.register(
  RadarController, RadialLinearScale, PointElement, LineElement, Filler, Tooltip, Legend,
  LineController, LinearScale, CategoryScale
)

const team = '#657AE2'

/* ---------- 상태 ---------- */
const userId = ref(null)
const period = ref('day') // 기본: 일간
const inputs = ref({
  onlyThisDay: todayStr(),
  anyDayInWeek: todayStr(),
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})
const report = ref(null)     // 주/월 응답(ReportVO)
const yearResp = ref(null)   // 연간 응답 { totals, series, details, ai }
const dailyResp = ref(null)  // 일간 응답 { userId, date, score0to100, level, emoji }

const loading = ref(false)
const error = ref('')
const detailOpen = ref(false)

/* “기록 부족” 상태 (주/월/연 전용) */
const insufficient = ref({ flag: false, expected: null, covered: null })

/* 랜덤 팁 (주/월/연만 노출) */
const tips = [
  '하루 물 6~8잔을 목표로 해요. 화장실 가까운 자리의 작은 물병이 도움돼요.',
  '일정은 크게 한 장에! 오늘 해야 할 일 3가지만 적어 같이 확인해요.',
  '낙상 예방을 위해 잘 다니는 길목의 매트를 정리하고, 밤에는 작은 조명을 켜요.',
  '짧은 산책 후 사진 한 장 남기고, 저녁에 함께 보며 대화를 이어가요.',
  '복약은 식사와 묶어 습관화하고, 약 상자는 요일별로 미리 채워두세요.'
]
const tipText = ref('')
function pickTip() { tipText.value = tips[Math.floor(Math.random() * tips.length)] }

/* 차트 refs */
const radarRef = ref(null); let radarChart = null
const lineRef = ref(null); let lineChart = null

/* ---------- 헬퍼 ---------- */
function periodLabel(p) {
  return p === 'day' ? '일간' : p === 'week' ? '주간' : p === 'month' ? '월간' : '연간'
}
function signed(n) { return (n >= 0 ? '+' : '') + n }
function pad2(n) { return String(n).padStart(2, '0') }
function todayStr() { return fmtLocal(new Date()) }
function fmtLocal(d) { const y = d.getFullYear(), m = pad2(d.getMonth() + 1), day = pad2(d.getDate()); return `${y}-${m}-${day}` }
function addDays(d, n) { const x = new Date(d.getFullYear(), d.getMonth(), d.getDate()); x.setDate(x.getDate() + n); return x }

/* 기간 계산 */
function weekRange(anyIso) {
  const d = new Date(anyIso + 'T00:00:00')
  const dow = d.getDay() || 7
  const mon = addDays(d, -(dow - 1))
  const nextMon = addDays(mon, 7)
  return { start: fmtLocal(mon), end: fmtLocal(nextMon) }
}
function monthRange(y, m) { return { start: fmtLocal(new Date(y, m - 1, 1)), end: fmtLocal(new Date(y, m, 1)) } }
function yearRange(y) { return { start: fmtLocal(new Date(y, 0, 1)), end: fmtLocal(new Date(y + 1, 0, 1)) } }

/* 상단 라벨 */
const rangeLabel = computed(() => {
  if (period.value === 'day') {
    return inputs.value.onlyThisDay || ''
  }
  if (report.value?.range?.start && report.value?.range?.end) {
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
  return `${s.getFullYear()}.${pad2(s.getMonth() + 1)}.${pad2(s.getDate())} ~ ${e.getMonth() + 1}.${e.getDate()}`
})

/* -------- JSON 파서 -------- */
function parseJsonMaybeTwice(x) {
  if (x == null) return null
  if (typeof x !== 'string') return x
  try {
    const a = JSON.parse(x)
    if (typeof a === 'string') {
      try { return JSON.parse(a) } catch { return a }
    }
    return a
  } catch { return null }
}
function getMetrics(rep) { return parseJsonMaybeTwice(rep?.metrics) }
function getSections(rep) { return parseJsonMaybeTwice(rep?.sections) }

/* 점수 배열/상태 (주/월 전용) */
function clamp20(v) { const n = Number(v); if (!Number.isFinite(n)) return 0; return Math.max(0, Math.min(20, n)) }
function toScoreArrayFromReport(rep) {
  const s = getMetrics(rep)?.scores; if (!s) return null
  return [clamp20(s.memory_short), clamp20(s.memory_long), clamp20(s.orientation), clamp20(s.adl), clamp20(s.behavior_safety)]
}
const currentArray = computed(() => (period.value === 'year' || period.value === 'day' ? null : toScoreArrayFromReport(report.value)))
const hasScores = computed(() => Array.isArray(currentArray.value) && currentArray.value.length === 5)
const allZero = computed(() => hasScores.value && currentArray.value.every(n => n === 0))

/* 총점/델타 (주/월) */
const totalScore0to100 = computed(() => {
  const s = getMetrics(report.value)?.scores; if (!s) return '-'
  const sum = ['memory_short', 'memory_long', 'orientation', 'adl', 'behavior_safety']
    .map(k => Number(s[k] || 0)).reduce((a, b) => a + b, 0)
  return Math.max(0, Math.min(100, Math.round(sum)))
})
const deltaScore = computed(() => {
  const d = report.value?.score?.delta
  return (typeof d === 'number') ? d : '-'
})

/* ✅ AI 메타(뱃지용) - 주/월/연 전용 */
const aiMeta = computed(() => {
  if (period.value === 'year') {
    const meta = yearResp.value?.ai
    return { status: meta?.status ?? (yearResp.value?.details?.length ? 'ok' : 'failed') }
  }
  if (period.value === 'day') return { status: 'n/a' }
  const sec = getSections(report.value)
  const meta = sec?.ai
  return { status: meta?.status ?? ((sec?.details?.length) ? 'ok' : 'failed') }
})

/* ✅ 항목별 자세히 보기 (주/월) */
const hybridDetailItems = computed(() => {
  if (period.value === 'year' || period.value === 'day') return []
  const details = getSections(report.value)?.details
  if (Array.isArray(details) && details.length) {
    return details.map(d => ({
      key: d.key,
      label: d.label,
      value: Number(d.value ?? 0),
      text: (d.text ?? ''),
      source: d.source === 'rule' ? 'rule' : 'ai',
      aiStatus: d.aiStatus ?? (d.source === 'rule' ? 'failed' : 'ok')
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
  return (typeof q === 'string') ? q.trim() : ''
})

/* 연간 보조 계산 */
const yearTotals = computed(() => Array.isArray(yearResp.value?.totals) ? yearResp.value.totals : [])

/* ---------- 일간 한줄 메시지 ---------- */
const dailyMessage = computed(() => {
  if (!dailyResp.value) return ''
  const score = Number(dailyResp.value.score0to100 ?? 0)
  const lvl = (dailyResp.value.level || inferLevel(score)).toLowerCase()
  if (lvl === 'good') return '오늘은 기분과 컨디션이 좋아 보여요. 행복한 하루 보내세요!'
  if (lvl === 'mid')  return '오늘은 무난한 컨디션이에요. 한 가지씩 천천히 해보면 충분해요.'
  return '오늘은 조금 지치실 수 있어요. 천천히 쉬고 안전을 먼저 챙겨요.'
})
function inferLevel(score) {
  if (score >= 67) return 'good'
  if (score >= 34) return 'mid'
  return 'low'
}

/* ---------- 차트 ---------- */
async function renderRadar() {
  const ctx = radarRef.value?.getContext?.('2d'); if (!ctx) return
  if (!hasScores.value || allZero.value) { if (radarChart) { radarChart.destroy(); radarChart = null } return }
  if (radarChart) { radarChart.destroy(); radarChart = null }
  radarChart = new Chart(ctx, {
    type: 'radar',
    data: {
      labels: ['단기·작업기억', '장기기억', '지남력', '일상기능', '행동·기분·안전'],
      datasets: [{
        label: '이번 기간',
        data: currentArray.value,
        fill: true,
        backgroundColor: 'rgba(101,122,226,0.20)',
        borderColor: team,
        pointBackgroundColor: team,
        pointRadius: 3,
        borderWidth: 2
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      scales: { r: { min: 0, max: 20, ticks: { stepSize: 5 }, grid: { circular: true, lineWidth: 1 } } },
      plugins: { legend: { display: false }, tooltip: { callbacks: { label: (c) => `${c.dataset.label}: ${c.parsed.r}` } } },
      elements: { line: { tension: 0.2 } }
    }
  })
}
async function renderYearLine() {
  const ctx = lineRef.value?.getContext?.('2d'); if (!ctx) return
  const totals = yearTotals.value || []
  if (lineChart) { lineChart.destroy(); lineChart = null }
  const labels = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']
  lineChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [{
        label: '월별 총점(20–80)',
        data: totals,
        fill: false,
        borderColor: team,
        pointBackgroundColor: team,
        pointRadius: 3,
        borderWidth: 2
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      scales: { x: { type: 'category' }, y: { type: 'linear', min: 20, max: 80, ticks: { stepSize: 10 } } },
      plugins: { legend: { display: false }, tooltip: { enabled: true } },
      elements: { line: { tension: 0.2 } }
    }
  })
}

/* ---------- API ---------- */
function computeRange() {
  if (period.value === 'day') {
    return { date: inputs.value.onlyThisDay, apiPeriod: 'daily' }
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

async function loadOrCreate() {
  error.value = ''
  insufficient.value = { flag: false, expected: null, covered: null }
  report.value = null
  yearResp.value = null
  dailyResp.value = null

  if (!userId.value) { error.value = '환자 연결 정보가 없습니다.'; return }

  const { start, end, apiPeriod, date } = computeRange()
  try {
    loading.value = true

    if (apiPeriod === 'daily') {
      const { data } = await axios.get('/api/ai/report', {
        params: { userId: userId.value, period: 'daily', date }
      })
      dailyResp.value = data || null
      return
    }

    const { data } = await axios.get('/api/ai/report', {
      params: { userId: userId.value, period: apiPeriod, start, end, mode: 'loadOrCreate' }
    })

    if (data && data.eligibility === 'INSUFFICIENT') {
      insufficient.value = { flag: true, expected: (data.expectedDays ?? null), covered: (data.coveredDays ?? null) }
      return
    }

    if (period.value === 'year' && (Array.isArray(data?.totals) || Array.isArray(data?.series) || Array.isArray(data?.details))) {
      yearResp.value = data
      return
    }

    if (data) { report.value = data }
    else { insufficient.value = { flag: true, expected: null, covered: null } }

    pickTip()
  } catch (e) {
    console.error(e)
    error.value = `응답 오류(${e?.response?.status ?? '???'})`
  } finally {
    loading.value = false
    await nextTick()
    if (period.value === 'year') renderYearLine()
    else if (period.value === 'week' || period.value === 'month') {
      if (!insufficient.value.flag) renderRadar()
    }
  }
}

/* 초기화 */
function setPeriod(p) {
  period.value = p; detailOpen.value = false
  if (p === 'day') {
    inputs.value.onlyThisDay = todayStr()
  } else if (p === 'week') {
    inputs.value.anyDayInWeek = todayStr()
  } else if (p === 'month') {
    const now = new Date()
    inputs.value.year = now.getFullYear()
    inputs.value.month = now.getMonth() + 1
  } else {
    inputs.value.year = new Date().getFullYear()
  }
  if (p !== 'day') pickTip()
}
onMounted(async () => {
  try {
    const me = await fetch('/api/user/my-patient', { credentials: 'include' })
      .then(r => r.ok ? r.json() : null).catch(() => null)
    userId.value = me?.userNo ?? me?.id ?? me ?? 4 // 없으면 4로 테스트
  } finally {
    if (period.value !== 'day') pickTip()
    await nextTick()
    if (period.value === 'year') renderYearLine()
    else if (period.value === 'week' || period.value === 'month') renderRadar()
  }
})
watch([report, period, yearResp], async () => {
  await nextTick()
  if (period.value === 'year') renderYearLine()
  else if ((period.value === 'week' || period.value === 'month') && !insufficient.value.flag) renderRadar()
}, { deep: true })
onBeforeUnmount(() => {
  if (radarChart) { radarChart.destroy(); radarChart = null }
  if (lineChart) { lineChart.destroy(); lineChart = null }
})
</script>

<style scoped>
.container-sm { padding-bottom: 24px; }
.radar-wrap, .line-wrap { position: relative; width: 100%; height: 280px; }
.fade-enter-active, .fade-leave-active { transition: opacity .15s ease }
.fade-enter-from, .fade-leave-to { opacity: 0 }
</style>

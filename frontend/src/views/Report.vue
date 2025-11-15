<!-- src/views/Report.vue -->
<template>
    <div class="container-sm report-page" style="max-width:414px;">
        <!-- 상단: 기간 선택 + 헤더 -->
        <div class="card border-0 shadow-sm mb-3">
            <div class="card-body">
                <!-- 오늘/주간/월간/연간 토글 -->
                <div class="period-toggle mb-3">
                    <div class="toggle-track" ref="toggleTrack">
                        <div class="toggle-thumb" :style="thumbStyle"></div>
                        <button
                            v-for="p in periodOrder"
                            :key="p"
                            type="button"
                            class="toggle-btn"
                            :class="{ active: period === p }"
                            @click="setPeriod(p)"
                            ref="toggleBtns"
                        >
                            {{ periodShortLabel(p) }}
                        </button>
                    </div>
                </div>

                <!-- 타이틀 -->
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="text-secondary small">AI 기록 리포트</div>
                        <div class="fw-semibold">CARE-5 보고서</div>
                    </div>
                    <span class="badge rounded-pill bg-light text-dark small">
                        {{ periodLabel(period) }}
                    </span>
                </div>

                <!-- 기간 입력 영역 -->
                <div class="row g-2 align-items-end period-inputs mt-3">
                    <!-- DAY -->
                    <template v-if="period === 'day'">
                        <div class="col-12 small text-secondary">
                            오늘 상태를 자동으로 업데이트해요. (약 {{ Math.round(POLL_MS / 1000) }}초 간격)
                        </div>
                    </template>

                    <!-- WEEK -->
                    <template v-else-if="period === 'week'">
                        <div class="col-12 mb-2">
                            <label class="form-label form-label-sm mb-1">한 주 중 아무 날이나 선택해 주세요</label>
                            <input
                                type="date"
                                class="form-control form-control-sm pill-input-single"
                                v-model="inputs.anyDayInWeek"
                            />
                        </div>
                        <div class="col-12">
                            <div class="small text-secondary">주간 범위</div>
                            <div class="fs-6 fw-semibold">{{ weekRangeLabelText }}</div>
                        </div>
                    </template>

                    <!-- MONTH (월 달력) -->
                    <template v-else-if="period === 'month'">
                        <div class="col-12">
                            <label class="form-label form-label-sm mb-1">연·월 선택</label>
                            <input
                                type="month"
                                class="form-control form-control-sm pill-input-single"
                                v-model="monthModel"
                            />
                        </div>
                    </template>

                    <!-- YEAR (연도 버튼 선택) -->
                    <template v-else>
                        <div class="col-12">
                            <label class="form-label form-label-sm mb-1">연도 선택</label>
                            <div class="year-toggle">
                                <button
                                    v-for="y in yearOptions"
                                    :key="y"
                                    type="button"
                                    class="btn btn-sm year-btn"
                                    :class="{ active: inputs.year === y }"
                                    @click="setYear(y)"
                                >
                                    {{ y }}년
                                </button>
                            </div>
                            <div class="form-text small text-secondary mt-1">
                                선택한 연도 전체 기록
                            </div>
                        </div>
                    </template>
                </div>

                <div v-if="rangeLabel && period !== 'day'" class="text-secondary small mt-2">
                    기간: {{ rangeLabel }}
                </div>
            </div>
        </div>

        <!-- 로딩: 단계형 애니메이션 -->
        <div v-if="loading" class="card border-0 shadow-sm mb-3">
            <div class="card-body">
                <div class="d-flex align-items-center justify-content-between mb-1">
                    <div class="fw-semibold">AI 리포트를 준비하고 있어요</div>
                    <div class="small text-secondary">{{ yearPercent }}%</div>
                </div>

                <div class="progress my-2" style="height:8px;">
                    <div
                        class="progress-bar progress-bar-striped progress-bar-animated"
                        role="progressbar"
                        :style="{ width: yearPercent + '%' }"
                    ></div>
                </div>

                <ul class="list-unstyled mt-3 mb-0 small">
                    <li v-for="(s, i) in yearSteps" :key="i" class="d-flex align-items-start mb-2">
                        <span
                            class="step-dot me-2"
                            :class="{
                                done: i < yearStepIndex,
                                active: i === yearStepIndex
                            }"
                        ></span>
                        <div class="flex-grow-1">
                            <span :class="{ 'text-secondary': i > yearStepIndex }">
                                {{ s.label }}<span v-if="i === yearStepIndex" class="dots">{{ dots }}</span>
                            </span>
                            <div v-if="i === yearStepIndex" class="shimmer mt-1"></div>
                        </div>
                        <span v-if="i < yearStepIndex" class="ms-2 text-success">✔</span>
                    </li>
                </ul>
            </div>
        </div>

        <!-- 에러 -->
        <div v-else-if="error" class="alert alert-danger mb-3">
            {{ error }}
        </div>

        <!-- 기록 부족 안내 (주/월/연 전용) -->
        <div v-else-if="insufficient.flag && period !== 'day'" class="alert alert-warning border-0 mb-3">
            <div class="fw-semibold mb-1">기록이 부족합니다</div>
            <div class="small">
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
        <div v-if="!loading && !error && period === 'day'" class="card border-0 shadow-sm mb-3">
            <div class="card-body d-flex flex-column align-items-center justify-content-center" style="min-height:180px;">
                <div class="fw-semibold mb-2">오늘 기분</div>

                <div class="today-emoji">
                    <template v-if="dailyResp && (dailyResp.coveredDays === 0 || dailyResp.level === 'none')">😴</template>
                    <template v-else-if="dailyResp && dailyResp.emoji">
                        {{ dailyResp.emoji }}
                    </template>
                    <template v-else>😴</template>
                </div>

                <div v-if="dailyMessage" class="mt-3 text-center small text-secondary">
                    {{ dailyMessage }}
                </div>

                <div class="small text-secondary mt-2" v-if="lastFetchedAt">
                    마지막 업데이트: {{ lastFetchedAt }}
                </div>
            </div>
        </div>

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
                    <div class="radar-wrap">
                        <canvas ref="radarRef" />
                    </div>
                </template>
                <template v-else>
                    <div class="text-secondary small py-4 text-center">
                        표시할 점수가 없어요. 기간/데이터를 확인하세요.
                    </div>
                </template>
            </div>
        </div>

        <!-- 연간 라인 -->
        <div class="card border-0 shadow-sm mb-3" v-if="!loading && !insufficient.flag && period === 'year'">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div class="fw-semibold">연간 추이 (월별 총점)</div>
                    <div class="small text-secondary">40–70점</div>
                </div>
                <template v-if="yearTotals && yearTotals.length">
                    <div class="line-wrap">
                        <canvas ref="lineRef" />
                    </div>
                </template>
                <template v-else>
                    <div class="text-secondary small py-4 text-center">
                        연간 데이터가 없어요.
                    </div>
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
                        <div class="display-6 fw-bold score-display">
                            {{ totalScore0to100 }}
                        </div>
                        <div class="small text-secondary">
                            이전 대비 {{ signed(deltaScore) }}
                        </div>
                    </div>
                </div>
            </div>
        </template>

        <!-- 항목별 자세히 보기 -->
        <div
            class="card border-0 shadow-sm mb-3"
            v-if="!loading && !insufficient.flag && hybridDetailItems.length && period !== 'day'"
        >
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div class="fw-semibold">
                        항목별 자세히 보기
                        <span v-if="aiMeta.status === 'ok'" class="badge text-bg-light ms-2">AI 코멘트</span>
                        <span v-else-if="aiMeta.status === 'partial-fallback'" class="badge text-bg-warning ms-2">
                            일부 자동 가이드
                        </span>
                        <span v-else class="badge text-bg-danger ms-2">생성 실패</span>
                    </div>
                    <button class="btn btn-sm btn-outline-secondary" type="button" @click="detailOpen = !detailOpen">
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
                                        >
                                            {{ item.source === 'ai' ? 'AI' : '가이드' }}
                                        </span>
                                    </div>
                                    <div class="text-secondary">
                                        {{ item.value }} / 20
                                    </div>
                                </div>

                                <div class="progress my-1" style="height:6px;">
                                    <div
                                        class="progress-bar"
                                        role="progressbar"
                                        :style="{
                                            width: Math.round((item.value || 0) / 20 * 100) + '%',
                                            backgroundColor: team
                                        }"
                                    />
                                </div>

                                <div class="text-secondary" v-if="validText(item.text)">
                                    {{ item.text }}
                                </div>
                                <div class="text-secondary" v-else>(설명 없음)</div>
                            </li>
                        </ul>
                    </div>
                </transition>
            </div>
        </div>

        <!-- 랜덤 한 줄 팁 -->
        <div v-if="!loading && tipText && period !== 'day'" class="tip-card">
            <div class="tip-title">오늘의 작은 팁</div>
            <div class="tip-body">
                {{ tipText }}
            </div>
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
    CategoryScale
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
const POLL_MS = 90 * 1000
let pollTimer = null
let midnightTimer = null

const userId = ref(null)
const period = ref('day')
const periodOrder = ['day', 'week', 'month', 'year']

const now = new Date()
const inputs = ref({
    anyDayInWeek: todayStr(),
    year: now.getFullYear(),
    month: now.getMonth() + 1
})

const yearOptions = [
    now.getFullYear() - 2,
    now.getFullYear() - 1,
    now.getFullYear()
]

const report = ref(null)
const yearResp = ref(null)
const dailyResp = ref(null)

const loading = ref(false)
const error = ref('')
const detailOpen = ref(false)
const lastFetchedAt = ref('')

const insufficient = ref({
    flag: false,
    required: null,
    covered: null,
    total: null
})

const tips = [
    '하루 물 6~8잔을 목표로 해요. 화장실 가까운 자리의 작은 물병이 도움돼요.',
    '일정은 크게 한 장에! 오늘 해야 할 일 3가지만 적어 같이 확인해요.',
    '낙상 예방을 위해 잘 다니는 길목의 매트를 정리하고, 밤에는 작은 조명을 켜요.',
    '짧은 산책 후 사진 한 장 남기고, 저녁에 함께 보며 대화를 이어가요.',
    '복약은 식사와 묶어 습관화하고, 약 상자는 요일별로 미리 채워두세요.'
]
const tipText = ref('')
function pickTip() {
    tipText.value = tips[Math.floor(Math.random() * tips.length)]
}

/* 상단 토글 thumb 계산 */
const toggleTrack = ref(null)
const toggleBtns = ref([])
const thumbStyle = ref({})

async function updateThumb() {
    await nextTick()
    const trackEl = toggleTrack.value
    const btnEls = toggleBtns.value
    if (!trackEl || !btnEls || !btnEls.length) return

    const idx = periodOrder.indexOf(period.value)
    const btnEl = btnEls[idx]
    if (!btnEl) return

    const trackRect = trackEl.getBoundingClientRect()
    const btnRect = btnEl.getBoundingClientRect()

    const left = btnRect.left - trackRect.left
    const width = btnRect.width

    thumbStyle.value = {
        left: left + 'px',
        width: width + 'px'
    }
}

/* 차트 refs */
const radarRef = ref(null)
let radarChart = null
const lineRef = ref(null)
let lineChart = null

/* 연간 로더 */
const yearSteps = ref([
    { label: 'AI가 오늘의 기록을 모으는 중' },
    { label: 'AI가 기록을 분석하는 중' },
    { label: 'AI가 그래프를 그리는 중' },
    { label: 'AI가 항목을 정리하는 중' }
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
            yearPercent.value += Math.max(
                1,
                Math.round((cap - yearPercent.value) * 0.07)
            )
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

/* 헬퍼 */
function periodLabel(p) {
    return p === 'day'
        ? '오늘 기분'
        : p === 'week'
            ? '주간'
            : p === 'month'
                ? '월간'
                : '연간'
}
function periodShortLabel(p) {
    return p === 'day'
        ? '오늘'
        : p === 'week'
            ? '주간'
            : p === 'month'
                ? '월간'
                : '연간'
}
function signed(n) {
    return n === '-' ? '-' : (n >= 0 ? '+' : '') + n
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
    return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(
        d.getSeconds()
    )}`
}

/* 월/연도 달력용 v-model (월간 전용) */
const monthModel = computed({
    get() {
        const y = inputs.value.year || new Date().getFullYear()
        const m = inputs.value.month || new Date().getMonth() + 1
        return `${y}-${pad2(m)}`
    },
    set(val) {
        if (!val) return
        const [yStr, mStr] = val.split('-')
        const y = Number(yStr)
        const m = Number(mStr)
        if (!Number.isFinite(y) || !Number.isFinite(m)) return
        inputs.value.year = y
        inputs.value.month = m
    }
})

/* 기간 계산 */
function weekRange(anyIso) {
    const d = new Date((anyIso || todayStr()) + 'T00:00:00')
    const dow = d.getDay() || 7
    const mon = addDays(d, -(dow - 1))
    const nextMon = addDays(mon, 7)
    return { start: fmtLocal(mon), end: fmtLocal(nextMon) }
}
function monthRange(y, m) {
    return {
        start: fmtLocal(new Date(y, m - 1, 1)),
        end: fmtLocal(new Date(y, m, 1))
    }
}
function yearRange(y) {
    return {
        start: fmtLocal(new Date(y, 0, 1)),
        end: fmtLocal(new Date(y + 1, 0, 1))
    }
}

const rangeLabel = computed(() => {
    if (
        report.value?.range?.start &&
        report.value?.range?.end &&
        period.value !== 'day'
    ) {
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
    return `${s.getFullYear()}.${pad2(s.getMonth() + 1)}.${pad2(
        s.getDate()
    )} ~ ${pad2(e.getMonth() + 1)}.${pad2(e.getDate())}`
})

/* JSON 파서 */
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

/* 점수 관련 */
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
        clamp20(s.behavior_safety)
    ]
}
const currentArray = computed(() =>
    period.value === 'year' || period.value === 'day'
        ? null
        : toScoreArrayFromReport(report.value)
)
const hasScores = computed(
    () => Array.isArray(currentArray.value) && currentArray.value.length === 5
)
const allZero = computed(
    () => hasScores.value && currentArray.value.every(n => n === 0)
)

const totalScore0to100 = computed(() => {
    const s = getMetrics(report.value)?.scores
    if (!s) return '-'
    const sum = [
        'memory_short',
        'memory_long',
        'orientation',
        'adl',
        'behavior_safety'
    ]
        .map(k => Number(s[k] || 0))
        .reduce((a, b) => a + b, 0)
    return Math.max(0, Math.min(100, Math.round(sum)))
})
const deltaScore = computed(() => {
    const d = report.value?.score?.delta
    return typeof d === 'number' ? d : '-'
})

/* AI 메타/디테일 */
const aiMeta = computed(() => {
    if (period.value === 'year') {
        const meta = yearResp.value?.ai
        return {
            status:
                meta?.status ??
                (yearResp.value?.details?.length ? 'ok' : 'failed')
        }
    }
    if (period.value === 'day') return { status: 'n/a' }
    const sec = getSections(report.value)
    const meta = sec?.ai
    return {
        status: meta?.status ?? (sec?.details?.length ? 'ok' : 'failed')
    }
})

const hybridDetailItems = computed(() => {
    if (period.value === 'day') return []
    const details =
        period.value === 'year'
            ? Array.isArray(yearResp.value?.details)
                ? yearResp.value.details
                : []
            : getSections(report.value)?.details || []

    if (Array.isArray(details) && details.length) {
        return details.map(d => ({
            key: d.key,
            label: d.label,
            value: Number(d.value ?? 0),
            text: d.text ?? '',
            source: d.source === 'rule' ? 'rule' : 'ai',
            aiStatus: d.aiStatus ?? (d.source === 'rule' ? 'failed' : 'ok')
        }))
    }
    return []
})

function validText(t) {
    if (t == null) return false
    const s = String(t).trim()
    return s.length > 0 && s !== '__AI_FAILED__'
}

const yearTotals = computed(() =>
    Array.isArray(yearResp.value?.totals) ? yearResp.value.totals : []
)

/* 오늘 기분 메시지 */
const dailyMessage = computed(() => {
    if (!dailyResp.value) return ''
    if (
        dailyResp.value.coveredDays === 0 ||
        dailyResp.value.level === 'none'
    ) {
        return '오늘은 기록이 없어요. 편히 쉬고, 내일 한 항목만 체크해볼까요?'
    }
    const score = Number(dailyResp.value.score0to100 ?? 0)
    const lvl = (dailyResp.value.level || inferLevel(score)).toLowerCase()
    if (lvl === 'good')
        return '오늘은 기분과 컨디션이 좋아 보여요. 행복한 하루 보내세요!'
    if (lvl === 'mid')
        return '오늘은 무난한 컨디션이에요. 한 가지씩 천천히 해보면 충분해요.'
    return '오늘은 조금 지치실 수 있어요. 천천히 쉬고 안전을 먼저 챙겨요.'
})
function inferLevel(score) {
    if (score >= 67) return 'good'
    if (score >= 34) return 'mid'
    return 'low'
}

/* 차트 렌더링 */
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
                    borderWidth: 2
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                r: {
                    min: 0,
                    max: 20,
                    ticks: { stepSize: 5 },
                    grid: { circular: true, lineWidth: 1 }
                }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: c => `${c.dataset.label}: ${c.parsed.r}`
                    }
                }
            },
            elements: { line: { tension: 0.2 } }
        }
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
    const labels = [
        '1월',
        '2월',
        '3월',
        '4월',
        '5월',
        '6월',
        '7월',
        '8월',
        '9월',
        '10월',
        '11월',
        '12월'
    ]
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
                    borderWidth: 2
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { type: 'category' },
                y: { type: 'linear', min: 40, max: 70, ticks: { stepSize: 10 } }
            },
            plugins: {
                legend: { display: false },
                tooltip: { enabled: true }
            },
            elements: { line: { tension: 0.2 } }
        }
    })
}

/* 요구일수 계산 */
function computeRequirement(periodKind, startIso, endIso) {
    if (periodKind === 'week') return { total: 7, required: 5 }
    const d0 = new Date(startIso + 'T00:00:00')
    const d1 = new Date(endIso + 'T00:00:00')
    const total = Math.max(0, Math.round((d1 - d0) / 86400000))
    const required = Math.ceil(total * 0.7)
    return { total, required }
}

/* API 호출용 범위 */
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

/* 오늘 기분 단일 호출 */
async function fetchTodayMoodOnce() {
    if (!userId.value) return
    try {
        const { date } = computeRange()
        const { data } = await axios.get('/api/ai/report', {
            params: { userId: userId.value, period: 'daily', date }
        })
        dailyResp.value = data || null
        lastFetchedAt.value = nowTimeLabel()
    } catch (e) {
        console.error(e)
    }
}

/* 메인 로딩 (주/월/연) */
async function loadOrCreate() {
    if (!userId.value) {
        error.value = '환자 연결 정보가 없습니다. 먼저 환자를 연결해 주세요.'
        report.value = null
        yearResp.value = null
        insufficient.value = {
            flag: true,
            required: null,
            covered: null,
            total: null
        }
        return
    }

    error.value = ''
    insufficient.value = {
        flag: false,
        required: null,
        covered: null,
        total: null
    }
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
            params: {
                userId: userId.value,
                period: apiPeriod,
                start,
                end,
                mode: 'loadOrCreate'
            }
        })

        if (data && data.eligibility === 'INSUFFICIENT') {
            const { total, required } = computeRequirement(period.value, start, end)
            insufficient.value = {
                flag: true,
                required,
                covered: data.coveredDays ?? null,
                total
            }
            return
        }

        if (
            period.value === 'year' &&
            (Array.isArray(data?.totals) ||
                Array.isArray(data?.series) ||
                Array.isArray(data?.details))
        ) {
            const totals = Array.isArray(data?.totals) ? data.totals : []
            const hasMeaningful =
                (totals.length && totals.some(v => (v ?? 0) > 0)) ||
                (Array.isArray(data?.details) && data.details.length > 0)
            if (!hasMeaningful) {
                const { total, required } = computeRequirement('year', start, end)
                insufficient.value = {
                    flag: true,
                    required,
                    covered: 0,
                    total
                }
                return
            }
            yearResp.value = data
            return
        }

        report.value = data || null
        if (!data) {
            const { total, required } = computeRequirement(period.value, start, end)
            insufficient.value = {
                flag: true,
                required,
                covered: null,
                total
            }
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

/* 오늘 기분 자동 갱신 */
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
    const nowD = new Date()
    const midnight = new Date(
        nowD.getFullYear(),
        nowD.getMonth(),
        nowD.getDate() + 1,
        0,
        0,
        5
    )
    const ms = Math.max(0, midnight.getTime() - nowD.getTime())
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

/* 디바운스 */
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

/* 연도 버튼 클릭 */
function setYear(y) {
    if (inputs.value.year === y) return
    inputs.value.year = y
}

/* period 변경 */
function setPeriod(p) {
    period.value = p
    detailOpen.value = false
    if (p === 'week') {
        inputs.value.anyDayInWeek = todayStr()
    } else if (p === 'month') {
        const nowD = new Date()
        inputs.value.year = nowD.getFullYear()
        inputs.value.month = nowD.getMonth() + 1
    } else if (p === 'year') {
        inputs.value.year = new Date().getFullYear()
    }

    updateThumb()

    if (p === 'day') {
        startDailyAutoRefresh()
    } else {
        stopDailyAutoRefresh()
        loadOrCreateDebounced()
        pickTip()
    }
}

/* 라이프사이클 */
onMounted(async () => {
    try {
        const me = await fetch('/api/user/my-patient', { credentials: 'include' })
            .then(r => (r.ok ? r.json() : null))
            .catch(() => null)
        userId.value = me?.userNo ?? me?.id ?? null
    } finally {
        startDailyAutoRefresh()
        document.addEventListener('visibilitychange', onVisibilityChange)
        window.addEventListener('resize', updateThumb)
        await nextTick()
        updateThumb()
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
        if (period.value === 'month' || period.value === 'year')
            loadOrCreateDebounced()
    }
)
watch(
    [report, period, yearResp, insufficient],
    async () => {
        await nextTick()
        if (period.value === 'year') renderYearLine()
        else if (
            (period.value === 'week' || period.value === 'month') &&
            !insufficient.value.flag
        )
            renderRadar()
    },
    { deep: true }
)

onBeforeUnmount(() => {
    stopDailyAutoRefresh()
    document.removeEventListener('visibilitychange', onVisibilityChange)
    window.removeEventListener('resize', updateThumb)
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
.report-page {
    padding-bottom: 24px;
}

/* 이 페이지 카드 공통 스타일 (각 칸을 더 또렷하게) */
.report-page .card {
    border-radius: 16px;
    border: 1px solid #e3e5f1;
    box-shadow: 0 8px 18px rgba(20, 32, 90, 0.06);
}

/* alert 류도 카드처럼 */
.report-page .alert {
    border-radius: 16px;
    border: 1px solid #e3e5f1;
    box-shadow: 0 6px 14px rgba(20, 32, 90, 0.04);
}

/* 상단 토글 */
.period-toggle {
    width: 100%;
}

.toggle-track {
    position: relative;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    border-radius: 999px;
    background-color: #eef0f8;
    padding: 4px;
    overflow: hidden;
}

.toggle-thumb {
    position: absolute;
    top: 4px;
    bottom: 4px;
    border-radius: 999px;
    background-color: #657ae2;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
    transition: left 0.18s ease-out, width 0.18s ease-out;
}

.toggle-btn {
    position: relative;
    z-index: 1;
    border: none;
    background: transparent;
    font-size: 0.82rem;
    padding: 8px 0;
    color: #7c8197;
    border-radius: 999px;
    transition: color 0.18s ease-out, font-weight 0.18s ease-out;
}

.toggle-btn.active {
    color: #ffffff;
    font-weight: 600;
}

/* 라벨 */
.form-label-sm {
    font-size: 0.78rem;
}

/* pill 인풋 공통 */
.pill-input-single {
    border-radius: 999px;
    background-color: #f5f6fb;
    border: none;
    font-size: 0.82rem;
}

.pill-input-single:focus {
    background-color: #ffffff;
    box-shadow: 0 0 0 2px rgba(101, 122, 226, 0.15);
}

/* 연도 버튼 그룹 */
.year-toggle {
    display: flex;
    gap: 6px;
    padding: 4px;
    border-radius: 999px;
    background-color: #f5f6fb;
}

.year-btn {
    flex: 1;
    border-radius: 999px;
    border: none;
    background-color: transparent;
    font-size: 0.8rem;
    color: #6c6f85;
}

.year-btn.active {
    background-color: #657ae2;
    color: #ffffff;
    font-weight: 600;
    box-shadow: 0 4px 10px rgba(101, 122, 226, 0.35);
}

/* 오늘 기분 이모지 */
.today-emoji {
    font-size: 72px;
    line-height: 1;
}

/* 차트 영역 */
.radar-wrap,
.line-wrap {
    position: relative;
    width: 100%;
    height: 280px;
}

/* 점수 숫자 */
.score-display {
    font-size: 2.4rem;
}

/* 페이드 트랜지션 */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

/* 단계형 로더 */
.step-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #d0d4e4;
    margin-top: 6px;
    flex: none;
}

.step-dot.active {
    background: #657ae2;
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

.dots {
    display: inline-block;
    width: 16px;
    text-align: left;
}

/* 로딩 스켈레톤 */
.shimmer {
    height: 8px;
    border-radius: 6px;
    background: linear-gradient(90deg, #f2f4ff 0%, #e9ecff 30%, #f2f4ff 60%);
    background-size: 200% 100%;
    animation: shimmer 1.2s linear infinite;
}

@keyframes shimmer {
    0% {
        background-position: 200% 0;
    }

    100% {
        background-position: -200% 0;
    }
}

/* 오늘의 작은 팁 카드 */
.tip-card {
    margin-bottom: 16px;
    padding: 12px 14px;
    border-radius: 16px;
    background: #f3f5ff;
    border: 1px solid #d9defa;
    box-shadow: 0 6px 14px rgba(20, 32, 90, 0.04);
}

.tip-title {
    font-weight: 700;
    font-size: 0.9rem;
    color: #20243a;
    margin-bottom: 4px;
}

.tip-body {
    font-size: 0.75rem; /* 제목보다 약 2pt 정도 작게 */
    color: #4b4f68;
    line-height: 1.5;
}
</style>

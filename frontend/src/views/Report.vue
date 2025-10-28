<!-- src/views/Report.vue -->
<template>
    <div class="container-sm" style="max-width: 414px;">

        <!-- 기간 선택 -->
        <div class="card border-0 shadow-sm mb-3">
            <div class="card-body">
                <div class="d-flex flex-wrap gap-2 mb-3">
                    <button v-for="p in ['week', 'month', 'year']" :key="p" class="btn btn-sm"
                        :class="period === p ? 'btn-primary' : 'btn-outline-secondary'" @click="setPeriod(p)">
                        {{ periodLabel(p) }}
                    </button>
                </div>

                <div class="row g-2 align-items-end">
                    <!-- WEEK -->
                    <template v-if="period === 'week'">
                        <div class="col-12 mb-2">
                            <label class="form-label small mb-1">주(임의의 하루)</label>
                            <input type="date" class="form-control" v-model="inputs.anyDayInWeek" />
                        </div>
                        <div class="col-12">
                            <div class="week-strip d-flex justify-content-between">
                                <div v-for="d in weekDays" :key="d.iso" class="day-pill"
                                    :class="{ active: true, today: d.isToday }">
                                    <div class="dow">{{ d.label }}</div>
                                    <div class="daynum">{{ d.day }}</div>
                                </div>
                            </div>
                        </div>
                    </template>

                    <!-- MONTH -->
                    <template v-else-if="period === 'month'">
                        <div class="col-6">
                            <label class="form-label small mb-1">연도</label>
                            <input type="number" class="form-control" v-model.number="inputs.year" min="2000"
                                max="2100" />
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
                            <input type="number" class="form-control" v-model.number="inputs.year" min="2000"
                                max="2100" />
                        </div>
                    </template>

                    <div class="col-auto d-flex gap-2">
                        <button class="btn btn-outline-secondary" @click="previewReport">불러오기</button>
                        <button class="btn btn-primary" v-if="period === 'week'" @click="saveReport">저장하기</button>
                    </div>
                </div>

                <div v-if="rangeLabel" class="text-secondary small mt-2">기간: {{ rangeLabel }}</div>
            </div>
        </div>

        <!-- 로딩 / 에러 / 성공 -->
        <div v-if="loading" class="text-center py-5 text-secondary">처리 중…</div>
        <div v-else-if="error" class="alert alert-danger mb-3">{{ error }}</div>
        <div v-else-if="okMsg" class="alert alert-success mb-3">{{ okMsg }}</div>

        <!-- CARE-5 레이더 (주간/월간) -->
        <div class="card border-0 shadow-sm mb-3" v-if="!loading && (period === 'week' || period === 'month')">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div class="fw-semibold">CARE-5 레이더</div>
                    <div class="small text-secondary">0–20점</div>
                </div>
                <template v-if="hasScores && !allZero">
                    <div class="radar-wrap"><canvas ref="radarRef" aria-label="CARE-5 radar chart"></canvas></div>
                </template>
                <template v-else>
                    <div class="text-secondary small py-4 text-center">표시할 점수가 없어요. 기간/데이터를 확인하세요.</div>
                </template>
            </div>
        </div>

        <!-- 연간 라인 차트 (월별 총점 0~100) -->
        <div class="card border-0 shadow-sm mb-3" v-if="!loading && period === 'year'">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div class="fw-semibold">연간 추이 (월별 총점)</div>
                    <div class="small text-secondary">0–100점</div>
                </div>
                <template v-if="yearTotals && yearTotals.length">
                    <div class="line-wrap"><canvas ref="lineRef" aria-label="yearly totals chart"></canvas></div>
                </template>
                <template v-else>
                    <div class="text-secondary small py-4 text-center">연간 데이터가 없어요.</div>
                </template>
            </div>
        </div>

        <!-- 점수 카드 (주/월 전용) -->
        <template v-if="report && (period === 'week' || period === 'month')">
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body d-flex justify-content-between align-items-center">
                    <div>
                        <div class="text-secondary small">
                            {{ (report.range && report.range.label) || '' }} ({{ periodLabel(report.period) }})
                        </div>
                        <div class="fw-semibold fs-5 mt-1">AI 점수</div>
                    </div>
                    <div class="text-end">
                        <div class="display-6 fw-bold">{{ report.score?.cognitive ?? '-' }}</div>
                        <div class="small text-secondary">이전 대비 {{ signed(report.score?.delta ?? 0) }}</div>
                    </div>
                </div>
            </div>
        </template>

        <!-- 항목별 자세히 보기 (주/월/연 공통 — AI 문장 or 규칙 문장) -->
        <div class="card border-0 shadow-sm mb-3" v-if="detailItems.length">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center">
                    <div class="fw-semibold">항목별 자세히 보기</div>
                    <button class="btn btn-sm btn-outline-secondary" @click="detailOpen = !detailOpen">
                        {{ detailOpen ? '닫기' : '열기' }}
                    </button>
                </div>
                <transition name="fade">
                    <div v-if="detailOpen" class="mt-3 small">
                        <ul class="list-unstyled mb-0">
                            <li v-for="item in detailItems" :key="item.key" class="mb-3">
                                <div class="d-flex justify-content-between">
                                    <div class="fw-semibold">{{ item.label }}</div>
                                    <div class="text-secondary">{{ item.value }} / 20</div>
                                </div>
                                <div class="progress my-1" style="height:6px;">
                                    <div class="progress-bar" role="progressbar"
                                        :style="{ width: Math.round((item.value || 0) / 20 * 100) + '%', backgroundColor: team }">
                                    </div>
                                </div>
                                <div class="text-secondary">{{ item.text }}</div>
                            </li>
                        </ul>
                    </div>
                </transition>
            </div>
        </div>

        <!-- 권장 사항 + AI Tip (주/월 전용) -->
        <template v-if="report && (period === 'week' || period === 'month')">
            <div class="card border-0 shadow-sm mb-3" v-if="Array.isArray(report.recommendations)">
                <div class="card-body">
                    <div class="fw-semibold mb-2">권장 사항</div>
                    <ul class="mb-0">
                        <li v-for="(rec, i) in report.recommendations" :key="i">{{ rec }}</li>
                    </ul>
                </div>
            </div>

            <div class="alert alert-primary border-0">
                <div class="fw-semibold mb-1">{{ report.aiTip?.title || '오늘의 추천' }}</div>
                <div>{{ report.aiTip?.body || '산책과 퍼즐 등 가벼운 인지 자극 활동을 권장합니다.' }}</div>
            </div>
        </template>

        <!-- 연간: 항목별 평균(간단 요약) -->
        <template v-if="period === 'year' && yearAverages">
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body">
                    <div class="fw-semibold mb-2">연간 항목 평균(추정)</div>
                    <ul class="mb-0 small">
                        <li v-for="row in yearAverages" :key="row.key">{{ row.label }}: {{ row.value }} / 20</li>
                    </ul>
                </div>
            </div>
        </template>

        <div v-if="!report && !yearTotals?.length" class="text-secondary small text-center py-4">
            기간을 선택하고 “불러오기” 또는 “저장하기”를 눌러주세요.
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import {
    Chart,
    RadarController, RadialLinearScale,
    PointElement, LineElement, Filler, Tooltip, Legend,
    LineController, LinearScale, CategoryScale
} from 'chart.js'

Chart.register(
    RadarController, RadialLinearScale,
    PointElement, LineElement, Filler, Tooltip, Legend,
    LineController, LinearScale, CategoryScale
)

const team = '#657AE2'

/* ---------------- state ---------------- */
const userId = ref(null)
const period = ref('week') // 'week' | 'month' | 'year'
const inputs = ref({
    anyDayInWeek: todayStr(),
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1
})
const report = ref(null)     // 주/월 응답
const yearResp = ref(null)   // 연간 응답 { series[], totals[] }
const loading = ref(false)
const error = ref('')
const okMsg = ref('')
const detailOpen = ref(false)

/* ---------------- refs for charts ---------------- */
const radarRef = ref(null)
let radarChart = null
const lineRef = ref(null)
let lineChart = null

/* ---------------- helpers ---------------- */
function periodLabel(p) {
    const k = p === 'weekly' ? 'week' : p === 'monthly' ? 'month' : p === 'yearly' ? 'year' : p
    return k === 'week' ? '주간' : k === 'month' ? '월간' : '연간'
}
function pad2(n) { return String(n).padStart(2, '0') }
function todayStr() { return new Date().toISOString().slice(0, 10) }
function fmt(d) { return d.toISOString().slice(0, 10) }
function signed(n) { return (n >= 0 ? '+' : '') + n }

/* 기간 라벨 */
const rangeLabel = computed(() => {
    if (period.value === 'year') {
        const y = inputs.value.year
        return `${y}-01-01 ~ ${y}-12-31 (${y}년)`
    }
    const r = report.value && report.value.range
    if (!r || !r.start || !r.end) return ''
    return `${r.start} ~ ${r.end} (${r.label})`
})

/* 주간 스트립 */
function weekdayLabel(i) { return ['월', '화', '수', '목', '금', '토', '일'][i] }
const weekDays = computed(() => {
    const anyIso = inputs.value.anyDayInWeek || todayStr()
    const base = new Date(anyIso + 'T00:00:00')
    const day = base.getDay() || 7
    const mon = new Date(base); mon.setDate(base.getDate() - (day - 1))
    const todayIso = todayStr()
    const out = []
    for (let i = 0; i < 7; i++) {
        const d = new Date(mon); d.setDate(mon.getDate() + i)
        const iso = d.toISOString().slice(0, 10)
        out.push({ iso, label: weekdayLabel(i), day: d.getDate(), isToday: iso === todayIso })
    }
    return out
})

/* 기간 계산 */
function weekRange(anyDayIso) {
    const d = new Date(anyDayIso + 'T00:00:00')
    const day = d.getDay() || 7
    const mon = new Date(d); mon.setDate(d.getDate() - (day - 1))
    const sun = new Date(mon); sun.setDate(mon.getDate() + 6)
    return { start: fmt(mon), end: fmt(sun) }
}
function monthRange(year, month) {
    const s = new Date(year, month - 1, 1)
    const e = new Date(year, month, 0)
    return { start: fmt(s), end: fmt(e) }
}
function yearRange(year) {
    const s = new Date(year, 0, 1)
    const e = new Date(year, 11, 31)
    return { start: fmt(s), end: fmt(e) }
}

/* ISO 주차 */
function isoWeekInfo(anyDayIso) {
    const d = new Date(anyDayIso + 'T00:00:00')
    const th = new Date(d); th.setDate(d.getDate() + 3 - ((d.getDay() + 6) % 7))
    const isoYear = th.getFullYear()
    const jan4 = new Date(isoYear, 0, 4)
    const week1Mon = new Date(jan4); week1Mon.setDate(jan4.getDate() - ((jan4.getDay() + 6) % 7))
    const week = 1 + Math.floor((d - week1Mon) / 604800000)
    const r = weekRange(anyDayIso)
    return { year: isoYear, week, start: r.start, end: r.end }
}

/* 내부 → API */
function toApiPeriod(pt) { return pt === 'year' ? 'yearly' : pt === 'month' ? 'monthly' : 'weekly' }

/* API 파라미터 */
function buildParams() {
    if (period.value === 'week') {
        const wk = isoWeekInfo(inputs.value.anyDayInWeek)
        return { periodType: 'week', periodKey: `${wk.year}-W${pad2(wk.week)}`, start: wk.start, end: wk.end }
    } else if (period.value === 'month') {
        const y = inputs.value.year, m = inputs.value.month
        const r = monthRange(y, m)
        return { periodType: 'month', periodKey: `${y}-${pad2(m)}`, start: r.start, end: r.end }
    } else {
        const y = inputs.value.year
        const r = yearRange(y)
        return { periodType: 'year', periodKey: String(y), start: r.start, end: r.end }
    }
}

/* metrics 파싱 */
function clamp20(v) { const n = Number(v); if (!Number.isFinite(n)) return 0; return Math.max(0, Math.min(20, n)) }
function getMetrics(rep) {
    let m = rep && rep.metrics
    if (!m) return null
    if (typeof m === 'string') { try { m = JSON.parse(m) } catch { return null } }
    return m
}
function toScoreArrayFromReport(rep) {
    const m = getMetrics(rep); const s = m && m.scores; if (!s) return null
    return [clamp20(s.memory_short), clamp20(s.memory_long), clamp20(s.orientation), clamp20(s.adl), clamp20(s.behavior_safety)]
}
const currentArray = computed(() => (period.value === 'year' ? null : toScoreArrayFromReport(report.value)))
const hasScores = computed(() => Array.isArray(currentArray.value) && currentArray.value.length === 5)
const allZero = computed(() => hasScores.value && currentArray.value.every(n => n === 0))

/* 항목별 자세히보기: 규칙/AI 문장 */
const detailItems = computed(() => {
    // 주/월: report 기반, 연: yearResp의 series 평균 기반으로 텍스트 생성
    if (period.value === 'year') {
        if (!yearAverages.value) return []
        return yearAverages.value.map(row => ({
            key: row.key, label: row.label, value: row.value, text: explain(row.key, row.value)
        }))
    }
    const m = getMetrics(report.value); const s = m && m.scores; if (!s) return []
    const map = [
        { key: 'memory_short', label: '단기·작업기억' },
        { key: 'memory_long', label: '장기기억' },
        { key: 'orientation', label: '지남력' },
        { key: 'adl', label: '일상기능' },
        { key: 'behavior_safety', label: '행동·기분·안전' },
    ]
    return map.map(it => {
        const val = clamp20(s[it.key])
        return { ...it, value: val, text: explain(it.key, val) }
    })
})

function explain(key, val) {
    const band = val >= 16 ? 'good' : val >= 8 ? 'mid' : 'low'
    const textMap = {
        memory_short: {
            good: '최근 대화/일정 기억이 비교적 안정적이에요. 짧은 작업기억 활동을 꾸준히 유지해요.',
            mid: '단기 기억에 기복이 있어요. 메모/알림과 짧은 반복 회상을 활용해요.',
            low: '최근 기억 누락이 잦을 수 있어요. 한 번에 한 가지 요청으로 간단히 안내하세요.'
        },
        memory_long: {
            good: '오래된 일화/인물 회상이 좋아요. 사진/음악 등 장기기억 자극을 이어가요.',
            mid: '장기 기억은 보통이에요. 사진·향·음악처럼 익숙한 단서를 활용해보세요.',
            low: '장기 회상에 어려움이 있어요. 긍정적 과거 경험을 짧게 상기시키며 안정감을 도와주세요.'
        },
        orientation: {
            good: '시간·장소 인지가 좋아요. 달력·시계 확인 루틴을 유지하세요.',
            mid: '요일·시간 혼동이 가끔 있어요. 큰 글씨 시계/안내판이 도움돼요.',
            low: '시간·장소 혼란이 커질 수 있어요. 동선 단순화와 안내 표지로 안전을 확보하세요.'
        },
        adl: {
            good: '식사·세면·복약 등 일상 기능이 안정적이에요. 가벼운 코칭만!',
            mid: '일부 동작에 도움이 필요해요. 순서 카드로 “함께 하기”를 시도해요.',
            low: '일상 기능 보조가 필요해요. 복약/세면 체크리스트, 위험 작업 동반이 좋아요.'
        },
        behavior_safety: {
            good: '낙상·야간 배회 신호가 낮아요. 환경을 유지하며 주기적으로 점검하세요.',
            mid: '불안/야간 각성 신호가 있어요. 수면 위생·안전등·미끄럼 방지 매트를 점검하세요.',
            low: '안전 리스크가 큽니다. 밤중 동선 차단, 문 경보, 보호자 호출 수단을 우선 정비하세요.'
        }
    }
    return textMap[key]?.[band] || ''
}

/* 차트 렌더링 */
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
        data: { labels, datasets: [{ label: '월별 총점(0–100)', data: totals, fill: false, borderColor: team, pointBackgroundColor: team, pointRadius: 3, borderWidth: 2 }] },
        options: {
            responsive: true, maintainAspectRatio: false,
            scales: { x: { type: 'category' }, y: { type: 'linear', min: 0, max: 100, ticks: { stepSize: 20 } } },
            plugins: { legend: { display: false }, tooltip: { enabled: true } },
            elements: { line: { tension: 0.2 } }
        }
    })
}

/* API 호출 */
async function previewReport() {
    error.value = ''; okMsg.value = ''
    if (!userId.value) { error.value = '환자 연결 정보가 없습니다.'; return }
    const p = buildParams()
    try {
        loading.value = true
        if (period.value === 'week') {
            const qs = new URLSearchParams({ userId: String(userId.value), period: toApiPeriod(p.periodType), start: p.start, end: p.end })
            const res = await fetch(`/api/ai/report?${qs.toString()}`, { credentials: 'include' })
            if (!res.ok) throw new Error(`응답 오류(${res.status})`)
            report.value = await res.json(); yearResp.value = null
        } else if (period.value === 'month') {
            const y = inputs.value.year, m = inputs.value.month
            const qs = new URLSearchParams({ userId: String(userId.value), year: String(y), month: String(m) })
            const res = await fetch(`/api/ai/report/monthly-from-week?${qs.toString()}`, { credentials: 'include' })
            if (!res.ok) throw new Error(`응답 오류(${res.status})`)
            report.value = await res.json(); yearResp.value = null
        } else {
            const y = inputs.value.year
            const qs = new URLSearchParams({ userId: String(userId.value), year: String(y) })
            const res = await fetch(`/api/ai/report/yearly-from-week?${qs.toString()}`, { credentials: 'include' })
            if (!res.ok) throw new Error(`응답 오류(${res.status})`)
            yearResp.value = await res.json(); report.value = null
        }
    } catch (e) {
        console.error(e); error.value = '[불러오기 실패] ' + (e?.message || e)
    } finally {
        loading.value = false
        await nextTick()
        if (period.value === 'year') { renderYearLine() } else { renderRadar() }
    }
}

async function saveReport() {
    error.value = ''; okMsg.value = ''
    if (period.value !== 'week') { okMsg.value = '월/연간 리포트는 저장 개념 없이 집계로 표시됩니다.'; return }
    if (!userId.value) { error.value = '환자 연결 정보가 없습니다.'; return }
    const p = buildParams()
    const qs = new URLSearchParams({ userId: String(userId.value), period: toApiPeriod(p.periodType), start: p.start, end: p.end, persist: 'true' })
    try {
        loading.value = true
        let res = await fetch(`/api/ai/report?${qs.toString()}`, { credentials: 'include' })
        if (!res.ok) { const qs2 = new URLSearchParams(qs); qs2.set('save', '1'); res = await fetch(`/api/ai/report?${qs2.toString()}`, { credentials: 'include' }) }
        if (!res.ok) { const text = await res.text().catch(() => ''); throw new Error(`응답 오류(${res.status}) ${text}`) }
        report.value = await res.json().catch(() => null)
        okMsg.value = '리포트 저장 완료'
    } catch (e) {
        console.error(e); error.value = '[저장 실패] ' + (e?.message || e)
    } finally {
        loading.value = false
        await nextTick(); renderRadar()
    }
}

/* 도우미 */
function setPeriod(p) {
    period.value = p
    detailOpen.value = false
    if (p === 'week') {
        inputs.value.anyDayInWeek = todayStr()
    } else if (p === 'month') {
        const now = new Date()
        inputs.value.year = now.getFullYear()
        inputs.value.month = now.getMonth() + 1
    } else {
        inputs.value.year = new Date().getFullYear()
    }
}

/* 연간 totals/평균 계산 */
const yearTotals = computed(() => Array.isArray(yearResp.value?.totals) ? yearResp.value.totals : [])
const yearAverages = computed(() => {
    if (!yearResp.value?.series?.length) return null
    const months = yearResp.value.series
    const avg = (arr) => Math.round(arr.reduce((a, b) => a + (b || 0), 0) / Math.max(1, arr.length))
    const ms = avg(months.map(m => m.metrics?.scores?.memory_short))
    const ml = avg(months.map(m => m.metrics?.scores?.memory_long))
    const or = avg(months.map(m => m.metrics?.scores?.orientation))
    const ad = avg(months.map(m => m.metrics?.scores?.adl))
    const be = avg(months.map(m => m.metrics?.scores?.behavior_safety))
    return [
        { key: 'memory_short', label: '단기·작업기억', value: ms },
        { key: 'memory_long', label: '장기기억', value: ml },
        { key: 'orientation', label: '지남력', value: or },
        { key: 'adl', label: '일상기능', value: ad },
        { key: 'behavior_safety', label: '행동·기분·안전', value: be },
    ]
})

onMounted(async () => {
    try {
        const me = await fetch('/api/user/my-patient', { credentials: 'include' })
            .then(r => r.ok ? r.json() : null).catch(() => null)
        userId.value = me?.userNo ?? me?.id ?? me ?? null
    } finally {
        await nextTick()
        if (period.value === 'year') { renderYearLine() } else { renderRadar() }
    }
})

watch([report, period, yearResp], async () => {
    await nextTick()
    if (period.value === 'year') { renderYearLine() } else { renderRadar() }
}, { deep: true })

onBeforeUnmount(() => {
    if (radarChart) { radarChart.destroy(); radarChart = null }
    if (lineChart) { lineChart.destroy(); lineChart = null }
})
</script>

<style scoped>
.container-sm {
    padding-bottom: 24px;
}

/* 레이더/라인 영역 */
.radar-wrap,
.line-wrap {
    position: relative;
    width: 100%;
    height: 280px;
}

/* 주간 스트립 */
.week-strip {
    gap: 6px;
}

.day-pill {
    flex: 1;
    border-radius: 10px;
    padding: 8px 0;
    text-align: center;
    background: #657AE233;
    border: 1px solid #657AE255;
    user-select: none;
}

.day-pill.active {
    background: #657AE2;
    color: #fff;
    border-color: #657AE2;
}

.day-pill.today {
    outline: 2px solid #ffffffaa;
    outline-offset: -2px;
}

.day-pill .dow {
    font-size: 12px;
    opacity: .9;
}

.day-pill .daynum {
    font-size: 14px;
    font-weight: 700;
    line-height: 1;
}

/* 트랜지션 */
.fade-enter-active,
.fade-leave-active {
    transition: opacity .15s ease
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0
}
</style>

<!-- src/views/Report.vue -->
<template>
    <div class="container-sm" style="max-width:414px;">
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
                            <div class="small text-secondary">주간 범위</div>
                            <div class="fs-5 fw-semibold">{{ weekRangeLabelText }}</div>
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

                    <!-- 액션 -->
                    <div class="col-auto d-flex gap-2">
                        <button class="btn btn-primary" @click="loadOrCreate">AI건강체크</button>
                    </div>
                </div>

                <div v-if="rangeLabel" class="text-secondary small mt-2">기간: {{ rangeLabel }}</div>
            </div>
        </div>

        <!-- 로딩 / 에러 / 성공 -->
        <div v-if="loading" class="text-center py-5 text-secondary">처리 중…</div>
        <div v-else-if="error" class="alert alert-danger mb-3">{{ error }}</div>
        <div v-else-if="okMsg" class="alert alert-success mb-3">{{ okMsg }}</div>

        <!-- CARE-5 레이더 (주/월) -->
        <div class="card border-0 shadow-sm mb-3" v-if="!loading && (period === 'week' || period === 'month')">
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
        <div class="card border-0 shadow-sm mb-3" v-if="!loading && period === 'year'">
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

        <!-- 점수 카드 -->
        <template v-if="report && (period === 'week' || period === 'month')">
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body d-flex justify-content-between align-items-center">
                    <div>
                        <div class="text-secondary small">
                            {{ report.range?.label || '' }} ({{ periodLabel(report.periodType || period) }})
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

        <!-- 항목별 자세히 보기 -->
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
                                <div class="text-secondary" v-if="item.text">{{ item.text }}</div>
                            </li>
                        </ul>
                    </div>
                </transition>
            </div>
        </div>

        <!-- 랜덤 한 줄 팁 -->
        <div class="alert alert-primary border-0" v-if="tipText">
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
const period = ref('week')
const inputs = ref({
    anyDayInWeek: todayStr(),
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1
})
const report = ref(null)
const yearResp = ref(null)           // { totals?: number[12], series?: [...], details?: [...] }
const loading = ref(false)
const error = ref('')
const okMsg = ref('')
const detailOpen = ref(false)

/* 간단 팁(5개 중 랜덤) */
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

/* 차트 refs */
const radarRef = ref(null); let radarChart = null
const lineRef = ref(null); let lineChart = null

/* ---------- 헬퍼 ---------- */
function periodLabel(p) { return p === 'week' ? '주간' : p === 'month' ? '월간' : '연간' }
function signed(n) { return (n >= 0 ? '+' : '') + n }
function pad2(n) { return String(n).padStart(2, '0') }
function todayStr() { return fmtLocal(new Date()) }

/* 로컬 날짜 포맷(UTC 변환 금지) */
function fmtLocal(d) {
    const y = d.getFullYear(), m = pad2(d.getMonth() + 1), day = pad2(d.getDate())
    return `${y}-${m}-${day}`
}
function addDays(d, n) { const x = new Date(d.getFullYear(), d.getMonth(), d.getDate()); x.setDate(x.getDate() + n); return x }

/* 기간 계산: end는 다음 기간 시작(미포함) */
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

/* metrics/sections 파싱 */
function clamp20(v) { const n = Number(v); if (!Number.isFinite(n)) return 0; return Math.max(0, Math.min(20, n)) }
function getMetrics(rep) {
    let m = rep?.metrics
    if (!m) return null
    if (typeof m === 'string') { try { m = JSON.parse(m) } catch { return null } }
    return m
}
function getSections(rep) {
    let s = rep?.sections
    if (!s) return null
    if (typeof s === 'string') { try { s = JSON.parse(s) } catch { return null } }
    return s
}
function toScoreArrayFromReport(rep) {
    const s = getMetrics(rep)?.scores; if (!s) return null
    return [clamp20(s.memory_short), clamp20(s.memory_long), clamp20(s.orientation), clamp20(s.adl), clamp20(s.behavior_safety)]
}
const currentArray = computed(() => (period.value === 'year' ? null : toScoreArrayFromReport(report.value)))
const hasScores = computed(() => Array.isArray(currentArray.value) && currentArray.value.length === 5)
const allZero = computed(() => hasScores.value && currentArray.value.every(n => n === 0))

/* 항목별 자세히 보기
   - 연간: 서버 yearResp.details가 있으면 그걸 우선 사용, 없으면 yearAverages + 규칙문구
   - 주/월: report.sections.details(있으면) 우선, 없으면 점수만 */
const detailItems = computed(() => {
    if (period.value === 'year') {
        const det = yearResp.value?.details
        if (Array.isArray(det) && det.length) {
            // 서버에서 이미 AI 코멘트 제공
            return det.map(d => ({
                key: d.key, label: d.label, value: Number(d.value ?? 0), text: (d.text ?? '').trim()
            }))
        }
        if (!yearAverages.value) return []
        // fallback: 평균 + 규칙문구
        return yearAverages.value.map(row => ({
            key: row.key, label: row.label, value: row.value, text: explain(row.key, row.value)
        }))
    }

    // 주/월
    const details = getSections(report.value)?.details
    if (Array.isArray(details) && details.length) {
        return details.map(d => ({
            key: d.key, label: d.label, value: Number(d.value ?? 0), text: (d.text ?? '').trim()
        }))
    }
    // fallback: 점수만
    const s = getMetrics(report.value)?.scores; if (!s) return []
    const map = [
        { key: 'memory_short', label: '단기·작업기억' },
        { key: 'memory_long', label: '장기기억' },
        { key: 'orientation', label: '지남력' },
        { key: 'adl', label: '일상기능' },
        { key: 'behavior_safety', label: '행동·기분·안전' },
    ]
    return map.map(it => ({ ...it, value: clamp20(s[it.key]), text: '' }))
})

/* 규칙 문구 (연간 fallback용) */
function explain(key, val) {
    const band = val >= 16 ? 'good' : val >= 8 ? 'mid' : 'low'
    const text = {
        memory_short: { good: '최근 대화/일정 기억이 비교적 안정적이에요. 짧은 작업기억 활동을 꾸준히 유지해요.', mid: '단기 기억에 기복이 있어요. 메모/알림과 짧은 반복 회상을 활용해요.', low: '최근 기억 누락이 잦을 수 있어요. 한 번에 한 가지 요청으로 간단히 안내하세요.' },
        memory_long: { good: '오래된 일화/인물 회상이 좋아요. 사진/음악 등 장기기억 자극을 이어가요.', mid: '장기 기억은 보통이에요. 사진·향·음악처럼 익숙한 단서를 활용해보세요.', low: '장기 회상에 어려움이 있어요. 긍정적 과거 경험을 짧게 상기시키며 안정감을 도와주세요.' },
        orientation: { good: '시간·장소 인지가 좋아요. 달력·시계 확인 루틴을 유지하세요.', mid: '요일·시간 혼동이 가끔 있어요. 큰 글씨 시계/안내판이 도움돼요.', low: '시간·장소 혼란이 커질 수 있어요. 동선 단순화와 안내 표지로 안전을 확보하세요.' },
        adl: { good: '식사·세면·복약 등 일상 기능이 안정적이에요. 가벼운 코칭만!', mid: '일부 동작에 도움이 필요해요. 순서 카드로 “함께 하기”를 시도해요.', low: '일상 기능 보조가 필요해요. 복약/세면 체크리스트, 위험 작업 동반이 좋아요.' },
        behavior_safety: { good: '낙상·야간 배회 신호가 낮아요. 환경을 유지하며 주기적으로 점검하세요.', mid: '불안/야간 각성 신호가 있어요. 수면 위생·안전등·미끄럼 방지 매트를 점검하세요.', low: '안전 리스크가 큽니다. 밤중 동선 차단, 문 경보, 보호자 호출 수단을 우선 정비하세요.' }
    }
    return text[key]?.[band] || ''
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

/* ---------- API: 한 엔드포인트 ---------- */
function computeRange() {
    if (period.value === 'week') { const r = weekRange(inputs.value.anyDayInWeek); return { start: r.start, end: r.end, apiPeriod: 'weekly' } }
    if (period.value === 'month') { const r = monthRange(inputs.value.year, inputs.value.month); return { start: r.start, end: r.end, apiPeriod: 'monthly' } }
    const r = yearRange(inputs.value.year); return { start: r.start, end: r.end, apiPeriod: 'yearly' }
}

async function loadOrCreate() {
    error.value = ''; okMsg.value = ''
    if (!userId.value) { error.value = '환자 연결 정보가 없습니다.'; return }
    const { start, end, apiPeriod } = computeRange()
    try {
        loading.value = true
        const { data } = await axios.get('/api/ai/report', {
            params: { userId: userId.value, period: apiPeriod, start, end, mode: 'loadOrCreate' }
        })
        if (period.value === 'year' && (Array.isArray(data?.totals) || Array.isArray(data?.series) || Array.isArray(data?.details))) {
            yearResp.value = data; report.value = null
        } else {
            report.value = data; yearResp.value = null
        }
        pickTip()
    } catch (e) {
        console.error(e); error.value = `응답 오류(${e?.response?.status ?? '???'})`
    } finally {
        loading.value = false
        await nextTick()
        if (period.value === 'year') renderYearLine(); else renderRadar()
    }
}

/* 연간 보조 계산 (fallback 용) */
const yearTotals = computed(() => Array.isArray(yearResp.value?.totals) ? yearResp.value.totals : [])
const yearAverages = computed(() => {
    if (!yearResp.value?.series?.length) return null
    const months = yearResp.value.series
    const avg = arr => Math.round(arr.reduce((a, b) => a + (b || 0), 0) / Math.max(1, arr.length))
    return [
        { key: 'memory_short', label: '단기·작업기억', value: avg(months.map(m => m.metrics?.scores?.memory_short)) },
        { key: 'memory_long', label: '장기기억', value: avg(months.map(m => m.metrics?.scores?.memory_long)) },
        { key: 'orientation', label: '지남력', value: avg(months.map(m => m.metrics?.scores?.orientation)) },
        { key: 'adl', label: '일상기능', value: avg(months.map(m => m.metrics?.scores?.adl)) },
        { key: 'behavior_safety', label: '행동·기분·안전', value: avg(months.map(m => m.metrics?.scores?.behavior_safety)) },
    ]
})

/* 초기화 */
function setPeriod(p) {
    period.value = p; detailOpen.value = false
    if (p === 'week') {
        inputs.value.anyDayInWeek = todayStr()
    } else if (p === 'month') {
        const now = new Date(); inputs.value.year = now.getFullYear(); inputs.value.month = now.getMonth() + 1
    } else {
        inputs.value.year = new Date().getFullYear()
    }
    pickTip()
}
onMounted(async () => {
    try {
        const me = await fetch('/api/user/my-patient', { credentials: 'include' }).then(r => r.ok ? r.json() : null).catch(() => null)
        userId.value = me?.userNo ?? me?.id ?? me ?? null
    } finally {
        pickTip()
        await nextTick()
        if (period.value === 'year') renderYearLine(); else renderRadar()
    }
})
watch([report, period, yearResp], async () => {
    await nextTick()
    if (period.value === 'year') renderYearLine(); else renderRadar()
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

.radar-wrap,
.line-wrap {
    position: relative;
    width: 100%;
    height: 280px;
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity .15s ease
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0
}
</style>

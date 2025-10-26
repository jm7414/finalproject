<!-- src/views/Report.vue -->
<template>
    <div class="container-sm" style="max-width: 414px;">
        <!-- 헤더 -->
        <header class="d-flex align-items-center py-2 mb-2 border-bottom">
            <button class="btn btn-link px-0 me-2" @click="goBack" aria-label="back">←</button>
            <div class="flex-grow-1 text-center fw-semibold">AI 리포트</div>
            <div style="width:24px;"></div>
        </header>

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

                        <!-- 일주일 스트립 (월~일) -->
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
                        <button class="btn btn-primary" @click="saveReport">저장하기</button>
                    </div>
                </div>

                <div v-if="rangeLabel" class="text-secondary small mt-2">기간: {{ rangeLabel }}</div>
            </div>
        </div>

        <!-- 로딩 / 에러 / 성공 -->
        <div v-if="loading" class="text-center py-5 text-secondary">처리 중…</div>
        <div v-else-if="error" class="alert alert-danger mb-3">{{ error }}</div>
        <div v-else-if="okMsg" class="alert alert-success mb-3">{{ okMsg }}</div>

        <template v-else-if="report">
            <!-- 점수 카드 -->
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body d-flex justify-content-between align-items-center">
                    <div>
                        <div class="text-secondary small">
                            {{ (report.range && report.range.label) || '' }} ({{ periodLabel(report.period) }})
                        </div>
                        <div class="fw-semibold fs-5 mt-1">인지 점수</div>
                    </div>
                    <div class="text-end">
                        <div class="display-6 fw-bold">{{ report.score?.cognitive ?? '-' }}</div>
                        <div class="small text-secondary">전기 대비 {{ signed(report.score?.delta ?? 0) }}</div>
                    </div>
                </div>
            </div>

            <!-- 메모리 게이지 -->
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body">
                    <div class="mb-3">
                        <div class="d-flex justify-content-between">
                            <span class="fw-semibold">단기 기억</span>
                            <span class="small text-secondary">{{ report.memory?.short?.label || '' }}</span>
                        </div>
                        <div class="progress" style="height:6px;">
                            <div class="progress-bar" role="progressbar"
                                :style="{ width: pct(report.memory?.short?.gauge) + '%', backgroundColor: team }"></div>
                        </div>
                    </div>
                    <div>
                        <div class="d-flex justify-content-between">
                            <span class="fw-semibold">장기 기억</span>
                            <span class="small text-secondary">{{ report.memory?.long?.label || '' }}</span>
                        </div>
                        <div class="progress" style="height:6px;">
                            <div class="progress-bar" role="progressbar"
                                :style="{ width: pct(report.memory?.long?.gauge) + '%', backgroundColor: team }"></div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 활동 요약 -->
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body">
                    <div class="fw-semibold mb-2">활동 요약</div>
                    <div class="d-flex flex-wrap gap-2">
                        <span class="badge rounded-pill text-bg-light px-3 py-2">
                            식사: {{ report.activities?.meal?.label || '' }}
                        </span>
                        <span class="badge rounded-pill text-bg-light px-3 py-2">
                            수면: {{ report.activities?.sleep?.label || '' }}
                        </span>
                        <span class="badge rounded-pill text-bg-light px-3 py-2">
                            운동: {{ report.activities?.exercise?.label || '' }}
                        </span>
                    </div>
                </div>
            </div>

            <!-- AI 요약 -->
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body">
                    <div class="fw-semibold mb-1">행동 변화</div>
                    <p class="mb-3">{{ report.behaviorChanges }}</p>
                    <div class="fw-semibold mb-1">종합 요약</div>
                    <p class="mb-0">{{ report.weeklySummary }}</p>
                </div>
            </div>

            <!-- 권장 사항 -->
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body">
                    <div class="fw-semibold mb-2">권장 사항</div>
                    <ul class="mb-0">
                        <li v-for="(rec, i) in report.recommendations" :key="i">{{ rec }}</li>
                    </ul>
                </div>
            </div>

            <!-- AI Tip -->
            <div class="alert alert-primary border-0">
                <div class="fw-semibold mb-1">{{ report.aiTip?.title || '오늘의 추천' }}</div>
                <div>{{ report.aiTip?.body || '산책과 퍼즐 등 가벼운 인지 자극 활동을 권장합니다.' }}</div>
            </div>
        </template>

        <div v-else class="text-secondary small text-center py-4">
            기간을 선택하고 “불러오기” 또는 “저장하기”를 눌러주세요.
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const team = '#657AE2'

// 상태
const userId = ref(null)
const period = ref('week') // 내부 표준: 'week' | 'month' | 'year'
const inputs = ref({
    anyDayInWeek: todayStr(),
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1
})
const report = ref(null)
const loading = ref(false)
const error = ref('')
const okMsg = ref('')

// 유틸
function goBack() { router.back() }
function periodLabel(p) { return p === 'week' ? '주간' : p === 'month' ? '월간' : '연간' }
function pad2(n) { return String(n).padStart(2, '0') }
function todayStr() { return new Date().toISOString().slice(0, 10) }
function fmt(d) { return d.toISOString().slice(0, 10) }
function pct(v) { return Math.round(Math.max(0, Math.min(1, v || 0)) * 100) }
function signed(n) { return (n >= 0 ? '+' : '') + n }

function setPeriod(p) {
    period.value = p
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

// 기간 라벨
const rangeLabel = computed(() => {
    const r = report.value && report.value.range
    if (!r || !r.start || !r.end) return ''
    return `${r.start} ~ ${r.end} (${r.label})`
})

// 한국어 요일 라벨
function weekdayLabel(idx) {
    const labels = ['월', '화', '수', '목', '금', '토', '일']
    return labels[idx]
}

// anyDay 기준 주간 보기 스트립
const weekDays = computed(() => {
    const anyIso = inputs.value.anyDayInWeek || todayStr()
    const base = new Date(anyIso + 'T00:00:00')
    const day = base.getDay() || 7
    const mon = new Date(base); mon.setDate(base.getDate() - (day - 1))

    const out = []
    const todayIso = todayStr()
    for (let i = 0; i < 7; i++) {
        const d = new Date(mon); d.setDate(mon.getDate() + i)
        const iso = d.toISOString().slice(0, 10)
        out.push({ iso, label: weekdayLabel(i), day: d.getDate(), isToday: iso === todayIso })
    }
    return out
})

// 기간 계산
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

// ISO 주차/주년 계산 (키 생성을 위해)
function isoWeekInfo(anyDayIso) {
    const d = new Date(anyDayIso + 'T00:00:00')
    // ISO: 목요일이 속한 해가 그 주의 연도
    const th = new Date(d); th.setDate(d.getDate() + 3 - ((d.getDay() + 6) % 7))
    const isoYear = th.getFullYear()
    const jan4 = new Date(isoYear, 0, 4)
    const week1Mon = new Date(jan4); week1Mon.setDate(jan4.getDate() - ((jan4.getDay() + 6) % 7))
    const week = 1 + Math.floor((d - week1Mon) / 604800000)
    const r = weekRange(anyDayIso)
    return { year: isoYear, week, start: r.start, end: r.end }
}

// 내부 표준('year'|'month'|'week') → API가 기대하는 값('yearly'|'monthly'|'weekly')
function toApiPeriod(pt) {
    return pt === 'year' ? 'yearly' : pt === 'month' ? 'monthly' : 'weekly'
}

// 현재 UI 값 → API 파라미터 빌드
function buildParams() {
    if (period.value === 'week') {
        const wk = isoWeekInfo(inputs.value.anyDayInWeek)
        return {
            periodType: 'week',
            periodKey: `${wk.year}-W${pad2(wk.week)}`,
            start: wk.start,
            end: wk.end
        }
    } else if (period.value === 'month') {
        const y = inputs.value.year
        const m = inputs.value.month
        const mm = pad2(m)
        const r = monthRange(y, m)
        return {
            periodType: 'month',
            periodKey: `${y}-${mm}`,
            start: r.start,
            end: r.end
        }
    } else {
        const y = inputs.value.year
        const r = yearRange(y)
        return {
            periodType: 'year',
            periodKey: `${y}`,
            start: r.start,
            end: r.end
        }
    }
}

// 미리보기(GET)
async function previewReport() {
    error.value = ''; okMsg.value = ''; report.value = null
    if (!userId.value) { error.value = '환자 연결 정보가 없습니다.'; return }
    const p = buildParams()
    const qs = new URLSearchParams({
        userId: String(userId.value),
        period: toApiPeriod(p.periodType), // yearly/monthly/weekly
        start: p.start,
        end: p.end
    })
    try {
        loading.value = true
        const res = await fetch(`/api/ai/report?${qs.toString()}`, { credentials: 'include' })
        if (!res.ok) throw new Error(`응답 오류(${res.status})`)
        report.value = await res.json()
    } catch (e) {
        console.error(e)
        error.value = '[불러오기 실패] ' + (e?.message || e)
    } finally {
        loading.value = false
    }
}

// 저장(GET + persist=true)
async function saveReport() {
    error.value = ''; okMsg.value = ''; report.value = null
    if (!userId.value) { error.value = '환자 연결 정보가 없습니다.'; return }

    const p = buildParams()
    const qs = new URLSearchParams({
        userId: String(userId.value),
        period: toApiPeriod(p.periodType),  // yearly/monthly/weekly
        start: p.start,
        end: p.end,
        persist: 'true'
    })

    try {
        loading.value = true

        // 1차: persist=true
        let res = await fetch(`/api/ai/report?${qs.toString()}`, {
            method: 'GET',
            credentials: 'include'
        })

        // 2차 폴백: 일부 서버는 save=1만 인식
        if (!res.ok) {
            const qs2 = new URLSearchParams(qs); qs2.set('save', '1')
            res = await fetch(`/api/ai/report?${qs2.toString()}`, {
                method: 'GET',
                credentials: 'include'
            })
        }

        if (!res.ok) {
            const text = await res.text().catch(() => '')
            throw new Error(`응답 오류(${res.status}) ${text}`)
        }

        const data = await res.json().catch(() => null)
        report.value = data
        okMsg.value = '리포트 저장 완료'
    } catch (e) {
        console.error(e)
        error.value = '[저장 실패] ' + (e?.message || e)
    } finally {
        loading.value = false
    }
}

// 내 환자 userId 가져오기
onMounted(async () => {
    try {
        const me = await fetch('/api/user/my-patient', { credentials: 'include' })
            .then(r => (r.ok ? r.json() : null))
            .catch(() => null)
        userId.value = me?.userNo ?? me?.id ?? me ?? null
    } catch {
        userId.value = null
    }
})
</script>

<style scoped>
.container-sm {
    padding-bottom: 24px;
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
    opacity: 0.9;
}

.day-pill .daynum {
    font-size: 14px;
    font-weight: 700;
    line-height: 1;
}
</style>

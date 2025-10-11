<template>
    <div class="container">
        <div class="title">
            <h2>{{ pickedDate }}</h2>
            <h2>이 주의 기록</h2>
            <!-- 날짜 선택 -->
            <div class="date-picker">
                <label for="pickDate">날짜 선택:</label>
                <input id="pickDate" type="date" v-model="pickedDate" @change="updateWeekRange" />
            </div>

            <h3>{{ weekRange }}</h3>
        </div>
    </div>

    <div class="content-container">
        <div class="report-card report-change">
            <h2>
                <ChangeMarker /> 변화
            </h2>
            <h4>
                이번 주에 약을 3번 잊었어요. 다음 주에는 약 복용 알림을 설정해보세요.
            </h4>
        </div>
        <div class="report-card report-analysis">
            <h2>
                <AnalysisMarker /> 분석
            </h2>
            <h4>
                기분 점수는 평균 3.8점으로, 지난주(4.2점)에 비해 소폭 하락했습니다. 이러한 변화는 스트레스나 수면 부족과 연관될 수 있으니,
                다음 주에는 수분 섭취를 늘리고 휴식 시간을 충분히 확보해 보시길 권장드립니다.
            </h4>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ChangeMarker from '@/components/ChangeMarker.vue'
import AnalysisMarker from '@/components/AnalysisMarker.vue'

// 날짜 포맷 예시
// 오늘 날짜
const now = new Date()
const yyyy = now.getFullYear()
const mm = String(now.getMonth() + 1).padStart(2, '0')
const dd = String(now.getDate()).padStart(2, '0')
const currentDate = ref(`${yyyy}-${mm}-${dd}`)

// 오늘 날짜를 받아서 저장해놓음 -> 나중 시간의 보고서 보는 것을 막기위해 해놓음
const today = ref('')
onMounted(() => {
    today.value = currentDate.value;
})

// 사용자가 선택한 날짜 (YYYY-MM-DD) -> 이 부분이 날짜를 선택하면 맨 위에 title도 바뀌고 달력 밑에 나오는 날짜도 바뀜
const pickedDate = ref(currentDate.value)

// 주차 범위 표시
const weekRange = ref('')

// 선택된 날짜에 따라 주차 범위 계산
function updateWeekRange() {

    // 만약 미래의 보고서를 보는 것은 금지
    if (new Date(today.value) < new Date(pickedDate.value) ) { // 이게 Date형식끼리만 비교가 가능한거같음
        alert(`오늘 날짜 이전만 확인 가능합니다.`)
        pickedDate.value = today.value;
        return
    }

    const [y, m, d] = pickedDate.value.split('-').map(Number)
    const date = new Date(y, m - 1, d)

    // 요일 인덱스 (0=일요일, 1=월요일…6=토요일)
    const dayOfWeek = date.getDay()
    // 한국 기준 주간(월~일): 월요일을 주시작(1)으로 보면,
    // 월요일 = 1이니까 → 차이 = dayOfWeek - 1
    const diffToMon = dayOfWeek === 0 ? -6 : 1 - dayOfWeek

    const mon = new Date(date)
    mon.setDate(date.getDate() + diffToMon)

    const sun = new Date(mon)
    sun.setDate(mon.getDate() + 6)

    const format = dt => {
        const Y = dt.getFullYear()
        const M = String(dt.getMonth() + 1).padStart(2, '0')
        const D = String(dt.getDate()).padStart(2, '0')
        return `${Y}.${M}.${D}`
    }

    weekRange.value = `${format(mon)} ~ ${format(sun)}`
}

// 초기 실행
updateWeekRange()
</script>

<style scoped>
.container {
    max-width: 600px;
    margin: 0 auto;
    padding: 20px;
}

.title h2 {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin: 0;
}

.title h3 {
    font-size: 16px;
    color: #666;
    margin-top: 4px;
}

.date-picker {
    margin: 16px 0;
    display: flex;
    align-items: center;
    gap: 8px;
}

.date-picker input {
    padding: 4px 8px;
    font-size: 14px;
}

.content-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-top: 20px;
}

.report-card {
    background: #f0e8ff;
    border: 1px solid #d6b8ff;
    border-radius: 12px;
    padding: 16px;
}

.report-card h2 {
    display: flex;
    align-items: center;
    font-size: 18px;
    font-weight: 600;
    color: #4a4a4a;
    gap: 8px;
    margin-bottom: 12px;
}

.report-card h4 {
    font-size: 14px;
    color: #555;
    line-height: 1.6;
    margin: 0;
}

/* 변화 카드 강조 컬러 (선택) */
.report-change {
    background: #f5f0ff;
    border-color: #cdb8ff;
}

/* 분석 카드 강조 컬러 (선택) */
.report-analysis {
    background: #f5f0ff;
    border-color: #cdb8ff;
}


/**달력 글자 변경 */
.date-picker input[type="date"] {
    padding: 6px 10px;
    font-size: 16px;
    border: 1px solid #ccc;
    border-radius: 6px;
    background-color: #fff;
}
</style>

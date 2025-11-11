<!-- src/views/QuizHangul.vue -->
<template>
  <div class="container-sm py-3" style="max-width:414px">
    <!-- 환자 헤더 -->
    <header class="app-header">
      <div class="icon-wrapper" @click="goBack">
        <i class="icon bi bi-arrow-left icon-bold"></i>
      </div>

      <div class="app-title">
        <img src="/mammamialogo.png" alt="Mamma Mia Logo" class="logo-image">
      </div>

      <div class="icon-wrapper" @click="goHome">
        <i class="icon bi bi-house icon-bold"></i>
      </div>
    </header>

    <!-- 퀴즈 시작 전 화면 -->
    <div v-if="!quizStarted && !showResult" class="text-center mt-5">
      <h3 class="mb-4">오늘의 두뇌 훈련</h3>
      <p class="text-muted mb-4">5개의 문제로 구성된 간단한 퀴즈입니다</p>
      <button class="btn btn-custom btn-lg px-5 py-3" @click="startQuiz">시작하기</button>

      <!-- 최근 기록 표시 -->
      <div v-if="recentScores.length > 0" class="mt-5">
        <h5 class="mb-3">최근 기록</h5>
        <div class="list-group">
          <div v-for="(record, index) in recentScores" :key="index"
            class="list-group-item d-flex justify-content-between align-items-center">
            <span>{{ record.date }}</span>
            <span class="badge bg-custom rounded-pill">{{ record.score }}/5</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 퀴즈 진행 화면 -->
    <div v-if="quizStarted && !showResult">
      <!-- 퀴즈 제목 -->
      <div class="text-center fw-semibold mb-3 fs-5">오늘의 두뇌 훈련</div>

      <!-- 진행/점수 -->
      <div class="d-flex justify-content-between small mb-2">
        <div class="text-body-secondary">
          문제 <span class="text-body fw-semibold">{{ currentQuestion + 1 }}/5</span>
        </div>
        <div class="text-body-secondary">
          맞춘 문제: <span class="text-body fw-semibold">{{ score }}</span>
        </div>
      </div>
      <div class="progress mb-3" style="height:8px">
        <div class="progress-bar bg-custom" role="progressbar"
          :style="{ width: ((currentQuestion + 1) / 5 * 100) + '%' }"></div>
      </div>

      <!-- 문제 카드 -->
      <div class="card border-0 shadow-sm mt-3 mb-3">
        <div class="card-body">
          <div class="d-flex justify-content-center mb-3">
            <span class="badge text-bg-light border text-body-secondary fs-6">
              문제 {{ currentQuestion + 1 }}
            </span>
          </div>

          <div class="fs-4 fw-semibold mb-4 text-center" style="line-height: 1.6;">
            {{ questions[currentQuestion].question }}
          </div>

          <!-- 이미지나 힌트가 있는 경우 -->
          <div v-if="questions[currentQuestion].hint" class="bg-light rounded-3 text-center py-4 mb-4">
            <div class="display-4 fw-semibold">
              {{ questions[currentQuestion].hint }}
            </div>
          </div>

          <!-- 보기 -->
          <div class="row g-3">
            <div v-for="(option, index) in questions[currentQuestion].options" :key="index" class="col-6">
              <button type="button" class="btn w-100 rounded-3 py-3 fs-5" :class="getButtonClass(index)"
                @click="selectAnswer(index)" :disabled="answered">
                {{ option }}
              </button>
            </div>
          </div>

          <!-- 정답/오답 피드백 -->
          <div v-if="answered" class="mt-3 text-center">
            <div v-if="isCorrect" class="alert alert-success py-2">
              <i class="bi bi-check-circle-fill me-2"></i>정답입니다!
            </div>
            <div v-else class="alert alert-danger py-2">
              <i class="bi bi-x-circle-fill me-2"></i>
              정답은 {{ questions[currentQuestion].options[questions[currentQuestion].correct] }} 입니다
            </div>
          </div>
        </div>
      </div>

      <!-- 하단 버튼 -->
      <div class="d-flex gap-2">
        <button type="button" class="btn btn-light w-50 py-3 rounded-3" @click="skipQuestion" :disabled="answered">
          건너뛰기
        </button>
        <button type="button" class="btn btn-custom w-50 py-3 rounded-3" @click="nextQuestion" :disabled="!answered">
          {{ currentQuestion < 4 ? '다음 문제' : '결과 보기' }} </button>
      </div>
    </div>

    <!-- 결과 화면 -->
    <div v-if="showResult" class="text-center mt-4">
      <h3 class="mb-4">퀴즈 완료!</h3>

      <div class="card border-0 shadow-sm mb-4">
        <div class="card-body py-5">
          <div class="display-1 fw-bold text-custom mb-3">{{ score }}/5</div>
          <p class="fs-5 mb-0">{{ getResultMessage() }}</p>
        </div>
      </div>

      <!-- 이전 기록과 비교 -->
      <div v-if="previousScore !== null" class="card border-0 shadow-sm mb-4">
        <div class="card-body">
          <h5 class="mb-3">이전 기록과 비교</h5>
          <div class="d-flex justify-content-around align-items-center py-3">
            <div>
              <div class="text-muted small">이전 점수</div>
              <div class="fs-4 fw-semibold">{{ previousScore }}/5</div>
            </div>
            <div>
              <i :class="scoreChangeIcon" class="fs-1"></i>
            </div>
            <div>
              <div class="text-muted small">오늘 점수</div>
              <div class="fs-4 fw-semibold">{{ score }}/5</div>
            </div>
          </div>
          <div class="mt-2">
            <span v-if="scoreDifference > 0" class="text-success fw-semibold">
              <i class="bi bi-arrow-up-circle-fill me-1"></i>
              {{ scoreDifference }}개 더 맞추셨습니다!
            </span>
            <span v-else-if="scoreDifference < 0" class="text-warning fw-semibold">
              <i class="bi bi-arrow-down-circle-fill me-1"></i>
              이전보다 {{ Math.abs(scoreDifference) }}개 적게 맞추셨습니다
            </span>
            <span v-else class="text-muted fw-semibold">
              <i class="bi bi-dash-circle-fill me-1"></i>
              이전과 동일한 점수입니다
            </span>
          </div>
        </div>
      </div>

      <button class="btn btn-custom btn-lg px-5 py-3" @click="resetQuiz">
        다시 시작하기
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const quizSets = [
  [
    { question: '현재 계절은 무엇인가요?', options: ['봄', '여름', '가을', '겨울'], correct: getCurrentSeason(), hint: '' },
    { question: '다음 중 동물이 아닌 것은?', options: ['강아지', '고양이', '사과', '토끼'], correct: 2, hint: '' },
    { question: '10 + 5는 얼마인가요?', options: ['13', '15', '17', '20'], correct: 1, hint: '10 + 5 = ?' },
    { question: '시계에서 짧은 바늘이 3을 가리키고 긴 바늘이 12를 가리키면 몇 시인가요?', options: ['2시', '3시', '12시', '15시'], correct: 1, hint: '' },
    { question: '다음 중 과일이 아닌 것은?', options: ['사과', '당근', '포도', '바나나'], correct: 1, hint: '' }
  ],
  [
    { question: '한 주는 몇 일인가요?', options: ['5일', '6일', '7일', '10일'], correct: 2, hint: '' },
    { question: '다음 중 식물이 아닌 것은?', options: ['장미', '소나무', '토끼', '사과나무'], correct: 2, hint: '' },
    { question: '20 - 6은 얼마인가요?', options: ['14', '12', '15', '16'], correct: 0, hint: '20 - 6 = ?' },
    { question: "설날에 먹는 대표적인 음식은?", options: ['떡국', '송편', '김밥', '라면'], correct: 0, hint: '' },
    { question: '오늘은 무슨 요일?', options: ['월요일', '화요일', '수요일', '모름'], correct: getTodayDayIndex(), hint: '' }
  ],
  [
    { question: '다음 중 겨울 옷은?', options: ['반팔티', '목도리', '모자', '운동화'], correct: 1, hint: '' },
    { question: '사과, 배, 포도는 모두 무엇인가요?', options: ['채소', '과일', '음료', '견과'], correct: 1, hint: '' },
    { question: '5 + 9는 얼마인가요?', options: ['13', '14', '12', '15'], correct: 1, hint: '5 + 9 = ?' },
    { question: '가장 늦은 시간은?', options: ['오전 9시', '오후 3시', '오후 6시', '오전 7시'], correct: 2, hint: '' },
    { question: '다음 중 탈것이 아닌 것은?', options: ['버스', '자동차', '자전거', '우산'], correct: 3, hint: '' }
  ],
  [
    { question: '일 년은 몇 개월인가요?', options: ['10개월', '12개월', '14개월', '16개월'], correct: 1, hint: '' },
    { question: '다음 중 새가 아닌 것은?', options: ['참새', '비둘기', '나비', '까치'], correct: 2, hint: '' },
    { question: '8 + 7은 얼마인가요?', options: ['14', '15', '16', '17'], correct: 1, hint: '8 + 7 = ?' },
    { question: '밥을 먹을 때 사용하는 것은?', options: ['연필', '숟가락', '가위', '망치'], correct: 1, hint: '' },
    { question: '다음 중 여름 과일은?', options: ['귤', '수박', '사과', '배'], correct: 1, hint: '' }
  ],
  [
    { question: '하루는 몇 시간인가요?', options: ['12시간', '24시간', '36시간', '48시간'], correct: 1, hint: '' },
    { question: '다음 중 물고기가 아닌 것은?', options: ['고등어', '오징어', '개구리', '참치'], correct: 2, hint: '' },
    { question: '15 - 8은 얼마인가요?', options: ['6', '7', '8', '9'], correct: 1, hint: '15 - 8 = ?' },
    { question: '비가 올 때 사용하는 것은?', options: ['선풍기', '우산', '모자', '장갑'], correct: 1, hint: '' },
    { question: '다음 중 봄꽃은?', options: ['국화', '벚꽃', '해바라기', '동백'], correct: 1, hint: '' }
  ]
]

function getCurrentSeason() {
  const month = new Date().getMonth() + 1
  if (month >= 3 && month <= 5) return 0
  if (month >= 6 && month <= 8) return 1
  if (month >= 9 && month <= 11) return 2
  return 3
}
function getTodayDayIndex() {
  const day = new Date().getDay() // 일:0, 월:1, ..., 토:6
  // '월,화,수,모름'에서 day === 1(월),2(화),3(수)만 사용할 경우
  // 나머지(일,목,금,토)는 '모름'(index 3)으로 처리
  if ([1, 2, 3].includes(day)) return day - 1
  return 3
}

// 상태 변수
const quizStarted = ref(false)
const showResult = ref(false)
const currentQuestion = ref(0)
const score = ref(0)
const answered = ref(false)
const selectedAnswer = ref(null)
const isCorrect = ref(false)
const previousScore = ref(null)
const recentScores = ref([])

const scoreDifference = computed(() => {
  if (previousScore.value === null) return 0
  return score.value - previousScore.value
})

const scoreChangeIcon = computed(() => {
  if (scoreDifference.value > 0) return 'bi bi-arrow-up-circle-fill text-success'
  if (scoreDifference.value < 0) return 'bi bi-arrow-down-circle-fill text-warning'
  return 'bi bi-dash-circle text-muted'
})

onMounted(() => { loadScores() })

function loadScores() {
  const stored = localStorage.getItem('brainTrainingScores')
  if (stored) {
    const allScores = JSON.parse(stored)
    recentScores.value = allScores.slice(-5).reverse()
    const today = new Date().toLocaleDateString('ko-KR')
    const todayScore = allScores.find(s => s.date === today)
    if (todayScore) previousScore.value = todayScore.score
    else if (allScores.length > 0) previousScore.value = allScores[allScores.length - 1].score
  }
}
function saveScore() {
  const stored = localStorage.getItem('brainTrainingScores')
  let allScores = stored ? JSON.parse(stored) : []
  const today = new Date().toLocaleDateString('ko-KR')
  const newScore = { date: today, score: score.value, timestamp: new Date().toISOString() }
  const todayIndex = allScores.findIndex(s => s.date === today)
  if (todayIndex >= 0) allScores[todayIndex] = newScore
  else allScores.push(newScore)
  localStorage.setItem('brainTrainingScores', JSON.stringify(allScores))
  loadScores()
}

const questions = ref([]) // 문제 세트를 startQuiz에서 할당

function startQuiz() {
  const idx = Math.floor(Math.random() * quizSets.length)
  questions.value = quizSets[idx].map(q => ({ ...q })) // 깊은 복사
  quizStarted.value = true
  currentQuestion.value = 0
  score.value = 0
  answered.value = false
  selectedAnswer.value = null
}

// 답변 선택
function selectAnswer(index) {
  if (answered.value) return

  selectedAnswer.value = index
  answered.value = true
  isCorrect.value = (index === questions.value[currentQuestion.value].correct)

  if (isCorrect.value) {
    score.value++
  }
}

// 문제 건너뛰기
function skipQuestion() {
  answered.value = true
  selectedAnswer.value = -1
  isCorrect.value = false
}

// 다음 문제로 이동
function nextQuestion() {
  if (currentQuestion.value < 4) {
    currentQuestion.value++
    answered.value = false
    selectedAnswer.value = null
    isCorrect.value = false
  } else {
    // 퀴즈 종료
    showResult.value = true
    quizStarted.value = false
    saveScore()
  }
}

// 버튼 스타일 결정
function getButtonClass(index) {
  if (!answered.value) {
    return 'btn-outline-secondary'
  }

  if (index === questions.value[currentQuestion.value].correct) {
    return 'btn-success'
  }

  if (index === selectedAnswer.value && !isCorrect.value) {
    return 'btn-danger'
  }

  return 'btn-outline-secondary'
}

// 결과 메시지
function getResultMessage() {
  if (score.value === 5) return '완벽합니다! 모든 문제를 맞추셨어요! 🎉'
  if (score.value >= 4) return '훌륭합니다! 아주 잘하셨어요! 👏'
  if (score.value >= 3) return '좋습니다! 계속 연습하면 더 좋아질 거예요! 😊'
  if (score.value >= 2) return '괜찮습니다! 다음엔 더 잘할 수 있어요! 💪'
  return '다시 한번 도전해보세요! 💪'
}

// 퀴즈 재시작
function resetQuiz() {
  showResult.value = false
  quizStarted.value = false
  currentQuestion.value = 0
  score.value = 0
  answered.value = false
  selectedAnswer.value = null
}

// 뒤로가기
function goBack() {
  if (quizStarted.value) {
    if (confirm('퀴즈를 종료하시겠습니까?')) {
      resetQuiz()
    }
  } else {
    router.go(-1)
  }
}

// 홈으로
function goHome() {
  if (quizStarted.value) {
    if (confirm('퀴즈를 종료하고 홈으로 가시겠습니까?')) {
      resetQuiz()
      router.push('/DP')
    }
  } else {
    router.push('/DP')
  }
}
</script>

<style scoped>
/* 환자 헤더 */
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 70px;
  padding: 0 24px;
  background-color: #FFFFFF;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.icon-wrapper {
  cursor: pointer;
}

.icon-wrapper .icon {
  width: 24px;
  height: 24px;
  fill: #000000;
}

.icon-bold {
  font-size: 1.3rem;
  -webkit-text-stroke: 0.8px currentColor;
}

.logo-image {
  height: 32px;
  object-fit: contain;
}

/* 포인트 컬러 (rgba(74, 98, 221, 1)) 적용 */
.btn-custom {
  background-color: rgba(74, 98, 221, 1);
  border-color: rgba(74, 98, 221, 1);
  color: white;
}

.btn-custom:hover {
  background-color: rgba(60, 80, 200, 1);
  border-color: rgba(60, 80, 200, 1);
  color: white;
}

.btn-custom:active,
.btn-custom:focus {
  background-color: rgba(60, 80, 200, 1);
  border-color: rgba(60, 80, 200, 1);
  color: white;
  box-shadow: 0 0 0 0.25rem rgba(74, 98, 221, 0.25);
}

.btn-custom:disabled {
  background-color: rgba(74, 98, 221, 0.6);
  border-color: rgba(74, 98, 221, 0.6);
  opacity: 0.6;
}

.bg-custom {
  background-color: rgba(74, 98, 221, 1) !important;
}

.text-custom {
  color: rgba(74, 98, 221, 1);
}

.badge.bg-custom {
  background-color: rgba(74, 98, 221, 1) !important;
}

.progress-bar.bg-custom {
  background-color: rgba(74, 98, 221, 1);
}

.btn {
  transition: all 0.3s ease;
}

.btn:disabled {
  opacity: 0.6;
}

.card {
  transition: transform 0.3s ease;
}

.progress-bar {
  transition: width 0.3s ease;
}
</style>

<!-- src/views/Loan.vue -->
<template>
  <div class="loan-page">
    <!-- 헤더 카드 -->
    <section class="header-card">
      <div class="header-text">
        <h1 class="title">노인 대상 대출 지원</h1>
        <p class="subtitle">
          공공데이터포털 복지서비스를 기반으로<br />
          노인·어르신을 위한 대출·이자지원 정보를 모았어요.
        </p>
        <div class="chip-row">
          <span class="chip">노인·어르신</span>
          <span class="chip chip-secondary">대출·융자</span>
          <span class="chip chip-outline">공공데이터 API</span>
        </div>
      </div>
    </section>

    <!-- 요약 영역 -->
    <section class="summary-section">
      <div class="summary-card">
        <div class="summary-left">
          <div class="summary-label">노인 대상 대출 복지</div>
          <div class="summary-count">
            {{ elderLoanList.length }}건
          </div>
          <div class="summary-desc">
            전국 지자체 복지 중, 노인·어르신에게 도움이 될 수 있는
            대출·이자지원 제도만 모았어요.
          </div>
        </div>
        <div class="summary-right">
          <button class="refresh-btn" @click="reload">
            🔄 새로고침
          </button>
        </div>
      </div>

      <p v-if="loading" class="info-text">복지 대출 정보를 불러오는 중입니다...</p>
      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div
        v-if="!loading && !errorMessage && elderLoanList.length === 0"
        class="fallback-box"
      >
        <p class="fallback-main">
          노인·어르신을 대상으로 한 대출형 복지서비스를 찾지 못했어요.
        </p>
        <p class="fallback-sub">
          대신 전체 대출·융자·이자지원 복지서비스
          ({{ loanList.length }}건)를 아래에 보여드릴게요.
        </p>
      </div>
    </section>

    <!-- 노인 대상 대출 리스트 -->
    <section v-if="elderLoanList.length > 0" class="list-section">
      <h2 class="section-title">노인·어르신 대상 대출 제도</h2>

      <article
        v-for="item in elderLoanList"
        :key="item.servId || item.servNm"
        class="loan-card"
      >
        <header class="card-header">
          <div class="pill-row">
            <span class="pill pill-elder">노인 대상</span>
            <span class="pill pill-loan">대출·이자지원</span>
          </div>
          <h3 class="loan-name">{{ item.servNm }}</h3>
          <p class="gov-text">
            {{ resolveGovName(item) }}
          </p>
        </header>

        <div class="card-body">
          <div class="info-row">
            <span class="label">지원 내용</span>
            <span class="value">
              {{ item.servDgst || '대출·융자·이자지원 관련 복지서비스입니다.' }}
            </span>
          </div>

          <div class="info-row">
            <span class="label">지원 대상</span>
            <span class="value">
              {{ item.trgterIndvdlNm || '상세 대상 조건은 지자체 안내문을 참고해주세요.' }}
            </span>
          </div>

          <div class="info-row">
            <span class="label">문의전화</span>
            <span class="value">
              {{ resolveTel(item) }}
            </span>
          </div>
        </div>

        <footer class="card-footer">
          <button class="detail-btn" @click="openLoanDetail(item)">
            지원 상세보기 →
          </button>
        </footer>
      </article>
    </section>

    <!-- 노인 조건이 없을 경우: 전체 대출 리스트 -->
    <section
      v-if="!loading && !errorMessage && elderLoanList.length === 0 && loanList.length > 0"
      class="list-section"
    >
      <h2 class="section-title">전체 대출·이자지원 복지서비스</h2>

      <article
        v-for="item in loanList"
        :key="item.servId || item.servNm"
        class="loan-card"
      >
        <header class="card-header">
          <div class="pill-row">
            <span class="pill pill-loan">대출·융자</span>
          </div>
          <h3 class="loan-name">{{ item.servNm }}</h3>
          <p class="gov-text">
            {{ resolveGovName(item) }}
          </p>
        </header>

        <div class="card-body">
          <div class="info-row">
            <span class="label">지원 내용</span>
            <span class="value">
              {{ item.servDgst || '대출·융자·이자지원 관련 복지서비스입니다.' }}
            </span>
          </div>

          <div class="info-row">
            <span class="label">지원 대상</span>
            <span class="value">
              {{ item.trgterIndvdlNm || '상세 대상 조건은 지자체 안내문을 참고해주세요.' }}
            </span>
          </div>

          <div class="info-row">
            <span class="label">문의전화</span>
            <span class="value">
              {{ resolveTel(item) }}
            </span>
          </div>
        </div>

        <footer class="card-footer">
          <button class="detail-btn" @click="openLoanDetail(item)">
            지원 상세보기 →
          </button>
        </footer>
      </article>
    </section>

    <!-- 대출 이용 팁 -->
    <section class="tip-section">
      <h2 class="section-title">대출 이용 시 꼭 확인하세요</h2>
      <div class="tip-card">
        <p class="tip-main">💡 상환 능력을 먼저 체크하기</p>
        <p class="tip-text">
          보호자와 가족이 함께 상환 계획을 세우고,
          여러 제도를 비교한 뒤 가장 부담이 적은 지원을 선택하는 것이 좋아요.
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const errorMessage = ref('')

// 전체 대출형 복지서비스
const loanList = ref([])
// 그 중에서 "노인/어르신/고령" 대상인 것만
const elderLoanList = ref([])

// 🔹 대출 관련 키워드
const loanKeywords = ['대출', '융자', '이자지원', '이자 지원', '보증', '전세자금', '주택구입']
// 🔹 노인/어르신 관련 키워드
const elderKeywords = ['노인', '어르신', '고령', '65세', '노령', '장기요양', '기초연금']

// 대출형 서비스인지 판별
const isLoanService = (item) => {
  const name = item.servNm || ''
  const summary = item.servDgst || ''
  const text = `${name} ${summary}`
  return loanKeywords.some((k) => text.includes(k))
}

// 노인/어르신 대상인지 판별
const isElderService = (item) => {
  const name = item.servNm || ''
  const summary = item.servDgst || ''
  const target = item.trgterIndvdlNm || ''
  const text = `${name} ${summary} ${target}`
  return elderKeywords.some((k) => text.includes(k))
}

// 지자체 / 부서 이름 정리
const resolveGovName = (item) => {
  return (
    item.selfGovNm || // 지자체(서울특별시, 부산광역시 등)
    item.jurMnofNm || // 소관부처
    item.bizChrDeptNm || // 담당부서
    '지자체 복지서비스'
  )
}

// 문의전화 정리 (Benefit.vue와 필드 맞춤)
const resolveTel = (item) => {
  return (
    item.cnsgnInsttOfcTelNo || // 수탁기관 전화번호
    item.jurMnofTelNo ||       // 소관부처 전화번호
    item.jurMnofContcNo ||     // 소관부처 연락처
    item.telNo ||              // 기타 tel 필드
    '지자체 홈페이지 또는 콜센터로 문의해주세요.'
  )
}

// 공공데이터 → 복지서비스 목록 로드
const loadLoanServices = async () => {
  loading.value = true
  errorMessage.value = ''
  elderLoanList.value = []
  loanList.value = []

  try {
    // 지역 제한 없이 전국 단위에서 검색
    const res = await axios.get('/api/support/welfare', {
      params: {
        pageNo: 1,
        numOfRows: 1000
      }
    })

    let data = res.data

    // 문자열로 내려오는 경우 처리
    if (typeof data === 'string') {
      try {
        data = JSON.parse(data)
      } catch (e) {
        console.error('[Loan.vue] 복지서비스 JSON 파싱 실패(문자열):', e, data)
        errorMessage.value = '복지서비스 데이터 형식이 올바르지 않습니다.'
        return
      }
    }

    // 백엔드에서 에러 래핑해 보낸 경우
    if (data && data.success === false) {
      console.warn('[Loan.vue] 복지서비스 응답 에러:', data)
      errorMessage.value = data.message || '복지 대출 정보를 불러오지 못했습니다.'
      return
    }

    const list = Array.isArray(data.servList) ? data.servList : []

    // 1차: 대출 관련 필터
    const loanCandidates = list.filter(isLoanService)

    // 2차: 노인/어르신 대상 필터
    const elderCandidates = loanCandidates.filter(isElderService)

    // 너무 많으면 각각 상위 N개만
    loanList.value = loanCandidates.slice(0, 50)
    elderLoanList.value = elderCandidates.slice(0, 50)

    console.log(
      '[Loan.vue] 전체:', list.length,
      '대출 후보:', loanCandidates.length,
      '노인 대출:', elderCandidates.length
    )
  } catch (err) {
    console.error('복지 대출 정보 조회 실패:', err)
    errorMessage.value = '복지 대출 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

// 새로고침
const reload = () => {
  loadLoanServices()
}

// 상세보기 → 공식 상세 URL이 없어서 검색 링크로 처리
const openLoanDetail = (item) => {
  const name = item.servNm || ''
  const gov = resolveGovName(item)
  const q = encodeURIComponent(`${gov} ${name} 대출 복지`)
  window.open(`https://search.naver.com/search.naver?query=${q}`, '_blank')
}

onMounted(() => {
  loadLoanServices()
})
</script>

<style scoped>
.loan-page {
  max-width: 500px;
  margin: 0 auto;
  padding: 16px;
  background: #f7f8fa;
  box-sizing: border-box;
}

/* 헤더 카드 */
.header-card {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  border-radius: 16px;
  padding: 18px 16px;
  color: #ffffff;
  margin-bottom: 16px;
}

.title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 4px 0;
}

.subtitle {
  font-size: 13px;
  margin: 0 0 12px 0;
  line-height: 1.4;
}

.chip-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.chip {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.chip-secondary {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.5);
}

.chip-outline {
  background: transparent;
  border-style: dashed;
}

/* 요약 카드 */
.summary-section {
  margin-bottom: 12px;
}

.summary-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 14px 12px;
  box-shadow: 0 4px 6px rgba(15, 23, 42, 0.06);
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: stretch;
}

.summary-left {
  flex: 1;
}

.summary-label {
  font-size: 13px;
  color: #4b5563;
}

.summary-count {
  font-size: 24px;
  font-weight: 700;
  margin-top: 2px;
  color: #111827;
}

.summary-desc {
  margin-top: 4px;
  font-size: 11px;
  color: #6b7280;
}

.summary-right {
  display: flex;
  align-items: flex-start;
}

.refresh-btn {
  border: none;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  background: #eef2ff;
  color: #4f46e5;
  cursor: pointer;
}

/* 텍스트 / 알림 */
.info-text {
  margin-top: 8px;
  font-size: 12px;
  color: #4b5563;
}

.error-text {
  margin-top: 8px;
  font-size: 12px;
  color: #b91c1c;
}

.fallback-box {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fef9c3;
  border: 1px solid #facc15;
}

.fallback-main {
  font-size: 13px;
  font-weight: 600;
  color: #854d0e;
  margin: 0 0 4px 0;
}

.fallback-sub {
  font-size: 12px;
  color: #92400e;
  margin: 0;
}

/* 리스트 섹션 */
.list-section {
  margin-top: 16px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #111827;
}

/* 대출 카드 */
.loan-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 12px 12px 10px;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.08);
  margin-bottom: 10px;
}

.card-header {
  margin-bottom: 8px;
}

.pill-row {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.pill {
  font-size: 10px;
  padding: 3px 6px;
  border-radius: 999px;
  background: #e5e7eb;
  color: #374151;
}

.pill-elder {
  background: #fef3c7;
  color: #92400e;
}

.pill-loan {
  background: #dbeafe;
  color: #1d4ed8;
}

.loan-name {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 2px 0;
  color: #111827;
}

.gov-text {
  font-size: 11px;
  color: #6b7280;
  margin: 0;
}

/* 카드 바디 */
.card-body {
  border-top: 1px solid #e5e7eb;
  padding-top: 8px;
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-row {
  display: flex;
  gap: 6px;
  align-items: flex-start;
}

.label {
  font-size: 11px;
  color: #9ca3af;
  min-width: 60px;
}

.value {
  font-size: 11px;
  color: #374151;
  flex: 1;
}

/* 카드 푸터 */
.card-footer {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.detail-btn {
  border: none;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  background: #eff6ff;
  color: #1d4ed8;
  cursor: pointer;
}

/* 팁 섹션 */
.tip-section {
  margin-bottom: 24px;
}

.tip-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 12px 12px;
  border: 1px solid #e5e7eb;
}

.tip-main {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: #111827;
}

.tip-text {
  font-size: 12px;
  color: #4b5563;
  margin: 0;
}

/* 반응형 */
@media (max-width: 600px) {
  .loan-page {
    padding: 12px;
  }
}
</style>

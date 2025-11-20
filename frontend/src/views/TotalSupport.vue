<template>
  <div class="dashboard">
    <!-- 상단 환영 카드 -->
    <div class="top-card">
      <div class="profile-section">
        <ProfileMarker class="profile-icon" />
        <div class="greeting-text">
          <div class="greeting-title">{{ userName }} 님 안녕하세요!</div>
          <div class="greeting-subtitle">필요한 지원을 찾아보세요</div>
        </div>
      </div>
    </div>

    <!-- 요약 버튼 그룹 -->
    <div class="summary-buttons">
      <!-- 치매 / 노인 관련 지원금 -->
      <div class="summary-btn receive-money">
        <div class="summary-left">
          <div class="summary-label">받을 수 있는 지원금</div>
          <h1 class="summary-count">
            {{ benefitSeoulCount }}건
          </h1>
          <div class="summary-sub">
            서울 노인·치매 {{ benefitSeoulCount }}건 / 전국 {{ benefitTotalCount }}건
          </div>
        </div>
        <div class="summary-right">
          <button class="summary-action" @click="goBenefit">
            확인하기 →
          </button>
        </div>
      </div>

      <!-- 가능한 대출 (노인 대상 + 전체 대출 표시) -->
      <div class="summary-btn receive-loan">
        <div class="summary-left">
          <div class="summary-label">가능한 대출</div>
          <h1 class="summary-count">
            {{ elderLoanCount }}건
          </h1>
          <div class="summary-sub">
            노인 대상 {{ elderLoanCount }}건 / 전체 {{ loanTotalCount }}건
          </div>
        </div>
        <div class="summary-right">
          <button class="summary-action" @click="goLoan">
            확인하기 →
          </button>
        </div>
      </div>
    </div>

    <!-- 기능 카드 그리드 -->
    <div class="function-grid">
      <div class="func-card">
        <MoneyMarker class="func-icon" />
        <div class="func-title">지원금</div>
        <div class="func-desc">정부 및 지자체 지원금 안내</div>
        <button class="func-link" @click="goBenefit">확인하기 →</button>
      </div>
      <div class="func-card">
        <BohumMarker class="func-icon" />
        <div class="func-title">보험금</div>
        <div class="func-desc">의료보험 및 생활보험 혜택</div>
        <button class="func-link" @click="goLoan">확인하기 →</button>
      </div>
      <div class="func-card">
        <SangDamMarker class="func-icon" />
        <div class="func-title">상담소</div>
        <div class="func-desc">전문가 상담 및 심리 지원</div>
        <button class="func-link" @click="goHeartCare">상담받기 →</button>
      </div>
      <div class="func-card">
        <HospitalMarker class="func-icon" />
        <div class="func-title">병원위치</div>
        <div class="func-desc">근처 병원 및 의료시설 찾기</div>
        <button class="func-link" @click="goHospitalCare">찾아보기 →</button>
      </div>
    </div>

    <!-- (선택) 에러 표시 -->
    <div v-if="errorMessage" class="error-box">
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

import MoneyMarker from '@/components/MoneyMarker.vue'
import BohumMarker from '@/components/BohumMarker.vue'
import SangDamMarker from '@/components/SangDamMarker.vue'
import HospitalMarker from '@/components/HospitalMarker.vue'
import ProfileMarker from '@/components/ProfileMarker.vue'

const router = useRouter()

// ✅ 사용자 정보
const userData = ref(null)
const userName = computed(() => {
  return userData.value?.name || 'User'
})

// ✅ 종합지원 관련 상태
const benefitSeoulCount = ref(0)
const benefitTotalCount = ref(0)
const elderLoanCount = ref(0)
const loanTotalCount = ref(0)
const errorMessage = ref('')

const localGovNm = ref('서울특별시 구로구')

// ✅ 사용자 정보 로드
const loadUserData = async () => {
  try {
    const response = await axios.get('/api/user/me')
    userData.value = response.data
  } catch (error) {
    console.error('사용자 정보 로드 실패:', error)
    if (error.response?.status === 401) {
      alert('로그인이 필요합니다.')
      router.push('/login')
    }
  }
}

const loadWelfareSummary = async () => {
  try {
    errorMessage.value = ''

    const res = await axios.get('/api/support/welfare', {
      params: {
        pageNo: 1,
        numOfRows: 1000
      }
    })

    const apiResult = res.data

    if (!apiResult || apiResult.upstreamStatus !== 200 || !apiResult.xml) {
      console.warn('복지서비스 응답 이상:', apiResult)
      benefitSeoulCount.value = 0
      benefitTotalCount.value = 0
      elderLoanCount.value = 0
      loanTotalCount.value = 0
      errorMessage.value = '지자체 복지서비스 정보를 불러오지 못했습니다.'
      return
    }

    let json
    try {
      json = typeof apiResult.xml === 'string'
        ? JSON.parse(apiResult.xml)
        : apiResult.xml
    } catch (parseErr) {
      console.error('복지서비스 JSON 파싱 실패:', parseErr, apiResult.xml)
      benefitSeoulCount.value = 0
      benefitTotalCount.value = 0
      elderLoanCount.value = 0
      loanTotalCount.value = 0
      errorMessage.value = '복지서비스 데이터 형식이 올바르지 않습니다.'
      return
    }

    const list = json.servList || []

    // 지원금 키워드
    const dementiaKeywords = ['치매', '인지', '노인', '65세', '돌봄', '요양', '보호자', '간병']

    const isDementiaSupport = (item) => {
      const name = item.servNm || ''
      const summary = item.servDgst || ''
      const target = item.trgterIndvdlNm || ''
      const text = `${name} ${summary} ${target}`
      return dementiaKeywords.some(k => text.includes(k))
    }

    const dementiaList = list.filter(isDementiaSupport)
    const dementiaSeoulList = dementiaList.filter(item => {
      const dept = item.bizChrDeptNm || ''
      return dept.includes('서울특별시')
    })

    // 대출 키워드
    const loanKeywords = ['대출', '융자', '이자지원', '이자 지원', '보증', '전세자금', '주택구입']
    const elderKeywords = ['노인', '어르신', '고령', '65세', '노령', '장기요양', '기초연금']

    const isLoanService = (item) => {
      const name = item.servNm || ''
      const summary = item.servDgst || ''
      const text = `${name} ${summary}`
      return loanKeywords.some(k => text.includes(k))
    }

    const isElderService = (item) => {
      const name = item.servNm || ''
      const summary = item.servDgst || ''
      const target = item.trgterIndvdlNm || ''
      const text = `${name} ${summary} ${target}`
      return elderKeywords.some(k => text.includes(k))
    }

    const loanCandidates = list.filter(isLoanService)
    const elderLoanCandidates = loanCandidates.filter(isElderService)

    benefitTotalCount.value = dementiaList.length
    benefitSeoulCount.value = dementiaSeoulList.length
    loanTotalCount.value = loanCandidates.length
    elderLoanCount.value = elderLoanCandidates.length

  } catch (err) {
    console.error('지자체 복지서비스 요약 조회 실패:', err)
    errorMessage.value = '지자체 복지서비스 정보를 불러오지 못했습니다.'
    benefitSeoulCount.value = 0
    benefitTotalCount.value = 0
    elderLoanCount.value = 0
    loanTotalCount.value = 0
  }
}

onMounted(() => {
  loadUserData()
  loadWelfareSummary()
})

function goBenefit () {
  router.push({
    path: '/benefit',
    query: {
      localGovNm: localGovNm.value
    }
  })
}

function goLoan () {
  router.push('/loan')
}

function goHeartCare () {
  router.push('/heartCare')
}

function goHospitalCare () {
  router.push('/hospitalCare')
}
</script>

<style scoped>
.dashboard {

  max-height: 720px;
  margin-top: -15px;
  padding: 16px 16px 96px; /* 아래 여유 */
  background: #f7f8fa;
}

/* 상단 카드 */
.top-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  margin-bottom: 16px;
}
.profile-section {
  display: flex;
  align-items: center;
  gap: 12px;
}
.profile-icon {
  width: 40px;
  height: 40px;
}
.greeting-text {
  display: flex;
  flex-direction: column;
}
.greeting-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.greeting-subtitle {
  font-size: 14px;
  color: #666;
}

/* 요약 버튼 그룹 */
.summary-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.summary-btn {
  flex: 1;
  background: #a78bfa;
  color: #fff;
  border-radius: 12px;
  padding: 16px;
  min-height: 110px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.summary-btn.receive-loan {
  background: #6366f1;
}

.summary-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.summary-label {
  font-size: 15px;
  font-weight: 500;
  letter-spacing: -1.5px;
}
.summary-count {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}
.summary-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #e0e7ff;
}

.summary-right {
  display: flex;
  justify-content: flex-end;
}

.summary-action {
  background: rgba(255,255,255,0.3);
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}
.summary-action:hover {
  background: rgba(255,255,255,0.5);
}

/* 기능 카드 그리드 */
.function-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.func-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 205px;
}
.func-icon {
  width: 60px;
  height: 60px;
  margin-bottom: 2px;
}
.func-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}
.func-desc {
  font-size: 14px;
  color: #666;
}
.func-link {
  width: 100px;
  align-self: flex-end;
  background: rgba(59, 130, 246, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 3px 5px;
  color: #3b82f6;
  font-size: 16px;
  font-weight: 500;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 8px 32px rgba(59, 130, 246, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.func-link:hover {
  background: rgba(59, 130, 246, 0.25);
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 12px 40px rgba(59, 130, 246, 0.2);
}

.error-box {
  margin-top: 12px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fee2e2;
  color: #b91c1c;
  font-size: 13px;
}
</style>

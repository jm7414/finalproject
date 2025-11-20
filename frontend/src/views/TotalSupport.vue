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

    <!-- 에러 표시 -->
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

// 상세 페이지에서 사용할 기본 지자체(쿼리용)
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

// ✅ 복지서비스 요약 로드 (백엔드 /api/support/welfare 에 맞춤)
const loadWelfareSummary = async () => {
  try {
    errorMessage.value = ''

    // 🔥 Benefit.vue랑 같은 엔드포인트 사용
    //  - 여기서는 전국 데이터를 받아서
    //    1) 치매/노인/돌봄 관련 필터
    //    2) 그 중 서울 관련만 다시 카운트
    const res = await axios.get('/api/support/welfare', {
      params: {
        // 전국 기준으로 받기 위해 localGovNm 안 넘김
        pageNo: 1,
        numOfRows: 1000
      }
    })

    let data = res.data

    // 혹시 문자열로 들어오면 직접 파싱
    if (typeof data === 'string') {
      try {
        data = JSON.parse(data)
      } catch (parseErr) {
        console.error('복지서비스 JSON 파싱 실패(문자열):', parseErr, data)
        throw new Error('복지서비스 데이터 형식이 올바르지 않습니다.')
      }
    }

    // 에러 형식으로 내려온 경우 (success:false)
    if (data && data.success === false) {
      console.warn('복지서비스 응답 에러:', data)
      throw new Error(data.message || '지자체 복지서비스 정보를 불러오지 못했습니다.')
    }

    const list = Array.isArray(data.servList) ? data.servList : []

    if (!list.length) {
      console.warn('복지서비스 목록이 비어있습니다:', data)
    }

    // 🔹 치매/노인/돌봄 관련 키워드
    const dementiaKeywords = ['치매', '인지', '노인', '65세', '돌봄', '요양', '보호자', '간병']

    const isDementiaSupport = (item) => {
      const name = item.servNm || ''
      const summary = item.servDgst || ''
      const target = item.trgterIndvdlNm || ''
      const text = `${name} ${summary} ${target}`
      return dementiaKeywords.some(k => text.includes(k))
    }

    const dementiaList = list.filter(isDementiaSupport)

    // 서울 관련(지자체/부서명에 '서울특별시' 포함)만
    const dementiaSeoulList = dementiaList.filter(item => {
      const regionText = [
        item.selfGovNm,
        item.jurMnofNm,
        item.bizChrDeptNm
      ]
        .filter(Boolean)
        .join(' ')
      return regionText.includes('서울특별시')
    })

    // 🔹 대출 키워드
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

    // 🔹 화면에 쓸 숫자 세팅
    benefitTotalCount.value = dementiaList.length         // 전국 치매/노인/돌봄 서비스 수
    benefitSeoulCount.value = dementiaSeoulList.length   // 서울 치매/노인/돌봄 서비스 수
    loanTotalCount.value = loanCandidates.length         // 전국 대출 관련 서비스 수
    elderLoanCount.value = elderLoanCandidates.length    // 그 중 노인 대상 대출 수
  } catch (err) {
    console.error('지자체 복지서비스 요약 조회 실패:', err)
    errorMessage.value = err.message || '지자체 복지서비스 정보를 불러오지 못했습니다.'
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
  max-height: 890px;
  margin: 0 auto;
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

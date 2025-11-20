<!-- src/views/Benefit.vue -->
<template>
  <div class="benefit-page">
    <!-- 상단 헤더 -->
    <header class="benefit-header">
      <div class="title-row">
        <h1 class="benefit-title">받을 수 있는 지원금</h1>
        <span class="badge-source">공공데이터포털 기반</span>
      </div>
      <p class="benefit-subtitle">
        {{ localGovNm }} 기준 치매·노인·돌봄 관련 복지서비스
      </p>
      <p class="benefit-count">
        총 <strong>{{ welfareList.length }}</strong>건
      </p>
    </header>

    <!-- 에러 표시 -->
    <div v-if="errorMessage" class="error-box">
      {{ errorMessage }}
    </div>

    <!-- 로딩 -->
    <div v-else-if="loading" class="loading-box">
      불러오는 중입니다...
    </div>

    <!-- 지원금 리스트 -->
    <div v-else class="benefit-list">
      <div
        v-for="item in welfareList"
        :key="item.servId || item.servNm"
        class="benefit-card"
        @click="openDetail(item)"
      >
        <!-- 서비스명 -->
        <h2 class="benefit-name">
          {{ item.servNm || '지원금 이름 미제공' }}
        </h2>

        <!-- 태그/대상 정보 -->
        <div class="benefit-tags">
          <span class="tag">
            {{ item.lifeArrayNm || item.lifeArray || '생애주기 정보 없음' }}
          </span>
          <span class="tag">
            {{ item.trgterIndvdlNm || item.trgterIndvdlArrayNm || '대상 정보 없음' }}
          </span>
        </div>

        <!-- 요약 설명 (두 줄 정도만) -->
        <p class="benefit-summary">
          {{ (item.servDgst || item.servDtl || '상세 설명이 제공되지 않습니다.').slice(0, 80) }}
          <span v-if="(item.servDgst || item.servDtl || '').length > 80">…</span>
        </p>

        <!-- 지자체 / 담당 부서 -->
        <p class="benefit-dept">
          {{ item.selfGovNm || '지자체 정보 없음' }} ·
          {{ item.bizChrDeptNm || item.jurMnofNm || '담당 부서 정보 없음' }}
        </p>

        <div class="benefit-footer">
          <span class="benefit-more">자세히 보기</span>
          <span v-if="item.servId" class="benefit-code">
            코드: {{ item.servId }}
          </span>
        </div>
      </div>

      <!-- 리스트가 비었을 때 -->
      <div v-if="!loading && welfareList.length === 0" class="empty-box">
        현재 조건에 맞는 지원금 정보를 찾지 못했습니다.
      </div>
    </div>

    <!-- ✅ 상세 모달 -->
    <div
      v-if="selectedItem"
      class="detail-overlay"
      @click.self="closeDetail"
    >
      <div class="detail-modal">
        <!-- 상단 제목 + 닫기 -->
        <header class="detail-header">
          <h2 class="detail-title">
            {{ selectedItem.servNm || '지원금 이름 미제공' }}
          </h2>
          <button class="detail-close" @click="closeDetail">✕</button>
        </header>

        <!-- 지자체 / 태그 -->
        <div class="detail-meta">
          <span class="detail-meta-main">
            {{ selectedItem.selfGovNm || '지자체 정보 없음' }}
          </span>
          <div class="detail-chip-row">
            <span class="detail-chip">
              {{ selectedItem.lifeArrayNm || selectedItem.lifeArray || '생애주기 정보 없음' }}
            </span>
            <span class="detail-chip">
              {{ selectedItem.trgterIndvdlNm || selectedItem.trgterIndvdlArrayNm || '대상 정보 없음' }}
            </span>
          </div>
        </div>

        <!-- 내용들 -->
        <section class="detail-section">
          <h3>요약</h3>
          <p>
            {{ selectedItem.servDgst || '요약 설명이 제공되지 않습니다.' }}
          </p>
        </section>

        <section class="detail-section">
          <h3>지원 내용</h3>
          <p>
            {{ selectedItem.servDtlCntn || selectedItem.servDgst || '지원 내용이 상세히 제공되지 않습니다.' }}
          </p>
        </section>

        <section class="detail-section">
          <h3>지원 대상</h3>
          <p>
            {{ selectedItem.trgterIndvdlNm || '지원 대상 정보가 제공되지 않습니다.' }}
          </p>
        </section>

        <section class="detail-section">
          <h3>담당 부서 / 문의</h3>
          <p class="detail-contact">
            <span>
              <strong>담당 부서</strong>
              {{ selectedItem.bizChrDeptNm || selectedItem.jurMnofNm || '담당 부서 정보 없음' }}
            </span>
            <span>
              <strong>문의 전화</strong>
              {{ getPhone(selectedItem) || '상세 문의는 해당 지자체 복지 담당 부서로 문의해주세요.' }}
            </span>
          </p>
        </section>

        <footer class="detail-footer">
          <small>
            ※ 위 정보는
            <strong>공공데이터포털(한국사회보장정보원_지자체복지서비스)</strong>
            API를 통해 조회되었습니다.
          </small>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'

// ✅ 서울 전체 기준
const localGovNm = computed(() => '서울특별시')

const welfareList = ref([])
const loading = ref(false)
const errorMessage = ref('')

// 모달에서 보여줄 선택된 항목
const selectedItem = ref(null)
const openDetail = (item) => {
  selectedItem.value = item
}
const closeDetail = () => {
  selectedItem.value = null
}

// ✅ 전화번호 후보 필드들에서 첫 번째 값 찾기
const getPhone = (item) => {
  if (!item) return null

  const candidates = [
    item.cnsgnInsttOfcTelNo, // 수탁기관 전화번호
    item.jurMnofTelNo,       // 소관부처 전화번호
    item.jurMnofContcNo,     // 소관부처 연락처
    item.telNo               // 기타 tel 필드
  ]

  const found = candidates.find(v => v && String(v).trim() !== '')
  return found || null
}

/**
 * 지자체 복지서비스 조회
 * - 백엔드에서 공공데이터 JSON을 그대로 내려준다는 가정
 *   (data.servList 배열)
 */
const loadWelfareBenefits = async () => {
  loading.value = true
  errorMessage.value = ''
  welfareList.value = []

  try {
    // 🔥 여기 URL은 네 컨트롤러 매핑에 맞춰서:
    //   - @GetMapping("/api/support/welfare") 쓰면 이대로 두고
    //   - @GetMapping("/api/total-support") 쓰면 그걸로 바꿔주면 됨
    const res = await axios.get('/api/support/welfare', {
      params: {
        localGovNm: localGovNm.value,
        pageNo: 1,
        numOfRows: 500
      }
    })

    let data = res.data

    // 문자열로 내려오면 직접 파싱
    if (typeof data === 'string') {
      try {
        data = JSON.parse(data)
      } catch (e) {
        console.error('복지서비스 JSON 파싱 실패(문자열):', e, data)
        errorMessage.value = '복지서비스 데이터 형식이 올바르지 않습니다.'
        return
      }
    }

    // 백엔드에서 에러를 JSON으로 내려주는 경우 처리 (예: {success:false,...})
    if (data && data.success === false) {
      console.warn('복지서비스 응답 에러:', data)
      errorMessage.value = data.message || '복지서비스 데이터를 불러오지 못했습니다.'
      return
    }

    const list = Array.isArray(data.servList) ? data.servList : []

    // 1️⃣ "서울" 관련만 필터
    const regionFiltered = list.filter(item => {
      const regionText = [
        item.selfGovNm,
        item.jurMnofNm,
        item.bizChrDeptNm
      ]
        .filter(Boolean)
        .join(' ')
      return regionText.includes('서울')
    })

    // 2️⃣ 치매/노인/돌봄 관련 필터
    const dementiaKeywords = ['치매', '인지', '노인', '65세', '돌봄', '요양', '보호자', '간병']

    const filtered = regionFiltered.filter(item => {
      const name = item.servNm || ''
      const summary = item.servDgst || ''
      const target = item.trgterIndvdlNm || ''
      const text = `${name} ${summary} ${target}`
      return dementiaKeywords.some(k => text.includes(k))
    })

    // ✅ 샘플 로그
    if (filtered.length > 0) {
      console.log('=== 복지서비스 샘플 ===')
      console.log(filtered[0])
      console.log('전화 관련 필드 확인:', {
        cnsgnInsttOfcTelNo: filtered[0].cnsgnInsttOfcTelNo,
        jurMnofTelNo: filtered[0].jurMnofTelNo,
        jurMnofContcNo: filtered[0].jurMnofContcNo,
        telNo: filtered[0].telNo
      })
    }

    console.log('전국 전체 복지서비스 개수:', list.length)
    console.log('서울 관련 서비스 개수:', regionFiltered.length)
    console.log('서울 + 치매/노인/돌봄 필터 결과:', filtered.length)

    welfareList.value = filtered
  } catch (err) {
    console.error('복지서비스 조회 실패:', err)
    errorMessage.value = '복지서비스 조회 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadWelfareBenefits()
})
</script>

<style scoped>
.benefit-page {
  max-width: 500px;
  margin: 0 auto;
  padding: 16px;
  background: #f7f8fa;
}

/* 헤더 */
.benefit-header {
  margin-bottom: 16px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.benefit-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  color: #111827;
}
.badge-source {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
}
.benefit-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}
.benefit-count {
  margin: 6px 0 0;
  font-size: 14px;
  color: #4b5563;
}

/* 에러 / 로딩 / 빈 상태 */
.error-box {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fee2e2;
  color: #b91c1c;
  font-size: 13px;
}
.loading-box,
.empty-box {
  margin-top: 16px;
  padding: 12px;
  border-radius: 8px;
  background: #e5e7eb;
  font-size: 14px;
  color: #374151;
  text-align: center;
}

/* 리스트 / 카드 */
.benefit-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}
.benefit-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 14px 14px 10px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
}
.benefit-card:hover {
  box-shadow: 0 8px 16px rgba(0,0,0,0.08);
}
.benefit-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #111827;
}

/* 태그 */
.benefit-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tag {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4f46e5;
}

/* 설명 / 부서 */
.benefit-summary {
  font-size: 13px;
  color: #4b5563;
  margin: 2px 0 0;
}
.benefit-dept {
  font-size: 12px;
  color: #6b7280;
  margin: 4px 0 0;
}

.benefit-footer {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.benefit-more {
  font-size: 12px;
  color: #2563eb;
}
.benefit-code {
  font-size: 11px;
  color: #9ca3af;
}

/* ✅ 상세 모달 – 중앙 카드 형태 */
.detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 50;
}
.detail-modal {
  width: 90%;
  max-width: 300px;
  max-height: 80vh;
  background: #ffffff;
  border-radius: 18px;
  padding: 16px 18px 14px;
  box-shadow: 0 12px 32px rgba(0,0,0,0.25);
  overflow-y: auto;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.detail-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0;
  color: #111827;
}
.detail-close {
  border: none;
  background: transparent;
  font-size: 18px;
  cursor: pointer;
  color: #6b7280;
}

/* 모달 메타 정보 */
.detail-meta {
  margin-top: 6px;
}
.detail-meta-main {
  font-size: 12px;
  color: #6b7280;
}
.detail-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}
.detail-chip {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
}

/* 섹션들 */
.detail-section {
  margin-top: 12px;
}
.detail-section h3 {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #4b5563;
}
.detail-section p {
  font-size: 13px;
  margin: 0;
  color: #374151;
  line-height: 1.4;
}
.detail-contact {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-contact strong {
  display: inline-block;
  width: 70px;
}

/* 하단 출처 */
.detail-footer {
  margin-top: 12px;
  font-size: 11px;
  color: #9ca3af;
}
</style>

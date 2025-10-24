<template>
  <div class="subscription-cancel-page bg-white">
    <!-- 상단 프로필 -->
    <div class="profile-section bg-light py-2">
      <div class="profile-container">
        <img :src="logoPath" alt="Logo" class="logo-icon mb-0" />
        <div class="d-flex align-items-center justify-content-center">
          <div class="user-avatar me-2 mb-2">
            <i class="bi bi-person-circle text-secondary"></i>
          </div>
          <div class="text-start mb-2">
            <h6 class="mb-0 fw-normal">{{ currentPlan }} 이용중</h6>
            <p class="text-secondary mb-0 small">{{ guardianName }} 님 안녕하세요</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 구독 정보 -->
    <div class="plan-section py-3">
      <div class="container">
        <h6 class="mb-3">구독중인 요금제</h6>

        <div class="card border-primary border-2 mb-3">
          <div class="card-body p-3">
            <h5 class="card-title mb-2">{{ currentPlan }}</h5>

            <p class="text-muted mb-3 small">
              다음 결제일 :
              <span class="fw-semibold">{{ nextPaymentDate || '-' }}</span>
            </p>

            <ul class="list-unstyled text-secondary mb-0">
              <li class="mb-2"><i class="bi bi-check text-secondary me-2"></i> <span>30% 할인</span></li>
              <li class="mb-2"><i class="bi bi-check text-secondary me-2"></i> <span>보호자 3명까지 연결</span></li>
              <li class="mb-2"><i class="bi bi-check text-secondary me-2"></i> <span>강화된 분석 리포트</span></li>
              <li class="mb-2"><i class="bi bi-check text-secondary me-2"></i> <span>가족 공유 기능</span></li>
              <li class="mb-2"><i class="bi bi-check text-secondary me-2"></i> <span>1년간 위치정보 기록</span></li>
            </ul>
          </div>
        </div>

        <button class="btn btn-cancel w-100 py-2" @click="showCancelModal = true">
          구독 취소하기
        </button>
      </div>
    </div>

    <!-- 푸터 -->
    <div class="footer-section border-top mt-auto">
      <div class="container py-2">
        <p class="text-center text-secondary mb-1" style="font-size: 0.7rem;">
          언제든지 구독을 취소할 수 있습니다
        </p>
        <div class="text-center" style="font-size: 0.7rem;">
          <a href="#" class="text-secondary text-decoration-underline me-2">이용약관</a>
          <a href="#" class="text-secondary text-decoration-underline">개인정보처리방침</a>
        </div>
      </div>
    </div>

    <!-- 구독 취소 모달 (인라인) -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showCancelModal" class="modal-backdrop" @click.self="showCancelModal = false">
          <div class="cancel-subscription-modal bg-white" @click.stop>
            <!-- Close Button -->
            <button class="modal-close-btn" @click="showCancelModal = false">
              <i class="bi bi-x-lg"></i>
            </button>

            <!-- 상단 로고 -->
            <div class="logo-section text-center pt-4 pb-3">
              <img src="/mammamialogo.png" alt="Logo" class="logo-image" />
            </div>

            <!-- 제목 섹션 -->
            <div class="title-section text-center px-4 pb-3">
              <h5 class="modal-title mb-2">
                플러스 구독을 취소하시면<br>
                아래 혜택들은 이용할 수 없게 됩니다.
              </h5>
              <p class="modal-subtitle text-secondary mb-0">
                그래도 취소하시겠습니까?
              </p>
            </div>

            <!-- 혜택 목록 섹션 -->
            <div class="benefits-section px-4 py-3">
              <div class="benefits-list">
                <div class="benefit-item">
                  <i class="bi bi-check-circle-fill"></i>
                  <span>보호자 3명 연결 → 1명 제한</span>
                </div>
                <div class="benefit-item">
                  <i class="bi bi-check-circle-fill"></i>
                  <span>강화된 분석 리포트 제한</span>
                </div>
                <div class="benefit-item">
                  <i class="bi bi-check-circle-fill"></i>
                  <span>가족 공유 기능 사용 불가</span>
                </div>
                <div class="benefit-item">
                  <i class="bi bi-check-circle-fill"></i>
                  <span>위치 기록 1년 → 90일</span>
                </div>
              </div>
            </div>

            <!-- 안내 문구 -->
            <div class="notice-section px-4 pb-4">
              <div class="renewal-notice">
                <i class="bi bi-exclamation-circle"></i>
                <span>
                  멤버십 혜택은 결제일인 <strong>{{ nextPaymentDate || '-' }}</strong> 까지 유지되며,
                  결제한 요금은 환불되지 않습니다.
                </span>
              </div>
            </div>

            <!-- 버튼 섹션 -->
            <div class="button-section px-4 pb-4">
              <!-- 해지하기 버튼 (위쪽, 크고 눈에 띄게) -->
              <button class="btn btn-cancel-confirm w-100 mb-2" @click="handleCancelConfirm">
                해지하기
              </button>
              <!-- 계속 유지하기 버튼 (아래쪽, 작고 텍스트 링크처럼) -->
              <button class="btn btn-keep-link w-100" @click="showCancelModal = false">
                멤버십 계속 유지하기
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

/** 라우터 */
const router = useRouter()

/** 로고/표시 값 */
const logoPath = '/mammamialogo.png'
const currentPlan = ref('플러스 플랜')
const nextPaymentDate = ref<string>('')

/** 세션 정보 */
const guardianNo = ref<number | null>(null)
const patientNo = ref<number | null>(null)

/** 사용자 정보 저장 */
const userData = ref(null)
const subscriptionData = ref(null)

/** 모달 상태 */
const showCancelModal = ref(false)

/** Computed 속성 */
const guardianName = computed(() => {
  return userData.value?.name || 'User'
})

/** 유틸: KST YYYY-MM-DD 포맷 */
function formatKstYmd(input: string | number | Date | undefined | null): string {
  if (!input) return ''
  const d = new Date(input)
  if (Number.isNaN(+d)) return ''
  const ymd = new Intl.DateTimeFormat('sv-SE', {
    timeZone: 'Asia/Seoul',
    year: 'numeric', month: '2-digit', day: '2-digit'
  }).format(d)
  return ymd
}

/** 1. 사용자 기본 정보 로드 */
async function loadUserData() {
  try {
    const response = await axios.get('/api/user/me')
    userData.value = response.data
    guardianNo.value = userData.value.userNo

    // 로컬스토리지에 저장 (fallback용)
    if (guardianNo.value) {
      localStorage.setItem('guardianNo', String(guardianNo.value))
    }
  } catch (error) {
    console.error('사용자 정보 로드 실패:', error)
    // fallback: 로컬스토리지에서 가져오기
    guardianNo.value = Number(localStorage.getItem('guardianNo') || 0) || null
  }
}

/** 2. 구독 정보 로드 */
async function loadSubscriptionData() {
  if (!guardianNo.value) return

  try {
    const response = await axios.get('/api/subscriptions/summary', {
      params: {
        guardianNo: guardianNo.value
      },
      headers: {
        'X-Mock-User': String(guardianNo.value)
      }
    })

    subscriptionData.value = response.data

    // plus 플랜이 아니면 베이직 페이지로 이동
    const plus = subscriptionData.value?.plus === true ||
      subscriptionData.value?.subscriptionActive === true

    if (!plus) {
      router.replace('/basicplan')
      return
    }

    patientNo.value = subscriptionData.value?.patientNo ?? null

    // subscriptionEndTime이 있으면 날짜 포맷팅
    const endTime = subscriptionData.value?.subscriptionEndTime ||
      subscriptionData.value?.subscription?.end ||
      subscriptionData.value?.end ||
      subscriptionData.value?.subscribe_endtime

    if (endTime) {
      nextPaymentDate.value = formatKstYmd(endTime)
    }
  } catch (error) {
    console.error('구독 정보 로드 실패:', error)
  }
}

/** 3. 추가 결제일 정보 로드 (fallback) */
async function fetchEndtimeFallback() {
  if (!patientNo.value || nextPaymentDate.value) return

  // 1) patient basic
  try {
    const resB = await fetch(`/api/connect/patient/${patientNo.value}/basic`, {
      credentials: 'include'
    })
    const b = await resB.json().catch(() => ({} as any))
    const end = b?.subscribe_endtime ||
      b?.subscription_endtime ||
      b?.subscribeEndtime ||
      b?.subscriptionEndTime ||
      b?.end
    if (end) {
      nextPaymentDate.value = formatKstYmd(end)
      return
    }
  } catch { /* ignore */ }

  // 2) guardians 목록
  try {
    const resG = await fetch(`/api/connect/patient/${patientNo.value}/guardians`, {
      credentials: 'include'
    })
    const g = await resG.json().catch(() => [])
    const scan = (obj: any) => obj?.subscribe_endtime ||
      obj?.subscription_endtime ||
      obj?.end ||
      obj?.subscribeEndtime
    if (Array.isArray(g)) {
      for (const it of g) {
        const end = scan(it)
        if (end) {
          nextPaymentDate.value = formatKstYmd(end)
          break
        }
      }
    } else {
      const end = scan(g)
      if (end) nextPaymentDate.value = formatKstYmd(end)
    }
  } catch { /* ignore */ }
}

/** 구독 취소 확인 (모달에서 "해지하기" 클릭 시) */
async function handleCancelConfirm() {
  if (!guardianNo.value) {
    alert('보호자 정보를 찾을 수 없습니다.')
    return
  }

  try {
    const res = await fetch('/api/subscriptions/cancel', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'X-Mock-User': String(guardianNo.value)
      },
      body: JSON.stringify({ guardianNo: guardianNo.value })
    })
    const data = await res.json().catch(() => ({} as any))

    if (!res.ok) {
      alert(data?.message || '구독 취소 처리에 실패했습니다.')
      return
    }

    alert('구독이 취소되었습니다.')
    showCancelModal.value = false
    router.replace('/basicplan')
  } catch (error) {
    console.error('구독 취소 실패:', error)
    alert('구독 취소 API가 아직 준비되지 않았습니다. (백엔드 연결 필요)')
  }
}

/** 초기화 */
onMounted(async () => {
  // 1. 사용자 정보 먼저 로드
  await loadUserData()

  // 2. 구독 정보 로드
  await loadSubscriptionData()

  // 3. 결제일 정보가 없으면 fallback 시도
  if (!nextPaymentDate.value) {
    await fetchEndtimeFallback()
  }
})
</script>

<style scoped>
/* ===== 전역 초록 글씨 차단: 이 컴포넌트 내부에서는 기본색 강제 ===== */
.subscription-cancel-page {
  color: #171717;
}

.subscription-cancel-page a {
  color: inherit !important;
}

.subscription-cancel-page .text-secondary {
  color: #6c757d !important;
}

/* 레이아웃 */
.subscription-cancel-page {
  height: 640px;
  max-width: 480px;
  margin: 0 auto;
  font-size: 0.9rem;
  transform: scale(1.0);
  transform-origin: top center;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  margin-top: -14px;
}

.profile-section {
  background-color: #FAFAFA;
  padding-top: 0.5rem !important;
  padding-bottom: 0.5rem !important;
  width: 100%;
  flex-shrink: 0;
}

.profile-container {
  width: 100%;
  text-align: center;
  padding: 0;
  margin: 0;
}

.logo-icon {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.user-avatar i {
  font-size: 40px;
}

.user-avatar {
  border-radius: 50%;
  overflow: hidden;
}

.plan-section {
  flex: 1;
  overflow-y: auto;
}

.card {
  border-radius: 8px;
}

.border-primary {
  border-color: rgba(74, 98, 221, 0.85) !important;
}

.card-title {
  color: #171717;
  font-weight: 500;
  font-size: 1.4rem;
}

.card-body {
  font-size: 0.9rem;
}

.list-unstyled li {
  display: flex;
  align-items: center;
}

.list-unstyled i {
  font-size: 1.1rem;
}

.btn-cancel {
  background-color: rgba(74, 98, 221, 0.85);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 0.95rem;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background-color: rgba(74, 98, 221, 1);
  transform: translateY(-1px);
  box-shadow: 0px 3px 6px rgba(0, 0, 0, 0.15);
}

.footer-section {
  background-color: white;
  border-top: 1px solid #E5E5E5;
  flex-shrink: 0;
}

/* ========== 모달 스타일 ========== */

/* Modal Backdrop */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 1rem;
}

/* Modal Container */
.cancel-subscription-modal {
  position: relative;
  height: 672px;
  max-width: 400px;
  width: 100%;
  margin: 0 auto;
  font-size: 0.9rem;
  transform: scale(0.9) translateX(-10px);
  transform-origin: center;
  overflow-y: auto;
  color: #171717 !important;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

/* Close Button */
.modal-close-btn {
  position: absolute;
  top: 1rem;
  right: 1rem;
  width: 36px;
  height: 36px;
  border: none;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.modal-close-btn:hover {
  background-color: rgba(255, 255, 255, 1);
  transform: scale(1.1);
}

.modal-close-btn i {
  font-size: 1rem;
  color: #6c757d;
}

/* Modal Transitions */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-active .cancel-subscription-modal,
.modal-fade-leave-active .cancel-subscription-modal {
  transition: transform 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .cancel-subscription-modal {
  transform: scale(0.8);
}

.modal-fade-leave-to .cancel-subscription-modal {
  transform: scale(0.8);
}

/* Logo Section */
.logo-section {
  padding-top: 2rem;
}

.logo-image {
  width: 70px;
  height: 70px;
  object-fit: contain;
}

/* Title Section */
.modal-title {
  font-size: 1.15rem;
  font-weight: 600;
  color: #171717 !important;
  line-height: 1.5;
}

.modal-subtitle {
  font-size: 0.9rem;
  color: #6c757d !important;
}

/* Benefits Section */
.benefits-section {
  background-color: #FAFAFA;
  margin: 0 1rem;
  border-radius: 12px;
}

.benefits-list {
  padding: 0.5rem 0;
}

.benefit-item {
  display: flex;
  align-items: center;
  padding: 0.65rem 0;
  font-size: 0.9rem;
  color: #171717 !important;
}

.benefit-item i {
  margin-right: 12px;
  font-size: 1.1rem;
  color: rgba(74, 98, 221, 0.85) !important;
  flex-shrink: 0;
}

/* Notice Section */
.renewal-notice {
  background: #F8F9FA;
  padding: 14px;
  border-radius: 12px;
  text-align: left;
  display: flex;
  gap: 10px;
  font-size: 0.75rem;
  color: #6c757d !important;
  border: 1px solid #E5E5E5;
}

.renewal-notice i {
  font-size: 1rem;
  flex-shrink: 0;
  margin-top: 2px;
  color: #6c757d !important;
}

.renewal-notice strong {
  color: #171717 !important;
  font-weight: 600;
}

/* Button Section */
.button-section {
  margin-top: auto;
}

/* 해지하기 버튼 - 크고 눈에 띄게 (빨간색 또는 경고색) */
.btn-cancel-confirm {
  background-color: #FF6B6B; /* 빨간색으로 경고 느낌 */
  border: none;
  border-radius: 12px;
  color: white !important;
  font-size: 0.95rem;
  font-weight: 500;
  padding: 14px;
  transition: all 0.2s;
}

.btn-cancel-confirm:hover {
  background-color: #FF5252;
  transform: translateY(-1px);
  box-shadow: 0px 3px 6px rgba(255, 107, 107, 0.3);
}

/* 멤버십 계속 유지하기 버튼 - 작고 텍스트 링크처럼 */
.btn-keep-link {
  background: transparent;
  border: none;
  color: #6c757d !important;
  font-size: 0.9rem;
  text-decoration: underline;
  cursor: pointer;
  padding: 10px;
}

.btn-keep-link:hover {
  color: #171717 !important;
  background-color: transparent;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .container {
    padding-left: 16px;
    padding-right: 16px;
  }

  .cancel-subscription-modal {
    height: 90vh;
    max-height: 672px;
  }

  .modal-backdrop {
    padding: 0.5rem;
  }
}
</style>

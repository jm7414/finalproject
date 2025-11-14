<template>
  <div class="desktop-mypage-plusplan">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">구독 및 결제 관리</h1>
        <p class="page-subtitle">현재 구독 중인 플러스 플랜 정보를 확인하세요</p>
      </div>

      <!-- 구독 정보 카드 -->
      <div class="subscription-info-card">
        <div class="card-header">
          <h2 class="card-title">구독중인 요금제</h2>
        </div>
        
        <div class="card-body">
          <div class="plan-name-section">
            <h3 class="plan-name">{{ currentPlan }}</h3>
            <p class="next-payment">
              다음 결제일: <strong>{{ nextPaymentDate || '-' }}</strong>
            </p>
          </div>

          <div class="benefits-section">
            <h4 class="benefits-title">이용 중인 혜택</h4>
            <ul class="benefits-list">
              <li class="benefit-item">
                <i class="bi bi-check-circle"></i>
                <span>30% 할인</span>
              </li>
              <li class="benefit-item">
                <i class="bi bi-check-circle"></i>
                <span>보호자 3명까지 연결</span>
              </li>
              <li class="benefit-item">
                <i class="bi bi-check-circle"></i>
                <span>강화된 분석 리포트</span>
              </li>
              <li class="benefit-item">
                <i class="bi bi-check-circle"></i>
                <span>가족 공유 기능</span>
              </li>
              <li class="benefit-item">
                <i class="bi bi-check-circle"></i>
                <span>1년간 위치정보 기록</span>
              </li>
            </ul>
          </div>

          <button class="btn-cancel" @click="showCancelModal = true">
            구독 취소하기
          </button>
        </div>
      </div>

      <!-- 푸터 -->
      <div class="footer-section">
        <p class="footer-note">언제든지 구독을 취소할 수 있습니다</p>
        <div class="footer-links">
          <a href="#" @click.prevent>이용약관</a>
          <a href="#" @click.prevent>개인정보처리방침</a>
        </div>
      </div>
    </div>

    <!-- 구독 취소 모달 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showCancelModal" class="modal-overlay" @click.self="showCancelModal = false">
          <div class="cancel-modal-content" @click.stop>
            <div class="modal-header">
              <h2 class="modal-title">구독 취소</h2>
              <button class="btn-close" @click="showCancelModal = false">
                <i class="bi bi-x-lg"></i>
              </button>
            </div>

            <div class="modal-body">
              <!-- 로고 -->
              <div class="logo-section">
                <img src="/mammamialogo.png" alt="Logo" class="logo-image" />
              </div>

              <!-- 제목 -->
              <div class="title-section">
                <h3 class="modal-title-text">
                  플러스 구독을 취소하시면<br>
                  아래 혜택들은 이용할 수 없게 됩니다.
                </h3>
                <p class="modal-subtitle">
                  그래도 취소하시겠습니까?
                </p>
              </div>

              <!-- 혜택 목록 -->
              <div class="benefits-modal-section">
                <div class="benefits-list-modal">
                  <div class="benefit-item-modal">
                    <i class="bi bi-check-circle-fill"></i>
                    <span>보호자 3명 연결 → 1명 제한</span>
                  </div>
                  <div class="benefit-item-modal">
                    <i class="bi bi-check-circle-fill"></i>
                    <span>강화된 분석 리포트 제한</span>
                  </div>
                  <div class="benefit-item-modal">
                    <i class="bi bi-check-circle-fill"></i>
                    <span>가족 공유 기능 사용 불가</span>
                  </div>
                  <div class="benefit-item-modal">
                    <i class="bi bi-check-circle-fill"></i>
                    <span>위치 기록 1년 → 90일</span>
                  </div>
                </div>
              </div>

              <!-- 안내 문구 -->
              <div class="notice-section">
                <div class="renewal-notice">
                  <i class="bi bi-exclamation-circle"></i>
                  <span>
                    멤버십 혜택은 결제일인 <strong>{{ nextPaymentDate || '-' }}</strong> 까지 유지되며,
                    결제한 요금은 환불되지 않습니다.
                  </span>
                </div>
              </div>

              <!-- 버튼 -->
              <div class="button-section">
                <button class="btn-cancel-confirm" @click="handleCancelConfirm">
                  해지하기
                </button>
                <button class="btn-keep-link" @click="showCancelModal = false">
                  멤버십 계속 유지하기
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const currentPlan = ref('플러스 플랜')
const nextPaymentDate = ref('')
const guardianNo = ref(null)
const patientNo = ref(null)
const userData = ref(null)
const subscriptionData = ref(null)
const showCancelModal = ref(false)

// 날짜 포맷팅
function formatKstYmd(input) {
  if (!input) return ''
  const d = new Date(input)
  if (Number.isNaN(+d)) return ''
  const ymd = new Intl.DateTimeFormat('sv-SE', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(d)
  return ymd.replace(/-/g, '.')
}

// 사용자 정보 로드
async function loadUserData() {
  try {
    const response = await axios.get('/api/user/me')
    userData.value = response.data
    guardianNo.value = userData.value.userNo

    if (guardianNo.value) {
      localStorage.setItem('guardianNo', String(guardianNo.value))
    }
  } catch (error) {
    console.error('사용자 정보 로드 실패:', error)
    guardianNo.value = Number(localStorage.getItem('guardianNo') || 0) || null
  }
}

// 구독 정보 로드
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
      router.replace('/desktop/mypage/billing')
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

// 결제일 정보 fallback
async function fetchEndtimeFallback() {
  if (!patientNo.value || nextPaymentDate.value) return

  try {
    const resB = await fetch(`/api/connect/patient/${patientNo.value}/basic`, {
      credentials: 'include'
    })
    const b = await resB.json().catch(() => ({}))
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

  try {
    const resG = await fetch(`/api/connect/patient/${patientNo.value}/guardians`, {
      credentials: 'include'
    })
    const g = await resG.json().catch(() => [])
    const scan = (obj) => obj?.subscribe_endtime ||
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

// 구독 취소 확인
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
    const data = await res.json().catch(() => ({}))

    if (!res.ok) {
      alert(data?.message || '구독 취소 처리에 실패했습니다.')
      return
    }

    alert('구독이 취소되었습니다.')
    showCancelModal.value = false
    router.replace('/desktop/mypage')
  } catch (error) {
    console.error('구독 취소 실패:', error)
    alert('구독 취소 처리 중 오류가 발생했습니다.')
  }
}

onMounted(async () => {
  await loadUserData()
  await loadSubscriptionData()
  if (!nextPaymentDate.value) {
    await fetchEndtimeFallback()
  }
})
</script>

<style scoped>
.desktop-mypage-plusplan {
  width: 100%;
  min-height: calc(100vh - 80px);
  padding: 0;
}

.page-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
}

.page-header {
  margin-bottom: 40px;
  text-align: center;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #171717;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #737373;
  margin: 0;
}

.subscription-info-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 2px solid rgba(74, 98, 221, 0.85);
  margin-bottom: 32px;
}

.card-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: #171717;
  margin: 0;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.plan-name-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.plan-name {
  font-size: 28px;
  font-weight: 600;
  color: #171717;
  margin: 0;
}

.next-payment {
  font-size: 16px;
  color: #737373;
  margin: 0;
}

.next-payment strong {
  color: #171717;
  font-weight: 600;
}

.benefits-section {
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

.benefits-title {
  font-size: 18px;
  font-weight: 600;
  color: #171717;
  margin: 0 0 16px 0;
}

.benefits-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: #525252;
}

.benefit-item i {
  color: rgba(74, 98, 221, 0.85);
  font-size: 18px;
  flex-shrink: 0;
}

.btn-cancel {
  width: 100%;
  padding: 14px 24px;
  background: rgba(74, 98, 221, 0.85);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
}

.btn-cancel:hover {
  background: rgba(74, 98, 221, 1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.footer-section {
  text-align: center;
  padding-top: 32px;
  border-top: 1px solid #e5e7eb;
}

.footer-note {
  font-size: 13px;
  color: #737373;
  margin: 0 0 16px 0;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 16px;
  font-size: 13px;
}

.footer-links a {
  color: #6c757d;
  text-decoration: underline;
}

.footer-links a:hover {
  color: #6366f1;
}

/* 모달 스타일 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 24px;
}

.cancel-modal-content {
  background: #ffffff;
  border-radius: 12px;
  max-width: 600px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  position: relative;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #171717;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  font-size: 20px;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.btn-close:hover {
  color: #171717;
}

.modal-body {
  padding: 24px;
}

.logo-section {
  text-align: center;
  margin-bottom: 24px;
}

.logo-image {
  width: 70px;
  height: 70px;
  object-fit: contain;
}

.title-section {
  text-align: center;
  margin-bottom: 24px;
}

.modal-title-text {
  font-size: 18px;
  font-weight: 600;
  color: #171717;
  line-height: 1.5;
  margin: 0 0 8px 0;
}

.modal-subtitle {
  font-size: 14px;
  color: #6c757d;
  margin: 0;
}

.benefits-modal-section {
  background: #fafafa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.benefits-list-modal {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.benefit-item-modal {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #171717;
}

.benefit-item-modal i {
  color: rgba(74, 98, 221, 0.85);
  font-size: 18px;
  flex-shrink: 0;
}

.notice-section {
  margin-bottom: 24px;
}

.renewal-notice {
  background: #f8f9fa;
  padding: 14px;
  border-radius: 12px;
  display: flex;
  gap: 10px;
  font-size: 13px;
  color: #6c757d;
  border: 1px solid #e5e7eb;
}

.renewal-notice i {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 2px;
  color: #6c757d;
}

.renewal-notice strong {
  color: #171717;
  font-weight: 600;
}

.button-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.btn-cancel-confirm {
  width: 100%;
  padding: 14px 24px;
  background: #ff6b6b;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-confirm:hover {
  background: #ff5252;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.btn-keep-link {
  width: 100%;
  padding: 10px;
  background: transparent;
  border: none;
  color: #6c757d;
  font-size: 14px;
  text-decoration: underline;
  cursor: pointer;
  transition: color 0.2s;
}

.btn-keep-link:hover {
  color: #171717;
}

/* 모달 트랜지션 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-active .cancel-modal-content,
.modal-fade-leave-active .cancel-modal-content {
  transition: transform 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .cancel-modal-content {
  transform: scale(0.9);
}

.modal-fade-leave-to .cancel-modal-content {
  transform: scale(0.9);
}
</style>


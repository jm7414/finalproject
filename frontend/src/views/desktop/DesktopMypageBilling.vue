<template>
  <div class="desktop-mypage-billing">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">구독 및 결제 관리</h1>
        <p class="page-subtitle">플러스 플랜으로 업그레이드하여 더 많은 기능을 이용하세요</p>
      </div>

      <div class="plans-section">
        <h2 class="section-title">구독 요금제</h2>
        
        <div class="plans-grid">
          <!-- 월간 플랜 -->
          <div 
            class="plan-card" 
            :class="{ 'selected': selectedPlan === 'monthly' }"
            @click="selectPlan('monthly')"
          >
            <div class="plan-header">
              <h3 class="plan-name">플러스 플랜 1개월</h3>
            </div>
            <div class="plan-price">
              <span class="price-amount">￦5,990</span>
              <span class="price-period">/월</span>
            </div>
            <ul class="plan-features">
              <li><i class="bi bi-check-circle"></i>보호자 3명까지 연결</li>
              <li><i class="bi bi-check-circle"></i>강화된 분석 리포트</li>
              <li><i class="bi bi-check-circle"></i>가족 공유 기능</li>
            </ul>
          </div>

          <!-- 연간 플랜 -->
          <div 
            class="plan-card plan-card-popular" 
            :class="{ 'selected': selectedPlan === 'yearly' }"
            @click="selectPlan('yearly')"
          >
            <div class="popular-badge">가장 인기</div>
            <div class="plan-header">
              <h3 class="plan-name">플러스 플랜 1년</h3>
            </div>
            <div class="plan-price">
              <div class="original-price">￦ 71,880</div>
              <div class="discount-price">
                <span class="price-amount">￦ 50,320</span>
                <span class="price-period">/년</span>
              </div>
              <div class="monthly-equivalent">￦ 4,193/월</div>
            </div>
            <ul class="plan-features">
              <li><i class="bi bi-check-circle"></i>30% 할인</li>
              <li><i class="bi bi-check-circle"></i>보호자 3명까지 연결</li>
              <li><i class="bi bi-check-circle"></i>강화된 분석 리포트</li>
              <li><i class="bi bi-check-circle"></i>가족 공유 기능</li>
              <li><i class="bi bi-check-circle"></i>1년간 위치정보 기록</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="footer-section">
        <button class="btn-primary" @click="openPaymentModal" :disabled="!selectedPlan">
          시작하기
        </button>
        <p class="footer-note">언제든지 구독을 취소할 수 있습니다</p>
        <div class="footer-links">
          <a href="#" @click.prevent="openTermsModal('service')">이용약관</a>
          <a href="#" @click.prevent="openTermsModal('privacy')">개인정보처리방침</a>
        </div>
      </div>
    </div>

    <!-- 결제 모달 -->
    <Teleport to="body">
      <div v-if="showPaymentModal" class="payment-modal-overlay" @click.self="closePaymentModal">
        <div class="payment-modal-content">
          <div class="payment-modal-header">
            <h2 class="modal-title">결제 정보</h2>
            <button class="btn-close" @click="closePaymentModal">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <div class="payment-modal-body">
            <!-- 약관 동의 -->
            <div class="payment-section">
              <h3 class="section-title-small">서비스 이용약관</h3>
              
              <div class="all-agree-box">
                <label class="agree-all-label">
                  <input type="checkbox" v-model="agreeAll" @change="toggleAll" />
                  <span>전체 동의</span>
                </label>
              </div>

              <div class="terms-list">
                <div class="term-item">
                  <label class="term-label">
                    <input type="checkbox" v-model="terms.service" />
                    <span class="text-danger">[필수]</span> 서비스 이용약관
                  </label>
                  <i class="bi bi-chevron-right" @click="openTermsModal('service')" style="cursor:pointer"></i>
                </div>

                <div class="term-item">
                  <label class="term-label">
                    <input type="checkbox" v-model="terms.privacy" />
                    <span class="text-danger">[필수]</span> 개인정보 처리방침
                  </label>
                  <i class="bi bi-chevron-right" @click="openTermsModal('privacy')" style="cursor:pointer"></i>
                </div>
              </div>
            </div>

            <!-- 결제 정보 -->
            <div class="payment-section">
              <h3 class="section-title-small">결제 정보</h3>
              
              <div class="selected-plan-card">
                <div class="plan-info-header">
                  <span class="plan-type">{{ selectedPlan === 'monthly' ? '플러스 플랜 월간' : '플러스 플랜 연간' }}</span>
                  <strong class="plan-price-text">{{ selectedPlan === 'monthly' ? '5,990원' : '50,320원' }}</strong>
                </div>
                <ul class="plan-features-list">
                  <li v-if="selectedPlan === 'yearly'">• 30% 할인</li>
                  <li>• 보호자 3명까지 연결</li>
                  <li>• 강화된 분석 리포트</li>
                  <li>• 가족 공유 기능</li>
                  <li v-if="selectedPlan === 'yearly'">• 1년간 위치정보 기록</li>
                </ul>
              </div>
            </div>

            <!-- 결제 수단 -->
            <div class="payment-section">
              <h3 class="section-title-small">결제 수단</h3>
              
              <div class="payment-methods">
                <div 
                  class="payment-method-item" 
                  :class="{ 'selected': paymentMethod === 'card' }"
                  @click="selectPaymentMethod('card')"
                >
                  <input type="radio" name="paymentMethod" id="card" v-model="paymentMethod" value="card" />
                  <i class="bi bi-credit-card"></i>
                  <label for="card">신용카드</label>
                </div>
              </div>

              <!-- 가격 요약 -->
              <div class="price-summary">
                <div class="price-row">
                  <span>월 이용료</span>
                  <span>{{ monthlyPrice }}원</span>
                </div>
                <div class="price-row">
                  <span>VAT</span>
                  <span>{{ vat }}원</span>
                </div>
                <div class="price-divider"></div>
                <div class="price-row total">
                  <strong>총 결제금액</strong>
                  <strong class="total-price">{{ totalPrice }}원</strong>
                </div>
              </div>
            </div>

            <!-- 결제 버튼 -->
            <button 
              class="btn-payment" 
              @click="handlePayment" 
              :disabled="!canProceed"
            >
              결제하기
            </button>
            <p class="payment-note">결제 시 위 약관에 모두 동의한 것으로 간주됩니다</p>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 약관 상세 모달 -->
    <Teleport to="body">
      <div v-if="showTermsModal" class="payment-modal-overlay" @click.self="closeTermsModal">
        <div class="terms-modal-content">
          <div class="payment-modal-header">
            <h2 class="modal-title">약관 정보</h2>
            <button class="btn-close" @click="closeTermsModal">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <div class="terms-modal-body">
            <!-- 서비스 이용약관 -->
            <div v-if="currentTermsType === 'service'" class="terms-content-section">
              <div class="terms-header">
                <span class="text-danger">*</span>
                <strong>서비스 이용약관</strong>
                <span class="text-danger">*</span>
              </div>
              <div class="terms-text">
                <p>
                  본 서비스는 회원가입 후 제공되며, 유료 구독 시 자동 결제가 진행됩니다.
                  사용자는 서비스 이용 중 발생하는 콘텐츠, 기능, 요금제 변경 등에 대해 안내를 받을 수 있으며,
                  서비스 이용은 약관에 동의한 것으로 간주됩니다.
                </p>
              </div>
              <div class="terms-agree-section">
                <label>서비스 이용약관 동의<span class="text-danger">*</span></label>
                <div class="radio-group">
                  <label>
                    <input type="radio" name="modalServiceAgree" v-model="modalTerms.service" :value="true" />
                    예
                  </label>
                  <label>
                    <input type="radio" name="modalServiceAgree" v-model="modalTerms.service" :value="false" />
                    아니요
                  </label>
                </div>
              </div>
            </div>

            <!-- 개인정보 처리방침 -->
            <div v-if="currentTermsType === 'privacy'" class="terms-content-section">
              <div class="terms-header">
                <span class="text-danger">*</span>
                <strong>개인정보 처리방침</strong>
                <span class="text-danger">*</span>
              </div>
              <div class="terms-text">
                <p>
                  입력하신 개인정보는 회원 관리, 결제 처리, 고객 지원을 위해 수집되며,
                  관련 법령에 따라 안전하게 보호됩니다. 제3자 제공 또는 마케팅 활용 시 별도 동의를 받습니다.
                  언제든지 열람, 수정, 삭제를 요청할 수 있습니다.
                </p>
              </div>
              <div class="terms-agree-section">
                <label>개인정보 처리방침 동의<span class="text-danger">*</span></label>
                <div class="radio-group">
                  <label>
                    <input type="radio" name="modalPrivacyAgree" v-model="modalTerms.privacy" :value="true" />
                    예
                  </label>
                  <label>
                    <input type="radio" name="modalPrivacyAgree" v-model="modalTerms.privacy" :value="false" />
                    아니요
                  </label>
                </div>
              </div>
            </div>

            <button class="btn-complete" @click="completeTermsModal">완료</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 카드 정보 입력 모달 -->
    <Teleport to="body">
      <div v-if="showCardModal" class="payment-modal-overlay" @click.self="closeCardModal">
        <div class="card-modal-content">
          <div class="payment-modal-header">
            <h2 class="modal-title">결제정보</h2>
            <button class="btn-close" @click="closeCardModal">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <div class="card-modal-body">
            <div class="form-group">
              <label class="form-label">카드번호<span class="text-danger">*</span></label>
              <input 
                type="text" 
                class="form-control" 
                placeholder="카드번호" 
                v-model="cardInfo.number" 
                maxlength="19"
              />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">유효기간<span class="text-danger">*</span></label>
                <input 
                  type="text" 
                  class="form-control" 
                  placeholder="MM/YY" 
                  v-model="cardInfo.expiry" 
                  maxlength="5"
                />
              </div>
              <div class="form-group">
                <label class="form-label">CVC번호<span class="text-danger">*</span></label>
                <input 
                  type="text" 
                  class="form-control" 
                  placeholder="3자리" 
                  v-model="cardInfo.cvc" 
                  maxlength="3"
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">카드 소유자명<span class="text-danger">*</span></label>
              <input 
                type="text" 
                class="form-control" 
                placeholder="카드 소유자명" 
                v-model="cardInfo.holder"
              />
            </div>

            <button 
              class="btn-card-payment" 
              @click="submitPayment" 
              :disabled="!isCardInfoValid"
            >
              결제하기
            </button>
            <button class="btn-cancel" @click="closeCardModal">취소</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const selectedPlan = ref('')
const guardianNo = ref(null)

// 결제 모달 상태
const showPaymentModal = ref(false)
const showTermsModal = ref(false)
const showCardModal = ref(false)
const currentTermsType = ref('')

// 약관 동의
const agreeAll = ref(false)
const terms = ref({ service: false, privacy: false })
const modalTerms = ref({ service: false, privacy: false })

// 결제 수단
const paymentMethod = ref('')

// 카드 정보
const cardInfo = ref({ number: '', expiry: '', cvc: '', holder: '' })

// 가격 계산
const monthlyPrice = computed(() => selectedPlan.value === 'monthly' ? '5,990' : '50,320')
const vat = computed(() => selectedPlan.value === 'monthly' ? '599' : '5,032')
const totalPrice = computed(() => selectedPlan.value === 'monthly' ? '6,589' : '55,352')

// 유효성 검사
const canProceed = computed(() => terms.value.service && terms.value.privacy && paymentMethod.value)
const isCardInfoValid = computed(() =>
  cardInfo.value.number && cardInfo.value.expiry && cardInfo.value.cvc && cardInfo.value.holder
)

// 전체 동의 ↔ 개별 동의 동기화
watch([() => terms.value.service, () => terms.value.privacy], () => {
  agreeAll.value = terms.value.service && terms.value.privacy
})

function toggleAll() {
  terms.value.service = agreeAll.value
  terms.value.privacy = agreeAll.value
}

async function fetchMe() {
  try {
    const res = await fetch('/api/user/me', { credentials: 'include' })
    const me = await res.json().catch(() => ({}))
    guardianNo.value = Number(me.userNo ?? me.userId ?? me.id ?? 0) || null
  } catch {
    guardianNo.value = null
  } finally {
    if (!guardianNo.value) {
      guardianNo.value = Number(localStorage.getItem('guardianNo') || 0) || null
    }
  }
}

async function redirectIfPlus() {
  try {
    if (!guardianNo.value) return
    const headers = { 'X-Mock-User': String(guardianNo.value) }
    const res = await fetch('/api/subscriptions/summary', { method: 'GET', credentials: 'include', headers })
    const s = await res.json().catch(() => ({}))
    const plus = s?.plus === true || s?.subscriptionActive === true
    if (plus) {
      router.replace('/desktop/mypage/plusplan')
    }
  } catch {/* ignore */}
}

function selectPlan(plan) {
  selectedPlan.value = plan
}

function openPaymentModal() {
  if (!selectedPlan.value) {
    alert('플랜을 선택해주세요')
    return
  }
  showPaymentModal.value = true
  document.body.style.overflow = 'hidden'
}

function closePaymentModal() {
  showPaymentModal.value = false
  document.body.style.overflow = ''
  // 상태 초기화
  agreeAll.value = false
  terms.value = { service: false, privacy: false }
  paymentMethod.value = ''
}

function openTermsModal(type) {
  currentTermsType.value = type
  modalTerms.value = { ...terms.value }
  showTermsModal.value = true
  document.body.style.overflow = 'hidden'
}

function closeTermsModal() {
  showTermsModal.value = false
  document.body.style.overflow = ''
}

function setModalAllAgree(agree) {
  modalTerms.value.service = agree
  modalTerms.value.privacy = agree
}

function completeTermsModal() {
  terms.value = { ...modalTerms.value }
  agreeAll.value = terms.value.service && terms.value.privacy
  closeTermsModal()
}

function selectPaymentMethod(method) {
  if (method === 'card') {
    paymentMethod.value = method
  }
}

function handlePayment() {
  if (!canProceed.value) return
  if (paymentMethod.value === 'card') {
    showCardModal.value = true
    document.body.style.overflow = 'hidden'
  }
}

function closeCardModal() {
  showCardModal.value = false
  document.body.style.overflow = ''
}

async function submitPayment() {
  if (!isCardInfoValid.value) return
  if (!guardianNo.value) {
    alert('보호자 정보를 찾을 수 없습니다.')
    return
  }

  const serverPlan = selectedPlan.value === 'yearly' ? '12month' : '1month'
  try {
    const res = await fetch('/api/payments/confirm', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Mock-User': String(guardianNo.value)
      },
      credentials: 'include',
      body: JSON.stringify({
        selectedPlan: serverPlan,
        cardNumber: cardInfo.value.number,
        expiry: cardInfo.value.expiry,
        cvc: cardInfo.value.cvc,
        owner: cardInfo.value.holder,
        guardianNo: guardianNo.value
      })
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      alert(data?.message || `결제 실패 (${res.status})`)
      return
    }

    if (data?.status === 'PAID') {
      closeCardModal()
      closePaymentModal()
      alert('구독이 완료되었습니다.')
      router.push('/desktop/mypage')
    } else {
      alert(data?.message || '결제 처리 결과를 확인할 수 없습니다.')
    }
  } catch (e) {
    console.error(e)
    alert('결제 처리 중 오류가 발생했습니다.')
  }
}

onMounted(async () => {
  await fetchMe()
  await redirectIfPlus()
})
</script>

<style scoped>
.desktop-mypage-billing {
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

.plans-section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: #171717;
  margin: 0 0 24px 0;
}

.plans-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.plan-card {
  background: #ffffff;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 32px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.plan-card:hover {
  border-color: rgba(74, 98, 221, 0.5);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.plan-card.selected {
  border-color: rgba(74, 98, 221, 0.85);
  box-shadow: 0 0 0 3px rgba(74, 98, 221, 0.1);
}

.plan-card-popular {
  border-color: rgba(74, 98, 221, 0.85);
}

.popular-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  background: rgba(170, 194, 254, 0.91);
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.plan-header {
  margin-bottom: 20px;
}

.plan-name {
  font-size: 20px;
  font-weight: 600;
  color: #171717;
  margin: 0;
}

.plan-price {
  margin-bottom: 24px;
}

.price-amount {
  font-size: 32px;
  font-weight: 700;
  color: #171717;
}

.price-period {
  font-size: 16px;
  color: #737373;
  margin-left: 4px;
}

.original-price {
  text-decoration: line-through;
  color: #9ca3af;
  font-size: 14px;
  margin-bottom: 8px;
}

.discount-price {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.monthly-equivalent {
  font-size: 16px;
  color: #525252;
}

.plan-features {
  list-style: none;
  padding: 0;
  margin: 0;
}

.plan-features li {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 15px;
  color: #525252;
}

.plan-features li i {
  color: rgba(74, 98, 221, 0.85);
  font-size: 18px;
}

.footer-section {
  text-align: center;
  padding-top: 32px;
  border-top: 1px solid #e5e7eb;
}

.btn-primary {
  width: 100%;
  max-width: 400px;
  padding: 16px 32px;
  background: rgba(74, 98, 221, 0.85);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin: 0 auto 16px;
  display: block;
}

.btn-primary:hover:not(:disabled) {
  background: rgba(74, 98, 221, 1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.btn-primary:disabled {
  background: #d1d5db;
  cursor: not-allowed;
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

@media (max-width: 768px) {
  .plans-grid {
    grid-template-columns: 1fr;
  }
}

/* 결제 모달 */
.payment-modal-overlay {
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

.payment-modal-content,
.terms-modal-content,
.card-modal-content {
  background: #ffffff;
  border-radius: 12px;
  max-width: 600px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.payment-modal-header {
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

.payment-modal-body {
  padding: 24px;
}

.payment-section {
  margin-bottom: 32px;
}

.payment-section:last-child {
  margin-bottom: 0;
}

.section-title-small {
  font-size: 16px;
  font-weight: 600;
  color: #171717;
  margin: 0 0 16px 0;
}

/* 약관 동의 */
.all-agree-box {
  background: rgba(170, 194, 254, 0.91);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.agree-all-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #171717;
  cursor: pointer;
  margin: 0;
}

.terms-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.term-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.term-item:hover {
  border-color: rgba(74, 98, 221, 0.3);
}

.term-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #171717;
  cursor: pointer;
  margin: 0;
}

/* 선택된 플랜 카드 */
.selected-plan-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.plan-info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.plan-type {
  font-size: 15px;
  font-weight: 600;
  color: #171717;
}

.plan-price-text {
  font-size: 18px;
  color: #171717;
}

.plan-features-list {
  list-style: none;
  padding: 0;
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.plan-features-list li {
  margin-bottom: 4px;
}

/* 결제 수단 */
.payment-methods {
  margin-bottom: 20px;
}

.payment-method-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #ffffff;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.payment-method-item:hover {
  border-color: rgba(74, 98, 221, 0.3);
}

.payment-method-item.selected {
  background: #f8f9ff;
  border-color: rgba(74, 98, 221, 0.85);
}

.payment-method-item i {
  font-size: 20px;
  color: #6366f1;
}

.payment-method-item label {
  flex: 1;
  cursor: pointer;
  margin: 0;
  font-size: 15px;
  color: #171717;
}

/* 가격 요약 */
.price-summary {
  background: #fafafa;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.price-row.total {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
  color: #171717;
}

.total-price {
  color: rgba(74, 98, 221, 0.85);
  font-size: 18px;
}

.price-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 8px 0;
}

/* 결제 버튼 */
.btn-payment {
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
  margin-top: 24px;
}

.btn-payment:hover:not(:disabled) {
  background: rgba(74, 98, 221, 1);
}

.btn-payment:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.payment-note {
  text-align: center;
  font-size: 12px;
  color: #6b7280;
  margin: 12px 0 0 0;
}

/* 약관 모달 */
.terms-modal-content {
  max-width: 700px;
}

.terms-modal-body {
  padding: 24px;
}

.terms-content-section {
  margin-bottom: 24px;
}

.terms-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #171717;
}

.terms-text {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.terms-text p {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0;
}

.terms-agree-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.terms-agree-section label {
  font-size: 13px;
  color: #171717;
}

.radio-group {
  display: flex;
  gap: 16px;
}

.radio-group label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
}

.terms-all-agree {
  text-align: center;
  margin: 24px 0;
  padding: 16px 0;
  border-top: 1px solid #e5e7eb;
}

.terms-all-agree label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-complete {
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
}

.btn-complete:hover {
  background: rgba(74, 98, 221, 1);
}

/* 카드 모달 */
.card-modal-content {
  max-width: 500px;
}

.card-modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #171717;
  margin-bottom: 8px;
}

.form-control {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-control:focus {
  outline: none;
  border-color: rgba(74, 98, 221, 0.85);
  box-shadow: 0 0 0 3px rgba(74, 98, 221, 0.1);
}

.btn-card-payment {
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
  margin-bottom: 12px;
}

.btn-card-payment:hover:not(:disabled) {
  background: rgba(74, 98, 221, 1);
}

.btn-card-payment:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.btn-cancel {
  width: 100%;
  padding: 14px 24px;
  background: #ffffff;
  color: #6b7280;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f9fafb;
}
</style>

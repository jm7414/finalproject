<template>
  <div class="payment-page container px-3 py-3">
    <!-- 로고 헤더 -->
    <section class="text-center mb-3">
      <img :src="logoPath" alt="Logo" class="logo-icon mb-2" />
      <h5 class="fw-semibold mb-1">케어 플러스 서비스</h5>
      <p class="text-secondary small mb-0">더 나은 돌봄을 제공합니다.</p>
    </section>

    <hr class="divider" />

    <!-- 약관 동의 -->
    <section class="mb-3">
      <h6 class="mb-2 fw-semibold">서비스 이용약관</h6>

      <div class="all-agree-box rounded-3 p-3 mb-2">
        <div class="form-check">
          <input class="form-check-input" type="checkbox" id="agreeAll" v-model="agreeAll" @change="toggleAll">
          <label class="form-check-label" for="agreeAll">전체 동의</label>
        </div>
      </div>

      <div class="terms-list">
        <div class="term-item rounded-3 shadow-sm p-3 mb-2 d-flex align-items-center justify-content-between">
          <div class="form-check mb-0">
            <input class="form-check-input" type="checkbox" id="terms1" v-model="terms.service">
            <label class="form-check-label small" for="terms1">
              <span class="text-danger">[필수]</span> 서비스 이용약관
            </label>
          </div>
          <i class="bi bi-chevron-right text-secondary" @click="openTermsModal('service')" style="cursor:pointer;font-size:.9rem"></i>
        </div>

        <div class="term-item rounded-3 shadow-sm p-3 mb-2 d-flex align-items-center justify-content-between">
          <div class="form-check mb-0">
            <input class="form-check-input" type="checkbox" id="terms2" v-model="terms.privacy">
            <label class="form-check-label small" for="terms2">
              <span class="text-danger">[필수]</span> 개인정보 처리방침
            </label>
          </div>
          <i class="bi bi-chevron-right text-secondary" @click="openTermsModal('privacy')" style="cursor:pointer;font-size:.9rem"></i>
        </div>
      </div>
    </section>

    <!-- 결제 정보 -->
    <section class="mb-3">
      <h6 class="mb-2 fw-semibold">결제 정보</h6>

      <!-- 월간 플랜 -->
      <div v-if="selectedPlan === 'monthly'" class="plan-card card rounded-3 shadow-sm mb-2">
        <div class="card-body p-3">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="fw-semibold">플러스 플랜 월간</span>
            <strong class="plan-price">5,990원</strong>
          </div>
          <ul class="list-unstyled small text-secondary mb-0">
            <li>• 보호자 3명까지 연결</li>
            <li>• 강화된 분석 리포트</li>
            <li>• 가족 공유 기능</li>
          </ul>
        </div>
      </div>

      <!-- 연간 플랜 -->
      <div v-if="selectedPlan === 'yearly'" class="plan-card card rounded-3 shadow-sm mb-2">
        <div class="card-body p-3">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="fw-semibold">플러스 플랜 연간</span>
            <strong class="plan-price">50,320원</strong>
          </div>
          <ul class="list-unstyled small text-secondary mb-0">
            <li>• 보호자 3명까지 연결</li>
            <li>• 강화된 분석 리포트</li>
            <li>• 가족 공유 기능</li>
            <li>• 1년간 위치정보 기록</li>
          </ul>
        </div>
      </div>
    </section>

    <!-- 결제 수단 -->
    <section class="mb-3">
      <h6 class="mb-2 fw-semibold">결제 수단</h6>

      <div class="payment-methods">
        <div class="payment-item card rounded-3 shadow-sm p-3 mb-2 d-flex align-items-center"
             :class="{ 'payment-selected': paymentMethod === 'card' }"
             @click="selectPaymentMethod('card')">
          <input type="radio" name="paymentMethod" id="card" v-model="paymentMethod" value="card" class="me-2">
          <i class="bi bi-credit-card me-2"></i>
          <label for="card" class="flex-grow-1 mb-0" style="cursor:pointer">신용카드</label>
        </div>

        <div class="payment-item card rounded-3 shadow-sm p-3 mb-2 d-flex align-items-center">
          <input type="radio" disabled class="me-2">
          <i class="bi bi-bank me-2"></i>
          <span class="flex-grow-1">계좌이체</span>
        </div>

        <div class="payment-item card rounded-3 shadow-sm p-3 mb-2 d-flex align-items-center">
          <input type="radio" disabled class="me-2">
          <i class="bi bi-phone me-2"></i>
          <span class="flex-grow-1">휴대폰 결제</span>
        </div>
      </div>

      <div class="price-summary card rounded-3 shadow-sm p-3 mb-2">
        <div class="d-flex justify-content-between mb-2 small">
          <span class="text-secondary">월 이용료</span>
          <span>{{ monthlyPrice }}원</span>
        </div>
        <div class="d-flex justify-content-between mb-2 small">
          <span class="text-secondary">VAT</span>
          <span>{{ vat }}원</span>
        </div>
        <hr class="my-2" style="opacity:.2">
        <div class="d-flex justify-content-between align-items-center">
          <strong>총 결제금액</strong>
          <strong class="text-primary">{{ totalPrice }}원</strong>
        </div>
      </div>
    </section>

    <!-- 결제 버튼 -->
    <button class="btn btn-payment w-100 mb-2 py-2" @click="handlePayment" :disabled="!canProceed">결제하기</button>
    <p class="text-center text-secondary small mb-0">결제 시 위 약관에 모두 동의한 것으로 간주됩니다</p>

    <!-- 약관 정보 모달 (Teleport 사용) -->
    <Teleport to="body">
      <div v-if="showTermsModal" class="modal-overlay-global" @click.self="closeTermsModal">
        <div class="modal-content-terms card shadow-lg">
          <div class="modal-header-terms p-3 border-bottom d-flex align-items-center justify-content-between">
            <div class="modal-icon-wrap-terms"><i class="bi bi-check-circle"></i></div>
            <h6 class="fw-semibold mb-0 flex-grow-1 text-center">약관 정보</h6>
            <button class="btn-close-icon" @click="closeTermsModal"><i class="bi bi-x-lg"></i></button>
          </div>

          <div class="modal-body-terms p-3">
            <!-- 서비스 이용약관 -->
            <div class="mb-3">
              <div class="d-flex align-items-start mb-2">
                <span class="text-danger me-1">*</span>
                <strong class="small">서비스 이용약관</strong>
                <span class="text-danger ms-1">*</span>
              </div>
              <div class="terms-content p-3 mb-2 rounded-3">
                <p class="small text-secondary mb-0">
                  본 서비스는 회원가입 후 제공되며, 유료 구독 시 자동 결제가 진행됩니다.
                  사용자는 서비스 이용 중 발생하는 콘텐츠, 기능, 요금제 변경 등에 대해 안내를 받을 수 있으며,
                  서비스 이용은 약관에 동의한 것으로 간주됩니다.
                </p>
              </div>
              <div class="d-flex align-items-center gap-3 mb-2">
                <label class="small mb-0">서비스 이용약관 동의<span class="text-danger">*</span></label>
                <div class="d-flex gap-3 ms-auto">
                  <div class="form-check">
                    <input class="form-check-input" type="radio" name="modalServiceAgree" id="modalServiceYes" v-model="modalTerms.service" :value="true">
                    <label class="form-check-label small" for="modalServiceYes">예</label>
                  </div>
                  <div class="form-check">
                    <input class="form-check-input" type="radio" name="modalServiceAgree" id="modalServiceNo" v-model="modalTerms.service" :value="false">
                    <label class="form-check-label small" for="modalServiceNo">아니요</label>
                  </div>
                </div>
              </div>
            </div>

            <!-- 개인정보 처리방침 -->
            <div class="mb-3">
              <div class="d-flex align-items-start mb-2">
                <span class="text-danger me-1">*</span>
                <strong class="small">개인정보 처리방침</strong>
                <span class="text-danger ms-1">*</span>
              </div>
              <div class="terms-content p-3 mb-2 rounded-3">
                <p class="small text-secondary mb-0">
                  입력하신 개인정보는 회원 관리, 결제 처리, 고객 지원을 위해 수집되며,
                  관련 법령에 따라 안전하게 보호됩니다. 제3자 제공 또는 마케팅 활용 시 별도 동의를 받습니다.
                  언제든지 열람, 수정, 삭제를 요청할 수 있습니다.
                </p>
              </div>
              <div class="d-flex align-items-center gap-3 mb-3">
                <label class="small mb-0" style="letter-spacing: -0.5px;">개인정보 처리방침 동의<span class="text-danger">*</span></label>
                <div class="d-flex gap-3 ms-auto">
                  <div class="form-check">
                    <input class="form-check-input" type="radio" name="modalPrivacyAgree" id="modalPrivacyYes" v-model="modalTerms.privacy" :value="true">
                    <label class="form-check-label small" for="modalPrivacyYes">예</label>
                  </div>
                  <div class="form-check">
                    <input class="form-check-input" type="radio" name="modalPrivacyAgree" id="modalPrivacyNo" v-model="modalTerms.privacy" :value="false">
                    <label class="form-check-label small" for="modalPrivacyNo">아니요</label>
                  </div>
                </div>
              </div>
            </div>

            <!-- 모두 동의 -->
            <div class="text-center mb-3">
              <div class="form-check d-inline-block">
                <input class="form-check-input" type="radio" name="modalAllAgree" id="modalAllYes" :checked="modalTerms.service && modalTerms.privacy" @change="setModalAllAgree(true)">
                <label class="form-check-label small" for="modalAllYes">모두 동의</label>
              </div>
            </div>

            <button class="btn btn-complete w-100 py-2" @click="completeTermsModal">완료</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 카드 정보 모달 (Teleport 사용) -->
    <Teleport to="body">
      <div v-if="showCardModal" class="modal-overlay-global" @click.self="closeCardModal">
        <div class="modal-content-custom card shadow-lg">
          <div class="modal-header-custom d-flex align-items-center justify-content-between p-3 border-bottom">
            <div class="modal-icon-wrap"><i class="bi bi-flag"></i></div>
            <button class="btn-close-icon" @click="closeCardModal"><i class="bi bi-x-lg"></i></button>
          </div>

          <div class="modal-body-custom p-3">
            <h6 class="fw-semibold mb-3">결제정보</h6>

            <div class="mb-2">
              <label class="form-label small mb-1">카드번호<span class="text-danger">*</span></label>
              <input type="text" class="form-control form-control-sm" placeholder="카드번호" v-model="cardInfo.number" maxlength="19">
            </div>

            <div class="row mb-2">
              <div class="col-6">
                <label class="form-label small mb-1">유효기간<span class="text-danger">*</span></label>
                <input type="text" class="form-control form-control-sm" placeholder="MM/YY" v-model="cardInfo.expiry" maxlength="5">
              </div>
              <div class="col-6">
                <label class="form-label small mb-1">CVC번호<span class="text-danger">*</span></label>
                <input type="text" class="form-control form-control-sm" placeholder="3자리" v-model="cardInfo.cvc" maxlength="3">
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label small mb-1">카드 소유자명<span class="text-danger">*</span></label>
              <input type="text" class="form-control form-control-sm" placeholder="카드 소유자명" v-model="cardInfo.holder">
            </div>

            <button class="btn btn-card-payment w-100 mb-2 py-2" @click="submitPayment" :disabled="!isCardInfoValid">결제하기</button>
            <button class="btn btn-outline-secondary w-100 py-2" @click="closeCardModal">취소</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const logoPath = '/mammamialogo.png'

/** 상태 */
const agreeAll = ref(false)
const terms = ref({ service: false, privacy: false })
const selectedPlan = ref<'monthly'|'yearly'>('monthly')
const paymentMethod = ref('')
const showCardModal = ref(false)
const showTermsModal = ref(false)
const currentTermsType = ref<'service'|'privacy'|''>('')
const modalTerms = ref({ service: false, privacy: false })
const cardInfo = ref({ number: '', expiry: '', cvc: '', holder: '' })
const guardianNo = ref<number|null>(null)

/** plan 파싱 */
function parsePlanFromRoute() {
  const p = (route.query.plan || route.params.plan || '').toString()
  if (p === 'yearly' || p === '12month' || p === 'annual') selectedPlan.value = 'yearly'
  else selectedPlan.value = 'monthly'
}

onMounted(() => {
  parsePlanFromRoute()
  ensureGuardianNo()
})

/** 가격 요약 */
const monthlyPrice = computed(() => selectedPlan.value === 'monthly' ? '5,990' : '50,320')
const vat = computed(() => selectedPlan.value === 'monthly' ? '599' : '5,032')
const totalPrice = computed(() => selectedPlan.value === 'monthly' ? '6,589' : '55,352')

/** 약관/버튼 enable */
const canProceed = computed(() => terms.value.service && terms.value.privacy && paymentMethod.value)
const isCardInfoValid = computed(() =>
  cardInfo.value.number && cardInfo.value.expiry && cardInfo.value.cvc && cardInfo.value.holder
)

/** 전체동의 ↔ 개별동의 동기화 */
watch([() => terms.value.service, () => terms.value.privacy], () => {
  agreeAll.value = terms.value.service && terms.value.privacy
})
const toggleAll = () => {
  terms.value.service = agreeAll.value
  terms.value.privacy = agreeAll.value
}

/** 약관 모달 */
const openTermsModal = (type: 'service'|'privacy') => {
  currentTermsType.value = type
  modalTerms.value = { ...terms.value }
  showTermsModal.value = true
  document.body.style.overflow = 'hidden'
}
const closeTermsModal = () => {
  showTermsModal.value = false
  document.body.style.overflow = ''
}
const setModalAllAgree = (agree: boolean) => { 
  modalTerms.value.service = agree
  modalTerms.value.privacy = agree 
}
const completeTermsModal = () => {
  terms.value = { ...modalTerms.value }
  agreeAll.value = terms.value.service && terms.value.privacy
  closeTermsModal()
}

/** 결제수단 선택 */
const selectPaymentMethod = (method: string) => { 
  if (method === 'card') paymentMethod.value = method 
}

/** guardianNo 확보 */
async function ensureGuardianNo() {
  try {
    const res = await fetch('/api/user/me', { credentials: 'include' })
    const me = await res.json().catch(() => ({} as any))
    guardianNo.value = Number(me.userNo ?? me.userId ?? me.id ?? 0) || null
  } catch { 
    guardianNo.value = null 
  } finally {
    if (!guardianNo.value) {
      guardianNo.value = Number(localStorage.getItem('guardianNo') || 0) || null
    }
  }
}

/** 결제 → 카드 모달 오픈 */
const handlePayment = () => {
  if (!canProceed.value) return
  if (paymentMethod.value === 'card') {
    showCardModal.value = true
    document.body.style.overflow = 'hidden'
  }
}

/** 카드 모달 닫기 */
const closeCardModal = () => {
  showCardModal.value = false
  document.body.style.overflow = ''
}

/** 실제 결제 확정 */
const submitPayment = async () => {
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
    const data = await res.json().catch(() => ({} as any))
    if (!res.ok) { 
      alert(data?.message || `결제 실패 (${res.status})`)
      return 
    }

    if (data?.status === 'PAID') {
      closeCardModal()
      alert('구독이 완료 되었습니다.')
      router.replace('/gdmypage')
    } else {
      alert(data?.message || '결제 처리 결과를 확인할 수 없습니다.')
    }
  } catch (e) {
    console.error(e)
    alert('결제 처리 중 오류가 발생했습니다.')
  }
}
</script>

<style scoped>
/* ===== 색상 강제 ===== */
.payment-page { 
  color: #171717; 
}
.payment-page a { 
  color: inherit !important; 
}
.payment-page .text-secondary { 
  color: #6c757d !important; 
}

@media (min-width: 576px) {
  .payment-page.container {
    max-width: 600px;
    max-height: 1050px;
    transform: scale(0.9);
    transform-origin: top center;
  }
}

.logo-icon { 
  width: 48px; 
  height: 48px; 
  object-fit: contain; 
}
.divider { 
  opacity: .2; 
  margin: .5rem 0; 
}

.all-agree-box {
  background: rgba(170, 194, 254, 0.91);
  border: 1px solid rgba(170, 194, 254, 0.91);
}

.term-item, .payment-item, .plan-card {
  background: #fff;
  border: 1px solid rgba(0,0,0,.06);
  transition: all .2s;
}
.term-item { 
  cursor: pointer; 
}
.term-item:hover { 
  border-color: rgba(74, 98, 221, 0.3); 
}
.payment-item { 
  cursor: pointer; 
}
.payment-item:hover:not(:has(input:disabled)) { 
  border-color: rgba(74, 98, 221, 0.3); 
}

.payment-selected {
  background: #f8f9ff !important;
  border: 2px solid rgba(74, 98, 221, 0.85) !important;
}

.plan-price { 
  color: #171717; 
  font-size: 1.1rem; 
}

.price-summary { 
  background: #FAFAFA; 
  border: 1px solid #E5E5E5; 
}

.btn-payment {
  background: rgba(74, 98, 221, 0.85);
  color: #fff; 
  border: none; 
  font-weight: 600; 
  border-radius: 8px;
}
.btn-payment:hover:not(:disabled) { 
  background: rgba(74, 98, 221, 1); 
}
.btn-payment:disabled { 
  opacity: .5; 
  cursor: not-allowed; 
}

.btn-complete { 
  background: rgba(74,98,221,.85); 
  color: #fff; 
  border: none; 
  font-weight: 600; 
  border-radius: 8px; 
}
.btn-complete:hover { 
  background: rgba(74,98,221,1); 
}

.form-label { 
  font-size: .85rem; 
  font-weight: 500; 
}
.form-control-sm { 
  border: 1px solid #E5E5E5; 
  font-size: .85rem; 
}
.form-control:focus, .form-control-sm:focus {
  border-color: rgba(74,98,221,.85);
  box-shadow: 0 0 0 .2rem rgba(74,98,221,.15);
}

.btn-card-payment {
  background: rgba(170, 194, 254, 0.91);
  color: #fff;
  border: none;
  font-weight: 600;
  border-radius: 8px;
  transition: background .3s;
}
.btn-card-payment:not(:disabled) { 
  background: rgba(74,98,221,.85); 
}
.btn-card-payment:hover:not(:disabled) { 
  background: rgba(74,98,221,1); 
}

.btn-outline-secondary { 
  border-color: #dee2e6; 
  border-radius: 8px; 
}

.btn-close-icon { 
  background: none; 
  border: none; 
  cursor: pointer; 
  color: #666; 
  font-size: 1rem; 
}
.btn-close-icon:hover { 
  color: #000; 
}

.terms-content { 
  background: #fff; 
  border: 1px solid rgba(255,255,255,0.5); 
}
</style>

<style>
/* ===== 전역 모달 스타일 (Teleport용, scoped 아님) ===== */
.modal-overlay-global {
  position: fixed; 
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 375px;
  height: 812px;
  background: rgba(0,0,0,.5);
  display: flex; 
  align-items: center; 
  justify-content: center;
  z-index: 9999;
  padding: 1rem;
}

/* 약관 모달 */
.modal-content-terms {
  position: relative;
  width: 95%;
  max-width: 95%;
  max-height: 85vh;
  overflow-y: auto;
  border-radius: 14px;
  background: #D6DEFF;
  z-index: 10000;
}
.modal-header-terms {
  background: #D6DEFF;
  border-radius: 14px 14px 0 0;
  border-bottom-color: rgba(255,255,255,0.3) !important;
}
.modal-icon-wrap-terms {
  width: 40px;
  height: 40px;
  background: transparent;
  border: 2px solid #000;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #000;
  font-size: 1.2rem;
}
.modal-body-terms { 
  background: #D6DEFF; 
}

/* 카드 모달 */
.modal-content-custom {
  position: relative;
  width: 90%;
  max-width: 340px;
  max-height: 700px;
  overflow-y: auto;
  border-radius: 14px;
  background: #fff;
  z-index: 10000;
}
.modal-header-custom { 
  background: #fff; 
  border-radius: 14px 14px 0 0; 
}
.modal-icon-wrap {
  width: 40px;
  height: 40px;
  background: #EDF6FF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(74, 98, 221, 0.85);
  font-size: 1.2rem;
}
</style>

<template>
  <div class="plus-page container px-3 py-3" data-bs-theme="light">
    <!-- 인사 카드 -->
    <section class="greet-card shadow-sm rounded-4 d-flex align-items-center gap-3 p-3 mb-4">
      <div class="profile-circle d-flex align-items-center justify-content-center rounded-circle flex-shrink-0">
        <img :src="u('/figma/Frame.svg')" alt="profile" class="profile-icon" />
      </div>
      <div class="flex-grow-1">
        <div class="fw-semibold text-dark greet-title">
          {{ userName }} 님 안녕하세요!
        </div>
        <div class="text-secondary small">
          현재 BASIC 요금제 이용중
        </div>
      </div>
    </section>

    <!-- 혜택 카피 + 브랜드 아이콘 -->
    <section class="d-flex align-items-start justify-content-between mb-4">
      <p class="mt-2 benefit-copy">
        환자를 위한<br />더 많은 혜택을 제공합니다!
      </p>
      <img :src="u('/figma/logo.png')" alt="brand" class="brand-icon ms-3" />
    </section>

    <!-- 섹션 타이틀 -->
    <h5 class="fw-bold mb-5">PLUS 구독제</h5>

    <!-- 12개월 플랜(할인 배지 포함) -->
    <article class="plan-card position-relative mb-3" @click="selectPlan('12month')">
      <div class="ribbon-pill text-white text-center fw-semibold">
        30% 할인중
      </div>
      <div class="row gx-3 align-items-center">
        <div class="col">
          <div class="plan-name">12개월</div>
        </div>
        <div class="col-auto text-end">
          <div class="text-muted text-decoration-line-through old-price">￦ 71,880</div>
          <div class="new-price fw-bold">￦ 50,320</div>
          <div class="per-month">￦ 4,193/월</div>
        </div>
      </div>
    </article>

    <!-- 1개월 플랜 -->
    <article class="plan-card position-relative mb-4" @click="selectPlan('1month')">
      <div class="row gx-3 align-items-center">
        <div class="col">
          <div class="plan-name">1개월</div>
        </div>
        <div class="col-auto text-end">
          <div class="new-price fw-semibold">￦ 5,990/월</div>
        </div>
      </div>
    </article>

    <!-- CTA 버튼 -->
    <button type="button" class="btn btn-plus w-100 py-3 fw-semibold mb-3" @click="openPayment">
      PLUS 요금제 시작하기
    </button>

    <p class="text-muted mb-0" style="text-align: center; font-size: 0.75rem;">
      PLUS 요금제 사용중에도 언제든지 구독을 취소할 수 있습니다.
    </p>

    <!-- 모바일 최적화된 모달 -->
    <teleport to="body">
      <div v-if="paymentOpen" class="modal-backdrop" @click.self="closePayment">
        <div class="modal-container">
          <div class="modal-box">
            <div class="modal-header">
              <span class="icon-flag">🏳️</span>
              <h4 class="modal-title">결제정보</h4>
              <button class="close-btn" @click="closePayment">×</button>
            </div>

            <div class="modal-content">
              <form class="pay-form" @submit.prevent="handlePayment">
                <!-- 플랜 선택 -->
                <div class="form-group">
                  <label>구독 플랜 선택*</label>
                  <div class="plan-selector">
                    <label class="plan-option" :class="{ selected: form.selectedPlan === '12month' }">
                      <input type="radio" v-model="form.selectedPlan" value="12month" required />
                      <div class="plan-option-content">
                        <div class="plan-option-name">12개월</div>
                        <div class="plan-option-price">
                          <span class="original-price">￦71,880</span>
                          <span class="discount-price">￦50,320</span>
                          <span class="discount-badge">30% 할인</span>
                        </div>
                      </div>
                    </label>
                    <label class="plan-option" :class="{ selected: form.selectedPlan === '1month' }">
                      <input type="radio" v-model="form.selectedPlan" value="1month" required />
                      <div class="plan-option-content">
                        <div class="plan-option-name">1개월</div>
                        <div class="plan-option-price">
                          <span class="discount-price">￦5,990</span>
                        </div>
                      </div>
                    </label>
                  </div>
                </div>

                <div class="form-group">
                  <label>카드번호*</label>
                  <input v-model="form.cardNumber" type="text" placeholder="카드번호" required />
                </div>

                <div class="form-row">
                  <div class="form-group">
                    <label>유효기간*</label>
                    <input v-model="form.expiry" type="text" placeholder="MM/YY" required />
                  </div>
                  <div class="form-group">
                    <label>CVC번호*</label>
                    <input v-model="form.cvc" type="text" placeholder="CVC" required />
                  </div>
                </div>

                <div class="form-group">
                  <label>카드 소유자명*</label>
                  <input v-model="form.owner" type="text" placeholder="카드 소유자명" required />
                </div>

                <div class="agreement-section">
                  <div class="agreement-row">
                    <span class="agreement-text">서비스 이용약관 동의*</span>
                    <div class="radio-group">
                      <label class="radio-label">
                        <input type="radio" v-model="form.agreeTerm" :value="true" required /> 예
                      </label>
                      <label class="radio-label">
                        <input type="radio" v-model="form.agreeTerm" :value="false" /> 아니요
                      </label>
                    </div>
                    <button type="button" class="terms-btn" @click="openTerms">약관 보기</button>
                  </div>

                  <div class="agreement-row">
                    <span class="agreement-text">개인정보 처리방침 동의*</span>
                    <div class="radio-group">
                      <label class="radio-label">
                        <input type="radio" v-model="form.agreePrivacy" :value="true" required /> 예
                      </label>
                      <label class="radio-label">
                        <input type="radio" v-model="form.agreePrivacy" :value="false" /> 아니요
                      </label>
                    </div>
                  </div>
                </div>

                <div class="button-group">
                  <button type="submit" class="pay-btn">결제하기</button>
                  <button type="button" class="cancel-btn" @click="closePayment">취소</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </teleport>

    <!-- 약관 보기 팝업 -->
    <teleport to="body">
      <div v-if="termsOpen" class="modal-backdrop" @click.self="closeTerms">
        <div class="terms-container">
          <div class="terms-box">
            <div class="terms-header">
              <div class="check-icon">✓</div>
              <h5 class="terms-title">약관 정보</h5>
              <button class="close-btn" @click="closeTerms">×</button>
            </div>

            <div class="terms-content">
              <div class="terms-section">
                <h6 class="section-title">*서비스 이용약관*</h6>
                <div class="section-text-box">
                  <p class="section-text">
                    본 서비스는 회원가입 후 제공되며, 유료 구독 시 자동 결제가 진행됩니다. 사용자는 서비스 이용 중 발생하는 콘텐츠, 기능, 요금제 변경 등에 대해 안내를 받을 수 있으며, 서비스
                    이용은 약관에 동의한 것으로 간주됩니다.
                  </p>
                </div>

                <div class="option-label">서비스 이용약관 동의*</div>
                <div class="radio-options">
                  <label class="radio-item">
                    <input type="radio" v-model="termsForm.agreeTerm" :value="true" />
                    <span>예</span>
                  </label>
                  <label class="radio-item">
                    <input type="radio" v-model="termsForm.agreeTerm" :value="false" />
                    <span>아니요</span>
                  </label>
                </div>
              </div>

              <div class="terms-section">
                <h6 class="section-title">*개인정보 처리방침*</h6>
                <div class="section-text-box">
                  <p class="section-text">
                    입력하신 개인정보는 회원 관리, 결제 처리, 고객 지원을 위해 수집되며, 관련 법령에 따라 안전하게 보호됩니다. 제3자 제공 또는 마케팅 활용 시 별도 동의를 받습니다. 언제든지
                    열람, 수정, 삭제를 요청할 수 있습니다.
                  </p>
                </div>

                <div class="option-label">개인정보 처리방침 동의*</div>
                <div class="radio-options">
                  <label class="radio-item">
                    <input type="radio" v-model="termsForm.agreePrivacy" :value="true" />
                    <span>예</span>
                  </label>
                  <label class="radio-item">
                    <input type="radio" v-model="termsForm.agreePrivacy" :value="false" />
                    <span>아니요</span>
                  </label>
                </div>
              </div>
            </div>

            <div class="terms-footer">
              <button class="complete-btn" @click="completeTerms">완료</button>
            </div>
          </div>
        </div>
      </div>
    </teleport>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  userName?: string
}>()

const userName = props.userName ?? 'User'
const u = (p: string) => encodeURI(p)
const router = useRouter()

// 모달 제어
const paymentOpen = ref(false)
const termsOpen = ref(false)
const openPayment = () => paymentOpen.value = true
const closePayment = () => paymentOpen.value = false
const openTerms = () => termsOpen.value = true
const closeTerms = () => termsOpen.value = false

// 결제 폼
const form = reactive({
  selectedPlan: '12month' as '12month' | '1month',
  cardNumber: '',
  expiry: '',
  cvc: '',
  owner: '',
  agreeTerm: null as boolean | null,
  agreePrivacy: null as boolean | null
})

// 약관 팝업 폼
const termsForm = reactive({
  agreeTerm: null as boolean | null,
  agreePrivacy: null as boolean | null
})

// 플랜 선택
const selectPlan = (plan: '12month' | '1month') => {
  form.selectedPlan = plan
  openPayment()
}

// 약관 완료 → 메인 폼에 반영
const completeTerms = () => {
  if (termsForm.agreeTerm !== null) form.agreeTerm = termsForm.agreeTerm
  if (termsForm.agreePrivacy !== null) form.agreePrivacy = termsForm.agreePrivacy
  closeTerms()
}

// 결제 처리 (백엔드 호출)
const handlePayment = async () => {
  if (!form.agreeTerm || !form.agreePrivacy) {
    alert('서비스 이용약관과 개인정보 처리방침에 동의해주세요.')
    return
  }

  try {
    const res = await fetch('/api/payments/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',                      // 로그인 세션 포함
      body: JSON.stringify({
        selectedPlan: form.selectedPlan,
        agreeTerm: form.agreeTerm,
        agreePrivacy: form.agreePrivacy,
        // 필요시 카드정보도 전달(실결제 없으면 서버에서 무시)
        cardNumber: form.cardNumber,
        expiry: form.expiry,
        cvc: form.cvc,
        owner: form.owner
      })
    })

    const data = await res.json().catch(() => ({} as any))
    if (!res.ok) {
      alert(data?.message || `결제 실패 (${res.status})`)
      return
    }

    // 성공
    if (data?.status === 'PAID') {
      alert('결제가 완료되었습니다!')
      closePayment()
      router.push('/plusplan')
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
@media (min-width: 576px) {
  .plus-page.container {
    max-width: 480px;
  }
}

.greet-card {
  background: #fff;
  box-shadow: 0 10px 15px rgba(0, 0, 0, .10) !important;
}

.profile-circle {
  width: 50px;
  height: 50px;
  background: #DBEAFE;
}

.profile-icon {
  width: 18px;
  height: 20px;
  display: block;
}

.greet-title {
  font-size: 1.05rem;
}

.benefit-copy {
  font-size: 1.15rem;
  line-height: 1.35;
  color: #111;
  margin-right: .5rem;
}

.brand-icon {
  width: 72px;
  height: 72px;
  object-fit: contain;
  margin-right: 1rem;
}

.plan-card {
  background: #fff;
  border: 1px solid #808AFF;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, .15);
  padding: 18px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.plan-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, .2);
}

.ribbon-pill {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  top: -22px;
  background: #8E97FD;
  border-radius: 25px;
  min-width: 220px;
  padding: 10px 16px;
  font-size: 0.95rem;
}

.plan-name {
  font-size: 1.85rem;
  font-weight: 600;
  color: #000;
  text-shadow: 0 2px 2px rgba(0, 0, 0, .25);
}

.old-price {
  font-size: .95rem;
}

.new-price {
  font-size: 1.55rem;
}

.per-month {
  font-size: 1.0rem;
}

.btn-plus {
  background: rgba(125, 136, 255, .9);
  border: 1px solid #7F56D9;
  color: #fff;
  border-radius: 8px;
}

.btn-plus:hover {
  filter: brightness(0.96);
}

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  height: 100dvh;
  background: rgba(0, 0, 0, 0.6);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

.modal-container {
  width: 100%;
  max-width: 400px;
  max-height: 90vh;
  max-height: 90dvh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.modal-box {
  background: #fff;
  border-radius: 18px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  margin: auto;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-title {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0;
  flex: 1;
  text-align: center;
}

.icon-flag {
  font-size: 24px;
  margin-right: 8px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  padding: 20px 24px 24px 24px;
}

.pay-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-weight: 500;
  font-size: 14px;
  color: #333;
}

.form-group input[type="text"] {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  box-sizing: border-box;
}

.form-group input[type="text"]:focus {
  outline: none;
  border-color: #7d88ff;
  box-shadow: 0 0 0 2px rgba(125, 136, 255, 0.1);
}

.plan-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-option {
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  display: block;
}

.plan-option input[type="radio"] {
  position: absolute;
  opacity: 0;
}

.plan-option.selected {
  border-color: #7d88ff;
  background: rgba(125, 136, 255, 0.05);
}

.plan-option:hover {
  border-color: #b8c0ff;
}

.plan-option-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plan-option-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #000;
}

.plan-option-price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.original-price {
  font-size: 0.85rem;
  color: #999;
  text-decoration: line-through;
}

.discount-price {
  font-size: 1.1rem;
  font-weight: 600;
  color: #7d88ff;
}

.discount-badge {
  font-size: 0.75rem;
  color: #fff;
  background: #8E97FD;
  padding: 2px 8px;
  border-radius: 12px;
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-row .form-group {
  flex: 1;
}

.agreement-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 0;
}

.agreement-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.agreement-text {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.radio-group {
  display: flex;
  gap: 16px;
  align-items: center;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  cursor: pointer;
}

.radio-label input[type="radio"] {
  margin: 0;
}

.terms-btn {
  align-self: flex-start;
  font-size: 12px;
  color: #7d88ff;
  background: rgba(125, 136, 255, 0.1);
  border: none;
  border-radius: 6px;
  padding: 6px 12px;
  cursor: pointer;
  margin-top: 4px;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 20px;
}

.pay-btn {
  width: 100%;
  background: #7d88ff;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  border: none;
  border-radius: 8px;
  padding: 16px 0;
  cursor: pointer;
}

.pay-btn:hover {
  background: #6b7aff;
}

.cancel-btn {
  width: 100%;
  background: #f5f5f5;
  color: #666;
  font-weight: 500;
  font-size: 16px;
  border: none;
  border-radius: 8px;
  padding: 14px 0;
  cursor: pointer;
}

.cancel-btn:hover {
  background: #eeeeee;
}

.terms-container {
  width: 100%;
  max-width: 440px;
  margin: auto;
}

.terms-box {
  background: #E8E4F3;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
}

.terms-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 20px 16px 20px;
  position: relative;
}

.check-icon {
  position: absolute;
  left: 24px;
  width: 40px;
  height: 40px;
  background: #000;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: bold;
}

.terms-title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
  text-align: center;
  color: #000;
}

.terms-header .close-btn {
  position: absolute;
  right: 16px;
  color: #666;
  font-size: 32px;
}

.terms-content {
  padding: 24px;
  max-height: 65vh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.terms-section {
  margin-bottom: 24px;
}

.terms-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #000;
  margin: 0 0 12px 0;
}

.section-text-box {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.section-text {
  font-size: 0.85rem;
  line-height: 1.6;
  color: #444;
  margin: 0;
}

.option-label {
  font-size: 0.9rem;
  font-weight: 500;
  color: #000;
  margin-bottom: 8px;
}

.radio-options {
  display: flex;
  gap: 20px;
  align-items: center;
  margin-bottom: 4px;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  color: #333;
}

.radio-item input[type="radio"] {
  width: 20px;
  height: 20px;
  margin: 0;
  cursor: pointer;
  accent-color: #5B67CA;
}

.radio-item span {
  user-select: none;
}

.terms-footer {
  padding: 16px 24px 24px 24px;
  text-align: center;
}

.complete-btn {
  width: 100%;
  max-width: 280px;
  background: #5B67CA;
  color: #fff;
  font-weight: 600;
  font-size: 1rem;
  border: none;
  border-radius: 30px;
  padding: 14px 0;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(91, 103, 202, 0.3);
}

.complete-btn:hover {
  background: #4A56B9;
}

@media (max-height: 700px) and (orientation: portrait) {
  .modal-container {
    max-height: 95vh;
    max-height: 95dvh;
  }

  .modal-content {
    padding: 16px 20px 20px 20px;
  }

  .pay-form {
    gap: 12px;
  }

  .agreement-section {
    padding: 12px 0;
  }

  .terms-content {
    max-height: 60vh;
  }
}

@media (max-height: 500px) and (orientation: landscape) {
  .modal-backdrop {
    padding: 10px;
  }

  .modal-container {
    max-height: 98vh;
    max-height: 98dvh;
  }

  .terms-content {
    max-height: 55vh;
  }
}
</style>

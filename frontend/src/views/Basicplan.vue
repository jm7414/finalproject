<template>
  <div class="subscription-page bg-white">

    <!-- Profile Section -->
    <div class="profile-section bg-light py-2 mt-0">
      <div class="profile-container">
        <img :src="logoPath" alt="Logo" class="logo-icon mb-0" />
        <div class="d-flex align-items-center justify-content-center">
          <div class="user-avatar me-2">
            <i class="bi bi-person-circle text-secondary"></i>
          </div>
          <div class="text-start">
            <h6 class="mb-0 fw-normal">베이직 플랜 이용중</h6>
            <p class="text-secondary mb-0 small">{{ userName }}님 안녕하세요</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Pricing Plans Section -->
    <div class="pricing-section py-3">
      <div class="container">
        <h6 class="mb-3 fw-bold">구독 요금제</h6>

        <!-- Basic Monthly Plan -->
        <div class="card mb-2 border-primary" :class="{ 'border-primary border-3': selectedPlan === 'monthly' }"
          @click="selectPlan('monthly')" style="cursor: pointer;">
          <div class="card-body p-3">
            <p class="card-title mb-2 small">플러스 플랜 1개월</p>
            <div class="d-flex align-items-baseline mb-2">
              <h5 class="fw-bold mb-0">￦5,990</h5>
              <span class="text-secondary ms-1 small">/월</span>
            </div>
            <ul class="list-unstyled text-secondary mb-0" style="font-size: 0.8rem;">
              <li class="mb-1"><i class="bi bi-check text-secondary me-1"></i>보호자 3명까지 연결</li>
              <li class="mb-1"><i class="bi bi-check text-secondary me-1"></i>강화된 분석 리포트</li>
              <li class="mb-1"><i class="bi bi-check text-secondary me-1"></i>가족 공유 기능</li>
            </ul>
          </div>
        </div>

        <!-- Annual Plan (Most Popular) -->
        <div class="card border-primary border-2 position-relative mt-3"
          :class="{ 'border-3': selectedPlan === 'yearly' }" @click="selectPlan('yearly')"
          style="cursor: pointer; margin-bottom: 3px;">
          <span class="badge badge-popular position-absolute top-0 translate-middle-y ms-2" style="left: 0;">
            가장 인기
          </span>
          <span class="badge bg-light text-secondary position-absolute top-0 end-0 m-2 py-0 px-2"
            style="font-size: 0.7rem;">
            추천
          </span>
          <div class="card-body p-3 pt-4">
            <p class="card-title mb-2 small">플러스 플랜 1년</p>
            <div class="mb-1">
              <span class="text-decoration-line-through text-muted" style="font-size: 0.75rem;">￦ 71,880</span>
            </div>
            <div class="d-flex align-items-baseline mb-1">
              <h5 class="fw-bold mb-0">￦ 50,320</h5>
              <span class="text-secondary ms-1" style="font-size: 0.75rem;">/년</span>
            </div>
            <div class="mb-2">
              <span style="font-size: 0.85rem;">￦ 4,193/월</span>
            </div>
            <ul class="list-unstyled text-secondary mb-0" style="font-size: 0.8rem;">
              <li class="mb-1"><i class="bi bi-check text-secondary me-1"></i>30% 할인</li>
              <li class="mb-1"><i class="bi bi-check text-secondary me-1"></i>보호자 3명까지 연결</li>
              <li class="mb-1"><i class="bi bi-check text-secondary me-1"></i>강화된 분석 리포트</li>
              <li class="mb-1"><i class="bi bi-check text-secondary me-1"></i>가족 공유 기능</li>
              <li class="mb-1"><i class="bi bi-check text-secondary me-1"></i>1년간 위치정보 기록</li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer Section (sticky) -->
    <div class="footer-section border-top mt-auto">
      <div class="container py-2">
        <button class="btn btn-primary w-100 mb-2 py-2" @click="startSubscription">
          시작하기
        </button>
        <p class="text-center text-secondary mb-1" style="font-size: 0.7rem;">
          언제든지 구독을 취소할 수 있습니다
        </p>
        <div class="text-center" style="font-size: 0.7rem;">
          <a href="#" class="text-secondary text-decoration-underline me-2">이용약관</a>
          <a href="#" class="text-secondary text-decoration-underline">개인정보처리방침</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BasicPlan',
  data() {
    return {
      userName: '아무개',
      logoPath: '/mammamialogo.png',
      /** UI 값 (front_test 규격) */
      selectedPlan: '', // 'monthly' | 'yearly'
      /** 백엔드 연동용 */
      guardianNo: null
    }
  },
  methods: {
    goBack() { this.$router.go(-1) },
    selectPlan(plan) { this.selectedPlan = plan },
    /** Payment로 이동 — UI값 그대로 넘김(월간/연간 정확히 반영) */
    startSubscription() {
      if (!this.selectedPlan) { alert('플랜을 선택해주세요'); return }
      this.$router.push({ name: 'payment', query: { plan: this.selectedPlan } })
    },
    async fetchMe() {
      try {
        const res = await fetch('/api/user/me', { credentials: 'include' })
        const me = await res.json().catch(() => ({}))
        this.guardianNo = Number(me.userNo ?? me.userId ?? me.id ?? 0) || null
      } catch { this.guardianNo = null }
      finally {
        if (!this.guardianNo) {
          this.guardianNo = Number(localStorage.getItem('guardianNo') || 0) || null
        }
      }
    },
    /** 플러스면 Basicplan 진입 시 자동 분기 */
    async redirectIfPlus() {
      try {
        if (!this.guardianNo) return
        const headers = { 'X-Mock-User': String(this.guardianNo) }
        const res = await fetch('/api/subscriptions/summary', { method: 'GET', credentials: 'include', headers })
        const s = await res.json().catch(() => ({}))
        const plus = s?.plus === true || s?.subscriptionActive === true
        if (plus) this.$router.replace('/plusplan')
      } catch {/* ignore */ }
    }
  },
  async mounted() {
    await this.fetchMe()
    await this.redirectIfPlus()
  }
}
</script>

<style scoped>
/* ===== 색상 강제 (전역 초록색 무력화) ===== */
.subscription-page {
  color: #171717;
}

.subscription-page a {
  color: inherit !important;
}

.subscription-page .text-secondary {
  color: #6c757d !important;
}

/* ===== 레이아웃 ===== */
.subscription-page {
  min-height: 50vh;
  display: flex;
  flex-direction: column;
  max-width: 480px;
  margin: 0 auto;
  font-size: 0.9rem;
}

.header {
  background-color: white;
  border-bottom: 1px solid #E5E5E5;
}

.profile-section {
  background-color: #FAFAFA;
  padding-top: .5rem !important;
  padding-bottom: .5rem !important;
  margin-top: -18px !important; /* 값을 조정하여 원하는 만큼 공간 제거 */
  width: 100%;
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

/* 카드 섹션 아래 여백 줄이기 → 버튼이 멀리 내려가지 않도록 */
.pricing-section {
  flex: 1;
  padding-bottom: 0.5rem;
}

.card {
  border-radius: 8px;
  transition: all .2s;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, .1);
}

.badge {
  border-radius: 4px;
}

.badge-popular {
  background: rgba(170, 194, 254, .91);
  color: #fff;
  font-size: .75rem;
  padding: 3px 14px;
}

.btn-primary {
  background: rgba(74, 98, 221, .85);
  border: none;
  border-radius: 8px;
  font-size: .9rem;
}

.btn-primary:hover {
  background: rgba(74, 98, 221, 1);
}

.border-primary {
  border-color: rgba(170, 193, 254, .91) !important;
}

/* 시작하기 버튼 영역을 화면 하단에 sticky */
.footer-section {
  position: sticky;
  bottom: 0;
  background: #fff;
}
</style>

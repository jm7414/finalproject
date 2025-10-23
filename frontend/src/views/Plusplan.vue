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

        <button class="btn btn-cancel w-100 py-2" @click="cancelSubscription">
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

/** props */
const props = defineProps<{ userName?: string }>()

/** 라우터 */
const router = useRouter()

/** 로고/표시 값 */
const logoPath = '/mammamialogo.png'
const currentPlan = ref('플러스 플랜')
const nextPaymentDate = ref<string>('')

/** 세션 정보 */
const guardianNo = ref<number|null>(null)
const patientNo = ref<number|null>(null)

// 사용자 정보 저장
const userData = ref(null)
const subscriptionData = ref(null)

// Computed 속성
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
    const scan = (obj:any) => obj?.subscribe_endtime || 
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

/** 구독 취소 */
async function cancelSubscription() {
  if (!guardianNo.value) { 
    alert('보호자 정보를 찾을 수 없습니다.')
    return 
  }
  
  if (!confirm('정말 구독을 취소하시겠습니까?')) {
    return
  }
  
  try {
    const res = await fetch('/api/subscriptions/cancel', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type':'application/json',
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
.subscription-cancel-page { color: #171717; }
.subscription-cancel-page a { color: inherit !important; }
.subscription-cancel-page .text-secondary { color: #6c757d !important; }

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

.profile-container { width: 100%; text-align: center; padding: 0; margin: 0; }
.logo-icon { width: 40px; height: 40px; object-fit: contain; }

.user-avatar i { font-size: 40px; }
.user-avatar { border-radius: 50%; overflow: hidden; }

.plan-section { flex: 1; overflow-y: auto; }
.card { border-radius: 8px; }
.border-primary { border-color: rgba(74, 98, 221, 0.85) !important; }

.card-title { color: #171717; font-weight: 500; font-size: 1.4rem; }
.card-body { font-size: 0.9rem; }

.list-unstyled li { display: flex; align-items: center; }
.list-unstyled i { font-size: 1.1rem; }

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

.footer-section { background-color: white; border-top: 1px solid #E5E5E5; flex-shrink: 0; }

@media (max-width: 768px) {
  .container { padding-left: 16px; padding-right: 16px; }
}
</style>

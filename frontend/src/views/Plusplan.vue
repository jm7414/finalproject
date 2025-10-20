<template>
  <div class="subscription-cancel-page bg-white">
    <!-- 상단 프로필 -->
    <div class="profile-section bg-light py-2">
      <div class="profile-container">
        <img :src="logoPath" alt="Logo" class="logo-icon mb-2" />
        <div class="d-flex align-items-center justify-content-center">
          <div class="user-avatar me-2">
            <i class="bi bi-person-circle text-secondary"></i>
          </div>
          <div class="text-start">
            <h6 class="mb-0 fw-normal">{{ currentPlan }} 이용중</h6>
            <p class="text-secondary mb-0 small">{{ userName }}님 안녕하세요</p>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

/** props */
const props = defineProps<{ userName?: string }>()
const userName = props.userName ?? 'User'

/** 라우터 */
const router = useRouter()

/** 로고/표시 값 */
const logoPath = '/mamaicon.png'
const currentPlan = ref('플러스 플랜')        // 월/연간 식별이 어려우면 “플러스 플랜”으로 표기
const nextPaymentDate = ref<string>('')        // YYYY-MM-DD (KST)

/** 세션 정보 */
const guardianNo = ref<number|null>(null)
const patientNo = ref<number|null>(null)

/** 유틸: KST YYYY-MM-DD 포맷 */
function formatKstYmd(input: string | number | Date | undefined | null): string {
  if (!input) return ''
  const d = new Date(input)
  if (Number.isNaN(+d)) return ''
  // sv-SE 포맷은 YYYY-MM-DD, timeZone으로 KST 고정
  const ymd = new Intl.DateTimeFormat('sv-SE', {
    timeZone: 'Asia/Seoul',
    year: 'numeric', month: '2-digit', day: '2-digit'
  }).format(d) // e.g. "2026-10-15"
  return ymd
}

/** 내 정보→guardianNo 확보 (localStorage fallback) */
async function ensureGuardian() {
  try {
    const res = await fetch('/api/user/me', { credentials: 'include' })
    const me = await res.json().catch(() => ({} as any))
    guardianNo.value = Number(me.userNo ?? me.userId ?? me.id ?? 0) || null
  } catch { /* ignore */ }
  if (!guardianNo.value) {
    guardianNo.value = Number(localStorage.getItem('guardianNo') || 0) || null
  }
}

/** 구독 요약 → 연결/플러스 여부/환자번호 확보 */
async function fetchSummaryAndGuard() {
  if (!guardianNo.value) return
  const res = await fetch('/api/subscriptions/summary?guardianNo=' + guardianNo.value, {
    credentials: 'include',
    headers: { 'X-Mock-User': String(guardianNo.value) }
  })
  const s = await res.json().catch(() => ({} as any))
  // plus 아니면 베이직으로 보냄
  const plus = s?.plus === true || s?.subscriptionActive === true
  if (!plus) {
    router.replace('/basicplan')
    return
  }
  patientNo.value = s?.patientNo ?? null

  // 혹시 요약에 end가 내려오면 즉시 사용
  const endFromSummary = s?.subscription?.end || s?.end || s?.subscribe_endtime
  if (endFromSummary) {
    nextPaymentDate.value = formatKstYmd(endFromSummary)
  }
}

/** 환자 basic/guardians 등에서 subscribe_endtime 추출(백엔드 구현 다양성 대응) */
async function fetchEndtimeFallback() {
  if (!patientNo.value) return
  // 1) patient basic
  try {
    const resB = await fetch(`/api/connect/patient/${patientNo.value}/basic`, { credentials: 'include' })
    const b = await resB.json().catch(() => ({} as any))
    const end =
      b?.subscribe_endtime ||
      b?.subscription_endtime ||
      b?.subscribeEndtime ||
      b?.subscriptionEndTime ||
      b?.end
    if (end && !nextPaymentDate.value) {
      nextPaymentDate.value = formatKstYmd(end)
    }
  } catch { /* ignore */ }

  // 2) guardians 목록에 포함되어 오는 경우 방어적 파싱
  try {
    const resG = await fetch(`/api/connect/patient/${patientNo.value}/guardians`, { credentials: 'include' })
    const g = await resG.json().catch(() => [])
    // 배열/객체 어느 쪽이든 subscribe_endtime 비슷한 키를 탐색
    const scan = (obj:any) => obj?.subscribe_endtime || obj?.subscription_endtime || obj?.end || obj?.subscribeEndtime
    if (Array.isArray(g)) {
      for (const it of g) {
        const end = scan(it)
        if (end) { nextPaymentDate.value = formatKstYmd(end); break }
      }
    } else {
      const end = scan(g)
      if (end && !nextPaymentDate.value) nextPaymentDate.value = formatKstYmd(end)
    }
  } catch { /* ignore */ }
}

/** 구독 취소 (백엔드 준비되면 실제 동작) */
async function cancelSubscription() {
  if (!guardianNo.value) { alert('보호자 정보를 찾을 수 없습니다.'); return }
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
  } catch {
    // 백엔드가 아직 없을 수도 있음
    alert('구독 취소 API가 아직 준비되지 않았습니다. (백엔드 연결 필요)')
  }
}

onMounted(async () => {
  await ensureGuardian()
  await fetchSummaryAndGuard()
  if (!nextPaymentDate.value) await fetchEndtimeFallback()
})
</script>

<style scoped>
/* ===== 전역 초록 글씨 차단: 이 컴포넌트 내부에서는 기본색 강제 ===== */
.subscription-cancel-page { color: #171717; }
.subscription-cancel-page a { color: inherit !important; }
.subscription-cancel-page .text-secondary { color: #6c757d !important; }

/* 레이아웃 */
.subscription-cancel-page {
  height: 672px;
  max-width: 480px;
  margin: 0 auto;
  font-size: 0.9rem;
  transform: scale(0.9);
  transform-origin: top center;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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

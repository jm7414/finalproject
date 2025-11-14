<template>
  <div class="desktop-mypage">
    <div class="mypage-container">
      <!-- 상단 프로필 영역 -->
      <section class="profile-section">
        <div class="profile-card">
          <div class="profile-image-wrapper">
            <div class="profile-image" :style="frameStyle"></div>
          </div>
          <div class="profile-info">
            <h2 class="profile-name">{{ guardianName }} 님 안녕하세요</h2>
            <span class="profile-role">보호자 계정</span>
          </div>
        </div>
      </section>

      <!-- 메인 컨텐츠 영역 -->
      <div class="content-grid">
        <!-- 왼쪽: 메뉴 리스트 -->
        <section class="menu-section">
          <div class="menu-card">
            <!-- 내정보 수정 -->
            <div class="menu-item" @click="goToModifyInfo">
              <div class="menu-item-content">
                <div class="menu-icon">
                  <i class="bi bi-person-gear"></i>
                </div>
                <span class="menu-text">내정보 수정</span>
              </div>
              <i class="bi bi-chevron-right menu-arrow"></i>
            </div>

            <!-- 환자 초대코드 연결 -->
            <div class="menu-item" @click="goToConnect">
              <div class="menu-item-content">
                <div class="menu-icon">
                  <i class="bi bi-link-45deg"></i>
                </div>
                <span class="menu-text">환자 초대코드 연결</span>
              </div>
              <i class="bi bi-chevron-right menu-arrow"></i>
            </div>

            <!-- 환자 정보 관리 -->
            <div class="menu-item" @click="goToAdminDP">
              <div class="menu-item-content">
                <div class="menu-icon">
                  <i class="bi bi-people"></i>
                </div>
                <span class="menu-text">환자 정보 관리</span>
              </div>
              <i class="bi bi-chevron-right menu-arrow"></i>
            </div>

            <!-- 기본 안심존 설정 -->
            <div class="menu-item" @click="goToBasicSafeZone">
              <div class="menu-item-content">
                <div class="menu-icon">
                  <i class="bi bi-shield-check"></i>
                </div>
                <span class="menu-text">기본 안심존 설정</span>
              </div>
              <i class="bi bi-chevron-right menu-arrow"></i>
            </div>

            <!-- 구독 및 결제 관리 -->
            <div class="menu-item" @click="goToBilling">
              <div class="menu-item-content">
                <div class="menu-icon">
                  <i class="bi bi-credit-card"></i>
                </div>
                <span class="menu-text">구독 및 결제 관리</span>
              </div>
              <i class="bi bi-chevron-right menu-arrow"></i>
            </div>

            <!-- 로그아웃 -->
            <div class="menu-item logout-item" @click="handleLogout">
              <div class="menu-item-content">
                <div class="menu-icon logout-icon">
                  <i class="bi bi-box-arrow-right"></i>
                </div>
                <span class="menu-text logout-text">로그아웃</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 오른쪽: 구독 카드 및 환자 정보 -->
        <section class="subscription-section">
          <div class="subscription-card">
            <div class="subscription-header">
              <h3 class="subscription-title">구독 현황</h3>
              <div class="subscription-status-badge">
                <span :class="['status-text', { active: isActive }]">
                  {{ isActive ? '활성' : '비활성' }}
                </span>
              </div>
            </div>
            <div class="subscription-body">
              <div class="plan-info">
                <span class="plan-label">현재 플랜</span>
                <span class="plan-name">{{ currentPlan }}</span>
              </div>
              <div class="payment-info" v-if="isActive">
                <span class="payment-label">다음 결제일</span>
                <span class="payment-date">{{ nextPaymentDate }}</span>
              </div>
              
              <!-- 구독 활성 시 현재 이용 중인 혜택 -->
              <div class="benefits-preview" v-if="isActive">
                <div class="benefits-title">현재 이용 중인 혜택</div>
                <ul class="benefits-list">
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
                    <span>위치 기록 1년 보관</span>
                  </li>
                </ul>
              </div>
              
              <!-- 미구독 시 혜택 안내 -->
              <div class="benefits-preview" v-if="!isActive">
                <div class="benefits-title">플러스 플랜 혜택</div>
                <ul class="benefits-list">
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
                    <span>위치 기록 1년 보관</span>
                  </li>
                </ul>
                <button 
                  type="button" 
                  class="upgrade-button"
                  @click="goToBilling"
                >
                  플러스 플랜으로 업그레이드
                </button>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '@/utils/auth'
import axios from 'axios'

const router = useRouter()

// 사용자 정보
const userData = ref(null)
const subscriptionData = ref(null)
const profilePhotoUrl = ref('')

// Computed 속성
const guardianName = computed(() => {
  return userData.value?.name || 'User'
})

// 프로필 사진 스타일
const frameStyle = computed(() => {
  if (profilePhotoUrl.value) {
    return {
      backgroundImage: `url(${profilePhotoUrl.value})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center top'
    }
  }
  // 기본 이미지
  return {
    backgroundImage: 'url(https://codia-f2c.s3.us-west-1.amazonaws.com/image/2025-10-16/WtZK7A4Uep.png)',
    backgroundSize: 'cover',
    backgroundPosition: 'center'
  }
})

const subscriptionStatus = computed(() => {
  return userData.value?.subscriptionStatus || 0
})

const isActive = computed(() => {
  return subscriptionData.value?.subscriptionActive || false
})

const currentPlan = computed(() => {
  return subscriptionStatus.value === 1
    ? '플러스 플랜 이용중'
    : '베이직 플랜 이용중'
})

const nextPaymentDate = computed(() => {
  if (subscriptionStatus.value !== 1) {
    return '-'
  }

  if (!subscriptionData.value?.subscriptionEndTime) {
    return '-'
  }

  const date = new Date(subscriptionData.value.subscriptionEndTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}.${month}.${day}`
})


// 데이터 로드
const loadMyPageData = async () => {
  try {
    // 1. 사용자 기본 정보 조회
    const userResponse = await axios.get('/api/user/me')
    userData.value = userResponse.data

    // 프로필 사진 URL 저장
    profilePhotoUrl.value = userResponse.data.profilePhoto || ''

    // 2. 구독 정보 조회 (보호자인 경우)
    if (userData.value.roleNo === 1) {
      const subscriptionResponse = await axios.get('/api/subscriptions/summary', {
        params: {
          guardianNo: userData.value.userNo
        }
      })
      subscriptionData.value = subscriptionResponse.data

    }
  } catch (error) {
    console.error('마이페이지 데이터 로드 실패:', error)
    if (error.response?.status === 401) {
      alert('로그인이 필요합니다.')
      router.push('/desktop/login')
    } else {
      alert('사용자 정보를 불러오는데 실패했습니다.')
    }
  }
}

onMounted(() => {
  loadMyPageData()
})

// 네비게이션 함수들
const goToConnect = () => {
  router.push('/desktop/mypage/connect')
}

const goToModifyInfo = () => {
  router.push('/desktop/mypage/modify')
}

const goToAdminDP = () => {
  router.push('/desktop/mypage/admin')
}

const goToBilling = async () => {
  // 구독 상태 확인
  try {
    if (!userData.value?.userNo) {
      router.push('/desktop/mypage/billing')
      return
    }
    const headers = { 'X-Mock-User': String(userData.value.userNo) }
    const res = await fetch('/api/subscriptions/summary', { 
      method: 'GET', 
      credentials: 'include', 
      headers 
    })
    const s = await res.json().catch(() => ({}))
    const plus = s?.plus === true || s?.subscriptionActive === true
    if (plus) {
      router.push('/desktop/mypage/plusplan')
    } else {
      router.push('/desktop/mypage/billing')
    }
  } catch {
    router.push('/desktop/mypage/billing')
  }
}

const goToBasicSafeZone = () => {
  router.push('/desktop/mypage/safezone')
}

const handleLogout = async () => {
  const success = await logout()
  if (success) {
    router.push('/desktop/login')
  } else {
    alert('로그아웃에 실패했습니다.')
  }
}
</script>

<style scoped>
.desktop-mypage {
  width: 100%;
  min-height: calc(100vh - 80px);
  padding: 0;
}

.mypage-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

/* 프로필 섹션 */
.profile-section {
  margin-bottom: 32px;
}

.profile-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 32px;
  display: flex;
  align-items: center;
  gap: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.profile-image-wrapper {
  flex-shrink: 0;
}

.profile-image {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  background-repeat: no-repeat;
  background-position: center top;
  background-size: cover;
  border: 3px solid #e5e7eb;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.profile-name {
  font-size: 24px;
  font-weight: 700;
  color: #171717;
  margin: 0;
}

.profile-role {
  font-size: 14px;
  color: #737373;
}

/* 컨텐츠 그리드 */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 24px;
  align-items: start;
}

/* 메뉴 섹션 */
.menu-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.menu-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:hover {
  background-color: #f9fafb;
}

.menu-item.logout-item:hover {
  background-color: #fff5f5;
}

.menu-item-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(170, 193, 253, 0.91);
  border-radius: 8px;
  font-size: 20px;
  color: #171717;
}

.menu-icon.logout-icon {
  background: #ffe8e8;
  color: #ff6b6b;
}

.menu-text {
  font-size: 16px;
  font-weight: 500;
  color: #171717;
}

.menu-text.logout-text {
  color: #ff6b6b;
}

.menu-arrow {
  font-size: 16px;
  color: #6c757d;
}

.logout-item .menu-arrow {
  display: none;
}

/* 구독 섹션 */
.subscription-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.subscription-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 2px solid rgba(74, 98, 221, 0.85);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.subscription-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.subscription-title {
  font-size: 18px;
  font-weight: 600;
  color: #171717;
  margin: 0;
}

.subscription-status-badge {
  background: rgba(170, 193, 253, 0.91);
  border-radius: 9999px;
  padding: 4px 12px;
}

.status-text {
  font-size: 13px;
  font-weight: 600;
  color: #525252;
}

.status-text.active {
  color: #059669;
}

.subscription-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
}

.plan-info,
.payment-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.plan-label,
.payment-label {
  font-size: 14px;
  color: #525252;
}

.plan-name {
  font-size: 20px;
  font-weight: 600;
  color: #171717;
}

.payment-date {
  font-size: 16px;
  color: #737373;
}

/* 구독 혜택 미리보기 */
.benefits-preview {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.benefits-title {
  font-size: 14px;
  font-weight: 600;
  color: #171717;
  margin-bottom: 12px;
}

.benefits-list {
  list-style: none;
  padding: 0;
  margin: 0 0 16px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #525252;
}

.benefit-item i {
  color: #6366f1;
  font-size: 14px;
  flex-shrink: 0;
}

.upgrade-button {
  width: 100%;
  padding: 10px 16px;
  background: #6366f1;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
  margin-top: 8px;
}

.upgrade-button:hover {
  background: #4f46e5;
}

/* 반응형 */
@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .subscription-section {
    order: -1;
  }
}

@media (max-width: 768px) {
  .mypage-container {
    padding: 16px;
  }

  .profile-card {
    padding: 24px;
  }

  .profile-name {
    font-size: 20px;
  }
}
</style>

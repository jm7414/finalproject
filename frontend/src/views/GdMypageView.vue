<template>
  <div class="main-container">
    <div class="body">
      <div class="content">
        <div class="main-section">
          <!-- 상단 프로필 영역 -->
          <div class="content-section">
            <div class="content-1">
              <!-- 지현수정: 프로필 사진 동적 표시 -->
              <div class="image">
                <div class="frame" :style="frameStyle"></div>
              </div>
              <div class="flex-column">
                <div class="content-2">
                  <span class="hello-kim-protector">{{ guardianName }} 님 안녕하세요</span>
                </div>
                <span class="protector-account">보호자 계정</span>
              </div>
            </div>
            <div class="vector"></div>
          </div>

          <!-- 메뉴 리스트 영역 -->
          <div class="content-section-3">
            <!-- 내정보수정 -->
            <div class="content-4" style="cursor: pointer;" @click="goToModifyInfo">
              <div class="div">
                <div class="div-5">
                  <i class="bi bi-person-gear menu-bi"></i>
                </div>
                <span class="my-info-edit">내정보 수정</span>
              </div>
              <div class="icon-7">
                <i class="bi bi-chevron-right chevron-bi"></i>
              </div>
            </div>

            <!-- 환자 초대코드 연결 -->
            <div class="div-a" @click="goToGdc" style="cursor: pointer;">
              <div class="div-b">
                <div class="div-c">
                  <i class="bi bi-link-45deg menu-bi"></i>
                </div>
                <span class="patient-invite-code-connection">환자 초대코드 연결</span>
              </div>
              <div class="icon-gdc">
                <i class="bi bi-chevron-right chevron-bi"></i>
              </div>
            </div>

            <!-- 환자 정보 관리 -->
            <div class="div-12" style="cursor: pointer;" @click="goToAdminDP">
              <div class="div-13">
                <div class="div-14">
                  <i class="bi bi-people menu-bi"></i>
                </div>
                <span class="patient-info-management">환자 정보 관리</span>
              </div>
              <div class="i-18">
                <i class="bi bi-chevron-right chevron-bi"></i>
              </div>
            </div>

            <!-- 안심존 설정 -->
            <div class="div-1b" @click="goToBasicSafeZone" style="cursor: pointer;">
              <div class="div-1c">
                <div class="div-1d">
                  <i class="bi bi-shield-check menu-bi"></i>
                </div>
                <span class="safe-zone-setting">기본 안심존 설정</span>
              </div>
              <div class="i-21">
                <i class="bi bi-chevron-right chevron-bi"></i>
              </div>
            </div>

            <!-- 구독 및 결제 관리 -->
            <div class="div-24" @click="goToBilling" style="cursor: pointer;">
              <div class="div-25">
                <div class="div-26">
                  <i class="bi bi-credit-card menu-bi"></i>
                </div>
                <span class="subscribe-payment-management">구독 및 결제 관리</span>
              </div>
              <div class="i-2a">
                <i class="bi bi-chevron-right chevron-bi"></i>
              </div>
            </div>
          </div>

          <!-- 로그아웃 -->
          <div class="div-logout" @click="handleLogout" style="cursor: pointer;">
            <div class="div-logout-inner">
              <div class="div-logout-icon">
                <i class="bi bi-box-arrow-right menu-bi-logout"></i>
              </div>
              <span class="logout-text">로그아웃</span>
            </div>
          </div>

          <!-- 구독 카드 -->
          <div class="section">
            <div class="div-2d">
              <div class="div-2e">
                <span class="subscription-status">구독 현황</span>
                <div class="div-2f">
                  <div class="span">
                    <span class="active">{{ isActive ? '활 성' : '비활성' }}</span>
                  </div>
                </div>
              </div>
              <div class="div-30">
                <span class="current-plan">현재 플랜</span>
                <span class="plus-plan-active">{{ currentPlan }}</span>
              </div>
              <span class="next-payment-date">다음 결제일: {{ nextPaymentDate }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '@/utils/auth'
import axios from 'axios'

const router = useRouter()

// 사용자 정보
const userData = ref(null)
const subscriptionData = ref(null)

// 지현수정: 프로필 사진 URL 상태 추가
const profilePhotoUrl = ref('')

// Computed 속성
const guardianName = computed(() => {
  return userData.value?.name || 'User'
})

// 지현수정: 프로필 사진 스타일 (배경 이미지)
const frameStyle = computed(() => {
  if (profilePhotoUrl.value) {
    return {
      backgroundImage: `url(${profilePhotoUrl.value})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center top' // 상단 중앙 정렬
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

    // 지현수정: 프로필 사진 URL 저장
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
      router.push('/login')
    } else {
      alert('사용자 정보를 불러오는데 실패했습니다.')
    }
  }
}

onMounted(() => {
  loadMyPageData()
})

// 네비게이션 함수들
const goToGdc = () => {
  router.push('/gdc')
}

const goToModifyInfo = () => {
  router.push('/gdmodifyinfo')
}

const goToAdminDP = () => {
  router.push('/gdadmindp')
}

const goToBilling = () => {
  router.push('/basicplan')
}

const goToBasicSafeZone = () => {
  router.push({ name: 'basic-safe-zone-location' })
}

const handleLogout = async () => {
  const success = await logout()
  if (success) {
    router.push('/login')
  } else {
    alert('로그아웃에 실패했습니다.')
  }
}
</script>

<style scoped>
/* =========== 공통 & 레이아웃 =========== */
:root {
  --default-font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    Ubuntu, "Helvetica Neue", Helvetica, Arial, "PingFang SC",
    "Hiragino Sans GB", "Microsoft Yahei UI", "Microsoft Yahei",
    "Source Han Sans CN", sans-serif;
}

.main-container,
.main-container * {
  box-sizing: border-box;
}

.main-container {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
  max-width: 480px;
  max-height: 720px;
  margin-top: -30px;
  background: #fff;
  border: none;
  border-radius: 0;
  overflow: hidden;
}

.body,
.content,
.main-section,
.content-section,
.content-section-3,
.section {
  width: 100% !important;
  height: auto;
  overflow: visible;
}

input,
select,
textarea,
button {
  outline: 0;
}

/* =========== 상단 프로필 =========== */
.content-section {
  position: relative;
  background: #fff;
}

.content-1 {
  position: relative;
  width: calc(100% - 32px);
  height: 64px;
  margin: 24px 0 0 16px;
}

.image {
  position: absolute;
  top: 0;
  left: 0;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 지현수정: 프로필 이미지 스타일 - 상단 중앙 정렬 */
.frame {
  width: 64px;
  height: 64px;
  border-radius: 9999px;
  overflow: hidden;
  background-repeat: no-repeat;
  background-position: center top;
  background-size: cover;
}

.flex-column {
  position: absolute;
  top: 8px;
  left: 80px;
  width: calc(100% - 96px);
  height: 50px;
}

.content-2 {
  position: absolute;
  top: 0;
  left: 0;
  height: 28px;
  width: 100%;
}

.hello-kim-protector {
  color: #171717;
  font-family: Inter, var(--default-font-family);
  font-size: 20px;
  line-height: 28px;
  white-space: nowrap;
}

.protector-account {
  position: absolute;
  top: 28px;
  left: 0;
  color: #737373;
  font-size: 14px;
  line-height: 20px;
  white-space: nowrap;
}

.vector {
  width: calc(100% - 32px);
  height: 0;
  margin: 16px 0 0 16px;
}

/* =========== 메뉴 리스트 =========== */
.content-section-3 {
  position: relative;
  background: #fff;
}

.content-4,
.div-a,
.div-12,
.div-1b,
.div-24 {
  position: relative;
  width: calc(100% - 32px);
  height: 64px;
  margin: 6px 0 0 16px;
  border-top: 1px solid #d9d9d9;
}

.div-5,
.div-c,
.div-14,
.div-1d,
.div-26 {
  position: absolute;
  top: 12px;
  left: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(170, 193, 253, 0.91);
  border-radius: 8px;
}

.menu-bi {
  font-size: 20px;
  color: #171717;
  line-height: 1;
}

.my-info-edit,
.patient-invite-code-connection,
.patient-info-management,
.safe-zone-setting,
.subscribe-payment-management {
  position: absolute;
  top: 20px;
  left: 52px;
  color: #171717;
  font-size: 16px;
  line-height: 24px;
  white-space: nowrap;
}

.icon-7,
.icon-gdc,
.i-18,
.i-21,
.i-2a {
  position: absolute !important;
  right: 16px !important;
  top: 50% !important;
  transform: translateY(-50%) !important;
  width: 16px !important;
  height: 16px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.chevron-bi {
  font-size: 16px !important;
  color: #6c757d !important;
}

/* =========== 로그아웃 =========== */
.div-logout {
  position: relative;
  width: calc(100% - 32px);
  height: 64px;
  margin: 16px 0 0 16px;
  border-top: 2px solid #f0f0f0;
}

.div-logout:hover {
  background: #FFF5F5 !important;
}

.div-logout-inner {
  position: relative;
  width: 100%;
  height: 100%;
}

.div-logout-icon {
  position: absolute;
  top: 12px;
  left: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FFE8E8;
  border-radius: 8px;
}

.menu-bi-logout {
  font-size: 20px;
  color: #FF6B6B;
  line-height: 1;
}

.logout-text {
  position: absolute;
  top: 20px;
  left: 52px;
  color: #FF6B6B;
  font-size: 16px;
  line-height: 24px;
  white-space: nowrap;
  font-weight: 500;
}

/* =========== 구독 카드 =========== */
.section {
  position: relative;
}

.div-2d {
  position: relative;
  width: calc(100% - 32px);
  margin: 20px 0 16px 16px;
  background: #fafafa;
  border: 2px solid rgba(74, 98, 221, 0.85);
  border-radius: 8px;
  overflow: visible;
  min-height: 158px;
}

.div-2e {
  position: relative;
  width: calc(100% - 34px);
  height: 28px;
  margin: 16px 0 0 17px;
}

.subscription-status {
  position: absolute;
  top: 0;
  left: 0;
  font-size: 18px;
  line-height: 28px;
  color: #171717;
}

.div-2f {
  position: absolute;
  top: 2px;
  right: 0px;
  width: 50px;
  height: 24px;
  background: rgba(170, 193, 253, 0.91);
  border-radius: 9999px;
}

.span {
  position: relative;
  width: 24px;
  height: 19px;
  margin: 3px 0 0 12px;
}

.active {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-46%, -58%);
  color: #525252;
  font-size: 13px;
  font-weight: bold;
  line-height: 1;
  white-space: nowrap;
}

.div-30 {
  position: absolute;
  top: 56px;
  left: 16px;
  width: calc(100% - 32px);
  font-size: 0;
}

.current-plan {
  display: block;
  color: #525252;
  font-size: 14px;
  line-height: 20px;
}

.plus-plan-active {
  display: block;
  margin-top: 6px;
  color: #171717;
  font-size: 20px;
  line-height: 28px;
}

.next-payment-date {
  position: absolute;
  top: 120px;
  left: 16px;
  color: #737373;
  font-size: 14px;
  line-height: 20px;
  white-space: nowrap;
}
</style>

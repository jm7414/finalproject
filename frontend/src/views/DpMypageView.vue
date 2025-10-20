<template>
  <div class="my-info-page bg-light">
    <!-- Header Section -->
    <div class="header-section bg-white border-bottom">
      <div class="container py-3">
        <div class="d-flex align-items-center">
          <div class="user-avatar me-3">
            <i class="bi bi-person-circle text-secondary"></i>
          </div>
          <div>
            <h5 class="mb-1">내 정보</h5>
            <p class="text-secondary mb-0 small">{{ userName }}님 안녕하세요</p>
          </div>
        </div>
      </div>
      <div class="divider mx-3"></div>
    </div>

    <!-- Menu Grid Section -->
    <div class="menu-section py-4">
      <div class="container">
        <div class="row g-3">
          <!-- 일정 추가하기 - Large Card (Left) -->
          <div class="col-7">
            <div class="menu-card menu-card-large h-100" @click="addSchedule">
              <div class="card-content">
                <div class="icon-wrapper mb-3">
                  <i class="bi bi-calendar-plus-fill"></i>
                </div>
                <h5 class="card-title">일정 추가<br/>하기</h5>
              </div>
            </div>
          </div>

          <!-- Right Column -->
          <div class="col-5">
            <div class="row g-3">
              <!-- 내 정보 수정 -->
              <div class="col-12">
                <div class="menu-card menu-card-small" @click="editProfile">
                  <div class="card-content">
                    <div class="icon-wrapper mb-2">
                      <i class="bi bi-person-fill-gear"></i>
                    </div>
                    <p class="card-title small mb-0">내 정보<br/>수정</p>
                  </div>
                </div>
              </div>

              <!-- 보호자 연결코드 -->
              <div class="col-12">
                <div class="menu-card menu-card-small" @click="showGuardianCode">
                  <div class="card-content">
                    <div class="icon-wrapper mb-2">
                      <i class="bi bi-link-45deg"></i>
                    </div>
                    <p class="card-title small mb-0">보호자<br/>연결코드</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 로그아웃 버튼 -->
        <div class="logout-section mt-3">
          <button class="btn btn-logout w-100" @click="handleLogout">
            <div class="d-flex align-items-center justify-content-between">
              <div class="d-flex align-items-center">
                <i class="bi bi-box-arrow-right me-3"></i>
                <span>로그아웃</span>
              </div>
              <i class="bi bi-chevron-right"></i>
            </div>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '@/utils/auth'

const userName = ref('아무개')
const router = useRouter()

const emit = defineEmits(['close'])

// 일정 추가하기
const addSchedule = () => {
  console.log('일정 추가하기')
  // 일정 추가 로직 또는 라우팅
  router.push('DP_schedule')
}

// 내 정보 수정
const editProfile = () => {
  console.log('내 정보 수정')
  // 프로필 수정 페이지로 이동
  // router.push('/profile/edit')
}

// 보호자 연결코드
const showGuardianCode = () => {
  console.log('보호자 연결코드')
  // 보호자 연결코드 페이지로 이동
  // router.push('/guardian/code')
}

// 로그아웃 처리 (1번 코드 로직)
const handleLogout = async () => {
  const success = await logout()
  
  if (success) {
    // 모달 닫기
    emit('close')
    
    // 로그인 페이지로 이동
    router.push('/login')
  } else {
    alert('로그아웃에 실패했습니다.')
  }
}
</script>

<style scoped>
.my-info-page {
  height: 672px;
  max-width: 480px;
  margin: 0 auto;
  font-size: 0.9rem;
  transform: scale(0.9);
  transform-origin: top center;
  overflow: hidden;
  color: #171717 !important; /* 전체 텍스트 검정색 강제 */
}

.header-section {
  background-color: white;
  border-bottom: 1px solid #E5E5E5;
}

.header-section h5 {
  color: #171717 !important; /* 제목 검정색 */
}

.header-section p {
  color: #6c757d !important; /* 부제목 회색 */
}

.user-avatar i {
  font-size: 58px;
  color: #6c757d !important; /* 아이콘 회색 */
}

.divider {
  height: 1px;
  background-color: #E5E5E5;
}

.menu-section {
  padding-top: 1.8rem;
  padding-bottom: 1.8rem;
}

.menu-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #E5E5E5;
  box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  overflow: hidden;
}

.menu-card:hover {
  transform: translateY(-2px);
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1);
}

.menu-card-large {
  min-height: 370px;
}

.menu-card-small {
  min-height: 178px;
}

.menu-card .card-content {
  padding: 1.35rem;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.menu-card-large .card-content {
  padding: 2.7rem 1.8rem;
}

.icon-wrapper {
  color: rgba(74, 98, 221, 0.85) !important; /* 아이콘 파란색 유지 */
}

.menu-card-large .icon-wrapper i {
  font-size: 54px;
}

.menu-card-small .icon-wrapper i {
  font-size: 43px;
}

.card-title {
  color: #171717 !important; /* 카드 제목 검정색 강제 */
  font-weight: 400;
  line-height: 1.6;
}

.menu-card-large .card-title {
  font-size: 1.35rem;
}

.menu-card-small .card-title {
  font-size: 1.13rem;
}

.logout-section {
  margin-top: 1.35rem;
}

.btn-logout {
  background-color: rgba(170, 194, 254, 0.91);
  border: 1px solid #E5E5E5;
  border-radius: 16px;
  box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
  padding: 1.35rem;
  color: #737373 !important; /* 로그아웃 버튼 텍스트 회색 */
  font-size: 1.13rem;
  transition: all 0.2s;
}

.btn-logout:hover {
  background-color: rgba(170, 194, 254, 1);
  transform: translateY(-1px);
  box-shadow: 0px 3px 6px rgba(0, 0, 0, 0.1);
}

.btn-logout span {
  color: #737373 !important; /* 로그아웃 텍스트 회색 */
}

.btn-logout i {
  font-size: 1.35rem;
  color: #737373 !important; /* 로그아웃 아이콘 회색 */
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .container {
    padding-left: 1.35rem;
    padding-right: 1.35rem;
  }
}
</style>


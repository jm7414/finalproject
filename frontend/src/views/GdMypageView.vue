<template>
  <div class="mypage container px-3 py-3">
    <!-- 제목: 헤더/푸터 감안해 컴팩트 -->
    <h2 class="text-center fw-semibold title mb-3">마이페이지</h2>

    <!-- 프로필 (카메라 배지 제거) -->
    <section class="d-flex align-items-center gap-3 mb-3">
      <div class="avatar-ring rounded-circle">
        <img :src="u('/figma/Rectangle107.png')" alt="아바타" class="avatar-img rounded-circle" />
      </div>
      <p class="greeting mb-0 text-center" style="margin-left:50px;"> 홍길동님 안녕하세요 </p>
    </section>

    <hr class="divider" />

    <!-- 메뉴: list-group (라이트 톤 고정) -->
    <div class="list-group rounded-3 shadow-sm my-2 mypage-menu" data-bs-theme="light">

      <!-- 내 정보 수정 -->
      <div class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3">
        <span class="menu-icon" style="background:#FFF4DE">
          <img :src="u('/figma/Bold Duotone/Essentional, UI/Question Circle.svg')" alt="내 정보 수정" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">내 정보 수정</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight`.svg')" alt="" class="chev-img" />
      </div>

      <!-- 환자 QR 연결 -->
      <div class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3">
        <span class="menu-icon" style="background:#EAF3FF">
          <img :src="u('/figma/qr.svg')" alt="환자 QR 연결" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">환자 QR 연결</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight.svg')" alt="" class="chev-img" />
      </div>

      <!-- 환자 정보 관리 -->
      <div class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3">
        <span class="menu-icon" style="background:#FFE2E2">
          <img :src="u('/figma/Bold Duotone/Security/Shield Star.svg')" alt="환자 정보 관리" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">환자 정보 관리</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight.svg')" alt="" class="chev-img" />
      </div>

      <!-- 안심존 설정 -->
      <div class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3">
        <span class="menu-icon" style="background:#EDF6FF">
          <img :src="u('/figma/Bold Duotone/Security/Shield Star-1.svg')" alt="안심존 설정" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">안심존 설정</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight.svg')" alt="" class="chev-img" />
      </div>

      <!-- 구독 및 결제 관리 -->
      <div class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3" @click="goToBilling" style="cursor: pointer;">
        <span class="menu-icon" style="background:#EFF9EF">
          <img :src="u('/figma/Bold Duotone/Settings, Fine Tuning/Settings.svg')" alt="구독 및 결제 관리" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">구독 및 결제 관리</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight.svg')" alt="" class="chev-img" />
      </div>

      <!-- 로그아웃 -->
      <div class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3 logout-item" @click="handleLogout" style="cursor: pointer;">
        <span class="menu-icon" style="background:#FFE8E8">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#FF6B6B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
        </span>
        <span class="flex-grow-1 menu-text logout-text">로그아웃</span>
      </div>
    </div>

    <!-- 구독 카드 (디테일 업) -->
    <section class="card sub-card border-0 shadow-sm my-3">
      <div class="card-body d-flex align-items-center py-3">
        <!-- 아이콘 + 배경톤 -->
        <div class="rounded-circle d-flex align-items-center justify-content-center me-3 sub-icon-wrap">
          <img :src="u('/figma/Star.svg')" alt="star" class="sub-icon" />
        </div>

        <!-- 텍스트: 또렷하게 -->
        <div class="flex-grow-1">
          <div class="fw-bold text-dark sub-title-strong">현재 BASIC 이용중</div>
          <div class="text-secondary sub-date">다음 결제일&nbsp;2025.10.18</div>
        </div>

        <!-- 버튼 -->
        <button type="button" class="btn btn-upgrade rounded-pill px-3 py-1">
          PLUS 구독하기
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { logout } from '@/utils/auth'

/** 공백/쉼표 포함 경로 안전 처리 */
const u = (p: string) => encodeURI(p)

const router = useRouter()

// 구독 및 결제 관리 페이지로 이동
const goToBilling = () => {
  router.push('/basicplan')
}

// 로그아웃 처리
const handleLogout = async () => {
  const success = await logout()
  
  if (success) {
    // 로그인 페이지로 이동
    router.push('/login')
  } else {
    alert('로그아웃에 실패했습니다.')
  }
}
</script>

<style scoped>
/* 화면을 한 뷰에 담기 위한 콤팩트 설정 */
@media (min-width: 576px) {
  .mypage.container {
    max-width: 480px; /* 필요시 460~500 사이로 조정 */
  }
}

.title {
  font-size: 1.6rem; /* 헤더(App.vue에서 추가될 예정이라 약간 작게 */
  line-height: 1.2;
  margin-top: .25rem;
  margin-bottom: .5rem;
  text-align: center;
  font-weight: 600;
}

.divider {
  opacity: .2;
  margin-left: .25rem;
  margin-right: .25rem;
}

/* 메뉴 리스트 */
.list-group-item {
  border: none;
  border-bottom: 1px solid rgba(0, 0, 0, .06);
  background: #fff;
}
.list-group-item:last-child {
  border-bottom: none;
}
.list-group-item:active {
  background: #f8f9fa;
}

/* 메뉴 아이콘 공통 */
.menu-icon {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.menu-text {
  font-size: 1.05rem;
  color: #333;
  font-weight: 400;
}

/* 프로필 */
.avatar-ring {
  width: 92px;
  height: 92px;
  padding: 3px;
  border: 3px solid #FDC300;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-ring img {
  border-radius: 50%;
  object-fit: cover;
  width: 84px;
  height: 84px;
}

/* 구독 카드 */
:root {
  --sub-card-bg: #F2EAFB; /* 연보라 (피그마 근접) */
  --sub-card-bd: #E3D8F4;
}

.sub-card {
  background: var(--sub-card-bg);
  border: 1px solid var(--sub-card-bd);
  border-radius: 14px;
  margin-top: 1rem;
}

.sub-card .card-body {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
}

.sub-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 9999px;
  background: var(--sub-card-bg); /* 카드와 동일 배경으로 통일 */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 0.5rem;
}
.sub-icon {
  width: 18px;
  height: 18px;
  display: block;
}

.sub-title-strong {
  font-size: 1.02rem;
  color: #212529;
  font-weight: 600;
}
.sub-date {
  font-size: .9rem;
  color: #61646A !important;
}

/* 버튼 */
.btn-upgrade {
  background: #4F378A;
  color: #fff;
  font-weight: 700;
  border-radius: 9999px;
}
.btn-upgrade:hover {
  background: #3b2c6a;
}
/* ✨ 카드 배경이 흰색으로 돌아가는 문제 방지 */
.sub-card{
  /* 이 카드 안에서만 쓰는 전용 변수 */
  --sub-card-bg: #F2EAFB;   /* 연보라 */
  --sub-card-bd: #E3D8F4;

  /* Bootstrap이 읽는 카드 배경 변수도 같이 세팅 */
  --bs-card-bg: var(--sub-card-bg);

  /* 실제 배경/테두리도 명시적으로 지정 */
  background-color: var(--sub-card-bg) !important;
  border: 1px solid var(--sub-card-bd) !important;
  border-radius: 14px;
}

/* 아이콘 칩 배경도 카드와 동일 톤으로 */
.sub-icon-wrap{
  width: 32px; height: 32px; border-radius: 9999px;
  background-color: var(--sub-card-bg) !important;
  display: inline-flex; align-items: center; justify-content: center;
}

.sub-icon{ width: 18px; height: 18px; display: block; }

.icon-img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

/* 로그아웃 메뉴 스타일 */
.logout-item {
  border-top: 2px solid #f0f0f0;
  margin-top: 8px;
}

.logout-item:hover {
  background: #FFF5F5 !important;
}

.logout-text {
  color: #FF6B6B;
  font-weight: 500;
}

</style>


<template>
  <div class="mypage container px-3 py-3">
    <!-- 제목: 헤더/푸터 감안해 컴팩트 -->
    <h2 class="text-center fw-semibold title mb-3">마이페이지</h2>

    <!-- 프로필 (카메라 배지 제거) -->
    <section class="d-flex align-items-center gap-3 mb-3">
      <div class="avatar-ring rounded-circle">
        <img :src="u('/figma/Rectangle107.png')" alt="아바타" class="avatar-img rounded-circle" />
      </div>
      <p class="greeting mb-0 text-center" style="margin-left:50px;"> {{ userName }}님 안녕하세요 </p>
    </section>

    <hr class="divider" />

    <!-- 메뉴: list-group (라이트 톤 고정) -->
    <div class="list-group rounded-3 shadow-sm my-2 mypage-menu" data-bs-theme="light">

      <!-- 내 정보 수정 -->
      <button
        type="button"
        class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3"
        @click="go('edit-info')"
        @keydown.enter.prevent="go('edit-info')"
        @keydown.space.prevent="go('edit-info')"
      >
        <span class="menu-icon" style="background:#FFF4DE">
          <img :src="u('/figma/Bold Duotone/Essentional, UI/Question Circle.svg')" alt="내 정보 수정" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">내 정보 수정</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight`.svg')" alt="" class="chev-img" />
      </button>

      <!-- 환자 QR 연결 -->
      <button
        type="button"
        class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3"
        @click="go('qr-connect')"
        @keydown.enter.prevent="go('qr-connect')"
        @keydown.space.prevent="go('qr-connect')"
      >
        <span class="menu-icon" style="background:#EAF3FF">
          <img :src="u('/figma/qr.svg')" alt="환자 QR 연결" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">환자 QR 연결</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight.svg')" alt="" class="chev-img" />
      </button>

      <!-- 환자 정보 관리 -->
      <button
        type="button"
        class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3"
        @click="go('manage-patient')"
        @keydown.enter.prevent="go('manage-patient')"
        @keydown.space.prevent="go('manage-patient')"
      >
        <span class="menu-icon" style="background:#FFE2E2">
          <img :src="u('/figma/Bold Duotone/Security/Shield Star.svg')" alt="환자 정보 관리" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">환자 정보 관리</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight.svg')" alt="" class="chev-img" />
      </button>

      <!-- 안심존 설정 (정확한 파일명 + 폴백) -->
      <button
        type="button"
        class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3"
        @click="go('safezone')"
        @keydown.enter.prevent="go('safezone')"
        @keydown.space.prevent="go('safezone')"
      >
        <span class="menu-icon" style="background:#EDF6FF">
          <img :src="safeIconSrc" @error="onSafeIconError" alt="안심존 설정" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">안심존 설정</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight.svg')" alt="" class="chev-img" />
      </button>

      <!-- 구독 및 결제 관리 -->
      <button
        type="button"
        class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3"
        @click="go('billing')"
        @keydown.enter.prevent="go('billing')"
        @keydown.space.prevent="go('billing')"
      >
        <span class="menu-icon" style="background:#EFF9EF">
          <img :src="u('/figma/Bold Duotone/Settings, Fine Tuning/Settings.svg')" alt="구독 및 결제 관리" class="icon-img" />
        </span>
        <span class="flex-grow-1 menu-text">구독 및 결제 관리</span>
        <img :src="u('/figma/Phosphor Icons Regular/CaretRight.svg')" alt="" class="chev-img" />
      </button>
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
          <div class="fw-bold text-dark sub-title-strong">현재 {{ plan }} 이용중</div>
          <div class="text-secondary sub-date">다음 결제일&nbsp;{{ formattedBilling }}</div>
        </div>

        <!-- 버튼 -->
        <button type="button" class="btn btn-upgrade rounded-pill px-3 py-1" @click="go('upgrade')">
          PLUS 구독하기
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps, ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  userName: { type: String, default: 'OOO' },
  plan: { type: String, default: 'BASIC' },
  nextBilling: { type: String, default: '2025-10-18' },
})

const router = useRouter()
function go(name: string) {
  try { router.push({ name }) } catch { console.log('Clicked:', name) }
}

/** 공백/쉼표 포함 경로 안전 처리 */
const u = (p: string) => encodeURI(p)

/** 안심존 아이콘: 정확한 파일명 + 폴백 */
const safeIconPrimary = u('/figma/Bold Duotone/Security/Shield Star-1.svg')
const safeIconFallback = u('/figma/Bold Duotone/Security/ShielStar-1.svg')
const safeIconSrc = ref(safeIconPrimary)
function onSafeIconError(e: Event) {
  if ((e.target as HTMLImageElement).src !== safeIconFallback) {
    (e.target as HTMLImageElement).src = safeIconFallback
  }
}

const formattedBilling = computed(() => {
  const d = new Date(props.nextBilling)
  if (isNaN(d.getTime())) return props.nextBilling
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`
})
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


</style>


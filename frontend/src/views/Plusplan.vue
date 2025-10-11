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
        <div class="text-secondary small">현재 PLUS 요금제 이용중</div>
      </div>
    </section>

    <!-- 카피 + 브랜드 아이콘 -->
    <section class="d-flex align-items-center justify-content-between mb-4 ms-2">
  <p class="benefit-copy mb-0">
    PLUS 요금제<br />
    혜택을 이용중입니다.
  </p>
  <img :src="u('/figma/logo.png')" alt="brand" class="brand-icon ms-3" />
</section>


    <!-- 혜택 카드 -->
    <article class="benefit-card position-relative mb-5">
      <h6 class="benefit-title fw-semibold mb-4">
        현재 PLUS 구독제로 이용중인 혜택
      </h6>

      <ul class="benefit-list mb-0">
        <li>보호자 최대 3명 등록 가능</li>
        <li>1년간 위치정보 기록</li>
        <li>더 강화된 레포트 제공</li>
      </ul>
    </article>

    <!-- 구독 취소 버튼 -->
    <button type="button" class="btn btn-cancel w-100 py-3 fw-semibold">
      PLUS 요금제 구독 취소하기
    </button>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  userName?: string
}>()

const userName = props.userName ?? 'User'
/** 공백/한글/쉼표가 있는 경로 안전 처리 */
const u = (p: string) => encodeURI(p)
</script>

<style scoped>
/* 화면 폭 (모바일 중심) */
@media (min-width: 576px) {
  .plus-page.container { max-width: 480px; }
}

/* 인사 카드 */
.greet-card {
  background: #fff;
  box-shadow: 0 10px 15px rgba(0,0,0,.10) !important;
}
.profile-circle { width: 50px; height: 50px; background: #DBEAFE; }
.profile-icon { width: 18px; height: 20px; display: block; }
.greet-title { font-size: 1.05rem; }

/* 카피 + 아이콘 */
.benefit-copy {
  font-size: 1.15rem;
  line-height: 1.35;
  color: #111;
  margin: 0;
}

.brand-icon {
  width: 72px;
  height: 72px;
  object-fit: contain;
  margin-right: 1rem; /* 필요에 따라 0~8px 정도로 조정 */
}
/* 혜택 카드 */
.benefit-card{
  background: #fff;
  border: 1px solid #808AFF;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0,0,0,.15);
  padding: 22px;
}
.benefit-title{
  font-size: 1.05rem;
  color: #111;
}
.benefit-list{
  list-style: none;
  padding-left: 0;
  margin: 0;
}
.benefit-list li{
  position: relative;
  padding-left: 14px;
  margin-bottom: 22px;
  font-size: 1.05rem;
  line-height: 1.6;
  color: #111;
}
.benefit-list li::before{
  content: '·';
  position: absolute;
  left: 0;
  top: -2px;
  font-size: 1.6rem;
  line-height: 1;
}

/* CTA 버튼(구독 취소) */
.btn-cancel{
  background: #A5A6F6;
  border: 1px solid #7F56D9;
  color: #fff;
  border-radius: 8px;
}
.btn-cancel:hover{ filter: brightness(0.96); }
</style>

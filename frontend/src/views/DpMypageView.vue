<template>
  <div class="info-page container px-3 py-3">
    <!-- 제목 -->
    <h2 class="text-center fw-semibold title mb-4">내정보</h2>

    <!-- 프로필 + 인사말 -->
    <section class="d-flex align-items-center gap-3 mb-3">
      <div class="avatar-ring rounded-circle">
        <img :src="u('/figma/Rectangle107.png')" alt="아바타" class="avatar-img rounded-circle" />
      </div>
      <p class="greeting mb-0">아무개님 안녕하세요</p>
    </section>

    <hr class="divider" />

    <!-- 카드 2x2 레이아웃 -->
    <section class="cards-grid">
      <!-- 일정 추가하기 (큰 카드) -->
      <article class="card-tile tile-big bg-blue2">
        <h3 class="tile-title">일정 추가하기</h3>
        <div class="icon-wrap">
          <img :src="u('/figma/Calendar.svg')" alt="일정 추가" class="tile-icon big" />
          <img :src="u('/figma/Plus.svg')" alt="plus" class="tile-plus" />
        </div>
      </article>

      <!-- 내 정보 수정하기 -->
      <article class="card-tile bg-blue3">
        <h3 class="tile-title">내 정보 수정하기</h3>
        <img :src="u('/figma/Edit.svg')" alt="내 정보 수정" class="tile-icon" />
      </article>

      <!-- QR 코드 보기 -->
      <article class="card-tile bg-blue1">
        <h3 class="tile-title">QR코드 보기</h3>
        <img :src="u('/figma/qr.svg')" alt="QR 코드" class="tile-icon" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
const u = (p: string) => encodeURI(p)
</script>

<style scoped>
/* ===== Layout ===== */
@media (min-width: 576px) {
  .info-page.container {
    max-width: 480px;
  }
}

.title {
  font-size: 1.7rem;
  line-height: 1.25;
}

.divider {
  border-top: 1px solid #D7D9E1;
  opacity: .9;
  margin: 12px 0;
}

/* ===== Profile ===== */
.avatar-ring {
  width: 92px;
  height: 92px;
  padding: 3px;
  border: 5px solid #FDC300;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 84px;
  height: 84px;
  object-fit: cover;
  border-radius: 50%;
}

.greeting {
  font-size: 1.18rem;
  color: #111;
}

/* ===== Cards (CSS Grid) ===== */
.cards-grid {
  --gap: 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--gap);
}

/* 공통 카드 */
.card-tile {
  position: relative;
  border-radius: 14px;
  padding: 16px;
  min-height: 170px;
  color: #FFECCC;
  box-shadow: 0 2px 10px rgba(0, 0, 0, .06);
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.tile-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin-bottom: 10px;
}

/* 아이콘 */
.icon-wrap {
  position: relative;
  flex-grow: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tile-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);

  width: 85px;
  height: 85px;
  object-fit: contain;
}

.tile-icon.big {
  width: 150px;
  height: 150px;
}

/* 캘린더 plus 아이콘 */
.tile-plus {
  position: absolute;
  top: 20%;
  right: 10%;
  width: 32px;
  height: 32px;
}

/* 큰 카드 */
.tile-big {
  grid-row: span 2;
  min-height: 360px;
}

.bg-blue1 {
  background: #AAC1FD;
}

.bg-blue2 {
  background: #7D88FF;
}

.bg-blue3 {
  background: #4A62DD;
}
</style>

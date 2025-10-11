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
                <div class="text-secondary small">
                    현재 BASIC 요금제 이용중
                </div>
            </div>
        </section>

        <!-- 혜택 카피 + 브랜드 아이콘 -->
        <section class="d-flex align-items-start justify-content-between mb-4">
            <p class="mt-2 benefit-copy">
                환자를 위한<br />
                더 많은 혜택을 제공합니다!
            </p>
            <img :src="u('/figma/logo.png')" alt="brand" class="brand-icon ms-3" />
        </section>

        <!-- 섹션 타이틀 -->
        <h5 class="fw-bold mb-5">PLUS 구독제</h5>

        <!-- 12개월 플랜(할인 배지 포함) -->
        <article class="plan-card position-relative mb-3">
            <!-- 할인 배지 -->
            <div class="ribbon-pill text-white text-center fw-semibold">
                30% 할인중
            </div>

            <div class="row gx-3 align-items-center">
                <div class="col">
                    <div class="plan-name">12개월</div>
                </div>
                <div class="col-auto text-end">
                    <div class="text-muted text-decoration-line-through old-price">￦ 71,880</div>
                    <div class="new-price fw-bold">￦ 50,320</div>
                    <div class="per-month">￦ 4,193/월</div>
                </div>
            </div>
        </article>

        <!-- 1개월 플랜 -->
        <article class="plan-card position-relative mb-4">
            <div class="row gx-3 align-items-center">
                <div class="col">
                    <div class="plan-name">1개월</div>
                </div>
                <div class="col-auto text-end">
                    <div class="new-price fw-semibold">￦ 5,990/월</div>
                </div>
            </div>
        </article>

        <!-- CTA 버튼 -->
        <button type="button" class="btn btn-plus w-100 py-3 fw-semibold mb-3">
            PLUS 요금제 시작하기
        </button>

        <!-- 안내 문구 -->
        <p class="text-muted mb-0" style="text-align: center; font-size: 0.75rem;">
            PLUS 요금제 사용중에도 언제든지 구독을 취소할 수 있습니다.
        </p>

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
/* 모바일 폭 고정 (너가 쓰는 패턴 유지) */
@media (min-width: 576px) {
    .plus-page.container {
        max-width: 480px;
    }
}

/* ===== 인사 카드 ===== */
.greet-card {
    background: #fff;
    box-shadow: 0 10px 15px rgba(0, 0, 0, .10) !important;
}

.profile-circle {
    width: 50px;
    height: 50px;
    background: #DBEAFE;
}

.profile-icon {
    width: 18px;
    height: 20px;
    display: block;
}

.greet-title {
    font-size: 1.05rem;
}

/* ===== 혜택 카피 & 아이콘 ===== */
.benefit-copy {
    font-size: 1.15rem;
    line-height: 1.35;
    color: #111;
    margin-right: .5rem;
}

.brand-icon {
    width: 72px;
    height: 72px;
    object-fit: contain;
    margin-right: 1rem;
}

/* ===== 플랜 카드 ===== */
.plan-card {
    background: #fff;
    border: 1px solid #808AFF;
    border-radius: 16px;
    box-shadow: 0 4px 15px rgba(0, 0, 0, .15);
    padding: 18px;
}

/* 할인 배지(카드 상단에 겹치도록 중앙 배치) */
.ribbon-pill {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    top: -22px;
    background: #8E97FD;
    border-radius: 25px;
    min-width: 220px;
    padding: 10px 16px;
    font-size: 0.95rem;
}

/* 텍스트 스타일 */
.plan-name {
    font-size: 1.85rem;
    font-weight: 600;
    color: #000;
    text-shadow: 0 2px 2px rgba(0, 0, 0, .25);
}

.old-price {
    font-size: .95rem;
}

.new-price {
    font-size: 1.55rem;
}

.per-month {
    font-size: 1.0rem;
}

/* CTA 버튼 */
.btn-plus {
    background: rgba(125, 136, 255, .9);
    border: 1px solid #7F56D9;
    color: #fff;
    border-radius: 8px;
}

.btn-plus:hover {
    filter: brightness(0.96);
}
</style>

<!-- src/views/QuizHangul.vue -->
<template>
    <div class="container-sm py-3" style="max-width:414px">
        <!-- 상단 바 -->
        <div class="d-flex align-items-center justify-content-between mb-2">
            <button type="button" class="btn btn-link text-body p-0">
                <i class="bi bi-chevron-left fs-4"></i>
            </button>
            <div class="fw-semibold">두뇌 훈련</div>
            <button type="button" class="btn btn-link text-body p-0">
                <i class="bi bi-house-door fs-4"></i>
            </button>
        </div>

        <!-- 퀴즈 제목 -->
        <div class="text-center fw-semibold mb-2">훈민정음 퀴즈</div>

        <!-- 진행/점수 -->
        <div class="d-flex justify-content-between small mb-1">
            <div class="text-body-secondary">
                문제 <span class="text-body fw-semibold">3/10</span>
            </div>
            <div class="text-body-secondary">
                점수: <span class="text-body fw-semibold">20점</span>
            </div>
        </div>
        <div class="progress mb-2" style="height:8px">
            <div class="progress-bar bg-primary" role="progressbar" style="width: 30%"></div>
        </div>

        <!-- 남은시간 / 연속정답 -->
        <div class="d-flex justify-content-around text-center py-2 mb-2 bg-white rounded-3 shadow-sm">
            <div>
                <div class="small text-body-secondary d-flex align-items-center justify-content-center gap-1">
                    <i class="bi bi-clock text-body-secondary"></i>
                    <span>남은시간</span>
                </div>
                <div class="fw-semibold fs-5">1:45</div>
            </div>
            <div>
                <div class="small text-body-secondary d-flex align-items-center justify-content-center gap-1">
                    <i class="bi bi-trophy text-body-secondary"></i>
                    <span>연속정답</span>
                </div>
                <div class="fw-semibold fs-5">2</div>
            </div>
        </div>

        <!-- 문제 카드 -->
        <div class="card border-0 shadow-sm mt-3 mb-3">
            <div class="card-body">
                <div class="d-flex justify-content-center mb-2">
                    <span class="badge text-bg-light border text-body-secondary">문제 3</span>
                </div>

                <div class="fs-5 fw-semibold mb-3">
                    다음 중 '가'의<br>올바른 훈민정음 자모는?
                </div>

                <div class="bg-light rounded-3 text-center py-4 mb-3">
                    <div class="display-3 fw-semibold">가</div>
                </div>

                <!-- 보기 -->
                <div class="row g-3">
                    <div class="col-6">
                        <button type="button" class="btn btn-outline-secondary w-100 rounded-3 py-3">
                            <div class="fs-5">①&nbsp; ㄱ + ㅏ</div>
                        </button>
                    </div>
                    <div class="col-6">
                        <button type="button" class="btn btn-outline-secondary w-100 rounded-3 py-3">
                            <div class="fs-5">②&nbsp; ㄱ + ㅑ</div>
                        </button>
                    </div>
                    <div class="col-6">
                        <button type="button" class="btn btn-outline-secondary w-100 rounded-3 py-3">
                            <div class="fs-5">③&nbsp; ㅋ + ㅏ</div>
                        </button>
                    </div>
                    <div class="col-6">
                        <button type="button" class="btn btn-outline-secondary w-100 rounded-3 py-3">
                            <div class="fs-5">④&nbsp; ㄱ + ㅓ</div>
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 하단 버튼 -->
        <div class="d-flex gap-2">
            <button type="button" class="btn btn-light w-50 py-3 rounded-3">건너뛰기</button>
            <button type="button" class="btn btn-primary w-50 py-3 rounded-3">다음 문제</button>
        </div>
    </div>
</template>

<script setup>
// 프론트 미리보기 전용
</script>

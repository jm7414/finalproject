<template>
  <div class="patient-main p-4">
    <!-- 다음 일정 -->
    <section class="card mb-4">
      <div class="card-header d-flex align-items-center justify-content-between py-3">
        <h3 class="card-title m-0 fs-5 fw-bold">다음 일정</h3>
        <div class="card-toolbar">
          <button type="button" class="btn btn-sm btn-light">전체 일정 보기</button>
        </div>
      </div>
      <!-- 오늘 일정 -->
      <div class="card-body">
        <div class="d-flex align-items-start gap-3">
          <div class="badge bg-primary fs-6">오늘</div>
          <div>
            <div class="fw-bold fs-5">9:00 출근</div>
            <div class="text-muted">18:00 퇴근</div>
            <div class="text-muted">준비물: 노트북, 마우스</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 버튼 -->
    <div class="row g-4">
      <div class="col-6">
        <button type="button"
          class="btn btn-light w-100 h-160px d-flex flex-column justify-content-center align-items-center rounded-3 shadow-sm">
          <span class="fs-1 lh-1">👤</span>
          <span class="mt-2 fs-3 fw-bold">내정보</span>
        </button>
      </div>

      <div class="col-6">
        <button type="button"
          class="btn btn-light w-100 h-160px d-flex flex-column justify-content-center align-items-center rounded-3 shadow-sm">
          <span class="fs-1 lh-1">🤖</span>
          <span class="mt-2 fs-3 fw-bold">AI챗봇</span>
        </button>
      </div>

      <div class="col-6">
        <button type="button"
          class="btn btn-light w-100 h-160px d-flex flex-column justify-content-center align-items-center rounded-3 shadow-sm">
          <span class="fs-1 lh-1">📝</span>
          <span class="mt-2 fs-3 fw-bold">기록</span>
        </button>
      </div>

      <div class="col-6">
        <button type="button"
          class="btn btn-danger w-100 h-160px d-flex flex-column justify-content-center align-items-center rounded-3 shadow-sm">
          <span class="fs-1 lh-1">📞</span>
          <span class="mt-2 fs-3 fw-bold">비상연락</span>
        </button>
      </div>
    </div>

  </div>
</template>

<script setup>

</script>

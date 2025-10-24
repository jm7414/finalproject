<template>
  <!-- 중간 회색 영역: 안심존 & 환자 걸음수 -->
  <div class="px-4 py-2 pb-3" style="background: #EEF3F8;" ref="topTilesRow">
    <div class="row g-2 mb-0">
      <!-- 안심존 카드 -->
      <div class="col-6">
        <div class="card border-0 rounded-3 d-flex flex-column" :style="{ background: safeZoneStatus.bgColor, height: '85px' }">
          <div class="card-body p-2 d-flex flex-column justify-content-between">
            <div class="d-flex align-items-center gap-1">
              <i class="bi bi-shield" :style="{ fontSize: '20px', color: safeZoneStatus.color }"></i>
              <span class="fw-bold text-dark" style="font-size: 0.85rem;">안심존</span>
            </div>
            <div style="line-height: 1.4;">
              <div class="text-muted" style="font-size: 0.75rem;">현재 위치</div>
              <div class="fw-semibold" :style="{ fontSize: '0.85rem', color: safeZoneStatus.color }">{{ safeZoneStatus.status }}</div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 환자 걸음수 카드 -->
      <div class="col-6">
        <div class="card border-0 rounded-3 d-flex flex-column bg-white" style="height: 85px;">
          <div class="card-body p-2 d-flex flex-column justify-content-between">
            <div class="d-flex align-items-center gap-1">
              <i class="bi bi-person-walking" style="font-size: 20px; color: #9CA3AF;"></i>
              <span class="fw-bold text-dark" style="font-size: 0.85rem;">환자 걸음수</span>
            </div>
            <div class="text-muted" style="font-size: 0.75rem;">{{ patientSteps }} 걸음</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// Props 정의
const props = defineProps({
  safeZoneStatus: {
    type: Object,
    required: true,
    default: () => ({
      isInside: true,
      status: '연결 필요',
      color: '#9CA3AF',
      bgColor: '#F3F4F6'
    })
  },
  patientSteps: {
    type: [String, Number],
    default: '1,057'
  }
})

// DOM 참조 (바텀시트 높이 계산용)
const topTilesRow = ref(null)

// DOM 참조를 부모에게 전달
defineExpose({
  topTilesRow
})
</script>

<style scoped>
/* 카드 스타일 */
.card {
  box-shadow: 0 1px 2px rgba(0, 0, 0, .05);
}

/* 안심존 상태별 색상 */
.safe-zone-safe {
  background: #DCFCE7 !important;
  color: #16A34A !important;
}

.safe-zone-danger {
  background: #FEE2E2 !important;
  color: #EF4444 !important;
}

.safe-zone-warning {
  background: #FEF3C7 !important;
  color: #F59E0B !important;
}

.safe-zone-disabled {
  background: #F3F4F6 !important;
  color: #9CA3AF !important;
}
</style>

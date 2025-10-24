<template>
  <!-- 하단 영역: 오늘의 일정 -->
  <div class="px-4 pt-3 pb-4" style="background: #EEF3F8;">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="fw-bold text-dark fs-5">오늘의 일정</div>
      <button @click="$emit('goToCalendar')" class="btn btn-link btn-sm text-decoration-none p-0 text-primary fw-semibold">
        + 더보기
      </button>
    </div>

    <!-- 일정 목록 -->
    <div 
      class="schedule-list d-flex flex-column gap-3"
      :class="{ 'schedule-scrollable': isScrollable }">
      <!-- 일정이 없을 때 -->
      <div v-if="todaySchedules.length === 0" class="card border-0 rounded-4 bg-white">
        <div class="card-body p-4 text-center text-muted">
          오늘 예정된 일정이 없습니다.
        </div>
      </div>

      <!-- 일정 카드들 -->
      <div 
        v-for="schedule in todaySchedules"
        :key="schedule.scheduleNo"
        @click="$emit('selectSchedule', schedule.scheduleNo)"
        :class="['schedule-card', 'card', 'rounded-4', { 'schedule-active': getScheduleStatus(schedule) === 'active' }]"
        :style="{
          cursor: 'pointer',
          ...getScheduleCardStyle(schedule),
          minHeight: '140px'
        }">
        <div class="card-body p-3">
          <div class="d-flex align-items-start gap-2 position-relative">
            <!-- 왼쪽 아이콘 -->
            <div class="d-flex align-items-center flex-shrink-0" style="padding-top: 4px;">
              <div 
                class="rounded-circle" 
                :style="{
                  width: '12px',
                  height: '12px',
                  background: getScheduleStatus(schedule) === 'active' ? '#3B82F6' : '#9CA3AF'
                }">
              </div>
            </div>
            
            <!-- 일정 정보 -->
            <div class="flex-grow-1">
              <div class="fw-semibold text-muted mb-2" style="font-size: 1.0625rem;">
                {{ schedule.scheduleTitle }}
              </div>
              <div class="text-muted mb-3" style="font-size: 0.9375rem;">
                {{ formatLocation(schedule.scheduleNo) || '위치 정보 없음' }}
              </div>
              <div class="d-flex align-items-center gap-2">
                <svg width="14" height="14" fill="#9CA3AF" viewBox="0 0 16 16">
                  <path d="M8 3.5a.5.5 0 0 0-1 0V9a.5.5 0 0 0 .252.434l3.5 2a.5.5 0 0 0 .496-.868L8 8.71V3.5z"/>
                  <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm7-8A7 7 0 1 1 1 8a7 7 0 0 1 14 0z"/>
                </svg>
                <span class="text-muted" style="font-size: 0.8125rem;">
                  {{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}
                </span>
              </div>
            </div>
            
            <!-- 상태 배지 -->
            <div class="text-end flex-shrink-0">
              <span 
                class="badge rounded-pill px-3 py-1" 
                :style="{
                  background: getScheduleStatus(schedule) === 'active' ? '#3B82F6' : '#9CA3AF',
                  color: 'white',
                  fontSize: '0.75rem',
                  fontWeight: '600'
                }">
                {{ getScheduleStatus(schedule) === 'active' ? '이동중' : '대기중' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// Props 정의
const props = defineProps({
  todaySchedules: {
    type: Array,
    required: true,
    default: () => []
  },
  formatTime: {
    type: Function,
    required: true
  },
  formatLocation: {
    type: Function,
    required: true
  },
  getScheduleStatus: {
    type: Function,
    required: true
  },
  getScheduleCardStyle: {
    type: Function,
    required: true
  },
  isScrollable: {
    type: Boolean,
    default: false
  }
})

// Emits 정의
const emit = defineEmits(['goToCalendar', 'selectSchedule'])
</script>

<style scoped>
/* 일정 카드 스타일 */
.schedule-card {
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .08);
}

.schedule-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, .12);
}

/* 일정 목록 스크롤 */
.schedule-list {
  max-height: none;
  overflow: visible;
}

.schedule-list.schedule-scrollable {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

/* 스크롤바 스타일링 */
.schedule-list.schedule-scrollable::-webkit-scrollbar {
  width: 6px;
}

.schedule-list.schedule-scrollable::-webkit-scrollbar-track {
  background: #E5E7EB;
  border-radius: 3px;
}

.schedule-list.schedule-scrollable::-webkit-scrollbar-thumb {
  background: #9CA3AF;
  border-radius: 3px;
}

.schedule-list.schedule-scrollable::-webkit-scrollbar-thumb:hover {
  background: #6B7280;
}
</style>

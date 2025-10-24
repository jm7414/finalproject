<template>
  <!-- 상단 영역: 환자 정보 카드 -->
  <div class="px-4 pt-3 pb-2" style="background: #EEF3F8;">
    <div class="card border-0 rounded-3 bg-white shadow-sm">
      <div class="card-body px-3 py-2">
        <!-- 환자 정보가 있는 경우 -->
        <div v-if="patientInfo.name" class="d-flex align-items-center gap-2">
          <!-- 아바타 아이콘 -->
          <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0 avatar-clickable" 
               :style="{
                 width: '48px', 
                 height: '48px', 
                 background: '#E5E7EB', 
                 border: `4px solid ${patientInfo.isOnline ? '#94FFA1' : '#9CA3AF'}`
               }"
               @click="$emit('goToMyPage')">
            <svg width="26" height="26" fill="#6B7280" viewBox="0 0 16 16">
              <path d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm2-3a2 2 0 1 1-4 0 2 2 0 0 1 4 0zm4 8c0 1-1 1-1 1H3s-1 0-1-1 1-4 6-4 6 3 6 4zm-1-.004c-.001-.246-.154-.986-.832-1.664C11.516 10.68 10.289 10 8 10c-2.29 0-3.516.68-4.168 1.332-.678.678-.83 1.418-.832 1.664h10z"/>
            </svg>
          </div>
          <!-- 텍스트 정보 -->
          <div class="flex-grow-1" style="min-width: 0;">
            <div class="fw-bold text-dark" style="font-size: 1.05rem; line-height: 1.3;">
              {{ patientInfo.name }} 님
            </div>
            <div class="text-muted" style="font-size: 0.8125rem; line-height: 1.3;">
              {{ patientInfo.isOnline ? '온라인' : '오프라인' }} • {{ formatLastActivity(patientInfo.lastActivity) }}
            </div>
          </div>
           <!-- 실종신고 버튼 -->
           <div class="flex-shrink-0">
             <button class="missing-report-btn" @click="$emit('reportMissing')">
               <i class="bi bi-exclamation-triangle-fill"></i>
               <span>상태 변경</span>
             </button>
           </div>
        </div>
        
        <!-- 환자 정보가 없는 경우 -->
        <div v-else class="d-flex align-items-center gap-2">
          <!-- 연결 아이콘 -->
          <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0" 
               style="width: 48px; height: 48px; background: #FEF3C7; border: 4px solid #F59E0B;">
            <svg width="26" height="26" fill="#D97706" viewBox="0 0 16 16">
              <path d="M8 0a8 8 0 1 0 0 16A8 8 0 0 0 8 0zM5.354 4.646a.5.5 0 1 1-.708.708L7.293 2.5 5.646.854a.5.5 0 1 1 .708-.708L8.293 1.793l2.146-2.147a.5.5 0 0 1 .708.708L8.707 2.5l2.146 2.146a.5.5 0 0 1-.708.708L8 3.207 5.854 5.354z"/>
            </svg>
          </div>
          <!-- 텍스트 정보 -->
          <div class="flex-grow-1" style="min-width: 0;">
            <div class="fw-bold text-dark" style="font-size: 1.05rem; line-height: 1.3;">
              환자 연결 필요
            </div>
            <div class="text-muted" style="font-size: 0.8125rem; line-height: 1.3;">
              환자와 연결하여 위치를 확인하세요
            </div>
          </div>
          <!-- 연결 버튼 -->
          <div class="flex-shrink-0">
            <button @click="$emit('goToConnect')" class="btn btn-sm btn-outline-primary" style="font-size: 0.75rem; padding: 0.25rem 0.5rem;">
              연결하기
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// Props 정의
const props = defineProps({
  patientInfo: {
    type: Object,
    required: true,
    default: () => ({
      name: '',
      userNo: null,
      isOnline: false,
      lastActivity: null
    })
  }
})

// Emits 정의
const emit = defineEmits(['goToMyPage', 'reportMissing', 'goToConnect'])

// 마지막 활동 시간 포맷팅
function formatLastActivity(lastActivity) {
  if (!lastActivity) return '정보 없음'
  
  const now = new Date()
  const activityTime = new Date(lastActivity)
  const diffInMinutes = Math.floor((now - activityTime) / (1000 * 60))
  
  if (diffInMinutes < 1) {
    return '방금 전'
  } else if (diffInMinutes < 60) {
    return `${diffInMinutes}분 전`
  } else if (diffInMinutes < 1440) { // 24시간
    const hours = Math.floor(diffInMinutes / 60)
    return `${hours}시간 전`
  } else {
    const days = Math.floor(diffInMinutes / 1440)
    return `${days}일 전`
  }
}
</script>

<style scoped>
/* 상태 변경 버튼 */
.missing-report-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%);
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(71, 85, 105, 0.1);
}

.missing-report-btn:hover {
  background: linear-gradient(135deg, #F1F5F9 0%, #E2E8F0 100%);
  border-color: #94A3B8;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(71, 85, 105, 0.15);
}

.missing-report-btn:active {
  transform: translateY(0);
  box-shadow: 0 1px 3px rgba(71, 85, 105, 0.1);
}

.missing-report-btn i {
  font-size: 14px;
  color: #475569;
}

.missing-report-btn span {
  white-space: nowrap;
  font-weight: 600;
}

/* 아바타 클릭 가능 스타일 */
.avatar-clickable {
  cursor: pointer;
  transition: all 0.2s ease;
}

.avatar-clickable:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.avatar-clickable:active {
  transform: scale(0.98);
}
</style>

<template>
  <!-- 모달 배경 -->
  <div v-if="show" class="modal-backdrop" @click="closeModal">
    <!-- 모달 컨테이너 -->
    <div class="modal-container" @click.stop>
      <!-- 모달 헤더 -->
      <div class="modal-header">
        <div class="alert-icon">
          <i class="bi bi-exclamation-triangle-fill siren-icon"></i>
        </div>
        <h5 class="modal-title">안심존 이탈 알림</h5>
      </div>

      <!-- 모달 내용 -->
      <div class="modal-body">
        <p class="alert-message">
          <strong>{{ patientName }}</strong>님이 안심존을 벗어났습니다
        </p>
      </div>

      <!-- 모달 버튼 -->
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="closeModal">
          닫기
        </button>
        <button type="button" class="btn btn-primary" @click="goToMap">
          지도로 이동
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { onMounted } from 'vue'

const router = useRouter()
const route = useRoute()

// Props
const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  patientName: {
    type: String,
    default: '환자'
  }
})

// Emits
const emit = defineEmits(['close'])

// 디버깅을 위한 로그
onMounted(() => {
  console.log('SafeZoneAlertModal 컴포넌트 마운트됨')
})

// 모달 닫기
function closeModal() {
  emit('close')
}

// 지도로 이동
function goToMap() {
  const isDesktop =
    (route && route.meta && route.meta.layout === 'desktop') ||
    (route && typeof route.path === 'string' && route.path.startsWith('/desktop'))

  router.push(isDesktop ? '/desktop/main' : '/map-main')
  emit('close')
}
</script>

<style scoped>
/* 모달 배경 */
.modal-backdrop {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  background-color: rgba(0, 0, 0, 0.5) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  z-index: 999999 !important;
  padding: 20px !important;
  width: 100vw !important;
  height: 100vh !important;
  margin: 0 !important;
}

/* 모달 컨테이너 */
.modal-container {
  background: white !important;
  border-radius: 16px !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2) !important;
  max-width: 320px !important;
  width: 320px !important;
  max-height: 90vh !important;
  overflow: hidden !important;
  animation: modalSlideIn 0.3s ease-out !important;
  position: relative !important;
  z-index: 1000000 !important;
  margin: 0 !important;
  transform: translateX(-10px) !important; /* 좌측으로 10px 이동 */
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 모달 헤더 */
.modal-header {
  background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
  color: white;
  padding: 12px 24px 10px;
  text-align: center;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.alert-icon {
  margin-bottom: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.alert-icon i {
  font-size: 24px;
  color: #FEF2F2;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 사이렌 애니메이션 */
.siren-icon {
  animation: sirenBlink 1s infinite;
}

@keyframes sirenBlink {
  0%, 50% {
    opacity: 1;
    transform: scale(1);
  }
  25%, 75% {
    opacity: 0.7;
    transform: scale(1.1);
  }
}

.modal-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
  color: white;
  text-align: center;
  width: 100%;
}

/* 모달 내용 */
.modal-body {
  padding: 24px;
  text-align: center;
}

.alert-message {
  font-size: 1rem;
  line-height: 1.5;
  color: #333;
  margin-bottom: 0;
  text-align: center;
}

.alert-message strong {
  color: #EF4444;
  font-weight: 700;
}

/* 모달 버튼 */
.modal-footer {
  padding: 16px 24px 24px;
  display: flex;
  gap: 12px;
}

.btn {
  flex: 1;
  padding: 12px 16px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 0.95rem;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary {
  background: #F8F9FA;
  color: #666;
  border: 1px solid #E9ECEF;
}

.btn-secondary:hover {
  background: #E9ECEF;
  color: #555;
  transform: translateY(-1px);
}

.btn-primary {
  background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

.btn-primary:hover {
  background: linear-gradient(135deg, #DC2626 0%, #B91C1C 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.btn:active {
  transform: translateY(0);
}

/* 반응형 */
@media (max-width: 375px) {
  .modal-backdrop {
    padding: 16px;
  }
  
  .modal-container {
    max-width: 100%;
  }
  
  .modal-header {
    padding: 16px 20px 12px;
  }
  
  .modal-body {
    padding: 20px;
  }
  
  .modal-footer {
    padding: 12px 20px 20px;
  }
}
</style>

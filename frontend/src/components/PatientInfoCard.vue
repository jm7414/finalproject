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
            }" @click="$emit('goToMyPage')">
            <svg width="26" height="26" fill="#6B7280" viewBox="0 0 16 16">
              <path
                d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm2-3a2 2 0 1 1-4 0 2 2 0 0 1 4 0zm4 8c0 1-1 1-1 1H3s-1 0-1-1 1-4 6-4 6 3 6 4zm-1-.004c-.001-.246-.154-.986-.832-1.664C11.516 10.68 10.289 10 8 10c-2.29 0-3.516.68-4.168 1.332-.678.678-.83 1.418-.832 1.664h10z" />
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
            <!-- 실종상태변환 기능 18: @click 이벤트를 내부 함수 'openModalByStatus'로 변경 -->
            <button class="missing-report-btn" @click="openModalByStatus">
              <i class="bi bi-exclamation-triangle-fill"></i>
              <!-- 실종상태변환 기능 19: 환자 상태(user_status)에 따라 버튼 텍스트 동적 변경 -->
              <span>{{ patientInfo.user_status === 0 ? '상태 변경' : '실종 해제' }}</span>
            </button>
          </div>
        </div>

        <!-- 환자 정보가 없는 경우 (원본 코드 유지) -->
        <div v-else class="d-flex align-items-center gap-2">
          <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
            style="width: 48px; height: 48px; background: #FEF3C7; border: 4px solid #F59E0B;">
            <svg width="26" height="26" fill="#D97706" viewBox="0 0 16 16">
              <path
                d="M8 0a8 8 0 1 0 0 16A8 8 0 0 0 8 0zM5.354 4.646a.5.5 0 1 1-.708.708L7.293 2.5 5.646.854a.5.5 0 1 1 .708-.708L8.293 1.793l2.146-2.147a.5.5 0 0 1 .708.708L8.707 2.5l2.146 2.146a.5.5 0 0 1-.708.708L8 3.207 5.854 5.354z" />
            </svg>
          </div>
          <div class="flex-grow-1" style="min-width: 0;">
            <div class="fw-bold text-dark" style="font-size: 1.05rem; line-height: 1.3;">환자 연결 필요</div>
            <div class="text-muted" style="font-size: 0.8125rem; line-height: 1.3;">환자와 연결하여 위치를 확인하세요</div>
          </div>
          <div class="flex-shrink-0">
            <button @click="$emit('goToConnect')" class="btn btn-sm btn-outline-primary"
              style="font-size: 0.75rem; padding: 0.25rem 0.5rem;">연결하기</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 실종상태변환 기능 20: '실종 신고' 폼 모달 (MissingReportModal.vue) -->
  <MissingReportModal :show="isReportModalVisible" :patient="patientInfo" @close="isReportModalVisible = false"
    @reportSuccess="handleReportSuccess" />

  <!-- 실종상태변환 기능 21: '실종 해제' 확인 모달 (ConfirmModal.vue) -->
  <ConfirmModal :show="isResolveModalVisible" title="상태 해제 확인" message="환자의 '실종' 상태를 해제하시겠습니까?" confirm-text="상태 해제"
    cancel-text="취소" @close="isResolveModalVisible = false" @cancel="isResolveModalVisible = false"
    @confirm="handleResolveConfirm" />
</template>

<script setup>
// 실종상태변환 기능 1: ref (Vue의 반응형 상태 관리) import
import { ref } from 'vue'
// 실종상태변환 기능 2: 2개의 모달 컴포넌트 import
import ConfirmModal from './ConfirmModal.vue' // '해제'용
import MissingReportModal from './MissingReportModal.vue' // '신고'용
// 실종상태변환 기능 3: 백엔드 API 통신을 위한 axios import
import axios from 'axios'

// Props 정의
const props = defineProps({
  patientInfo: {
    type: Object,
    required: true,
    default: () => ({
      name: '',
      userNo: null,
      isOnline: false,
      lastActivity: null,
      // 실종상태변환 기능 4: 부모로부터 환자의 'user_status'를 받음 (0:정상, 1:실종)
      user_status: 0
    })
  }
})

// Emits 정의
// (참고: 'statusUpdated'는 부모가 데이터를 갱신할 때 사용하도록 남겨둘 수 있습니다)
const emit = defineEmits(['goToMyPage', 'reportMissing', 'goToConnect', 'statusUpdated'])

// 마지막 활동 시간 포맷팅 (원본 코드 유지)
function formatLastActivity(lastActivity) {
  if (!lastActivity) return '정보 없음'
  const now = new Date()
  const activityTime = new Date(lastActivity)
  const diffInMinutes = Math.floor((now - activityTime) / (1000 * 60))
  if (diffInMinutes < 1) return '방금 전'
  if (diffInMinutes < 60) return `${diffInMinutes}분 전`
  if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}시간 전`
  return `${Math.floor(diffInMinutes / 1440)}일 전`
}

// --- 실종상태변환 기능 ---

// 실종상태변환 기능 5: 2개의 모달 표시 상태를 각각 제어
const isReportModalVisible = ref(false)  // '신고' 폼 모달 (MissingReportModal)
const isResolveModalVisible = ref(false) // '해제' 확인 모달 (ConfirmModal)

// 실종상태변환 기능 6: '상태 변경' 버튼 클릭 시 실행되는 메인 함수
function openModalByStatus() {
  // 실종상태변환 기능 7: 환자의 현재 user_status 값 확인
  if (props.patientInfo.user_status === 0) {
    // 0 (정상) -> 1 (실종) : '신고' 폼 모달(MissingReportModal) 열기
    isReportModalVisible.value = true
  } else {
    // 1 (실종) -> 0 (정상) : '해제' 확인 모달(ConfirmModal) 열기
    isResolveModalVisible.value = true
  }
}

// 실종상태변환 기능 9: '신고' 폼이 성공적으로 제출됐을 때 (MissingReportModal이 @reportSuccess 호출)
async function handleReportSuccess(reportData) {
  console.log('--- 실종 신고 성공 (MissingReportModal) ---')
  console.log('신고 데이터:', reportData)

  isReportModalVisible.value = false // 모달 닫기

  // 실종상태변환 기능 10: 부모에게 상태가 갱신되었음을 알림
  // (실종 신고 후 다른 페이지로 이동하므로, 'location.reload()'는 불필요)
  emit('statusUpdated')
}

// 실종상태변환 기능 11: '실종 해제' 모달의 '확인' 버튼 클릭 시 실행
async function handleResolveConfirm() {
  // 실종상태변환 기능 12: DB에 업데이트할 새 상태(0) 계산
  const newStatus = 0; // 해제는 0으로 고정

  console.log(`--- '실종 해제' API 호출 실행 ---`);
  console.log(`대상 환자 userNo: ${props.patientInfo.userNo}`);
  console.log(`새 상태: ${newStatus}`);

  try {
    // 실종상태변환 기능 13: 백엔드 API 호출 (user_status를 0으로 변경)
    const response = await axios.post('/api/users/update-status',
      {
        userNo: props.patientInfo.userNo,
        userStatus: newStatus  // 컨트롤러가 'userStatus' 키를 기대
      },
      {
        withCredentials: true // '시큐리티' (인증) 처리
      }
    );

    // 실종상태변환 기능 14: API 호출 성공
    console.log('API 호출 성공 (상태 해제):', response.data);
    alert('환자의 실종 상태가 성공적으로 해제되었습니다.');

    // 실종상태변환 기능 15: (요청사항) 페이지를 강제 새로고침하여 변경된 상태(버튼 텍스트)를 즉시 반영
    location.reload();

  } catch (error) {
    // 실종상태변환 기능 16: API 호출 실패
    console.error("상태 해제 API 호출 실패:", error);
    alert('상태 변경에 실패했습니다. (콘솔 로그 확인)');
  } finally {
    // 실종상태변환 기능 17: 모달 닫기
    isResolveModalVisible.value = false;
  }
}
// --- 실종상태변환 기능 종료 ---
</script>

<style scoped>
/* 원본 <style> 코드 전체 유지 */
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

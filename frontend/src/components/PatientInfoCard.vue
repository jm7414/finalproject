<script setup>
// 기능 1: Vue, 라우터, HTTP 클라이언트(axios) import
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
// 기능 2: '실종 해제' 시 사용할 ConfirmModal import
import ConfirmModal from './ConfirmModal.vue';

// 기능 3: 라우터 인스턴스
const router = useRouter();

// 기능 4: Props 정의 (user_status 포함)
const props = defineProps({
  patientInfo: {
    type: Object,
    required: true,
    default: () => ({
      name: '', userNo: null, isOnline: false, lastActivity: null, user_status: 0
    })
  }
});

// 기능 5: Emits 정의 (부모 알림용 - 새로고침 대신 사용 가능)
const emit = defineEmits(['goToMyPage', 'goToConnect', 'statusUpdated', 'reportMissing']);

// 기능 6: 마지막 활동 시간 포맷팅 함수
function formatLastActivity(lastActivity) {
  if (!lastActivity) return '정보 없음';
  const now = new Date();
  const activityTime = new Date(lastActivity);
  const diffInMinutes = Math.floor((now - activityTime) / (1000 * 60));
  if (diffInMinutes < 1) return '방금 전';
  if (diffInMinutes < 60) return `${diffInMinutes}분 전`;
  if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}시간 전`;
  return `${Math.floor(diffInMinutes / 1440)}일 전`;
}

// --- 실종 상태 변경 (토글) 기능 ---

// 기능 7: '실종 해제' 확인 모달 표시 상태 ref
const isResolveModalVisible = ref(false);

// 기능 8: '상태 변경/실종 해제' 버튼 클릭 시 실행될 메인 함수
function handleStatusChangeClick() {
  if (!props.patientInfo?.userNo) {
    alert("환자 정보가 없습니다.");
    return;
  }

  // 기능 9: 환자 상태(user_status) 값에 따라 분기
if (props.patientInfo.user_status === 0) {
    // ★★★ 이 로그를 추가하여 userNo 값 확인 ★★★
    console.log(`실종 신고 페이지(/MissingReport/:id)로 이동 시도. 전달할 ID:`, props.patientInfo.userNo);

    // userNo 값이 유효한 숫자인지 확인 후 push
    if (props.patientInfo.userNo !== null && props.patientInfo.userNo !== undefined) {
      router.push({ name: 'MissingReport', params: { id: props.patientInfo.userNo } });
    } else {
      console.error('환자 ID(userNo)가 유효하지 않아 페이지 이동 불가');
      alert('환자 정보가 올바르지 않아 신고 페이지로 이동할 수 없습니다.');
    }
  } else {
    isResolveModalVisible.value = true;
  }
}

// 기능 12: '실종 해제' 모달의 '확인' 버튼 클릭 시 실행 (API 호출)
async function handleResolveConfirm() {
  const newStatus = 0; // 해제 시 상태는 0

  console.log(`--- '실종 해제' API 호출 실행 ---`);
  console.log(`대상 환자 userNo: ${props.patientInfo.userNo}`);
  console.log(`새 상태: ${newStatus}`);

  try {
    // 백엔드 API 호출 (user_status를 0으로 변경)
    const response = await axios.post('/api/users/update-status',
      { userNo: props.patientInfo.userNo, userStatus: newStatus },
      { withCredentials: true } // 인증 처리
    );

    console.log('API 호출 성공 (상태 해제):', response.data);
    alert('환자의 실종 상태가 성공적으로 해제되었습니다.');

    // 페이지 새로고침으로 변경된 상태(버튼 텍스트) 반영
    location.reload();
    // emit('statusUpdated'); // 또는 부모에게 알려 갱신

  } catch (error) {
    console.error("상태 해제 API 호출 실패:", error);
    alert('상태 변경에 실패했습니다. (콘솔 로그 확인)');
  } finally {
    isResolveModalVisible.value = false; // 모달 닫기
  }
}
// --- 실종 상태 변경 (토글) 기능 종료 ---

</script>

<template>
  <!-- 상단 영역: 환자 정보 카드 -->
  <div class="px-4 pt-3 pb-2" style="background: #EEF3F8;">
    <div class="card border-0 rounded-3 bg-white shadow-sm">
      <div class="card-body px-3 py-2">
        <!-- 환자 정보가 있는 경우 -->
        <div v-if="patientInfo.name" class="d-flex align-items-center gap-2">
          <!-- 아바타 아이콘 -->
          <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0 avatar-clickable"
            :style="{ width: '48px', height: '48px', background: '#E5E7EB', border: `4px solid ${patientInfo.isOnline ? '#94FFA1' : '#9CA3AF'}` }"
            @click="$emit('goToMyPage')">
            <svg width="26" height="26" fill="#6B7280" viewBox="0 0 16 16">
              <path
                d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm2-3a2 2 0 1 1-4 0 2 2 0 0 1 4 0zm4 8c0 1-1 1-1 1H3s-1 0-1-1 1-4 6-4 6 3 6 4zm-1-.004c-.001-.246-.154-.986-.832-1.664C11.516 10.68 10.289 10 8 10c-2.29 0-3.516.68-4.168 1.332-.678.678-.83 1.418-.832 1.664h10z" />
            </svg>
          </div>
          <!-- 텍스트 정보 -->
          <div class="flex-grow-1" style="min-width: 0;">
            <div class="fw-bold text-dark" style="font-size: 1.05rem; line-height: 1.3;">{{ patientInfo.name }} 님</div>
            <div class="text-muted" style="font-size: 0.8125rem; line-height: 1.3;">{{ patientInfo.isOnline ? '온라인' :
              '오프라인' }} • {{ formatLastActivity(patientInfo.lastActivity) }}</div>
          </div>
          <!-- 실종신고/해제 버튼 -->
          <div class="flex-shrink-0">
            <!-- 기능 13: 버튼 클릭 시 handleStatusChangeClick 함수 호출 -->
            <button class="missing-report-btn" @click="handleStatusChangeClick">
              <i class="bi bi-exclamation-triangle-fill"></i>
              <!-- 기능 14: user_status 값에 따라 버튼 텍스트 변경 -->
              <span>{{ patientInfo.user_status === 0 ? '상태 변경' : '실종 해제' }}</span>
            </button>
          </div>
        </div>

        <!-- 환자 정보가 없는 경우 -->
        <div v-else class="d-flex align-items-center gap-2">
          <div class="d-flex align-items-center justify-content-center rounded-circle flex-shrink-0"
            style="width: 48px; height: 48px; background: #FEF3C7; border: 4px solid #F59E0B;"><svg width="26"
              height="26" fill="#D97706" viewBox="0 0 16 16">
              <path
                d="M8 0a8 8 0 1 0 0 16A8 8 0 0 0 8 0zM5.354 4.646a.5.5 0 1 1-.708.708L7.293 2.5 5.646.854a.5.5 0 1 1 .708-.708L8.293 1.793l2.146-2.147a.5.5 0 0 1 .708.708L8.707 2.5l2.146 2.146a.5.5 0 0 1-.708.708L8 3.207 5.854 5.354z" />
            </svg></div>
          <div class="flex-grow-1" style="min-width: 0;">
            <div class="fw-bold text-dark" style="font-size: 1.05rem; line-height: 1.3;">환자 연결 필요</div>
            <div class="text-muted" style="font-size: 0.8125rem; line-height: 1.3;">환자와 연결하여 위치를 확인하세요</div>
          </div>
          <div class="flex-shrink-0"><button @click="$emit('goToConnect')" class="btn btn-sm btn-outline-primary"
              style="font-size: 0.75rem; padding: 0.25rem 0.5rem;">연결하기</button></div>
        </div>
      </div>
    </div>
  </div>

  <!-- 기능 15: '실종 해제' 시 사용할 ConfirmModal -->
  <ConfirmModal :show="isResolveModalVisible" title="상태 해제 확인" message="환자의 '실종' 상태를 해제하시겠습니까?" confirm-text="상태 해제"
    cancel-text="취소" @close="isResolveModalVisible = false" @cancel="isResolveModalVisible = false"
    @confirm="handleResolveConfirm" />
  <!-- (MissingReportModal은 이 컴포넌트에서 사용하지 않음) -->
</template>

<style scoped>
/* 원본 <style> 코드 전체 유지 */
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

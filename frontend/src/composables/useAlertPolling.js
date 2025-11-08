import { ref, onMounted } from 'vue';
import axios from 'axios';

export const showMissingAlert = ref(false);
export const alertMessage = ref('');
const currentAlertId = ref(null);

// 나이계산 함수
function calculateAge(birthDateString) {
  if (!birthDateString) return '?';
  try {
    const birthDate = new Date(birthDateString);
    if (isNaN(birthDate)) return '?';
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age >= 0 ? age : '?';
  } catch(e) { return '?'; }
}

export function useAlertPolling() {

  async function checkAlerts() {
    try {
      // 백엔드 API 호출
      const response = await axios.get('/api/missing-persons/latest-alert', { withCredentials: true });
      const latestAlert = response.data;
      
      if (latestAlert && latestAlert.missingPostId) {
          const age = calculateAge(latestAlert.patientBirthDate); 

          alertMessage.value = `실종자 발생 ${latestAlert.patientName || '실종자'} (${age}세)\n지금 확인하시겠습니까?`;

          currentAlertId.value = latestAlert.missingPostId;
          showMissingAlert.value = true;
      }

    } catch (err) {
      if (err.response && err.response.status === 204) {
        return;
      }
      if (err.response && err.response.status === 401) {
        return; 
      }
      console.error("알림 확인 중 오류:", err);
    }
  }

  onMounted(() => {
    // (참고: setInterval은 비로그인 사용자도 계속 호출하지만, 서버 API가 401을 반환하거나 204를 반환하므로 모달이 뜨지 않음)
    checkAlerts(); // 즉시 1회
    setInterval(checkAlerts, 10000); // 30초마다 반복
  });
}

// 모달 버튼 처리 함수
export async function handleConfirmAlert() {
  if (currentAlertId.value !== null) {
    try {
      await axios.post(
        `/api/missing-persons/${currentAlertId.value}/confirm-alert`, 
        {}, 
        { withCredentials: true }
      );
    } catch (err) {
      console.error("알림 확인 처리 실패:", err);
    }
  }
  showMissingAlert.value = false;
  currentAlertId.value = null;
}

// export function handleCloseAlert() {
//   // 닫기 버튼은 그냥 닫기만 함 (30초 뒤에 다시 뜸)
//   showMissingAlert.value = false;
//   currentAlertId.value = null;
// }

export async function handleCloseAlert() {
  if (currentAlertId.value !== null) {
    try {
      // "확인" API 호출
      await axios.post(
        `/api/missing-persons/${currentAlertId.value}/confirm-alert`, 
        {}, 
        { withCredentials: true }
      );
    } catch (err) {
      console.error("알림 확인 처리 실패:", err);
    }
  }
  showMissingAlert.value = false;
  currentAlertId.value = null;
}
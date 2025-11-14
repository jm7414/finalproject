import { ref } from 'vue';
import axios from 'axios';

// 이 값들은 App.vue의 ConfirmModal이 사용
export const showMissingAlert = ref(false);
export const alertMessage = ref('');

const currentAlertId = ref(null);
const pollingTimer = ref(null); // setInterval의 ID를 저장할 변수

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
  } catch (e) { return '?'; }
}

//실제로 백엔드에 알림이 있는지 확인하는 함수
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
    if (err.response && (err.response.status === 204 || err.response.status === 401)) {
      // 204 (새 알림 없음) 또는 401 (로그인 안함)은 정상 응답이므로 오류 로그를 찍지 않음
      return;
    }
    console.error("알림 확인 중 오류:", err);
  }
}

export function startAlertPolling(userRole) { // <-- userRole 인자 받기
  if (pollingTimer.value) return; // 이미 실행 중이면 중복 방지

  // 역할 2(환자) 또는 3(구독자)이면 폴링을 시작하지 않음
  if (userRole === 2 || userRole === 3) {
    console.log(`🔕 [useAlertPolling] 사용자 역할(${userRole})은 알림 감시를 시작하지 않습니다.`);
    return; // 함수 종료
  }

  console.log("🔔 [useAlertPolling] 실종 알림 감시를 시작합니다.");
  checkAlerts(); // 즉시 1회 실행
  pollingTimer.value = setInterval(checkAlerts, 10000);
}

//모달에서 '확인' 버튼을 눌렀을 때
export async function handleConfirmAlert() {
  if (currentAlertId.value !== null) {
    try {
      // '확인' API를 호출하여, 다시 알림이 뜨지 않도록 함
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

//모달에서 닫기 버튼을 눌렀을 때
export async function handleCloseAlert() {
  if (currentAlertId.value !== null) {
    try {
      // '확인' API를 호출하여, 다시 알림이 뜨지 않도록 함
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
// MissingDetailModal.vue
<script setup>
import { defineProps, defineEmits } from 'vue';

// 부모로부터 받을 데이터 정의 (person 객체)
const props = defineProps({
  person: {
    type: Object,
    required: true,
  },
});

// 부모로 전달할 이벤트 정의
const emit = defineEmits(['close', 'join-search']);

// '닫기' 버튼 클릭 시
function closeModal() {
  emit('close');
}

// '함께 찾기' 버튼 클릭 시 (ID 전달)
function joinSearch() {
  if (props.person && props.person.missingPostId !== null) {
    emit('join-search', props.person.missingPostId);
  } else {
    console.error("MissingPostId is missing in person data:", props.person);
    // 사용자에게 알림 추가 가능
  }
}

// CommunityMissing.vue에 있던 함수 재사용 (필요 시)
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

function formatDescription(desc) {
  if (!desc) return '상세 정보 없음';
  return String(desc).replace(/\\n/g, '\n');
}

// 기본 이미지 경로 (CommunityMissing.vue와 동일하게 설정)
const defaultPersonImage = '/default-person.png';
</script>

<template>
  <div class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <button class="close-button" @click="closeModal">&times;</button>

      <h2>실종자 상세 정보</h2>

      <div class="modal-body">
        <section class="info-section">
          <h3>실종자 사진</h3>
          <div class="photo-container">
            <img :src="person.photoPath || defaultPersonImage" :alt="person.patientName" class="missing-photo">
          </div>
        </section>

        <section class="info-section">
          <h3>실종자 정보</h3>
          <div class="info-box">
            <div class="info-row">
              <span>이름</span>
              <span>{{ person.patientName || '정보 없음' }}</span>
            </div>
            <div class="info-row">
              <span>나이</span>
              <span>{{ calculateAge(person.patientBirthDate) }}세</span>
            </div>
            <div class="info-row detail-description">
              <span>상세정보</span>
              <p>{{ formatDescription(person.description) }}</p>
            </div>
          </div>
        </section>
      </div>

      <div class="modal-footer">
        <button class="action-button secondary" @click="closeModal">닫기</button>
        <button class="action-button primary" @click="joinSearch">함께 찾기</button>
      </div>
    </div>
  </div>
</template>


<style scoped>
/* 모달 배경 */
.modal-overlay {
  position: fixed; /* 화면 전체 고정 */
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6); /* 반투명 검정 배경 */
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000; /* 다른 요소들 위에 표시 */
}

/* 모달 컨텐츠 영역 */
.modal-content {
  background-color: white;
  padding: 10px;
  border-radius: 12px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  width: 90%; /* 화면 너비의 90% */
  max-width: 450px; /* 최대 너비 제한 */
  height: 80%; /* 최대 높이 제한 (화면 높이의 85%) */
  display: flex;
  flex-direction: column;
  position: relative; /* 닫기 버튼 기준점 */
}

/* 닫기 버튼 */
.close-button {
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  color: #888;
}
.close-button:hover {
  color: #333;
}

/* 모달 제목 */
.modal-content h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  color: #333;
}

/* 모달 본문 (스크롤 가능 영역) */
.modal-body {
  overflow-y: auto; /* 내용 길면 스크롤 */
  margin-bottom: 24px;

}

/* 모달 내 정보 섹션 (CommunityMissingDetail/PredictLocation 스타일 재사용) */
.modal-body .info-section { margin-bottom: 20px; }
.modal-body .info-section h3 { font-size: 15px; font-weight: 600; color: #555; margin: 0 0 10px 0; }
.modal-body .photo-container {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
  background-color: #f0f0f0;
}

/* 모달 내 실종자 사진 이미지 */
.modal-body .missing-photo {
  display: block; /* 이미지 하단 공백 제거 */
  width: 100%;
  height: auto; /* 비율 유지 */
  max-height: 300px; /* 모달에서는 사진 크기 제한 */
  object-fit: contain; /* 비율 유지하며 채우기 */
}

/* 모달 내 실종자 정보 박스 */
.modal-body .info-box {
  background: #FAFAFA;
  border-radius: 6px;
  padding: 16px;
  font-size: 14px;
}

/* 모달 내 정보 행 */
.modal-body .info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}
.modal-body .info-row.detail-description p { max-height: 150px; overflow-y: auto; } /* 긴 설명 스크롤 (선택사항) */

/* 모달 하단 버튼 영역 */
.modal-footer {
  display: flex;
  gap: 12px;
  margin-top: auto; /* 내용을 아래로 밀어냄 */
  padding-top: 16px;
  border-top: 1px solid #eee;
}

/* 모달 내 액션 버튼 (CommunityMissingDetail 스타일 재사용) */
.modal-footer .action-button {
  flex: 1; /* 버튼 너비 균등 분할 */
  padding: 12px;
  border-radius: 8px;
  border: none;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
}
.modal-footer .action-button.primary { background: #8E97FD; color: #FFFFFF; }
.modal-footer .action-button.secondary { background: #E5E5E5; color: #404040; }
</style>
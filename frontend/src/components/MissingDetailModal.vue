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

// 상세정보 출력용
const descriptionLabels = {
  appearance: "인상착의",
  hair: "두발상태",
  health: "건강상태/병력",
  items: "소지품",
  other: "기타 특이사항"
};

function formatDescription(jsonString) {
  if (!jsonString) return '정보 없음';

  try {
    // 1. JSON 문자열을 실제 객체로 변환합니다.
    const data = JSON.parse(jsonString);
    const parts = []; // 한글 라인들을 저장할 배열

    // 2. 라벨 순서대로(appearance, hair 등) 값을 확인하고 배열에 추가합니다.
    for (const key in descriptionLabels) {
      const value = data[key];
      
      // 3. 값이 비어있지 않은 항목만 추가합니다.
      if (value) {
        const label = descriptionLabels[key]; // 한글 라벨 (예: "인상착의")
        parts.push(`- ${label}: ${value}`);
      }
    }

    if (parts.length === 0) return '상세 정보가 없습니다.';
    
    // 4. 모든 항목을 "줄바꿈(\n)" 문자로 합쳐서 반환합니다.
    return parts.join('\n');

  } catch (e) {
    // 5. 만약 JSON이 아닌 일반 텍스트가 들어오면, 그냥 \n만 처리해서 반환합니다.
    console.error("설명란(description) JSON 파싱 실패:", e, jsonString);
    return jsonString.replace(/\\n/g, '\n');
  }
}

// '닫기' 버튼 클릭 시
function closeModal() {
  emit('close');
}

// '함께 찾기' 버튼 클릭 시 (ID 전달)
function joinSearch() {

if (props.person.currentUserJoined === true) {
    // 이미 참여한 사람은 경고창 없이 미싱 커뮤니티에 요청보내기
    emit('join-search', props.person.missingPostId);
    return; // 함수 종료
  }

  // --- 2. 신규 참여자인 경우 경고창 띄우기 ---
  const isConfirmed = confirm("위치가 함께찾는사람들에게 공유됩니다.\n위 사실을 동의하십니까?");

  if (isConfirmed) {
    // 사용자가 동의했니까 미싱 커뮤니티에 요청 보내기
    if (props.person && props.person.missingPostId !== null) {
      emit('join-search', props.person.missingPostId);
    } else {
      console.error("MissingPostId is missing in person data:", props.person);
      alert("오류: 게시물 ID가 없어 '함께 찾기'를 진행할 수 없습니다.");
    }
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
          <div class="info-row">
            <span>함께하는 사람 수 : </span>
            <span>{{ person.searchTogetherCount || 0 }}명</span>
          </div>
          <div class="info-box">
            <div class="info-row">
              <span>이름</span>
              <span>{{ person.patientName || '정보 없음' }}</span>
            </div>
            <div class="info-row">
              <span>나이</span>
              <span>{{ calculateAge(person.patientBirthDate) }}세</span>
            </div>
            <div class="detail-description">
              <p>상세정보</p>
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
  z-index: 1050;; /* 다른 요소들 위에 표시 */
}

/* 모달 컨텐츠 영역 */
.modal-content {
  background-color: white;
  padding: 10px;
  border-radius: 12px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  width: 90%; /* 화면 너비의 90% */
  max-width: 350px; /* 최대 너비 제한 */
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
  margin-bottom: 10px;
  flex: 1;
  min-height: 0;
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

/* '상세정보' 제목 <p> (첫 번째 p) */
.modal-body .detail-description p:first-child {
  margin: 0 0 3px; 
}

/* '상세정보' 내용 <p> (두 번째 p) */
.modal-body .detail-description p:last-child {
  margin: 0; 
  max-height: 150px;
  overflow-y: auto;
  white-space: pre-wrap; 
  word-break: break-all;
  line-height: 1.6;
}

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
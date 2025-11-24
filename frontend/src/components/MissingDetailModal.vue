<script setup>
import { defineProps, defineEmits, computed } from 'vue';

// ✅ theme props 추가
const props = defineProps({
  person: {
    type: Object,
    required: true,
  },
  theme: {
    type: String,
    default: 'guardian',
    validator: (value) => ['guardian', 'neighbor'].includes(value)
  }
});

// ✅ 테마별 색상
const themeColors = computed(() => {
  return props.theme === 'neighbor' 
    ? {
        primary: '#a7cc10',
        activeText: '#171717',
        inactiveText: '#737373'
      }
    : {
        primary: '#8E97FD',
        activeText: '#171717',
        inactiveText: '#737373'
      };
});

const emit = defineEmits(['close', 'join-search']);

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
    const data = JSON.parse(jsonString);
    const parts = [];

    for (const key in descriptionLabels) {
      const value = data[key];
      
      if (value) {
        const label = descriptionLabels[key];
        parts.push(`- ${label}: ${value}`);
      }
    }

    if (parts.length === 0) return '상세 정보가 없습니다.';
    
    return parts.join('\n');

  } catch (e) {
    console.error("설명란(description) JSON 파싱 실패:", e, jsonString);
    return jsonString.replace(/\\n/g, '\n');
  }
}

function closeModal() {
  emit('close');
}

function joinSearch() {
  if (props.person.currentUserJoined === true) {
    emit('join-search', props.person.missingPostId);
    return;
  }

  const isConfirmed = confirm("위치가 함께찾는사람들에게 공유됩니다.\n위 사실을 동의하십니까?");

  if (isConfirmed) {
    if (props.person && props.person.missingPostId !== null) {
      emit('join-search', props.person.missingPostId);
    } else {
      console.error("MissingPostId is missing in person data:", props.person);
      alert("오류: 게시물 ID가 없어 '함께 찾기'를 진행할 수 없습니다.");
    }
  }
}

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
        <button class="action-button primary" 
                :style="{ background: themeColors.primary }"
                @click="joinSearch">
          함께 찾기
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1050;
}

.modal-content {
  background-color: white;
  padding: 10px;
  border-radius: 12px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 350px;
  height: 80%;
  display: flex;
  flex-direction: column;
  position: relative;
}

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

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  color: #333;
}

.modal-body {
  overflow-y: auto;
  margin-bottom: 10px;
  flex: 1;
  min-height: 0;
}

.modal-body .info-section { margin-bottom: 20px; }
.modal-body .info-section h3 { font-size: 15px; font-weight: 600; color: #555; margin: 0 0 10px 0; }
.modal-body .photo-container {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
  background-color: #f0f0f0;
}

.modal-body .missing-photo {
  display: block;
  width: 100%;
  height: auto;
  max-height: 300px;
  object-fit: contain;
}

.modal-body .info-box {
  background: #FAFAFA;
  border-radius: 6px;
  padding: 16px;
  font-size: 14px;
}

.modal-body .info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.modal-body .detail-description p:first-child {
  margin: 0 0 3px; 
}

.modal-body .detail-description p:last-child {
  margin: 0; 
  max-height: 150px;
  overflow-y: auto;
  white-space: pre-wrap; 
  word-break: break-all;
  line-height: 1.6;
}

.modal-footer {
  display: flex;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.modal-footer .action-button {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  border: none;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  transition: opacity 0.2s ease;
}

.modal-footer .action-button:hover {
  opacity: 0.9;
}

.modal-footer .action-button.primary { 
  color: #FFFFFF; 
}

.modal-footer .action-button.secondary { 
  background: #E5E5E5; 
  color: #404040; 
}
</style>

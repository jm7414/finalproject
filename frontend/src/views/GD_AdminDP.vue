<template>
  <div class="patient-admin-page bg-white">
    <!-- 상단 프로필 -->
    <div class="profile-section bg-light py-2">
      <div class="profile-container">
        <img :src="logoPath" alt="Logo" class="logo-icon mb-0" />
        <div class="d-flex align-items-center justify-content-center">
          <div class="user-avatar me-2 mb-2">
            <i class="bi bi-person-circle text-secondary"></i>
          </div>
          <div class="text-start mb-2">
            <h6 class="mb-0 fw-normal">환자 정보 관리</h6>
            <p class="text-secondary mb-0 small">{{ patientName }}님의 정보를 관리합니다</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 메인 컨텐츠 -->
    <div class="content-section py-3">
      <div class="container">
        <!-- 복약 정보 -->
        <div class="info-card mb-3">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h6 class="mb-0 fw-semibold">
              <i class="bi bi-capsule me-2 text-primary"></i>복약 정보
            </h6>
            <button class="btn btn-sm btn-edit" @click="toggleEdit('medication')">
              <i class="bi" :class="editMode.medication ? 'bi-check-lg' : 'bi-pencil'"></i>
              {{ editMode.medication ? '완료' : '수정' }}
            </button>
          </div>
          <div class="card-body">
            <div v-if="!editMode.medication" class="info-display">
              <p class="info-text">{{ patientInfo.medication || '등록된 복약 정보가 없습니다.' }}</p>
            </div>
            <div v-else class="info-edit">
              <textarea 
                v-model="patientInfo.medication" 
                class="form-control" 
                rows="4" 
                placeholder="복용 중인 약과 복용 방법을 입력하세요.&#10;예) 아침 식후 혈압약 1알, 저녁 식후 당뇨약 2알"
              ></textarea>
            </div>
          </div>
        </div>

        <!-- 특이사항 -->
        <div class="info-card mb-3">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h6 class="mb-0 fw-semibold">
              <i class="bi bi-exclamation-circle me-2 text-warning"></i>특이사항
            </h6>
            <button class="btn btn-sm btn-edit" @click="toggleEdit('notes')">
              <i class="bi" :class="editMode.notes ? 'bi-check-lg' : 'bi-pencil'"></i>
              {{ editMode.notes ? '완료' : '수정' }}
            </button>
          </div>
          <div class="card-body">
            <div v-if="!editMode.notes" class="info-display">
              <p class="info-text">{{ patientInfo.notes || '등록된 특이사항이 없습니다.' }}</p>
            </div>
            <div v-else class="info-edit">
              <textarea 
                v-model="patientInfo.notes" 
                class="form-control" 
                rows="4" 
                placeholder="알레르기, 주의사항, 건강 상태 등을 입력하세요.&#10;예) 페니실린 알레르기, 고혈압 주의, 무릎 관절 약함"
              ></textarea>
            </div>
          </div>
        </div>

        <!-- 최근 모습 (사진) -->
        <div class="info-card mb-3">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h6 class="mb-0 fw-semibold">
              <i class="bi bi-camera me-2 text-success"></i>최근 모습
            </h6>
            <button class="btn btn-sm btn-edit" @click="toggleEdit('photo')">
              <i class="bi" :class="editMode.photo ? 'bi-check-lg' : 'bi-pencil'"></i>
              {{ editMode.photo ? '완료' : '수정' }}
            </button>
          </div>
          <div class="card-body">
            <div v-if="!editMode.photo" class="photo-display">
              <div v-if="patientInfo.photos.length > 0" class="photo-grid">
                <div v-for="(photo, index) in patientInfo.photos" :key="index" class="photo-item">
                  <img :src="photo.url" :alt="photo.name" class="photo-img" />
                  <div class="photo-date">{{ photo.date }}</div>
                </div>
              </div>
              <div v-else class="no-photo">
                <i class="bi bi-image text-secondary"></i>
                <p class="text-secondary mb-0 mt-2">등록된 사진이 없습니다</p>
              </div>
            </div>
            <div v-else class="photo-edit">
              <div class="mb-3">
                <label class="form-label small">사진 추가</label>
                <input 
                  type="file" 
                  class="form-control form-control-sm" 
                  accept="image/*" 
                  @change="handlePhotoUpload"
                  multiple
                />
                <small class="text-secondary">여러 장 선택 가능</small>
              </div>
              <div v-if="patientInfo.photos.length > 0" class="photo-list">
                <div v-for="(photo, index) in patientInfo.photos" :key="index" class="photo-list-item">
                  <img :src="photo.url" :alt="photo.name" class="photo-thumbnail" />
                  <span class="photo-name">{{ photo.name }}</span>
                  <button class="btn btn-sm btn-delete" @click="removePhoto(index)">
                    <i class="bi bi-trash"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 하단 버튼 -->
        <div class="action-buttons">
          <button class="btn btn-back w-100 py-2 mb-2" @click="goBack">
            <i class="bi bi-arrow-left me-2"></i>뒤로 가기
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const logoPath = '/mammamialogo.png'

// 보호자 및 환자 정보
const guardianNo = ref(null)
const patientData = ref(null)

// DB에서 환자 이름 가져오기
const patientName = computed(() => {
  return patientData.value?.name || '환자'
})

// 수정 모드 상태
const editMode = ref({
  medication: false,
  notes: false,
  photo: false
})

// 환자 정보 (로컬 상태 관리)
const patientInfo = ref({
  medication: '',
  notes: '',
  photos: []
})

// 보호자 정보 로드
const loadGuardianInfo = async () => {
  try {
    const response = await axios.get('/api/user/me')
    guardianNo.value = response.data.userNo
    
    if (guardianNo.value) {
      localStorage.setItem('guardianNo', String(guardianNo.value))
    }
  } catch (error) {
    console.error('보호자 정보 로드 실패:', error)
    guardianNo.value = Number(localStorage.getItem('guardianNo') || 0) || null
  }
}

// 연결된 환자 정보 로드
const loadPatientInfo = async () => {
  if (!guardianNo.value) return
  
  try {
    const summaryResponse = await axios.get('/api/subscriptions/summary', {
      params: { guardianNo: guardianNo.value },
      headers: { 'X-Mock-User': String(guardianNo.value) }
    })
    
    const patientNo = summaryResponse.data.patientNo
    
    if (patientNo) {
      const patientResponse = await axios.get(`/api/connect/patient/${patientNo}/basic`)
      patientData.value = patientResponse.data
    }
  } catch (error) {
    console.error('환자 정보 로드 실패:', error)
  }
}

// 수정 모드 토글
const toggleEdit = (field) => {
  editMode.value[field] = !editMode.value[field]
  
  if (!editMode.value[field]) {
    saveToLocalStorage()
  }
}

// 사진 업로드 처리
const handlePhotoUpload = (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return

  Array.from(files).forEach(file => {
    const reader = new FileReader()
    reader.onload = (e) => {
      patientInfo.value.photos.push({
        name: file.name,
        url: e.target.result,
        date: new Date().toLocaleDateString('ko-KR')
      })
    }
    reader.readAsDataURL(file)
  })

  event.target.value = ''
}

// 사진 삭제
const removePhoto = (index) => {
  patientInfo.value.photos.splice(index, 1)
}

// 로컬스토리지에 저장
const saveToLocalStorage = () => {
  const key = `patientInfo_${guardianNo.value}`
  localStorage.setItem(key, JSON.stringify(patientInfo.value))
}

// 로컬스토리지에서 불러오기
const loadFromLocalStorage = () => {
  if (!guardianNo.value) return
  
  const key = `patientInfo_${guardianNo.value}`
  const saved = localStorage.getItem(key)
  if (saved) {
    patientInfo.value = JSON.parse(saved)
  }
}

// 초기화
onMounted(async () => {
  await loadGuardianInfo()
  await loadPatientInfo()
  loadFromLocalStorage()
})

// 뒤로 가기
const goBack = () => {
  router.back()
}
</script>

<style scoped>
/* ===== 색상 강제 ===== */
.patient-admin-page {
  color: #171717;
}
.patient-admin-page a {
  color: inherit !important;
}
.patient-admin-page .text-secondary {
  color: #6c757d !important;
}

/* 레이아웃 */
.patient-admin-page {
  height: 640px;
  max-width: 480px;
  margin: 0 auto;
  font-size: 0.9rem;
  transform: scale(1.0);
  transform-origin: top center;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  margin-top: -14px;
}

.profile-section {
  background-color: #FAFAFA;
  padding-top: 0.5rem !important;
  padding-bottom: 0.5rem !important;
  width: 100%;
}

.profile-container {
  width: 100%;
  text-align: center;
  padding: 0;
  margin: 0;
}

.logo-icon {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.user-avatar i {
  font-size: 40px;
}

.user-avatar {
  border-radius: 50%;
  overflow: hidden;
}

.content-section {
  flex: 1;
}

/* 정보 카드 */
.info-card {
  background: #fff;
  border: 1px solid #E5E5E5;
  border-radius: 12px;
  overflow: hidden;
}

.card-header {
  background: #FAFAFA;
  border-bottom: 1px solid #E5E5E5;
  padding: 12px 16px;
}

.card-body {
  padding: 16px;
}

.text-primary {
  color: rgba(74, 98, 221, 0.85) !important;
}

.text-warning {
  color: #f59e0b !important;
}

.text-success {
  color: #10b981 !important;
}

/* 수정 버튼 */
.btn-edit {
  background: rgba(74, 98, 221, 0.85);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.btn-edit:hover {
  background: rgba(74, 98, 221, 1);
  transform: translateY(-1px);
}

/* 정보 표시 */
.info-display {
  min-height: 60px;
}

.info-text {
  color: #171717;
  line-height: 1.6;
  white-space: pre-wrap;
  margin: 0;
}

/* 정보 수정 */
.form-control {
  border: 1px solid #E5E5E5;
  border-radius: 8px;
  font-size: 0.9rem;
}

.form-control:focus {
  border-color: rgba(74, 98, 221, 0.85);
  box-shadow: 0 0 0 0.2rem rgba(74, 98, 221, 0.15);
}

/* 사진 표시 */
.photo-display {
  min-height: 120px;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #E5E5E5;
}

.photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-date {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 4px;
  font-size: 0.7rem;
  text-align: center;
}

.no-photo {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.no-photo i {
  font-size: 48px;
}

/* 사진 수정 */
.photo-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.photo-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  background: #FAFAFA;
  border-radius: 8px;
  border: 1px solid #E5E5E5;
}

.photo-thumbnail {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 6px;
}

.photo-name {
  flex: 1;
  font-size: 0.85rem;
  color: #171717;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-delete {
  background: #FF6B6B;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 4px 8px;
  transition: all 0.2s;
}

.btn-delete:hover {
  background: #FF5252;
}

/* 하단 버튼 */
.action-buttons {
  margin-top: 16px;
}

.btn-back {
  background-color: #e5e7eb;
  border: none;
  border-radius: 8px;
  color: #171717;
  font-size: 0.95rem;
  transition: all 0.2s;
}

.btn-back:hover {
  background-color: #d1d5db;
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .container {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>

<template>
  <div class="desktop-mypage-admin">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">환자 정보 관리</h1>
        <p class="page-subtitle">{{ patientName }}님의 정보를 관리합니다</p>
      </div>

      <div class="content-wrapper">
        <!-- 복약 정보 -->
        <div class="info-card">
          <div class="card-header">
            <div class="header-left">
              <i class="bi bi-capsule"></i>
              <h3 class="card-title">복약 정보</h3>
            </div>
            <button class="btn-edit" @click="toggleEdit('medication')">
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
        <div class="info-card">
          <div class="card-header">
            <div class="header-left">
              <i class="bi bi-exclamation-circle"></i>
              <h3 class="card-title">특이사항</h3>
            </div>
            <button class="btn-edit" @click="toggleEdit('notes')">
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
        <div class="info-card">
          <div class="card-header">
            <div class="header-left">
              <i class="bi bi-camera"></i>
              <h3 class="card-title">최근 모습</h3>
            </div>
            <button class="btn-edit" @click="toggleEdit('photo')">
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
                <i class="bi bi-image"></i>
                <p>등록된 사진이 없습니다</p>
              </div>
            </div>
            <div v-else class="photo-edit">
              <div class="upload-section">
                <label class="upload-label">사진 추가</label>
                <input 
                  type="file" 
                  class="form-control" 
                  accept="image/*" 
                  @change="handlePhotoUpload"
                  multiple
                />
                <small class="upload-hint">여러 장 선택 가능</small>
              </div>
              <div v-if="patientInfo.photos.length > 0" class="photo-list">
                <div v-for="(photo, index) in patientInfo.photos" :key="index" class="photo-list-item">
                  <img :src="photo.url" :alt="photo.name" class="photo-thumbnail" />
                  <span class="photo-name">{{ photo.name }}</span>
                  <button class="btn-delete" @click="removePhoto(index)">
                    <i class="bi bi-trash"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 저장 버튼 -->
        <div class="button-section">
          <button class="btn-save" @click="goBack">
            <i class="bi bi-check-circle me-2"></i>저장
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

const guardianNo = ref(null)
const patientData = ref(null)

const patientName = computed(() => {
  return patientData.value?.name || '환자'
})

const editMode = ref({
  medication: false,
  notes: false,
  photo: false
})

const patientInfo = ref({
  medication: '',
  notes: '',
  photos: []
})

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

const toggleEdit = (field) => {
  editMode.value[field] = !editMode.value[field]
  
  if (!editMode.value[field]) {
    saveToLocalStorage()
  }
}

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

const removePhoto = (index) => {
  patientInfo.value.photos.splice(index, 1)
}

const saveToLocalStorage = () => {
  const key = `patientInfo_${guardianNo.value}`
  localStorage.setItem(key, JSON.stringify(patientInfo.value))
}

const loadFromLocalStorage = () => {
  if (!guardianNo.value) return
  
  const key = `patientInfo_${guardianNo.value}`
  const saved = localStorage.getItem(key)
  if (saved) {
    patientInfo.value = JSON.parse(saved)
  }
}

const goBack = () => {
  saveToLocalStorage()
  router.push('/desktop/mypage')
}

onMounted(async () => {
  await loadGuardianInfo()
  await loadPatientInfo()
  loadFromLocalStorage()
})
</script>

<style scoped>
.desktop-mypage-admin {
  width: 100%;
  min-height: calc(100vh - 80px);
  padding: 0;
}

.page-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #171717;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #737373;
  margin: 0;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-header {
  background: #fafafa;
  border-bottom: 1px solid #e5e7eb;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left i {
  font-size: 20px;
  color: rgba(74, 98, 221, 0.85);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #171717;
  margin: 0;
}

.btn-edit {
  background: rgba(74, 98, 221, 0.85);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 6px 16px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-edit:hover {
  background: rgba(74, 98, 221, 1);
  transform: translateY(-1px);
}

.card-body {
  padding: 24px;
}

.info-display {
  min-height: 60px;
}

.info-text {
  color: #171717;
  line-height: 1.6;
  white-space: pre-wrap;
  margin: 0;
  font-size: 15px;
}

.form-control {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  font-family: inherit;
  resize: vertical;
}

.form-control:focus {
  outline: none;
  border-color: rgba(74, 98, 221, 0.85);
  box-shadow: 0 0 0 3px rgba(74, 98, 221, 0.1);
}

.photo-display {
  min-height: 120px;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
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
  padding: 6px;
  font-size: 12px;
  text-align: center;
}

.no-photo {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #9ca3af;
}

.no-photo i {
  font-size: 64px;
  margin-bottom: 16px;
}

.no-photo p {
  margin: 0;
  font-size: 15px;
}

.upload-section {
  margin-bottom: 20px;
}

.upload-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #171717;
  margin-bottom: 8px;
}

.upload-hint {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
}

.photo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.photo-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.photo-thumbnail {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
}

.photo-name {
  flex: 1;
  font-size: 14px;
  color: #171717;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-delete {
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete:hover {
  background: #dc2626;
}

.button-section {
  margin-top: 8px;
}

.btn-save {
  width: 100%;
  padding: 14px 24px;
  background: rgba(74, 98, 221, 0.85);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-save:hover {
  background: rgba(74, 98, 221, 1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>

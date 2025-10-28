<template>
  <!-- Modal Backdrop -->
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-backdrop" @click.self="closeModal">
        <div class="my-info-modal bg-light" @click.stop>
          <!-- Close Button -->
          <button class="modal-close-btn" @click="closeModal">
            <i class="bi bi-x-lg"></i>
          </button>

          <!-- Header Section -->
          <div class="header-section bg-white border-bottom">
            <div class="container py-3">
              <div class="d-flex align-items-center">
                <!-- ✅ 수정: 프로필 사진 표시 -->
                <div class="user-avatar me-3">
                  <img v-if="profilePhotoUrl" :src="profilePhotoUrl" alt="Profile" class="profile-photo" />
                  <i v-else class="bi bi-person-circle text-secondary"></i>
                </div>
                <div>
                  <h5 class="mb-1">내 정보</h5>
                  <p class="text-secondary mb-0 small">{{ displayUserName }} 님 안녕하세요</p>
                </div>
              </div>
            </div>
            <div class="divider mx-3"></div>
          </div>

          <!-- Menu Grid Section -->
          <div class="menu-section py-4">
            <div class="container">
              <div class="row g-3">
                <!-- 일정 추가하기 - Large Card (Left) -->
                <div class="col-7">
                  <div class="menu-card menu-card-large h-100" @click="addSchedule">
                    <div class="card-content">
                      <div class="icon-wrapper mb-3">
                        <i class="bi bi-calendar-plus-fill"></i>
                      </div>
                      <h5 class="card-title">일정 추가<br/>하기</h5>
                    </div>
                  </div>
                </div>

                <!-- Right Column -->
                <div class="col-5">
                  <div class="row g-3">
                    <!-- 내 정보 수정 -->
                    <div class="col-12">
                      <div class="menu-card menu-card-small" @click="modifyInfo">
                        <div class="card-content">
                          <div class="icon-wrapper mb-2">
                            <i class="bi bi-person-fill-gear"></i>
                          </div>
                          <p class="card-title small mb-0">내 정보<br/>수정</p>
                        </div>
                      </div>
                    </div>

                    <!-- 보호자 연결코드 -->
                    <div class="col-12">
                      <div class="menu-card menu-card-small" @click="showConnectCode">
                        <div class="card-content">
                          <div class="icon-wrapper mb-2">
                            <i class="bi bi-link-45deg"></i>
                          </div>
                          <p class="card-title small mb-0">보호자<br/>연결코드</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 로그아웃 버튼 -->
              <div class="logout-section mt-3">
                <button class="btn btn-logout w-100" @click="handleLogout">
                  <div class="d-flex align-items-center justify-content-between">
                    <div class="d-flex align-items-center">
                      <i class="bi bi-box-arrow-right me-3"></i>
                      <span>로그아웃</span>
                    </div>
                    <i class="bi bi-chevron-right"></i>
                  </div>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '@/utils/auth'
import axios from 'axios'

// Props
interface Props {
  modelValue: boolean
  userName?: string
}

const props = withDefaults(defineProps<Props>(), {
  userName: '아무개'
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'close': []
}>()

const router = useRouter()

// ✅ 추가: 프로필 사진 URL
const profilePhotoUrl = ref('')

// Computed
const displayUserName = computed(() => props.userName || '아무개')

// ✅ 추가: 프로필 사진 로드
const loadProfilePhoto = async () => {
  try {
    const response = await axios.get('/api/user/me')
    profilePhotoUrl.value = response.data.profilePhoto || ''
  } catch (error) {
    console.error('프로필 사진 로드 실패:', error)
  }
}

// ✅ 추가: 모달이 열릴 때마다 프로필 사진 새로고침
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    loadProfilePhoto()
  }
})

// 초기 로드
onMounted(() => {
  if (props.modelValue) {
    loadProfilePhoto()
  }
})

// 모달 닫기
const closeModal = () => {
  emit('update:modelValue', false)
  emit('close')
}

// 일정 추가하기
const addSchedule = () => {
  console.log('일정 추가하기')
  closeModal()
  router.push('/DP_schedule')
}

// 내 정보 수정
const modifyInfo = () => {
  console.log('내 정보 수정')
  router.push('/dpmodifyinfo')
}

// 보호자 연결코드
const showConnectCode = () => {
  console.log('보호자 연결코드')
  closeModal()
  router.push('/dpc')
}

// 로그아웃 처리
const handleLogout = async () => {
  const success = await logout()
  
  if (success) {
    closeModal()
    router.push('/login')
  } else {
    alert('로그아웃에 실패했습니다.')
  }
}
</script>

<style scoped>
/* Modal Backdrop */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 1rem;
}

/* Modal Container */
.my-info-modal {
  position: relative;
  height: 672px;
  max-width: 400px;
  width: 100%;
  margin: 0 auto;
  font-size: 0.9rem;
  transform: scale(0.9) translateX(-10px);
  transform-origin: center;
  overflow: hidden;
  color: #171717 !important;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

/* Close Button */
.modal-close-btn {
  position: absolute;
  top: 1rem;
  right: 1rem;
  width: 36px;
  height: 36px;
  border: none;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.modal-close-btn:hover {
  background-color: rgba(255, 255, 255, 1);
  transform: scale(1.1);
}

.modal-close-btn i {
  font-size: 1rem;
  color: #6c757d;
}

/* Modal Transitions */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-active .my-info-modal,
.modal-fade-leave-active .my-info-modal {
  transition: transform 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .my-info-modal {
  transform: scale(0.8);
}

.modal-fade-leave-to .my-info-modal {
  transform: scale(0.8);
}

/* Original Styles */
.header-section {
  background-color: white;
  border-bottom: 1px solid #E5E5E5;
}

.header-section h5 {
  color: #171717 !important;
}

.header-section p {
  color: #6c757d !important;
}

/* ✅ 수정: 프로필 사진 스타일 */
.user-avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.user-avatar i {
  font-size: 58px;
  color: #6c757d !important;
}

.profile-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
}

.divider {
  height: 1px;
  background-color: #E5E5E5;
}

.menu-section {
  padding-top: 1.8rem;
  padding-bottom: 1.8rem;
  overflow-y: auto;
  max-height: calc(672px - 120px);
}

.menu-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #E5E5E5;
  box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  overflow: hidden;
}

.menu-card:hover {
  transform: translateY(-2px);
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1);
}

.menu-card-large {
  min-height: 370px;
}

.menu-card-small {
  min-height: 178px;
}

.menu-card .card-content {
  padding: 1.35rem;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.menu-card-large .card-content {
  padding: 2.7rem 1.8rem;
}

.icon-wrapper {
  color: rgba(74, 98, 221, 0.85) !important;
}

.menu-card-large .icon-wrapper i {
  font-size: 54px;
}

.menu-card-small .icon-wrapper i {
  font-size: 43px;
}

.card-title {
  color: #171717 !important;
  font-weight: 400;
  line-height: 1.6;
}

.menu-card-large .card-title {
  font-size: 1.35rem;
}

.menu-card-small .card-title {
  font-size: 1.13rem;
}

.logout-section {
  margin-top: 1.35rem;
}

.btn-logout {
  background-color: rgba(170, 194, 254, 0.91);
  border: 1px solid #E5E5E5;
  border-radius: 16px;
  box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
  padding: 1.35rem;
  color: #737373 !important;
  font-size: 1.13rem;
  transition: all 0.2s;
}

.btn-logout:hover {
  background-color: rgba(170, 194, 254, 1);
  transform: translateY(-1px);
  box-shadow: 0px 3px 6px rgba(0, 0, 0, 0.1);
}

.btn-logout span {
  color: #484848 !important;
}

.btn-logout i {
  font-size: 1.35rem;
  color: #484848 !important;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .container {
    padding-left: 1.35rem;
    padding-right: 1.35rem;
  }
  
  .my-info-modal {
    height: 90vh;
    max-height: 672px;
  }
  
  .modal-backdrop {
    padding: 0.5rem;
  }
}
</style>

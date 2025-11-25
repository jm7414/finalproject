<template>
  <div class="page">
    <!-- 설정된 위치 카드 -->
    <div v-if="hasExistingLocation" class="location-preview">
      <div class="preview-header">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z"
            stroke="#a7cc10" stroke-width="2" />
          <circle cx="12" cy="10" r="3" stroke="#a7cc10" stroke-width="2" />
        </svg>
        <span>설정된 위치</span>
        <button class="change-location-btn" @click="showMethodSelection">변경</button>
      </div>
      <div class="preview-content">
        <div class="preview-name">{{ selectedLocation.place_name }}</div>
        <div class="preview-address">{{ selectedLocation.road_address_name || selectedLocation.address_name }}</div>
      </div>
    </div>

    <!-- 기본 위치가 없는 경우 -->
    <div v-else class="no-location-card">
      <div class="no-location-header">
        <div>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M21 10C21 17 12 23 12 23S3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.3639 3.63604C20.0518 5.32387 21 7.61305 21 10Z"
              stroke="#6b7280" stroke-width="2" />
            <circle cx="12" cy="10" r="3" stroke="#6b7280" stroke-width="2" />
          </svg>
          <span>설정된 위치</span>
        </div>
        <button class="add-location-btn" @click="showMethodSelection">추가하기</button>
      </div>
      <div class="no-location-content">
        <div class="no-location-message">설정된 광장 위치가 없습니다</div>
      </div>
    </div>

    <!-- 위치 설정 방법 선택 -->
    <div v-if="showMethods" class="method-selection">
      <h3 class="method-title">위치 설정 방법을 선택하세요</h3>

      <!-- 방법 1: 직접 검색 -->
      <div class="method-card" :class="{ active: selectedMethod === 'search' }" @click="selectMethod('search')">
        <div class="method-header">
          <div class="method-icon search-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="2" />
              <path d="m21 21-4.35-4.35" stroke="currentColor" stroke-width="2" />
            </svg>
          </div>
          <div class="method-info">
            <h4 class="method-name">직접 검색</h4>
            <p class="method-desc">주소나 장소명을 입력하여 검색</p>
          </div>
        </div>
        <div v-if="selectedMethod === 'search'" class="method-content">
          <div class="input-item">
            <input v-model="locationQuery" type="text" placeholder="예) 서울시 강남구 역삼동" @focus="showResults = true"
              @keyup.enter="searchLocation" />
          </div>
        </div>
      </div>
      <div class="method-card" :class="{ active: selectedMethod === 'current' }" @click="selectMethod('current')">
        <div class="method-header">
          <div class="method-icon current-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" />
              <circle cx="12" cy="12" r="3" fill="currentColor" />
            </svg>
          </div>
          <div class="method-info">
            <h4 class="method-name">내 현 위치</h4>
            <p class="method-desc">현재 내 위치를 기준으로 설정</p>
          </div>
        </div>
        <div v-if="selectedMethod === 'current'" class="method-content">
          <button class="current-location-btn" @click="useMyLocation" :disabled="isLoadingLocation">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" />
              <circle cx="12" cy="12" r="3" fill="currentColor" />
            </svg>
            {{ isLoadingLocation ? '위치 조회 중...' : '현 위치로 설정' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 장소 검색 결과 -->
    <div class="section-title" v-if="showResults && searchResults.length > 0">장소 결과</div>
    <ul class="results" v-show="showResults && searchResults.length > 0">
      <li v-for="place in searchResults" :key="place.id" @click="selectLocation(place)">
        <div class="item-left">
          <div class="name">{{ place.place_name }}</div>
          <div class="addr">{{ place.road_address_name || place.address_name }}</div>
        </div>
        <div class="item-right"><button class="select-btn">선택</button></div>
      </li>
    </ul>

    <!-- 광장 이름 입력 -->
    <div v-if="hasExistingLocation" class="card">
      <div class="input-item">
        <div class="label">광장 이름</div>
        <input v-model="plazaName" type="text" placeholder="예) 우리 동네 광장" maxlength="30" />
      </div>
    </div>

    <!-- 설명 -->
    <div class="description-card">
      <p class="description-text">
        <strong>만남의 광장 만들기</strong><br />
        이웃들과 만나는 장소를 설정하여 광장을 만듭니다.<br />
        광장 범위(50m) 안에 있는 이웃들을 실시간으로 확인할 수 있습니다.
      </p>
    </div>

    <!-- 액션 버튼 -->
    <div class="actions">
      <button class="primary" :disabled="!canProceed || isCreating" @click="createPlaza">
        {{ isCreating ? '광장 만드는 중...' : '광장 만들기' }}
      </button>
      <button class="ghost" @click="confirmCancel">취소</button>
    </div>

    <!-- (지현 수정) 범용 경고/확인 모달 -->
    <CreatePlazaAlertModal :show="alertModal.show" :title="alertModal.title" :message="alertModal.message"
      :type="alertModal.type" :mode="alertModal.mode" @close="closeAlertModal" @confirm="handleAlertConfirm"
      @cancel="handleAlertCancel" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import CreatePlazaAlertModal from '@/components/CreatePlazaAlertModal.vue'

const router = useRouter()

const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'
const locationQuery = ref('')
const searchResults = ref([])
const selectedLocation = ref(null)
const showResults = ref(false)
const isLoadingLocation = ref(false)
const selectedMethod = ref('')
const showMethods = ref(false)
const hasExistingLocation = ref(false)
const plazaName = ref('')
const isCreating = ref(false)

let placesService = null

const alertModal = ref({
  show: false,
  title: '',
  message: '',
  type: 'info',
  mode: 'alert',
  onConfirm: null,
  onCancel: null
})

function showAlert(type, title, message, onConfirm = null) {
  alertModal.value = { show: true, title, message, type, mode: 'alert', onConfirm, onCancel: null }
}
function showConfirm(type, title, message, onConfirm, onCancel = null) {
  alertModal.value = { show: true, title, message, type, mode: 'confirm', onConfirm, onCancel }
}
function closeAlertModal() { alertModal.value.show = false }
function handleAlertConfirm() { closeAlertModal(); if (alertModal.value.onConfirm) alertModal.value.onConfirm() }
function handleAlertCancel() { closeAlertModal(); if (alertModal.value.onCancel) alertModal.value.onCancel() }

onMounted(() => { ensureKakaoPlaces() })

const canProceed = computed(() => selectedLocation.value && plazaName.value.trim().length > 0)

async function useMyLocation() {
  if (isLoadingLocation.value) return
  isLoadingLocation.value = true
  try {
    if (!navigator.geolocation) {
      showAlert('error', '지원 오류', '이 브라우저는 위치 서비스를 지원하지 않습니다.')
      return
    }
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const latitude = position.coords.latitude
        const longitude = position.coords.longitude
        const addressInfo = await reverseGeocode(latitude, longitude)
        selectedLocation.value = {
          place_name: addressInfo.name,
          address_name: addressInfo.address,
          road_address_name: addressInfo.address,
          x: longitude,
          y: latitude
        }
        onLocationSelected()
        isLoadingLocation.value = false
      },
      (error) => {
        showAlert('error', '위치 오류', '현재 위치를 확인할 수 없습니다. 위치 권한을 허용해주세요.')
        isLoadingLocation.value = false
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    )
  } catch (error) {
    showAlert('error', '위치 오류', '현재 위치를 확인할 수 없습니다.')
    isLoadingLocation.value = false
  }
}

function reverseGeocode(latitude, longitude) {
  return new Promise((resolve) => {
    if (!window.kakao || !window.kakao.maps || !window.kakao.maps.services) {
      const fallbackName = `위도: ${latitude.toFixed(6)}, 경도: ${longitude.toFixed(6)}`
      resolve({ name: fallbackName, address: fallbackName })
      return
    }
    const geocoder = new window.kakao.maps.services.Geocoder()
    geocoder.coord2Address(longitude, latitude, (result, status) => {
      if (status === window.kakao.maps.services.Status.OK && result && result[0]) {
        const address = result[0].address
        const addressName = address.address_name || `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`
        resolve({ name: addressName, address: addressName })
      } else {
        const fallbackName = `위도: ${latitude.toFixed(6)}, 경도: ${longitude.toFixed(6)}`
        resolve({ name: fallbackName, address: fallbackName })
      }
    })
  })
}

function ensureKakaoPlaces() {
  if (window.kakao && window.kakao.maps && window.kakao.maps.services) {
    placesService = new window.kakao.maps.services.Places()
    return
  }
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services&autoload=false`
  document.head.appendChild(script)
  script.onload = () => { window.kakao.maps.load(() => { placesService = new window.kakao.maps.services.Places() }) }
}

function searchLocation() {
  if (!locationQuery.value || !placesService) return
  placesService.keywordSearch(locationQuery.value, (data, status) => {
    if (status !== window.kakao.maps.services.Status.OK) {
      searchResults.value = []
      return
    }
    searchResults.value = data.slice(0, 10)
    showResults.value = true
  })
}

function showMethodSelection() {
  showMethods.value = true
  selectedMethod.value = ''
  locationQuery.value = ''
  showResults.value = false
  searchResults.value = []
}

function selectMethod(method) {
  selectedMethod.value = method
  if (method === 'search') {
    showResults.value = false
    searchResults.value = []
    locationQuery.value = ''
  }
}

function selectLocation(place) {
  selectedLocation.value = place
  locationQuery.value = place.place_name
  showResults.value = false
  searchResults.value = []
  onLocationSelected()
}

function onLocationSelected() {
  showMethods.value = false
  selectedMethod.value = ''
  hasExistingLocation.value = true
}

async function createPlaza() {
  if (!selectedLocation.value) {
    showAlert('warning', '입력 오류', '위치를 선택해주세요.')
    return
  }
  if (!plazaName.value.trim()) {
    showAlert('warning', '입력 오류', '광장 이름을 입력해주세요.')
    return
  }
  if (isCreating.value) return
  isCreating.value = true
  try {
    const response = await axios.post('/NH/api/neighbor/plazas/create', {
      plazaName: plazaName.value.trim(),
      centerLat: parseFloat(selectedLocation.value.y),
      centerLng: parseFloat(selectedLocation.value.x),
      radiusMeters: 50
    })
    showAlert('success', '등록 완료', '광장이 성공적으로 만들어졌습니다! 🎉', () => {
      sessionStorage.removeItem('plazaLocation')
      router.push(`/plazaDetail/${response.data.plazaNo}`)
    })
  } catch (error) {
    if (error.response?.data?.message) {
      showAlert('error', '오류', error.response.data.message)
    } else if (error.response?.status === 400) {
      showAlert('error', '오류', '이미 광장에 속해 있습니다. 광장은 1개만 소속 가능합니다.')
    } else {
      showAlert('error', '오류', '광장 생성에 실패했습니다.')
    }
  } finally {
    isCreating.value = false
  }
}

function confirmCancel() {
  showConfirm('warning', '취소 확인', '정말 취소하시겠습니까?', () => {
    sessionStorage.removeItem('plazaLocation')
    router.back()
  })
}
</script>


<style scoped>
@import url('https://cdn.jsdelivr.net/gh/sunn-us/SUIT/fonts/static/woff2/SUIT.css');

* {
  font-family: 'SUIT', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.page {
  min-height: auto;
  max-height: 100vh;
  background: #f9fafb;
  padding: 20px 0;
  position: relative;
  z-index: 1;
  margin-top: -30px;
  overflow-y: auto;
}

.location-preview {
  margin: 20px;
  padding: 16px;
  background: #f5f9e8;
  border: 2px solid #c2d477;
  border-radius: 12px;
  position: relative;
  z-index: 2;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.preview-header svg {
  margin-right: 6px;
  vertical-align: middle;
}

.change-location-btn {
  margin-left: auto;
  padding: 6px 14px;
  background: #fff;
  color: #a7cc10;
  border: 2px solid #a7cc10;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.change-location-btn:hover {
  background: #a7cc10;
  color: #fff;
}

.preview-content {
  padding-left: 28px;
}

.preview-name {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.preview-address {
  font-size: 14px;
  color: #6b7280;
}

.no-location-card {
  margin: 20px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  position: relative;
  z-index: 2;
}

.no-location-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.no-location-header>div {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
}

.add-location-btn {
  padding: 6px 14px;
  background: #a7cc10;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.add-location-btn:hover {
  background: #8fb80e;
}

.no-location-content {
  padding-left: 28px;
}

.no-location-message {
  font-size: 14px;
  color: #6b7280;
  font-style: italic;
}

.method-selection {
  margin: 20px;
  position: relative;
  z-index: 2;
}

.method-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 16px 0;
}

.method-card {
  background: #fff;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
}

.method-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.method-card.active {
  border-color: #a7cc10;
  box-shadow: 0 0 0 3px rgba(167, 204, 16, 0.1);
}

.method-header {
  display: flex;
  align-items: center;
  padding: 16px;
  gap: 12px;
}

.method-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.search-icon {
  background: #f5f9e8;
  color: #a7cc10;
}

.current-icon {
  background: #f0fdf4;
  color: #16a34a;
}

.method-info {
  flex: 1;
}

.method-name {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
}

.method-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.method-content {
  padding: 0 16px 16px 16px;
}

.current-location-btn {
  width: 100%;
  padding: 12px 16px;
  background: #a7cc10;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.current-location-btn:hover:not(:disabled) {
  background: #8fb80e;
}

.current-location-btn:disabled {
  background: #e5e7eb;
  color: #6b7280;
  cursor: not-allowed;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin: 0 20px 12px 20px;
}

.results {
  list-style: none;
  padding: 0;
  margin: 0 20px 20px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.results li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background-color 0.2s;
}

.results li:last-child {
  border-bottom: none;
}

.results li:hover {
  background: #f9fafb;
}

.item-left {
  flex: 1;
}

.name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.addr {
  font-size: 13px;
  color: #6b7280;
}

.select-btn {
  padding: 6px 16px;
  background: #a7cc10;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.select-btn:hover {
  background: #8fb80e;
}

.card {
  margin: 20px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.input-item {
  margin-bottom: 0;
}

.label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #c2d477;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s;
  box-sizing: border-box;
}

input:focus {
  outline: none;
  border-color: #a7cc10;
  box-shadow: 0 0 0 3px rgba(167, 204, 16, 0.1);
}

.description-card {
  margin: 20px;
  padding: 16px;
  background: #f5f9e8;
  border: 1px solid #c2d477;
  border-radius: 12px;
  position: relative;
  z-index: 2;
}

.description-text {
  font-size: 14px;
  color: #5a8f0d;
  line-height: 1.6;
  margin: 0;
}

.actions {
  margin: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  gap: 12px;
  margin-bottom: 100px;
}

.primary,
.ghost {
  flex: 1;
  padding: 14px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.primary {
  background: #a7cc10;
  color: #fff;
  border: none;
}

.primary:hover:not(:disabled) {
  background: #8fb80e;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(167, 204, 16, 0.3);
}

.primary:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.ghost {
  background: #fff;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.ghost:hover {
  background: #f9fafb;
  border-color: #a7cc10;
  color: #a7cc10;
}

/* 모달 스타일 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 350px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  text-align: center;
  padding: 24px;
}

.modal-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.icon-success i {
  color: #a7cc10;
}

.icon-error i {
  color: #e74c3c;
}

.icon-warning i {
  color: #f39c12;
}

.icon-info i {
  color: #3498db;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 12px;
  color: #333;
}

.modal-message {
  font-size: 15px;
  color: #555;
  margin: 0 0 20px;
  line-height: 1.5;
  white-space: pre-line;
}

.modal-buttons {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.modal-btn {
  flex: 1;
  padding: 12px 0;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.modal-btn:hover {
  opacity: 0.9;
}

.btn-cancel {
  background: #e74c3c;
  color: #fff;
}

.btn-success {
  background: #a7cc10;
  color: #fff;
}

.btn-error {
  background: #e74c3c;
  color: #fff;
}

.btn-warning {
  background: #f39c12;
  color: #fff;
}

.btn-info {
  background: #3498db;
  color: #fff;
}
</style>

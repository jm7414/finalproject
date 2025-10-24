import { ref, nextTick } from 'vue'

/**
 * 카카오 지도 관리 composable
 * @param {Object} options - 설정 옵션
 * @param {string} options.kakaoKey - 카카오 지도 API 키
 * @param {Object} options.center - 기본 중심 좌표 { lat, lng }
 * @param {number} options.defaultLevel - 기본 줌 레벨 (기본: 3)
 * @param {boolean} options.enableControls - 줌 컨트롤 활성화 여부 (기본: true)
 * @param {boolean} options.enableTracking - 환자 위치 추적 활성화 여부 (기본: true)
 */
export function useKakaoMap(options = {}) {
  const {
    kakaoKey = '',
    center = { lat: 37.4943524920695, lng: 126.88767655688868 },
    defaultLevel = 3,
    enableControls = true,
    enableTracking = true
  } = options

  // 지도 관련 상태
  const mapEl = ref(null)
  const mapInstance = ref(null)
  const err = ref('')
  const isMapLoaded = ref(false)

  // 카카오 SDK 로드 함수
  function loadKakao(key) {
    return new Promise((resolve, reject) => {
      if (!key) return reject(new Error('Kakao JavaScript 키가 비어 있습니다. (.env 또는 prop 확인)'))
      if (window.kakao?.maps) return resolve(window.kakao)
      
      let s = document.querySelector('script[data-kakao-sdk]')
      if (!s) {
        s = document.createElement('script')
        s.setAttribute('data-kakao-sdk', 'true')
        s.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${key}&autoload=false&libraries=services`
        s.async = true
        s.onerror = () => reject(new Error('Kakao SDK 로드 실패(도메인/키 확인)'))
        document.head.appendChild(s)
      }
      
      s.addEventListener('load', () => {
        if (!window.kakao?.maps) return reject(new Error('kakao 객체 미탑재'))
        window.kakao.maps.load(() => resolve(window.kakao))
      }, { once: true })
    })
  }

  // 지도 초기화 함수
  async function initMap(customCenter = null) {
    try {
      const key = kakaoKey || import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'
      const kakao = await loadKakao(key)
      
      // 중심 좌표 결정 (커스텀 중심이 있으면 사용, 없으면 기본값)
      const centerCoords = customCenter || center
      const defaultCenter = new kakao.maps.LatLng(centerCoords.lat, centerCoords.lng)
      
      // 지도 생성
      const map = new kakao.maps.Map(mapEl.value, { 
        center: defaultCenter, 
        level: defaultLevel 
      })
      
      mapInstance.value = map
      isMapLoaded.value = true
      
      await nextTick()
      map.relayout()
      
      console.log('카카오 지도 초기화 완료')
      return map
    } catch (e) {
      console.error('카카오 지도 초기화 오류:', e)
      err.value = e.message
      throw e
    }
  }

  // 지도 중심 이동
  function setCenter(lat, lng) {
    if (!mapInstance.value || !window.kakao?.maps) return
    
    const position = new window.kakao.maps.LatLng(lat, lng)
    mapInstance.value.setCenter(position)
  }

  // 지도 줌 레벨 설정
  function setLevel(level) {
    if (!mapInstance.value) return
    mapInstance.value.setLevel(level)
  }

  // 지도 줌 인
  function zoomIn() {
    if (!mapInstance.value || !enableControls) return
    const level = mapInstance.value.getLevel()
    mapInstance.value.setLevel(level - 1) // 레벨 감소 = 확대
  }

  // 지도 줌 아웃
  function zoomOut() {
    if (!mapInstance.value || !enableControls) return
    const level = mapInstance.value.getLevel()
    mapInstance.value.setLevel(level + 1) // 레벨 증가 = 축소
  }

  // 지도 범위 설정 (여러 좌표를 포함하도록)
  function setBounds(coordinates) {
    if (!mapInstance.value || !window.kakao?.maps || !coordinates?.length) return
    
    const bounds = new window.kakao.maps.LatLngBounds()
    coordinates.forEach(coord => {
      const latLng = new window.kakao.maps.LatLng(coord.lat || coord.latitude, coord.lng || coord.longitude)
      bounds.extend(latLng)
    })
    mapInstance.value.setBounds(bounds)
  }

  // 환자 위치로 이동
  async function moveToPatientLocation(patientLocation, patientUserNo, fetchPatientLocation, goToConnect) {
    if (!mapInstance.value || !window.kakao?.maps) {
      console.warn('지도가 초기화되지 않았습니다.')
      return
    }
    
    // 환자 번호가 없으면 연결 페이지로 이동
    if (!patientUserNo) {
      alert('환자와 연결이 필요합니다.')
      if (goToConnect) goToConnect()
      return
    }
    
    try {
      // 현재 환자 위치가 있으면 해당 위치로 이동
      if (patientLocation && patientLocation.latitude && patientLocation.longitude) {
        const position = new window.kakao.maps.LatLng(
          patientLocation.latitude, 
          patientLocation.longitude
        )
        mapInstance.value.setCenter(position)
        mapInstance.value.setLevel(defaultLevel)
        console.log('환자 위치로 이동 완료')
        return
      }
      
      // 환자 위치가 없으면 최신 위치 조회 후 이동
      console.log('환자 위치 조회 중...')
      if (fetchPatientLocation) {
        await fetchPatientLocation()
        
        // 조회 후에도 위치가 있으면 이동
        if (patientLocation && patientLocation.latitude && patientLocation.longitude) {
          const position = new window.kakao.maps.LatLng(
            patientLocation.latitude, 
            patientLocation.longitude
          )
          mapInstance.value.setCenter(position)
          mapInstance.value.setLevel(defaultLevel)
          console.log('환자 위치 조회 후 이동 완료')
        } else {
          alert('환자의 현재 위치를 찾을 수 없습니다. 환자가 오프라인 상태일 수 있습니다.')
        }
      }
    } catch (error) {
      console.error('환자 위치로 이동 중 오류:', error)
      alert('환자 위치로 이동하는 중 오류가 발생했습니다.')
    }
  }

  // 지도 리사이즈 (화면 크기 변경 시)
  function relayout() {
    if (!mapInstance.value) return
    mapInstance.value.relayout()
  }

  // 지도 인스턴스 가져오기
  function getMapInstance() {
    return mapInstance.value
  }

  // 지도 로드 상태 확인
  function isMapReady() {
    return isMapLoaded.value && mapInstance.value
  }

  return {
    // DOM 참조
    mapEl,
    
    // 상태
    mapInstance,
    err,
    isMapLoaded,
    
    // 지도 제어 함수들
    initMap,
    setCenter,
    setLevel,
    zoomIn,
    zoomOut,
    setBounds,
    moveToPatientLocation,
    relayout,
    
    // 유틸리티 함수들
    getMapInstance,
    isMapReady,
    
    // SDK 로드 함수 (내부용)
    loadKakao
  }
}

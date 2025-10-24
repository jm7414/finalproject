import { ref, onBeforeUnmount } from 'vue'

export function usePatientLocation(options = {}) {
  const {
    patientUserNo,
    patientInfo,
    mapInstance,
    onLocationUpdate = () => {},
    onPatientInfoUpdate = () => {},
    onError = () => {}
  } = options

  // 환자 위치 관련 상태
  const patientLocation = ref(null)
  const patientMarker = ref(null)
  const locationUpdateInterval = ref(null)

  // 환자 위치 추적 시작
  function startPatientLocationTracking() {
    // 즉시 한 번 조회
    fetchPatientLocation()
    
    // 20초마다 환자 위치 조회
    locationUpdateInterval.value = setInterval(() => {
      fetchPatientLocation()
    }, 20000) // 20초
  }

  // 환자 위치 추적 중지
  function stopPatientLocationTracking() {
    if (locationUpdateInterval.value) {
      clearInterval(locationUpdateInterval.value)
      locationUpdateInterval.value = null
    }
  }

  // 환자 위치 조회
  async function fetchPatientLocation() {
    // 환자 번호가 없으면 조회하지 않음
    if (!patientUserNo.value) {
      return
    }
    
    try {
      const response = await fetch(`/api/location/patient/${patientUserNo.value}`, {
        method: 'GET',
        credentials: 'include'
      })
      
      if (!response.ok) {
        // API 호출 실패 시 오프라인으로 설정 (마지막 활동 시간은 변경하지 않음)
        if (patientInfo.value.userNo) {
          patientInfo.value.isOnline = false
          // lastActivity는 변경하지 않음 - 기존 시간 유지
          onPatientInfoUpdate(patientInfo.value)
        }
        return
      }
      
      const location = await response.json()
      
      if (location && location.latitude && location.longitude) {
        patientLocation.value = location
        updatePatientMarker(location)
        
        // 환자 정보 업데이트 (온라인일 때만 마지막 활동 시간 업데이트)
        if (patientInfo.value.userNo === location.userNo) {
          patientInfo.value.isOnline = location.status === 'online'
          // 온라인일 때만 마지막 활동 시간 업데이트
          if (location.status === 'online') {
            patientInfo.value.lastActivity = new Date(location.timestamp)
          }
          // 오프라인일 때는 기존 마지막 활동 시간 유지
          onPatientInfoUpdate(patientInfo.value)
        }
        
        // 위치 업데이트 콜백 호출
        onLocationUpdate(location)
      } else {
        // 위치 데이터가 없으면 오프라인으로 설정 (마지막 활동 시간은 변경하지 않음)
        if (patientInfo.value.userNo) {
          patientInfo.value.isOnline = false
          // lastActivity는 변경하지 않음 - 기존 시간 유지
          onPatientInfoUpdate(patientInfo.value)
        }
      }
    } catch (error) {
      console.error('환자 위치 조회 오류:', error)
      onError(error)
      
      // 네트워크 오류 등으로 위치 조회 실패 시 오프라인으로 설정 (마지막 활동 시간은 변경하지 않음)
      if (patientInfo.value.userNo) {
        patientInfo.value.isOnline = false
        // lastActivity는 변경하지 않음 - 기존 시간 유지
        onPatientInfoUpdate(patientInfo.value)
      }
    }
  }

  // 환자 마커 업데이트
  function updatePatientMarker(location) {
    if (!mapInstance.value || !window.kakao?.maps) return
    
    try {
      // 기존 환자 마커 제거
      if (patientMarker.value) {
        patientMarker.value.setMap(null)
      }
      
      // 새로운 환자 마커 생성
      const position = new window.kakao.maps.LatLng(location.latitude, location.longitude)
      
      // 환자 마커 이미지 (빨간색 원)
      const markerImage = new window.kakao.maps.MarkerImage(
        'data:image/svg+xml;base64,' + btoa(`
          <svg width="24" height="24" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" fill="${location.status === 'online' ? '#EF4444' : '#9CA3AF'}" 
                    stroke="white" stroke-width="2"/>
            <circle cx="12" cy="12" r="4" fill="white"/>
          </svg>
        `),
        new window.kakao.maps.Size(24, 24),
        { offset: new window.kakao.maps.Point(12, 12) }
      )
      
      patientMarker.value = new window.kakao.maps.Marker({
        position: position,
        image: markerImage,
        title: `${patientInfo.value.name || '환자'} 위치`
      })
      
      patientMarker.value.setMap(mapInstance.value)
      
      // 환자 위치로 지도 중심 이동
      mapInstance.value.setCenter(position)
      
      console.log(`환자 위치 업데이트: (${location.latitude}, ${location.longitude}) - ${location.status}`)
    } catch (error) {
      console.error('환자 마커 업데이트 오류:', error)
      onError(error)
    }
  }

  // 컴포넌트 언마운트 시 정리
  onBeforeUnmount(() => {
    stopPatientLocationTracking()
  })

  return {
    // 상태
    patientLocation,
    patientMarker,
    locationUpdateInterval,
    
    // 함수
    startPatientLocationTracking,
    stopPatientLocationTracking,
    fetchPatientLocation,
    updatePatientMarker
  }
}

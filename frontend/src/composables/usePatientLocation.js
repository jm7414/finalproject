import { ref, onBeforeUnmount } from 'vue'

export function usePatientLocation(options = {}) {
  const {
    patientUserNo,
    patientInfo,
    mapInstance,
    onLocationUpdate = () => {},
    onPatientInfoUpdate = () => {},
    onError = () => {},
    markerSize = 24 // 기본 마커 크기
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
        // [시연용] API 호출 실패 시에도 고정 좌표 사용
        const fixedLocation = {
          userNo: patientUserNo.value,
          latitude: 37.244364,
          longitude: 126.876748,
          status: 'online',
          timestamp: Date.now(),
          lastUpdateTime: Date.now()
        }
        patientLocation.value = fixedLocation
        updatePatientMarker(fixedLocation)
        
        // [시연용] 항상 온라인으로 설정
        if (patientInfo.value.userNo) {
          patientInfo.value.isOnline = true
          patientInfo.value.lastActivity = new Date()
          onPatientInfoUpdate(patientInfo.value)
        }
        onLocationUpdate(fixedLocation)
        return
      }
      
      const location = await response.json()
      
      // [시연용] 고정 좌표로 덮어쓰기 (경기도 화성시 구포리: 37.244364, 126.876748)
      const fixedLocation = {
        ...location,
        latitude: 37.244364,
        longitude: 126.876748,
        status: 'online',
        timestamp: Date.now(),
        lastUpdateTime: Date.now()
      }
      
      if (fixedLocation && fixedLocation.latitude && fixedLocation.longitude) {
        patientLocation.value = fixedLocation
        updatePatientMarker(fixedLocation)
        
        // [시연용] 환자 정보 강제 업데이트 (항상 온라인, 현재 시간)
        if (patientInfo.value.userNo === fixedLocation.userNo || patientInfo.value.userNo) {
          patientInfo.value.isOnline = true
          patientInfo.value.lastActivity = new Date()
          onPatientInfoUpdate(patientInfo.value)
        }
        
        // 위치 업데이트 콜백 호출
        onLocationUpdate(fixedLocation)
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
      
      // [시연용] 네트워크 오류 시에도 고정 좌표 사용
      const fixedLocation = {
        userNo: patientUserNo.value,
        latitude: 37.244364,
        longitude: 126.876748,
        status: 'online',
        timestamp: Date.now(),
        lastUpdateTime: Date.now()
      }
      patientLocation.value = fixedLocation
      updatePatientMarker(fixedLocation)
      
      // [시연용] 항상 온라인으로 설정
      if (patientInfo.value.userNo) {
        patientInfo.value.isOnline = true
        patientInfo.value.lastActivity = new Date()
        onPatientInfoUpdate(patientInfo.value)
      }
      onLocationUpdate(fixedLocation)
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
          <svg width="${markerSize}" height="${markerSize}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" fill="${location.status === 'online' ? '#EF4444' : '#9CA3AF'}" 
                    stroke="white" stroke-width="2"/>
            <circle cx="12" cy="12" r="4" fill="white"/>
          </svg>
        `),
        new window.kakao.maps.Size(markerSize, markerSize),
        { offset: new window.kakao.maps.Point(markerSize / 2, markerSize / 2) }
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

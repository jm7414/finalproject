import { ref, computed } from 'vue'

/**
 * 일정 관리 composable
 * @param {Object} options - 설정 옵션
 * @param {Function} options.fetchPatientInfo - 환자 정보 조회 함수
 * @param {Function} options.onScheduleLoaded - 일정 로드 완료 콜백
 */
export function useSchedule(options = {}) {
  const { fetchPatientInfo, onScheduleLoaded } = options

  // 일정 관련 상태
  const patientUserNo = ref(null)
  const allSchedules = ref([])
  const scheduleLocations = ref({}) // scheduleNo를 키로 하는 위치 정보 맵
  const selectedScheduleIndex = ref(null)

  // 시간을 12시간 형식으로 변환 (오전/오후 포함)
  function formatTime(timeString) {
    if (!timeString) return ''
    
    const [hour, minute] = timeString.split(':')
    const hourNum = parseInt(hour)
    
    if (hourNum === 0) {
      return `오전 12:${minute}`
    } else if (hourNum < 12) {
      return `오전 ${String(hourNum).padStart(2, '0')}:${minute}`
    } else if (hourNum === 12) {
      return `오후 12:${minute}`
    } else {
      return `오후 ${String(hourNum - 12).padStart(2, '0')}:${minute}`
    }
  }

  // 위치 정보를 화살표 형식으로 포맷팅
  function formatLocation(scheduleNo) {
    const locations = scheduleLocations.value[scheduleNo]
    if (!locations || locations.length === 0) return ''
    
    // sequence_order 순서대로 정렬
    const sortedLocations = [...locations].sort((a, b) => a.sequenceOrder - b.sequenceOrder)
    
    // 위치명을 화살표로 연결
    return sortedLocations.map(loc => loc.locationName).join(' → ')
  }

  // 일정 목록 가져오기
  async function fetchSchedules(userNo) {
    try {
      const response = await fetch(`/api/schedule/list/${userNo}`, {
        method: 'GET',
        credentials: 'include'
      })
      
      if (!response.ok) {
        throw new Error('일정 목록을 가져올 수 없습니다.')
      }
      
      const schedules = await response.json()
      return schedules
    } catch (error) {
      console.error('일정 목록 조회 오류:', error)
      return []
    }
  }

  // 특정 일정의 위치 목록 가져오기
  async function fetchScheduleLocations(scheduleNo) {
    try {
      const response = await fetch(`/api/schedule/${scheduleNo}/locations`, {
        method: 'GET',
        credentials: 'include'
      })
      
      if (!response.ok) {
        throw new Error('위치 정보를 가져올 수 없습니다.')
      }
      
      const locations = await response.json()
      return locations
    } catch (error) {
      console.error('위치 정보 조회 오류:', error)
      return []
    }
  }

  // 모든 일정 데이터 로드
  async function loadScheduleData() {
    // 1. 환자 정보 조회
    const userNo = await fetchPatientInfo()
    if (!userNo) {
      console.warn('관리하는 환자가 없습니다.')
      return null
    }
    
    patientUserNo.value = userNo
    
    // 2. 일정 목록 조회
    const schedules = await fetchSchedules(userNo)
    allSchedules.value = schedules
    
    // 3. 각 일정의 위치 정보 조회
    for (const schedule of schedules) {
      const locations = await fetchScheduleLocations(schedule.scheduleNo)
      scheduleLocations.value[schedule.scheduleNo] = locations
    }
    
    // 4. 일정 로드 완료 콜백 호출
    if (onScheduleLoaded) {
      onScheduleLoaded()
    }
    
    return userNo
  }

  // 현재 진행 중인 일정 찾기
  function getCurrentSchedule() {
    const now = new Date()
    const today = new Date()
    const year = today.getFullYear()
    const month = String(today.getMonth() + 1).padStart(2, '0')
    const day = String(today.getDate()).padStart(2, '0')
    const todayKey = `${year}-${month}-${day}`
    
    // 오늘 일정만 필터링
    const todaySchedules = allSchedules.value.filter(schedule => schedule.scheduleDate === todayKey)
    
    // 현재 시간
    const currentHour = now.getHours()
    const currentMinute = now.getMinutes()
    const currentTimeInMinutes = currentHour * 60 + currentMinute
    
    // 현재 시간에 해당하는 일정들 모두 찾기
    const currentSchedules = []
    
    for (const schedule of todaySchedules) {
      const [startHour, startMinute] = schedule.startTime.split(':').map(Number)
      const [endHour, endMinute] = schedule.endTime.split(':').map(Number)
      
      const startTimeInMinutes = startHour * 60 + startMinute
      const endTimeInMinutes = endHour * 60 + endMinute
      
      // 현재 시간이 일정 시간 범위 안에 있는지 확인
      if (currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes <= endTimeInMinutes) {
        currentSchedules.push(schedule)
      }
    }
    
    // 일정이 없으면 null 반환
    if (currentSchedules.length === 0) return null
    
    // 일정이 여러 개 겹치면 시작 시간이 가장 빠른 것 선택
    if (currentSchedules.length > 1) {
      console.warn(`⚠️ ${currentSchedules.length}개의 일정이 현재 시간에 겹칩니다. 가장 먼저 시작된 일정을 표시합니다.`)
      currentSchedules.forEach(s => {
        console.log(`  - ${s.scheduleTitle} (${s.startTime} ~ ${s.endTime})`)
      })
    }
    
    // 시작 시간 기준으로 정렬 후 첫 번째 반환
    return currentSchedules.sort((a, b) => 
      a.startTime.localeCompare(b.startTime)
    )[0]
  }

  // 일정의 상태 판단 (대기중/이동중)
  function getScheduleStatus(schedule) {
    const now = new Date()
    const currentHour = now.getHours()
    const currentMinute = now.getMinutes()
    const currentTimeInMinutes = currentHour * 60 + currentMinute
    
    const [startHour, startMinute] = schedule.startTime.split(':').map(Number)
    const [endHour, endMinute] = schedule.endTime.split(':').map(Number)
    
    const startTimeInMinutes = startHour * 60 + startMinute
    const endTimeInMinutes = endHour * 60 + endMinute
    
    // 시작 시간 이전: 대기중
    if (currentTimeInMinutes < startTimeInMinutes) {
      return 'waiting'
    }
    
    // 시작~종료 시간 사이: 이동중
    if (currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes <= endTimeInMinutes) {
      return 'active'
    }
    
    // 종료 후 (이 경우는 todaySchedules에서 필터링되어 나타나지 않음)
    return 'finished'
  }

  // 일정 카드 스타일 가져오기
  function getScheduleCardStyle(schedule) {
    const status = getScheduleStatus(schedule)
    
    if (status === 'active') {
      return {
        background: 'rgba(191, 219, 254, 0.5)',
        border: selectedScheduleIndex.value === schedule.scheduleNo ? '3px solid #000' : '1px solid rgba(191, 219, 254, 0.8)'
      }
    } else {
      return {
        background: 'white',
        border: selectedScheduleIndex.value === schedule.scheduleNo ? '3px solid #000' : '1px solid #E5E7EB'
      }
    }
  }

  // 일정 선택 함수
  function selectSchedule(scheduleNo) {
    // 같은 일정을 다시 클릭하면 선택 해제
    if (selectedScheduleIndex.value === scheduleNo) {
      selectedScheduleIndex.value = null
    } else {
      selectedScheduleIndex.value = scheduleNo
    }
    // TODO: 나중에 여기서 해당 일정의 안심존을 지도에 표시하는 로직 추가
  }

  // 오늘의 일정 계산 (종료되지 않은 일정만)
  const todaySchedules = computed(() => {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const todayKey = `${year}-${month}-${day}`
    
    const currentHour = now.getHours()
    const currentMinute = now.getMinutes()
    const currentTimeInMinutes = currentHour * 60 + currentMinute
    
    return allSchedules.value
      .filter(schedule => {
        // 오늘 일정만
        if (schedule.scheduleDate !== todayKey) return false
        
        // 종료 시간 체크
        const [endHour, endMinute] = schedule.endTime.split(':').map(Number)
        const endTimeInMinutes = endHour * 60 + endMinute
        
        // 종료되지 않은 일정만 (현재 시간 <= 종료 시간)
        return currentTimeInMinutes <= endTimeInMinutes
      })
      .sort((a, b) => a.startTime.localeCompare(b.startTime))
  })

  return {
    // 상태
    patientUserNo,
    allSchedules,
    scheduleLocations,
    selectedScheduleIndex,
    
    // 계산된 속성
    todaySchedules,
    
    // 유틸리티 함수들
    formatTime,
    formatLocation,
    getScheduleStatus,
    getScheduleCardStyle,
    
    // 일정 관리 함수들
    loadScheduleData,
    fetchSchedules,
    fetchScheduleLocations,
    getCurrentSchedule,
    selectSchedule
  }
}

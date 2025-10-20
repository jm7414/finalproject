import axios from 'axios'


/**
 * 현재 사용자가 인증되어 있는지 확인
 */
export async function isAuthenticated() {
  try {
    const response = await axios.get(`/api/user/me`, {
      withCredentials: true
    })
    return response.status === 200
  } catch (error) {
    return false
  }
}

/**
 * 현재 로그인한 사용자 정보 가져오기
 */
export async function getCurrentUser() {
  try {
    const response = await axios.get(`/api/user/me`, {
      withCredentials: true
    })
    return response.data
  } catch (error) {
    return null
  }
}

/**
 * 사용자 역할에 따른 기본 라우트 반환
 * @param {number} roleNo - 역할 번호 (1: 보호자, 2: 환자, 3: 구독자)
 */
export function getDefaultRouteByRole(roleNo) {
  switch (roleNo) {
    case 1: // 보호자
    case 3: // 구독자
      return '/map-main'
    case 2: // 환자
      return '/DP'
    default:
      return '/login'
  }
}

/**
 * 로그아웃 처리
 */
export async function logout() {
  try {
    await axios.post(`/logout`, {}, {
      withCredentials: true
    })
    return true
  } catch (error) {
    console.error('로그아웃 실패:', error)
    return false
  }
}


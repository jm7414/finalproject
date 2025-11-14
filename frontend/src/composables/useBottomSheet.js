import { ref, computed, onBeforeUnmount } from 'vue'

/**
 * 바텀시트 드래그 및 높이 관리 composable
 * @param {Object} options - 설정 옵션
 * @param {number} options.foldNudge - 접힘 높이 조정값 (기본: 10)
 * @param {number} options.minHeight - 최소 높이 (기본: 100)
 * @param {number} options.openRatio - 열린 상태 비율 (기본: 0.8)
 * @param {number} options.collapsedRatio - 접힌 상태 비율 (기본: 0.28)
 */
export function useBottomSheet(options = {}) {
  const {
    foldNudge = 10,
    minHeight = 100,
    openRatio = 0.8,
    collapsedRatio = 0.28
  } = options

  // DOM 참조
  const sheetEl = ref(null)
  const topTilesRow = ref(null)
  const foldAnchor = ref(null)

  // 높이 계산 함수들
  const vh = () => window.innerHeight
  const openH = () => Math.round(vh() * openRatio)
  const collapsedH = ref(Math.round(vh() * collapsedRatio) || 280) // 초기값
  const sheetHeight = ref(collapsedH.value)

  // 드래그 상태
  let startY = 0
  let startH = collapsedH.value
  let dragging = false

  // 계산된 속성들
  const sheetStyle = computed(() => ({
    height: sheetHeight.value + 'px',
  }))

  const backdropOpacity = computed(() => {
    const t = Math.max(0, Math.min(1, (sheetHeight.value - collapsedH.value) / (openH() - collapsedH.value)))
    return (0.6 * t).toFixed(2)
  })

  // 현위치 버튼의 bottom 위치 계산 (기본 위치에서 아래로 내릴 때만 반응형으로 내려감)
  const locationBtnBottom = computed(() => {
    const btnOffset = 20 // 바텀시트 위로 20px 여백
    // 기본 위치(collapsedH) 이하로 내릴 때만 반응형, 위로 올라갈 때는 기본 위치에 고정
    return Math.min(sheetHeight.value, collapsedH.value) + btnOffset
  })

  // 드래그 이벤트 핸들러들
  function onPointerDown(e) {
    dragging = true
    startY = e.clientY || (e.touches && e.touches[0].clientY)
    startH = sheetHeight.value
    window.addEventListener('pointermove', onPointerMove, { passive: false })
    window.addEventListener('pointerup', onPointerUp, { once: true })
  }

  function onPointerMove(e) {
    if (!dragging) return
    e.preventDefault()
    const y = e.clientY || (e.touches && e.touches[0].clientY)
    const delta = startY - y
    const next = Math.max(minHeight, Math.min(openH(), startH + delta))
    sheetHeight.value = next
  }

  function onPointerUp() {
    dragging = false
    // 3단계 스냅: 최소 높이(손잡이만 보임) / 기본 높이(collapsedH) / 완전히 열린 상태(openH)
    const minH = minHeight
    const midH = collapsedH.value
    const maxH = openH()
    
    const current = sheetHeight.value
    const toMin = Math.abs(current - minH)
    const toMid = Math.abs(current - midH)
    const toMax = Math.abs(current - maxH)
    
    // 가장 가까운 높이로 스냅
    if (toMin < toMid && toMin < toMax) {
      sheetHeight.value = minH
    } else if (toMid < toMax) {
      sheetHeight.value = midH
    } else {
      sheetHeight.value = maxH
    }
    
    window.removeEventListener('pointermove', onPointerMove)
  }

  // 접힘 높이 자동 계산 (환자 정보 + 안심존/걸음수 카드까지 보이게)
  function measureCollapsed() {
    try {
      if (!sheetEl.value || !foldAnchor.value) return
      const sheetRect = sheetEl.value.getBoundingClientRect()
      const anchorRect = foldAnchor.value.getBoundingClientRect()
      // 앵커의 bottom이 시트 상단에서 얼마나 떨어져 있는지 + 여백
      const desired = Math.ceil(anchorRect.bottom - sheetRect.top + 50 + foldNudge)
      const clamped = Math.max(200, Math.min(desired, openH() - 8))
      collapsedH.value = clamped
      // 드래그 중이 아니면 접힘 높이로 설정
      if (!dragging) {
        sheetHeight.value = collapsedH.value
      }
    } catch (e) {
      console.warn('measureCollapsed failed', e)
    }
  }

  // 리사이즈 이벤트 핸들러
  function onResize() {
    // 뷰포트 변할 때 오픈/접힘 기준 갱신
    const wasOpen = sheetHeight.value > (collapsedH.value + openH()) / 2
    measureCollapsed()
    sheetHeight.value = wasOpen ? openH() : collapsedH.value
  }

  // 접힌 상태로 이동
  function toCollapsed() { 
    sheetHeight.value = collapsedH.value 
  }

  // 열린 상태로 이동
  function toOpen() { 
    sheetHeight.value = openH() 
  }

  // 최소 높이로 이동
  function toMinimized() { 
    sheetHeight.value = minHeight 
  }

  // 초기화 함수
  function init() {
    window.addEventListener('resize', onResize)
  }

  // 정리 함수
  function cleanup() {
    window.removeEventListener('resize', onResize)
    window.removeEventListener('pointermove', onPointerMove)
  }

  // 컴포넌트 언마운트 시 정리
  onBeforeUnmount(() => {
    cleanup()
  })

  return {
    // DOM 참조
    sheetEl,
    topTilesRow,
    foldAnchor,
    
    // 상태
    sheetHeight,
    collapsedH,
    dragging: computed(() => dragging),
    
    // 계산된 속성
    sheetStyle,
    backdropOpacity,
    locationBtnBottom,
    
    // 높이 계산 함수들
    openH,
    vh,
    
    // 이벤트 핸들러
    onPointerDown,
    onPointerUp,
    onPointerMove,
    
    // 유틸리티 함수들
    measureCollapsed,
    toCollapsed,
    toOpen,
    toMinimized,
    
    // 초기화/정리
    init,
    cleanup
  }
}

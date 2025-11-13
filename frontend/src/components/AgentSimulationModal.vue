<template>
    <div class="agent-simulation-modal-overlay" v-if="isVisible" @click.self="closeModal">
        <div class="agent-simulation-modal">
            <!-- 헤더 -->
            <div class="modal-header">
                <div class="header-content">
                    <i class="bi bi-diagram-3"></i>
                    <h2>에이전트 시뮬레이션</h2>
                </div>
                <button class="close-button" @click="closeModal">
                    <i class="bi bi-x-lg"></i>
                </button>
            </div>

            <!-- 로딩 상태 -->
            <div v-if="isLoading" class="loading-state">
                <div class="loading-spinner"></div>
                <p>시뮬레이션 데이터 로딩 중...</p>
            </div>

            <!-- 에러 상태 -->
            <div v-else-if="error" class="error-state">
                <i class="bi bi-exclamation-triangle"></i>
                <p>{{ error }}</p>
                <button @click="loadSimulationData" class="retry-button">다시 시도</button>
            </div>

            <!-- 메인 콘텐츠 -->
            <div v-else class="modal-content">
                <!-- 지도 영역 -->
                <div class="map-section">
                    <div ref="mapContainer" class="simulation-map"></div>

                    <!-- 지도 위 오버레이 정보 -->
                    <div class="map-overlay">
                        <div class="scenario-badge">
                            <i class="bi bi-clock"></i>
                            <span>{{ currentScenario }}</span>
                        </div>
                        <div class="agent-count-badge">
                            <i class="bi bi-people"></i>
                            <span>{{ agentsList.length }}명</span>
                        </div>
                    </div>
                </div>

                <!-- 컨트롤 패널 -->
                <div class="control-panel">
                    <!-- 시나리오 선택 -->
                    <div class="scenario-selector">
                        <label>시나리오 선택</label>
                        <div class="scenario-buttons">
                            <button v-for="scenario in availableScenarios" :key="scenario.scenario"
                                :class="['scenario-btn', { active: currentScenario === scenario.scenario }]"
                                @click="changeScenario(scenario.scenario)" :disabled="!scenario.available">
                                <i class="bi bi-clock-history"></i>
                                <span>{{ scenario.scenario }}</span>
                            </button>
                        </div>
                    </div>

                    <!-- 재생 컨트롤 -->
                    <div class="playback-controls">
                        <button @click="togglePlay" class="btn-play">
                            <i :class="isPlaying ? 'bi bi-pause-fill' : 'bi bi-play-fill'"></i>
                            <span>{{ isPlaying ? '일시정지' : '재생' }}</span>
                        </button>
                        <button @click="resetAnimation" class="btn-reset">
                            <i class="bi bi-arrow-counterclockwise"></i>
                            <span>초기화</span>
                        </button>
                        <select v-model="playbackSpeed" class="speed-selector">
                            <option :value="0.5">0.5x</option>
                            <option :value="1">1x</option>
                            <option :value="2">2x</option>
                            <option :value="5">5x</option>
                            <option :value="10">10x</option>
                        </select>
                    </div>

                    <!-- 타임라인 -->
                    <div class="timeline-section">
                        <div class="timeline-info">
                            <span class="current-time">{{ formatTime(currentTime) }}</span>
                            <span class="separator">/</span>
                            <span class="total-time">{{ formatTime(totalTime) }}</span>
                        </div>
                        <input type="range" v-model="currentStep" :min="0" :max="totalSteps - 1" @input="updateFrame"
                            class="timeline-slider" />
                        <div class="timeline-progress-bar">
                            <div class="progress-fill" :style="{ width: progressPercentage + '%' }"></div>
                        </div>
                    </div>
                </div>

                <!-- 에이전트 리스트 -->
                <div class="agents-list-panel">
                    <div class="panel-header">
                        <h3>상위 10개 위치</h3>
                        <span class="subtitle">확률 기준</span>
                    </div>
                    <div class="agents-list">
                        <div v-for="agent in agentsList" :key="agent.rank"
                            :class="['agent-item', { selected: selectedAgent === agent.rank }]"
                            @click="selectAgent(agent.rank)">
                            <div class="rank-badge" :style="{ backgroundColor: getColorByRank(agent.rank) }">
                                {{ agent.rank }}
                            </div>
                            <div class="agent-info">
                                <div class="info-row">
                                    <span class="label">확률</span>
                                    <span class="value probability">{{ agent.probability }}%</span>
                                </div>
                                <div class="info-row">
                                    <span class="label">에이전트</span>
                                    <span class="value">{{ agent.agent_count }}명</span>
                                </div>
                                <div class="info-row">
                                    <span class="label">거리</span>
                                    <span class="value">{{ agent.distance.toFixed(0) }}m</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import axios from 'axios'

// Props
// AgentSimulationModal.vue의 props 부분
const props = defineProps({
    isVisible: {
        type: Boolean,
        default: false
    },
    userNo: {
        type: Number,
        required: true
    },
    missingLocation: {
        type: Object,
        required: true,
        default: () => ({ lat: 37.238257, lon: 126.681727 })
    },
    missingTime: {
        type: Number,  // timestamp
        required: true
    }
})

// Emits
const emit = defineEmits(['close'])

// 카카오 지도 관련
const KAKAO_JS_KEY = '7e0332c38832a4584b3335bed6ae30d8'
const mapContainer = ref(null)
let map = null
const markers = {}
const paths = {}
const overlays = {}

// 상태 관리
const isLoading = ref(false)
const error = ref(null)
const currentScenario = ref('30분')
const availableScenarios = ref([])
const animationData = ref(null)
const currentStep = ref(0)
const isPlaying = ref(false)
const playbackSpeed = ref(1)
const selectedAgent = ref(null)
let animationTimer = null

// 계산된 속성
const totalSteps = computed(() => {
    return animationData.value?.data?.total_steps || 0
})

const currentTime = computed(() => {
    if (!animationData.value || currentStep.value >= animationData.value.data.frames.length) {
        return 0
    }
    const frame = animationData.value.data.frames[currentStep.value]
    return frame.agents[0]?.time_seconds || 0
})

const totalTime = computed(() => {
    return currentScenario.value === '30분' ? 1800 :
        currentScenario.value === '60분' ? 3600 :
            5400 // 90분
})

const progressPercentage = computed(() => {
    if (totalSteps.value === 0) return 0
    return (currentStep.value / (totalSteps.value - 1)) * 100
})

const agentsList = computed(() => {
    if (!animationData.value || !animationData.value.data.frames[0]) return []

    return animationData.value.data.frames[0].agents.map(agent => ({
        rank: agent.rank,
        agent_id: agent.agent_id,
        probability: agent.final_position.probability,
        agent_count: agent.final_position.agent_count_at_position || 0,
        distance: 0, // 실제로는 별도 계산 필요
        final_lat: agent.final_position.latitude,
        final_lon: agent.final_position.longitude
    }))
})

// 순위별 색상
const getColorByRank = (rank) => {
    if (rank <= 3) return '#FF0000'
    if (rank <= 6) return '#FF6B00'
    return '#FFA500'
}

// 시간 포맷팅
const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 카카오 지도 초기화
const initMap = async () => {
    if (!window.kakao || !window.kakao.maps) {
        await loadKakaoMapScript()
    }

    await nextTick()

    if (!mapContainer.value) return

    const options = {
        center: new window.kakao.maps.LatLng(
            props.missingLocation.lat,
            props.missingLocation.lon
        ),
        level: 5
    }

    map = new window.kakao.maps.Map(mapContainer.value, options)

    // 실종 위치 마커 추가
    const startMarker = new window.kakao.maps.Marker({
        position: new window.kakao.maps.LatLng(
            props.missingLocation.lat,
            props.missingLocation.lon
        ),
        map: map,
        image: new window.kakao.maps.MarkerImage(
            'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png',
            new window.kakao.maps.Size(32, 32)
        )
    })
}

// 카카오맵 스크립트 로드
const loadKakaoMapScript = () => {
    return new Promise((resolve, reject) => {
        if (window.kakao && window.kakao.maps) {
            resolve()
            return
        }

        const script = document.createElement('script')
        script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false`
        script.onload = () => {
            window.kakao.maps.load(() => {
                resolve()
            })
        }
        script.onerror = reject
        document.head.appendChild(script)
    })
}

// 시뮬레이션 데이터 로드
const loadSimulationData = async () => {
    isLoading.value = true
    error.value = null

    console.log(`사용자 번호 : ${props.userNo}`)
    console.log(`마지막 위치 lat : ${props.missingLocation.lat}`)
    console.log(`마지막 위치 lon : ${props.missingLocation.lon}`)

    try {
        const response = await axios.post(
            'http://localhost:8000/api/agent-simulation/run-all',
            {
                user_no: props.userNo,
                latitude: props.missingLocation.lat,
                longitude: props.missingLocation.lon
            },
            {
                withCredentials: true,
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        )

        console.log('시뮬레이션 응답:', response.data)

        const scenarios = response.data.scenarios
        
        availableScenarios.value = [
            { scenario: '30분', available: !!scenarios['30분'] },
            { scenario: '60분', available: !!scenarios['60분'] },
            { scenario: '90분', available: !!scenarios['90분'] }
        ]

        animationData.value = {
            data: scenarios['30분']
        }
        
        currentStep.value = 0

        // ⭐ 지도 초기화 대기 후 에이전트 표시
        await nextTick()
        
        // ⭐ 약간의 딜레이 추가 (지도 완전 렌더링 대기)
        setTimeout(() => {
            initializeAgents()
        }, 300)

    } catch (err) {
        console.error('시뮬레이션 데이터 로드 실패:', err)
        console.error('에러 상세:', err.response?.data)
        error.value = err.response?.data?.detail || '데이터를 불러올 수 없습니다.'
    } finally {
        isLoading.value = false
    }
}



// 시나리오 변경
const changeScenario = async (scenario) => {
    if (scenario === currentScenario.value) return

    currentScenario.value = scenario
    isPlaying.value = false

    // 기존 마커와 경로 제거
    clearMapElements()

    await loadSimulationData()
}

// 에이전트 초기화
const initializeAgents = () => {
    if (!animationData.value || !map) {
        console.warn('지도 또는 애니메이션 데이터가 준비되지 않음')
        return
    }

    if (!animationData.value.data || !animationData.value.data.frames || animationData.value.data.frames.length === 0) {
        console.warn('프레임 데이터 없음')
        return
    }

    const firstFrame = animationData.value.data.frames[0]

    if (!firstFrame.agents || firstFrame.agents.length === 0) {
        console.warn('에이전트 데이터 없음')
        return
    }

    console.log(`에이전트 초기화 시작: ${firstFrame.agents.length}개`)

    firstFrame.agents.forEach(agent => {
        createAgentMarker(agent)
        createAgentPath(agent)
    })

    updateFrame()
    console.log('에이전트 초기화 완료')
}

// 에이전트 마커 생성
const createAgentMarker = (agent) => {
    if (!map) return

    try {
        const position = new window.kakao.maps.LatLng(agent.latitude, agent.longitude)

        const markerContent = document.createElement('div')
        markerContent.className = 'custom-agent-marker'
        markerContent.style.backgroundColor = getColorByRank(agent.rank)
        markerContent.innerHTML = `<span>${agent.rank}</span>`

        const customOverlay = new window.kakao.maps.CustomOverlay({
            position: position,
            content: markerContent,
            map: map,
            zIndex: 3
        })

        markers[agent.rank] = { overlay: customOverlay, position: position }
        console.log(`마커 생성 완료: rank ${agent.rank}`)
    } catch (error) {
        console.error(`마커 생성 실패 (rank ${agent.rank}):`, error)
    }
}

// 에이전트 경로 생성
const createAgentPath = (agent) => {
    if (!map) return

    try {
        // ⭐ 빈 배열로 초기화만 하고, 실제 경로는 updateFrame에서 추가
        const polyline = new window.kakao.maps.Polyline({
            map: map,
            path: [],  // 빈 배열
            strokeWeight: 3,
            strokeColor: getColorByRank(agent.rank),
            strokeOpacity: 0.7,
            strokeStyle: 'solid',
            zIndex: 2
        })

        paths[agent.rank] = { line: polyline, points: [] }
        console.log(`경로 생성 완료: rank ${agent.rank}`)
    } catch (error) {
        console.error(`경로 생성 실패 (rank ${agent.rank}):`, error)
    }
}

// 프레임 업데이트
const updateFrame = () => {
    if (!animationData.value || !map) return

    if (!animationData.value.data || !animationData.value.data.frames) {
        console.warn('프레임 데이터 없음')
        return
    }

    const frames = animationData.value.data.frames
    if (currentStep.value >= frames.length) {
        console.warn(`프레임 인덱스 초과: ${currentStep.value} >= ${frames.length}`)
        return
    }

    const frame = frames[currentStep.value]

    if (!frame.agents) {
        console.warn('프레임에 에이전트 없음')
        return
    }

    frame.agents.forEach(agent => {
        try {
            // 마커 위치 업데이트
            if (markers[agent.rank]) {
                const newPosition = new window.kakao.maps.LatLng(agent.latitude, agent.longitude)
                markers[agent.rank].position = newPosition
                markers[agent.rank].overlay.setPosition(newPosition)
            }

            // 경로에 점 추가
            if (paths[agent.rank]) {
                const point = new window.kakao.maps.LatLng(agent.latitude, agent.longitude)
                paths[agent.rank].points.push(point)
                paths[agent.rank].line.setPath(paths[agent.rank].points)
            }
        } catch (error) {
            console.error(`프레임 업데이트 실패 (rank ${agent.rank}):`, error)
        }
    })
}

// 애니메이션 재생
const playAnimation = () => {
    if (!isPlaying.value || currentStep.value >= totalSteps.value - 1) {
        isPlaying.value = false
        return
    }

    animationTimer = setTimeout(() => {
        currentStep.value++
        updateFrame()
        playAnimation()
    }, 100 / playbackSpeed.value)
}

// 애니메이션 정지
const stopAnimation = () => {
    if (animationTimer) {
        clearTimeout(animationTimer)
        animationTimer = null
    }
}

// 초기화
const resetAnimation = () => {
    isPlaying.value = false
    stopAnimation()
    currentStep.value = 0

    // 경로 초기화
    Object.values(paths).forEach(path => {
        path.points = []
        path.line.setPath([])
    })

    updateFrame()
}

// 에이전트 선택
const selectAgent = (rank) => {
    selectedAgent.value = rank === selectedAgent.value ? null : rank

    // 모든 마커 스타일 초기화
    Object.entries(markers).forEach(([r, marker]) => {
        const element = marker.overlay.getContent()
        if (parseInt(r) === rank && selectedAgent.value !== null) {
            element.classList.add('selected')
        } else {
            element.classList.remove('selected')
        }
    })
}

// 지도 요소 초기화
const clearMapElements = () => {
    Object.values(markers).forEach(marker => {
        marker.overlay.setMap(null)
    })
    Object.values(paths).forEach(path => {
        path.line.setMap(null)
    })

    Object.keys(markers).forEach(key => delete markers[key])
    Object.keys(paths).forEach(key => delete paths[key])
}

// 모달 닫기
const closeModal = () => {
    stopAnimation()
    emit('close')
}

// Watch
watch(() => props.isVisible, async (newVal) => {
    if (newVal) {
        await nextTick()
        await initMap()
        setTimeout(async () => {
            await loadSimulationData()
        }, 200)
    } else {
        stopAnimation()
        clearMapElements()
    }
})

// Lifecycle
onUnmounted(() => {
    stopAnimation()
})
</script>

<style scoped>
.agent-simulation-modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.7);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10000;
    animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
    from {
        opacity: 0;
    }

    to {
        opacity: 1;
    }
}

.agent-simulation-modal {
    background: white;
    border-radius: 20px;
    width: 355px;
    height: 780px;
    display: flex;
    flex-direction: column;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
    animation: slideUp 0.3s ease;
    overflow: hidden;
}

@keyframes slideUp {
    from {
        transform: translateY(50px);
        opacity: 0;
    }

    to {
        transform: translateY(0);
        opacity: 1;
    }
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px 20px;
    border-bottom: 2px solid #f0f0f0;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 20px 20px 0 0;
    flex-shrink: 0;
}

.header-content {
    display: flex;
    align-items: center;
    gap: 10px;
}

.header-content i {
    font-size: 20px;
}

.header-content h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
}

.close-button {
    background: rgba(255, 255, 255, 0.2);
    border: none;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    transition: all 0.3s;
    font-size: 14px;
}

.close-button:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: rotate(90deg);
}

.loading-state,
.error-state {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 20px;
    padding: 40px;
}

.loading-spinner {
    width: 60px;
    height: 60px;
    border: 4px solid #f3f3f3;
    border-top: 4px solid #667eea;
    border-radius: 50%;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    0% {
        transform: rotate(0deg);
    }

    100% {
        transform: rotate(360deg);
    }
}

.error-state i {
    font-size: 48px;
    color: #ff6b6b;
}

.retry-button {
    padding: 12px 24px;
    background: #667eea;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-weight: 600;
    transition: all 0.3s;
}

.retry-button:hover {
    background: #5568d3;
    transform: translateY(-2px);
}

.modal-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.map-section {
    height: 280px;
    position: relative;
    flex-shrink: 0;
}

.simulation-map {
    width: 100%;
    height: 100%;
}

.map-overlay {
    position: absolute;
    top: 10px;
    left: 10px;
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.scenario-badge,
.agent-count-badge {
    background: white;
    padding: 6px 10px;
    border-radius: 20px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    font-size: 11px;
}

.scenario-badge i,
.agent-count-badge i {
    color: #667eea;
    font-size: 12px;
}

.control-panel {
    background: #f8f9fa;
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    border-top: 1px solid #e0e0e0;
    flex-shrink: 0;
}

.scenario-selector label {
    display: block;
    font-weight: 600;
    margin-bottom: 8px;
    color: #333;
    font-size: 13px;
}

.scenario-buttons {
    display: flex;
    gap: 6px;
}

.scenario-btn {
    flex: 1;
    padding: 8px 4px;
    background: white;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    transition: all 0.3s;
    font-size: 11px;
}

.scenario-btn i {
    font-size: 14px;
}

.scenario-btn:hover:not(:disabled) {
    border-color: #667eea;
    transform: translateY(-2px);
}

.scenario-btn.active {
    background: #667eea;
    color: white;
    border-color: #667eea;
}

.scenario-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.playback-controls {
    display: flex;
    gap: 6px;
}

.btn-play,
.btn-reset {
    flex: 1;
    padding: 8px;
    background: #667eea;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-weight: 600;
    transition: all 0.3s;
    font-size: 11px;
}

.btn-play:hover,
.btn-reset:hover {
    background: #5568d3;
    transform: translateY(-2px);
}

.speed-selector {
    padding: 8px;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    background: white;
    cursor: pointer;
    font-weight: 600;
    font-size: 11px;
}

.timeline-section {
    background: white;
    padding: 10px;
    border-radius: 8px;
}

.timeline-info {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;
    font-weight: 600;
    color: #333;
    font-size: 12px;
}

.timeline-slider {
    width: 100%;
    height: 5px;
    border-radius: 3px;
    background: #e0e0e0;
    outline: none;
    cursor: pointer;
}

.timeline-progress-bar {
    width: 100%;
    height: 6px;
    background: #e0e0e0;
    border-radius: 3px;
    margin-top: 8px;
    overflow: hidden;
}

.progress-fill {
    height: 100%;
    background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    transition: width 0.1s ease;
}

.agents-list-panel {
    flex: 1;
    background: white;
    border-top: 1px solid #e0e0e0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.panel-header {
    padding: 12px;
    border-bottom: 2px solid #f0f0f0;
    flex-shrink: 0;
}

.panel-header h3 {
    margin: 0 0 3px 0;
    font-size: 15px;
    color: #333;
}

.subtitle {
    font-size: 11px;
    color: #888;
}

.agents-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
}

.agent-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px;
    margin-bottom: 6px;
    background: #f8f9fa;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
}

.agent-item:hover {
    background: #e8f4f8;
    transform: translateX(3px);
}

.agent-item.selected {
    background: #d0e8f0;
    border: 2px solid #667eea;
}

.rank-badge {
    width: 35px;
    height: 35px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    font-size: 14px;
    flex-shrink: 0;
}

.agent-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.info-row {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
}

.info-row .label {
    color: #888;
}

.info-row .value {
    font-weight: 600;
    color: #333;
}

.info-row .probability {
    color: #667eea;
}

/* 커스텀 에이전트 마커 */
:deep(.custom-agent-marker) {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    font-size: 11px;
    border: 2px solid white;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    transition: all 0.3s;
}

:deep(.custom-agent-marker.selected) {
    width: 32px;
    height: 32px;
    font-size: 14px;
    border-width: 3px;
    z-index: 1000;
}

/* 스크롤바 스타일 */
.agents-list::-webkit-scrollbar {
    width: 6px;
}

.agents-list::-webkit-scrollbar-track {
    background: #f1f1f1;
}

.agents-list::-webkit-scrollbar-thumb {
    background: #888;
    border-radius: 3px;
}

.agents-list::-webkit-scrollbar-thumb:hover {
    background: #555;
}
</style>
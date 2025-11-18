<template>
    <ConfirmModal ref="modal" />
    <div class="page-container">
        <!-- 로딩 오버레이 -->
        <div v-if="isLoading" class="loading-overlay">
            <div class="loading-content">
                <div class="loading-animation">
                    <div class="map-marker-pulse">
                        <i class="bi bi-geo-alt-fill"></i>
                    </div>
                    <div class="search-circles">
                        <div class="circle circle-1"></div>
                        <div class="circle circle-2"></div>
                        <div class="circle circle-3"></div>
                    </div>
                </div>
                <h3 class="loading-title">위치 분석 중</h3>
                <p class="loading-message">환자분의 위치를 통해 실종시 예측 지점을 분석하고 있어요!</p>
                <p class="loading-submessage">약 20초 정도 기다려주세요</p>
                <div class="loading-progress">
                    <div class="progress-bar"></div>
                </div>
            </div>
        </div>

        <!-- 지도 영역 -->
        <div class="page-containera">
            <div ref="mapContainer" class="map-area"></div>
        </div>

        <!-- 토글 버튼 영역 -->
        <div class="toggle-button-wrapper">
            <div class="d-flex">
                <button class="toggle-button" :class="{ active: selectedType === 'map' }" @click="mapOrInfo('map')">
                    <i class="bi bi-map-fill"></i>
                    <span class="button-text">예상위치</span>
                </button>

                <button class="toggle-button" :class="{ active: selectedType === 'simulation' }"
                    @click="mapOrInfo('simulation')">
                    <i class="bi bi-diagram-3-fill"></i>
                    <span class="button-text">시뮬레이션</span>
                </button>
            </div>
        </div>

        <!-- ⭐ 예상위치 타임라인 (예상위치일 때만) -->
        <div class="timeline-container" v-if="selectedType === 'map'">
            <div class="timeline-header">
                <i class="bi bi-clock-history"></i>
                <span class="timeline-title">실종 경과 시간</span>
            </div>

            <div class="timeline-wrapper" ref="timelineWrapper" @mousedown="startDrag" @touchstart="startDrag">
                <div class="timeline-track">
                    <div class="timeline-segment segment-1"></div>
                    <div class="timeline-segment segment-2"></div>
                    <div class="timeline-segment segment-3"></div>
                    <div class="timeline-progress" :style="{ width: progressWidth + '%' }">
                        <div class="timeline-glow"></div>
                    </div>
                </div>

                <div class="timeline-markers">
                    <div class="timeline-marker" style="left: 0%">
                        <div class="marker-dot"></div>
                        <span class="marker-label">현재</span>
                    </div>
                    <div class="timeline-marker" style="left: 33.33%">
                        <div class="marker-dot-1"></div>
                        <span class="marker-label">30분</span>
                    </div>
                    <div class="timeline-marker" style="left: 66.66%">
                        <div class="marker-dot-2"></div>
                        <span class="marker-label">60분</span>
                    </div>
                    <div class="timeline-marker" style="left: 100%">
                        <div class="marker-dot-3"></div>
                        <span class="marker-label">90분</span>
                    </div>
                </div>

                <div class="timeline-handle" :style="{ left: progressWidth + '%' }" @mousedown.stop="startDrag"
                    @touchstart.stop="startDrag">
                    <div class="handle-icon">
                        <i class="bi bi-person-walking"></i>
                    </div>
                    <div class="handle-tooltip">{{ selectedMinutes }}분</div>
                </div>
            </div>

            <div class="timeline-legend">
                <div class="legend-item" :class="{ active: selectedMinutes <= 30 }" @click="setTime(30)">
                    <div class="legend-color" style="background: #66bb6a;"></div>
                    <span class="legend-text">현재~30분</span>
                </div>
                <div class="legend-item" :class="{ active: selectedMinutes > 30 && selectedMinutes <= 60 }"
                    @click="setTime(60)">
                    <div class="legend-color" style="background: #ff9e7e;"></div>
                    <span class="legend-text">30~60분</span>
                </div>
                <div class="legend-item" :class="{ active: selectedMinutes > 60 }" @click="setTime(90)">
                    <div class="legend-color" style="background: #ff6b9d;"></div>
                    <span class="legend-text">60~90분</span>
                </div>
            </div>
        </div>

        <!-- ⭐⭐⭐ 시뮬레이션 컨트롤 패널 (시뮬레이션일 때만) -->
        <div class="simulation-control-container" v-if="selectedType === 'simulation'">
            <!-- 시나리오 선택 -->
            <div class="scenario-selector">
                <label>시나리오 선택</label>
                <div class="scenario-buttons">
                    <button v-for="scenario in availableScenarios" :key="scenario.scenario"
                        :class="['scenario-btn', { active: currentScenario === scenario.scenario }]"
                        @click="changeScenario(scenario.scenario)" :disabled="!scenario.available || simulationLoading">
                        <i class="bi bi-clock-history"></i>
                        <span>{{ scenario.scenario }}</span>
                    </button>
                </div>
            </div>

            <!-- 재생 컨트롤 -->
            <div class="playback-controls">
                <button @click="togglePlay" class="btn-play" :disabled="!simulationData || simulationLoading">
                    <i :class="isPlaying ? 'bi bi-pause-fill' : 'bi bi-play-fill'"></i>
                    <span>{{ isPlaying ? '일시정지' : '재생' }}</span>
                </button>
                <button @click="resetAnimation" class="btn-reset" :disabled="!simulationData || simulationLoading">
                    <i class="bi bi-arrow-counterclockwise"></i>
                    <span>초기화</span>
                </button>
                <select v-model="playbackSpeed" class="speed-selector" :disabled="!simulationData || simulationLoading">
                    <option :value="0.5">0.5배속</option>
                    <option :value="1">1배속</option>
                    <option :value="2">2배속</option>
                    <option :value="5">5배속</option>
                    <option :value="10">10배속</option>
                </select>
            </div>

            <!-- 시뮬레이션 타임라인 -->
            <div class="simulation-timeline-section" v-if="simulationData && !simulationLoading">
                <div class="simulation-timeline-info">
                    <span class="current-time">{{ formatTime(currentTime) }}</span>
                    <span class="separator">/</span>
                    <span class="total-time">{{ formatTime(totalTime) }}</span>
                </div>
                <input type="range" v-model="currentStep" :min="0" :max="totalSteps - 1" @input="updateFrame"
                    class="simulation-timeline-slider" :disabled="!simulationData || simulationLoading" />
            </div>
        </div>

        <!-- 컨텐츠 영역 -->
        <div class="content-section">
            <!-- ⭐ 예상 위치 리스트 -->
            <div v-if="selectedType === 'map'" class="prediction-list">
                <div v-if="isLoading" class="skeleton-container">
                    <div class="skeleton-card" v-for="i in 3" :key="i">
                        <div class="skeleton-icon"></div>
                        <div class="skeleton-content">
                            <div class="skeleton-line skeleton-line-long"></div>
                            <div class="skeleton-line skeleton-line-short"></div>
                        </div>
                    </div>
                </div>

                <div class="prediction-card" v-for="(loc, index) in displayedZoneToShow" :key="index"
                    :class="{ 'selected': selectedLocation && selectedLocation.lat === loc.lat && selectedLocation.lon === loc.lon }"
                    @click="selectLocation(loc, index)">
                    <div class="card-content">
                        <div class="location-header">
                            <div class="card-icon-wrapper">
                                <div class="location-icon-modern" :style="{
                                    background: getZoneLevelGradient(displayZoneLevel),
                                    boxShadow: `0 8px 20px ${getZoneLevelColor(displayZoneLevel)}60`
                                }">
                                    <span class="rank-number">{{ index + 1 }}</span>
                                </div>
                            </div>
                            <div class="location-text-wrapper">
                                <h4 class="location-name">
                                    {{ loc.sgg_nm + ' ' + loc.emd_nm + ' ' + loc.ri_nm || '주소 정보 없음' }}에 있는
                                </h4>
                                <p class="location-detail" v-if="loc.name">
                                    {{ loc.name }}에 있을 것 같아요!
                                </p>
                                <p class="location-detail" v-else>
                                    {{ loc.address2 }}
                                </p>
                            </div>
                            <div class="probability-badge-modern">
                                <span class="probability-text">최근 한달간 {{ loc.visitCount }}회 방문</span>
                            </div>
                        </div>
                        <p class="location-distance">
                        <div>
                            <i class="bi bi-geo-alt-fill"></i>
                            실종지로부터 {{ loc.dist_m }}m · {{ getTimeRangeText(((loc.dist_m) / 20).toFixed(0)) }}
                        </div>
                        </p>
                    </div>
                </div>

                <div class="d-flex justify-content-center" v-if="hasMoreData">
                    <button class="more-btn" @click="toggleShowMore">
                        <span>{{ showAllLocations ? '접기' : '더보기' }}</span>
                        <i :class="showAllLocations ? 'bi bi-chevron-up' : 'bi bi-chevron-down'"></i>
                    </button>
                </div>

                <div class="stats-dashboard-modern glass-card" v-if="!isLoading">
                    <h3 class="stats-title-modern">
                        <i class="bi bi-bar-chart"></i>
                        예측 분석 정보
                    </h3>
                    <div class="stats-grid">
                        <div v-if="less_data && selectedType === 'map'" class="stat-card-modern-1">
                            <div class="warning-icon-wrapper">
                                <i class="bi bi-exclamation-circle-fill"></i>
                            </div>
                            <div class="warning-content">
                                <h3 class="warning-title">데이터가 부족합니다</h3>
                                <p class="warning-message">관리하고 있는 환자에 대한 데이터가 부족해요.</p>
                                <span class="warning-subtitle">예측 위치들이 부정확할 수 있습니다.</span>
                            </div>
                        </div>
                        <div class="stat-card-modern">
                            <div class="stat-icon-modern" style="--stat-color: #667eea;">
                                <i class="bi bi-geo-alt"></i>
                            </div>
                            <div class="stat-content-modern">
                                <p class="stat-label-modern">분석 지점</p>
                                <p class="stat-value-modern"><span class="stat-unit"> {{ total_cluster }}개의 위치 분석
                                        결과</span>
                                </p>
                                <p class="stat-sublabel-modern-1">님의 실종위치로부터 각 시간대별</p>
                                <p class="stat-sublabel-modern-1">최대 5개의 위치를 보여줍니다</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ⭐⭐⭐ 시뮬레이션 결과 리스트 -->
            <div v-if="selectedType === 'simulation'" class="simulation-list">
                <div v-if="simulationLoading" class="skeleton-container">
                    <div class="skeleton-card" v-for="i in 3" :key="i">
                        <div class="skeleton-icon"></div>
                        <div class="skeleton-content">
                            <div class="skeleton-line skeleton-line-long"></div>
                            <div class="skeleton-line skeleton-line-short"></div>
                        </div>
                    </div>
                </div>

                <div v-else-if="simulationAgentsList.length > 0" class="prediction-card"
                    v-for="agent in simulationAgentsList" :key="agent.rank"
                    :class="{ 'selected': selectedSimulationAgent === agent.rank }"
                    @click="selectSimulationAgentItem(agent.rank)">
                    <div class="card-content">
                        <div class="location-header">
                            <div class="card-icon-wrapper">
                                <div class="location-icon-modern" :style="{
                                    background: getSimulationGradientByRank(agent.rank),
                                    boxShadow: `0 8px 20px ${getSimulationColorByRank(agent.rank)}60`
                                }">
                                    <span class="rank-number">{{ agent.rank }}</span>
                                </div>
                            </div>
                            <div class="location-text-wrapper">
                                <h4 class="location-name">
                                    {{ agent.address }}
                                </h4>
                                <p class="location-detail">
                                    시뮬레이션 기반 예측 위치
                                </p>
                            </div>
                            <div class="probability-badge-modern">
                                <span class="probability-text">
                                    확률 {{ agent.probability.toFixed(1) }}%
                                </span>
                            </div>
                        </div>
                        <p class="location-distance">
                        <div>
                            <i class="bi bi-geo-alt-fill"></i>
                            실종지로부터 {{ agent.distance }}m
                        </div>
                        </p>
                    </div>
                </div>

                <div class="stats-dashboard-modern glass-card">
                    <h3 class="stats-title-modern">
                        <i class="bi bi-diagram-3"></i>
                        시뮬레이션 분석 정보
                    </h3>
                    <div class="stats-grid">
                        <div class="stat-card-modern">
                            <div class="stat-icon-modern" style="--stat-color: #667eea;">
                                <i class="bi bi-diagram-3"></i>
                            </div>
                            <div class="stat-content-modern">
                                <p class="stat-label-modern">분석 방법</p>
                                <p class="stat-sublabel-modern-1">
                                    치매환자 행동 특성 기반으로
                                </p>
                                <p class="stat-sublabel-modern-1">
                                    100번의 시뮬레이션 결과입니다
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

// ========================================================================================
// API 키 설정
// ========================================================================================
const mapContainer = ref(null)
const KAKAO_JS_KEY = '7e0332c38832a4584b3335bed6ae30d8'
const VWORLD_API_KEY = '6A0CFFEF-45CF-3426-882D-44A63B5A5289'
const TMAP_API_KEY = 'pu1CWi6rz48GHLWhk7NI239il6I2j9fHaSLFeYoi'

const router = useRouter()

// ========================================================================================
// 데이터 상태 관리
// ========================================================================================
const zone_level_1 = ref([])
const zone_level_2 = ref([])
const zone_level_3 = ref([])

const missingLocation = ref({ lat: null, lon: null })
const displayZoneLevel = ref(1)
const isLoading = ref(false)
const selectedType = ref('map')
const selectedLocation = ref(null)
const selectedMinutes = ref(30)

const showAllLocations = ref(false)
const isDragging = ref(false)
const timelineWrapper = ref(null)

const patientUserNo = ref(null)
const missingTimeDB = ref(null)
const missingAddress = ref(null)

let less_data = ref(false)
let total_cluster = ref(null)

// ========================================================================================
// 시뮬레이션 상태 관리
// ========================================================================================
const allScenariosData = ref(null)
const simulationData = ref(null)
const simulationLoading = ref(false)
const simulationMarkers = []
const simulationPaths = {}
const selectedSimulationAgent = ref(null)

// ⭐ 시뮬레이션 애니메이션 관련
const currentScenario = ref('30분')
const availableScenarios = ref([
    { scenario: '30분', available: false },
    { scenario: '60분', available: false },
    { scenario: '90분', available: false }
])
const currentStep = ref(0)
const isPlaying = ref(false)
const playbackSpeed = ref(1)
let animationTimer = null

// ========================================================================================
// 지도 및 마커 관련 변수
// ========================================================================================
let map = null
let markers = []
let polylines = []
let centerMarker = null

const circles = ref({
    circle700: null,
    circle1500: null,
    circle2100: null
})

// ========================================================================================
// Computed Properties
// ========================================================================================
const displayedZone1 = computed(() => {
    const data = zone_level_1.value || []
    return showAllLocations.value ? data : data.slice(0, 3)
})

const displayedZone2 = computed(() => {
    const data = zone_level_2.value || []
    return showAllLocations.value ? data : data.slice(0, 3)
})

const displayedZone3 = computed(() => {
    const data = zone_level_3.value || []
    return showAllLocations.value ? data : data.slice(0, 3)
})

const displayedZoneToShow = computed(() => {
    if (displayZoneLevel.value === 1) return displayedZone1.value
    if (displayZoneLevel.value === 2) return displayedZone2.value
    if (displayZoneLevel.value === 3) return displayedZone3.value
    return []
})

const hasMoreData = computed(() => {
    let totalCount = 0
    if (displayZoneLevel.value === 1) totalCount = zone_level_1.value?.length || 0
    else if (displayZoneLevel.value === 2) totalCount = zone_level_2.value?.length || 0
    else if (displayZoneLevel.value === 3) totalCount = zone_level_3.value?.length || 0
    return totalCount > 3
})

const markerDataToDisplay = computed(() => {
    if (showAllLocations.value) {
        return displayedZoneToShow.value.map(item => ({
            ...item,
            zoneLevel: displayZoneLevel.value
        }))
    }

    let result = []
    if (displayZoneLevel.value >= 1) {
        result = result.concat(displayedZone1.value.map(item => ({ ...item, zoneLevel: 1 })))
    }
    if (displayZoneLevel.value >= 2) {
        result = result.concat(displayedZone2.value.map(item => ({ ...item, zoneLevel: 2 })))
    }
    if (displayZoneLevel.value >= 3) {
        result = result.concat(displayedZone3.value.map(item => ({ ...item, zoneLevel: 3 })))
    }
    return result
})

const progressWidth = computed(() => {
    return (selectedMinutes.value / 90) * 100
})

// ⭐ 시뮬레이션 에이전트 리스트
const simulationAgentsList = computed(() => {
    if (!simulationData.value?.frames?.[0]?.agents) return []

    return simulationData.value.frames[0].agents.map(agent => ({
        rank: agent.rank,
        agent_id: agent.agent_id,
        probability: agent.final_position.probability,
        lat: agent.final_position.latitude,
        lon: agent.final_position.longitude,
        address: agent.address || '주소 없음',
        distance: calculateDistance(
            missingLocation.value.lat,
            missingLocation.value.lon,
            agent.final_position.latitude,
            agent.final_position.longitude
        )
    }))
})

// ⭐ 시뮬레이션 애니메이션 Computed
const totalSteps = computed(() => {
    return simulationData.value?.total_steps || 0
})

const currentTime = computed(() => {
    if (!simulationData.value || currentStep.value >= simulationData.value.frames.length) {
        return 0
    }
    const frame = simulationData.value.frames[currentStep.value]
    return frame.agents[0]?.time_seconds || 0
})

const totalTime = computed(() => {
    return currentScenario.value === '30분' ? 1800 :
        currentScenario.value === '60분' ? 3600 :
            5400
})

// ========================================================================================
// 시뮬레이션 함수들
// ========================================================================================

// 시간 포맷팅
const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 재생/일시정지 토글
const togglePlay = () => {
    if (!simulationData.value) return
    isPlaying.value = !isPlaying.value

    if (isPlaying.value) {
        playAnimation()
    } else {
        stopAnimation()
    }
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

    Object.values(simulationPaths).forEach(path => {
        if (path.line && path.line.setMap) {
            path.line.setMap(null)
        }
        path.points = []
        if (path.line && path.line.setPath) {
            path.line.setPath([])
        }
    })

    updateFrame()
}

// 프레임 업데이트
const updateFrame = () => {
    if (!simulationData.value || !map) return

    if (!simulationData.value.frames) {
        console.warn('프레임 데이터 없음')
        return
    }

    const frames = simulationData.value.frames
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
            // 마커 업데이트
            const markerObj = simulationMarkers.find(m => m.rank === agent.rank)
            if (markerObj) {
                const newPosition = new window.kakao.maps.LatLng(agent.latitude, agent.longitude)
                markerObj.overlay.setPosition(newPosition)
            }

            // 경로 업데이트
            if (simulationPaths[agent.rank]) {
                const point = new window.kakao.maps.LatLng(agent.latitude, agent.longitude)
                simulationPaths[agent.rank].points.push(point)
                simulationPaths[agent.rank].line.setPath(simulationPaths[agent.rank].points)

                if (simulationPaths[agent.rank].points.length >= 2 && !simulationPaths[agent.rank].line.getMap()) {
                    simulationPaths[agent.rank].line.setMap(map)
                }
            }
        } catch (error) {
            console.error(`프레임 업데이트 실패 (rank ${agent.rank}):`, error)
        }
    })
}

// 시나리오 변경
const changeScenario = async (scenario) => {
    if (scenario === currentScenario.value) return
    if (!allScenariosData.value || !allScenariosData.value[scenario]) {
        console.error('❌ 시나리오 데이터 없음:', scenario)
        return
    }

    console.log(`🔄 시나리오 변경: ${currentScenario.value} → ${scenario}`)

    currentScenario.value = scenario
    isPlaying.value = false
    stopAnimation()

    // 기존 마커/경로 제거
    clearSimulationMarkers()
    clearSimulationPaths()

    // 새 시나리오 데이터 설정
    simulationData.value = allScenariosData.value[scenario]
    currentStep.value = 0

    console.log(`✅ ${scenario} 데이터 로드 완료`)

    await nextTick()

    setTimeout(() => {
        console.log('🚀 에이전트 재초기화')
        initializeSimulationAgents()
    }, 300)
}

// 시뮬레이션 경로 초기화
const clearSimulationPaths = () => {
    Object.values(simulationPaths).forEach(path => {
        if (path.line && path.line.setMap) {
            path.line.setMap(null)
        }
    })
    Object.keys(simulationPaths).forEach(key => delete simulationPaths[key])
}

// 에이전트 초기화
const initializeSimulationAgents = () => {
    console.log('👥 initializeSimulationAgents 호출')

    if (!simulationData.value || !map) {
        console.warn('❌ 지도 또는 시뮬레이션 데이터가 준비되지 않음')
        return
    }

    if (!simulationData.value.frames || simulationData.value.frames.length === 0) {
        console.warn('❌ 프레임 데이터 없음')
        return
    }

    const firstFrame = simulationData.value.frames[0]

    if (!firstFrame.agents || firstFrame.agents.length === 0) {
        console.warn('❌ 에이전트 데이터 없음')
        return
    }

    console.log(`✅ 에이전트 초기화 시작: ${firstFrame.agents.length}개`)

    firstFrame.agents.forEach(agent => {
        createAgentMarker(agent)
        createAgentPath(agent)
    })

    updateFrame()
    console.log('✅ 에이전트 초기화 완료')
}

// 에이전트 마커 생성
const createAgentMarker = (agent) => {
    if (!map) {
        console.warn('❌ map이 없어서 마커 생성 불가')
        return
    }

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

        // 클릭 이벤트
        markerContent.onclick = function () {
            selectSimulationAgentItem(agent.rank)
        }

        simulationMarkers.push({ rank: agent.rank, overlay: customOverlay, position: position })
        console.log(`✅ 마커 생성 완료: rank ${agent.rank}`)
    } catch (error) {
        console.error(`❌ 마커 생성 실패 (rank ${agent.rank}):`, error)
    }
}

// 에이전트 경로 생성
const createAgentPath = (agent) => {
    if (!map) {
        console.warn('❌ map이 없어서 경로 생성 불가')
        return
    }

    try {
        const polyline = new window.kakao.maps.Polyline({
            map: null,
            path: [],
            strokeWeight: 3,
            strokeColor: getColorByRank(agent.rank),
            strokeOpacity: 0.7,
            strokeStyle: 'solid',
            zIndex: 2
        })

        simulationPaths[agent.rank] = { line: polyline, points: [] }
        console.log(`✅ 경로 생성 완료: rank ${agent.rank}`)
    } catch (error) {
        console.error(`❌ 경로 생성 실패 (rank ${agent.rank}):`, error)
    }
}

// 에이전트 아이템 선택
const selectSimulationAgentItem = (rank) => {
    selectedSimulationAgent.value = rank === selectedSimulationAgent.value ? null : rank

    simulationMarkers.forEach(markerObj => {
        const element = markerObj.overlay.getContent()
        if (markerObj.rank === rank && selectedSimulationAgent.value !== null) {
            element.classList.add('selected')
        } else {
            element.classList.remove('selected')
        }
    })

    // 선택된 에이전트 위치로 지도 이동
    if (selectedSimulationAgent.value !== null) {
        const agent = simulationAgentsList.value.find(a => a.rank === rank)
        if (agent && map) {
            const position = new window.kakao.maps.LatLng(agent.lat, agent.lon)
            map.panTo(position)
        }
    }
}
///
/// Modal 띄우기위해 import
///
import ConfirmModal from '../components/predict_split_Modal.vue'
const modal = ref(null)

// ========================================================================================
// API 호출 - 예측 데이터
// ========================================================================================
async function fetchPredictionData() {
    console.log(`fetchPredictionData 실행됨`)
    isLoading.value = true

    try {
        const myPatientResponse = await axios.get('/api/user/my-patient', {
            withCredentials: true
        })

        patientUserNo.value = myPatientResponse.data.userNo

        if (!patientUserNo.value) {
            console.error('❌ 연결된 환자가 없습니다!')
            alert('연결된 환자가 없습니다. 메인화면으로 돌아갑니다.')
            router.push('/GD')
            return false
        }

        console.log(`✅ 환자 번호: ${patientUserNo.value}`)

        const now = new Date()
        const year = now.getFullYear()
        const month = String(now.getMonth() + 1).padStart(2, '0')
        const day = String(now.getDate()).padStart(2, '0')
        const hours = String(now.getHours()).padStart(2, '0')
        const minutes = String(now.getMinutes()).padStart(2, '0')

        const missingTime = `${year}-${month}-${day} ${hours}:${minutes}`
        console.log(`📅 실종 시간: ${missingTime}`)

        const gpsResponse = await axios.get(`/api/pred/${patientUserNo.value}`, {
            params: { datetime: new Date(missingTime).getTime() },
            withCredentials: true
        })

        const gpsData = gpsResponse.data
        console.log(`✅ GPS 데이터 수신: ${gpsData.length}개 레코드`)

        if (gpsData.length < 10080) {
            const lastGPSData = gpsData[gpsData.length - 1];
            console.log(`lastGPSData.latitude,lastGPSData.latitude :: ${lastGPSData.latitude} ${lastGPSData.longitude} `)
            const confirmed = await modal.value.show(
                '환자의 위치데이터가 부족하여 시뮬레이션만 제공됩니다',
                { showCancel: false }
            )

            router.push({
                path: '/simulation',
                query: {
                    userNo: patientUserNo.value,
                    lat: lastGPSData.latitude,
                    lon: lastGPSData.longitude,
                    missingTime: missingTimeDB.value
                }
            });
            return;
        } else if (gpsData.length < 3 * 20 * 24 * 28) {
            less_data.value = true
        }

        const gpsRecords = gpsData.map(record => {
            let recordTime = record.recordTime
            if (recordTime && recordTime.split(':').length === 2) {
                recordTime = `${recordTime}:00`
            }
            return {
                latitude: record.latitude,
                longitude: record.longitude,
                record_time: recordTime
            }
        })

        const requestBody = {
            user_no: patientUserNo.value,
            missing_time: missingTime,
            gps_data: gpsRecords,
            analysis_days: 60,
            time_window_hours: 6,
            session_gap: 30,
            min_cluster_size: 5,
            max_search_radius: 2000,
            min_cluster_separation: 200,
            road_network_radius: 2500,
            csv_path: 'all_locations.csv'
        }

        const response = await axios.post(
            `http://localhost:8000/api/predict-destinations/test`,
            requestBody,
            {
                withCredentials: true,
                headers: { 'Content-Type': 'application/json' }
            }
        )

        const data = response.data
        console.log(`✅ FastAPI 응답 수신`)
        total_cluster.value = data.total_clusters_found

        await processDestinationsToZones(data)

        console.log(`✅ fetchPredictionData 완료`)
        return true

    } catch (error) {
        console.error('❌ 예측 데이터 로드 실패:', error)
        alert('예측 데이터를 불러오는 중 오류가 발생했습니다.')

        if (error.response) {
            console.error(`   - 상태 코드: ${error.response.status}`)
            console.error(`   - 에러 메시지: ${JSON.stringify(error.response.data)}`)
        }
        return false
    } finally {
        isLoading.value = false
    }
}

// ========================================================================================
// API 응답을 Zone 배열로 변환
// ========================================================================================
async function processDestinationsToZones(apiResponse) {
    console.log('🔄 API 응답 처리 시작...')

    if (apiResponse.last_known_location) {
        missingLocation.value.lat = apiResponse.last_known_location.latitude
        missingLocation.value.lon = apiResponse.last_known_location.longitude
    }

    const destinationsByDistance = apiResponse.destinations_by_distance || {}

    zone_level_1.value = (destinationsByDistance['500m'] || []).map((dest) => ({
        id: dest.destination_id,
        lat: dest.latitude,
        lon: dest.longitude,
        name: dest.name || '',
        visitCount: dest.visit_count,
        distance: dest.distance_meters,
        waypoints: dest.waypoints || [],
        preferenceScore: dest.preference_score,
        totalGpsRecords: dest.total_gps_records,
        clusterStability: dest.cluster_stability,
        routeMethod: dest.route_method,
        value: dest.preference_score,
        address1: '',
        address2: '',
        sido_nm: '',
        sgg_nm: '',
        emd_nm: '',
        ri_nm: '',
        jimok: '',
        dist_m: 0
    }))

    zone_level_2.value = (destinationsByDistance['1000m'] || []).map((dest) => ({
        id: dest.destination_id,
        lat: dest.latitude,
        lon: dest.longitude,
        name: dest.name || '',
        visitCount: dest.visit_count,
        distance: dest.distance_meters,
        waypoints: dest.waypoints || [],
        preferenceScore: dest.preference_score,
        totalGpsRecords: dest.total_gps_records,
        clusterStability: dest.cluster_stability,
        routeMethod: dest.route_method,
        value: dest.preference_score,
        address1: '',
        address2: '',
        sido_nm: '',
        sgg_nm: '',
        emd_nm: '',
        ri_nm: '',
        jimok: '',
        dist_m: 0
    }))

    zone_level_3.value = (destinationsByDistance['1500m'] || []).map((dest) => ({
        id: dest.destination_id,
        lat: dest.latitude,
        lon: dest.longitude,
        name: dest.name || '',
        visitCount: dest.visit_count,
        distance: dest.distance_meters,
        waypoints: dest.waypoints || [],
        preferenceScore: dest.preference_score,
        totalGpsRecords: dest.total_gps_records,
        clusterStability: dest.cluster_stability,
        routeMethod: dest.route_method,
        value: dest.preference_score,
        address1: '',
        address2: '',
        sido_nm: '',
        sgg_nm: '',
        emd_nm: '',
        ri_nm: '',
        jimok: '',
        dist_m: 0
    }))

    console.log(`✅ Zone 데이터 생성 완료`)

    await getAddressAndJimok()

    zone_level_1.value = [...zone_level_1.value]
    zone_level_2.value = [...zone_level_2.value]
    zone_level_3.value = [...zone_level_3.value]

    await requestAllRoutes()

    if (map) {
        setCenter(true)
        makeMarker()
        initCircles()
        showCirclesByZoneLevel(displayZoneLevel.value)
    }

    console.log('✅ processDestinationsToZones 완료')
}

// ========================================================================================
// 주소 조회 관련 함수들 (기존 코드와 동일)
// ========================================================================================
async function getAddressAndJimok() {
    console.log('🗺️ VWorld API 호출 시작...')

    const columns = [
        'pnu', 'sido_nm', 'sgg_nm', 'emd_nm', 'ri_nm',
        'jibun', 'jimok', 'parea', 'rn_nm', 'bld_mnnm',
        'bld_slno', 'ag_geom'
    ].join(',')

    const allZones = [
        { level: 1, data: zone_level_1.value },
        { level: 2, data: zone_level_2.value },
        { level: 3, data: zone_level_3.value }
    ]

    for (const zone of allZones) {
        if (!zone.data || zone.data.length === 0) continue

        console.log(`⏳ Zone ${zone.level} 처리 시작 (${zone.data.length}개)`)

        await Promise.all(
            zone.data.map(async (location, index) => {
                try {
                    await processLocation(location, zone.level, index, columns)
                } catch (e) {
                    console.error(`❌ Zone ${zone.level}-${index + 1} 예상치 못한 에러:`, e)
                }
            })
        )
    }

    console.log('🗺️ 모든 API 호출 완료')
}

async function processLocation(location, zoneLevel, locationIndex, columns) {
    location.dist_m = calculateDistance(
        missingLocation.value.lat,
        missingLocation.value.lon,
        location.lat,
        location.lon
    )

    try {
        const vworldData = await fetchVWorldData(location, columns)

        if (vworldData.status === 'NOT_FOUND' || !vworldData.properties) {
            console.warn(`Zone ${zoneLevel}-${locationIndex + 1}: VWorld 데이터 없음 - Kakao 폴백`)
            await handleKakaoFallback(location)
            return
        }

        if (vworldData.status === 'ERROR') {
            console.error(`Zone ${zoneLevel}-${locationIndex + 1}: ERROR - ${vworldData.errorText || '알수없는에러'}`)
            setLocationError(location)
            return
        }

        await setLocationFromVWorld(location, vworldData.properties)

        await delay(150)

    } catch (e) {
        console.error(`Zone ${zoneLevel}-${locationIndex + 1} 예상치 못한 에러:`, e)
        setLocationError(location)
    }
}

async function fetchVWorldData(location, columns) {
    const dataParams = new URLSearchParams({
        service: 'data',
        version: '2.0',
        request: 'GetFeature',
        format: 'json',
        errorformat: 'json',
        size: '10',
        page: '1',
        data: 'LT_C_LANDINFOBASEMAP',
        geomfilter: `POINT(${location.lon} ${location.lat})`,
        columns: columns,
        geometry: 'true',
        attribute: 'true',
        buffer: '10',
        crs: 'EPSG:4326',
        key: VWORLD_API_KEY,
        domain: 'api.vworld.kr'
    })

    const dataUrl = `https://api.vworld.kr/req/data?${dataParams.toString()}`
    const dataProxyUrl = `https://www.vworld.kr/proxy.do?url=${encodeURIComponent(dataUrl)}`

    const dataRes = await fetch(dataProxyUrl)

    if (!dataRes.ok) {
        throw new Error(`VWorld Data API HTTP error! status: ${dataRes.status}`)
    }

    const dataText = await dataRes.text()

    let dataResp = JSON.parse(dataText)
    const properties = dataResp?.response?.result?.featureCollection?.features?.[0]?.properties

    return {
        status: dataResp?.response?.status || dataResp?.status || 'ERROR',
        errorText: dataResp?.response?.error?.text || dataResp?.error?.text,
        properties: properties
    }
}

async function setLocationFromVWorld(location, props) {
    const sido = props.sido_nm || ''
    const sgg = props.sgg_nm || ''
    const emd = props.emd_nm || ''
    const ri = props.ri_nm || ''
    const jimok = props.jimok || '토지'

    location.sido_nm = sido
    location.sgg_nm = sgg
    location.emd_nm = emd
    location.ri_nm = ri
    location.jimok = jimok

    const addressParts = [sgg, emd, ri].filter(part => part)
    location.address1 = addressParts.join(' ')

    location.address2 = await generateAddress2(jimok, location.address1)
}

async function generateAddress2(jimok, address1) {
    const jimokNaturalText = convertJimokToNaturalLanguage(jimok)

    const excludeJimok = ['전', '답', '임야', '도로']

    if (!excludeJimok.includes(jimok)) {
        try {
            const poiResult = await searchVWorldPOI(address1)

            if (poiResult && poiResult.poiName) {
                const result = `'${poiResult.poiName}'에 있을 것 같아요!`
                return result
            } else {
                const result = `도로에 있을 것 같아요!`
                return result
            }
        } catch (e) {
            console.error(`❌ POI 검색 에러:`, e)
            const result = `도로에 있을 것 같아요!`
            return result
        }
    } else {
        const result = `${jimokNaturalText}에 있을 것 같아요!`
        return result
    }
}

async function handleKakaoFallback(location) {
    const kakaoAddress = await getKakaoAddressFromCoord(location.lat, location.lon)

    if (kakaoAddress && kakaoAddress.sido) {
        const addressParts = [kakaoAddress.sido, kakaoAddress.gungu, kakaoAddress.eup].filter(part => part)
        location.address1 = addressParts.join(' ')
        location.address2 = '에 있을 것 같아요!'
        location.sido_nm = kakaoAddress.sido
        location.sgg_nm = kakaoAddress.gungu
        location.emd_nm = kakaoAddress.eup

        console.log(`✅ Kakao 폴백 완료: address1=${location.address1}`)
    } else {
        setLocationError(location)
        console.warn('❌ Kakao Geocoder 폴백 실패')
    }
}

function setLocationError(location) {
    location.address1 = '위치 정보 없음'
    location.address2 = ''
    console.warn(`⚠️ 위치 정보 에러 처리`)
}

function convertJimokToNaturalLanguage(jimok) {
    const jimokMap = {
        '전': '밭',
        '답': '논',
        '임야': '산',
        '도로': '도로',
        '공원': '공원',
        '건물': '건물',
        '주택': '주택'
    }

    if (jimokMap[jimok]) {
        return jimokMap[jimok]
    }

    for (const [key, value] of Object.entries(jimokMap)) {
        if (jimok.includes(key)) {
            return value
        }
    }

    return jimok
}

async function searchVWorldPOI(address) {
    if (!address || address.trim() === '') {
        return null
    }

    try {
        const searchData = new URLSearchParams({
            service: 'search',
            version: '2.0',
            request: 'search',
            query: address,
            type: 'place',
            format: 'json',
            errorformat: 'json',
            crs: 'EPSG:4326',
            page: '1',
            size: '5',
            key: VWORLD_API_KEY,
            domain: 'api.vworld.kr'
        })

        const searchUrl = `https://api.vworld.kr/req/search?${searchData.toString()}`
        const dataProxyUrl = `https://www.vworld.kr/proxy.do?url=${encodeURIComponent(searchUrl)}`

        const response = await fetch(dataProxyUrl)

        if (!response.ok) {
            console.warn(`⚠️ POI API 응답 실패: ${response.status}`)
            return null
        }

        const data = await response.json()

        if (data?.response?.result?.items && data.response.result.items.length > 0) {
            const firstItem = data.response.result.items[0]

            const result = {
                poiName: firstItem.title || firstItem.name || 'POI',
                poiType: firstItem.category || '',
                address: firstItem.address || '',
                point: firstItem.point ? {
                    x: firstItem.point.x,
                    y: firstItem.point.y
                } : null
            }

            return result
        }

        return null

    } catch (e) {
        console.error('❌ VWorld POI 검색 에러:', e)
        return null
    }
}

// Kakao Geocoder (폴백용)
async function getKakaoAddressFromCoord(lat, lon) {
    return new Promise((resolve) => {
        if (!window.kakao || !window.kakao.maps || !window.kakao.maps.services) {
            console.error('Kakao Geocoder 서비스가 로드되지 않았습니다.')
            resolve(null)
            return
        }

        const geocoder = new window.kakao.maps.services.Geocoder()

        geocoder.coord2Address(lon, lat, (result, status) => {
            if (status === window.kakao.maps.services.Status.OK && result.length > 0) {
                const addressInfo = result[0].address
                resolve({
                    sido: addressInfo.region_1depth_name || '',
                    gungu: addressInfo.region_2depth_name || '',
                    eup: addressInfo.region_3depth_name || ''
                })
            } else {
                console.warn('Kakao Geocoder: 주소 조회 실패')
                resolve(null)
            }
        })
    })
}

// ========================================================================================
// 시뮬레이션 데이터 로드
// ========================================================================================
async function loadSimulationData() {
    simulationLoading.value = true

    console.log(`🚀 시뮬레이션 데이터 로드 시작`)
    console.log(`위도: ${missingLocation.value.lat}, 경도: ${missingLocation.value.lon}`)

    try {
        const response = await axios.post(
            'http://localhost:8000/api/agent-simulation/run-all',
            {
                user_no: patientUserNo.value,
                latitude: missingLocation.value.lat,
                longitude: missingLocation.value.lon
            },
            {
                withCredentials: true,
                headers: { 'Content-Type': 'application/json' }
            }
        )

        console.log('✅ 시뮬레이션 응답 수신:', response.data)

        const { scenarios } = response.data

        if (scenarios) {
            console.log('🔄 모든 시나리오의 에이전트 주소 조회 시작...')

            for (const [scenarioKey, scenarioData] of Object.entries(scenarios)) {
                console.log(`\n📍 시나리오: ${scenarioKey}`)

                if (!scenarioData.frames || scenarioData.frames.length === 0) {
                    console.log(`⚠️ ${scenarioKey}에 프레임 없음`)
                    continue
                }

                const firstFrame = scenarioData.frames[0]
                if (!firstFrame.agents || firstFrame.agents.length === 0) {
                    console.log(`⚠️ ${scenarioKey}의 첫 프레임에 에이전트 없음`)
                    continue
                }

                console.log(`✅ ${scenarioKey}: ${firstFrame.agents.length}개 에이전트 발견`)

                for (let i = 0; i < firstFrame.agents.length; i++) {
                    const agent = firstFrame.agents[i]

                    try {
                        console.log(`⏳ [${scenarioKey}] Agent ${agent.rank} 조회 중... (${i + 1}/${firstFrame.agents.length})`)

                        await fetchAgentAddress(agent)

                        console.log(`✅ [${scenarioKey}] Agent ${agent.rank}: ${agent.address}`)
                    } catch (e) {
                        console.error(`❌ [${scenarioKey}] Agent ${agent.rank} 조회 실패:`, e)
                        agent.address = '조회 실패'
                    }

                    await delay(300)
                }

                console.log(`✅ ${scenarioKey} 모든 에이전트 주소 조회 완료!\n`)
            }

            console.log('✅✅✅ 모든 시나리오 주소 조회 완료')
        }

        allScenariosData.value = scenarios

        availableScenarios.value = [
            { scenario: '30분', available: !!scenarios['30분'] },
            { scenario: '60분', available: !!scenarios['60분'] },
            { scenario: '90분', available: !!scenarios['90분'] }
        ]

        simulationData.value = scenarios['30분']
        currentStep.value = 0

        console.log('📦 시뮬레이션 데이터 설정 완료')

    } catch (err) {
        console.error('❌ 시뮬레이션 데이터 로드 실패:', err)
        console.error('에러 상세:', err.response?.data)
        alert('시뮬레이션 데이터를 불러올 수 없습니다.')
    } finally {
        simulationLoading.value = false
    }
}

// VWorld 주소 조회
async function fetchAgentAddress(agent) {
    try {
        const params = new URLSearchParams({
            service: 'address',
            version: '2.0',
            request: 'getaddress',
            crs: 'epsg:4326',
            format: 'json',
            point: `${agent.final_position.longitude},${agent.final_position.latitude}`,
            type: 'both',
            key: VWORLD_API_KEY
        })

        const apiUrl = `https://api.vworld.kr/req/address?${params.toString()}`
        const proxyUrl = `https://www.vworld.kr/proxy.do?url=${encodeURIComponent(apiUrl)}`

        const response = await fetch(proxyUrl)
        const data = await response.json()

        if (data.response?.result?.length > 0) {
            agent.address = data.response.result[0].text
        } else {
            agent.address = '주소 조회 실패'
        }
    } catch (error) {
        console.error('주소 조회 실패:', error)
        agent.address = '주소 조회 실패'
    }
}

// ========================================================================================
// 시뮬레이션 마커 생성/제거
// ========================================================================================
function clearSimulationMarkers() {
    simulationMarkers.forEach(markerObj => {
        if (markerObj.overlay && markerObj.overlay.setMap) {
            markerObj.overlay.setMap(null)
        }
    })
    simulationMarkers.length = 0
}

function getSimulationColorByRank(rank) {
    if (rank <= 3) return '#FF0000'
    if (rank <= 6) return '#FF6B00'
    return '#FFA500'
}

const getColorByRank = getSimulationColorByRank

function getSimulationGradientByRank(rank) {
    if (rank <= 3) return 'linear-gradient(135deg, #FF0000 0%, #FF4444 100%)'
    if (rank <= 6) return 'linear-gradient(135deg, #FF6B00 0%, #FF8833 100%)'
    return 'linear-gradient(135deg, #FFA500 0%, #FFB733 100%)'
}

// ========================================================================================
// 탭 전환 함수
// ========================================================================================
async function mapOrInfo(type) {
    selectedType.value = type
    console.log(`\n🔀 탭 전환: ${type}`)

    if (type === 'map') {
        // 예상위치 표시
        clearSimulationMarkers()
        clearSimulationPaths()
        stopAnimation()
        isPlaying.value = false
        hideMarkers()
        showMarkers()
        showCirclesByZoneLevel(displayZoneLevel.value)

    } else if (type === 'simulation') {
        // 시뮬레이션 표시
        hideMarkers()
        hideCircles()
        clearAllRoutes()

        if (!simulationData.value) {
            await loadSimulationData()
        }

        await nextTick()

        setTimeout(() => {
            initializeSimulationAgents()

            if (map) {
                setCenter(true)
            }
        }, 300)
    }
}

// ========================================================================================
// 거리 계산 함수
// ========================================================================================
function calculateDistance(lat1, lon1, lat2, lon2) {
    const toRadian = angle => (Math.PI / 180) * angle
    const R = 6371000

    const dLat = toRadian(lat2 - lat1)
    const dLon = toRadian(lon2 - lon1)

    const lat1Rad = toRadian(lat1)
    const lat2Rad = toRadian(lat2)

    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1Rad) * Math.cos(lat2Rad) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    const distance = R * c

    return Math.round(distance)
}

// ========================================================================================
// 카카오맵 초기화
// ========================================================================================
onMounted(async () => {
    isLoading.value = true
    selectedType.value = 'map'

    await fetchPredictionData()

    try {
        loadKakaoMap(mapContainer.value)
        setTimeout(() => {
            if (map) {
                setCenter(true)
                makeMarker()
                initCircles()
                showCirclesByZoneLevel(displayZoneLevel.value)
            }
        }, 1000)
    } catch (e) {
        console.error("지도 초기화 중 오류:", e)
        isLoading.value = false
    }
})

const loadKakaoMap = (container) => {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false&libraries=services`
    document.head.appendChild(script)

    script.onload = () => {
        window.kakao.maps.load(() => {
            const options = {
                center: new window.kakao.maps.LatLng(
                    missingLocation.value.lat || 37.5666805,
                    missingLocation.value.lon || 126.9784147
                ),
                level: 5,
            }

            map = new window.kakao.maps.Map(container, options)
            console.log('지도 초기화 완료')

            if (missingLocation.value.lat && missingLocation.value.lon) {
                centerMarker = new window.kakao.maps.Marker({
                    position: new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon),
                    map: map,
                    image: createCenterMarkerImage()
                })
            }
        })
    }
}

function createCenterMarkerImage() {
    const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 20 20">
  <circle cx="10" cy="10" r="9.5" 
          fill="none" 
          stroke="#FF6B35" 
          stroke-width="1"/>
  <g transform="translate(2, 2)">
    <path fill="#E63946" d="M11 5a3 3 0 1 1-6 0 3 3 0 0 1 6 0m-9 8c0 1 1 1 1 1h5.256A4.5 4.5 0 0 1 8 12.5a4.5 4.5 0 0 1 1.544-3.393Q8.844 9.002 8 9c-5 0-6 3-6 4"/>
    <path fill="#E63946" d="M16 12.5a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0m-3.5-2a.5.5 0 0 0-.5.5v1.5a.5.5 0 0 0 1 0V11a.5.5 0 0 0-.5-.5m0 4a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1"/>
  </g>
</svg>
    `

    return new window.kakao.maps.MarkerImage(
        'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg),
        new window.kakao.maps.Size(50, 60),
        { offset: new window.kakao.maps.Point(25, 58) }
    )
}

// ========================================================================================
// 마커 관련 함수
// ========================================================================================
function hideMarkers() {
    for (let marker of markers) {
        if (marker && marker.setVisible) {
            marker.setVisible(false)
        }
    }
}

function showMarkers() {
    for (let marker of markers) {
        if (marker && marker.setVisible) {
            marker.setVisible(true)
        }
    }
}

function makeMarker() {
    if (!map) {
        console.warn('⚠️ 지도가 초기화되지 않아 마커를 생성할 수 없습니다.')
        return
    }

    const currentCenter = map.getCenter()
    const currentLevel = map.getLevel()

    try {
        markers.forEach(marker => {
            if (marker && marker.setMap && typeof marker.setMap === 'function') {
                marker.setMap(null)
            }
        })
    } catch (error) {
        console.warn('마커 제거 중 오류:', error)
    }

    markers = []

    markerDataToDisplay.value.forEach((item, index) => {
        try {
            const markerPosition = new window.kakao.maps.LatLng(item.lat, item.lon)
            const markerColor = getMarkerColor(item.zoneLevel)

            const marker = new window.kakao.maps.Marker({
                position: markerPosition,
                map: map,
                title: `${item.name} - ${Math.round(item.distance)}m`,
                image: createColoredMarkerImage(markerColor)
            })

            window.kakao.maps.event.addListener(marker, 'click', function () {
                selectLocation(item, index)
            })

            markers.push(marker)
        } catch (error) {
            console.error(`마커 ${index} 생성 중 오류:`, error)
        }
    })

    if (!selectedLocation.value) {
        map.setCenter(currentCenter)
        map.setLevel(currentLevel)
    }

    console.log(`✅ 마커 ${markers.length}개 생성 완료`)
}

function getMarkerColor(zoneLevel) {
    const colors = {
        1: '#66bb6a',
        2: '#ff9e7e',
        3: '#ff6b9d'
    }
    return colors[zoneLevel] || '#4ECDC4'
}

function createColoredMarkerImage(color) {
    const svg = `
        <svg width="32" height="40" viewBox="0 0 32 40" xmlns="http://www.w3.org/2000/svg">
            <path d="M16 0C7.16 0 0 7.16 0 16c0 12 16 24 16 24s16-12 16-24c0-8.84-7.16-16-16-16z" 
                  fill="${color}"/>
            <circle cx="16" cy="16" r="6" fill="white"/>
        </svg>
    `

    return new window.kakao.maps.MarkerImage(
        `data:image/svg+xml;base64,${btoa(svg)}`,
        new window.kakao.maps.Size(32, 40),
        { offset: new window.kakao.maps.Point(16, 40) }
    )
}

// ========================================================================================
// UI 관련 함수
// ========================================================================================
function selectLocation(loc, index) {
    if (selectedLocation.value &&
        selectedLocation.value.lat === loc.lat &&
        selectedLocation.value.lon === loc.lon) {
        selectedLocation.value = null
        clearAllRoutes()
        return
    }

    selectedLocation.value = loc

    if (map) {
        const position = new window.kakao.maps.LatLng(loc.lat, loc.lon)
        map.panTo(position)

        const currentLevel = map.getLevel()
        if (currentLevel > 6) {
            map.setLevel(6, { animate: true })
        }

        drawRoute(index, displayZoneLevel.value)

        console.log(`✅ 위치 선택: ${loc.address1}`)
    }
}

// ========================================================================================
// 색상 관련 헬퍼 함수
// ========================================================================================
function getZoneLevelColor(level) {
    const colors = {
        1: '#4CAF50',
        2: '#FF6B35',
        3: '#E91E63'
    }
    return colors[level] || '#4CAF50'
}

function getZoneLevelGradient(level) {
    const gradients = {
        1: 'linear-gradient(135deg, #66bb6a 0%, #4caf50 100%)',
        2: 'linear-gradient(135deg, #ff9e7e 0%, #ff6b35 100%)',
        3: 'linear-gradient(135deg, #ff6b9d 0%, #e91e63 100%)'
    }
    return gradients[level] || gradients[1]
}

function getTimeRangeText(minutes) {
    const min = parseInt(minutes)
    if (min < 30) return '0-30분'
    if (min < 60) return '30-60분'
    return '60-90분'
}

// ========================================================================================
// 타임라인 관련
// ========================================================================================
function setTime(minutes) {
    selectedMinutes.value = minutes
    showAllLocations.value = false
    selectedLocation.value = null
    clearAllRoutes()

    let newLevel = 1
    if (minutes <= 30) newLevel = 1
    else if (minutes <= 60) newLevel = 2
    else newLevel = 3

    displayZoneLevel.value = newLevel
}

function startDrag(event) {
    if (!timelineWrapper.value) return

    isDragging.value = true
    updateTimeFromEvent(event)

    const moveHandler = (e) => {
        if (isDragging.value) {
            updateTimeFromEvent(e)
        }
    }

    const endHandler = () => {
        isDragging.value = false
        document.removeEventListener('mousemove', moveHandler)
        document.removeEventListener('mouseup', endHandler)
        document.removeEventListener('touchmove', moveHandler)
        document.removeEventListener('touchend', endHandler)
    }

    document.addEventListener('mousemove', moveHandler)
    document.addEventListener('mouseup', endHandler)
    document.addEventListener('touchmove', moveHandler)
    document.addEventListener('touchend', endHandler)
}

function updateTimeFromEvent(event) {
    if (!timelineWrapper.value) return

    const rect = timelineWrapper.value.getBoundingClientRect()
    const clientX = event.touches ? event.touches[0].clientX : event.clientX
    const x = clientX - rect.left
    const percentage = Math.max(0, Math.min(1, x / rect.width))

    selectedMinutes.value = Math.round(percentage * 90)
}

// ========================================================================================
// 더보기 버튼
// ========================================================================================
function toggleShowMore() {
    showAllLocations.value = !showAllLocations.value
    console.log(`더보기 토글: ${showAllLocations.value}`)
}

// ========================================================================================
// Circle 관련
// ========================================================================================
function initCircles() {
    if (!map || !missingLocation.value.lat || !missingLocation.value.lon) {
        console.error('지도 또는 실종 위치가 초기화되지 않았습니다.')
        return
    }

    const center = new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon)

    circles.value.circle700 = new window.kakao.maps.Circle({
        center: center,
        radius: 0,
        strokeWeight: 3,
        strokeColor: '#66bb6a',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
        fillColor: '#66bb6a',
        fillOpacity: 0.15
    })

    circles.value.circle1500 = new window.kakao.maps.Circle({
        center: center,
        radius: 0,
        strokeWeight: 3,
        strokeColor: '#ff9e7e',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
        fillColor: '#ff9e7e',
        fillOpacity: 0.15
    })

    circles.value.circle2100 = new window.kakao.maps.Circle({
        center: center,
        radius: 0,
        strokeWeight: 3,
        strokeColor: '#ff6b9d',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
        fillColor: '#ff6b9d',
        fillOpacity: 0.15
    })

    console.log('✅ Circle 초기화 완료')
}

function hideCircles() {
    if (circles.value.circle700) circles.value.circle700.setMap(null)
    if (circles.value.circle1500) circles.value.circle1500.setMap(null)
    if (circles.value.circle2100) circles.value.circle2100.setMap(null)
}

function showCirclesByZoneLevel(level) {
    if (!map) {
        console.error('지도가 초기화되지 않았습니다.')
        return
    }

    const currentCenter = map.getCenter()
    const currentLevel = map.getLevel()

    hideCircles()

    if (level >= 1 && circles.value.circle700) {
        circles.value.circle700.setMap(map)
    }

    if (level >= 2 && circles.value.circle1500) {
        circles.value.circle1500.setMap(map)
    }

    if (level >= 3 && circles.value.circle2100) {
        circles.value.circle2100.setMap(map)
    }

    updateMapForTime(selectedMinutes.value)

    if (!selectedLocation.value) {
        map.setCenter(currentCenter)
        map.setLevel(currentLevel)
    }
}

function updateMapForTime(minutes) {
    if (!map) {
        console.log('⚠️ updateMapForTime: 지도가 초기화되지 않음')
        return
    }

    if (!circles.value.circle700 || !circles.value.circle1500 || !circles.value.circle2100) {
        console.log('⚠️ Circle이 초기화되지 않음')
        return
    }

    try {
        if (minutes <= 30) {
            const radius = (minutes / 30) * 500
            circles.value.circle700.setRadius(radius)
            circles.value.circle1500.setRadius(0)
            circles.value.circle2100.setRadius(0)
        } else if (minutes <= 60) {
            circles.value.circle700.setRadius(500)
            const radius = 500 + ((minutes - 30) / 30) * (1000 - 500)
            circles.value.circle1500.setRadius(radius)
            circles.value.circle2100.setRadius(0)
        } else if (minutes <= 90) {
            circles.value.circle700.setRadius(500)
            circles.value.circle1500.setRadius(1000)
            const radius = 1000 + ((minutes - 60) / 30) * (1500 - 1000)
            circles.value.circle2100.setRadius(radius)
        } else {
            circles.value.circle700.setRadius(500)
            circles.value.circle1500.setRadius(1000)
            circles.value.circle2100.setRadius(1500)
        }

        console.log(`Circle 반경 업데이트 완료 (${minutes}분)`)
    } catch (error) {
        console.error('Circle 업데이트 중 오류:', error)
    }
}

function setCenter(force = false) {
    if (!map) {
        console.error('지도가 아직 초기화되지 않았습니다.')
        return
    }

    if (!force && selectedLocation.value) {
        console.log('선택된 위치가 있어 중심 이동을 건너뜁니다.')
        return
    }

    const moveLatLon = new window.kakao.maps.LatLng(
        missingLocation.value.lat,
        missingLocation.value.lon
    )

    map.setCenter(moveLatLon)

    if (centerMarker) {
        centerMarker.setPosition(moveLatLon)
    } else {
        centerMarker = new window.kakao.maps.Marker({
            position: moveLatLon,
            map: map,
            image: createCenterMarkerImage()
        })
    }

    console.log('✅ 지도 중심 설정 완료')
}

// ========================================================================================
// Tmap 경로 관련 함수
// ========================================================================================
const zone1Routes = ref([])
const zone2Routes = ref([])
const zone3Routes = ref([])

async function requestAllRoutes() {
    const s = missingLocation.value

    if (!s || !s.lat || !s.lon) {
        console.error('❌ 출발지 위치 정보가 없습니다.')
        return
    }

    const allZones = [
        { level: 1, data: zone_level_1.value, storage: zone1Routes },
        { level: 2, data: zone_level_2.value, storage: zone2Routes },
        { level: 3, data: zone_level_3.value, storage: zone3Routes }
    ]

    for (const zone of allZones) {
        if (!zone.data || zone.data.length === 0) {
            console.log(`⏭️  Zone ${zone.level} 데이터가 없습니다.`)
            continue
        }

        zone.storage.value = []

        for (let i = 0; i < zone.data.length; i++) {
            const d = zone.data[i]

            try {
                let waypointsStr = ''
                if (d.waypoints && Array.isArray(d.waypoints) && d.waypoints.length > 0) {
                    const waypointsCoords = d.waypoints.map(wp => {
                        if (!wp.lon || !wp.lat) {
                            console.warn(`⚠️  유효하지 않은 waypoint:`, wp)
                            return null
                        }
                        return `${wp.lon},${wp.lat}`
                    }).filter(coord => coord !== null)

                    if (waypointsCoords.length > 0) {
                        waypointsStr = waypointsCoords.join('_')
                    }
                } else {
                    console.log(`ℹ️  경유지 없음`)
                }

                let body = {
                    startName: 'start',
                    startX: Number(s.lon),
                    startY: Number(s.lat),
                    endName: `${d.address1 || 'end'}`,
                    endX: Number(d.lon),
                    endY: Number(d.lat),
                    reqCoordType: 'WGS84GEO',
                    resCoordType: 'WGS84GEO',
                    searchOption: '0',
                }

                if (waypointsStr && waypointsStr.length > 0) {
                    body.passList = waypointsStr
                }

                const resp = await fetch(
                    `https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json`,
                    {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'appKey': TMAP_API_KEY
                        },
                        body: JSON.stringify(body),
                    }
                )

                if (!resp.ok) {
                    console.error(`❌ Zone ${zone.level}-${i + 1} 경로 요청 실패: ${resp.status}`)
                    zone.storage.value.push(null)
                    continue
                }

                const data = await resp.json()

                if (data && data.features && Array.isArray(data.features) && data.features.length > 0) {
                    zone.storage.value.push(data.features)
                    console.log(`✅ Zone ${zone.level}-${i + 1} 경로 저장 (${data.features.length}개 feature)`)
                }

                await delay(200)

            } catch (e) {
                console.error(`❌ Zone ${zone.level}-${i + 1} 경로 요청 에러:`, e)
                zone.storage.value.push(null)
            }
        }

        console.log(`✅ Zone ${zone.level} 경로 요청 완료 (저장된 경로: ${zone.storage.value.filter(r => r !== null).length}개 / 전체: ${zone.storage.value.length}개)`)
    }

    console.log('🚶 모든 경로 요청 완료')
}

function clearPolylines() {
    for (let polyline of polylines) {
        if (polyline && polyline.setMap) {
            polyline.setMap(null)
        }
    }
    polylines.length = 0
}

function clearAllRoutes() {
    clearPolylines()
}

function drawRoute(index, zoneLevel = 1) {
    if (!map) {
        console.error('❌ 지도가 초기화되지 않았습니다.')
        return
    }

    let routeStorage
    if (zoneLevel === 1) {
        routeStorage = zone1Routes.value
    } else if (zoneLevel === 2) {
        routeStorage = zone2Routes.value
    } else if (zoneLevel === 3) {
        routeStorage = zone3Routes.value
    } else {
        console.error('❌ 유효하지 않은 zone level:', zoneLevel)
        return
    }

    if (index < 0 || index >= routeStorage.length) {
        console.error('❌ 유효하지 않은 인덱스:', index)
        return
    }

    const routeFeatures = routeStorage[index]

    if (!routeFeatures || routeFeatures.length === 0) {
        console.error(`❌ Zone ${zoneLevel}-${index}의 경로 데이터가 없습니다.`)
        return
    }

    console.log(`🗺️ Zone ${zoneLevel}-${index} 경로 그리기 시작... (${routeFeatures.length}개 feature)`)

    clearPolylines()

    let totalPolylines = 0

    routeFeatures.forEach((feature, featureIndex) => {
        try {
            if (feature.geometry && feature.geometry.type === 'LineString') {
                const coordinates = feature.geometry.coordinates

                if (!coordinates || coordinates.length === 0) {
                    console.warn(`⚠️  Feature ${featureIndex}: coordinates가 없음`)
                    return
                }

                const linePath = coordinates.map(([lng, lat]) => {
                    return new window.kakao.maps.LatLng(lat, lng)
                })

                const polyline = new window.kakao.maps.Polyline({
                    map: map,
                    path: linePath,
                    strokeColor: '#2563EB',
                    strokeWeight: 5,
                    strokeOpacity: 0.8,
                    strokeStyle: 'solid'
                })

                polylines.push(polyline)
                totalPolylines++

                if (featureIndex === 0 && linePath.length > 0) {
                    const midIndex = Math.floor(linePath.length / 2)
                    map.panTo(linePath[midIndex])
                    console.log(`📍 지도 중심 이동 (좌표 개수: ${linePath.length})`)
                }
            } else {
                console.warn(`⚠️  Feature ${featureIndex}: geometry type이 LineString이 아님 (${feature.geometry?.type})`)
            }
        } catch (e) {
            console.error(`❌ Feature ${featureIndex} 그리기 실패:`, e)
        }
    })

    console.log(`✅ Zone ${zoneLevel}-${index} 경로 그리기 완료 (총 ${totalPolylines}개 폴리라인)`)
}

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}

// ========================================================================================
// Watch
// ========================================================================================
watch(selectedMinutes, (newMinutes) => {
    let newLevel = 1
    if (newMinutes <= 30) newLevel = 1
    else if (newMinutes <= 60) newLevel = 2
    else newLevel = 3

    showAllLocations.value = false
    selectedLocation.value = null
    clearAllRoutes()
    displayZoneLevel.value = newLevel

    if (!map) {
        console.warn('지도가 초기화되지 않았습니다.')
        return
    }

    try {
        updateMapForTime(newMinutes)
    } catch (error) {
        console.error('지도 업데이트 실패:', error)
    }
})

watch(displayZoneLevel, (newLevel, oldLevel) => {
    console.log(`🗺️ Zone Level 변경: ${oldLevel} → ${newLevel}`)

    showAllLocations.value = false
    clearAllRoutes()
    selectedLocation.value = null

    if (!map) {
        console.warn('지도가 초기화되지 않았습니다.')
        return
    }

    try {
        makeMarker()
        showCirclesByZoneLevel(newLevel)
    } catch (error) {
        console.error('Zone Level 변경 실패:', error)
    }
})

watch(showAllLocations, (newValue) => {
    console.log(`📍 모든 위치 보기: ${newValue}`)
    selectedLocation.value = null

    if (!map) {
        console.warn('지도가 초기화되지 않았습니다.')
        return
    }

    try {
        makeMarker()
    } catch (error) {
        console.error('마커 업데이트 실패:', error)
    }
})

watch(selectedType, (newType) => {
    console.log(`📍 탭 전환: ${newType}`)

    if (newType === 'map') {
        clearSimulationMarkers()
        clearSimulationPaths()
        stopAnimation()
        showMarkers()
        showCirclesByZoneLevel(displayZoneLevel.value)
    } else if (newType === 'simulation') {
        hideMarkers()
        hideCircles()
        clearAllRoutes()
    }
})

// ========================================================================================
// Lifecycle
// ========================================================================================
onUnmounted(() => {
    stopAnimation()
    clearSimulationMarkers()
    clearSimulationPaths()
})
</script>

<style scoped>
/* 기존 CSS와 동일 + 시뮬레이션 컨트롤 추가 */
.page-container {
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: 100%;
    height: 100vh;
    margin: 0;
    margin-top: -18px;
    background: linear-gradient(180deg, #f8f9fd 0%, #ffffff 100%);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    overflow-y: auto;
    scrollbar-width: none;
}

/* ⭐ 시뮬레이션 컨트롤 컨테이너 */
.simulation-control-container {
    position: relative;
    width: 330px;
    padding: 20px 16px;
    left: 10px;
    background: linear-gradient(135deg, #ffffff 0%, #f8f9fd 100%);
    border-bottom: 1px solid #e5e5e5;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    display: flex;
    flex-direction: column;
    gap: 16px;
}

/* 시나리오 선택 */
.scenario-selector label {
    display: block;
    font-weight: 600;
    margin-bottom: 8px;
    color: #333;
    font-size: 13px;
}

.scenario-buttons {
    display: flex;
    justify-content: space-between;
    gap: 8px;
}

.scenario-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 10px 16px;
    background: #ffffff;
    border-radius: 20px;
    cursor: pointer;
    border: 1.5px solid #e0e0e0;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
    font-size: 13px;
    font-weight: 600;
    color: #666;
    transition: all 0.3s ease;
}

.scenario-btn i {
    font-size: 14px;
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

.scenario-btn:not(.active):not(:disabled):hover {
    border-color: #667eea;
    transform: translateY(-1px);
}

/* 재생 컨트롤 */
.playback-controls {
    display: flex;
    justify-content: space-between;
    gap: 8px;
}

.btn-play,
.btn-reset {
    padding: 10px 16px;
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
    font-size: 13px;
    flex: 1;
}

.btn-play:hover,
.btn-reset:hover {
    background: #5568d3;
    transform: translateY(-2px);
}

.btn-play:disabled,
.btn-reset:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.speed-selector {
    padding: 10px 12px;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    background: white;
    cursor: pointer;
    font-weight: 600;
    font-size: 12px;
    flex: 1;
}

.speed-selector:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

/* 시뮬레이션 타임라인 */
.simulation-timeline-section {
    background: white;
    padding: 12px;
    border-radius: 8px;
}

.simulation-timeline-info {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;
    font-weight: 600;
    color: #333;
    font-size: 14px;
}

.simulation-timeline-slider {
    widthwidth: 100%;
    height: 6px;
    border-radius: 3px;
    background: #e0e0e0;
    outline: none;
    cursor: pointer;
    -webkit-appearance: none;
}

.simulation-timeline-slider::-webkit-slider-thumb {
    -webkit-appearance: none;
    appearance: none;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #667eea;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
}

.simulation-timeline-slider::-moz-range-thumb {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #667eea;
    cursor: pointer;
    border: none;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
}

.simulation-timeline-slider:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

/* 시뮬레이션 에이전트 마커 스타일 */
.custom-agent-marker {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    font-size: 14px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    cursor: pointer;
    transition: all 0.3s ease;
    border: 3px solid white;
}

.custom-agent-marker:hover {
    transform: scale(1.15);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
}

.custom-agent-marker.selected {
    width: 40px;
    height: 40px;
    font-size: 18px;
    border-width: 4px;
    z-index: 1000;
}

.custom-agent-marker span {
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

/* 기존 CSS 스타일들 */
.loading-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.7);
    backdrop-filter: blur(8px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
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

.loading-content {
    text-align: center;
    padding: 40px;
    background: white;
    border-radius: 24px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    max-width: 400px;
    width: 90%;
}

.loading-animation {
    position: relative;
    width: 120px;
    height: 120px;
    margin: 0 auto 30px;
}

.map-marker-pulse {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 60px;
    height: 60px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    animation: pulse 2s ease-in-out infinite;
    z-index: 2;
}

.map-marker-pulse i {
    font-size: 32px;
    color: white;
}

@keyframes pulse {

    0%,
    100% {
        transform: translate(-50%, -50%) scale(1);
        box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7);
    }

    50% {
        transform: translate(-50%, -50%) scale(1.1);
        box-shadow: 0 0 0 15px rgba(102, 126, 234, 0);
    }
}

.search-circles {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 100%;
    height: 100%;
}

.circle {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    border: 3px solid;
    border-radius: 50%;
    animation: ripple 2s ease-out infinite;
}

.circle-1 {
    width: 80px;
    height: 80px;
    border-color: rgba(102, 126, 234, 0.6);
    animation-delay: 0s;
}

.circle-2 {
    width: 100px;
    height: 100px;
    border-color: rgba(102, 126, 234, 0.4);
    animation-delay: 0.5s;
}

.circle-3 {
    width: 120px;
    height: 120px;
    border-color: rgba(102, 126, 234, 0.2);
    animation-delay: 1s;
}

@keyframes ripple {
    0% {
        transform: translate(-50%, -50%) scale(0.8);
        opacity: 1;
    }

    100% {
        transform: translate(-50%, -50%) scale(1.5);
        opacity: 0;
    }
}

.loading-title {
    font-size: 24px;
    font-weight: 800;
    color: #333;
    margin: 0 0 12px 0;
    letter-spacing: -0.5px;
}

.loading-message {
    font-size: 16px;
    font-weight: 500;
    color: #666;
    margin: 0 0 8px 0;
    line-height: 1.5;
}

.loading-submessage {
    font-size: 14px;
    font-weight: 400;
    color: #999;
    margin: 0 0 30px 0;
}

.loading-progress {
    width: 100%;
    height: 6px;
    background: #e8ebf2;
    border-radius: 3px;
    overflow: hidden;
}

.progress-bar {
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    animation: progressAnimation 2s ease-in-out infinite;
}

@keyframes progressAnimation {
    0% {
        transform: translateX(-100%);
    }

    50% {
        transform: translateX(0);
    }

    100% {
        transform: translateX(100%);
    }
}

.map-area {
    position: relative;
    left: 0;
    top: 0;
    width: 100%;
    height: 350px;
    flex-shrink: 0;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.toggle-button-wrapper {
    display: flex;
    flex-direction: column;
    width: 100%;
    background: #ffffff;
    border-bottom: 2px solid #e0e0e0;
    flex-shrink: 0;
}

.toggle-button {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 48px;
    padding: 0 20px;
    background: #ffffff;
    border: none;
    border-bottom: 3px solid transparent;
    cursor: pointer;
    gap: 8px;
    position: relative;
    transition: all 0.2s ease;
}

.toggle-button i {
    font-size: 20px;
    color: #999999;
    transition: color 0.2s ease;
}

.button-text {
    font-size: 15px;
    font-weight: 500;
    color: #666666;
    transition: color 0.2s ease;
}

.toggle-button.active {
    border-bottom: 3px solid #3182f6;
}

.toggle-button.active i {
    color: #3182f6;
}

.toggle-button.active .button-text {
    color: #3182f6;
    font-weight: 600;
}

.toggle-button:not(.active):hover {
    background: #f8f9fa;
}

.toggle-button:not(.active):hover i,
.toggle-button:not(.active):hover .button-text {
    color: #333333;
}

.content-section {
    background: linear-gradient(180deg, #ffffff 0%, #f8f9fd 100%);
    padding: 0;
    display: flex;
    flex-direction: column;
}

.more-btn {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 6px;
    height: 44px;
    padding: 0 24px;
    margin: 16px auto;
    border-radius: 22px;
    border: 1.5px solid #e0e0e0;
    background: #ffffff;
    font-size: 14px;
    font-weight: 600;
    color: #666666;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.more-btn i {
    font-size: 12px;
    transition: transform 0.3s ease;
}

.more-btn:hover {
    border-color: #5b7cef;
    color: #5b7cef;
    background: rgba(91, 124, 239, 0.05);
    box-shadow: 0 4px 12px rgba(91, 124, 239, 0.15);
    transform: translateY(-1px);
}

.more-btn:hover i {
    transform: translateY(2px);
}

.more-btn:active {
    transform: translateY(0);
    box-shadow: 0 2px 6px rgba(91, 124, 239, 0.2);
}

.timeline-container {
    position: relative;
    width: 330px;
    padding: 20px 16px;
    left: 10px;
    background: linear-gradient(135deg, #ffffff 0%, #f8f9fd 100%);
    border-bottom: 1px solid #e5e5e5;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.timeline-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
}

.timeline-header i {
    font-size: 20px;
    color: #667eea;
}

.timeline-title {
    flex: 1;
    font-size: 15px;
    font-weight: 700;
    color: #333;
}

.timeline-wrapper {
    position: relative;
    height: 60px;
    cursor: pointer;
    user-select: none;
}

.timeline-track {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    left: 0;
    right: 0;
    height: 12px;
    background: #f0f0f0;
    border-radius: 6px;
    overflow: hidden;
    display: flex;
}

.timeline-progress {
    position: absolute;
    top: 0;
    left: 0;
    height: 100%;
    background: linear-gradient(90deg, #667eea 0%, #667eea 100%);
    border-radius: 6px;
    transition: width 0.15s ease-out;
    pointer-events: none;
}

.timeline-markers {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 100%;
    pointer-events: none;
}

.timeline-marker {
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
}

.marker-dot,
.marker-dot-1,
.marker-dot-2,
.marker-dot-3 {
    position: relative;
    top: 19px;
    width: 12px;
    height: 12px;
    background: white;
    border: 3px solid #667eea;
    border-radius: 50%;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.marker-label {
    font-size: 11px;
    font-weight: 600;
    color: #666;
    white-space: nowrap;
    margin-top: 18px;
}

.timeline-handle {
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
    width: 38px;
    height: 38px;
    cursor: grab;
    z-index: 10;
}

.timeline-handle:active {
    cursor: grabbing;
}

.handle-icon {
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 20px;
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
    transition: all 0.3s ease;
}

.handle-tooltip {
    position: absolute;
    top: -40px;
    left: 50%;
    transform: translateX(-50%);
    padding: 6px 12px;
    background: rgba(0, 0, 0, 0.8);
    color: white;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    pointer-events: none;
}

.timeline-legend {
    display: flex;
    position: relative;
    margin-top: 16px;
    justify-content: space-around;
    flex-wrap: wrap;
    width: 330px;
    left: -15px;
}

.legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    background: #ffffff;
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 1.5px solid #e0e0e0;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
}

.legend-item:hover {
    border-color: #c0c0c0;
    box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);
    transform: translateY(-1px);
}

.legend-item.active {
    background: white;
    border-color: #667eea;
    box-shadow: 0 3px 12px rgba(102, 126, 234, 0.25);
    transform: translateY(-2px);
}

.legend-color {
    width: 14px;
    height: 14px;
    border-radius: 50%;
    flex-shrink: 0;
    box-shadow: 0 0 4px rgba(0, 0, 0, 0.15);
}

.legend-text {
    font-size: 12px;
    font-weight: 300;
    color: #666666;
    white-space: nowrap;
}

.legend-item.active .legend-text {
    color: #333333;
    font-weight: 700;
}

.skeleton-container {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px;
}

.skeleton-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: rgba(255, 255, 255, 0.7);
    border-radius: 16px;
    animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@keyframes skeleton-pulse {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.5;
    }
}

.skeleton-icon {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-shimmer 1.5s infinite;
}

.skeleton-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.skeleton-line {
    height: 16px;
    border-radius: 8px;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-shimmer 1.5s infinite;
}

.skeleton-line-long {
    width: 80%;
}

.skeleton-line-short {
    width: 60%;
}

@keyframes skeleton-shimmer {
    0% {
        background-position: 200% 0;
    }

    100% {
        background-position: -200% 0;
    }
}

.location-icon-modern {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    left: 5px;
    transition: all 0.3s ease;
}

.rank-number {
    font-size: 20px;
    font-weight: 800;
    color: white;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    z-index: 1;
}

.probability-badge-modern {
    position: relative;
    width: 60px;
    height: 44px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}

.probability-text {
    font-size: 12px;
    font-weight: 800;
    color: #888888;
    z-index: 1;
}

.stats-dashboard-modern {
    margin-bottom: 100px;
    padding: 24px;
    border-radius: 20px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.stats-title-modern {
    font-size: 18px;
    font-weight: 700;
    color: #333;
    margin: 0 0 20px 0;
    display: flex;
    align-items: center;
    gap: 10px;
}

.stats-title-modern i {
    font-size: 22px;
    color: #667eea;
}

.stats-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 5px;
}

.stat-card-modern {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 18px;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
    border-radius: 16px;
    border: 1px solid rgba(255, 255, 255, 0.5);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    transition: all 0.3s ease;
    width: 350px;
    position: relative;
    right: 20px;
    height: 150px;
}

.stat-card-modern-1 {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 18px;
    background: #fef5f5;
    backdrop-filter: blur(10px);
    border-radius: 16px;
    border: 1px solid rgba(255, 255, 255, 0.5);
    border-left: 4px solid #ff6b6b;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    transition: all 0.3s ease;
    width: 350px;
    position: relative;
    right: 20px;
    height: 100px;
}

.stat-icon-modern {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 14px;
    background: var(--stat-color);
    color: white;
    font-size: 24px;
    flex-shrink: 0;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content-modern {
    flex: 1;
    margin-bottom: 30px;
}

.stat-label-modern {
    font-size: 12px;
    font-weight: 600;
    color: #585858;
    margin: 0 0 6px 0;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.stat-value-modern {
    font-size: 28px;
    font-weight: 800;
    color: #333;
    margin: 0;
    line-height: 1;
    display: flex;
    align-items: baseline;
    gap: 4px;
}

.stat-unit {
    font-size: 16px;
    font-weight: 600;
    color: #666;
}

.stat-sublabel-modern-1 {
    font-size: 14px;
    color: #3f3f3f;
    margin: 4px 0 0 0;
}

.prediction-card {
    display: flex;
    flex-direction: column;
    background: #ffffff;
    border: 2px solid #e8ebf2;
    border-radius: 16px;
    padding: 16px;
    margin-bottom: 12px;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.prediction-card.selected {
    border: 2px solid #667eea;
    background: linear-gradient(135deg, #667eea0a 0%);
}

.card-content {
    display: flex;
    flex-direction: column;
    gap: 12px;
    flex: 1;
    padding: 0 16px 0 0;
    width: 350px;
    min-width: 0;
}

.location-header {
    display: flex;
    align-items: flex-start;
    gap: 12px;
}

.location-text-wrapper {
    position: relative;
    left: 10px;
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.location-name {
    font-size: 14px;
    font-weight: 700;
    color: #191f28;
    margin: 0;
    line-height: 1.4;
    word-break: keep-all;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.location-detail {
    font-size: 13px;
    font-weight: 500;
    color: #666666;
    margin: 0;
    line-height: 1.3;
    word-break: keep-all;
}

.location-distance {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-left: 56px;
    gap: 8px;
    font-size: 14px;
    color: #666666;
    margin: 0;
}

.location-distance>div {
    display: flex;
    align-items: center;
    gap: 4px;
}

.location-distance i {
    font-size: 14px;
    color: #5b7cef;
}

.glass-card {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.5);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.warning-icon-wrapper {
    flex-shrink: 0;
    position: relative;
    bottom: 6px;
    color: #ff6b6b;
    font-size: 22px;
}

.warning-content {
    flex: 1;
}

.warning-title {
    margin: 0 0 4px 0;
    font-size: 15px;
    font-weight: 600;
    color: #d63031;
}

.warning-message {
    margin: 0 0 2px 0;
    font-size: 13px;
    color: #e17055;
}

.warning-subtitle {
    font-size: 12px;
    color: #e17055;
}

.prediction-list,
.simulation-list {
    padding: 16px;
}
</style>
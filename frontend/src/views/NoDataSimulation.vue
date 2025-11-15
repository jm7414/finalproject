<template>
    <div class="simulation-page">
        <!-- ⭐ 로딩 오버레이 -->
        <div v-if="isLoading" class="loading-overlay">
            <div class="loading-state">
                <div class="loading-spinner"></div>
                <p>시뮬레이션 데이터 로딩 중...</p>
            </div>
        </div>

        <!-- ⭐ 에러 상태 -->
        <div v-else-if="error" class="error-overlay">
            <div class="error-state">
                <i class="bi bi-exclamation-triangle"></i>
                <p>{{ error }}</p>
                <button @click="loadSimulationData" class="retry-button">다시 시도</button>
            </div>
        </div>

        <!-- 메인 콘텐츠 -->
        <div v-else class="page-content">
            <div class="header-content">
                <i class="bi bi-diagram-3"></i>
                <h2>에이전트 시뮬레이션</h2>
            </div>
            <!-- 지도 영역 -->
            <div class="map-section">
                <div v-if="!mapLoaded" class="map-loading">
                    <div class="loading-spinner"></div>
                    <p>지도 로딩 중...</p>
                </div>
                <div ref="mapContainer" class="simulation-map"></div>
            </div>

            <!-- 컨트롤 패널 -->
            <div class="control-panel">
                <!-- 시나리오 선택 -->
                <div class="scenario-selector">
                    <label>시뮬레이션 선택</label>
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
                        <option :value="0.5">속도 0.5배</option>
                        <option :value="1">속도 1배</option>
                        <option :value="2">속도 2배</option>
                        <option :value="5">속도 5배</option>
                        <option :value="10">속도 10배</option>
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
                        <div class="agent-card">
                            <div class="agent-header">
                                <div class="rank-badge" :style="{ backgroundColor: getColorByRank(agent.rank) }">
                                    {{ agent.rank }}
                                </div>
                                <div class="title-section">
                                    <h3>{{ agent.address }}</h3>
                                </div>
                            </div>

                            <div class="agent-info">
                                <div class="location-info">
                                    <svg class="location-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                                        <circle cx="12" cy="10" r="3"></circle>
                                        <path
                                            d="M12 1C6.48 1 2 5.48 2 11c0 3.83 2.67 7.12 6.35 7.87C9.73 20.29 10.8 21.5 12 21.5s2.27-1.21 3.65-1.63C19.33 18.12 22 14.83 22 11c0-5.52-4.48-10-10-10z">
                                        </path>
                                    </svg>
                                    <span class="distance-text">
                                        실종지로부터
                                        <strong>{{ calculateDistance(missingLocation.lat, missingLocation.lon,
                                            agent.final_lat, agent.final_lon) }}m</strong>
                                    </span>
                                    <div class="distance-text">
                                        <span>확률 :
                                            <strong>{{ agent.probability.toFixed(1) }}%</strong>
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="stat-card-modern">
                    <div class="stat-icon-modern" style="--stat-color: #667eea;">
                        <i class="bi bi-geo-alt"></i>
                    </div>
                    <div class="stat-content-modern">
                        <p class="stat-label-modern">분석 방법</p>
                        <p class="stat-sublabel-modern-1">
                            치매환자 행동 특성 기반으로 진행한
                        </p>
                        <p class="stat-sublabel-modern-1">100번의 시뮬레이션의 이동경로 및</p>
                        <p class="stat-sublabel-modern-1">예측 위치를 나타냅니다.</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

// ========================================================================================
// 데이터 초기화
// ========================================================================================
const userNo = ref(0)
const missingLocation = ref({ lat: 0, lon: 0 })

// 카카오 지도 관련
const KAKAO_JS_KEY = '7e0332c38832a4584b3335bed6ae30d8'
const mapContainer = ref(null)
let map = null
const markers = {}
const paths = {}
const mapLoaded = ref(false)

// 상태 관리
const isLoading = ref(true)
const error = ref(null)
const currentScenario = ref('30분')
const availableScenarios = ref([])
const animationData = ref(null)
const currentStep = ref(0)
const isPlaying = ref(false)
const playbackSpeed = ref(1)
const selectedAgent = ref(null)
let animationTimer = null
const allScenariosData = ref(null)

// VWorld API
const VWORLD_API_KEY = '6A0CFFEF-45CF-3426-882D-44A63B5A5289'

// 리사이즈 타이머
let resizeTimer = null

// ========================================================================================
// Computed
// ========================================================================================
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
        currentScenario.value === '60분' ? 3600 : 5400
})

const agentsList = computed(() => {
    if (!animationData.value || !animationData.value.data.frames[0]) return []

    return animationData.value.data.frames[0].agents.map(agent => ({
        rank: agent.rank,
        agent_id: agent.agent_id,
        probability: agent.final_position.probability,
        agent_count: agent.final_position.agent_count_at_position || 0,
        distance: 0,
        final_lat: agent.final_position.latitude,
        final_lon: agent.final_position.longitude,
        address: agent.address || '주소 없음'
    }))
})

// ========================================================================================
// 유틸리티 함수
// ========================================================================================
function calculateDistance(lat1, lon1, lat2, lon2) {
    const toRadian = angle => (Math.PI / 180) * angle
    const R = 6371000

    const dLat = toRadian(lat2 - lat1)
    const dLon = toRadian(lon2 - lon1)

    const lat1Rad = toRadian(lat1)
    const lat2Rad = toRadian(lat2)

    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1Rad) * Math.cos(lat2Rad) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    const distance = R * c

    return Math.round(distance)
}

const getColorByRank = (rank) => {
    if (rank <= 3) return '#FF0000'
    if (rank <= 6) return '#FF6B00'
    return '#FFA500'
}

const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
}

// ========================================================================================
// 카카오맵
// ========================================================================================
const loadKakaoMapScript = () => {
    return new Promise((resolve, reject) => {
        if (window.kakao && window.kakao.maps) {
            resolve()
            return
        }

        const existingScript = document.querySelector(`script[src*="dapi.kakao.com"]`)
        if (existingScript) {
            existingScript.addEventListener('load', () => {
                window.kakao.maps.load(() => resolve())
            })
            return
        }

        const script = document.createElement('script')
        script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false`
        script.async = true

        script.onload = () => {
            window.kakao.maps.load(() => resolve())
        }

        script.onerror = (error) => reject(error)

        document.head.appendChild(script)
    })
}

const initMap = async (lat, lon) => {
    console.log('🗺️ initMap 호출:', { lat, lon })

    if (!window.kakao || !window.kakao.maps) {
        await loadKakaoMapScript()
    }

    await nextTick()

    if (!mapContainer.value) {
        console.error('❌ mapContainer 없음')
        return
    }

    try {
        const options = {
            center: new window.kakao.maps.LatLng(lat, lon),
            level: 5
        }

        console.log('✅ 지도 옵션:', options)
        map = new window.kakao.maps.Map(mapContainer.value, options)
        console.log('✅ 지도 생성 완료')

        // ⭐ 지도 크기 재조정 및 로딩 상태 업데이트
        setTimeout(() => {
            if (map) {
                map.relayout()
                mapLoaded.value = true
                console.log('✅ 지도 relayout 완료')
            }
        }, 100)

        // 실종 위치 마커
        new window.kakao.maps.Marker({
            position: new window.kakao.maps.LatLng(lat, lon),
            map: map,
            image: new window.kakao.maps.MarkerImage(
                'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png',
                new window.kakao.maps.Size(32, 32)
            )
        })
        console.log('✅ 실종 위치 마커 생성 완료')
    } catch (error) {
        console.error('❌ 지도 생성 실패:', error)
        mapLoaded.value = true
    }
}

// ========================================================================================
// VWorld 주소 조회
// ========================================================================================
async function fetchVWorldData(location) {
    try {
        const params = new URLSearchParams({
            service: 'address',
            version: '2.0',
            request: 'getaddress',
            crs: 'epsg:4326',
            format: 'json',
            point: `${location.lon},${location.lat}`,
            type: 'both',
            key: VWORLD_API_KEY
        })

        const apiUrl = `https://api.vworld.kr/req/address?${params.toString()}`
        const proxyUrl = `https://www.vworld.kr/proxy.do?url=${encodeURIComponent(apiUrl)}`

        const response = await fetch(proxyUrl, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        })

        if (!response.ok) {
            return { address: '주소 조회 실패' }
        }

        const data = await response.json()

        if (data.response?.result?.length > 0) {
            return { address: data.response.result[0].text }
        }

        return { address: '주소 조회 실패' }
    } catch (error) {
        return { address: '주소 조회 실패' }
    }
}

async function fetchAgentAddress(agent) {
    try {
        const addressData = await fetchVWorldData({
            lat: agent.final_position.latitude,
            lon: agent.final_position.longitude
        })

        agent.address = addressData.address
    } catch (error) {
        agent.address = '주소 조회 실패'
    }
}

// ========================================================================================
// 시뮬레이션 데이터 로드
// ========================================================================================
const loadSimulationData = async () => {
    isLoading.value = true
    error.value = null

    try {
        const response = await axios.post(
            'http://localhost:8000/api/agent-simulation/run-all',
            {
                user_no: userNo.value,
                latitude: missingLocation.value.lat,
                longitude: missingLocation.value.lon
            },
            {
                withCredentials: true,
                headers: { 'Content-Type': 'application/json' }
            }
        )

        const { scenarios } = response.data

        if (scenarios) {
            // ⭐ 개선: 주소 조회를 병렬로 처리
            const addressPromises = []

            for (const [scenarioKey, scenarioData] of Object.entries(scenarios)) {
                if (!scenarioData.frames || scenarioData.frames.length === 0) continue

                const firstFrame = scenarioData.frames[0]
                if (!firstFrame.agents || firstFrame.agents.length === 0) continue

                for (let i = 0; i < firstFrame.agents.length; i++) {
                    const agent = firstFrame.agents[i]
                    addressPromises.push(
                        fetchAgentAddress(agent).catch(e => {
                            agent.address = '조회 실패'
                        })
                    )
                }
            }

            // ⭐ 개선: 모든 주소 조회를 동시에 처리 (최대 5개씩)
            const chunkSize = 5
            for (let i = 0; i < addressPromises.length; i += chunkSize) {
                await Promise.all(addressPromises.slice(i, i + chunkSize))
                // 짧은 지연으로 API 제한 회피
                if (i + chunkSize < addressPromises.length) {
                    await new Promise(resolve => setTimeout(resolve, 100))
                }
            }
        }

        allScenariosData.value = scenarios

        availableScenarios.value = [
            { scenario: '30분', available: !!scenarios['30분'] },
            { scenario: '60분', available: !!scenarios['60분'] },
            { scenario: '90분', available: !!scenarios['90분'] }
        ]

        animationData.value = { data: scenarios['30분'] }
        currentStep.value = 0

        await nextTick()

        setTimeout(() => {
            initializeAgents()
        }, 6000)

    } catch (err) {
        error.value = err.response?.data?.detail || '데이터를 불러올 수 없습니다.'
    } finally {
        isLoading.value = false
    }
}

// ========================================================================================
// 시나리오 변경
// ========================================================================================
const changeScenario = async (scenario) => {
    if (scenario === currentScenario.value) return

    if (!allScenariosData.value || !allScenariosData.value[scenario]) return

    currentScenario.value = scenario
    isPlaying.value = false

    clearMapElements()

    animationData.value = { data: allScenariosData.value[scenario] }
    currentStep.value = 0

    await nextTick()

    // ⭐ 추가: 지도 재조정
    if (map) {
        map.relayout()
    }

    setTimeout(() => {
        initializeAgents()
    }, 300)
}

// ========================================================================================
// 에이전트 초기화
// ========================================================================================
const initializeAgents = () => {
    if (!animationData.value || !map) return
    if (!animationData.value.data || !animationData.value.data.frames || animationData.value.data.frames.length === 0) return

    const firstFrame = animationData.value.data.frames[0]
    if (!firstFrame.agents || firstFrame.agents.length === 0) return

    firstFrame.agents.forEach(agent => {
        createAgentMarker(agent)
        createAgentPath(agent)
    })

    updateFrame()
}

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
    } catch (error) {
        console.error(`❌ 마커 생성 실패:`, error)
    }
}

const createAgentPath = (agent) => {
    if (!map) return

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

        paths[agent.rank] = { line: polyline, points: [] }
    } catch (error) {
        console.error(`❌ 경로 생성 실패:`, error)
    }
}

// ========================================================================================
// 프레임 업데이트
// ========================================================================================
const updateFrame = () => {
    if (!animationData.value || !map) return
    if (!animationData.value.data || !animationData.value.data.frames) return

    const frames = animationData.value.data.frames
    if (currentStep.value >= frames.length) return

    const frame = frames[currentStep.value]
    if (!frame.agents) return

    frame.agents.forEach(agent => {
        try {
            if (markers[agent.rank]) {
                const newPosition = new window.kakao.maps.LatLng(agent.latitude, agent.longitude)
                markers[agent.rank].position = newPosition
                markers[agent.rank].overlay.setPosition(newPosition)
            }

            if (paths[agent.rank]) {
                const point = new window.kakao.maps.LatLng(agent.latitude, agent.longitude)
                paths[agent.rank].points.push(point)
                paths[agent.rank].line.setPath(paths[agent.rank].points)

                if (paths[agent.rank].points.length >= 2 && !paths[agent.rank].line.getMap()) {
                    paths[agent.rank].line.setMap(map)
                }
            }
        } catch (error) {
            console.error(`프레임 업데이트 실패:`, error)
        }
    })
}

// ========================================================================================
// 애니메이션 제어
// ========================================================================================
const togglePlay = () => {
    isPlaying.value = !isPlaying.value

    if (isPlaying.value) {
        playAnimation()
    } else {
        stopAnimation()
    }
}

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

const stopAnimation = () => {
    if (animationTimer) {
        clearTimeout(animationTimer)
        animationTimer = null
    }
}

const resetAnimation = () => {
    isPlaying.value = false
    stopAnimation()
    currentStep.value = 0

    Object.values(paths).forEach(path => {
        path.line.setMap(null)
        path.points = []
        path.line.setPath([])
    })

    updateFrame()
}

const selectAgent = (rank) => {
    selectedAgent.value = rank === selectedAgent.value ? null : rank

    Object.entries(markers).forEach(([r, marker]) => {
        const element = marker.overlay.getContent()
        if (parseInt(r) === rank && selectedAgent.value !== null) {
            element.classList.add('selected')
        } else {
            element.classList.remove('selected')
        }
    })
}

// ========================================================================================
// 지도 리사이즈 핸들러
// ========================================================================================
const handleResize = () => {
    if (resizeTimer) clearTimeout(resizeTimer)

    resizeTimer = setTimeout(() => {
        if (map) {
            map.relayout()
            console.log('✅ 윈도우 리사이즈 - 지도 relayout 완료')
        }
    }, 200)
}

// ========================================================================================
// 초기화 및 정리
// ========================================================================================
const clearMapElements = () => {
    Object.values(markers).forEach(marker => marker.overlay.setMap(null))
    Object.values(paths).forEach(path => path.line.setMap(null))

    Object.keys(markers).forEach(key => delete markers[key])
    Object.keys(paths).forEach(key => delete paths[key])
}

// ========================================================================================
// Lifecycle
// ========================================================================================
onMounted(async () => {
    console.log('📍 SimulationPage mounted')

    const parsedUserNo = parseInt(route.query.userNo)
    const parsedLat = parseFloat(route.query.lat)
    const parsedLon = parseFloat(route.query.lon)

    if (isNaN(parsedUserNo) || isNaN(parsedLat) || isNaN(parsedLon)) {
        alert('잘못된 데이터입니다.')
        router.back()
        return
    }

    userNo.value = parsedUserNo
    missingLocation.value = { lat: parsedLat, lon: parsedLon }

    // ⭐ 개선: 카카오맵 스크립트 먼저 로드
    try {
        await loadKakaoMapScript()
        await nextTick()

        // 데이터 로딩 시작
        await loadSimulationData()

        // 데이터 로딩 완료 후 지도 초기화
        await initMap(parsedLat, parsedLon)

    } catch (error) {
        console.error('초기화 실패:', error)
        error.value = '초기화 중 오류가 발생했습니다.'
    }

    // ⭐ 리사이즈 이벤트 리스너 등록
    window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
    stopAnimation()
    clearMapElements()
    currentScenario.value = null

    // ⭐ 리사이즈 이벤트 리스너 제거
    window.removeEventListener('resize', handleResize)
    if (resizeTimer) clearTimeout(resizeTimer)
})
</script>

<style scoped>
/* ========================================================================================
   확률 말해주는 부분
   ======================================================================================== */


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
    position: relative;
    top:15px;
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

.stat-sublabel-modern-1 {
    font-size: 13px;
    color: #3f3f3f;
    margin: 4px 0 0 0;
}


/* ========================================================================================
   헤더부분
   ======================================================================================== */
.header-content {
    padding: 5px;
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

/* ========================================================================================
   페이지 레이아웃
   ======================================================================================== */
.simulation-page {
    width: 100%;
    max-width: 375px;
    margin-top: -15px;
    height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f8f9fa;
}

/* ========================================================================================
   로딩 & 에러
   ======================================================================================== */
.loading-overlay,
.error-overlay {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    bottom: 150px;
}

.loading-state,
.error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
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

/* ========================================================================================
   메인 콘텐츠
   ======================================================================================== */
.page-content {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;
    scrollbar-width: none;
    -ms-overflow-style: none;
}

.page-content::-webkit-scrollbar {
    display: none;
}

/* ========================================================================================
   지도
   ======================================================================================== */
.map-section {
    height: 250px;
    min-height: 250px;
    position: relative;
    overflow: hidden;
}

.map-loading {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #f8f9fa;
    z-index: 10;
}

.map-loading p {
    margin: 12px 0 0 0;
    color: #666;
    font-size: 14px;
    font-weight: 600;
}

.simulation-map {
    width: 100%;
    height: 100%;
    position: relative;
}

/* ========================================================================================
   컨트롤 패널
   ======================================================================================== */
.control-panel {
    background: white;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

.scenario-selector label {
    display: block;
    font-weight: 600;
    margin-bottom: 10px;
    color: #333;
    font-size: 14px;
}

.scenario-buttons {
    display: flex;
    gap: 8px;
}

.scenario-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 8px 23px;
    background: #ffffff;
    border-radius: 20px;
    cursor: pointer;
    border: 1.5px solid #e0e0e0;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
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

.playback-controls {
    display: flex;
    justify-content: space-between;
    gap: 6px;
}

.btn-play,
.btn-reset {
    padding: 8px;
    background: #667eea;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100px;
    gap: 6px;
    position: relative;
    left: -10px;
    margin-left: 10px;
    font-weight: 600;
    transition: all 0.3s;
    font-size: 14px;
}

.btn-play:hover,
.btn-reset:hover {
    background: #5568d3;
    transform: translateY(-2px);
}

.btn-play:hover,
.btn-reset:hover {
    background: #5568d3;
}

.speed-selector {
    padding: 10px;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    background: white;
    cursor: pointer;
    font-weight: 600;
    font-size: 13px;
    min-width: 80px;
}

/* 타임라인 */
.timeline-section {
    background: #f8f9fa;
    padding: 12px;
    border-radius: 8px;
}

.timeline-info {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    font-weight: 600;
    color: #333;
    font-size: 14px;
}

.timeline-slider {
    width: 100%;
    height: 6px;
    border-radius: 3px;
    background: #e0e0e0;
    outline: none;
    cursor: pointer;
}

/* ========================================================================================
   에이전트 리스트
   ======================================================================================== */
.agents-list-panel {
    background: white;
    padding: 16px;
}

.panel-header {
    margin-bottom: 12px;
}

.panel-header h3 {
    margin: 0 0 4px 0;
    font-size: 16px;
    font-weight: 700;
    color: #333;
}

.subtitle {
    font-size: 12px;
    color: #888;
}

.agents-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.agent-item {
    cursor: pointer;
    transition: all 0.3s;
}

.agent-item.selected .agent-card {
    border-color: #667eea;
    background: rgba(102, 126, 234, 0.05);
}

.agent-card {
    background: white;
    border: 2px solid #f0f0f0;
    border-radius: 12px;
    padding: 14px;
    transition: all 0.3s;
}

.agent-header {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 10px;
}

.rank-badge {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: 700;
    font-size: 16px;
    flex-shrink: 0;
}

.title-section {
    flex: 1;
    position: relative;
    top: 8px;
    display: flex;
    align-items: center;
    min-width: 0;
}

.title-section h3 {
    margin: 0;
    font-size: 13px;
    font-weight: 600;
    color: #222;
    word-break: break-word;
    line-height: 1.4;
}

.probability-badge {
    flex-shrink: 0;
}

.probability-badge span {
    display: block;
    font-size: 14px;
    font-weight: 700;
    color: #667eea;
}

.agent-info {
    padding-top: 10px;
    border-top: 1px solid #f5f5f5;
}

.location-info {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #666;
}

.location-icon {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    color: #667eea;
}

.distance-text strong {
    color: #222;
    font-weight: 600;
}

/* ========================================================================================
   커스텀 마커
   ======================================================================================== */
:deep(.custom-agent-marker) {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: 700;
    font-size: 12px;
    border: 3px solid white;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    transition: all 0.3s;
}

:deep(.custom-agent-marker.selected) {
    width: 36px;
    height: 36px;
    font-size: 16px;
    border-width: 4px;
    z-index: 1000;
}
</style>
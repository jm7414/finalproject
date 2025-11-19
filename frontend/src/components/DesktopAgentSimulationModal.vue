<template>
    <div class="agent-simulation-modal-overlay" v-if="isVisible" @click.self="closeModal">
        <div class="agent-simulation-modal">
            <!-- 헤더 (항상 표시) -->
            <div class="modal-header">
                <div class="header-content">
                    <i class="bi bi-diagram-3"></i>
                    <h2>에이전트 시뮬레이션</h2>
                </div>
                <button class="close-button" @click="closeModal">
                    <i class="bi bi-x-lg"></i>
                </button>
            </div>

            <!-- ⭐ 로딩 오버레이 (position: absolute) -->
            <div v-if="isLoading" class="loading-overlay-inner">
                <div class="loading-state">
                    <div class="loading-spinner"></div>
                    <p>시뮬레이션 데이터 로딩 중...</p>
                </div>
            </div>

            <!-- ⭐ 에러 오버레이 (position: absolute) -->
            <div v-if="error" class="error-overlay-inner">
                <div class="error-state">
                    <i class="bi bi-exclamation-triangle"></i>
                    <p>{{ error }}</p>
                    <button @click="loadSimulationData" class="retry-button">다시 시도</button>
                </div>
            </div>

            <!-- 메인 콘텐츠 (항상 렌더링, 로딩/에러 시에도 DOM에 존재) -->
            <div class="modal-content">
                <!-- 지도 영역 (항상 렌더링) -->
                <div class="map-section">
                    <div ref="mapContainer" class="simulation-map" style="background: #e5e5e5;"></div>
                </div>



                <!-- 에이전트 리스트 (로딩/에러 아닐 때만 표시) -->
                <div class="agents-list-panel" v-show="!isLoading && !error">
                    <!-- 컨트롤 패널 (로딩/에러 아닐 때만 표시) -->
                    <div class="control-panel" v-show="!isLoading && !error">
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
                                <option :value="0.5">속도 0.5배</option>
                                <option :value="1">속도 1배 </option>
                                <option :value="2">속도 2배 </option>
                                <option :value="5">속도 5배 </option>
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
                            <input type="range" v-model="currentStep" :min="0" :max="totalSteps - 1"
                                @input="updateFrame" class="timeline-slider" />
                        </div>
                    </div>
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
                                        <p class="description">{{ agent.description }}</p>
                                    </div>
                                </div>

                                <div class="agent-info">
                                    <div class="location-info">
                                        <svg class="location-icon" viewBox="0 0 24 24" fill="none"
                                            stroke="currentColor">
                                            <circle cx="12" cy="10" r="3"></circle>
                                            <path
                                                d="M12 1C6.48 1 2 5.48 2 11c0 3.83 2.67 7.12 6.35 7.87C9.73 20.29 10.8 21.5 12 21.5s2.27-1.21 3.65-1.63C19.33 18.12 22 14.83 22 11c0-5.52-4.48-10-10-10z">
                                            </path>
                                        </svg>
                                        <span class="distance-text">
                                            실종지로부터
                                            <strong>{{
                                                calculateDistance(props.missingLocation.lat, props.missingLocation.lon,
                                                    agent.final_lat, agent.final_lon) }}m</strong>
                                        </span>
                                        <div class="distance-text">
                                            <span>확률 :
                                                <strong>
                                                    {{ agent.probability.toFixed(1) }}%
                                                </strong>
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
    </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import axios from 'axios'

/**
 * Haversine 공식을 사용하여 두 좌표 간의 거리를 미터 단위로 계산
 * @param {number} lat1 - 시작점 위도
 * @param {number} lon1 - 시작점 경도
 * @param {number} lat2 - 도착점 위도
 * @param {number} lon2 - 도착점 경도
 * @returns {number} 거리 (미터)
 */
function calculateDistance(lat1, lon1, lat2, lon2) {
    const toRadian = angle => (Math.PI / 180) * angle
    const R = 6371000 // 지구 반경 (미터)

    const dLat = toRadian(lat2 - lat1)
    const dLon = toRadian(lon2 - lon1)

    const lat1Rad = toRadian(lat1)
    const lat2Rad = toRadian(lat2)

    // Haversine 공식
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1Rad) * Math.cos(lat2Rad) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    const distance = R * c

    return Math.round(distance) // 미터 단위, 반올림
}

// Props
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
        type: Number,
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
            5400
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
        distance: 0,
        final_lat: agent.final_position.latitude,
        final_lon: agent.final_position.longitude,
        address: agent.address || '주소 없음'
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

// 카카오맵 스크립트 로드
const loadKakaoMapScript = () => {
    return new Promise((resolve, reject) => {
        console.log('📦 카카오맵 스크립트 로딩 시작')

        if (window.kakao && window.kakao.maps) {
            console.log('✅ 카카오맵 이미 로드됨')
            resolve()
            return
        }

        const existingScript = document.querySelector(`script[src*="dapi.kakao.com"]`)
        if (existingScript) {
            console.log('⏳ 카카오맵 스크립트 로딩 중...')
            existingScript.addEventListener('load', () => {
                window.kakao.maps.load(() => {
                    console.log('✅ 카카오맵 로드 완료')
                    resolve()
                })
            })
            return
        }

        const script = document.createElement('script')
        script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false`
        script.async = true

        script.onload = () => {
            console.log('✅ 스크립트 로드됨, kakao.maps.load 호출')
            window.kakao.maps.load(() => {
                console.log('✅ 카카오맵 준비 완료')
                resolve()
            })
        }

        script.onerror = (error) => {
            console.error('❌ 스크립트 로드 실패:', error)
            reject(error)
        }

        document.head.appendChild(script)
    })
}

// 카카오 지도 초기화
const initMap = async () => {
    console.log('🗺️ initMap 시작')
    console.log('mapContainer.value:', mapContainer.value)
    console.log('window.kakao:', window.kakao)
    console.log('중심 좌표:', props.missingLocation)

    if (!window.kakao || !window.kakao.maps) {
        console.log('카카오맵 스크립트 로딩 필요')
        await loadKakaoMapScript()
    }

    await nextTick()

    if (!mapContainer.value) {
        console.error('❌ mapContainer가 없습니다!')
        return
    }

    console.log('지도 생성 시작...')

    try {
        const options = {
            center: new window.kakao.maps.LatLng(
                props.missingLocation.lat,
                props.missingLocation.lon
            ),
            level: 5
        }

        map = new window.kakao.maps.Map(mapContainer.value, options)
        console.log('✅ 지도 생성 성공:', map)

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
        console.log('✅ 시작 마커 생성 성공')
    } catch (error) {
        console.error('❌ 지도 생성 실패:', error)
    }
}

// 전체 시나리오 저장
const allScenariosData = ref(null)

// ⭐ 시뮬레이션 데이터 로드 (이 부분을 수정)
const loadSimulationData = async () => {
    isLoading.value = true
    error.value = null

    console.log(`위도 : ${props.missingLocation.lat}`)
    console.log(`경도 : ${props.missingLocation.lon}`)

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

        console.log('✅ 시뮬레이션 응답 수신:', response.data)

        const { scenarios } = response.data

        // ⭐ 여기서 모든 시나리오의 에이전트 주소 조회
        if (scenarios) {
            console.log('🔄 모든 시나리오의 에이전트 주소 조회 시작...')

            // 각 시나리오별로 순회
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

                // 각 에이전트의 주소를 순차적으로 조회
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

                    // API 요청 throttle (300ms마다 1개씩)
                    await new Promise(resolve => setTimeout(resolve, 300))
                }

                console.log(`✅ ${scenarioKey} 모든 에이전트 주소 조회 완료!\n`)
            }

            console.log('✅✅✅ 모든 시나리오 주소 조회 완료')
        }

        // 전체 데이터 저장
        allScenariosData.value = scenarios

        availableScenarios.value = [
            { scenario: '30분', available: !!scenarios['30분'] },
            { scenario: '60분', available: !!scenarios['60분'] },
            { scenario: '90분', available: !!scenarios['90분'] }
        ]

        // 초기 데이터 설정 (30분)
        animationData.value = {
            data: scenarios['30분']
        }

        currentStep.value = 0

        console.log('📦 애니메이션 데이터 설정 완료')

        await nextTick()

        setTimeout(() => {
            console.log('🚀 에이전트 초기화 시작')
            initializeAgents()
        }, 500)

    } catch (err) {
        console.error('❌ 시뮬레이션 데이터 로드 실패:', err)
        console.error('에러 상세:', err.response?.data)
        error.value = err.response?.data?.detail || '데이터를 불러올 수 없습니다.'
    } finally {
        isLoading.value = false
    }
}

// VWorld를 통해 도로명주소 불러오는 것 추가
// VWorld API 키
const VWORLD_API_KEY = '6A0CFFEF-45CF-3426-882D-44A63B5A5289'

// ⭐ VWorld 역지오코딩 API
async function fetchVWorldData(location) {
    try {
        console.log(`🗺️ VWorld 역지오코딩 API: ${location.lon}, ${location.lat}`)

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

        console.log(`📍 요청 URL:`, proxyUrl)

        const response = await fetch(proxyUrl, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        })

        if (!response.ok) {
            console.error(`❌ HTTP ${response.status}`)
            return { address: '주소 조회 실패' }
        }

        const data = await response.json()

        console.log(`✅ VWorld 응답:`, data)

        // ⭐ data.response.result[0].text로 바로 반환
        if (data.response?.result?.length > 0) {
            const fullAddress = data.response.result[0].text
            console.log(`📍 전체 주소:`, fullAddress)

            return { address: fullAddress }
        }

        return { address: '주소 조회 실패' }

    } catch (error) {
        console.error(`❌ VWorld API 호출 실패:`, error)
        return { address: '주소 조회 실패' }
    }
}

// ⭐ 에이전트 주소 조회
async function fetchAgentAddress(agent) {
    try {
        const addressData = await fetchVWorldData({
            lat: agent.final_position.latitude,
            lon: agent.final_position.longitude
        })

        // ⭐ 바로 agent.address에 할당
        agent.address = addressData.address

        console.log(`✅ Agent ${agent.rank} 주소 조회 완료:`, agent.address)

    } catch (error) {
        console.error(`❌ Agent ${agent.rank} 주소 조회 에러:`, error)
        agent.address = '주소 조회 실패'
    }
}

// ⭐ 시나리오 변경 - 수정됨
const changeScenario = async (scenario) => {
    if (scenario === currentScenario.value) return

    console.log(`🔄 시나리오 변경: ${currentScenario.value} → ${scenario}`)

    // ⭐ 저장된 전체 데이터에서 가져오기
    if (!allScenariosData.value || !allScenariosData.value[scenario]) {
        console.error('❌ 시나리오 데이터 없음:', scenario)
        return
    }

    currentScenario.value = scenario
    isPlaying.value = false

    // 기존 마커/경로 제거
    clearMapElements()

    // ⭐ 새 시나리오 데이터 설정
    animationData.value = {
        data: allScenariosData.value[scenario]
    }

    currentStep.value = 0

    console.log(`✅ ${scenario} 데이터 로드 완료`)
    console.log(`agents: ${allScenariosData.value[scenario]?.agents?.length}개`)
    console.log(`frames: ${allScenariosData.value[scenario]?.frames?.length}개`)

    await nextTick()

    setTimeout(() => {
        console.log('🚀 에이전트 재초기화')
        initializeAgents()
    }, 300)
}

// 에이전트 초기화
const initializeAgents = () => {
    console.log('👥 initializeAgents 호출')
    console.log('map:', map)
    console.log('animationData:', animationData.value)

    if (!animationData.value || !map) {
        console.warn('❌ 지도 또는 애니메이션 데이터가 준비되지 않음')
        console.log('map:', map)
        console.log('animationData.value:', animationData.value)
        return
    }

    if (!animationData.value.data || !animationData.value.data.frames || animationData.value.data.frames.length === 0) {
        console.warn('❌ 프레임 데이터 없음')
        return
    }

    const firstFrame = animationData.value.data.frames[0]

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

        markers[agent.rank] = { overlay: customOverlay, position: position }
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

        paths[agent.rank] = { line: polyline, points: [] }
        console.log(`✅ 경로 생성 완료: rank ${agent.rank}`)
    } catch (error) {
        console.error(`❌ 경로 생성 실패 (rank ${agent.rank}):`, error)
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
            console.error(`프레임 업데이트 실패 (rank ${agent.rank}):`, error)
        }
    })
}

// 재생/일시정지 토글
const togglePlay = () => {
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

    Object.values(paths).forEach(path => {
        path.line.setMap(null)
        path.points = []
        path.line.setPath([])
    })

    updateFrame()
}

// 에이전트 선택
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
    clearMapElements()
    emit('close')
}

// Watch - 모달 열림/닫힘 감지
watch(() => props.isVisible, async (newVal) => {
    console.log('🔄 isVisible 변경:', newVal)

    if (newVal) {
        await nextTick()
        console.log('✅ nextTick 완료')
        console.log('🔍 mapContainer.value:', mapContainer.value)

        await new Promise(resolve => setTimeout(resolve, 100))

        await initMap()
        console.log('✅ 지도 초기화 완료')

        await new Promise(resolve => setTimeout(resolve, 300))

        await loadSimulationData()
        console.log('✅ 데이터 로드 완료')
    } else {
        stopAnimation()
        clearMapElements()
    }
})

// Lifecycle
onUnmounted(() => {
    stopAnimation()
    clearMapElements()
    currentScenario.value = null
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
    width: 100%;
    position: relative;
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
    top: 15px;
    margin-bottom: 30px;
}

.stat-label-modern {
    font-size: 14px;
    font-weight: 600;
    color: #585858;
    margin: 0 0 6px 0;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.stat-sublabel-modern-1 {
    font-size: 14px;
    color: #3f3f3f;
    margin: 4px 0 0 0;
}

/* ⭐ 웹 버전 모달 - 크게 변경 */

/* 1. 오버레이 */
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
    overflow: hidden;
}

@keyframes fadeIn {
    from {
        opacity: 0;
    }

    to {
        opacity: 1;
    }
}

/* 2. ⭐⭐⭐ 모달 크기 대폭 확대 (웹 버전) */
.agent-simulation-modal {
    background: white;
    border-radius: 20px;
    width: 1200px;
    /* ⭐ 355px → 1200px */
    height: 85vh;
    /* ⭐ 780px → 85vh (화면 높이의 85%) */
    max-width: 95vw;
    /* ⭐ 화면이 작으면 95%까지 축소 */
    display: flex;
    flex-direction: row;
    /* ⭐ column → row (좌우 배치) */
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
    animation: slideUp 0.3s ease;
    overflow: hidden;
    position: relative;
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

/* 3. 헤더 (전체 상단에 배치) */
.modal-header {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 30px;
    /* ⭐ 15px → 20px */
    border-bottom: 2px solid #f0f0f0;
    background: #667eea;
    color: white;
    border-radius: 20px 20px 0 0;
    z-index: 10;
}

.header-content {
    display: flex;
    align-items: center;
    gap: 12px;
}

.header-content i {
    font-size: 24px;
    /* ⭐ 20px → 24px */
}

.header-content h2 {
    margin: 0;
    font-size: 20px;
    /* ⭐ 16px → 20px */
    font-weight: 700;
}

.close-button {
    background: rgba(255, 255, 255, 0.2);
    border: none;
    width: 40px;
    /* ⭐ 32px → 40px */
    height: 40px;
    border-radius: 50%;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    transition: all 0.3s;
    font-size: 18px;
    /* ⭐ 14px → 18px */
}

.close-button:hover {
    background: rgba(255, 255, 255, 0.3);
}

/* 로딩/에러 상태 */
.loading-overlay-inner,
.error-overlay-inner {
    position: absolute;
    top: 70px;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.95);
    z-index: 999;
}

.loading-state,
.error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 20px;
    padding: 40px;
}

.loading-spinner {
    width: 80px;
    /* ⭐ 60px → 80px */
    height: 80px;
    border: 5px solid #f3f3f3;
    border-top: 5px solid #667eea;
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

.loading-state p,
.error-state p {
    font-size: 16px;
    /* ⭐ 크기 증가 */
}

.error-state i {
    font-size: 60px;
    /* ⭐ 48px → 60px */
    color: #ff6b6b;
}

.retry-button {
    padding: 14px 28px;
    /* ⭐ 12px → 14px */
    background: #667eea;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-weight: 600;
    font-size: 15px;
    transition: all 0.3s;
}

.retry-button:hover {
    background: #5568d3;
    transform: translateY(-2px);
}

/* 4. ⭐⭐⭐ modal-content: 좌우 레이아웃 */
.modal-content {
    display: flex;
    flex-direction: row;
    /* ⭐ 좌우 배치 */
    width: 100%;
    height: 100%;
    padding-top: 70px;
    /* ⭐ 헤더 높이만큼 여백 */
    overflow: hidden;
}

/* 5. ⭐⭐⭐ 왼쪽: 지도 + 컨트롤 (60% 너비) */
.map-and-controls {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

/* 지도 영역 */
.map-section {
    flex: 1;
    /* ⭐ 남은 공간 모두 차지 */
    position: relative;
    min-height: 0;
}

.simulation-map {
    width: 100%;
    height: 100%;
    position: relative;
}

/* 컨트롤 패널 */
.control-panel {
    background: #f8f9fa;
    padding: 20px;
    /* ⭐ 12px → 20px */
    display: flex;
    flex-direction: column;
    gap: 16px;
    /* ⭐ 12px → 16px */
    border-top: 1px solid #e0e0e0;
    flex-shrink: 0;
}

.scenario-selector label {
    display: block;
    font-weight: 600;
    margin-bottom: 10px;
    color: #333;
    font-size: 15px;
    /* ⭐ 13px → 15px */
}

.scenario-buttons {
    display: flex;
    gap: 12px;
    /* ⭐ 간격 추가 */
}

.scenario-btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 20px;
    /* ⭐ 8px → 12px */
    background: #ffffff;
    border-radius: 10px;
    cursor: pointer;
    border: 2px solid #e0e0e0;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
    font-size: 16px;
    /* ⭐ 크기 증가 */
    font-weight: 600;
    transition: all 0.3s;
}

.scenario-btn i {
    font-size: 16px;
    /* ⭐ 14px → 16px */
}

.scenario-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
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

/* 재생 컨트롤 */
.playback-controls {
    display: flex;
    gap: 12px;
}

.btn-play,
.btn-reset {
    flex: 1;
    padding: 12px;
    /* ⭐ 8px → 12px */
    background: #667eea;
    color: white;
    border: none;
    border-radius: 10px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    font-weight: 600;
    transition: all 0.3s;
    font-size: 14px;
    /* ⭐ 11px → 14px */
}

.btn-play:hover,
.btn-reset:hover {
    background: #5568d3;
    transform: translateY(-2px);
}

.speed-selector {
    flex: 1;
    padding: 12px;
    /* ⭐ 8px → 12px */
    border: 2px solid #e0e0e0;
    border-radius: 10px;
    background: white;
    cursor: pointer;
    font-weight: 600;
    font-size: 14px;
    /* ⭐ 10px → 14px */
}

/* 타임라인 */
.timeline-section {
    background: white;
    padding: 16px;
    /* ⭐ 10px → 16px */
    border-radius: 10px;
}

.timeline-info {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    font-weight: 600;
    color: #333;
    font-size: 16px;
    /* ⭐ 14px → 16px */
}

.timeline-slider {
    width: 100%;
    height: 6px;
    /* ⭐ 5px → 6px */
    border-radius: 3px;
    background: #e0e0e0;
    outline: none;
    cursor: pointer;
}

/* 6. ⭐⭐⭐ 오른쪽: 에이전트 리스트 (40% 너비) */
.agents-list-panel {
    width: 450px;
    /* ⭐ 고정 너비 */
    background: white;
    border-left: 2px solid #e0e0e0;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    /* ⭐ 스크롤 가능 */
    scrollbar-width: none;
}

.panel-header {
    margin-top: -30px;
    padding: 20px;
    /* ⭐ 12px → 20px */
    border-bottom: 2px solid #f0f0f0;
    flex-shrink: 0;
    background: #fafbfc;
}

.panel-header h3 {
    margin: 0 0 6px 0;
    font-size: 18px;
    /* ⭐ 15px → 18px */
    font-weight: 700;
    color: #333;
}

.subtitle {
    font-size: 13px;
    /* ⭐ 11px → 13px */
    color: #888;
}

/* 에이전트 리스트 */
.agents-list {
    display: flex;
    flex-direction: column;
    padding: 20px;
    /* ⭐ 12px → 20px */
    gap: 16px;
    /* ⭐ 12px → 16px */
}

.agent-item {
    cursor: pointer;
    transition: all 0.3s;
}

.agent-item:hover .agent-card {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
}

.agent-item.selected .agent-card {
    border-color: #667eea;
    background: rgba(102, 126, 234, 0.05);
}

.agent-card {
    background: white;
    border: 2px solid #f0f0f0;
    border-radius: 12px;
    padding: 18px;
    /* ⭐ 14px → 18px */
    transition: all 0.3s;
    
}

.agent-header {
    display: flex;
    align-items: flex-start;
    gap: 14px;
}

.rank-badge {
    width: 40px;
    /* ⭐ 35px → 40px */
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    font-size: 16px;
    /* ⭐ 14px → 16px */
    flex-shrink: 0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.title-section {
    flex: 1;
    min-width: 0;
}

.title-section h3 {
    margin: 0 0 6px 0;
    font-size: 15px;
    /* ⭐ 14px → 15px */
    font-weight: 600;
    color: #222;
    word-break: break-word;
    line-height: 1.4;
}

.description {
    margin: 0;
    font-size: 13px;
    /* ⭐ 12px → 13px */
    color: #888;
    line-height: 1.4;
}

.agent-info {
    padding-top: 12px;
    margin-top: 12px;
    border-top: 1px solid #f5f5f5;
}

.location-info {
    display: flex;
    gap: 8px;
    font-size: 13px;
    /* ⭐ 12px → 13px */
    color: #666;
}

.location-icon {
    width: 18px;
    /* ⭐ 16px → 18px */
    height: 18px;
    flex-shrink: 0;
    color: #667eea;
}

.distance-text {
    display: flex;
    align-items: center;
    gap: 6px;
    line-height: 1.5;
}

.distance-text strong {
    color: #222;
    font-weight: 600;
}

/* 커스텀 에이전트 마커 */
:deep(.custom-agent-marker) {
    width: 28px;
    /* ⭐ 24px → 28px */
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    font-size: 13px;
    /* ⭐ 11px → 13px */
    border: 2px solid white;
    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.3);
    transition: all 0.3s;
}

:deep(.custom-agent-marker.selected) {
    width: 36px;
    /* ⭐ 32px → 36px */
    height: 36px;
    font-size: 16px;
    /* ⭐ 14px → 16px */
    border-width: 3px;
    z-index: 1000;
}

/* 스크롤바 스타일 */
.agents-list-panel::-webkit-scrollbar {
    width: 10px;
    /* ⭐ 8px → 10px */
}

.agents-list-panel::-webkit-scrollbar-track {
    background: #f8f9fa;
}

.agents-list-panel::-webkit-scrollbar-thumb {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 5px;
}

.agents-list-panel::-webkit-scrollbar-thumb:hover {
    background: linear-gradient(135deg, #5568d3 0%, #653a8a 100%);
}

/* ⭐ 반응형: 화면이 작으면 세로 레이아웃으로 전환 */
@media (max-width: 1024px) {
    .agent-simulation-modal {
        width: 95vw;
        height: 90vh;
        flex-direction: column;
    }

    .modal-content {
        flex-direction: column;
    }

    .agents-list-panel {
        width: 100%;
        border-left: none;
        border-top: 2px solid #e0e0e0;
        max-height: 40%;
    }
}
</style>
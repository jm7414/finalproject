<template>
    <div class="page-container">

        <!-- 지도 영역 -->
        <div ref="mapContainer" class="map-area">
            <canvas ref="heatmapCanvas" class="heatmap-canvas"></canvas>

            <!-- ⭐ 지도 위 Floating Action Buttons -->
            <div class="floating-actions">
                <button class="fab" @click="centerToMissingLocation" title="실종 위치로 이동">
                    <i class="bi bi-crosshair"></i>
                </button>
            </div>
        </div>

        <!-- 토글 버튼 영역 -->
        <div class="toggle-button-wrapper">
            <button class="toggle-button" :class="{ active: selectedType === 'info' }" @click="mapOrInfo('info')">
                <i class="bi bi-person-fill"></i>
                <span class="button-text">실종자 정보</span>
            </button>

            <button class="toggle-button" :class="{ active: selectedType === 'map' }" @click="mapOrInfo('map')">
                <i class="bi bi-map-fill"></i>
                <span class="button-text">예상위치</span>
            </button>
        </div>

        <!-- ⭐ 드래그 가능한 타임라인 프로그레스 바 -->
        <div class="timeline-container" v-if="selectedType === 'map'">
            <div class="timeline-header">
                <i class="bi bi-clock-history"></i>
                <span class="timeline-title">실종 경과 시간</span>
            </div>

            <div class="timeline-wrapper" ref="timelineWrapper" @mousedown="startDrag" @touchstart="startDrag">

                <!-- 프로그레스 바 배경 -->
                <div class="timeline-track">
                    <!-- 구간별 색상 세그먼트 -->
                    <div class="timeline-segment segment-1"></div>
                    <div class="timeline-segment segment-2"></div>
                    <div class="timeline-segment segment-3"></div>

                    <!-- 활성화된 프로그레스 -->
                    <div class="timeline-progress" :style="{ width: progressWidth + '%' }">
                        <div class="timeline-glow"></div>
                    </div>
                </div>

                <!-- 타임 마커들 -->
                <div class="timeline-markers">
                    <div class="timeline-marker" style="left: 0%">
                        <div class="marker-dot"></div>
                        <span class="marker-label">실종</span>
                    </div>
                    <div class="timeline-marker" style="left: 33.33%">
                        <div class="marker-dot"></div>
                        <span class="marker-label">30분</span>
                    </div>
                    <div class="timeline-marker" style="left: 66.66%">
                        <div class="marker-dot"></div>
                        <span class="marker-label">60분</span>
                    </div>
                    <div class="timeline-marker" style="left: 100%">
                        <div class="marker-dot"></div>
                        <span class="marker-label">90분</span>
                    </div>
                </div>

                <!-- 드래그 가능한 핸들 -->
                <div class="timeline-handle" :style="{ left: progressWidth + '%' }" @mousedown.stop="startDrag"
                    @touchstart.stop="startDrag">
                    <div class="handle-icon">
                        <i class="bi bi-person-walking"></i>
                    </div>
                    <div class="handle-tooltip">{{ selectedMinutes }}분</div>
                </div>
            </div>

            <!-- 구간 설명 -->
            <div class="timeline-legend">
                <div class="legend-item" :class="{ active: selectedMinutes <= 30 }" @click="setTime(30)">
                    <div class="legend-color" style="background: #66bb6a;"></div>
                    <span class="legend-text">실종~30분</span>
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

        <!-- 컨텐츠 영역 -->
        <div class="content-section">
            <!-- 실종자 정보 -->
            <div v-if="selectedType === 'info'" class="missing-person-info">
                <div class="info-header-section">
                    <div class="profile-image-wrapper">
                        <img class="profile-image" src="../assets/logo.svg" alt="실종자 사진" />
                        <div class="profile-border-glow"></div>
                    </div>

                    <div class="basic-info-wrapper">
                        <div class="name-age-row">
                            <h2 class="person-name">김○○ (78세)</h2>
                        </div>
                        <p class="age-info">
                            <i class="bi bi-clock"></i>
                            {{ Math.floor(elapsedMinutes / 60) }}시간 전
                        </p>
                        <p class="missing-datetime">
                            <i class="bi bi-calendar-event"></i>
                            실종일시: {{ missingTime }}
                        </p>
                        <p class="missing-location">
                            <i class="bi bi-geo-alt"></i>
                            실종장소: 서울 강남구 역삼동
                        </p>
                    </div>
                </div>

                <div class="detail-sections">
                    <div class="info-item glass-card">
                        <div class="info-badge">
                            <i class="bi bi-person-badge"></i>
                            <span class="badge-label">신체 특징</span>
                        </div>
                        <span class="info-content">키 160cm, 지적장애</span>
                    </div>

                    <div class="info-item glass-card">
                        <div class="info-badge">
                            <i class="bi bi-bag"></i>
                            <span class="badge-label">착의사항</span>
                        </div>
                        <span class="info-content">회색 티셔츠, 흰색 운동화</span>
                    </div>

                    <div class="info-item glass-card">
                        <div class="info-badge">
                            <i class="bi bi-exclamation-triangle"></i>
                            <span class="badge-label">특이사항</span>
                        </div>
                        <span class="info-content">키 160cm, 지적장애</span>
                    </div>

                    <div class="info-item glass-card">
                        <div class="info-badge">
                            <i class="bi bi-people"></i>
                            <span class="badge-label">함께하는 이웃</span>
                        </div>
                        <span class="info-content">3명</span>
                        <button class="btn btn-info modern-btn" @click="wherePeople">
                            <i class="bi bi-arrow-right-circle"></i>
                            함께하는 사람 보기
                        </button>
                    </div>
                </div>
            </div>

            <!-- 예상 위치 카드 리스트 -->
            <div v-if="selectedType === 'map'" class="prediction-list">
                <!-- ⭐ Skeleton Loading -->
                <div v-if="isLoading" class="skeleton-container">
                    <div class="skeleton-card" v-for="i in 3" :key="i">
                        <div class="skeleton-icon"></div>
                        <div class="skeleton-content">
                            <div class="skeleton-line skeleton-line-long"></div>
                            <div class="skeleton-line skeleton-line-short"></div>
                        </div>
                    </div>
                </div>

                <div v-else-if="sortedAndFilteredLocations.length === 0" class="empty-state">
                    <div class="empty-icon-wrapper">
                        <i class="bi bi-search"></i>
                    </div>
                    <p v-if="filterEasyAccess">접근 쉬운 위치가 없습니다</p>
                    <p v-else>예상 위치 데이터를 불러오는 중...</p>
                </div>

                <div class="prediction-card" v-for="(loc, index) in sortedAndFilteredLocations" :key="index"
                    :class="{ 'selected': selectedLocation && selectedLocation.lat === loc.lat && selectedLocation.lon === loc.lon }"
                    @click="selectLocation(loc)">


                    <!-- 우측: 상세 정보 -->
                    <div class="card-content">
                        <div class="location-header">
                            <!-- 좌측: 순위 아이콘 
                             -->
                            <div class="card-icon-wrapper">
                                <div class="location-icon-modern" :style="{
                                    background: getAccessibilityGradient(loc.accessibility_score),
                                    boxShadow: `0 8px 20px ${getAccessibilityColor(loc.accessibility_score)}60`
                                }">
                                    <span class="rank-number">{{ index + 1 }}</span>
                                    <div class="particle-ring"></div>
                                </div>
                            </div>

                            <h4 class="location-name">
                                {{ loc.address }}
                            </h4>

                            <div class="probability-badge-modern" :style="{
                                '--progress': loc.value,
                                '--color': getProbabilityColor(loc.value)
                            }">
                                <svg class="progress-ring" viewBox="0 0 36 36">
                                    <circle class="progress-ring-bg" cx="18" cy="18" r="15.915" />
                                    <circle class="progress-ring-progress" cx="18" cy="18" r="15.915"
                                        :style="{ strokeDashoffset: 100 - (loc.value * 100) }" />
                                </svg>
                                <span class="probability-text">{{ (loc.value * 100).toFixed(0) }}%</span>
                            </div>
                        </div>
                        <p class="location-distance">
                        <div>
                            <i class="bi bi-geo-alt-fill"></i>
                            {{ loc.dist_m }}m · {{ getTimeRangeText(((loc.dist_m) / 20).toFixed(0)) }}
                        </div>
                        <span class="type-badge-modern" :class="loc.type">
                            {{ loc.type === 'cluster' ? '자주 방문' : '예상 이동' }}
                        </span>
                        </p>


                        <!-- 선택된 카드일 때만 경로 버튼 표시 -->
                        <div v-if="selectedLocation && selectedLocation.lat === loc.lat && selectedLocation.lon === loc.lon"
                            class="route-controls">
                            <div>
                                <button class="route-toggle-btn-modern" :class="{ active: showingStraightRoute }"
                                    @click.stop="toggleStraightRoute(loc)">
                                    <i class="bi bi-arrow-right-circle"></i>
                                    직진형 경로 {{ showingStraightRoute ? '취소' : '보기' }}
                                </button>
                            </div>
                            <div>
                                <button class="route-toggle-btn-modern" :class="{ active: showingWanderingRoute }"
                                    @click.stop="toggleWanderingRoute(loc)">
                                    <i class="bi bi-arrow-repeat"></i>
                                    배회형 경로 {{ showingWanderingRoute ? '취소' : '보기' }}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 통계 대시보드 -->
                <div class="stats-dashboard-modern glass-card" v-if="metadata && !isLoading">
                    <h3 class="stats-title-modern">
                        <i class="bi bi-bar-chart"></i>
                        예측 분석 정보
                    </h3>

                    <div class="stats-grid">
                        <div class="stat-card-modern">
                            <div class="stat-icon-modern" style="--stat-color: #667eea;">
                                <i class="bi bi-geo-alt"></i>
                            </div>
                            <div class="stat-content-modern">
                                <p class="stat-label-modern">분석 지점</p>
                                <p class="stat-value-modern">{{ metadata.total_points }}<span class="stat-unit">개</span>
                                </p>
                                <p class="stat-sublabel-modern">클러스터 {{ metadata.dbscan_clusters }}개 포함</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import axios from 'axios'

const selectedType = ref('info')
const user_no = ref(3)
const missingTime = ref('2025-10-17 12:00')
const sortBy = ref('probability')
const filterEasyAccess = ref(false)
const selectedLocation = ref(null)
const isLoading = ref(false)

// ⭐ 드래그 가능한 타임라인 관련 상태
const selectedMinutes = ref(30) // 0~90 사이의 분 단위 값
const isDragging = ref(false)
const timelineWrapper = ref(null)

// 경로 표시 관련 상태
const showingStraightRoute = ref(false)
const showingWanderingRoute = ref(false)
const currentStraightPolyline = ref(null)
const currentWanderingPolyline = ref(null)
const currentStraightArrows = ref([])
const currentWanderingArrows = ref([])

// ⭐ API 키 및 URL 정의
const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
const KAKAO_REST_API_KEY = '00a1098d4440e84190fb958e65251575' // REST API key가 없어요.. 제꺼 왜 안되는지 모르겠슴
const VWORLD_API_KEY = '6A0CFFEF-45CF-3426-882D-44A63B5A5289'
const API_BASE_URL = 'http://localhost:8000'

// ⭐ Zone별 색상 정의 (레벨별 3단계 그라데이션)
const ZONE_COLORS = {
    zone_level_1: '#4CAF50',
    zone_level_2: '#FF6B35',
    zone_level_3: '#E91E63'

}

const mapContainer = ref(null)
const heatmapCanvas = ref(null)
let mapInstance = null
let geocoder = null
let heatmapContext = null

const missingLocation = ref({
    lat: 37.234257,
    lon: 126.681727
})

const circles = ref({
    circle700: null,
    circle1500: null,
    circle2100: null
})

let centerMarker = null

const predictionMarkers = ref({
    zone_level_1: [],
    zone_level_2: [],
    zone_level_3: []
})

const predictionData = ref({
    zone_level_1: [],
    zone_level_2: [],
    zone_level_3: []
})

const metadata = ref(null)
const elapsedMinutes = ref(0)

// ⭐ 프로그레스 바 너비 계산 (0~100%)
const progressWidth = computed(() => {
    return (selectedMinutes.value / 90) * 100
})

// ⭐ 현재 시간대에 표시할 zone들 (누적)
const currentZones = computed(() => {
    const minutes = selectedMinutes.value
    if (minutes <= 30) return ['zone_level_1']
    if (minutes <= 60) return ['zone_level_2']
    return ['zone_level_3']
})
const currentZoneMarkers = computed(() => {
    const minutes = selectedMinutes.value
    if (minutes <= 30) return ['zone_level_1']
    if (minutes <= 60) return ['zone_level_1', 'zone_level_2']
    return ['zone_level_1', 'zone_level_2', 'zone_level_3']
})

// ⭐ 두 좌표 간의 거리 계산 (하버사인 공식)
function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371e3 // 지구 반지름 (미터)
    const φ1 = lat1 * Math.PI / 180
    const φ2 = lat2 * Math.PI / 180
    const Δφ = (lat2 - lat1) * Math.PI / 180
    const Δλ = (lon2 - lon1) * Math.PI / 180

    const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
        Math.cos(φ1) * Math.cos(φ2) *
        Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    return R * c // 미터 단위 거리
}

// ⭐⭐⭐ VWorld API로 지목 조회
async function getJimokFromVWorld(lng, lat) {
    const columns = [
        'pnu', 'sido_nm', 'sgg_nm', 'emd_nm', 'ri_nm',
        'jibun', 'jimok', 'parea', 'rn_nm', 'bld_mnnm',
        'bld_slno', 'ag_geom'
    ].join(',')

    const params = new URLSearchParams({
        service: 'data',
        version: '2.0',
        request: 'GetFeature',
        format: 'json',
        errorformat: 'json',
        size: '10',
        page: '1',
        data: 'LT_C_LANDINFOBASEMAP',
        geomfilter: `POINT(${lng} ${lat})`,
        columns: columns,
        geometry: 'true',
        attribute: 'true',
        buffer: '10',
        crs: 'EPSG:4326',
        key: VWORLD_API_KEY,
        domain: 'api.vworld.kr'
    })

    const apiUrl = `https://api.vworld.kr/req/data?${params.toString()}`
    const proxyUrl = `https://www.vworld.kr/proxy.do?url=${encodeURIComponent(apiUrl)}`

    try {
        const res = await fetch(proxyUrl)

        if (!res.ok) {
            console.error(`VWorld API HTTP error! status: ${res.status}`)
            return null
        }

        const data = await res.text()
        const jsonData = JSON.parse(data)

        console.log('🗺️ VWorld 응답 데이터:', jsonData)

        // jimok 추출
        const jimok = jsonData.response.result.featureCollection.features[0].properties.jimok
        return jimok

    } catch (err) {
        console.error('❌ VWorld API 오류:', err)
        return null
    }
}

// ⭐⭐⭐ Kakao Map API로 좌표 주변 POI 검색 (반경 1m)
async function searchKakaoPOI(lng, lat) {
    if (!KAKAO_REST_API_KEY || KAKAO_REST_API_KEY === '') {
        console.error('❌ Kakao API Key가 비어있습니다!')
        return null
    }
    console.log(`✅ Kakao REST API Key 사용 중`)

    try {
        // ✅ 좌표로 주소 정보 가져오기
        const addressUrl = `https://dapi.kakao.com/v2/local/geo/coord2address.json?x=${lng}&y=${lat}`

        const addressRes = await fetch(addressUrl, {
            headers: {
                'Authorization': `KakaoAK ${KAKAO_REST_API_KEY}`
            }
        })

        console.log(`📍 주소 API 응답 상태: ${addressRes.status}`)

        let addressInfo = null
        if (addressRes.ok) {
            const addressData = await addressRes.json()
            console.log('📍 주소 API 응답:', addressData)

            if (addressData.documents && addressData.documents.length > 0) {
                const addr = addressData.documents[0].address || addressData.documents[0].road_address
                addressInfo = {
                    name: addr?.address_name || '알 수 없는 위치',
                    address: addr?.address_name || '',
                    category: '주소'
                }
            }
        }

        // ✅ 카테고리로 주변 장소 검색 (반경 1m로 변경)
        const categoryUrl = `https://dapi.kakao.com/v2/local/search/category.json?x=${lng}&y=${lat}&radius=1&sort=distance`

        const categoryRes = await fetch(categoryUrl, {
            headers: {
                'Authorization': `KakaoAK ${KAKAO_REST_API_KEY}`
            }
        })

        console.log(`🏪 카테고리 API 응답 상태: ${categoryRes.status}`)

        if (categoryRes.ok) {
            const categoryData = await categoryRes.json()
            console.log('🏪 카테고리 API 응답:', categoryData)

            // 제일 첫 번째 장소 반환
            if (categoryData.documents && categoryData.documents.length > 0) {
                return {
                    name: categoryData.documents[0].place_name,
                    address: categoryData.documents[0].address_name,
                    category: categoryData.documents[0].category_name
                }
            }
        }

        // 카테고리 검색 결과 없으면 주소 정보 반환
        return addressInfo

    } catch (err) {
        console.error('❌ Kakao POI 검색 오류:', err)
        return null
    }
}

// ⭐⭐⭐ 지목에 따른 메시지 생성 함수
function getGrandmaMessage(jimok, poi = null) {
    // 특정 지목만 처리
    const specificJimoks = {
        '전': '밭',
        '답': '논',
        '임야': '산',
        '과수원': '과수원',
        '도로': '길'
    }

    // 특정 지목이면 해당 장소명 반환
    if (specificJimoks[jimok]) {
        return specificJimoks[jimok]
    }

    // 그 외 지목은 POI 정보 사용 (제일 첫 번째 POI)
    if (poi && poi.name) {
        return poi.name
    }

    // POI 정보도 없으면 지목만 반환
    return jimok || '알 수 없는 위치'
}

// ⭐⭐⭐ 좌표를 주소로 변환하는 함수 (VWorld + Kakao 통합)
async function getAddressFromCoords(lat, lon) {
    return new Promise(async (resolve) => {
        try {
            // 1. VWorld API로 지목 조회
            const jimok = await getJimokFromVWorld(lon, lat)
            console.log('🏷️ 지목:', jimok)

            // 2. 특정 지목이 아니면 Kakao POI 검색 (반경 1m)
            const specificJimoks = ['전', '답', '임야', '과수원', '도로']
            let poi = null

            if (!specificJimoks.includes(jimok)) {
                poi = await searchKakaoPOI(lon, lat)
                if (poi) {
                    console.log('📍 POI 정보:', poi)
                }
            }

            // 3. 최종 메시지 생성
            const locationName = getGrandmaMessage(jimok, poi)
            console.log('✅ 최종 위치명:', locationName)

            // 4. 기존 geocoder로 주소 조회
            if (!geocoder) {
                resolve(locationName)
                return
            }

            geocoder.coord2Address(lon, lat, (result, status) => {
                if (status === window.kakao.maps.services.Status.OK) {
                    const address = result[0]?.address
                    if (address) {
                        // 할머니가 '<위치명>'에 있는 것 같아요 형식으로 반환
                        const shortAddress = `${address.region_2depth_name} ${address.region_3depth_name}`
                        resolve(`${shortAddress} (${locationName})`)
                    } else {
                        resolve(locationName)
                    }
                } else {
                    resolve(locationName)
                }
            })

        } catch (err) {
            console.error('❌ 주소 변환 오류:', err)
            resolve('주소 조회 실패')
        }
    })
}

// ⭐ 지도에 표시할 마커 데이터 (누적, 각 zone별 1,2,3번)
const visibleMarkersData = computed(() => {
    const result = []
    const zones = currentZoneMarkers.value

    zones.forEach(zoneName => {
        const zoneData = predictionData.value[zoneName] || []
        // 확률 순으로 정렬하여 상위 3개만 선택
        const sortedData = [...zoneData].sort((a, b) => (b.value || 0) - (a.value || 0)).slice(0, 3)

        sortedData.forEach((loc, idx) => {
            const rank = idx + 1 // 각 zone별로 1, 2, 3번

            // ⭐ 거리 계산
            const dist_m = calculateDistance(
                missingLocation.value.lat,
                missingLocation.value.lon,
                loc.lat,
                loc.lon
            )

            result.push({
                ...loc,
                value: loc.value || 0,
                dist_m: Math.round(dist_m),
                zone: zoneName,
                rank: rank,
                color: ZONE_COLORS[zoneName]
            })
        })
    })

    return result
})

// ⭐ 카드에 표시할 데이터 (각 zone별 상위 3개만)
const cardDisplayData = computed(() => {
    const result = []
    const zones = currentZones.value

    zones.forEach(zoneName => {
        const zoneData = predictionData.value[zoneName] || []
        // ⭐ 확률 순으로 정렬하여 상위 3개만
        const sortedData = [...zoneData].sort((a, b) => (b.value || 0) - (a.value || 0)).slice(0, 3)

        sortedData.forEach((loc, idx) => {
            // ⭐ 거리 계산
            const dist_m = calculateDistance(
                missingLocation.value.lat,
                missingLocation.value.lon,
                loc.lat,
                loc.lon
            )

            result.push({
                ...loc,
                value: loc.value || 0,
                dist_m: Math.round(dist_m),
                address: loc.address || '주소 로딩 중...',
                zone: zoneName,
                rank: idx + 1,
                color: ZONE_COLORS[zoneName]
            })
        })
    })

    return result
})

// ⭐ 타임라인 변경 감지하여 지도 업데이트
watch(selectedMinutes, (newMinutes) => {
    updateMapForTime(newMinutes)
})

// ⭐ 카드 필터링 및 정렬 (상위 3개만)
const sortedAndFilteredLocations = computed(() => {
    let locations = [...cardDisplayData.value]

    if (filterEasyAccess.value) {
        locations = locations.filter(loc =>
            loc.accessibility_score !== null &&
            loc.accessibility_score >= 0.7
        )
    }

    return locations.sort((a, b) => {
        if (sortBy.value === 'probability') {
            return (b.value || 0) - (a.value || 0)
        } else if (sortBy.value === 'distance') {
            return (a.dist_m || 0) - (b.dist_m || 0)
        } else if (sortBy.value === 'accessibility') {
            const aScore = a.accessibility_score ?? -1
            const bScore = b.accessibility_score ?? -1
            return bScore - aScore
        }
        return 0
    })
})

function setTime(minutes) {
    selectedMinutes.value = minutes
}

// ⭐ 드래그 시작
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

// ⭐ 이벤트로부터 시간 업데이트
function updateTimeFromEvent(event) {
    if (!timelineWrapper.value) return

    const rect = timelineWrapper.value.getBoundingClientRect()
    const clientX = event.touches ? event.touches[0].clientX : event.clientX
    const x = clientX - rect.left
    const percentage = Math.max(0, Math.min(1, x / rect.width))

    selectedMinutes.value = Math.round(percentage * 90)
}

// ⭐ 시간에 따라 지도 업데이트 (원과 마커 - 누적)
function updateMapForTime(minutes) {
    if (!mapInstance || !circles.value.circle700) return

    // 모든 경로와 선택 상태 초기화
    clearAllRoutes()
    selectedLocation.value = null

    // 원 업데이트
    if (minutes <= 30) {
        const radius = (minutes / 30) * 700
        circles.value.circle700.setRadius(radius)
        circles.value.circle700.setMap(mapInstance)
        circles.value.circle1500.setMap(null)
        circles.value.circle2100.setMap(null)
    } else if (minutes <= 60) {
        circles.value.circle700.setRadius(700)
        circles.value.circle700.setMap(mapInstance)

        const radius = 700 + ((minutes - 30) / 30) * (1500 - 700)
        circles.value.circle1500.setRadius(radius)
        circles.value.circle1500.setMap(mapInstance)
        circles.value.circle2100.setMap(null)
    } else {
        circles.value.circle700.setRadius(700)
        circles.value.circle700.setMap(mapInstance)
        circles.value.circle1500.setRadius(1500)
        circles.value.circle1500.setMap(mapInstance)

        const radius = 1500 + ((minutes - 60) / 30) * (2100 - 1500)
        circles.value.circle2100.setRadius(radius)
        circles.value.circle2100.setMap(mapInstance)
    }

    // ⭐ 마커 표시 (누적)
    showMarkers()
}

function getTimeRangeText(minutes) {
    if (minutes <= 30) return `실종 후 ${minutes}분`
    if (minutes <= 60) return `실종 후 ${minutes}분`
    return `실종 후 ${minutes}분`
}


function getProbabilityColor(value) {
    if (value >= 0.7) return '#f5576c'
    if (value >= 0.4) return '#00f2fe'
    return '#38f9d7'
}

function getAccessibilityGradient(score) {
    if (score === null || score === undefined) return 'linear-gradient(135deg, #c4c4c4 0%, #9e9e9e 100%)'
    if (score >= 0.7) return 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)'
    if (score >= 0.4) return 'linear-gradient(135deg, #f2994a 0%, #f2c94c 100%)'
    return 'linear-gradient(135deg, #eb3349 0%, #f45c43 100%)'
}

function getAccessibilityColor(score) {
    if (score === null || score === undefined) return '#9e9e9e'
    if (score >= 0.7) return '#38ef7d'
    if (score >= 0.4) return '#f2c94c'
    return '#f45c43'
}


function mapOrInfo(type) {
    selectedType.value = type

    if (type === 'map') {
        updateMapForTime(selectedMinutes.value)
        if (centerMarker) centerMarker.setMap(mapInstance)
    } else {
        hideMarkers()
        hideCircles()
        clearAllRoutes()
        if (centerMarker) centerMarker.setMap(null)
    }
}

// ⭐ 마커 표시 (누적, 각 zone별 1,2,3번)
function showMarkers() {
    if (selectedType.value !== 'map') return

    // 먼저 모든 마커 숨김
    hideMarkers()

    const markersToShow = visibleMarkersData.value

    markersToShow.forEach(locData => {
        const zoneMarkers = predictionMarkers.value[locData.zone]
        const originalIndex = predictionData.value[locData.zone].findIndex(
            item => item.lat === locData.lat && item.lon === locData.lon
        )

        if (originalIndex !== -1 && zoneMarkers[originalIndex]) {
            const marker = zoneMarkers[originalIndex]
            marker.setImage(createCustomMarker(locData.rank, locData.color))
            marker.setMap(mapInstance)
        }
    })
}

function hideMarkers() {
    Object.values(predictionMarkers.value).forEach(markers => {
        markers.forEach(marker => marker.setMap(null))
    })
}

function hideCircles() {
    if (!circles.value.circle700) return
    circles.value.circle700.setMap(null)
    circles.value.circle1500.setMap(null)
    circles.value.circle2100.setMap(null)
}

// ⭐ 위치 선택 (같은 카드 재클릭시 경로 제거 및 선택 해제)
function selectLocation(loc) {
    // 같은 카드를 다시 클릭한 경우
    if (selectedLocation.value &&
        selectedLocation.value.lat === loc.lat &&
        selectedLocation.value.lon === loc.lon) {
        // 모든 경로 제거
        clearAllRoutes()
        // 선택 해제
        selectedLocation.value = null
        return
    }

    // 다른 카드를 클릭한 경우
    clearAllRoutes()
    selectedLocation.value = loc

    // 지도 중심을 해당 위치로 이동
    if (mapInstance) {
        const position = new window.kakao.maps.LatLng(loc.lat, loc.lon)
        mapInstance.panTo(position)
        mapInstance.setLevel(4)
    }
}

function wherePeople() {
    alert('함께하는 사람 보기 기능 (미구현)')
}

function centerToMissingLocation() {
    if (mapInstance) {
        const center = new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon)
        mapInstance.panTo(center)
        mapInstance.setLevel(6)
    }
}

function toggleMapStyle() {
    alert('지도 스타일 변경 기능 (미구현)')
}

// ⭐ 직선 경로 토글
async function toggleStraightRoute(loc) {
    if (showingStraightRoute.value) {
        if (currentStraightPolyline.value) {
            currentStraightPolyline.value.setMap(null)
            currentStraightPolyline.value = null
        }
        currentStraightArrows.value.forEach(arr => arr.setMap(null))
        currentStraightArrows.value = []
        showingStraightRoute.value = false
    } else {
        if (loc.straight_route && loc.straight_route.points) {
            const pathCoords = loc.straight_route.points.map(p =>
                new window.kakao.maps.LatLng(p.lat, p.lon)
            )

            if (currentStraightPolyline.value) {
                currentStraightPolyline.value.setMap(null)
            }

            currentStraightPolyline.value = new window.kakao.maps.Polyline({
                path: pathCoords,
                strokeWeight: 5,
                strokeColor: '#FF6B6B',
                strokeOpacity: 0.8,
                strokeStyle: 'solid'
            })

            currentStraightPolyline.value.setMap(mapInstance)

            currentStraightArrows.value.forEach(arr => arr.setMap(null))
            currentStraightArrows.value = []

            // ⭐ 화살표 마커 생성 (heading 정보 활용)
            for (let i = 1; i < pathCoords.length; i += Math.max(1, Math.floor(pathCoords.length / 5))) {
                const prev = loc.straight_route.points[i - 1]
                const curr = loc.straight_route.points[i]
                const heading = curr.heading || calculateHeading(prev.lat, prev.lon, curr.lat, curr.lon)

                const arrowMarker = new window.kakao.maps.Marker({
                    position: pathCoords[i],
                    map: mapInstance,
                    image: createArrowMarker('#FF6B6B', heading)
                })
                currentStraightArrows.value.push(arrowMarker)
            }

            showingStraightRoute.value = true
        } else {
            alert('직진형 경로 정보가 없습니다.')
        }
    }
}

// ⭐ 배회형 경로 토글
async function toggleWanderingRoute(loc) {
    if (showingWanderingRoute.value) {
        if (currentWanderingPolyline.value) {
            currentWanderingPolyline.value.setMap(null)
            currentWanderingPolyline.value = null
        }
        currentWanderingArrows.value.forEach(arr => arr.setMap(null))
        currentWanderingArrows.value = []
        showingWanderingRoute.value = false
    } else {
        if (loc.wandering_route && loc.wandering_route.points) {
            const pathCoords = loc.wandering_route.points.map(p =>
                new window.kakao.maps.LatLng(p.lat, p.lon)
            )

            if (currentWanderingPolyline.value) {
                currentWanderingPolyline.value.setMap(null)
            }

            currentWanderingPolyline.value = new window.kakao.maps.Polyline({
                path: pathCoords,
                strokeWeight: 5,
                strokeColor: '#9B59B6',
                strokeOpacity: 0.8,
                strokeStyle: 'shortdash'
            })

            currentWanderingPolyline.value.setMap(mapInstance)

            currentWanderingArrows.value.forEach(arr => arr.setMap(null))
            currentWanderingArrows.value = []

            // ⭐ 화살표 마커 생성 (heading 정보 활용)
            for (let i = 1; i < pathCoords.length; i += Math.max(1, Math.floor(pathCoords.length / 5))) {
                const prev = loc.wandering_route.points[i - 1]
                const curr = loc.wandering_route.points[i]
                const heading = curr.heading || calculateHeading(prev.lat, prev.lon, curr.lat, curr.lon)

                const arrowMarker = new window.kakao.maps.Marker({
                    position: pathCoords[i],
                    map: mapInstance,
                    image: createArrowMarker('#9B59B6', heading)
                })
                currentWanderingArrows.value.push(arrowMarker)
            }

            showingWanderingRoute.value = true
        } else {
            alert('배회형 경로 정보가 없습니다.')
        }
    }
}

// ⭐ 모든 경로 제거
function clearAllRoutes() {
    // 직선 경로 제거
    if (currentStraightPolyline.value) {
        currentStraightPolyline.value.setMap(null)
        currentStraightPolyline.value = null
    }
    currentStraightArrows.value.forEach(arrow => arrow.setMap(null))
    currentStraightArrows.value = []
    showingStraightRoute.value = false

    // 배회형 경로 제거
    if (currentWanderingPolyline.value) {
        currentWanderingPolyline.value.setMap(null)
        currentWanderingPolyline.value = null
    }
    currentWanderingArrows.value.forEach(arrow => arrow.setMap(null))
    currentWanderingArrows.value = []
    showingWanderingRoute.value = false
}

// ⭐ 방향 계산 함수
function calculateHeading(lat1, lon1, lat2, lon2) {
    const dLon = (lon2 - lon1) * Math.PI / 180
    const y = Math.sin(dLon) * Math.cos(lat2 * Math.PI / 180)
    const x = Math.cos(lat1 * Math.PI / 180) * Math.sin(lat2 * Math.PI / 180) -
        Math.sin(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.cos(dLon)
    let bearing = Math.atan2(y, x) * 180 / Math.PI
    return (bearing + 360) % 360
}

// ⭐ 화살표 마커 생성 (heading 적용)
function createArrowMarker(color, heading = 0) {
    const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" 
         style="transform: rotate(${heading}deg)">
      <path d="M12 2 L12 18 M12 18 L6 12 M12 18 L18 12" 
            stroke="${color}" 
            stroke-width="2" 
            fill="none" 
            stroke-linecap="round"/>
    </svg>
  `
    return new window.kakao.maps.MarkerImage(
        'data:image/svg+xml;base64,' + btoa(svg),
        new window.kakao.maps.Size(24, 24),
        { offset: new window.kakao.maps.Point(12, 12) }
    )
}

// ⭐ API 호출 - POST 방식으로 수정
async function getPrediction(userNo, missingTimeStr) {
    isLoading.value = true
    try {
        const response = await axios.post(`${API_BASE_URL}/api/predict-location`, {
            user_no: userNo,
            missing_time: missingTimeStr,
            home_lat: null,
            home_lon: null
        })

        const data = response.data

        // ⭐ Zone별 데이터 저장
        predictionData.value = {
            zone_level_1: data.zone_level_1 || [],
            zone_level_2: data.zone_level_2 || [],
            zone_level_3: data.zone_level_3 || []
        }

        // ⭐⭐⭐ 각 위치에 VWorld + Kakao 통합 주소 정보 추가
        for (const [zoneName, locations] of Object.entries(predictionData.value)) {
            for (const loc of locations) {
                // VWorld 지목 + Kakao POI 주소 변환 (비동기로 처리)
                getAddressFromCoords(loc.lat, loc.lon).then(address => {
                    loc.address = address
                })
            }

            // 마커 생성
            createMarkersForZone(zoneName, locations)
        }

        // ⭐ 실종 위치 업데이트
        if (data.metadata && data.metadata.missing_center_lat && data.metadata.missing_center_lon) {
            missingLocation.value.lat = data.metadata.missing_center_lat
            missingLocation.value.lon = data.metadata.missing_center_lon

            if (mapInstance) {
                const newCenter = new window.kakao.maps.LatLng(
                    missingLocation.value.lat,
                    missingLocation.value.lon
                )
                mapInstance.setCenter(newCenter)

                if (centerMarker) {
                    centerMarker.setPosition(newCenter)
                }

                createCircles()
            }
        }

        metadata.value = data.metadata

        const missingDate = new Date(missingTimeStr)
        const now = new Date()
        elapsedMinutes.value = Math.floor((now - missingDate) / (1000 * 60))

        console.log('Prediction data loaded:', {
            zone_level_1: predictionData.value.zone_level_1.length,
            zone_level_2: predictionData.value.zone_level_2.length,
            zone_level_3: predictionData.value.zone_level_3.length
        })

    } catch (error) {
        console.error('예측 데이터 불러오기 실패:', error)
        if (error.response) {
            console.error('Response data:', error.response.data)
            console.error('Response status:', error.response.status)
        }
        alert('데이터를 불러오는데 실패했습니다. 콘솔을 확인해주세요.')
    } finally {
        setTimeout(() => {
            isLoading.value = false
        }, 500)
    }
}

function createMarkersForZone(zoneName, locations) {
    predictionMarkers.value[zoneName] = []

    locations.forEach((loc, idx) => {
        const position = new window.kakao.maps.LatLng(loc.lat, loc.lon)

        const marker = new window.kakao.maps.Marker({
            position: position,
            map: null,
            image: createCustomMarker(idx + 1, '#667eea')
        })

        // 마커 클릭 이벤트
        window.kakao.maps.event.addListener(marker, 'click', () => {
            selectLocation(loc)
        })

        predictionMarkers.value[zoneName].push(marker)
    })
}

function createCustomMarker(rank, color) {
    const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="40" height="50" viewBox="0 0 40 50">
      <defs>
        <filter id="shadow-${color.replace('#', '')}" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur in="SourceAlpha" stdDeviation="3"/>
          <feOffset dx="0" dy="3" result="offsetblur"/>
          <feFlood flood-color="#000000" flood-opacity="0.3"/>
          <feComposite in2="offsetblur" operator="in"/>
          <feMerge>
            <feMergeNode/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      <path d="M20,2 C10,2 3,9 3,19 C3,28 20,48 20,48 C20,48 37,28 37,19 C37,9 30,2 20,2 Z" 
            fill="${color}" 
            filter="url(#shadow-${color.replace('#', '')})"
            stroke="white"
            stroke-width="2"/>
      
      <circle cx="20" cy="19" r="12" fill="white" opacity="0.9"/>
      
      <text x="20" y="19" 
            text-anchor="middle" 
            dominant-baseline="central"
            font-size="14" 
            font-weight="bold" 
            fill="${color}">${rank}</text>
    </svg>
  `

    return new window.kakao.maps.MarkerImage(
        'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg),
        new window.kakao.maps.Size(40, 50),
        {
            offset: new window.kakao.maps.Point(20, 48)
        }
    )
}

function createCircles() {
    const center = new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon)

    // 기존 원 제거
    if (circles.value.circle700) circles.value.circle700.setMap(null)
    if (circles.value.circle1500) circles.value.circle1500.setMap(null)
    if (circles.value.circle2100) circles.value.circle2100.setMap(null)

    // Zone Level 1 (초록색 - 0~30분)
    circles.value.circle700 = new window.kakao.maps.Circle({
        center: center,
        radius: 0,
        strokeWeight: 2,
        strokeColor: '#66bb6a',
        strokeOpacity: 0.6,
        fillColor: '#66bb6a',
        fillOpacity: 0.15,
        map: null
    })

    // Zone Level 2 (주황색 - 30~60분)
    circles.value.circle1500 = new window.kakao.maps.Circle({
        center: center,
        radius: 0,
        strokeWeight: 2,
        strokeColor: '#ff9e7e',
        strokeOpacity: 0.6,
        fillColor: '#ff9e7e',
        fillOpacity: 0.15,
        map: null
    })

    // Zone Level 3 (분홍색 - 60~90분)
    circles.value.circle2100 = new window.kakao.maps.Circle({
        center: center,
        radius: 0,
        strokeWeight: 2,
        strokeColor: '#ff6b9d',
        strokeOpacity: 0.6,
        fillColor: '#ff6b9d',
        fillOpacity: 0.15,
        map: null
    })
}

function initMap() {
    if (!window.kakao || !window.kakao.maps) {
        console.error('Kakao Maps API is not loaded')
        return
    }

    const container = mapContainer.value
    const options = {
        center: new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon),
        level: 6
    }

    mapInstance = new window.kakao.maps.Map(container, options)
    geocoder = new window.kakao.maps.services.Geocoder()

    // 실종 위치 마커 생성
    centerMarker = new window.kakao.maps.Marker({
        position: new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon),
        map: null,
        image: createCenterMarker()
    })

    createCircles()
    getPrediction(user_no.value, missingTime.value)
}

function createCenterMarker() {
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
        {
            offset: new window.kakao.maps.Point(25, 58)
        }
    )
}

onMounted(() => {
    const script = document.createElement('script')
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services&autoload=false`
    script.onload = () => {
        window.kakao.maps.load(() => {
            initMap()
        })
    }
    document.head.appendChild(script)
})
</script>
<style scoped>
/* ⭐ 드래그 가능한 타임라인 스타일 */
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

.timeline-value {
    font-size: 18px;
    font-weight: 800;
    color: #667eea;
    padding: 4px 12px;
    background: rgba(102, 126, 234, 0.1);
    border-radius: 12px;
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

.timeline-segment {
    flex: 1;
    height: 100%;
    opacity: 0.3;
    transition: opacity 0.3s ease;
}

.timeline-segment.segment-1 {
    background: linear-gradient(90deg, #66bb6a 0%, #85d088 100%);
}

.timeline-segment.segment-2 {
    background: linear-gradient(90deg, #ff9e7e 0%, #ffb899 100%);
}

.timeline-segment.segment-3 {
    background: linear-gradient(90deg, #ff6b9d 0%, #ff8bb4 100%);
}

.timeline-progress {
    position: absolute;
    top: 0;
    left: 0;
    height: 100%;
    background: linear-gradient(90deg, #66bb6a 0%, #66bb6a 33.33%, #ff9e7e 33.33%, #ff9e7e 66.66%, #ff6b9d 66.66%, #ff6b9d 100%);
    border-radius: 6px;
    transition: width 0.15s ease-out;
    pointer-events: none;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.timeline-glow {
    position: absolute;
    inset: -2px;
    background: inherit;
    filter: blur(6px);
    opacity: 0.5;
    border-radius: 8px;
}

.timeline-markers {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    left: 0;
    right: 0;
    height: 12px;
    pointer-events: none;
}

.timeline-marker {
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
}

.marker-dot {
    width: 16px;
    height: 16px;
    background: white;
    border: 3px solid #667eea;
    border-radius: 50%;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
    z-index: 2;
}

.marker-label {
    position: absolute;
    top: 30px;
    font-size: 11px;
    font-weight: 600;
    color: #666;
    white-space: nowrap;
    background: white;
    padding: 2px 6px;
    border-radius: 4px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.timeline-handle {
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
    width: 32px;
    height: 32px;
    cursor: grab;
    z-index: 10;
    transition: left 0.15s ease-out;
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
    font-size: 16px;
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
    border: 3px solid white;
    transition: all 0.2s ease;
}

.timeline-handle:hover .handle-icon {
    transform: scale(1.15);
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

.timeline-handle:active .handle-icon {
    transform: scale(1.05);
}

.handle-tooltip {
    position: absolute;
    top: -45px;
    left: 50%;
    transform: translateX(-50%);
    padding: 6px 12px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    font-size: 13px;
    font-weight: 700;
    border-radius: 8px;
    white-space: nowrap;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    pointer-events: none;
}

.handle-tooltip::after {
    content: '';
    position: absolute;
    bottom: -6px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 6px solid transparent;
    border-right: 6px solid transparent;
    border-top: 6px solid #764ba2;
}

.timeline-legend {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    margin-top: 20px;
}

.legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 6px;
    background: rgba(255, 255, 255, 0.7);
    border-radius: 20px;
    border: 1px solid rgb(214, 214, 214);
    transition: all 0.3s ease;
    cursor: pointer;
}

.legend-item.active {
    border-color: #667eea;
    background: rgba(102, 126, 234, 0.1);
    transform: scale(1.05);
}

.legend-color {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.legend-text {
    font-size: 12px;
    font-weight: 600;
    color: #666;
    white-space: nowrap;
}

.legend-item.active .legend-text {
    color: #667eea;
}

/* ============================================
   🎨 Modern UI Enhancements (기존 스타일 유지)
   ============================================ */
/* Floating Action Buttons */
.floating-actions {
    position: absolute;
    top: 16px;
    right: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    z-index: 100;
}

.fab {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    color: white;
    font-size: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fab:hover {
    transform: scale(1.1) rotate(5deg);
    box-shadow: 0 6px 28px rgba(102, 126, 234, 0.6);
}

.fab:active {
    transform: scale(0.95);
}

/* Skeleton Loading */
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

/* Profile Image Border Glow */
.profile-image-wrapper {
    position: relative;
}

.profile-border-glow {
    position: absolute;
    inset: -4px;
    border-radius: 16px;
    background: linear-gradient(45deg, #667eea, #764ba2, #667eea);
    background-size: 200% 200%;
    opacity: 0.6;
    filter: blur(8px);
    z-index: -1;
    animation: gradient-rotate 3s ease infinite;
}

@keyframes gradient-rotate {

    0%,
    100% {
        background-position: 0% 50%;
    }

    50% {
        background-position: 100% 50%;
    }
}

/* Modern Location Icon with Particle Ring */
.location-icon-modern {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    transition: all 0.3s ease;
}

.rank-number {
    font-size: 18px;
    font-weight: 800;
    color: white;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    z-index: 1;
}

/* Circular Progress Badge */
.probability-badge-modern {
    position: relative;
    right: 30px;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.progress-ring {
    position: absolute;
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
}

.progress-ring-bg {
    fill: none;
    stroke: #e0e0e0;
    stroke-width: 3;
}

.progress-ring-progress {
    fill: none;
    stroke: var(--color);
    stroke-width: 3;
    stroke-dasharray: 100;
    stroke-dashoffset: 0;
    stroke-linecap: round;
    transition: stroke-dashoffset 0.6s ease;
}

.probability-text {
    font-size: 10px;
    font-weight: 700;
    color: var(--color);
    z-index: 1;
}

/* Modern Type Badge */
.type-badge-modern {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 600;
    margin-left: 8px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.15) 100%);
    color: #667eea;
    border: 1px solid rgba(102, 126, 234, 0.3);
}

.type-badge-modern.random {
    background: linear-gradient(135deg, rgba(255, 152, 0, 0.15) 0%, rgba(255, 193, 7, 0.15) 100%);
    color: #ff9800;
    border: 1px solid rgba(255, 152, 0, 0.3);
}

/* Modern Accessibility Info */
.accessibility-info-modern {
    margin-top: 10px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 0.85rem;
}

.accessibility-bar {
    flex: 1;
    height: 6px;
    background: #f0f0f0;
    border-radius: 3px;
    overflow: hidden;
    position: relative;
}

.accessibility-fill {
    height: 100%;
    border-radius: 3px;
    transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
}

.accessibility-fill::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
    animation: shimmer-bar 2s infinite;
}

@keyframes shimmer-bar {
    0% {
        transform: translateX(-100%);
    }

    100% {
        transform: translateX(100%);
    }
}

.accessibility-label {
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
}

/* Modern Route Toggle Buttons */
.route-toggle-btn-modern {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 10px;
    background: rgba(255, 255, 255, 0.9);
    width: 150px;
    border: 2px solid #e0e0e0;
    border-radius: 24px;
    font-size: 0.85rem;
    font-weight: 600;
    color: #666;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    white-space: nowrap;
    position: relative;
}

.route-toggle-btn-modern::before {
    content: '';
    position: relative;
    inset: 0;
    background: linear-gradient(135deg, #FF6B6B 0%, #FF5252 100%);
    opacity: 0;
    transition: opacity 0.3s ease;
}

.route-toggle-btn-modern:hover::before {
    opacity: 0.1;
}

.route-toggle-btn-modern:hover {
    border-color: #FF6B6B;
    color: #FF6B6B;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.route-toggle-btn-modern.active {
    background: linear-gradient(135deg, #FF6B6B 0%, #FF5252 100%);
    border-color: transparent;
    color: white;
    box-shadow: 0 4px 16px rgba(255, 107, 107, 0.4);
}

.route-toggle-btn-modern.active::before {
    opacity: 0;
}

.route-toggle-btn-modern i {
    font-size: 1.1rem;
    z-index: 1;
}

/* Modern Stats Dashboard */
.stats-dashboard-modern {
    padding: 24px;
    margin: 16px;
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
    gap: 14px;
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
}

.stat-label-modern {
    font-size: 12px;
    font-weight: 600;
    color: #999;
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

.stat-sublabel-modern {
    font-size: 11px;
    color: #999;
    margin: 4px 0 0 0;
}

/* Empty State Enhancement */
.empty-icon-wrapper {
    width: 80px;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
    margin-bottom: 16px;
}

.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
    color: #999;
}

.empty-state i {
    font-size: 36px;
    color: #667eea;
    margin: 0;
}

.empty-state p {
    font-size: 14px;
    margin: 8px 0 0 0;
    font-weight: 500;
}

/* Info Items */
.info-item {
    position: relative;
}

.info-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
}

.info-badge i {
    font-size: 14px;
}

.modern-btn {
    margin-top: 8px;
    padding: 8px 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    border-radius: 20px;
    color: white;
    font-weight: 600;
    font-size: 13px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.modern-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.modern-btn:active {
    transform: translateY(0);
}

/* Enhanced Basic Info with Icons */
.age-info,
.missing-datetime,
.missing-location {
    display: flex;
    align-items: center;
    gap: 6px;
}

.age-info i,
.missing-datetime i,
.missing-location i {
    font-size: 14px;
    color: #667eea;
}

/* Prediction Card Enhancement */
.prediction-card {
    display: flex;
    flex-direction: column;
    border-radius: 16px;
    width: 347px;
}

.prediction-card.selected {
    border: 2px solid #FF6B6B;
    box-shadow: 0 8px 32px rgba(255, 107, 107, 0.3);
    background: linear-gradient(135deg, rgba(255, 107, 107, 0.05) 0%, rgba(255, 255, 255, 0.95) 100%);
}

.route-controls {
    display: flex;
    justify-content: start;
    padding-top: 14px;
    border-top: 2px solid rgba(0, 0, 0, 0.05);
    gap: 10px;
}

/* Card Components */
.card-icon-wrapper {
    display: flex;
    align-items: center;
    padding-left: 10px;
    justify-content: center;
}

.card-content {
    flex: 1;
    padding: 0 16px 0 0;
    width: 350px;
    min-width: 0;

}

.location-header {
    display: flex;
    align-items: center;
    gap: 5px;
    margin-bottom: 4px;
}

.location-name {
    flex: 1;
    align-items: center;
    width: 150px;
    font-size: 15px;
    font-weight: 700;
    color: #333;
    margin: 0;
    line-height: 1.4;
    word-break: keep-all;
}

.location-distance {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-left: 20px;
    padding-right: 20px;
    gap: 6px;
    font-size: 13px;
    color: #666;
    margin: 0;
    flex-wrap: wrap;
}

.location-distance i {
    font-size: 14px;
    color: #667eea;
}
</style>

<style src="./predictLocation.css"></style>
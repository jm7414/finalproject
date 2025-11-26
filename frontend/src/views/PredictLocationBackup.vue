<template>
    <div class="page-container">

        <!-- ⭐ 전체 화면 로딩 오버레이 추가 -->
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
                <button class="toggle-button" :class="{ active: selectedType === 'info' }" @click="mapOrInfo('info')">
                    <i class="bi bi-person-fill"></i>
                    <span class="button-text">실종자 정보</span>
                </button>

                <button class="toggle-button" :class="{ active: selectedType === 'map' }" @click="mapOrInfo('map')">
                    <i class="bi bi-map-fill"></i>
                    <span class="button-text">예상위치</span>
                </button>
            </div>
            <div v-if="less_data" class="">
                <p>관리하고있는 환자에 대한 데이터가 부족해요.</p>
                <span>예측 위치들이 부정확할 수 있습니다.</span>
                <!-- 추후에 수정할 예정 + 이 부분 predictLocationBackup파일에 적어놨어요!
                    3분간격으로 28일 안되면 예측위치 부정확하다고하고 막아버림
                    모델 결정 후 전체 수정예정
                -->
            </div>
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
            <!-- ⭐ Skeleton Loading -->
            <div v-if="isLoading" class="skeleton-container">
                <div class="skeleton-card" v-for="i in 1" :key="i">
                    <div class="skeleton-icon"></div>
                    <div class="skeleton-content">
                        <div class="skeleton-line skeleton-line-long"></div>
                        <div class="skeleton-line skeleton-line-short"></div>
                    </div>
                </div>
            </div>
            <div v-if="selectedType === 'info'" class="missing-person-info">

                <!-- 병욱 정보 불러오는중 추가 -->
                <div v-if="personLoading" class="status-message">정보를 불러오는 중...</div>
                <div v-else-if="personError" class="status-message error">{{ personError }}</div>


                <div v-if="!isLoading" class="info-header-section">
                    <div class="profile-image-wrapper">
                        <img class="profile-image" :src="personDetail.photoPath || defaultPersonImage"
                            :alt="personDetail.patientName" />
                    </div>
                    <div class="basic-info-wrapper">
                        <div class="name-age-row">
                            <h2 class="person-name">{{ personDetail.patientName || '정보 없음' }} ({{
                                calculateAge(personDetail.patientBirthDate) }}세)</h2>
                        </div>
                        <p class="age-info">
                            <i class="bi bi-clock"></i>
                            {{ elapsedTimeText }}
                        </p>
                        <p class="missing-datetime">
                            <i class="bi bi-calendar-event"></i>
                            실종일시: {{ formatSimpleDateTime(missingTimeDB) }}
                        </p>
                        <p class="missing-location" style="font-size: 12px;">
                            <i class="bi bi-geo-alt"></i>
                            실종장소: {{ missingAddress.fullAddress || '구로구 구로동ㅋ' }}
                        </p>

                        <!-- <p v-if="missingAddress" class="missing-location" style="font-size: 12px;">
                            <i class="bi bi-geo-alt"></i>
                            실종장소: {{ missingAddress.fullAddress }}
                        </p> -->
                    </div>
                </div>

                <div v-if="!isLoading" class="detail-sections">
                    <div class="info-item glass-card">

                        <div class="d-flex align-items-center gap-1">
                            <div class="info-badge">
                                <i class="bi bi-person-badge"></i>
                                <span class="badge-label">인상착의</span>
                            </div>
                            <span class="info-content">{{ formatDescription(personDetail.description).clothing || '170cm 마른 체형' }}</span>
                        </div>

                        <div class="d-flex align-items-center gap-1">
                            <div class="info-badge">
                                <i class="bi bi-bag"></i>
                                <span class="badge-label">소지품</span>
                            </div>
                            <span class="info-content">{{ formatDescription(personDetail.description).belongings ||
                                '정보없음' }}</span>
                        </div>

                        <div class="d-flex align-items-center gap-1">
                            <div class="info-badge">
                                <i class="bi bi-exclamation-triangle"></i>
                                <span class="badge-label">특이사항</span>
                            </div>
                            <span class="info-content">{{ formatDescription(personDetail.description).specialNotes ||
                                '지팡이를 짚고 다니심' }}</span>
                        </div>

                        <div>
                            <div class="info-badge">
                                <i class="bi bi-people"></i>
                                <span class="badge-label">함께하는 이웃</span>
                            </div>
                            <span class="info-content ml-1">{{ (personDetail && personDetail.searchTogetherCount !=
                                null) ? personDetail.searchTogetherCount : participantsCount }}명</span>
                            <div class="d-flex justify-content-center">
                                <button class="btn btn-info modern-btn" :class="{ active: isParticipantsLayerVisible }"
                                    @click="wherePeople">
                                    <i class="bi bi-arrow-right-circle"></i>
                                    {{ isParticipantsLayerVisible ? '함께하는 중...' : '함께하는 사람 보기' }}
                                </button>

                                <button class="btn btn-warning modern-btn report-btn" @click="goToReportPage">
                                    <i class="bi bi-megaphone-fill"></i>
                                    제보하기
                                </button>
                            </div>
                        </div>
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

                <div v-else-if="displayedZoneToShow.length === 0" class="empty-state">
                    <div class="empty-icon-wrapper">
                        <i class="bi bi-search"></i>
                    </div>
                    <p>예상 위치 데이터를 불러오는 중...</p>
                </div>

                <div class="prediction-card" v-for="(loc, index) in displayedZoneToShow" :key="index"
                    :class="{ 'selected': selectedLocation && selectedLocation.lat === loc.lat && selectedLocation.lon === loc.lon }"
                    @click="selectLocation(loc, index)">

                    <!-- 우측: 상세 정보 -->
                    <div class="card-content">
                        <div class="location-header">
                            <!-- 좌측: 순위 아이콘 -->
                            <div class="card-icon-wrapper">
                                <div class="location-icon-modern" :style="{
                                    background: getZoneLevelGradient(displayZoneLevel),
                                    boxShadow: `0 8px 20px ${getZoneLevelColor(displayZoneLevel)}60`
                                }">
                                    <span class="rank-number">{{ index + 1 }}</span>
                                    <div class="particle-ring"></div>
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
                            <i class="bi bi-geo-alt-fill"></i>실종지로부터 {{ loc.dist_m }}m · {{
                                getTimeRangeText(((loc.dist_m) / 20).toFixed(0)) }}
                        </div>
                        </p>
                    </div>
                </div>

                <!-- ⭐ 더보기 버튼 -->
                <div class="d-flex justify-content-center" v-if="hasMoreData">
                    <button class="more-btn" @click="toggleShowMore">
                        <span>{{ showAllLocations ? '접기' : '더보기' }}</span>
                        <i :class="showAllLocations ? 'bi bi-chevron-up' : 'bi bi-chevron-down'"></i>
                    </button>
                </div>

                <!-- 통계 대시보드 -->
                <div class="stats-dashboard-modern glass-card" v-if="!isLoading">
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
                                <p class="stat-value-modern"><span class="stat-unit"> {{ total_cluster }}개의 위치 분석
                                        결과</span>
                                </p>
                                <p class="stat-sublabel-modern-1">{{ personDetail.patientName }}님의 실종위치로부터 각 시간대별</p>
                                <p class="stat-sublabel-modern-1">최대 5개의 위치를 보여줍니다</p>
                            </div>
                        </div>
                        <!-- ★★★ 수정된 stat-card: 클릭 시 모달 오픈 ★★★ -->
                        <div class="stat-card-modern clickable" @click="openAgentSimulation">
                            <div class="stat-icon-modern" style="--stat-color: #667eea;">
                                <i class="bi bi-diagram-3"></i>
                            </div>
                            <div class="stat-content-modern">
                                <p class="stat-label-modern">에이전트 시뮬레이션</p>
                                <p class="stat-sublabel-modern-1">AI 에이전트 기반 경로 예측</p>
                                <p class="stat-sublabel-modern-1">10개 주요 위치의 이동 패턴</p>
                                <div class="click-hint">
                                    <i class="bi bi-cursor-fill"></i>
                                    <span>클릭하여 보기</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!-- ★★★ 에이전트 시뮬레이션 모달 추가 ★★★ -->
        <AgentSimulationModal :isVisible="showAgentSimulation" :userNo="1" :missingLocation="missingLocation"
            :missingTime="missingTimeDB" @close="closeAgentSimulation" />
        <ConfirmModal ref="modal" />
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios'
import { useParticipantLocations } from '@/composables/useParticipantLocations';
import { useSearchStore } from '@/stores/useSearchStore';
import AgentSimulationModal from '@/components/AgentSimulationModal.vue'
import ConfirmModal from '../components/predict_split_Modal.vue'
const modal = ref(null)

// ========================================================================================
// 카카오지도 및 API 키 설정
// ========================================================================================
const mapContainer = ref(null)
const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'
const VWORLD_API_KEY = '6A0CFFEF-45CF-3426-882D-44A63B5A5289'
const TMAP_API_KEY = 'pu1CWi6rz48GHLWhk7NI239il6I2j9fHaSLFeYoi'

const route = useRoute();
const router = useRouter();
const searchStore = useSearchStore();

// 모달
// ★★★ 에이전트 시뮬레이션 모달 상태 추가 ★★★
const showAgentSimulation = ref(false)

// 모달 열기 함수 - 유효성 검사 추가
const openAgentSimulation = () => {

    if (!missingLocation.value.lat || !missingLocation.value.lon) {
        console.error('❌ missingLocation이 없습니다:', missingLocation.value)
        alert('실종 위치 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.')
        return
    }

    console.log('✅ 시뮬레이션 모달 열기:', {
        patientUserNo: patientUserNo.value,
        missingLocation: missingLocation.value,
        missingTimeDB: missingTimeDB.value
    })

    showAgentSimulation.value = true
}

// 모달 닫기 함수
const closeAgentSimulation = () => {
    showAgentSimulation.value = false
}


// ========================================================================================
// 데이터 상태 관리 - API 응답 구조에 맞게 수정
// ========================================================================================

// ⭐ Zone Level별 목적지 데이터 (최대 5개씩)
const zone_level_1 = ref([])  // 500m 이내
const zone_level_2 = ref([])  // 1000m 이내
const zone_level_3 = ref([])  // 1500m 이내

// 실종위치
const missingLocation = ref({
    lat: null,
    lon: null
})

// 마지막 알려진 위치
const lastKnownLocation = ref({
    latitude: null,
    longitude: null,
    time: null
})

// ⭐ 표시할 Zone Level 선택 (1, 2, 3)
const displayZoneLevel = ref(1)

// 로딩 상태
const isLoading = ref(false)

// 선택된 타입 (info 또는 map)
const selectedType = ref(null)

// 선택된 위치
const selectedLocation = ref(null)

// 경과 시간 (분 단위)
const elapsedMinutes = ref(0)

// ⭐ 드래그 가능한 타임라인 관련 상태
const selectedMinutes = ref(30) // 0~90 사이의 분 단위 값
const isDragging = ref(false)
const timelineWrapper = ref(null)

// ⭐ 더보기 관련 상태
const showAllLocations = ref(false)

// ID 관리
const patientUserNo = ref(null)
const missingPostId = ref(null)

// 실종자 정보
const personDetail = ref(null)
const personLoading = ref(true)
const personError = ref(null)
const defaultPersonImage = '@/default-person.png'
const participantsCount = ref(0)

// 시간 변수
const missingTimeDB = ref(null)

// 주소
const missingAddress = ref(null)
let fullAddress = ''

// 유효 데이터 수
let less_data = ref(false)
let total_cluster = ref(null)
// ========================================================================================
// API 호출 함수 - 예측 데이터 가져오기
// ========================================================================================
let userNo = ref('')
let lessData = ref(false)

// ========================================================================================
// 1. 하드코딩 데이터 정의
// ========================================================================================

// (1) 예상 위치 데이터
const dummyData = ref({
    "user_no": 4,
    "missing_time": "2025-11-25T21:16:00",
    "last_known_location": {
        "latitude": 37.49448664,
        "longitude": 126.88772717,
        "time": "2025-11-25T21:14:00"
    },
    "destinations_by_distance": {
        "500m": [
            { "destination_id": 1, "latitude": 37.496825, "longitude": 126.88546, "visit_count": 53, "distance_meters": 328, "name": "구로중앙로21길에 있는 것 같아요!" },
            { "destination_id": 2, "latitude": 37.492829, "longitude": 126.887768, "visit_count": 46, "distance_meters": 184.3, "name": "구로중앙로에 있는 것 같아요!" },
            { "destination_id": 3, "latitude": 37.497062, "longitude": 126.892239, "visit_count": 36, "distance_meters": 490.4, "name": "구로동 공원에 있는 것 같아요!" },
            { "destination_id": 4, "latitude": 37.493286, "longitude": 126.884334, "visit_count": 33, "distance_meters": 327.8, "name": "신도림역 인근에 있는 것 같아요!" },
            { "destination_id": 5, "latitude": 37.495341, "longitude": 126.889028, "visit_count": 32, "distance_meters": 149, "name": "구로1동 주민센터에 있는 것 같아요!" }
        ],
        "1000m": [
            { "destination_id": 6, "latitude": 37.486667, "longitude": 126.888185, "visit_count": 45, "distance_meters": 870.5, "name": "구로디지털단지역에 있는 것 같아요!" },
            { "destination_id": 7, "latitude": 37.489739, "longitude": 126.887963, "visit_count": 45, "distance_meters": 528.4, "name": "구로구청에 있는 것 같아요!" },
            { "destination_id": 8, "latitude": 37.498004, "longitude": 126.884143, "visit_count": 38, "distance_meters": 502.9, "name": "구로근린공원에 있는 것 같아요!" },
            { "destination_id": 9, "latitude": 37.49082, "longitude": 126.891015, "visit_count": 33, "distance_meters": 500.3, "name": "구로시장에 있는 것 같아요!" },
            { "destination_id": 10, "latitude": 37.498343, "longitude": 126.893059, "visit_count": 30, "distance_meters": 636.5, "name": "구로문화원에 있는 것 같아요!" }
        ],
        "1500m": [
            { "destination_id": 11, "latitude": 37.485114, "longitude": 126.888224, "visit_count": 42, "distance_meters": 1043.1, "name": "구로공원에 있는 것 같아요!" },
            { "destination_id": 12, "latitude": 37.482068, "longitude": 126.888452, "visit_count": 42, "distance_meters": 1382.3, "name": "대림역에 있는 것 같아요!" },
            { "destination_id": 13, "latitude": 37.487777, "longitude": 126.895622, "visit_count": 14, "distance_meters": 1020.7, "name": "구로보건소에 있는 것 같아요!" },
            { "destination_id": 14, "latitude": 37.485769, "longitude": 126.898204, "visit_count": 13, "distance_meters": 1339.4, "name": "신대방역에 있는 것 같아요!" },
            { "destination_id": 15, "latitude": 37.489937, "longitude": 126.903709, "visit_count": 6, "distance_meters": 1498, "name": "여의대방로에 있는 것 같아요!" }
        ]
    },
    "total_clusters_found": 83
});

// (2) 하드코딩 경로 데이터 (route.txt 파일의 내용을 JS 객체로 변환)
// ※ 내용이 너무 길어 생략된 부분은 실제 route.txt의 JSON 배열 전체를 붙여넣으셔야 합니다.
const mockRouteData = {
    "500m": [
        // 1번째 경로 (route.txt의 500m 첫번째 features)
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":539,"totalTime":446,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 38m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 38m","distance":38,"time":29,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":4,"pointIndex":2,"name":"한우나주곰탕 구로점","description":"한우나주곰탕 구로점에서 좌회전 후 2m 이동","direction":"","nearPoiName":"한우나주곰탕 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.88752580102256,37.49401426294294]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 2m","distance":2,"time":2,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88752580102256,37.49401426294294]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88752580102256,37.49401426294294],[126.88750635866165,37.49400315277615]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 2m","distance":2,"time":1,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":8,"pointIndex":4,"name":"한우나주곰탕 구로점","description":"한우나주곰탕 구로점에서 우회전 후 38m 이동","direction":"","nearPoiName":"한우나주곰탕 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.8872563737094,37.49427811635943]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 38m","distance":38,"time":29,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":10,"pointIndex":5,"name":"도진빌딩","description":"도진빌딩에서 좌회전 후 도림로를 따라 5m 이동","direction":"","nearPoiName":"도진빌딩","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88720915649147,37.49425311842225]]},"properties":{"index":11,"lineIndex":5,"name":"도림로","description":"도림로, 5m","distance":5,"time":34,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88720915649147,37.49425311842225]},"properties":{"index":12,"pointIndex":6,"name":"도진빌딩","description":"도진빌딩에서 우측 횡단보도 후 보행자도로를 따라 30m 이동","direction":"","nearPoiName":"도진빌딩","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":213,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88720915649147,37.49425311842225],[126.88705083086992,37.494491976725925]]},"properties":{"index":13,"lineIndex":6,"name":"보행자도로","description":"보행자도로, 30m","distance":30,"time":20,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88705083086992,37.494491976725925]},"properties":{"index":14,"pointIndex":7,"name":"","description":"좌회전 후 도림로를 따라 11m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88705083086992,37.494491976725925],[126.88694528649127,37.49443642574342]]},"properties":{"index":15,"lineIndex":7,"name":"도림로","description":"도림로, 11m","distance":11,"time":8,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88694528649127,37.49443642574342],[126.88678141495184,37.494350321715615],[126.88669531262188,37.49431699071981],[126.88667309258015,37.49431143541346]]},"properties":{"index":16,"lineIndex":8,"name":"","description":", 28m","distance":28,"time":19,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88667309258015,37.49431143541346]},"properties":{"index":17,"pointIndex":8,"name":"","description":"경유지 후 가마산로를 따라 8m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88667309258015,37.49431143541346],[126.88663976251758,37.49430310245392],[126.88658976754077,37.49428643683215]]},"properties":{"index":18,"lineIndex":9,"name":"가마산로","description":"가마산로, 8m","distance":8,"time":6,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88658976754077,37.49428643683215]},"properties":{"index":19,"pointIndex":9,"name":"성보빌딩","description":"성보빌딩에서 우회전 후 80m 이동","direction":"","nearPoiName":"성보빌딩","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88658976754077,37.49428643683215],[126.88642033283517,37.49448918802292],[126.88625367581044,37.494686384353244],[126.88608424126102,37.494883580633946]]},"properties":{"index":20,"lineIndex":10,"name":"","description":", 80m","distance":80,"time":66,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88608424126102,37.494883580633946]},"properties":{"index":21,"pointIndex":10,"name":"","description":"경유지 후 73m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88608424126102,37.494883580633946],[126.88607035324718,37.494897467661076],[126.8858842529251,37.49511688328422],[126.88578425860106,37.495239089519266],[126.8857009302839,37.495330744046896],[126.88564259906484,37.49544461866022]]},"properties":{"index":22,"lineIndex":11,"name":"","description":", 73m","distance":73,"time":62,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88564259906484,37.49544461866022]},"properties":{"index":23,"pointIndex":11,"name":"보광아파트","description":"보광아파트에서 우회전 후 3m 이동","direction":"","nearPoiName":"보광아파트","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88564259906484,37.49544461866022],[126.88567315175872,37.495447396660246]]},"properties":{"index":24,"lineIndex":12,"name":"","description":", 3m","distance":3,"time":2,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88567315175872,37.495447396660246]},"properties":{"index":25,"pointIndex":12,"name":"","description":"경유지 후 3m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88567315175872,37.495447396660246],[126.88564259906484,37.49544461866022]]},"properties":{"index":26,"lineIndex":13,"name":"","description":", 3m","distance":3,"time":2,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88564259906484,37.49544461866022]},"properties":{"index":27,"pointIndex":13,"name":"보광아파트","description":"보광아파트에서 우회전 후 보행자도로를 따라 28m 이동","direction":"","nearPoiName":"보광아파트","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88564259906484,37.49544461866022],[126.88560926658289,37.49552238680486],[126.88559537810077,37.4955529385618],[126.88558981914899,37.49569181121128]]},"properties":{"index":28,"lineIndex":14,"name":"보행자도로","description":"보행자도로, 28m","distance":28,"time":23,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88558981914899,37.49569181121128]},"properties":{"index":29,"pointIndex":14,"name":"","description":"좌회전 후 16m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88558981914899,37.49569181121128],[126.88551760272578,37.4957195844727],[126.88542316743165,37.49570014060322]]},"properties":{"index":30,"lineIndex":15,"name":"","description":", 16m","distance":16,"time":13,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88542316743165,37.49570014060322]},"properties":{"index":31,"pointIndex":15,"name":"","description":"우회전 후 64m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88542316743165,37.49570014060322],[126.88541205467918,37.49579457387404],[126.88536205540991,37.495930668275705],[126.88535372189918,37.4959639975867],[126.88534538823237,37.496002881807634],[126.88532038535872,37.496186193389676],[126.88528983040148,37.49626396158376]]},"properties":{"index":32,"lineIndex":16,"name":"","description":", 64m","distance":64,"time":49,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88528983040148,37.49626396158376]},"properties":{"index":33,"pointIndex":16,"name":"","description":"경유지 후 63m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88528983040148,37.49626396158376],[126.88528427503985,37.49627507130454],[126.88523149910438,37.4963806136519],[126.8852176099198,37.49643616250352],[126.88516760745047,37.496686132558715],[126.88515371810979,37.49674723632023],[126.88515093863383,37.496816672644854]]},"properties":{"index":34,"lineIndex":17,"name":"","description":", 63m","distance":63,"time":48,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88515093863383,37.496816672644854]},"properties":{"index":35,"pointIndex":17,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        // 2번째 경로
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":371,"totalTime":281,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 38m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 38m","distance":38,"time":29,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":4,"pointIndex":2,"name":"한우나주곰탕 구로점","description":"한우나주곰탕 구로점에서 좌회전 후 59m 이동","direction":"","nearPoiName":"한우나주곰탕 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.88777577457991,37.49414480778668],[126.8880313031086,37.49427813018457],[126.88806463293706,37.494294795509084]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 59m","distance":59,"time":46,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88806463293706,37.494294795509084]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 29m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88806463293706,37.494294795509084],[126.8880313031086,37.49427813018457],[126.8877896619694,37.49415314039938]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 29m","distance":29,"time":22,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8877896619694,37.49415314039938]},"properties":{"index":8,"pointIndex":4,"name":"","description":"경유지 후 30m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8877896619694,37.49415314039938],[126.88777577457991,37.49414480778668],[126.88750635866165,37.49400315277615]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 30m","distance":30,"time":23,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":10,"pointIndex":5,"name":"미니스톱 구로구청점","description":"미니스톱 구로구청점에서 좌회전 후 37m 이동","direction":"","nearPoiName":"미니스톱 구로구청점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.88770634738839,37.49375596285064],[126.88771745857996,37.49371707867912]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 37m","distance":37,"time":28,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88771745857996,37.49371707867912]},"properties":{"index":12,"pointIndex":6,"name":"","description":"경유지 후 42m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88771745857996,37.49371707867912],[126.88782856987133,37.49335045660359]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 42m","distance":42,"time":32,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88782856987133,37.49335045660359]},"properties":{"index":14,"pointIndex":7,"name":"","description":"좌회전 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88782856987133,37.49335045660359],[126.88785078999113,37.493353234454986]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 2m","distance":2,"time":1,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88785078999113,37.493353234454986]},"properties":{"index":16,"pointIndex":8,"name":"","description":"경유지 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88785078999113,37.493353234454986],[126.88782856987133,37.49335045660359]]},"properties":{"index":17,"lineIndex":8,"name":"","description":", 2m","distance":2,"time":1,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88782856987133,37.49335045660359]},"properties":{"index":18,"pointIndex":9,"name":"","description":"좌회전 후 33m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88782856987133,37.49335045660359],[126.88791745877948,37.49306160287099]]},"properties":{"index":19,"lineIndex":9,"name":"","description":", 33m","distance":33,"time":25,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88791745877948,37.49306160287099]},"properties":{"index":20,"pointIndex":10,"name":"","description":"우회전 후 30m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88791745877948,37.49306160287099],[126.887592490571,37.49298382833394]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 30m","distance":30,"time":23,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.887592490571,37.49298382833394]},"properties":{"index":22,"pointIndex":11,"name":"","description":"경유지 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.887592490571,37.49298382833394],[126.88756749300448,37.49297827297802]]},"properties":{"index":23,"lineIndex":11,"name":"","description":", 2m","distance":2,"time":2,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88756749300448,37.49297827297802]},"properties":{"index":24,"pointIndex":12,"name":"","description":"좌회전 후 20m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88756749300448,37.49297827297802],[126.88761471584168,37.49280329415639]]},"properties":{"index":25,"lineIndex":12,"name":"","description":", 20m","distance":20,"time":16,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88761471584168,37.49280329415639]},"properties":{"index":26,"pointIndex":13,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        // 3번째 경로
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":808,"totalTime":759,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 38m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 38m","distance":38,"time":29,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":4,"pointIndex":2,"name":"한우나주곰탕 구로점","description":"한우나주곰탕 구로점에서 좌회전 후 84m 이동","direction":"","nearPoiName":"한우나주곰탕 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.88777577457991,37.49414480778668],[126.8880313031086,37.49427813018457],[126.88829794165811,37.494414230235705],[126.88830627415425,37.49441700783934]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 84m","distance":84,"time":66,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88830627415425,37.49441700783934]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 71m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88830627415425,37.49441700783934],[126.8886117976596,37.4945669958592],[126.8889812031409,37.49475586938862]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 71m","distance":71,"time":53,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8889812031409,37.49475586938862]},"properties":{"index":8,"pointIndex":4,"name":"신한은행 구로동지점","description":"신한은행 구로동지점에서 좌회전 후 구로중앙로를 따라 30m 이동","direction":"","nearPoiName":"신한은행 구로동지점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8889812031409,37.49475586938862],[126.88888120866012,37.494883630533614],[126.88872844417563,37.49490584744788]]},"properties":{"index":9,"lineIndex":4,"name":"구로중앙로","description":"구로중앙로, 30m","distance":30,"time":51,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88872844417563,37.49490584744788]},"properties":{"index":10,"pointIndex":5,"name":"신한은행 구로동지점","description":"신한은행 구로동지점에서 우측 횡단보도 후 20m 이동","direction":"","nearPoiName":"신한은행 구로동지점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"15","facilityName":"","turnType":213,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88872844417563,37.49490584744788],[126.888770102051,37.49508360530977]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 20m","distance":20,"time":14,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.888770102051,37.49508360530977]},"properties":{"index":12,"pointIndex":6,"name":"","description":"직진 후 10m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":11,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.888770102051,37.49508360530977],[126.88877843392268,37.49510860255323],[126.88884787282143,37.49508082924236]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 10m","distance":10,"time":37,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88884787282143,37.49508082924236]},"properties":{"index":14,"pointIndex":7,"name":"","description":"횡단보도 후 17m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88884787282143,37.49508082924236],[126.88900896691395,37.495164155765934]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 17m","distance":17,"time":11,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88900896691395,37.495164155765934]},"properties":{"index":16,"pointIndex":8,"name":"","description":"직진 후 11m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":11,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88900896691395,37.495164155765934],[126.88905896165653,37.49518915375274],[126.88912562217195,37.49519193239708]]},"properties":{"index":17,"lineIndex":8,"name":"","description":", 11m","distance":11,"time":38,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88912562217195,37.49519193239708]},"properties":{"index":18,"pointIndex":9,"name":"","description":"횡단보도 후 보행자도로를 따라 10m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88912562217195,37.49519193239708],[126.88924227805441,37.49519748938842]]},"properties":{"index":19,"lineIndex":9,"name":"보행자도로","description":"보행자도로, 10m","distance":10,"time":7,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88924227805441,37.49519748938842]},"properties":{"index":20,"pointIndex":10,"name":"대림오페라타운","description":"대림오페라타운에서 우회전 후 31m 이동","direction":"","nearPoiName":"대림오페라타운","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88924227805441,37.49519748938842],[126.88921450616361,37.495078058328815],[126.88932005585004,37.494944742373036]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 31m","distance":31,"time":22,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88932005585004,37.494944742373036]},"properties":{"index":22,"pointIndex":11,"name":"","description":"경유지 후 18m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88932005585004,37.494944742373036],[126.88921450616361,37.495078058328815]]},"properties":{"index":23,"lineIndex":11,"name":"","description":", 18m","distance":18,"time":12,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88921450616361,37.495078058328815]},"properties":{"index":24,"pointIndex":12,"name":"대림오페라타운","description":"대림오페라타운에서 우회전 후 구로중앙로를 따라 13m 이동","direction":"","nearPoiName":"대림오페라타운","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88921450616361,37.495078058328815],[126.88924227805441,37.49519748938842]]},"properties":{"index":25,"lineIndex":12,"name":"구로중앙로","description":"구로중앙로, 13m","distance":13,"time":9,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88924227805441,37.49519748938842],[126.8892783813485,37.495358582421325],[126.88930060123408,37.495369692637695]]},"properties":{"index":26,"lineIndex":13,"name":"","description":", 21m","distance":21,"time":14,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88930060123408,37.495369692637695]},"properties":{"index":27,"pointIndex":13,"name":"","description":"경유지 후 가마산로를 따라 138m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88930060123408,37.495369692637695],[126.88999497234619,37.49572799671919],[126.89056713416531,37.49602241715598],[126.89060879641171,37.4960446375392]]},"properties":{"index":28,"lineIndex":14,"name":"가마산로","description":"가마산로, 138m","distance":138,"time":129,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89060879641171,37.4960446375392]},"properties":{"index":29,"pointIndex":14,"name":"영등포수도사업소","description":"영등포수도사업소에서 좌측 횡단보도 후 12m 이동","direction":"","nearPoiName":"영등포수도사업소","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89060879641171,37.4960446375392],[126.89053102298703,37.49614184707563]]},"properties":{"index":30,"lineIndex":15,"name":"","description":", 12m","distance":12,"time":9,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89053102298703,37.49614184707563]},"properties":{"index":31,"pointIndex":15,"name":"","description":"경유지 후 보행자도로를 따라 13m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89053102298703,37.49614184707563],[126.89060879641171,37.4960446375392]]},"properties":{"index":32,"lineIndex":16,"name":"보행자도로","description":"보행자도로, 13m","distance":13,"time":8,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89060879641171,37.4960446375392]},"properties":{"index":33,"pointIndex":16,"name":"영등포수도사업소","description":"영등포수도사업소에서 좌회전 후 가마산로를 따라 152m 이동","direction":"","nearPoiName":"영등포수도사업소","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89060879641171,37.4960446375392],[126.8907004534006,37.49609185590929],[126.89186699692327,37.49669180700022],[126.89200587112977,37.496764023307776],[126.89205864370922,37.496777911524376]]},"properties":{"index":34,"lineIndex":17,"name":"가마산로","description":"가마산로, 152m","distance":152,"time":139,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89205864370922,37.496777911524376]},"properties":{"index":35,"pointIndex":17,"name":"컴포 구로구청점","description":"컴포 구로구청점에서 좌측 횡단보도 후 15m 이동","direction":"","nearPoiName":"컴포 구로구청점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89205864370922,37.496777911524376],[126.89196975963885,37.49689456304723]]},"properties":{"index":36,"lineIndex":18,"name":"","description":", 15m","distance":15,"time":10,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89196975963885,37.49689456304723]},"properties":{"index":37,"pointIndex":18,"name":"","description":"경유지 후 보행자도로를 따라 17m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89196975963885,37.49689456304723],[126.89186976507922,37.497025101646656]]},"properties":{"index":38,"lineIndex":19,"name":"보행자도로","description":"보행자도로, 17m","distance":17,"time":11,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89186976507922,37.497025101646656]},"properties":{"index":39,"pointIndex":19,"name":"금성볼링장","description":"금성볼링장에서 우회전 후 가마산로를 따라 11m 이동","direction":"","nearPoiName":"금성볼링장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89186976507922,37.497025101646656],[126.89188365246864,37.4970334342594],[126.89190587149534,37.49707509648052],[126.891914203523,37.49709453881403],[126.89191142545175,37.49711398094927]]},"properties":{"index":40,"lineIndex":20,"name":"가마산로","description":"가마산로, 11m","distance":11,"time":38,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89191142545175,37.49711398094927]},"properties":{"index":41,"pointIndex":20,"name":"채선당 대림점","description":"채선당 대림점에서 횡단보도 후 보행자도로를 따라 21m 이동","direction":"","nearPoiName":"채선당 대림점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89191142545175,37.49711398094927],[126.89211418202417,37.49721119549158]]},"properties":{"index":42,"lineIndex":21,"name":"보행자도로","description":"보행자도로, 21m","distance":21,"time":13,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89211418202417,37.49721119549158]},"properties":{"index":43,"pointIndex":21,"name":"KT텔레캅 본사","description":"KT텔레캅 본사에서 우회전 후 8m 이동","direction":"","nearPoiName":"KT텔레캅 본사","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89211418202417,37.49721119549158],[126.89219473094391,37.49718619983422]]},"properties":{"index":44,"lineIndex":22,"name":"","description":", 8m","distance":8,"time":6,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89219473094391,37.49718619983422]},"properties":{"index":45,"pointIndex":22,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        // 4번째 경로
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":592,"totalTime":485,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 38m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 38m","distance":38,"time":29,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":4,"pointIndex":2,"name":"한우나주곰탕 구로점","description":"한우나주곰탕 구로점에서 좌회전 후 30m 이동","direction":"","nearPoiName":"한우나주곰탕 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.88777577457991,37.49414480778668],[126.8877896619694,37.49415314039938]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 30m","distance":30,"time":23,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8877896619694,37.49415314039938]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 30m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8877896619694,37.49415314039938],[126.88777577457991,37.49414480778668],[126.88750635866165,37.49400315277615]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 30m","distance":30,"time":23,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":8,"pointIndex":4,"name":"한우나주곰탕 구로점","description":"한우나주곰탕 구로점에서 우회전 후 38m 이동","direction":"","nearPoiName":"한우나주곰탕 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.8872563737094,37.49427811635943]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 38m","distance":38,"time":29,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":10,"pointIndex":5,"name":"도진빌딩","description":"도진빌딩에서 좌회전 후 도림로를 따라 5m 이동","direction":"","nearPoiName":"도진빌딩","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88720915649147,37.49425311842225]]},"properties":{"index":11,"lineIndex":5,"name":"도림로","description":"도림로, 5m","distance":5,"time":33,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88720915649147,37.49425311842225]},"properties":{"index":12,"pointIndex":6,"name":"도진빌딩","description":"도진빌딩에서 우측 횡단보도 후 16m 이동","direction":"","nearPoiName":"도진빌딩","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":213,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88720915649147,37.49425311842225],[126.88712304955669,37.494383657270134]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 16m","distance":16,"time":11,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88712304955669,37.494383657270134]},"properties":{"index":14,"pointIndex":7,"name":"","description":"경유지 후 보행자도로를 따라 14m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88712304955669,37.494383657270134],[126.88705083086992,37.494491976725925]]},"properties":{"index":15,"lineIndex":7,"name":"보행자도로","description":"보행자도로, 14m","distance":14,"time":9,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88705083086992,37.494491976725925]},"properties":{"index":16,"pointIndex":8,"name":"","description":"좌회전 후 도림로를 따라 11m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88705083086992,37.494491976725925],[126.88694528649127,37.49443642574342]]},"properties":{"index":17,"lineIndex":8,"name":"도림로","description":"도림로, 11m","distance":11,"time":8,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88694528649127,37.49443642574342],[126.88678141495184,37.494350321715615],[126.88669531262188,37.49431699071981],[126.88667309258015,37.49431143541346]]},"properties":{"index":18,"lineIndex":9,"name":"","description":", 28m","distance":28,"time":19,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88667309258015,37.49431143541346]},"properties":{"index":19,"pointIndex":9,"name":"","description":"경유지 후 가마산로를 따라 108m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88667309258015,37.49431143541346],[126.88663976251758,37.49430310245392],[126.88658976754077,37.49428643683215],[126.88654532761342,37.494269771309476],[126.88588428609374,37.4939364649194],[126.88583706864164,37.49391979934721],[126.88580096121039,37.4939059114282],[126.8856843068888,37.493844805337666],[126.88564264464222,37.49382258495462],[126.88561764715381,37.49381425214376]]},"properties":{"index":20,"lineIndex":10,"name":"가마산로","description":"가마산로, 108m","distance":108,"time":108,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88561764715381,37.49381425214376]},"properties":{"index":21,"pointIndex":10,"name":"덕우약국","description":"덕우약국에서 좌측 횡단보도 후 13m 이동","direction":"","nearPoiName":"덕우약국","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88561764715381,37.49381425214376],[126.88568153318805,37.493708709994166]]},"properties":{"index":22,"lineIndex":11,"name":"","description":", 13m","distance":13,"time":9,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88568153318805,37.493708709994166]},"properties":{"index":23,"pointIndex":11,"name":"","description":"경유지 후 보행자도로를 따라 17m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88568153318805,37.493708709994166],[126.88576486283186,37.49356983873164]]},"properties":{"index":24,"lineIndex":12,"name":"보행자도로","description":"보행자도로, 17m","distance":17,"time":11,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88576486283186,37.49356983873164]},"properties":{"index":25,"pointIndex":12,"name":"후문약국","description":"후문약국에서 우회전 후 164m 이동","direction":"","nearPoiName":"후문약국","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88576486283186,37.49356983873164],[126.88542045327742,37.49344484711371],[126.88538156839945,37.49342818169018],[126.88530657609024,37.493397628347644],[126.88486217494327,37.49329763204103],[126.88430667358756,37.49316985920302],[126.88401503544215,37.49310041762645]]},"properties":{"index":26,"lineIndex":13,"name":"","description":", 164m","distance":164,"time":117,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88401503544215,37.49310041762645]},"properties":{"index":27,"pointIndex":13,"name":"","description":"경유지 후 가마산로를 따라 33m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88401503544215,37.49310041762645],[126.88430667358756,37.49316985920302],[126.88436777874138,37.49318374756789]]},"properties":{"index":28,"lineIndex":14,"name":"가마산로","description":"가마산로, 33m","distance":33,"time":23,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88436777874138,37.49318374756789]},"properties":{"index":29,"pointIndex":14,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        // 5번째 경로
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":440,"totalTime":468,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 38m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 38m","distance":38,"time":29,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":4,"pointIndex":2,"name":"한우나주곰탕 구로점","description":"한우나주곰탕 구로점에서 좌회전 후 59m 이동","direction":"","nearPoiName":"한우나주곰탕 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.88777577457991,37.49414480778668],[126.8880313031086,37.49427813018457],[126.88806463293706,37.494294795509084]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 59m","distance":59,"time":46,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88806463293706,37.494294795509084]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 25m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88806463293706,37.494294795509084],[126.88829794165811,37.494414230235705],[126.88830627415425,37.49441700783934]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 25m","distance":25,"time":19,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88830627415425,37.49441700783934]},"properties":{"index":8,"pointIndex":4,"name":"","description":"경유지 후 34m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88830627415425,37.49441700783934],[126.8886117976596,37.4945669958592],[126.88862846257383,37.49457532852147]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 34m","distance":34,"time":25,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88862846257383,37.49457532852147]},"properties":{"index":10,"pointIndex":5,"name":"","description":"경유지 후 37m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88862846257383,37.49457532852147],[126.8889812031409,37.49475586938862]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 37m","distance":37,"time":28,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8889812031409,37.49475586938862]},"properties":{"index":12,"pointIndex":6,"name":"신한은행 구로동지점","description":"신한은행 구로동지점에서 좌회전 후 구로중앙로를 따라 30m 이동","direction":"","nearPoiName":"신한은행 구로동지점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8889812031409,37.49475586938862],[126.88888120866012,37.494883630533614],[126.88872844417563,37.49490584744788]]},"properties":{"index":13,"lineIndex":6,"name":"구로중앙로","description":"구로중앙로, 30m","distance":30,"time":52,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88872844417563,37.49490584744788]},"properties":{"index":14,"pointIndex":7,"name":"신한은행 구로동지점","description":"신한은행 구로동지점에서 우측 횡단보도 후 20m 이동","direction":"","nearPoiName":"신한은행 구로동지점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"15","facilityName":"","turnType":213,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88872844417563,37.49490584744788],[126.888770102051,37.49508360530977]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 20m","distance":20,"time":13,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.888770102051,37.49508360530977]},"properties":{"index":16,"pointIndex":8,"name":"","description":"직진 후 10m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":11,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.888770102051,37.49508360530977],[126.88877843392268,37.49510860255323],[126.88884787282143,37.49508082924236]]},"properties":{"index":17,"lineIndex":8,"name":"","description":", 10m","distance":10,"time":37,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88884787282143,37.49508082924236]},"properties":{"index":18,"pointIndex":9,"name":"","description":"횡단보도 후 17m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88884787282143,37.49508082924236],[126.88900896691395,37.495164155765934]]},"properties":{"index":19,"lineIndex":9,"name":"","description":", 17m","distance":17,"time":11,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88900896691395,37.495164155765934]},"properties":{"index":20,"pointIndex":10,"name":"","description":"직진 후 11m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":11,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88900896691395,37.495164155765934],[126.88905896165653,37.49518915375274],[126.88912562217195,37.49519193239708]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 11m","distance":11,"time":38,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88912562217195,37.49519193239708]},"properties":{"index":22,"pointIndex":11,"name":"","description":"횡단보도 후 보행자도로를 따라 10m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88912562217195,37.49519193239708],[126.88924227805441,37.49519748938842]]},"properties":{"index":23,"lineIndex":11,"name":"보행자도로","description":"보행자도로, 10m","distance":10,"time":7,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88924227805441,37.49519748938842]},"properties":{"index":24,"pointIndex":12,"name":"대림오페라타운","description":"대림오페라타운에서 우회전 후 31m 이동","direction":"","nearPoiName":"대림오페라타운","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88924227805441,37.49519748938842],[126.88921450616361,37.495078058328815],[126.88932005585004,37.494944742373036]]},"properties":{"index":25,"lineIndex":12,"name":"","description":", 31m","distance":31,"time":22,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88932005585004,37.494944742373036]},"properties":{"index":26,"pointIndex":13,"name":"","description":"경유지 후 구로중앙로를 따라 31m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88932005585004,37.494944742373036],[126.88921450616361,37.495078058328815],[126.88924227805441,37.49519748938842]]},"properties":{"index":27,"lineIndex":13,"name":"구로중앙로","description":"구로중앙로, 31m","distance":31,"time":51,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88924227805441,37.49519748938842]},"properties":{"index":28,"pointIndex":14,"name":"대림오페라타운","description":"대림오페라타운에서 좌측 횡단보도 후 11m 이동","direction":"","nearPoiName":"대림오페라타운","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88924227805441,37.49519748938842],[126.88912562217195,37.49519193239708]]},"properties":{"index":29,"lineIndex":14,"name":"","description":", 11m","distance":11,"time":7,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88912562217195,37.49519193239708]},"properties":{"index":30,"pointIndex":15,"name":"","description":"직진 후 16m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":11,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88912562217195,37.49519193239708],[126.88905896165653,37.49518915375274],[126.88900618626694,37.49527525391545]]},"properties":{"index":31,"lineIndex":15,"name":"","description":", 16m","distance":16,"time":42,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88900618626694,37.49527525391545]},"properties":{"index":32,"pointIndex":16,"name":"","description":"횡단보도 후 9m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88900618626694,37.49527525391545],[126.88894507900568,37.4953363568347]]},"properties":{"index":33,"lineIndex":16,"name":"","description":", 9m","distance":9,"time":6,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88894507900568,37.4953363568347]},"properties":{"index":34,"pointIndex":17,"name":"","description":"경유지 후 보행자도로를 따라 4m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88894507900568,37.4953363568347],[126.88897285503353,37.495308582780495]]},"properties":{"index":35,"lineIndex":17,"name":"보행자도로","description":"보행자도로, 4m","distance":4,"time":2,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88897285503353,37.495308582780495]},"properties":{"index":36,"pointIndex":18,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}]
    ],
    "1000m": [
        // 1번째 ~ 5번째 features 배열을 순서대로 넣으세요
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":1077,"totalTime":853,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 117m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615],[126.88770634738839,37.49375596285064],[126.88782856987133,37.49335045660359]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 117m","distance":117,"time":90,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88782856987133,37.49335045660359]},"properties":{"index":4,"pointIndex":2,"name":"","description":"좌회전 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88782856987133,37.49335045660359],[126.88785078999113,37.493353234454986]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 2m","distance":2,"time":2,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88785078999113,37.493353234454986]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88785078999113,37.493353234454986],[126.88782856987133,37.49335045660359]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 2m","distance":2,"time":1,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88782856987133,37.49335045660359]},"properties":{"index":8,"pointIndex":4,"name":"","description":"좌회전 후 227m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88782856987133,37.49335045660359],[126.88791745877948,37.49306160287099],[126.8879341251766,37.49301716388856],[126.88805079190752,37.49263665463681],[126.88832023092743,37.4919561829715],[126.8884674487914,37.49163400081899],[126.8885618889248,37.49148124247929],[126.88855633629458,37.49139514127547]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 227m","distance":227,"time":175,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88855633629458,37.49139514127547]},"properties":{"index":10,"pointIndex":5,"name":"구로2동공영주차장","description":"구로2동공영주차장에서 2시 방향 우회전 후 38m 이동","direction":"","nearPoiName":"구로2동공영주차장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":18,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88855633629458,37.49139514127547],[126.88828136563755,37.49124237634544],[126.8882147060583,37.491206268241356],[126.88820359619346,37.49119793567818]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 38m","distance":38,"time":29,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88820359619346,37.49119793567818]},"properties":{"index":12,"pointIndex":6,"name":"","description":"경유지 후 53m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88820359619346,37.49119793567818],[126.88802028276021,37.491084056753245],[126.88777864239988,37.49093129241792],[126.88773975775584,37.49090629462931]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 53m","distance":53,"time":40,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88773975775584,37.49090629462931]},"properties":{"index":14,"pointIndex":7,"name":"하루애PC방 구로","description":"하루애PC방 구로에서 좌회전 후 113m 이동","direction":"","nearPoiName":"하루애PC방 구로","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88773975775584,37.49090629462931],[126.8877258717709,37.49084796782674],[126.88773698460112,37.490750757100216],[126.88774254074309,37.490711872829415],[126.88785087263578,37.49041190962258],[126.88791753869198,37.49021748896216],[126.88800087020877,37.490011958779],[126.88801753613757,37.489984184526335],[126.88808975256124,37.48995641126464]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 113m","distance":113,"time":94,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88808975256124,37.48995641126464]},"properties":{"index":16,"pointIndex":8,"name":"소문난순대집","description":"소문난순대집에서 10시 방향 좌회전 후 5m 이동","direction":"","nearPoiName":"소문난순대집","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":17,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88808975256124,37.48995641126464],[126.88814530250987,37.48997585444058]]},"properties":{"index":17,"lineIndex":8,"name":"","description":", 5m","distance":5,"time":4,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88814530250987,37.48997585444058]},"properties":{"index":18,"pointIndex":9,"name":"","description":"경유지 후 5m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88814530250987,37.48997585444058],[126.88808975256124,37.48995641126464]]},"properties":{"index":19,"lineIndex":9,"name":"","description":", 5m","distance":5,"time":4,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88808975256124,37.48995641126464]},"properties":{"index":20,"pointIndex":10,"name":"주영수산","description":"주영수산에서 좌회전 후 58m 이동","direction":"","nearPoiName":"주영수산","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88808975256124,37.48995641126464],[126.8881091972633,37.48988419778159],[126.88810919765348,37.48987031050661],[126.88810364432072,37.4898092063976],[126.88792866993779,37.48946479885649]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 58m","distance":58,"time":44,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88792866993779,37.48946479885649]},"properties":{"index":22,"pointIndex":11,"name":"","description":"좌회전 후 115m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88792866993779,37.48946479885649],[126.8880036670855,37.4893231499894],[126.88806478012084,37.489056515399604],[126.88809255887965,37.48893153042011],[126.88817033433239,37.48876210705246],[126.88827588355004,37.48864545582524],[126.88838143284575,37.48852602714301],[126.88838976573211,37.48851491747164]]},"properties":{"index":23,"lineIndex":11,"name":"","description":", 115m","distance":115,"time":88,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88838976573211,37.48851491747164]},"properties":{"index":24,"pointIndex":12,"name":"","description":"경유지 후 75m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88838976573211,37.48851491747164],[126.88858697732407,37.48825384021935],[126.88863141928064,37.48819829191206],[126.88868141652081,37.488134411338855],[126.88872863631427,37.48806775326109],[126.88885362804922,37.4879566572906]]},"properties":{"index":25,"lineIndex":12,"name":"","description":", 75m","distance":75,"time":57,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88885362804922,37.4879566572906]},"properties":{"index":26,"pointIndex":13,"name":"GS25 남구로점","description":"GS25 남구로점에서 우회전 후 도림로를 따라 35m 이동","direction":"","nearPoiName":"GS25 남구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88885362804922,37.4879566572906],[126.88882863110675,37.48792888229463],[126.8886369859563,37.48778445121563],[126.88857032676673,37.487734455836424]]},"properties":{"index":27,"lineIndex":13,"name":"도림로","description":"도림로, 35m","distance":35,"time":55,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88857032676673,37.487734455836424]},"properties":{"index":28,"pointIndex":14,"name":"","description":"좌측 횡단보도 후 보행자도로를 따라 23m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88857032676673,37.487734455836424],[126.8887592025853,37.48758725409038]]},"properties":{"index":29,"lineIndex":14,"name":"보행자도로","description":"보행자도로, 23m","distance":23,"time":16,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8887592025853,37.48758725409038]},"properties":{"index":30,"pointIndex":15,"name":"기아자동차 가리봉테크노대리점","description":"기아자동차 가리봉테크노대리점에서 우회전 후 도림로를 따라 78m 이동","direction":"","nearPoiName":"기아자동차 가리봉테크노대리점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8887592025853,37.48758725409038],[126.88872865035921,37.48756781136032],[126.88870643062934,37.48755114623392],[126.88867865584997,37.48753448100841],[126.88862310707148,37.48747337600735],[126.88861199744063,37.487456711079126],[126.88856200347756,37.48740393854218],[126.88849812228084,37.48733727848247],[126.88837591478081,37.48720951337216],[126.88831203358404,37.48714285331245],[126.8882481524653,37.48707341579773],[126.8882176007073,37.48703730833765]]},"properties":{"index":31,"lineIndex":15,"name":"도림로","description":"도림로, 78m","distance":78,"time":55,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8882176007073,37.48703730833765]},"properties":{"index":32,"pointIndex":16,"name":"월드공인중개사","description":"월드공인중개사에서 좌회전 후 31m 이동","direction":"","nearPoiName":"월드공인중개사","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8882176007073,37.48703730833765],[126.88835925618626,37.48697620685432],[126.88852035465062,37.486903995897805]]},"properties":{"index":33,"lineIndex":16,"name":"","description":", 31m","distance":31,"time":24,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88852035465062,37.486903995897805]},"properties":{"index":34,"pointIndex":17,"name":"","description":"우회전 후 30m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88852035465062,37.486903995897805],[126.88847313751002,37.48687622050545],[126.88843980853935,37.48682900317577],[126.88842036656827,37.48680400573388],[126.88837870556948,37.486737346070505],[126.88835093157027,37.48669290629493],[126.88833982186136,37.4866790188217]]},"properties":{"index":35,"lineIndex":17,"name":"","description":", 30m","distance":30,"time":22,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88833982186136,37.4866790188217]},"properties":{"index":36,"pointIndex":18,"name":"","description":"경유지 후 1m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88833982186136,37.4866790188217],[126.88833426696789,37.48667346381259]]},"properties":{"index":37,"lineIndex":18,"name":"","description":", 1m","distance":1,"time":1,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88833426696789,37.48667346381259]},"properties":{"index":38,"pointIndex":19,"name":"","description":"우회전 후 보행자도로를 따라 14m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88833426696789,37.48667346381259],[126.8881898337301,37.48674289761143]]},"properties":{"index":39,"lineIndex":19,"name":"보행자도로","description":"보행자도로, 14m","distance":14,"time":13,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8881898337301,37.48674289761143]},"properties":{"index":40,"pointIndex":20,"name":"","description":"좌회전 후 8m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8881898337301,37.48674289761143],[126.88814539489445,37.48668734771855]]},"properties":{"index":41,"lineIndex":20,"name":"","description":", 8m","distance":8,"time":6,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88814539489445,37.48668734771855]},"properties":{"index":42,"pointIndex":21,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":703,"totalTime":542,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 38m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 38m","distance":38,"time":29,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":4,"pointIndex":2,"name":"한우나주곰탕 구로점","description":"한우나주곰탕 구로점에서 좌회전 후 30m 이동","direction":"","nearPoiName":"한우나주곰탕 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.88777577457991,37.49414480778668],[126.8877896619694,37.49415314039938]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 30m","distance":30,"time":23,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8877896619694,37.49415314039938]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 30m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8877896619694,37.49415314039938],[126.88777577457991,37.49414480778668],[126.88750635866165,37.49400315277615]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 30m","distance":30,"time":23,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":8,"pointIndex":4,"name":"미니스톱 구로구청점","description":"미니스톱 구로구청점에서 좌회전 후 79m 이동","direction":"","nearPoiName":"미니스톱 구로구청점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.88770634738839,37.49375596285064],[126.88782856987133,37.49335045660359]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 79m","distance":79,"time":60,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88782856987133,37.49335045660359]},"properties":{"index":10,"pointIndex":5,"name":"","description":"좌회전 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88782856987133,37.49335045660359],[126.88785078999113,37.493353234454986]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 2m","distance":2,"time":2,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88785078999113,37.493353234454986]},"properties":{"index":12,"pointIndex":6,"name":"","description":"경유지 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88785078999113,37.493353234454986],[126.88782856987133,37.49335045660359]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 2m","distance":2,"time":1,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88782856987133,37.49335045660359]},"properties":{"index":14,"pointIndex":7,"name":"","description":"좌회전 후 160m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88782856987133,37.49335045660359],[126.88791745877948,37.49306160287099],[126.8879341251766,37.49301716388856],[126.88805079190752,37.49263665463681],[126.88831745316857,37.49196451528692]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 160m","distance":160,"time":122,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88831745316857,37.49196451528692]},"properties":{"index":16,"pointIndex":8,"name":"","description":"경유지 후 67m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88831745316857,37.49196451528692],[126.88832023092743,37.4919561829715],[126.8884674487914,37.49163400081899],[126.8885618889248,37.49148124247929],[126.88855633629458,37.49139514127547]]},"properties":{"index":17,"lineIndex":8,"name":"","description":", 67m","distance":67,"time":52,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88855633629458,37.49139514127547]},"properties":{"index":18,"pointIndex":9,"name":"구로2동공영주차장","description":"구로2동공영주차장에서 2시 방향 우회전 후 84m 이동","direction":"","nearPoiName":"구로2동공영주차장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":18,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88855633629458,37.49139514127547],[126.88828136563755,37.49124237634544],[126.8882147060583,37.491206268241356],[126.88802028276021,37.491084056753245],[126.88780086220763,37.49094518008927]]},"properties":{"index":19,"lineIndex":9,"name":"","description":", 84m","distance":84,"time":65,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88780086220763,37.49094518008927]},"properties":{"index":20,"pointIndex":10,"name":"","description":"경유지 후 7m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88780086220763,37.49094518008927],[126.88777864239988,37.49093129241792],[126.88773975775584,37.49090629462931]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 7m","distance":7,"time":5,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88773975775584,37.49090629462931]},"properties":{"index":22,"pointIndex":11,"name":"하루애PC방 구로","description":"하루애PC방 구로에서 좌회전 후 113m 이동","direction":"","nearPoiName":"하루애PC방 구로","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88773975775584,37.49090629462931],[126.8877258717709,37.49084796782674],[126.88773698460112,37.490750757100216],[126.88774254074309,37.490711872829415],[126.88785087263578,37.49041190962258],[126.88791753869198,37.49021748896216],[126.88800087020877,37.490011958779],[126.88801753613757,37.489984184526335],[126.88808975256124,37.48995641126464]]},"properties":{"index":23,"lineIndex":11,"name":"","description":", 113m","distance":113,"time":94,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88808975256124,37.48995641126464]},"properties":{"index":24,"pointIndex":12,"name":"소문난순대집","description":"소문난순대집에서 4시 방향 우회전 후 8m 이동","direction":"","nearPoiName":"소문난순대집","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":19,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88808975256124,37.48995641126464],[126.88801476040759,37.489920303011885]]},"properties":{"index":25,"lineIndex":12,"name":"","description":", 8m","distance":8,"time":6,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88801476040759,37.489920303011885]},"properties":{"index":26,"pointIndex":13,"name":"","description":"경유지 후 7m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88801476040759,37.489920303011885],[126.88808975256124,37.48995641126464]]},"properties":{"index":27,"lineIndex":13,"name":"","description":", 7m","distance":7,"time":5,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88808975256124,37.48995641126464]},"properties":{"index":28,"pointIndex":14,"name":"청암한의원","description":"청암한의원에서 우회전 후 29m 이동","direction":"","nearPoiName":"청암한의원","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88808975256124,37.48995641126464],[126.8881091972633,37.48988419778159],[126.88810919765348,37.48987031050661],[126.88810364432072,37.4898092063976],[126.88805365168452,37.48970921712586]]},"properties":{"index":29,"lineIndex":14,"name":"","description":", 29m","distance":29,"time":22,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88805365168452,37.48970921712586]},"properties":{"index":30,"pointIndex":15,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":679,"totalTime":582,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88720915649147,37.49425311842225]]},"properties":{"index":2,"lineIndex":1,"name":"도림로","description":"도림로, 5m","distance":5,"time":34,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88720915649147,37.49425311842225]},"properties":{"index":3,"pointIndex":1,"name":"도진빌딩","description":"도진빌딩에서 우측 횡단보도 후 15m 이동","direction":"","nearPoiName":"도진빌딩","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":213,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88720915649147,37.49425311842225],[126.88713416012388,37.49436699273847]]},"properties":{"index":4,"lineIndex":2,"name":"","description":", 15m","distance":15,"time":10,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88713416012388,37.49436699273847]},"properties":{"index":5,"pointIndex":2,"name":"","description":"경유지 후 보행자도로를 따라 15m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88713416012388,37.49436699273847],[126.88705083086992,37.494491976725925]]},"properties":{"index":6,"lineIndex":3,"name":"보행자도로","description":"보행자도로, 15m","distance":15,"time":10,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88705083086992,37.494491976725925]},"properties":{"index":7,"pointIndex":3,"name":"","description":"좌회전 후 도림로를 따라 11m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88705083086992,37.494491976725925],[126.88694528649127,37.49443642574342]]},"properties":{"index":8,"lineIndex":4,"name":"도림로","description":"도림로, 11m","distance":11,"time":8,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88694528649127,37.49443642574342],[126.88678141495184,37.494350321715615],[126.88669531262188,37.49431699071981],[126.88663976251758,37.49430310245392],[126.88658976754077,37.49428643683215]]},"properties":{"index":9,"lineIndex":5,"name":"가마산로","description":"가마산로, 36m","distance":36,"time":25,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88658976754077,37.49428643683215]},"properties":{"index":10,"pointIndex":4,"name":"성보빌딩","description":"성보빌딩에서 우회전 후 80m 이동","direction":"","nearPoiName":"성보빌딩","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88658976754077,37.49428643683215],[126.88642033283517,37.49448918802292],[126.88625367581044,37.494686384353244],[126.88608424126102,37.494883580633946]]},"properties":{"index":11,"lineIndex":6,"name":"","description":", 80m","distance":80,"time":66,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88608424126102,37.494883580633946]},"properties":{"index":12,"pointIndex":5,"name":"","description":"경유지 후 보행자도로를 따라 102m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88608424126102,37.494883580633946],[126.88607035324718,37.494897467661076],[126.8858842529251,37.49511688328422],[126.88578425860106,37.495239089519266],[126.8857009302839,37.495330744046896],[126.88564259906484,37.49544461866022],[126.88560926658289,37.49552238680486],[126.88559537810077,37.4955529385618],[126.88558981914899,37.49569181121128]]},"properties":{"index":13,"lineIndex":7,"name":"보행자도로","description":"보행자도로, 102m","distance":102,"time":85,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88558981914899,37.49569181121128]},"properties":{"index":14,"pointIndex":6,"name":"","description":"좌회전 후 10m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88558981914899,37.49569181121128],[126.88551760272578,37.4957195844727],[126.88548427258524,37.495714028968166]]},"properties":{"index":15,"lineIndex":8,"name":"","description":", 10m","distance":10,"time":8,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88548427258524,37.495714028968166]},"properties":{"index":16,"pointIndex":7,"name":"","description":"경유지 후 6m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88548427258524,37.495714028968166],[126.88542316743165,37.49570014060322]]},"properties":{"index":17,"lineIndex":9,"name":"","description":", 6m","distance":6,"time":5,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88542316743165,37.49570014060322]},"properties":{"index":18,"pointIndex":8,"name":"","description":"우회전 후 213m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88542316743165,37.49570014060322],[126.88541205467918,37.49579457387404],[126.88536205540991,37.495930668275705],[126.88535372189918,37.4959639975867],[126.88534538823237,37.496002881807634],[126.88532038535872,37.496186193389676],[126.88528427503985,37.49627507130454],[126.88523149910438,37.4963806136519],[126.8852176099198,37.49643616250352],[126.88516760745047,37.496686132558715],[126.88515371810979,37.49674723632023],[126.88515093863383,37.496816672644854],[126.88513426747564,37.49703053637995],[126.885125932638,37.49711108242531],[126.88512870501131,37.49729439450256],[126.8851231481667,37.49735827586765],[126.88510092273945,37.497544364953846],[126.88508981123566,37.49759435894499]]},"properties":{"index":19,"lineIndex":10,"name":"","description":", 213m","distance":213,"time":164,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88508981123566,37.49759435894499]},"properties":{"index":20,"pointIndex":9,"name":"구로순복음교회","description":"구로순복음교회에서 좌회전 후 11m 이동","direction":"","nearPoiName":"구로순복음교회","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88508981123566,37.49759435894499],[126.88498426404773,37.497638796341484]]},"properties":{"index":21,"lineIndex":11,"name":"","description":", 11m","distance":11,"time":9,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88498426404773,37.497638796341484]},"properties":{"index":22,"pointIndex":10,"name":"구로순복음교회","description":"구로순복음교회에서 우회전 후 2m 이동","direction":"","nearPoiName":"구로순복음교회","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88498426404773,37.497638796341484],[126.8849925960755,37.49765823867487]]},"properties":{"index":23,"lineIndex":12,"name":"","description":", 2m","distance":2,"time":2,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8849925960755,37.49765823867487]},"properties":{"index":24,"pointIndex":11,"name":"","description":"경유지 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8849925960755,37.49765823867487],[126.88498426404773,37.497638796341484]]},"properties":{"index":25,"lineIndex":13,"name":"","description":", 2m","distance":2,"time":1,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88498426404773,37.497638796341484]},"properties":{"index":26,"pointIndex":12,"name":"구로순복음교회","description":"구로순복음교회에서 우회전 후 24m 이동","direction":"","nearPoiName":"구로순복음교회","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88498426404773,37.497638796341484],[126.88474817155928,37.49774155796334]]},"properties":{"index":27,"lineIndex":14,"name":"","description":", 24m","distance":24,"time":19,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88474817155928,37.49774155796334]},"properties":{"index":28,"pointIndex":13,"name":"","description":"우회전 후 7m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88474817155928,37.49774155796334],[126.88480927601019,37.49778044342292]]},"properties":{"index":29,"lineIndex":15,"name":"","description":", 7m","distance":7,"time":35,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88480927601019,37.49778044342292]},"properties":{"index":30,"pointIndex":14,"name":"","description":"좌측 횡단보도 후 보행자도로를 따라 24m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88480927601019,37.49778044342292],[126.88458151547151,37.497905424833114]]},"properties":{"index":31,"lineIndex":16,"name":"보행자도로","description":"보행자도로, 24m","distance":24,"time":16,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88458151547151,37.497905424833114]},"properties":{"index":32,"pointIndex":15,"name":"멕시카나 구로점","description":"멕시카나 구로점에서 좌회전 후 11m 이동","direction":"","nearPoiName":"멕시카나 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88458151547151,37.497905424833114],[126.88456207326686,37.49788875975649],[126.88447597093744,37.49785542876091]]},"properties":{"index":33,"lineIndex":17,"name":"","description":", 11m","distance":11,"time":8,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88447597093744,37.49785542876091]},"properties":{"index":34,"pointIndex":16,"name":"멕시카나 구로점","description":"멕시카나 구로점에서 2시 방향 우회전 후 45m 이동","direction":"","nearPoiName":"멕시카나 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":18,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88447597093744,37.49785542876091],[126.88424543412299,37.49793597084219],[126.88402045212386,37.49802484538747]]},"properties":{"index":35,"lineIndex":18,"name":"","description":", 45m","distance":45,"time":34,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88402045212386,37.49802484538747]},"properties":{"index":36,"pointIndex":17,"name":"진흥안경원","description":"진흥안경원에서 우회전 후 1m 이동","direction":"","nearPoiName":"진흥안경원","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88402045212386,37.49802484538747],[126.88402322933638,37.49803595525686]]},"properties":{"index":37,"lineIndex":19,"name":"","description":", 1m","distance":1,"time":1,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88402322933638,37.49803595525686]},"properties":{"index":38,"pointIndex":18,"name":"","description":"경유지 후 1m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88402322933638,37.49803595525686],[126.88402045212386,37.49802484538747]]},"properties":{"index":39,"lineIndex":20,"name":"","description":", 1m","distance":1,"time":0,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88402045212386,37.49802484538747]},"properties":{"index":40,"pointIndex":19,"name":"진흥안경원","description":"진흥안경원에서 좌회전 후 11m 이동","direction":"","nearPoiName":"진흥안경원","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88402045212386,37.49802484538747],[126.88412877675832,37.497983185495464]]},"properties":{"index":41,"lineIndex":21,"name":"","description":", 11m","distance":11,"time":9,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88412877675832,37.497983185495464]},"properties":{"index":42,"pointIndex":20,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":663,"totalTime":515,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 71m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615],[126.88770634738839,37.49375596285064]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 71m","distance":71,"time":54,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770634738839,37.49375596285064]},"properties":{"index":4,"pointIndex":2,"name":"미니스톱 구로구청점","description":"미니스톱 구로구청점에서 좌회전 후 64m 이동","direction":"","nearPoiName":"미니스톱 구로구청점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770634738839,37.49375596285064],[126.88803964895118,37.49380596298659],[126.88813130742338,37.49380040971187],[126.88837572842898,37.49384207589728],[126.88842016874662,37.49384485414512]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 64m","distance":64,"time":50,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88842016874662,37.49384485414512]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 24m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88842016874662,37.49384485414512],[126.88853960207592,37.49385318864088],[126.88869514260233,37.493883743420696]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 24m","distance":24,"time":19,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88869514260233,37.493883743420696]},"properties":{"index":8,"pointIndex":4,"name":"","description":"우회전 후 15m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88869514260233,37.493883743420696],[126.88879791382725,37.49378375687486]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 15m","distance":15,"time":11,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88879791382725,37.49378375687486]},"properties":{"index":10,"pointIndex":5,"name":"","description":"경유지 후 75m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88879791382725,37.49378375687486],[126.8888451329965,37.49373931843757],[126.8892951062866,37.49323105220344]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 75m","distance":75,"time":58,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8892951062866,37.49323105220344]},"properties":{"index":12,"pointIndex":6,"name":"","description":"우회전 후 77m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8892951062866,37.49323105220344],[126.88925344396192,37.493211609275235],[126.88923955883584,37.493122730467945],[126.88928956473967,37.492750552391826],[126.88935623048454,37.492567241552]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 77m","distance":77,"time":63,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88935623048454,37.492567241552]},"properties":{"index":14,"pointIndex":7,"name":"","description":"경유지 후 113m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88935623048454,37.492567241552],[126.88935623056258,37.49256446409701],[126.88952844661814,37.49222561766084],[126.88963399630448,37.492092301704425],[126.88987842636433,37.491811783111224],[126.88993398091759,37.49166735644278]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 113m","distance":113,"time":90,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88993398091759,37.49166735644278]},"properties":{"index":16,"pointIndex":8,"name":"","description":"경유지 후 123m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88993398091759,37.49166735644278],[126.88993953643535,37.49165069181195],[126.89009785979427,37.49149237970203],[126.89011730316987,37.49146738295398],[126.89014785695665,37.49143127658419],[126.89027840241462,37.49136739744843],[126.89036450732063,37.49130907242978],[126.8904672787019,37.491203530973564],[126.89052005432562,37.49110909844533],[126.89053949777929,37.491081324242266],[126.89053672969806,37.49074525213835]]},"properties":{"index":17,"lineIndex":8,"name":"","description":", 123m","distance":123,"time":95,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89053672969806,37.49074525213835]},"properties":{"index":18,"pointIndex":9,"name":"","description":"좌회전 후 32m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89053672969806,37.49074525213835],[126.89081447733376,37.49091745930357],[126.8908228097519,37.490923014362224]]},"properties":{"index":19,"lineIndex":9,"name":"","description":", 32m","distance":32,"time":25,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8908228097519,37.490923014362224]},"properties":{"index":20,"pointIndex":10,"name":"","description":"경유지 후 1m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8908228097519,37.490923014362224],[126.89081447733376,37.49091745930357]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 1m","distance":1,"time":0,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89081447733376,37.49091745930357]},"properties":{"index":22,"pointIndex":11,"name":"","description":"좌회전 후 10m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89081447733376,37.49091745930357],[126.8908866949283,37.49084802421713]]},"properties":{"index":23,"lineIndex":11,"name":"","description":", 10m","distance":10,"time":8,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8908866949283,37.49084802421713]},"properties":{"index":24,"pointIndex":12,"name":"","description":"좌회전 후 보행자도로를 따라 11m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8908866949283,37.49084802421713],[126.89100057289656,37.49086746843388]]},"properties":{"index":25,"lineIndex":12,"name":"보행자도로","description":"보행자도로, 11m","distance":11,"time":9,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89100057289656,37.49086746843388]},"properties":{"index":26,"pointIndex":13,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":1002,"totalTime":987,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 38m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 38m","distance":38,"time":29,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88750635866165,37.49400315277615]},"properties":{"index":4,"pointIndex":2,"name":"한우나주곰탕 구로점","description":"한우나주곰탕 구로점에서 좌회전 후 84m 이동","direction":"","nearPoiName":"한우나주곰탕 구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88750635866165,37.49400315277615],[126.88777577457991,37.49414480778668],[126.8880313031086,37.49427813018457],[126.88829794165811,37.494414230235705],[126.88830627415425,37.49441700783934]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 84m","distance":84,"time":66,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88830627415425,37.49441700783934]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 71m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88830627415425,37.49441700783934],[126.8886117976596,37.4945669958592],[126.8889812031409,37.49475586938862]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 71m","distance":71,"time":53,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8889812031409,37.49475586938862]},"properties":{"index":8,"pointIndex":4,"name":"신한은행 구로동지점","description":"신한은행 구로동지점에서 좌회전 후 구로중앙로를 따라 30m 이동","direction":"","nearPoiName":"신한은행 구로동지점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8889812031409,37.49475586938862],[126.88888120866012,37.494883630533614],[126.88872844417563,37.49490584744788]]},"properties":{"index":9,"lineIndex":4,"name":"구로중앙로","description":"구로중앙로, 30m","distance":30,"time":51,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88872844417563,37.49490584744788]},"properties":{"index":10,"pointIndex":5,"name":"신한은행 구로동지점","description":"신한은행 구로동지점에서 우측 횡단보도 후 20m 이동","direction":"","nearPoiName":"신한은행 구로동지점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"15","facilityName":"","turnType":213,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88872844417563,37.49490584744788],[126.888770102051,37.49508360530977]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 20m","distance":20,"time":14,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.888770102051,37.49508360530977]},"properties":{"index":12,"pointIndex":6,"name":"","description":"직진 후 10m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":11,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.888770102051,37.49508360530977],[126.88877843392268,37.49510860255323],[126.88884787282143,37.49508082924236]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 10m","distance":10,"time":37,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88884787282143,37.49508082924236]},"properties":{"index":14,"pointIndex":7,"name":"","description":"횡단보도 후 17m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88884787282143,37.49508082924236],[126.88900896691395,37.495164155765934]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 17m","distance":17,"time":11,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88900896691395,37.495164155765934]},"properties":{"index":16,"pointIndex":8,"name":"","description":"직진 후 11m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":11,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88900896691395,37.495164155765934],[126.88905896165653,37.49518915375274],[126.88912562217195,37.49519193239708]]},"properties":{"index":17,"lineIndex":8,"name":"","description":", 11m","distance":11,"time":38,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88912562217195,37.49519193239708]},"properties":{"index":18,"pointIndex":9,"name":"","description":"횡단보도 후 보행자도로를 따라 10m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88912562217195,37.49519193239708],[126.88924227805441,37.49519748938842]]},"properties":{"index":19,"lineIndex":9,"name":"보행자도로","description":"보행자도로, 10m","distance":10,"time":7,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88924227805441,37.49519748938842]},"properties":{"index":20,"pointIndex":10,"name":"대림오페라타운","description":"대림오페라타운에서 우회전 후 17m 이동","direction":"","nearPoiName":"대림오페라타운","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88924227805441,37.49519748938842],[126.88921450616361,37.495078058328815],[126.88923117193642,37.49505583898634]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 17m","distance":17,"time":12,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88923117193642,37.49505583898634]},"properties":{"index":22,"pointIndex":11,"name":"","description":"경유지 후 3m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88923117193642,37.49505583898634],[126.88921450616361,37.495078058328815]]},"properties":{"index":23,"lineIndex":11,"name":"","description":", 3m","distance":3,"time":2,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88921450616361,37.495078058328815]},"properties":{"index":24,"pointIndex":12,"name":"대림오페라타운","description":"대림오페라타운에서 우회전 후 구로중앙로를 따라 13m 이동","direction":"","nearPoiName":"대림오페라타운","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구청구의회","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88921450616361,37.495078058328815],[126.88924227805441,37.49519748938842]]},"properties":{"index":25,"lineIndex":12,"name":"구로중앙로","description":"구로중앙로, 13m","distance":13,"time":9,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88924227805441,37.49519748938842],[126.8892783813485,37.495358582421325],[126.88946725029791,37.49545579671541]]},"properties":{"index":26,"lineIndex":13,"name":"","description":", 38m","distance":38,"time":27,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88946725029791,37.49545579671541]},"properties":{"index":27,"pointIndex":13,"name":"","description":"경유지 후 가마산로를 따라 272m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88946725029791,37.49545579671541],[126.88999497234619,37.49572799671919],[126.89056713416531,37.49602241715598],[126.89060879641171,37.4960446375392],[126.8907004534006,37.49609185590929],[126.89186699692327,37.49669180700022],[126.89200587112977,37.496764023307776],[126.89205864370922,37.496777911524376]]},"properties":{"index":28,"lineIndex":14,"name":"가마산로","description":"가마산로, 272m","distance":272,"time":224,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89205864370922,37.496777911524376]},"properties":{"index":29,"pointIndex":14,"name":"컴포 구로구청점","description":"컴포 구로구청점에서 좌측 횡단보도 후 15m 이동","direction":"","nearPoiName":"컴포 구로구청점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89205864370922,37.496777911524376],[126.89196975963885,37.49689456304723]]},"properties":{"index":30,"lineIndex":15,"name":"","description":", 15m","distance":15,"time":10,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89196975963885,37.49689456304723]},"properties":{"index":31,"pointIndex":15,"name":"","description":"경유지 후 보행자도로를 따라 17m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89196975963885,37.49689456304723],[126.89186976507922,37.497025101646656]]},"properties":{"index":32,"lineIndex":16,"name":"보행자도로","description":"보행자도로, 17m","distance":17,"time":11,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89186976507922,37.497025101646656]},"properties":{"index":33,"pointIndex":16,"name":"금성볼링장","description":"금성볼링장에서 우회전 후 가마산로를 따라 11m 이동","direction":"","nearPoiName":"금성볼링장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89186976507922,37.497025101646656],[126.89188365246864,37.4970334342594],[126.89190587149534,37.49707509648052],[126.891914203523,37.49709453881403],[126.89191142545175,37.49711398094927]]},"properties":{"index":34,"lineIndex":17,"name":"가마산로","description":"가마산로, 11m","distance":11,"time":38,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89191142545175,37.49711398094927]},"properties":{"index":35,"pointIndex":17,"name":"채선당 대림점","description":"채선당 대림점에서 횡단보도 후 보행자도로를 따라 21m 이동","direction":"","nearPoiName":"채선당 대림점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89191142545175,37.49711398094927],[126.89211418202417,37.49721119549158]]},"properties":{"index":36,"lineIndex":18,"name":"보행자도로","description":"보행자도로, 21m","distance":21,"time":13,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89211418202417,37.49721119549158]},"properties":{"index":37,"pointIndex":18,"name":"KT텔레캅 본사","description":"KT텔레캅 본사에서 우회전 후 47m 이동","direction":"","nearPoiName":"KT텔레캅 본사","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89211418202417,37.49721119549158],[126.89219473094391,37.49718619983422],[126.89257802420316,37.49736951870254]]},"properties":{"index":38,"lineIndex":19,"name":"","description":", 47m","distance":47,"time":34,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89257802420316,37.49736951870254]},"properties":{"index":39,"pointIndex":19,"name":"오일뱅크 신구로","description":"오일뱅크 신구로에서 좌회전 후 공원로를 따라 31m 이동","direction":"","nearPoiName":"오일뱅크 신구로","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89257802420316,37.49736951870254],[126.89263912451591,37.49755560927617],[126.89266411997416,37.49763615591647]]},"properties":{"index":40,"lineIndex":20,"name":"공원로","description":"공원로, 31m","distance":31,"time":22,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89266411997416,37.49763615591647],[126.89246413140108,37.49787779093016]]},"properties":{"index":41,"lineIndex":21,"name":"","description":", 32m","distance":32,"time":53,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89246413140108,37.49787779093016]},"properties":{"index":42,"pointIndex":20,"name":"","description":"우측 횡단보도 후 22m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"거리공원오거리","facilityType":"15","facilityName":"","turnType":213,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89246413140108,37.49787779093016],[126.89266410997999,37.4979916701528]]},"properties":{"index":43,"lineIndex":22,"name":"","description":", 22m","distance":22,"time":14,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89266410997999,37.4979916701528]},"properties":{"index":44,"pointIndex":21,"name":"","description":"직진 후 35m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":11,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89266410997999,37.4979916701528],[126.89281409350423,37.498091661208385],[126.8929946306593,37.49816110080457]]},"properties":{"index":45,"lineIndex":23,"name":"","description":", 35m","distance":35,"time":60,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8929946306593,37.49816110080457]},"properties":{"index":46,"pointIndex":22,"name":"","description":"횡단보도 후 보행자도로를 따라 21m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"거리공원오거리","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8929946306593,37.49816110080457],[126.89319460947235,37.498266647662376]]},"properties":{"index":47,"lineIndex":24,"name":"보행자도로","description":"보행자도로, 21m","distance":21,"time":14,"roadType":23,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89319460947235,37.498266647662376]},"properties":{"index":48,"pointIndex":23,"name":"","description":"좌회전 후 공원로를 따라 7m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"거리공원오거리","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89319460947235,37.498266647662376],[126.8931501675151,37.49832219596868]]},"properties":{"index":49,"lineIndex":25,"name":"공원로","description":"공원로, 7m","distance":7,"time":15,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8931501675151,37.49832219596868]},"properties":{"index":50,"pointIndex":24,"name":"","description":"육교 진입 후 6m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"거리공원오거리","facilityType":"12","facilityName":"","turnType":125,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8931501675151,37.49832219596868],[126.89315849868379,37.49837219030684]]},"properties":{"index":51,"lineIndex":26,"name":"","description":", 6m","distance":6,"time":5,"roadType":21,"categoryRoadType":0,"facilityType":"12","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89315849868379,37.49837219030684]},"properties":{"index":52,"pointIndex":25,"name":"","description":"우회전 후 22m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89315849868379,37.49837219030684],[126.89320849959464,37.49817776935117]]},"properties":{"index":53,"lineIndex":27,"name":"","description":", 22m","distance":22,"time":18,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89320849959464,37.49817776935117]},"properties":{"index":54,"pointIndex":26,"name":"","description":"경유지 후 22m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89320849959464,37.49817776935117],[126.89315849868379,37.49837219030684]]},"properties":{"index":55,"lineIndex":28,"name":"","description":", 22m","distance":22,"time":28,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89315849868379,37.49837219030684]},"properties":{"index":56,"pointIndex":27,"name":"","description":"육교 진입 후 보행자도로를 따라 6m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"12","facilityName":"","turnType":125,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89315849868379,37.49837219030684],[126.8931501675151,37.49832219596868]]},"properties":{"index":57,"lineIndex":29,"name":"보행자도로","description":"보행자도로, 6m","distance":6,"time":5,"roadType":23,"categoryRoadType":0,"facilityType":"12","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8931501675151,37.49832219596868]},"properties":{"index":58,"pointIndex":28,"name":"","description":"우회전 후 공원로를 따라 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"거리공원오거리","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8931501675151,37.49832219596868],[126.89313350182017,37.49834163785606]]},"properties":{"index":59,"lineIndex":30,"name":"공원로","description":"공원로, 2m","distance":2,"time":32,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89313350182017,37.49834163785606]},"properties":{"index":60,"pointIndex":29,"name":"","description":"횡단보도 후 보행자도로를 따라 4m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":211,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89313350182017,37.49834163785606],[126.89311128084155,37.498369412009204]]},"properties":{"index":61,"lineIndex":31,"name":"보행자도로","description":"보행자도로, 4m","distance":4,"time":2,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89311128084155,37.498369412009204]},"properties":{"index":62,"pointIndex":30,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}]
    ],
    "1500m": [
        // 1번째 ~ 5번째 features 배열을 순서대로 넣으세요
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":1403,"totalTime":1100,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 198m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615],[126.88770634738839,37.49375596285064],[126.88782856987133,37.49335045660359],[126.88791745877948,37.49306160287099],[126.8879341251766,37.49301716388856],[126.88805079190752,37.49263665463681]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 198m","distance":198,"time":153,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88805079190752,37.49263665463681]},"properties":{"index":4,"pointIndex":2,"name":"","description":"좌회전 후 3m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88805079190752,37.49263665463681],[126.8880757892399,37.49265054235771]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 3m","distance":3,"time":2,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8880757892399,37.49265054235771]},"properties":{"index":6,"pointIndex":3,"name":"","description":"경유지 후 3m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8880757892399,37.49265054235771],[126.88805079190752,37.49263665463681]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 3m","distance":3,"time":2,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88805079190752,37.49263665463681]},"properties":{"index":8,"pointIndex":4,"name":"","description":"좌회전 후 146m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88805079190752,37.49263665463681],[126.88832023092743,37.4919561829715],[126.8884674487914,37.49163400081899],[126.8885618889248,37.49148124247929],[126.88855633629458,37.49139514127547]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 146m","distance":146,"time":112,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88855633629458,37.49139514127547]},"properties":{"index":10,"pointIndex":5,"name":"구로2동공영주차장","description":"구로2동공영주차장에서 2시 방향 우회전 후 90m 이동","direction":"","nearPoiName":"구로2동공영주차장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":18,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88855633629458,37.49139514127547],[126.88828136563755,37.49124237634544],[126.8882147060583,37.491206268241356],[126.88802028276021,37.491084056753245],[126.88777864239988,37.49093129241792],[126.88773975775584,37.49090629462931]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 90m","distance":90,"time":69,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88773975775584,37.49090629462931]},"properties":{"index":12,"pointIndex":6,"name":"하루애PC방 구로","description":"하루애PC방 구로에서 좌회전 후 113m 이동","direction":"","nearPoiName":"하루애PC방 구로","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88773975775584,37.49090629462931],[126.8877258717709,37.49084796782674],[126.88773698460112,37.490750757100216],[126.88774254074309,37.490711872829415],[126.88785087263578,37.49041190962258],[126.88791753869198,37.49021748896216],[126.88800087020877,37.490011958779],[126.88801753613757,37.489984184526335],[126.88808975256124,37.48995641126464]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 113m","distance":113,"time":95,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88808975256124,37.48995641126464]},"properties":{"index":14,"pointIndex":7,"name":"소문난순대집","description":"소문난순대집에서 4시 방향 우회전 후 8m 이동","direction":"","nearPoiName":"소문난순대집","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":19,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88808975256124,37.48995641126464],[126.88801476040759,37.489920303011885]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 8m","distance":8,"time":6,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88801476040759,37.489920303011885]},"properties":{"index":16,"pointIndex":8,"name":"","description":"경유지 후 8m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88801476040759,37.489920303011885],[126.88808975256124,37.48995641126464]]},"properties":{"index":17,"lineIndex":8,"name":"","description":", 8m","distance":8,"time":5,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88808975256124,37.48995641126464]},"properties":{"index":18,"pointIndex":9,"name":"청암한의원","description":"청암한의원에서 우회전 후 57m 이동","direction":"","nearPoiName":"청암한의원","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88808975256124,37.48995641126464],[126.8881091972633,37.48988419778159],[126.88810919765348,37.48987031050661],[126.88810364432072,37.4898092063976],[126.88792866993779,37.48946479885649]]},"properties":{"index":19,"lineIndex":9,"name":"","description":", 57m","distance":57,"time":45,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88792866993779,37.48946479885649]},"properties":{"index":20,"pointIndex":10,"name":"","description":"좌회전 후 190m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88792866993779,37.48946479885649],[126.8880036670855,37.4893231499894],[126.88806478012084,37.489056515399604],[126.88809255887965,37.48893153042011],[126.88817033433239,37.48876210705246],[126.88827588355004,37.48864545582524],[126.88838143284575,37.48852602714301],[126.88858697732407,37.48825384021935],[126.88863141928064,37.48819829191206],[126.88868141652081,37.488134411338855],[126.88872863631427,37.48806775326109],[126.88885362804922,37.4879566572906]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 190m","distance":190,"time":146,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88885362804922,37.4879566572906]},"properties":{"index":22,"pointIndex":11,"name":"GS25 남구로점","description":"GS25 남구로점에서 우회전 후 도림로를 따라 35m 이동","direction":"","nearPoiName":"GS25 남구로점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88885362804922,37.4879566572906],[126.88882863110675,37.48792888229463],[126.8886369859563,37.48778445121563],[126.88857032676673,37.487734455836424]]},"properties":{"index":23,"lineIndex":11,"name":"도림로","description":"도림로, 35m","distance":35,"time":55,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88857032676673,37.487734455836424]},"properties":{"index":24,"pointIndex":12,"name":"","description":"좌측 횡단보도 후 보행자도로를 따라 23m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88857032676673,37.487734455836424],[126.8887592025853,37.48758725409038]]},"properties":{"index":25,"lineIndex":12,"name":"보행자도로","description":"보행자도로, 23m","distance":23,"time":15,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8887592025853,37.48758725409038]},"properties":{"index":26,"pointIndex":13,"name":"기아자동차 가리봉테크노대리점","description":"기아자동차 가리봉테크노대리점에서 좌회전 후 37m 이동","direction":"","nearPoiName":"기아자동차 가리봉테크노대리점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8887592025853,37.48758725409038],[126.88882030680341,37.487634471915506],[126.8888814110215,37.48768168974064],[126.88905639141362,37.487812233247396]]},"properties":{"index":27,"lineIndex":13,"name":"","description":", 37m","distance":37,"time":26,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88905639141362,37.487812233247396]},"properties":{"index":28,"pointIndex":14,"name":"","description":"경유지 후 도림로를 따라 114m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88905639141362,37.487812233247396],[126.8888814110215,37.48768168974064],[126.88882030680341,37.487634471915506],[126.8887592025853,37.48758725409038],[126.88872865035921,37.48756781136032],[126.88870643062934,37.48755114623392],[126.88867865584997,37.48753448100841],[126.88862310707148,37.48747337600735],[126.88861199744063,37.487456711079126],[126.88856200347756,37.48740393854218],[126.88849812228084,37.48733727848247],[126.88837591478081,37.48720951337216],[126.88831203358404,37.48714285331245],[126.8882481524653,37.48707341579773],[126.8882176007073,37.48703730833765]]},"properties":{"index":29,"lineIndex":14,"name":"도림로","description":"도림로, 114m","distance":114,"time":81,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8882176007073,37.48703730833765]},"properties":{"index":30,"pointIndex":15,"name":"월드공인중개사","description":"월드공인중개사에서 좌회전 후 101m 이동","direction":"","nearPoiName":"월드공인중개사","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8882176007073,37.48703730833765],[126.88835925618626,37.48697620685432],[126.88852035465062,37.486903995897805],[126.88879255504152,37.486798457463046],[126.88890643472688,37.48675679766934],[126.88905642285826,37.48669291887967],[126.88907864329039,37.48668458691102],[126.88921752116673,37.486626262833184],[126.8892369640741,37.48661793081499]]},"properties":{"index":31,"lineIndex":15,"name":"","description":", 101m","distance":101,"time":78,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8892369640741,37.48661793081499]},"properties":{"index":32,"pointIndex":16,"name":"","description":"경유지 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8892369640741,37.48661793081499],[126.88921752116673,37.486626262833184]]},"properties":{"index":33,"lineIndex":16,"name":"","description":", 2m","distance":2,"time":1,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88921752116673,37.486626262833184]},"properties":{"index":34,"pointIndex":17,"name":"예광교회","description":"예광교회에서 좌회전 후 203m 이동","direction":"","nearPoiName":"예광교회","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88921752116673,37.486626262833184],[126.88907865179496,37.486381844314984],[126.88903976816486,37.48632073961111],[126.88899810724406,37.486251302492654],[126.88894533661433,37.486167977900955],[126.8888925660626,37.48608187585425],[126.88882035369575,37.48596522145559],[126.8886842609121,37.48575413244698],[126.88851761699433,37.485484716338085],[126.88847595591736,37.48542083412963],[126.8884204079187,37.48533195457832],[126.8883370859987,37.48519585779634],[126.88828709289348,37.485112533254146],[126.88820932571087,37.484987546391295]]},"properties":{"index":35,"lineIndex":17,"name":"","description":", 203m","distance":203,"time":156,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88820932571087,37.484987546391295]},"properties":{"index":36,"pointIndex":18,"name":"구로3동공영주차장","description":"구로3동공영주차장에서 좌회전 후 2m 이동","direction":"","nearPoiName":"구로3동공영주차장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88820932571087,37.484987546391295],[126.88823432366777,37.48497921447214]]},"properties":{"index":37,"lineIndex":18,"name":"","description":", 2m","distance":2,"time":2,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88823432366777,37.48497921447214]},"properties":{"index":38,"pointIndex":19,"name":"","description":"경유지 후 2m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88823432366777,37.48497921447214],[126.88820932571087,37.484987546391295]]},"properties":{"index":39,"lineIndex":19,"name":"","description":", 2m","distance":2,"time":2,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88820932571087,37.484987546391295]},"properties":{"index":40,"pointIndex":20,"name":"구로3동공영주차장","description":"구로3동공영주차장에서 우회전 후 16m 이동","direction":"","nearPoiName":"구로3동공영주차장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88820932571087,37.484987546391295],[126.88828709289348,37.485112533254146]]},"properties":{"index":41,"lineIndex":20,"name":"","description":", 16m","distance":16,"time":11,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88828709289348,37.485112533254146]},"properties":{"index":42,"pointIndex":21,"name":"구로3동공영주차장","description":"구로3동공영주차장에서 좌회전 후 5m 이동","direction":"","nearPoiName":"구로3동공영주차장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88828709289348,37.485112533254146],[126.88823431937693,37.48513197449791]]},"properties":{"index":43,"lineIndex":21,"name":"","description":", 5m","distance":5,"time":5,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88823431937693,37.48513197449791]},"properties":{"index":44,"pointIndex":22,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":1702,"totalTime":1346,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 277m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615],[126.88770634738839,37.49375596285064],[126.88782856987133,37.49335045660359],[126.88791745877948,37.49306160287099],[126.8879341251766,37.49301716388856],[126.88805079190752,37.49263665463681],[126.88831745316857,37.49196451528692]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 277m","distance":277,"time":212,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88831745316857,37.49196451528692]},"properties":{"index":4,"pointIndex":2,"name":"","description":"경유지 후 67m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88831745316857,37.49196451528692],[126.88832023092743,37.4919561829715],[126.8884674487914,37.49163400081899],[126.8885618889248,37.49148124247929],[126.88855633629458,37.49139514127547]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 67m","distance":67,"time":52,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88855633629458,37.49139514127547]},"properties":{"index":6,"pointIndex":3,"name":"구로2동공영주차장","description":"구로2동공영주차장에서 2시 방향 우회전 후 91m 이동","direction":"","nearPoiName":"구로2동공영주차장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":18,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88855633629458,37.49139514127547],[126.88828136563755,37.49124237634544],[126.8882147060583,37.491206268241356],[126.88802028276021,37.491084056753245],[126.88777864239988,37.49093129241792],[126.88773975775584,37.49090629462931]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 91m","distance":91,"time":70,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88773975775584,37.49090629462931]},"properties":{"index":8,"pointIndex":4,"name":"하루애PC방 구로","description":"하루애PC방 구로에서 좌회전 후 113m 이동","direction":"","nearPoiName":"하루애PC방 구로","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88773975775584,37.49090629462931],[126.8877258717709,37.49084796782674],[126.88773698460112,37.490750757100216],[126.88774254074309,37.490711872829415],[126.88785087263578,37.49041190962258],[126.88791753869198,37.49021748896216],[126.88800087020877,37.490011958779],[126.88801753613757,37.489984184526335],[126.88808975256124,37.48995641126464]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 113m","distance":113,"time":94,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88808975256124,37.48995641126464]},"properties":{"index":10,"pointIndex":5,"name":"소문난순대집","description":"소문난순대집에서 2시 방향 우회전 후 56m 이동","direction":"","nearPoiName":"소문난순대집","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":18,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88808975256124,37.48995641126464],[126.8881091972633,37.48988419778159],[126.88810919765348,37.48987031050661],[126.88810364432072,37.4898092063976],[126.88793422459712,37.48947868623056]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 56m","distance":56,"time":43,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88793422459712,37.48947868623056]},"properties":{"index":12,"pointIndex":6,"name":"","description":"경유지 후 232m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88793422459712,37.48947868623056],[126.88792866993779,37.48946479885649],[126.88773425069652,37.4891981597084],[126.88757871617729,37.48895374089393],[126.88748706184006,37.48881208905396],[126.88738985284343,37.48865654983989],[126.88737318831915,37.48863432990261],[126.88727875676916,37.48848156819308],[126.88708156086068,37.48818437699033],[126.88673994131105,37.48761499262105]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 232m","distance":232,"time":179,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88673994131105,37.48761499262105]},"properties":{"index":14,"pointIndex":7,"name":"","description":"경유지 후 206m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88673994131105,37.48761499262105],[126.88672605431147,37.48759277273331],[126.8864455372628,37.48713171019892],[126.88642331815704,37.48709282543253],[126.88620390415583,37.48672064254809],[126.8857345242382,37.485945724228955]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 206m","distance":206,"time":158,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8857345242382,37.485945724228955]},"properties":{"index":16,"pointIndex":8,"name":"예술인주차장","description":"예술인주차장에서 좌회전 후 구로동로를 따라 70m 이동","direction":"","nearPoiName":"예술인주차장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8857345242382,37.485945724228955],[126.88585118800081,37.48567075826375],[126.88599007344364,37.485343021049545]]},"properties":{"index":17,"lineIndex":8,"name":"구로동로","description":"구로동로, 70m","distance":70,"time":80,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88599007344364,37.485343021049545]},"properties":{"index":18,"pointIndex":9,"name":"구로종합동물병원","description":"구로종합동물병원에서 우측 횡단보도 후 보행자도로를 따라 20m 이동","direction":"","nearPoiName":"구로종합동물병원","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":213,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88599007344364,37.485343021049545],[126.88577620575364,37.48528191322483]]},"properties":{"index":19,"lineIndex":9,"name":"보행자도로","description":"보행자도로, 20m","distance":20,"time":13,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88577620575364,37.48528191322483]},"properties":{"index":20,"pointIndex":10,"name":"","description":"좌회전 후 50m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88577620575364,37.48528191322483],[126.88581231607172,37.4851930353085],[126.88582620455358,37.485162483551065],[126.88587898048822,37.48505694120187],[126.88590120177837,37.48501805722801],[126.88590675737393,37.48499861514201],[126.88595953408864,37.48486529824266]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 50m","distance":50,"time":52,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88595953408864,37.48486529824266]},"properties":{"index":22,"pointIndex":11,"name":"","description":"경유지 후 구로동로를 따라 12m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88595953408864,37.48486529824266],[126.88600119992425,37.48475975569527]]},"properties":{"index":23,"lineIndex":11,"name":"구로동로","description":"구로동로, 12m","distance":12,"time":21,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88600119992425,37.48475975569527]},"properties":{"index":24,"pointIndex":12,"name":"남구로역  3번출구","description":"남구로역  3번출구에서 좌측 횡단보도 후 보행자도로를 따라 26m 이동","direction":"","nearPoiName":"남구로역  3번출구","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"남구로역","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88600119992425,37.48475975569527],[126.88629561786061,37.48474865112631]]},"properties":{"index":25,"lineIndex":12,"name":"보행자도로","description":"보행자도로, 26m","distance":26,"time":18,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88629561786061,37.48474865112631]},"properties":{"index":26,"pointIndex":13,"name":"그랜드마트","description":"그랜드마트에서 우회전 후 구로동로를 따라 255m 이동","direction":"","nearPoiName":"그랜드마트","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"남구로역","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88629561786061,37.48474865112631],[126.88628173132892,37.48470976650843],[126.88629006476127,37.484679214651905],[126.8868344938589,37.48345992160952],[126.88685115994353,37.483426592446534],[126.88721780881322,37.48287110798167],[126.88728169422295,37.48278778547042],[126.88731224793142,37.4827544565551],[126.88728725371864,37.48262947063326]]},"properties":{"index":27,"lineIndex":13,"name":"구로동로","description":"구로동로, 255m","distance":255,"time":182,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88728725371864,37.48262947063326]},"properties":{"index":28,"pointIndex":14,"name":"유치과의원","description":"유치과의원에서 좌회전 후 117m 이동","direction":"","nearPoiName":"유치과의원","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88728725371864,37.48262947063326],[126.88735391626312,37.48256003544663],[126.88762612172465,37.482273962434064],[126.88782888859902,37.48200455291305],[126.88795110421302,37.4818434627013],[126.88801776738158,37.48175180787439]]},"properties":{"index":29,"lineIndex":14,"name":"","description":", 117m","distance":117,"time":90,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88801776738158,37.48175180787439]},"properties":{"index":30,"pointIndex":15,"name":"연길진미명태","description":"연길진미명태에서 좌회전 후 1m 이동","direction":"","nearPoiName":"연길진미명태","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88801776738158,37.48175180787439],[126.88803165484943,37.48175736303213]]},"properties":{"index":31,"lineIndex":15,"name":"","description":", 1m","distance":1,"time":1,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88803165484943,37.48175736303213]},"properties":{"index":32,"pointIndex":16,"name":"","description":"경유지 후 52m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88803165484943,37.48175736303213],[126.88829829348337,37.48189068562919],[126.888531602288,37.48200734290167]]},"properties":{"index":33,"lineIndex":16,"name":"","description":", 52m","distance":52,"time":40,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.888531602288,37.48200734290167]},"properties":{"index":34,"pointIndex":17,"name":"","description":"좌회전 후 10m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.888531602288,37.48200734290167],[126.8885204917209,37.48202400743366],[126.88847049471492,37.482079555642436]]},"properties":{"index":35,"lineIndex":17,"name":"","description":", 10m","distance":10,"time":8,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88847049471492,37.482079555642436]},"properties":{"index":36,"pointIndex":18,"name":"구로구 가리봉동","description":"도착","direction":"","nearPoiName":"구로구 가리봉동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 가리봉동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        [{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770354863446,37.49451143055553]},"properties":{"totalDistance":1278,"totalTime":1019,"index":0,"pointIndex":0,"name":"","description":"가마산로를 따라 47m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":200,"pointType":"SP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770354863446,37.49451143055553],[126.8872563737094,37.49427811635943]]},"properties":{"index":1,"lineIndex":0,"name":"가마산로","description":"가마산로, 47m","distance":47,"time":33,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8872563737094,37.49427811635943]},"properties":{"index":2,"pointIndex":1,"name":"애경새마을금고","description":"애경새마을금고에서 좌회전 후 71m 이동","direction":"","nearPoiName":"애경새마을금고","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8872563737094,37.49427811635943],[126.88750635866165,37.49400315277615],[126.88770634738839,37.49375596285064]]},"properties":{"index":3,"lineIndex":1,"name":"","description":", 71m","distance":71,"time":54,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88770634738839,37.49375596285064]},"properties":{"index":4,"pointIndex":2,"name":"미니스톱 구로구청점","description":"미니스톱 구로구청점에서 좌회전 후 88m 이동","direction":"","nearPoiName":"미니스톱 구로구청점","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88770634738839,37.49375596285064],[126.88803964895118,37.49380596298659],[126.88813130742338,37.49380040971187],[126.88837572842898,37.49384207589728],[126.88853960207592,37.49385318864088],[126.88869514260233,37.493883743420696]]},"properties":{"index":5,"lineIndex":2,"name":"","description":", 88m","distance":88,"time":69,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88869514260233,37.493883743420696]},"properties":{"index":6,"pointIndex":3,"name":"","description":"우회전 후 15m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88869514260233,37.493883743420696],[126.88879791382725,37.49378375687486]]},"properties":{"index":7,"lineIndex":3,"name":"","description":", 15m","distance":15,"time":11,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88879791382725,37.49378375687486]},"properties":{"index":8,"pointIndex":4,"name":"","description":"경유지 후 75m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP1"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88879791382725,37.49378375687486],[126.8888451329965,37.49373931843757],[126.8892951062866,37.49323105220344]]},"properties":{"index":9,"lineIndex":4,"name":"","description":", 75m","distance":75,"time":58,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8892951062866,37.49323105220344]},"properties":{"index":10,"pointIndex":5,"name":"","description":"우회전 후 190m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8892951062866,37.49323105220344],[126.88925344396192,37.493211609275235],[126.88923955883584,37.493122730467945],[126.88928956473967,37.492750552391826],[126.88935623056258,37.49256446409701],[126.88952844661814,37.49222561766084],[126.88963399630448,37.492092301704425],[126.88987842636433,37.491811783111224],[126.88993398091759,37.49166735644278]]},"properties":{"index":11,"lineIndex":5,"name":"","description":", 190m","distance":190,"time":154,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.88993398091759,37.49166735644278]},"properties":{"index":12,"pointIndex":6,"name":"","description":"경유지 후 150m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP2"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.88993398091759,37.49166735644278],[126.88993953643535,37.49165069181195],[126.89009785979427,37.49149237970203],[126.89011730316987,37.49146738295398],[126.89014785695665,37.49143127658419],[126.89027840241462,37.49136739744843],[126.89036450732063,37.49130907242978],[126.8904672787019,37.491203530973564],[126.89052005432562,37.49110909844533],[126.89053949777929,37.491081324242266],[126.89053672969806,37.49074525213835],[126.89056451173538,37.49050361404926]]},"properties":{"index":13,"lineIndex":6,"name":"","description":", 150m","distance":150,"time":116,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89056451173538,37.49050361404926]},"properties":{"index":14,"pointIndex":7,"name":"","description":"좌회전 후 5m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89056451173538,37.49050361404926],[126.89059506536607,37.4904730625894]]},"properties":{"index":15,"lineIndex":7,"name":"","description":", 5m","distance":5,"time":4,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89059506536607,37.4904730625894]},"properties":{"index":16,"pointIndex":8,"name":"","description":"경유지 후 도림로를 따라 132m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP3"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89059506536607,37.4904730625894],[126.89060895338004,37.490459175562194],[126.89078949631273,37.4903230834884],[126.890878379446,37.49023976142423],[126.89090615562999,37.4902064324598],[126.89098392882035,37.49011755528741],[126.89103114853609,37.490053674664885],[126.89105059245803,37.49000923573178],[126.89112836525818,37.489934245834355],[126.89131168532559,37.48981204108503],[126.89140612483516,37.48968150238491],[126.8913783509144,37.48963428515435],[126.89129780402337,37.48958706698224],[126.8912672517974,37.48956762425213]]},"properties":{"index":17,"lineIndex":8,"name":"도림로","description":"도림로, 132m","distance":132,"time":138,"roadType":23,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.8912672517974,37.48956762425213]},"properties":{"index":18,"pointIndex":9,"name":"한독프라자약국","description":"한독프라자약국에서 좌측 횡단보도 후 보행자도로를 따라 26m 이동","direction":"","nearPoiName":"한독프라자약국","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"15","facilityName":"","turnType":212,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.8912672517974,37.48956762425213],[126.89144224116365,37.48937876043403]]},"properties":{"index":19,"lineIndex":9,"name":"보행자도로","description":"보행자도로, 26m","distance":26,"time":17,"roadType":21,"categoryRoadType":0,"facilityType":"15","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89144224116365,37.48937876043403]},"properties":{"index":20,"pointIndex":10,"name":"라라동물병원","description":"라라동물병원에서 좌회전 후 33m 이동","direction":"","nearPoiName":"라라동물병원","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89144224116365,37.48937876043403],[126.8914700159429,37.48939542565959],[126.89163388764,37.4894759747784],[126.89168388168073,37.48952596986042],[126.89172276593463,37.48956485492422]]},"properties":{"index":21,"lineIndex":10,"name":"","description":", 33m","distance":33,"time":23,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89172276593463,37.48956485492422]},"properties":{"index":22,"pointIndex":11,"name":"","description":"경유지 후 도림로를 따라 6m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP4"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89172276593463,37.48956485492422],[126.89168388168073,37.48952596986042]]},"properties":{"index":23,"lineIndex":11,"name":"도림로","description":"도림로, 6m","distance":6,"time":3,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89168388168073,37.48952596986042]},"properties":{"index":24,"pointIndex":12,"name":"라라동물병원","description":"라라동물병원에서 좌회전 후 434m 이동","direction":"","nearPoiName":"라라동물병원","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":12,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89168388168073,37.48952596986042],[126.89177554109015,37.489487087125745],[126.89239216079457,37.48915935843699],[126.89266436235692,37.48901215817838],[126.89298378238657,37.48884551657722],[126.89308933020041,37.48877885954029],[126.89333931164302,37.4886288814302],[126.89365317638935,37.48847057209493],[126.89453088724746,37.48800675276929],[126.89473364952092,37.4879012130968],[126.89478920313809,37.48779011588757],[126.89496141552699,37.48758180983445],[126.89507251776632,37.48753737253668],[126.89509196051773,37.4875345954286],[126.89511140303499,37.48754015068555],[126.89558357436194,37.487820682067245],[126.89561134953155,37.487823460017886]]},"properties":{"index":25,"lineIndex":12,"name":"","description":", 434m","distance":434,"time":335,"roadType":21,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89561134953155,37.487823460017886]},"properties":{"index":26,"pointIndex":13,"name":"한체대 중앙태권도장","description":"한체대 중앙태권도장에서 우회전 후 2m 이동","direction":"","nearPoiName":"한체대 중앙태권도장","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":13,"pointType":"GP"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89561134953155,37.487823460017886],[126.89562523746757,37.487812350445644],[126.89562801514843,37.487806795585186]]},"properties":{"index":27,"lineIndex":13,"name":"","description":", 2m","distance":2,"time":1,"roadType":0,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89562801514843,37.487806795585186]},"properties":{"index":28,"pointIndex":14,"name":"","description":"경유지 후 4m 이동","direction":"","nearPoiName":"","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"","facilityType":"11","facilityName":"","turnType":0,"pointType":"PP5"}},{"type":"Feature","geometry":{"type":"LineString","coordinates":[[126.89562801514843,37.487806795585186],[126.89563634850323,37.48777902118375]]},"properties":{"index":29,"lineIndex":14,"name":"","description":", 4m","distance":4,"time":3,"roadType":22,"categoryRoadType":0,"facilityType":"11","facilityName":""}},{"type":"Feature","geometry":{"type":"Point","coordinates":[126.89563634850323,37.48777902118375]},"properties":{"index":30,"pointIndex":15,"name":"구로구 구로동","description":"도착","direction":"","nearPoiName":"구로구 구로동","nearPoiX":"0.0","nearPoiY":"0.0","intersectionName":"구로구 구로동","facilityType":"","facilityName":"","turnType":201,"pointType":"EP"}}],
        [/* 1500m 네번째 */],
        [/* 1500m 다섯번째 */]
    ]
};

async function fetchPredictionData() {
    console.log(`fetchPredictionData (Hardcoded) 실행됨`)
    
    // 로딩 효과를 위해 잠시 대기
    await delay(12000);

    try {
        // 하드코딩된 데이터 사용
        const data = dummyData.value;
        
        console.log(`Destination DATA :: ${JSON.stringify(data)}`)
        
        total_cluster.value = data.total_clusters_found;
        await processDestinationsToZones(data);

        return true;
    } catch (error) {
        console.error('❌ 예측 데이터 로드 실패:', error);
        personError.value = '예측 데이터를 불러올 수 없습니다.';
        return false;
    } finally {
        isLoading.value = false;
    }
}

// ========================================================================================
// ⭐ API 응답을 Zone 배열로 변환하는 함수 + 경로 생성
// ========================================================================================


async function processDestinationsToZones(apiResponse) {
    console.log('🔄 하드코딩 데이터 처리 시작...')

    if (apiResponse.last_known_location) {
        lastKnownLocation.value = apiResponse.last_known_location
        missingLocation.value.lat = apiResponse.last_known_location.latitude
        missingLocation.value.lon = apiResponse.last_known_location.longitude
    }

    const destinationsByDistance = apiResponse.destinations_by_distance || {}

    // 하드코딩된 데이터 매핑 (address2에 name을 사용)
    const mapToZone = (dest) => ({
        id: dest.destination_id,
        lat: dest.latitude,
        lon: dest.longitude,
        name: dest.name,
        visitCount: dest.visit_count,
        distance: dest.distance_meters,
        // UI 표시용 필드 매핑
        address1: '서울시 구로구', // 기본 주소 (필요시 수정)
        address2: dest.name,       // "구로중앙로에 있는 것 같아요!" 등
        sgg_nm: '구로구',
        dist_m: Math.round(dest.distance_meters),
        zoneLevel: 0 // 나중에 설정됨
    })

    zone_level_1.value = (destinationsByDistance['500m'] || []).map(d => ({ ...mapToZone(d), zoneLevel: 1 }))
    zone_level_2.value = (destinationsByDistance['1000m'] || []).map(d => ({ ...mapToZone(d), zoneLevel: 2 }))
    zone_level_3.value = (destinationsByDistance['1500m'] || []).map(d => ({ ...mapToZone(d), zoneLevel: 3 }))

    // VWorld 주소 조회는 하드코딩된 이름(name)을 덮어쓸 수 있으므로 생략하거나 필요시 주석 해제
    // await getAddressAndJimok() 

    // 반응성 갱신
    zone_level_1.value = [...zone_level_1.value]
    zone_level_2.value = [...zone_level_2.value]
    zone_level_3.value = [...zone_level_3.value]

    // 하드코딩된 경로 로드
    await requestAllRoutes()

    if (map) {
        setCenter()
        makeMarker()
        showCirclesByZoneLevel(displayZoneLevel.value)
    }
}

//
// VWorld에서 주소와 지목정보를 가져와서 화면에 표시
//
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

    // 모든 존의 모든 location을 순회하며 처리
    for (const zone of allZones) {
        if (!zone.data || zone.data.length === 0) continue

        console.log(`⏳ Zone ${zone.level} 처리 시작 (${zone.data.length}개)`)

        // ⭐ 각 존의 모든 location을 병렬 처리
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

/**
 * 개별 location에 대한 주소 및 지목 정보 처리
 */
async function processLocation(location, zoneLevel, locationIndex, columns) {
    // 1. 거리 계산
    location.dist_m = calculateDistance(
        missingLocation.value.lat,
        missingLocation.value.lon,
        location.lat,
        location.lon
    )

    try {
        // 2. VWorld API 호출
        const vworldData = await fetchVWorldData(location, columns)

        // 3. 응답 상태에 따른 분기 처리
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

        // 4. VWorld properties 데이터로 location 정보 설정
        await setLocationFromVWorld(location, vworldData.properties)

        // 5. API 호출 지연 (rate limit 방지)
        await delay(150)

    } catch (e) {
        console.error(`Zone ${zoneLevel}-${locationIndex + 1} 예상치 못한 에러:`, e)
        setLocationError(location)
    }
}

/**
 * VWorld Data API 호출
 */
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
        domain: 'lx12mammamia.xyz'
    })

    const dataUrl = `https://api.vworld.kr/req/data?${dataParams.toString()}`
    const dataProxyUrl = `https://www.vworld.kr/proxy.do?url=${encodeURIComponent(dataUrl)}`

    const dataRes = await fetch(dataProxyUrl)

    if (!dataRes.ok) {
        throw new Error(`VWorld Data API HTTP error! status: ${dataRes.status}`)
    }

    // 1. text로 받기
    let dataText = await dataRes.text()
    
    // 2. 이중 래핑된 JSON 처리
    if (dataText.startsWith('"') && dataText.endsWith('"')) {
        dataText = JSON.parse(dataText)
    }
    
    // 3. JSON 파싱
    const dataResp = JSON.parse(dataText)
    
    console.log('VWORLD Response Status:', dataResp?.response?.status)

    // 4. features 접근
    const features = dataResp?.response?.result?.featureCollection?.features

    if (features && features.length > 0) {
        const props = features[0].properties
        const addr = `${props.sgg_nm || ''} ${props.emd_nm || ''} ${props.rn_nm || ''}`.trim()
        console.log('주소 ::', addr)
    }

    const properties = features?.[0]?.properties

    // 응답 구조 정규화하여 반환
    return {
        status: dataResp?.response?.status || 'ERROR',
        errorText: dataResp?.response?.error?.text,
        properties: properties,
        allFeatures: features
    }
}

/**
 * VWorld 데이터로 location 정보 설정
 */
async function setLocationFromVWorld(location, props) {
    // 1. properties에서 필요한 정보 추출
    const sido = props.sido_nm || ''
    const sgg = props.sgg_nm || ''
    const emd = props.emd_nm || ''
    const ri = props.ri_nm || ''
    const jimok = props.jimok || '토지'

    // 2. location 객체에 기본 정보 저장
    location.sido_nm = sido
    location.sgg_nm = sgg
    location.emd_nm = emd
    location.ri_nm = ri
    location.jimok = jimok


    // 3. address1: 행정구역 조합 (시/군 + 읍면동 + 리)
    const addressParts = [sgg, emd, ri].filter(part => part)
    location.address1 = addressParts.join(' ')

    // 4. address2: 자연어 설명 생성
    location.address2 = await generateAddress2(jimok, location.address1)
}

/**
 * address2 자연어 설명 생성
 */
async function generateAddress2(jimok, address1) {
    // 1. 지목을 자연어로 변환
    const jimokNaturalText = convertJimokToNaturalLanguage(jimok)

    // 2. 특정 지목은 POI 검색 없이 바로 반환
    const excludeJimok = ['전', '답', '임야', '도로']

    if (!excludeJimok.includes(jimok)) {
        // 3. 기타 지목의 경우 POI 검색 수행

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
        // 4. 전/답/임야/도로는 지목 그대로 사용
        const result = `${jimokNaturalText}에 있을 것 같아요!`
        return result
    }
}

/**
 * Kakao Geocoder 폴백 처리
 */
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

/**
 * location에 에러 정보 설정
 */
function setLocationError(location) {
    location.address1 = '위치 정보 없음'
    location.address2 = ''
    console.warn(`⚠️ 위치 정보 에러 처리`)
}

/**
 * 지목을 자연어로 변환하는 함수
 */
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

    // 정확한 일치
    if (jimokMap[jimok]) {
        return jimokMap[jimok]
    }

    // 부분 일치
    for (const [key, value] of Object.entries(jimokMap)) {
        if (jimok.includes(key)) {
            return value
        }
    }

    // 기본값
    return jimok
}

/**
 * VWorld 검색 API로 POI 검색
 */
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

        // VWorld Search API 응답 구조  
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

// ========================================================================================
// Tmap 경로 관련 함수 - waypoints 포함 수정
// ========================================================================================

// ⭐ zone_level_1~3의 모든 경로를 저장하는 배열
const zone1Routes = ref([])
const zone2Routes = ref([])
const zone3Routes = ref([])

async function requestAllRoutes() {
    console.log('🚶 하드코딩 경로 데이터 로드 중...')

    // TMap API 호출 대신 mockRouteData 사용
    // 데이터가 비어있을 수 있으므로 안전하게 접근
    zone1Routes.value = mockRouteData["500m"] ? [...mockRouteData["500m"]] : []
    zone2Routes.value = mockRouteData["1000m"] ? [...mockRouteData["1000m"]] : []
    zone3Routes.value = mockRouteData["1500m"] ? [...mockRouteData["1500m"]] : []

    console.log(`✅ 경로 로드 완료:`)
    console.log(`  - Zone1: ${zone1Routes.value.length}개`)
    console.log(`  - Zone2: ${zone2Routes.value.length}개`)
    console.log(`  - Zone3: ${zone3Routes.value.length}개`)
}

/**
 * polyline 제거 함수
 */
function clearPolylines() {
    for (let polyline of polylines) {
        if (polyline && polyline.setMap) {
            polyline.setMap(null)
        }
    }

    polylines.length = 0
    console.log('✅ 모든 폴리라인 제거')
}

/**
 * 모든 경로 제거
 */
function clearAllRoutes() {
    clearPolylines()
}

/**
 * 경로 그리기 함수
 */
function drawRoute(index, zoneLevel = 1) {
    if (!map) return

    let routeStorage
    if (zoneLevel === 1) routeStorage = zone1Routes.value
    else if (zoneLevel === 2) routeStorage = zone2Routes.value
    else if (zoneLevel === 3) routeStorage = zone3Routes.value
    
    if (!routeStorage || index >= routeStorage.length) return

    const routeFeatures = routeStorage[index]
    if (!routeFeatures || routeFeatures.length === 0) return

    clearPolylines()

    routeFeatures.forEach((feature) => {
        if (feature.geometry && feature.geometry.type === 'LineString') {
            const coordinates = feature.geometry.coordinates
            const linePath = coordinates.map(([lng, lat]) => new window.kakao.maps.LatLng(lat, lng))

            const polyline = new window.kakao.maps.Polyline({
                map: map,
                path: linePath,
                strokeColor: '#2563EB',
                strokeWeight: 5,
                strokeOpacity: 0.8,
                strokeStyle: 'solid'
            })
            polylines.push(polyline)
        }
    })
}

/**
 * 딜레이 함수
 */
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * Watch - 시간대 변경 시
 */
watch(selectedMinutes, (newMinutes) => {
    let newLevel = 1

    if (newMinutes <= 30) {
        newLevel = 1
    } else if (newMinutes <= 60) {
        newLevel = 2
    } else {
        newLevel = 3
    }

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

/**
 * Watch - Zone Level 변경 시
 */
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

/**
 * Watch - 모든 위치 보기 토글 시
 */
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


/**
 * 임시 테스트용 마커 3개를 생성합니다. 함께하기용 나중에 진짜 함께하기가 되면 삭제해야 함
 */
function createTemporaryMarkers() {
    // map 객체가 초기화되었는지 확인
    if (!map) {
        console.warn('임시 마커 생성 실패: map 객체가 아직 초기화되지 않았습니다.');
        return;
    }

    console.log("🗺️ 3개의 임시 테스트 마커를 생성합니다...");

    // 1. 현재 지도 중심을 기준으로 임의의 위치 3개 설정
    const mapCenter = map.getCenter();
    const testPositions = [
        new window.kakao.maps.LatLng(mapCenter.getLat() + 0.0015, mapCenter.getLng() - 0.001), // 1 mapCenter.getLat() + 0.001, mapCenter.getLng() + 0.001
        new window.kakao.maps.LatLng(mapCenter.getLat() - 0.001, mapCenter.getLng() - 0.002),  // 3 mapCenter.getLat() - 0.001, mapCenter.getLng()
        new window.kakao.maps.LatLng(mapCenter.getLat() - 0.001, mapCenter.getLng() - 0.001)    // 2 mapCenter.getLat(), mapCenter.getLng() - 0.001
    ];

    // 2. 3개의 마커를 생성하여 지도에 바로 표시
    // (이 마커들은 'markers' 배열에 추가하지 않으므로,
    // 나중에 makeMarker()가 실행되어도 지워지지 않습니다.)
    testPositions.forEach((position, index) => {
        new window.kakao.maps.Marker({
            position: position,
            map: map, // 맵 객체에 바로 표시
            title: `테스트 마커 ${index + 1}`
        });
    });
}

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

// ========================================================================================
// Computed Properties - Zone별 표시 데이터
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
    if (displayZoneLevel.value === 1) {
        return displayedZone1.value
    } else if (displayZoneLevel.value === 2) {
        return displayedZone2.value
    } else if (displayZoneLevel.value === 3) {
        return displayedZone3.value
    }
    return []
})

const hasMoreData = computed(() => {
    let totalCount = 0
    if (displayZoneLevel.value === 1) {
        totalCount = zone_level_1.value?.length || 0
    } else if (displayZoneLevel.value === 2) {
        totalCount = zone_level_2.value?.length || 0
    } else if (displayZoneLevel.value === 3) {
        totalCount = zone_level_3.value?.length || 0
    }
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

// ========================================================================================
// 지도 및 마커 관련 변수
// ========================================================================================

let map = null;
let markers = []
let polylines = []
let centerMarker = null

// ========================================================================================
// 실종자 정보 조회
// ========================================================================================

// async function fetchMissingPersonDetail() {
//     if (!missingPostId.value) {
//         console.warn('⚠️ missingPostId가 없어서 실종자 정보 조회를 건너뜁니다.')
//         personLoading.value = false
//         return
//     }

//     personLoading.value = true
//     personError.value = null

//     console.log(`실종 신고 ID로 상세 정보 조회: missingPostId=${missingPostId.value}`)
//     try {
//         const response = await axios.get(`/api/missing-persons/${missingPostId.value}`, {
//             withCredentials: true
//         })
//         personDetail.value = response.data

//         console.log('✅ 실종자 상세 정보:', personDetail.value)

//         if (response.data && response.data.reportedAt) {
//             missingTimeDB.value = new Date(response.data.reportedAt).getTime()
//             console.log('변환된 timestamp:', missingTimeDB.value)
//         }

//     } catch (err) {
//         console.error("❌ 실종자 상세 정보를 불러오는 데 실패했습니다:", err)
//         personError.value = "상세 정보를 불러올 수 없습니다."
//     } finally {
//         personLoading.value = false
//     }
// }

// 참여자 조회
async function fetchParticipants() {
    if (!missingPostId.value) {
        console.warn('⚠️ missingPostId가 없어서 참여자 조회를 건너뜁니다.')
        return
    }

    console.log(`참여자 목록 조회 시도: missingPostId=${missingPostId.value}`)
    try {
        const response = await axios.get(`/api/missing-persons/${missingPostId.value}/participants`, {
            withCredentials: true
        });

        console.log('✅ 함께 찾는 사람들:', response.data);
        if (Array.isArray(response.data)) {
            participantsCount.value = response.data.length
        } else if (response.data && typeof response.data === 'object') {
            participantsCount.value = (response.data.count ?? response.data.total ?? 0)
        } else {
            participantsCount.value = 0
        }

    } catch (error) {
        console.error("❌ 참여자 목록 조회 실패:", error);
    }
}

// 주소 조회
async function getMissingAddress() {
    console.log(`getMissingAddress 함수 호출됨 => 실종된 위치로 주소조회하는 함수`)
    
    try {
        console.log(`missingLocation으로 조회 시작 lat: ${missingLocation.value.lat}, lon: ${missingLocation.value.lon}`)

        const columns = 'sido_nm,sgg_nm,emd_nm,ri_nm,rn_nm'
        const vworldResult = await fetchVWorldData(missingLocation.value, columns)

        // API 응답 검증
        if (vworldResult.status !== 'OK' || !vworldResult.properties) {
            console.warn('VWorld API에서 주소 정보를 찾을 수 없음')
            return { sgg: '', emd: '', ri: '', roadAddress: '', fullAddress: '' }
        }

        const props = vworldResult.properties

        // 주소 조합
        const addressParts = [
            props.sgg_nm,
            props.emd_nm,
            props.ri_nm
        ].filter(Boolean)

        let fullAddress = addressParts.join(' ')
        console.log('getMissingAddress에서 받은 fullAddress:', fullAddress)

        if (props.rn_nm) {
            fullAddress += ` (${props.rn_nm})`
        }

        const result = {
            sgg: props.sgg_nm || '',
            emd: props.emd_nm || '',
            ri: props.ri_nm || '',
            roadAddress: props.rn_nm || '',
            fullAddress: fullAddress
        }

        // Vue 반응형 상태에 할당
        missingAddress.value = result
        console.log(`missingLocation => ${missingAddress.value}`)
        console.log(`조회된 주소 정보:`, result)
        return result

    } catch (error) {
        console.error(`실종자 정보에서 위경도값으로 주소 조회 중 오류 -> ${error}`)
        return { sgg: '', emd: '', ri: '', roadAddress: '', fullAddress: '' }
    }
}
// ID 찾기
async function findMissingReportId() {
    const idFromParam = route.params.id;

    if (idFromParam) {
        console.log("ID가 있습니다 (게시판 경로):", idFromParam);
        return parseInt(idFromParam, 10);
    }

    console.log("ID가 없습니다 (홈 경로). '내 환자'의 최신 신고 ID를 찾습니다.");
    try {
        console.log("[ID 찾기] '내 환자' 정보를 /api/user/my-patient 에서 조회합니다.");
        const myPatientResponse = await axios.get('/api/user/my-patient', {
            withCredentials: true
        });

        patientUserNo.value = myPatientResponse.data.userNo
        const myPatientId = myPatientResponse.data.userNo;
        if (!myPatientId) {
            throw new Error("연결된 환자 정보를 찾을 수 없습니다.");
        }

        console.log(`[ID 찾기] 환자 ID(${myPatientId})로 '최신 실종 신고'를 조회합니다.`);
        const reportResponse = await axios.get(
            `/api/missing-persons/patient/${myPatientId}/latest`,
            { withCredentials: true }
        );

        return reportResponse.data.missingPostId;

    } catch (err) {
        if (err.response && err.response.status === 404) {
            console.log("[ID 찾기] 현재 등록된 실종 신고가 없습니다. (404)");
            personError.value = "현재 등록된 실종 신고가 없습니다.";
        } else {
            console.error("❌ [ID 찾기] 실패:", err.message);
            personError.value = err.message || "정보를 불러올 수 없습니다.";
        }
        return false;
    }
}


let notMyPatientNo

// 데이터 로드
async function fetchPatientAndMissingReport() {
    personLoading.value = true;
    personError.value = null;

    try {
        console.log(`[데이터 로드] ID(${missingPostId.value})로 실종자 정보를 조회합니다.`);
        const response = await axios.get(`/api/missing-persons/${missingPostId.value}`, {
            withCredentials: true
        });

        personDetail.value = response.data;
        console.log('✅ 실종자 상세 정보:', personDetail.value);

        notMyPatientNo = personDetail.value.patientUserNo
        console.log(`내 환자가 아닐 경우의 값 : : : : : : ${notMyPatientNo}`)

        if (response.data && response.data.reportedAt) {
            missingTimeDB.value = new Date(response.data.reportedAt).getTime();
        }

        await fetchParticipants();
        return true;

    } catch (err) {
        console.error("❌ 실종자 상세 정보를 불러오는 데 실패했습니다:", err);
        personError.value = "실종 신고 정보를 불러오는 데 실패했습니다.";
        return false;
    } finally {
        personLoading.value = false;
    }
}

function formatDescription(desc) {
    if (!desc) {
        return {
            clothing: '정보 없음',      // 인상착의
            belongings: '정보 없음',   // 소지품
            specialNotes: '정보 없음'  // 특이사항
        };
    }

    let parsed = null;

    // desc가 이미 객체인지, 문자열(JSON)인지 둘 다 대응
    if (typeof desc === 'string') {
        try {
            parsed = JSON.parse(desc);
        } catch (e) {
            // JSON이 아니면 기존 문자열 파싱 로직으로 보내도 되고,
            // 여기서는 안전하게 기본값 리턴
            return {
                clothing: '정보 없음',
                belongings: '정보 없음',
                specialNotes: '정보 없음'
            };
        }
    } else if (typeof desc === 'object') {
        parsed = desc;
    } else {
        return {
            clothing: '정보 없음',
            belongings: '정보 없음',
            specialNotes: '정보 없음'
        };
    }

    return {
        // appearance: 인상착의(상의,하의,신발)
        clothing: parsed.appearance || '정보 없음',
        // items: 소지품
        belongings: parsed.items || '정보 없음',
        // other: 기타 특이사항
        specialNotes: parsed.other || '정보 없음'
    };
}

// ========================================================================================
// 카카오맵 초기화
// ========================================================================================

onMounted(async () => {
    isLoading.value = true;
    selectedType.value = 'info';

    const idToLoad = await findMissingReportId();

    if (idToLoad) {
        console.log("최종 로드할 ID:", idToLoad);
        missingPostId.value = idToLoad;

        const fetchSuccess = await fetchPatientAndMissingReport();
        await fetchPredictionData();

        if (fetchSuccess) {
            try {
                // 주형 카카오지도 await 추가
                await loadKakaoMap(mapContainer.value);
                
                // ✅ setTimeout을 Promise로 감싸서 await 가능하게
                await new Promise(resolve => setTimeout(resolve, 500));
                
                // ✅ getMissingAddress를 await로 기다림
                await getMissingAddress();
                console.log(`missingAddress => ${missingAddress.value.fullAddress}`);                
                calcElapsedTime();

                if (map) {
                    setCenter(true);
                    makeMarker();
                    initCircles();
                    showCirclesByZoneLevel(displayZoneLevel.value);
                }
            } catch (e) {
                console.error("지도 초기화 중 오류:", e);
                personError.value = "지도 로딩 중 오류가 발생했습니다.";
                isLoading.value = false;
            }
        } else {
            isLoading.value = false;
        }
    } else {
        console.log("로드할 ID가 없습니다.");
        isLoading.value = false;
    }
});


onUnmounted(() => {
    if (isParticipantsLayerVisible.value) {
        searchStore.stopSearch();
        console.log("[PredictLocation] 페이지 이탈. '함께 찾기' 스위치를 끕니다.");
    }
});

const loadKakaoMap = (container) => {
  return new Promise((resolve, reject) => {
    // 이미 Kakao SDK가 로드되어 있는지 확인
    if (window.kakao && window.kakao.maps) {
      window.kakao.maps.load(() => {
        try {
          if (!container) {
            throw new Error('Map container가 없습니다');
          }
          
          const options = {
            center: new window.kakao.maps.LatLng(
              missingLocation.value.lat || 37.494406,
              missingLocation.value.lon || 126.887701
            ),
            level: 5,
          };
          
          map = new window.kakao.maps.Map(container, options);
          console.log('✅ 지도 초기화 완료');
          
          // composable 설정
          if (setMap) setMap(map);
          
          // 중심 마커 생성
          if (missingLocation.value.lat && missingLocation.value.lon) {
            centerMarker = new window.kakao.maps.Marker({
              position: new window.kakao.maps.LatLng(
                missingLocation.value.lat,
                missingLocation.value.lon
              ),
              map: map,
              image: createCenterMarkerImage(),
            });
          }
          
          resolve(map);
        } catch (error) {
          console.error('지도 초기화 실패:', error);
          reject(error);
        }
      });
      return;
    }
    
    // 스크립트 새로 로드
    const script = document.createElement('script');
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false&libraries=services`;
    
    script.onload = () => {
      window.kakao.maps.load(() => {
        try {
          if (!container) {
            throw new Error('Map container가 없습니다');
          }
          
          const options = {
            center: new window.kakao.maps.LatLng(
              missingLocation.value.lat || 37.5666805,
              missingLocation.value.lon || 126.9784147
            ),
            level: 5,
          };
          
          map = new window.kakao.maps.Map(container, options);
          console.log('✅ 지도 초기화 완료');
          
          if (setMap) setMap(map);
          
          if (missingLocation.value.lat && missingLocation.value.lon) {
            centerMarker = new window.kakao.maps.Marker({
              position: new window.kakao.maps.LatLng(
                missingLocation.value.lat,
                missingLocation.value.lon
              ),
              map: map,
              image: createCenterMarkerImage(),
            });
          }
          
          resolve(map);
        } catch (error) {
          console.error('지도 초기화 실패:', error);
          reject(error);
        }
      });
    };
    
    script.onerror = () => {
      const error = new Error('Kakao Map 스크립트 로드 실패');
      console.error(error);
      reject(error);
    };
    
    document.head.appendChild(script);
  });
};


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
    // ⭐ 지도가 초기화되지 않았으면 중단
    if (!map) {
        console.warn('⚠️ 지도가 초기화되지 않아 마커를 생성할 수 없습니다.')
        return
    }

    // ⭐ 현재 지도 상태 저장 (중심 좌표와 줌 레벨)
    const currentCenter = map.getCenter()
    const currentLevel = map.getLevel()

    // 기존 마커 안전하게 제거
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

    // 새 마커 생성
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

            // 마커 클릭 이벤트
            window.kakao.maps.event.addListener(marker, 'click', function () {
                selectLocation(item, index)
            })

            markers.push(marker)
        } catch (error) {
            console.error(`마커 ${index} 생성 중 오류:`, error)
        }
    })

    // ⭐ 지도 상태 복원 (중심과 줌 레벨 유지)
    // 단, 선택된 위치가 있을 때는 제외
    if (!selectedLocation.value) {
        map.setCenter(currentCenter)
        map.setLevel(currentLevel)
    }

    console.log(`✅ 마커 ${markers.length}개 생성 완료 (줌 레벨: ${currentLevel} 유지)`)
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

function mapOrInfo(type) {
    selectedType.value = type
    console.log(`\n🔀 mapOrInfo 호출: ${type}`)
}

/**
 * 위치 선택 (마커/카드 클릭)
 * ⭐ 모든 경로는 미리 로드됨. 클릭 시에만 해당 경로를 표시
 */
function selectLocation(loc, index) {
    // 같은 위치를 다시 클릭하면 선택 해제
    if (selectedLocation.value &&
        selectedLocation.value.lat === loc.lat &&
        selectedLocation.value.lon === loc.lon) {
        selectedLocation.value = null
        clearAllRoutes()  // 경로 제거
        return
    }

    // 다른 위치를 클릭했을 때
    selectedLocation.value = loc

    if (map) {
        const position = new window.kakao.maps.LatLng(loc.lat, loc.lon)
        map.panTo(position)

        // ⭐ 지도 레벨 조정
        const currentLevel = map.getLevel()
        if (currentLevel > 6) {
            map.setLevel(6, { animate: true })
        }

        // ⭐ 저장된 경로 그리기 (이미 requestAllRoutes()에서 요청됨)
        drawRoute(index, displayZoneLevel.value)

        console.log(`✅ 위치 선택: ${loc.address1}, 경로 표시 (Zone ${displayZoneLevel.value})`)
    }
}



// 제보 페이지 이동
function goToReportPage() {
    console.log('제보 목록 페이지로 이동합니다...'); // 1. 로그 메시지 수정

    // 2. 현재 실종 ID (missingPostId)가 있는지  
    if (!missingPostId.value) {
        alert("현재 실종 건 ID를 찾을 수 없어 제보 목록으로 이동할 수 없습니다.");
        return;
    }

    // 3. (디버깅용) 어떤 ID를 전달하는지 콘솔에 출력
    console.log(`현재 실종 ID (${missingPostId.value})를 사용하여 제보 목록(SightingReport)으로 이동합니다.`);

    // 4. 라우터 설정('/SightingReport/:id')에 맞게 'params'로 ID 전달
    router.push({
        name: 'SightingReportBoard',
        // 라우터의 path가 ':id'이므로, params의 key도 'id'여야 합니다.
        params: { id: missingPostId.value }
    });
}

const { startParticipantTracking, stopParticipantTracking, setMap } = useParticipantLocations({
    missingPostId: missingPostId
});
const isParticipantsLayerVisible = ref(false);

function wherePeople() {
    isParticipantsLayerVisible.value = !isParticipantsLayerVisible.value;

    if (isParticipantsLayerVisible.value) {
        startParticipantTracking();

        if (missingPostId.value) {
            console.log(`[PredictLocation] '함께 찾기' 스위치를 켭니다. ID: ${missingPostId.value || '아직 로딩 중...'}`);
            searchStore.startSearch(missingPostId.value);
        }

    } else {
        stopParticipantTracking();
        console.log("[PredictLocation] '함께 찾기' 스위치를 끕니다.");
        searchStore.stopSearch();
    }
}

// ========================================================================================
// 색상 관련 헬퍼 함수
// ========================================================================================

function getProbabilityColor(probability) {
    if (probability >= 0.7) return '#4CAF50'
    if (probability >= 0.4) return '#FF9800'
    return '#F44336'
}

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
// 유틸 함수
// ========================================================================================

const elapsedTimeText = ref('')

function calcElapsedTime() {
    try {
        const missingTime = new Date(missingTimeDB.value)

        if (isNaN(missingTime.getTime())) {
            console.error('❌ 실종 시간이 유효하지 않습니다:', missingTimeDB.value)
            elapsedTimeText.value = '시간 불명'
            return
        }

        const now = new Date()
        const diffInMs = now.getTime() - missingTime.getTime()
        const diffInMinutes = Math.floor(diffInMs / (1000 * 60))
        const minutes = Math.max(0, diffInMinutes)

        // 🧩 90분 이상이면 90으로 고정
        const clampedMinutes = Math.min(minutes, 90)

        if (minutes < 60) {
            elapsedTimeText.value = `${minutes}분 전`
        } else {
            const hours = Math.floor(minutes / 60)
            elapsedTimeText.value = `약 ${hours}시간 전`
        }

        console.log(`⏱️ 경과 시간: ${minutes}분 → 표시: ${elapsedTimeText.value}`)
        setTime(clampedMinutes)

    } catch (error) {
        console.error('❌ 경과 시간 계산 중 오류:', error)
        elapsedTimeText.value = '시간 불명'
    }
}

function formatSimpleDateTime(dateString) {
    if (!dateString) return '시간 정보 없음';
    try {
        const date = new Date(dateString);
        if (isNaN(date)) return '날짜 형식 오류';

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');

        return `${year}-${month}-${day} ${hours}:${minutes}`;
    } catch (e) {
        console.error("날짜 포맷 오류:", e, dateString);
        return '날짜 형식 오류';
    }
}

function calculateAge(birthDateString) {
    if (!birthDateString) return '?';
    try {
        const birthDate = new Date(birthDateString);
        if (isNaN(birthDate)) return '?';
        const today = new Date();
        let age = today.getFullYear() - birthDate.getFullYear();
        const m = today.getMonth() - birthDate.getMonth();
        if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
            age--;
        }
        return age >= 0 ? age : '?';
    } catch (e) { return '?'; }
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
    if (minutes <= 30) {
        newLevel = 1
    } else if (minutes <= 60) {
        newLevel = 2
    } else {
        newLevel = 3
    }

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
// Watchers
// ========================================================================================

// displayZoneLevel watch - 지도 상태 유지
watch(displayZoneLevel, (newLevel, oldLevel) => {
    console.log(`Zone Level 변경: ${oldLevel} → ${newLevel}`)

    showAllLocations.value = false
    clearAllRoutes()
    selectedLocation.value = null

    if (!map) {
        console.warn('⚠️ 지도가 초기화되지 않음')
        return
    }

    try {
        makeMarker()
        showCirclesByZoneLevel(newLevel)
    } catch (error) {
        console.error('Zone Level 업데이트 중 오류:', error)
    }
})

// selectedMinutes watch - 지도 상태 유지
watch(selectedMinutes, (newMinutes) => {
    let newLevel = 1
    if (newMinutes <= 30) {
        newLevel = 1
    } else if (newMinutes <= 60) {
        newLevel = 2
    } else {
        newLevel = 3
    }

    showAllLocations.value = false
    selectedLocation.value = null
    clearAllRoutes()

    displayZoneLevel.value = newLevel

    // ✅ null 체크만 추가 (debounce 제거)
    if (!map) {
        console.warn('⚠️ 지도가 초기화되지 않음')
        return
    }

    try {
        updateMapForTime(newMinutes)
    } catch (error) {
        console.error('시간 업데이트 중 오류:', error)
    }
})

// ⭐ showAllLocations watch 추가 - 더보기 토글 시에도 지도 유지
watch(showAllLocations, (newValue) => {
    console.log(`더보기 상태 변경: ${newValue}`)

    // ✅ null 체크만 추가
    if (!map) {
        console.warn('⚠️ 지도가 초기화되지 않음')
        return
    }

    try {
        makeMarker()
    } catch (error) {
        console.error('마커 업데이트 중 오류:', error)
    }
})


// ========================================================================================
// 더보기 버튼
// ========================================================================================

function toggleShowMore() {
    showAllLocations.value = !showAllLocations.value
    console.log(`더보기 토글: ${showAllLocations.value ? '전체 보기' : '3개만 보기'}`)
}

// ========================================================================================
// Circle 관련
// ========================================================================================

const circles = ref({
    circle700: null,
    circle1500: null,
    circle2100: null
})

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

    // ⭐ 현재 지도 상태 저장
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

    // ⭐ 지도 상태 복원
    if (!selectedLocation.value) {
        map.setCenter(currentCenter)
        map.setLevel(currentLevel)
    }
}


function updateMapForTime(minutes) {
    // ⭐ null 체크
    if (!map) {
        console.log('⚠️ updateMapForTime: 지도가 초기화되지 않음')
        return
    }

    if (!circles.value.circle700 || !circles.value.circle1500 || !circles.value.circle2100) {
        console.log('⚠️ Circle이 초기화되지 않음')
        return
    }

    try {
        // ⭐ Circle 반경만 업데이트 (중심이나 줌은 변경하지 않음)
        if (minutes <= 30) {
            const radius = (minutes / 30) * 500
            circles.value.circle700.setRadius(radius)
            circles.value.circle1500.setRadius(0)
            circles.value.circle2100.setRadius(0)
        }
        else if (minutes <= 60) {
            circles.value.circle700.setRadius(500)
            const radius = 500 + ((minutes - 30) / 30) * (1000 - 500)
            circles.value.circle1500.setRadius(radius)
            circles.value.circle2100.setRadius(0)
        }
        else if (minutes <= 90) {
            circles.value.circle700.setRadius(500)
            circles.value.circle1500.setRadius(1000)
            const radius = 1000 + ((minutes - 60) / 30) * (1500 - 1000)
            circles.value.circle2100.setRadius(radius)
        }
        else {
            circles.value.circle700.setRadius(500)
            circles.value.circle1500.setRadius(1000)
            circles.value.circle2100.setRadius(1500)
        }

        // ⭐ 지도 중심과 줌은 변경하지 않음!
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

    // ⭐ force가 true일 때만 중심 이동 (초기화 시에만)
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

</script>


<style scoped>
/* 기존 CSS 동일하게 유지 */
.page-container {
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: 100%;
    height: 100%;
    margin: 0;
    padding-top: 50px;
    background: linear-gradient(180deg, #f8f9fd 0%, #ffffff 100%);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    overflow-y: auto;
    scrollbar-width: none;
}

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

.missing-person-info {
    padding: 24px 16px;
    margin-bottom: 30px;
}

.content-section {
    background: linear-gradient(180deg, #ffffff 0%, #f8f9fd 100%);
    padding: 0;
    display: flex;
    flex-direction: column;
}

.info-header-section {
    display: flex;
    gap: 18px;
    margin-bottom: 0;
    align-items: flex-start;
}

.profile-image-wrapper {
    flex-shrink: 0;
}

.profile-image {
    width: 130px;
    height: 130px;
    border-radius: 16px;
    object-fit: cover;
    background: linear-gradient(135deg, #f5f5f5 0%, #e5e5e5 100%);
    border: 3px solid #ffffff;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.basic-info-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.person-name {
    font-size: 17px;
    font-weight: 800;
    color: #171717;
    margin: 0;
    letter-spacing: -0.5px;
}

.age-info {
    font-size: 14px;
    color: #737373;
    margin: 0;
    font-weight: 500;
}

.missing-datetime,
.missing-location {
    font-size: 12px;
    color: #525252;
    margin: 0;
    font-weight: 500;
}

.detail-sections {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.info-item {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 16px;
    border-radius: 14px;
    transition: all 0.3s ease;
}

.info-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 6px 14px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.1) 100%);
    border-radius: 15px;
    flex-shrink: 0;
    border: 1px solid rgba(102, 126, 234, 0.2);
    width: fit-content;
}

.badge-label {
    font-size: 12px;
    font-weight: 700;
    color: #667eea;
    white-space: nowrap;
    letter-spacing: 0.3px;
}

.info-content {
    font-size: 14px;
    color: #333;
    flex: 1;
    font-weight: 500;
    line-height: 1.5;
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

.timeline-progress {
    position: absolute;
    top: 0;
    left: 0;
    height: 100%;
    background: linear-gradient(90deg, #667eea 0%, #667eea 33.33%, #667eea 33.33%, #667eea 66.66%, #667eea 66.66%, #667eea 100%);
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

.marker-dot {
    position: relative;
    top: 19px;
    width: 12px;
    height: 12px;
    background: white;
    border: 3px solid #667eea;
    border-radius: 50%;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.marker-dot-1 {
    position: relative;
    top: 19px;
    width: 12px;
    height: 12px;
    background: white;
    border: 3px solid #667eea;
    border-radius: 50%;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.marker-dot-2 {
    position: relative;
    top: 19px;
    width: 12px;
    height: 12px;
    background: white;
    border: 3px solid #667eea;
    border-radius: 50%;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

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

.floating-actions {
    position: absolute;
    bottom: 20px;
    right: 20px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    z-index: 100;
}

.fab {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    color: white;
    font-size: 24px;
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

.progress-ring {
    position: absolute;
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
}

.progress-ring-bg {
    fill: none;
    stroke: #e8ebf2;
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
    font-size: 12px;
    font-weight: 800;
    color: #888888;
    z-index: 1;
}

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

.stat-sublabel-modern {
    font-size: 11px;
    color: #999;
    margin: 4px 0 0 0;
}

.stat-sublabel-modern-1 {
    font-size: 14px;
    color: #3f3f3f;
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
    background: #667eea;
    border: none;
    border-radius: 20px;
    color: white;
    font-weight: 600;
    font-size: 13px;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
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

.route-controls {
    display: flex;
    justify-content: start;
    padding-top: 14px;
    border-top: 2px solid rgba(0, 0, 0, 0.05);
    gap: 10px;
}

/* Card Components */
.card-content {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.card-content {
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

.location-name {
    flex: 1;
    font-size: 14px;
    font-weight: 700;
    color: #191f28;
    margin: 0;
    line-height: 1.5;
    padding-top: 2px;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
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

.more-btn {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 44px;
    padding: 0 24px;
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

.more-btn:hover {
    border-color: #5b7cef;
    color: #5b7cef;
    background: rgba(91, 124, 239, 0.05);
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

/* '함께하는 이웃' 라벨이 활성화되었을 때 */
.info-badge .badge-label.active {
    color: #667eea;
    font-weight: 900;
}

/* '함께하는 사람 보기' 버튼이 활성화되었을 때 */
.modern-btn.active {
    background: linear-gradient(135deg, #667eea 0%, #ae8ad1 100%);
    color: white;
    box-shadow: 0 4px 15px rgba(118, 75, 162, 0.3);
    border: none;
}

/* 버튼 두 개를 감싸는 그룹 */
.button-group {
    display: flex;
    justify-content: center;
    gap: 10px;
    /* 버튼 사이의 간격 */
    width: 100%;
}

/* '제보하기' 버튼 스타일 (주황색 계열) */
.report-btn {
    background: linear-gradient(135deg, #667eea 0%);
    color: white;
    border: none;
    flex-grow: 1;
}

/* '함께하는 사람 보기' 버튼도 동일하게 공간을 나눠가지도록 */
.modern-btn {
    flex-grow: 1;
}
</style>
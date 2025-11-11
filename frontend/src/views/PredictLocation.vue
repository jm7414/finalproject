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
        <div class="page-container">
            <div ref="mapContainer" class="map-area"></div>
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
                            실종장소: {{ missingAddress.fullAddress }}
                        </p>
                    </div>
                </div>

                <div v-if="!isLoading" class="detail-sections">
                    <div class="info-item glass-card">

                        <div>
                            <div class="info-badge">
                                <i class="bi bi-person-badge"></i>
                                <span class="badge-label">신체 특징</span>
                            </div>
                            <span class="info-content">{{ formatDescription(personDetail.description).physicalFeatures
                                || '170cm 마른 체형' }}</span>
                        </div>

                        <div>
                            <div class="info-badge">
                                <i class="bi bi-bag"></i>
                                <span class="badge-label">착의사항</span>
                            </div>
                            <span class="info-content">{{ formatDescription(personDetail.description).clothing || '정보없음'
                                }}</span>
                        </div>

                        <div>
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
                            <span class="info-content">{{ (personDetail && personDetail.searchTogetherCount != null) ?
                                personDetail.searchTogetherCount : participantsCount }}명</span>
                            <div class="d-flex justify-content-center">
                                <button class="btn btn-info modern-btn" :class="{ active: isParticipantsLayerVisible }"
                                    @click="wherePeople">
                                    <i class="bi bi-arrow-right-circle"></i>
                                    {{ isParticipantsLayerVisible ? '함께하는 중...' : '함께하는 사람 보기' }}
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
                                    {{ loc.address1 || '주소 정보 없음' }}에 있는
                                </h4>
                                <p class="location-detail">
                                    {{ loc.address2 }}
                                </p>
                            </div>

                            <div class="probability-badge-modern" :style="{
                                '--progress': loc.value,
                                '--color': getProbabilityColor(loc.value)
                            }">
                                <svg class="progress-ring" viewBox="0 0 36 36">
                                    <circle class="progress-ring-bg" cx="18" cy="18" r="15.915" />
                                    <circle class="progress-ring-progress" cx="18" cy="18" r="15.915"
                                        :style="{ strokeDashoffset: 100 - (loc.value * 100) }" />
                                </svg>
                                <span class="probability-text">{{ (loc.value * 40).toFixed(0) }}%</span>
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

                <!-- ⭐ 더보기 버튼 -->
                <div class="d-flex justify-content-center" v-if="hasMoreData">
                    <button class="more-btn" @click="toggleShowMore">
                        <span>{{ showAllLocations ? '접기' : '더보기' }}</span>
                        <i :class="showAllLocations ? 'bi bi-chevron-up' : 'bi bi-chevron-down'"></i>
                    </button>
                </div>

                <!-- 통계 대시보드 -->
                <div class="stats-dashboard-modern glass-card" v-if="predictionData.metadata && !isLoading">
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
                                <p class="stat-value-modern">{{ predictionData.metadata.dbscan_clusters }}<span
                                        class="stat-unit">개의 위치 분석 결과</span>
                                </p>
                                <p class="stat-sublabel-modern">랜덤 클러스터 {{ predictionData.metadata.random_candidate }}개
                                    포함
                                </p>
                                <p class="stat-sublabel-modern-1">각 시간 별 확률이 높은 상위 10개 지역을 보여줍니다</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { useRoute } from 'vue-router';
import axios from 'axios'
import { useParticipantLocations } from '@/composables/useParticipantLocations';
import { useSearchStore } from '@/stores/useSearchStore';

const route = useRoute();
const searchStore = useSearchStore(); // 함께찾는 사람들

// ========================================================================================
// jjamTong 데이터 정의
// ========================================================================================
const jjamTong = {
    "metadata": {
        "total_points": 30,
        "dbscan_clusters": 193,
        "random_candidates": 0,
        "range_50_700_count": 10,
        "range_700_1500_count": 10,
        "range_1500_plus_count": 10,
        "max_prob": 0.78,
        "mean_prob": 0.5980000000000001,
        "confidence_score": 0.7176000000000001,
        "prediction_quality": "medium",
        "missing_center_lat": 37.236521788023126,
        "missing_center_lon": 126.68079566651024,
        "total_waypoints_generated": 918
    },
    "zone_level_1": [
        {
            "lat": 37.23386744768245,
            "lon": 126.68298938931824,
            "value": 0.78,
            "address1": "화성시 송산면 마산리",
            "address2": "도로에 있을 것 같아요!",
            "jimok": "도로",
            "dist_m": 234
        },
        {
            "lat": 37.23473450571951,
            "lon": 126.68172589255347,
            "value": 0.77,
            "address1": "화성시 송산면 고포리",
            "address2": "밭에 있을 것 같아요!",
            "jimok": "전",
            "dist_m": 198
        },
        {
            "lat": 37.236064384993135,
            "lon": 126.68040911649587,
            "value": 0.77,
            "address1": "화성시 송산면 고포리",
            "address2": "도로에 있을 것 같아요!",
            "jimok": "도로",
            "dist_m": 56
        },
        {
            "lat": 37.233594795673866,
            "lon": 126.68180504411481,
            "value": 0.72,
            "address1": "화성시 송산면 고포리",
            "address2": "도로에 있을 것 같아요!",
            "jimok": "공장용지",
            "dist_m": 345
        },
        {
            "lat": 37.234242769263574,
            "lon": 126.68155074945578,
            "value": 0.72,
            "address1": "화성시 송산면 고포리",
            "address2": "밭에 있을 것 같아요!",
            "jimok": "전",
            "dist_m": 278
        },
        {
            "lat": 37.23364779713962,
            "lon": 126.68204967212515,
            "value": 0.71,
            "address1": "화성시 송산면 마산리",
            "address2": "산에 있을 것 같아요!",
            "jimok": "임야",
            "dist_m": 345
        },
        {
            "lat": 37.2337011948454,
            "lon": 126.68230532951101,
            "value": 0.71,
            "address1": "화성시 송산면 마산리",
            "address2": "산에 있을 것 같아요!",
            "jimok": "임야",
            "dist_m": 367
        },
        {
            "lat": 37.23373907697495,
            "lon": 126.68246972357231,
            "value": 0.71,
            "address1": "화성시 송산면 마산리",
            "address2": "산에 있을 것 같아요!",
            "jimok": "임야",
            "dist_m": 389
        },
        {
            "lat": 37.23324212017859,
            "lon": 126.68498401738543,
            "value": 0.71,
            "address1": "화성시 송산면 마산리",
            "address2": "산에 있을 것 같아요!",
            "jimok": "임야",
            "dist_m": 445
        },
        {
            "lat": 37.23471852524021,
            "lon": 126.68222587430238,
            "value": 0.69,
            "address1": "화성시 송산면 고포리",
            "address2": "밭에 있을 것 같아요!",
            "jimok": "전",
            "dist_m": 234
        }
    ],
    "zone_level_2": [
        {
            "lat": 37.23323930399239,
            "lon": 126.68628356544302,
            "value": 0.71,
            "address1": "화성시 송산면 마산리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 567
        },
        {
            "lat": 37.23773664004988,
            "lon": 126.68838875066,
            "value": 0.66,
            "address1": "화성시 송산면 마산리",
            "address2": "논에 있을 것 같아요!",
            "dist_m": 890
        },
        {
            "lat": 37.24098809803297,
            "lon": 126.67411064074422,
            "value": 0.64,
            "address1": "화성시 송산면 고포리",
            "address2": "산에 있을 것 같아요!",
            "dist_m": 1023
        },
        {
            "lat": 37.24021068811336,
            "lon": 126.67280502449263,
            "value": 0.62,
            "address1": "화성시 송산면 고포리",
            "address2": "산에 있을 것 같아요!",
            "dist_m": 1156
        },
        {
            "lat": 37.23263755259063,
            "lon": 126.68723125441413,
            "value": 0.56,
            "address1": "화성시 송산면 마산리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 723
        },
        {
            "lat": 37.240100908879924,
            "lon": 126.67342624798773,
            "value": 0.54,
            "address1": "화성시 송산면 고포리",
            "address2": "산에 있을 것 같아요!",
            "dist_m": 1089
        },
        {
            "lat": 37.23936035519318,
            "lon": 126.6683902692904,
            "value": 0.54,
            "address1": "화성시 송산면 고포리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 956
        },
        {
            "lat": 37.23893929194048,
            "lon": 126.6676173348053,
            "value": 0.54,
            "address1": "화성시 송산면 고포리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 845
        },
        {
            "lat": 37.23929383920692,
            "lon": 126.66818825581989,
            "value": 0.53,
            "address1": "화성시 송산면 고포리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 923
        },
        {
            "lat": 37.23914128334645,
            "lon": 126.6679221012685,
            "value": 0.53,
            "address1": "화성시 송산면 고포리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 901
        }
    ],
    "zone_level_3": [
        {
            "lat": 37.22543734319893,
            "lon": 126.69016326145689,
            "value": 0.54,
            "address1": "화성시 송산면 마산리",
            "address2": "산에 있을 것 같아요!",
            "dist_m": 1456
        },
        {
            "lat": 37.24192953961568,
            "lon": 126.69410939372973,
            "value": 0.52,
            "address1": "화성시 송산면 마산리",
            "address2": "논에 있을 것 같아요!",
            "dist_m": 1678
        },
        {
            "lat": 37.23750297607449,
            "lon": 126.66486868128675,
            "value": 0.49,
            "address1": "화성시 송산면 고포리",
            "address2": "밭에 있을 것 같아요!",
            "dist_m": 1289
        },
        {
            "lat": 37.24999046886826,
            "lon": 126.67868641144558,
            "value": 0.49,
            "address1": "화성시 송산면 독지리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 1534
        },
        {
            "lat": 37.22972060832949,
            "lon": 126.6936845850643,
            "value": 0.48,
            "address1": "화성시 송산면 마산리",
            "address2": "논에 있을 것 같아요!",
            "dist_m": 1445
        },
        {
            "lat": 37.25001328961985,
            "lon": 126.67899324477477,
            "value": 0.48,
            "address1": "화성시 송산면 독지리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 1556
        },
        {
            "lat": 37.22586225328711,
            "lon": 126.69257662930521,
            "value": 0.45,
            "address1": "화성시 송산면 마산리",
            "address2": "밭에 있을 것 같아요!",
            "dist_m": 1623
        },
        {
            "lat": 37.23075491608878,
            "lon": 126.69429967424405,
            "value": 0.45,
            "address1": "화성시 송산면 마산리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 1467
        },
        {
            "lat": 37.226376899999835,
            "lon": 126.69353680840648,
            "value": 0.44,
            "address1": "화성시 송산면 마산리",
            "address2": "밭에 있을 것 같아요!",
            "dist_m": 1578
        },
        {
            "lat": 37.22722326107302,
            "lon": 126.69367199250485,
            "value": 0.44,
            "address1": "화성시 송산면 마산리",
            "address2": "도로에 있을 것 같아요!",
            "dist_m": 1489
        }
    ]
}

// ========================================================================================
// 카카오지도 및 API 키 설정
// ========================================================================================
const mapContainer = ref(null)

// 카카오맵 API 키
const KAKAO_JS_KEY = '7e0332c38832a4584b3335bed6ae30d8'

// VWorld API Key
const VWORLD_API_KEY = '6A0CFFEF-45CF-3426-882D-44A63B5A5289'

// Tmap API Key
const TMAP_API_KEY = 'pu1CWi6rz48GHLWhk7NI239il6I2j9fHaSLFeYoi'

// ========================================================================================
// 데이터 상태 관리
// ========================================================================================

// ⭐ jjamTong 데이터를 predictionData에 할당
const predictionData = ref(jjamTong)

// 실종위치 중심으로 바꾸기위한 반응형변수
const missingLocation = ref({
    lat: jjamTong.metadata.missing_center_lat,
    lon: jjamTong.metadata.missing_center_lon
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

// 실종 시간 (예시)
const missingTime = '2025-10-20 09:30'

// ⭐ 드래그 가능한 타임라인 관련 상태
const selectedMinutes = ref(30) // 0~90 사이의 분 단위 값
const isDragging = ref(false)
const timelineWrapper = ref(null)

// ⭐ 더보기 관련 상태 추가
const showAllLocations = ref(false)

// ============================================================================
// ID 관리 변수 - 환자 번호와 실종 신고 ID를 명확히 구분
// ============================================================================
const patientUserNo = ref(null)
const missingPostId = ref(null)

// 병욱 작업공간 확보 시작 
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

const personDetail = ref(null)
const personLoading = ref(true)
const personError = ref(null)
const defaultPersonImage = '@/default-person.png'
const participantsCount = ref(0)

// 시간 변수
const missingTimeDB = ref(null)

async function fetchMissingPersonDetail() {
    if (!missingPostId.value) {
        console.warn('⚠️ missingPostId가 없어서 실종자 정보 조회를 건너뜁니다.')
        personLoading.value = false
        return
    }

    personLoading.value = true
    personError.value = null

    console.log(`실종 신고 ID로 상세 정보 조회: missingPostId=${missingPostId.value}`)
    try {
        const response = await axios.get(`/api/missing-persons/${missingPostId.value}`, {
            withCredentials: true
        })
        personDetail.value = response.data

        console.log('✅ 실종자 상세 정보:', personDetail.value)

        if (response.data && response.data.reportedAt) {
            missingTimeDB.value = new Date(response.data.reportedAt).getTime()
            console.log('변환된 timestamp:', missingTimeDB.value)
        }

    } catch (err) {
        console.error("❌ 실종자 상세 정보를 불러오는 데 실패했습니다:", err)
        personError.value = "상세 정보를 불러올 수 없습니다."
    } finally {
        personLoading.value = false
    }
}

const missingAddress = ref(null)
let fullAddress = ''

async function getMissingAddress() {
    try {
        console.log(`missingLocation으로 조회 시작 lat : ${missingLocation.value.lat}, lon : ${missingLocation.value.lon}`)

        const columns = 'sido_nm, sgg_nm, emd_nm , ri_nm, rn_nm'

        const dataParams = new URLSearchParams({
            service: 'data',
            version: '2.0',
            request: 'GetFeature',
            format: 'json',
            errorformat: 'json',
            size: '10',
            page: '1',
            data: 'LT_C_LANDINFOBASEMAP',
            geomfilter: `POINT(${missingLocation.value.lon} ${missingLocation.value.lat})`,
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
            console.error(`VWorld Data API HTTP error! status: ${dataRes.status}`)
            return {
                sgg: '',
                emd: '',
                ri: '',
                roadAddress: ''
            }
        }

        const data = await dataRes.json()

        if (data.response?.status === 'OK' && data.response?.result?.featureCollection?.features?.length > 0) {
            const feature = data.response.result.featureCollection.features[0]
            const props = feature.properties

            const addressParts = [
                props.sgg_nm,
                props.emd_nm,
                props.ri_nm
            ].filter(Boolean)

            fullAddress = addressParts.join(' ')

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

            missingAddress.value = result
            console.log(`조회된 주소 정보:`, result)
            return result

        } else {
            console.warn('VWorld API에서 주소 정보를 찾을 수 없음')
            return {
                sgg: '',
                emd: '',
                ri: '',
                roadAddress: ''
            }
        }

    } catch (error) {
        console.error(`실종자 정보에서 위경도값으로 주소 조회중 오류 -> ${error}`)
        return {
            sgg: '',
            emd: '',
            ri: '',
            roadAddress: ''
        }
    }
}

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

        if (minutes < 60) {
            elapsedTimeText.value = `${minutes}분 전`
        } else {
            const hours = Math.floor(minutes / 60)
            elapsedTimeText.value = `약 ${hours}시간 전`
        }

        console.log(`⏱️ 경과 시간: ${minutes}분 → 표시: ${elapsedTimeText.value}`)
        setTime(minutes)

    } catch (error) {
        console.error('❌ 경과 시간 계산 중 오류:', error)
        elapsedTimeText.value = '시간 불명'
    }
}

// 홈에서 왔을때 연결된 환자의 ID를 확인하는 함수
async function findMissingReportId() {
    const idFromParam = route.params.id;

    if (idFromParam) {
        // 경로 - 게시판
        console.log("ID가 있습니다 (게시판 경로):", idFromParam);
        return parseInt(idFromParam, 10);
    }

    // 경로 - 홈
    console.log("ID가 없습니다 (홈 경로). '내 환자'의 최신 신고 ID를 찾습니다.");
    try {
        // 내 환자 정보 조회
        console.log("[ID 찾기] '내 환자' 정보를 /api/user/my-patient 에서 조회합니다.");
        const myPatientResponse = await axios.get('/api/user/my-patient', {
            withCredentials: true
        });

        const myPatientId = myPatientResponse.data.userNo;
        if (!myPatientId) {
            throw new Error("연결된 환자 정보를 찾을 수 없습니다.");
        }

        // 환자 ID로 최신 실종 신고 조회
        console.log(`[ID 찾기] 환자 ID(${myPatientId})로 '최신 실종 신고'를 조회합니다.`);
        const reportResponse = await axios.get(
            `/api/missing-persons/patient/${myPatientId}/latest`,
            { withCredentials: true }
        );

        // 실종 신고 ID 반환
        return reportResponse.data.missingPostId;

    } catch (err) {
        if (err.response && err.response.status === 404) {
            console.log("[ID 찾기] 현재 등록된 실종 신고가 없습니다. (404)");
            personError.value = "현재 등록된 실종 신고가 없습니다.";
        } else {
            console.error("❌ [ID 찾기] 실패:", err.message);
            personError.value = err.message || "정보를 불러올 수 없습니다.";
        }
        return null;
    }
}

// 실종자의 정보를 조회하는 함수
async function fetchPatientAndMissingReport() {
    // (이 함수는 missingPostId.value가 있다는 것이 보장될 때 호출됨)

    personLoading.value = true;
    personError.value = null; // (오류 메시지 초기화 - ID 찾기 오류를 덮어쓰기 위함)

    try {
        console.log(`[데이터 로드] ID(${missingPostId.value})로 실종자 정보를 조회합니다.`);
        const response = await axios.get(`/api/missing-persons/${missingPostId.value}`, {
            withCredentials: true
        });

        personDetail.value = response.data;
        console.log('✅ 실종자 상세 정보:', personDetail.value);

        if (response.data && response.data.reportedAt) {
            missingTimeDB.value = new Date(response.data.reportedAt).getTime();
        }

        fetchParticipants(); // 참가자 조회
        return true; // 성공

    } catch (err) {
        console.error("❌ 실종자 상세 정보를 불러오는 데 실패했습니다:", err);
        personError.value = "실종 신고 정보를 불러오는 데 실패했습니다.";
        return false; // 실패
    } finally {
        personLoading.value = false;
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

function formatDescription(desc) {
    if (!desc) {
        return {
            physicalFeatures: '정보 없음',
            clothing: '정보 없음',
            specialNotes: '정보 없음'
        };
    }

    const lines = String(desc).split('\\n');

    const result = {
        physicalFeatures: '',
        clothing: '',
        specialNotes: ''
    };

    lines.forEach(line => {
        if (line.includes(':')) {
            const [key, ...valueParts] = line.split(':');
            const value = valueParts.join(':').trim();

            if (key.includes('인상착의') || key.includes('착의사항')) {
                result.clothing = value;
            } else if (key.includes('신체') || key.includes('체형')) {
                result.physicalFeatures = value;
            } else if (key.includes('특이사항') || key.includes('특이')) {
                result.specialNotes = value;
            }
        }
    });

    return result;
}

// ========================================================================================
// Computed Properties
// ========================================================================================

const displayedZone1 = computed(() => {
    const data = predictionData.value.zone_level_1 || []
    return showAllLocations.value ? data.slice(0, 10) : data.slice(0, 3)
})

const displayedZone2 = computed(() => {
    const data = predictionData.value.zone_level_2 || []
    return showAllLocations.value ? data.slice(0, 10) : data.slice(0, 3)
})

const displayedZone3 = computed(() => {
    const data = predictionData.value.zone_level_3 || []
    return showAllLocations.value ? data.slice(0, 10) : data.slice(0, 3)
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
        totalCount = predictionData.value.zone_level_1?.length || 0
    } else if (displayZoneLevel.value === 2) {
        totalCount = predictionData.value.zone_level_2?.length || 0
    } else if (displayZoneLevel.value === 3) {
        totalCount = predictionData.value.zone_level_3?.length || 0
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
// 더보기 버튼 클릭 함수
// ========================================================================================
function toggleShowMore() {
    showAllLocations.value = !showAllLocations.value
    console.log(`더보기 토글: ${showAllLocations.value ? '전체 보기' : '3개만 보기'}`)

    if (map.value) {
        makeMarker()
    }
}

// ========================================================================================
// 타임라인 관련 함수
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
// 시간 체크 및 Zone Level 변경
// ========================================================================================

watch(displayZoneLevel, (newLevel, oldLevel) => {
    showAllLocations.value = false

    clearAllRoutes()
    selectedLocation.value = null

    if (map.value) {
        makeMarker()
        showCirclesByZoneLevel(newLevel)
    }
})

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

    if (map.value && selectedType.value === 'map' || 'info') {
        updateMapForTime(newMinutes)
    }
})

// ========================================================================================
// 지도 및 마커 관련 변수
// ========================================================================================

const map = ref(null)
let markers = []
let polylines = []
let centerMarker = null

// Test용 사용자 정보
const user_no = 3

// ========================================================================================
// Circle 관련 변수 및 함수
// ========================================================================================

const circles = ref({
    circle700: null,
    circle1500: null,
    circle2100: null
})

function initCircles() {
    if (!map.value || !missingLocation.value.lat || !missingLocation.value.lon) {
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

    if (!map.value) {
        console.error('지도가 초기화되지 않았습니다.')
        return
    }

    hideCircles()

    if (level >= 1 && circles.value.circle700) {
        circles.value.circle700.setMap(map.value)
    }

    if (level >= 2 && circles.value.circle1500) {
        circles.value.circle1500.setMap(map.value)
    }

    if (level >= 3 && circles.value.circle2100) {
        circles.value.circle2100.setMap(map.value)
    }

    updateMapForTime(selectedMinutes.value)
}

function updateMapForTime(minutes) {
    if (!map.value || !circles.value.circle700) {
        console.log('⚠️ updateMapForTime: 지도 또는 Circle이 초기화되지 않음')
        return
    }

    if (minutes <= 30) {
        const radius = (minutes / 30) * 600
        circles.value.circle700.setRadius(radius)

        if (circles.value.circle1500) circles.value.circle1500.setRadius(0)
        if (circles.value.circle2100) circles.value.circle2100.setRadius(0)
    }
    else if (minutes <= 60) {
        circles.value.circle700.setRadius(600)

        const radius = 600 + ((minutes - 30) / 30) * (1300 - 600)
        if (circles.value.circle1500) {
            circles.value.circle1500.setRadius(radius)
        }

        if (circles.value.circle2100) circles.value.circle2100.setRadius(0)
    }
    else if (minutes <= 90) {
        circles.value.circle700.setRadius(600)

        if (circles.value.circle1500) {
            circles.value.circle1500.setRadius(1300)
        }

        const radius = 1300 + ((minutes - 60) / 30) * (2000 - 1300)
        if (circles.value.circle2100) {
            circles.value.circle2100.setRadius(radius)
        }
    }
    else {
        circles.value.circle700.setRadius(600)

        if (circles.value.circle1500) {
            circles.value.circle1500.setRadius(1300)
        }

        if (circles.value.circle2100) {
            circles.value.circle2100.setRadius(2000)
        }
    }
}

// ========================================================================================
// 초기화 함수 (jjamTong 데이터 사용)
// ========================================================================================

async function initializeWithJjamTong() {
    console.log('🎯 jjamTong 데이터로 초기화 시작...')

    isLoading.value = true

    try {
        // 실종 위치 설정
        missingLocation.value.lat = jjamTong.metadata.missing_center_lat
        missingLocation.value.lon = jjamTong.metadata.missing_center_lon

        // 주소 조회
        await getMissingAddress()

        // 경과 시간 계산
        calcElapsedTime()

        // 지도 중심 설정
        setCenter()

        // 마커 생성
        makeMarker()

        // Circle 초기화 및 표시
        initCircles()
        if (selectedType.value === 'map' || selectedType.value === 'info') {
            showCirclesByZoneLevel(displayZoneLevel.value)
        }

        console.log('✅ jjamTong 데이터 초기화 완료')

    } catch (error) {
        console.error('❌ 초기화 중 오류 발생:', error)
    } finally {
        isLoading.value = false
    }
}

// ========================================================================================
// 카카오맵 초기화
// ========================================================================================

onMounted(async () => {
    isLoading.value = true;
    selectedType.value = 'info';

    // 불러올 ID 찾기 
    const idToLoad = await findMissingReportId();

    // ID 찾기 결과에 따라서 갈라짐
    if (idToLoad) {
        // ID를 찾았음
        console.log("최종 로드할 ID:", idToLoad);
        missingPostId.value = idToLoad; // ⭐ 핵심: 찾은 ID를 state에 저장

        // 4 찾은 ID로 실제 데이터 불러오기
        const fetchSuccess = await fetchPatientAndMissingReport();

        if (fetchSuccess) {
            // 데이터 로드 성공 -> 지도 그리기
            try {
                loadKakaoMap(mapContainer.value);
                setTimeout(() => initializeWithJjamTong(), 1000);
            } catch (e) {
                console.error("지도 초기화 중 오류:", e);
                personError.value = "지도 로딩 중 오류가 발생했습니다.";
                isLoading.value = false;
            }
        } else {
            // 데이터 로드 실패 (ID는 맞았는데 API가 실패)
            isLoading.value = false; // 로딩 끄기
        }
    } else {
        // ID를 못 찾았음 (신고가 없거나(404), 환자 연결이 없거나)
        console.log("로드할 ID가 없습니다. (신고 없음 또는 오류)");
        isLoading.value = false; // 로딩 끄기
    }
});

onUnmounted(() => {
    if (isParticipantsLayerVisible.value) {
        searchStore.stopSearch();
        console.log("[PredictLocation] 페이지 이탈. '함께 찾기' 스위치를 끕니다.");
    }
});

const loadKakaoMap = (container) => {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false&libraries=services`
    document.head.appendChild(script)

    script.onload = () => {
        window.kakao.maps.load(() => {
            const options = {
                center: new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon),
                level: 5,
            }

            map.value = new window.kakao.maps.Map(container, options)
            console.log('지도 초기화 완료')

            if (missingLocation.value.lat && missingLocation.value.lon) {
                centerMarker = new window.kakao.maps.Marker({
                    position: new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon),
                    map: map.value,
                    image: createCenterMarkerImage()
                })
            }
        })
    }
}

function setCenter() {
    if (!map.value) {
        console.error('지도가 아직 초기화되지 않았습니다.')
        return
    }

    const moveLatLon = new window.kakao.maps.LatLng(
        missingLocation.value.lat,
        missingLocation.value.lon
    )

    map.value.setCenter(moveLatLon)

    if (centerMarker) {
        centerMarker.setPosition(moveLatLon)
    } else {
        centerMarker = new window.kakao.maps.Marker({
            position: moveLatLon,
            map: map.value,
            image: createCenterMarkerImage()
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
        {
            offset: new window.kakao.maps.Point(25, 58)
        }
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
    hideMarkers()

    markerDataToDisplay.value.forEach((item, index) => {
        const markerPosition = new window.kakao.maps.LatLng(item.lat, item.lon)

        const markerColor = getMarkerColor(item.zoneLevel)

        const marker = new window.kakao.maps.Marker({
            position: markerPosition,
            map: map.value,
            title: `Zone ${item.zoneLevel} - 위치 ${index + 1} (확률: ${(item.value * 100).toFixed(1)}%)`,
            image: createColoredMarkerImage(markerColor)
        })

        markers.push(marker)
    })
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
// 경로 관련 함수 (경로는 생략 - 필요시 Tmap API 호출 구현)
// ========================================================================================

const zone1Routes = ref([])
const zone2Routes = ref([])
const zone3Routes = ref([])

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
    console.log(`경로 그리기: Zone ${zoneLevel}-${index}`)
    // ⭐ 경로 그리기는 Tmap API를 사용하거나 별도 구현 필요
    // 여기서는 생략
}

// ========================================================================================
// UI 관련 함수
// ========================================================================================

function mapOrInfo(type) {
    selectedType.value = type
    console.log(`\n🔀 mapOrInfo 호출: ${type}`)
}

function selectLocation(loc, index) {
    if (selectedLocation.value &&
        selectedLocation.value.lat === loc.lat &&
        selectedLocation.value.lon === loc.lon) {
        clearAllRoutes()
        selectedLocation.value = null
        return
    }

    clearAllRoutes()
    selectedLocation.value = loc

    if (map.value) {
        const position = new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon)
        map.value.panTo(position)
        map.value.setLevel(6)
    }

    drawRoute(index, displayZoneLevel.value)
}

const { startParticipantTracking, stopParticipantTracking } = useParticipantLocations({
    map: map, // (map.value 아님)
    missingPostId: missingPostId
});
const isParticipantsLayerVisible = ref(false);  // 함께하는 사람 마커용


function wherePeople() { // ParticipantsLayer.vue 컴포넌트로 이동
    isParticipantsLayerVisible.value = !isParticipantsLayerVisible.value;

    if (isParticipantsLayerVisible.value) {
        // --- [ON] 버튼을 켰을 때 ---

        // 1. (기존) 다른 사람들 위치 *조회* 시작
        startParticipantTracking();

        // 2. (신규) '나의' 위치 *전송* 시작
        // (App.vue의 useMyCurrentLocation 엔진을 켬)
        if (missingPostId.value) {
            console.log(`[PredictLocation] '함께 찾기' 스위치를 켭니다. ID: ${missingPostId.value}`);
            searchStore.startSearch(missingPostId.value);
        }

    } else {
        // --- [OFF] 버튼을 껐을 때 ---

        // 1. (기존) 다른 사람들 위치 *조회* 중지
        stopParticipantTracking();

        // 2. (신규) '나의' 위치 *전송* 중지
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
    position: fixed;
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
    padding: 8px 14px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.1) 100%);
    border-radius: 20px;
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

.timeline-handle:hover .handle-icon {
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
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
    width: 44px;
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
    font-size: 13px;
    font-weight: 800;
    color: var(--color);
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
</style>

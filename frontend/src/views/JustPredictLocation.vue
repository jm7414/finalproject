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
            <div v-if="less_data.value" class="">
                <p>관리하고있는 환자에 대한 데이터가 부족해요.</p>
                <span>예측 위치들이 부정확할 수 있습니다.</span>
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
                            실종장소: {{ missingAddress.fullAddress }}
                        </p>
                    </div>
                </div>

                <div v-if="!isLoading" class="detail-sections">
                    <div class="info-item glass-card">

                        <div class="d-flex align-items-center gap-1">
                            <div class="info-badge">
                                <i class="bi bi-person-badge"></i>
                                <span class="badge-label">신체 특징</span>
                            </div>
                            <span class="info-content">{{ formatDescription(personDetail.description).physicalFeatures
                                || '170cm 마른 체형' }}</span>
                        </div>

                        <div class="d-flex align-items-center gap-1">
                            <div class="info-badge">
                                <i class="bi bi-bag"></i>
                                <span class="badge-label">착의사항</span>
                            </div>
                            <span class="info-content">{{ formatDescription(personDetail.description).clothing || '정보없음'
                                }}</span>
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
                                null) ?
                                personDetail.searchTogetherCount : participantsCount }}명</span>
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
        <AgentSimulationModal :isVisible="showAgentSimulation" :userNo="patientUserNo"
            :missingLocation="missingLocation" :missingTime="missingTimeDB" @close="closeAgentSimulation" />
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios'
import { useParticipantLocations } from '@/composables/useParticipantLocations';
import { useSearchStore } from '@/stores/useSearchStore';
import AgentSimulationModal from '@/components/AgentSimulationModal.vue'

// ========================================================================================
// 카카오지도 및 API 키 설정
// ========================================================================================
const mapContainer = ref(null)
const KAKAO_JS_KEY = '7e0332c38832a4584b3335bed6ae30d8'
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
    // ⭐ 필수 값 유효성 검사
    if (!patientUserNo.value) {
        console.error('❌ patientUserNo가 없습니다:', patientUserNo.value)
        alert('환자 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.')
        return
    }

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
async function fetchPredictionData() {

    console.log(`fetchPredictionData 실행됨`)

    const missingTime = formatSimpleDateTime(missingTimeDB.value).toString();

    if (patientUserNo.value && !notMyPatientNo) {
        userNo = patientUserNo.value
    } else {
        userNo = notMyPatientNo
    }

    try {
        // GPS 데이터 가져오기
        const gpsResponse = await axios.get(`/api/pred/${userNo}`, {
            params: {
                datetime: new Date(missingTimeDB.value).getTime()
            },
            withCredentials: true
        });

        const gpsData = gpsResponse.data;

        console.log(`fetchPrediction GPS DATA ::::: ${JSON.stringify(gpsData)}`);

        // 일주일간 데이터 부족시 뒤로 돌아가기 만들음
        if (gpsData.length < 3 * 20 * 24 * 7) {
            const lastGPSData = gpsData[gpsData.length - 1];
            console.log(`lastGPSData => ${JSON.stringify(lastGPSData)}`)
            if (confirm(`환자의 위치데이터가 부족하여 시뮬레이션만 볼 수 있습니다. 페이지를 이동합니다.`)) {
                router.push({
                    path: '/simulation',
                    query: {
                        userNo: userNo,
                        lat: lastGPSData.latitude,
                        lon: lastGPSData.longitude,
                        missingTime: missingTimeDB.value
                    }
                });
            } else {
                router.push(`/GD`)
            }


            return false;
        } else if (gpsData.length < 3 * 20 * 24 * 28) {
            lessData.value = true;
        }

        // ⭐ 카멜케이스 → 스네이크케이스 변환 + 초 추가
        const gpsRecords = gpsData.map(record => {
            let recordTime = record.recordTime;  // ⭐ camelCase

            // 초가 없으면 추가
            if (recordTime && recordTime.split(':').length === 2) {
                recordTime = `${recordTime}:00`;
            }

            return {
                latitude: record.latitude,
                longitude: record.longitude,
                record_time: recordTime  // ⭐ snake_case로 변환
            };
        });

        // Request Body 생성
        const requestBody = {
            user_no: userNo,
            missing_time: missingTime,
            gps_data: gpsRecords,
            analysis_days: 60,
            time_window_hours: 3,
            session_gap: 30,
            min_cluster_size: 10,
            max_search_radius: 2000,
            min_cluster_separation: 200,
            road_network_radius: 2500,
            csv_path: 'all_locations.csv'
        };


        // POST 요청
        const response = await axios.post(
            `http://localhost:8000/api/predict-destinations`,
            requestBody,
            {
                withCredentials: true,
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );

        const data = response.data;

        // 데이터 충분성 체크
        if (data.data_sufficiency === 'nono') {
            alert(`데이터가 충분하지않아 예상위치가 제공되지 않습니다. 홈으로 돌아갑니다.`);
            router.push(`/GD_main`);
        } else if (data.data_sufficiency === 'no') {
            less_data.value = true;
        }

        console.log(`총 클러스터 수 : ${data.total_clusters_found}`);

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
    console.log('🔄 API 응답 처리 시작...')

    if (apiResponse.last_known_location) {
        lastKnownLocation.value = apiResponse.last_known_location
        missingLocation.value.lat = apiResponse.last_known_location.latitude
        missingLocation.value.lon = apiResponse.last_known_location.longitude
    }

    const destinationsByDistance = apiResponse.destinations_by_distance || {}

    // ⭐ address1, address2를 초기 구조에 포함
    zone_level_1.value = (destinationsByDistance['500m'] || []).map((dest, index) => ({
        id: dest.destination_id,
        lat: dest.latitude,
        lon: dest.longitude,
        name: dest.name,
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

    zone_level_2.value = (destinationsByDistance['1000m'] || []).map((dest, index) => ({
        id: dest.destination_id,
        lat: dest.latitude,
        lon: dest.longitude,
        name: dest.name,
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

    zone_level_3.value = (destinationsByDistance['1500m'] || []).map((dest, index) => ({
        id: dest.destination_id,
        lat: dest.latitude,
        lon: dest.longitude,
        name: dest.name,
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

    // ⭐ VWorld API 호출 (주소와 지목정보 설정)
    await getAddressAndJimok()

    // ⭐ VWorld API 호출 후 배열 재할당으로 Vue 반응성 강제 트리거
    zone_level_1.value = [...zone_level_1.value]
    zone_level_2.value = [...zone_level_2.value]
    zone_level_3.value = [...zone_level_3.value]

    // ⭐ 경로 생성 (TMap API)
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

    // 응답 구조 정규화하여 반환
    return {
        status: dataResp?.response?.status || dataResp?.status || 'ERROR',
        errorText: dataResp?.response?.error?.text || dataResp?.error?.text,
        properties: properties
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

        // VWorld Search API 응답 구조 확인
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

/**
 * 모든 zone의 경로를 자동으로 요청하는 함수
 * ⭐ 수정: waypoints를 제대로 passList로 변환
 */
async function requestAllRoutes() {

    const s = missingLocation.value

    if (!s || !s.lat || !s.lon) {
        console.error('❌ 출발지 위치 정보가 없습니다.')
        return
    }

    // ⭐ zone_level_1~3 모두 처리
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

                // ⭐ waypoints 변환
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

                // ⭐ 요청 본문 구성`
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

                // ⭐ 경유지 추가 (waypointsStr이 존재할 때만)
                if (waypointsStr && waypointsStr.length > 0) {
                    body.passList = waypointsStr
                }

                // ⭐ TMap API 호출
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

                // ⭐ features 존재 여부 확인
                if (data && data.features && Array.isArray(data.features) && data.features.length > 0) {
                    zone.storage.value.push(data.features)
                    console.log(`✅ Zone ${zone.level}-${i + 1} 경로 저장 (${data.features.length}개 feature)`)
                }

                // ⭐ API 요청 지연 (rate limit 방지)
                await delay(200)

            } catch (e) {
                console.error(`❌ Zone ${zone.level}-${i + 1} 경로 요청 에러:`, e)
                zone.storage.value.push(null)
            }
        }

        console.log(`✅ Zone ${zone.level} 경로 요청 완료 (저장된 경로: ${zone.storage.value.filter(r => r !== null).length}개 / 전체: ${zone.storage.value.length}개)`)
    }

    console.log('🚶 모든 경로 요청 완료')
    console.log(`최종 결과:`)
    console.log(`  - Zone1: ${zone1Routes.value.length}개 (유효: ${zone1Routes.value.filter(r => r !== null).length}개)`)
    console.log(`  - Zone2: ${zone2Routes.value.length}개 (유효: ${zone2Routes.value.filter(r => r !== null).length}개)`)
    console.log(`  - Zone3: ${zone3Routes.value.length}개 (유효: ${zone3Routes.value.filter(r => r !== null).length}개)`)
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

    // ⭐ 이전 폴리라인 제거
    clearPolylines()

    let totalPolylines = 0

    // ⭐ 새로운 경로 그리기
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

                // 첫 번째 feature의 중간 지점으로 이동
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
            return { sgg: '', emd: '', ri: '', roadAddress: '' }
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
            return { sgg: '', emd: '', ri: '', roadAddress: '' }
        }

    } catch (error) {
        console.error(`실종자 정보에서 위경도값으로 주소 조회중 오류 -> ${error}`)
        return { sgg: '', emd: '', ri: '', roadAddress: '' }
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
        return null;
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
                loadKakaoMap(mapContainer.value);
                setTimeout(() => {
                    getMissingAddress()
                    calcElapsedTime()

                    if (map) {
                        // ⭐ 초기화 시에만 force=true로 중심 설정
                        setCenter(true)
                        makeMarker()
                        initCircles()
                        showCirclesByZoneLevel(displayZoneLevel.value)
                    }
                }, 1000);
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
    console.log('제보하기 페이지로 이동합니다...');
    router.push({ name: 'ReportCreate' });
}

const { startParticipantTracking, stopParticipantTracking } = useParticipantLocations({
    map: map,
    missingPostId: missingPostId
});
const isParticipantsLayerVisible = ref(false);

function wherePeople() {
    isParticipantsLayerVisible.value = !isParticipantsLayerVisible.value;

    if (isParticipantsLayerVisible.value) {
        startParticipantTracking();

        if (missingPostId.value) {
            console.log(`[PredictLocation] '함께 찾기' 스위치를 켭니다. ID: ${missingPostId.value}`);
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

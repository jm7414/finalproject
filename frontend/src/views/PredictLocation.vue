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
        <div ref="mapContainer" class="map-area"></div>

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
                    <div class="handle-tooltip">{{ Math.round(selectedMinutes) }}분</div>
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
                        <img class="profile-image" :src="personDetail?.photoPath || defaultPersonImage"
                            :alt="personDetail?.patientName || '실종자'" />
                    </div>
                    <div class="basic-info-wrapper">
                        <div class="name-age-row">
                            <h2 class="person-name">{{ personDetail?.patientName || '정보 없음' }} ({{
                                calculateAge(personDetail?.patientBirthDate) }}세)</h2>
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
                            실종장소: {{ missingAddress?.fullAddress || '주소 정보 없음' }}
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
                            <span class="info-content">{{ formatDescription(personDetail?.description).physicalFeatures
                                || '170cm 마른 체형' }}</span>
                        </div>

                        <div>
                            <div class="info-badge">
                                <i class="bi bi-bag"></i>
                                <span class="badge-label">착의사항</span>
                            </div>
                            <span class="info-content">{{ formatDescription(personDetail?.description).clothing || '정보없음' }}</span>
                        </div>

                        <div>
                            <div class="info-badge">
                                <i class="bi bi-exclamation-triangle"></i>
                                <span class="badge-label">특이사항</span>
                            </div>
                            <span class="info-content">{{ formatDescription(personDetail?.description).specialNotes ||
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
                                <button class="btn btn-info modern-btn" @click="wherePeople">
                                    <i class="bi bi-arrow-right-circle"></i>
                                    함께하는 사람 보기
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
                        <!--
                            <span class="type-badge-modern" :class="loc.type">
                                {{ loc.type === 'cluster' ? '자주 방문' : '예상 이동' }}
                            </span>
                            -->
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
import { useRoute } from 'vue-router'
const route = useRoute()

// ============================================================================
// [수정] ID 관리 변수 - 환자 번호와 실종 신고 ID를 명확히 구분
// ============================================================================
//  문제: 기존 userNo는 route.params.id를 받았는데, 이게 환자번호인지 실종신고ID인지 불명확
//  해결: 두 가지 ID를 명확히 분리
const patientUserNo = ref(null)      // 환자의 user_no (users 테이블)
const missingPostId = ref(null)      // 실종 신고 ID (missing_post 테이블)

// 병욱 작업공간 확보 시작 
// [수정] 이제 missingPostId를 명확히 사용
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

        // --- 요청 성공 시 콘솔에 출력 ---
        console.log('✅ 함께 찾는 사람들:', response.data);
        // 참여자 수만 상태에 반영
        if (Array.isArray(response.data)) {
            participantsCount.value = response.data.length
        } else if (response.data && typeof response.data === 'object') {
            participantsCount.value = (response.data.count ?? response.data.total ?? 0)
        } else {
            participantsCount.value = 0
        }

    } catch (error) {
        console.error("❌ 참여자 목록 조회 실패:", error);
        if (error.response) {
            console.error(" - 응답 상태:", error.response.status);
            console.error(" - 응답 데이터:", error.response.data);
        } else if (error.request) {
            console.error(" - 서버 응답 없음");
        } else {
            console.error(" - 요청 설정 오류:", error.message);
        }
    }
}


//  상태 변수 (이름 충돌 없음)
const personDetail = ref(null)
const personLoading = ref(true)
const personError = ref(null)
const defaultPersonImage = '@/default-person.png'
const participantsCount = ref(0)

// 시간 변수
const missingTimeDB = ref(null)

// ============================================================================
// [수정] 실종자 정보 API 호출 함수 - missingPostId를 사용하도록 변경
// ============================================================================

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

        // API 응답에서 'reportedAt' 값을 'missingTimeDB'에 저장
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

// ============================================================================
// [주형 추가] 실종자 정보에서 missingLocation에서 lat,lon을 가지고 주소 가져오기
// ============================================================================
const missingAddress = ref(null)
let fullAddress = ''
async function getMissingAddress() {
    try {
        console.log(`missingLocation으로 조회 시작 lat : ${missingLocation.value.lat}, lon : ${missingLocation.value.lon}`)

        // sgg(시군구), emd(읍면동), ri(리), rn(도로명) 컬럼만 요청
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

            // 주소 부분들을 배열로 구성
            const addressParts = [
                props.sgg_nm,
                props.emd_nm,
                props.ri_nm
            ].filter(Boolean)  // null, undefined, 빈 문자열 제거

            // 기본 주소 문자열
            fullAddress = addressParts.join(' ')

            // 도로명이 있으면 괄호 안에 추가
            if (props.rn_nm) {
                fullAddress += ` (${props.rn_nm})`
            }

            const result = {
                sgg: props.sgg_nm || '',
                emd: props.emd_nm || '',
                ri: props.ri_nm || '',
                roadAddress: props.rn_nm || '',
                fullAddress: fullAddress  // 전체 주소 문자열 추가
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

// ============================================================================
// [주형 추가] 실종자 정보에서 missingTime을 기반으로 현재시간에서 빼가지고 실종자 정보에 띄워주는 함수
// ============================================================================
// ref로 경과 시간 문자열 저장
const elapsedTimeText = ref('')

function calcElapsedTime() {
    try {
        // 실종자 정보 탭: missingTimeDB.value 사용 (실종자 정보에서 가져온 시간)
        // 예상 위치 탭: missingTime 상수 사용 (시연용 데이터)
        let timeToCalculate = missingTimeDB.value
        
        // missingTimeDB가 없으면 시연용 기본 시간 사용 (예상 위치 탭용)
        if (!timeToCalculate) {
            const missingTimeDate = new Date(missingTime)
            timeToCalculate = missingTimeDate.getTime()
        } else {
            timeToCalculate = new Date(timeToCalculate).getTime()
        }
        
        const missingTimeDate = new Date(timeToCalculate)

        // 유효한 날짜인지 확인
        if (isNaN(missingTimeDate.getTime())) {
            console.error('❌ 실종 시간이 유효하지 않습니다:', timeToCalculate)
            elapsedTimeText.value = '시간 불명'
            return
        }

        // 현재 시간
        const now = new Date()

        // 시간 차이 계산 (밀리초)
        const diffInMs = now.getTime() - missingTimeDate.getTime()

        // 밀리초를 분으로 변환
        const diffInMinutes = Math.floor(diffInMs / (1000 * 60))

        // 음수면 0으로 설정 (미래 시간인 경우)
        const minutes = Math.max(0, diffInMinutes)
        
        // 분 또는 시간으로 표시하기
        if (minutes < 60) {
            elapsedTimeText.value = `${minutes}분 전`
        } else {
            const hours = Math.floor(minutes / 60)
            elapsedTimeText.value = `약 ${hours}시간 전`
        }

        console.log(`⏱️ 경과 시간: ${minutes}분 → 표시: ${elapsedTimeText.value}`)
        setTime(minutes)  // 필요하면 여기서도 원본 분 단위 값 저장

    } catch (error) {
        console.error('❌ 경과 시간 계산 중 오류:', error)
        elapsedTimeText.value = '시간 불명'
    }
}


// ============================================================================
// [추가] 보호자 → 환자 → 실종신고 순서로 데이터 조회하는 새 함수
// ============================================================================
//  데이터 흐름:
// 1. 로그인한 보호자의 환자 조회 (/api/user/my-patient)
// 2. 환자 번호로 최신 실종 신고 조회 (/api/missing-persons/patient/{patientUserNo}/latest)
// 3. 실종 신고 ID를 저장하여 다른 API 호출에 사용
async function fetchPatientAndMissingReport() {
    try {
        console.log('📋 Step 1: 보호자가 관리하는 환자 조회 중...')

        // Step 1: 보호자가 관리하는 환자 조회
        const patientResponse = await axios.get('/api/user/my-patient', {
            withCredentials: true
        })

        if (!patientResponse.data || !patientResponse.data.userNo) {
            console.error('❌ 관리하는 환자가 없습니다.')
            personError.value = '관리하는 환자 정보를 찾을 수 없습니다.'
            personLoading.value = false
            return
        }

        patientUserNo.value = patientResponse.data.userNo
        console.log(`✅ 환자 정보 조회 성공: patientUserNo=${patientUserNo.value}`)

        // Step 2: 환자 번호로 최신 실종 신고 조회
        console.log('📋 Step 2: 환자의 최신 실종 신고 조회 중...')
        const missingReportResponse = await axios.get(
            `/api/missing-persons/patient/${patientUserNo.value}/latest`,
            { withCredentials: true }
        )

        if (!missingReportResponse.data || !missingReportResponse.data.missingPostId) {
            console.error('❌ 실종 신고 정보가 없습니다.')
            personError.value = '실종 신고 정보를 찾을 수 없습니다.'
            personLoading.value = false
            return
        }

        missingPostId.value = missingReportResponse.data.missingPostId
        console.log(`✅ 실종 신고 조회 성공: missingPostId=${missingPostId.value}`)

        // Step 3: 이제 missingPostId가 준비되었으니 다른 정보들을 조회
        console.log('📋 Step 3: 실종자 상세 정보 및 참여자 목록 조회 중...')
        await fetchMissingPersonDetail()  // 실종자 상세 정보
        await fetchParticipants()         // 함께 찾는 사람들

    } catch (error) {
        console.error('❌ 데이터 조회 중 오류 발생:', error)
        if (error.response && error.response.status === 401) {
            personError.value = '로그인이 필요합니다.'
        } else if (error.response && error.response.status === 404) {
            personError.value = '환자 또는 실종 신고 정보를 찾을 수 없습니다.'
        } else {
            personError.value = '데이터를 불러오는 중 오류가 발생했습니다.'
        }
        personLoading.value = false
    }
}

// 추가 : 실종시간 받아오는 방식
function formatSimpleDateTime(dateString) {
    if (!dateString) return '시간 정보 없음';
    try {
        const date = new Date(dateString);
        if (isNaN(date)) return '날짜 형식 오류';

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0'); // 월은 0부터 시작하므로 +1, 두 자리로 패딩
        const day = String(date.getDate()).padStart(2, '0'); // 날짜 두 자리로 패딩
        const hours = String(date.getHours()).padStart(2, '0'); // 시간 두 자리로 패딩
        const minutes = String(date.getMinutes()).padStart(2, '0'); // 분 두 자리로 패딩

        return `${year}-${month}-${day} ${hours}:${minutes}`;
    } catch (e) {
        console.error("날짜 포맷 오류:", e, dateString);
        return '날짜 형식 오류';
    }
}

// 추가 : 나이 계산 함수
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

// 추가 : 설명 포맷팅 함수
function formatDescription(desc) {
    if (!desc) {
        return {
            physicalFeatures: '정보 없음',
            clothing: '정보 없음',
            specialNotes: '정보 없음'
        };
    }

    const descStr = String(desc).trim();
    
    // 실제 줄바꿈 문자(\n)와 문자열로 저장된 줄바꿈(\\n) 모두 처리
    const lines = descStr.includes('\\n') 
        ? descStr.split('\\n') 
        : descStr.split('\n');

    const result = {
        physicalFeatures: '',
        clothing: '',
        specialNotes: ''
    };

    let hasParsedKey = false; // 키워드가 포함된 줄이 있는지 확인

    lines.forEach(line => {
        const trimmedLine = line.trim();
        if (!trimmedLine) return; // 빈 줄은 건너뛰기

        // "키워드: 값" 형태로 파싱
        if (trimmedLine.includes(':')) {
            hasParsedKey = true;
            const [key, ...valueParts] = trimmedLine.split(':');
            const value = valueParts.join(':').trim(); // ":"가 여러 개 있을 경우 대비
            const keyLower = key.toLowerCase().trim();

            if (keyLower.includes('인상착의') || keyLower.includes('착의사항')) {
                result.clothing = value;
            } else if (keyLower.includes('신체') || keyLower.includes('체형')) {
                result.physicalFeatures = value;
            } else if (keyLower.includes('특이사항') || keyLower.includes('특이')) {
                result.specialNotes = value;
            }
        }
    });

    // 키워드가 없는 경우 (단순 텍스트 입력), 전체 내용을 특이사항으로 처리
    if (!hasParsedKey && descStr) {
        result.specialNotes = descStr;
    }

    // 파싱된 값이 없는 경우 기본값 설정
    if (!result.physicalFeatures && !result.clothing && !result.specialNotes && descStr) {
        result.specialNotes = descStr;
    }

    return result;
}

// 병욱 작업공간 확보 끝

import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import axios from 'axios'

// ========================================================================================
// 카카오지도 및 API 키 설정
// ========================================================================================
const mapContainer = ref(null)

// 카카오맵 API 키
const KAKAO_JS_KEY = '7e0332c38832a4584b3335bed6ae30d8'

// VWorld API Key
const VWORLD_API_KEY = '6A0CFFEF-45CF-3426-882D-44A63B5A5289'

// Tmap API Key
const TMAP_API_KEY = 'dcWZUHevJw6z8GD6zXhNb3X3pDjyDqs99YDxMbHh'

// ========================================================================================
// 데이터 상태 관리
// ========================================================================================

// FastAPI를 통한 응답을 받기위한 반응형 변수
const predictionData = ref({
    metadata: null,
    zone_level_1: [],
    zone_level_2: [],
    zone_level_3: []
})

// 실종위치 중심으로 바꾸기위한 반응형변수
const missingLocation = ref({
    lat: null,
    lon: null
})

// ⭐ 표시할 Zone Level 선택 (1, 2, 3)
const displayZoneLevel = ref(1)

// 로딩 상태
const isLoading = ref(true)

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

// ⭐ 시연용 시뮬레이션 상태 (Ctrl+4로 시간 자동 증가)
const simulationState = ref({
  isRunning: false, // 시뮬레이션 실행 중 여부
  intervalId: null, // setInterval ID
  startMinutes: 0 // 시뮬레이션 시작 시점의 분 값
})

// ⭐ 더보기 관련 상태 추가
const showAllLocations = ref(false)

// ========================================================================================
// Computed Properties
// ========================================================================================

// ⭐ 더보기 상태에 따라 표시할 개수 결정
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

// ⭐ 선택된 시간에 따라 표시할 zone을 동적으로 반환
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

// ⭐ 현재 zone level의 전체 데이터 개수 확인
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

// ⭐⭐⭐ 핵심 변경: 더보기 상태에 따라 마커 데이터 변경
const markerDataToDisplay = computed(() => {
    // ⭐ 더보기를 누른 경우: 현재 zone level의 마커만 표시
    if (showAllLocations.value) {
        return displayedZoneToShow.value.map(item => ({
            ...item,
            zoneLevel: displayZoneLevel.value
        }))
    }

    // ⭐ 더보기를 누르지 않은 경우: 기존처럼 누적 표시
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

// ⭐ 프로그레스 바 너비 계산 (0~100%)
const progressWidth = computed(() => {
    return (selectedMinutes.value / 90) * 100
})

// ========================================================================================
// 더보기 버튼 클릭 함수
// ========================================================================================
function toggleShowMore() {
    showAllLocations.value = !showAllLocations.value
    console.log(`더보기 토글: ${showAllLocations.value ? '전체 보기' : '3개만 보기'}`)

    // ⭐ 더보기 상태 변경 시 마커 재생성
    if (map.value) {
        makeMarker()
    }
}

// ========================================================================================
// 타임라인 관련 함수
// ========================================================================================

function setTime(minutes) {
    // 0~90분 범위로 제한
    const clampedMinutes = Math.max(0, Math.min(90, minutes))
    selectedMinutes.value = clampedMinutes
    showAllLocations.value = false // ⭐ 더보기 상태 초기화
    selectedLocation.value = null
    clearAllRoutes()

    // ⭐⭐⭐ zone level 계산
    let newLevel = 1
    if (clampedMinutes <= 30) {
        newLevel = 1
    } else if (clampedMinutes <= 60) {
        newLevel = 2
    } else {
        newLevel = 3
    }

    displayZoneLevel.value = newLevel
}

// ⭐ 부드러운 시간 업데이트 함수 (시뮬레이션용, 소수점 분 허용)
function updateTimeSmoothly(minutes) {
    // 0~90분 범위로 제한
    const clampedMinutes = Math.max(0, Math.min(90, minutes))
    
    // 소수점 분을 허용하여 저장
    selectedMinutes.value = clampedMinutes
    
    // zone level 계산 (실제 분 값으로 계산하여 30분, 60분을 넘어서는 순간 바로 변경)
    let newLevel = 1
    if (clampedMinutes <= 30) {
        newLevel = 1
    } else if (clampedMinutes <= 60) {
        newLevel = 2
    } else {
        newLevel = 3
    }
    
    // zone level이 변경된 경우에만 전체 상태 초기화 및 지도 줌 조정
    if (displayZoneLevel.value !== newLevel) {
        const oldLevel = displayZoneLevel.value
        displayZoneLevel.value = newLevel
        showAllLocations.value = false
        selectedLocation.value = null
        clearAllRoutes()
        
        if (map.value) {
            makeMarker()
            showCirclesByZoneLevel(newLevel)
            
            // ⭐ 시뮬레이션 중일 때만 지도 줌 자동 조정
            if (simulationState.value.isRunning) {
                // 30분을 넘어서 level 2로 변경될 때 (circle1500 최대 반경 1300m)
                if (oldLevel === 1 && newLevel === 2) {
                    adjustMapZoomForRadius(1300)
                }
                // 60분을 넘어서 level 3로 변경될 때 (circle2100 최대 반경 2000m)
                else if (oldLevel === 2 && newLevel === 3) {
                    adjustMapZoomForRadius(2000)
                }
            }
        }
    }
    
    // 원의 반경을 소수점 분에 맞게 부드럽게 업데이트
    if (map.value && (selectedType.value === 'map' || selectedType.value === 'info')) {
        updateMapForTime(clampedMinutes)
    }
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

// ========================================================================================
// 시간 체크 및 Zone Level 변경
// ========================================================================================

// ⭐ displayZoneLevel이 변경될 때마다 circle 업데이트
watch(displayZoneLevel, (newLevel, oldLevel) => {
    showAllLocations.value = false

    clearAllRoutes()
    selectedLocation.value = null

    if (map.value) {
        makeMarker()
        showCirclesByZoneLevel(newLevel)
    }
})

// ⭐⭐⭐ selectedMinutes가 변경될 때 zone level 변경 및 원 업데이트
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

    // ⭐⭐⭐ displayZoneLevel 변경 (이게 watch(displayZoneLevel)을 트리거함)
    displayZoneLevel.value = newLevel

    // ⭐⭐⭐ 원 반경 동적 업데이트
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

// 예측 API 호출용 사용자 번호 (테스트 단계: 항상 3번 환자로 고정)
const user_no = 3

// ========================================================================================
// Circle 관련 변수 및 함수
// ========================================================================================

const circles = ref({
    circle700: null,
    circle1500: null,
    circle2100: null
})

// ⭐ Circle 초기화 함수
function initCircles() {
    if (!map.value || !missingLocation.value.lat || !missingLocation.value.lon) {
        console.error('지도 또는 실종 위치가 초기화되지 않았습니다.')
        return
    }

    const center = new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon)

    // 700m 원 생성
    circles.value.circle700 = new window.kakao.maps.Circle({
        center: center,
        radius: 0, // 초기 반경 0
        strokeWeight: 3,
        strokeColor: '#66bb6a',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
        fillColor: '#66bb6a',
        fillOpacity: 0.15
    })

    // 1500m 원 생성
    circles.value.circle1500 = new window.kakao.maps.Circle({
        center: center,
        radius: 0, // 초기 반경 0
        strokeWeight: 3,
        strokeColor: '#ff9e7e',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
        fillColor: '#ff9e7e',
        fillOpacity: 0.15
    })

    // 2100m 원 생성
    circles.value.circle2100 = new window.kakao.maps.Circle({
        center: center,
        radius: 0, // 초기 반경 0
        strokeWeight: 3,
        strokeColor: '#ff6b9d',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
        fillColor: '#ff6b9d',
        fillOpacity: 0.15
    })

    console.log('✅ Circle 초기화 완료')
}

// ⭐ 모든 Circle 숨기기
function hideCircles() {
    if (circles.value.circle700) circles.value.circle700.setMap(null)
    if (circles.value.circle1500) circles.value.circle1500.setMap(null)
    if (circles.value.circle2100) circles.value.circle2100.setMap(null)
}

// ⭐ Zone Level에 따라 Circle 표시
function showCirclesByZoneLevel(level) {

    if (!map.value) {
        console.error('지도가 초기화되지 않았습니다.')
        return
    }

    // 먼저 모든 원 숨기기
    hideCircles()

    // Zone Level에 따라 해당하는 원들만 표시
    if (level >= 1 && circles.value.circle700) {
        circles.value.circle700.setMap(map.value)
    }

    if (level >= 2 && circles.value.circle1500) {
        circles.value.circle1500.setMap(map.value)
    }

    if (level >= 3 && circles.value.circle2100) {
        circles.value.circle2100.setMap(map.value)
    }

    // 원의 반경을 현재 시간에 맞게 업데이트
    updateMapForTime(selectedMinutes.value)
}

// ⭐ 반경에 맞춰 지도 줌 조정하는 함수
function adjustMapZoomForRadius(maxRadius) {
    if (!map.value || !missingLocation.value.lat || !missingLocation.value.lon) {
        console.log('⚠️ adjustMapZoomForRadius: 지도 또는 위치 정보 없음')
        return
    }
    
    console.log(`🔍 adjustMapZoomForRadius 호출: maxRadius=${maxRadius}m`)
    
    // 반경(미터)에 따라 적절한 카카오맵 level 계산
    // ⚠️ 카카오맵 level 개념: 숫자가 작을수록 확대(좁은 범위), 숫자가 클수록 축소(넓은 범위)
    // 원이 클수록 지도를 조금 축소시켜 넓은 범위가 보이도록 해야 하지만, 너무 축소되지 않게 적절히 조정
    // 예: level 3 = 확대, level 5 = 중간, level 6 = 약간 축소, level 7 = 축소
    let targetLevel = 5
    if (maxRadius <= 600) {
        targetLevel = 5  // 600m 이하 → 중간
    } else if (maxRadius <= 1000) {
        targetLevel = 5  // 600m ~ 1000m → 중간 유지
    } else if (maxRadius <= 1500) {
        targetLevel = 6  // 1000m ~ 1500m → 약간 축소 (넓은 범위) ← 1300m (30~60분, 현재 적절함)
    } else {
        targetLevel = 7  // 1500m 이상 → 축소 (더 넓은 범위) ← 2000m (60~90분, 30~60분보다 더 축소)
    }

    // 현재 level과 다르면 조정
    const currentLevel = map.value.getLevel()
    console.log(`🔍 현재 level: ${currentLevel}, 목표 level: ${targetLevel}`)
    
    if (Math.abs(currentLevel - targetLevel) >= 1) {
        // 카카오맵 API에서 animate 옵션이 지원되는지 확인 후 적용
        try {
            map.value.setLevel(targetLevel)
            console.log(`🗺️ 지도 줌 조정 완료: 반경 ${maxRadius}m → level ${currentLevel} → ${targetLevel}`)
        } catch (error) {
            console.error('❌ 지도 줌 조정 실패:', error)
        }
    } else {
        console.log(`ℹ️ 지도 줌 조정 불필요: 현재 level(${currentLevel})이 목표 level(${targetLevel})과 유사함`)
    }
}

// ⭐ 시간에 따라 원의 반경 업데이트
function updateMapForTime(minutes) {
    if (!map.value || !circles.value.circle700) {
        console.log('⚠️ updateMapForTime: 지도 또는 Circle이 초기화되지 않음')
        return
    }

    // ⭐ 원이 지도에 표시되어 있는지 확인하는 헬퍼 함수
    const ensureCircleOnMap = (circle) => {
        if (circle && !circle.getMap()) {
            circle.setMap(map.value)
        }
    }

    // 0-30분: circle700만 0~600m로 점진적 확대
    if (minutes <= 30) {
        // circle700을 지도에 추가 (필요한 경우)
        ensureCircleOnMap(circles.value.circle700)
        const radius = (minutes / 30) * 600
        circles.value.circle700.setRadius(radius)

        // 나머지 원은 반경 0으로 설정하고 지도에서 제거 (숨김 효과)
        if (circles.value.circle1500) {
            circles.value.circle1500.setRadius(0)
            circles.value.circle1500.setMap(null)
        }
        if (circles.value.circle2100) {
            circles.value.circle2100.setRadius(0)
            circles.value.circle2100.setMap(null)
        }
    }
    // 30-60분: circle700은 600m 고정, circle1500은 600m(30분 종료값)에서 1300m로 점진적 확대
    else if (minutes <= 60) {
        // circle700과 circle1500을 지도에 추가 (필요한 경우)
        ensureCircleOnMap(circles.value.circle700)
        ensureCircleOnMap(circles.value.circle1500)
        
        circles.value.circle700.setRadius(600)

        // circle1500: 600m(0~30분의 최대값)에서 시작해서 1300m까지 점진적 확대
        // 30분에서 600m, 60분에서 1300m가 되도록
        const radius = 600 + ((minutes - 30) / 30) * (1300 - 600)
        if (circles.value.circle1500) {
            circles.value.circle1500.setRadius(radius)
        }

        // circle2100은 반경 0으로 설정하고 지도에서 제거 (숨김 효과)
        if (circles.value.circle2100) {
            circles.value.circle2100.setRadius(0)
            circles.value.circle2100.setMap(null)
        }
    }
    // 60-90분: circle700은 600m, circle1500은 1300m 고정, circle2100은 1300m(60분 종료값)에서 2000m로 점진적 확대
    else if (minutes <= 90) {
        // 모든 원을 지도에 추가 (필요한 경우)
        ensureCircleOnMap(circles.value.circle700)
        ensureCircleOnMap(circles.value.circle1500)
        ensureCircleOnMap(circles.value.circle2100)
        
        circles.value.circle700.setRadius(600)

        if (circles.value.circle1500) {
            circles.value.circle1500.setRadius(1300)
        }

        // circle2100: 1300m(30~60분의 최대값)에서 시작해서 2000m까지 점진적 확대
        // 60분에서 1300m, 90분에서 2000m가 되도록
        const radius = 1300 + ((minutes - 60) / 30) * (2000 - 1300)
        if (circles.value.circle2100) {
            circles.value.circle2100.setRadius(radius)
        }
    }
    // 90분 초과: 모든 원을 최대 반경으로 고정
    else {
        ensureCircleOnMap(circles.value.circle700)
        ensureCircleOnMap(circles.value.circle1500)
        ensureCircleOnMap(circles.value.circle2100)
        
        circles.value.circle700.setRadius(600)
        
        if (circles.value.circle1500) {
            circles.value.circle1500.setRadius(1300)
        }
        
        if (circles.value.circle2100) {
            circles.value.circle2100.setRadius(2000)  // 최대 2000m로 제한
        }
    }
}


// ========================================================================================
// FastAPI 호출 함수
// ========================================================================================

async function fetchPrediction() {
    console.log(`clicked fetch`)
    isLoading.value = true

    try {
        const response = await fetch('http://localhost:8000/api/predict-location', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                user_no: user_no,
                missing_time: missingTime,
            })
        })

        if (!response.ok) {
            console.error('Failed to fetch prediction:', response.statusText)
            return
        }

        const data = await response.json()
        predictionData.value = data

        // 지도 중심을 실종위치로 바꾸기 위해 missing location을 가져옴
        missingLocation.value.lat = predictionData.value.metadata.missing_center_lat
        missingLocation.value.lon = predictionData.value.metadata.missing_center_lon

        getMissingAddress()
        calcElapsedTime()


        console.log('Prediction Data:', data)

        // ⭐ VWorld API 호출하여 지목 및 주소 정보 가져오기
        await getPropsFromVworld()

        // ⭐ 모든 zone의 경로 자동 요청
        await requestAllRoutes()

        // ⭐ 마커 생성
        setCenter()
        makeMarker()

        // ⭐⭐⭐ Circle 초기화 및 표시
        initCircles()
        if (selectedType.value === 'map' || 'info') {
            showCirclesByZoneLevel(displayZoneLevel.value)
        }

    } catch (error) {
        console.error('Error fetching prediction:', error)
    } finally {
        isLoading.value = false
    }
}

// ========================================================================================
// 카카오맵 초기화
// ========================================================================================

// ⭐ 시연용 시뮬레이션 기능 (Ctrl+4)
function handleSimulationToggle() {
  if (simulationState.value.isRunning) {
    // 시뮬레이션 정지 및 초기화
    console.log('[시연] Ctrl+4: 실종 경과 시간 시뮬레이션 정지 및 초기화')
    stopSimulation()
  } else {
    // 시뮬레이션 시작
    console.log(`[시연] Ctrl+4: 실종 경과 시간 시뮬레이션 시작 (현재: ${selectedMinutes.value}분)`)
    startSimulation()
  }
}

// 시뮬레이션 시작
function startSimulation() {
  if (simulationState.value.isRunning) return
  
  // 현재 값에서 시작
  simulationState.value.startMinutes = selectedMinutes.value
  simulationState.value.isRunning = true
  
  // 50ms마다 0.1분씩 증가 (1초에 2분 증가)
  // 더 부드러운 애니메이션을 위해 업데이트 주기를 세밀하게 함
  simulationState.value.intervalId = setInterval(() => {
    // 소수점까지 허용하여 부드럽게 증가
    const nextMinutes = selectedMinutes.value + 0.1
    
    if (nextMinutes >= 90) {
      // 90분 도달 시 멈춤
      setTime(90)
      stopSimulation()
      console.log('[시연] 실종 경과 시간 시뮬레이션 완료: 90분 도달')
    } else {
      // 소수점 분을 허용하여 부드럽게 업데이트
      updateTimeSmoothly(nextMinutes)
    }
  }, 50) // 50ms마다 실행 (더 부드러운 애니메이션)
}

// 시뮬레이션 정지 및 초기화
function stopSimulation() {
  if (simulationState.value.intervalId) {
    clearInterval(simulationState.value.intervalId)
    simulationState.value.intervalId = null
  }
  simulationState.value.isRunning = false
  // 시작했던 값으로 초기화
  setTime(simulationState.value.startMinutes)
}

// Ctrl + 3: 현관문 열림 감지 알림
function handleDoorOpenAlert() {
  console.log('[시연] Ctrl+3: 현관문 열림 감지 알림')
  // 모달 표시 (App.vue의 전역 함수 호출)
  setTimeout(() => {
    if (window.showDoorOpenModal) {
      window.showDoorOpenModal()
    }
  }, 100)
}

// 키보드 이벤트 리스너
function handleKeyDown(event) {
  // Ctrl + 3: 현관문 열림 감지 알림
  if (event.ctrlKey && event.key === '3') {
    event.preventDefault()
    handleDoorOpenAlert()
  }
  // Ctrl + 4: 시간 시뮬레이션 토글 (predict-location 페이지에서만 작동)
  if (event.ctrlKey && event.key === '4') {
    event.preventDefault()
    handleSimulationToggle()
  }
}

onMounted(async () => {
    // 쿼리 파라미터에서 missingPostId 확인
    const queryMissingPostId = route.query.missingPostId
    
    if (queryMissingPostId) {
        // 실종 게시판에서 "함께하기"를 통해 온 경우
        console.log(`📋 게시판에서 온 경우: missingPostId=${queryMissingPostId}`)
        missingPostId.value = Number(queryMissingPostId)
        
        // 실종자 정보 직접 조회
        await fetchMissingPersonDetail()
        await fetchParticipants()
        
        // 실종자 정보에서 환자 번호 가져오기 (실종자 정보 표시용)
        if (personDetail.value && personDetail.value.patientUserNo) {
            patientUserNo.value = personDetail.value.patientUserNo
            console.log(`✅ 환자 번호 확인: patientUserNo=${patientUserNo.value} (예측 API는 항상 user_no=3 사용)`)
        } else {
            console.error('❌ 실종자 정보에서 환자 번호를 찾을 수 없습니다.')
        }
    } else {
        // GD_main에서 "예상 위치"를 통해 온 경우 (기존 로직)
        console.log('📋 메인에서 온 경우: 내 연결된 환자 조회')
        await fetchPatientAndMissingReport()
        console.log(`✅ 환자 번호 확인: patientUserNo=${patientUserNo.value} (예측 API는 항상 user_no=3 사용)`)
    }
    
    try {
        loadKakaoMap(mapContainer.value)
        // 페이지 로드 시 자동으로 데이터 가져오기
        setTimeout(() => {
            fetchPrediction()
        }, 1000)        
    } finally {
        selectedType.value = 'info'
    }
    
    // 키보드 이벤트 리스너 등록 (시뮬레이션용)
    window.addEventListener('keydown', handleKeyDown)
})

onBeforeUnmount(() => {
    // 키보드 이벤트 리스너 제거
    window.removeEventListener('keydown', handleKeyDown)
    // 시뮬레이션 정리
    stopSimulation()
})

const loadKakaoMap = (container) => {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false&libraries=services`
    document.head.appendChild(script)

    script.onload = () => {
        window.kakao.maps.load(() => {
            const options = {
                center: new window.kakao.maps.LatLng(37.234257, 126.681727),
                level: 5,
            }

            map.value = new window.kakao.maps.Map(container, options)
            console.log('지도 초기화 완료')

            // 중앙 마커 생성
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

    // 중앙 마커 업데이트
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

// ⭐ 마커 숨기기 함수
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

// ⭐ 마커 생성 함수
function makeMarker() {
    // ⭐ 기존 마커 숨기기
    hideMarkers()

    // markerDataToDisplay에 포함된 모든 마커 표시
    markerDataToDisplay.value.forEach((item, index) => {
        const markerPosition = new window.kakao.maps.LatLng(item.lat, item.lon)

        // Zone Level에 따른 마커 색상 변경
        const markerColor = getMarkerColor(item.zoneLevel)

        const marker = new window.kakao.maps.Marker({
            position: markerPosition,
            map: map.value,
            title: `Zone ${item.zoneLevel} - 위치 ${index + 1} (확률: ${(item.value * 100).toFixed(1)}%)`,
            image: createColoredMarkerImage(markerColor)
        })

        // ⭐ 마커를 배열에 저장
        markers.push(marker)
    })
}

// ⭐ Zone Level에 따른 마커 색상
function getMarkerColor(zoneLevel) {
    const colors = {
        1: '#66bb6a',  // 청록
        2: '#ff9e7e',  // 주황
        3: '#ff6b9d'   // 빨강
    }
    return colors[zoneLevel] || '#4ECDC4'
}

// ⭐ 컬러 마커 이미지 생성
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
// Tmap 경로 관련 함수
// ========================================================================================

// ⭐ zone_level_1~3의 모든 경로를 저장하는 배열
const zone1Routes = ref([])
const zone2Routes = ref([])
const zone3Routes = ref([])

// ⭐ 모든 zone의 경로를 자동으로 요청하는 함수
async function requestAllRoutes() {
    console.log('🚶 모든 경로 요청 시작...')

    const s = missingLocation.value

    // ⭐ zone_level_1~3 모두 처리
    const allZones = [
        { level: 1, data: predictionData.value.zone_level_1, storage: zone1Routes },
        { level: 2, data: predictionData.value.zone_level_2, storage: zone2Routes },
        { level: 3, data: predictionData.value.zone_level_3, storage: zone3Routes }
    ]

    for (const zone of allZones) {
        if (!zone.data || zone.data.length === 0) continue

        zone.storage.value = []

        for (let i = 0; i < zone.data.length; i++) {
            const d = zone.data[i]

            try {
                let waypointsStr = ''
                if (d.waypoints && Array.isArray(d.waypoints)) {
                    const waypointsCoords = d.waypoints.map(wp => `${wp.lon},${wp.lat}`)
                    waypointsStr = waypointsCoords.join('_')
                }

                let body = {
                    startName: 'start',
                    startX: Number(s.lon),
                    startY: Number(s.lat),
                    endName: 'end',
                    endX: Number(d.lon),
                    endY: Number(d.lat),
                    reqCoordType: 'WGS84GEO',
                    resCoordType: 'WGS84GEO',
                    searchOption: '0',
                }

                if (waypointsStr) {
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
                    console.error(`Zone ${zone.level}-${i} 경로 요청 실패:`, resp.status)
                    zone.storage.value.push(null)
                    continue
                }

                const data = await resp.json()

                if (data && data.features && Array.isArray(data.features)) {
                    zone.storage.value.push(data.features)
                } else {
                    zone.storage.value.push(null)
                    console.warn(`Zone ${zone.level}-${i} 유효한 경로 데이터 없음`)
                }

                await new Promise(resolve => setTimeout(resolve, 200))

            } catch (e) {
                console.error(`Zone ${zone.level}-${i} 경로 요청 에러:`, e)
                zone.storage.value.push(null)
            }
        }
    }

    console.log('🚶 모든 경로 요청 완료')
    console.log(`Zone1: ${zone1Routes.value.length}개, Zone2: ${zone2Routes.value.length}개, Zone3: ${zone3Routes.value.length}개`)
}

// ⭐ polyline 제거 함수
function clearPolylines() {

    for (let polyline of polylines) {
        if (polyline && polyline.setMap) {
            polyline.setMap(null)
        }
    }

    polylines.length = 0
}

// ⭐ 모든 경로 제거
function clearAllRoutes() {
    clearPolylines()
}

// ⭐ 경로 그리기 함수
function drawRoute(index, zoneLevel = 1) {
    if (!map.value) {
        console.error('지도가 초기화되지 않았습니다.')
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
        console.error('유효하지 않은 zone level:', zoneLevel)
        return
    }

    if (index < 0 || index >= routeStorage.length) {
        console.error('유효하지 않은 인덱스:', index)
        return
    }

    const routeFeatures = routeStorage[index]

    if (!routeFeatures || routeFeatures.length === 0) {
        console.error(`Zone ${zoneLevel}-${index}의 경로 데이터가 없습니다.`)
        return
    }

    console.log(`🗺️ Zone ${zoneLevel}-${index} 경로 그리기 시작...`)

    clearPolylines()

    routeFeatures.forEach((feature, featureIndex) => {
        if (feature.geometry.type === 'LineString') {
            const coordinates = feature.geometry.coordinates

            const linePath = coordinates.map(([lng, lat]) => {
                return new window.kakao.maps.LatLng(lat, lng)
            })

            const polyline = new window.kakao.maps.Polyline({
                map: map.value,
                path: linePath,
                strokeColor: '#2563EB',
                strokeWeight: 5,
                strokeOpacity: 0.8,
                strokeStyle: 'solid'
            })

            polylines.push(polyline)

            if (featureIndex === 0 && linePath.length > 0) {
                const midIndex = Math.floor(linePath.length / 2)
                map.value.panTo(linePath[midIndex])
            }
        }
    })

    console.log(`🗺️ Zone ${zoneLevel}-${index} 경로 그리기 완료 (총 ${polylines.length}개 폴리라인)`)
}


// ========================================================================================
// VWorld API 관련 함수
// ========================================================================================

// ⭐ Kakao Maps Geocoder를 사용한 좌표→주소 변환
async function getKakaoAddressFromCoord(lat, lon) {
    try {
        console.log(`🔍 Kakao Geocoder 호출: ${lat}, ${lon}`)

        const geocoder = new window.kakao.maps.services.Geocoder()

        return new Promise((resolve, reject) => {
            geocoder.coord2RegionCode(lon, lat, (result, status) => {
                if (status === window.kakao.maps.services.Status.OK) {

                    const region = result[0]

                    const address = {
                        sido: region.region_1depth_name || '',
                        gungu: region.region_2depth_name || '',
                        eup: region.region_3depth_name || '',
                        name: region.region_name || ''
                    }

                    console.log(`✅ 지역 정보: ${address.sido} ${address.gungu} ${address.eup}`)
                    resolve(address)
                } else if (status === window.kakao.maps.services.Status.ZERO_RESULT) {
                    console.warn(`⚠️ Kakao Geocoder: 결과 없음`)
                    resolve(null)
                } else if (status === window.kakao.maps.services.Status.ERROR) {
                    console.error(`❌ Kakao Geocoder: 에러 발생`)
                    reject(new Error('Kakao Geocoder 에러'))
                }
            })
        })

    } catch (e) {
        console.error('Kakao Geocoder 예외 에러:', e)
        return null
    }
}

// ========================================================================================
// 거리 계산 함수 (Haversine Formula)
// ========================================================================================

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

// ⭐ VWorld API로 주소 및 지목 정보 가져오기
async function getPropsFromVworld() {
    console.log('🗺️ VWorld API 호출 시작...')

    const columns = [
        'pnu', 'sido_nm', 'sgg_nm', 'emd_nm', 'ri_nm',
        'jibun', 'jimok', 'parea', 'rn_nm', 'bld_mnnm',
        'bld_slno', 'ag_geom'
    ].join(',')

    const allZones = [
        { level: 1, data: predictionData.value.zone_level_1 },
        { level: 2, data: predictionData.value.zone_level_2 },
        { level: 3, data: predictionData.value.zone_level_3 }
    ]

    for (const zone of allZones) {
        if (!zone.data || zone.data.length === 0) continue

        for (let i = 0; i < zone.data.length; i++) {
            const location = zone.data[i]

            location.dist_m = calculateDistance(
                missingLocation.value.lat,
                missingLocation.value.lon,
                location.lat,
                location.lon
            )

            try {
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
                    console.error(`VWorld Data API HTTP error! status: ${dataRes.status}`)
                    location.address1 = '위치 정보 없음'
                    location.address2 = ''
                    continue
                }

                const dataText = await dataRes.text()

                let dataResp
                try {
                    dataResp = JSON.parse(dataText)
                } catch (e) {
                    console.error('JSON 파싱 에러:', e)
                    location.address1 = '위치 정보 없음'
                    location.address2 = ''
                    continue
                }

                if (dataResp?.status === 'NOT_FOUND') {
                    console.warn(`Zone ${zone.level}-${i + 1}: VWorld NOT_FOUND - Kakao Geocoder 폴백 시작`)
                    const kakaoAddress = await getKakaoAddressFromCoord(location.lat, location.lon)

                    if (kakaoAddress && kakaoAddress.sido) {
                        const addressParts = [kakaoAddress.sido, kakaoAddress.gungu, kakaoAddress.eup].filter(part => part)
                        // ⭐ address1: 행정구역, address2: 빈 값
                        location.address1 = addressParts.join(' ')
                        location.address2 = '에 있을 것 같아요!'
                        location.sido_nm = kakaoAddress.sido
                        location.sgg_nm = kakaoAddress.gungu
                        location.emd_nm = kakaoAddress.eup

                    } else {
                        location.address1 = '위치 정보 없음'
                        location.address2 = ''
                        console.warn(`Zone ${zone.level}-${i + 1}: Kakao Geocoder 폴백 실패`)
                    }
                    continue
                }

                if (dataResp?.status === 'ERROR') {
                    console.error(`Zone ${zone.level}-${i + 1}: ERROR - ${dataResp.error?.text || '알수없는에러'}`)
                    location.address1 = '위치 정보 없음'
                    location.address2 = ''
                    continue
                }

                let sido = ''
                let sgg = ''
                let emd = ''
                let ri = ''
                let jimok = '토지'

                if (dataResp?.response?.result?.featureCollection?.features?.[0]) {
                    const props = dataResp.response.result.featureCollection.features[0].properties

                    sido = props.sido_nm || ''
                    sgg = props.sgg_nm || ''
                    emd = props.emd_nm || ''
                    ri = props.ri_nm || ''
                    jimok = props.jimok || '토지'

                } else {
                    console.warn(`Zone ${zone.level}-${i + 1}: VWorld 응답 데이터 없음 - Kakao Geocoder 폴백`)
                    const kakaoAddress = await getKakaoAddressFromCoord(location.lat, location.lon)

                    if (kakaoAddress && kakaoAddress.sido) {
                        const addressParts = [kakaoAddress.sido, kakaoAddress.gungu, kakaoAddress.eup].filter(part => part)
                        // ⭐ address1: 행정구역, address2: 빈 값
                        location.address1 = addressParts.join(' ')
                        location.address2 = '에 있을 것 같아요!'
                        location.sido_nm = kakaoAddress.sido
                        location.sgg_nm = kakaoAddress.gungu
                        location.emd_nm = kakaoAddress.eup

                    } else {
                        location.address1 = '위치 정보 없음'
                        location.address2 = ''
                        console.warn(`Zone ${zone.level}-${i + 1}: Kakao Geocoder 폴백 실패`)
                    }
                    continue
                }

                location.sido_nm = sido
                location.sgg_nm = sgg
                location.emd_nm = emd
                location.ri_nm = ri
                location.jimok = jimok

                // ⭐ address1: 행정구역 (시 군 리)
                const addressParts = [sgg, emd, ri].filter(part => part)
                location.address1 = addressParts.join(' ')

                const jimokNaturalText = convertJimokToNaturalLanguage(jimok)
                const excludeJimok = ['전', '답', '임야', '도로']

                // ⭐ address2: 자연어 설명
                if (!excludeJimok.includes(jimok)) {
                    try {
                        const poiResult = await searchVWorldPOI(location.address1)

                        if (poiResult && poiResult.poiName) {
                            location.address2 = `'${poiResult.poiName}'에 있을 것 같아요!`
                        } else {
                            location.address2 = `도로에 있을 것 같아요!`
                        }
                    } catch (poiError) {
                        // POI 검색 실패 시에도 기본 메시지 설정하여 진행
                        console.warn(`Zone ${zone.level}-${i + 1} POI 검색 실패, 기본 메시지 사용:`, poiError)
                        location.address2 = `도로에 있을 것 같아요!`
                    }
                } else {
                    location.address2 = `${jimokNaturalText}에 있을 것 같아요!`
                }

                await new Promise(resolve => setTimeout(resolve, 150))

            } catch (e) {
                console.error(`Zone ${zone.level}-${i + 1} 예상치 못한 에러:`, e)
                location.address1 = '위치 정보 없음'
                location.address2 = ''
            }
        }
    }
    console.log('🗺️ 모든 API 호출 완료')
}

// ⭐ 지목을 자연어로 변환하는 함수
function convertJimokToNaturalLanguage(jimok) {
    const jimokMap = {
        '전': '밭',
        '답': '논',
        '임야': '산',
        '도로': '도로'
    }

    if (jimokMap[jimok]) return jimokMap[jimok]

    for (const [key, value] of Object.entries(jimokMap)) {
        if (jimok.includes(key)) return value
    }

    return jimok
}

// ⭐ VWorld 검색 API로 반경 내 POI 검색
async function searchVWorldPOI(address) {
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
        // ⭐ CORS 문제 해결: VWorld 프록시 URL 사용
        const proxyUrl = `https://www.vworld.kr/proxy.do?url=${encodeURIComponent(searchUrl)}`
        const response = await fetch(proxyUrl)

        if (!response.ok) {
            console.warn('VWorld POI 검색 API 응답 실패:', response.status)
            return null
        }

        const data = await response.json()

        if (data?.response?.result?.items && data.response.result.items.length > 0) {
            const firstItem = data.response.result.items[0]

            return {
                poiName: firstItem.title || firstItem.name || 'POI',
                poiType: firstItem.category || '',
                address: firstItem.address || '',
                point: firstItem.point ? {
                    x: firstItem.point.x,
                    y: firstItem.point.y
                } : null
            }
        }

        return null
    } catch (e) {
        console.error('VWorld POI 검색 에러:', e)
        return null
    }
}

// ========================================================================================
// UI 관련 함수
// ========================================================================================

function mapOrInfo(type) {
    selectedType.value = type
    console.log(`\n🔀 mapOrInfo 호출: ${type}`)
}

function selectLocation(loc, index) {
    // 같은 카드를 다시 클릭한 경우
    if (selectedLocation.value &&
        selectedLocation.value.lat === loc.lat &&
        selectedLocation.value.lon === loc.lon) {
        clearAllRoutes()
        selectedLocation.value = null
        return
    }

    // 다른 카드를 클릭한 경우
    clearAllRoutes()
    selectedLocation.value = loc

    // 지도 중심을 해당 위치로 이동
    if (map.value) {
        const position = new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon)
        map.value.panTo(position)
        map.value.setLevel(6)
    }

    // 경로 그리기
    drawRoute(index, displayZoneLevel.value)
}

function wherePeople() {
    alert('함께하는 사람 보기 기능 (미구현)')
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
/* 기존 CSS는 동일하게 유지 */
.page-container {
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: 100%;
    height: 100%;
    margin: 0;
    padding-top: 50px;
    /* 상단 여백 제거 */
    background: linear-gradient(180deg, #f8f9fd 0%, #ffffff 100%);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    overflow-y: auto;
    scrollbar-width: none;  /* 스크롤바 숨기기 */
}

/* ⭐⭐⭐ 전체 화면 로딩 오버레이 스타일 ⭐⭐⭐ */
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
    0%, 100% {
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
    /* 40vh 대신 고정 높이 사용 */
    flex-shrink: 0;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}


/* ============ 토글 버튼 (지도 바로 아래, 탭 스타일) ============ */
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

/* ⭐ 활성화된 탭 스타일 */
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

/* 호버 효과 (비활성 탭만) */
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

/* ============ 컨텐츠 섹션 ============ */
.content-section {
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fd 100%);
  padding: 0;
  display: flex;
  flex-direction: column;
}


/* ========================실종자 정보============================== */
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
    position : relative;
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

/* ⭐ 더보기 버튼 추가 스타일 */
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

/* ⭐ 드래그 가능한 타임라인 스타일 */
.timeline-container {
    position: relative;
    width: 100%;
    padding: 20px 16px;
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
    transition: width 0.1s ease-out;
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
    width: 100%;
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


/* Floating Action Button */
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

/* Circular Progress Badge */
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

/* Modern Stats Dashboard */
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
</style>
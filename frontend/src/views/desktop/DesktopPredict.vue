<template>
  <div class="desktop-predict">
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
        <h3 class="loading-title">예상 위치 분석 중</h3>
        <p class="loading-message">최근 이동 데이터로 보호자의 환자 이동 패턴을 분석하고 있어요.</p>
        <p class="loading-submessage">분석에는 약간의 시간이 걸릴 수 있습니다.</p>
        <div class="loading-progress">
          <div class="progress-bar"></div>
        </div>
      </div>
    </div>

    <div class="predict-header">
      <div>
        <h1>예상 위치 분석</h1>
        <p class="subtitle">
          GPS 이동 기록을 기반으로 실종 시점 이후 최대 90분까지의 예상 위치를 미리 확인할 수 있어요.
        </p>
      </div>
      <div class="meta">
        <div v-if="analysisTimestamp" class="meta-item">
          <span class="label">분석 시각</span>
          <span class="value">{{ formatDisplayDate(analysisTimestamp) }}</span>
        </div>
        <div v-if="lastKnownLocation?.time" class="meta-item">
          <span class="label">최근 위치 기록</span>
          <span class="value">{{ formatDisplayDate(lastKnownLocation.time) }}</span>
        </div>
      </div>
    </div>

    <div v-if="loadError" class="error-banner">
      <i class="bi bi-exclamation-triangle"></i>
      <span>{{ loadError }}</span>
    </div>

    <div v-if="lessData" class="warning-banner">
      <i class="bi bi-info-circle"></i>
      <span>
        최근 GPS 데이터가 부족해 정확도가 낮을 수 있어요. 환자의 웨어러블이 정상적으로 데이터를 전송하고 있는지 점검해 주세요.
      </span>
    </div>

    <div class="predict-layout">
      <section class="map-section">
        <div class="map-card">
          <div ref="mapContainer" class="map-view"></div>
        </div>

        <div class="timeline-panel">
          <header>
            <div class="left">
              <i class="bi bi-clock-history"></i>
              <div>
                <strong>실종 경과 시간</strong>
                <span>{{ selectedMinutes }}분 경과 시 예상 범위</span>
              </div>
            </div>
            <span class="range-chip">{{ currentRangeLabel }}</span>
          </header>

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
                <span>실종</span>
              </div>
              <div class="timeline-marker" style="left: 33.33%">
                <div class="marker-dot mid"></div>
                <span>30분</span>
              </div>
              <div class="timeline-marker" style="left: 66.66%">
                <div class="marker-dot mid"></div>
                <span>60분</span>
              </div>
              <div class="timeline-marker" style="left: 100%">
                <div class="marker-dot end"></div>
                <span>90분</span>
              </div>
            </div>

            <div
              class="timeline-handle"
              :style="{ left: progressWidth + '%' }"
              @mousedown.stop="startDrag"
              @touchstart.stop="startDrag"
            >
              <div class="handle-icon">
                <i class="bi bi-person-walking"></i>
              </div>
              <div class="handle-tooltip">{{ selectedMinutes }}분</div>
            </div>
          </div>

          <div class="timeline-legend">
            <button
              class="legend-item"
              :class="{ active: selectedMinutes <= 30 }"
              @click="setTime(30)"
            >
              <span class="legend-color color-1"></span>
              <span class="legend-text">실종~30분</span>
            </button>
            <button
              class="legend-item"
              :class="{ active: selectedMinutes > 30 && selectedMinutes <= 60 }"
              @click="setTime(60)"
            >
              <span class="legend-color color-2"></span>
              <span class="legend-text">30~60분</span>
            </button>
            <button
              class="legend-item"
              :class="{ active: selectedMinutes > 60 }"
              @click="setTime(90)"
            >
              <span class="legend-color color-3"></span>
              <span class="legend-text">60~90분</span>
            </button>
          </div>
        </div>
      </section>

      <aside class="insight-section">
        <div class="metrics">
          <div class="metric-card">
            <span class="metric-label">최우선 확인 위치</span>
            <strong class="metric-value">
              {{ highlightStats.topLocation?.name || '데이터 수집 중' }}
            </strong>
            <p class="metric-sub" v-if="highlightStats.topLocation">
              실종지로부터 {{ highlightStats.topLocation.dist }}m · 도보 {{ highlightStats.topLocation.walkMinutes }}분
            </p>
          </div>
          <div class="metric-card">
            <span class="metric-label">예상 반경</span>
            <strong class="metric-value">
              {{ highlightStats.radius }}m
            </strong>
            <p class="metric-sub">현재 선택된 시간대 기준</p>
          </div>
          <div class="metric-card">
            <span class="metric-label">데이터 기반</span>
            <strong class="metric-value">
              {{ highlightStats.gpsPoints }}건
            </strong>
            <p class="metric-sub">분석에 사용된 GPS 레코드 수</p>
          </div>
        </div>

        <div class="list-header">
          <div>
            <h2>예상 위치 우선순위</h2>
            <p>시간대별 상위 {{ displayedZoneToShow.length }}개의 유력 후보지를 확인하세요.</p>
          </div>
        </div>

        <div
          v-if="!isLoading && displayedZoneToShow.length === 0"
          class="empty-state"
        >
          <i class="bi bi-geo-alt"></i>
          <p>표시할 예상 위치가 없습니다. 데이터를 다시 시도해 주세요.</p>
        </div>

        <div class="location-list">
          <article
            v-for="(loc, index) in displayedZoneToShow"
            :key="`${displayZoneLevel}-${index}`"
            class="location-card"
            :class="{ selected: selectedLocation && selectedLocation.lat === loc.lat && selectedLocation.lon === loc.lon }"
            @click="selectLocation(loc, index)"
          >
            <div class="rank">
              <span>{{ index + 1 }}</span>
            </div>
            <div class="details">
              <h3>{{ loc.address1 || loc.name || '주소 정보 없음' }}</h3>
              <p>{{ loc.address2 }}</p>
              <div class="meta">
                <span><i class="bi bi-geo-alt-fill"></i>{{ loc.dist_m }}m 거리</span>
                <span><i class="bi bi-clock"></i>{{ getTimeRangeText(((loc.dist_m) / 20).toFixed(0)) }}</span>
              </div>
            </div>
            <div class="visit">
              <span>{{ loc.visitCount ?? 0 }}회</span>
              <small>최근 방문</small>
            </div>
          </article>
        </div>

        <div v-if="hasMoreData" class="more-container">
          <button class="more-btn" @click="toggleShowMore">
            <span>{{ showAllLocations ? '접기' : '더보기' }}</span>
            <i :class="showAllLocations ? 'bi bi-chevron-up' : 'bi bi-chevron-down'"></i>
          </button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import axios from 'axios'

const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY || '52b0ab3fbb35c5b7adc31c9772065891'
const VWORLD_API_KEY = '6A0CFFEF-45CF-3426-882D-44A63B5A5289'
const TMAP_API_KEY = 'pu1CWi6rz48GHLWhk7NI239il6I2j9fHaSLFeYoi'

const mapContainer = ref(null)
const timelineWrapper = ref(null)

const isLoading = ref(false)
const loadError = ref('')
const lessData = ref(false)

const selectedMinutes = ref(30)
const displayZoneLevel = ref(1)
const showAllLocations = ref(false)
const selectedLocation = ref(null)

const zone_level_1 = ref([])
const zone_level_2 = ref([])
const zone_level_3 = ref([])

const predictionMetadata = ref(null)
const analysisTimestamp = ref(null)
const lastKnownLocation = ref(null)
const missingLocation = ref({ lat: null, lon: null })

const patientUserNo = ref(null)

let map = null
let markers = []
let polylines = []
let centerMarker = null
const circles = ref({
  circle700: null,
  circle1500: null,
  circle2100: null
})

const zone1Routes = ref([])
const zone2Routes = ref([])
const zone3Routes = ref([])

const displayedZone1 = computed(() => {
  const data = zone_level_1.value || []
  return showAllLocations.value ? data : data.slice(0, 4)
})

const displayedZone2 = computed(() => {
  const data = zone_level_2.value || []
  return showAllLocations.value ? data : data.slice(0, 4)
})

const displayedZone3 = computed(() => {
  const data = zone_level_3.value || []
  return showAllLocations.value ? data : data.slice(0, 4)
})

const displayedZoneToShow = computed(() => {
  if (displayZoneLevel.value === 1) return displayedZone1.value
  if (displayZoneLevel.value === 2) return displayedZone2.value
  if (displayZoneLevel.value === 3) return displayedZone3.value
  return []
})

const hasMoreData = computed(() => {
  let total = 0
  if (displayZoneLevel.value === 1) total = zone_level_1.value?.length || 0
  else if (displayZoneLevel.value === 2) total = zone_level_2.value?.length || 0
  else total = zone_level_3.value?.length || 0
  return total > 4
})

const progressWidth = computed(() => (selectedMinutes.value / 90) * 100)

const highlightStats = computed(() => {
  const result = {
    radius: getRadiusByMinutes(selectedMinutes.value),
    gpsPoints: predictionMetadata.value?.gps_records ?? 0,
    topLocation: null
  }

  const first = displayedZoneToShow.value[0]
  if (first) {
    result.topLocation = {
      name: first.address1 || first.name || '주소 정보 없음',
      dist: first.dist_m,
      walkMinutes: Math.round(first.dist_m / 80) || 1
    }
  }

  return result
})

const currentRangeLabel = computed(() => {
  if (selectedMinutes.value <= 30) return '실종~30분'
  if (selectedMinutes.value <= 60) return '30~60분'
  return '60~90분'
})

onMounted(async () => {
  isLoading.value = true
  loadError.value = ''

  try {
    await ensureKakaoLoaded()
    patientUserNo.value = await fetchMyPatient()

    if (!patientUserNo.value) {
      loadError.value = '연결된 환자를 찾을 수 없습니다. 환자 연결 정보를 확인해 주세요.'
      return
    }

    await fetchPredictionData()
    await initMap()
    initCircles()
    makeMarker()
    showCirclesByZoneLevel(displayZoneLevel.value)
    analysisTimestamp.value = new Date()
  } catch (error) {
    console.error('예상 위치 불러오기 오류:', error)
    loadError.value = error?.message || '예상 위치를 불러오는 중 오류가 발생했습니다.'
  } finally {
    isLoading.value = false
  }
})

onBeforeUnmount(() => {
  clearAllRoutes()
  markers.forEach(marker => marker?.setMap?.(null))
  markers = []
  if (centerMarker) {
    centerMarker.setMap(null)
    centerMarker = null
  }
  if (circles.value.circle700) circles.value.circle700.setMap(null)
  if (circles.value.circle1500) circles.value.circle1500.setMap(null)
  if (circles.value.circle2100) circles.value.circle2100.setMap(null)
})

watch(selectedMinutes, (minutes) => {
  let newLevel = 1
  if (minutes <= 30) newLevel = 1
  else if (minutes <= 60) newLevel = 2
  else newLevel = 3

  displayZoneLevel.value = newLevel
  selectedLocation.value = null
  clearAllRoutes()
  updateMapForTime(minutes)
})

watch(displayZoneLevel, () => {
  selectedLocation.value = null
  clearAllRoutes()
  makeMarker()
  showCirclesByZoneLevel(displayZoneLevel.value)
})

watch(showAllLocations, () => {
  selectedLocation.value = null
  makeMarker()
})

function getRadiusByMinutes(minutes) {
  if (minutes <= 30) return Math.round((minutes / 30) * 600)
  if (minutes <= 60) return Math.round(600 + ((minutes - 30) / 30) * (1300 - 600))
  if (minutes <= 90) return Math.round(1300 + ((minutes - 60) / 30) * (2000 - 1300))
  return 2000
}

async function ensureKakaoLoaded() {
  if (window.kakao && window.kakao.maps) {
    await new Promise(resolve => window.kakao.maps.load(resolve))
    return
  }

  await new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false&libraries=services`
    script.onload = () => window.kakao.maps.load(resolve)
    script.onerror = () => reject(new Error('카카오 지도 스크립트를 불러오지 못했습니다.'))
    document.head.appendChild(script)
  })
}

async function initMap() {
  if (!mapContainer.value) return

  map = new window.kakao.maps.Map(mapContainer.value, {
    center: new window.kakao.maps.LatLng(
      missingLocation.value.lat || 37.5665,
      missingLocation.value.lon || 126.978
    ),
    level: 5
  })

  if (missingLocation.value.lat && missingLocation.value.lon) {
    centerMarker = new window.kakao.maps.Marker({
      position: new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon),
      map,
      image: createCenterMarkerImage()
    })
  }
}

function createCenterMarkerImage() {
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 20 20">
  <circle cx="10" cy="10" r="9.5" fill="none" stroke="#4F46E5" stroke-width="1"/>
  <g transform="translate(2, 2)">
    <path fill="#4F46E5" d="M11 5a3 3 0 1 1-6 0 3 3 0 0 1 6 0m-9 8c0 1 1 1 1 1h5.256A4.5 4.5 0 0 1 8 12.5a4.5 4.5 0 0 1 1.544-3.393Q8.844 9.002 8 9c-5 0-6 3-6 4"/>
  </g>
</svg>
`

  return new window.kakao.maps.MarkerImage(
    'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg),
    new window.kakao.maps.Size(40, 40),
    { offset: new window.kakao.maps.Point(20, 38) }
  )
}

function initCircles() {
  if (!map || !missingLocation.value.lat || !missingLocation.value.lon) return

  const center = new window.kakao.maps.LatLng(missingLocation.value.lat, missingLocation.value.lon)

  circles.value.circle700 = new window.kakao.maps.Circle({
    center,
    radius: 0,
    strokeWeight: 3,
    strokeColor: '#22c55e',
    strokeOpacity: 0.8,
    strokeStyle: 'solid',
    fillColor: '#22c55e',
    fillOpacity: 0.12
  })

  circles.value.circle1500 = new window.kakao.maps.Circle({
    center,
    radius: 0,
    strokeWeight: 3,
    strokeColor: '#f97316',
    strokeOpacity: 0.8,
    strokeStyle: 'solid',
    fillColor: '#f97316',
    fillOpacity: 0.12
  })

  circles.value.circle2100 = new window.kakao.maps.Circle({
    center,
    radius: 0,
    strokeWeight: 3,
    strokeColor: '#ec4899',
    strokeOpacity: 0.8,
    strokeStyle: 'solid',
    fillColor: '#ec4899',
    fillOpacity: 0.12
  })
}

function showCirclesByZoneLevel(level) {
  if (!map) return

  hideCircles()

  if (level >= 1 && circles.value.circle700) circles.value.circle700.setMap(map)
  if (level >= 2 && circles.value.circle1500) circles.value.circle1500.setMap(map)
  if (level >= 3 && circles.value.circle2100) circles.value.circle2100.setMap(map)

  updateMapForTime(selectedMinutes.value)
}

function hideCircles() {
  if (circles.value.circle700) circles.value.circle700.setMap(null)
  if (circles.value.circle1500) circles.value.circle1500.setMap(null)
  if (circles.value.circle2100) circles.value.circle2100.setMap(null)
}

function updateMapForTime(minutes) {
  if (!map) return
  if (!circles.value.circle700 || !circles.value.circle1500 || !circles.value.circle2100) return

  if (minutes <= 30) {
    const radius = (minutes / 30) * 600
    circles.value.circle700.setRadius(radius)
    circles.value.circle1500.setRadius(0)
    circles.value.circle2100.setRadius(0)
  } else if (minutes <= 60) {
    circles.value.circle700.setRadius(600)
    const radius = 600 + ((minutes - 30) / 30) * (1300 - 600)
    circles.value.circle1500.setRadius(radius)
    circles.value.circle2100.setRadius(0)
  } else {
    circles.value.circle700.setRadius(600)
    circles.value.circle1500.setRadius(1300)
    const radius = 1300 + ((minutes - 60) / 30) * (2000 - 1300)
    circles.value.circle2100.setRadius(radius)
  }
}

async function fetchMyPatient() {
  const response = await fetch('/api/user/my-patient', {
    method: 'GET',
    credentials: 'include'
  })

  if (!response.ok) return null

  const data = await response.json()
  if (data.message) return null

  return data.userNo
}

async function fetchLatestMissingInfo(patientNo) {
  try {
    const response = await axios.get(`/api/missing-persons/patient/${patientNo}/latest`, {
      withCredentials: true
    })

    if (response?.data?.reportedAt) {
      return {
        missingTime: response.data.reportedAt,
        missingLatitude: response.data.latitude || null,
        missingLongitude: response.data.longitude || null
      }
    }
  } catch (error) {
    console.warn('최신 실종 신고 정보를 찾을 수 없습니다.', error?.message)
  }

  return {
    missingTime: new Date().toISOString(),
    missingLatitude: null,
    missingLongitude: null
  }
}

async function fetchPredictionData() {
  const latestMissingInfo = await fetchLatestMissingInfo(patientUserNo.value)
  const missingTime = latestMissingInfo.missingTime

  const gpsResponse = await axios.get(`/api/pred/${patientUserNo.value}`, {
    params: {
      datetime: new Date(missingTime).getTime()
    },
    withCredentials: true
  })

  const gpsRecords = (gpsResponse.data || []).map(record => {
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
    missing_time: formatSimpleDateTime(missingTime),
    gps_data: gpsRecords,
    analysis_days: 60,
    time_window_hours: 3,
    session_gap: 30,
    min_cluster_size: 10,
    max_search_radius: 2000,
    min_cluster_separation: 200,
    road_network_radius: 2500,
    csv_path: 'all_locations.csv'
  }

  const response = await axios.post(
    'http://localhost:8000/api/predict-destinations',
    requestBody,
    {
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json'
      }
    }
  )

  const data = response.data

  if (data.data_sufficiency === 'nono') {
    throw new Error('예상 위치를 계산할 데이터가 부족합니다.')
  } else if (data.data_sufficiency === 'no') {
    lessData.value = true
  }

  predictionMetadata.value = {
    gps_records: data.metadata?.gps_records || gpsRecords.length,
    total_clusters: data.total_clusters_found || 0
  }

  if (data.last_known_location) {
    lastKnownLocation.value = data.last_known_location
    missingLocation.value = {
      lat: data.last_known_location.latitude,
      lon: data.last_known_location.longitude
    }
  } else if (latestMissingInfo.missingLatitude && latestMissingInfo.missingLongitude) {
    missingLocation.value = {
      lat: latestMissingInfo.missingLatitude,
      lon: latestMissingInfo.missingLongitude
    }
  }

  await processDestinationsToZones(data)
  await requestAllRoutes()
}

async function processDestinationsToZones(apiResponse) {
  const destinationsByDistance = apiResponse.destinations_by_distance || {}

  zone_level_1.value = (destinationsByDistance['500m'] || []).map(dest => normaliseDestination(dest))
  zone_level_2.value = (destinationsByDistance['1000m'] || []).map(dest => normaliseDestination(dest))
  zone_level_3.value = (destinationsByDistance['1500m'] || []).map(dest => normaliseDestination(dest))

  await getAddressAndJimok()

  zone_level_1.value = [...zone_level_1.value]
  zone_level_2.value = [...zone_level_2.value]
  zone_level_3.value = [...zone_level_3.value]
}

function normaliseDestination(dest) {
  return {
    id: dest.destination_id,
    lat: dest.latitude,
    lon: dest.longitude,
    name: dest.name,
    visitCount: dest.visit_count || 0,
    distance: dest.distance_meters,
    waypoints: dest.waypoints || [],
    preferenceScore: dest.preference_score || 0,
    totalGpsRecords: dest.total_gps_records || 0,
    clusterStability: dest.cluster_stability || 0,
    routeMethod: dest.route_method || '',
    value: dest.preference_score || 0,
    address1: '',
    address2: '',
    sido_nm: '',
    sgg_nm: '',
    emd_nm: '',
    ri_nm: '',
    jimok: '',
    dist_m: 0
  }
}

async function getAddressAndJimok() {
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

    await Promise.all(zone.data.map(async (location) => {
      location.dist_m = Math.round(calculateDistance(
        missingLocation.value.lat,
        missingLocation.value.lon,
        location.lat,
        location.lon
      ))

      try {
        const vworldData = await fetchVWorldData(location, columns)
        if (vworldData.status === 'OK' && vworldData.properties) {
          await setLocationFromVWorld(location, vworldData.properties)
        } else {
          await handleKakaoFallback(location)
        }
      } catch (error) {
        console.warn('위치 정보 가져오기 실패, Kakao로 폴백합니다.', error)
        await handleKakaoFallback(location)
      }
    }))
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
    columns,
    geometry: 'true',
    attribute: 'true',
    buffer: '10',
    crs: 'EPSG:4326',
    key: VWORLD_API_KEY,
    domain: 'api.vworld.kr'
  })

  const dataUrl = `https://api.vworld.kr/req/data?${dataParams.toString()}`
  const proxyUrl = `https://www.vworld.kr/proxy.do?url=${encodeURIComponent(dataUrl)}`

  const res = await fetch(proxyUrl)
  if (!res.ok) {
    throw new Error(`VWorld API 오류: ${res.status}`)
  }

  const data = await res.json()
  const properties = data?.response?.result?.featureCollection?.features?.[0]?.properties

  return {
    status: data?.response?.status || 'ERROR',
    properties
  }
}

async function handleKakaoFallback(location) {
  const kakaoAddress = await getKakaoAddressFromCoord(location.lat, location.lon)

  if (kakaoAddress) {
    const addressParts = [kakaoAddress.sido, kakaoAddress.gungu, kakaoAddress.eup].filter(Boolean)
    location.address1 = addressParts.join(' ')
    location.address2 = '주변 도로 인접 지역입니다.'
    location.sido_nm = kakaoAddress.sido
    location.sgg_nm = kakaoAddress.gungu
    location.emd_nm = kakaoAddress.eup
  } else {
    location.address1 = '위치 정보 없음'
    location.address2 = ''
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

  const addressParts = [sgg, emd, ri].filter(Boolean)
  location.address1 = addressParts.join(' ')
  location.address2 = await generateAddress2(jimok, location.address1)
}

async function generateAddress2(jimok, address1) {
  const jimokText = convertJimokToNaturalLanguage(jimok)
  const excludeJimok = ['전', '답', '임야', '도로']

  if (excludeJimok.includes(jimok)) {
    return `${jimokText} 인근 지역으로 이동 가능성이 있어요.`
  }

  const poiResult = await searchVWorldPOI(address1)
  if (poiResult?.poiName) {
    return `${poiResult.poiName} 주변에 머무를 가능성이 높아요.`
  }

  return `${jimokText} 주변 지역일 가능성이 있어요.`
}

function convertJimokToNaturalLanguage(jimok) {
  const table = {
    '전': '밭',
    '답': '논',
    '임야': '산지',
    '도로': '도로',
    '공원': '공원',
    '대': '주거 지역',
    '주택': '주택 단지'
  }

  if (table[jimok]) return table[jimok]

  const match = Object.entries(table).find(([key]) => jimok.includes(key))
  return match ? match[1] : jimok
}

async function searchVWorldPOI(address) {
  if (!address || address.trim() === '') return null

  const params = new URLSearchParams({
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

  const searchUrl = `https://api.vworld.kr/req/search?${params.toString()}`
  const proxyUrl = `https://www.vworld.kr/proxy.do?url=${encodeURIComponent(searchUrl)}`

  const response = await fetch(proxyUrl)
  if (!response.ok) return null

  const data = await response.json()
  const firstItem = data?.response?.result?.items?.[0]

  if (!firstItem) return null

  return {
    poiName: firstItem.title || firstItem.name || ''
  }
}

async function getKakaoAddressFromCoord(lat, lon) {
  return await new Promise(resolve => {
    const geocoder = new window.kakao.maps.services.Geocoder()
    geocoder.coord2RegionCode(lon, lat, (result, status) => {
      if (status === window.kakao.maps.services.Status.OK) {
        const region = result[0]
        resolve({
          sido: region.region_1depth_name || '',
          gungu: region.region_2depth_name || '',
          eup: region.region_3depth_name || ''
        })
      } else {
        resolve(null)
      }
    })
  })
}

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
  return Math.round(R * c)
}

function makeMarker() {
  if (!map) return

  markers.forEach(marker => marker?.setMap?.(null))
  markers = []

  displayedZoneToShow.value.forEach((item, index) => {
    const position = new window.kakao.maps.LatLng(item.lat, item.lon)
    const color = getMarkerColor(displayZoneLevel.value)

    const marker = new window.kakao.maps.Marker({
      position,
      map,
      title: `${item.address1} - ${Math.round(item.dist_m)}m`,
      image: createColoredMarkerImage(color)
    })

    window.kakao.maps.event.addListener(marker, 'click', () => {
      selectLocation(item, index)
    })

    markers.push(marker)
  })
}

function getMarkerColor(level) {
  const colors = {
    1: '#22c55e',
    2: '#f97316',
    3: '#ec4899'
  }
  return colors[level] || '#4F46E5'
}

function createColoredMarkerImage(color) {
  const svg = `
<svg width="32" height="40" viewBox="0 0 32 40" xmlns="http://www.w3.org/2000/svg">
  <path d="M16 0C7.16 0 0 7.16 0 16c0 12 16 24 16 24s16-12 16-24c0-8.84-7.16-16-16-16z" fill="${color}"/>
  <circle cx="16" cy="16" r="6" fill="white"/>
</svg>
`

  return new window.kakao.maps.MarkerImage(
    `data:image/svg+xml;base64,${btoa(svg)}`,
    new window.kakao.maps.Size(32, 40),
    { offset: new window.kakao.maps.Point(16, 40) }
  )
}

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
    drawRoute(index, displayZoneLevel.value)
  }
}

function clearAllRoutes() {
  polylines.forEach(polyline => polyline?.setMap?.(null))
  polylines = []
}

async function requestAllRoutes() {
  if (!missingLocation.value.lat || !missingLocation.value.lon) return

  const allZones = [
    { level: 1, data: zone_level_1.value, storage: zone1Routes },
    { level: 2, data: zone_level_2.value, storage: zone2Routes },
    { level: 3, data: zone_level_3.value, storage: zone3Routes }
  ]

  for (const zone of allZones) {
    zone.storage.value = []

    for (let i = 0; i < zone.data.length; i++) {
      const destination = zone.data[i]

      try {
        let waypointsStr = ''
        if (destination.waypoints && destination.waypoints.length > 0) {
          const coords = destination.waypoints
            .map(wp => (wp.lon && wp.lat) ? `${wp.lon},${wp.lat}` : null)
            .filter(Boolean)
          if (coords.length > 0) {
            waypointsStr = coords.join('_')
          }
        }

        const body = {
          startName: 'start',
          startX: Number(missingLocation.value.lon),
          startY: Number(missingLocation.value.lat),
          endName: destination.address1 || 'end',
          endX: Number(destination.lon),
          endY: Number(destination.lat),
          reqCoordType: 'WGS84GEO',
          resCoordType: 'WGS84GEO',
          searchOption: '0'
        }

        if (waypointsStr) {
          body.passList = waypointsStr
        }

        const resp = await fetch(
          'https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json',
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'appKey': TMAP_API_KEY
            },
            body: JSON.stringify(body)
          }
        )

        if (!resp.ok) {
          zone.storage.value.push(null)
          continue
        }

        const data = await resp.json()
        if (data && Array.isArray(data.features) && data.features.length > 0) {
          zone.storage.value.push(data.features)
        } else {
          zone.storage.value.push(null)
        }
      } catch (error) {
        console.warn('경로 요청 실패', error)
        zone.storage.value.push(null)
      }
    }
  }
}

function drawRoute(index, zoneLevel = 1) {
  if (!map) return

  let routeStorage
  if (zoneLevel === 1) routeStorage = zone1Routes.value
  else if (zoneLevel === 2) routeStorage = zone2Routes.value
  else routeStorage = zone3Routes.value

  const routeFeatures = routeStorage[index]
  if (!routeFeatures || routeFeatures.length === 0) return

  clearAllRoutes()

  routeFeatures.forEach(feature => {
    if (feature.geometry?.type !== 'LineString') return

    const linePath = feature.geometry.coordinates.map(([lng, lat]) =>
      new window.kakao.maps.LatLng(lat, lng)
    )

    const polyline = new window.kakao.maps.Polyline({
      map,
      path: linePath,
      strokeColor: '#2563EB',
      strokeWeight: 5,
      strokeOpacity: 0.8
    })

    polylines.push(polyline)
  })
}

function setTime(minutes) {
  selectedMinutes.value = minutes
}

function startDrag(event) {
  if (!timelineWrapper.value) return

  const moveHandler = (e) => {
    const clientX = e.touches ? e.touches[0].clientX : e.clientX
    updateTimeFromClientX(clientX)
  }

  const endHandler = () => {
    document.removeEventListener('mousemove', moveHandler)
    document.removeEventListener('mouseup', endHandler)
    document.removeEventListener('touchmove', moveHandler)
    document.removeEventListener('touchend', endHandler)
  }

  document.addEventListener('mousemove', moveHandler)
  document.addEventListener('mouseup', endHandler)
  document.addEventListener('touchmove', moveHandler)
  document.addEventListener('touchend', endHandler)

  const clientX = event.touches ? event.touches[0].clientX : event.clientX
  updateTimeFromClientX(clientX)
}

function updateTimeFromClientX(clientX) {
  if (!timelineWrapper.value) return

  const rect = timelineWrapper.value.getBoundingClientRect()
  const x = clientX - rect.left
  const percentage = Math.max(0, Math.min(1, x / rect.width))
  selectedMinutes.value = Math.round(percentage * 90)
}

function getTimeRangeText(minutes) {
  const min = parseInt(minutes, 10)
  if (min < 30) return '0-30분'
  if (min < 60) return '30-60분'
  return '60-90분'
}

function toggleShowMore() {
  showAllLocations.value = !showAllLocations.value
}

function formatSimpleDateTime(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return ''

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function formatDisplayDate(date) {
  const d = new Date(date)
  if (Number.isNaN(d.getTime())) return '-'

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')

  return `${year}.${month}.${day} ${hours}:${minutes}`
}
</script>

<style scoped>
.desktop-predict {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.predict-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  flex-shrink: 0;
}

.predict-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: #f3f4f6;
  border-radius: 12px;
  padding: 10px 14px;
  min-width: 160px;
}

.meta-item .label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
}

.meta-item .value {
  font-size: 14px;
  color: #111827;
  font-weight: 600;
}

.predict-layout {
  display: grid;
  grid-template-columns: minmax(0, 56%) minmax(0, 44%);
  gap: 16px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.map-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  overflow: hidden;
}

.map-card {
  flex: 1 1 auto;
  border-radius: 18px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.1);
  min-height: 0;
}

.map-view {
  width: 100%;
  height: 100%;
  min-height: 320px;
}

.timeline-panel {
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fd 100%);
  border-radius: 20px;
  padding: 18px 20px 20px;
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
}

.timeline-panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.timeline-panel header .left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.timeline-panel header .left i {
  font-size: 20px;
  color: #6366f1;
}

.timeline-panel header .left strong {
  font-size: 16px;
  color: #111827;
}

.timeline-panel header .left span {
  font-size: 12px;
  color: #6b7280;
}

.range-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 14px;
  background: rgba(79, 70, 229, 0.12);
  color: #4338ca;
  font-size: 12px;
  font-weight: 700;
}

.timeline-wrapper {
  position: relative;
  height: 64px;
  user-select: none;
}

.timeline-track {
  position: relative;
  height: 12px;
  border-radius: 6px;
  overflow: hidden;
  background: #e5e7f8;
  display: flex;
}

.timeline-segment {
  flex: 1;
}

.timeline-segment.segment-1 {
  background: rgba(99, 102, 241, 0.18);
}

.timeline-progress {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  border-radius: 6px;
  background: linear-gradient(90deg, #5b7cf5 0%, #7a66f6 100%);
  box-shadow: inset 0 0 0 1px rgba(79, 70, 229, 0.15);
  transition: width 0.18s ease-out;
}

.timeline-glow {
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at center, rgba(79, 70, 229, 0.35), transparent 70%);
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
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
}

.marker-dot,
.marker-dot-1,
.marker-dot-2,
.marker-dot-3 {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  border: 3px solid #d1d5f9;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.1);
}

.timeline-marker:first-of-type .marker-dot {
  border-color: #5b7cf5;
}

.marker-label {
  margin-top: 18px;
}

.timeline-handle {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 42px;
  height: 42px;
  cursor: grab;
  z-index: 10;
}

.timeline-handle:active {
  cursor: grabbing;
}

.handle-icon {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6b5cf6 0%, #9366f2 100%);
  color: #fff;
  box-shadow: 0 12px 28px rgba(79, 70, 229, 0.4);
}

.handle-tooltip {
  position: absolute;
  top: -40px;
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 12px;
  border-radius: 10px;
  background: rgba(17, 24, 39, 0.9);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.timeline-legend {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
}

.legend-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  border-radius: 20px;
  border: 1px solid #e5e7f3;
  background: #ffffff;
  color: #5b6475;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s ease;
}

.legend-item:hover {
  border-color: rgba(79, 70, 229, 0.45);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.12);
}

.legend-item.active {
  border: 1px solid #5b7cf5;
  background: rgba(91, 124, 245, 0.08);
  color: #4338ca;
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.18);
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-color.color-1 {
  background: #66bb6a;
}

.legend-color.color-2 {
  background: #ff9e7e;
}

.legend-color.color-3 {
  background: #ff6b9d;
}

.legend-text {
  font-size: 13px;
  font-weight: 600;
  color: inherit;
}

.insight-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.metric-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px 18px;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 100px;
}

.metric-label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.metric-value {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.metric-sub {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.list-header h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.list-header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.location-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}

.location-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 16px;
  background: #fff;
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
  border: 1px solid transparent;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.location-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 36px rgba(15, 23, 42, 0.12);
}

.location-card.selected {
  border-color: rgba(79, 70, 229, 0.4);
  box-shadow: 0 22px 40px rgba(79, 70, 229, 0.18);
}

.rank {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.details h3 {
  margin: 0;
  font-size: 16px;
  color: #111827;
  font-weight: 700;
}

.details p {
  margin: 6px 0 8px;
  font-size: 13px;
  color: #6b7280;
}

.details .meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #4b5563;
  font-weight: 600;
}

.details .meta span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.visit {
  width: 70px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 12px;
  background: rgba(34, 197, 94, 0.1);
  color: #15803d;
  font-weight: 700;
  font-size: 16px;
}

.visit small {
  font-size: 11px;
  font-weight: 600;
}

.more-container {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.more-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  cursor: pointer;
  transition: 0.2s ease;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
}

.more-btn:hover {
  border-color: #4f46e5;
  color: #4338ca;
  box-shadow: 0 10px 22px rgba(79, 70, 229, 0.18);
}

.warning-banner,
.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 600;
}

.warning-banner {
  background: rgba(234, 179, 8, 0.08);
  color: #92400e;
  border: 1px solid rgba(234, 179, 8, 0.18);
}

.error-banner {
  background: rgba(248, 113, 113, 0.12);
  color: #b91c1c;
  border: 1px solid rgba(248, 113, 113, 0.24);
}

.empty-state {
  padding: 40px 16px;
  text-align: center;
  color: #6b7280;
  background: #fff;
  border-radius: 16px;
  border: 1px dashed #d1d5db;
}

.empty-state i {
  font-size: 28px;
  display: block;
  margin-bottom: 10px;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-content {
  text-align: center;
  padding: 40px;
  background: white;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  max-width: 420px;
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
  color: white;
  font-size: 28px;
  z-index: 2;
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

.loading-title {
  font-size: 22px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px;
}

.loading-message {
  font-size: 15px;
  color: #4b5563;
  margin: 0 0 6px;
}

.loading-submessage {
  font-size: 13px;
  color: #9ca3af;
  margin: 0 0 24px;
}

.loading-progress {
  width: 100%;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.progress-bar {
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
  animation: progressAnimation 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7);
  }
  50% {
    transform: translate(-50%, -50%) scale(1.08);
    box-shadow: 0 0 0 15px rgba(102, 126, 234, 0);
  }
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

@keyframes progressAnimation {
  0% { transform: translateX(-100%); }
  50% { transform: translateX(0); }
  100% { transform: translateX(100%); }
}

@media (max-width: 1280px) {
  .predict-layout {
    grid-template-columns: minmax(0, 55%) minmax(0, 45%);
  }

  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1120px) {
  .predict-layout {
    grid-template-columns: 1fr;
  }

  .map-view {
    height: 480px;
  }
}
</style>


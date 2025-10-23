<!-- 되는 부분 : 
    1. 날짜 선택해서 날짜, 시간 값 받아와서 시간 제대로 설정했는지 확인하는 부분 
    2. 나중에 음성인식으로 출발지점, 도착지점 받아오면 버퍼 그릴 수 있게(코드 수정 가능합니다.)
-->

<!-- 아직 안되는 부분 : 
    1. 음성인식으로 출발, 도착 지점 받아오는 부분
-->
<template>
    <!-- 환자 헤더 -->
    <header class="app-header">
        <div class="icon-wrapper" @click="goBack">
            <i class="icon bi bi-arrow-left icon-bold"></i>
        </div>

        <div class="app-title">
            <img src="/mammamialogo.png" alt="Mamma Mia Logo" class="logo-image">
        </div>

        <div class="icon-wrapper" @click="goHome">
            <i class="icon bi bi-house icon-bold"></i>
        </div>
    </header>
    <div class="main-container">
        <div class="main-body">
            <div class="main-div">
                <div class="map">
                    <div ref="mapContainer" class="sub-div">
                    </div>
                </div>
                <div class="start-location">
                    <span class="start-location-text">어디서 출발하세요?</span>
                    <div class="flex-row-aab">
                        <div class="sub-div-1">

                            <!--  여기가 어디서 출발하세요? -> 장소를 검색해주세요 부분 -->
                            <div class="input-div" @click="voiceSearchStart">
                                <div class="button-div">
                                    <div class="icon">
                                        <div class="svg">
                                            <div class="frame">
                                                <!-- bootstrap-icon에서 받은건데 i태그로 올렸을때 z-index문제때문에 svg태그로 넣었습니다.-->
                                                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24"
                                                    fill="currentColor" class="bi bi-mic" viewBox="0 0 16 16">
                                                    <path
                                                        d="M3.5 6.5A.5.5 0 0 1 4 7v1a4 4 0 0 0 8 0V7a.5.5 0 0 1 1 0v1a5 5 0 0 1-4.5 4.975V15h3a.5.5 0 0 1 0 1h-7a.5.5 0 0 1 0-1h3v-2.025A5 5 0 0 1 3 8V7a.5.5 0 0 1 .5-.5" />
                                                    <path
                                                        d="M10 8a2 2 0 1 1-4 0V3a2 2 0 1 1 4 0zM8 0a3 3 0 0 0-3 3v5a3 3 0 0 0 6 0V3a3 3 0 0 0-3-3" />
                                                </svg>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                        </div>
                        <span class="search-place">장소를 검색해주세요</span>
                    </div>
                </div>
                <div class="where-to-go">
                    <span class="where-are-you-going">어디로 가세요?</span>
                    <div class="flex-row-e">
                        <div class="div">

                            <!-- 여기가 어디로가세요? -> 장소를 검색해주세요 부분 -->
                            <div class="input" @click="voiceSearchEnd">
                                <!-- 여기를 바꾸면됨-->
                                <div class="button">
                                    <div class="i">
                                        <div class="svg-2">
                                            <div class="frame-3">
                                                <!-- bootstrap-icon에서 받은건데 i태그로 올렸을때 z-index문제때문에 svg태그로 넣었습니다.-->
                                                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24"
                                                    fill="currentColor" class="bi bi-mic" viewBox="0 0 16 16">
                                                    <path
                                                        d="M3.5 6.5A.5.5 0 0 1 4 7v1a4 4 0 0 0 8 0V7a.5.5 0 0 1 1 0v1a5 5 0 0 1-4.5 4.975V15h3a.5.5 0 0 1 0 1h-7a.5.5 0 0 1 0-1h3v-2.025A5 5 0 0 1 3 8V7a.5.5 0 0 1 .5-.5" />
                                                    <path
                                                        d="M10 8a2 2 0 1 1-4 0V3a2 2 0 1 1 4 0zM8 0a3 3 0 0 0-3 3v5a3 3 0 0 0 6 0V3a3 3 0 0 0-3-3" />
                                                </svg>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                            </div>
                        </div>
                        <span class="search-place-4">장소를 검색해주세요
                        </span>
                    </div>
                </div>
                <div class="when-are-you-going">
                    <div class="div-5">
                        <span class="span">언제 가세요?</span>
                        <div class="flex-row-bf">
                            <div class="DP_today" :class="{ active: selectedDay == 'today' }"
                                @click="checkDay('today')">
                                <span class="span-6" :class="{ active: selectedDay == 'today' }">오늘</span>
                            </div>

                            <div class="tomorrow" :class="{ active: selectedDay == 'tomorrow' }"
                                @click="checkDay('tomorrow')">
                                <span class="span-7" :class="{ active: selectedDay == 'tomorrow' }">내일</span>
                            </div>
                        </div>
                        <div class="flex-row-cacd">
                            <div class="day-after-tomorrow" :class="{ active: selectedDay == 'totomorrow' }"
                                @click="checkDay('totomorrow')">
                                <span class="span-8" :class="{ active: selectedDay == 'totomorrow' }">모레</span>
                            </div>
                            <div class="other-date" :class="{ active: selectedDay == 'other' }" @click="openModal">
                                <span class="span-9" :class="{ active: selectedDay == 'other' }">{{ customDate }}</span>
                                <div class="vector"></div>
                            </div>
                        </div>
                    </div>
                    <DatePickerModal :isVisible="showModal" @close="closeModal" @confirm="handleDateConfirm" />
                </div>

                <!-- 몇 시에 가세요? -->
                <div class="going-time">
                    <span class="how-many-go">몇 시에 가세요?</span>
                    <div class="div-a">
                        <select id="ampm1" v-model="selectedAmpm1" class="form-select custom-time-select">
                            <option value="" disabled selected hidden>오전/오후</option>
                            <option v-for="(item, index) in ampm" :key="index" :value="item">
                                {{ item }}
                            </option>
                        </select>

                        <select id="time1" v-model="selectedTime1" class="form-select custom-time-select"
                            @change="onTime1Change">
                            <option value="" disabled selected hidden>시간</option>
                            <option v-for="(item, index) in time_" :key="index" :value="item">
                                {{ item }}
                            </option>
                        </select>
                    </div>
                </div>

                <!-- 몇 시에 돌아오세요? -->
                <div class="return-time">
                    <span class="how-many-come-back">몇 시에 돌아오세요?</span>
                    <div class="div-e">
                        <select id="ampm2" v-model="selectedAmpm2" class="form-select custom-time-select">
                            <option value="" disabled selected hidden>오전/오후</option>
                            <option v-for="(item, index) in ampm" :key="index" :value="item">
                                {{ item }}
                            </option>
                        </select>

                        <select id="time2" v-model="selectedTime2" class="form-select custom-time-select"
                            @change="onTime2Change">
                            <option value="" disabled selected hidden>시간</option>
                            <option v-for="(item, index) in time_" :key="index" :value="item">
                                {{ item }}
                            </option>
                        </select>
                    </div>
                </div>

                <!-- 저장취소 버튼 -->
                <div class="save-cancel">
                    <div class="div-element">
                        <div class="button-div-15" @click="scheduleRegistration()">
                            <span class="span-schedule-save">일정 저장</span>
                        </div>
                        <div class="button-div-14" @click="cancle">
                            <span class="span-cancel">취소</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <button class="btn btn-primary" @click="requestRoute"> 경로저장 테스트</button>
    <button class="btn btn-info" @click="bufferTest"> 버퍼 테스트 (구로구청 - 짜장중학교) </button>

</template>

<script setup>
import "./DP_schedule.css";
import { ref, onMounted, watch } from 'vue'
import { lineString, buffer, simplify } from '@turf/turf'
import DatePickerModal from './DatePickerModal_DP_shcedule.vue'
import { useRouter } from 'vue-router'

const routeCoordinates = sessionStorage.getItem('routeCoordinates')
const bufferCoordinates = sessionStorage.getItem('bufferCoordinates')

function bufferTest() {
    if (!routeCoordinates) {
        alert('먼저 경로저장을 눌러주세요!')
        return
    }
    const coords = JSON.parse(routeCoordinates)
    drawRouteOnKakaoMap(mapInstance, coords)
}

// ==========================================================
// 여기서 test목적으로 구로구청 -> 버거킹 구로구청점을 테스트함
// 나중에 음성인식 지원되는거면 어떻게 받아오는지만 해주시면 여기에 추가하겠습니다.
// const s와 d에 반응형변수 value집어넣으면 될거같아요
async function requestRoute() {
    try {
        const s = { place_name: '구로구청', x: 126.887055, y: 37.494411 }
        const d = { place_name: '버거킹 구로구청점', x: 126.890072, y: 37.494841 }

        // 카카오 응답은 x=lng, y=lat (문자열)
        const body = {
            startName: s.place_name,
            startX: Number(s.x),
            startY: Number(s.y),
            endName: d.place_name,
            endX: Number(d.x),
            endY: Number(d.y),
            reqCoordType: 'WGS84GEO',
            resCoordType: 'WGS84GEO',
            searchOption: '0',
        }

        const resp = await fetch(`/api/route/pedestrian`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        })
        if (!resp.ok) {
            console.error('경로 요청 실패:', resp.status, await resp.text())
            return
        }
        const data = await resp.json()
        if (!data || !Array.isArray(data.coordinates) || data.coordinates.length < 2) {
            console.warn('응답 좌표 없음:', data)
            return
        }

        // GeoFencingView에서 사용할 좌표를 세션 스토리지로 전달
        sessionStorage.setItem('routeCoordinates', JSON.stringify(data.coordinates))

        // 출발지/목적지 정보도 세션 스토리지에 저장 (일정 저장 시 사용)
        const locationData = [
            {
                name: s.place_name,
                latitude: Number(s.y),
                longitude: Number(s.x),
                sequenceOrder: 1
            },
            {
                name: d.place_name,
                latitude: Number(d.y),
                longitude: Number(d.x),
                sequenceOrder: 2
            }
        ]
        sessionStorage.setItem('scheduleLocations', JSON.stringify(locationData))
    } catch (e) {
        console.error('요청 오류:', e)
    }
}

const router = useRouter()
const mapContainer = ref(null)
const bufferLevel = ref('1')
let mapInstance = null
let currentPolyline = null
let currentBuffer = null
let startMarker = null
let endMarker = null

/* 카카오 지도 */
const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
onMounted(() => {
    loadKakaoMap(mapContainer.value)
})

function loadKakaoMap(container) {
    const script = document.createElement('script')
    // service, cluster, drawing 라이브러리 활성화
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services,clusterer,drawing&autoload=false`
    document.head.appendChild(script)
    script.onload = () => {
        window.kakao.maps.load(() => {
            const options = {
                center: new window.kakao.maps.LatLng(37.495488, 126.887642),
                level: 4,
                maxLevel: 14,
            }
            mapInstance = new window.kakao.maps.Map(container, options)

        })
    }
}
/**
 * Kakao 지도에 좌표 배열로 Polyline(경로)을 그려주는 함수
 * - coords 형식: [{ latitude: number, longitude: number }, ...] 이거라서
 * - 보통 백엔드(Tmap) 응답의 coordinates 배열을 그대로 사용하면 될 거 같아욤
 */
function drawRouteOnKakaoMap(map, coords) {
    // 유효한 좌표 배열인지 확인
    if (!Array.isArray(coords) || coords.length < 2) {
        console.warn('경로를 그리려면 2개 이상의 좌표가 필요합니다.')
        return
    }

    // Kakao LatLng 배열로 변환 (new kakao.maps.LatLng(위도, 경도))
    const path = coords.map((c) => new kakao.maps.LatLng(c.latitude, c.longitude))

    // 기존 경로/버퍼/마커 제거
    if (currentPolyline) currentPolyline.setMap(null)
    if (currentBuffer) currentBuffer.setMap(null)
    if (startMarker) startMarker.setMap(null)
    if (endMarker) endMarker.setMap(null)

    // Polyline 생성
    currentPolyline = new kakao.maps.Polyline({
        path,
        strokeWeight: 5,
        strokeColor: '#8B5CF6', // 보라색으로 변경
        strokeOpacity: 0.9,
        strokeStyle: 'solid',
    })

    // 기존 폴리라인 제거
    if (currentPolyline) {
        currentPolyline.setMap(null)
    }

    // 지도에 Polyline 표시
    currentPolyline.setMap(map)

    // 시작점과 끝점에 마커 추가
    const startPoint = coords[0]
    const endPoint = coords[coords.length - 1]

    startMarker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(startPoint.latitude, startPoint.longitude),
        map: map,
        image: new kakao.maps.MarkerImage(
            'data:image/svg+xml;base64,' + btoa(`
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#EF4444"/>
          <circle cx="12" cy="9" r="3" fill="white"/>
        </svg>
      `),
            new kakao.maps.Size(40, 40),
            { offset: new kakao.maps.Point(12, 12) }
        )
    })

    endMarker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(endPoint.latitude, endPoint.longitude),
        map: map,
        image: new kakao.maps.MarkerImage(
            'data:image/svg+xml;base64,' + btoa(`
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#EF4444"/>
          <circle cx="12" cy="9" r="3" fill="white"/>
        </svg>
      `),
            new kakao.maps.Size(40, 40),
            { offset: new kakao.maps.Point(12, 12) }
        )
    })

    // 버퍼 생성
    createBuffer(map, coords)

    // 경로 전체가 한 화면에 보이도록 지도 범위 맞추기
    const bounds = new window.kakao.maps.LatLngBounds()
    path.forEach((latLng) => bounds.extend(latLng))
    map.setBounds(bounds)
}

/**
 * Turf.js를 사용해 경로 주변에 버퍼를 생성하고 카카오맵에 폴리곤으로 그립니다.
 */
function createBuffer(map, coords) {
    try {
        // 1) 좌표를 Turf 형식으로 변환 [lng, lat]
        const turfCoords = coords.map(c => [c.longitude, c.latitude])

        // 2) LineString 생성
        const line = lineString(turfCoords)

        // 3) 단계별 버퍼 크기 설정 (미터 단위)
        //============================================================//
        // 주형/ 환자가 추가하는 부분은 일단 30m로만 설정하는것으로 할게요
        const bufferSizes = {
            '1': 30,   // 30m
        }

        const bufferSize = bufferSizes['1'] || 30

        // 4) 버퍼 생성
        const buffered = buffer(line, bufferSize, { units: 'meters' })

        // 5) 기존 버퍼 제거
        if (currentBuffer) {
            currentBuffer.setMap(null)
        }

        // 6) GeoJSON Polygon을 카카오맵 폴리곤으로 변환
        const coordinates = buffered.geometry.coordinates[0] // 외곽 링
        const kakaoPath = coordinates.map(coord =>
            new kakao.maps.LatLng(coord[1], coord[0]) // [lng, lat] -> (lat, lng)
        )

        // 7) 카카오맵 폴리곤 생성
        currentBuffer = new kakao.maps.Polygon({
            path: kakaoPath,
            strokeWeight: 2,
            strokeColor: '#EF4444',
            strokeOpacity: 0.8,
            fillColor: '#EF4444',
            fillOpacity: 0.4
        })

        // 8) 지도에 표시
        currentBuffer.setMap(map)

        // 9) 버퍼 좌표를 저장 (나중에 위치 판정용)
        sessionStorage.setItem('routeBufferPolygon', JSON.stringify(coordinates))

        console.log(`버퍼 생성 완료: ${bufferSize}m (${bufferLevel.value}단계)`)

    } catch (error) {
        console.error('버퍼 생성 중 오류:', error)
    }
}

// ==========================================================
// 출발장소 도착장소 로직 끝

// 시간설정 로직 부분
// ==========================================================
const selectedDay = ref('')
const ampm = ['오전', '오후']
const time_ = ['1시', '2시', '3시', '4시', '5시', '6시', '7시', '8시', '9시', '10시', '11시', '12시']

const selectedAmpm1 = ref('')
const selectedAmpm2 = ref('')
const selectedTime1 = ref('')
const selectedTime2 = ref('')

if (selectedDay.value) {
    customDate.value === '다른 날짜'
}

function checkDay(s) {
    selectedDay.value = s
    if (customDate.value != '다른 날짜') {
        customDate.value = '다른 날짜'
    }
}

function cancle() {
    window.location.reload();
}

function onTime1Change() {
    console.log('시간1 변경:', selectedTime1.value)
}

function onTime2Change() {
    console.log('시간2 변경:', selectedTime2.value)
}

// 
function save() {
    if (!selectedDay.value) {
        alert(` 날짜를 선택해주세요 `)
        return
    } else if (!selectedAmpm1.value || !selectedAmpm2.value || !selectedTime1.value || !selectedTime2.value) {
        alert(`시간을 설정해주세요`)
        return
    }

    if (selectedAmpm1.value === '오후' && selectedAmpm2.value === '오전') {
        alert('오전과 오후가 바뀐걸까요?')
        return
    }
    const startHour = Number(selectedTime1.value.replace('시', ''))
    const endHour = Number(selectedTime2.value.replace('시', ''))
    if (startHour < endHour) {
        if (startHour > endHour) {
            alert(`시간설정 잘못됨`)
            return
        } else if (startHour === endHour) {
            alert(`도착 시간을 조금 늦춰볼까요?`)
            return
        } else {

            // 오늘 선택일떄 현재시간보다 빠른일정 등록 방지
            if (selectedDay.value === 'today') {
                // 현재 시/분 및 오후/오전 계산
                const now = new Date();
                let nowHour = now.getHours();

                // 현재 오전/오후 및 12시간제 시 계산
                const nowAmpm = nowHour < 12 ? '오전' : '오후';
                let nowHour12 = nowHour % 12;
                if (nowHour12 === 0) nowHour12 = 12; // 0시는 12시

                // 사용자 입력(문자열 숫자 -> 실제 숫자!)
                const pickAmpm = selectedAmpm1.value;
                const pickHour = Number(selectedTime1.value.replace('시', ''));

                // 비교: 오전/오후가 기준, 그리고 시 기준 비교
                let isPast = false;
                if (pickAmpm === nowAmpm) {
                    if (pickHour < nowHour12) {
                        isPast = true;
                    }
                } else if (pickAmpm === '오전' && nowAmpm === '오후') {
                    isPast = true;
                }

                if (isPast) {
                    alert('현재 시간보다 빠른 일정은 설정할 수 없습니다.');
                    return;
                }
            }
            // 나중에 백이랑 연결할 때, 출발/도착 장소 + 날짜, 시간 보내는 함수를 넣으면 됨 들어가는 params : userNo startLocation endLocation scheduleDate startTime endTime
            if (selectedDay.value === 'other') {
                alert(`다른 날짜 선택 시간설정 잘됨 T1 : ${selectedTime1.value} T2 : ${selectedTime2.value} customDate : ${customDate.value}`)
            }
            // 날짜선택에 따라서 if 와 else구문으로 각 날짜에 맞게 넣으면됨 + 오늘은 오늘, 내일은 +1, 모레는 +2로 넣어야함
            else {
                alert(`시간설정 잘됨 T1 : ${selectedTime1.value} T2 : ${selectedTime2.value} Date : ${selectedDay.value}`)
            }
        }
    }
    else if (selectedAmpm1.value === '오전' && selectedAmpm2.value === '오후') {
        if (selectedDay.value === 'today') {
            // 현재 시/분 및 오후/오전 계산
            const now = new Date();
            let nowHour = now.getHours();

            // 현재 오전/오후 및 12시간제 시 계산
            const nowAmpm = nowHour < 12 ? '오전' : '오후';
            let nowHour12 = nowHour % 12;
            if (nowHour12 === 0) nowHour12 = 12; // 0시는 12시

            // 사용자 입력(문자열 숫자 -> 실제 숫자!)
            const pickAmpm = selectedAmpm1.value;
            const pickHour = Number(selectedTime1.value.replace('시', ''));

            // 비교: 오전/오후가 기준, 그리고 시 기준 비교
            let isPast = false;
            if (pickAmpm === nowAmpm) {
                if (pickHour < nowHour12) {
                    isPast = true;
                }
            } else if (pickAmpm === '오전' && nowAmpm === '오후') {
                isPast = true;
            }

            if (isPast) {
                alert('현재 시간보다 빠른 일정은 설정할 수 없습니다.');
                return;
            }
        }
    } else {
        alert(`도착하는 시간이 출발시간보다 빨라요!`)
    }
}
//modal
// ========================================================
const showModal = ref(false)
const customDate = ref('다른 날짜')

function openModal() {
    selectedDay.value = 'other'
    showModal.value = true
}

function closeModal() {
    showModal.value = false
}

// 모달에서 오늘보다 전 일정 선택 방지
function handleDateConfirm(date) {
    const pickedDate = new Date(date)

    const today = new Date();
    today.setHours(0, 0, 0, 0)

    if (pickedDate < today) {
        alert('오늘보다 전 일정은 등록할 수 없어요')
        return
    }

    customDate.value = date
    selectedDay.value = 'other'
    closeModal()
}

// 일정추가 API
async function scheduleRegistration(userNo, startLoc, endLoc, date, startTime, endTime) {
    save()
    const requestData = {
        title: endLoc,
        content: endLoc,
        date: date,
        startTime: startTime,
        endTime: endTime,
        user_no: userNo,
        routeCoordinates: JSON.parse(routeCoordinates),
        bufferCoordinates: JSON.parse(bufferCoordinates)
    }
    const response = await fetch(`/api/schedule/create`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        credentials: 'include', // 세션 쿠키 포함
        body: JSON.stringify(requestData)
    })
    if (!response.ok) {
        const error = await response.json()
        throw new Error(error.message || `일정 ${isEditMode.value ? '수정' : '저장'}에 실패했습니다.`)
    }

    const result = await response.json()
    console.log(`일정 ${isEditMode.value ? '수정' : '저장'} 성공:`, result)
}

// 장소입력 출발~도착
function voiceSearchStart() {
    alert(`시작`)
}

function voiceSearchEnd() {
    alert(`끝`)
}

/* 헤더 */
function goBack() {
  router.go(-1)
}

function goHome() {
    router.push('/DP');
}

</script>

<style scoped>
.main-container,
.main-body,
.main-div {
    /* z-index 값이 있다면 제거하거나 낮은 값으로 변경 */
    z-index: auto;
    /* 또는 1 정도로 */
    right: 4px;
    top: -45px;
    overflow-x: hidden;
    /* 이 줄 추가 */
    width: 120%;
    max-width: 100vw;
    /* 뷰포트 너비 초과 방지 */
}

.bd {
    border: dashed 2px violet;
}

.DP_today.active,
.tomorrow.active,
.day-after-tomorrow.active,
.other-date.active {
    background: rgba(74, 98, 221, 0.85) !important;
    border-color: rgba(74, 98, 221, 0.85);
}

.span-6.active,
.span-7.active,
.span-8.active {
    color: #FFFFFF !important;
}

.span-9.active {
    font-size: 20px;
    color: #FFFFFF !important;
}

/** modal */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 9999 !important;
}

.modal-content {
    background: white;
    border-radius: 12px;
    padding: 24px;
    width: 90%;
    max-width: 400px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    z-index: 10000;
    /* overlay보다 더 높게 */
    position: relative;
    /* z-index 적용을 위해 필수 */
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.close-btn {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
}

.modal-body {
    margin-bottom: 20px;
}

.date-input {
    width: 100%;
    padding: 12px;
    font-size: 16px;
    border: 1px solid #ddd;
    border-radius: 8px;
}

.modal-footer {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
}

.confirm-btn,
.cancel-btn {
    padding: 10px 20px;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 16px;
}

.confirm-btn {
    background: rgba(74, 98, 221, 0.85);
    color: white;
}

.cancel-btn {
    background: #ddd;
    color: #333;
}

/* ===== Bootstrap 5 기반 커스텀 select 스타일 ===== */
.custom-time-select {
    font-size: 24px !important;
    font-weight: 600 !important;
    color: #516578 !important;
    /* Bootstrap의 진한 회색 */
    padding: 0.5rem 2.5rem 0.5rem 0.75rem !important;
    background-color: #fff;
    border: 1px solid #ced4da;
    border-radius: 0.375rem;
    height: 100px !important;
    /* Bootstrap 5의 form-select 화살표 유지 */
    background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3e%3cpath fill='none' stroke='%23343a40' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='m2 5 6 6 6-6'/%3e%3c/svg%3e");
    background-repeat: no-repeat;
    background-position: right 1rem center;
    background-size: 16px 20px;
}

/* placeholder 색상 (선택 안 된 상태) */
.custom-time-select option[disabled] {
    color: #6c757d !important;
}

/* 선택된 값 색상 */
.custom-time-select:not([value=""]) {
    color: #5d656d !important;
}

/* focus 시 스타일 */
.custom-time-select:focus {
    border-color: rgba(74, 98, 221, 0.85);
    outline: 0;
    box-shadow: 0 0 0 0.25rem rgba(74, 98, 221, 0.25);
}

/* option 스타일 */
.custom-time-select option {
    color: #212529;
    background-color: #fff;
    padding: 0.5rem;
    font-size: 22px;
}

/* div 레이아웃 */
.div-a,
.div-e {
    display: flex;
    gap: 0.5rem;
    align-items: center;
}

/* 시간 관련 라벨 */
.how-many-go,
.how-many-come-back {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 500;
    color: #212529;
}

/* 환자 헤더 */
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 70px;
  padding: 0 24px;
  background-color: #FFFFFF;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.icon-wrapper {
  cursor: pointer;
}

.icon-wrapper .icon {
  width: 24px;
  height: 24px;
  fill: #000000;
}

.icon-bold {
  font-size: 1.3rem;
  -webkit-text-stroke: 0.8px currentColor;
}

.logo-image {
  height: 32px;
  /* 또는 원하는 크기로 조정 */
  object-fit: contain;
}

</style>

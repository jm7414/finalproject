<template>
    <div class="total-view">
        <div class="map-container mb-5">
            <div ref="mapContainer" class="map-box"></div>
            <div class="card-wrapper">
                <div class="card-wrapper-info">
                    <div class="card-info-name">
                        <span>ㅇㅇㅇ님</span> <!-- 여기에는 이제 처음 아이디로 가져오는 값이 들어가야함-->
                    </div>
                    <div class="card-info-time">
                        <span>{{ currentDateTime }}</span> <!-- 현재시간을 넣는 로직 -->
                    </div>
                    <button class="btn-random" @click="randomMarker()">랜덤마커 찍기</button>
                </div>
                <div class="card" v-for="(item, idx) in coords" :key="idx" :class="{ 'primary': idx === 0 }">
                    <IconMarker :primary="idx != -1" class="alert-icon" />
                    <div class="text">
                        <div class="title">{{ idx + 1 }}번 예상위치</div>
                        <div class="subtitle">{{ item.regionName }}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import axios from 'axios'
import IconMarker from '@/components/AlertIconMarker.vue' // 느낌표 사진(svg)받아오기위해 components폴더에있는 IconMarker.vue가져옴

const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
const mapContainer = ref(null)
let mapInstance = null
let geocoder = null  // 좌표값 -> 주소로 바꾸기 위해서 geocorder사용

const markers = reactive([])
const circles = reactive([])
const coords = reactive([])

///
/// 시간 추가하는 로직
/// 반응형으로 시간넣을 공간 만듦 -> 년/월/일/시/분까지 가져옴
/// 추가적으로 const ss = String(now.getSeconds()).padStart(2,'0')을 추가하고 :${ss} 넣으면 초까지 나오는데 너무 난잡해보여서 뺌
///
// 반응형 현재 시간
const currentDateTime = ref('')
let intervalId = null

function updateDateTime() {
    const now = new Date()
    const yyyy = now.getFullYear()
    const mm = String(now.getMonth() + 1).padStart(2, '0')
    const dd = String(now.getDate()).padStart(2, '0')
    const hh = String(now.getHours()).padStart(2, '0')
    const min = String(now.getMinutes()).padStart(2, '0')
    currentDateTime.value = `${yyyy}-${mm}-${dd} ${hh}:${min}`
}

onMounted(() => {
    // 1초마다 시간 갱신
    updateDateTime()
    intervalId = setInterval(updateDateTime, 1000)

    ///
    /// 여기부터 카카오맵 API key이용해서 지도 불러오는 작업
    ///
    // Kakao SDK 로드
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services&autoload=false`
    document.head.appendChild(script)

    script.onload = () => {
        window.kakao.maps.load(() => {
            geocoder = new window.kakao.maps.services.Geocoder()
            mapInstance = new window.kakao.maps.Map(mapContainer.value, {
                center: new window.kakao.maps.LatLng(37.537248, 126.857187),
                level: 6
            })
            // 처음 지도 불러오고 그 다음에 랜덤좌표 뿌리는 것 -> 여기가 나중에 훈련한 모델로 불러오는 FastAPI주소값으로 바꿔야함
            randomMarker()
        })
    }
})

// 컴포넌트 언마운트 시 타이머 정리
onUnmounted(() => {
    clearInterval(intervalId)
})

/// 랜덤좌표 가져와서 마커 + 버퍼 생성하기 일단 3개만 가져오는 함수를 적용
async function randomMarker(count = 3) {
    try {
        // FastAPI 좌표 가져오기
        const res = await axios.get('http://localhost:8001/random_coords', { params: { count } })
        const list = res.data.map(item => ({
            latitude: item.latitude,
            longitude: item.longitude,
            regionName: '',
        }))
        coords.splice(0, coords.length, ...list)

        // 각 좌표에 대해 coord2RegionCode 호출
        coords.forEach((c, idx) => {
            geocoder.coord2RegionCode(
                c.longitude,
                c.latitude,
                (result, status) => {
                    if (status === window.kakao.maps.services.Status.OK && result.length) {
                        c.regionName = result[0].address_name

                        const jsonStr = JSON.stringify(result);
                        const arr = JSON.parse(jsonStr);      // 문자열을 다시 객체(배열)로 변환
                        console.log('두 번째 주소:', arr[1].address_name);
                        console.log(`coords[${idx}]:`, c)
                    } else {
                        c.regionName = '조회실패'
                    }
                }
            )
        })

        // 마커 & 서클 그리기
        /// 이 부분이 처음 마커, 버퍼 초기화하는 부분 -> 그래야 시연할 때 버퍼가 지워지고 다시 생김 
        /// 여기는 나중에 진짜 예측값 들어오면 지워야함 저기 forEach만 지우면 됨
        markers.forEach(m => m.setMap(null))
        circles.forEach(c => c.setMap(null))
        markers.length = 0; circles.length = 0

        /// 이거는 3개 랜덤좌표의 값을 쭉 가져왔을 때 3개의 좌표에 대한 평균을 내서 center 값을 할당할 것임
        let sumLat = 0, sumLng = 0

        coords.forEach(item => {
            const pos = new window.kakao.maps.LatLng(item.latitude, item.longitude)
            sumLat += item.latitude; sumLng += item.longitude

            // 마커 생성한것을 좌표값 3개로 슉 슉 넗어줌 (forEach안에 있기때문에 반복으로 슉 슉 들어감)
            const marker = new window.kakao.maps.Marker({ position: pos, map: mapInstance })
            markers.push(marker)

            const circle = new window.kakao.maps.Circle({
                /// 이 부분이 radius : 200 => 200m로 버퍼 설정하는 부분
                center: pos, radius: 200,
                strokeWeight: 5, strokeColor: '#75B8FA', strokeOpacity: 1, strokeStyle: 'line', /// 여기 strokeStyle이 line이 좋을지 dashed가 좋을지 모르겠음ㅁㅁㅁㅁ
                fillColor: '#CFE7FF', fillOpacity: 0.7
            })
            circle.setMap(mapInstance)
            circles.push(circle)
        })

        /// 여기가 지도 중앙 좌표값 계산하는거
        if (coords.length) {
            const avg = new window.kakao.maps.LatLng(sumLat / coords.length, sumLng / coords.length)
            mapInstance.setCenter(avg) // 여기서 center를 avg값으로 해줌
        }
    } catch (e) {
        console.error(e)
    }
    console.log(`생성된 마커는 : ${JSON.stringify(coords, null, 2)}`)
}
</script>
<style scoped>
.total-view {
  position: relative;
  width: 500px;
  margin: 38px auto 25px;
}

/* 1. map-container는 백그라운드 */
.map-container {
  width: 100%;
  height: auto;
  position: relative;
  z-index: 0;
}

/* 2. .card-wrapper-info를 map-container 위에 올리기 */
.card-wrapper-info {
  position: relative;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;       /* 최상위로 */
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 500px;
  background: transparent;
  margin-top: 10px;
}

/* 3. 카드 그룹 역시 절대 위치로 관리 */
.card-wrapper {
  position: relative;
  left: 50%;
  transform: translateX(-50%);
  z-index: 0;
  width: 100%;
  max-width: 500px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 카드 높이와 스타일 유지 */
.card {
  position: relative;
  display: flex;
  flex-direction : row;
  justify-content: flex-start;
  align-items: center;
  height: 120px;
  padding: 0 16px 0 0;
  background: #fff;
  border: 1px solid #d0d0d0;
  border-radius: 15px;
  box-shadow: 0px 4px 15px rgba(0,0,0,0.15);
  z-index: 6;
}

.card-info-name {
  display: flex;
  justify-content: center;
  align-self: center;
  background: #808AFF;
  border: 1px solid #d0d0d0;
  border-radius: 8px;
  width: 150px;
  height: 40px;
  box-shadow: 0px 4px 15px rgba(0, 0, 0, 0.15);
  font-size: 1.5em;
  font-weight: 800;
}

.card-info-time {
  display: flex;
  justify-content: center;
  align-items: center;
  background: #808AFF;
  border: 1px solid #d0d0d0;
  border-radius: 8px;
  width: 170px;
  height: 40px;
  box-shadow: 0px 4px 15px rgba(0, 0, 0, 0.15);
  font-size: 20px;
}

/* 지도 박스 높이 고정 */
.map-box {
  width: 100%;
  height: 400px;
  border-radius: 15px;
  overflow: hidden;
}

.btn-random {
  align-self: center;
  /* 중앙 정렬 */
  padding: 8px 12px;
  background: #808AFF;
  color: #fff;
  width: 30%;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  height: 40px;
}

.text .title {
    font-size: 24px;
    font-weight: bold;
    color: #333;
}

.text .subtitle {
    font-size: 18px;
    color: #666;
    margin-top: 4px;
}

.alert-icon {
  flex-shrink: 0;
  margin-right: 40px;  
  margin-left: 0; 
}
</style>

<template>
  <div class="main-container">
    <div class="alert">
      <div class="div-1">
        <div class="div-2"></div>
        <div class="button"><span class="allow">허용</span></div>
        <div class="div-3">
          <div class="svg">
            <div class="frame"></div>
          </div>
        </div>
      </div>
      <span class="location-permission">위치 기반 서비스를 위해 위치 권한이 필요 합니다</span>
    </div>
    <div class="map">
      <div class="div-4">

        <!-- 현위치, +,- 버튼 있는 div-->
        <div class="div-5">
          <div class="button-6">
            <div class="div-7">
              <div class="svg-8">
                <div class="frame-9"></div>
              </div>
            </div>
          </div>
          <div class="plus">
            <div class="div-a">
              <div class="svg-b">
                <div class="frame-c"></div>
              </div>
            </div>
          </div>
          <div class="minus">
            <div class="i">
              <div class="svg-d">
                <div class="frame-e"></div>
              </div>
            </div>
          </div>
        </div>
        
        <div ref="mapContainer" class="flex-column-e">
        </div>
        <ul ref="placesList" class="places-list" style="display: flex; top:400px;"></ul>
      </div>
    </div>
    <div class="search">
      <div class="div-13">
        <div class="input">
          <div class="i-14" @click="searchPlace">
            <div class="svg-15">
              <div class="frame-16"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <form id="search" @submit.prevent="searchNearby(content)">
      <input v-model="content" type="text" class="search-bar" placeholder="상담소명, 지역명으로 검색"></input>
    </form>

     <!-- 
      <div class="section">
        
      <div class="content">
        
        <div class="frame-20">
          <div class="frame-21">
            <span class="counseling-center">유해피 심리상담센터 목동점</span><span class="address">서울시 양천구 신정동 128-113 <div
              class="vector"></div><span class="km">5.3km</span></span>
          </div>
          <div class="span"><span class="open">영업중</span></div>
        </div>
        
        <div class="content-17">
          <div class="button-18" @click="call">
            <div class="frame-19">
              <span class="phone">전화</span>
              <div class="svg-1a">
                <div class="frame-1b"></div>
              </div>
            </div>
          </div>
          <div class="button-1c" @click="findRoute">
            <div class="frame-1d">
              <div class="svg-1e">
                <div class="frame-1f"></div>
              </div>
              <span class="directions">길찾기</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    -->
    
    
  </div>
</template>

<script setup>
import "./heartCare.css";
import { ref, onMounted } from 'vue';

const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891';
const mapContainer = ref(null);
let map;                         // 1. 전역 변수 선언
let geocoder = null;
let markers = []
let infowindow = null;
const content = ref('');
 
onMounted(() => {
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services&autoload=false`
  document.head.appendChild(script)

  script.onload = () => {
    window.kakao.maps.load(() => {
      geocoder = new window.kakao.maps.services.Geocoder()
      map = new window.kakao.maps.Map(mapContainer.value, {    // 2. 지도 생성 시 전역 변수에 할당
        center: new window.kakao.maps.LatLng(37.494382, 126.887690),
        level: 6
      })
    })
  }
})


function searchNearby() {
  // 3. 검색 전 기존 마커 제거
  markers.forEach(marker => marker.setMap(null))
  markers = [];
  placesList.value.innerHTML = '';


  if (!content.value) {
    alert('입력해라')
    return;
  }

  const ps = new kakao.maps.services.Places()
  const center = map.getCenter()
  const options = {
    location: center,
    radius: 1000,
    sort: kakao.maps.services.SortBy.DISTANCE
  }

  ps.keywordSearch(content.value, placesSearchCB, options)

}

function placesSearchCB(data, status) {
  if (status === kakao.maps.services.Status.OK) {
    const bounds = new kakao.maps.LatLngBounds()
    data.forEach(place => {
      displayMarker(place)
      bounds.extend(new kakao.maps.LatLng(place.y, place.x))
    })
    map.setBounds(bounds)

  }
}

function displayMarker(place) {
  getListItem(0,place)
  const marker = new kakao.maps.Marker({
    map,
    position: new kakao.maps.LatLng(place.y, place.x)
  })

  // 2. 생성된 마커를 배열에 추가
  markers.push(marker)

  kakao.maps.event.addListener(marker, 'click', () => {
    infowindow.setContent(
      `<div style="padding:5px;font-size:12px;">${place.place_name}</div>`
    )
    infowindow.open(map, marker)
  })
}

function getListItem(index, places) {

    var el = document.createElement('li'),
    itemStr = '<span class="markerbg marker_' + (index+1) + '"></span>' +
                '<div class="info">' +
                '   <h5>' + places.place_name + '</h5>';

    if (places.road_address_name) {
        itemStr += '    <span>' + places.road_address_name + '</span>' +
                    '   <span class="jibun gray">' +  places.address_name  + '</span>';
    } else {
        itemStr += '    <span>' +  places.address_name  + '</span>'; 
    }
                 
      itemStr += '  <span class="tel">' + places.phone  + '</span>' +
                '</div>';           

    el.innerHTML = itemStr;
    el.className = 'item';

    return el;
}


function call() {
  alert('전화');
}

function findRoute() {
  alert('길찾기');
}
</script>


<style scoped></style>
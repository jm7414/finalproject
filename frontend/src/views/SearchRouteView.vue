<template>
  <div class="page">
    <div class="card">
      <!-- 교체 버튼: 카드 내부 오른쪽 중앙 고정 -->
      <button class="swap-fab" title="상하 교체" @click="swapFromTo">⇅</button>
      <div class="row between">
        <div class="fromto">
          <div class="input-item" :class="{ active: activeField==='start' }">
            <div class="label">From</div>
            <input
              v-model="startQuery"
              type="text"
              placeholder="예) 구로구청"
              @focus="setActive('start')"
              @keyup.enter="searchActive()"
            />
          </div>
          <div class="input-item" :class="{ active: activeField==='dest' }">
            <div class="label">To</div>
            <input
              v-model="destQuery"
              type="text"
              placeholder="예) 고려대 구로병원"
              @focus="setActive('dest')"
              @keyup.enter="searchActive()"
            />
          </div>
        </div>
      </div>
      <!-- 안내 문구 제거 -->
    </div>

    <div class="section-title">장소 결과</div>

    <!-- 단일 결과 리스트: activeField에 따라 검색/선택 동작 -->
    <ul class="results unified" v-show="showResults">
      <li v-for="(p, idx) in currentResults" :key="p.id" @click="selectActive(p)">
        <div class="item-left">
          <div class="name">{{ p.place_name }}</div>
          <div class="addr">{{ p.road_address_name || p.address_name }}</div>
        </div>
        <div class="item-right">
          <button class="arrive">도착</button>
        </div>
      </li>
      <li v-if="!currentResults.length" class="empty">검색 결과가 없습니다. 검색어를 입력하고 Enter를 눌러보세요.</li>
    </ul>

    <div class="actions">
      <button class="primary" :disabled="!canRoute" @click="requestRoute">설정</button>
      <button class="ghost" @click="resetAll">취소</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const KAKAO_JS_KEY = '52b0ab3fbb35c5b7adc31c9772065891'
const startQuery = ref('')
const destQuery = ref('')
const startResults = ref([])
const destResults = ref([])
const currentResults = computed(() => (activeField.value === 'start' ? startResults.value : destResults.value))
const selectedStart = ref(null)
const selectedDest = ref(null)
let placesService = null
const activeField = ref('start')
const showResults = ref(false)

onMounted(() => {
  ensureKakaoPlaces()
})

function ensureKakaoPlaces() {
  if (window.kakao && window.kakao.maps && window.kakao.maps.services) {
    placesService = new window.kakao.maps.services.Places()
    return
  }
  const script = document.createElement('script')
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&libraries=services&autoload=false`
  document.head.appendChild(script)
  script.onload = () => {
    window.kakao.maps.load(() => {
      placesService = new window.kakao.maps.services.Places()
    })
  }
}

function searchPlaces(kind) {
  const query = kind === 'start' ? startQuery.value : destQuery.value
  if (!query || !placesService) return

  // 간단 테스트: 상위 5개만
  placesService.keywordSearch(query, (data, status) => {
    if (status !== window.kakao.maps.services.Status.OK) {
      if (kind === 'start') startResults.value = []
      else destResults.value = []
      return
    }
    const list = (data || []).slice(0, 5)
    if (kind === 'start') startResults.value = list
    else destResults.value = list
    showResults.value = true
  })
}

function selectPlace(kind, place) {
  if (kind === 'start') {
    selectedStart.value = place
    startQuery.value = place.place_name
  } else {
    selectedDest.value = place
    destQuery.value = place.place_name
  }
  // 항목 선택 시 리스트 숨김
  showResults.value = false
}

function setActive(kind) {
  activeField.value = kind
  // 포커스 시 직전 검색 결과가 있다면 리스트 노출
  showResults.value = (activeField.value === 'start' ? startResults.value.length : destResults.value.length) > 0
}

function searchActive() {
  searchPlaces(activeField.value)
}

function selectActive(place) {
  selectPlace(activeField.value, place)
}

function swapFromTo() {
  // 쿼리와 선택 결과를 맞교환
  const tq = startQuery.value; startQuery.value = destQuery.value; destQuery.value = tq
  const ts = selectedStart.value; selectedStart.value = selectedDest.value; selectedDest.value = ts
  // 결과 리스트는 활성 필드 기준 유지
}

function resetAll() {
  startQuery.value = ''
  destQuery.value = ''
  selectedStart.value = null
  selectedDest.value = null
  startResults.value = []
  destResults.value = []
  activeField.value = 'start'
  showResults.value = false
}

const canRoute = computed(() => !!(selectedStart.value && selectedDest.value))

async function requestRoute() {
  try {
    if (!canRoute.value) return
    const s = selectedStart.value
    const d = selectedDest.value
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

    const resp = await fetch('http://localhost:8080/api/route/pedestrian', {
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
    router.push({ name: 'geo-fencing' })
  } catch (e) {
    console.error('요청 오류:', e)
  }
}
</script>

<style scoped>
.page { padding: 16px; }
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 12px;
  position: relative;
}
.row.between { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.fromto { flex: 1; display: grid; gap: 10px; }
.input-item { border: 1px solid #e5e7eb; border-radius: 12px; padding: 10px 12px; background: #fafafa; }
.input-item.active { border-color: #6366f1; background: #f5f7ff; }
.label { font-size: 12px; color: #6b7280; margin-bottom: 6px; }
.input-item input { width: 100%; border: none; outline: none; background: transparent; font-size: 14px; }
.swap-fab { position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); width: 44px; height: 44px; border-radius: 50%; border: none; background: #6366f1; color: #fff; font-size: 18px; cursor: pointer; box-shadow: 0 6px 12px rgba(0,0,0,0.08); }
.hint { margin-top: 8px; font-size: 12px; color: #6b7280; }

.section-title { margin: 18px 2px 8px; font-size: 14px; color: #6b7280; }
.results.unified { list-style: none; padding: 0; margin: 0; }
.results.unified li { display: flex; justify-content: space-between; align-items: center; padding: 12px 8px; border-bottom: 1px solid #f1f5f9; cursor: pointer; }
.results.unified li:hover { background: #f8fafc; }
.item-left .name { font-weight: 600; color: #111827; }
.item-left .addr { font-size: 12px; color: #6b7280; margin-top: 2px; }
.item-right .arrive { border: 1px solid #e5e7eb; background: #fff; border-radius: 6px; padding: 6px 10px; cursor: pointer; }
.empty { color: #94a3b8; font-size: 13px; padding: 12px; }

.actions { display: grid; grid-template-columns: 1fr 1fr auto; gap: 10px; margin-top: 18px; }
.primary { background: #6366f1; color: #fff; border: none; border-radius: 12px; padding: 12px; cursor: pointer; }
.primary:disabled { background: #a5b4fc; cursor: not-allowed; }
.ghost { background: #fff; color: #4b5563; border: 1px solid #e5e7eb; border-radius: 12px; padding: 12px; cursor: pointer; }
.search { background: #111827; color: #fff; border: none; border-radius: 12px; padding: 12px 16px; cursor: pointer; }
</style>


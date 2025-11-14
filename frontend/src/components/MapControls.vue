<template>
  <!-- 지도 컨트롤 버튼들 -->
  <!-- 왼쪽 상단: 안심존 버튼들 -->
  <div class="map-controls-left" :class="{ desktop }">
    <!-- 안심존 활성화 버튼 (위로 이동) -->
    <button 
      class="map-btn-text" 
      @click="$emit('toggleSafeZone')"
      :disabled="!isPatientConnected">
      <i class="bi bi-power" :class="{ 'text-success': isSafeZoneEnabled, 'text-danger': !isSafeZoneEnabled }"></i>
      {{ isSafeZoneEnabled ? '안심존 활성화' : '안심존 비활성화' }}
    </button>
    
    <div class="safe-zone-control-wrapper">
      <!-- 안심존 범위 설정 버튼 (아래로 이동) -->
      <button 
        class="map-btn-text" 
        :class="{ 'active': isSafeZoneControlExpanded }"
        @click="$emit('toggleSafeZoneControl')"
        :disabled="!currentActiveZone || !isSafeZoneEnabled">
        {{ isSafeZoneControlExpanded ? '확인' : '안심존 범위 설정' }}
      </button>
      
      <!-- 확장 가능한 단계 선택 컨트롤러 -->
      <transition name="slide-fade">
        <div v-if="isSafeZoneControlExpanded" class="level-selector">
          <button 
            v-for="level in bufferLevels" 
            :key="level.value"
            class="level-btn"
            :class="{ 'active': selectedLevel === level.value }"
            @click="$emit('selectLevel', level.value)">
            <span class="level-number">{{ level.value }}</span>
            <span class="level-range">{{ level.desc }}</span>
          </button>
        </div>
      </transition>
    </div>
  </div>

  <!-- 현위치 버튼과 줌 버튼 그룹 (바텀시트와 연동) -->
  <div class="map-controls-location-group" :class="{ desktop }" :style="{ bottom: locationBtnBottom + 'px' }">
    <!-- 줌 버튼들 -->
    <div class="zoom-controls">
      <button class="map-btn-square" @click="$emit('zoomIn')">
        <i class="bi bi-plus-lg"></i>
      </button>
      <button class="map-btn-square" @click="$emit('zoomOut')">
        <i class="bi bi-dash-lg"></i>
      </button>
    </div>
    <!-- 현위치 버튼 -->
    <button class="map-btn-circle" @click="$emit('moveToPatientLocation')">
      <i class="bi bi-crosshair"></i>
    </button>
  </div>
</template>

<script setup>
// Props 정의
const props = defineProps({
  isPatientConnected: {
    type: Boolean,
    required: true
  },
  // 데스크탑 UI 스케일 업 여부
  desktop: {
    type: Boolean,
    default: false
  },
  isSafeZoneEnabled: {
    type: Boolean,
    required: true
  },
  isSafeZoneControlExpanded: {
    type: Boolean,
    required: true
  },
  currentActiveZone: {
    type: Object,
    default: null
  },
  selectedLevel: {
    type: Number,
    required: true
  },
  bufferLevels: {
    type: Array,
    required: true
  },
  locationBtnBottom: {
    type: Number,
    required: true
  }
})

// Emits 정의
const emit = defineEmits([
  'toggleSafeZone',
  'toggleSafeZoneControl', 
  'selectLevel',
  'zoomIn',
  'zoomOut',
  'moveToPatientLocation'
])
</script>

<style scoped>
/* ===== 지도 컨트롤 버튼들 ===== */
.map-controls-left {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 안심존 컨트롤 래퍼 */
.safe-zone-control-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 레벨 선택기 */
.level-selector {
  display: flex;
  gap: 6px;
  align-items: center;
  background: white;
  padding: 6px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.level-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 50px;
}

.level-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.level-btn.active {
  background: #eef2ff;
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

.level-number {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 2px;
}

.level-btn.active .level-number {
  color: #6366f1;
}

.level-range {
  font-size: 11px;
  color: #6b7280;
  font-weight: 500;
}

.level-btn.active .level-range {
  color: #4f46e5;
}

/* 슬라이드 페이드 애니메이션 */
.slide-fade-enter-active {
  transition: all 0.3s ease;
}

.slide-fade-leave-active {
  transition: all 0.2s ease;
}

.slide-fade-enter-from {
  transform: translateX(-10px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(-10px);
  opacity: 0;
}

.map-controls-location-group {
  position: fixed;
  right: 20px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  transition: bottom 0.2s ease;
}

/* 모바일 레이아웃에서는 모바일 너비 내부에 위치 */
body.mobile-mode .map-controls-location-group:not(.desktop) {
  right: calc(50% - 167.5px);
}

.zoom-controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 텍스트 버튼 (안심존 관련) */
.map-btn-text {
  padding: 8px 12px;
  border-radius: 6px;
  background: white;
  border: 1px solid #D1D5DB;
  font-size: 12px;
  font-weight: 600;
  color: #1F2937;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
  white-space: nowrap;
  min-width: 120px; /* 최소 너비 설정으로 버튼 크기 안정화 */
  text-align: center; /* 텍스트 중앙 정렬 */
}

.map-btn-text:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  transform: translateY(-1px);
}

.map-btn-text:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.map-btn-text.active {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
}

.map-btn-text:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f3f4f6;
  color: #9ca3af;
}

.map-btn-text:disabled:hover {
  transform: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 네모난 아이콘 버튼 (+, -) */
.map-btn-square {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
}

.map-btn-square:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  transform: translateY(-1px);
}

.map-btn-square:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.map-btn-square i {
  font-size: 20px;
  color: #1F2937;
}

/* 원형 버튼 (현위치) */
.map-btn-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
}

.map-btn-circle:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  transform: translateY(-1px);
}

.map-btn-circle:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.map-btn-circle i {
  font-size: 20px;
  color: #1F2937;
}

/* ===== 데스크탑 전용 확장 스타일 ===== */
.map-controls-left.desktop {
  gap: 12px;
}
.map-controls-left.desktop .map-btn-text {
  padding: 11px 15px;
  border-radius: 9px;
  font-size: 13px;
  min-width: 148px;
}
.map-controls-left.desktop .level-selector {
  gap: 9px;
  padding: 9px;
  border-radius: 11px;
  box-shadow: 0 6px 18px rgba(0,0,0,0.16);
}
.map-controls-left.desktop .level-btn {
  padding: 11px 13px;
  border-radius: 9px;
  min-width: 60px;
}
.map-controls-left.desktop .level-number {
  font-size: 15px;
}
.map-controls-left.desktop .level-range {
  font-size: 12px;
}

.map-controls-location-group.desktop {
  gap: 11px;
}
.map-controls-location-group.desktop .zoom-controls {
  gap: 9px;
}
.map-controls-location-group.desktop .map-btn-square {
  width: 50px;
  height: 50px;
  border-radius: 9px;
}
.map-controls-location-group.desktop .map-btn-square i {
  font-size: 21px;
}
.map-controls-location-group.desktop .map-btn-circle {
  width: 50px;
  height: 50px;
}
.map-controls-location-group.desktop .map-btn-circle i {
  font-size: 21px;
}
</style>

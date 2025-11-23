<template>
  <div class="desktop-report-container">
    <!-- 상단 탭 -->
    <div class="tab-header">
      <button
        class="tab-button"
        :class="{ active: activeTab === 'record' }"
        @click="activeTab = 'record'"
      >
        기록 입력
      </button>
      <button
        class="tab-button"
        :class="{ active: activeTab === 'report' }"
        @click="activeTab = 'report'"
      >
        AI 보고서
      </button>
    </div>

    <!-- 탭 컨텐츠 -->
    <div class="tab-content">
      <!-- 기록 입력 탭 -->
      <div v-if="activeTab === 'record'" class="record-section">
        <RecordComponent @saved="handleRecordSaved" />
      </div>

      <!-- AI 보고서 탭 -->
      <div v-if="activeTab === 'report'" class="report-section">
        <ReportComponent />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import RecordComponent from './DesktopReportRecord.vue'
import ReportComponent from './DesktopReportView.vue'

const activeTab = ref('record')

function handleRecordSaved() {
  // 기록 저장 후 AI 보고서 탭으로 전환
  activeTab.value = 'report'
}
</script>

<style scoped>
.desktop-report-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.tab-header {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 24px;
}

.tab-button {
  padding: 12px 24px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: -2px;
}

.tab-button:hover {
  color: #171717;
}

.tab-button.active {
  color: #657AE2;
  border-bottom-color: #657AE2;
  font-weight: 600;
}

.tab-content {
  min-height: 600px;
}

.record-section,
.report-section {
  width: 100%;
}
</style>






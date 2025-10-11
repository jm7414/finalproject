<!-- src/components/AnalysisMarker.vue -->
<template>
  <svg width="24" height="24" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect width="16" height="16" rx="8" fill="#A0A3B1" />
    <path
      d="M4 4.5H5V6H10V4.5H11V7H12V4.5C12 3.95 11.55 3.5 11 3.5H8.91C8.7 2.92 8.15 2.5 7.5 2.5C6.85 2.5 6.3 2.92 6.09 3.5H4C3.45 3.5 3 3.95 3 4.5V11.5C3 12.05 3.45 12.5 4 12.5H6.5V11.5H4V4.5ZM7.5 3.5C7.775 3.5 8 3.725 8 4C8 4.275 7.775 4.5 7.5 4.5C7.225 4.5 7 4.275 7 4C7 3.725 7.225 3.5 7.5 3.5Z"
      fill="white" />
    <path
      d="M11.65 11.45C11.85 11.1 12 10.7 12 10.25C12 9 11 8 9.75 8C8.5 8 7.5 9 7.5 10.25C7.5 11.5 8.5 12.5 9.75 12.5C10.2 12.5 10.6 12.35 10.95 12.15L12.3 13.5L13 12.8L11.65 11.45ZM9.75 11.5C9.05 11.5 8.5 10.95 8.5 10.25C8.5 9.55 9.05 9 9.75 9C10.45 9 11 9.55 11 10.25C11 10.95 10.45 11.5 9.75 11.5Z"
      fill="white" />
  </svg>
</template>

<script setup>
import { defineProps } from 'vue'

const props = defineProps({
  primary: { type: Boolean, default: false }
})
</script>

<style scoped>
.icon {
  height: 100%;
  width: auto;
  /* 가로는 비율 유지 */
  margin-right: 12px;
}
</style>

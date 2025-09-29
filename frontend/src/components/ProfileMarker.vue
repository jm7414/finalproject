<!-- src/components/ProfileMarker.vue -->
<template>
  <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path
      d="M24 0C37.2548 0 48 10.7452 48 24C48 37.2548 37.2548 48 24 48C10.7452 48 0 37.2548 0 24C0 10.7452 10.7452 0 24 0Z"
      fill="#DBEAFE" />
    <path
      d="M24 0C37.2548 0 48 10.7452 48 24C48 37.2548 37.2548 48 24 48C10.7452 48 0 37.2548 0 24C0 10.7452 10.7452 0 24 0Z"
      stroke="#E5E7EB" />
    <path d="M31.875 38H16.125V10H31.875V38Z" stroke="#E5E7EB" />
    <g clip-path="url(#clip0_3987_10518)">
      <path
        d="M24 25.25C25.1935 25.25 26.3381 24.7759 27.182 23.932C28.0259 23.0881 28.5 21.9435 28.5 20.75C28.5 19.5565 28.0259 18.4119 27.182 17.568C26.3381 16.7241 25.1935 16.25 24 16.25C22.8065 16.25 21.6619 16.7241 20.818 17.568C19.9741 18.4119 19.5 19.5565 19.5 20.75C19.5 21.9435 19.9741 23.0881 20.818 23.932C21.6619 24.7759 22.8065 25.25 24 25.25ZM22.3934 26.9375C18.9305 26.9375 16.125 29.743 16.125 33.2059C16.125 33.7824 16.5926 34.25 17.1691 34.25H30.8309C31.4074 34.25 31.875 33.7824 31.875 33.2059C31.875 29.743 29.0695 26.9375 25.6066 26.9375H22.3934Z"
        fill="#2563EB" />
    </g>
    <defs>
      <clipPath id="clip0_3987_10518">
        <path d="M16.125 16.25H31.875V34.25H16.125V16.25Z" fill="white" />
      </clipPath>
    </defs>
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

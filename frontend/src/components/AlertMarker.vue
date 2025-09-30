<!-- src/components/AlertMarker.vue -->
<template>
    <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="12" height="12" rx="6" fill="#A0A3B1" />
        <path
            d="M6.00002 9.75C6.46026 9.75 6.83335 9.3769 6.83335 8.91667C6.83335 8.45643 6.46026 8.08333 6.00002 8.08333C5.53978 8.08333 5.16669 8.45643 5.16669 8.91667C5.16669 9.3769 5.53978 9.75 6.00002 9.75Z"
            fill="white" />
        <path d="M5.16669 2.25H6.83335V7.25H5.16669V2.25Z" fill="white" />
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

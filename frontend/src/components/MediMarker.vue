<!-- src/components/MediMarker.vue -->
<template>
    <svg width="24" height="24" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="12" height="12" rx="6" fill="#A0A3B1" />
        <path
            d="M5.37498 7.25H4.33331V6H5.37498V4.95833H6.62498V6H7.66665V7.25H6.62498V8.29167H5.37498V7.25ZM8.91665 4.33333V8.91667C8.91665 9.375 8.54165 9.75 8.08331 9.75H3.91665C3.45831 9.75 3.08331 9.375 3.08331 8.91667V4.33333C3.08331 3.875 3.45831 3.5 3.91665 3.5H8.08331C8.54165 3.5 8.91665 3.875 8.91665 4.33333ZM8.08331 4.33333H3.91665V8.91667H8.08331V4.33333ZM8.49998 2.25H3.49998V3.08333H8.49998V2.25Z"
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

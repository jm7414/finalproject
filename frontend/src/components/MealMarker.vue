<!-- src/components/MealMarker.vue -->
<template>
    <svg width="24" height="24" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="12" height="12" rx="6" fill="#A0A3B1" />
        <path
            d="M4.24981 6.47012L5.42898 5.29095L2.50398 2.37012C1.85398 3.02012 1.85398 4.07429 2.50398 4.72845L4.24981 6.47012ZM7.07481 5.71595C7.71231 6.01179 8.60815 5.80345 9.27065 5.14095C10.0665 4.34512 10.2206 3.20345 9.60815 2.59095C8.99981 1.98262 7.85815 2.13262 7.05815 2.92845C6.39565 3.59095 6.18731 4.48679 6.48315 5.12429L2.41648 9.19095L3.00398 9.77845L5.87481 6.91595L8.74148 9.78262L9.32898 9.19512L6.46231 6.32845L7.07481 5.71595Z"
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

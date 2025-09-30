<!-- src/components/ActivityMarker.vue -->
<template>
    <svg width="24" height="24" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="12" height="12" rx="6" fill="#A0A3B1" />
        <path
            d="M6.46154 2.86047C6.96923 2.86047 7.38462 2.44186 7.38462 1.93023C7.38462 1.4186 6.96923 1 6.46154 1C5.95385 1 5.53846 1.4186 5.53846 1.93023C5.53846 2.44186 5.95385 2.86047 6.46154 2.86047ZM4.75385 4.44186L3.46154 11H4.43077L5.26154 7.27907L6.23077 8.2093V11H7.15385V7.51163L6.18462 6.5814L6.46154 5.18605C7.06154 5.88372 7.98462 6.34884 9 6.34884V5.4186C8.12308 5.4186 7.38462 4.95349 7.01538 4.30233L6.55385 3.55814C6.29538 3.14419 5.77846 2.97674 5.33077 3.16744L3 4.16279V6.34884H3.92308V4.76744L4.75385 4.44186Z"
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

<!-- src/components/AlertIconMarker.vue -->
<template>
  <svg
    class="icon"
    width="44"
    height="45"
    viewBox="0 0 111 114"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <g filter="url(#filter0)">
      <path
        fill-rule="evenodd"
        clip-rule="evenodd"
        d="M16 27C16 18.7157 22.7157 12 31 12H80C88.2843 12 95 18.7157 95 27V79C95 87.2843 88.2843 94 80 94H31C22.7157 94 16 87.2843 16 79V27Z"
        :fill="primary ? '#FF5959' : '#F0F0F0'"
        :fill-opacity="primary ? '0.83' : '1'"
      />
      <path
        fill-rule="evenodd"
        clip-rule="evenodd"
        d="M16 27C16 18.7157 22.7157 12 31 12H80C88.2843 12 95 18.7157 95 27V79C95 87.2843 88.2843 94 80 94H31C22.7157 94 16 87.2843 16 79V27Z"
        :stroke="primary ? '#808AFF' : '#D0D0D0'"
      />
    </g>
    <path
      d="M56 45V53M56 61H56.02M47.72 33H64.28L76 44.72V61.28L64.28 73H47.72L36 61.28V44.72L47.72 33Z"
      :stroke="primary ? '#FEE9E7' : '#A0A0A0'"
      stroke-width="4"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <defs>
      <filter id="filter0" x="0.5" y="0.5" width="110" height="113" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
        <feFlood flood-opacity="0" result="BackgroundImageFix"/>
        <feColorMatrix in="SourceAlpha" type="matrix"
          values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
        <feOffset dy="4"/>
        <feGaussianBlur stdDeviation="7.5"/>
        <feColorMatrix type="matrix"
          values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.15 0"/>
        <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow"/>
        <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow" result="shape"/>
      </filter>
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
  width: auto;         /* 가로는 비율 유지 */
  margin-right: 12px;
}
</style>

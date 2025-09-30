<template>
  <div class="img">
    <div class="text">
      <h1>지원금 안내</h1>
    </div>
    <img src="@/assets/support1.png"></img>
    <img src="@/assets/support2.png"></img>
    <img src="@/assets/support3.png"></img>
    <img src="@/assets/support4.png"></img>
  </div>
  <button @click="goTotalSupport()">돌아가기</button>
  </template>

<script setup>
import {useRouter} from 'vue-router'
const router = useRouter()

function goTotalSupport() {
  router.push('/total-support')
}
</script>

<style scoped>
.img {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: auto;
}

.img .text {
  display: flex;
  justify-content: center;
  background-color: lightskyblue;
  font-size: 14px;
  font-weight: bold;
  margin-top: 40px;
  margin-bottom: 30px;
}

</style>
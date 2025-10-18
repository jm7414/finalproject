<template>
  <div class="main-container">
    <div class="div">
      <div class="main">
        <div class="page-title">
          <div class="div-1">
            <div class="div-2">
              <div class="i">
                <div class="svg">
                  <div class="frame"></div>
                </div>
              </div>
            </div>
            <span class="dementia-support-info">치매 환자 지원금 안내</span>
          </div>
          <span class="government-support-benefits">정부에서 제공하는 다양한 지원 혜택을 확인하세요</span>
        </div>
        <div class="option-button">
          <div class="div-3">
            <button class="button" :class="{ 'active': selectedType === 'total' }" @click="type('total')">
              <span class="full-stack" :class="{ 'active': selectedType === 'total' }">전체</span></button>
            <button class="button-4" :class="{ 'active': selectedType === 'medi' }" @click="type('medi')">
              <span class="medical-cost" :class="{ 'active': selectedType === 'medi' }">의료비</span></button>
            <button class="button-5" :class="{ 'active': selectedType === 'living' }" @click="type('living')">
              <span class="living-cost" :class="{ 'active': selectedType === 'living' }">생활비</span></button>
            <button class="button-6" :class="{ 'active': selectedType === 'care' }" @click="type('care')">
              <span class="care-cost" :class="{ 'active': selectedType === 'care' }">간병비</span></button>
          </div>
        </div>

        <!-- 이 부분이 이후에 DB에 지원금 테이블 만들어서 가져와야 쓸 수 있을듯함 -->
        <!-- 아니면 하드코딩으로 script에 쭉 써놔서 불러오는 방법도 있음 -->
        <div class="content"  @click="goPage('#')">
          <div class="div-7">
            <div class="div-8">
              <span class="jang-gi-yo-yang">장기요양급여</span><span class="weol-chwae-da-man-won">월 최대 150만원</span>
              <div class="div-9">
                <div class="span">
                  <span class="medical-expense">의료비</span>
                </div>
              </div>
            </div>
              <div class="i-a">
                <div class="svg-b">
                  <div class="frame-c"></div>
                </div>
              </div>
          </div>
          <div class="div-d"></div>
          <span class="dementia-patients">65세 이상 치매 환자 대상</span>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import "./Benefit.css";

import {ref} from 'vue'
import {useRouter} from 'vue-router'
const router = useRouter()

const selectedType = ref('')

// 선택되는 옵션 클릭될때마다 바뀌게 해주는 함수
function type(s) {
  selectedType.value = s
}

function goPage(s) {
  console.log(`이동 ${s}`)
  router.push(s)
}

</script>


<style scoped>
.bd {
  border: dashed 2px violet;
}

.button.active,
.button-4.active,
.button-5.active,
.button-6.active {
  background: rgba(74, 98, 221, 0.85) !important;
  border-color: rgba(74, 98, 221, 0.85);
}

.full-stack.active,
.medical-cost.active,
.living-cost.active,
.care-cost.active {
    color: #FFFFFF !important;
}

</style>
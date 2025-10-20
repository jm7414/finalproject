<template>

    <div class="div">
      <div class="page-title">
        <div class="title-content">
          <div class="icon-wrapper">
            <div class="icon" style="right: 3px;">
              <svg width="27" height="24" viewBox="0 0 27 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M27 24H0V0H27V24Z" />
                <g clip-path="url(#clip0_145_3669)">
                  <path
                    d="M14.625 1.125V1.61719C14.925 1.67344 15.2156 1.74375 15.4781 1.81406C16.0781 1.97344 16.4344 2.59219 16.275 3.19219C16.1156 3.79219 15.4969 4.14844 14.8969 3.98906C14.3859 3.85312 13.9078 3.75937 13.4813 3.75469C13.1391 3.75 12.7922 3.83437 12.5719 3.96094C12.4734 4.02187 12.4266 4.07344 12.4078 4.10156C12.3938 4.125 12.375 4.15781 12.375 4.23281C12.375 4.24688 12.375 4.25625 12.375 4.26094C12.3844 4.27031 12.4172 4.31719 12.5297 4.38281C12.8016 4.54688 13.2047 4.67344 13.8141 4.85625L13.8563 4.87031C14.3766 5.025 15.0703 5.23594 15.6328 5.5875C16.275 5.99062 16.8562 6.66094 16.8703 7.69219C16.8844 8.74688 16.3359 9.51562 15.6188 9.96563C15.3047 10.1578 14.9672 10.2937 14.6203 10.3781V10.875C14.6203 11.4984 14.1187 12 13.4953 12C12.8719 12 12.3703 11.4984 12.3703 10.875V10.3406C11.925 10.2328 11.5172 10.0922 11.1703 9.975C11.0719 9.94219 10.9781 9.90938 10.8891 9.88125C10.2984 9.68437 9.97969 9.04688 10.1766 8.45625C10.3734 7.86562 11.0109 7.54688 11.6016 7.74375C11.7234 7.78594 11.8359 7.82344 11.9438 7.86094C12.5813 8.07656 13.0406 8.23125 13.5328 8.25C13.9078 8.26406 14.2406 8.175 14.4328 8.05781C14.5219 8.00156 14.5641 7.95469 14.5828 7.92188C14.6016 7.89375 14.625 7.8375 14.6203 7.72969V7.72031C14.6203 7.67344 14.6203 7.62188 14.4328 7.50469C14.1656 7.33594 13.7625 7.20469 13.1625 7.02188L13.0734 6.99375C12.5672 6.84375 11.9016 6.64219 11.3672 6.31875C10.7344 5.93906 10.125 5.2875 10.1203 4.25156C10.1156 3.17812 10.725 2.44219 11.4188 2.02969C11.7188 1.85156 12.0422 1.72969 12.3656 1.64531V1.125C12.3656 0.501562 12.8672 0 13.4906 0C14.1141 0 14.6156 0.501562 14.6156 1.125H14.625ZM26.6344 15.7641C27.2484 16.5984 27.0703 17.7703 26.2359 18.3844L20.3016 22.7578C19.2047 23.5641 17.8828 24 16.5187 24H9H1.5C0.670312 24 0 23.3297 0 22.5V19.5C0 18.6703 0.670312 18 1.5 18H3.225L5.32969 16.3125C6.39375 15.4594 7.71562 15 9.07969 15H12.75H13.5H16.5C17.3297 15 18 15.6703 18 16.5C18 17.3297 17.3297 18 16.5 18H13.5H12.75C12.3375 18 12 18.3375 12 18.75C12 19.1625 12.3375 19.5 12.75 19.5H18.4031L24.0141 15.3656C24.8484 14.7516 26.0203 14.9297 26.6344 15.7641ZM9.075 18H9.03281C9.04688 18 9.06094 18 9.075 18Z"
                    fill="white" />
                </g>
              </svg>
            </div>
          </div>
          <span class="dementia-support-info">치매 환자 지원금 안내</span>
          <span class="government-support-benefits">정부에서 제공하는 다양한 지원 혜택을 확인하세요</span>
        </div>
      </div>

      <div class="option-button">
        <div class="button-group">
          <button class="filter-button" :class="{ 'active': selectedType === 'total' }" @click="type('total')">
            <span>전체</span>
          </button>
          <button class="filter-button" :class="{ 'active': selectedType === 'medi' }" @click="type('medi')">
            <span>의료비</span>
          </button>
          <button class="filter-button" :class="{ 'active': selectedType === 'living' }" @click="type('living')">
            <span>생활비</span>
          </button>
          <button class="filter-button" :class="{ 'active': selectedType === 'care' }" @click="type('care')">
            <span>간병비</span>
          </button>
        </div>
      </div>

      <!-- 이 부분이 이후에 DB에 지원금 테이블 만들어서 가져와야 쓸 수 있을듯함 -->
      <!-- 아니면 하드코딩으로 script에 쭉 써놔서 불러오는 방법도 있음 -->
      <div class="content" @click="goPage('#')">
        <div class="content-header">
          <div class="content-info">
            <h3 class="content-title">장기요양급여</h3>
            <p class="content-amount">월 최대 150만원</p>
            <div class="tag-wrapper">
              <span class="tag">의료비</span>
            </div>
          </div>
          <div class="arrow-icon">
            <svg width="10" height="16" viewBox="0 0 10 16" fill="none">
              <path d="M2 2L8 8L2 14" stroke="#A3A3A3" stroke-width="2" stroke-linecap="round"
                stroke-linejoin="round" />
            </svg>
          </div>
        </div>
        <p class="content-description">65세 이상 치매 환자 대상</p>
      </div>


      <div class="content" @click="goPage('#')">
        <div class="content-header">
          <div class="content-info">
            <h3 class="content-title">장기요양급여</h3>
            <p class="content-amount">월 최대 150만원</p>
            <div class="tag-wrapper">
              <span class="tag">의료비</span>
            </div>
          </div>
          <div class="arrow-icon">
            <svg width="10" height="16" viewBox="0 0 10 16" fill="none">
              <path d="M2 2L8 8L2 14" stroke="#A3A3A3" stroke-width="2" stroke-linecap="round"
                stroke-linejoin="round" />
            </svg>
          </div>
        </div>
        <p class="content-description">65세 이상 치매 환자 대상</p>
      </div>


      <div class="content" @click="goPage('#')">
        <div class="content-header">
          <div class="content-info">
            <h3 class="content-title">장기요양급여</h3>
            <p class="content-amount">월 최대 150만원</p>
            <div class="tag-wrapper">
              <span class="tag">의료비</span>
            </div>
          </div>
          <div class="arrow-icon">
            <svg width="10" height="16" viewBox="0 0 10 16" fill="none">
              <path d="M2 2L8 8L2 14" stroke="#A3A3A3" stroke-width="2" stroke-linecap="round"
                stroke-linejoin="round" />
            </svg>
          </div>
        </div>
        <p class="content-description">65세 이상 치매 환자 대상</p>
      </div>

    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()

const selectedType = ref('total')

// 선택되는 옵션 클릭될때마다 바뀌게 해주는 함수
function type(s) {
  selectedType.value = s
}

function goPage(s) {
  console.log(`이동 ${s}`)
  alert(`이동`)
}
</script>

<style scoped>
.div {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  top: 0;
  margin-top: 75px;
  padding-top: 0;
  overflow-y: auto;
}



/* 페이지 타이틀 영역 */
.page-title {
  position: relative;
  width: 100%;
  background: #fafafa;
  display: flex;
  justify-content: center;
  padding-top: 0;
}

.title-content {
  width: 100%;
  max-width: 343px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 5px;
}

.icon-wrapper {
  width: 72px;
  height: 72px;
  background: rgba(170, 193, 253, 0.91);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dementia-support-info {
  color: #171717;
  font-family: Inter, var(--default-font-family);
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
  text-align: center;
}

.government-support-benefits {
  color: #525252;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  text-align: center;
}

/* 옵션 버튼 영역 */
.option-button {
  position: relative;
  width: 100%;
  min-height: 50px;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f5f5f5;
  display: flex;
  justify-content: center;
  overflow-x: auto;
  /* 작은 화면에서 스크롤 가능 */
}

.button-group {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 343px;
  flex-wrap: nowrap;
  /* 줄바꿈 방지 - 이게 핵심! */
}

.filter-button {
  background: #f5f5f5;
  border: 1px solid #d4d4d4;
  border-radius: 9999px;
  padding: 8px 12px;
  /* 좌우 패딩 줄임 */
  cursor: pointer;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  white-space: nowrap;
  color: #404040;
  flex-shrink: 0;
  /* 버튼이 줄어들지 않도록 */
  min-width: fit-content;
  /* 내용에 맞는 최소 너비 */
}

.filter-button:hover {
  background: #e5e5e5;
}

.filter-button.active {
  background: rgba(74, 98, 221, 0.85);
  border-color: rgba(74, 98, 221, 0.85);
  color: #fff;
}

.filter-button span {
  color: inherit;
}

/* 컨텐츠 카드 */
.content {
  width: 95%;
  height: 145px;
  max-width: 343px;
  margin: 16px auto;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  box-sizing: border-box;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: all 0.2s ease;
  margin-top: 8px;
  gap:10px;
}

.content:hover {
  border-color: #d4d4d4;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.content-info {
  flex: 1;
  width: 150px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.content-title {
  margin: 0;
  color: #171717;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.content-amount {
  margin: 0;
  color: #525252;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
}

.tag-wrapper {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.tag {
  padding: 4px 1px;
  background: #f5f5f5;
  border-radius: 4px;
  color: #262626;
  font-size: 12px;
  font-weight: 400;
  line-height: 16px;
}

.arrow-icon {
  width: 130px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: end;
  flex-shrink: 0;
}

.content-description {
  margin: 0;
  color: #737373;
  font-size: 12px;
  font-weight: 400;
  line-height: 16px;
}

/* 반응형 디자인 */
@media (min-width: 376px) {
  .main-container {
    max-width: 100%;
  }

  .page-title,
  .option-button {
    padding-left: 24px;
    padding-right: 24px;
  }

  .content {
    max-width: 600px;
  }
}
</style>

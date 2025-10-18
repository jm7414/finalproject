<template>
  <div class="main-container">
    <div class="body">
      <button class="check-my-btn" @click="check_my()">신용등급 확인</button>
      <div class="div">
        <div class="user-info">
          <div class="div-1">
            <div class="user-detail">
              <div class="img">
                <div class="frame"></div>
              </div>
              <div class="flex-column-eae">
                <div class="div-2"><span class="user-name">김○○님</span></div>
                <span class="caregiver">치매환자 보호자</span>
              </div>
            </div>
            <div class="credit-grade">
              <div class="div-3">
                <span class="unknown-grade">?등급</span>
                <span class="credit-grade-4">신용등급</span>
              </div>
              <div class="div-5">
                <span class="loan-limit">?,???만원</span>
                <span class="loan-info">대출 가능 한도</span>
              </div>
            </div>
          </div>
        </div>


        <div class="available-loans">
          <span class="loan-amount">이용 가능한 대출</span>




          <div class="div-19">
            <div class="flex-row">
              <div class="div-1a">
                <div class="div-1b">
                  <div class="div-1c">
                    <span class="kb-credit-loan">KB국민은행 신용대출</span>
                  </div>
                  <div class="div-1d">
                    <div class="i">
                      <div class="svg-1e">
                        <div class="frame-1f"></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <span class="kb-bank">KB국민은행</span>
            </div>
            <div class="flex-row-f">
              <div class="div-20">
                <div class="flex-row-dae">
                  <div class="div-21"></div>
                  <span class="interest-rate">금리</span><span class="annual-rate">연 4.8%~8.5%</span>
                </div>
                <div class="flex-row-aa">
                  <div class="div-22"></div>
                  <span class="limit">한도</span><span class="max-amount">최대 5,000만원</span>
                </div>
                <div class="div-23"></div>
              </div>
              <span class="duration">기간</span><span class="max-years">최대 7년</span>
            </div>
            <button class="button-24" @click="check_my"> <span class="details"></span>상세보기</button>
          </div>



          <div class="div-19">
            <div class="flex-row">
              <div class="div-1a">
                <div class="div-1b">
                  <div class="div-1c">
                    <span class="kb-credit-loan">KB국민은행 신용대출</span>
                  </div>
                  <div class="div-1d">
                    <div class="i">
                      <div class="svg-1e">
                        <div class="frame-1f"></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <span class="kb-bank">KB국민은행</span>
            </div>
            <div class="flex-row-f">
              <div class="div-20">
                <div class="flex-row-dae">
                  <div class="div-21"></div>
                  <span class="interest-rate">금리</span><span class="annual-rate">연 4.8%~8.5%</span>
                </div>
                <div class="flex-row-aa">
                  <div class="div-22"></div>
                  <span class="limit">한도</span><span class="max-amount">최대 5,000만원</span>
                </div>
                <div class="div-23"></div>
              </div>
              <span class="duration">기간</span><span class="max-years">최대 3년</span>
            </div>
            <button class="button-24" @click="check_my"> <span class="details"></span>상세보기</button>
          </div>





          <div class="caution">
            <span class="loan-tips">대출 이용 팁</span>
            <div class="div-38">
              <div class="div-39">
                <div class="div-3a">
                  <span class="excessive-loan-warning">과도한 대출 주의</span>
                </div>
                <div class="i-3b">
                  <div class="svg-3c">
                    <div class="frame-3d"></div>
                  </div>
                </div>
              </div>
              <span class="repayment-consideration">상환 능력을 고려하여 적정 수준의 대출을 이용하세 요.</span>
            </div>
          </div>
        </div>
      </div>
      <div class="main"></div>
    </div>
  </div>
</template>

<script setup>
import "./Loan.css";

function check_my() {
  alert(`토스 또는 카카오 신용등급 확인하는 페이지로 이동`)
}

</script>

<style scoped>
</style>

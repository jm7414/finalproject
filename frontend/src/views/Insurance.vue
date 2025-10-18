<template>
  <div class="main-container">
    <div class="main-div">
      <div class="main-div-1">
        <div class="user-info" @click="goMypage">
          <div class="main-div-2">
            <div class="image"><div class="frame"></div></div>
            <div class="flex-column">
              <div class="main-div-3">
                <span class="user-name">김○○님</span>
              </div>
              <span class="patient-info">환자: 김△△</span>
            </div>
            <div class="view-my-info">
              <div class="svg"><div class="frame-4"></div></div>
            </div>
          </div>
        </div>
        <div class="section">
          <span class="span">보험금 현황</span>
          <div class="div">
            <div class="div-5">
              <div class="right"><span class="span-6" @click="goMediIns">신청가능</span></div>
              <div class="div-7">
                <span class="span-8">의료보험</span>
                <div class="div-9">
                  <div class="div-a"><div class="div-b"></div></div>
                </div>
              </div>
            </div>
            <div class="flex-row-bfd">
              <div class="div-c">
                <div class="div-d">
                  <span class="monthly-support">월 지원금</span>
                </div>
                <div class="total-received-amount">
                  <span class="accumulated-received-amount">누적 수령액</span>
                </div>
              </div>
              <span class="currency-450000">₩450,000</span
              ><span class="currency-2700000">₩2,700,000</span>
            </div>
          </div>
          <div class="life-insurance">
            <div class="title">
              <div class="right-e">
                <span class="apply-possible" @click="goLifeIns">신청가능</span>
              </div>
              <div class="left">
                <span class="life-insurance-f">생활보험</span>
                <div class="i">
                  <div class="svg-10"><div class="frame-11"></div></div>
                </div>
              </div>
            </div>
            <div class="flex-row">
              <div class="content">
                <div class="monthly-support-12">
                  <span class="monthly-support-13">월 지원금</span>
                </div>
                <div class="total-received">
                  <span class="total-received-amount-14">누적 수령액</span>
                </div>
              </div>
              <span class="currency">₩450,000</span
              ><span class="total-amount">₩2,700,000</span>
            </div>
          </div>
        </div>
        <div class="help">
          <div class="help-subtitle">
            <span class="need-help">도움이 필요하신가요?</span>
            <div class="i-15">
              <div class="svg-16"><div class="frame-17"></div></div>
            </div>
          </div>
          <div class="help-detail">
            <span class="insurance-inquiry"
              >보험금 신청 관련 문의사항이 있다면 연락주세요</span
            >
          </div>
          <div class="help-btn">
            <div class="chat-btn" @click="check">
              <span class="chat-consultation">채팅상담</span>
              <div class="i-18">
                <div class="svg-19"><div class="frame-1a"></div></div>
              </div>
            </div>
            <div class="call-btn" @click="check">
              <span class="phone-consultation">전화상담</span>
              <div class="i-1b">
                <div class="svg-1c"><div class="frame-1d"></div></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import "./Insurance.css";

function goMypage() {
    alert(`마이페이지 이동버튼`)
}

function check() {
    alert(`전화 or 채팅`)
}

function goMediIns() {
    alert(`다른 의료보험 페이지 이동`)
}

function goLifeIns() {
    alert(`다른 생활보험 페이지 이동`)
}

</script>

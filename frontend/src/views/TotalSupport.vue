<template>
  <div class="dashboard">
    <!-- 상단 환영 카드 -->
    <div class="top-card">
      <div class="profile-section">
        <ProfileMarker class="profile-icon" />
        <div class="greeting-text">
          <div class="greeting-title">User 님 안녕하세요!</div>
          <div class="greeting-subtitle">필요한 지원을 찾아보세요</div>
        </div>
      </div>
    </div>

    <!-- 요약 버튼 그룹 -->
    <div class="summary-buttons">
      <!-- 지원금 -->
      <div class="summary-btn receive-money">
        <div class="summary-left">
          <div class="summary-label">받을 수 있는 지원금</div>
          <h1 class="summary-count">0건</h1>
        </div>
        <div class="summary-right">
          <button class="summary-action">확인하기 →</button>
        </div>
      </div>

      <!-- 대출 -->
      <div class="summary-btn receive-loan">
        <div class="summary-left">
          <div class="summary-label">가능한 대출</div>
          <h1 class="summary-count">0건</h1>
        </div>
        <div class="summary-right">
          <button class="summary-action">확인하기 →</button>
        </div>
      </div>
    </div>

    <!-- 기능 카드 그리드 -->
    <div class="function-grid">
      <div class="func-card">
        <MoneyMarker class="func-icon" />
        <div class="func-title">지원금</div>
        <div class="func-desc">정부 및 지자체 지원금 안내</div>
        <button class="func-link" @click="goBenefit">확인하기 →</button>
      </div>
      <div class="func-card">
        <BohumMarker class="func-icon" />
        <div class="func-title">보험금</div>
        <div class="func-desc">의료보험 및 생활보험 혜택</div>
        <button class="func-link" @click="goLoan">확인하기 →</button>
      </div>
      <div class="func-card">
        <SangDamMarker class="func-icon" />
        <div class="func-title">상담소</div>
        <div class="func-desc">전문가 상담 및 심리 지원</div>
        <button class="func-link" @click="goHeartCare">상담받기 →</button>
      </div>
      <div class="func-card">
        <HospitalMarker class="func-icon" />
        <div class="func-title">병원위치</div>
        <div class="func-desc">근처 병원 및 의료시설 찾기</div>
        <button class="func-link" @click="goHospitalCare">찾아보기 →</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import MoneyMarker from '@/components/MoneyMarker.vue' 
import BohumMarker from '@/components/BohumMarker.vue' 
import SangDamMarker from '@/components/SangDamMarker.vue' 
import HospitalMarker from '@/components/HospitalMarker.vue'
import ProfileMarker from '@/components/ProfileMarker.vue' 

const router = useRouter()

function goBenefit() {
  router.push('/benefit')
}
function goLoan() {
  router.push('/loan')
}
function goHeartCare() {
  router.push('/heartCare')
}
function goHospitalCare() {
  router.push('/hospitalCare')
}
</script>

<style scoped>
.dashboard {
  max-width: 500px;
  margin: 0 auto;
  margin-top: 35px;
  padding: 16px;
  background: #f7f8fa;
}

/* 상단 카드 */
.top-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  margin-bottom: 16px;
}
.profile-section {
  display: flex;
  align-items: center;
  gap: 12px;
}
.profile-icon {
  width: 40px;
  height: 40px;
}
.greeting-text {
  display: flex;
  flex-direction: column;
}
.greeting-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.greeting-subtitle {
  font-size: 14px;
  color: #666;
}

/* 요약 버튼 그룹 */
.summary-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.summary-btn {
  flex: 1;
  background: #a78bfa;
  color: #fff;
  border-radius: 12px;
  padding: 16px;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.summary-btn.receive-loan {
  background: #6366f1;
}

/* summary left/top */
.summary-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.summary-label {
  font-size: 15px;
  font-weight: 500;
}
.summary-count {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}

/* summary right/bottom */
.summary-right {
  display: flex;
  justify-content: flex-end;
}

/* summary action */
.summary-action {
  background: rgba(255,255,255,0.3);
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}
.summary-action:hover {
  background: rgba(255,255,255,0.5);
}

/* 기능 카드 그리드 */
.function-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.func-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 230px;
}
.func-icon {
  width: 60px;
  height: 60px;
  margin-bottom: 4px;
}
.func-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}
.func-desc {
  font-size: 14px;
  color: #666;
}
.func-link {
  align-self: flex-end;
  background: rgba(59,130,246,0.1);
  border: none;
  color: #3b82f6;
  font-size: 16px;
  font-weight: 500;
  border-radius: 5%;
  cursor: pointer;
}
</style>

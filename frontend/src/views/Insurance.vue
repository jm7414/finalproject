<template>
  <div class="ins-container">
    <!-- 프로필 영역 -->
    <div class="ins-profile" @click="goMypage">
      <div class="ins-user">
        <div class="ins-avatar">
          <div class="ins-frame">
            <img />
          </div>
        </div>
        <div class="ins-info">
          <span class="ins-name">김○○님</span>
          <span class="ins-caregiver">일지매님의 보호자</span>
        </div>
        <div class="ins-arrow">
          <svg width="10" height="16" viewBox="0 0 10 16" fill="none">
            <path d="M2 2L8 8L2 14" stroke="#A3A3A3" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
      </div>
    </div>

    <!-- 보험금 현황 -->
    <section class="ins-section">
      <h2 class="ins-title">보험금 현황</h2>

      <div class="ins-card">
        <div class="ins-card-header">
          <div class="ins-card-info">
            <svg width="20" height="16" viewBox="0 0 20 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M6 1.5C6 0.671875 6.67188 0 7.5 0H12.5C13.3281 0 14 0.671875 14 1.5V16H11.5V13.5C11.5 12.6719 10.8281 12 10 12C9.17188 12 8.5 12.6719 8.5 13.5V16H6V1.5ZM1.5 3H5V16H1.5C0.671875 16 0 15.3281 0 14.5V10H2.5C2.775 10 3 9.775 3 9.5C3 9.225 2.775 9 2.5 9H0V7H2.5C2.775 7 3 6.775 3 6.5C3 6.225 2.775 6 2.5 6H0V4.5C0 3.67188 0.671875 3 1.5 3ZM18.5 3C19.3281 3 20 3.67188 20 4.5V6H17.5C17.225 6 17 6.225 17 6.5C17 6.775 17.225 7 17.5 7H20V9H17.5C17.225 9 17 9.225 17 9.5C17 9.775 17.225 10 17.5 10H20V14.5C20 15.3281 19.3281 16 18.5 16H15V3H18.5ZM9.75 2C9.475 2 9.25 2.225 9.25 2.5V3.25H8.5C8.225 3.25 8 3.475 8 3.75V4.25C8 4.525 8.225 4.75 8.5 4.75H9.25V5.5C9.25 5.775 9.475 6 9.75 6H10.25C10.525 6 10.75 5.775 10.75 5.5V4.75H11.5C11.775 4.75 12 4.525 12 4.25V3.75C12 3.475 11.775 3.25 11.5 3.25H10.75V2.5C10.75 2.225 10.525 2 10.25 2H9.75Z"
                fill="#525252" />
            </svg>
            <span class="ins-type">의료보험</span>
          </div>
          <span class="ins-label-apply" @click="goMediIns">신청가능</span>
        </div>
        <div class="ins-card-body">
          <div class="ins-stats">
            <div class="d-flex justify-content-between">
              <span class="ins-sub">월 지원금</span>
              <span class="ins-val">₩450,000</span>
            </div>
            <div class="d-flex justify-content-between">
              <span class="ins-sub">누적 수령액</span>
              <span class="ins-val">₩2,700,000</span>
            </div>
          </div>
        </div>
      </div>

      <div class="ins-card">
        <div class="ins-card-header">
          <div class="ins-card-info">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M16 16H0V0H16V16Z" stroke="#E5E7EB" />
              <g clip-path="url(#clip0_165_13990)">
                <path
                  d="M8.00001 0C8.14376 0 8.28751 0.03125 8.41876 0.090625L14.3031 2.5875C14.9906 2.87813 15.5031 3.55625 15.5 4.375C15.4844 7.475 14.2094 13.1469 8.82501 15.725C8.30314 15.975 7.69689 15.975 7.17501 15.725C1.79064 13.1469 0.515639 7.475 0.500014 4.375C0.496889 3.55625 1.00939 2.87813 1.69689 2.5875L7.58439 0.090625C7.71251 0.03125 7.85626 0 8.00001 0ZM8.00001 2.0875V13.9C12.3125 11.8125 13.4719 7.19062 13.5 4.41875L8.00001 2.0875Z"
                  fill="#525252" />
              </g>
              <defs>
                <clipPath id="clip0_165_13990">
                  <path d="M0 0H16V16H0V0Z" fill="white" />
                </clipPath>
              </defs>
            </svg>

            <span class="ins-type">생활보험</span>
          </div>
          <span class="ins-label-apply" @click="goLifeIns">신청가능</span>
        </div>
        <div class="ins-card-body">
          <div class="ins-stats">
            <div class="d-flex justify-content-between">
              <span class="ins-sub">월 지원금</span>
              <span class="ins-val">₩450,000</span>
            </div>
            <div class="d-flex justify-content-between">
              <span class="ins-sub">누적 수령액</span>
              <span class="ins-val">₩2,700,000</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 도움 -->
    <section class="ins-help">
      <h3 class="help-title"><i class="bi bi-question-circle-fill" style="color: #525252;"></i> 도움이 필요하신가요?</h3>
      <p class="help-text">보험금 신청 관련 문의사항이 있다면 연락주세요</p>
      <div class="help-actions">
        <button class="help-btn call" @click="check"><i class="bi bi-telephone-fill"></i> 전화상담</button>
        <button class="help-btn chat" @click="check"><i class="bi bi-chat-fill"></i> 채팅상담</button>
      </div>
    </section>
  </div>
</template>

<script setup>

function goMypage() { alert(`마이페이지 이동버튼`); }
function goMediIns() { alert(`다른 의료보험 페이지 이동`); }
function goLifeIns() { alert(`다른 생활보험 페이지 이동`); }
function check() { alert(`전화 or 채팅`); }
</script>

<style scoped>
.ins-container {
  width: 100%;
  height: 100%;
  padding: 16px;
  box-sizing: border-box;
  margin-top: -15px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 프로필 */
.ins-profile {
  border-bottom: 1px solid #e5e5e5;
  padding-bottom: 16px;
}

.ins-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ins-avatar .ins-frame {
  width: 48px;
  height: 48px;
  background: #ddd;
  border-radius: 50%;
  overflow: hidden;
}

.ins-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ins-name {
  font-size: 16px;
  font-weight: bold;
}

.ins-caregiver {
  font-size: 14px;
  color: #666;
}

.ins-arrow {
  flex-shrink: 0;
}

/* 보험금 현황 섹션 */
.ins-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ins-title {
  font-size: 18px;
  font-weight: 600;
}

.ins-card {
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.ins-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.ins-label-apply {
  background: rgba(170, 194, 254, 0.91);
  color: #525252;
  border-radius: 5px;
  padding: 4px 12px;
  font-size: 14px;
  cursor: pointer;
}

.ins-card-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ins-type {
  font-size: 16px;
  font-weight: 500;
}

.ins-icon-med .ins-icon-frame,
.ins-icon-life .ins-icon-frame {
  width: 32px;
  height: 32px;
  background: #ccc;
  border-radius: 4px;
}

.ins-card-body {
  padding: 12px 16px;
}

.ins-stats {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
}

.ins-sub {
  font-size: 14px;
  color: #999;
}

.ins-val {
  font-size: 16px;
  font-weight: bold;
  margin-top: 4px;
}

/* 도움 섹션 */
.ins-help {
  background: #fafafa;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.help-title {
  font-size: 16px;
  font-weight: 600;
}

.help-text {
  font-size: 14px;
  color: #666;
}

.help-actions {
  display: flex;
  gap: 12px;
}

.help-btn {
  flex: 1;
  padding: 8px 0;
  font-size: 14px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.help-btn.call {
  background: rgba(74, 98, 221, 0.85);
  color: #fff;
  border-radius: 9px;
}

.help-btn.chat {
  background: #fff;
  color: #000000;
  border: 1px solid #afafaf;
  border-radius: 9px;
}


/* 반응형 */
@media (max-width: 600px) {
  .ins-container {
    padding: 12px;
    gap: 16px;
  }

  .ins-user {
    gap: 8px;
  }

  .ins-title {
    font-size: 16px;
  }

  .ins-card {
    font-size: 14px;
  }

  .help-btn {
    font-size: 13px;
  }
}
</style>

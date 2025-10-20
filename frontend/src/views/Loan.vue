<template>
  <div class="loan-container">
    <!-- 프로필 정보 + 신용등급 확인 버튼 -->
    <div class="profile-section">
      <div class="user-detail">
        <div class="img">
          <!--여기에 받아온 사진 들어가면 됩니다.-->
          <div class="frame">
            <img />
          </div>
        </div>
        <div class="user-text d-flex flex-row">
          <span class="user-name d-flex fs-4">김○○님</span>
          <span class="caregiver d-flex mt-3">일지매님의 보호자</span>
        </div>
        <button class="check-my-btn" @click="check_my()"> <span class="icon">🔍</span> 신용등급</button>
      </div>
      <div class="credit-info">
        <div class="grade">
          <span class="label">신용등급</span>
          <span class="unknown-grade">?등급</span>
        </div>
        <div class="limit">
          <span class="label">대출 가능 한도</span>
          <span class="loan-limit">?,???만원</span>
        </div>
      </div>
    </div>

    <!-- 이용 가능한 대출 -->
    <section class="available-section">
      <h2 class="section-title">이용 가능한 대출</h2>
      <div class="loan-card">
        <div class="card-header">
          <div class="bank-info">
            <div class="logo-frame">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg"
                style="margin-left: 9px;">
                <path d="M14 14H0V0H14V14Z" stroke="#E5E7EB" />
                <g clip-path="url(#clip0_165_13404)">
                  <path
                    d="M6.65554 0.0711914L0.530541 2.69619C0.147729 2.86025 -0.0655523 3.27041 0.0192133 3.6751C0.103979 4.07978 0.459448 4.3751 0.875073 4.3751V4.59385C0.875073 4.95752 1.16765 5.2501 1.53132 5.2501H12.4688C12.8325 5.2501 13.1251 4.95752 13.1251 4.59385V4.3751C13.5407 4.3751 13.8989 4.08252 13.9809 3.6751C14.063 3.26768 13.8497 2.85752 13.4696 2.69619L7.3446 0.0711914C7.12585 -0.0217773 6.87429 -0.0217773 6.65554 0.0711914ZM3.50007 6.1251H1.75007V11.4927C1.73367 11.5009 1.71726 11.5118 1.70085 11.5228L0.388354 12.3978C0.0684321 12.611 -0.0764898 13.0103 0.0356196 13.3794C0.147729 13.7485 0.489526 14.0001 0.875073 14.0001H13.1251C13.5106 14.0001 13.8497 13.7485 13.9618 13.3794C14.0739 13.0103 13.9317 12.611 13.6091 12.3978L12.2966 11.5228C12.2802 11.5118 12.2637 11.5036 12.2473 11.4927V6.1251H10.5001V11.3751H9.40632V6.1251H7.65632V11.3751H6.34382V6.1251H4.59382V11.3751H3.50007V6.1251ZM7.00007 1.7501C7.23214 1.7501 7.4547 1.84228 7.61879 2.00638C7.78289 2.17047 7.87507 2.39303 7.87507 2.6251C7.87507 2.85716 7.78289 3.07972 7.61879 3.24382C7.4547 3.40791 7.23214 3.5001 7.00007 3.5001C6.76801 3.5001 6.54545 3.40791 6.38135 3.24382C6.21726 3.07972 6.12507 2.85716 6.12507 2.6251C6.12507 2.39303 6.21726 2.17047 6.38135 2.00638C6.54545 1.84228 6.76801 1.7501 7.00007 1.7501Z"
                    fill="white" />
                </g>
                <defs>
                  <clipPath id="clip0_165_13404">
                    <path d="M0 0H14V14H0V0Z" fill="white" />
                  </clipPath>
                </defs>
              </svg>

            </div>
            <div class="bank-text">
              <span class="loan-name">KB국민은행 신용대출</span>
              <span class="bank-name">KB국민은행</span>
            </div>
          </div>
        </div>
        <div class="card-body">
          <div class="info-group">
            <span class="label">금리</span>
            <span class="value">연 4.8%~8.5%</span>
          </div>
          <div class="info-group">
            <span class="label">한도</span>
            <span class="value">최대 5,000만원</span>
          </div>
          <div class="info-group">
            <span class="label">기간</span>
            <span class="value">최대 7년</span>
          </div>
        </div>
        <button class="detail-btn" @click="check_my()">상세보기</button>
      </div>
    </section>

    <section class="available-section">
      <div class="loan-card">
        <div class="card-header">
          <div class="bank-info">
            <div class="logo-frame">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg"
                style="margin-left: 9px;">
                <path d="M14 14H0V0H14V14Z" stroke="#E5E7EB" />
                <g clip-path="url(#clip0_165_13404)">
                  <path
                    d="M6.65554 0.0711914L0.530541 2.69619C0.147729 2.86025 -0.0655523 3.27041 0.0192133 3.6751C0.103979 4.07978 0.459448 4.3751 0.875073 4.3751V4.59385C0.875073 4.95752 1.16765 5.2501 1.53132 5.2501H12.4688C12.8325 5.2501 13.1251 4.95752 13.1251 4.59385V4.3751C13.5407 4.3751 13.8989 4.08252 13.9809 3.6751C14.063 3.26768 13.8497 2.85752 13.4696 2.69619L7.3446 0.0711914C7.12585 -0.0217773 6.87429 -0.0217773 6.65554 0.0711914ZM3.50007 6.1251H1.75007V11.4927C1.73367 11.5009 1.71726 11.5118 1.70085 11.5228L0.388354 12.3978C0.0684321 12.611 -0.0764898 13.0103 0.0356196 13.3794C0.147729 13.7485 0.489526 14.0001 0.875073 14.0001H13.1251C13.5106 14.0001 13.8497 13.7485 13.9618 13.3794C14.0739 13.0103 13.9317 12.611 13.6091 12.3978L12.2966 11.5228C12.2802 11.5118 12.2637 11.5036 12.2473 11.4927V6.1251H10.5001V11.3751H9.40632V6.1251H7.65632V11.3751H6.34382V6.1251H4.59382V11.3751H3.50007V6.1251ZM7.00007 1.7501C7.23214 1.7501 7.4547 1.84228 7.61879 2.00638C7.78289 2.17047 7.87507 2.39303 7.87507 2.6251C7.87507 2.85716 7.78289 3.07972 7.61879 3.24382C7.4547 3.40791 7.23214 3.5001 7.00007 3.5001C6.76801 3.5001 6.54545 3.40791 6.38135 3.24382C6.21726 3.07972 6.12507 2.85716 6.12507 2.6251C6.12507 2.39303 6.21726 2.17047 6.38135 2.00638C6.54545 1.84228 6.76801 1.7501 7.00007 1.7501Z"
                    fill="white" />
                </g>
                <defs>
                  <clipPath id="clip0_165_13404">
                    <path d="M0 0H14V14H0V0Z" fill="white" />
                  </clipPath>
                </defs>
              </svg>

            </div>
            <div class="bank-text">
              <span class="loan-name">우리은행 신용대출</span>
              <span class="bank-name">우리은행</span>
            </div>
          </div>
        </div>
        <div class="card-body">
          <div class="info-group">
            <span class="label">금리</span>
            <span class="value">연 4.8%~6.5%</span>
          </div>
          <div class="info-group">
            <span class="label">한도</span>
            <span class="value">최대 2,000만원</span>
          </div>
          <div class="info-group">
            <span class="label">기간</span>
            <span class="value">최대 5년</span>
          </div>
        </div>
        <button class="detail-btn" @click="check_my()">상세보기</button>
      </div>
    </section>

    <!-- 대출 이용 팁 -->
    <section class="caution-section">
      <h2 class="section-title">대출 이용 팁</h2>
      <div class="tip-card">
        <div class="tip-header">
          <span class="warning">과도한 대출 주의</span>
        </div>
        <p class="tip-text">상환 능력을 고려하여 적정 수준의 대출을 이용하세요.</p>
      </div>
    </section>
  </div>
</template>

<script setup>
function check_my() {
  alert(`토스 또는 카카오 신용등급 확인하는 페이지로 이동`);
}
</script>

<style scoped>
.loan-container {
  width: 100%;
  padding: 16px;
  box-sizing: border-box;
}

/* 프로필 섹션 */
.profile-section {
  border-bottom: 1px solid #e5e5e5;
  padding-bottom: 24px;
  margin-bottom: 24px;
  margin-top: -40px;
}

.user-detail {
  display: flex;
  align-items: center;
  height: 80px;
}

.img .frame {
  width: 48px;
  height: 48px;
  background: #ddd;
  border-radius: 50%;
}

.user-text {
  align-items: center;
}

.user-text .caregiver {
  font-size: 14px;
  color: #666;
}

.user-text .user-name {
  font-size: 14px;
  font-weight: bold;
  color: #000;
}
.check-my-btn {
  margin-left: auto;
  width: 180px;
  height : 50px;
  padding-right: 9px;           /* 상하 여백 줄이고 좌우도 약간 축소 */
  font-size: 0.9rem;           /* 글자 크기 조금 작게 */
  background: #4a90e2;
  color: #fff;
  border: none;                /* dashed 제거 */
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2); /* 살짝 그림자 추가 */
  cursor: pointer;
  transition: background 0.2s;
}

.check-my-btn:hover {
  background: #3a78c2;         /* hover 시 색 짙어짐 */
}
.icon {
  font-size: 10px;
}

/* 신용등급/한도 정보 */
.credit-info {
  display: flex;
  justify-content: space-between;
  flex-direction: column;
  margin-top: 16px;
}

.grade,
.limit {
  display: flex;
  justify-content: space-between;
}

.label {
  font-size: 0.9rem;
  color: #999;
}

.unknown-grade,
.loan-limit {
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
}

/* 이용 가능한 대출 섹션 */
.available-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 1.2rem;
  font-weight: bold;
  margin-bottom: 16px;
}

.loan-card {
  width: 100%;
  background: #fafafa;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 16px;
}

.card-header .bank-info {
  display: flex;
  align-items: center;
}

.logo-frame {
  width: 32px;
  height: 32px;
  background: #4A62DD;
  border-radius: 4px;
}

.bank-text {
  margin-left: 12px;
}

.loan-name {
  font-weight: 600;
}

.bank-name {
  display: block;
  font-size: 0.9rem;
  color: #666;
}

.card-body {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  margin: 16px 0;
}

.info-group {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-group .label {
  font-size: 0.9rem;
  color: #999;
}

.info-group .value {
  font-size: 1rem;
  font-weight: bold;
  color: #333;
  margin-top: 4px;
  margin-left: 5px;
}

.detail-btn {
  width: 100%;
  padding: 8px 0;
  background: #ffffff;
  color: #000000;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* 대출 이용 팁 섹션 */
.caution-section {
  margin-bottom: 32px;
}

.tip-card {
  background: #fff8e1;
  border: 1px solid #ffe082;
  border-radius: 8px;
  padding: 16px;
}

.tip-header .warning {
  font-weight: bold;
  color: #e65100;
}

.tip-text {
  margin-top: 8px;
  color: #666;
}

/* 반응형 */
@media (max-width: 600px) {
  .loan-container {
    padding: 12px;
  }

  .check-my-btn,
  .detail-btn {
    padding: 6px 10px;
    font-size: 12px;
  }
}
</style>

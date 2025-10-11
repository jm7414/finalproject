<!-- 주형 ::: 아직 수정 덜 된 페이지입니다. -->

<template>
    <div class="insurance-page">
        <!-- 페이지 타이틀 -->
        <div class="page-title">
            <p>보험 검색</p>
        </div>

        <!-- 검색 카드 -->
        <div class="card-wrapper">
            <div class="card search-card">
                <div class="input-group">
                    <ProfileMarker class="input-icon" />
                    <input type="text" placeholder="성함" />
                </div>
                <div class="input-group">
                    <select>
                        <option>성별</option>
                        <option>남</option>
                        <option>여</option>
                    </select>
                </div>
                <div class="input-group">
                    <ProfileMarker class="input-icon" />
                    <input type="date" placeholder="생년월일" />
                </div>
                <div class="input-row">
                    <select>
                        <option>010</option>
                        <option>011</option>
                        <option>016</option>
                        <option>017</option>
                    </select>
                    <input type="text" placeholder=" - 를 제와하고 입력해주세요" />
                </div>
                <button class="search-button">보험료 확인하기 ></button>
            </div>
        </div>

        <!-- 페이지 서브타이틀 -->
        <div class="page-subtitle">
            <p>주요 보험사</p>
            <div class="advertise">
                <div class="insurer-card">
                    <img src="#" alt="한화손해보험" /> <!-- 이미지가 지금은 #으로 만들어놈 나중에 해야해-->
                    <div class="insurer-card-btn">
                        <button class="btn-consult">상담 신청</button>
                        <button class="btn-website">홈페이지</button>
                    </div>
                </div>
                <div class="insurer-card">
                    <img src="#" alt="삼성화재" /> <!-- 이미지가 지금은 #으로 만들어놈 나중에 해야해-->
                    <div class="insurer-card-btn">
                        <button class="btn-consult">상담 신청</button>
                        <button class="btn-website">홈페이지</button>
                    </div>
                </div>
                <!-- 추가 보험사도 동일 구조로 반복 -->
            </div>
        </div>
    </div>
</template>

<script setup>
import ProfileMarker from '@/components/ProfileMarker.vue'
</script>

<style scoped>
.insurance-page {
    max-width: 500px;
    margin: 0 auto;
    padding: 16px;
}

/* 페이지 타이틀 */
.page-title {
    background: #c3dafe;
    text-align: center;
    padding: 8px 0;
    border-radius: 8px;
    margin-bottom: 16px;
}

.page-title p {
    font-size: 18px;
    font-weight: 600;
    color: #2d3748;
    margin: 0;
}

/* 검색 카드 */
.card-wrapper {
    display: flex;
    justify-content: center;
    margin-bottom: 24px;
}

.search-card {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.input-group {
    position: relative;
    margin-bottom: 12px;
}

.input-icon {
    position: absolute;
    top: 50%;
    left: 12px;
    transform: translateY(-50%);
    width: 20px;
    height: 20px;
    color: #a0aec0;
}

.input-group input,
.input-group select {
    width: 100%;
    padding: 10px 12px 10px 40px;
    border: 1px solid #cbd5e0;
    border-radius: 8px;
    font-size: 14px;
    color: #2d3748;
}

.input-row {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
}

.input-row select {
    width: 30%;
    padding: 10px;
}

.input-row input {
    flex: 1;
    padding: 10px;
}

.search-button {
    width: 100%;
    background: #667eea;
    color: #fff;
    padding: 12px;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
}

.search-button:hover {
    background: #5a67d8;
}

/* 페이지 서브타이틀 */
.page-subtitle {
    margin-top: 32px;
}

.page-subtitle p {
    text-align: center;
    font-size: 18px;
    font-weight: 600;
    color: #2d3748;
    margin-bottom: 12px;
}

/* 광고(주요 보험사) */
.advertise {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.insurer-card img {
    width: 80px;
    height: auto;
    margin-right: 16px;
}

.insurer-card {
    display: flex;
    align-items: center;
    background: #fff;
    border-radius: 12px;
    padding: 12px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.insurer-card-btn {
    display: flex;
    justify-content: end;
}

.btn-consult,
.btn-website {
    flex-shrink: 0;
    margin-left: auto;
    padding: 8px 12px;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    cursor: pointer;
}

.btn-consult {
    background: #667eea;
    color: #fff;
}

.btn-consult:hover {
    background: #5a67d8;
}

.btn-website {
    background: #faf089;
    color: #2d3748;
    margin-left: 8px;
}

.btn-website:hover {
    background: #f6e05e;
}
</style>

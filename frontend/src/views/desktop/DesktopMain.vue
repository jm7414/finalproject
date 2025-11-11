<template>
  <div class="guardian-desktop">
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="avatar">👤</div>
        <div class="caretaker">
          <span class="label">보호자님</span>
          <span class="name">{{ guardianName }}</span>
        </div>
      </div>

      <nav class="menu">
        <button
          v-for="(item, idx) in menuItems"
          :key="idx"
          type="button"
          class="menu-item"
        >
          <span>{{ item }}</span>
        </button>
      </nav>

      <div class="sidebar-footer">
        <p class="support-text">궁금한 점이 있으신가요?</p>
        <button type="button" class="support-btn">고객센터 연결</button>
      </div>
    </aside>

    <section class="main-split">
      <div class="map-column">
        <div class="map-header">
          <div>
            <h1>안전존 관리</h1>
            <p class="subtitle">환자의 현재 위치와 안전존을 모니터링하세요.</p>
          </div>
          <button type="button" class="create-zone-btn">+ 새 안전존 추가</button>
        </div>

        <div class="map-placeholder">
          <div class="map-empty-state">
            <div class="icon-circle">🗺️</div>
            <p class="title">지도 준비 중</p>
            <p class="desc">실제 지도 연동 전까지는 이 영역이 자리잡고 있어요.</p>
          </div>
        </div>
      </div>

      <aside class="info-column">
        <section class="panel patient-card">
          <header class="panel-header">
            <h2>환자 정보</h2>
            <button type="button" class="panel-action">상세 보기</button>
          </header>
          <div class="patient-body">
            <div class="patient-avatar">🙂</div>
            <div class="patient-meta">
              <strong>{{ patient.name }}</strong>
              <span>{{ patient.age }}세 · {{ patient.gender }}</span>
              <span>등록일 {{ patient.registeredAt }}</span>
            </div>
            <ul class="patient-stats">
              <li>
                <span class="label">현재 상태</span>
                <span class="value" :class="{ alert: !safeZoneStatus.isInside }">
                  {{ safeZoneStatus.isInside ? '안전존 내부' : '안전존 이탈' }}
                </span>
              </li>
              <li>
                <span class="label">마지막 업데이트</span>
                <span class="value">{{ safeZoneStatus.lastUpdated }}</span>
              </li>
              <li>
                <span class="label">현재 위치</span>
                <span class="value">{{ safeZoneStatus.currentArea }}</span>
              </li>
            </ul>
          </div>
        </section>

        <section class="panel">
          <header class="panel-header">
            <h2>등록된 안전존</h2>
            <span class="badge">{{ safeZones.length }}개</span>
          </header>
          <ul class="safezone-list">
            <li v-for="zone in safeZones" :key="zone.id">
              <div>
                <strong>{{ zone.name }}</strong>
                <span class="muted">{{ zone.address }}</span>
              </div>
              <span class="distance">{{ zone.distance }}</span>
            </li>
          </ul>
        </section>

        <section class="panel">
          <header class="panel-header">
            <h2>최근 활동</h2>
          </header>
          <ul class="activity-list">
            <li v-for="(activity, index) in activities" :key="index">
              <div class="dot" :class="activity.type"></div>
              <div class="activity-meta">
                <strong>{{ activity.title }}</strong>
                <span class="muted">{{ activity.timeAgo }}</span>
              </div>
            </li>
          </ul>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup>
const guardianName = '김보호';

const menuItems = [
  '안전존',
  '예상위치',
  'AI보고서',
  '환자 연결관리',
  '일정',
  '커뮤니티',
  '종합 지원'
];

const patient = {
  name: '김영희',
  age: 78,
  gender: '여성',
  registeredAt: '2025.01.15'
};

const safeZoneStatus = {
  isInside: true,
  lastUpdated: '2분 전',
  currentArea: '서울시 강남구'
};

const safeZones = [
  { id: 1, name: '집', address: '서울시 강남구 한복판로 123', distance: '500m' },
  { id: 2, name: '병원', address: '서울시 강남구 논현로 456', distance: '300m' },
  { id: 3, name: '공원', address: '서울시 강남구 선릉로 789', distance: '400m' }
];

const activities = [
  { title: '안전존 내부 확인', timeAgo: '5분 전', type: 'safe' },
  { title: '위치 업데이트', timeAgo: '15분 전', type: 'info' },
  { title: '안전존 이탈 감지', timeAgo: '1시간 전', type: 'alert' }
];
</script>

<style scoped>
.guardian-desktop {
  display: flex;
  min-height: calc(100vh - 56px);
  background: transparent;
  color: #111827;
}

.sidebar {
  width: 240px;
  background: #111827;
  color: #f9fafb;
  display: flex;
  flex-direction: column;
  padding: 24px 20px;
  border-radius: 16px;
  margin-right: 24px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #1f2937;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.caretaker {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.caretaker .label {
  font-size: 12px;
  color: #9ca3af;
}

.caretaker .name {
  font-weight: 700;
  font-size: 15px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: auto;
}

.menu-item {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  border: 0;
  background: rgba(255, 255, 255, 0.06);
  color: inherit;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  padding: 0 14px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(3px);
}

.sidebar-footer {
  margin-top: 24px;
  padding: 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  text-align: center;
}

.support-text {
  font-size: 13px;
  margin-bottom: 12px;
}

.support-btn {
  width: 100%;
  height: 36px;
  border-radius: 10px;
  border: 0;
  background: #f59e0b;
  color: #111827;
  font-weight: 700;
  cursor: pointer;
}

.main-split {
  display: flex;
  flex: 1;
  gap: 24px;
}

.map-column {
  flex: 1 1 60%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.map-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.create-zone-btn {
  height: 36px;
  border-radius: 18px;
  border: 0;
  background: #6366f1;
  color: #ffffff;
  font-weight: 600;
  padding: 0 18px;
  cursor: pointer;
  transition: filter 0.2s ease;
}

.create-zone-btn:hover {
  filter: brightness(0.95);
}

.map-placeholder {
  flex: 1;
  border-radius: 16px;
  background: linear-gradient(135deg, #e5e7eb, #d1d5db);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 480px;
  border: 2px dashed rgba(55, 65, 81, 0.2);
}

.map-empty-state {
  text-align: center;
  color: #374151;
  max-width: 320px;
}

.icon-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(99, 102, 241, 0.15);
  margin: 0 auto 16px;
  font-size: 30px;
}

.map-empty-state .title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 6px;
}

.map-empty-state .desc {
  font-size: 14px;
  color: #4b5563;
}

.info-column {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.panel-action {
  border: 0;
  background: transparent;
  color: #6366f1;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 24px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
  color: #4c1d95;
  background: rgba(129, 140, 248, 0.18);
  border-radius: 999px;
}

.patient-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.patient-avatar {
  width: 80px;
  height: 80px;
  border-radius: 24px;
  background: #eef2ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
}

.patient-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #4b5563;
}

.patient-meta strong {
  font-size: 18px;
  color: #111827;
}

.patient-stats {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.patient-stats li {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #4b5563;
}

.patient-stats .value {
  font-weight: 600;
  color: #111827;
}

.patient-stats .value.alert {
  color: #ef4444;
}

.safezone-list,
.activity-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.safezone-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(238, 242, 255, 0.6);
}

.safezone-list strong {
  font-size: 15px;
  color: #111827;
}

.muted {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.distance {
  font-size: 13px;
  font-weight: 600;
  color: #4338ca;
}

.activity-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f9fafb;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.safe {
  background: #10b981;
}

.dot.info {
  background: #3b82f6;
}

.dot.alert {
  background: #ef4444;
}

.activity-meta strong {
  font-size: 14px;
  color: #111827;
}

.activity-meta .muted {
  margin-top: 2px;
}

@media (max-width: 1440px) {
  .map-placeholder {
    min-height: 420px;
  }

  .info-column {
    width: 300px;
  }
}
</style>


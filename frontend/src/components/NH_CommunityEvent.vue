<script setup>
import { ref, computed } from 'vue';

const filters = ['전체', '봉사활동', '모임', '교육'];
const activeFilter = ref('전체');

function changeFilter(filter) {
  activeFilter.value = filter;
}

// --- 더미 데이터 ---
const events = ref([
    {
    id: 1,
    type: 'large-image',
    category: '봉사활동',
    date: '2025-10-20',
    title: '치매 어르신 동반 산책 봉사자 모집',
    description: '매주 토요일 오전 10시, 근린공원에서 치매 어르신과 함께하는 산책 봉사활동에 참여하실 분을 모집합니다.',
    location: '서울 강남구',
    image: 'https://images.unsplash.com/photo-1593113598332-cd288d649433?q=80&w=2070',
    url: 'https://www.1365.go.kr/'
  },
  {
    id: 2,
    type: 'side-image',
    category: '모임',
    date: '2025-10-25',
    title: '치매 가족 소통 모임',
    description: '치매 환자 가족들과의 경험 공유 및 정보 교환',
    participants: 12,
    image: 'https://images.unsplash.com/photo-1543269865-cbf427effbad?q=80&w=2070',
    url: 'https://www.nid.or.kr/'
  },
  {
    id: 3,
    type: 'side-image',
    category: '교육',
    date: '2025-10-28',
    title: '치매 예방 교육 프로그램',
    description: '전문의와 함께하는 치매 예방 및 관리 방법 교육',
    duration: '2시간',
    image: 'https://images.unsplash.com/photo-1552664730-d307ca884978?q=80&w=2070',
    url: 'https://www.nid.or.kr/'
  },
    {
    id: 4,
    type: 'side-image',
    category: '봉사활동',
    date: '2025-11-01',
    title: '치매 센터 도우미 활동',
    description: '지역 치매 센터에서 어르신들과 함께 하는 활동 보조',
    typeOfWork: '정기 봉사',
    image: 'https://images.unsplash.com/photo-1618593639185-a77986c12573?q=80&w=2069',
    url: 'https://www.1365.go.kr/'
  }
]);

const filteredEvents = computed(() => {
  if (activeFilter.value === '전체') {
    return events.value;
  }
  return events.value.filter(event => event.category === activeFilter.value);
});

function goToUrl(url) {
  if (url) {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
}
</script>

<template>
  <div class="event-page-container">
    <section class="filter-section">
      <button 
        v-for="filter in filters" 
        :key="filter"
        class="filter-btn"
        :class="{ active: activeFilter === filter }"
        @click="changeFilter(filter)"
      >
        {{ filter }}
      </button>
    </section>

    <main class="event-list">
      <div 
        v-for="event in filteredEvents" 
        :key="event.id"
        class="event-card-link"
        @click="goToUrl(event.url)"
      >
        <div v-if="event.type === 'large-image'" class="event-card large-type">
          <img :src="event.image" alt="이벤트 썸네일" class="card-img-large">
          <div class="card-content">
            <div class="card-header">
              <span class="category-tag">{{ event.category }}</span>
              <span class="date">{{ event.date }}</span>
            </div>
            <h3 class="title">{{ event.title }}</h3>
            <p class="description">{{ event.description }}</p>
            <div class="card-footer">
              <span class="location">📍 {{ event.location }}</span>
              <span class="details-link">자세히 보기</span>
            </div>
          </div>
        </div>

        <div v-if="event.type === 'side-image'" class="event-card side-type">
          <img :src="event.image" alt="이벤트 썸네일" class="card-img-side">
          <div class="card-content">
            <div class="card-header">
              <span class="category-tag small">{{ event.category }}</span>
              <span class="date">{{ event.date }}</span>
            </div>
            <h3 class="title">{{ event.title }}</h3>
            <p class="description">{{ event.description }}</p>
            <div class="card-footer">
              <span class="info">
                <template v-if="event.participants">👥 {{ event.participants }}명 참여</template>
                <template v-if="event.duration">⏰ {{ event.duration }}</template>
                <template v-if="event.typeOfWork">🤝 {{ event.typeOfWork }}</template>
              </span>
              <span class="action-link">
                {{ event.category === '모임' ? '참여하기' : (event.category === '교육' ? '신청하기' : '문의하기') }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* 전체 레이아웃 */
.event-page-container {
  width: 100%;
  background: #FFFFFF;
  font-family: 'Inter', sans-serif;
}

/* 필터 섹션 */
.filter-section {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #E5E5E5;
}

.filter-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 9999px;
  font-size: 14px;
  cursor: pointer;
  background: #F5F5F5;
  color: #404040;
  transition: all 0.2s ease;
}

/* 아보카도 색상으로 변경 */
.filter-btn.active {
  background: #a7cc10; /* 기존 #8E97FD에서 변경 */
  color: #FFFFFF;
  font-weight: bold;
}

/* 이벤트 목록 */
.event-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background-color: #FAFAFA;
}

.event-card-link {
  color: inherit;
  cursor: pointer;
}

/* 카드 공통 스타일 */
.event-card {
  background: #FFFFFF;
  border: 1px solid #E5E5E5;
  box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.event-card:hover {
  transform: translateY(-4px);
  box-shadow: 0px 4px 15px rgba(0, 0, 0, 0.1);
}

/* 카드 1: 큰 이미지 타입 */
.large-type .card-img-large {
  width: 100%;
  height: 160px;
  object-fit: cover;
  background-color: #D4D4D4;
}

.large-type .card-content {
  padding: 16px;
}

.large-type .card-footer {
  margin-top: 16px;
}

.large-type .details-link {
  font-size: 14px;
  color: #525252;
  font-weight: 500;
}

/* 카드 2: 옆으로 작은 이미지 타입 */
.side-type {
  display: flex;
  padding: 16px;
  gap: 16px;
}

.side-type .card-img-side {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 8px;
  background-color: #D4D4D4;
  flex-shrink: 0;
}

.side-type .card-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.side-type .card-footer {
  margin-top: auto;
  padding-top: 8px;
}

.side-type .info {
  font-size: 12px;
  color: #737373;
}

.side-type .action-link {
  font-size: 12px;
  color: #525252;
  font-weight: 500;
}

/* 카드 내부 공통 요소 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.category-tag {
  padding: 4px 8px;
  background: #F5F5F5;
  border-radius: 9999px;
  font-size: 12px;
  color: #262626;
  font-weight: 500;
}

.date {
  font-size: 12px;
  color: #737373;
}

.title {
  font-size: 16px;
  font-weight: bold;
  color: #171717;
  margin: 0 0 8px 0;
}

.description {
  font-size: 14px;
  color: #525252;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

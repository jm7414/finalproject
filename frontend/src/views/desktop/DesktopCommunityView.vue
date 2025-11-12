<template>
  <div class="desktop-community-container">
    
    <!-- 
      이제 넓은 화면을 마음껏 사용할 수 있습니다.
      아까 논의했던 '자식 라우팅'이나 '그리드'를 적용하기 완벽합니다.
    -->

    <!-- 예시: 3단 그리드 레이아웃 -->
    <div class="community-grid">
      
      <!-- 1. 왼쪽 컬럼: 실종자 목록 -->
      <aside class="left-panel">
        <h2>최근 실종자</h2>
        <CommunityMissing /> 
      </aside>

      <!-- 2. 중앙 컬럼: 메인 게시판 -->
      <main class="main-content">
        <h2>커뮤니티 게시글</h2>
        <CommunityBoard />
      </main>

      <!-- 3. 오른쪽 컬럼: 동네 소식 -->
      <aside class="right-panel">
        <h2>동네 소식</h2>
        <CommunityEvent />
      </aside>

    </div>
  </div>
</template>

<script setup>
// PC 레이아웃에 맞게 3개 컴포넌트를 모두 불러옵니다.
import CommunityMissing from '@/components/CommunityMissing.vue';
import CommunityBoard from '@/components/CommunityBoard.vue';
import CommunityEvent from '@/components/CommunityEvent.vue';
</script>

<style scoped>
.desktop-community-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.community-grid {
  display: grid;
  grid-template-columns: 300px 1fr 280px; /* 예시 3단 컬럼 */
  gap: 20px;
}

.left-panel, .main-content, .right-panel {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
</style>
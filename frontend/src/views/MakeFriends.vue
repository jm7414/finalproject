<template>
  <div class="container-sm py-3" style="max-width:414px;">
    <!-- 헤더 -->
    <div class="header-section mb-4">
      <button @click="router.back()" class="btn-back">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h2 class="header-title">🌿친구 추가</h2>
      <p class="header-subtitle">이웃을 추가하고 함께 활동해보세요</p>
    </div>

    <!-- 친구 추가 카드 -->
    <div class="card rounded-4 shadow-sm mb-4">
      <div class="card-body p-4">
        <h5 class="card-title mb-3">
          <i class="bi bi-person-plus-fill me-2" style="color:#a7cc10"></i>
          이웃 검색
        </h5>
        
        <div class="search-section">
          <input 
            v-model="searchUserNo" 
            type="number" 
            placeholder="이웃의 사용자 번호를 입력하세요"
            class="form-control search-input"
            @keyup.enter="addFriend"
          />
          <button @click="addFriend" class="btn-add-friend">
            <i class="bi bi-plus-circle-fill me-1"></i>
            추가하기
          </button>
        </div>

        <!-- 메시지 -->
        <div v-if="message" :class="['alert-message', message.type]" class="mt-3">
          <i :class="message.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'" class="me-2"></i>
          {{ message.text }}
        </div>

        <div class="info-box mt-3">
          <i class="bi bi-info-circle-fill me-2"></i>
          이웃의 사용자 번호를 입력하여 친구를 추가할 수 있습니다.
        </div>
      </div>
    </div>

    <!-- 내 친구 목록 -->
    <div class="card rounded-4 shadow-sm">
      <div class="card-body p-4">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h5 class="card-title mb-0">
            <i class="bi bi-people-fill me-2" style="color:#a7cc10"></i>
            내 친구 목록
          </h5>
          <button @click="loadFriends" class="btn-refresh">
            <i class="bi bi-arrow-clockwise"></i>
          </button>
        </div>

        <!-- 로딩 -->
        <div v-if="loading" class="text-center py-5">
          <div class="spinner-border text-success" style="color:#a7cc10 !important" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p class="mt-3 text-muted">친구 목록을 불러오는 중...</p>
        </div>

        <!-- 친구 없음 -->
        <div v-else-if="friends.length === 0" class="empty-state text-center py-5">
          <i class="bi bi-person-x" style="font-size:3rem; color:#c2d477"></i>
          <p class="mt-3 text-muted mb-1">아직 친구가 없습니다</p>
          <p class="text-muted small">위에서 친구를 추가해보세요!</p>
        </div>

        <!-- 친구 목록 -->
        <div v-else class="friend-list">
          <div v-for="friend in friends" :key="friend.userNo" class="friend-item">
            <div class="friend-avatar">
              <img v-if="friend.profilePhoto" :src="friend.profilePhoto" alt="프로필" />
              <div v-else class="avatar-placeholder">
                {{ friend.name.charAt(0) }}
              </div>
            </div>
            <div class="friend-info">
              <h6 class="friend-name">{{ friend.name }}</h6>
              <p class="friend-detail">사용자 번호: {{ friend.userNo }}</p>
              <p class="friend-detail">전화번호: {{ friend.phoneNumber || '미등록' }}</p>
            </div>
            <button @click="removeFriend(friend.userNo)" class="btn-delete">
              <i class="bi bi-trash-fill"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const searchUserNo = ref('')
const friends = ref([])
const loading = ref(false)
const message = ref(null)

/* ===== 친구 추가 ===== */
const addFriend = async () => {
  if (!searchUserNo.value) {
    showMessage('사용자 번호를 입력해주세요.', 'error')
    return
  }

  try {
    await axios.post(`/NH/api/neighbor/friends/${searchUserNo.value}`)
    showMessage('친구가 추가되었습니다! 🎉', 'success')
    searchUserNo.value = ''
    loadFriends()
  } catch (error) {
    showMessage(error.response?.data?.message || '친구 추가에 실패했습니다.', 'error')
  }
}

/* ===== 친구 목록 조회 ===== */
const loadFriends = async () => {
  loading.value = true
  try {
    const response = await axios.get('/NH/api/neighbor/friends')
    friends.value = response.data
  } catch (error) {
    showMessage('친구 목록 조회에 실패했습니다.', 'error')
  } finally {
    loading.value = false
  }
}

/* ===== 친구 삭제 ===== */
const removeFriend = async (userNo) => {
  if (!confirm('정말 삭제하시겠습니까?')) return

  try {
    await axios.delete(`/NH/api/neighbor/friends/${userNo}`)
    showMessage('친구가 삭제되었습니다.', 'success')
    loadFriends()
  } catch (error) {
    showMessage('친구 삭제에 실패했습니다.', 'error')
  }
}

/* ===== 메시지 표시 ===== */
const showMessage = (text, type) => {
  message.value = { text, type }
  setTimeout(() => {
    message.value = null
  }, 3000)
}

/* ===== 초기화 ===== */
onMounted(() => {
  loadFriends()
})
</script>

<style scoped>
@import url('https://cdn.jsdelivr.net/gh/sunn-us/SUIT/fonts/static/woff2/SUIT.css');

* {
  font-family: 'SUIT', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.header-section {
  text-align: center;
}

.btn-back {
  position: absolute;
  left: 20px;
  top: 20px;
  background: white;
  border: 2px solid #c2d477;
  border-radius: 12px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a7cc10;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-back:hover {
  background: #f5f9e8;
  border-color: #a7cc10;
}

.header-title {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
  
}

.header-subtitle {
  color: #666;
  font-size: 15px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.search-section {
  display: flex;
  gap: 10px;
}

.search-input {
  flex: 1;
  padding: 14px 18px;
  border: 2px solid #c2d477;
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #a7cc10;
  box-shadow: 0 0 0 3px rgba(167, 204, 16, 0.1);
}

.btn-add-friend {
  padding: 14px 24px;
  background: linear-gradient(135deg, #a7cc10 0%, #8fb80e 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.btn-add-friend:hover {
  background: linear-gradient(135deg, #8fb80e 0%, #7aa00c 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(167, 204, 16, 0.3);
}

.alert-message {
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
}

.alert-message.success {
  background: #e8f5d6;
  color: #5a8f0d;
  border: 1px solid #a7cc10;
}

.alert-message.error {
  background: #fee;
  color: #c33;
  border: 1px solid #fcc;
}

.info-box {
  background: #f5f9e8;
  border: 1px solid #c2d477;
  border-radius: 10px;
  padding: 12px;
  font-size: 13px;
  color: #666;
}

.btn-refresh {
  background: white;
  border: 2px solid #c2d477;
  border-radius: 10px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a7cc10;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-refresh:hover {
  background: #f5f9e8;
  border-color: #a7cc10;
  transform: rotate(180deg);
}

.friend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #f5f9e8;
  border: 2px solid #c2d477;
  border-radius: 14px;
  transition: all 0.3s ease;
}

.friend-item:hover {
  border-color: #a7cc10;
  box-shadow: 0 2px 8px rgba(167, 204, 16, 0.15);
}

.friend-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  overflow: hidden;
  background: #a7cc10;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.friend-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  color: white;
  font-size: 22px;
  font-weight: 700;
}

.friend-info {
  flex: 1;
}

.friend-name {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.friend-detail {
  margin: 2px 0;
  font-size: 13px;
  color: #666;
}

.btn-delete {
  background: #fee;
  color: #c33;
  border: 1px solid #fcc;
  border-radius: 10px;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.btn-delete:hover {
  background: #fcc;
  transform: scale(1.1);
}
</style>

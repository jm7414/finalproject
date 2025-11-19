<template>
  <!-- 공지 작성 버튼 -->
  <button v-if="isPlazaMaster" class="write-notice-btn mt-4" @click="emit('write-notice')">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round"
        stroke-linejoin="round" />
    </svg>
    공지 작성
  </button>
  
  <div>
    <div v-if="loading" class="loading-state">
      공지를 불러오는 중입니다...
    </div>

    <div v-else-if="error" class="error-state">
      {{ error }}
    </div>

    <div v-else>
      <div v-if="notices.length === 0" class="empty-state">
        <p>공지사항이 없습니다.</p>
      </div>

      <div v-else class="post-list">
        <div v-for="notice in notices" :key="notice.noticeNo" class="post-card">
          <div class="card-header">
            <div class="author-info">
              <div class="author-details">
                <span class="author-name">{{ notice.authorName }}</span>
                <span class="post-time">{{ formatTimeAgo(notice.createdAt) }}</span>
              </div>
            </div>
            
            <div v-if="isPlazaMaster" class="options-wrapper">
              <button 
                class="options-button" 
                @click.stop="toggleMenu(notice.noticeNo)"
              >
                ⋮
              </button>
              
              <!-- 핵심: style 바인딩으로 직접 제어 -->
              <div 
                class="dropdown-menu"
                :style="{ display: openMenuId === notice.noticeNo ? 'block' : 'none' }"
              >
                <button @click.stop="editNotice(notice)" class="menu-item">
                  <span>✏️</span> 수정
                </button>
                <button @click.stop="deleteNotice(notice.noticeNo)" class="menu-item delete">
                  <span>🗑️</span> 삭제
                </button>
              </div>
            </div>
          </div>

          <div class="card-body">
            <h3 class="post-title">{{ notice.title }}</h3>
            <p class="post-content">{{ notice.content }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'

const props = defineProps({
  isPlazaMaster: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['write-notice', 'edit-notice'])

const notices = ref([])
const loading = ref(true)
const error = ref(null)
const openMenuId = ref(null)

onMounted(() => {
  fetchNotices()
  setTimeout(() => {
    document.addEventListener('click', handleDocumentClick)
  }, 0)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
})

async function fetchNotices() {
  loading.value = true
  error.value = null
  try {
    const response = await axios.get('/NH/api/neighbor/notices')
    notices.value = response.data
    console.log('Notices loaded:', notices.value)
  } catch (err) {
    console.error("공지 목록을 불러오는 데 실패했습니다:", err)
    error.value = "데이터를 불러올 수 없습니다."
  } finally {
    loading.value = false
  }
}

function toggleMenu(noticeNo) {
  console.log('Toggle clicked:', noticeNo, 'Type:', typeof noticeNo)
  console.log('Current openMenuId:', openMenuId.value, 'Type:', typeof openMenuId.value)
  
  // 타입 불일치 방지: 명시적으로 같은 타입으로 변환
  const menuId = Number(noticeNo)
  
  if (openMenuId.value === menuId) {
    openMenuId.value = null
  } else {
    openMenuId.value = menuId
  }
  
  console.log('New openMenuId:', openMenuId.value)
}

function handleDocumentClick(event) {
  if (event.target.closest('.options-wrapper')) {
    return
  }
  
  if (openMenuId.value !== null) {
    openMenuId.value = null
  }
}

function editNotice(notice) {
  openMenuId.value = null
  emit('edit-notice', notice)
}

async function deleteNotice(noticeNo) {
  openMenuId.value = null
  
  if (!confirm('이 공지를 삭제하시겠습니까?')) return

  try {
    await axios.delete(`/NH/api/neighbor/notices/${noticeNo}`)
    await fetchNotices()
    alert('공지가 삭제되었습니다.')
  } catch (error) {
    console.error('공지 삭제 실패:', error)
    if (error.response && error.response.status === 403) {
      alert('방장만 공지를 삭제할 수 있습니다.')
    } else {
      alert('공지 삭제에 실패했습니다.')
    }
  }
}

function formatTimeAgo(dateString) {
  const now = new Date()
  const postDate = new Date(dateString)
  const seconds = Math.floor((now - postDate) / 1000)
  let interval = seconds / 31536000
  if (interval > 1) return Math.floor(interval) + "년 전"
  interval = seconds / 2592000
  if (interval > 1) return Math.floor(interval) + "달 전"
  interval = seconds / 86400
  if (interval > 1) return Math.floor(interval) + "일 전"
  interval = seconds / 3600
  if (interval > 1) return Math.floor(interval) + "시간 전"
  interval = seconds / 60
  if (interval > 1) return Math.floor(interval) + "분 전"
  return "방금 전"
}
</script>

<style scoped>
.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.error-state {
  color: red;
}

.post-list {
  display: flex;
  flex-direction: column;
  margin-top: 10px;
  gap: 12px;
  padding: 16px;
}

.post-card {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.05);
  padding: 16px;
  transition: transform 0.2s ease;
  position: relative;
}

.post-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  position: relative;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-details {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.post-time {
  font-size: 12px;
  color: #888;
}

.options-wrapper {
  position: relative;
  z-index: 100;
}

.options-button {
  background: none;
  border: none;
  font-size: 20px;
  font-weight: bold;
  color: #aaa;
  cursor: pointer;
  padding: 8px;
  transition: color 0.2s;
  position: relative;
}

.options-button:hover {
  color: #666;
  background: #f5f5f5;
  border-radius: 4px;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  min-width: 120px;
  overflow: hidden;
  margin-top: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: #fff;
  color: #333;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s;
}

.menu-item:hover {
  background: #f5f5f5;
}

.menu-item.delete {
  color: #dc2626;
}

.menu-item.delete:hover {
  background: #fee;
}

.menu-item span {
  font-size: 16px;
}

.card-body {
  margin-bottom: 12px;
}

.post-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-content {
  font-size: 14px;
  color: #555;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  min-height: calc(1.5em * 1);
}

.write-notice-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 auto;
  margin-top: 32px;
  padding: 12px 60px;
  background: #a7cc10;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0px 2px 8px rgba(167, 204, 16, 0.13);
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
}

.write-notice-btn:hover {
  background: #8fb00d;
  box-shadow: 0px 6px 18px rgba(140, 176, 13, 0.22);
}

.write-notice-btn svg {
  color: #fff;
}
</style>

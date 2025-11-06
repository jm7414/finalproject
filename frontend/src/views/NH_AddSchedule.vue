<template>
  <div class="add-schedule-page">
    <!-- 헤더 -->
    <div class="header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">←</button>
        <h1 class="page-title">일정 추가</h1>
      </div>
    </div>

    <!-- 폼 섹션 -->
    <div class="form-section">
      <!-- 일정 제목 -->
      <div class="form-group">
        <input 
          v-model="scheduleForm.title"
          type="text" 
          class="form-input"
          placeholder="일정 제목을 입력하세요*"
          required
        />
      </div>

      <!-- 일정 내용 -->
      <div class="form-group">
        <textarea 
          v-model="scheduleForm.content"
          class="form-textarea"
          placeholder="일정 내용을 입력하세요"
          rows="4"
        ></textarea>
      </div>

      <!-- 날짜 선택 -->
      <div class="form-group">
        <input 
          v-model="scheduleForm.date"
          type="date" 
          class="form-input"
          required
        />
      </div>
    </div>

    <!-- 액션 버튼 -->
    <div class="action-buttons">
      <button class="save-btn" @click="saveSchedule">저장</button>
      <button class="cancel-btn" @click="cancelSchedule">취소</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const scheduleForm = ref({
  title: '',
  content: '',
  date: ''
})

function goBack() {
  router.go(-1)
}

async function saveSchedule() {
  if (!scheduleForm.value.title || !scheduleForm.value.date) {
    alert('제목과 날짜를 입력해주세요.')
    return
  }

  try {
    const response = await fetch('/api/schedule/create-NH', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(scheduleForm.value)
    })

    if (!response.ok) throw new Error('저장 실패')
    
    alert('일정이 저장되었습니다.')
    router.push({ name: 'NH_Calender' })
  } catch (error) {
    alert('오류: ' + error.message)
  }
}

function cancelSchedule() {
  router.go(-1)
}

onMounted(() => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  scheduleForm.value.date = `${year}-${month}-${day}`
})
</script>

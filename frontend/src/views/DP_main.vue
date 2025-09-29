<template>
  <v-container fluid class="pa-0">
    <!-- 헤더 -->
    <v-sheet color="white" class="px-4 pt-4 pb-2">
      <v-list-item :title="'안녕하세요!'" :subtitle="user.name + '님'" :prepend-avatar="user.avatar">
        <template #append>
          <v-btn icon variant="text" density="comfortable" :ripple="false" @click="ping('알림(미구현)')">
            <v-icon>mdi-bell-outline</v-icon>
          </v-btn>
        </template>
      </v-list-item>
    </v-sheet>
    <v-divider />

    <!-- 오늘의 일정 -->
    <v-container class="py-4">
      <v-card color="primary" variant="flat" rounded="xl" class="pa-4">
        <div class="d-flex align-center justify-space-between">
          <div class="text-white text-subtitle-1 font-weight-medium">오늘의 일정</div>
          <v-btn icon variant="text" density="comfortable" :ripple="false" @click="ping('캘린더(미구현)')">
            <v-icon color="white">mdi-calendar-month-outline</v-icon>
          </v-btn>
        </div>

        <v-card v-for="(it, i) in schedules" :key="i" class="mt-3 pa-3" color="white" variant="tonal" rounded="lg">
          <div class="d-flex align-center justify-space-between">
            <div>
              <div class="text-white">{{ it.title }}</div>
              <div class="text-white text-body-2">{{ it.sub }}</div>
            </div>
            <div class="text-right">
              <div class="text-white">{{ it.time }}</div>
              <div class="text-white text-caption">{{ it.eta }}</div>
            </div>
          </div>
        </v-card>
      </v-card>
    </v-container>

    <!-- 빠른 액션 2x2 -->
    <v-container class="pt-2">
      <v-row dense>
        <v-col cols="6" v-for="a in actions" :key="a.key">
          <v-card variant="outlined" rounded="xl" class="pa-4" @click="onAction(a.key)">
            <div class="d-flex flex-column align-center">
              <v-avatar size="56" :color="a.bg">
                <v-icon :icon="a.icon" size="28" />
              </v-avatar>
              <div class="mt-3 text-body-1">{{ a.label }}</div>
            </div>
          </v-card>
        </v-col>
      </v-row>
    </v-container>

    <!-- SOS 큰 버튼 -->
    <v-container class="py-6">
      <v-btn color="error" size="large" rounded="xl" block @click="ping('비상연락(미구현)')">
        <v-icon start>mdi-alert-circle-outline</v-icon>
        비상연락
      </v-btn>
    </v-container>

    <!-- 클릭 피드백 -->
    <v-snackbar v-model="snack" timeout="1500" location="bottom">
      {{ snackMsg }}
    </v-snackbar>
  </v-container>
</template>

<script setup>
import { ref } from 'vue'

const user = ref({
  name: '김환자',
  avatar: 'https://placehold.co/80x80'
})

const schedules = ref([
  { title: '정기 검진', sub: '내과 - 김의사', time: '오후 2:30', eta: '1시간 30분 후' },
  { title: '약물 복용', sub: '혈압약 복용 시간', time: '오후 6:00', eta: '5시간 후' }
])

const actions = ref([
  { key: 'log', label: '기록', icon: 'mdi-clipboard-text-outline', bg: 'blue-lighten-4' },
  { key: 'sos', label: '비상연락', icon: 'mdi-phone-alert-outline', bg: 'red-lighten-4' },
  { key: 'profile', label: '내정보', icon: 'mdi-account-circle-outline', bg: 'green-lighten-4' },
  { key: 'chat', label: 'AI챗봇', icon: 'mdi-robot-outline', bg: 'deep-purple-lighten-4' }
])

const snack = ref(false)
const snackMsg = ref('')

function ping(msg) {
  snackMsg.value = msg
  snack.value = true
}

function onAction(key) {
  const a = actions.value.find(x => x.key === key)
  ping(`${a?.label ?? key} (미구현)`)
}
</script>

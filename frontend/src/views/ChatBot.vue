<!-- src/views/Chatbot.vue -->
<template>
  <div class="chatbot-wrap">
    <!-- 스크롤되는 대화 영역 -->
    <main class="chat-area" ref="scrollRef">
      <!-- 초기 한줄 Tip (닫거나 첫 입력 시 자동 숨김) -->
      <div v-if="showTip" class="tip" role="note">
        <span class="bulb">💡</span>
        <span class="tip-text">{{ tipText }}</span>
        <button class="tip-close" @click="showTip = false" aria-label="안내 닫기">
          ✕
        </button>
      </div>

      <!-- 기존 메시지 -->
      <div v-for="(m, i) in messages" :key="i" class="row" :class="m.role">
        <!-- 봇 아바타: 손자 얼굴 -->
        <div v-if="m.role === 'bot'" class="avatar">
          <img :src="grandsonAvatar" alt="손자 아바타" class="avatar-img" />
        </div>
        <div class="bubble" v-text="m.text"></div>
      </div>

      <!-- 로딩(타이핑) 표시 -->
      <div v-if="loading" class="row bot">
        <div class="avatar">
          <img :src="grandsonAvatar" alt="손자 아바타" class="avatar-img" />
        </div>
        <div class="bubble typing">
          <span></span><span></span><span></span>
        </div>
      </div>
    </main>

    <!-- 하단 입력 바 (고정) -->
    <footer class="inputbar">
      <button
        class="icon-btn"
        @click="onQuickAction"
        :disabled="loading"
        aria-label="퀵액션"
      >
        🔎
      </button>
      <input
        v-model.trim="draft"
        class="textbox"
        type="text"
        placeholder="채팅을 적어주세요"
        :disabled="loading"
        @keydown.enter.prevent="sendMessage"
        @focus="scrollToBottom"
      />
      <button
        class="mic-btn"
        @click="toggleMic"
        :class="{ on: listening }"
        :disabled="loading"
        aria-label="음성 입력"
      >
        🎤
      </button>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import grandsonAvatar from '@/assets/images/grand.png' // 손자 이미지

const API_BASE = '' // 같은 오리진이면 공백 유지

/** 상태 */
const messages = ref([]) // [{ role:'me'|'bot', text:'...' }]
const draft = ref('')
const loading = ref(false)
const scrollRef = ref(null)

/** 초기 한 줄 팁 */
const TIPS = [
  '“도움말”을 입력하면 사용 가능한 명령을 알려드려요.',
  '음성 버튼을 눌러 “오늘 기록 시작”처럼 말해보세요.',
  '“일정보여줘”라고 입력하면 일정 화면으로 이동할 수 있어요.',
]
const showTip = ref(true)
const tipText = TIPS[Math.floor(Math.random() * TIPS.length)]

/** 스크롤 하단 고정 (chat-area 안에서만) */
const scrollToBottom = async () => {
  await nextTick()
  const el = scrollRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

onMounted(scrollToBottom)

/** 전송 → /api/chat/handle */
const sendMessage = async () => {
  if (!draft.value || loading.value) return
  if (showTip.value) showTip.value = false

  messages.value.push({ role: 'me', text: draft.value })
  const userText = draft.value
  draft.value = ''
  await scrollToBottom()

  loading.value = true
  try {
    const res = await fetch(`${API_BASE}/api/chat/handle`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: 1, text: userText }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = await res.json()
    const last = data?.messages?.[data.messages.length - 1]
    const reply = last?.text || '응답이 비어 있어요.'
    messages.value.push({ role: 'bot', text: reply })
  } catch (err) {
    console.error(err)
    messages.value.push({
      role: 'bot',
      text: '서버 연결 오류가 있어요. 잠시 후 다시 시도해주세요.',
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

/** 퀵 액션(예시) */
const onQuickAction = () => {
  draft.value = '일정보여줘'
}

/** (옵션) 음성 입력 */
const listening = ref(false)
let rec = null
if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  rec = new SR()
  rec.lang = 'ko-KR'
  rec.interimResults = false
  rec.maxAlternatives = 1
  rec.onresult = (e) => {
    const txt = e.results[0][0].transcript
    draft.value = (draft.value ? draft.value + ' ' : '') + txt
  }
  rec.onend = () => {
    listening.value = false
  }
}
const toggleMic = () => {
  if (!rec) {
    alert('이 브라우저는 음성 인식을 지원하지 않습니다.')
    return
  }
  if (listening.value) {
    rec.stop()
    listening.value = false
  } else {
    rec.start()
    listening.value = true
  }
}
</script>

<style scoped>
:root {
  --appbar-h: 64px;
  --tabbar-h: 72px;
  --input-h: 64px;

  --me-bg: #eef1ff;
  --me-text: #0b1f33;
  --me-border: #c9d2ff;

  --bot-bg: #ffffff;
  --bot-text: #1b2330;

  --accent: #667cff;
}

/* 🔥 메인 래퍼: 부모 영역(헤더~탭바 사이)을 꽉 채우고
   안에서 위는 채팅, 아래는 입력창으로 flex 분리 */
.chatbot-wrap {
  display: flex;
  flex-direction: column;
  max-width: 420px;
  margin: 0 auto;
  background: linear-gradient(180deg, #e8f9ff 0%, #f0eaff 100%);
  min-height: 100%;          /* 부모 높이만큼 꽉 채우기 */
}

/* 대화영역(스크롤만 담당) */
.chat-area {
  flex: 1 1 auto;            /* 남는 공간 전부 사용 */
  min-height: 0;             /* flex 안에서 스크롤 되게 필수 */
  overflow-y: auto;
  padding: 12px 12px 16px;   /* 아래 너무 크지 않게 */
  backdrop-filter: blur(6px);
}

.chat-area::-webkit-scrollbar {
  width: 0;
  height: 0;
}

/* Tip */
.tip {
  position: sticky;
  top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0 14px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px dashed rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
  animation: fadeIn 0.25s ease-out;
}

.bulb {
  font-size: 16px;
}

.tip-text {
  flex: 1;
}

.tip-close {
  all: unset;
  cursor: pointer;
  font-size: 14px;
  opacity: 0.55;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* 말풍선 공통 */
.bubble {
  max-width: 78%;
  padding: 12px 14px;
  border-radius: 16px;
  font-size: 16px;
  line-height: 1.6;
  letter-spacing: 0.1px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
}

/* 봇 버블 */
.row.bot {
  justify-content: flex-start;
}

.row.bot .bubble {
  background: var(--bot-bg);
  color: var(--bot-text);
  border-top-left-radius: 10px;
}

/* 사용자 버블 */
.row.me {
  justify-content: flex-end;
}

.row.me .bubble {
  background: var(--me-bg);
  color: var(--me-text);
  border: 1px solid var(--me-border);
  border-top-right-radius: 10px;
  font-weight: 600;
}

/* 줄 */
.row {
  display: flex;
  gap: 8px;
  margin: 10px 0;
  align-items: flex-end;
}

/* 아바타 */
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 25px;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  border: 2px solid #ffffff;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 🔥 입력바: flex 맨 아래에 고정 (position 안 씀) */
.inputbar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
  height: var(--input-h);
  padding: 12px 12px 12px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.icon-btn,
.mic-btn {
  all: unset;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  cursor: pointer;
  background: #fff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  font-size: 18px;
}

.mic-btn.on {
  outline: 3px solid color-mix(in oklab, var(--accent) 35%, transparent);
}

.textbox {
  flex: 1;
  height: 40px;
  border: 0;
  outline: none;
  background: #fff;
  border-radius: 12px;
  padding: 0 12px;
  font-size: 15px;
}

/* 로딩 타이핑 점 */
.typing {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #b9c3d3;
  display: inline-block;
  animation: blink 1.1s infinite;
}

.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.2;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-2px);
  }
}
</style>

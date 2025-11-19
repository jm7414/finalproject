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

    <!-- 하단 입력 바 (뷰 내부 고정) -->
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
import grandsonAvatar from '@/assets/images/grand.png'

const API_BASE = ''

const messages = ref([])
const draft = ref('')
const loading = ref(false)
const scrollRef = ref(null)

const TIPS = [
  '“도움말”을 입력하면 사용 가능한 명령을 알려드려요.',
  '음성 버튼을 눌러 “오늘 기록 시작”처럼 말해보세요.',
  '“일정보여줘”라고 입력하면 일정 화면으로 이동할 수 있어요.',
]
const showTip = ref(true)
const tipText = TIPS[Math.floor(Math.random() * TIPS.length)]

const scrollToBottom = async () => {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

onMounted(scrollToBottom)

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

const onQuickAction = () => {
  draft.value = '일정보여줘'
}

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
  --me-bg: #eef1ff;
  --me-text: #0b1f33;
  --me-border: #c9d2ff;

  --bot-bg: #ffffff;
  --bot-text: #1b2330;

  --accent: #667cff;
}

/* ▶ 전체 래퍼: 뷰포트 기준 고정 높이 */
.chatbot-wrap {
  position: relative;
  max-width: 420px;
  margin: 0 auto;
  height: 100%;
  background: linear-gradient(180deg, #e8f9ff 0%, #f0eaff 100%);
  padding-top: 8px;
  padding-bottom: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;            /* 래퍼는 더 이상 커지지 않게 */
}

/* 대화영역만 스크롤 */
.chat-area {
  flex: 1 1 auto;
  min-height: 10;
  overflow-y: auto;
  padding: 12px 12px 110px;    /* 입력창 높이만큼 여유 */
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

/* 메시지 줄 */
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
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 입력바: 항상 같은 자리 (탭바 위) */
.inputbar {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  box-sizing: border-box;

  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 999px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  z-index: 5;
}

.icon-btn,
.mic-btn {
  all: unset;
  width: 40px;
  height: 40px;
  border-radius: 50%;
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
  border-radius: 999px;
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

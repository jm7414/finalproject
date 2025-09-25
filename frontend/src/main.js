import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 카카오 api 연결용
import { createApp } from 'vue'
import App from './App.vue'
import { useKakao } from 'vue3-kakao-maps/@utils'

useKakao('110dada163e51c3af92f7eebc8de2440')

createApp(App).mount('#app')

app.use(createPinia())
app.use(router)

app.mount('#app')

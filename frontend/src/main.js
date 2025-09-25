
import 'bootstrap/dist/css/bootstrap.min.css'
import './assets/main.css'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Vuetify
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { aliases, mdi } from 'vuetify/iconsets/mdi'

// Pinia 스토어 (파일 존재: src/stores/app.js)
import { useAppStore } from './stores/app'

// ----- 인스턴스 생성 -----
const pinia = createPinia()
const vuetify = createVuetify({
  components,
  directives,
  icons: { defaultSet: 'mdi', aliases, sets: { mdi } },
})

// 라우터 가드: 헤더 표시/숨김 플래그
router.beforeEach((to) => {
  const app = useAppStore()
  app.main = !!to.meta.hideHeader
  return true
})

// 앱 부트스트랩: use 전부 등록 후 한 번만 mount
createApp(App)
  .use(pinia)
  .use(router)
  .use(vuetify)
  .mount('#app')

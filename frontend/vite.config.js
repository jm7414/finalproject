import fs from 'fs'
import path from 'path'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// ES 모듈에서 __dirname 대체
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,  // Vite 기본 HTTPS 포트 유지
    // 개발 환경에서만 HTTPS 인증서 사용 (프로덕션 빌드 시에는 필요 없음)
    ...(process.env.NODE_ENV !== 'production' && {
      https: {
        key: fs.readFileSync(
          path.resolve(__dirname, '../backend/src/main/resources/localhost+1-key.pem')
        ),
        cert: fs.readFileSync(
          path.resolve(__dirname, '../backend/src/main/resources/localhost+1.pem')
        ),
      },
    }),
    proxy: {
      '/api': {
        target: process.env.VITE_API_BASE_URL || 'https://localhost:8080',  // 환경변수 사용
        changeOrigin: true,
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('proxy error', err);
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('Sending Request to the Target:', req.method, req.url);
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
          });
        },
      },
      '/NH/api': {
        target: process.env.VITE_API_BASE_URL || 'https://localhost:8080',  // 환경변수 사용
        changeOrigin: true,
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('proxy error', err);
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('Sending Request to the Target:', req.method, req.url);
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
          });
        },
      },
      '/uploads': {
        target: process.env.VITE_API_BASE_URL || 'https://localhost:8080',  // 업로드된 이미지 파일
        changeOrigin: true,
        secure: false,
      },
    },
  },
})

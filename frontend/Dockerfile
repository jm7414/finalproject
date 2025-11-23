# frontend/Dockerfile
# 빌드 단계
FROM node:20-alpine as build

WORKDIR /app

# package.json과 package-lock.json 복사
COPY package*.json ./

# 의존성 설치
RUN npm ci

# 소스 코드 복사
COPY . .

# 빌드 인자 정의 (빌드 시 전달받음)
ARG VITE_API_BASE_URL
ARG VITE_FASTAPI_URL
ARG VITE_KAKAO_JS_KEY

# 환경변수로 설정 (빌드 시 사용)
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_FASTAPI_URL=$VITE_FASTAPI_URL
ENV VITE_KAKAO_JS_KEY=$VITE_KAKAO_JS_KEY

# 애플리케이션 빌드
RUN npm run build

# 프로덕션 단계
FROM nginx:alpine

# 빌드된 파일들을 nginx로 복사
COPY --from=build /app/dist /usr/share/nginx/html

# nginx 설정 복사 (SPA 라우팅 지원)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 포트 노출
EXPOSE 80

# nginx 실행
CMD ["nginx", "-g", "daemon off;"]

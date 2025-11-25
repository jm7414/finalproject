#!/bin/sh
set -e

# 백엔드 URL 환경변수 확인
if [ -z "$BACKEND_URL" ]; then
  echo "⚠️  BACKEND_URL 환경변수가 설정되지 않았습니다. 기본값을 사용합니다."
  BACKEND_URL="https://localhost:8080"
fi

echo "✅ 백엔드 URL: $BACKEND_URL"

# nginx.conf 템플릿에서 환경변수 치환
envsubst '${BACKEND_URL}' < /nginx.conf.template > /etc/nginx/conf.d/default.conf

echo "✅ 생성된 nginx.conf 내용:"
cat /etc/nginx/conf.d/default.conf

echo "✅ nginx 설정 파일 생성 완료"

# nginx 실행
exec nginx -g 'daemon off;'


# python/Dockerfile
FROM python:3.11-slim

WORKDIR /app

# 시스템 패키지 설치 (libexpat 등)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libexpat1 \
 && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY FastAPI_v2.py .
COPY all_locations.csv .
COPY gps_trajectory_1year.csv .
COPY gps_trajectory_1year_later.csv .

EXPOSE 8000
CMD ["python", "FastAPI_v2.py"]
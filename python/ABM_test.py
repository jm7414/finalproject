from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
from fastapi.middleware.cors import CORSMiddleware
import numpy as np
from sklearn.cluster import DBSCAN
from datetime import datetime
from collections import defaultdict

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class GPSRawPoint(BaseModel):
    userNo: int
    latitude: float
    longitude: float
    recordTime: str

def extract_speed_segments(gps_points):
    speeds = []
    for i in range(len(gps_points) - 1):
        prev, curr = gps_points[i], gps_points[i+1]
        t0 = datetime.fromisoformat(prev["recordTime"])
        t1 = datetime.fromisoformat(curr["recordTime"])
        dt = max(1, (t1 - t0).total_seconds())
        dx = np.radians(curr["longitude"] - prev["longitude"]) * 6371000 * np.cos(np.radians((curr["latitude"] + prev["latitude"])/2))
        dy = np.radians(curr["latitude"] - prev["latitude"]) * 6371000
        dist = np.sqrt(dx**2 + dy**2)
        speed = dist / dt
        speeds.append(speed)
    return np.array(speeds)

def extract_speed_feature(gps_points):
    speeds = extract_speed_segments(gps_points)
    return float(np.mean(speeds)), float(np.std(speeds))

def cluster_locations(gps_points, eps=100):
    coords = np.array([[p["latitude"], p["longitude"]] for p in gps_points])
    # eps in degrees approx (0.001 ~ 111m)
    clustering = DBSCAN(eps=0.001, min_samples=3).fit(coords)
    clusters = []
    for label in set(clustering.labels_):
        if label == -1:
            continue
        indices = np.where(clustering.labels_ == label)[0]
        cluster_center = coords[indices].mean(axis=0)
        clusters.append({
            "latitude": float(cluster_center[0]),
            "longitude": float(cluster_center[1]),
            "count": int(len(indices))
        })
    return clusters

@app.post("/api/dementia/set-agent-rule")
def set_agent_rule(gps_data: List[GPSRawPoint]):
    # userNo별 gps 포인트 그룹핑
    user_dict = defaultdict(list)
    for p in gps_data:
        user_dict[p.userNo].append(p.dict())

    results = []
    for user_no, points in user_dict.items():
        if len(points) < 2:
            results.append({"userNo": user_no, "msg": "2개 이상의 gps포인트가 필요합니다."})
            continue

        # 시간순 정렬 필요
        points.sort(key=lambda x: x["recordTime"])

        speed_mean, speed_std = extract_speed_feature(points)
        clusters = cluster_locations(points)

        directions = []
        for i in range(len(points)-1):
            dx = points[i+1]["longitude"] - points[i]["longitude"]
            dy = points[i+1]["latitude"] - points[i]["latitude"]
            angle = np.arctan2(dy, dx)
            directions.append(angle)
        if directions:
            direction_mean = float(np.mean(directions))
            direction_std = float(np.std(directions))
        else:
            direction_mean = 0.0
            direction_std = 0.0

        results.append({
            "userNo": user_no,
            "speed_mean_mps": speed_mean,
            "speed_std_mps": speed_std,
            "direction_mean_rad": direction_mean,
            "direction_std_rad": direction_std,
            "frequent_places": clusters
        })

    return results
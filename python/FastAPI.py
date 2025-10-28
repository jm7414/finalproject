from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
import pandas as pd
import numpy as np
from sklearn.cluster import DBSCAN
from sklearn.ensemble import RandomForestClassifier
import uvicorn
import math
import random
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from concurrent.futures import ProcessPoolExecutor
import asyncio
from functools import lru_cache

# ⭐ 접근성 분석 라이브러리
import osmnx as ox
import networkx as nx
import srtm
from geopy.distance import geodesic

# ============= 전역 변수 =============
DATA_CACHE = {}
ELEVATION_CACHE = {}
ROAD_NETWORK_CACHE = {}
executor = ProcessPoolExecutor(max_workers=3)

# ============= 상수 정의 =============
HOME_EXCLUSION_RADIUS_M = 50
MAX_WAYPOINTS = 5
ROUTE_NETWORK_RADIUS = 3000

# 접근성 파라미터
SLOPE_THRESHOLD_EASY = 0.05
SLOPE_THRESHOLD_MODERATE = 0.10
SLOPE_THRESHOLD_HARD = 0.15

# ============= SRTM 초기화 =============
try:
    elevation_data = srtm.get_data()
    print("SRTM elevation data initialized successfully")
except Exception as e:
    print(f"Warning: SRTM initialization failed: {e}")
    elevation_data = None   

# ============= Lifespan =============
@asynccontextmanager
async def lifespan(app: FastAPI):
    print("Loading CSV data...")
    DATA_CACHE['df'] = pd.read_csv('road_based_gps_data.csv')
    DATA_CACHE['df']['timestamp'] = pd.to_datetime(DATA_CACHE['df']['timestamp'])
    print(f"Loaded {len(DATA_CACHE['df'])} records")
    
    # 도로 네트워크 사전 로딩
    try:
        center_lat = DATA_CACHE['df']['latitude'].mean()
        center_lon = DATA_CACHE['df']['longitude'].mean()
        load_road_network(center_lat, center_lon, radius=5000)
        print("Road network loaded successfully")
    except Exception as e:
        print(f"Warning: Could not pre-load road network: {e}")
    
    yield
    DATA_CACHE.clear()
    ELEVATION_CACHE.clear()
    ROAD_NETWORK_CACHE.clear()
    executor.shutdown()

app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============= Pydantic Schemas =============
class PredictionRequest(BaseModel):
    user_no: int
    missing_time: str
    home_lat: Optional[float] = None
    home_lon: Optional[float] = None
    last_lat: Optional[float] = None
    last_lon: Optional[float] = None

class WaypointInfo(BaseModel):
    lat: float
    lon: float
    probability: float
    cluster_size: Optional[int] = None

class LocationPoint(BaseModel):
    lat: float
    lon: float
    value: float
    accessibility_score: Optional[float] = None
    waypoints: Optional[List[WaypointInfo]] = None
    zone_level: Optional[str] = None

class PredictionMetadata(BaseModel):
    total_points: int
    dbscan_clusters: int
    random_candidates: int
    range_50_700_count: int
    range_700_1500_count: int
    range_1500_plus_count: int
    max_prob: float
    mean_prob: float
    confidence_score: float
    prediction_quality: str
    missing_center_lat: float
    missing_center_lon: float
    total_waypoints_generated: Optional[int] = None

class PredictionResponse(BaseModel):
    metadata: PredictionMetadata
    zone_level_1: List[LocationPoint]
    zone_level_2: List[LocationPoint]
    zone_level_3: List[LocationPoint]

# ============= 유틸 함수 =============
EARTH_M_PER_DEG = 111000.0

def deg_distance_m(lat1, lon1, lat2, lon2):
    return math.sqrt((lat1 - lat2) ** 2 + (lon1 - lon2) ** 2) * EARTH_M_PER_DEG

def meters_to_deg(m):
    return m / EARTH_M_PER_DEG

@lru_cache(maxsize=128)
def get_time_based_random_ratio(missing_hour: int) -> float:
    if 16 <= missing_hour <= 20:
        return 0.5
    elif missing_hour > 20 or missing_hour < 6:
        return 0.4
    else:
        return 0.25

@lru_cache(maxsize=128)
def get_random_base_prob(missing_hour: int) -> float:
    if missing_hour >= 18 or missing_hour < 9:
        return 0.25
    else:
        return 0.15

# ============= 접근성 함수 (SRTM + OSMnx) =============
def load_road_network(center_lat: float, center_lon: float, radius: int = 2000) -> nx.MultiDiGraph:
    cache_key = f"{center_lat:.4f}_{center_lon:.4f}_{radius}"
    
    if cache_key in ROAD_NETWORK_CACHE:
        return ROAD_NETWORK_CACHE[cache_key]
    
    try:
        G = ox.graph_from_point(
            (center_lat, center_lon), 
            dist=radius, 
            network_type='all',
            simplify=True,
            retain_all=True
        )
        ROAD_NETWORK_CACHE[cache_key] = G
        return G
    except Exception as e:
        print(f"Error loading road network: {e}")
        return None

def get_elevation(lat: float, lon: float) -> Optional[float]:
    if elevation_data is None:
        return None
    
    cache_key = f"{lat:.6f}_{lon:.6f}"
    if cache_key in ELEVATION_CACHE:
        return ELEVATION_CACHE[cache_key]
    
    try:
        elev = elevation_data.get_elevation(lat, lon)
        ELEVATION_CACHE[cache_key] = elev
        return elev
    except:
        return None

def calculate_slope(lat1: float, lon1: float, lat2: float, lon2: float) -> Optional[float]:
    elev1 = get_elevation(lat1, lon1)
    elev2 = get_elevation(lat2, lon2)
    
    if elev1 is None or elev2 is None:
        return None
    
    distance_m = geodesic((lat1, lon1), (lat2, lon2)).meters
    if distance_m == 0:
        return 0.0
    
    elevation_diff = abs(elev2 - elev1)
    slope = elevation_diff / distance_m
    return slope

def get_accessibility_score(lat: float, lon: float, center_lat: float, center_lon: float) -> float:
    accessibility = 1.0
    
    # 경사도 평가
    slope = calculate_slope(center_lat, center_lon, lat, lon)
    if slope is not None:
        if slope <= SLOPE_THRESHOLD_EASY:
            slope_score = 1.0
        elif slope <= SLOPE_THRESHOLD_MODERATE:
            slope_score = 0.7
        elif slope <= SLOPE_THRESHOLD_HARD:
            slope_score = 0.4
        else:
            slope_score = 0.2
        accessibility *= slope_score
    
    # 도로 접근성 평가
    G = load_road_network(center_lat, center_lon, radius=ROUTE_NETWORK_RADIUS)
    if G is not None:
        try:
            nearest_node = ox.distance.nearest_nodes(G, lon, lat)
            node_data = G.nodes[nearest_node]
            node_lat, node_lon = node_data['y'], node_data['x']
            dist_to_road = geodesic((lat, lon), (node_lat, node_lon)).meters
            
            if dist_to_road < 50:
                road_score = 1.0
            elif dist_to_road < 150:
                road_score = 0.8
            elif dist_to_road < 300:
                road_score = 0.5
            else:
                road_score = 0.3
            
            accessibility *= road_score
        except:
            pass
    
    return accessibility

# ============= 벡터화된 visit_count =============
def calculate_visit_counts_vectorized(range_df, threshold_deg=meters_to_deg(50)):
    coords = range_df[['latitude', 'longitude']].values
    lat_diff = coords[:, 0].reshape(-1, 1) - coords[:, 0].reshape(1, -1)
    lon_diff = coords[:, 1].reshape(-1, 1) - coords[:, 1].reshape(1, -1)
    distances = np.sqrt(lat_diff ** 2 + lon_diff ** 2)
    visit_counts = (distances < threshold_deg).sum(axis=1)
    return visit_counts

# ============= 경유지 생성 함수 =============
def generate_waypoints(start_lat: float, start_lon: float, 
                      end_lat: float, end_lon: float,
                      zone_level: str,
                      all_clusters: List[Dict[str, Any]]) -> List[WaypointInfo]:
    waypoints = []
    total_distance = geodesic((start_lat, start_lon), (end_lat, end_lon)).meters
    
    candidates = []
    for cluster in all_clusters:
        if abs(cluster['lat'] - end_lat) < 0.00001 and abs(cluster['lon'] - end_lon) < 0.00001:
            continue
        
        cluster_lat = cluster['lat']
        cluster_lon = cluster['lon']
        
        dist_from_start = geodesic((start_lat, start_lon), (cluster_lat, cluster_lon)).meters
        dist_to_end = geodesic((cluster_lat, cluster_lon), (end_lat, end_lon)).meters
        path_deviation = (dist_from_start + dist_to_end) - total_distance
        
        if path_deviation < total_distance * 0.2:
            path_fitness = 1.0 - (path_deviation / (total_distance * 0.2))
            combined_score = cluster.get('value', 0.5) * 0.6 + path_fitness * 0.4
            
            candidates.append({
                'lat': cluster_lat,
                'lon': cluster_lon,
                'probability': cluster.get('value', 0.5),
                'cluster_size': cluster.get('cluster_size', 1),
                'dist_from_start': dist_from_start,
                'combined_score': combined_score
            })
    
    candidates.sort(key=lambda x: x['combined_score'], reverse=True)
    selected_candidates = candidates[:MAX_WAYPOINTS]
    selected_candidates.sort(key=lambda x: x['dist_from_start'])
    
    for candidate in selected_candidates:
        waypoints.append(WaypointInfo(
            lat=candidate['lat'],
            lon=candidate['lon'],
            probability=candidate['probability'],
            cluster_size=candidate.get('cluster_size')
        ))
    
    return waypoints

# ============= 거리 범위 처리 (병렬용) =============
def process_distance_range(user_df, last_lat, last_lon, home_lat, home_lon, min_dist_m, max_dist_m, missing_hour):
    """병렬 처리용 거리 구간 분석"""
    
    # 벡터화된 거리 계산
    distances = np.sqrt((user_df['latitude'].values - last_lat) ** 2 + 
                       (user_df['longitude'].values - last_lon) ** 2) * EARTH_M_PER_DEG
    range_mask = (distances >= min_dist_m) & (distances <= max_dist_m)
    range_df = user_df[range_mask].copy()
    
    if len(range_df) < 3:
        return []
    
    # 벡터화된 특징 생성
    range_df['dist_last'] = np.sqrt((range_df['latitude'] - last_lat) ** 2 + 
                                    (range_df['longitude'] - last_lon) ** 2)
    range_df['dist_from_home'] = np.sqrt((range_df['latitude'] - home_lat) ** 2 + 
                                         (range_df['longitude'] - home_lon) ** 2)
    
    lat_diff = range_df['latitude'].diff().fillna(0.0).values
    lon_diff = range_df['longitude'].diff().fillna(0.0).values
    range_df['move_distance'] = np.sqrt(lat_diff ** 2 + lon_diff ** 2)
    range_df['cumulative_distance'] = range_df['move_distance'].cumsum()
    range_df['visit_count'] = calculate_visit_counts_vectorized(range_df)
    range_df['direction'] = np.arctan2(lat_diff, lon_diff)
    
    # DBSCAN
    X = range_df[['latitude', 'longitude']].values
    
    if max_dist_m <= 700:
        eps = meters_to_deg(10)
        min_samples = 3
    elif max_dist_m <= 1500:
        eps = meters_to_deg(15)
        min_samples = 3
    else:
        eps = meters_to_deg(20)
        min_samples = 2
    
    dbscan = DBSCAN(eps=eps, min_samples=min_samples, algorithm='ball_tree', n_jobs=-1)
    labels = dbscan.fit_predict(X)
    range_df['cluster'] = labels
    
    valid_labels = [lbl for lbl in set(labels) if lbl != -1]
    cluster_centroids = []
    cluster_info = {}
    
    for lbl in valid_labels:
        cluster_mask = range_df['cluster'] == lbl
        cluster_pts = range_df[cluster_mask]
        c_lat, c_lon = float(cluster_pts['latitude'].mean()), float(cluster_pts['longitude'].mean())
        cluster_centroids.append((c_lat, c_lon))
        cluster_info[lbl] = {"centroid": (c_lat, c_lon), "size": int(cluster_mask.sum())}
    
    n_clusters = len(cluster_centroids)
    if n_clusters == 0:
        return []
    
    # RF 특징 생성
    n_distance_features = min(5, n_clusters)
    for i in range(n_distance_features):
        c_lat, c_lon = cluster_centroids[i]
        range_df[f'dist_to_cluster_{i}'] = np.sqrt(
            (range_df['latitude'] - c_lat) ** 2 + (range_df['longitude'] - c_lon) ** 2
        )
    
    rf_features = [
        'dist_last', 'dist_from_home', 'move_distance',
        'cumulative_distance', 'visit_count', 'direction',
        'latitude', 'longitude'
    ] + [f'dist_to_cluster_{i}' for i in range(n_distance_features)]
    
    valid_data = range_df[range_df['cluster'] != -1]
    if len(valid_data) < 2:
        return []
    
    # RF 훈련
    y = valid_data['cluster']
    rf = RandomForestClassifier(
        n_estimators=50,
        random_state=42,
        max_depth=10,
        n_jobs=-1
    )
    rf.fit(valid_data[rf_features], y)
    
    # 클러스터 예측
    cluster_predictions = []
    cumulative_dist = float(range_df['cumulative_distance'].iloc[-1] if len(range_df) > 0 else 0)
    
    for i, (c_lat, c_lon) in enumerate(cluster_centroids):
        cluster_label = valid_labels[i]
        cluster_size = cluster_info[cluster_label]['size']
        dist_m = deg_distance_m(c_lat, c_lon, last_lat, last_lon)
        
        # Zone 레벨 결정
        if dist_m < 700:
            zone_level = 'zone_level_1'
        elif dist_m < 1500:
            zone_level = 'zone_level_2'
        else:
            zone_level = 'zone_level_3'
        
        # RF 예측을 위한 Feature
        feat_dict = {
            'dist_last': dist_m / EARTH_M_PER_DEG,
            'dist_from_home': np.sqrt((c_lat - home_lat) ** 2 + (c_lon - home_lon) ** 2),
            'move_distance': 0.0,
            'cumulative_distance': cumulative_dist,
            'visit_count': float(cluster_size),
            'direction': 0.0,
            'latitude': c_lat,
            'longitude': c_lon
        }
        
        for j in range(n_distance_features):
            feat_dict[f'dist_to_cluster_{j}'] = np.sqrt(
                (c_lat - cluster_centroids[j][0]) ** 2 + (c_lon - cluster_centroids[j][1]) ** 2
            )
        
        feat = pd.DataFrame([feat_dict])[rf_features]
        proba_array = rf.predict_proba(feat)[0]
        predicted_cluster = int(np.argmax(proba_array))
        rf_confidence = float(proba_array[predicted_cluster])
        
        # ⭐ 접근성 점수 계산
        accessibility = get_accessibility_score(c_lat, c_lon, last_lat, last_lon)
        
        # ⭐ 최종 확률 계산 (RF + 접근성 + 거리)
        distance_weight = math.exp(-dist_m / 1000.0)
        visit_bonus = cluster_size / max(1, len(range_df))
        
        rf_prob = rf_confidence * distance_weight * (1.0 + visit_bonus)
        final_prob = rf_prob * 0.6 + accessibility * 0.4
        
        # 0.3 ~ 0.85 범위로 제한
        final_prob = max(0.3, min(0.85, final_prob))
        
        cluster_predictions.append({
            'lat': c_lat,
            'lon': c_lon,
            'dist_m': dist_m,
            'value': round(final_prob, 2),
            'accessibility_score': round(accessibility, 2),
            'cluster_size': cluster_size,
            'zone_level': zone_level
        })
    
    return cluster_predictions

# ============= 메인 예측 API =============
@app.post("/api/predict-location", response_model=PredictionResponse)
async def predict_location(request: PredictionRequest):
    """
    통합 예측 API:
    - 병렬 처리로 속도 최적화
    - RF 기반 확률 예측
    - 접근성 점수 (경사도 + 도로망)
    - 경유지 생성 (최대 5개)
    - Zone별 상위 10개 필터링
    """
    df = DATA_CACHE.get('df')
    if df is None or df.empty:
        raise Exception("Data not loaded")
    
    user_data = df[df['user_no'] == request.user_no].copy()
    if user_data.empty:
        raise Exception(f"No data found for user {request.user_no}")
    
    missing_dt = pd.to_datetime(request.missing_time)
    missing_hour = missing_dt.hour
    
    # 실종 위치 결정
    user_data['time_diff'] = (user_data['timestamp'] - missing_dt).abs()
    closest_record = user_data.loc[user_data['time_diff'].idxmin()]
    
    last_lat = closest_record['latitude']
    last_lon = closest_record['longitude']
    
    # 집 위치
    home_lat = request.home_lat if request.home_lat else user_data.iloc[0]['latitude']
    home_lon = request.home_lon if request.home_lon else user_data.iloc[0]['longitude']
    
    # 과거 데이터만 사용
    user_data = user_data[user_data['timestamp'] < missing_dt].copy()
    if user_data.empty:
        raise Exception("No historical data found")
    
    print(f"Processing {len(user_data)} GPS records for user {request.user_no}")
    
    # ⭐ 병렬 처리로 3개 범위 동시 분석
    distance_ranges = [(50, 700), (700, 1500), (1500, 3000)]
    
    loop = asyncio.get_event_loop()
    tasks = [
        loop.run_in_executor(
            executor,
            process_distance_range,
            user_data, last_lat, last_lon, home_lat, home_lon,
            min_dist, max_dist, missing_hour
        )
        for min_dist, max_dist in distance_ranges
    ]
    
    results = await asyncio.gather(*tasks)
    
    # 결과 수집
    all_clusters = []
    for result in results:
        all_clusters.extend(result)
    
    print(f"Total clusters before filtering: {len(all_clusters)}")
    
    # 집 근처 제외
    excluded_count = 0
    filtered_clusters = []
    for cluster in all_clusters:
        dist_from_home = deg_distance_m(cluster['lat'], cluster['lon'], home_lat, home_lon)
        if dist_from_home < HOME_EXCLUSION_RADIUS_M:
            excluded_count += 1
            continue
        filtered_clusters.append(cluster)
    
    all_clusters = filtered_clusters
    print(f"After home exclusion: {len(all_clusters)} clusters (excluded: {excluded_count})")
    
    # ⭐ 경유지 생성
    total_waypoints = 0
    for cluster in all_clusters:
        waypoints = generate_waypoints(
            last_lat, last_lon,
            cluster['lat'], cluster['lon'],
            cluster['zone_level'],
            all_clusters
        )
        cluster['waypoints'] = waypoints
        total_waypoints += len(waypoints)
    
    # 확률 기준 정렬
    all_clusters.sort(key=lambda x: x['value'], reverse=True)
    
    # Zone별 분류 및 상위 10개 선택
    zone_level_1 = [c for c in all_clusters if c['zone_level'] == 'zone_level_1'][:10]
    zone_level_2 = [c for c in all_clusters if c['zone_level'] == 'zone_level_2'][:10]
    zone_level_3 = [c for c in all_clusters if c['zone_level'] == 'zone_level_3'][:10]
    
    print(f"Final clusters: Zone1={len(zone_level_1)}, Zone2={len(zone_level_2)}, Zone3={len(zone_level_3)}")
    
    # 메타데이터 생성
    filtered_final = zone_level_1 + zone_level_2 + zone_level_3
    
    if filtered_final:
        avg_prob = np.mean([c['value'] for c in filtered_final])
        if avg_prob > 0.7:
            quality = "high"
        elif avg_prob > 0.5:
            quality = "medium"
        else:
            quality = "low"
        confidence = min(0.95, avg_prob * 1.2)
    else:
        avg_prob = 0.0
        quality = "low"
        confidence = 0.3
    
    metadata = PredictionMetadata(
        total_points=len(filtered_final),
        dbscan_clusters=len(all_clusters),
        random_candidates=0,
        range_50_700_count=len(zone_level_1),
        range_700_1500_count=len(zone_level_2),
        range_1500_plus_count=len(zone_level_3),
        max_prob=max([c['value'] for c in filtered_final]) if filtered_final else 0,
        mean_prob=avg_prob,
        confidence_score=confidence,
        prediction_quality=quality,
        missing_center_lat=last_lat,
        missing_center_lon=last_lon,
        total_waypoints_generated=total_waypoints
    )
    
    return PredictionResponse(
        metadata=metadata,
        zone_level_1=[LocationPoint(**loc) for loc in zone_level_1],
        zone_level_2=[LocationPoint(**loc) for loc in zone_level_2],
        zone_level_3=[LocationPoint(**loc) for loc in zone_level_3]
    )

@app.get("/")
async def root():
    return {
        "status": "ok",
        "message": "Integrated Missing Person Location Prediction API",
        "features": [
            "Parallel processing for speed",
            "Random Forest based probability",
            "Accessibility scoring (slope + road network)",
            "Waypoint generation (max 5 per cluster)",
            "Zone-based top-10 filtering"
        ]
    }

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "cached_records": len(DATA_CACHE.get('df', [])),
        "elevation_cache_size": len(ELEVATION_CACHE),
        "road_network_cache_size": len(ROAD_NETWORK_CACHE)
    }

if __name__ == "__main__":
    uvicorn.run("final_predict_api:app", host="0.0.0.0", port=8000, workers=4)
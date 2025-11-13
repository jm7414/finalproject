"""
Missing Person Destination Prediction API - Simplified Version
실종자 목적지 예측 시스템 (Spring Boot 연동)

Features:
- BallTree 기반 빠른 도로 노드 스냅
- 시간대별 패턴 분석
- 선호 경로 반영
- 지리적 분산 고려
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Dict, Optional
from datetime import datetime, timedelta
import os
import sys
import numpy as np
import pandas as pd
import hdbscan
import asyncio
from concurrent.futures import ThreadPoolExecutor
from collections import Counter
import osmnx as ox
import networkx as nx
from sklearn.neighbors import BallTree

# =============================================================================
# Configuration
# =============================================================================

os.environ['LC_ALL'] = 'C.UTF-8'
os.environ['LANG'] = 'C.UTF-8'
os.environ['PYTHONIOENCODING'] = 'utf-8'

# Windows 전용: 임시 폴더를 영문 경로로 지정
import tempfile
if sys.platform == 'win32':
    temp_dir = 'C:/temp'
    if not os.path.exists(temp_dir):
        os.makedirs(temp_dir)
    os.environ['TEMP'] = temp_dir
    os.environ['TMP'] = temp_dir
    tempfile.tempdir = temp_dir
    print(f"✅ 임시 폴더 설정: {temp_dir}")

executor = ThreadPoolExecutor(max_workers=4)

# Caches
ROAD_NETWORK_CACHE = {}
BALLTREE_CACHE = {}

# =============================================================================
# FastAPI App
# =============================================================================

app = FastAPI(
    title="실종자 목적지 예측 API",
    description="Spring Boot 연동 실종자 경로 예측 시스템",
    version="12.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =============================================================================
# Pydantic Models
# =============================================================================

class GPSRecord(BaseModel):
    latitude: float
    longitude: float
    record_time: str  # "YYYY-MM-DD HH:MM:SS"


class PredictionRequest(BaseModel):
    user_no: int
    missing_time: str  # "YYYY-MM-DD HH:MM"
    gps_data: List[GPSRecord]
    analysis_days: Optional[int] = 120
    time_window_hours: Optional[int] = 3
    session_gap: Optional[int] = 30
    min_cluster_size: Optional[int] = 10
    max_search_radius: Optional[int] = 2000
    min_cluster_separation: Optional[int] = 200
    road_network_radius: Optional[int] = 2500
    csv_path: Optional[str] = "all_locations.csv"


class Waypoint(BaseModel):
    lat: float
    lon: float
    node_id: Optional[int] = None


class Destination(BaseModel):
    destination_id: int
    latitude: float
    longitude: float
    visit_count: int
    total_gps_records: int
    distance_meters: float
    cluster_stability: float
    waypoints: List[Waypoint]
    preference_score: float
    route_method: str
    name: Optional[str] = None


class LastKnownLocation(BaseModel):
    latitude: float
    longitude: float
    time: str


class PredictionResponse(BaseModel):
    user_no: int
    missing_time: str
    last_known_location: LastKnownLocation
    analysis_period_days: int
    session_gap_minutes: int
    time_filtered_records: int
    total_clusters_found: int
    destinations_by_distance: Dict[str, List[Destination]]
    data_sufficiency: str = Field(description="데이터 충분성 (yes/no/nono)")
    total_gps_records: int = Field(description="전체 GPS 레코드 수")


# =============================================================================
# Utility Functions
# =============================================================================

def haversine_distance(lat1, lon1, lat2, lon2):
    """Haversine 거리 계산 (미터)"""
    R = 6371000
    lat1, lon1, lat2, lon2 = map(np.radians, [lat1, lon1, lat2, lon2])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = np.sin(dlat/2)**2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon/2)**2
    c = 2 * np.arcsin(np.sqrt(a))
    return R * c


def get_road_network_sync(center_lat, center_lon, radius_m=2000):
    """OSMnx 도로망 다운로드 (동기, 캐시)"""
    cache_key = f"{center_lat:.4f}_{center_lon:.4f}_{radius_m}"
    
    if cache_key in ROAD_NETWORK_CACHE:
        print(f"  ✅ 캐시된 도로망 사용")
        return ROAD_NETWORK_CACHE[cache_key]
    
    try:
        print(f"  🌐 OSMnx 도로망 다운로드... (반경 {radius_m}m)")
        G = ox.graph_from_point(
            (center_lat, center_lon),
            dist=radius_m,
            network_type='walk',
            simplify=True
        )
        ROAD_NETWORK_CACHE[cache_key] = G
        print(f"  ✅ 완료: {G.number_of_nodes()}개 노드")
        return G
    except Exception as e:
        print(f"  ⚠️ OSMnx 실패: {e}")
        return None


async def get_road_network(center_lat, center_lon, radius_m=2000):
    """비동기 버전"""
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(
        executor,
        get_road_network_sync,
        center_lat, center_lon, radius_m
    )


def build_balltree(G):
    """BallTree 인덱스 구축 (빠른 최근접 노드 검색)"""
    cache_key = id(G)
    
    if cache_key in BALLTREE_CACHE:
        return BALLTREE_CACHE[cache_key]
    
    nodes = []
    coords = []
    
    for node, data in G.nodes(data=True):
        nodes.append(node)
        coords.append([np.radians(data['y']), np.radians(data['x'])])
    
    coords = np.array(coords)
    tree = BallTree(coords, metric='haversine')
    
    result = (tree, nodes, coords)
    BALLTREE_CACHE[cache_key] = result
    
    return result


def snap_to_nearest_node_fast(G, tree_data, lat, lon):
    """BallTree로 빠른 노드 스냅"""
    if G is None or tree_data is None:
        return None
    
    try:
        tree, nodes, coords = tree_data
        query_point = np.radians([[lat, lon]])
        dist, idx = tree.query(query_point, k=1)
        
        nearest_idx = idx[0][0]
        nearest_node = nodes[nearest_idx]
        
        node_data = G.nodes[nearest_node]
        return (int(nearest_node), float(node_data['y']), float(node_data['x']))
    except Exception as e:
        print(f"    ⚠️ 스냅 실패: {e}")
        return None


def sample_gps_data(gps_data, max_samples=500):
    """GPS 데이터 샘플링"""
    if len(gps_data) <= max_samples:
        return gps_data
    
    indices = np.linspace(0, len(gps_data) - 1, max_samples, dtype=int)
    sampled = [gps_data[i] for i in indices]
    
    return sampled


def generate_road_snapped_waypoints_sync(G, tree_data, gps_data, start_lat, start_lon, end_lat, end_lon):
    """최적화된 도로 노드 기반 waypoint 생성"""
    if G is None or tree_data is None:
        waypoints = [
            {"lat": float(round(start_lat, 6)), "lon": float(round(start_lon, 6)), "node_id": None},
            {"lat": float(round(end_lat, 6)), "lon": float(round(end_lon, 6)), "node_id": None}
        ]
        return waypoints, 0.0, "straight_line"
    
    start_node_data = snap_to_nearest_node_fast(G, tree_data, start_lat, start_lon)
    end_node_data = snap_to_nearest_node_fast(G, tree_data, end_lat, end_lon)
    
    if not start_node_data or not end_node_data:
        waypoints = [
            {"lat": float(round(start_lat, 6)), "lon": float(round(start_lon, 6)), "node_id": None},
            {"lat": float(round(end_lat, 6)), "lon": float(round(end_lon, 6)), "node_id": None}
        ]
        return waypoints, 0.0, "straight_line"
    
    start_node_id, start_node_lat, start_node_lon = start_node_data
    end_node_id, end_node_lat, end_node_lon = end_node_data
    
    try:
        route = nx.shortest_path(G, start_node_id, end_node_id, weight='length')
    except (nx.NetworkXNoPath, nx.NodeNotFound):
        waypoints = [
            {"lat": float(round(start_node_lat, 6)), "lon": float(round(start_node_lon, 6)), "node_id": start_node_id},
            {"lat": float(round(end_node_lat, 6)), "lon": float(round(end_node_lon, 6)), "node_id": end_node_id}
        ]
        return waypoints, 0.0, "straight_line"
    
    sampled_gps = sample_gps_data(gps_data, max_samples=500)
    
    node_visit_count = Counter()
    for gps_lat, gps_lon, gps_time in sampled_gps:
        node_data = snap_to_nearest_node_fast(G, tree_data, gps_lat, gps_lon)
        if node_data:
            node_id = node_data[0]
            node_visit_count[node_id] += 1
    
    frequent_nodes = []
    for node_id in route:
        if node_id == start_node_id or node_id == end_node_id:
            continue
        
        visit_count = node_visit_count.get(node_id, 0)
        if visit_count > 0:
            node_data = G.nodes[node_id]
            frequent_nodes.append((node_id, node_data['y'], node_data['x'], visit_count))
    
    frequent_nodes.sort(key=lambda x: x[3], reverse=True)
    selected_nodes = frequent_nodes[:3]
    
    if selected_nodes:
        node_order = {node_id: idx for idx, node_id in enumerate(route)}
        selected_nodes.sort(key=lambda x: node_order.get(x[0], 0))
    
    waypoints = []
    
    waypoints.append({
        "lat": float(round(start_node_lat, 6)),
        "lon": float(round(start_node_lon, 6)),
        "node_id": start_node_id
    })
    
    for node_id, lat, lon, visit_count in selected_nodes:
        waypoints.append({
            "lat": float(round(lat, 6)),
            "lon": float(round(lon, 6)),
            "node_id": int(node_id)
        })
    
    waypoints.append({
        "lat": float(round(end_node_lat, 6)),
        "lon": float(round(end_node_lon, 6)),
        "node_id": end_node_id
    })
    
    total_visits = sum([x[3] for x in selected_nodes])
    preference_score = min(1.0, total_visits / 30)
    
    return waypoints, preference_score, "road_network"


async def generate_road_snapped_waypoints(G, tree_data, gps_data, start_lat, start_lon, end_lat, end_lon):
    """비동기 버전"""
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(
        executor,
        generate_road_snapped_waypoints_sync,
        G, tree_data, gps_data, start_lat, start_lon, end_lat, end_lon
    )


def filter_by_similar_time(gps_data, target_time, time_window_hours=3):
    """시간대별 필터링"""
    target_hour = target_time.hour
    
    filtered = []
    for lat, lon, record_time in gps_data:
        record_hour = record_time.hour
        hour_diff = abs(record_hour - target_hour)
        if hour_diff > 12:
            hour_diff = 24 - hour_diff
        
        if hour_diff <= time_window_hours:
            filtered.append((lat, lon, record_time))
    
    return filtered


def select_diverse_clusters(clusters, max_count=3, min_separation_m=200):
    """지리적 분산 고려 클러스터 선택"""
    selected = []
    sorted_clusters = sorted(clusters, key=lambda x: x[2], reverse=True)
    
    for cluster in sorted_clusters:
        if len(selected) >= max_count:
            break
        
        cluster_lat, cluster_lon = cluster[0], cluster[1]
        
        too_close = False
        for selected_cluster in selected:
            selected_lat, selected_lon = selected_cluster[0], selected_cluster[1]
            dist = haversine_distance(cluster_lat, cluster_lon, selected_lat, selected_lon)
            
            if dist < min_separation_m:
                too_close = True
                break
        
        if not too_close:
            selected.append(cluster)
    
    return selected


def count_visit_sessions(timestamps, gap_threshold_minutes=30):
    """방문 세션 카운트"""
    if len(timestamps) == 0:
        return 0
    
    sorted_times = sorted(timestamps)
    session_count = 1
    
    for i in range(1, len(sorted_times)):
        time_gap = (sorted_times[i] - sorted_times[i-1]).total_seconds() / 60
        if time_gap > gap_threshold_minutes:
            session_count += 1
    
    return session_count


def find_frequent_locations_with_sessions_sync(gps_data, last_known_coords, max_search_radius_m,
                                               min_visits, session_gap_minutes, min_cluster_size):
    """HDBSCAN 클러스터링"""
    last_lat, last_lon = last_known_coords
    
    if len(gps_data) < min_visits:
        return []
    
    nearby_gps = []
    for lat, lon, time in gps_data:
        dist = haversine_distance(last_lat, last_lon, lat, lon)
        if dist <= max_search_radius_m:
            nearby_gps.append((lat, lon, time))
    
    if len(nearby_gps) < min_visits:
        return []
    
    coords = np.array([[lat, lon] for lat, lon, _ in nearby_gps])
    times = [time for _, _, time in nearby_gps]
    
    coords_radians = np.radians(coords)
    
    clusterer = hdbscan.HDBSCAN(
        min_cluster_size=min_cluster_size,
        min_samples=min_visits,
        metric='haversine',
        cluster_selection_epsilon=0.0,
        cluster_selection_method='eom'
    )
    
    clusterer.fit(coords_radians)
    labels = clusterer.labels_
    
    clusters = []
    for label in set(labels):
        if label == -1:
            continue
        
        cluster_mask = labels == label
        cluster_points = coords[cluster_mask]
        cluster_times = [times[i] for i in range(len(times)) if cluster_mask[i]]
        
        center_lat = cluster_points[:, 0].mean()
        center_lon = cluster_points[:, 1].mean()
        
        total_records = len(cluster_points)
        visit_sessions = count_visit_sessions(cluster_times, gap_threshold_minutes=session_gap_minutes)
        
        try:
            cluster_stability = float(clusterer.cluster_persistence_[label])
        except:
            cluster_stability = 0.5
        
        clusters.append((center_lat, center_lon, visit_sessions, total_records, cluster_stability))
    
    clusters.sort(key=lambda x: x[2], reverse=True)
    
    return clusters


async def find_frequent_locations_with_sessions(gps_data, last_known_coords, max_search_radius_m,
                                                min_visits, session_gap_minutes, min_cluster_size):
    """비동기 버전"""
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(
        executor,
        find_frequent_locations_with_sessions_sync,
        gps_data, last_known_coords, max_search_radius_m,
        min_visits, session_gap_minutes, min_cluster_size
    )


# =============================================================================
# API Endpoints
# =============================================================================

@app.get("/")
async def root():
    """API 루트"""
    return {
        "status": "running",
        "service": "Missing Person Destination Prediction API",
        "version": "12.0.0",
        "endpoints": {
            "/api/predict-destinations": "실시간 목적지 예측",
            "/api/health": "헬스 체크",
            "/docs": "API 문서"
        },
        "features": [
            "BallTree 기반 빠른 도로 노드 스냅",
            "시간대별 패턴 분석",
            "선호 경로 반영",
            "지리적 분산 고려",
            "Spring Boot 연동"
        ]
    }


@app.post("/api/predict-destinations", response_model=PredictionResponse)
async def predict_destinations(request: PredictionRequest):
    """
    실종자 목적지 예측
    
    Spring Boot에서 GPS 데이터를 직접 받아서 처리
    """
    
    print(f"\n{'='*60}")
    print(f"[예측 요청] user={request.user_no}, time={request.missing_time}")
    print(f"수신 GPS: {len(request.gps_data)}개")
    print(f"{'='*60}")
    
    # 시간 파싱
    try:
        target_time = datetime.strptime(request.missing_time, "%Y-%m-%d %H:%M")
    except ValueError:
        raise HTTPException(status_code=400, detail="시간 형식 오류 (YYYY-MM-DD HH:MM)")
    
    # GPS 데이터 검증
    if not request.gps_data:
        raise HTTPException(status_code=404, detail="GPS 데이터 없음")
    
    # GPS 데이터 파싱
    gps_data = []
    for record in request.gps_data:
        try:
            record_time = datetime.strptime(record.record_time, "%Y-%m-%d %H:%M:%S")
            gps_data.append((record.latitude, record.longitude, record_time))
        except ValueError:
            continue  # 잘못된 데이터 스킵
    
    if not gps_data:
        raise HTTPException(status_code=400, detail="유효한 GPS 데이터 없음")
    
    print(f"✅ 파싱된 GPS: {len(gps_data)}개")
    
    # POI 로드
    try:
        pois_df = pd.read_csv(request.csv_path)
        print(f"✅ POI: {len(pois_df)}개")
    except:
        pois_df = pd.DataFrame()
        print(f"⚠️ POI 파일 없음")
    
    # 데이터 충분성 판단
    EXPECTED_28DAYS = 3 * 20 * 24 * 28
    EXPECTED_7DAYS = 3 * 20 * 24 * 7
    
    if len(gps_data) < EXPECTED_7DAYS:
        data_sufficiency = "nono"
    elif len(gps_data) < EXPECTED_28DAYS:
        data_sufficiency = "no"
    else:
        data_sufficiency = "yes"
    
    print(f"📊 데이터 충분성: {data_sufficiency}")
    
    # 마지막 위치 추출
    sorted_gps = sorted(gps_data, key=lambda x: x[2])
    last_lat, last_lon, last_time = sorted_gps[-1]
    
    print(f"📍 마지막 위치: ({last_lat:.6f}, {last_lon:.6f})")
    
    # 시간대 필터링
    print(f"⏰ 시간대 필터링 (±{request.time_window_hours}시간)...")
    time_filtered_gps = filter_by_similar_time(gps_data, target_time, time_window_hours=request.time_window_hours)
    print(f"✅ 시간대 데이터: {len(time_filtered_gps)}개")
    
    if len(time_filtered_gps) < 100:
        time_filtered_gps = filter_by_similar_time(gps_data, target_time, time_window_hours=6)
        print(f"   범위 확대: {len(time_filtered_gps)}개")
    
    if len(time_filtered_gps) < 50:
        time_filtered_gps = gps_data
        print(f"   전체 사용: {len(time_filtered_gps)}개")
    
    # 도로망 다운로드
    print(f"🌐 도로망 로딩...")
    G = await get_road_network(last_lat, last_lon, radius_m=request.road_network_radius)
    
    # BallTree 구축
    tree_data = None
    if G is not None:
        print(f"🔧 BallTree 구축...")
        tree_data = build_balltree(G)
        print(f"✅ BallTree 준비")
    
    # 클러스터링
    print(f"🔎 클러스터링...")
    
    all_clusters = await find_frequent_locations_with_sessions(
        time_filtered_gps,
        last_known_coords=(last_lat, last_lon),
        max_search_radius_m=request.max_search_radius,
        min_visits=5,
        session_gap_minutes=request.session_gap,
        min_cluster_size=request.min_cluster_size
    )
    
    print(f"✅ 클러스터: {len(all_clusters)}개")
    
    if not all_clusters:
        raise HTTPException(status_code=404, detail="클러스터 없음")
    
    # 거리별 분류
    clusters_by_distance = {"500m": [], "1000m": [], "1500m": []}
    
    for cluster_lat, cluster_lon, visit_sessions, total_records, stability in all_clusters:
        distance = haversine_distance(last_lat, last_lon, cluster_lat, cluster_lon)
        
        if distance < 50:
            continue
        
        cluster_data = (cluster_lat, cluster_lon, visit_sessions, total_records, stability, distance)
        
        if distance <= 500:
            clusters_by_distance["500m"].append(cluster_data)
        elif distance <= 1000:
            clusters_by_distance["1000m"].append(cluster_data)
        elif distance <= 1500:
            clusters_by_distance["1500m"].append(cluster_data)
    
    print(f"\n📊 거리별 분포: 500m({len(clusters_by_distance['500m'])}), "
          f"1000m({len(clusters_by_distance['1000m'])}), 1500m({len(clusters_by_distance['1500m'])})")
    
    # 각 범위 처리
    destinations_by_distance = {}
    
    for range_key in ["500m", "1000m", "1500m"]:
        print(f"\n🎯 {range_key}...")
        
        selected_clusters = select_diverse_clusters(
            clusters_by_distance[range_key],
            max_count=5,
            min_separation_m=request.min_cluster_separation
        )
        
        destinations = []
        
        for cluster_lat, cluster_lon, visit_sessions, total_records, stability, distance in selected_clusters:
            waypoints, preference_score, route_method = await generate_road_snapped_waypoints(
                G,
                tree_data,
                time_filtered_gps,
                last_lat, last_lon,
                cluster_lat, cluster_lon
            )
            
            print(f"  - {distance:.0f}m, 방문 {visit_sessions}회, {route_method}")
            
            poi_name = None
            if not pois_df.empty:
                for _, poi in pois_df.iterrows():
                    if haversine_distance(cluster_lat, cluster_lon, poi['lat'], poi['lon']) < 100:
                        poi_name = poi['name']
                        break
            
            destination = {
                "destination_id": len(destinations) + 1,
                "latitude": float(round(cluster_lat, 6)),
                "longitude": float(round(cluster_lon, 6)),
                "visit_count": int(visit_sessions),
                "total_gps_records": int(total_records),
                "distance_meters": float(round(distance, 1)),
                "cluster_stability": float(round(stability, 3)),
                "waypoints": waypoints,
                "preference_score": float(round(preference_score, 3)),
                "route_method": route_method
            }
            
            if poi_name:
                destination["name"] = poi_name
            
            destinations.append(destination)
        
        destinations_by_distance[range_key] = destinations
    
    print(f"\n✅ 예측 완료!\n")
    
    response_data = {
        "user_no": request.user_no,
        "missing_time": target_time.isoformat(),
        "last_known_location": {
            "latitude": float(last_lat),
            "longitude": float(last_lon),
            "time": last_time.isoformat()
        },
        "analysis_period_days": request.analysis_days,
        "session_gap_minutes": request.session_gap,
        "time_filtered_records": len(time_filtered_gps),
        "total_clusters_found": len(all_clusters),
        "destinations_by_distance": destinations_by_distance,
        "data_sufficiency": data_sufficiency,
        "total_gps_records": len(gps_data)
    }
    
    return response_data


@app.get("/api/health")
async def health_check():
    """헬스 체크"""
    return {
        "status": "healthy",
        "version": "12.0.0",
        "features": [
            "BallTree optimization",
            "Time-based filtering",
            "Road network snapping",
            "Spring Boot integration"
        ]
    }


# =============================================================================
# Main
# =============================================================================

if __name__ == "__main__":
    import uvicorn
    print("="*60)
    print("🚀 실종자 목적지 예측 API 시작")
    print("="*60)
    print("📖 문서: http://0.0.0.0:8000/docs")
    print("🔍 예측: POST /api/predict-destinations")
    print("💚 헬스: GET /api/health")
    print("="*60)
    uvicorn.run(app, host="0.0.0.0", port=8000)
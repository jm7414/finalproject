"""
Missing Person Destination Prediction API - Complete Version
실종자 목적지 예측 시스템 (검증 기능 포함)

Features:
- BallTree 기반 빠른 도로 노드 스냅
- 시간대별 패턴 분석
- 선호 경로 반영
- 지리적 분산 고려
- 모델 검증 API
"""

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Dict, Optional
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy import Table, Column, Integer, Float, DateTime, MetaData, select, and_
from sqlalchemy.orm import declarative_base
from datetime import datetime, timedelta
import os
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
os.environ['LANG'] = 'en_US.UTF-8'

DATABASE_URL = "postgresql+asyncpg://postgres:rootroot@localhost:5432/finalproject"

engine = create_async_engine(
    DATABASE_URL,
    echo=False,
    pool_pre_ping=True,
    connect_args={"ssl": False}
)

SessionLocal = async_sessionmaker(engine, expire_on_commit=False)
Base = declarative_base()

executor = ThreadPoolExecutor(max_workers=4)

# Caches
ROAD_NETWORK_CACHE = {}
BALLTREE_CACHE = {}

# =============================================================================
# FastAPI App
# =============================================================================

app = FastAPI(
    title="실종자 목적지 예측 API (완전판)",
    description="검증 기능 포함 실종자 경로 예측 시스템",
    version="11.0.0"
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
    data_sufficiency: str = Field(description="데이터 충분성 (yes/no) - 28일 이상 데이터 기준")
    total_gps_records: int = Field(description="전체 GPS 레코드 수")


class ValidationMetrics(BaseModel):
    total_test_cases: int
    hit_rate_500m: float = Field(description="500m 이내 적중률")
    hit_rate_1000m: float = Field(description="1000m 이내 적중률")
    hit_rate_1500m: float = Field(description="1500m 이내 적중률")
    average_distance_error: float = Field(description="평균 거리 오차 (미터)")
    median_distance_error: float = Field(description="중간값 거리 오차 (미터)")
    route_similarity_score: float = Field(description="경로 유사도 (0~1)")


class TestCase(BaseModel):
    test_time: str
    predicted_location: Dict[str, float]
    actual_location: Dict[str, float]
    distance_error: float
    hit_500m: bool
    hit_1000m: bool
    hit_1500m: bool


class ValidationResponse(BaseModel):
    user_no: int
    validation_period: str
    metrics: ValidationMetrics
    test_cases: List[TestCase]


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
        "version": "11.0.0",
        "endpoints": {
            "/api/predict-destinations": "실시간 목적지 예측",
            "/api/validate-model": "모델 검증",
            "/api/health": "헬스 체크",
            "/docs": "API 문서"
        },
        "features": [
            "BallTree 기반 빠른 도로 노드 스냅",
            "시간대별 패턴 분석",
            "선호 경로 반영",
            "지리적 분산 고려",
            "모델 검증 기능"
        ]
    }


@app.post("/api/predict-destinations", response_model=PredictionResponse)
async def predict_destinations(
    user_no: int = Query(..., description="사용자 번호"),
    missing_time: str = Query(..., description="실종 시간 (YYYY-MM-DD HH:MM)"),
    analysis_days: int = Query(120, ge=1, le=365, description="분석 기간 (일)"),
    time_window_hours: int = Query(3, ge=1, le=12, description="시간대 필터 (±시간)"),
    session_gap: int = Query(30, ge=5, le=120, description="세션 구분 (분)"),
    min_cluster_size: int = Query(10, ge=3, le=50, description="최소 클러스터 크기"),
    max_search_radius: int = Query(2000, ge=500, le=5000, description="검색 반경 (m)"),
    min_cluster_separation: int = Query(200, ge=50, le=500, description="클러스터 간 거리 (m)"),
    road_network_radius: int = Query(2500, ge=1000, le=5000, description="도로망 반경 (m)"),
    csv_path: str = Query("all_locations.csv", description="POI CSV")
):
    """
    실종자 목적지 예측
    
    실종 시간 이전의 GPS 데이터를 분석하여 가능성 높은 목적지와 경로 예측
    """
    
    print(f"\n{'='*60}")
    print(f"[예측 요청] user={user_no}, time={missing_time}")
    print(f"{'='*60}")
    
    try:
        target_time = datetime.strptime(missing_time, "%Y-%m-%d %H:%M")
    except ValueError:
        raise HTTPException(status_code=400, detail="시간 형식 오류 (YYYY-MM-DD HH:MM)")
    
    try:
        pois_df = pd.read_csv(csv_path)
        print(f"✅ POI: {len(pois_df)}개")
    except:
        pois_df = pd.DataFrame()
        print(f"⚠️ POI 파일 없음")
    
    async with SessionLocal() as session:
        metadata = MetaData()
        locations_table = Table(
            'user_location', metadata,
            Column('location_no', Integer, primary_key=True),
            Column('user_no', Integer),
            Column('latitude', Float),
            Column('longitude', Float),
            Column('record_time', DateTime)
        )
        
        start_time = target_time - timedelta(days=analysis_days)
        end_time = target_time
        
        print(f"📅 분석 기간: {analysis_days}일")
        
        query = select(
            locations_table.c.latitude,
            locations_table.c.longitude,
            locations_table.c.record_time
        ).where(
            and_(
                locations_table.c.user_no == user_no,
                locations_table.c.record_time >= start_time,
                locations_table.c.record_time < end_time
            )
        ).order_by(locations_table.c.record_time)
        
        result = await session.execute(query)
        locations = result.fetchall()
        
        if not locations:
            raise HTTPException(status_code=404, detail="GPS 데이터 없음")
        
        print(f"✅ GPS: {len(locations)}개")
        
        # ✅ 수정: 데이터 충분성 판단
        EXPECTED_28DAYS_RECORDS = 3 * 20 * 24 * 28  # 데이터 기간이 28일 보다 적다면?
        EXPECTED_7DAYS_RECORDS = 3 * 20 * 24 * 7 # 데이터 기간이 7일보다 적다면?
        if len(locations) < EXPECTED_7DAYS_RECORDS :
            data_sufficiency = "nono"
        elif len(locations) < EXPECTED_28DAYS_RECORDS :
            data_sufficiency = "no"
        else : 
            data_sufficiency = "yes"        
        print(f"✅ GPS: {len(locations)}개")
                
        last_location = locations[-1]
        last_lat = last_location.latitude
        last_lon = last_location.longitude
        last_time = last_location.record_time
        
        print(f"📍 마지막 위치: ({last_lat:.6f}, {last_lon:.6f})")
        
        gps_data = [(loc.latitude, loc.longitude, loc.record_time) for loc in locations]
        
        # 시간대 필터링
        print(f"⏰ 시간대 필터링 (±{time_window_hours}시간)...")
        time_filtered_gps = filter_by_similar_time(gps_data, target_time, time_window_hours=time_window_hours)
        print(f"✅ 시간대 데이터: {len(time_filtered_gps)}개")
        
        if len(time_filtered_gps) < 100:
            time_filtered_gps = filter_by_similar_time(gps_data, target_time, time_window_hours=6)
            print(f"   범위 확대: {len(time_filtered_gps)}개")
        
        if len(time_filtered_gps) < 50:
            time_filtered_gps = gps_data
            print(f"   전체 사용: {len(time_filtered_gps)}개")
        
        # 도로망 다운로드
        print(f"🌐 도로망 로딩...")
        G = await get_road_network(last_lat, last_lon, radius_m=road_network_radius)
        
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
            max_search_radius_m=max_search_radius,
            min_visits=5,
            session_gap_minutes=session_gap,
            min_cluster_size=min_cluster_size
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
                min_separation_m=min_cluster_separation
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
            "user_no": user_no,
            "missing_time": target_time.isoformat(),
            "last_known_location": {
                "latitude": float(last_lat),
                "longitude": float(last_lon),
                "time": last_time.isoformat()
            },
            "analysis_period_days": analysis_days,
            "session_gap_minutes": session_gap,
            "time_filtered_records": len(time_filtered_gps),
            "total_clusters_found": len(all_clusters),
            "destinations_by_distance": destinations_by_distance,
            "data_sufficiency": data_sufficiency,
            "total_gps_records": len(locations)
        }
        
        return response_data


@app.post("/api/validate-model", response_model=ValidationResponse)
async def validate_model(
    user_no: int = Query(..., description="사용자 번호"),
    validation_start: str = Query(..., description="검증 시작 (YYYY-MM-DD HH:MM)"),
    validation_end: str = Query(..., description="검증 종료 (YYYY-MM-DD HH:MM)"),
    training_days: int = Query(120, description="학습 기간 (일)"),
    test_interval_hours: int = Query(24, description="테스트 간격 (시간)"),
    csv_path: str = Query("all_locations.csv")
):
    """
    모델 검증
    
    과거 데이터로 모델 성능 평가
    - Hit Rate: 예측 정확도
    - 평균 거리 오차
    - 경로 유사도
    """
    
    print(f"\n{'='*60}")
    print(f"[검증 시작] user={user_no}")
    print(f"검증 기간: {validation_start} ~ {validation_end}")
    print(f"{'='*60}\n")
    
    try:
        val_start_time = datetime.strptime(validation_start, "%Y-%m-%d %H:%M")
        val_end_time = datetime.strptime(validation_end, "%Y-%m-%d %H:%M")
    except ValueError:
        raise HTTPException(status_code=400, detail="시간 형식 오류")
    
    try:
        pois_df = pd.read_csv(csv_path)
    except:
        pois_df = pd.DataFrame()
    
    async with SessionLocal() as session:
        metadata = MetaData()
        locations_table = Table(
            'user_location', metadata,
            Column('location_no', Integer, primary_key=True),
            Column('user_no', Integer),
            Column('latitude', Float),
            Column('longitude', Float),
            Column('record_time', DateTime)
        )
        
        # 전체 데이터 로드
        full_start = val_start_time - timedelta(days=training_days)
        full_end = val_end_time
        
        query = select(
            locations_table.c.latitude,
            locations_table.c.longitude,
            locations_table.c.record_time
        ).where(
            and_(
                locations_table.c.user_no == user_no,
                locations_table.c.record_time >= full_start,
                locations_table.c.record_time <= full_end
            )
        ).order_by(locations_table.c.record_time)
        
        result = await session.execute(query)
        all_locations = result.fetchall()
        
        if not all_locations:
            raise HTTPException(status_code=404, detail="데이터 없음")
        
        print(f"✅ 전체 데이터: {len(all_locations)}개")
        
        # 테스트 케이스 생성
        test_times = []
        current_test_time = val_start_time
        while current_test_time <= val_end_time:
            test_times.append(current_test_time)
            current_test_time += timedelta(hours=test_interval_hours)
        
        print(f"📊 테스트 케이스: {len(test_times)}개\n")
        
        # 검증 지표
        hits_500m = 0
        hits_1000m = 0
        hits_1500m = 0
        distance_errors = []
        route_similarities = []
        test_cases = []
        
        for test_idx, test_time in enumerate(test_times):
            print(f"[테스트 {test_idx+1}/{len(test_times)}] {test_time.strftime('%Y-%m-%d %H:%M')}")
            
            # 학습 데이터
            train_start = test_time - timedelta(days=training_days)
            train_end = test_time
            
            train_data = [
                (loc.latitude, loc.longitude, loc.record_time)
                for loc in all_locations
                if train_start <= loc.record_time < train_end
            ]
            
            if len(train_data) < 100:
                print(f"  ⚠️ 학습 데이터 부족, 스킵")
                continue
            
            # 마지막 위치
            last_lat, last_lon, last_time = train_data[-1]
            
            # 시간대별 필터링
            time_filtered = filter_by_similar_time(train_data, test_time, time_window_hours=3)
            
            if len(time_filtered) < 50:
                time_filtered = train_data
            
            # 클러스터링
            try:
                clusters = await find_frequent_locations_with_sessions(
                    time_filtered,
                    last_known_coords=(last_lat, last_lon),
                    max_search_radius_m=2000,
                    min_visits=5,
                    session_gap_minutes=30,
                    min_cluster_size=10
                )
            except Exception as e:
                print(f"  ⚠️ 클러스터링 실패: {e}, 스킵")
                continue
            
            if not clusters:
                print(f"  ⚠️ 클러스터 없음, 스킵")
                continue
            
            # 예측: 가장 가능성 높은 목적지
            pred_lat, pred_lon = clusters[0][0], clusters[0][1]
            
            # 실제 데이터: 미래 6시간
            actual_future_data = [
                (loc.latitude, loc.longitude, loc.record_time)
                for loc in all_locations
                if test_time <= loc.record_time < test_time + timedelta(hours=6)
            ]
            
            if len(actual_future_data) < 10:
                print(f"  ⚠️ 실제 데이터 부족, 스킵")
                continue
            
            # 실제 목적지
            try:
                actual_clusters = await find_frequent_locations_with_sessions(
                    actual_future_data,
                    last_known_coords=(last_lat, last_lon),
                    max_search_radius_m=2000,
                    min_visits=3,
                    session_gap_minutes=30,
                    min_cluster_size=5
                )
            except Exception as e:
                print(f"  ⚠️ 실제 클러스터링 실패, 스킵")
                continue
            
            if not actual_clusters:
                print(f"  ⚠️ 실제 클러스터 없음, 스킵")
                continue
            
            actual_lat, actual_lon = actual_clusters[0][0], actual_clusters[0][1]
            
            # 거리 오차
            distance_error = haversine_distance(pred_lat, pred_lon, actual_lat, actual_lon)
            distance_errors.append(distance_error)
            
            # Hit rate
            hit_500 = distance_error <= 500
            hit_1000 = distance_error <= 1000
            hit_1500 = distance_error <= 1500
            
            if hit_500:
                hits_500m += 1
                hits_1000m += 1
                hits_1500m += 1
            elif hit_1000:
                hits_1000m += 1
                hits_1500m += 1
            elif hit_1500:
                hits_1500m += 1
            
            # 경로 유사도 (예측 위치 근처 통과 여부)
            passed_near = any(
                haversine_distance(pred_lat, pred_lon, gps_lat, gps_lon) <= 100
                for gps_lat, gps_lon, gps_time in actual_future_data
            )
            route_similarities.append(1.0 if passed_near else 0.0)
            
            # 테스트 케이스 저장
            test_cases.append({
                "test_time": test_time.isoformat(),
                "predicted_location": {"lat": float(pred_lat), "lon": float(pred_lon)},
                "actual_location": {"lat": float(actual_lat), "lon": float(actual_lon)},
                "distance_error": float(round(distance_error, 1)),
                "hit_500m": hit_500,
                "hit_1000m": hit_1000,
                "hit_1500m": hit_1500
            })
            
            print(f"  ✓ 오차: {distance_error:.0f}m")
        
        total_tests = len(test_cases)
        
        if total_tests == 0:
            raise HTTPException(status_code=404, detail="유효한 테스트 없음")
        
        # 검증 지표 계산
        metrics = {
            "total_test_cases": total_tests,
            "hit_rate_500m": float(round(hits_500m / total_tests, 3)),
            "hit_rate_1000m": float(round(hits_1000m / total_tests, 3)),
            "hit_rate_1500m": float(round(hits_1500m / total_tests, 3)),
            "average_distance_error": float(round(np.mean(distance_errors), 1)),
            "median_distance_error": float(round(np.median(distance_errors), 1)),
            "route_similarity_score": float(round(np.mean(route_similarities), 3))
        }
        
        print(f"\n{'='*60}")
        print(f"검증 결과")
        print(f"{'='*60}")
        print(f"총 테스트: {total_tests}개")
        print(f"500m 적중률: {metrics['hit_rate_500m']*100:.1f}%")
        print(f"1000m 적중률: {metrics['hit_rate_1000m']*100:.1f}%")
        print(f"1500m 적중률: {metrics['hit_rate_1500m']*100:.1f}%")
        print(f"평균 오차: {metrics['average_distance_error']:.0f}m")
        print(f"중간값 오차: {metrics['median_distance_error']:.0f}m")
        print(f"경로 유사도: {metrics['route_similarity_score']:.2f}")
        print(f"{'='*60}\n")
        
        response = {
            "user_no": user_no,
            "validation_period": f"{validation_start} ~ {validation_end}",
            "metrics": metrics,
            "test_cases": test_cases[:20]  # 최대 20개
        }
        
        return response


@app.get("/api/health")
async def health_check():
    """헬스 체크"""
    try:
        async with SessionLocal() as session:
            await session.execute(select(1))
        return {
            "status": "healthy",
            "database": "connected",
            "version": "11.0.0",
            "features": [
                "BallTree optimization",
                "Time-based filtering",
                "Road network snapping",
                "Model validation"
            ]
        }
    except Exception as e:
        return {"status": "unhealthy", "error": str(e)}


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
    print("📊 검증: POST /api/validate-model")
    print("💚 헬스: GET /api/health")
    print("="*60)
    uvicorn.run(app, host="0.0.0.0", port=8000)

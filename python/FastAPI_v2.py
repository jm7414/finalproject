"""
Missing Person Destination Prediction API - Simplified Version
실종자 목적지 예측 시스템 (Spring Boot 연동)

Features:
- BallTree 기반 빠른 도로 노드 스냅
- 시간대별 패턴 분석
- 선호 경로 반영
- 지리적 분산 고려
- ⭐⭐⭐ 도로 네트워크를 따라가는 waypoints (수정됨)
"""
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Dict, Optional
from datetime import datetime, timedelta
import numpy as np
import os
import pandas as pd
import hdbscan
import asyncio
from concurrent.futures import ThreadPoolExecutor
from collections import Counter
import osmnx as ox
import networkx as nx
from sklearn.neighbors import BallTree
from mesa import Model
from mesa.time import RandomActivation
from mesa_geo import GeoSpace, GeoAgent
from shapely.geometry import Point
import random
# ⭐⭐ 아두이노 센서에 필요한 라이브러리
import serial
import threading

def monitor_sensor():
    global pir_state
    while True:
        if ser.in_waiting > 0:
            line = ser.readline().decode('utf-8').strip()
            if line in ['0', '1']:
                val = int(line)
                if val == 1 and pir_state == 0:
                    print("Movement detected! Triggering event.")
                    # 여기서 원하는 API 호출 또는 동작 실행 가능
                pir_state = val

# 아두이노 센서 추가부분 이거는 안꽂혀있어도 괜찮음
try:
    ser = serial.Serial('COM3', 9600, timeout=1)
    thread = threading.Thread(target=monitor_sensor, daemon=True)
    thread.start()
    print("✅ 아두이노 센서 모니터링 시작")
except serial.SerialException as e:
    print(f"⚠️ 아두이노 연결 실패: {e}")
    ser = None
    pir_state = -1  # 센서 비활성 상태



# =============================================================================
# Configuration
# =============================================================================

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
    version="12.0.2"
)

# CORS 설정
origins = [
    os.getenv("FRONTEND_URL", "http://localhost:3000"),
    "https://localhost:3000",
    "https://localhost:5173",
    "http://localhost:3000",
    "http://localhost:5173",
    "https://lx12mammamia.xyz",
    "https://www.lx12mammamia.xyz"
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
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
    time_window_hours: Optional[int] = 5
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
# 시뮬레이션에 필요한 변수 class
# =============================================================================
class SimulationRequest(BaseModel):
    user_no: int
    latitude: float
    longitude: float

class AgentPathPoint(BaseModel):
    step: int
    time_seconds: float
    latitude: float
    longitude: float
    node: int


class PositionInfo(BaseModel):
    latitude: float
    longitude: float
    probability: float
    agent_count_at_position: int


class RepresentativeAgent(BaseModel):
    rank: int
    agent_id: int
    total_distance_m: float
    position_info: PositionInfo
    path: List[AgentPathPoint]


class AnimationAgent(BaseModel):
    rank: int
    agent_id: int
    latitude: float
    longitude: float
    time_seconds: float
    final_position: PositionInfo


class AnimationFrame(BaseModel):
    step: int
    agents: List[AnimationAgent]


class SimulationResponse(BaseModel):
    scenario: str
    description: str
    total_representative_agents: int
    simulation_info: Dict
    data: Dict


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


# ⭐⭐⭐ 핵심 수정 함수 1: 도로 네트워크를 따라가는 waypoints 생성 ⭐⭐⭐
def generate_road_snapped_waypoints_sync(G, tree_data, gps_data, start_lat, start_lon, end_lat, end_lon):
    """
    ⭐ 수정된 함수: 도로 네트워크의 모든 노드를 waypoint로 반환
    
    변경사항:
    - 이전: 중요한 2-3개 노드만 선택 → 직선 연결
    - 수정: 경로상의 모든 노드를 반환 → 도로를 따라감
    """
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
        # 최단 경로 계산
        route = nx.shortest_path(G, start_node_id, end_node_id, weight='length')
    except (nx.NetworkXNoPath, nx.NodeNotFound):
        waypoints = [
            {"lat": float(round(start_node_lat, 6)), "lon": float(round(start_node_lon, 6)), "node_id": start_node_id},
            {"lat": float(round(end_node_lat, 6)), "lon": float(round(end_node_lon, 6)), "node_id": end_node_id}
        ]
        return waypoints, 0.0, "straight_line"
    
    # ⭐⭐⭐ 핵심 변경: 경로상의 모든 노드를 waypoint로 추가 ⭐⭐⭐
    waypoints = []
    
    # 경로상의 모든 노드를 순서대로 waypoint로 변환
    for node_id in route:
        node_data = G.nodes[node_id]
        waypoints.append({
            "lat": float(round(node_data['y'], 6)),
            "lon": float(round(node_data['x'], 6)),
            "node_id": int(node_id)
        })
    
    # GPS 데이터를 기반으로 선호도 계산 (기존 로직 유지)
    sampled_gps = sample_gps_data(gps_data, max_samples=500)
    
    node_visit_count = Counter()
    for gps_lat, gps_lon, gps_time in sampled_gps:
        node_data = snap_to_nearest_node_fast(G, tree_data, gps_lat, gps_lon)
        if node_data:
            node_id = node_data[0]
            if node_id in route:  # 경로상의 노드만 카운트
                node_visit_count[node_id] += 1
    
    total_visits = sum(node_visit_count.values())
    preference_score = min(1.0, total_visits / 30) if total_visits > 0 else 0.0
    
    print(f"    ✅ 경로 생성: {len(waypoints)}개 waypoints (선호도: {preference_score:.2f})")
    
    return waypoints, preference_score, "road_network"


async def generate_road_snapped_waypoints(G, tree_data, gps_data, start_lat, start_lon, end_lat, end_lon):
    """비동기 버전"""
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(
        executor,
        generate_road_snapped_waypoints_sync,
        G, tree_data, gps_data, start_lat, start_lon, end_lat, end_lon
    )


def filter_by_similar_time(gps_data, target_time, time_window_hours=5):
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

# ============================================
# 시뮬레이션 클래스 및 함수
# ============================================

def analyze_turn_preference(gps_data, user_no=1):
    """GPS 데이터에서 회전 선호도 분석"""
    try:
        user_data = gps_data[gps_data['user_no'] == user_no].copy()
        user_data = user_data.sort_values('record_time').reset_index(drop=True)
        if len(user_data) < 3:
            return 'right', 0.5
        
        left_turns, right_turns = 0, 0
        for i in range(1, len(user_data) - 1):
            prev = (user_data.iloc[i-1]['latitude'], user_data.iloc[i-1]['longitude'])
            curr = (user_data.iloc[i]['latitude'], user_data.iloc[i]['longitude'])
            nxt = (user_data.iloc[i+1]['latitude'], user_data.iloc[i+1]['longitude'])
            
            v1 = (prev[0] - curr[0], prev[1] - curr[1])
            v2 = (nxt[0] - curr[0], nxt[1] - curr[1])
            cross = v1[0]*v2[1] - v1[1]*v2[0]
            
            if cross > 0.00000001:
                left_turns += 1
            elif cross < -0.00000001:
                right_turns += 1
        
        total_turns = left_turns + right_turns
        if total_turns == 0:
            return 'right', 0.5
        
        if left_turns > right_turns:
            return 'left', left_turns / total_turns
        else:
            return 'right', right_turns / total_turns
    except:
        return 'right', 0.5


class RealisticPatientAgent(GeoAgent):
    """에이전트 클래스"""
    
    def __init__(self, unique_id, model, shape, crs, start_node, 
                 max_time_minutes=90, turn_preference='right'):
        super().__init__(unique_id=unique_id, model=model, geometry=shape, crs=crs)
        self.start_node = start_node
        self.current_node = start_node
        self.next_node = None
        self.visited_nodes = [start_node]
        self.max_time_seconds = max_time_minutes * 60
        self.elapsed_time_seconds = 0.0
        self.total_distance_m = 0.0
        self.stopped = False
        self.walking_speed_ms = random.uniform(0.5, 1.0)
        self.edge_progress = 0.0
        self.current_edge_data = None
        self.turn_preference = turn_preference
        
        # ⭐ 수정: 더 현실적인 이동을 위해 방향 유지 비중 증가
        self.behavior_weights = {
            'random_walk': 0.3,      # 랜덤 이동 감소
            'direction_persist': 0.40, # 방향 유지 증가 (직진 선호)
            'backtrack': 0.05,         # 되돌아가기
            'stay_put': 0.25           # 멈춤
        }
        self.last_direction = None
        self.stay_duration = 0
        
        self.path_history = []
        self._record_position()

    def _record_position(self):
        if self.geometry:
            self.path_history.append({
                'step': self.model.schedule.steps,
                'time_seconds': self.elapsed_time_seconds,
                'latitude': self.geometry.y,
                'longitude': self.geometry.x,
                'node': self.current_node
            })

    def calculate_distance(self, node1, node2):
        from math import radians, sin, cos, sqrt, atan2
        pos1 = self.model.graph.nodes[node1]
        pos2 = self.model.graph.nodes[node2]
        R = 6371000
        lat1, lon1 = radians(pos1['y']), radians(pos1['x'])
        lat2, lon2 = radians(pos2['y']), radians(pos2['x'])
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        a = sin(dlat/2)**2 + cos(lat1)*cos(lat2)*sin(dlon/2)**2
        c = 2*atan2(sqrt(a), sqrt(1-a))
        return R * c

    def interpolate_position(self, node1, node2, progress):
        pos1 = self.model.graph.nodes[node1]
        pos2 = self.model.graph.nodes[node2]
        x = pos1['x'] + (pos2['x'] - pos1['x']) * progress
        y = pos1['y'] + (pos2['y'] - pos1['y']) * progress
        return Point(x, y)

    def choose_with_turn_preference(self, neighbors):
        if not self.last_direction or len(neighbors) == 1:
            return random.choice(neighbors)
        
        current_pos = self.model.graph.nodes[self.current_node]
        neighbor_info = []
        
        for neighbor in neighbors:
            neighbor_pos = self.model.graph.nodes[neighbor]
            new_direction = (
                neighbor_pos['x'] - current_pos['x'],
                neighbor_pos['y'] - current_pos['y']
            )
            cross_product = (
                self.last_direction[0] * new_direction[1] - 
                self.last_direction[1] * new_direction[0]
            )
            neighbor_info.append({'node': neighbor, 'cross_product': cross_product})
        
        threshold = 0.0001
        left_turns = [n for n in neighbor_info if n['cross_product'] > threshold]
        right_turns = [n for n in neighbor_info if n['cross_product'] < -threshold]
        straight = [n for n in neighbor_info if abs(n['cross_product']) <= threshold]
        
        if self.turn_preference == 'left':
            preferred, opposite = left_turns, right_turns
        else:
            preferred, opposite = right_turns, left_turns
        
        rand = random.random()
        if rand < 0.3 and preferred:
            return random.choice(preferred)['node']
        elif rand < 0.4 and opposite:
            return random.choice(opposite)['node']
        else:
            return random.choice(straight)['node'] if straight else random.choice(neighbors)

    def choose_next_node(self):
        neighbors = list(self.model.graph.neighbors(self.current_node))
        if not neighbors:
            return None, None
        
        behavior = random.choices(
            list(self.behavior_weights.keys()), 
            list(self.behavior_weights.values())
        )[0]
        
        if behavior == 'random_walk':
            # ⭐ 수정: 가까운 노드를 우선적으로 선택
            if len(neighbors) > 1:
                # 각 이웃 노드까지의 거리 계산
                neighbor_distances = []
                for neighbor in neighbors:
                    dist = self.calculate_distance(self.current_node, neighbor)
                    neighbor_distances.append((neighbor, dist))
                
                # 거리 기반 가중치 (가까울수록 높은 가중치)
                total_weight = sum(1.0 / (d + 1.0) for _, d in neighbor_distances)
                weights = [(1.0 / (d + 1.0)) / total_weight for _, d in neighbor_distances]
                
                # 가중치 기반 선택 (80%), 회전 선호도 고려 (20%)
                if random.random() < 0.8:
                    selected_node = random.choices(
                        [n for n, _ in neighbor_distances],
                        weights=weights
                    )[0]
                else:
                    selected_node = self.choose_with_turn_preference(neighbors)
            else:
                selected_node = neighbors[0]
                
        elif behavior == 'direction_persist':
            best_node, best_similarity = None, -1
            current_pos = self.model.graph.nodes[self.current_node]
            for neighbor in neighbors:
                neighbor_pos = self.model.graph.nodes[neighbor]
                direction = (
                    neighbor_pos['x'] - current_pos['x'],
                    neighbor_pos['y'] - current_pos['y']
                )
                similarity = (
                    direction[0]*self.last_direction[0] +
                    direction[1]*self.last_direction[1]
                ) if self.last_direction else -1
                if similarity > best_similarity:
                    best_similarity = similarity
                    best_node = neighbor
            selected_node = best_node if best_node else random.choice(neighbors)
        elif behavior == 'backtrack':
            if len(self.visited_nodes) > 1:
                previous_node = self.visited_nodes[-2]
                selected_node = previous_node if previous_node in neighbors else random.choice(neighbors)
            else:
                selected_node = random.choice(neighbors)
        else:  # stay_put
            self.stay_duration = random.uniform(5*60, 10*60)
            selected_node = self.current_node
        
        if selected_node != self.current_node:
            edge_data = self.model.graph.get_edge_data(
                self.current_node, selected_node, default={}
            )
            if edge_data:
                edge_data = edge_data.get(0, edge_data)
            return selected_node, edge_data
        else:
            return None, None

    def step(self):
        
        if self.stay_duration > 0:
            time_step = 10.0
            self.stay_duration = max(0, self.stay_duration - time_step)
            self.elapsed_time_seconds += time_step
            self._record_position()
            return
        
        if self.next_node is None:
            next_node, edge_data = self.choose_next_node()
            if next_node is None:
                self.stopped = True
                return
            self.next_node = next_node
            self.current_edge_data = edge_data
            self.edge_progress = 0.0
        
        if self.current_edge_data:
            edge_length_m = self.current_edge_data.get('length', 0)
        else:
            edge_length_m = self.calculate_distance(self.current_node, self.next_node)
        
        time_step = 10.0
        distance_step = self.walking_speed_ms * time_step
        
        if edge_length_m > 0:
            progress_increment = distance_step / edge_length_m
            self.edge_progress += progress_increment
        else:
            self.edge_progress = 1.0
        
        # ⭐ 수정: 엣지를 따라 이동하는 중간 위치도 기록
        if self.edge_progress < 1.0:
            # 중간 지점에서의 위치 기록
            self.geometry = self.interpolate_position(
                self.current_node, self.next_node, self.edge_progress
            )
        else:
            # 다음 노드 도착
            self.geometry = Point(
                self.model.graph.nodes[self.next_node]['x'],
                self.model.graph.nodes[self.next_node]['y']
            )
            current_pos = self.model.graph.nodes[self.current_node]
            next_pos = self.model.graph.nodes[self.next_node]
            self.last_direction = (
                next_pos['x'] - current_pos['x'],
                next_pos['y'] - current_pos['y']
            )
            self.visited_nodes.append(self.next_node)
            self.current_node = self.next_node
            self.next_node = None
            self.current_edge_data = None
            self.edge_progress = 0.0
            self.total_distance_m += edge_length_m
        
        self.elapsed_time_seconds += time_step
        self._record_position()


class DementiaWanderingModel(Model):
    """시뮬레이션 모델"""
    
    def __init__(self, graph, start_node, max_time_minutes=90, 
                 turn_preference='right', n_agents=200):
        super().__init__()
        self.graph = graph
        self.start_node = start_node
        self.max_time_minutes = max_time_minutes
        self.turn_preference = turn_preference
        self.schedule = RandomActivation(self)
        self.space = GeoSpace(crs='EPSG:4326')
        
        start_pos = graph.nodes[start_node]
        start_point = Point(start_pos['x'], start_pos['y'])
        
        for i in range(n_agents):
            agent = RealisticPatientAgent(
                unique_id=i,
                model=self,
                shape=start_point,
                crs='EPSG:4326',
                start_node=start_node,
                max_time_minutes=max_time_minutes,
                turn_preference=turn_preference
            )
            self.schedule.add(agent)
            self.space.add_agents(agent)
    
    def step(self):
        self.schedule.step()

def run_simulation_for_scenario(latitude, longitude, time_minutes, distance_m, turn_preference):
    """단일 시나리오 시뮬레이션 실행"""
    print(f"시뮬레이션 시작: {time_minutes}분, {distance_m}m")
    
    center_point = (latitude, longitude)
    G = ox.graph_from_point(center_point, dist=3000, network_type='walk')
    
    start_node = ox.nearest_nodes(G, longitude, latitude)
    
    model = DementiaWanderingModel(
        graph=G,
        start_node=start_node,
        max_time_minutes=time_minutes,
        turn_preference=turn_preference,
        n_agents=100
    )
    
    max_steps = time_minutes * 6
    for step in range(max_steps):
        model.step()
    
    return extract_top10_data(model, time_minutes)


def extract_top10_data(model, time_minutes):
    """상위 10개 대표 에이전트 데이터 추출"""
    # 최종 위치별 그룹화
    final_positions = {}
    
    for agent in model.schedule.agents:
        if agent.geometry:
            final_pos = (round(agent.geometry.x, 6), round(agent.geometry.y, 6))
            if final_pos not in final_positions:
                final_positions[final_pos] = []
            final_positions[final_pos].append(agent)
    
    # 확률 계산
    total_agents = len(model.schedule.agents)
    position_data = []
    
    for position, agents in final_positions.items():
        probability = (len(agents) / total_agents) * 100
        position_data.append({
            'position': position,
            'latitude': position[1],
            'longitude': position[0],
            'count': len(agents),
            'probability': probability,
            'agents': agents
        })
    
    position_data.sort(key=lambda x: x['probability'], reverse=True)
    top_positions = position_data[:10]
    
    # 대표 에이전트 선택
    representative_agents = []
    
    for rank, pos_data in enumerate(top_positions, 1):
        agents_at_position = pos_data['agents']
        representative = max(agents_at_position, key=lambda a: len(a.path_history))
        
        representative_agents.append({
            'rank': rank,
            'latitude': pos_data['latitude'],
            'longitude': pos_data['longitude'],
            'probability': pos_data['probability'],
            'agent_count': pos_data['count'],
            'agent_id': representative.unique_id,
            'path': representative.path_history,
            'total_distance': representative.total_distance_m
        })
    
    # 애니메이션 프레임 생성
    max_steps = time_minutes * 6
    
    animation_frames = []
    for step in range(max_steps + 1):
        frame = {
            'step': step,
            'agents': []
        }
        
        for rep in representative_agents:
            positions = [p for p in rep['path'] if p['step'] == step]
            if positions:
                pos = positions[0]
                frame['agents'].append({
                    'rank': rep['rank'],
                    'agent_id': rep['agent_id'],
                    'latitude': pos['latitude'],
                    'longitude': pos['longitude'],
                    'time_seconds': pos['time_seconds'],
                    'final_position': {
                        'latitude': rep['latitude'],
                        'longitude': rep['longitude'],
                        'probability': rep['probability']
                    }
                })
        
        animation_frames.append(frame)
    
    return {
        'agents': representative_agents,
        'frames': animation_frames,
        'total_steps': max_steps + 1
    }


# =============================================================================
# API Endpoints
# =============================================================================

@app.get("/")
async def root():
    """API 루트"""
    return {
        "status": "running",
        "service": "Missing Person Destination Prediction API",
        "version": "12.0.2",
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
            "Spring Boot 연동",
            "⭐ 도로 네트워크를 따라가는 waypoints"
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
    time_filtered_gps = filter_by_similar_time(gps_data, target_time, request.time_window_hours)
    print(f"✅ 시간대 데이터: {len(time_filtered_gps)}개")
    
    if len(time_filtered_gps) < 100:
        time_filtered_gps = filter_by_similar_time(gps_data, target_time, time_window_hours=3)
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
        min_visits=10,
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
            
            print(f"  - {distance:.0f}m, 방문 {visit_sessions}회, {route_method}, waypoints: {len(waypoints)}개")
            
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

@app.post("/api/predict-destinations/test", response_model=PredictionResponse)
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
    time_filtered_gps = filter_by_similar_time(gps_data, target_time, time_window_hours=3)
    print(f"✅ 시간대 데이터: {len(time_filtered_gps)}개")
    
    if len(time_filtered_gps) < 100:
        time_filtered_gps = filter_by_similar_time(gps_data, target_time, time_window_hours=3)
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
        min_visits=3,
        session_gap_minutes=request.session_gap,
        min_cluster_size=request.min_cluster_size
    )
    
    print(f"✅ 클러스터: {len(all_clusters)}개")
    
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
            
            print(f"  - {distance:.0f}m, 방문 {visit_sessions}회, {route_method}, waypoints: {len(waypoints)}개")
            
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

@app.post("/api/agent-simulation/run-all")
async def run_all_simulations(request: SimulationRequest):
    """
    ★★★ 3개 시나리오 동시 실행 및 결과 반환 ★★★
    
    Parameters:
    - request: SimulationRequest 
        - user_no: 사용자 번호
        - latitude: 실종 위치 위도
        - longitude: 실종 위치 경도
    
    Returns:
    - 30분, 60분, 90분 시나리오의 시뮬레이션 데이터
    
    Example Request:
```json
    {
      "user_no": 1,
      "latitude": 37.238257,
      "longitude": 126.681727
    }
```
    """
    
    try:
        print(f"\n{'='*60}")
        print(f"[시뮬레이션 요청] user={request.user_no}")
        print(f"📍 실종 위치: ({request.latitude:.6f}, {request.longitude:.6f})")
        print(f"{'='*60}")
        
        # ⭐ GPS 데이터 파싱 로직 제거 (필요 없음)
        latitude = request.latitude
        longitude = request.longitude
        
        # ⭐ 회전 선호도는 기본값 사용 (GPS 데이터 없이는 분석 불가)
        turn_preference = 'right'
        print(f"🔄 회전 선호도: {turn_preference} (기본값)")
        
        # 시나리오 정의
        scenarios = [
            {'time': 30, 'distance': 500, 'label': '30분'},
            {'time': 60, 'distance': 1000, 'label': '60분'},
            {'time': 90, 'distance': 1500, 'label': '90분'}
        ]
        
        print(f"🚀 3개 시나리오 병렬 실행 시작...")
        
        # 병렬 실행을 위한 작업 생성
        loop = asyncio.get_event_loop()
        tasks = []
        
        for scenario in scenarios:
            task = loop.run_in_executor(
                executor,
                run_simulation_for_scenario,
                latitude,
                longitude,
                scenario['time'],
                scenario['distance'],
                turn_preference
            )
            tasks.append((scenario['label'], task))
        
        # 모든 시뮬레이션 완료 대기
        results = {}
        for label, task in tasks:
            data = await task
            results[label] = data
            print(f"✅ {label} 시나리오 완료 (프레임: {data['total_steps']}, 에이전트: {len(data['agents'])})")
        
        print(f"\n{'='*60}")
        print(f"✅ 전체 시뮬레이션 완료!")
        print(f"{'='*60}\n")
        
        return {
            "success": True,
            "message": "3개 시나리오 시뮬레이션 완료",
            "user_no": request.user_no,
            "start_location": {
                "latitude": latitude,
                "longitude": longitude
            },
            "turn_preference": turn_preference,
            "scenarios": results
        }
        
    except HTTPException:
        raise
    except Exception as e:
        print(f"❌ 시뮬레이션 오류: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=500,
            detail=f"시뮬레이션 실행 중 오류: {str(e)}"
        )

@app.get("/api/health")
async def health_check():
    """헬스 체크"""
    return {
        "status": "healthy",
        "version": "12.0.2",
        "features": [
            "BallTree optimization",
            "Time-based filtering",
            "Road network snapping",
            "Spring Boot integration",
            "⭐ Realistic road-following paths"
        ]
    }
    
    

## /sensor로 들어오면 이걸 계속 보내줌
@app.get('/sensor')
def get_sensor_value():
    return {"pir": pir_state}

# =============================================================================
# Main
# =============================================================================

if __name__ == "__main__":
    import uvicorn
    print("="*60)
    print("🚀 실종자 목적지 예측 API 시작 (도로 경로 버전)")
    print("="*60)
    print("📖 문서: http://0.0.0.0:8000/docs")
    print("🔍 예측: POST /api/predict-destinations")
    print("💚 헬스: GET /api/health")
    print("⭐ 특징: 도로 네트워크를 따라가는 경로")
    print("="*60)
    uvicorn.run(app, host=os.getenv("HOST", "0.0.0.0"), port=int(os.getenv("PORT", "8000")))
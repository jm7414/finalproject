"""
실제 도로 네트워크를 따라 이동하는 GPS 궤적 생성기
OpenStreetMap 기반으로 현실적인 경로 생성

필요한 패키지 설치:
pip install osmnx networkx pandas numpy geopy matplotlib
"""
import osmnx as ox
import networkx as nx
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from geopy.distance import geodesic
import matplotlib.pyplot as plt
import warnings
import srtm  # 고도 데이터 추가
from osmnx import convert
warnings.filterwarnings('ignore')


# 한글 폰트 설정
import matplotlib.font_manager as fm
import platform

if platform.system() == 'Windows':
    plt.rc('font', family='Malgun Gothic')
elif platform.system() == 'Darwin':
    plt.rc('font', family='AppleGothic')
else:
    plt.rc('font', family='NanumGothic')
plt.rcParams['axes.unicode_minus'] = False


class RoadBasedGPSGenerator:
    """실제 도로 네트워크 기반 GPS 생성기 (고도 고려)"""
    
    def __init__(self, center_lat=37.234257, center_lon=126.681727, distance=3000, 
                 use_elevation=True, elevation_weight=2.0):
        """
        Args:
            center_lat: 중심 위도 (안산)
            center_lon: 중심 경도
            distance: 중심으로부터 반경 (m)
            use_elevation: 고도 데이터 사용 여부
            elevation_weight: 고도 가중치 (높을수록 경사를 더 회피)
        """
        print(f"\n{'='*70}")
        print("🗺️  도로 네트워크 다운로드 중...")
        print(f"{'='*70}")
        print(f"📍 중심: ({center_lat}, {center_lon})")
        print(f"📏 반경: {distance}m")
        
        # OpenStreetMap에서 도로 네트워크 다운로드
        self.graph = ox.graph_from_point(
            (center_lat, center_lon),
            dist=distance,
            network_type='walk',
            simplify=True
        )
        
        # 그래프를 undirected로 변환
        
        self.graph = convert.to_undirected(self.graph)  # ⭐ 변경
        
        print("✅ 도로 네트워크 로드 완료!")
        print(f"   - 노드 수: {len(self.graph.nodes):,}개")
        print(f"   - 도로 수: {len(self.graph.edges):,}개")
        
        self.center_lat = center_lat
        self.center_lon = center_lon
        self.use_elevation = use_elevation
        self.elevation_weight = elevation_weight
        
        # 고도 데이터 로드
        if use_elevation:
            print("\n🏔️  고도 데이터 로드 중...")
            self.elevation_data = srtm.get_data()
            self._add_elevation_to_graph()
            print(f"{'='*70}\n")
        else:
            print(f"{'='*70}\n")
    
    def _add_elevation_to_graph(self):
        """그래프 노드에 고도 정보 추가"""
        print("   노드 고도 계산 중...")
        
        # 각 노드에 고도 추가
        for node, data in self.graph.nodes(data=True):
            lat, lon = data['y'], data['x']
            try:
                elevation = self.elevation_data.get_elevation(lat, lon)
                # None인 경우 0으로 설정 (바다 또는 데이터 없음)
                data['elevation'] = elevation if elevation is not None else 0
            except Exception as e:
                data['elevation'] = 0
        
        # 각 엣지에 경사도 계산 및 가중치 추가
        print("   경사도 및 가중치 계산 중...")
        for u, v, key, data in self.graph.edges(keys=True, data=True):
            # 시작/끝 노드 고도
            elev_start = self.graph.nodes[u].get('elevation', 0)
            elev_end = self.graph.nodes[v].get('elevation', 0)
            
            # 경사도 계산 (고도차 / 거리)
            length = data.get('length', 1.0)  # 미터 단위
            elevation_change = abs(elev_end - elev_start)
            grade = elevation_change / length if length > 0 else 0
            
            # 경사도를 퍼센트로 저장
            data['grade'] = grade * 100
            data['elevation_change'] = elevation_change
            
            # 고도 기반 가중치 계산
            # 경사가 클수록 가중치 증가 (exponential)
            elevation_penalty = np.exp(grade * self.elevation_weight)
            
            # 최종 가중치 = 거리 * 고도 패널티
            data['weighted_length'] = length * elevation_penalty
        
        print("✅ 고도 데이터 추가 완료!")
        
        # 통계 출력
        grades = [data['grade'] for _, _, _, data in self.graph.edges(keys=True, data=True)]
        print(f"   평균 경사도: {np.mean(grades):.2f}%")
        print(f"   최대 경사도: {np.max(grades):.2f}%")
        print(f"   중앙 경사도: {np.median(grades):.2f}%")
    
    def get_nearest_node(self, lat, lon):
        """가장 가까운 도로 노드 찾기"""
        return ox.distance.nearest_nodes(self.graph, lon, lat)
    
    def find_route(self, start_node, end_node, avoid_steep=True):
        """
        두 노드 사이의 최단 경로 찾기
        
        Args:
            start_node: 시작 노드
            end_node: 끝 노드
            avoid_steep: True면 경사를 고려한 경로, False면 최단거리
        """
        try:
            # 가중치 선택
            weight = 'weighted_length' if (avoid_steep and self.use_elevation) else 'length'
            
            # 최단 경로 계산
            route = nx.shortest_path(
                self.graph,
                start_node,
                end_node,
                weight=weight
            )
            return route
        except (nx.NetworkXNoPath, nx.NodeNotFound):
            return None
    
    def calculate_route_difficulty(self, route):
        """경로의 난이도 계산 (고도 변화 기반)"""
        if not route or len(route) < 2:
            return 0.0
        
        total_elevation_gain = 0.0
        total_distance = 0.0
        
        for i in range(len(route) - 1):
            u, v = route[i], route[i + 1]
            
            # 여러 edge가 있을 수 있으므로 첫 번째 사용
            edge_data = list(self.graph[u][v].values())[0]
            
            elev_start = self.graph.nodes[u].get('elevation', 0)
            elev_end = self.graph.nodes[v].get('elevation', 0)
            
            # 올라가는 구간만 카운트
            if elev_end > elev_start:
                total_elevation_gain += (elev_end - elev_start)
            
            total_distance += edge_data.get('length', 0)
        
        # 난이도 = 총 상승 고도 / 총 거리 (m당 상승 미터)
        difficulty = total_elevation_gain / total_distance if total_distance > 0 else 0
        
        return difficulty
    
    def route_to_coordinates(self, route):
        """노드 리스트를 위경도 좌표로 변환"""
        coords = []
        for node in route:
            node_data = self.graph.nodes[node]
            coords.append((node_data['y'], node_data['x']))
        return coords
    
    def interpolate_route(self, coords, interval_meters=15):
        """경로를 일정 간격으로 보간"""
        interpolated = [coords[0]]
        
        for i in range(len(coords) - 1):
            start = coords[i]
            end = coords[i + 1]
            
            distance = geodesic(start, end).meters
            n_points = max(int(distance / interval_meters), 1)
            
            for j in range(1, n_points + 1):
                t = j / n_points
                lat = start[0] + t * (end[0] - start[0])
                lon = start[1] + t * (end[1] - start[1])
                interpolated.append((lat, lon))
        
        return interpolated
    
    def add_gps_noise(self, lat, lon, noise_meters=2):
        """GPS 노이즈 추가"""
        lat_noise = np.random.normal(0, noise_meters / 111000)
        lon_noise = np.random.normal(0, noise_meters / (111000 * np.cos(np.radians(lat))))
        return lat + lat_noise, lon + lon_noise
    
    def generate_key_locations(self, home_lat, home_lon, n_locations=8, 
                              max_elevation_diff=30, ensure_directional_diversity=True):
        """
        환자의 주요 방문 장소 생성 (방향 다양성 고려)

        Args:
            home_lat: 집 위도
            home_lon: 집 경도
            n_locations: 생성할 장소 수
            max_elevation_diff: 집으로부터 최대 허용 고도 차이 (미터)
            ensure_directional_diversity: True면 8방향으로 균등 배치
        """
        locations = {'home': {'lat': home_lat, 'lon': home_lon, 'node': None}}

        home_node = self.get_nearest_node(home_lat, home_lon)
        locations['home']['node'] = home_node

        # 집의 고도
        home_elevation = self.graph.nodes[home_node].get('elevation', 0)

        all_nodes = list(self.graph.nodes())

        place_types = [
            '슈퍼마켓', '공원', '병원', '약국', 
            '복지관', '시장', '버스정류장', '친구집',
            '카페', '은행', '우체국', '도서관'
        ]

        selected_nodes = set([home_node])

        if ensure_directional_diversity:
            # 8방향으로 나누기 (N, NE, E, SE, S, SW, W, NW)
            directions = [
                ('북쪽', 0, 45),           # 0° ~ 45°
                ('북동쪽', 45, 90),        # 45° ~ 90°
                ('동쪽', 90, 135),         # 90° ~ 135°
                ('남동쪽', 135, 180),      # 135° ~ 180°
                ('남쪽', 180, 225),        # 180° ~ 225°
                ('남서쪽', 225, 270),      # 225° ~ 270°
                ('서쪽', 270, 315),        # 270° ~ 315°
                ('북서쪽', 315, 360)       # 315° ~ 360°
            ]

            # 각 방향당 최소 1개씩 배치
            locations_per_direction = max(1, n_locations // len(directions))

            print(f"\n   📍 방향별 장소 배치 (집 고도: {home_elevation:.1f}m)")
            print(f"   {'='*65}")

            direction_idx = 0

            for i in range(min(n_locations, len(place_types))):
                # 현재 방향 선택 (순환)
                dir_name, angle_min, angle_max = directions[direction_idx % len(directions)]
                direction_idx += 1

                attempts = 0
                found = False

                while attempts < 300 and not found:
                    candidate_node = np.random.choice(all_nodes)

                    if candidate_node in selected_nodes:
                        attempts += 1
                        continue
                    
                    node_data = self.graph.nodes[candidate_node]
                    node_lat, node_lon = node_data['y'], node_data['x']
                    node_elevation = node_data.get('elevation', 0)

                    # 거리 계산
                    dist = geodesic((home_lat, home_lon), (node_lat, node_lon)).meters

                    # 방향 계산 (북쪽 기준 시계방향 각도)
                    delta_lat = node_lat - home_lat
                    delta_lon = node_lon - home_lon

                    # atan2로 각도 계산 (라디안 -> 도)
                    # atan2(y, x) 형태이며, 동쪽이 0도, 반시계방향
                    angle_rad = np.arctan2(delta_lon, delta_lat)
                    angle_deg = np.degrees(angle_rad)

                    # 북쪽을 0도로 변환 (시계방향)
                    # 북쪽(위) = 0도, 동쪽 = 90도, 남쪽 = 180도, 서쪽 = 270도
                    angle_deg = (angle_deg + 360) % 360

                    # 고도 차이 확인
                    elevation_diff = abs(node_elevation - home_elevation)

                    # 조건 확인: 거리, 고도, 방향
                    distance_ok = 100 <= dist <= 2500
                    elevation_ok = elevation_diff <= max_elevation_diff

                    # 방향 확인 (약간의 여유 추가 ±10도)
                    angle_margin = 20
                    if angle_max == 360:  # 북서쪽 특수 처리
                        direction_ok = (angle_deg >= angle_min - angle_margin) or (angle_deg <= 45)
                    else:
                        direction_ok = (angle_min - angle_margin <= angle_deg <= angle_max + angle_margin)

                    if distance_ok and elevation_ok and direction_ok:
                        locations[f'loc_{i}'] = {
                            'lat': node_lat,
                            'lon': node_lon,
                            'node': candidate_node,
                            'type': place_types[i],
                            'elevation': node_elevation,
                            'elevation_diff': elevation_diff,
                            'distance': dist,
                            'direction': dir_name,
                            'angle': angle_deg
                        }
                        selected_nodes.add(candidate_node)
                        found = True

                        print(f"   {place_types[i]:10s} | {dir_name:6s} ({angle_deg:6.1f}°) | "
                              f"{dist:6.0f}m | 고도: {node_elevation:5.1f}m (±{elevation_diff:4.1f}m)")

                    attempts += 1

                if not found:
                    print(f"   ⚠️ {dir_name} 방향에 적합한 장소를 찾지 못했습니다.")

            print(f"   {'='*65}\n")

        else:
            # 기존 방식 (방향 고려 없음)
            for i in range(min(n_locations, len(place_types))):
                attempts = 0
                while attempts < 200:
                    candidate_node = np.random.choice(all_nodes)

                    if candidate_node in selected_nodes:
                        attempts += 1
                        continue
                    
                    node_data = self.graph.nodes[candidate_node]
                    node_lat, node_lon = node_data['y'], node_data['x']
                    node_elevation = node_data.get('elevation', 0)

                    dist = geodesic((home_lat, home_lon), (node_lat, node_lon)).meters
                    elevation_diff = abs(node_elevation - home_elevation)

                    if 30 <= dist <= 3000 and elevation_diff <= max_elevation_diff:
                        locations[f'loc_{i}'] = {
                            'lat': node_lat,
                            'lon': node_lon,
                            'node': candidate_node,
                            'type': place_types[i],
                            'elevation': node_elevation,
                            'elevation_diff': elevation_diff
                        }
                        selected_nodes.add(candidate_node)
                        break
                    
                    attempts += 1

        return locations

    
    def get_time_pattern(self, hour):
        """시간대별 방문 확률 패턴 (개선 버전)"""
        if 0 <= hour < 6:
            return {'home': 1.0}  # 수면 시간
        elif 6 <= hour < 8:
            # 아침 (기상, 준비)
            return {'home': 0.6, 'loc_0': 0.25, 'loc_1': 0.15}  # 슈퍼마켓/산책
        elif 8 <= hour < 10:
            # 아침 외출 (산책, 운동, 시장)
            return {'home': 0.2, 'loc_1': 0.3, 'loc_5': 0.25, 'loc_0': 0.25}
        elif 10 <= hour < 12:
            # 오전 활동 (병원, 복지관, 공원)
            return {'home': 0.1, 'loc_2': 0.25, 'loc_4': 0.25, 'loc_1': 0.2, 'loc_3': 0.2}
        elif 12 <= hour < 14:
            # 점심 시간 (집 또는 식당)
            return {'home': 0.4, 'loc_5': 0.3, 'loc_0': 0.2, 'loc_7': 0.1}
        elif 14 <= hour < 16:
            # 오후 활동 (은행, 약국, 복지관)
            return {'home': 0.15, 'loc_3': 0.25, 'loc_4': 0.25, 'loc_7': 0.2, 'loc_2': 0.15}
        elif 16 <= hour < 18:
            # 오후 후반 (친구 방문, 카페)
            return {'home': 0.2, 'loc_7': 0.3, 'loc_8': 0.25, 'loc_1': 0.15, 'loc_5': 0.1}
        elif 18 <= hour < 20:
            # 저녁 시간 (귀가 또는 장보기)
            return {'home': 0.5, 'loc_0': 0.25, 'loc_5': 0.15, 'loc_6': 0.1}
        elif 20 <= hour < 22:
            # 저녁 늦은 시간 (주로 집)
            return {'home': 0.7, 'loc_0': 0.15, 'loc_1': 0.1, 'loc_7': 0.05}
        else:  # 22-24시
            # 야간 (취침 준비)
            return {'home': 0.9, 'loc_0': 0.08, 'loc_1': 0.02}

    
    def generate_patient_trajectory(self, user_no, days=30, interval_minutes=3):
        """한 환자의 실제 도로 기반 GPS 궤적 생성 (고도 고려) - 00~06시 집 강제"""
        print(f"🚶 환자 {user_no} 궤적 생성 중...")

        all_nodes = list(self.graph.nodes())
        home_node = np.random.choice(all_nodes)
        home_data = self.graph.nodes[home_node]
        home_lat, home_lon = home_data['y'], home_data['x']

        # 주요 방문 장소 생성 (고도 제한 적용)
        locations = self.generate_key_locations(home_lat, home_lon, n_locations=8)

        trajectory = []
        start_date = datetime(2025, 9, 21, 0, 0, 0)
        current_time = start_date
        current_location = 'home'
        current_coords = (home_lat, home_lon)

        route_buffer = []
        route_idx = 0
        stay_until = current_time
        is_moving = False

        total_steps = days * 24 * 60 // interval_minutes

        for step in range(total_steps):
            hour = current_time.hour

            # ⭐⭐⭐ 00~06시 집 강제 로직 추가 ⭐⭐⭐
            if 0 <= hour < 6:
                # 무조건 집으로 이동 (아직 집이 아니면)
                if current_location != 'home':
                    # 집으로 강제 이동
                    start_node = locations[current_location]['node']
                    end_node = locations['home']['node']
                    
                    route = self.find_route(start_node, end_node, avoid_steep=True)
                    
                    if route:
                        coords = self.route_to_coordinates(route)
                        route_buffer = self.interpolate_route(coords, interval_meters=15)
                        route_idx = 0
                        current_location = 'home'
                        is_moving = True
                    else:
                        # 경로를 못 찾으면 그냥 집 좌표로 텔레포트
                        current_coords = (home_lat, home_lon)
                        current_location = 'home'
                        route_buffer = [current_coords]
                        route_idx = 0
                        is_moving = False
                
                # 이미 집이면 그냥 머무름
                else:
                    route_buffer = [current_coords]
                    route_idx = 0
                    is_moving = False
            
            # 06시 이후 정상 활동
            else:
                if len(route_buffer) == 0 or route_idx >= len(route_buffer):
                    if is_moving:
                        is_moving = False
                        # 머무는 시간 조정 (더 짧게)
                        if current_location == 'home':
                            stay_duration = np.random.randint(30, 120)  # 집: 30-120분
                        else:
                            stay_duration = np.random.randint(15, 60)   # 외부: 15-60분
                        stay_until = current_time + timedelta(minutes=stay_duration)
                        route_buffer = [current_coords]
                        route_idx = 0
                    elif current_time >= stay_until:
                        time_pattern = self.get_time_pattern(hour)
                        available_locs = [loc for loc in time_pattern.keys() if loc in locations]

                        if available_locs:
                            probs = [time_pattern[loc] for loc in available_locs]
                            probs = np.array(probs) / np.sum(probs)
                            next_location = np.random.choice(available_locs, p=probs)
                        else:
                            next_location = 'home'

                        if next_location != current_location:
                            start_node = locations[current_location]['node']
                            end_node = locations[next_location]['node']

                            # 경사를 고려한 경로 찾기
                            route = self.find_route(start_node, end_node, avoid_steep=True)

                            if route:
                                coords = self.route_to_coordinates(route)
                                route_buffer = self.interpolate_route(coords, interval_meters=15)
                                route_idx = 0
                                current_location = next_location
                                is_moving = True
                            else:
                                stay_duration = np.random.randint(15, 60)
                                stay_until = current_time + timedelta(minutes=stay_duration)
                                route_buffer = [current_coords]
                                route_idx = 0
                        else:
                            # 같은 장소에서 추가 체류 (더 짧게)
                            if current_location == 'home':
                                stay_duration = np.random.randint(30, 90)
                            else:
                                stay_duration = np.random.randint(10, 40)
                            stay_until = current_time + timedelta(minutes=stay_duration)
                            route_buffer = [current_coords]
                            route_idx = 0
                    else:
                        route_buffer = [current_coords]
                        route_idx = 0

            
            if route_idx < len(route_buffer):
                lat, lon = route_buffer[route_idx]
                route_idx += 1
            else:
                lat, lon = route_buffer[-1]
            
            lat, lon = self.add_gps_noise(lat, lon, noise_meters=2)
            current_coords = (lat, lon)
            
            trajectory.append({
                'user_no': user_no,
                'latitude': lat,
                'longitude': lon,
                'timestamp': current_time,
                'home_lat': home_lat,
                'home_lon': home_lon
            })
            
            current_time += timedelta(minutes=interval_minutes)
        
        print(f"   ✅ 환자 {user_no} 완료 ({len(trajectory):,}개 포인트)")
        return trajectory
    

    
    def generate_multiple_patients(self, n_patients=5, days=30, interval_minutes=3):
        """여러 환자 데이터 생성"""
        print(f"\n{'='*70}")
        print(f"👥 {n_patients}명 환자 GPS 궤적 생성 시작")
        print(f"{'='*70}\n")
        
        all_trajectories = []
        for user_no in range(1, n_patients + 1):
            trajectory = self.generate_patient_trajectory(user_no, days, interval_minutes)
            all_trajectories.extend(trajectory)
        
        df = pd.DataFrame(all_trajectories)
        
        print(f"\n{'='*70}")
        print("✅ 전체 생성 완료!")
        print(f"{'='*70}")
        print(f"📊 총 GPS 포인트: {len(df):,}개")
        print(f"👥 환자 수: {df['user_no'].nunique()}명")
        print(f"{'='*70}\n")
        
        return df
    
    def visualize_road_network_and_trajectories(self, df, save_path='road_based_trajectories.png'):
        """도로 네트워크와 궤적 시각화"""
        fig, axes = plt.subplots(1, 2, figsize=(18, 8))
        
        # 1. 도로 네트워크 + 전체 궤적
        ax1 = axes[0]
        
        # 도로 네트워크 그리기
        ox.plot_graph(self.graph, ax=ax1, show=False, close=False, 
                      node_size=0, edge_color='lightgray', edge_linewidth=0.5)
        
        # 환자별 궤적 (샘플)
        sample_patients = df['user_no'].unique()[:3]
        colors = ['red', 'blue', 'green']
        
        for idx, user_no in enumerate(sample_patients):
            patient_data = df[df['user_no'] == user_no].sample(frac=0.1)  # 10% 샘플링
            ax1.scatter(patient_data['longitude'], patient_data['latitude'],
                       s=1, alpha=0.5, c=colors[idx], label=f'환자 {user_no}')
            
            # 집 위치
            home = patient_data[['home_lon', 'home_lat']].iloc[0]
            ax1.scatter(home['home_lon'], home['home_lat'],
                       marker='*', s=300, c=colors[idx], 
                       edgecolors='black', linewidths=2, zorder=10)
        
        ax1.set_title('실제 도로 네트워크 기반 GPS 궤적', fontsize=14, fontweight='bold')
        ax1.legend(markerscale=10)
        ax1.set_xlabel('경도')
        ax1.set_ylabel('위도')
        
        # 2. 한 환자의 하루 궤적 상세
        ax2 = axes[1]
        
        # 한 환자의 하루치 데이터
        sample_patient = df[df['user_no'] == 1]
        one_day = sample_patient[
            sample_patient['timestamp'].dt.date == sample_patient['timestamp'].dt.date.iloc[500]
        ]
        
        # 시간에 따른 색상 그라데이션
        times = one_day['timestamp'].dt.hour + one_day['timestamp'].dt.minute / 60
        scatter = ax2.scatter(one_day['longitude'], one_day['latitude'],
                             c=times, cmap='viridis', s=20, alpha=0.7)
        
        # 집 위치
        home = one_day[['home_lon', 'home_lat']].iloc[0]
        ax2.scatter(home['home_lon'], home['home_lat'],
                   marker='*', s=500, c='red', 
                   edgecolors='black', linewidths=3, zorder=10, label='집')
        
        # 경로 선으로 연결
        ax2.plot(one_day['longitude'], one_day['latitude'], 
                'gray', linewidth=1, alpha=0.3, zorder=1)
        
        cbar = plt.colorbar(scatter, ax=ax2)
        cbar.set_label('시간 (Hour)', fontsize=12)
        
        ax2.set_title('하루 이동 경로 상세 (환자 1)', fontsize=14, fontweight='bold')
        ax2.legend()
        ax2.set_xlabel('경도')
        ax2.set_ylabel('위도')
        ax2.grid(True, alpha=0.3)
        
        plt.tight_layout()
        plt.savefig(save_path, dpi=200, bbox_inches='tight')
        print(f"📊 도로 네트워크 시각화 저장: {save_path}")
        plt.show()


    def analyze_patient_trajectory(self, df, user_no=1):
        """환자의 주요 방문 장소 및 이동 패턴 분석 (시간대별 장소 포함)"""
        from sklearn.cluster import DBSCAN
        
        patient_df = df[df['user_no'] == user_no].copy()
        
        if patient_df.empty:
            print(f"⚠️ 환자 {user_no}의 데이터가 없습니다.")
            return None
        
        print(f"\n{'='*70}")
        print(f"📊 환자 {user_no} 궤적 분석")
        print(f"{'='*70}\n")
        
        # 1. 기본 통계
        date_range = pd.to_datetime(patient_df['timestamp'])
        print("📅 기간 정보\n")
        print(f"{'='*70}")
        print(f"시작:                {date_range.min()}")
        print(f"종료:                {date_range.max()}")
        print(f"기간:                {(date_range.max() - date_range.min()).days}일")
        print(f"GPS 포인트 수:       {len(patient_df):,}개")
        print(f"{'='*70}\n")
        
        # 2. DBSCAN 클러스터링으로 주요 방문 장소 탐지
        coords = patient_df[['latitude', 'longitude']].values
        
        # DBSCAN: eps=30m (약 0.0003도), min_samples=10
        clustering = DBSCAN(eps=0.0003, min_samples=10).fit(coords)
        patient_df['location_cluster'] = clustering.labels_
        
        # 3. 클러스터별 통계
        print("📍 주요 방문 장소 분석\n")
        print(f"{'='*70}")
        
        location_stats = []
        unique_labels = set(clustering.labels_)
        
        home_lat = patient_df['home_lat'].iloc[0]
        home_lon = patient_df['home_lon'].iloc[0]
        
        # 시간대 정의
        time_periods = [
            ('새벽 (00-06시)', list(range(0, 6))),
            ('아침 (06-10시)', list(range(6, 10))),
            ('오전 (10-12시)', list(range(10, 12))),
            ('점심 (12-14시)', list(range(12, 14))),
            ('오후 (14-18시)', list(range(14, 18))),
            ('저녁 (18-22시)', list(range(18, 22))),
            ('밤 (22-24시)', list(range(22, 24)))
        ]
        
        patient_df['hour'] = pd.to_datetime(patient_df['timestamp']).dt.hour
        
        for label in sorted(unique_labels):
            if label == -1:  # 노이즈
                continue
                
            cluster_data = patient_df[patient_df['location_cluster'] == label]
            
            lat_mean = cluster_data['latitude'].mean()
            lon_mean = cluster_data['longitude'].mean()
            
            # 집으로부터 거리
            distance_from_home = geodesic((home_lat, home_lon), (lat_mean, lon_mean)).meters
            
            # 집 여부 판단 (30m 이내)
            is_home = distance_from_home < 30
            
            # 방향 계산
            delta_lat = lat_mean - home_lat
            delta_lon = lon_mean - home_lon
            angle_rad = np.arctan2(delta_lon, delta_lat)
            angle_deg = (np.degrees(angle_rad) + 360) % 360
            
            # 방향 이름
            if 337.5 <= angle_deg or angle_deg < 22.5:
                direction = '북'
            elif 22.5 <= angle_deg < 67.5:
                direction = '북동'
            elif 67.5 <= angle_deg < 112.5:
                direction = '동'
            elif 112.5 <= angle_deg < 157.5:
                direction = '남동'
            elif 157.5 <= angle_deg < 202.5:
                direction = '남'
            elif 202.5 <= angle_deg < 247.5:
                direction = '남서'
            elif 247.5 <= angle_deg < 292.5:
                direction = '서'
            else:
                direction = '북서'
            
            # 시간대별 방문 비율 계산
            time_distribution = {}
            for period_name, hours in time_periods:
                period_visits = len(cluster_data[cluster_data['hour'].isin(hours)])
                time_distribution[period_name] = period_visits
            
            # 가장 많이 방문한 시간대
            most_visited_period = max(time_distribution, key=time_distribution.get)
            
            location_stats.append({
                'label': label,
                'name': '집' if is_home else f'{direction}쪽 장소',
                'lat': lat_mean,
                'lon': lon_mean,
                'visits': len(cluster_data),
                'distance': distance_from_home,
                'direction': direction,
                'angle': angle_deg,
                'is_home': is_home,
                'time_distribution': time_distribution,
                'most_visited_period': most_visited_period
            })
        
        # 방문 횟수 기준 정렬
        location_stats.sort(key=lambda x: x['visits'], reverse=True)
        
        for loc in location_stats[:10]:  # 상위 10개
            print(f"\n{loc['name']:20s} (라벨: {loc['label']})")
            print(f"   위치:      ({loc['lat']:.6f}, {loc['lon']:.6f})")
            print(f"   방문:      {loc['visits']:>5}회")
            print(f"   거리:      {loc['distance']:>6.0f}m")
            if not loc['is_home']:
                print(f"   방향:      {loc['direction']:>5}쪽 ({loc['angle']:.1f}°)")
            print(f"   주 시간대: {loc['most_visited_period']}")
            
            # 시간대별 방문 비율 (상위 3개)
            sorted_periods = sorted(loc['time_distribution'].items(), 
                                   key=lambda x: x[1], reverse=True)
            print(f"   시간대별:")
            for period_name, count in sorted_periods[:3]:
                if count > 0:
                    percentage = (count / loc['visits']) * 100
                    print(f"      {period_name}: {count}회 ({percentage:.1f}%)")
        
        print(f"\n{'='*70}\n")
        
        # 4. 이동 거리 분석
        print("🚶 이동 거리 분석\n")
        print(f"{'='*70}")
        
        move_distances = []
        for i in range(1, len(patient_df)):
            prev_row = patient_df.iloc[i-1]
            curr_row = patient_df.iloc[i]
            
            prev_lat, prev_lon = prev_row['latitude'], prev_row['longitude']
            curr_lat, curr_lon = curr_row['latitude'], curr_row['longitude']
            
            dist = geodesic((prev_lat, prev_lon), (curr_lat, curr_lon)).meters
            move_distances.append(dist)
        
        move_distances = [0] + move_distances  # 첫 번째는 0
        patient_df['move_dist'] = move_distances
        
        total_distance = sum(move_distances)
        daily_avg_distance = total_distance / date_range.days
        max_single_move = max(move_distances) if move_distances else 0
        
        print(f"총 이동 거리:        {total_distance/1000:>8.2f} km")
        print(f"일평균 이동 거리:    {daily_avg_distance/1000:>8.2f} km")
        print(f"최대 단일 이동:      {max_single_move:>8.1f} m")
        print(f"{'='*70}\n")
        
        # 5. 요일별 패턴
        patient_df['weekday'] = patient_df['timestamp'].dt.day_name()
        weekday_order = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
        weekday_kr = ['월요일', '화요일', '수요일', '목요일', '금요일', '토요일', '일요일']
        
        print("📅 요일별 활동 패턴\n")
        print(f"{'='*70}")
        
        for eng_day, kr_day in zip(weekday_order, weekday_kr):
            day_data = patient_df[patient_df['weekday'] == eng_day]
            if len(day_data) > 0:
                day_distance = day_data['move_dist'].sum() / 1000
                day_count = len(day_data) / (24 * 20)  # 대략 몇 일치인지
                print(f"{kr_day:<8} 이동거리: {day_distance:>6.2f} km  (평균: {day_distance/max(day_count,1):>5.2f} km/일)")
        
        print(f"{'='*70}\n")
        
        # 6. 장소 간 이동 빈도 (상위 5개)
        print("🔄 장소 간 이동 빈도 (Top 5)\n")
        print(f"{'='*70}")
        
        # 연속된 클러스터 변화 추적
        transitions = []
        prev_label = None
        
        for idx, row in patient_df.iterrows():
            curr_label = row['location_cluster']
            if prev_label is not None and curr_label != prev_label and curr_label != -1 and prev_label != -1:
                transitions.append((prev_label, curr_label))
            prev_label = curr_label
        
        if transitions:
            from collections import Counter
            transition_counts = Counter(transitions)
            
            for (from_label, to_label), count in transition_counts.most_common(5):
                from_name = next((loc['name'] for loc in location_stats if loc['label'] == from_label), f"장소{from_label}")
                to_name = next((loc['name'] for loc in location_stats if loc['label'] == to_label), f"장소{to_label}")
                print(f"{from_name} → {to_name}: {count}회")
        else:
            print("이동 데이터 부족")
        
        print(f"{'='*70}\n")
        
        # 7. 추가: 시간대별 이동 거리
        print("⏰ 시간대별 이동 거리\n")
        print(f"{'='*70}")
        
        for period_name, hours in time_periods:
            period_data = patient_df[patient_df['hour'].isin(hours)]
            if len(period_data) > 0:
                period_distance = period_data['move_dist'].sum() / 1000
                print(f"{period_name:<18} {period_distance:>6.2f} km")
        
        print(f"{'='*70}\n")
        
        return location_stats


    def visualize_patient_locations(self, df, user_no=1, location_stats=None, 
                                    save_path='patient_locations.png'):
        """환자의 주요 방문 장소 시각화"""
        patient_df = df[df['user_no'] == user_no].copy()

        if patient_df.empty:
            return

        home_lat = patient_df['home_lat'].iloc[0]
        home_lon = patient_df['home_lon'].iloc[0]

        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 8))

        # 1. 지도 상 실제 위치
        colors = plt.cm.tab10(np.linspace(0, 1, 10))

        # 전체 궤적 (희미하게)
        ax1.scatter(patient_df['longitude'], patient_df['latitude'], 
                   s=0.5, alpha=0.1, c='gray')

        # 집
        ax1.scatter(home_lon, home_lat, marker='*', s=500, c='red', 
                   edgecolors='black', linewidths=2, zorder=10, label='집')

        if location_stats:
            for loc in location_stats:
                if not loc['is_home']:
                    color_idx = loc['label'] % 10
                    ax1.scatter(loc['lon'], loc['lat'], s=300, c=[colors[color_idx]], 
                               alpha=0.7, edgecolors='black', linewidths=2, zorder=5)
                    ax1.annotate(f"{loc['name']}\n{loc['visits']}회", 
                                (loc['lon'], loc['lat']), 
                                xytext=(10, 10), textcoords='offset points', 
                                fontsize=9, fontweight='bold',
                                bbox=dict(boxstyle='round,pad=0.5', facecolor='white', alpha=0.7))

        ax1.set_xlabel('경도', fontsize=12)
        ax1.set_ylabel('위도', fontsize=12)
        ax1.set_title(f'환자 {user_no} 주요 방문 장소', fontsize=14, fontweight='bold')
        ax1.legend(fontsize=11)
        ax1.grid(True, alpha=0.3)

        # 2. 방향별 분포 (극좌표)
        ax2 = plt.subplot(122, projection='polar')
        ax2.set_theta_zero_location('N')
        ax2.set_theta_direction(-1)

        if location_stats:
            for loc in location_stats:
                if not loc['is_home']:
                    angle_rad = np.radians(loc['angle'])
                    distance = loc['distance']
                    color_idx = loc['label'] % 10

                    # 방문 빈도에 따라 크기 조절
                    size = 100 + loc['visits'] * 20

                    ax2.scatter(angle_rad, distance, s=size, c=[colors[color_idx]], 
                               alpha=0.7, edgecolors='black', linewidths=2)
                    ax2.annotate(loc['name'], (angle_rad, distance), 
                                xytext=(5, 5), textcoords='offset points', fontsize=9)

        # 8방향 표시
        directions_deg = [0, 45, 90, 135, 180, 225, 270, 315]
        direction_labels = ['북', '북동', '동', '남동', '남', '남서', '서', '북서']

        for deg, label in zip(directions_deg, direction_labels):
            ax2.plot([np.radians(deg), np.radians(deg)], [0, 3000], 
                    'gray', linewidth=0.5, alpha=0.3)
            ax2.text(np.radians(deg), 3200, label, ha='center', va='center', 
                    fontsize=11, fontweight='bold')

        ax2.set_ylim(0, 3000)
        ax2.set_title(f'환자 {user_no} 방향별 거리 분포', fontsize=14, fontweight='bold', pad=20)
        ax2.set_ylabel('거리 (m)', labelpad=30, fontsize=11)

        plt.tight_layout()
        plt.savefig(save_path, dpi=200, bbox_inches='tight')
        print(f"📊 환자 {user_no} 위치 시각화 저장: {save_path}")
        plt.show()


# ============================================================
# 메인 실행 코드
# ============================================================
def main():
    print("\n" + "="*70)
    print("🚀 실제 도로 기반 GPS 궤적 생성기 (00~06시 집 강제)")
    print("="*70)
    
    # 1. 생성기 초기화
    generator = RoadBasedGPSGenerator(
        center_lat=37.234257,
        center_lon=126.681727,
        distance=3000,
        use_elevation=True,
        elevation_weight=2.0
    )
    
    # 2. GPS 데이터 생성
    df = generator.generate_multiple_patients(
        n_patients=4,
        days=30,
        interval_minutes=3
    )
    
    # 3. CSV 저장
    filename = 'road_based_gps_data.csv'
    df.to_csv(filename, index=False, encoding='utf-8-sig')
    print(f"💾 CSV 저장 완료: {filename}")
    
    # 4. 환자 1 상세 분석 ⭐ 새로 추가!
    location_stats = generator.analyze_patient_trajectory(df, user_no=1)
    
    # 5. 환자 1 시각화 ⭐ 새로 추가!
    generator.visualize_patient_locations(df, user_no=1, location_stats=location_stats)
    
    # 6. 전체 시각화
    generator.visualize_road_network_and_trajectories(df)
    
    # 7. 기본 통계
    print(f"\n{'='*70}")
    print("📊 전체 데이터 요약")
    print(f"{'='*70}")
    print(f"날짜 범위: {df['timestamp'].min()} ~ {df['timestamp'].max()}")
    print(f"총 GPS 포인트: {len(df):,}개")
    print(f"환자당 평균: {len(df) / df['user_no'].nunique():.0f}개")
    print(f"{'='*70}\n")
    
    return df


if __name__ == "__main__":
    # 실행!
    df = main()
    
    print("\n" + "="*70)
    print("💡 사용 팁:")
    print("="*70)
    print("1. 환자 수/기간 조정:")
    print("   generator.generate_multiple_patients(n_patients=30, days=30)")
    print("\n2. 다른 지역 사용:")
    print("   RoadBasedGPSGenerator(center_lat=37.5665, center_lon=126.9780)")
    print("\n3. 더 넓은 범위:")
    print("   RoadBasedGPSGenerator(..., distance=5000)  # 5km 반경")
    print("="*70 + "\n")
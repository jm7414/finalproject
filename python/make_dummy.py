"""
실제 도로 네트워크를 따라 이동하는 GPS 궤적 생성기
OpenStreetMap 기반으로 현실적인 경로 생성
야간 시간대(00~06시) 집 위치 완전 보장 버전

필요한 패키지 설치:
pip install osmnx networkx pandas numpy geopy matplotlib srtm scikit-learn
"""
import osmnx as ox
import networkx as nx
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from geopy.distance import geodesic
import matplotlib.pyplot as plt
import warnings
import srtm
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
    """실제 도로 네트워크 기반 GPS 생성기 (고도 고려 + 야간 시간대 완전 보장)"""
    
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
        self.graph = ox.get_undirected(self.graph)
        
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
        
        for node, data in self.graph.nodes(data=True):
            lat, lon = data['y'], data['x']
            try:
                elevation = self.elevation_data.get_elevation(lat, lon)
                data['elevation'] = elevation if elevation is not None else 0
            except Exception as e:
                data['elevation'] = 0
        
        print("   경사도 및 가중치 계산 중...")
        for u, v, key, data in self.graph.edges(keys=True, data=True):
            elev_start = self.graph.nodes[u].get('elevation', 0)
            elev_end = self.graph.nodes[v].get('elevation', 0)
            
            length = data.get('length', 1.0)
            elevation_change = abs(elev_end - elev_start)
            grade = elevation_change / length if length > 0 else 0
            
            data['grade'] = grade * 100
            data['elevation_change'] = elevation_change
            
            elevation_penalty = np.exp(grade * self.elevation_weight)
            data['weighted_length'] = length * elevation_penalty
        
        print("✅ 고도 데이터 추가 완료!")
        
        grades = [data['grade'] for _, _, _, data in self.graph.edges(keys=True, data=True)]
        print(f"   평균 경사도: {np.mean(grades):.2f}%")
        print(f"   최대 경사도: {np.max(grades):.2f}%")
        print(f"   중앙 경사도: {np.median(grades):.2f}%")
    
    def get_nearest_node(self, lat, lon):
        """가장 가까운 도로 노드 찾기"""
        return ox.distance.nearest_nodes(self.graph, lon, lat)
    
    def find_route(self, start_node, end_node, avoid_steep=True):
        """두 노드 사이의 최단 경로 찾기"""
        try:
            weight = 'weighted_length' if (avoid_steep and self.use_elevation) else 'length'
            route = nx.shortest_path(self.graph, start_node, end_node, weight=weight)
            return route
        except (nx.NetworkXNoPath, nx.NodeNotFound):
            return None
    
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
        """환자의 주요 방문 장소 생성 (방향 다양성 고려)"""
        locations = {'home': {'lat': home_lat, 'lon': home_lon, 'node': None}}
        home_node = self.get_nearest_node(home_lat, home_lon)
        locations['home']['node'] = home_node
        
        home_elevation = self.graph.nodes[home_node].get('elevation', 0)
        all_nodes = list(self.graph.nodes())
        
        place_types = [
            '슈퍼마켓', '공원', '병원', '약국', 
            '복지관', '시장', '버스정류장', '친구집',
            '카페', '은행', '우체국', '도서관'
        ]
        
        selected_nodes = set([home_node])
        
        if ensure_directional_diversity:
            directions = [
                ('북쪽', 0, 45),
                ('북동쪽', 45, 90),
                ('동쪽', 90, 135),
                ('남동쪽', 135, 180),
                ('남쪽', 180, 225),
                ('남서쪽', 225, 270),
                ('서쪽', 270, 315),
                ('북서쪽', 315, 360)
            ]
            
            print(f"\n   📍 방향별 장소 배치 (집 고도: {home_elevation:.1f}m)")
            print(f"   {'='*65}")
            
            direction_idx = 0
            
            for i in range(min(n_locations, len(place_types))):
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
                    
                    dist = geodesic((home_lat, home_lon), (node_lat, node_lon)).meters
                    
                    delta_lat = node_lat - home_lat
                    delta_lon = node_lon - home_lon
                    angle_rad = np.arctan2(delta_lon, delta_lat)
                    angle_deg = (np.degrees(angle_rad) + 360) % 360
                    
                    elevation_diff = abs(node_elevation - home_elevation)
                    
                    distance_ok = 100 <= dist <= 2500
                    elevation_ok = elevation_diff <= max_elevation_diff
                    
                    angle_margin = 20
                    if angle_max == 360:
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
        
        return locations
    
    def get_time_pattern(self, hour):
        """시간대별 방문 확률 패턴"""
        if 0 <= hour < 6:
            return {'home': 1.0}  # 수면 시간
        elif 6 <= hour < 8:
            return {'home': 0.6, 'loc_0': 0.25, 'loc_1': 0.15}
        elif 8 <= hour < 10:
            return {'home': 0.2, 'loc_1': 0.3, 'loc_5': 0.25, 'loc_0': 0.25}
        elif 10 <= hour < 12:
            return {'home': 0.1, 'loc_2': 0.25, 'loc_4': 0.25, 'loc_1': 0.2, 'loc_3': 0.2}
        elif 12 <= hour < 14:
            return {'home': 0.4, 'loc_5': 0.3, 'loc_0': 0.2, 'loc_7': 0.1}
        elif 14 <= hour < 16:
            return {'home': 0.15, 'loc_3': 0.25, 'loc_4': 0.25, 'loc_7': 0.2, 'loc_2': 0.15}
        elif 16 <= hour < 18:
            return {'home': 0.2, 'loc_7': 0.3, 'loc_8': 0.25, 'loc_1': 0.15, 'loc_5': 0.1}
        elif 18 <= hour < 20:
            return {'home': 0.5, 'loc_0': 0.25, 'loc_5': 0.15, 'loc_6': 0.1}
        elif 20 <= hour < 22:
            return {'home': 0.7, 'loc_0': 0.15, 'loc_1': 0.1, 'loc_7': 0.05}
        else:  # 22-24시
            return {'home': 0.9, 'loc_0': 0.08, 'loc_1': 0.02}
    
    def generate_patient_trajectory(self, user_no, days=30, interval_minutes=3):
        """한 환자의 실제 도로 기반 GPS 궤적 생성 (야간 시간대 완전 보장)"""
        print(f"🚶 환자 {user_no} 궤적 생성 중...")

        all_nodes = list(self.graph.nodes())
        home_node = np.random.choice(all_nodes)
        home_data = self.graph.nodes[home_node]
        home_lat, home_lon = home_data['y'], home_data['x']

        # 주요 방문 장소 생성
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

            # ⭐⭐⭐ 핵심 수정: 야간 시간대(00~06시) 강제 귀가 ⭐⭐⭐
            if 0 <= hour < 6:
                # 야간 시간대에는 무조건 집에 있어야 함
                if current_location != 'home' or is_moving:
                    # 집이 아니거나 이동 중이면 강제로 집으로 설정
                    current_location = 'home'
                    current_coords = (home_lat, home_lon)
                    route_buffer = [current_coords]
                    route_idx = 0
                    is_moving = False
                    # 6시까지 남은 시간 계산
                    time_until_6am = (6 - hour) * 60 - current_time.minute
                    if time_until_6am > 0:
                        stay_until = current_time + timedelta(minutes=time_until_6am)
                
                # 야간에는 집 좌표만 사용
                lat, lon = home_lat, home_lon
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
                continue  # ⭐ 야간에는 다른 로직 실행하지 않음

            # 기존 로직 (낮 시간대: 06~24시)
            if len(route_buffer) == 0 or route_idx >= len(route_buffer):
                if is_moving:
                    is_moving = False
                    if current_location == 'home':
                        stay_duration = np.random.randint(30, 120)
                    else:
                        stay_duration = np.random.randint(15, 60)
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
        
        ax1 = axes[0]
        ox.plot_graph(self.graph, ax=ax1, show=False, close=False, 
                      node_size=0, edge_color='lightgray', edge_linewidth=0.5)
        
        sample_patients = df['user_no'].unique()[:3]
        colors = ['red', 'blue', 'green']
        
        for idx, user_no in enumerate(sample_patients):
            patient_data = df[df['user_no'] == user_no].sample(frac=0.1)
            ax1.scatter(patient_data['longitude'], patient_data['latitude'],
                       s=1, alpha=0.5, c=colors[idx], label=f'환자 {user_no}')
            
            home = patient_data[['home_lon', 'home_lat']].iloc[0]
            ax1.scatter(home['home_lon'], home['home_lat'],
                       marker='*', s=300, c=colors[idx], 
                       edgecolors='black', linewidths=2, zorder=10)
        
        ax1.set_title('실제 도로 네트워크 기반 GPS 궤적', fontsize=14, fontweight='bold')
        ax1.legend(markerscale=10)
        ax1.set_xlabel('경도')
        ax1.set_ylabel('위도')
        
        ax2 = axes[1]
        sample_patient = df[df['user_no'] == 1]
        one_day = sample_patient[
            sample_patient['timestamp'].dt.date == sample_patient['timestamp'].dt.date.iloc[500]
        ]
        
        times = one_day['timestamp'].dt.hour + one_day['timestamp'].dt.minute / 60
        scatter = ax2.scatter(one_day['longitude'], one_day['latitude'],
                             c=times, cmap='viridis', s=20, alpha=0.7)
        
        home = one_day[['home_lon', 'home_lat']].iloc[0]
        ax2.scatter(home['home_lon'], home['home_lat'],
                   marker='*', s=500, c='red', 
                   edgecolors='black', linewidths=3, zorder=10, label='집')
        
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
        print(f"📊 시각화 저장: {save_path}")
        plt.show()
    
    def analyze_patient_trajectory(self, df, user_no=1):
        """환자의 궤적 분석"""
        from sklearn.cluster import DBSCAN
        
        patient_df = df[df['user_no'] == user_no].copy()
        
        if patient_df.empty:
            print(f"환자 {user_no}의 데이터가 없습니다.")
            return None
        
        print(f"\n{'='*70}")
        print(f"📊 환자 {user_no} 이동 패턴 상세 분석")
        print(f"{'='*70}\n")
        
        home_lat = patient_df['home_lat'].iloc[0]
        home_lon = patient_df['home_lon'].iloc[0]
        total_points = len(patient_df)
        date_range = patient_df['timestamp'].max() - patient_df['timestamp'].min()
        
        print(f"🏠 집 위치: ({home_lat:.6f}, {home_lon:.6f})")
        print(f"📅 기록 기간: {date_range.days}일")
        print(f"📍 총 GPS 포인트: {total_points:,}개\n")
        
        # DBSCAN 클러스터링
        coords = patient_df[['latitude', 'longitude']].values
        clustering = DBSCAN(eps=0.0003, min_samples=20).fit(coords)
        patient_df['location_cluster'] = clustering.labels_
        
        unique_labels = set(clustering.labels_)
        unique_labels.discard(-1)
        
        print(f"🎯 주요 방문 장소: {len(unique_labels)}개소\n")
        print(f"{'='*70}")
        print(f"{'장소':<6} {'중심 좌표':<30} {'방문':<8} {'총 체류':<12} {'비율'}")
        print(f"{'='*70}")
        
        location_stats = []
        
        for label in sorted(unique_labels):
            cluster_data = patient_df[patient_df['location_cluster'] == label]
            
            center_lat = cluster_data['latitude'].mean()
            center_lon = cluster_data['longitude'].mean()
            
            visit_count = (cluster_data['location_cluster'].diff() != 0).sum()
            total_minutes = len(cluster_data) * 3
            total_hours = total_minutes / 60
            
            distance = geodesic((home_lat, home_lon), (center_lat, center_lon)).meters
            ratio = len(cluster_data) / total_points * 100
            
            is_home = (label == patient_df['location_cluster'].value_counts().idxmax())
            place_name = "🏠 집" if is_home else f"장소{label}"
            
            location_stats.append({
                'label': label,
                'name': place_name,
                'lat': center_lat,
                'lon': center_lon,
                'visits': visit_count,
                'hours': total_hours,
                'distance': distance,
                'ratio': ratio,
                'is_home': is_home
            })
            
            coord_str = f"({center_lat:.5f}, {center_lon:.5f})"
            print(f"{place_name:<6} {coord_str:<30} {visit_count:>4}회  "
                  f"{total_hours:>6.1f}시간  {ratio:>5.1f}%")
        
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

        colors = plt.cm.tab10(np.linspace(0, 1, 10))

        ax1.scatter(patient_df['longitude'], patient_df['latitude'], 
                   s=0.5, alpha=0.1, c='gray')

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

        ax2 = plt.subplot(122, projection='polar')
        ax2.set_theta_zero_location('N')
        ax2.set_theta_direction(-1)

        if location_stats:
            for loc in location_stats:
                if not loc['is_home']:
                    # 간단한 각도 계산
                    delta_lat = loc['lat'] - home_lat
                    delta_lon = loc['lon'] - home_lon
                    angle_rad = np.arctan2(delta_lon, delta_lat)
                    distance = loc['distance']
                    color_idx = loc['label'] % 10

                    size = 100 + loc['visits'] * 20

                    ax2.scatter(angle_rad, distance, s=size, c=[colors[color_idx]], 
                               alpha=0.7, edgecolors='black', linewidths=2)

        ax2.set_ylim(0, 3000)
        ax2.set_title(f'환자 {user_no} 방향별 거리 분포', fontsize=14, fontweight='bold', pad=20)
        ax2.set_ylabel('거리 (m)', labelpad=30, fontsize=11)

        plt.tight_layout()
        plt.savefig(save_path, dpi=200, bbox_inches='tight')
        print(f"📊 환자 {user_no} 위치 시각화 저장: {save_path}")
        plt.show()


# ============================================================
# 야간 시간대 검증 함수
# ============================================================
def verify_night_time_location(df, user_no=1):
    """야간 시간대(00~06시)에 환자가 집에 있는지 검증"""
    
    print(f"\n{'='*70}")
    print(f"🔍 환자 {user_no} 야간 시간대(00~06시) 위치 검증")
    print(f"{'='*70}\n")
    
    patient_df = df[df['user_no'] == user_no].copy()
    
    if patient_df.empty:
        print(f"환자 {user_no}의 데이터가 없습니다.")
        return None
    
    home_lat = patient_df['home_lat'].iloc[0]
    home_lon = patient_df['home_lon'].iloc[0]
    
    patient_df['hour'] = patient_df['timestamp'].dt.hour
    night_data = patient_df[(patient_df['hour'] >= 0) & (patient_df['hour'] < 6)]
    
    print(f"📊 야간 시간대 GPS 포인트: {len(night_data):,}개")
    print(f"🏠 집 좌표: ({home_lat:.6f}, {home_lon:.6f})\n")
    
    distances = []
    for idx, row in night_data.iterrows():
        dist = geodesic((home_lat, home_lon), (row['latitude'], row['longitude'])).meters
        distances.append(dist)
    
    night_data['distance_from_home'] = distances
    
    max_distance = night_data['distance_from_home'].max()
    mean_distance = night_data['distance_from_home'].mean()
    
    print(f"집으로부터 최대 거리: {max_distance:.2f}m")
    print(f"집으로부터 평균 거리: {mean_distance:.2f}m")
    
    threshold = 10.0
    
    at_home_count = (night_data['distance_from_home'] <= threshold).sum()
    not_at_home_count = (night_data['distance_from_home'] > threshold).sum()
    
    print(f"\n집에 있는 포인트 (≤{threshold}m): {at_home_count:,}개 ({at_home_count/len(night_data)*100:.2f}%)")
    print(f"집에 없는 포인트 (>{threshold}m): {not_at_home_count:,}개 ({not_at_home_count/len(night_data)*100:.2f}%)")
    
    if not_at_home_count > 0:
        print(f"\n⚠️ 경고: {not_at_home_count}개의 야간 포인트가 집에서 {threshold}m 이상 떨어져 있습니다!")
        print("\n가장 먼 거리의 포인트 (상위 5개):")
        print(night_data.nlargest(5, 'distance_from_home')[['timestamp', 'latitude', 'longitude', 'distance_from_home']])
    else:
        print(f"\n✅ 모든 야간 시간대 GPS 데이터가 집에 있습니다!")
    
    print(f"\n{'='*70}\n")
    
    return night_data


# ============================================================
# 메인 실행 코드
# ============================================================
def main():
    print("\n" + "="*70)
    print("🚀 실제 도로 기반 GPS 궤적 생성기 (야간 시간대 완전 보장)")
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
    filename = 'road_based_gps_data_fixed.csv'
    df.to_csv(filename, index=False, encoding='utf-8-sig')
    print(f"💾 CSV 저장 완료: {filename}")
    
    # 4. ⭐ 야간 시간대 검증
    print("\n" + "="*70)
    print("🌙 야간 시간대(00~06시) 데이터 검증 시작")
    print("="*70)
    
    for user_no in range(1, min(5, df['user_no'].nunique() + 1)):
        verify_night_time_location(df, user_no=user_no)
    
    # 5. 환자 1 상세 분석
    location_stats = generator.analyze_patient_trajectory(df, user_no=1)
    
    # 6. 환자 1 시각화
    generator.visualize_patient_locations(df, user_no=1, location_stats=location_stats)
    
    # 7. 전체 시각화
    generator.visualize_road_network_and_trajectories(df)
    
    # 8. 기본 통계
    print(f"\n{'='*70}")
    print("📊 전체 데이터 요약")
    print(f"{'='*70}")
    print(f"날짜 범위: {df['timestamp'].min()} ~ {df['timestamp'].max()}")
    print(f"총 GPS 포인트: {len(df):,}개")
    print(f"환자당 평균: {len(df) / df['user_no'].nunique():.0f}개")
    print(f"{'='*70}\n")
    
    return df


if __name__ == "__main__":
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
    print("\n4. 야간 검증:")
    print("   verify_night_time_location(df, user_no=1)")
    print("="*70 + "\n")

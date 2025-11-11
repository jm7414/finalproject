import osmnx as ox

# 연구지역 중심 좌표와 반경(m)
center_point = (37.5263, 126.8758)
distance = 3000  # 미터 단위

# 네트워크 추출 (보행자 네트워크)
G = ox.graph_from_point(center_point, dist=distance, network_type='walk')

# 고도 데이터 추가 (예: Google API 키 없으면 SRTM 사용 가능)
# G = ox.add_node_elevations_google(G, api_key="your_key")
G = ox.add_node_elevations_raster(G, "srtm_data.tif")

# 경사도(edge grades) 계산
G = ox.add_edge_grades(G)

# 그래프 저장 (모든 속성 포함)
ox.save_graphml(G, filepath="./data/study_area_network.graphml")

# 저장된 그래프 불러오기 (재사용 가능)
# G_loaded = ox.load_graphml("./data/study_area_network.graphml")

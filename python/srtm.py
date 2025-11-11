import elevation
import rasterio

# SRTM 데이터 다운로드 (bbox: left, bottom, right, top)
# 필요한 좁은 영역 경계 지정 (longitude, latitude) 소수점 좌표
bbox = (126.7, 37.4, 127.0, 37.6)

elevation.clip(bounds=bbox, output='srtm_clip.tif', product='SRTM1')

# geotiff 파일 열기
with rasterio.open('srtm.tif') as dataset:
    # 좌표(x, y)에 해당하는 고도값 읽기
    # example: latitude, longitude 좌표
    lat, lon = 37.5263, 126.8758
    row, col = dataset.index(lon, lat)
    elevation_value = dataset.read(1)[row, col]
    print(f"Elevation at ({lat}, {lon}): {elevation_value} meters")

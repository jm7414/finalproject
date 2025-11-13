image.png에서 보면 C:\final-project\python\venv\Scripts\Activate.ps1 이라고 되어있는 노란색 글자가
venv\Scripts\Activate.ps1은 동일한데 앞에 파일 경로만 현재 최종프로젝트 폴더의 python폴더까지 열어서 진행하면 앞에 PS C:\final-project\python> 이렇게 시작하는 부분이 현재 디렉토리입니다.

venv까지 올렸기 때문에 사진에서 두번째줄 명령어만 실행시키면 됩니다.(이걸 가상환경을 실행시킨다고 합니다.)
여기서 밑에 초록으로 (venv) 라고 되어있으면 끝
---

프로젝트에서 실행되어야하는 파일은 FastAPI.py라는 파일인데 이것을 실행하기 위해서는 라이브러리를 불러와야하는 것들이 많아요.
차근차근 불러와봅시다

1. 간단한 numpy, pandas 불러오기
pip install numpy pandas pydantic

2. 오픈스트리트 라이브러리 불러오기
pip install osmnx networkx 

3. asyncio
pip install asyncio==4.0.0 

4. fastapi uvicorn
pip install fastapi uvicorn 

5. Mesa (에이전트 생성 및 진행에 필요)
pip install scikit-learn==1.3.2 Mesa==2.1.5 Mesa-Geo==0.6.0

6. hdbscan
pip install hdbscan

7. shapely
pip install shapely==2.0.7
---

이렇게 다 했으면 이제 FastAPI.py파일을 열어봤을 때 아무런 노란색 밑줄이 없을거에요
그렇다면 이제 서버를 실행시켜봅시다.
---

uvicorn FastAPI_v2:app --host 0.0.0.0 --port 8000 --reload

or

python FastAPI_v2.py

이제 이러면 Application startup complete가 보이면 완전히 실행된것임

localhost:8000/docs

로 이동하면 여기서 실행된 FastAPI가 보일텐데 여기는 그냥 Test용도임 그냥 궁굼하면 여기서 뚝딱뚝딱하면됨

![첫번째 : 가상환경 실행하기](image.png)
![두번째 : numpy, pandas 설치](image-1.png)
![uvicorn 실행시 나타나는 로그? ](image-2.png)
![docs에서 test실행](image-3.png)


==============================================================================
추가적으로 postgreSQL에 넣어야할 것 ((각자 파일경로 달라서 두번째줄에 FROM 'C:\~~이 부분 고쳐야함'))

-- user_location 테이블에 CSV 데이터 삽입
COPY user_location(user_no, latitude, longitude, record_time)
FROM 'C:\final-project\python\gps_trajectory_1year.csv'
DELIMITER ','
CSV HEADER;



# Core Web Framework
fastapi==0.120.1
pydantic==2.10.6

# Data Processing
numpy==1.24.4
pandas==2.0.3

# Clustering
hdbscan==0.8.40

# Async
asyncio==4.0.0

# Machine Learning
scikit-learn==1.3.2

# Geospatial & Network
osmnx==1.9.4
networkx==3.1
shapely==2.0.7

# Agent-Based Modeling
Mesa==2.1.5
Mesa-Geo==0.6.0

-- ===================================================================
-- 테스트용 더미 데이터 INSERT 쿼리문
-- ===================================================================

-- 1. 보호자-환자 연결 테이블에 더미 데이터 추가
-- 1번, 2번 보호자가 3번 환자를 관리하는 관계 설정
INSERT INTO guardian_patient_connection (patient_no, guardian1_no, guardian2_no, guardian3_no) 
VALUES (3, 1, 2, NULL);

-- 2. 추가 테스트 유저 생성 (선택 사항)
-- 만약 더 많은 테스트가 필요하다면 아래 주석을 해제하여 사용

/*
-- 추가 환자 생성 (user_no: 4)
INSERT INTO users (user_id, user_pw, name, role_no, invitation_code) VALUES
('patient_lee', '$2a$10$sampleHashedPassword', '이철수', 2, 'ABCD1234');

-- 추가 보호자 생성 (user_no: 5)
INSERT INTO users (user_id, user_pw, name, role_no) VALUES
('guardian_kim', '$2a$10$sampleHashedPassword', '김민수', 1);

-- 추가 보호자-환자 연결 (5번 보호자가 4번 환자 관리)
INSERT INTO guardian_patient_connection (patient_no, guardian1_no, guardian2_no, guardian3_no) 
VALUES (4, 5, NULL, NULL);
*/

-- ===================================================================
-- 데이터 확인 쿼리
-- ===================================================================

-- 보호자-환자 연결 확인
SELECT 
    gpc.connection_no,
    p.user_no as patient_user_no,
    p.name as patient_name,
    g1.user_no as guardian1_user_no,
    g1.name as guardian1_name,
    g2.user_no as guardian2_user_no,
    g2.name as guardian2_name
FROM guardian_patient_connection gpc
JOIN users p ON gpc.patient_no = p.user_no
LEFT JOIN users g1 ON gpc.guardian1_no = g1.user_no
LEFT JOIN users g2 ON gpc.guardian2_no = g2.user_no;

-- 모든 유저 정보 확인
SELECT 
    user_no,
    user_id,
    name,
    role_no,
    CASE 
        WHEN role_no = 1 THEN '보호자'
        WHEN role_no = 2 THEN '환자'
        WHEN role_no = 3 THEN '구독자'
        ELSE '기타'
    END as role_name
FROM users
ORDER BY user_no;

-- ===================================================================
-- 참고: 일정 저장 후 데이터 확인 쿼리
-- ===================================================================

-- 일정 확인
SELECT 
    schedule_no,
    schedule_title,
    schedule_date,
    start_time,
    end_time,
    user_no
FROM schedule
ORDER BY schedule_date DESC, start_time DESC;

-- 일정 위치 확인
SELECT 
    sl.location_no,
    sl.location_name,
    sl.latitude,
    sl.longitude,
    sl.sequence_order,
    s.schedule_title
FROM schedule_location sl
JOIN schedule s ON sl.schedule_no = s.schedule_no
ORDER BY sl.schedule_no, sl.sequence_order;

-- 경로 확인
SELECT 
    r.route_no,
    s.schedule_title,
    r.schedule_no
FROM route r
JOIN schedule s ON r.schedule_no = s.schedule_no;

-- 안심존 확인
SELECT 
    sz.safe_zone_no,
    sz.zone_type,
    u.name as user_name,
    sz.user_no
FROM safe_zone sz
JOIN users u ON sz.user_no = u.user_no
ORDER BY sz.safe_zone_no DESC;

-- 특정 환자(3번)의 모든 일정 및 관련 정보 확인
SELECT 
    s.schedule_no,
    s.schedule_title,
    s.schedule_date,
    s.start_time,
    s.end_time,
    COUNT(DISTINCT sl.location_no) as location_count,
    CASE WHEN r.route_no IS NOT NULL THEN 'O' ELSE 'X' END as has_route,
    COUNT(DISTINCT sz.safe_zone_no) as safe_zone_count
FROM schedule s
LEFT JOIN schedule_location sl ON s.schedule_no = sl.schedule_no
LEFT JOIN route r ON s.schedule_no = r.schedule_no
LEFT JOIN safe_zone sz ON s.user_no = sz.user_no AND sz.zone_type = '경로형'
WHERE s.user_no = 3
GROUP BY s.schedule_no, s.schedule_title, s.schedule_date, s.start_time, s.end_time, r.route_no
ORDER BY s.schedule_date DESC, s.start_time DESC;


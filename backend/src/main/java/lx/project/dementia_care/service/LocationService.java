package lx.project.dementia_care.service;

import lx.project.dementia_care.dto.LocationRequestDTO;
import lx.project.dementia_care.dto.LocationResponseDTO;
import lx.project.dementia_care.dao.UserLocationDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final UserLocationDAO userLocationDAO;
    
    // 임시 위치 저장소 (메모리)
    private final Map<Integer, LocationResponseDTO> temporaryLocations = new ConcurrentHashMap<>();
    
    // 마지막 DB 저장 시간 추적
    private final Map<Integer, Long> lastDbSaveTime = new ConcurrentHashMap<>();

    /**
     * 임시 위치 저장소에 위치 업데이트
     */
    public void updateTemporaryLocation(LocationRequestDTO request) {
        long currentTime = System.currentTimeMillis();
        
        LocationResponseDTO location = new LocationResponseDTO();
        location.setUserNo(request.getUserNo());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : currentTime);
        location.setStatus("online");
        location.setLastUpdateTime(currentTime);
        
        // 임시 저장소에 저장
        temporaryLocations.put(request.getUserNo(), location);
        
        log.info("환자 {} 위치 업데이트: ({}, {})", 
            request.getUserNo(), request.getLatitude(), request.getLongitude());
    }

    /**
     * 환자 위치 조회 (임시 저장소에서)
     * [시연용] 항상 고정 좌표 반환 (경기도 화성시 구포리: 37.244364, 126.876748)
     */
    public LocationResponseDTO getPatientLocation(Integer patientUserNo) {
        // 시연용: 항상 고정 좌표 반환
        long currentTime = System.currentTimeMillis();
        LocationResponseDTO location = new LocationResponseDTO();
        location.setUserNo(patientUserNo);
        location.setLatitude(37.244364);
        location.setLongitude(126.876748);
        location.setTimestamp(currentTime);
        location.setStatus("online");
        location.setLastUpdateTime(currentTime);
        
        log.info("[시연용] 환자 {} 위치 고정 좌표 반환: ({}, {})", 
            patientUserNo, location.getLatitude(), location.getLongitude());
        
        return location;
    }

    /**
     * '함께하는 사람들' 위치 조회 (임시 저장소에서) - 여러 명 조회
     * (MissingPersonService에서 호출됨)
     * [시연용] 중심 좌표 근처 고정 좌표 반환
     *
     * @param userIds '함께하는 사람'들의 user_no 리스트
     * @return 위치 정보 DTO 리스트
     */
    public List<LocationResponseDTO> getParticipantLocations(List<Integer> userIds) {
        List<LocationResponseDTO> locations = new ArrayList<>();
        if (userIds == null || userIds.isEmpty()) {
            return locations; // ID 리스트가 비어있으면 빈 목록 반환
        }

        // 시연용: 중심 좌표 (경기도 화성시 구포리: 37.244364, 126.876748)
        double centerLat = 37.244364;
        double centerLng = 126.876748;
        
        // 중심 좌표 근처 5개의 고정 좌표 (반경 약 50-80m)
        double[][] fixedOffsets = {
            {0.0005, 0.0006},   // 북동쪽
            {-0.0004, 0.0007},  // 남동쪽
            {-0.0006, -0.0005}, // 남서쪽
            {0.0004, -0.0006},  // 북서쪽
            {0.0003, 0.0003}    // 북쪽
        };

        long currentTime = System.currentTimeMillis();

        // ID 목록을 순회하며 고정 좌표 할당 (최대 5명)
        int index = 0;
        for (Integer userId : userIds) {
            if (index >= 5) break; // 최대 5명까지만
            
            LocationResponseDTO location = new LocationResponseDTO();
            location.setUserNo(userId);
            
            // 고정 오프셋 적용
            double[] offset = fixedOffsets[index % fixedOffsets.length];
            location.setLatitude(centerLat + offset[0]);
            location.setLongitude(centerLng + offset[1]);
            location.setTimestamp(currentTime);
            location.setStatus("online");
            location.setLastUpdateTime(currentTime);
            
            locations.add(location);
            index++;
        }
        
        log.info("[시연용] 함께하는 사람들 {}명 위치 고정 좌표 반환", locations.size());
        
        return locations;
    }

    /**
     * 3분마다 실행되는 스케줄러 - DB에 위치 저장
     */
    @Scheduled(fixedRate = 3 * 60 * 1000) // 3분마다
    public void saveLocationsToDatabase() {
        log.info("위치 데이터 DB 저장 시작...");
        
        for (Map.Entry<Integer, LocationResponseDTO> entry : temporaryLocations.entrySet()) {
            Integer userNo = entry.getKey();
            LocationResponseDTO location = entry.getValue();
            
            try {
                // 마지막 DB 저장 시간 확인
                Long lastSave = lastDbSaveTime.get(userNo);
                long currentTime = System.currentTimeMillis();
                
                // 3분 이상 지났거나 처음 저장하는 경우
                if (lastSave == null || (currentTime - lastSave) >= 3 * 60 * 1000) {
                    // DB에 저장
                    userLocationDAO.insertLocation(
                        userNo, 
                        location.getLatitude(), 
                        location.getLongitude()
                    );
                    
                    // 마지막 저장 시간 업데이트
                    lastDbSaveTime.put(userNo, currentTime);
                    
                    log.info("환자 {} 위치 DB 저장 완료: ({}, {})", 
                        userNo, location.getLatitude(), location.getLongitude());
                }
            } catch (Exception e) {
                log.error("환자 {} 위치 DB 저장 실패: {}", userNo, e.getMessage());
            }
        }
    }

    /**
     * 임시 저장소 정리 (10분 이상 오래된 데이터 삭제)
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5분마다
    public void cleanupTemporaryLocations() {
        long currentTime = System.currentTimeMillis();
        long tenMinutesAgo = currentTime - (10 * 60 * 1000);
        
        temporaryLocations.entrySet().removeIf(entry -> 
            entry.getValue().getLastUpdateTime() < tenMinutesAgo
        );
        
        log.info("임시 위치 저장소 정리 완료. 현재 저장된 환자 수: {}", temporaryLocations.size());
    }
}

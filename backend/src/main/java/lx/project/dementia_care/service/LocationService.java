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
     */
    public LocationResponseDTO getPatientLocation(Integer patientUserNo) {
        LocationResponseDTO location = temporaryLocations.get(patientUserNo);
        
        if (location != null) {
            // 5분 이상 업데이트가 없으면 오프라인으로 표시 (오프라인 판단은 5분 유지)
            long currentTime = System.currentTimeMillis();
            if (currentTime - location.getLastUpdateTime() > 5 * 60 * 1000) {
                location.setStatus("offline");
            }
        }
        
        return location;
    }

    /**
     * '함께하는 사람들' 위치 조회 (임시 저장소에서) - 여러 명 조회
     * (MissingPersonService에서 호출됨)
     *
     * @param userIds '함께하는 사람'들의 user_no 리스트
     * @return 위치 정보 DTO 리스트
     */
    public List<LocationResponseDTO> getParticipantLocations(List<Integer> userIds) {
        List<LocationResponseDTO> locations = new ArrayList<>();
        if (userIds == null || userIds.isEmpty()) {
            return locations; // ID 리스트가 비어있으면 빈 목록 반환
        }

        long currentTime = System.currentTimeMillis();

        // ID 목록을 순회하며 'temporaryLocations' 맵에서 위치를 찾음
        for (Integer userId : userIds) {
            LocationResponseDTO location = temporaryLocations.get(userId);
            
            if (location != null) {
                // ⭐ (재사용) 1명 조회 로직과 동일하게 오프라인 상태 체크
                if (currentTime - location.getLastUpdateTime() > 5 * 60 * 1000) {
                    location.setStatus("offline");
                }
                
                // 찾은 위치를 리스트에 추가
                locations.add(location);
            }
            // (location == null 인 경우: 참여는 했지만 아직 위치 전송을 안 한 사람 -> 무시)
        }
        
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

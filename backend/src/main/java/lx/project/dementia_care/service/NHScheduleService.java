package lx.project.dementia_care.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lx.project.dementia_care.dao.ScheduleDAO;
import lx.project.dementia_care.dto.LocationDto;
import lx.project.dementia_care.dto.ScheduleRequest;
import lx.project.dementia_care.vo.RouteVO;
import lx.project.dementia_care.vo.SafeZoneVO;
import lx.project.dementia_care.vo.ScheduleLocationVO;
import lx.project.dementia_care.vo.ScheduleVO;

@Service
public class NHScheduleService {

    @Autowired
    private ScheduleDAO scheduleDAO;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 일정 및 관련 데이터 저장 (트랜잭션)
     * 1. 일정 저장 → scheduleNo 생성
     * 2. 일정 위치 저장 (출발지, 경유지, 목적지)
     * 3. 경로 저장 (routeCoordinates, bufferCoordinates)
     * 4. 안심존 저장 (경로형 안심존)
     */
    @Transactional
    public void saveScheduleWithDetails(ScheduleRequest request, int patientUserNo) throws Exception {
        // 1. 일정 저장
        ScheduleVO schedule = new ScheduleVO();
        schedule.setScheduleTitle(request.getTitle());
        schedule.setContent(request.getContent());
        schedule.setScheduleDate(LocalDate.parse(request.getDate())); // YYYY-MM-DD
        schedule.setStartTime(convertKoreanTimeToLocalTime(request.getStartTime())); // "오전 09:00" → 09:00:00
        schedule.setEndTime(convertKoreanTimeToLocalTime(request.getEndTime()));     // "오후 02:30" → 14:30:00
        schedule.setUserNo(patientUserNo); // 환자의 user_no

        scheduleDAO.insertSchedule(schedule);
        int scheduleNo = schedule.getScheduleNo(); // 자동 생성된 scheduleNo

        // 2. 일정 위치 저장
        if (request.getLocations() != null && !request.getLocations().isEmpty()) {
            for (LocationDto loc : request.getLocations()) {
                ScheduleLocationVO location = new ScheduleLocationVO();
                location.setLocationName(loc.getName());
                location.setLatitude(BigDecimal.valueOf(loc.getLatitude()));
                location.setLongitude(BigDecimal.valueOf(loc.getLongitude()));
                location.setSequenceOrder(loc.getSequenceOrder());
                location.setScheduleNo(scheduleNo);
                scheduleDAO.insertScheduleLocation(location);
            }
        }

        // 3. 경로 저장
        if (request.getRouteCoordinates() != null && !request.getRouteCoordinates().isEmpty()) {
            RouteVO route = new RouteVO();
            route.setRouteCoordinates(objectMapper.writeValueAsString(request.getRouteCoordinates()));
            
            // level 정보를 포함한 bufferCoordinates 저장
            Map<String, Object> bufferData = new HashMap<>();
            bufferData.put("level", request.getBufferLevel() != null ? request.getBufferLevel() : 1);
            bufferData.put("coordinates", request.getBufferCoordinates());
            route.setBufferCoordinates(objectMapper.writeValueAsString(bufferData));
            
            route.setScheduleNo(scheduleNo);
            scheduleDAO.insertRoute(route);
        }

        // 4. 안심존 저장 (경로형)
        if (request.getBufferCoordinates() != null && !request.getBufferCoordinates().isEmpty()) {
            SafeZoneVO safeZone = new SafeZoneVO();
            safeZone.setZoneType("경로형");
            
            // level 정보를 포함한 boundaryCoordinates 생성
            Map<String, Object> boundaryData = new HashMap<>();
            boundaryData.put("level", request.getBufferLevel() != null ? request.getBufferLevel() : 1);
            boundaryData.put("coordinates", request.getBufferCoordinates());
            
            safeZone.setBoundaryCoordinates(objectMapper.writeValueAsString(boundaryData));
            safeZone.setUserNo(patientUserNo); // 환자의 user_no
            scheduleDAO.insertSafeZone(safeZone);
        }
    }

    /**
     * 한글 시간 형식 → LocalTime 변환
     * "오전 01:00" → 01:00:00
     * "오후 02:30" → 14:30:00
     */
    private LocalTime convertKoreanTimeToLocalTime(String koreanTime) {
        String[] parts = koreanTime.split(" ");
        String period = parts[0]; // "오전" or "오후"
        String time = parts[1];   // "01:00"

        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // 오후이면서 12시가 아닌 경우 +12
        if ("오후".equals(period) && hour != 12) {
            hour += 12;
        }
        // 오전 12시는 00시로 변환
        if ("오전".equals(period) && hour == 12) {
            hour = 0;
        }

        return LocalTime.of(hour, minute, 0);
    }

    /**
     * 특정 사용자의 모든 일정 조회
     */
    public List<ScheduleVO> getSchedulesByUserNo(int userNo) throws Exception {
        return scheduleDAO.getSchedulesByUserNo(userNo);
    }

    /**
     * 특정 일정의 위치 목록 조회
     */
    public List<ScheduleLocationVO> getLocationsByScheduleNo(int scheduleNo) throws Exception {
        return scheduleDAO.getLocationsByScheduleNo(scheduleNo);
    }

    /**
     * 특정 일정의 경로 조회
     */
    public RouteVO getRouteByScheduleNo(int scheduleNo) throws Exception {
        return scheduleDAO.getRouteByScheduleNo(scheduleNo);
    }

    /**
     * 특정 사용자의 모든 안심존 조회
     */
    public List<SafeZoneVO> getSafeZonesByUserNo(int userNo) throws Exception {
        return scheduleDAO.getSafeZonesByUserNo(userNo);
    }

    /**
     * 특정 사용자의 특정 타입 안심존 조회
     */
    public List<SafeZoneVO> getSafeZonesByUserNoAndType(int userNo, String zoneType) throws Exception {
        return scheduleDAO.getSafeZonesByUserNoAndType(userNo, zoneType);
    }

    /**
     * 기본형 안심존 조회 (사용자당 1개만 존재)
     */
    public SafeZoneVO getBasicSafeZoneByUserNo(int userNo) throws Exception {
        List<SafeZoneVO> basicZones = scheduleDAO.getSafeZonesByUserNoAndType(userNo, "기본형");
        return basicZones.isEmpty() ? null : basicZones.get(0);
    }

    /**
     * 기본형 안심존 저장/수정 (트랜잭션)
     * 기존 기본형이 있으면 수정, 없으면 생성
     */
    @Transactional
    public void saveOrUpdateBasicSafeZone(int userNo, String boundaryCoordinates) throws Exception {
        // 기존 기본형 안심존 조회
        SafeZoneVO existingZone = getBasicSafeZoneByUserNo(userNo);
        
        if (existingZone != null) {
            // 기존 기본형 안심존 수정
            existingZone.setBoundaryCoordinates(boundaryCoordinates);
            scheduleDAO.updateSafeZone(existingZone);
        } else {
            // 새로운 기본형 안심존 생성
            SafeZoneVO newZone = new SafeZoneVO();
            newZone.setZoneType("기본형");
            newZone.setBoundaryCoordinates(boundaryCoordinates);
            newZone.setUserNo(userNo);
            scheduleDAO.insertSafeZone(newZone);
        }
    }

    /**
     * 현재 시간에 해당하는 일정 조회
     */
    public ScheduleVO getCurrentSchedule(int userNo) throws Exception {
        return scheduleDAO.getCurrentSchedule(userNo);
    }

    /**
     * 특정 일정 조회 (by scheduleNo)
     */
    public ScheduleVO getScheduleByNo(int scheduleNo) throws Exception {
        return scheduleDAO.getScheduleByNo(scheduleNo);
    }

    /**
     * 일정 수정 (트랜잭션)
     * 1. 기존 위치, 경로, 안심존 삭제
     * 2. 일정 기본 정보 업데이트
     * 3. 새로운 위치, 경로, 안심존 저장
     */
    @Transactional
    public void updateScheduleWithDetails(int scheduleNo, ScheduleRequest request, int patientUserNo) throws Exception {
        // 1. 기존 관련 데이터 삭제
        scheduleDAO.deleteScheduleLocations(scheduleNo);
        scheduleDAO.deleteRoute(scheduleNo);
        // 안심존은 사용자별로 관리되므로 기존 경로형 안심존 삭제 후 재생성
        scheduleDAO.deleteSafeZoneByUserNo(patientUserNo);

        // 2. 일정 기본 정보 업데이트
        ScheduleVO schedule = new ScheduleVO();
        schedule.setScheduleNo(scheduleNo);
        schedule.setScheduleTitle(request.getTitle());
        schedule.setContent(request.getContent());
        schedule.setScheduleDate(LocalDate.parse(request.getDate()));
        schedule.setStartTime(convertKoreanTimeToLocalTime(request.getStartTime()));
        schedule.setEndTime(convertKoreanTimeToLocalTime(request.getEndTime()));
        schedule.setUserNo(patientUserNo);

        scheduleDAO.updateSchedule(schedule);

        // 3. 새로운 위치 저장
        if (request.getLocations() != null && !request.getLocations().isEmpty()) {
            for (LocationDto loc : request.getLocations()) {
                ScheduleLocationVO location = new ScheduleLocationVO();
                location.setLocationName(loc.getName());
                location.setLatitude(BigDecimal.valueOf(loc.getLatitude()));
                location.setLongitude(BigDecimal.valueOf(loc.getLongitude()));
                location.setSequenceOrder(loc.getSequenceOrder());
                location.setScheduleNo(scheduleNo);
                scheduleDAO.insertScheduleLocation(location);
            }
        }

        // 4. 새로운 경로 저장
        if (request.getRouteCoordinates() != null && !request.getRouteCoordinates().isEmpty()) {
            RouteVO route = new RouteVO();
            route.setRouteCoordinates(objectMapper.writeValueAsString(request.getRouteCoordinates()));
            
            // level 정보를 포함한 bufferCoordinates 저장
            Map<String, Object> bufferData = new HashMap<>();
            bufferData.put("level", request.getBufferLevel() != null ? request.getBufferLevel() : 1);
            bufferData.put("coordinates", request.getBufferCoordinates());
            route.setBufferCoordinates(objectMapper.writeValueAsString(bufferData));
            
            route.setScheduleNo(scheduleNo);
            scheduleDAO.insertRoute(route);
        }

        // 5. 새로운 안심존 저장
        if (request.getBufferCoordinates() != null && !request.getBufferCoordinates().isEmpty()) {
            SafeZoneVO safeZone = new SafeZoneVO();
            safeZone.setZoneType("경로형");
            
            // level 정보를 포함한 boundaryCoordinates 생성
            Map<String, Object> boundaryData = new HashMap<>();
            boundaryData.put("level", request.getBufferLevel() != null ? request.getBufferLevel() : 1);
            boundaryData.put("coordinates", request.getBufferCoordinates());
            
            safeZone.setBoundaryCoordinates(objectMapper.writeValueAsString(boundaryData));
            safeZone.setUserNo(patientUserNo);
            scheduleDAO.insertSafeZone(safeZone);
        }
    }

    /**
     * 일정 삭제 (트랜잭션)
     * 1. 관련 위치 삭제
     * 2. 관련 경로 삭제
     * 3. 일정 삭제
     * 4. 해당 사용자의 경로형 안심존 재생성 (다른 일정들의 안심존)
     */
    @Transactional
    public void deleteScheduleWithDetails(int scheduleNo, int patientUserNo) throws Exception {
        // 1. 관련 위치 삭제
        scheduleDAO.deleteScheduleLocations(scheduleNo);

        // 2. 관련 경로 삭제
        scheduleDAO.deleteRoute(scheduleNo);

        // 3. 일정 삭제
        scheduleDAO.deleteSchedule(scheduleNo);

        // 4. 경로형 안심존 재생성 (모든 경로형 안심존 삭제 후, 남은 일정들의 안심존 재생성)
        scheduleDAO.deleteSafeZoneByUserNo(patientUserNo);

        // 남은 일정들의 안심존 재생성
        List<ScheduleVO> remainingSchedules = scheduleDAO.getSchedulesByUserNo(patientUserNo);
        for (ScheduleVO schedule : remainingSchedules) {
            RouteVO route = scheduleDAO.getRouteByScheduleNo(schedule.getScheduleNo());
            if (route != null && route.getBufferCoordinates() != null) {
                SafeZoneVO safeZone = new SafeZoneVO();
                safeZone.setZoneType("경로형");
                safeZone.setBoundaryCoordinates(route.getBufferCoordinates());
                safeZone.setUserNo(patientUserNo);
                scheduleDAO.insertSafeZone(safeZone);
            }
        }
    }

    /**
     * 특정 일정의 경로형 안심존 단계 업데이트 (트랜잭션)
     * 현재 진행 중인 일정의 버퍼를 새로운 단계(반경)로 재생성
     * 
     * Note: 실제 버퍼 생성은 프론트엔드(Turf.js)에서 처리되며,
     *       이 메서드는 updateRouteBuffer를 통해 호출됨
     */
    @Transactional
    public void updateRouteSafeZoneLevel(int scheduleNo, int level, int patientUserNo) throws Exception {
        // 1. 해당 일정의 경로 조회
        RouteVO route = scheduleDAO.getRouteByScheduleNo(scheduleNo);
        if (route == null || route.getRouteCoordinates() == null) {
            throw new Exception("경로 정보를 찾을 수 없습니다.");
        }
        
        // 실제 버퍼 업데이트는 updateRouteBuffer 메서드를 통해 처리됨
        // 이 메서드는 검증용으로 유지
    }

    /**
     * 경로 정보 업데이트 (버퍼 좌표 및 level 포함)
     * 현재 진행 중인 일정의 안심존만 업데이트
     */
    @Transactional
    public void updateRouteBuffer(int scheduleNo, String bufferCoordinates, int level, int patientUserNo) throws Exception {
        // 1. 경로 정보 조회
        RouteVO route = scheduleDAO.getRouteByScheduleNo(scheduleNo);
        if (route == null) {
            throw new Exception("경로 정보를 찾을 수 없습니다.");
        }

        // 2. 버퍼 좌표 업데이트 (level 정보 포함)
        route.setBufferCoordinates(bufferCoordinates);
        scheduleDAO.updateRoute(route);

        // 3. 해당 사용자의 경로형 안심존 삭제
        scheduleDAO.deleteSafeZoneByUserNo(patientUserNo);
        
        // 4. 현재 진행 중인 일정의 안심존만 재생성
        ScheduleVO currentSchedule = getCurrentSchedule(patientUserNo);
        if (currentSchedule != null && currentSchedule.getScheduleNo() == scheduleNo) {
            // 현재 진행 중인 일정이 업데이트 대상과 일치하는 경우에만 안심존 생성
            SafeZoneVO safeZone = new SafeZoneVO();
            safeZone.setZoneType("경로형");
            safeZone.setBoundaryCoordinates(bufferCoordinates); // level 정보가 포함된 bufferCoordinates
            safeZone.setUserNo(patientUserNo);
            scheduleDAO.insertSafeZone(safeZone);
        } else {
            // 현재 진행 중인 일정이 없거나 다른 일정인 경우, 
            // 기존의 모든 일정 안심존을 재생성 (기존 로직 유지)
            List<ScheduleVO> allSchedules = scheduleDAO.getSchedulesByUserNo(patientUserNo);
            for (ScheduleVO schedule : allSchedules) {
                RouteVO scheduleRoute = scheduleDAO.getRouteByScheduleNo(schedule.getScheduleNo());
                if (scheduleRoute != null && scheduleRoute.getBufferCoordinates() != null) {
                    SafeZoneVO safeZone = new SafeZoneVO();
                    safeZone.setZoneType("경로형");
                    safeZone.setBoundaryCoordinates(scheduleRoute.getBufferCoordinates());
                    safeZone.setUserNo(patientUserNo);
                    scheduleDAO.insertSafeZone(safeZone);
                }
            }
        }
    }
}


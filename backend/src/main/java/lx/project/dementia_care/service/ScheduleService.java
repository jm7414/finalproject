package lx.project.dementia_care.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
public class ScheduleService {

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
            route.setBufferCoordinates(objectMapper.writeValueAsString(request.getBufferCoordinates()));
            route.setScheduleNo(scheduleNo);
            scheduleDAO.insertRoute(route);
        }

        // 4. 안심존 저장 (경로형)
        if (request.getBufferCoordinates() != null && !request.getBufferCoordinates().isEmpty()) {
            SafeZoneVO safeZone = new SafeZoneVO();
            safeZone.setZoneType("경로형");
            safeZone.setBoundaryCoordinates(objectMapper.writeValueAsString(request.getBufferCoordinates()));
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
            route.setBufferCoordinates(objectMapper.writeValueAsString(request.getBufferCoordinates()));
            route.setScheduleNo(scheduleNo);
            scheduleDAO.insertRoute(route);
        }

        // 5. 새로운 안심존 저장
        if (request.getBufferCoordinates() != null && !request.getBufferCoordinates().isEmpty()) {
            SafeZoneVO safeZone = new SafeZoneVO();
            safeZone.setZoneType("경로형");
            safeZone.setBoundaryCoordinates(objectMapper.writeValueAsString(request.getBufferCoordinates()));
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
}


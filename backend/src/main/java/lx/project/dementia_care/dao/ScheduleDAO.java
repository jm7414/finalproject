package lx.project.dementia_care.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lx.project.dementia_care.vo.RouteVO;
import lx.project.dementia_care.vo.SafeZoneVO;
import lx.project.dementia_care.vo.ScheduleLocationVO;
import lx.project.dementia_care.vo.ScheduleVO;

@Component
public class ScheduleDAO {

    @Autowired
    SqlSession session;

    // 일정 저장
    public int insertSchedule(ScheduleVO schedule) throws Exception {
        return session.insert("lx.project.dementia_care.dao.ScheduleDAO.insertSchedule", schedule);
    }

    // 일정 위치 저장
    public int insertScheduleLocation(ScheduleLocationVO location) throws Exception {
        return session.insert("lx.project.dementia_care.dao.ScheduleDAO.insertScheduleLocation", location);
    }

    // 경로 저장
    public int insertRoute(RouteVO route) throws Exception {
        return session.insert("lx.project.dementia_care.dao.ScheduleDAO.insertRoute", route);
    }

    // 안심존 저장
    public int insertSafeZone(SafeZoneVO safeZone) throws Exception {
        return session.insert("lx.project.dementia_care.dao.ScheduleDAO.insertSafeZone", safeZone);
    }

    // 특정 사용자의 모든 일정 조회
    public List<ScheduleVO> getSchedulesByUserNo(int userNo) throws Exception {
        return session.selectList("lx.project.dementia_care.dao.ScheduleDAO.getSchedulesByUserNo", userNo);
    }

    // 특정 일정의 위치 목록 조회
    public List<ScheduleLocationVO> getLocationsByScheduleNo(int scheduleNo) throws Exception {
        return session.selectList("lx.project.dementia_care.dao.ScheduleDAO.getLocationsByScheduleNo", scheduleNo);
    }

    // 특정 일정의 경로 조회
    public RouteVO getRouteByScheduleNo(int scheduleNo) throws Exception {
        return session.selectOne("lx.project.dementia_care.dao.ScheduleDAO.getRouteByScheduleNo", scheduleNo);
    }

    // 특정 사용자의 모든 안심존 조회
    public List<SafeZoneVO> getSafeZonesByUserNo(int userNo) throws Exception {
        return session.selectList("lx.project.dementia_care.dao.ScheduleDAO.getSafeZonesByUserNo", userNo);
    }

    // 특정 사용자의 특정 타입 안심존 조회
    public List<SafeZoneVO> getSafeZonesByUserNoAndType(int userNo, String zoneType) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("userNo", userNo);
        params.put("zoneType", zoneType);
        return session.selectList("lx.project.dementia_care.dao.ScheduleDAO.getSafeZonesByUserNoAndType", params);
    }

    // 현재 시간에 해당하는 일정 조회
    public ScheduleVO getCurrentSchedule(int userNo) throws Exception {
        return session.selectOne("lx.project.dementia_care.dao.ScheduleDAO.getCurrentSchedule", userNo);
    }

    // 특정 일정 조회 (by scheduleNo)
    public ScheduleVO getScheduleByNo(int scheduleNo) throws Exception {
        return session.selectOne("lx.project.dementia_care.dao.ScheduleDAO.getScheduleByNo", scheduleNo);
    }

    // 일정 수정
    public int updateSchedule(ScheduleVO schedule) throws Exception {
        return session.update("lx.project.dementia_care.dao.ScheduleDAO.updateSchedule", schedule);
    }

    // 일정 삭제
    public int deleteSchedule(int scheduleNo) throws Exception {
        return session.delete("lx.project.dementia_care.dao.ScheduleDAO.deleteSchedule", scheduleNo);
    }

    // 특정 일정의 위치 삭제
    public int deleteScheduleLocations(int scheduleNo) throws Exception {
        return session.delete("lx.project.dementia_care.dao.ScheduleDAO.deleteScheduleLocations", scheduleNo);
    }

    // 특정 일정의 경로 삭제
    public int deleteRoute(int scheduleNo) throws Exception {
        return session.delete("lx.project.dementia_care.dao.ScheduleDAO.deleteRoute", scheduleNo);
    }

    // 특정 사용자의 경로형 안심존 삭제 (일정 삭제 시)
    public int deleteSafeZoneByUserNo(int userNo) throws Exception {
        return session.delete("lx.project.dementia_care.dao.ScheduleDAO.deleteSafeZoneByUserNo", userNo);
    }

    // 안심존 수정
    public int updateSafeZone(SafeZoneVO safeZone) throws Exception {
        return session.update("lx.project.dementia_care.dao.ScheduleDAO.updateSafeZone", safeZone);
    }
}


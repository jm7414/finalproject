package lx.project.dementia_care.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lx.project.dementia_care.dao.NeighborScheduleDAO;
import lx.project.dementia_care.dto.ScheduleRequest;
import lx.project.dementia_care.vo.NeighborScheduleVO;
import java.time.LocalDate;
import java.util.List;

@Service
public class NeighborScheduleService {
    
    @Autowired
    private NeighborScheduleDAO neighborScheduleDAO;
    
    // 이웃 일정 저장
    @Transactional
    public void saveNeighborSchedule(ScheduleRequest request, int userNo) throws Exception {
        NeighborScheduleVO schedule = new NeighborScheduleVO();
        schedule.setScheduleTitle(request.getTitle());
        schedule.setContent(request.getContent());
        schedule.setScheduleDate(LocalDate.parse(request.getDate()));
        schedule.setUserNo(userNo);  // 현재 로그인한 사용자
        
        neighborScheduleDAO.insertNeighborSchedule(schedule);
    }
    
    // 특정 이웃의 모든 일정 조회 (개인용)
    public List<NeighborScheduleVO> getNeighborSchedulesByUserNo(int userNo) {
        return neighborScheduleDAO.getNeighborSchedulesByUserNo(userNo);
    }
    
    // ✅ 같은 광장의 모든 일정 조회 (광장용)
    public List<NeighborScheduleVO> getNeighborSchedulesByPlazaName(String plazaName) {
        return neighborScheduleDAO.getNeighborSchedulesByPlazaName(plazaName);
    }
    
    // ✅ 사용자의 광장명 조회
    public String getPlazaNameByUserNo(int userNo) {
        return neighborScheduleDAO.getPlazaNameByUserNo(userNo);
    }
    
    // 특정 일정 조회
    public NeighborScheduleVO getNeighborScheduleByNo(int scheduleNo) {
        return neighborScheduleDAO.getNeighborScheduleByNo(scheduleNo);
    }
    
    // 이웃 일정 삭제
    @Transactional
    public void deleteNeighborSchedule(int scheduleNo) {
        neighborScheduleDAO.deleteNeighborSchedule(scheduleNo);
    }
    // 오늘, 내일 일정 2개 조회
    public List<NeighborScheduleVO> getUpcomingSchedules(int userNo) {
        return neighborScheduleDAO.selectUpcomingSchedules(userNo);
    }
}

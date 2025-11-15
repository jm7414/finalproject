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
        schedule.setUserNo(userNo); // 현재 로그인한 사용자

        neighborScheduleDAO.insertNeighborSchedule(schedule);
    }

    // 특정 이웃의 모든 일정 조회 (개인용)
    public List<NeighborScheduleVO> getNeighborSchedulesByUserNo(int userNo) {
        return neighborScheduleDAO.getNeighborSchedulesByUserNo(userNo);
    }

    // 같은 광장의 모든 일정 조회 (광장용)
    public List<NeighborScheduleVO> getNeighborSchedulesByPlazaName(String plazaName) {
        return neighborScheduleDAO.getNeighborSchedulesByPlazaName(plazaName);
    }

    // 사용자의 광장명 조회
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

    /**
     * 오늘, 내일 일정 2개 조회 (광장 기준)
     * 
     * 절차:
     * 1. 로그인한 유저번호로 plazaName조회
     * 2. plazaName으로 plaza내 전체 멤버 오늘/내일 일정 조회
     */
    public List<NeighborScheduleVO> getUpcomingSchedules(int userNo) {
        // 1. 로그인한 사용자의 광장명 조회
        String plazaName = neighborScheduleDAO.getPlazaNameByUserNo(userNo);
        if (plazaName == null) {
            // 광장 없는 경우 빈 배열 리턴
            return List.of();
        }
        // 2. 광장명으로 광장 멤버 전체 일정 조회 (오늘/내일 2개)
        return neighborScheduleDAO.selectUpcomingPlazaSchedules(plazaName);
    }
}

package lx.project.dementia_care.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import lx.project.dementia_care.vo.NeighborScheduleVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class NeighborScheduleDAO {

    @Autowired
    private SqlSessionTemplate sqlSession;

    // 이웃 일정 저장
    public void insertNeighborSchedule(NeighborScheduleVO schedule) {
        sqlSession.insert("insertNeighborSchedule", schedule);
    }

    // 특정 이웃의 모든 일정 조회 (개인용)
    public List<NeighborScheduleVO> getNeighborSchedulesByUserNo(int userNo) {
        return sqlSession.selectList("getNeighborSchedulesByUserNo", userNo);
    }

    // 같은 광장의 모든 일정 조회 (광장용)
    public List<NeighborScheduleVO> getNeighborSchedulesByPlazaName(String plazaName) {
        return sqlSession.selectList("getNeighborSchedulesByPlazaName", plazaName);
    }
    
    public List<NeighborScheduleVO> selectUpcomingPlazaSchedules(String plazaName) {
        return sqlSession.selectList("selectUpcomingPlazaSchedules", plazaName);
    }

    // 특정 일정 조회
    public NeighborScheduleVO getNeighborScheduleByNo(int scheduleNo) {
        return sqlSession.selectOne("getNeighborScheduleByNo", scheduleNo);
    }

    // 이웃 일정 삭제
    public void deleteNeighborSchedule(int scheduleNo) {
        sqlSession.delete("deleteNeighborSchedule", scheduleNo);
    }

    // 사용자의 광장명 조회
    public String getPlazaNameByUserNo(int userNo) {
        return sqlSession.selectOne("getPlazaNameByUserNo", userNo);
    }

    // 오늘, 내일 일정 2개 조회
    public List<NeighborScheduleVO> selectUpcomingSchedules(int userNo) {
        return sqlSession.selectList("selectUpcomingSchedules", userNo);
    }
}

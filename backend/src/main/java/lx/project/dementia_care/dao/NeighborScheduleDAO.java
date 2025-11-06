package lx.project.dementia_care.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lx.project.dementia_care.vo.NeighborScheduleVO;
import java.util.List;

@Component
public class NeighborScheduleDAO {
    
    @Autowired
    private SqlSessionTemplate sqlSession;
    
    // 이웃 일정 저장
    public void insertNeighborSchedule(NeighborScheduleVO schedule) {
        sqlSession.insert("insertNeighborSchedule", schedule);
    }
    
    // 특정 이웃의 모든 일정 조회
    public List<NeighborScheduleVO> getNeighborSchedulesByUserNo(int userNo) {
        return sqlSession.selectList("getNeighborSchedulesByUserNo", userNo);
    }
    
    // 특정 일정 조회
    public NeighborScheduleVO getNeighborScheduleByNo(int scheduleNo) {
        return sqlSession.selectOne("getNeighborScheduleByNo", scheduleNo);
    }
    
    // 이웃 일정 삭제
    public void deleteNeighborSchedule(int scheduleNo) {
        sqlSession.delete("deleteNeighborSchedule", scheduleNo);
    }
}

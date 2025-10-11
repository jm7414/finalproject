package lx.project.dementia_care.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lx.project.dementia_care.vo.UserVO;

@Component
public class GuardianPatientConnectionDAO {

    @Autowired
    SqlSession session;

    /**
     * 보호자 user_no로 관리하는 환자 조회
     * guardian1_no, guardian2_no, guardian3_no 중 하나라도 일치하면 해당 환자 반환
     */
    public UserVO getPatientByGuardianNo(int guardianNo) throws Exception {
        return session.selectOne("lx.project.dementia_care.dao.GuardianPatientConnectionDAO.getPatientByGuardianNo", guardianNo);
    }
}


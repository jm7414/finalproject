package lx.project.dementia_care.dao;

import lx.project.dementia_care.vo.ConnectVO;
import lx.project.dementia_care.vo.UserVO;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ConnectDAO {

    @Autowired
    private SqlSession session;

    /** 연결 1건 생성 (ON CONFLICT DO NOTHING) */
    public int insertConnection(ConnectVO vo) throws Exception {
        return session.insert("mapper-connect.insertConnection", vo);
    }

    /** guardian1 교체 */
    public int replaceGuardian1(int patientNo, int guardianNo) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("patientNo", patientNo);
        params.put("guardianNo", guardianNo);
        return session.update("mapper-connect.replaceGuardian1", params);
    }

    /** 환자 기준 연결 삭제 */
    public int deleteByPatientNo(int patientNo) throws Exception {
        return session.delete("mapper-connect.deleteByPatientNo", patientNo);
    }

    /** 초대코드로 환자 찾기 */
    public UserVO findPatientByInvitationCode(String code) throws Exception {
        return session.selectOne("mapper-connect.findPatientByInvitationCode", code);
    }

    /** 다음 빈 슬롯(guardian1→2→3) 한 칸만 채우기 */
    public int updateFillNextSlot(Map<String, Object> param) throws Exception {
        return session.update("mapper-connect.updateFillNextSlot", param);
    }

    public int updateFillNextSlot(int patientNo, int guardianNo) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("patientNo", patientNo);
        p.put("guardianNo", guardianNo);
        return updateFillNextSlot(p);
    }

    /** 환자 최소 정보(Map) 조회 */
    public Map<String, Object> selectPatientBasic(int patientNo) throws Exception {
        return session.<Map<String, Object>>selectOne("mapper-connect.selectPatientBasic", patientNo);
    }

    /** 환자에 연결된 보호자 목록(List<Map>) */
    public List<Map<String, Object>> selectGuardiansByPatientNo(int patientNo) throws Exception {
        return session.<Map<String, Object>>selectList("mapper-connect.selectGuardiansByPatientNo", patientNo);
    }

    /** 보호자 슬롯에서 해당 guardianNo 제거 */
    public int unsetGuardianSlot(int patientNo, int guardianNo) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("patientNo", patientNo);
        p.put("guardianNo", guardianNo);
        return session.update("mapper-connect.unsetGuardianSlot", p);
    }

    /** 좌로 정렬 2단계 */
    public void compactSlots(int patientNo) throws Exception {
        session.update("mapper-connect.compactStep1", patientNo);
        session.update("mapper-connect.compactStep2", patientNo);
    }

    /** 모두 NULL이면 행 삭제 */
    public int deleteIfEmpty(int patientNo) throws Exception {
        return session.delete("mapper-connect.deleteIfEmpty", patientNo);
    }

    	/**
	 * 보호자 user_no로 관리하는 환자 조회
	 * guardian1_no, guardian2_no, guardian3_no 중 하나라도 일치하면 해당 환자 반환
	 */
	public UserVO getPatientByGuardianNo(int guardianNo) throws Exception {
		return session.selectOne("mapper-connect.getPatientByGuardianNo", guardianNo);
	}
}

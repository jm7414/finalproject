package lx.project.dementia_care.dao;

import lx.project.dementia_care.vo.ConnectVO;
import lx.project.dementia_care.vo.UserVO;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ConnectDAO {

	@Autowired
	SqlSession session;

	/** 연결 1건 생성 */
	public int insertConnection(ConnectVO vo) throws Exception {
		return session.insert("mapper-connect.insertConnection", vo);
	}

	/** guardian1 교체 (필수: guardianNo != null) */
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

	/**
	 * 보호자 user_no로 관리하는 환자 조회
	 * guardian1_no, guardian2_no, guardian3_no 중 하나라도 일치하면 해당 환자 반환
	 */
	public UserVO getPatientByGuardianNo(int guardianNo) throws Exception {
		return session.selectOne("mapper-connect.getPatientByGuardianNo", guardianNo);
	}
}

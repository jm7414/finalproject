package lx.project.dementia_care.dao;

import lx.project.dementia_care.vo.ConnectVO;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
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

	/** guardian1_no 기준으로 보호자가 관리하는 환자 1명 반환 */
	public Integer findPatientNoByGuardian1No(int guardianNo) throws Exception {
		return session.selectOne("mapper-connect.findPatientNoByGuardian1No", guardianNo);
	}

}

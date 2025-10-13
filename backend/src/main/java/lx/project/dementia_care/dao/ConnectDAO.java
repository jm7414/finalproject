package lx.project.dementia_care.dao;

import lx.project.dementia_care.vo.ConnectVO;
import lx.project.dementia_care.vo.UserVO;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/** guardian1_no 기준으로 보호자가 관리하는 환자 1명 반환 */
	public Integer findPatientNoByGuardian1No(int guardianNo) throws Exception {
		return session.selectOne("mapper-connect.findPatientNoByGuardian1No", guardianNo);
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

    /** 전역 중복 사전 체크: 보호자가 이미 다른 환자에 연결돼 있는지 */
    public boolean isGuardianBusy(int guardianNo) throws Exception {
        Boolean exists = session.selectOne("mapper-connect.isGuardianBusy", guardianNo);
        return Boolean.TRUE.equals(exists);
    }

    /** 안전 버전: 다음 빈 슬롯 채우기 (전역 중복 NOT EXISTS 포함) - map 파라미터 */
    public int updateFillNextSlotSafe(Map<String, Object> param) throws Exception {
        return session.update("mapper-connect.updateFillNextSlotSafe", param);
    }

    /** 안전 버전: 다음 빈 슬롯 채우기 (오버로드) */
    public int updateFillNextSlotSafe(int patientNo, int guardianNo) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("patientNo", patientNo);
        p.put("guardianNo", guardianNo);
        return session.update("mapper-connect.updateFillNextSlotSafe", p);
    }

    /** 안전 버전: guardian1 교체 (전역 중복 NOT EXISTS 포함) - map 파라미터 */
    public int replaceGuardian1Safe(Map<String, Object> param) throws Exception {
        return session.update("mapper-connect.replaceGuardian1Safe", param);
    }

    /** 안전 버전: guardian1 교체 (오버로드) */
    public int replaceGuardian1Safe(int patientNo, int guardianNo) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("patientNo", patientNo);
        p.put("guardianNo", guardianNo);
        return session.update("mapper-connect.replaceGuardian1Safe", p);
    }

    /** (옵션) 구독 만료: 2/3번 슬롯 비우기 */
    public int expireExtraSlots() throws Exception {
        return session.update("mapper-connect.expireExtraSlots");
    }
    
 // --추가-- 보호자 수 카운트
    public int countGuardians(int patientNo) throws Exception {
        Integer n = session.selectOne("mapper-connect.countGuardians", patientNo);
        return n == null ? 0 : n;
    }

    // --추가-- 환자가 role 3(구독자 환자)인지
    public boolean isPatientRole3(int patientNo) throws Exception {
        Boolean b = session.selectOne("mapper-connect.isPatientRole3", patientNo);
        return Boolean.TRUE.equals(b);
    }

    // --추가-- 이미 이 환자에 이 보호자가 연결돼 있는지(멱등)
    public boolean isGuardianLinkedToPatient(int patientNo, int guardianNo) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("patientNo", patientNo);
        p.put("guardianNo", guardianNo);
        Boolean b = session.selectOne("mapper-connect.isGuardianLinkedToPatient", p);
        return Boolean.TRUE.equals(b);
    }
    
}

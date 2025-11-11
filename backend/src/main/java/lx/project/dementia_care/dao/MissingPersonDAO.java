package lx.project.dementia_care.dao;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lx.project.dementia_care.dto.MissingPersonDto;
import lx.project.dementia_care.vo.UserVO;

@Component
public class MissingPersonDAO {

    @Autowired
    private SqlSession session; 

    private static final String NAMESPACE = "lx.project.dementia_care.mapper.MissingPersonMapper";

    /**
     * 특정 실종자(user_no 2번) 정보를 조회합니다.
     */
    // public MissingPersonDto findMissingPersonByUserNo2() {
    //     return session.selectOne(NAMESPACE + ".findMissingPersonByUserNo2");
    // }

    /**
     * 모든 '실종' 상태인 실종자 목록을 조회합니다.
     */
    public List<MissingPersonDto> findAllMissingPersons() {
        return session.selectList(NAMESPACE + ".findAllMissingPersons");
    }

    /**
     * 새로운 실종 신고를 DB에 저장합니다.
     */
    public void createMissingPersonReport(MissingPersonDto missingPersonDto) {
        session.insert(NAMESPACE + ".createMissingPersonReport", missingPersonDto);
    }

    /**
     * 실종 상태를 업데이트합니다. (예: '찾음'으로 변경)
     */
    public void updateMissingStatus(Integer missingPostId, String status) {
        Map<String, Object> params = new HashMap<>();
        params.put("missingPostId", missingPostId);
        params.put("status", status);
        session.update(NAMESPACE + ".updateMissingStatus", params);
    }

    /**
     * 환자 번호로 가장 최근 실종 신고 정보를 조회합니다.
     * currentUserId를 받아 '내가 참여했는지' 여부도 조회합니다.
     */
    public MissingPersonDto findLatestMissingReportByPatientNo(Integer patientUserNo, Integer currentUserId) {
        Map<String, Object> params = new HashMap<>();
        params.put("patientUserNo", patientUserNo);
        params.put("currentUserId", currentUserId);
        return session.selectOne(NAMESPACE + ".findLatestMissingReportByPatientNo", params);
    }

    /**
     * 신고 ID로 특정 신고 정보를 조회합니다.
     */
    public MissingPersonDto findMissingReportById(Integer missingPostId) {
        return session.selectOne(NAMESPACE + ".findMissingReportById", missingPostId);
    }
    
    /**
     * search_Together 테이블에 참여 정보를 추가합니다.
     */
    public void addSearchTogetherEntry(Integer missingPostId, Integer userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("missingPostId", missingPostId);
        params.put("userId", userId);
        session.insert(NAMESPACE + ".addSearchTogetherEntry", params);
    }

    /**
     * 특정 실종 신고 ID에 참여하는 사용자 목록을 조회합니다.
     */
    public List<UserVO> findParticipantsByMissingPostId(Integer missingPostId) {
        return session.selectList(NAMESPACE + ".findParticipantsByMissingPostId", missingPostId);
    }

    /**
     * 확인하지 않은 최신 알림 1건 조회 (폴링용)
     */
    public MissingPersonDto findLatestAlertForUser(Integer userId) {
        return session.selectOne(NAMESPACE + ".findLatestAlertForUser", userId);
    }

    /**
     * missing_alert_check 테이블에 확인 기록 INSERT
     */
    public void addAlertConfirmation(Integer missingPostId, Integer userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("missingPostId", missingPostId);
        params.put("userId", userId);
        session.insert(NAMESPACE + ".addAlertConfirmation", params);
    }

    /**
     * 환자 번호(patient_user_no)를 기준으로 '실종' 상태인 모든 게시물을 삭제합니다.
     * (UserController에서 실종 해제 시 호출)
     *
     * @param patientUserNo 환자의 user_no
     * @return 삭제된 행(post)의 수
     */
    public int resolveActivePostsByPatientNo(Integer patientUserNo) {
        // Mapper XML의 <delete id="resolveActivePostsByPatientNo">를 호출합니다.
        return session.delete(NAMESPACE + ".resolveActivePostsByPatientNo", patientUserNo); 
    }

    /**
     * 특정 실종 신고에 참여 중인 사람들의 'user_id' 목록만 조회합니다.
     */
    public List<Integer> findParticipantUserIds(Integer missingPostId) {
        return session.selectList(NAMESPACE + ".findParticipantUserIds", missingPostId);
    }

}
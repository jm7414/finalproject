package lx.project.dementia_care.dao;

import java.util.List;
import java.util.Map; // Map 사용을 위해 추가
import java.util.HashMap; // HashMap 사용을 위해 추가

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
    public MissingPersonDto findMissingPersonByUserNo2() {
        return session.selectOne(NAMESPACE + ".findMissingPersonByUserNo2");
    }

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
     */
    public MissingPersonDto findLatestMissingReportByPatientNo(Integer patientUserNo) {
        return session.selectOne(NAMESPACE + ".findLatestMissingReportByPatientNo", patientUserNo);
    }

    /**
     * 신고 ID로 특정 신고 정보를 조회합니다.
     */
    public MissingPersonDto findMissingReportById(Integer missingPostId) {
        return session.selectOne(NAMESPACE + ".findMissingReportById", missingPostId);
    }
    
    /**
     * search_Together 테이블에 참여 정보를 추가합니다.
     * @param missingPostId 참여할 실종 신고 ID
     * @param userId 참여하는 사용자 ID
     * @throws DuplicateKeyException 이미 해당 조합이 존재할 경우 발생 (Primary Key 위반)
     * @throws DataIntegrityViolationException 등 FK 제약조건 위반 시 발생 가능
     */
    public void addSearchTogetherEntry(Integer missingPostId, Integer userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("missingPostId", missingPostId);
        params.put("userId", userId);
        // MyBatis Mapper에 "addSearchTogetherEntry" ID로 insert 구문 필요
        session.insert(NAMESPACE + ".addSearchTogetherEntry", params);
    }

    /**
     * 특정 실종 신고 ID에 참여하는 사용자 목록을 조회합니다.
     * @param missingPostId 실종 신고 ID
     * @return 참여자 정보 목록 (UserVO 리스트)
     */
    public List<UserVO> findParticipantsByMissingPostId(Integer missingPostId) {
        // Mapper XML의 select id와 일치해야 함
        return session.selectList(NAMESPACE + ".findParticipantsByMissingPostId", missingPostId);
    }

}
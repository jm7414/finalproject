package lx.project.dementia_care.dao;

import java.util.List;
import java.util.Map; // Map 사용을 위해 추가
import java.util.HashMap; // HashMap 사용을 위해 추가

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lx.project.dementia_care.dto.MissingPersonDto;

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
    
}
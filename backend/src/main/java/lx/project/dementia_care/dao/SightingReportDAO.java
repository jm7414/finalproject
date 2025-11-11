package lx.project.dementia_care.dao;

import lx.project.dementia_care.dto.SightingReportListDto;
import lx.project.dementia_care.dto.SightingReportRequestDto;
import lx.project.dementia_care.dto.SightingReportResponseDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

/**
 * mapper-sighting-report.xml을 호출하는 Mybatis 인터페이스
 */
@Mapper
public interface SightingReportDAO {

    /**
     * 특정 실종 건(missingPostId)에 달린 모든 제보 목록을 조회
     */
    List<SightingReportListDto> findReportsByMissingPostId(Integer missingPostId);

    /**
     * 제보 ID로 1건의 상세 정보를 조회
     */
    SightingReportResponseDto findReportById(Integer sightingReportId);

    /**
     * 새로운 제보 1건을 생성 (Map을 사용하여 userNo와 dto를 함께 전달)
     */
    int createReport(Map<String, Object> params);

    /**
     * 제보 1건의 내용을 수정 (Map을 사용하여 reportId와 dto를 함께 전달)
     */
    int updateReport(Map<String, Object> params);

    /**
     * 제보 ID로 1건을 삭제
     */
    int deleteReport(Integer sightingReportId);
}
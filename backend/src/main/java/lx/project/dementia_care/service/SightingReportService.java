package lx.project.dementia_care.service;

import lx.project.dementia_care.dao.SightingReportDAO;
import lx.project.dementia_care.dto.SightingReportListDto;
import lx.project.dementia_care.dto.SightingReportRequestDto;
import lx.project.dementia_care.dto.SightingReportResponseDto;
import lx.project.dementia_care.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SightingReportService {

    private final SightingReportDAO sightingReportDAO;

    /**
     * (읽기 전용) 특정 실종 건에 대한 모든 제보 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SightingReportListDto> findReportsByMissingPostId(Integer missingPostId) {
        return sightingReportDAO.findReportsByMissingPostId(missingPostId);
    }

    /**
     * (읽기 전용) 제보 1건 상세 조회
     */
    @Transactional(readOnly = true)
    public SightingReportResponseDto findReportById(Integer sightingReportId) {
        return sightingReportDAO.findReportById(sightingReportId);
    }

    /**
     * 새 제보 생성
     */
    public Integer createReport(SightingReportRequestDto dto, Integer userNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("dto", dto);
        params.put("userNo", userNo);
        
        sightingReportDAO.createReport(params);
        
        // Mybatis useGeneratedKeys 덕분에 params 맵에 생성된 ID가 담겨 돌아옵니다.
        return (Integer) params.get("sighting_report_id");
    }

    /**
     * 제보 수정 (권한 확인)
     */
    public void updateReport(Integer reportId, SightingReportRequestDto dto, Integer currentUserNo) throws AccessDeniedException {
        SightingReportResponseDto existingReport = sightingReportDAO.findReportById(reportId);
        
        // 권한 확인: 본인 또는 관리자(userNo=1)만 수정 가능
        if (!existingReport.getUserNo().equals(currentUserNo) && currentUserNo != 1) {
            throw new AccessDeniedException("이 제보를 수정할 권한이 없습니다.");
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("reportId", reportId);
        params.put("dto", dto);
        
        sightingReportDAO.updateReport(params);
    }

    /**
     * 제보 삭제 (권한 확인)
     */
    public void deleteReport(Integer reportId, Integer currentUserNo) throws AccessDeniedException {
        SightingReportResponseDto existingReport = sightingReportDAO.findReportById(reportId);
        
        // 권한 확인: 본인 또는 관리자(userNo=1)만 삭제 가능
        if (existingReport == null) {
            throw new RuntimeException("삭제할 제보를 찾을 수 없습니다.");
        }
        if (!existingReport.getUserNo().equals(currentUserNo) && currentUserNo != 1) {
            throw new AccessDeniedException("이 제보를 삭제할 권한이 없습니다.");
        }
        
        sightingReportDAO.deleteReport(reportId);
    }
}
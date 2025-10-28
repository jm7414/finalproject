package lx.project.dementia_care.service;

import java.nio.file.AccessDeniedException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lx.project.dementia_care.dao.UserDAO;
import lx.project.dementia_care.dao.MissingPersonDAO;

import lx.project.dementia_care.dto.MissingPersonDto;
import lx.project.dementia_care.vo.UserVO;

@Service
public class MissingPersonService {

    // 두 개의 DAO를 모두 주입받음
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private MissingPersonDAO missingPersonDAO;

    /**
     * user_status가 1인 ('실종' 상태) 사용자 목록과
     * 관련된 최신 missing_post 정보를 조합하여 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<MissingPersonDto> findMissingPersonsWithDetails() {
        // 1. UserDAO를 통해 user_status가 1인 사용자 목록(기본 정보)을 가져옵니다.
        List<UserVO> missingUsers = userDAO.findMissingUsers();

        // 2. 각 사용자에 대해 MissingPersonDAO를 통해 최신 실종 신고 정보를 가져와 합칩니다.
        List<MissingPersonDto> results = new ArrayList<>();
        for (UserVO user : missingUsers) {
            // MissingPersonDAO에 userNo로 최신 신고 1건을 찾는 메서드가 필요합니다. (아래 가정)
            MissingPersonDto reportDetails = missingPersonDAO.findLatestMissingReportByPatientNo(user.getUserNo());

            // MissingPersonDto를 만들고 UserVO와 reportDetails 정보를 합칩니다.
            MissingPersonDto dto = new MissingPersonDto();
            dto.setPatientUserNo(user.getUserNo());
            dto.setPatientName(user.getName());
            dto.setPatientBirthDate(user.getBirthDate());
            // UserVO의 profilePhoto를 photoPath로 사용 (만약 신고 사진이 없다면)
            dto.setPhotoPath(reportDetails != null && reportDetails.getPhotoPath() != null ? reportDetails.getPhotoPath() : user.getProfilePhoto());

            if (reportDetails != null) {
                dto.setMissingPostId(reportDetails.getMissingPostId());
                dto.setReporterUserNo(reportDetails.getReporterUserNo());
                dto.setDescription(reportDetails.getDescription());
                dto.setReportedAt(reportDetails.getReportedAt());
                dto.setStatus(reportDetails.getStatus());
                dto.setSearchTogetherCount(reportDetails.getSearchTogetherCount()); // DAO에서 계산해 온 값 사용
            } else {
                // 만약 missing_post 정보가 없다면 기본값 설정 (예외 처리 필요 시 수정)
                dto.setStatus("정보 없음");
                dto.setSearchTogetherCount(0);
            }
            results.add(dto);
        }

        return results;
    }

    /**
     * 새로운 실종 신고를 등록하고, 해당 사용자의 user_status를 1로 변경합니다.
     */
    @Transactional
    public MissingPersonDto createMissingPersonReportAndUpdateStatus(MissingPersonDto missingPersonDto, Integer reporterUserId) {
        // 1. 기본값 설정 (Service 역할)
        missingPersonDto.setReporterUserNo(reporterUserId);
        if (missingPersonDto.getStatus() == null || missingPersonDto.getStatus().isEmpty()) {
            missingPersonDto.setStatus("실종");
        }
        if (missingPersonDto.getReportedAt() == null) {
            missingPersonDto.setReportedAt(OffsetDateTime.now());
        }

        // 2. MissingPersonDAO를 통해 신고 정보 저장
        missingPersonDAO.createMissingPersonReport(missingPersonDto);

        // 3. UserDAO를 통해 해당 사용자의 user_status를 1로 변경 (UserDAO에 메서드 추가 필요)
        userDAO.updateUserStatus(missingPersonDto.getPatientUserNo(), 1);

        // 생성된 정보 반환 (ID 포함)
        return missingPersonDto;
    }

    /**
     * 실종 상태를 업데이트하고, 해당 사용자의 user_status를 0으로 변경합니다.
     */
    @Transactional
    public void updateStatusAndUserStatus(Integer missingPostId, String newStatus, Integer currentUserId) throws AccessDeniedException {
        // (선택사항) 권한 확인 로직 추가 가능
        
        // 1. MissingPersonDAO를 통해 신고 상태 변경
        missingPersonDAO.updateMissingStatus(missingPostId, newStatus);

        // 2. 상태가 '찾음'(Found) 등으로 변경되었다면, 해당 사용자의 user_status를 0으로 변경
        if ("찾음".equals(newStatus)) { // 실제 사용할 상태값으로 변경
            // 신고 ID로 환자 ID를 조회하는 기능이 DAO에 필요
            MissingPersonDto report = missingPersonDAO.findMissingReportById(missingPostId); // 이 메서드 필요
            if (report != null) {
                userDAO.updateUserStatus(report.getPatientUserNo(), 0); // UserDAO에 메서드 추가 필요
            }
        }
    }

    /**
     * user_status가 1인 ('실종' 상태) 사용자 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<UserVO> findMissingStatusUsers() { 
        return userDAO.findMissingUsers();
    }

    
}
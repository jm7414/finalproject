package lx.project.dementia_care.service;

import java.nio.file.AccessDeniedException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lx.project.dementia_care.dao.UserDAO;
import lx.project.dementia_care.dao.MissingPersonDAO;

import lx.project.dementia_care.dto.MissingPersonDto;
import lx.project.dementia_care.vo.UserVO;

@Service
public class MissingPersonService {

    @Autowired
    private UserDAO userDAO;
    @Autowired
    private MissingPersonDAO missingPersonDAO;

    /**
     * user_status가 1인 ('실종' 상태) 사용자 목록과
     * 관련된 최신 missing_post 정보를 조합하여 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<MissingPersonDto> findMissingPersonsWithDetails(Integer currentUserId) { 

        // 1. UserDAO를 통해 user_status가 1인 사용자 목록(기본 정보)을 가져옵니다.
        List<UserVO> missingUsers = userDAO.findMissingUsers();

        // 2. 각 사용자에 대해 MissingPersonDAO를 통해 최신 실종 신고 정보를 가져와 합칩니다.
        List<MissingPersonDto> results = new ArrayList<>();
        for (UserVO user : missingUsers) {
            
            // DAO 호출 시 currentUserId 전달 (DAO/Mapper는 이전에 수정 완료)
            MissingPersonDto reportDetails = missingPersonDAO.findLatestMissingReportByPatientNo(user.getUserNo(), currentUserId); 

            // MissingPersonDto를 만들고 UserVO와 reportDetails 정보를 합칩니다.
            MissingPersonDto dto = new MissingPersonDto();
            dto.setPatientUserNo(user.getUserNo());
            dto.setPatientName(user.getName());
            dto.setPatientBirthDate(user.getBirthDate());
            dto.setPhotoPath(reportDetails != null && reportDetails.getPhotoPath() != null ? reportDetails.getPhotoPath() : user.getProfilePhoto());

            if (reportDetails != null) {
                dto.setMissingPostId(reportDetails.getMissingPostId());
                dto.setReporterUserNo(reportDetails.getReporterUserNo());
                dto.setDescription(reportDetails.getDescription());
                dto.setReportedAt(reportDetails.getReportedAt());
                dto.setStatus(reportDetails.getStatus());
                dto.setSearchTogetherCount(reportDetails.getSearchTogetherCount());
                dto.setCurrentUserJoined(reportDetails.getCurrentUserJoined()); 
                
            } else {
                dto.setStatus("정보 없음");
                dto.setSearchTogetherCount(0);
                dto.setCurrentUserJoined(false); // 신고 정보가 없으면 false
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
     * 특정 실종 신고 ID (missingPostId)로 
     * 신고 정보와 환자 정보를 조합하여 상세 조회합니다.
     */
    @Transactional(readOnly = true)
    public MissingPersonDto getMissingPersonDetailById(Integer missingPostId) {
        
        // 1. DAO를 통해 ID로 기본 신고 정보를 조회합니다.
        MissingPersonDto reportDetails = missingPersonDAO.findMissingReportById(missingPostId);

        // 2. 신고 정보가 없으면 null을 반환합니다 (Controller에서 404 처리).
        if (reportDetails == null) {
            return null;
        }

        // 3. 신고 정보에 연결된 환자 ID(patientUserNo)를 가져옵니다.
        Integer patientUserNo = reportDetails.getPatientUserNo();
        if (patientUserNo == null) {
            return reportDetails; // 환자 ID가 없으면 신고 정보만 반환
        }

        // 4. 환자 ID로 UserDAO를 통해 환자의 상세 정보(UserVO)를 조회합니다.
        UserVO patientUser = null;
        try {
             // UserDAO의 findByUserNo는 Exception을 던질 수 있으므로 try-catch
            patientUser = userDAO.findByUserNo(patientUserNo);
        } catch (Exception e) {
            // 예외 처리 (로그 남기기 등)
            System.err.println("환자 정보 조회 중 오류 발생: " + e.getMessage());
        }

        // 5. 환자 정보가 있다면, DTO에 환자 정보를 추가(Merge)합니다.
        if (patientUser != null) {
            reportDetails.setPatientName(patientUser.getName());
            reportDetails.setPatientBirthDate(patientUser.getBirthDate());

            // 신고 사진(photoPath)이 없다면 사용자 프로필 사진(profilePhoto)을 사용
            if (reportDetails.getPhotoPath() == null || reportDetails.getPhotoPath().isEmpty()) {
                reportDetails.setPhotoPath(patientUser.getProfilePhoto());
            }
        }
        
        // 7. 정보가 조합된 DTO를 반환합니다.
        return reportDetails;
    }

    /**
     * 사용자가 특정 실종 신고의 '함께 찾기'에 참여합니다.
     * @param missingPostId 참여할 실종 신고 ID
     * @param userId 참여하는 사용자 ID
     * @return true: 새로 참여 성공 / false: 이미 참여 중
     * @throws IllegalArgumentException 실종 신고 ID가 존재하지 않거나 유효하지 않을 때
     */
    @Transactional
    public boolean joinSearchTogether(Integer missingPostId, Integer userId) {
        // missingPostId가 실제로 DB에 존재하는지 확인
        MissingPersonDto existingPost = missingPersonDAO.findMissingReportById(missingPostId);
        if (existingPost == null) {
            throw new IllegalArgumentException("존재하지 않는 실종 신고 ID입니다: " + missingPostId);
        }

        try {
            // DAO를 호출하여 search_Together 테이블에 INSERT 시도
            missingPersonDAO.addSearchTogetherEntry(missingPostId, userId);
            return true;
        } catch (DuplicateKeyException e) {
            //  이미 참여 중인 경우 예외를 잡아서 false 반환
            System.out.println("이미 참여 중인 사용자입니다 (missingPostId: " + missingPostId + ", userId: " + userId + ")");
            return false;
        } catch (DataIntegrityViolationException e) {
            // 만약 missing_post_id나 user_id가 FK 제약조건을 위반하는 경우
             System.err.println("함께 찾기 FK 오류 (missingPostId: " + missingPostId + ", userId: " + userId + "): " + e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 실종 신고 ID 또는 사용자 ID 입니다.");
        }
    }

    /**
     * 특정 실종 신고에 참여하는 사용자 목록을 조회합니다.
     * @param missingPostId 실종 신고 ID
     * @return 참여자 정보 목록 (UserVO 리스트)
     * @throws IllegalArgumentException 실종 신고 ID가 존재하지 않을 때
     */
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    public List<UserVO> findParticipants(Integer missingPostId) {
        MissingPersonDto existingPost = missingPersonDAO.findMissingReportById(missingPostId);
        if (existingPost == null) {
            throw new IllegalArgumentException("존재하지 않는 실종 신고 ID입니다: " + missingPostId);
        }
        // DAO를 호출하여 참여자 목록 조회
        List<UserVO> participants = missingPersonDAO.findParticipantsByMissingPostId(missingPostId);
        return participants;
    }

    /**
     * [추가] 환자 번호로 최신 실종 신고를 조회하는 메서드
     * 
     * 기존의 getMissingPersonDetailById는 missingPostId를 받지만,
     * 이 메서드는 patientUserNo(환자 번호)를 받아서 조회합니다.
     * 
     * @param patientUserNo 환자의 user_no
     * @return 환자의 최신 실종 신고 정보 (환자 정보 포함)
     */
    @Transactional(readOnly = true)
    // currentUserId 파라미터 추가
    public MissingPersonDto findLatestReportByPatientNo(Integer patientUserNo, Integer currentUserId) {
        
        // --- [수정 2] DAO 호출 시 currentUserId 전달 ---
        MissingPersonDto reportDetails = missingPersonDAO.findLatestMissingReportByPatientNo(patientUserNo, currentUserId);
        
        // 2. 신고 정보가 없으면 null 반환
        if (reportDetails == null) {
            return null;
        }
        
        // 3. 환자 정보 추가 (기존 로직 동일)
        UserVO patientUser = null;
        try {
            patientUser = userDAO.findByUserNo(patientUserNo);
        } catch (Exception e) {
            System.err.println("환자 정보 조회 중 오류 발생: " + e.getMessage());
        }
        
        // 4. 환자 정보를 DTO에 추가
        if (patientUser != null) {
            reportDetails.setPatientName(patientUser.getName());
            reportDetails.setPatientBirthDate(patientUser.getBirthDate());
            
            // 신고 사진이 없다면 사용자 프로필 사진 사용
            if (reportDetails.getPhotoPath() == null || reportDetails.getPhotoPath().isEmpty()) {
                reportDetails.setPhotoPath(patientUser.getProfilePhoto());
            }
        }
        
        return reportDetails;
    }

    /**
     * [추가] 확인하지 않은 최신 알림 1건 조회 (폴링용)
     */
    @Transactional(readOnly = true)
    public MissingPersonDto findLatestAlertForUser(Integer userId) {
        return missingPersonDAO.findLatestAlertForUser(userId);
    }

    /**
     * [추가] 알림 확인 기록을 DB에 저장 (확인 버튼 클릭 시)
     */
    @Transactional
    public void confirmAlert(Integer missingPostId, Integer userId) {
        missingPersonDAO.addAlertConfirmation(missingPostId, userId);
    }

}
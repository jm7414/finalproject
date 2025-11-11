package lx.project.dementia_care.controller;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lx.project.dementia_care.dto.MissingPersonDto;
import lx.project.dementia_care.service.MissingPersonService;
import lx.project.dementia_care.vo.UserVO;
import lx.project.dementia_care.dto.LocationResponseDTO;

@RestController
@RequestMapping("/api/missing-persons")
public class MissingPersonController {

    @Autowired
    private MissingPersonService missingPersonService;

    /**
     * user_status가 1인 사용자와 관련된 최신 실종 정보를 조합하여 조회하는 API
     */
    @GetMapping
    public ResponseEntity<List<MissingPersonDto>> getMissingPersonsWithDetails(Authentication authentication) { // <---
                                                                                                                // (1)
                                                                                                                // Authentication
                                                                                                                // 추가

        Integer currentUserId = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            UserVO userVO = (UserVO) authentication.getPrincipal();
            currentUserId = userVO.getUserNo(); // <--- (2) 현재 사용자 ID 확보
        }

        List<MissingPersonDto> missingPersons = missingPersonService.findMissingPersonsWithDetails(currentUserId); // <---
                                                                                                                   // (3)
                                                                                                                   // 서비스로
                                                                                                                   // ID
                                                                                                                   // 전달
        return ResponseEntity.ok(missingPersons);
    }

    /**
     * 특정 실종 신고 ID로 상세 정보를 조회하는 API
     */
    @GetMapping("/{missingPostId}")
    public ResponseEntity<MissingPersonDto> getMissingPersonDetailById(@PathVariable Integer missingPostId) {
        MissingPersonDto missingPersonDetail = missingPersonService.getMissingPersonDetailById(missingPostId);
        if (missingPersonDetail != null) {
            return ResponseEntity.ok(missingPersonDetail);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * [추가] 환자 번호(user_no)로 최신 실종 신고를 조회하는 API
     * GET /api/missing-persons/patient/{patientUserNo}/latest
     * 
     * 용도: 프론트엔드에서 환자 ID는 알지만 실종 신고 ID를 모를 때 사용
     * 예시: /api/missing-persons/patient/4/latest
     * 
     * @param patientUserNo 환자의 user_no
     * @return 해당 환자의 가장 최근 실종 신고 정보 (없으면 404)
     */
    @GetMapping("/patient/{patientUserNo}/latest")
    public ResponseEntity<MissingPersonDto> getLatestMissingReportByPatient(
            @PathVariable Integer patientUserNo,
            Authentication authentication) {

        try {
            // 현재 사용자 ID 추출
            Integer currentUserId = null;
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                UserVO userVO = (UserVO) authentication.getPrincipal();
                currentUserId = userVO.getUserNo();
            }

            // 서비스 호출 시 currentUserId 전달
            MissingPersonDto latestReport = missingPersonService.findLatestReportByPatientNo(patientUserNo,
                    currentUserId);

            if (latestReport != null) {
                return ResponseEntity.ok(latestReport);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

        } catch (Exception e) {
            System.err.println("환자 번호로 최신 실종 신고 조회 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 새로운 실종 신고를 접수하고 해당 사용자의 user_status를 1로 변경하는 API
     */
    @PostMapping("/report")
    public ResponseEntity<MissingPersonDto> reportMissingPerson(
            @RequestBody MissingPersonDto missingPersonDto,
            Authentication authentication) {

        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer reporterUserId = userVO.getUserNo();

        if (missingPersonDto.getPatientUserNo() == null) {
            return ResponseEntity.badRequest().body(null);
        }

        try {
            MissingPersonDto createdReport = missingPersonService
                    .createMissingPersonReportAndUpdateStatus(missingPersonDto, reporterUserId);
            return new ResponseEntity<>(createdReport, HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println("실종 신고 처리 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 실종 신고 상태와 해당 사용자의 user_status를 업데이트하는 API
     */
    @PutMapping("/{missingPostId}/status")
    public ResponseEntity<Void> updateStatusAndUserStatus(
            @PathVariable Integer missingPostId,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer currentUserId = userVO.getUserNo();
        String newStatus = payload.get("status");

        if (newStatus == null || newStatus.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            missingPersonService.updateStatusAndUserStatus(missingPostId, newStatus, currentUserId);
            return ResponseEntity.ok().build();
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 사용자가 특정 실종 신고의 '함께 찾기'에 참여하는 API
     * POST /api/missing-persons/{missingPostId}/join
     */
    @PostMapping("/{missingPostId}/join")
    public ResponseEntity<Map<String, Object>> joinSearchTogether(
            @PathVariable Integer missingPostId,
            Authentication authentication) {

        // 1. 로그인 확인
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        // 2. 현재 사용자 ID 가져오기
        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer userId = userVO.getUserNo();

        try {
            // 3. Service 호출
            boolean joined = missingPersonService.joinSearchTogether(missingPostId, userId);

            if (joined) {
                // 새로 참여한 경우
                return ResponseEntity.status(HttpStatus.CREATED) // 201 Created
                        .body(Map.of("success", true, "message", "함께 찾기에 참여했습니다.")); // tlsrltn
            } else {
                // 이미 참여 중인 경우
                return ResponseEntity.ok() // 200 OK
                        .body(Map.of("success", true, "message", "이미 참여 중입니다."));
            }

        } catch (IllegalArgumentException e) {
            // Service에서 실종 신고 ID를 찾지 못한 경우 등
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            // 기타 예외 처리
            System.err.println("함께 찾기 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace(); // 상세 로그 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 실종 신고에 참여하는 사용자 목록을 조회하는 API
     * GET /api/missing-persons/{missingPostId}/participants
     */
    @GetMapping("/{missingPostId}/participants")
    public ResponseEntity<?> getParticipants(
            @PathVariable Integer missingPostId) {

        try {
            // Service 호출하여 참여자 목록 조회
            List<UserVO> participants = missingPersonService.findParticipants(missingPostId);
            return ResponseEntity.ok(participants); // 조회된 목록 반환

        } catch (IllegalArgumentException e) {
            // Service에서 실종 신고 ID 못 찾은 경우
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // 기타 예외 처리
            System.err.println("참여자 목록 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "참여자 목록 조회 중 오류 발생"));
        }
    }

    /**
     * [추가] 폴링(Polling) 알림용 API
     * 로그인한 사용자가 '확인'하지 않은 '최신 실종 신고' 1건을 조회합니다.
     */
    @GetMapping("/latest-alert") // (수정) /latest 대신 /latest-alert로 주소 변경 (기존 API와 충돌 방지)
    public ResponseEntity<MissingPersonDto> getLatestMissingAlert(Authentication authentication) {
        
        Integer currentUserId = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            UserVO userVO = (UserVO) authentication.getPrincipal();
            currentUserId = userVO.getUserNo();
        } else {
            // 비로그인 사용자는 알림 없음
            return ResponseEntity.noContent().build();
        }

        MissingPersonDto latestAlert = missingPersonService.findLatestAlertForUser(currentUserId);

        if (latestAlert != null) {
            return ResponseEntity.ok(latestAlert); // (200 OK) 새 알림 반환
        } else {
            return ResponseEntity.noContent().build(); // (204 No Content) 알림 없음
        }
    }

    /**
     * [추가] 사용자가 실종 알림을 "확인"했음을 기록하는 API
     */
    @PostMapping("/{missingPostId}/confirm-alert")
    public ResponseEntity<Void> confirmMissingAlert(
            @PathVariable Integer missingPostId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer currentUserId = userVO.getUserNo();
        
        try {
            missingPersonService.confirmAlert(missingPostId, currentUserId);
            return ResponseEntity.ok().build(); // (200 OK)
        } catch (Exception e) {
            // 이미 확인했거나, DB 오류 발생 시
            System.err.println("알림 확인 처리 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409
        }
    }

    /**
     * 특정 실종 신고에 참여하는 사용자들의 '실시간 위치' 목록을 조회하는 API
     */
    // ⭐ 3. ResponseEntity의 타입을 LocationResponseDTO로 변경
    @GetMapping("/{missingPostId}/participants/locations")
    public ResponseEntity<List<LocationResponseDTO>> getParticipantLocations(
            @PathVariable Integer missingPostId) {
        
        try {
            // Service의 새 로직을 호출
            List<LocationResponseDTO> locations = missingPersonService.findParticipantLocations(missingPostId);
            return ResponseEntity.ok(locations);
            
        } catch (Exception e) {
            System.err.println("참여자 위치 목록 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
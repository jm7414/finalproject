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

@RestController
@RequestMapping("/api/missing-persons")
public class MissingPersonController {

    @Autowired
    private MissingPersonService missingPersonService;

    /**
     * user_status가 1인 사용자와 관련된 최신 실종 정보를 조합하여 조회하는 API
     */
    @GetMapping
    public ResponseEntity<List<MissingPersonDto>> getMissingPersonsWithDetails() {
        List<MissingPersonDto> missingPersons = missingPersonService.findMissingPersonsWithDetails();
        return ResponseEntity.ok(missingPersons);
    }

    // /**
    //  * 특정 실종 신고 ID로 상세 정보를 조회하는 API
    //  */
    // @GetMapping("/{missingPostId}")
    // public ResponseEntity<MissingPersonDto> getMissingPersonDetailById(@PathVariable Integer missingPostId) {
    //     MissingPersonDto missingPersonDetail = missingPersonService.getMissingPersonDetailById(missingPostId); // Service에 이 메서드 필요
    //     if (missingPersonDetail != null) {
    //         return ResponseEntity.ok(missingPersonDetail);
    //     } else {
    //         return ResponseEntity.notFound().build();
    //     }
    // }

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
            MissingPersonDto createdReport = missingPersonService.createMissingPersonReportAndUpdateStatus(missingPersonDto, reporterUserId);
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
}
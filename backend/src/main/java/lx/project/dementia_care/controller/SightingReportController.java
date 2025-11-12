package lx.project.dementia_care.controller;

import lx.project.dementia_care.dto.SightingReportCommentDto;
import lx.project.dementia_care.dto.SightingReportListDto;
import lx.project.dementia_care.dto.SightingReportRequestDto;
import lx.project.dementia_care.dto.SightingReportResponseDto;
import lx.project.dementia_care.service.SightingReportCommentService;
import lx.project.dementia_care.service.SightingReportService;
import lx.project.dementia_care.vo.UserVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sighting-reports") // API 기본 경로 (PostController와 다름)
@RequiredArgsConstructor
public class SightingReportController {

    private final SightingReportService sightingReportService;
    private final SightingReportCommentService commentService;

    /**
     * (제보 목록) 특정 실종 건(missingPostId)에 대한 모든 제보 조회
     * GET /api/sighting-reports/missing/{missingPostId}
     * (참고: MissingSightingReportBoard.vue가 이 주소를 호출하도록 수정해야 합니다)
     */
    @GetMapping("/missing/{missingPostId}")
    public ResponseEntity<List<SightingReportListDto>> getReportsByMissingPostId(
            @PathVariable Integer missingPostId) {
        
        List<SightingReportListDto> reports = sightingReportService.findReportsByMissingPostId(missingPostId);
        return ResponseEntity.ok(reports);
    }

    /**
     * (제보 상세) ID로 1건의 제보 상세 조회
     * GET /api/sighting-reports/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SightingReportResponseDto> getReportById(@PathVariable Integer id) {
        SightingReportResponseDto report = sightingReportService.findReportById(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    /**
     * (제보 생성) 새로운 제보 생성
     * POST /api/sighting-reports
     */
    @PostMapping
    public ResponseEntity<Integer> createReport(@RequestBody SightingReportRequestDto requestDto, Authentication authentication) {
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer newReportId = sightingReportService.createReport(requestDto, userVO.getUserNo());
        return new ResponseEntity<>(newReportId, HttpStatus.CREATED);
    }

    /**
     * (제보 수정) ID로 1건의 제보 수정
     * PUT /api/sighting-reports/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateReport(@PathVariable Integer id, @RequestBody SightingReportRequestDto requestDto, Authentication authentication) {
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();
        
        try {
            sightingReportService.updateReport(id, requestDto, userVO.getUserNo());
            return ResponseEntity.ok().build();
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // 403 권한 없음
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * (제보 삭제) ID로 1건의 제보 삭제
     * DELETE /api/sighting-reports/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Integer id, Authentication authentication) {
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();

        try {
            sightingReportService.deleteReport(id, userVO.getUserNo());
            return ResponseEntity.noContent().build(); // 204
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * 특정 제보(ID)에 달린 모든 댓글 목록을 조회합니다.
     * (SightingReport.vue가 onMounted될 때 이 API를 호출합니다)
     */
    @GetMapping("/{reportId}/comments")
    public ResponseEntity<List<SightingReportCommentDto>> getCommentsByReportId(
            @PathVariable Integer reportId) {
        
        List<SightingReportCommentDto> comments = commentService.findCommentsByReportId(reportId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 특정 제보(ID)에 새로운 댓글을 생성합니다.
     * (SightingReport.vue가 '게시' 버튼을 누를 때 이 API를 호출합니다)
     */
    @PostMapping("/{reportId}/comments")
    public ResponseEntity<SightingReportCommentDto> createComment(
            @PathVariable Integer reportId,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();
        String content = payload.get("content"); // JSON에서 "content" 값 추출

        // 5. 'SightingReportCommentService'의 'createComment' 함수를 호출
        SightingReportCommentDto newComment = commentService.createComment(reportId, content, userVO.getUserNo());
        return new ResponseEntity<>(newComment, HttpStatus.CREATED);
    }
}
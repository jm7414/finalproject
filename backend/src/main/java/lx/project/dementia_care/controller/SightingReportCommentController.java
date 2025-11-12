package lx.project.dementia_care.controller;

import lx.project.dementia_care.dto.SightingReportCommentDto;
import lx.project.dementia_care.service.SightingReportCommentService;
import lx.project.dementia_care.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sighting-report-comments")
@RequiredArgsConstructor
public class SightingReportCommentController {

    private final SightingReportCommentService commentService;

    // ==========================================================
    // [API 4] '댓글 ID'로 댓글 1건 삭제
    // (DELETE /api/sighting-report-comments/{commentId})
    // ==========================================================
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Integer commentId,
            Authentication authentication) {
        
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();

        try {
            commentService.deleteComment(commentId, userVO.getUserNo());
            return ResponseEntity.noContent().build(); // 204
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // 403
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // 500
        }
    }

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/{reportId}/comments")
    public ResponseEntity<List<SightingReportCommentDto>> getCommentsByReportId(
            @PathVariable Integer reportId) {
        
        List<SightingReportCommentDto> comments = commentService.findCommentsByReportId(reportId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 댓글 생성
     */
    @PostMapping("/{reportId}/comments")
    public ResponseEntity<SightingReportCommentDto> createComment(
            @PathVariable Integer reportId,
            @RequestBody Map<String, String> payload, // {"content": "..."}
            Authentication authentication) {
        
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();
        String content = payload.get("content");

        SightingReportCommentDto newComment = commentService.createComment(reportId, content, userVO.getUserNo());
        return new ResponseEntity<>(newComment, HttpStatus.CREATED);
    }
}
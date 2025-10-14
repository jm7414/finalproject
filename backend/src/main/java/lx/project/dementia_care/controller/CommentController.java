package lx.project.dementia_care.controller;

import java.nio.file.AccessDeniedException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lx.project.dementia_care.dto.CommentDto;
import lx.project.dementia_care.service.CommentService;
import lx.project.dementia_care.vo.UserVO;

@RestController
@RequestMapping("/api") // API 요청들의 기본 경로
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 특정 게시물의 모든 댓글 목록을 조회하는 API
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDto>> getCommentsByPostId(@PathVariable Integer postId) {
        List<CommentDto> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 특정 게시물에 새로운 댓글을 작성하는 API
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentDto> createComment(
            @PathVariable Integer postId,
            @RequestBody CommentDto commentDto,
            Authentication authentication) {
        
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer currentUserId = userVO.getUserNo();
        
        commentDto.setPostId(postId);

        CommentDto createdComment = commentService.createComment(commentDto, currentUserId);
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }

    /**
     * 특정 댓글을 삭제하는 API
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Integer commentId, Authentication authentication) {
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer currentUserId = userVO.getUserNo();

        try {
            commentService.deleteComment(commentId, currentUserId);
            return ResponseEntity.noContent().build(); // 성공 시 204 No Content 응답
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // 권한 없음 시 403 Forbidden 응답
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 댓글 없음 시 404 Not Found 응답
        }
    }
}
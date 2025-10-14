package lx.project.dementia_care.service;

import java.nio.file.AccessDeniedException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lx.project.dementia_care.dao.CommentDAO;
import lx.project.dementia_care.dto.CommentDto;

@Service // 이 클래스가 핵심 비즈니스 로직을 담당하는 서비스임을 알립니다.
public class CommentService {

    @Autowired
    private CommentDAO commentDAO;

    /**
     * 특정 게시물의 모든 댓글을 조회합니다.
     */
    @Transactional(readOnly = true) // 데이터를 읽기만 하므로 readOnly 옵션 사용
    public List<CommentDto> getCommentsByPostId(Integer postId) {
        return commentDAO.findCommentsByPostId(postId);
    }

    /**
     * 새로운 댓글을 생성합니다.
     */
    @Transactional
    public CommentDto createComment(CommentDto commentDto, Integer userId) {
        // 컨트롤러로부터 받은 DTO에 작성자 ID를 설정합니다.
        commentDto.setUserId(userId);
        
        // DAO를 통해 DB에 댓글을 저장합니다.
        commentDAO.createComment(commentDto);
        
        // 생성된 댓글 정보를 다시 조회해서 반환합니다 (생성된 ID, 시간 등 포함).
        return commentDAO.findCommentById(commentDto.getCommentId());
    }

    /**
     * 댓글을 삭제합니다. (권한 확인 로직 포함)
     */
    @Transactional
    public void deleteComment(Integer commentId, Integer currentUserId) throws AccessDeniedException {
        // 1. 먼저 삭제하려는 댓글의 전체 정보를 가져옵니다.
        CommentDto comment = commentDAO.findCommentById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("해당 댓글이 없습니다. id=" + commentId);
        }

        // 2. ✨✨✨ 가장 중요한 권한 체크 로직 ✨✨✨
        // 만약 (현재 사용자가 댓글 작성자가 아니고) AND (현재 사용자가 운영자(1번)도 아니라면)
        if (!comment.getUserId().equals(currentUserId) && currentUserId != 1) {
            // AccessDeniedException을 발생시켜 삭제를 막습니다.
            throw new AccessDeniedException("댓글을 삭제할 권한이 없습니다.");
        }
        
        // 3. 권한이 있다면, 댓글 삭제를 진행합니다.
        commentDAO.deleteComment(commentId);
    }
}
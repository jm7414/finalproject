package lx.project.dementia_care.service;

import lx.project.dementia_care.dao.SightingReportCommentDAO;
import lx.project.dementia_care.dto.SightingReportCommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SightingReportCommentService {

    private final SightingReportCommentDAO commentDAO;

    // [API 2] 댓글 목록 조회
    @Transactional(readOnly = true)
    public List<SightingReportCommentDto> findCommentsByReportId(Integer reportId) {
        return commentDAO.findCommentsByReportId(reportId);
    }

    // [API 3] 댓글 생성
    @Transactional
    public SightingReportCommentDto createComment(Integer reportId, String content, Integer userNo) {
        SightingReportCommentDto comment = new SightingReportCommentDto();
        comment.setSightingReportId(reportId);
        comment.setContent(content);
        comment.setUserNo(userNo);
        commentDAO.createComment(comment);
        return commentDAO.findCommentById(comment.getCommentId());
    }

    // [API 4] 댓글 삭제
    @Transactional
    public void deleteComment(Integer commentId, Integer currentUserNo) throws AccessDeniedException {
        Integer authorUserNo = commentDAO.findUserNoByCommentId(commentId);
        if (authorUserNo == null) {
            throw new RuntimeException("댓글을 찾을 수 없습니다.");
        }
        if (!authorUserNo.equals(currentUserNo) && currentUserNo != 1) {
            throw new AccessDeniedException("이 댓글을 삭제할 권한이 없습니다.");
        }
        commentDAO.deleteComment(commentId, currentUserNo);
    }
}
package lx.project.dementia_care.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lx.project.dementia_care.dto.CommentDto;

@Component
public class CommentDAO {

    @Autowired
    private SqlSession session;

    // mapper-postComment.xml의 namespace와 일치해야 합니다.
    private static final String NAMESPACE = "lx.project.dementia_care.mapper.CommentMapper";

    /**
     * 특정 게시물의 모든 댓글 목록을 조회합니다.
     */
    public List<CommentDto> findCommentsByPostId(Integer postId) {
        return session.selectList(NAMESPACE + ".findCommentsByPostId", postId);
    }

    /**
     * 새로운 댓글을 DB에 저장합니다.
     */
    public void createComment(CommentDto commentDto) {
        session.insert(NAMESPACE + ".createComment", commentDto);
    }
    
    /**
     * 댓글 ID로 특정 댓글의 정보를 조회합니다. (삭제 시 권한 확인용)
     */
    public CommentDto findCommentById(Integer commentId) {
        return session.selectOne(NAMESPACE + ".findCommentById", commentId);
    }

    /**
     * 댓글 ID로 특정 댓글을 삭제합니다.
     */
    public void deleteComment(Integer commentId) {
        session.delete(NAMESPACE + ".deleteComment", commentId);
    }
}
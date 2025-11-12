package lx.project.dementia_care.dao;

import lx.project.dementia_care.dto.SightingReportCommentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SightingReportCommentDAO {

    // 특정 제보(ID)에 달린 모든 댓글 조회
    List<SightingReportCommentDto> findCommentsByReportId(Integer sightingReportId);

    // 댓글 1건 생성
    void createComment(SightingReportCommentDto comment);

    // 댓글 생성 직후, 방금 생성된 댓글의 ID와 상세정보를 다시 조회 (프론트에 반환하기 위함)
    SightingReportCommentDto findCommentById(Integer commentId);

    // 댓글 1건 삭제
    int deleteComment(@Param("commentId") Integer commentId, @Param("userNo") Integer userNo);

    // (수정/삭제 권한 확인용) 댓글 1건의 작성자 user_no 조회
    Integer findUserNoByCommentId(Integer commentId);
}
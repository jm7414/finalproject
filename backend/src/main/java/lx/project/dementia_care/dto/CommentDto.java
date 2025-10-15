package lx.project.dementia_care.dto;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentDto {

    // 댓글 고유 ID
    private Integer commentId;

    // 이 댓글이 달린 게시물 ID
    private Integer postId;

    // 댓글을 작성한 사용자의 ID
    private Integer userId;

    // 댓글을 작성한 사용자의 이름
    private String author;

    private String authorProfileImage;    

    // 댓글 내용
    private String content;

    // 댓글 작성 시간
    private OffsetDateTime createdAt;

    
}
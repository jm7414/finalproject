package lx.project.dementia_care.dto;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostResponseDto {

    // 게시물 고유 ID
    private Integer postId;

    // 제목
    private String title;

    // 내용 전체
    private String content;
    
    // 작성자 닉네임 (DB 조인을 통해 가져온 값)
    private String author;

    // 작성 시간
    private OffsetDateTime createdAt;

    // 조회수
    private Integer views;

    // 좋아요 수 (DB에서 COUNT하여 가져온 값)
    private int likes;

    // 댓글 수 (DB에서 COUNT하여 가져온 값)
    private int comments;

    // 이미지 경로
    private String image;

    // 게시물 쓴 사람
    private Integer userId;
}
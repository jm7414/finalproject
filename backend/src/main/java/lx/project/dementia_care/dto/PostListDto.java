package lx.project.dementia_care.dto;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;         // ◀ Setter 추가

@Getter
@Setter
public class PostListDto {

    // 게시물 고유 ID
    private Integer postId;

    // 제목
    private String title;

    // 작성자 닉네임
    private String author;

    // 댓글 수
    private int comments;

    // 좋아요 수
    private int likes;

    // 조회수
    private int views;

    // 작성 시간
    private OffsetDateTime createdAt;
    
    // 목록에서는 'content'나 'image' 같은 무거운 데이터는 제외합니다.
}
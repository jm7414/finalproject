package lx.project.dementia_care.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.OffsetDateTime;

@Getter
@Builder
public class PostListDto {
    private Integer id;
    private String title;
    private String author;
    private int comments;
    private int likes;
    private int views;
    private OffsetDateTime createdAt;
}

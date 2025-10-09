package lx.project.dementia_care.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostResponseDto {

    private Integer id;
    private String author;
    private String authorProfileImage;
    private OffsetDateTime date;
    private String title;
    private String content;
    private String image;
    private Integer likes;
    private Integer views;
    private List<CommentDto> comments;

}
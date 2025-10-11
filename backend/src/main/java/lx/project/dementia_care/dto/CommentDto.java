package lx.project.dementia_care.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentDto {

    private Integer id;
    private String author;
    private String authorProfileImage;
    private String time; // '3분 전' 과 같은 상대 시간을 위해 String 타입 사용
    private String text;

}
package lx.project.dementia_care.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequestDto {

    private Integer postId;

    // 사용자가 직접 입력하는 '제목'
    private String title;

    // 사용자가 직접 입력하는 '내용'
    private String content;

    // 이미지가 있다면 이미지 경로 또는 파일 정보
    private String image;

    // 어떤 유저가 작성했는지 식별하기 위한 ID
    private Integer userId;

    //지겸
    // 공지 여부: '공지' 또는 null
    private String noticeCheck;

}
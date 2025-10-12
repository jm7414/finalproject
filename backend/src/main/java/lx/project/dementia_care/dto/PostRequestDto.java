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
    // (실제로는 세션이나 토큰에서 추출하여 Service에서 설정하는 경우가 많습니다)
    private Integer userId;

    // postId, createdAt, views 등은 서버에서 생성하므로 요청 DTO에는 포함하지 않습니다.
}
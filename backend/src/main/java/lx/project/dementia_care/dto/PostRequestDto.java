package lx.project.dementia_care.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // 프론트엔드에서 오는 JSON 데이터를 객체로 변환할 때 필요합니다.
public class PostRequestDto {
    private String title;
    private String content;
    // user_id, image_path 등은 나중에 추가될 수 있습니다.
}

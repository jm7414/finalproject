package lx.project.dementia_care.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.OffsetDateTime;

@Getter
@Builder
public class MissingPostDto {
    private Integer id;
    private String name;
    private Integer age;
    private String photoPath;
    private OffsetDateTime reportedAt; // 실종 신고 시간
    private Integer viewCount;
}
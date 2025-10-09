package lx.project.dementia_care.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.OffsetDateTime;

@Getter
@Builder
public class MissingPostDetailDto {
    private Integer id;
    private String name;
    private Integer age;
    private String photoPath;
    private OffsetDateTime reportedAt;
    private String description;         // 특이사항
    private Integer participantCount;   // 함께찾기 참여자 수
}
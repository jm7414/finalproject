package lx.project.dementia_care.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class SightingReportCommentDto {
    private Integer commentId;
    private Integer sightingReportId;
    private Integer userNo;
    private OffsetDateTime createdAt; 
    private String author; // user.name
    private String authorProfileImage; // user.profile_photo
    private String content;
}
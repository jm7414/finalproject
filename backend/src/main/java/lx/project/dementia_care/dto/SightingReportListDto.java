package lx.project.dementia_care.dto;

import lombok.Data;
import java.time.OffsetDateTime;
/**
 * '제보 목록' (MissingSightingReportBoard.vue)에
 * 여러 개의 제보를 뿌려줄 때 사용하는 DTO
 */
@Data
public class SightingReportListDto {

    private Integer sightingReportId; // 제보 ID
    private String author;              // 작성자 이름 (users.name)
    private String authorProfileImage;  // 작성자 프로필 사진 (users.profile_photo)
    private OffsetDateTime createdAt;   // 제보 작성 시간
    private String content;             // 제보 내용 (요약)
    private String imagePath;           // 제보 이미지 (썸네일)
    private int commentCount;           // 댓글 수
}
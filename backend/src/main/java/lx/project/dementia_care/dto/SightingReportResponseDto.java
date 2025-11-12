package lx.project.dementia_care.dto;

import lombok.Data;
import java.time.OffsetDateTime;

/**
 * '제보 상세' (MissingSightingReport.vue) 페이지에
 * 1개의 제보 데이터를 뿌려줄 때 사용하는 DTO
 */
@Data
public class SightingReportResponseDto {

    private Integer sightingReportId;
    private Integer userNo;             // 작성자의 user_no (수정/삭제 권한 비교용)
    private String author;              // 작성자 이름
    private String authorProfileImage;  // 작성자 프로필 사진
    private OffsetDateTime createdAt;
    private String content;
    private String imagePath;
    private Double latitude;
    private Double longitude;
    private int commentCount;           // 댓글 수
}
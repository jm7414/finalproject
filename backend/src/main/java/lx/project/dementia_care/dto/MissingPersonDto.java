package lx.project.dementia_care.dto;

import java.time.OffsetDateTime;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissingPersonDto {

    //  missing_post 테이블 컬럼 
    private Integer missingPostId;
    private Integer patientUserNo;
    private Integer reporterUserNo; // 신고자 번호
    private String photoPath;
    private String description;
    private OffsetDateTime reportedAt;
    private String status;

    //  JOIN 컬럼 (Mapper에서 AS로 지정한 별칭) 
    private String patientName;       // u.name AS patient_name
    private LocalDate patientBirthDate; // u.birth_date (Mapper 별칭 사용 안 함, DB 타입 LocalDate)
    private Integer searchTogetherCount; // COUNT(*) AS search_together_count
    
    private Boolean currentUserJoined; // 함께하는 사람 참여 확인 여부
    
}
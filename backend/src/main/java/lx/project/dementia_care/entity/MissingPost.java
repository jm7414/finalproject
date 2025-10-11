package lx.project.dementia_care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "missing_post") // DB 테이블 이름과 일치
public class MissingPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer missingPostId;

    private Integer patientUserNo;
    private Integer reporterUserNo;
    private String photoPath;
    private String description;
    private OffsetDateTime reportedAt;
    private String status;
}
package lx.project.dementia_care.vo;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class ReportVO {
    private Integer reportId;
    private Integer periodId;
    private Integer patientId;
    private String  content;
    private String  periodType;
    private String  periodKey;
    private Integer version;
    private OffsetDateTime generatedAt;
    private String  generatedBy;
    private Boolean locked;
    private String  sourceHash;

    // ★ jsonb는 전부 String으로
    private String  metrics;
    private String  sections;
    private String  chartPrefs;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
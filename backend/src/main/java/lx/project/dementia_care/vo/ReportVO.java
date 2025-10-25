package lx.project.dementia_care.vo;

import lombok.Data;

@Data
public class ReportVO {
	private Integer reportId;
	private Integer periodId;
	private Long patientId;
	private String content;

	private String periodType; // "year"
	private String periodKey; // "2024"
	private Integer version;
	private String generatedAt;
	private String generatedBy;
	private Boolean locked;
	private String sourceHash;

	private String metrics; // JSON 문자열
	private String sections; // JSON 문자열
	private String chartPrefs; // JSON 문자열

	private String createdAt;
	private String updatedAt;
}

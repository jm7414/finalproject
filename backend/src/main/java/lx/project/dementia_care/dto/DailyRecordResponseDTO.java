// src/main/java/lx/project/dementia_care/dto/DailyRecordResponseDTO.java
package lx.project.dementia_care.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecordResponseDTO {

	/** record.record_id */
	private Long id;

	/** 대상 환자 users.user_no */
	private Long userId;

	/** 기록 날짜 (yyyy-MM-dd) */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate recordDate;

	/** JSON 문자열 (ex: {"mood":"😀","note":"걷기 20분"}) */
	private String content;

	/** 생성/수정 시각(서버에서 채움) */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime createdAt;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime updatedAt;
}

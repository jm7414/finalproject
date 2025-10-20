// src/main/java/lx/project/dementia_care/dto/DailyRecordRequestDTO.java
package lx.project.dementia_care.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * 요청 DTO "오늘의 기록" 저장/업서트에 사용합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecordRequestDTO {

	/** 대상 환자 users.user_no (보호자가 연결된 환자) */
	private Long userId;

	/** 기록 날짜 (yyyy-MM-dd). 생략 시 서비스에서 today로 대체 가능 */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate recordDate;

	/** 프론트 selectedAnswers를 그대로 담는 JSON 내용 */
	private Map<String, Object> content;
}

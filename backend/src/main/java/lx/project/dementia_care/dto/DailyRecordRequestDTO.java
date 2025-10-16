// src/main/java/lx/project/dementia_care/dto/DailyRecordRequestDTO.java
package lx.project.dementia_care.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.Map;

/**
 * 일별 기록 저장/수정 요청용 DTO
 */
@Data
@Getter
@Setter
public class DailyRecordRequestDTO {
    /** 사용자 고유 ID */
    private Long userId;
    /** 기록 날짜 (yyyy-MM-dd) */
    private LocalDate recordDate;
    /** 속하는 기간(period) ID */
    private Long periodId;
    /** 식사 관련 답변 (questionKey→answerIndex) */
    private Map<String, Integer> mealAnswers;
    /** 약 복용 관련 답변 (questionKey→answerIndex) */
    private Map<String, Integer> medicationAnswers;
    /** 활동 관련 답변 (questionKey→answerIndex) */
    private Map<String, Integer> activityAnswers;
    /** 감정 관련 답변 (questionKey→answerIndex) */
    private Map<String, Integer> emotionAnswers;
    /** 특이사항 관련 답변 (questionKey→answerIndex) */
    private Map<String, Integer> specialAnswers;
}

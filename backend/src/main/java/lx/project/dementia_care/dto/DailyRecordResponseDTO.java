// src/main/java/lx/project/dementia_care/dto/DailyRecordResponseDTO.java
package lx.project.dementia_care.dto;

import lombok.Data;
import lombok.Getter;
import java.time.LocalDate;
import java.util.Map;

/**
 * 일별 기록 조회 응답용 DTO
 */
@Data
@Getter
public class DailyRecordResponseDTO {
    /** 레코드 고유 ID */
    private Long id;
    /** 사용자 고유 ID */
    private Long userId;
    /** 기록 날짜 */
    private LocalDate recordDate;
    /** 속하는 기간(period) ID */
    private Long periodId;
    /** 식사 관련 답변 맵 */
    private Map<String, Integer> mealAnswers;
    /** 약 복용 관련 답변 맵 */
    private Map<String, Integer> medicationAnswers;
    /** 활동 관련 답변 맵 */
    private Map<String, Integer> activityAnswers;
    /** 감정 관련 답변 맵 */
    private Map<String, Integer> emotionAnswers;
    /** 특이사항 관련 답변 맵 */
    private Map<String, Integer> specialAnswers;
}

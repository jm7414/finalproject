// src/main/java/lx/project/dementia_care/service/RecordService.java
package lx.project.dementia_care.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lx.project.dementia_care.dao.RecordDAO;
import lx.project.dementia_care.dto.DailyRecordRequestDTO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {

	private final RecordDAO recordDAO;
	private final ObjectMapper objectMapper; // Spring Boot가 자동 주입(Jackson)

	/**
	 * (user_id, record_date) 기준 업서트
	 */
	@Transactional
	public DailyRecordResponseDTO upsert(DailyRecordRequestDTO req) {
		if (req == null)
			throw new IllegalArgumentException("요청 바디가 비었습니다.");
		if (req.getUserId() == null)
			throw new IllegalArgumentException("userId는 필수입니다.");
		if (req.getRecordDate() == null)
			throw new IllegalArgumentException("recordDate는 필수입니다.");

		try {
			// Map -> JSON 문자열로 직렬화하여 JSONB 컬럼에 저장
			String contentJson = objectMapper.writeValueAsString(req.getContent());
			return recordDAO.upsert(req.getUserId(), req.getRecordDate(), contentJson);
		} catch (Exception e) {
			throw new IllegalArgumentException("content 직렬화 실패: " + e.getMessage());
		}
	}

	/**
	 * 특정 날짜 단건 조회
	 */
	@Transactional(readOnly = true)
	public DailyRecordResponseDTO getOne(Long userId, LocalDate date) {
		if (userId == null)
			throw new IllegalArgumentException("userId는 필수입니다.");
		if (date == null)
			throw new IllegalArgumentException("date는 필수입니다.");
		return recordDAO.getOne(userId, date);
	}

	/**
	 * 기간 조회 (start ~ end, 날짜 오름차순)
	 */
	@Transactional(readOnly = true)
	public List<DailyRecordResponseDTO> getRange(Long userId, LocalDate start, LocalDate end) {
		if (userId == null)
			throw new IllegalArgumentException("userId는 필수입니다.");
		if (start == null || end == null)
			throw new IllegalArgumentException("start/end는 필수입니다.");
		if (start.isAfter(end))
			throw new IllegalArgumentException("start는 end보다 이후일 수 없습니다.");
		return recordDAO.getRange(userId, start, end);
	}
}

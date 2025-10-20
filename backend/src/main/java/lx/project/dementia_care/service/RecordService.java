package lx.project.dementia_care.service;

import lx.project.dementia_care.dao.RecordDAO;
import lx.project.dementia_care.dto.DailyRecordRequestDTO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class RecordService {

	private final RecordDAO recordDAO;
	private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

	public RecordService(RecordDAO recordDAO) {
		this.recordDAO = recordDAO;
	}

	/** (user_id, record_date) 기준 업서트 */
	@Transactional
	public DailyRecordResponseDTO upsert(DailyRecordRequestDTO req) {
		if (req == null)
			throw new IllegalArgumentException("request is null");
		if (req.getUserId() == null)
			throw new IllegalArgumentException("userId is required");
		if (req.getContent() == null)
			throw new IllegalArgumentException("content is required");
		// recordDate가 null이어도 DAO에서 KST 오늘로 처리하므로 그대로 전달
		return recordDAO.upsert(req);
	}

	/** 단건 조회: userId + date (date가 null이면 KST 오늘로 처리) */
	@Transactional(readOnly = true)
	public DailyRecordResponseDTO getOne(Long userId, LocalDate date) {
		if (userId == null)
			throw new IllegalArgumentException("userId is required");
		if (date == null)
			date = LocalDate.now(ZONE_SEOUL);
		return recordDAO.findByUserIdAndDate(userId, date);
	}

	/** 기간 조회: [start, end] */
	@Transactional(readOnly = true)
	public List<DailyRecordResponseDTO> getRange(Long userId, LocalDate start, LocalDate end) {
		if (userId == null)
			throw new IllegalArgumentException("userId is required");
		if (start == null || end == null)
			throw new IllegalArgumentException("start/end are required");
		if (end.isBefore(start))
			throw new IllegalArgumentException("end must be on/after start");
		return recordDAO.findByRange(userId, start, end);
	}
}
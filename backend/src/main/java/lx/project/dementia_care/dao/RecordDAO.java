// src/main/java/lx/project/dementia_care/dao/RecordDAO.java
package lx.project.dementia_care.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lx.project.dementia_care.dto.DailyRecordRequestDTO;
import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RecordDAO {

	private final SqlSessionTemplate sql;
	private final ObjectMapper objectMapper;

	/** XML의 namespace (mapper-record.xml의 namespace와 정확히 일치해야 합니다) */
	private static final String NS = "lx.project.dementia_care.dao.RecordMapper";

	/** KST 기준 오늘 날짜 계산용 */
	private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

	public RecordDAO(SqlSessionTemplate sql, ObjectMapper objectMapper) {
		this.sql = sql;
		this.objectMapper = objectMapper;
	}

	/**
	 * 업서트 (user_id, record_date UNIQUE 전제) - content(Map)를 JSON 문자열로 직렬화 →
	 * contentJson으로 XML에 전달 - recordDate가 null이면 KST 오늘(LocalDate.now(Asia/Seoul))
	 * - XML: <select id="upsert" ...> INSERT ... ON CONFLICT ... RETURNING ...
	 */
	public DailyRecordResponseDTO upsert(DailyRecordRequestDTO req) {
		if (req == null)
			throw new IllegalArgumentException("request is null");
		if (req.getUserId() == null)
			throw new IllegalArgumentException("userId is required");
		if (req.getContent() == null)
			throw new IllegalArgumentException("content is required");

		LocalDate date = (req.getRecordDate() != null) ? req.getRecordDate() : LocalDate.now(ZONE_SEOUL);

		String contentJson;
		try {
			contentJson = objectMapper.writeValueAsString(req.getContent());
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("invalid content json", e);
		}

		Map<String, Object> p = new HashMap<>();
		p.put("userId", req.getUserId());
		p.put("recordDate", date);
		p.put("contentJson", contentJson);

		// INSERT ... RETURNING 을 <select>로 매핑했으므로 selectOne 사용
		return sql.selectOne(NS + ".upsert", p);
	}

	/** 단건 조회: userId + recordDate */
	public DailyRecordResponseDTO findByUserIdAndDate(Long userId, LocalDate recordDate) {
		if (userId == null || recordDate == null)
			return null;

		Map<String, Object> p = new HashMap<>();
		p.put("userId", userId);
		p.put("recordDate", recordDate);

		return sql.selectOne(NS + ".findByUserIdAndDate", p);
	}

	/** 기간 조회: [start, end] */
	public List<DailyRecordResponseDTO> findByRange(Long userId, LocalDate start, LocalDate end) {
		if (userId == null || start == null || end == null)
			return java.util.Collections.emptyList();

		Map<String, Object> p = new HashMap<>();
		p.put("userId", userId);
		p.put("start", start);
		p.put("end", end);

		return sql.selectList(NS + ".findByRange", p);
	}
}

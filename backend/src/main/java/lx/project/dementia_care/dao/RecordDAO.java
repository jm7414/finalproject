// src/main/java/lx/project/dementia_care/dao/RecordDAO.java
package lx.project.dementia_care.dao;

import lx.project.dementia_care.dto.DailyRecordResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * record 테이블용 MyBatis DAO - 업서트(INSERT ... ON CONFLICT ... RETURNING) - 단건 조회
 * (userId + date) - 기간 조회 (userId + start~end)
 *
 * 매퍼 XML: src/main/resources/mappers/mapper-record.xml
 */
@Mapper
public interface RecordDAO {

	DailyRecordResponseDTO upsert(@Param("userId") Long userId, @Param("recordDate") LocalDate recordDate,
			@Param("contentJson") String contentJson);

	/**
	 * 특정 날짜 단건 조회
	 */
	DailyRecordResponseDTO getOne(@Param("userId") Long userId, @Param("date") LocalDate date);

	/**
	 * 기간 조회 (start ~ end, 날짜 오름차순)
	 */
	List<DailyRecordResponseDTO> getRange(@Param("userId") Long userId, @Param("start") LocalDate start,
			@Param("end") LocalDate end);
}

// src/main/java/lx/project/dementia_care/dao/PeriodDAO.java
package lx.project.dementia_care.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Mapper
public interface PeriodDAO {

	// (기존) 범위로만 찾던 메서드 — 있어도 무방
	Integer findIdByRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

	// type + key 로 period_id 조회
	Integer findIdByTypeKey(@Param("periodType") String periodType, @Param("periodKey") String periodKey);

	// type + key 로 전체 레코드(날짜 비교용) 조회
	Map<String, Object> getByTypeKey(@Param("periodType") String periodType, @Param("periodKey") String periodKey);

	// INSERT (end는 포함으로 저장)
	int insert(@Param("periodType") String periodType, @Param("periodKey") String periodKey,
			@Param("start") LocalDate start, @Param("end") LocalDate endInclusive);

	// 날짜 보정
	int updateDates(@Param("periodId") Integer periodId, @Param("start") LocalDate start,
			@Param("end") LocalDate endInclusive);

	/**
	 * 핵심: 먼저 type+key로 찾고, 있으면 그 id 재사용(필요 시 날짜 보정), 없으면 INSERT 후 id 반환. 컨트롤러/서비스는
	 * [start, end)로 넘기므로 여기서 end-1일로 맞춰 저장/비교.
	 */
	default Integer ensureId(String periodType, String periodKey, LocalDate start, LocalDate endExclusive) {

		final String normType = (periodType == null) ? "" : periodType.toLowerCase(); // DB 트리거와 정합
		final LocalDate endIncl = endExclusive.minusDays(1); // DB는 end '포함'

		// 1) type+key로 먼저 조회
		Integer id = findIdByTypeKey(normType, periodKey);
		if (id != null) {
			// 1-1) 필요 시 날짜 보정
			Map<String, Object> row = getByTypeKey(normType, periodKey);
			if (row != null) {
				LocalDate dbStart = coerceToLocalDate(row.get("start_date"));
				LocalDate dbEnd = coerceToLocalDate(row.get("end_date"));

				// 둘 다 값이 있을 때만 비교, 다르면 보정
				if (dbStart != null && dbEnd != null) {
					boolean needUpdate = !(dbStart.equals(start) && dbEnd.equals(endIncl));
					if (needUpdate) {
						updateDates(id, start, endIncl);
					}
				}
			}
			return id;
		}

		// 2) 없으면 새로 생성 후 id 재조회
		insert(normType, periodKey, start, endIncl);
		return findIdByTypeKey(normType, periodKey);
	}

	/* ---------- 안전 변환 헬퍼 ---------- */

	/**
	 * MyBatis resultType="map"으로 읽은 start_date/end_date는 드라이버/설정에 따라 LocalDate,
	 * java.sql.Date, Timestamp, String 등으로 올 수 있음. 어떤 경우든 LocalDate로 최대한 안전하게 변환.
	 */
	static LocalDate coerceToLocalDate(Object v) {
		if (v == null)
			return null;

		// 이미 LocalDate
		if (v instanceof LocalDate ld)
			return ld;

		// java.sql.Date
		if (v instanceof java.sql.Date d)
			return d.toLocalDate();

		// java.sql.Timestamp
		if (v instanceof Timestamp ts)
			return ts.toLocalDateTime().toLocalDate();

		// java.util.Date
		if (v instanceof java.util.Date ud) {
			return ud.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}

		// 문자열: "yyyy-MM-dd" 또는 "yyyy-MM-ddTHH:mm:ss" 같은 형식 보호
		if (v instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.length() >= 10) {
				String d = s.substring(0, 10); // 앞의 yyyy-MM-dd만 취함
				try {
					return LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE);
				} catch (DateTimeParseException ignore) {
					// no-op
				}
			}
		}

		// 변환 실패
		return null;
	}
}

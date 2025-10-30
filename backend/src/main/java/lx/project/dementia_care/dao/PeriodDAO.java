// src/main/java/lx/project/dementia_care/dao/PeriodDAO.java
package lx.project.dementia_care.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface PeriodDAO {

	/** 1) type+key 단건 조회 */
	@Select("""
			SELECT period_id
			  FROM period
			 WHERE period_type = #{type}
			   AND period_key  = #{key}
			 LIMIT 1
			""")
	Integer findIdByTypeKey(@Param("type") String type, @Param("key") String key);

	/**
	 * 2) 보장 업서트: (type,key,start,end)로 INSERT 시도 후 이미 있으면 기존 행을 건드리지 않고 같은 행의
	 * period_id를 RETURNING으로 돌려받는다. - DO UPDATE를 쓰는 이유: DO NOTHING은 RETURNING이 비어
	 * 버리기 때문. - 여기서는 컬럼 값을 바꾸지 않도록 기존 값을 그대로 재설정한다.
	 */
	@Select("""
			INSERT INTO period (period_type, period_key, start_date, end_date)
			VALUES (#{type}, #{key}, #{start}, #{end})
			ON CONFLICT (period_type, period_key)
			DO UPDATE SET
			    start_date = period.start_date,
			    end_date   = period.end_date
			RETURNING period_id
			""")
	Integer upsertReturningId(@Param("type") String type, @Param("key") String key, @Param("start") LocalDate start,
			@Param("end") LocalDate end);

	/** 3) 항상 period_id 확보 (최종 1회 호출로 끝나지만, 이중 안전장치로 조회를 한 번 더 둠) */
	default Integer ensureId(String type, String key, LocalDate start, LocalDate end) {
		Integer id = upsertReturningId(type, key, start, end);
		return (id != null) ? id : findIdByTypeKey(type, key);
	}

	/** (선택) 기존 코드 호환용: start/end로 조회 */
	@Select("""
			SELECT period_id
			  FROM period
			 WHERE start_date = #{start}
			   AND end_date   = #{end}
			 LIMIT 1
			""")
	Integer findIdByRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

// src/main/java/lx/project/dementia_care/dao/PeriodDAO.java
package lx.project.dementia_care.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface PeriodDAO {
	Integer findIdByRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

	int insert(@Param("start") LocalDate start, @Param("end") LocalDate end);

	default Integer getOrCreateYear(LocalDate start, LocalDate end) {
		Integer id = findIdByRange(start, end);
		if (id != null)
			return id;
		insert(start, end);
		return findIdByRange(start, end);
	}
}
